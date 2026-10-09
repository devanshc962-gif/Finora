package com.finora.model;

public class Budget {
    private final long id;
    private final String category;
    private final long limitCents;
    private final String startsOn;
    private final String endsOn;
    private final long spentCents;
    public Budget(long id, String category, long limitCents, String startsOn, String endsOn, long spentCents) {
        this.id = id; this.category = category; this.limitCents = limitCents; this.startsOn = startsOn; this.endsOn = endsOn; this.spentCents = spentCents;
    }
    public long getId() { return id; }
    public String getCategory() { return category; }
    public long getLimitCents() { return limitCents; }
    public String getLimit() { return String.format(java.util.Locale.US, "₹%,.2f", limitCents / 100.0); }
    public String getStartsOn() { return startsOn; }
    public String getEndsOn() { return endsOn; }
    public long getSpentCents() { return spentCents; }
    public String getSpent() { return String.format(java.util.Locale.US, "₹%,.2f", spentCents / 100.0); }
    public long getRemainingCents() { return Math.max(0, limitCents - spentCents); }
    public String getRemaining() { return String.format(java.util.Locale.US, "₹%,.2f", getRemainingCents() / 100.0); }
    public int getProgressPercent() { return limitCents == 0 ? 0 : (int) Math.min(100, Math.round(spentCents * 100.0 / limitCents)); }
}
