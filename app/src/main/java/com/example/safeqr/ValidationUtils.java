package com.example.safeqr;

import android.util.Patterns;

public class ValidationUtils {
    public static boolean isValidEmail(String email) {
        return email != null && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    public static boolean isValidPassword(String password) {
        if (password == null || password.length() < 8) return false;
        boolean hasUppercase = !password.equals(password.toLowerCase());
        boolean hasDigit = password.matches(".*\\d.*");
        boolean hasSpecial = password.matches(".*[^A-Za-z0-9].*");
        return hasUppercase && hasDigit && hasSpecial;
    }

    public static boolean isValidPhone(String phone) {
        return phone != null && phone.matches("^\\+\\d{10,15}$");
    }
}
