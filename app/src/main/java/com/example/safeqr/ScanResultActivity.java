package com.example.safeqr;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ScanResultActivity extends AppCompatActivity {

    public static final String EXTRA_URL = "extra_url";
    public static final String EXTRA_STATUS = "extra_status";
    public static final String EXTRA_RISK = "extra_risk";
    public static final String EXTRA_REASONS_JSON = "extra_reasons_json";

    private ImageView btnBack;
    private Chip chipStatus;
    private TextView tvUrl;
    private TextView tvWhyTitle;
    private LinearLayout reasonsBox;
    private TextView tvReasons;
    private TextView tvNote;
    private MaterialButton btnOpen;
    private TextView btnCancel;

    private String url;
    private String status;
    private double risk;
    private String reasonsJson;

    private CountDownTimer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_result);

        btnBack = findViewById(R.id.btnBack);
        chipStatus = findViewById(R.id.chipStatus);
        tvUrl = findViewById(R.id.tvUrl);
        tvWhyTitle = findViewById(R.id.tvWhyTitle);
        reasonsBox = findViewById(R.id.reasonsBox);
        tvReasons = findViewById(R.id.tvReasons);
        tvNote = findViewById(R.id.tvNote);
        btnOpen = findViewById(R.id.btnOpen);
        btnCancel = findViewById(R.id.btnCancel);

        Intent intent = getIntent();
        url = intent.getStringExtra(EXTRA_URL);
        status = intent.getStringExtra(EXTRA_STATUS);
        risk = intent.getDoubleExtra(EXTRA_RISK, -1);
        reasonsJson = intent.getStringExtra(EXTRA_REASONS_JSON);

        if (url == null) url = "";
        if (status == null) status = "unknown";
        if (reasonsJson == null) reasonsJson = "";

        tvUrl.setText(url);

        applyThemeAndTexts(status, risk, reasonsJson);

        btnBack.setOnClickListener(v -> finish());
        btnCancel.setOnClickListener(v -> finish());

        startOpenCountdown(10);

        btnOpen.setOnClickListener(v -> {
            if (!btnOpen.isEnabled()) return;
            if (url.trim().isEmpty()) return;

            try {
                Intent open = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(open);
            } catch (Exception e) {
                Toast.makeText(this, "Cannot open link", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void applyThemeAndTexts(String st, double riskScore, String reasonsJson) {
        st = (st == null) ? "unknown" : st.trim().toLowerCase();

        if ("safe".equals(st)) {
            chipStatus.setText("Safe link");
            chipStatus.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#E8F5E9")));
            chipStatus.setTextColor(Color.parseColor("#2E7D32"));
            btnOpen.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#7CB342"))); // green
            reasonsBox.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFFDE7"))); // light yellow
        } else if ("suspicious".equals(st)) {
            chipStatus.setText("Suspicious link");
            chipStatus.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#FDECEA")));
            chipStatus.setTextColor(Color.parseColor("#C62828"));
            btnOpen.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E57373"))); // red
            reasonsBox.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFEBEE"))); // light red
        } else {
            chipStatus.setText("Unknown link");
            chipStatus.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#FFF8E1")));
            chipStatus.setTextColor(Color.parseColor("#B26A00"));
            btnOpen.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFB74D"))); // orange
            reasonsBox.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFF3E0"))); // light orange
        }

        String title;
        String note;

        if ("safe".equals(st)) {
            title = "What we checked";
            note = "This link looks safe based on our checks. Still, be careful if it asks for passwords or sensitive information.";
        } else if ("suspicious".equals(st)) {
            title = "Why this looks risky";
            note = "This link may be unsafe. Attackers often use such links for fake login pages or scams. Avoid opening unless you fully trust it.";
        } else {
            title = "We couldn't confirm safety";
            note = "We couldn't verify this link (network error or limited data). Proceed carefully and avoid sharing sensitive information.";
        }

        tvWhyTitle.setText(title);

        tvReasons.setText(buildDetailsText(st, riskScore, reasonsJson));

        tvNote.setText(note);
    }

    private String buildDetailsText(String st, double riskScore, String reasonsJson) {
        st = (st == null) ? "unknown" : st.trim().toLowerCase();

        List<String> high = new ArrayList<>();
        List<String> medium = new ArrayList<>();
        List<String> info = new ArrayList<>();

        String riskLine = formatRiskLine(st, riskScore);

        try {
            if (reasonsJson == null) reasonsJson = "";
            reasonsJson = reasonsJson.trim();

            if (!reasonsJson.isEmpty()) {
                JSONObject reasons = new JSONObject(reasonsJson);

                String summary = reasons.optString("summary", "").trim();
                if (!summary.isEmpty()) info.add(summary);

                JSONArray signals = reasons.optJSONArray("signals");
                if (signals != null) {
                    for (int i = 0; i < signals.length(); i++) {
                        JSONObject s = signals.optJSONObject(i);
                        if (s == null) continue;

                        String type = s.optString("type", "");
                        String severity = s.optString("severity", "low");
                        String msg = s.optString("message", "").trim();

                        if (msg.isEmpty()) continue;
                        if ("normalization".equals(type)) continue; // hide technical noise

                        if ("high".equals(severity)) high.add(msg);
                        else if ("medium".equals(severity)) medium.add(msg);
                    }
                }
            }
        } catch (Exception ignored) {}

        StringBuilder out = new StringBuilder();

        out.append("• ").append(riskLine).append("\n");

        if ("safe".equals(st)) {
            if (!info.isEmpty()) {
                for (String s : info) out.append("• ").append(s).append("\n");
            } else {
                out.append("• No Safe Browsing threat matches found.\n");
            }

            if (!medium.isEmpty()) {
                out.append("\n• Minor cautions:\n");
                for (String s : medium) out.append("• ").append(s).append("\n");
            } else {
                out.append("\n• Minor cautions: none.\n");
            }

            return out.toString().trim();
        }

        if ("unknown".equals(st)) {
            out.append("• Safety could not be confirmed.\n");

            if (!high.isEmpty() || !medium.isEmpty()) {
                out.append("\n• Signals found:\n");
                for (String s : high) out.append("• ").append(s).append("\n");
                for (String s : medium) out.append("• ").append(s).append("\n");
            } else {
                out.append("\n• No strong signals returned, but verification failed.\n");
            }

            return out.toString().trim();
        }

        if (!high.isEmpty()) {
            out.append("• High risk signals:\n");
            for (String s : high) out.append("• ").append(s).append("\n");
        } else {
            out.append("• High risk signals detected (flagged as suspicious).\n");
        }

        if (!medium.isEmpty()) {
            out.append("\n• Other warnings:\n");
            for (String s : medium) out.append("• ").append(s).append("\n");
        }

        return out.toString().trim();
    }

    private String formatRiskLine(String st, double risk) {
        if (risk < 0) return "Risk score: N/A";

        int pct = (int) Math.round(risk * 100);

        String label;
        if (pct <= 20) label = "Low";
        else if (pct <= 60) label = "Medium";
        else label = "High";

        if ("suspicious".equals(st)) label = "High";

        return "Risk score: " + pct + "% (" + label + ")";
    }

    private void startOpenCountdown(int seconds) {
        btnOpen.setEnabled(false);
        btnOpen.setAlpha(0.6f);

        if (timer != null) timer.cancel();

        timer = new CountDownTimer(seconds * 1000L, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                long s = millisUntilFinished / 1000L;
                btnOpen.setText("Open Link (" + s + ")");
            }

            @Override
            public void onFinish() {
                btnOpen.setText("Open Link (0)");
                btnOpen.setEnabled(true);
                btnOpen.setAlpha(1f);
            }
        }.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timer != null) timer.cancel();
    }
}
