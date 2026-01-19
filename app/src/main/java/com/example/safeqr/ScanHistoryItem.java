package com.example.safeqr;

public class ScanHistoryItem {
    public int historyId;
    public int linkId;
    public String url;
    public String time;
    public String status;
    public String reasons;
    public double riskScore;
    public String provider;

    public ScanHistoryItem(int historyId, int linkId, String url, String time,
                           String status, String reasons, double riskScore, String provider) {
        this.historyId = historyId;
        this.linkId = linkId;
        this.url = url;
        this.time = time;
        this.status = status;
        this.reasons = reasons;
        this.riskScore = riskScore;
        this.provider = provider;
    }
}
