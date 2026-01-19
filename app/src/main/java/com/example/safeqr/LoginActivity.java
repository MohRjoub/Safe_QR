package com.example.safeqr;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {
    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvSignUp, tvForgotPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initViews();
        setClickListeners();
    }

    private void initViews() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvSignUp = findViewById(R.id.tvSignUp);

        String text = getString(R.string.no_account);
        SpannableString spannable = new SpannableString(text);

        int start = text.indexOf("Sign up");
        int end = start + "Sign up".length();

        spannable.setSpan(
                new ForegroundColorSpan(getResources().getColor(R.color.colorPrimary)),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        spannable.setSpan(
                new StyleSpan(Typeface.BOLD),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        tvSignUp.setText(spannable);

        tvForgotPassword = findViewById(R.id.tvForgotPassword);
    }

    private void setClickListeners() {
        btnLogin.setOnClickListener(v -> handleLogin());
        tvSignUp.setOnClickListener(v -> startActivity(new Intent(this, SignupActivity.class)));

        tvForgotPassword.setOnClickListener(v -> {
            startActivity(new Intent(this, ForgotPasswordActivity.class));
        });
    }

    private void handleLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (!ValidationUtils.isValidEmail(email)) {
            etEmail.setError("Invalid email");
            return;
        }
        if (password.isEmpty()) {
            etPassword.setError("Password required");
            return;
        }

        LoginRequest request = new LoginRequest(email, password);

        ApiService.post(ApiEndpoints.LOGIN, request, Object.class, new ApiService.ApiCallback<Object>() {
            @Override
            public void onSuccess(Object response) {
                Intent intent = new Intent(LoginActivity.this, VerifyOtpActivity.class);
                intent.putExtra("email", email);
                startActivity(intent);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(LoginActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
