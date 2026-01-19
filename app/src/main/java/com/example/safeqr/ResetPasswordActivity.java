package com.example.safeqr;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ResetPasswordActivity extends AppCompatActivity {

    private EditText etOtp, etNewPassword;
    private Button btnReset;
    private String email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        email = getIntent().getStringExtra("email");

        etOtp = findViewById(R.id.etOtp);
        etNewPassword = findViewById(R.id.etNewPassword);
        btnReset = findViewById(R.id.btnReset);

        btnReset.setOnClickListener(v -> resetPassword());
    }

    private void resetPassword() {
        String otp = etOtp.getText().toString().trim();
        String newPass = etNewPassword.getText().toString().trim();

        if (email == null || email.trim().isEmpty()) {
            Toast.makeText(this, "Missing email", Toast.LENGTH_SHORT).show();
            return;
        }

        if (otp.isEmpty()) {
            etOtp.setError("OTP required");
            return;
        }

        if (newPass.isEmpty()) {
            etNewPassword.setError("Password required");
            return;
        }

        if (!ValidationUtils.isValidPassword(newPass)) {
            etNewPassword.setError("8 chars, one uppercase, one number, and one symbol minimum");
            return;
        }

        ResetPasswordRequest request = new ResetPasswordRequest(email, otp, newPass);

        ApiService.post(ApiEndpoints.RESET_PASSWORD, request, Object.class,
                new ApiService.ApiCallback<Object>() {
                    @Override
                    public void onSuccess(Object response) {
                        Toast.makeText(ResetPasswordActivity.this, "Password updated. Please login.", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(ResetPasswordActivity.this, LoginActivity.class));
                        finishAffinity();
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(ResetPasswordActivity.this, error, Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
