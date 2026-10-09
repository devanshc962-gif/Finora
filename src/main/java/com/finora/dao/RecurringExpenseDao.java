package com.finora.dao;

import com.finora.config.Database;
import com.finora.model.RecurringExpense;
import com.finora.security.FinanceCipher;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class RecurringExpenseDao {
    private final Database database;
    private final FinanceCipher cipher;

    public RecurringExpenseDao(Database database, FinanceCipher cipher) {
        this.database = database;
        this.cipher = cipher;
    }

    public List<RecurringExpense> findByUser(long userId) {
        List<RecurringExpense> rows = new ArrayList<>();
        try (Connection connection = database.getConnection(); PreparedStatement statement = connection.prepareStatement(
                "SELECT id, active, encrypted_payload FROM recurring_expenses WHERE user_id=? ORDER BY id DESC")) {
            statement.setLong(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) rows.add(fromRow(result.getLong("id"), result.getBoolean("active"), result.getString("encrypted_payload")));
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Could not load recurring expenses", exception);
        }
        rows.sort((first, second) -> first.getNextDueOn().compareTo(second.getNextDueOn()));
        return rows;
    }

    public void create(long userId, String description, String category, long amountCents, String frequency, LocalDate firstDue) throws SQLException {
        String payload = cipher.encryptFields(description, category, Long.toString(amountCents), frequency, firstDue.toString(), firstDue.toString());
        try (Connection connection = database.getConnection(); PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO recurring_expenses(user_id, active, encrypted_payload) VALUES(?,1,?)")) {
            statement.setLong(1, userId);
            statement.setString(2, payload);
            statement.executeUpdate();
        }
    }

    public boolean setActive(long id, long userId, boolean active) throws SQLException {
        try (Connection connection = database.getConnection(); PreparedStatement statement = connection.prepareStatement(
                "UPDATE recurring_expenses SET active=? WHERE id=? AND user_id=?")) {
            statement.setBoolean(1, active);
            statement.setLong(2, id);
            statement.setLong(3, userId);
            return statement.executeUpdate() == 1;
        }
    }

    public boolean delete(long id, long userId) throws SQLException {
        try (Connection connection = database.getConnection(); PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM recurring_expenses WHERE id=? AND user_id=?")) {
            statement.setLong(1, id);
            statement.setLong(2, userId);
            return statement.executeUpdate() == 1;
        }
    }

    /** Records one due occurrence and advances its schedule atomically. */
    public LocalDate recordDue(long id, long userId, LocalDate today) throws Exception {
        try (Connection connection = database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                String oldPayload;
                boolean active;
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT active, encrypted_payload FROM recurring_expenses WHERE id=? AND user_id=?")) {
                    statement.setLong(1, id);
                    statement.setLong(2, userId);
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) throw new IllegalArgumentException("Recurring expense not found.");
                        active = result.getBoolean("active");
                        oldPayload = result.getString("encrypted_payload");
                    }
                }
                if (!active) throw new IllegalArgumentException("This recurring expense is paused. Resume it before recording.");
                String[] fields = cipher.decryptFields(oldPayload, 6);
                LocalDate dueDate = LocalDate.parse(fields[4]);
                if (dueDate.isAfter(today)) throw new IllegalArgumentException("This expense is not due yet. It is due on " + dueDate + ".");

                long amountCents = Long.parseLong(fields[2]);
                new ExpenseDao(database, cipher).create(connection, userId, fields[0], fields[1], amountCents, dueDate.toString());
                LocalDate nextDue = nextDueDate(fields[3], dueDate, LocalDate.parse(fields[5]));
                fields[4] = nextDue.toString();
                String updatedPayload = cipher.encryptFields(fields);
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE recurring_expenses SET encrypted_payload=? WHERE id=? AND user_id=? AND active=1 AND encrypted_payload=?")) {
                    statement.setString(1, updatedPayload);
                    statement.setLong(2, id);
                    statement.setLong(3, userId);
                    statement.setString(4, oldPayload);
                    if (statement.executeUpdate() != 1) throw new IllegalArgumentException("This reminder changed before it could be recorded. Refresh and try again.");
                }
                connection.commit();
                return nextDue;
            } catch (Exception exception) {
                try { connection.rollback(); } catch (SQLException rollbackError) { exception.addSuppressed(rollbackError); }
                throw exception;
            }
        }
    }

    private RecurringExpense fromRow(long id, boolean active, String payload) {
        String[] fields = cipher.decryptFields(payload, 6);
        return new RecurringExpense(id, fields[0], fields[1], Long.parseLong(fields[2]), fields[3],
                LocalDate.parse(fields[4]), LocalDate.parse(fields[5]), active);
    }

    private static LocalDate nextDueDate(String frequency, LocalDate due, LocalDate anchor) {
        return switch (frequency) {
            case "WEEKLY" -> due.plusWeeks(1);
            case "MONTHLY" -> {
                YearMonth nextMonth = YearMonth.from(due).plusMonths(1);
                yield nextMonth.atDay(Math.min(anchor.getDayOfMonth(), nextMonth.lengthOfMonth()));
            }
            case "YEARLY" -> {
                YearMonth targetMonth = YearMonth.of(due.getYear() + 1, anchor.getMonth());
                yield targetMonth.atDay(Math.min(anchor.getDayOfMonth(), targetMonth.lengthOfMonth()));
            }
            default -> throw new IllegalStateException("Unknown recurring expense frequency");
        };
    }
}
