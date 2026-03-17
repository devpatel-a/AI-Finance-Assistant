package com.finance.expense_copilot.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "expenses")
public class Expense {
    @Id
    private String id;
    private String description;
    private Double amount;
    private String category; // <--- Ensure this is here
    private String type;     // INCOME or EXPENSE
    private String date;     // yyyy-MM-dd
    private String accountType; // "Cash" or "Bank"

    @org.springframework.data.mongodb.core.index.Indexed
    private String userEmail;
    
    // GETTERS AND SETTERS
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public String getCategory() { return category; }

    // This is the specific method the error is looking for:
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
}