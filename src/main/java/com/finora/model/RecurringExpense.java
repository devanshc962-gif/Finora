package com.finora.model;

import java.time.LocalDate;

public class RecurringExpense {
    private final long id;
    private final String description;
    private final String category;
    private final long amountCents;
    private final String frequency;
    private final LocalDate nextDueOn;
    private final LocalDate anchorDate;
    private final boolean active;

    public RecurringExpense(long id, String description, String category, long amountCents, String frequency,
                            LocalDate nextDueOn, LocalDate anchorDate, boolean active) {
        this.id = id;
        this.description = description;
        this.category = category;
        this.amountCents = amountCents;
        this.frequency = frequency;
        this.nextDueOn = nextDueOn;
        this.anchorDate = anchorDate;
        this.active = active;
    }

    public long getId() { return id; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public long getAmountCents() { return amountCents; }
    public String getAmount() { return String.format(java.util.Locale.US, "₹%,.2f", amountCents / 100.0); }
    public String getFrequency() { return frequency; }
    public String getFrequencyLabel() {
        return switch (frequency) {
            case "WEEKLY" -> "Weekly";
            case "MONTHLY" -> "Monthly";
            case "YEARLY" -> "Yearly";
            default -> "Recurring";
        };
    }
    public LocalDate getNextDueOn() { return nextDueOn; }
    public LocalDate getAnchorDate() { return anchorDate; }
    public boolean isActive() { return active; }
    public boolean isDue() { return !nextDueOn.isAfter(LocalDate.now()); }
}
