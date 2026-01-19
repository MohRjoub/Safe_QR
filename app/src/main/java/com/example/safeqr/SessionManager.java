package com.example.safeqr;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class SessionManager {

    private static final String PREF_NAME = "SafeQRSession";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_LOGGED_IN = "is_logged_in";

    private static SharedPreferences getEncryptedPrefs(Context context) {
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            return EncryptedSharedPreferences.create(
                    PREF_NAME,
                    masterKeyAlias,
                    context,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException("Failed to create encrypted prefs", e);
        }
    }

    /* ================= LOGIN ================= */

    public static void saveToken(Context context, String token) {
        getEncryptedPrefs(context)
                .edit()
                .putString(KEY_TOKEN, token)
                .putBoolean(KEY_LOGGED_IN, true)
                .apply();
    }

    public static String getToken(Context context) {
        return getEncryptedPrefs(context).getString(KEY_TOKEN, null);
    }

    public static boolean isLoggedIn(Context context) {
        return getEncryptedPrefs(context).getBoolean(KEY_LOGGED_IN, false);
    }

    /* ================= LOGOUT ================= */

    public static void logout(Context context) {
        getEncryptedPrefs(context).edit().clear().apply();
    }


}
