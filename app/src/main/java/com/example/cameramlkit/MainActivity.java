package com.example.cameramlkit;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.cameramlkit.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        binding.btnCameraSimple.setOnClickListener(view -> {
            startActivity(new Intent(MainActivity.this, CameraSimpleActivity.class));
        });

        //btn to open camera with text recognition
        binding.btnCameraTextRec.setOnClickListener(view -> {
            startActivity(new Intent(MainActivity.this, CameraTextRecActivity.class));
        });

        binding.btnImageLabeling.setOnClickListener(view -> {
            startActivity(new Intent(MainActivity.this, CameraLabelActivity.class));
        });

        binding.btnCameraObjDetec.setOnClickListener(view -> {
            startActivity(new Intent(MainActivity.this, CameraObjRecActivity.class));
        });

        binding.btnCameraMyObjDetec.setOnClickListener(view -> {
            startActivity(new Intent(MainActivity.this, CameraMyObjRecActivity.class));
        });
    }
}