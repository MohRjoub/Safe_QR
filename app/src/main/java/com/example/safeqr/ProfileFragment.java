package com.example.safeqr;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONObject;

import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import androidx.appcompat.app.AlertDialog;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;



public class ProfileFragment extends Fragment {

    private static final String API_BASE = ApiEndpoints.BASE_URL;
    private final OkHttpClient httpClient = new OkHttpClient();

    private TextInputLayout layoutName, layoutEmail, layoutPassword, layoutPhone;
    private TextInputEditText etName, etEmail, etPassword, etPhone;
    private MaterialButton btnEditProfile;

    private boolean editMode = false;

    private String originalName = "";
    private String originalEmail = "";
    private String originalPhone = "";
    private TextView tvProfileName, tvProfileEmail;
    private MaterialButton btnLogout;

    private int userId = -1;

    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9]{8,15}$");

    public ProfileFragment() { }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        layoutName = view.findViewById(R.id.layoutName);
        layoutEmail = view.findViewById(R.id.layoutEmail);
        layoutPassword = view.findViewById(R.id.layoutPassword);
        layoutPhone = view.findViewById(R.id.layoutPhone);

        etName = view.findViewById(R.id.etName);
        etEmail = view.findViewById(R.id.etEmail);
        etPassword = view.findViewById(R.id.etPassword);
        etPhone = view.findViewById(R.id.etPhone);

        tvProfileName = view.findViewById(R.id.tvProfileName);
        tvProfileEmail = view.findViewById(R.id.tvProfileEmail);


        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        btnLogout = view.findViewById(R.id.btnLogout);

        btnLogout.setOnClickListener(v -> showLogoutConfirm());

        setEditMode(false);

        btnEditProfile.setOnClickListener(v -> {
            if (!editMode) {
                setEditMode(true);
            } else {
                clearErrors();
                if (!validateInputs()) return;
                showOldPasswordDialog();
            }
        });

        fetchProfile();
        return view;
    }

    private void setEditMode(boolean enabled) {
        editMode = enabled;

        etName.setEnabled(enabled);
        etEmail.setEnabled(enabled);
        etPhone.setEnabled(enabled);
        etPassword.setEnabled(enabled);

        if (enabled) {
            btnEditProfile.setText("Save Edits");
            etPassword.setText("");
            layoutPassword.setHelperText("Leave empty if you don't want to change password");
        } else {
            btnEditProfile.setText("Edit Profile");
            layoutPassword.setHelperText(null);
            etPassword.setText("********");
        }
    }

    private void clearErrors() {
        layoutName.setError(null);
        layoutEmail.setError(null);
        layoutPhone.setError(null);
        layoutPassword.setError(null);
    }

    private boolean validateInputs() {
        String name = safeText(etName);
        String email = safeText(etEmail);
        String phone = safeText(etPhone);
        String newPassword = safeText(etPassword);

        boolean ok = true;

        if (name.length() < 3) {
            layoutName.setError("Name must be at least 3 characters");
            ok = false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            layoutEmail.setError("Enter a valid email");
            ok = false;
        }

        if (!phone.isEmpty() && !PHONE_PATTERN.matcher(phone).matches()) {
            layoutPhone.setError("Phone must be 8-15 digits (optional +)");
            ok = false;
        }

        if (!newPassword.isEmpty()) {
            if (newPassword.length() < 8 ||
                    !newPassword.matches(".*[A-Z].*") ||
                    !newPassword.matches(".*[0-9].*") ||
                    !newPassword.matches(".*[^A-Za-z0-9].*")) {
                layoutPassword.setError("Password must be 8+ with A-Z, number, and symbol");
                ok = false;
            }
        }

        return ok;
    }

    private void showOldPasswordDialog() {
        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_verify_old_password, null);

        TextInputLayout til = view.findViewById(R.id.tilOldPassword);
        TextInputEditText etOld = view.findViewById(R.id.etOldPassword);
        View btnCancel = view.findViewById(R.id.btnCancel);
        View btnContinue = view.findViewById(R.id.btnContinue);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(view)
                .setCancelable(false)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnContinue.setOnClickListener(v -> {
            String oldPass = etOld.getText() == null ? "" : etOld.getText().toString().trim();

            if (oldPass.isEmpty()) {
                til.setError("Old password is required");
                return;
            }

            til.setError(null);
            dialog.dismiss();

            boolean emailChanged = !safeText(etEmail).equals(originalEmail);
            boolean phoneChanged = !safeText(etPhone).equals(originalPhone);

            if (emailChanged || phoneChanged) {
                sendOtp(oldPass, emailChanged, phoneChanged);
            } else {
                updateProfileDirect(oldPass);
            }
        });

        dialog.show();
    }


    private void updateProfileDirect(String oldPass) {
        String name = safeText(etName);
        String email = safeText(etEmail);
        String phone = safeText(etPhone);
        String newPassword = safeText(etPassword);

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("user_id", userId);
                payload.put("old_password", oldPass);

                payload.put("name", name);
                payload.put("email", email);
                payload.put("phone_number", phone);

                if (!newPassword.isEmpty()) payload.put("new_password", newPassword);

                Request req = new Request.Builder()
                        .url(API_BASE + "profile_update.php")
                        .post(RequestBody.create(payload.toString(), MediaType.parse("application/json; charset=utf-8")))
                        .build();

                Response res = httpClient.newCall(req).execute();
                String body = res.body() != null ? res.body().string() : "{}";

                JSONObject obj = new JSONObject(body);
                boolean ok = obj.optBoolean("ok", false);

                requireActivity().runOnUiThread(() -> {
                    if (ok) {
                        Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show();
                        originalName = name;
                        originalEmail = email;
                        originalPhone = phone;
                        tvProfileName.setText(originalName);
                        tvProfileEmail.setText(originalEmail);
                        setEditMode(false);
                    } else {
                        Toast.makeText(requireContext(), obj.optString("error", "Update failed"), Toast.LENGTH_LONG).show();
                    }
                });

            } catch (Exception e) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Update error: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }
        });
    }

    private void sendOtp(String oldPass, boolean emailChanged, boolean phoneChanged) {
        String newEmail = emailChanged ? safeText(etEmail) : "";
        String newPhone = phoneChanged ? safeText(etPhone) : "";

        Toast.makeText(requireContext(), "Sending OTP...", Toast.LENGTH_SHORT).show();

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("user_id", userId);
                payload.put("old_password", oldPass);
                if (!newEmail.isEmpty()) payload.put("new_email", newEmail);
                else payload.put("new_email", etEmail.getText().toString());
                if (!newPhone.isEmpty()) payload.put("new_phone", newPhone);

                Request req = new Request.Builder()
                        .url(API_BASE + "profile_send_otp.php")
                        .post(RequestBody.create(payload.toString(), MediaType.parse("application/json; charset=utf-8")))
                        .build();

                Response res = httpClient.newCall(req).execute();
                String body = res.body() != null ? res.body().string() : "{}";

                JSONObject obj = new JSONObject(body);
                boolean ok = obj.optBoolean("ok", false);

                requireActivity().runOnUiThread(() -> {
                    if (!ok) {
                        Toast.makeText(requireContext(), obj.optString("error", "Failed to send OTP"), Toast.LENGTH_LONG).show();
                        return;
                    }
                    showOtpDialog(oldPass);
                });

            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Send OTP error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e("ProfileFragment", "Send OTP error", e);
                });
            }
        });
    }

    private void showOtpDialog(String oldPass) {
        TextInputLayout til = new TextInputLayout(requireContext());
        til.setHint("6-digit OTP");

        TextInputEditText etOtp = new TextInputEditText(requireContext());
        etOtp.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        til.addView(etOtp);

        final long totalMs = 5 * 60 * 1000L;

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Verify new email/phone")
                .setMessage("Enter the 6-digit OTP. Expires in 5 minutes.")
                .setView(til)
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .setPositiveButton("Verify & Save", null)
                .show();

        CountDownTimer timer = new CountDownTimer(totalMs, 1000) {
            @Override public void onTick(long ms) {
                long sec = ms / 1000;
                long m = sec / 60;
                long s = sec % 60;
                dialog.setMessage("Enter the 6-digit OTP. Expires in " + String.format("%02d:%02d", m, s));
            }
            @Override public void onFinish() {
                dialog.setMessage("OTP expired. Press Cancel and resend.");
            }
        };
        timer.start();

        dialog.setOnDismissListener(d -> timer.cancel());

        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String otp = etOtp.getText() == null ? "" : etOtp.getText().toString().trim();

            if (!otp.matches("^\\d{6}$")) {
                til.setError("Enter exactly 6 digits");
                return;
            }
            til.setError(null);
            dialog.dismiss();
            confirmUpdate(oldPass, otp);
        });
    }

    private void confirmUpdate(String oldPass, String otp) {
        String name = safeText(etName);
        String email = safeText(etEmail);
        String phone = safeText(etPhone);
        String newPassword = safeText(etPassword);

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("user_id", userId);
                payload.put("old_password", oldPass);
                payload.put("otp", otp);

                payload.put("name", name);
                payload.put("email", email);
                payload.put("phone_number", phone);

                if (!newPassword.isEmpty()) payload.put("new_password", newPassword);

                Request req = new Request.Builder()
                        .url(API_BASE + "profile_confirm_update.php")
                        .post(RequestBody.create(payload.toString(), MediaType.parse("application/json; charset=utf-8")))
                        .build();

                Response res = httpClient.newCall(req).execute();
                String body = res.body() != null ? res.body().string() : "{}";

                JSONObject obj = new JSONObject(body);
                boolean ok = obj.optBoolean("ok", false);

                requireActivity().runOnUiThread(() -> {
                    if (ok) {
                        Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show();
                        originalName = name;
                        originalEmail = email;
                        originalPhone = phone;
                        tvProfileName.setText(originalName);
                        tvProfileEmail.setText(originalEmail);
                        setEditMode(false);
                    } else {
                        Toast.makeText(requireContext(), obj.optString("error", "OTP verification failed"), Toast.LENGTH_LONG).show();
                    }
                });

            } catch (Exception e) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Confirm error: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }
        });
    }

    private void fetchProfile() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                SharedPreferences sp = requireContext().getSharedPreferences("safeqr_prefs", 0);
                userId = sp.getInt("user_id", -1);

                Request req = new Request.Builder()
                        .url(API_BASE + "profile_get.php?user_id=" + userId)
                        .get()
                        .build();

                Response res = httpClient.newCall(req).execute();
                String body = res.body() != null ? res.body().string() : "{}";

                JSONObject obj = new JSONObject(body);
                if (!obj.optBoolean("ok", false)) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), obj.optString("error", "Failed to load profile"), Toast.LENGTH_LONG).show()
                    );
                    return;
                }

                JSONObject data = obj.getJSONObject("data");
                originalName = data.optString("name", "");
                originalEmail = data.optString("email", "");
                originalPhone = data.optString("phone_number", "");

                requireActivity().runOnUiThread(() -> {
                    etName.setText(originalName);
                    etEmail.setText(originalEmail);
                    etPhone.setText(originalPhone);
                    tvProfileName.setText(originalName);
                    tvProfileEmail.setText(originalEmail);
                    setEditMode(false);
                });

            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Profile load error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e("ProfileFragment", "Profile load error", e);
                });
            }
        });
    }

    private String safeText(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }


    private void showLogoutConfirm() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Log out")
                .setMessage("Are you sure you want to log out?")
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .setPositiveButton("Log out", (d, w) -> doLogout())
                .show();
    }

    private void doLogout() {
        SessionManager.logout(requireContext());

        SharedPreferences sp = requireContext().getSharedPreferences("safeqr_prefs", 0);
        sp.edit().clear().apply();

        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

}
