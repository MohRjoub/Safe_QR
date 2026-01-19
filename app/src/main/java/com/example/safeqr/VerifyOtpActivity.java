package com.example.safeqr;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class VerifyOtpActivity extends AppCompatActivity {

    private EditText etOtp;
    private TextView tvResend;
    private String email;

    private CountDownTimer resendTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_otp);

        email = getIntent().getStringExtra("email");

        etOtp = findViewById(R.id.etOtp);
        tvResend = findViewById(R.id.tvResend);

        findViewById(R.id.btnVerify).setOnClickListener(v -> verifyOtp());
        tvResend.setOnClickListener(v -> resendOtp());
    }

    private void verifyOtp() {
        String otp = etOtp.getText().toString().trim();

        if (email == null || email.trim().isEmpty()) {
            Toast.makeText(this, "Missing email", Toast.LENGTH_SHORT).show();
            return;
        }

        if (otp.isEmpty()) {
            Toast.makeText(this, "Please enter the OTP", Toast.LENGTH_SHORT).show();
            return;
        }

        OtpRequest request = new OtpRequest(email, otp);

        ApiService.postFull(ApiEndpoints.VERIFY_OTP, request, VerifyOtpData.class,
                new ApiService.ApiCallback<ApiResponse<VerifyOtpData>>() {
                    @Override
                    public void onSuccess(ApiResponse<VerifyOtpData> res) {
                        Toast.makeText(VerifyOtpActivity.this, res.getMessage(), Toast.LENGTH_SHORT).show();

                        if (!res.isSuccess()) return;

                        VerifyOtpData data = res.getData();
                        String token = data.getToken();
                        int userId = data.getUserId();

                        SessionManager.saveToken(VerifyOtpActivity.this, token);

                        Intent intent = new Intent(VerifyOtpActivity.this, DashboardActivity.class);
                        intent.putExtra("userId", userId);
                        SharedPreferences prefs = getSharedPreferences("safeqr_prefs", MODE_PRIVATE);
                        prefs.edit().putInt("user_id", userId).apply();
                        startActivity(intent);
                        finishAffinity();
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(VerifyOtpActivity.this, error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void resendOtp() {
        if (email == null || email.trim().isEmpty()) {
            Toast.makeText(this, "Missing email", Toast.LENGTH_SHORT).show();
            return;
        }

        setResendEnabled(false);

        ResendOtpRequest request = new ResendOtpRequest(email);

        ApiService.postFull(ApiEndpoints.RESEND_OTP, request, Object.class,
                new ApiService.ApiCallback<ApiResponse<Object>>() {
                    @Override
                    public void onSuccess(ApiResponse<Object> res) {
                        Toast.makeText(VerifyOtpActivity.this, res.getMessage(), Toast.LENGTH_SHORT).show();

                        if (res.isSuccess()) {
                            startResendCountdown(30);
                        } else {
                            resetResend();
                        }
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(VerifyOtpActivity.this, error, Toast.LENGTH_SHORT).show();
                        resetResend();
                    }
                });
    }

    private void startResendCountdown(int seconds) {
        if (resendTimer != null) resendTimer.cancel();

        resendTimer = new CountDownTimer(seconds * 1000L, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                long s = millisUntilFinished / 1000L;
                tvResend.setText("Resend (" + s + "s)");
            }

            @Override
            public void onFinish() {
                resetResend();
            }
        }.start();
    }

    private void resetResend() {
        if (resendTimer != null) {
            resendTimer.cancel();
            resendTimer = null;
        }
        tvResend.setText("Resend");
        setResendEnabled(true);
    }

    private void setResendEnabled(boolean enabled) {
        tvResend.setEnabled(enabled);
        tvResend.setAlpha(enabled ? 1f : 0.5f);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (resendTimer != null) resendTimer.cancel();
    }
}
