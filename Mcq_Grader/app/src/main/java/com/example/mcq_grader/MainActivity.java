package com.example.mcq_grader;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import org.opencv.android.OpenCVLoader;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";
    static {
        if (!OpenCVLoader.initDebug()) {
            Log.e(TAG, "OpenCV initialization failed!");
        } else {
            System.loadLibrary("opencv_java4"); // Ensure library name matches your setup.
            Log.i(TAG, "OpenCV loaded successfully!");
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnStudent = findViewById(R.id.btn_student);
        Button btnTeacher = findViewById(R.id.btn_teacher);

        btnStudent.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, StudentImageActivity.class))
        );
        btnTeacher.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, TeacherImageActivity.class))
        );
    }
}
