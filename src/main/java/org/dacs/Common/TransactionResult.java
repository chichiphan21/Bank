package org.dacs.Common;

import java.io.Serializable;

public class TransactionResult implements Serializable {
    private boolean success;
    private double newBalance;
    private double amountChanged;
    private String description;
    private String timestamp;

    public TransactionResult() {
    }

    public TransactionResult(boolean success, double newBalance, double amountChanged, String description, String timestamp) {
        this.success = success;
        this.newBalance = newBalance;
        this.amountChanged = amountChanged;
        this.description = description;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public double getNewBalance() {
        return newBalance;
    }

    public void setNewBalance(double newBalance) {
        this.newBalance = newBalance;
    }

    public double getAmountChanged() {
        return amountChanged;
    }

    public void setAmountChanged(double amountChanged) {
        this.amountChanged = amountChanged;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
