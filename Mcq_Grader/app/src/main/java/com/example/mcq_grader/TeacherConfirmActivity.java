package com.example.mcq_grader;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import com.github.chrisbanes.photoview.PhotoView;

public class TeacherConfirmActivity extends Activity {
    private PhotoView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_confirm);

        imageView = findViewById(R.id.teacher_confirm_image_view);
        Bitmap processedBitmap = ImageCache.getInstance().getTeacherImage();
        if (processedBitmap == null) {
            Toast.makeText(this, "No processed teacher image found!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        imageView.setImageBitmap(processedBitmap);

        Button btnRetake = findViewById(R.id.btn_teacher_retake);
        Button btnConfirm = findViewById(R.id.btn_teacher_confirm);

        btnRetake.setOnClickListener(v -> {
            startActivity(new Intent(TeacherConfirmActivity.this, TeacherImageActivity.class));
            finish();
        });

        btnConfirm.setOnClickListener(v -> {
            startActivity(new Intent(TeacherConfirmActivity.this, ResultActivity.class));
            finish();
        });
    }
}
