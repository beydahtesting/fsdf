package com.example.mcq_grader;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.exifinterface.media.ExifInterface;
import com.github.chrisbanes.photoview.PhotoView;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class StudentImageActivity extends Activity {
    private static final String TAG = "StudentImageActivity";
    private static final int REQUEST_CAMERA = 1;
    private static final int REQUEST_GALLERY = 2;
    private Uri photoURI;
    private String currentPhotoPath;  // Store full file path here
    private Bitmap studentBitmap;
    private PhotoView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_image);

        imageView = findViewById(R.id.student_image_view);
        Button btnCamera = findViewById(R.id.btn_camera);
        Button btnGallery = findViewById(R.id.btn_gallery);
        Button btnRotate = findViewById(R.id.btn_rotate);
        Button btnFlip = findViewById(R.id.btn_flip);
        Button btnNext = findViewById(R.id.btn_next);

        btnCamera.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA);
            } else {
                dispatchTakePictureIntent();
            }
        });

        btnGallery.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_GALLERY);
            } else {
                launchGallery();
            }
        });

        btnRotate.setOnClickListener(v -> {
            if (studentBitmap != null) {
                studentBitmap = rotateImage(studentBitmap, 90);
                imageView.setImageBitmap(studentBitmap);
            }
        });

        btnFlip.setOnClickListener(v -> {
            if (studentBitmap != null) {
                studentBitmap = flipImage(studentBitmap);
                imageView.setImageBitmap(studentBitmap);
            }
        });

        btnNext.setOnClickListener(v -> {
            if (studentBitmap != null) {
                new ProcessStudentImageTask().execute(studentBitmap);
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
                Toast.makeText(this, "Could not create file for image", Toast.LENGTH_SHORT).show();
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
        String imageFileName = "STUDENT_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath(); // Save full path here
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
        if (requestCode == REQUEST_CAMERA && resultCode == RESULT_OK) {
            try {
                // Use the stored absolute path instead of photoURI.getPath()
                File file = new File(currentPhotoPath);
                if (!file.exists()) {
                    Log.e(TAG, "File not found: " + file.getAbsolutePath());
                }
                studentBitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                studentBitmap = rotateImageIfRequired(studentBitmap, photoURI);
                imageView.setImageBitmap(studentBitmap);
            } catch (IOException e) {
                Log.e(TAG, "Error processing captured image", e);
                Toast.makeText(this, "Error processing image orientation", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_GALLERY && resultCode == RESULT_OK && data != null) {
            try {
                Uri selectedImage = data.getData();
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImage);
                studentBitmap = rotateImageIfRequired(bitmap, selectedImage);
                imageView.setImageBitmap(studentBitmap);
            } catch (IOException e) {
                Log.e(TAG, "Error processing gallery image", e);
                Toast.makeText(this, "Error processing image orientation", Toast.LENGTH_SHORT).show();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    // AsyncTask to process the student image: warp/crop and detect filled circles.
    private class ProcessStudentImageTask extends AsyncTask<Bitmap, Void, Bitmap> {
        @Override
        protected Bitmap doInBackground(Bitmap... bitmaps) {
            Bitmap input = bitmaps[0];
            Mat origMat = new Mat();
            Utils.bitmapToMat(input, origMat);
            // Call your processing function (which performs warping/cropping)
            Mat processedMat = ImageProcessor.processImage(origMat);
            origMat.release();
            if (processedMat == null || processedMat.empty()) {
                return null;
            }
            // Overlay detected circles
            Mat outputMat = ImageProcessor.drawDetectedCircles(processedMat);
            processedMat.release();
            Bitmap outputBitmap = Bitmap.createBitmap(outputMat.cols(), outputMat.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(outputMat, outputBitmap);
            outputMat.release();
            return outputBitmap;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            if (result != null) {
                ImageCache.getInstance().setStudentImage(result);
                startActivity(new Intent(StudentImageActivity.this, StudentConfirmActivity.class));
            } else {
                Toast.makeText(StudentImageActivity.this, "Image processing failed. Please try again.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
