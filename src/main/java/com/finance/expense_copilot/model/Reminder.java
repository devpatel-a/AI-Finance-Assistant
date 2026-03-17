package com.finance.expense_copilot.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "reminders")
public class Reminder {
    @Id
    private String id;
    private String title;
    private Double amount;
    private String dueDate;
    private String type; // Subscription, EMI, Bills
    
    @org.springframework.data.mongodb.core.index.Indexed
    private String userEmail;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
}
