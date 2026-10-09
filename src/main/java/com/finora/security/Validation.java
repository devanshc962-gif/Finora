package com.finora.security;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public final class Validation {
    private Validation() { }

    public static String requiredText(String value, String label, int maxLength) {
        String cleaned = value == null ? "" : value.trim();
        if (cleaned.isEmpty() || cleaned.length() > maxLength) throw new IllegalArgumentException(label + " is required and must be at most " + maxLength + " characters.");
        return cleaned;
    }

    public static long amountCents(String value, String label) {
        long cents = parseCents(value, label);
        if (cents <= 0) throw new IllegalArgumentException(label + " must be greater than zero.");
        return cents;
    }

    public static long nonNegativeAmountCents(String value, String label) {
        long cents = parseCents(value, label);
        if (cents < 0) throw new IllegalArgumentException(label + " cannot be negative.");
        return cents;
    }

    private static long parseCents(String value, String label) {
        try {
            BigDecimal amount = new BigDecimal(value.trim()).setScale(2, RoundingMode.UNNECESSARY);
            long cents = amount.movePointRight(2).longValueExact();
            return cents;
        } catch (NumberFormatException | ArithmeticException | NullPointerException exception) {
            throw new IllegalArgumentException(label + " must be a valid amount with at most two decimal places.");
        }
    }

    public static LocalDate date(String value, String label) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException | NullPointerException exception) {
            throw new IllegalArgumentException(label + " must be a valid date.");
        }
    }

    public static String email(String value) {
        String email = requiredText(value, "Email", 160).toLowerCase(java.util.Locale.ROOT);
        if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) throw new IllegalArgumentException("Enter a valid email address.");
        return email;
    }
}
