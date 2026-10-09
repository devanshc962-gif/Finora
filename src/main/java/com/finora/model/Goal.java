package com.finora.model;

public class Goal {
    private final long id;
    private final String title;
    private final long targetCents;
    private final long savedCents;
    private final String deadline;
    public Goal(long id, String title, long targetCents, long savedCents, String deadline) {
        this.id = id; this.title = title; this.targetCents = targetCents; this.savedCents = savedCents; this.deadline = deadline;
    }
    public long getId() { return id; }
    public String getTitle() { return title; }
    public long getTargetCents() { return targetCents; }
    public long getSavedCents() { return savedCents; }
    public String getTarget() { return String.format(java.util.Locale.US, "₹%,.2f", targetCents / 100.0); }
    public String getSaved() { return String.format(java.util.Locale.US, "₹%,.2f", savedCents / 100.0); }
    public String getDeadline() { return deadline; }
    public int getProgressPercent() { return targetCents == 0 ? 0 : (int) Math.min(100, Math.round(savedCents * 100.0 / targetCents)); }
    public boolean isCompleted() { return savedCents >= targetCents; }
}
