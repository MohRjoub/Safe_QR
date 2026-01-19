package com.example.safeqr;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText etEmail;
    private Button btnSendCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        etEmail = findViewById(R.id.etEmail);
        btnSendCode = findViewById(R.id.btnSendCode);

        btnSendCode.setOnClickListener(v -> sendResetCode());
    }

    private void sendResetCode() {
        String email = etEmail.getText().toString().trim();

        if (!ValidationUtils.isValidEmail(email)) {
            etEmail.setError("Invalid email");
            return;
        }

        ForgotPasswordRequest request = new ForgotPasswordRequest(email);

        ApiService.post(ApiEndpoints.FORGOT_PASSWORD, request, Object.class,
                new ApiService.ApiCallback<Object>() {
                    @Override
                    public void onSuccess(Object response) {
                        Toast.makeText(ForgotPasswordActivity.this, "If the email exists, a reset code was sent.", Toast.LENGTH_SHORT).show();

                        Intent i = new Intent(ForgotPasswordActivity.this, ResetPasswordActivity.class);
                        i.putExtra("email", email);
                        startActivity(i);
                        finish();
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(ForgotPasswordActivity.this, error, Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
