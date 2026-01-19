package com.example.safeqr;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import okhttp3.*;

public class HistoryFragment extends Fragment {

    private static final String API_BASE = ApiEndpoints.BASE_URL;

    private RecyclerView rvHistory;
    private ScanHistoryAdapter adapter;

    private final List<ScanHistoryItem> allList = new ArrayList<>();

    private Chip chipAll, chipSafe, chipSuspicious, chipUnknown;
    private String selectedStatus = "all";

    private TextInputEditText etSearch;
    private String currentQuery = "";

    public HistoryFragment() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_history, container, false);

        rvHistory = view.findViewById(R.id.rvHistory);
        chipAll = view.findViewById(R.id.chipAll);
        chipSafe = view.findViewById(R.id.chipSafe);
        chipUnknown = view.findViewById(R.id.chipUnknown);
        chipSuspicious = view.findViewById(R.id.chipSuspicious);
        etSearch = view.findViewById(R.id.etSearch);

        rvHistory.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new ScanHistoryAdapter(
                new ArrayList<>(),

                // item click -> open ScanResultActivity
                this::openHistoryItemInResultScreen,

                new ScanHistoryAdapter.OnMenuActionListener() {
                    @Override
                    public void onDelete(ScanHistoryItem item, int position) {
                        deleteHistory(item, position);
                    }

                    @Override
                    public void onRecheck(ScanHistoryItem item, int position) {
                        recheckHistoryItem(item);
                    }
                }
        );

        rvHistory.setAdapter(adapter);

        setupFilters();
        setupSearch();
        fetchHistoryWithVolley();

        return view;
    }

    private void openHistoryItemInResultScreen(ScanHistoryItem item) {
        Intent intent = new Intent(requireContext(), ScanResultActivity.class);
        intent.putExtra(ScanResultActivity.EXTRA_URL, item.url);
        intent.putExtra(ScanResultActivity.EXTRA_STATUS, item.status);
        intent.putExtra(ScanResultActivity.EXTRA_RISK, item.riskScore);
        intent.putExtra(ScanResultActivity.EXTRA_REASONS_JSON, item.reasons == null ? "" : item.reasons);
        startActivity(intent);
    }

    // ================== FETCH HISTORY ==================
    private void fetchHistoryWithVolley() {
        SharedPreferences sp = requireContext().getSharedPreferences("safeqr_prefs", 0);
        int userId = sp.getInt("user_id", 4); // fallback for testing

        String url = API_BASE + "history.php?user_id=" + userId;

        RequestQueue queue = Volley.newRequestQueue(requireContext());

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        boolean ok = response.optBoolean("ok", false);
                        if (!ok) {
                            Toast.makeText(getContext(),
                                    response.optString("error", "Unknown error"),
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        JSONArray data = response.getJSONArray("data");
                        List<ScanHistoryItem> list = new ArrayList<>();

                        for (int i = 0; i < data.length(); i++) {
                            JSONObject row = data.getJSONObject(i);

                            int historyId = row.getInt("history_id");
                            int linkId = row.getInt("link_id");
                            String date = row.getString("date");
                            String status = row.optString("status", "unknown");
                            String linkUrl = row.optString("url", "");
                            String reasons = row.optString("reasons", "");

                            double riskScore = row.isNull("risk_score") ? -1 : row.optDouble("risk_score", -1);
                            String provider = row.optString("provider", "");

                            list.add(new ScanHistoryItem(
                                    historyId, linkId, linkUrl, date, status, reasons, riskScore, provider
                            ));
                        }

                        allList.clear();
                        allList.addAll(list);

                        applyFilter(selectedStatus);

                    } catch (Exception e) {
                        Toast.makeText(getContext(), "Parse error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    String msg = (error.getMessage() != null) ? error.getMessage() : error.toString();
                    Toast.makeText(getContext(), "Volley error: " + msg, Toast.LENGTH_SHORT).show();
                }
        );

        queue.add(request);
    }

    // ================== FILTERS ==================
    private void setupFilters() {
        chipAll.setOnClickListener(v -> { selectedStatus = "all"; applyFilter(selectedStatus); });
        chipSafe.setOnClickListener(v -> { selectedStatus = "safe"; applyFilter(selectedStatus); });
        chipSuspicious.setOnClickListener(v -> { selectedStatus = "suspicious"; applyFilter(selectedStatus); });
        chipUnknown.setOnClickListener(v -> { selectedStatus = "unknown"; applyFilter(selectedStatus); });
    }

    // ================== SEARCH ==================
    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentQuery = (s == null) ? "" : s.toString().trim().toLowerCase();
                applyFilter(selectedStatus);
            }

            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void applyFilter(String statusFilter) {
        String query = currentQuery == null ? "" : currentQuery.trim().toLowerCase();

        List<ScanHistoryItem> filtered = new ArrayList<>();

        for (ScanHistoryItem item : allList) {
            String st = item.status == null ? "" : item.status.trim().toLowerCase();
            String u = item.url == null ? "" : item.url.trim().toLowerCase();

            boolean statusOk = statusFilter.equals("all") || st.equals(statusFilter);
            boolean searchOk = query.isEmpty() || u.contains(query);

            if (statusOk && searchOk) filtered.add(item);
        }

        adapter.setData(filtered);
    }

    // ================== RECHECK ==================
    private void recheckHistoryItem(ScanHistoryItem item) {
        OkHttpClient client = new OkHttpClient();

        SharedPreferences sp = requireContext().getSharedPreferences("safeqr_prefs", 0);
        int userId = sp.getInt("user_id", 4);

        Toast.makeText(requireContext(), "Rechecking...", Toast.LENGTH_SHORT).show();

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("user_id", userId);
                payload.put("history_id", item.historyId);
                payload.put("url", item.url);

                okhttp3.Request okReq = new okhttp3.Request.Builder()
                        .url(API_BASE + "recheck_history.php")
                        .post(RequestBody.create(
                                payload.toString(),
                                MediaType.parse("application/json; charset=utf-8")
                        ))
                        .build();

                Response res = client.newCall(okReq).execute();
                String body = (res.body() != null) ? res.body().string() : "{}";

                JSONObject obj = new JSONObject(body);
                if (!obj.optBoolean("ok", false)) {
                    String err = obj.optString("error", "Recheck failed");
                    if (isAdded()) requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), err, Toast.LENGTH_LONG).show()
                    );
                    return;
                }

                String newStatus = obj.optString("status", item.status);
                double newRisk = obj.isNull("risk_score") ? -1 : obj.optDouble("risk_score", -1);
                String newProvider = obj.optString("provider", item.provider);

                JSONObject reasonsObj = obj.optJSONObject("reasons");
                String reasonsJsonStr = (reasonsObj != null) ? reasonsObj.toString() : item.reasons;

                item.status = newStatus;
                item.riskScore = newRisk;
                item.provider = newProvider;
                item.reasons = reasonsJsonStr;

                if (isAdded()) requireActivity().runOnUiThread(() -> {
                    applyFilter(selectedStatus);
                    Toast.makeText(requireContext(), "Updated: " + newStatus, Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                if (isAdded()) requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Recheck error: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }
        });
    }

    // ================== DELETE ==================
    private void deleteHistory(ScanHistoryItem item, int position) {
        // keep your delete endpoint if correct
        String url = API_BASE + "deleteHistory.php";

        RequestQueue queue = Volley.newRequestQueue(requireContext());

        StringRequest request = new StringRequest(
                Request.Method.POST,
                url,
                response -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        boolean ok = json.getBoolean("ok");

                        if (ok) {
                            allList.remove(item);
                            applyFilter(selectedStatus);
                            Toast.makeText(getContext(), "Deleted successfully", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(),
                                    json.optString("error", "Delete failed"),
                                    Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(getContext(),
                                "Parse error: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(getContext(),
                        "Volley error: " + error.toString(),
                        Toast.LENGTH_SHORT).show()
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("history_id", String.valueOf(item.historyId));
                params.put("user_id", String.valueOf(1));
                return params;
            }
        };

        queue.add(request);
    }
}
