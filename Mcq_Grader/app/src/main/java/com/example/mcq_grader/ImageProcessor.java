package com.example.mcq_grader;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import java.util.ArrayList;
import java.util.List;

public class ImageProcessor {

    // Reorder 4 points to [top-left, top-right, bottom-right, bottom-left]
    public static MatOfPoint2f reorderPoints(MatOfPoint2f points) {
        Point[] pts = points.toArray();
        if (pts.length != 4) return points;

        Point[] ordered = new Point[4];
        double[] sums = new double[4];
        double[] diffs = new double[4];

        for (int i = 0; i < 4; i++) {
            sums[i] = pts[i].x + pts[i].y;
            diffs[i] = pts[i].y - pts[i].x;
        }

        int tl = 0, br = 0, tr = 0, bl = 0;
        for (int i = 1; i < 4; i++) {
            if (sums[i] < sums[tl]) tl = i;
            if (sums[i] > sums[br]) br = i;
            if (diffs[i] < diffs[tr]) tr = i;
            if (diffs[i] > diffs[bl]) bl = i;
        }

        ordered[0] = pts[tl];
        ordered[1] = pts[tr];
        ordered[2] = pts[br];
        ordered[3] = pts[bl];

        return new MatOfPoint2f(ordered);
    }

    // Process the image: converts to grayscale, blurs, applies adaptive thresholding, detects edges,
    // finds the largest quadrilateral, and warps the perspective.
    public static Mat processImage(Mat image) {
        // Check that the image is loaded.
        if (image == null || image.empty()) {
            System.out.println("Error: Input image is null or empty");
            return image;
        }

        // Ensure image is in BGR (if not already).
        // (If the image has 4 channels, convert from RGBA to BGR.)
        if (image.channels() == 4) {
            Mat bgr = new Mat();
            Imgproc.cvtColor(image, bgr, Imgproc.COLOR_RGBA2BGR);
            image = bgr;
        } else if (image.channels() == 1) {
            Mat bgr = new Mat();
            Imgproc.cvtColor(image, bgr, Imgproc.COLOR_GRAY2BGR);
            image = bgr;
        }

        // Convert to grayscale.
        Mat gray = new Mat();
        Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);

        // Apply Gaussian blur.
        Mat blurred = new Mat();
        Imgproc.GaussianBlur(gray, blurred, new Size(5, 5), 0);

        // Apply adaptive threshold.
        Mat thresh = new Mat();
        Imgproc.adaptiveThreshold(blurred, thresh, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                Imgproc.THRESH_BINARY_INV, 11, 2);

        // Detect edges.
        Mat edges = new Mat();
        Imgproc.Canny(thresh, edges, 50, 150);

        // Find contours.
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(edges, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        // Release temporary Mats.
        gray.release();
        blurred.release();
        thresh.release();
        edges.release();

        if (!contours.isEmpty()) {
            // Find the largest contour.
            MatOfPoint largestContour = contours.get(0);
            for (MatOfPoint cnt : contours) {
                if (Imgproc.contourArea(cnt) > Imgproc.contourArea(largestContour)) {
                    largestContour = cnt;
                }
            }

            double perimeter = Imgproc.arcLength(new MatOfPoint2f(largestContour.toArray()), true);
            MatOfPoint2f approx = new MatOfPoint2f();
            Imgproc.approxPolyDP(new MatOfPoint2f(largestContour.toArray()), approx, 0.02 * perimeter, true);

            // If a quadrilateral is detected, warp the image.
            if (approx.total() == 4) {
                MatOfPoint2f orderedPts = reorderPoints(approx);
                double width = 700, height = 800;
                MatOfPoint2f dst = new MatOfPoint2f(
                        new Point(0, 0),
                        new Point(width - 1, 0),
                        new Point(width - 1, height - 1),
                        new Point(0, height - 1)
                );
                Mat matrix = Imgproc.getPerspectiveTransform(orderedPts, dst);
                Mat warped = new Mat();
                Imgproc.warpPerspective(image, warped, matrix, new Size(width, height));

                approx.release();
                orderedPts.release();
                matrix.release();
                dst.release();

                return warped;
            }
        }
        return image.clone();
    }

    // Detect filled circles in the image using HSV-based blue mask.
    // Returns a list of center points of detected circles.
    public static List<Point> detectFilledCircles(Mat image) {
        Mat hsv = new Mat();
        Imgproc.cvtColor(image, hsv, Imgproc.COLOR_BGR2HSV);

        Scalar lowerBlue = new Scalar(90, 50, 50);
        Scalar upperBlue = new Scalar(130, 255, 255);
        Mat mask = new Mat();
        Core.inRange(hsv, lowerBlue, upperBlue, mask);

        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(mask, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        List<Point> filledCircles = new ArrayList<>();
        for (MatOfPoint cnt : contours) {
            double area = Imgproc.contourArea(cnt);
            if (area > 200 && area < 5000) {
                Point center = new Point();
                float[] radius = new float[1];
                Imgproc.minEnclosingCircle(new MatOfPoint2f(cnt.toArray()), center, radius);
                filledCircles.add(center);
            }
        }
        hsv.release();
        mask.release();
        return filledCircles;
    }

    // Draw detected circles on the image for visualization (draws circles with fixed radius 20).
    public static Mat drawDetectedCircles(Mat image) {
        List<Point> circles = detectFilledCircles(image);
        Mat output = image.clone();
        for (Point p : circles) {
            Imgproc.circle(output, p, 20, new Scalar(0, 255, 0), 2);
        }
        return output;
    }

    // Compare teacher and student circles (grading). This method draws teacher circles in green outlines,
    // correct student answers (within threshold) filled in green, and wrong answers filled in red.
    public static Mat compareCircles(List<Point> teacherCircles, List<Point> studentCircles, Mat image) {
        double threshold = 25;
        List<Point> correctMatches = new ArrayList<>();
        List<Point> incorrectMatches = new ArrayList<>();

        for (Point s : studentCircles) {
            boolean matched = false;
            for (Point t : teacherCircles) {
                double dist = Math.hypot(t.x - s.x, t.y - s.y);
                if (dist < threshold) {
                    correctMatches.add(s);
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                incorrectMatches.add(s);
            }
        }

        Mat gradedImage = image.clone();
        // Draw teacher circles (fixed radius 20) in green.
        for (Point t : teacherCircles) {
            Imgproc.circle(gradedImage, t, 20, new Scalar(0, 255, 0), 3);
        }
        // Draw correct student answers filled in green.
        for (Point s : correctMatches) {
            Imgproc.circle(gradedImage, s, 20, new Scalar(0, 255, 0), -1);
        }
        // Draw incorrect student answers filled in red.
        for (Point s : incorrectMatches) {
            Imgproc.circle(gradedImage, s, 20, new Scalar(0, 0, 255), -1);
        }
        return gradedImage;
    }
}
