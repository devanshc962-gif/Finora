package com.finora.model;

public class Expense {
    private final long id;
    private final String description;
    private final String category;
    private final long amountCents;
    private final String spentOn;
    public Expense(long id, String description, String category, long amountCents, String spentOn) {
        this.id = id; this.description = description; this.category = category; this.amountCents = amountCents; this.spentOn = spentOn;
    }
    public long getId() { return id; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public long getAmountCents() { return amountCents; }
    public String getAmount() { return String.format(java.util.Locale.US, "₹%,.2f", amountCents / 100.0); }
    public String getSpentOn() { return spentOn; }
}
