package com.example.cameramlkit;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.media.Image;
import android.net.Uri;
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
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.UseCaseGroup;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.mlkit.vision.MlKitAnalyzer;
import androidx.camera.view.LifecycleCameraController;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.cameramlkit.databinding.ActivityCameraObjRecBinding;
import com.example.cameramlkit.helpers.BoundingBoxDrawing;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraObjRecActivity extends AppCompatActivity {

    ActivityCameraObjRecBinding binding;
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
        binding = ActivityCameraObjRecBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (ContextCompat.checkSelfPermission(CameraObjRecActivity.this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
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

        ObjectDetectorOptions options = new ObjectDetectorOptions.Builder()
                .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
                .enableClassification()
                .enableMultipleObjects()
                .build();

        ObjectDetector objectDetector = ObjectDetection.getClient(options);

        cameraController.setImageAnalysisAnalyzer(cameraExecutor,
                new MlKitAnalyzer(
                        List.of(objectDetector),
                        ImageAnalysis.COORDINATE_SYSTEM_VIEW_REFERENCED,
                        cameraExecutor,
                        result -> {
                            List<DetectedObject> detectedObjects = result.getValue(objectDetector);
                            if (detectedObjects != null)
                                processObjs(detectedObjects);
                        }
                )
        );

        cameraController.bindToLifecycle(this);
        binding.cameraPreview.setController(cameraController);
    }

    private void processObjs(List<DetectedObject> detectedObjects) {
        binding.cameraPreview.getOverlay().clear();

        for (DetectedObject detectedObject : detectedObjects) {
            Rect boundingBox = detectedObject.getBoundingBox();
            for (DetectedObject.Label label : detectedObject.getLabels()) {
                String text = label.getText();
                float confidence = label.getConfidence();
                //Log.d("info", "boundingBox.toString(): " + boundingBox.flattenToString() +  "| trackingId: " + trackingId + " | text: " + text);
                String showingText = text + ": " + (Math.round(confidence * 10000) / 100.f) + "%";
                binding.cameraPreview.getOverlay().add(new BoundingBoxDrawing(boundingBox, Color.RED, showingText, Color.BLACK));

                /*only 5 categories
                    PredefinedCategory.FOOD
                    PredefinedCategory.FASHION_GOOD
                    PredefinedCategory.HOME_GOOD
                    PredefinedCategory.PLACE
                    PredefinedCategory.PLANT
                */
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
                        Toast.makeText(CameraObjRecActivity.this, "Photo taken correctly", Toast.LENGTH_SHORT).show();
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