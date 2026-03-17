package com.finance.expense_copilot.model;
import java.util.HashMap;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "users")
public class User {
    @Id
    private String id;
    private String email;
    private String name;

    private boolean notificationsEnabled = true;
    private double dailyLimit = 500.0;
    private boolean appLockEnabled = false;
    private boolean smsTrackingEnabled = false;
    private double cashBalance = 0.0;
    private double bankBalance = 0.0;
    private Map<String, Double> categoryLimits = new HashMap<>();

    public User() {}

    public User(String email) {
        this.email = email;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public boolean isNotificationsEnabled() { return notificationsEnabled; }
    public void setNotificationsEnabled(boolean notificationsEnabled) { this.notificationsEnabled = notificationsEnabled; }
    public double getDailyLimit() { return dailyLimit; }
    public void setDailyLimit(double dailyLimit) { this.dailyLimit = dailyLimit; }
    
    public boolean isAppLockEnabled() { return appLockEnabled; }
    public void setAppLockEnabled(boolean appLockEnabled) { this.appLockEnabled = appLockEnabled; }
    public boolean isSmsTrackingEnabled() { return smsTrackingEnabled; }
    public void setSmsTrackingEnabled(boolean smsTrackingEnabled) { this.smsTrackingEnabled = smsTrackingEnabled; }
    
    public double getCashBalance() { return cashBalance; }
    public void setCashBalance(double cashBalance) { this.cashBalance = cashBalance; }
    public double getBankBalance() { return bankBalance; }
    public void setBankBalance(double bankBalance) { this.bankBalance = bankBalance; }
    
    public Map<String, Double> getCategoryLimits() { return categoryLimits; }
    public void setCategoryLimits(Map<String, Double> categoryLimits) { this.categoryLimits = categoryLimits; }
}
