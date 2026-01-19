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

public class SignupActivity extends AppCompatActivity {
    private EditText etName, etEmail, etPassword, etPhone;
    private Button btnSignup;
    private TextView tvLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        initViews();
        setClickListeners();
    }

    private void initViews() {
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etPhone = findViewById(R.id.etPhone);
        btnSignup = findViewById(R.id.btnSignup);
        tvLogin = findViewById(R.id.tvLogin);
        String text = getString(R.string.have_account);
        SpannableString spannable = new SpannableString(text);

        int start = text.indexOf("Log in");
        int end = start + "Log in".length();

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

        tvLogin.setText(spannable);
    }

    private void setClickListeners() {
        btnSignup.setOnClickListener(v -> handleSignup());
        tvLogin.setOnClickListener(v -> startActivity(new Intent(this, LoginActivity.class)));
    }

    private void handleSignup() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        if (name.length() < 2) {
            etName.setError("Name too short");
            return;
        }
        if (!ValidationUtils.isValidEmail(email)) {
            etEmail.setError("Invalid email");
            return;
        }
        if (!ValidationUtils.isValidPassword(password)) {
            etPassword.setError("8 chars, one uppercase, one number, and one symbol minimum");
            return;
        }
        if (!ValidationUtils.isValidPhone(phone)) {
            etPhone.setError("Use +1234567890 format");
            return;
        }

        SignupRequest request = new SignupRequest(name, email, password, phone);
        ApiService.post(ApiEndpoints.SIGNUP, request, Object.class, new ApiService.ApiCallback<Object>() {
            @Override
            public void onSuccess(Object response) {
                Intent intent = new Intent(SignupActivity.this, VerifyOtpActivity.class);
                intent.putExtra("email", email);
                startActivity(intent);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(SignupActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}