package com.example.cameramlkit;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.mlkit.vision.MlKitAnalyzer;
import androidx.camera.view.LifecycleCameraController;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.cameramlkit.databinding.ActivityCameraLabelBinding;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraLabelActivity extends AppCompatActivity {

    ActivityCameraLabelBinding binding;
    CameraSelector lensFacingPosition = CameraSelector.DEFAULT_BACK_CAMERA;
    ExecutorService cameraExecutor = Executors.newSingleThreadExecutor();
    LifecycleCameraController cameraController;

    ActivityResultLauncher<String> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), result -> {
        if (result) {
            initCameraProvider();
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityCameraLabelBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (ContextCompat.checkSelfPermission(CameraLabelActivity.this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            activityResultLauncher.launch(Manifest.permission.CAMERA);
        } else {
            initCameraProvider();
        }

        binding.imageButton.setOnClickListener(view -> takePhoto());

        binding.btnSwitchCamera.setOnClickListener(view -> changeCameraLens());
    }


    void initCameraProvider() {

        cameraController = new LifecycleCameraController(binding.getRoot().getContext());
        cameraController.setCameraSelector(lensFacingPosition);

        ImageLabeler labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS);

        cameraController.setImageAnalysisBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST);
        cameraController.setImageAnalysisAnalyzer(cameraExecutor,
                new MlKitAnalyzer(
                        List.of(labeler),
                        ImageAnalysis.COORDINATE_SYSTEM_VIEW_REFERENCED,
                        cameraExecutor,
                        result -> {
                            List<ImageLabel> labels = result.getValue(labeler);
                            if (labels != null)
                                processLabels(labels);
                        }
                )
        );

        cameraController.bindToLifecycle(this);
        binding.cameraPreview.setController(cameraController);
    }

    public void processLabels(List<ImageLabel> labels) {
        StringBuilder showingText = new StringBuilder();
        for (ImageLabel label : labels) {
            String text = label.getText();
            float confidence = label.getConfidence();

            showingText.append("Elem: ")
                    .append(text)
                    .append(" | Conf: ")
                    .append(Math.round(confidence * 10000) / 100.f)
                    .append("% \n");
        }
        Log.d("infoAA", showingText.toString());
        String finalShowingText = showingText.toString();
        runOnUiThread(() -> {
            // Update UI elements here
            binding.textView.setText("");
            binding.textView.setText(finalShowingText);
        });
        Log.d("infoAA", "--------------------");
    }


    public void changeCameraLens() {
        lensFacingPosition = lensFacingPosition == CameraSelector.DEFAULT_BACK_CAMERA ?
                CameraSelector.DEFAULT_FRONT_CAMERA :
                CameraSelector.DEFAULT_BACK_CAMERA;
        cameraController.setCameraSelector(lensFacingPosition);
    }

    public void takePhoto() {
        long timestamp = System.currentTimeMillis();

        ImageCapture.OutputFileOptions outputFileOptions = getOutputFileOptions(timestamp);

        cameraController.takePicture(outputFileOptions,
                ContextCompat.getMainExecutor(binding.getRoot().getContext()),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Toast.makeText(CameraLabelActivity.this, "Photo taken correctly", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e("info", exception.getMessage());
                    }
                });
    }

    private ImageCapture.OutputFileOptions getOutputFileOptions(long timestamp) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, timestamp);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_DCIM);
        }

        return new ImageCapture.OutputFileOptions.Builder(binding.getRoot().getContext().getContentResolver(),
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues)
                .build();
    }


}