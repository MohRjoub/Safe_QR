package com.example.safeqr;

public class ApiEndpoints {
    public static final String BASE_URL = "http://127.0.0.1:8080/safeqr/";
    public static final String SIGNUP = BASE_URL + "auth/signup.php";
    public static final String LOGIN = BASE_URL + "auth/login.php";
    public static final String VERIFY_OTP = BASE_URL + "auth/verify-otp.php";
    public static final String RESEND_OTP = BASE_URL + "/auth/resend_otp.php";
    public static final String FORGOT_PASSWORD = BASE_URL + "/auth/forgot_password.php";
    public static final String RESET_PASSWORD  = BASE_URL + "/auth/reset_password.php";

}
