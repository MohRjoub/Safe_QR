package com.example.safeqr;


import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class SessionManager {
    private static final String PREF_NAME = "SafeQRSession";
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

    public static void saveToken(Context context, String token) {
        getEncryptedPrefs(context).edit().putString("token", token).apply();
    }

    public static String getToken(Context context) {
        return getEncryptedPrefs(context).getString("token", null);
    }

    public static void clearToken(Context context) {
        getEncryptedPrefs(context).edit().remove("token").apply();
    }
}
