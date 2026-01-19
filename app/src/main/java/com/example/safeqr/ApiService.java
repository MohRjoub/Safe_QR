package com.example.safeqr;


import android.os.Handler;
import android.os.Looper;

import com.google.gson.Gson;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ApiService {
    private static final OkHttpClient client = new OkHttpClient();
    private static final Gson gson = new Gson();
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static <T> void post(String url, Object request, Class<T> responseType, ApiCallback<T> callback) {
        String json = gson.toJson(request);
        RequestBody body = RequestBody.create(json, JSON);
        Request httpRequest = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        client.newCall(httpRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError("Network error: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();
                mainHandler.post(() -> {
                    try {
                        if (response.isSuccessful()) {
                            ApiResponse<T> apiResponse = gson.fromJson(responseBody, getResponseType(responseType));
                            if (apiResponse.isSuccess()) {
                                callback.onSuccess(apiResponse.getData());
                            } else {
                                callback.onError(apiResponse.getMessage());
                            }
                        } else {
                            callback.onError("Error: " + response.code());
                        }
                    } catch (Exception e) {
                        callback.onError("Parse error: " + e.getMessage());
                    }
                });
            }
        });
    }

    public static <T> void postFull(String url, Object request, Class<T> dataType, ApiCallback<ApiResponse<T>> callback) {
        String json = gson.toJson(request);
        RequestBody body = RequestBody.create(json, JSON);
        Request httpRequest = new Request.Builder().url(url).post(body).build();

        client.newCall(httpRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError("Network error: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();
                mainHandler.post(() -> {
                    try {
                        if (response.isSuccessful()) {
                            java.lang.reflect.Type type =
                                    com.google.gson.reflect.TypeToken.getParameterized(ApiResponse.class, dataType).getType();

                            ApiResponse<T> apiResponse = gson.fromJson(responseBody, type);
                            callback.onSuccess(apiResponse);
                        } else {
                            callback.onError("Error: " + response.code());
                        }
                    } catch (Exception e) {
                        callback.onError("Parse error: " + e.getMessage());
                    }
                });
            }
        });
    }


    private static <T> java.lang.reflect.Type getResponseType(Class<T> clazz) {
        return com.google.gson.reflect.TypeToken.getParameterized(ApiResponse.class, clazz).getType();
    }

    public interface ApiCallback<T> {
        void onSuccess(T response);
        void onError(String error);
    }
}