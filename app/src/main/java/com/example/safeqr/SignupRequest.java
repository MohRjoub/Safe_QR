package com.example.safeqr;

public class SignupRequest {
    private String name;
    private String email;
    private String password;
    private String phone_number;

    public SignupRequest(String name, String email, String password, String phone) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.phone_number = phone;
    }
}
