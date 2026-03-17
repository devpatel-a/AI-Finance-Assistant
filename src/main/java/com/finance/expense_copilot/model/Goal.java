package com.finance.expense_copilot.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "goals")
public class Goal {
    @Id
    private String id;
    private String goalName;
    private Double targetAmount;
    private Double currentSaved;
    private String deadline;
    
    @org.springframework.data.mongodb.core.index.Indexed
    private String userEmail;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getGoalName() { return goalName; }
    public void setGoalName(String goalName) { this.goalName = goalName; }
    public Double getTargetAmount() { return targetAmount; }
    public void setTargetAmount(Double targetAmount) { this.targetAmount = targetAmount; }
    public Double getCurrentSaved() { return currentSaved; }
    public void setCurrentSaved(Double currentSaved) { this.currentSaved = currentSaved; }
    public String getDeadline() { return deadline; }
    public void setDeadline(String deadline) { this.deadline = deadline; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
}
