package com.project.smartparking.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;

public class AiInsightsDtos {

 
    public static class AiInsightsRequest {
        private String dateRange;
        private Map<String, Object> currentPeriod;
        private Map<String, Object> previousPeriod;
        private Map<String, Object> growth;

        public String getDateRange() { return dateRange; }
        public void setDateRange(String dateRange) { this.dateRange = dateRange; }

        public Map<String, Object> getCurrentPeriod() { return currentPeriod; }
        public void setCurrentPeriod(Map<String, Object> currentPeriod) { this.currentPeriod = currentPeriod; }

        public Map<String, Object> getPreviousPeriod() { return previousPeriod; }
        public void setPreviousPeriod(Map<String, Object> previousPeriod) { this.previousPeriod = previousPeriod; }

        public Map<String, Object> getGrowth() { return growth; }
        public void setGrowth(Map<String, Object> growth) { this.growth = growth; }
    }

    // ── Outgoing response — must match the shape the frontend expects ──
    public static class AiInsightsResponse {
        private String summary;
        private List<String> insights;

        @JsonInclude(JsonInclude.Include.NON_NULL)
        private List<Anomaly> anomalies;

        @JsonInclude(JsonInclude.Include.NON_NULL)
        private Forecast forecast;

        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }

        public List<String> getInsights() { return insights; }
        public void setInsights(List<String> insights) { this.insights = insights; }

        public List<Anomaly> getAnomalies() { return anomalies; }
        public void setAnomalies(List<Anomaly> anomalies) { this.anomalies = anomalies; }

        public Forecast getForecast() { return forecast; }
        public void setForecast(Forecast forecast) { this.forecast = forecast; }
    }

    public static class Anomaly {
        private String severity; // "low" | "medium" | "high"
        private String message;

        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    public static class Forecast {
        private long predictedBookings;
        private double predictedRevenue;
        private String trend; // "up" | "down" | "flat"
        private String confidence; // "low" | "medium" | "high"
        private String note;

        public long getPredictedBookings() { return predictedBookings; }
        public void setPredictedBookings(long predictedBookings) { this.predictedBookings = predictedBookings; }

        public double getPredictedRevenue() { return predictedRevenue; }
        public void setPredictedRevenue(double predictedRevenue) { this.predictedRevenue = predictedRevenue; }

        public String getTrend() { return trend; }
        public void setTrend(String trend) { this.trend = trend; }

        public String getConfidence() { return confidence; }
        public void setConfidence(String confidence) { this.confidence = confidence; }

        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }
    }
}