package com.project.smartparking.dto;

public class DashboardDTO {
    private long totalBookings;
    private double totalRevenue;
    private long activeParkings;

    // Default constructor
    public DashboardDTO() {}

    public DashboardDTO(long totalBookings, double totalRevenue, long activeParkings) {
        this.totalBookings = totalBookings;
        this.totalRevenue = totalRevenue;
        this.activeParkings = activeParkings;
    }

    // Getters and Setters
    public long getTotalBookings() { return totalBookings; }
    public void setTotalBookings(long totalBookings) { this.totalBookings = totalBookings; }

    public double getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(double totalRevenue) { this.totalRevenue = totalRevenue; }

    public long getActiveParkings() { return activeParkings; }
    public void setActiveParkings(long activeParkings) { this.activeParkings = activeParkings; }
}