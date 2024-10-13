package com.example.cameramlkit;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Rect;
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
import androidx.camera.mlkit.vision.MlKitAnalyzer;
import androidx.camera.view.LifecycleCameraController;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.cameramlkit.databinding.ActivityCameraTextRecBinding;
import com.example.cameramlkit.helpers.BoundingBoxDrawing;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraTextRecActivity extends AppCompatActivity {

    ActivityCameraTextRecBinding binding;
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
        binding = ActivityCameraTextRecBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (ContextCompat.checkSelfPermission(CameraTextRecActivity.this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
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

        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        cameraController.setImageAnalysisAnalyzer(cameraExecutor,
                new MlKitAnalyzer(List.of(recognizer),
                        ImageAnalysis.COORDINATE_SYSTEM_VIEW_REFERENCED,
                        cameraExecutor,
                        result -> {
                            Text visionText = result.getValue(recognizer);
                            processText(visionText);
                        }
                )
        );

        cameraController.bindToLifecycle(this);
        binding.cameraPreview.setController(cameraController);
    }

    private void processText(Text visionText) {
        binding.cameraPreview.getOverlay().clear();
        if (binding.switchTextRec.isChecked()) {

            for (Text.TextBlock block : visionText.getTextBlocks()) {
                for (Text.Line line : block.getLines()) {
                    for (Text.Element element : line.getElements()) {
                        String elementText = element.getText();
                        Rect boundingBox = element.getBoundingBox();

                        int rectColor = Color.YELLOW;
                        int textColor = Color.BLUE;
                        if (elementText.equalsIgnoreCase("Dulergina")) { //to highlight some element in red bounding box
                            rectColor = Color.RED;
                        }
                        binding.cameraPreview.getOverlay().add(new BoundingBoxDrawing(boundingBox, rectColor, elementText, textColor));

                    }
                }
            }
        }
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
                        Toast.makeText(CameraTextRecActivity.this, "Photo taken correctly", Toast.LENGTH_SHORT).show();
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