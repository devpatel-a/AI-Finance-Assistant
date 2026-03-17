package com.finance.expense_copilot.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

@Document(collection = "split_expenses")
public class SplitExpense {
    @Id
    private String id;
    private String description;
    private Double totalAmount;
    private String paidBy; // e.g. "Me" or friend's name
    private List<SplitDetail> splitDetails;

    @org.springframework.data.mongodb.core.index.Indexed
    private String userEmail;

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public static class SplitDetail {
        private String person;
        private Double amountOwed;
        private Boolean settled;

        public String getPerson() { return person; }
        public void setPerson(String person) { this.person = person; }
        public Double getAmountOwed() { return amountOwed; }
        public void setAmountOwed(Double amountOwed) { this.amountOwed = amountOwed; }
        public Boolean getSettled() { return settled; }
        public void setSettled(Boolean settled) { this.settled = settled; }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }
    public String getPaidBy() { return paidBy; }
    public void setPaidBy(String paidBy) { this.paidBy = paidBy; }
    public List<SplitDetail> getSplitDetails() { return splitDetails; }
    public void setSplitDetails(List<SplitDetail> splitDetails) { this.splitDetails = splitDetails; }
}
