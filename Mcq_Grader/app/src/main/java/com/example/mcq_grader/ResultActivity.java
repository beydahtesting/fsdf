package com.example.mcq_grader;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.TextView;
import com.github.chrisbanes.photoview.PhotoView;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;
import java.util.List;

public class ResultActivity extends Activity {
    private PhotoView teacherImageView, gradedImageView;
    private TextView resultTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        teacherImageView = findViewById(R.id.teacher_result_image_view);
        gradedImageView = findViewById(R.id.graded_image_view);
        resultTextView = findViewById(R.id.result_text_view);

        Bitmap teacherBitmap = ImageCache.getInstance().getTeacherImage();
        Bitmap studentBitmap = ImageCache.getInstance().getStudentImage();

        teacherImageView.setImageBitmap(teacherBitmap);

        Mat teacherMat = new Mat();
        Mat studentMat = new Mat();
        Utils.bitmapToMat(teacherBitmap, teacherMat);
        Utils.bitmapToMat(studentBitmap, studentMat);

        List<Point> teacherCircles = ImageProcessor.detectFilledCircles(teacherMat);
        List<Point> studentCircles = ImageProcessor.detectFilledCircles(studentMat);

        int correct = 0;
        double threshold = 50;
        for (Point s : studentCircles) {
            for (Point t : teacherCircles) {
                if (Math.hypot(s.x - t.x, s.y - t.y) < threshold) {
                    correct++;
                    break;
                }
            }
        }
        int total = teacherCircles.size();
        String text = correct + " / " + total + " correct";
        resultTextView.setText(text);

        Mat gradedMat = ImageProcessor.compareCircles(teacherCircles, studentCircles, studentMat);
        Imgproc.putText(gradedMat, text, new org.opencv.core.Point(gradedMat.cols() - 400, 50),
                Imgproc.FONT_HERSHEY_SIMPLEX, 1.2, new org.opencv.core.Scalar(0, 0, 255), 3);
        Bitmap gradedBitmap = Bitmap.createBitmap(gradedMat.cols(), gradedMat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(gradedMat, gradedBitmap);
        gradedImageView.setImageBitmap(gradedBitmap);
    }
}
