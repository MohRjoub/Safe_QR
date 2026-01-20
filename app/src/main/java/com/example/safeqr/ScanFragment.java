package com.example.safeqr;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Response;

@ExperimentalGetImage
public class ScanFragment extends Fragment {

    private PreviewView previewView;
    private MaterialButton btnGallery, btnFlash;

    private ExecutorService cameraExecutor;
    private BarcodeScanner barcodeScanner;
    private Camera camera;
    private boolean torchOn = false;
    private volatile boolean isProcessingScan = false;

    private final OkHttpClient httpClient = new OkHttpClient();
    private static final String API_BASE = ApiEndpoints.BASE_URL;

    private String lastRawValue = null;
    private long lastScanAtMs = 0;
    private static final long SAME_QR_COOLDOWN_MS = 3000;

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) startCamera();
                else Toast.makeText(requireContext(), "Camera permission denied", Toast.LENGTH_SHORT).show();
            });

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) scanQrFromGalleryImage(uri);
            });

    public ScanFragment() { }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_scan, container, false);

        previewView = view.findViewById(R.id.previewView);
        btnGallery = view.findViewById(R.id.btnGallery);
        btnFlash = view.findViewById(R.id.btnFlash);

        cameraExecutor = Executors.newSingleThreadExecutor();

        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build();
        barcodeScanner = BarcodeScanning.getClient(options);

        btnGallery.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        btnFlash.setOnClickListener(v -> toggleFlash());

        Context ctx = requireContext();
        if (!ctx.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            btnFlash.setEnabled(false);
            btnFlash.setAlpha(0.4f);
        }

        if (hasCameraPermission()) startCamera();
        else cameraPermissionLauncher.launch(Manifest.permission.CAMERA);

        return view;
    }

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(requireContext());

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis analysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                analysis.setAnalyzer(cameraExecutor, this::analyzeFrame);

                cameraProvider.unbindAll();
                camera = cameraProvider.bindToLifecycle(
                        getViewLifecycleOwner(),
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis
                );

            } catch (Exception e) {
                Toast.makeText(requireContext(), "Camera error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void analyzeFrame(@NonNull ImageProxy imageProxy) {
        if (isProcessingScan || imageProxy.getImage() == null) {
            imageProxy.close();
            return;
        }

        InputImage image = InputImage.fromMediaImage(
                imageProxy.getImage(),
                imageProxy.getImageInfo().getRotationDegrees()
        );

        barcodeScanner.process(image)
                .addOnSuccessListener(barcodes -> {
                    if (!barcodes.isEmpty()) {
                        String raw = barcodes.get(0).getRawValue();
                        if (raw != null && !raw.trim().isEmpty()) {
                            handleScannedQr(raw.trim());
                        }
                    }
                })
                .addOnCompleteListener(task -> imageProxy.close());
    }

    private void toggleFlash() {
        if (camera == null) return;
        torchOn = !torchOn;
        camera.getCameraControl().enableTorch(torchOn);
        btnFlash.setIconResource(torchOn ? R.drawable.ic_flash_on : R.drawable.ic_flash);
    }

    private void scanQrFromGalleryImage(Uri uri) {
        try {
            InputImage image = InputImage.fromFilePath(requireContext(), uri);
            barcodeScanner.process(image)
                    .addOnSuccessListener(barcodes -> {
                        if (!barcodes.isEmpty()) {
                            String raw = barcodes.get(0).getRawValue();
                            if (raw != null) handleScannedQr(raw.trim());
                        } else {
                            Toast.makeText(requireContext(), "No QR found", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(requireContext(), "Gallery scan error: " + e.getMessage(), Toast.LENGTH_LONG).show()
                    );
        } catch (Exception e) {
            Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void handleScannedQr(String rawValue) {
        long now = System.currentTimeMillis();

        if (lastRawValue != null && lastRawValue.equals(rawValue)
                && (now - lastScanAtMs) < SAME_QR_COOLDOWN_MS) {
            return;
        }
        if (isProcessingScan) return;

        isProcessingScan = true;
        lastRawValue = rawValue;
        lastScanAtMs = now;

        String url = extractFirstUrl(rawValue);
        if (url == null) {
            Toast.makeText(requireContext(), "Not a URL", Toast.LENGTH_SHORT).show();
            isProcessingScan = false;
            return;
        }

        analyzeOnServerAndOpen(url);
    }

    private void analyzeOnServerAndOpen(String url) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                SharedPreferences sp = requireContext().getSharedPreferences("safeqr_prefs", 0);
                int userId = sp.getInt("user_id", 4); // fallback

                JSONObject payload = new JSONObject();
                payload.put("user_id", userId);
                payload.put("url", url);

                okhttp3.Request okReq = new okhttp3.Request.Builder()
                        .url(API_BASE + "analyze_and_save.php")
                        .post(RequestBody.create(
                                payload.toString(),
                                MediaType.parse("application/json; charset=utf-8")
                        ))
                        .build();

                Response res = httpClient.newCall(okReq).execute();
                String resStr = (res.body() != null) ? res.body().string() : "{}";

                JSONObject obj = new JSONObject(resStr);
                if (!obj.optBoolean("ok", false)) {
                    String err = obj.optString("error", "Unknown error");
                    if (isAdded()) requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), err, Toast.LENGTH_LONG).show()
                    );
                    return;
                }

                String status = obj.optString("status", "unknown");
                double risk = obj.isNull("risk_score") ? -1 : obj.optDouble("risk_score", -1);
                JSONObject reasonsObj = obj.optJSONObject("reasons");
                String reasonsJson = (reasonsObj != null) ? reasonsObj.toString() : "";

                if (isAdded()) requireActivity().runOnUiThread(() -> {
                    Intent intent = new Intent(requireContext(), ScanResultActivity.class);
                    intent.putExtra(ScanResultActivity.EXTRA_URL, url);
                    intent.putExtra(ScanResultActivity.EXTRA_STATUS, status);
                    intent.putExtra(ScanResultActivity.EXTRA_RISK, risk);
                    intent.putExtra(ScanResultActivity.EXTRA_REASONS_JSON, reasonsJson);
                    startActivity(intent);
                });

            } catch (Exception e) {
                if (isAdded()) requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "API error: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            } finally {
                if (previewView != null) previewView.postDelayed(() -> isProcessingScan = false, 1200);
                else isProcessingScan = false;
            }
        });
    }

    private String extractFirstUrl(String text) {
        if (Patterns.WEB_URL.matcher(text).matches()) return text;
        for (String p : text.split("\\s+")) {
            if (Patterns.WEB_URL.matcher(p).matches()) return p;
        }
        return null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
            cameraExecutor = null;
        }
        if (barcodeScanner != null) {
            barcodeScanner.close();
            barcodeScanner = null;
        }
        camera = null;
        previewView = null;
    }
}
