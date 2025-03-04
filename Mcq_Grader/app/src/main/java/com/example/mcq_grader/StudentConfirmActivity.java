package com.example.mcq_grader;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import com.github.chrisbanes.photoview.PhotoView;

public class StudentConfirmActivity extends Activity {
    private PhotoView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_confirm);

        imageView = findViewById(R.id.student_confirm_image_view);
        Bitmap processedBitmap = ImageCache.getInstance().getStudentImage();
        if (processedBitmap == null) {
            Toast.makeText(this, "No processed image found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        imageView.setImageBitmap(processedBitmap);

        Button btnRetake = findViewById(R.id.btn_student_retake);
        Button btnConfirm = findViewById(R.id.btn_student_confirm);

        btnRetake.setOnClickListener(v -> {
            startActivity(new Intent(StudentConfirmActivity.this, StudentImageActivity.class));
            finish();
        });

        btnConfirm.setOnClickListener(v -> {
            startActivity(new Intent(StudentConfirmActivity.this, TeacherImageActivity.class));
            finish();
        });
    }
}
