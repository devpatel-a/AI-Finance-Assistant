package com.finance.expense_copilot.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "vouchers")
public class Voucher {
    @Id
    private String id;
    private String platform; // Amazon, Flipkart, etc.
    private String code;
    private Double value;
    private String expiryDate; // yyyy-MM-dd
    
    @org.springframework.data.mongodb.core.index.Indexed
    private String userEmail;

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public Double getValue() { return value; }
    public void setValue(Double value) { this.value = value; }
    public String getExpiryDate() { return expiryDate; }
    public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }
}
