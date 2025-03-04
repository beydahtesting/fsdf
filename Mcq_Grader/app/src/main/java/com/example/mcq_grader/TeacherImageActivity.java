package com.example.mcq_grader;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import androidx.exifinterface.media.ExifInterface;
import com.github.chrisbanes.photoview.PhotoView;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TeacherImageActivity extends Activity {
    private static final String TAG = "TeacherImageActivity";
    private static final int REQUEST_CAMERA = 1;
    private static final int REQUEST_GALLERY = 2;
    private Uri photoURI;
    private String currentPhotoPath; // Store full absolute path here
    private Bitmap teacherBitmap;
    private PhotoView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_image);

        imageView = findViewById(R.id.teacher_image_view);
        Button btnCamera = findViewById(R.id.btn_teacher_camera);
        Button btnGallery = findViewById(R.id.btn_teacher_gallery);
        Button btnRotate = findViewById(R.id.btn_teacher_rotate);
        Button btnFlip = findViewById(R.id.btn_teacher_flip);
        Button btnNext = findViewById(R.id.btn_teacher_next);

        btnCamera.setOnClickListener(v -> dispatchTakePictureIntent());
        btnGallery.setOnClickListener(v -> launchGallery());

        btnRotate.setOnClickListener(v -> {
            if (teacherBitmap != null) {
                teacherBitmap = rotateImage(teacherBitmap, 90);
                imageView.setImageBitmap(teacherBitmap);
            }
        });

        btnFlip.setOnClickListener(v -> {
            if (teacherBitmap != null) {
                teacherBitmap = flipImage(teacherBitmap);
                imageView.setImageBitmap(teacherBitmap);
            }
        });

        btnNext.setOnClickListener(v -> {
            if (teacherBitmap != null) {
                new ProcessTeacherImageTask().execute(teacherBitmap);
            } else {
                Toast.makeText(this, "Please capture or select an image", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.e(TAG, "Error creating image file", ex);
                Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show();
                return;
            }
            if (photoFile != null) {
                photoURI = FileProvider.getUriForFile(this, "com.example.mcq_grader.fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_CAMERA);
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "TEACHER_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath(); // Save the full absolute path here
        return image;
    }

    private void launchGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_GALLERY);
    }

    private Bitmap rotateImageIfRequired(Bitmap img, Uri imageUri) throws IOException {
        ExifInterface ei = new ExifInterface(getContentResolver().openInputStream(imageUri));
        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        Log.i(TAG, "EXIF orientation: " + orientation);
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                return rotateImage(img, 90);
            case ExifInterface.ORIENTATION_ROTATE_180:
                return rotateImage(img, 180);
            case ExifInterface.ORIENTATION_ROTATE_270:
                return rotateImage(img, 270);
            default:
                return img;
        }
    }

    private Bitmap rotateImage(Bitmap img, int degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        return Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
    }

    private Bitmap flipImage(Bitmap img) {
        Matrix matrix = new Matrix();
        matrix.preScale(-1.0f, 1.0f);
        return Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            try {
                if (requestCode == REQUEST_CAMERA) {
                    // Use the stored absolute path to decode the image.
                    File file = new File(currentPhotoPath);
                    if (!file.exists()) {
                        Log.e(TAG, "File not found: " + file.getAbsolutePath());
                    }
                    teacherBitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                    teacherBitmap = rotateImageIfRequired(teacherBitmap, photoURI);
                } else if (requestCode == REQUEST_GALLERY && data != null) {
                    Uri selectedImage = data.getData();
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImage);
                    teacherBitmap = rotateImageIfRequired(bitmap, selectedImage);
                }
                imageView.setImageBitmap(teacherBitmap);
            } catch (IOException e) {
                Toast.makeText(this, "Error processing image orientation", Toast.LENGTH_SHORT).show();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private class ProcessTeacherImageTask extends AsyncTask<Bitmap, Void, Bitmap> {
        @Override
        protected Bitmap doInBackground(Bitmap... bitmaps) {
            Bitmap input = bitmaps[0];
            Mat origMat = new Mat();
            Utils.bitmapToMat(input, origMat);
            // Warp and crop the image
            Mat warpedMat = ImageProcessor.processImage(origMat);
            if (warpedMat == null || warpedMat.empty())
                return null;
            // Overlay detected circles using HSV-based detection
            Mat outputMat = ImageProcessor.drawDetectedCircles(warpedMat);
            Bitmap outputBitmap = Bitmap.createBitmap(outputMat.cols(), outputMat.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(outputMat, outputBitmap);
            return outputBitmap;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            if (result != null) {
                ImageCache.getInstance().setTeacherImage(result);
                startActivity(new Intent(TeacherImageActivity.this, TeacherConfirmActivity.class));
            } else {
                Toast.makeText(TeacherImageActivity.this, "Image processing failed. Please try again.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
