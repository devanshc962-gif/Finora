package com.finora.web;

import com.finora.dao.AuditDao;
import com.finora.dao.BudgetDao;
import com.finora.dao.ExpenseDao;
import com.finora.dao.GoalDao;
import com.finora.dao.RecurringExpenseDao;
import com.finora.dao.SettingsDao;
import com.finora.model.Budget;
import com.finora.model.Expense;
import com.finora.model.Goal;
import com.finora.model.RecurringExpense;
import com.finora.security.Validation;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@WebServlet("/app/*")
public class AppServlet extends HttpServlet {
    private static final List<String> CATEGORIES = List.of("Food", "Transport", "Housing", "Utilities", "Health", "Shopping", "Education", "Entertainment", "Other");

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String page = req.getPathInfo() == null ? "/dashboard" : req.getPathInfo();
        try {
            SettingsDao settings = new SettingsDao(WebSupport.database(req));
            boolean budgetsEnabled = settings.getBoolean("budgets_enabled", true);
            boolean goalsEnabled = settings.getBoolean("goals_enabled", true);
            boolean reportsEnabled = settings.getBoolean("reports_enabled", true);
            req.setAttribute("budgetsEnabled", budgetsEnabled);
            req.setAttribute("goalsEnabled", goalsEnabled);
            req.setAttribute("reportsEnabled", reportsEnabled);
            if (("/budgets".equals(page) && !budgetsEnabled) || ("/goals".equals(page) && !goalsEnabled)) {
                WebSupport.flash(req, "error", "This feature is temporarily disabled by an administrator.");
                resp.sendRedirect(req.getContextPath() + "/app/dashboard"); return;
            }
            req.setAttribute("categories", CATEGORIES);
            LocalDate today = LocalDate.now();
            req.setAttribute("today", today.toString());
            req.setAttribute("budgetStart", today.withDayOfMonth(1).toString());
            req.setAttribute("budgetEnd", today.withDayOfMonth(today.lengthOfMonth()).toString());
            switch (page) {
                case "/", "/dashboard" -> dashboard(req);
                case "/expenses" -> expenses(req);
                case "/budgets" -> budgets(req);
                case "/goals" -> goals(req);
                default -> { resp.sendError(404); return; }
            }
            WebSupport.forward(req, resp, page.equals("/expenses") ? "expenses" : page.equals("/budgets") ? "budgets" : page.equals("/goals") ? "goals" : "dashboard");
        } catch (Exception exception) { throw new ServletException("Unable to load Finora", exception); }
    }

    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String[] parts = (req.getPathInfo() == null ? "" : req.getPathInfo()).split("/");
        if (parts.length < 3) { resp.sendError(404); return; }
        String section = parts[1], action = parts[2];
        long userId = WebSupport.userId(req);
        try {
            SettingsDao settings = new SettingsDao(WebSupport.database(req));
            if (("budgets".equals(section) && !settings.getBoolean("budgets_enabled", true)) ||
                    ("goals".equals(section) && !settings.getBoolean("goals_enabled", true))) {
                throw new IllegalArgumentException("This feature is temporarily disabled by an administrator.");
            }
            String successMessage;
            switch (section) {
                case "expenses" -> { saveExpense(req, userId, action); successMessage = "Your changes have been saved."; }
                case "recurring" -> successMessage = saveRecurringExpense(req, userId, action);
                case "budgets" -> { saveBudget(req, userId, action); successMessage = "Your changes have been saved."; }
                case "goals" -> { saveGoal(req, userId, action); successMessage = "Your changes have been saved."; }
                default -> { resp.sendError(404); return; }
            }
            WebSupport.flash(req, "success", successMessage);
        } catch (IllegalArgumentException exception) {
            WebSupport.flash(req, "error", exception.getMessage());
        } catch (Exception exception) {
            throw new ServletException("Unable to save your changes", exception);
        }
        resp.sendRedirect(req.getContextPath() + "/app/" + ("recurring".equals(section) ? "expenses" : section));
    }

    private void dashboard(HttpServletRequest req) throws Exception {
        long userId = WebSupport.userId(req);
        ExpenseDao expenseDao = new ExpenseDao(WebSupport.database(req), WebSupport.cipher(req));
        LocalDate now = LocalDate.now();
        long monthTotal = expenseDao.totalForMonth(userId, now.withDayOfMonth(1).toString(), now.withDayOfMonth(1).plusMonths(1).toString());
        List<Expense> recent = expenseDao.findByUser(userId, null, now.minusDays(30).toString(), null);
        if (recent.size() > 5) recent = recent.subList(0, 5);
        List<Map<String, Object>> categoryRows = expenseDao.categoryTotals(userId, now.withDayOfMonth(1).toString(), now.withDayOfMonth(1).plusMonths(1).toString());
        long max = categoryRows.stream().mapToLong(row -> (long) row.get("amountCents")).max().orElse(1);
        List<Map<String, Object>> categories = new ArrayList<>();
        for (Map<String, Object> row : categoryRows) categories.add(Map.of("category", row.get("category"),
                "amount", WebSupport.money((long) row.get("amountCents")), "width", Math.max(4, Math.round((long) row.get("amountCents") * 100.0 / max))));
        List<Budget> budgetRows = new BudgetDao(WebSupport.database(req), WebSupport.cipher(req)).findByUser(userId);
        List<Goal> goalRows = new GoalDao(WebSupport.database(req), WebSupport.cipher(req)).findByUser(userId);
        req.setAttribute("monthTotal", WebSupport.money(monthTotal));
        req.setAttribute("recentExpenses", recent);
        req.setAttribute("categoryRows", categories);
        req.setAttribute("budgetCount", budgetRows.size());
        req.setAttribute("goalCount", goalRows.size());
        req.setAttribute("budgetRows", budgetRows.stream().limit(3).toList());
        req.setAttribute("goalRows", goalRows.stream().limit(3).toList());
    }

    private void expenses(HttpServletRequest req) throws Exception {
        String category = req.getParameter("category");
        if (category != null && !category.isBlank() && !CATEGORIES.contains(category)) category = null;
        String from = optionalDate(req.getParameter("from"), "Start date");
        String to = optionalDate(req.getParameter("to"), "End date");
        List<Expense> expenseRows = new ExpenseDao(WebSupport.database(req), WebSupport.cipher(req)).findByUser(WebSupport.userId(req), category, from, to);
        List<RecurringExpense> recurringRows = new RecurringExpenseDao(WebSupport.database(req), WebSupport.cipher(req)).findByUser(WebSupport.userId(req));
        req.setAttribute("expenses", expenseRows);
        req.setAttribute("recurringExpenses", recurringRows);
        req.setAttribute("filteredExpenseTotal", WebSupport.money(expenseRows.stream().mapToLong(Expense::getAmountCents).sum()));
        req.setAttribute("selectedCategory", category == null ? "" : category);
        req.setAttribute("from", from == null ? "" : from);
        req.setAttribute("to", to == null ? "" : to);
    }

    private void budgets(HttpServletRequest req) throws Exception {
        req.setAttribute("budgets", new BudgetDao(WebSupport.database(req), WebSupport.cipher(req)).findByUser(WebSupport.userId(req)));
    }

    private void goals(HttpServletRequest req) throws Exception {
        req.setAttribute("goals", new GoalDao(WebSupport.database(req), WebSupport.cipher(req)).findByUser(WebSupport.userId(req)));
    }

    private void saveExpense(HttpServletRequest req, long userId, String action) throws Exception {
        ExpenseDao dao = new ExpenseDao(WebSupport.database(req), WebSupport.cipher(req));
        AuditDao audit = new AuditDao(WebSupport.database(req));
        if ("create".equals(action) || "update".equals(action)) {
            String description = Validation.requiredText(req.getParameter("description"), "Description", 120);
            String category = category(req.getParameter("category"));
            long amount = Validation.amountCents(req.getParameter("amount"), "Amount");
            String date = Validation.date(req.getParameter("spentOn"), "Expense date").toString();
            if ("create".equals(action)) { dao.create(userId, description, category, amount, date); audit.record(userId, "EXPENSE_CREATED", "Expense added"); }
            else { if (!dao.update(WebSupport.parseId(req.getParameter("id")), userId, description, category, amount, date)) throw new IllegalArgumentException("Expense not found."); audit.record(userId, "EXPENSE_UPDATED", "Expense updated"); }
        } else if ("delete".equals(action)) {
            if (!dao.delete(WebSupport.parseId(req.getParameter("id")), userId)) throw new IllegalArgumentException("Expense not found.");
            audit.record(userId, "EXPENSE_DELETED", "Expense removed");
        } else throw new IllegalArgumentException("Unknown expense action.");
    }

    private String saveRecurringExpense(HttpServletRequest req, long userId, String action) throws Exception {
        RecurringExpenseDao dao = new RecurringExpenseDao(WebSupport.database(req), WebSupport.cipher(req));
        AuditDao audit = new AuditDao(WebSupport.database(req));
        switch (action) {
            case "create" -> {
                String description = Validation.requiredText(req.getParameter("description"), "Description", 120);
                String category = category(req.getParameter("category"));
                long amount = Validation.amountCents(req.getParameter("amount"), "Amount");
                String frequency = req.getParameter("frequency");
                if (frequency == null || !List.of("WEEKLY", "MONTHLY", "YEARLY").contains(frequency)) throw new IllegalArgumentException("Choose a valid repeat schedule.");
                LocalDate firstDue = Validation.date(req.getParameter("nextDueOn"), "First due date");
                dao.create(userId, description, category, amount, frequency, firstDue);
                audit.record(userId, "RECURRING_CREATED", "Recurring expense reminder created");
                return "Recurring expense reminder added.";
            }
            case "record" -> {
                LocalDate nextDue = dao.recordDue(WebSupport.parseId(req.getParameter("id")), userId, LocalDate.now());
                audit.record(userId, "RECURRING_RECORDED", "Due recurring expense recorded");
                return "Expense recorded. The next occurrence is due " + nextDue + ".";
            }
            case "pause", "resume" -> {
                boolean active = "resume".equals(action);
                if (!dao.setActive(WebSupport.parseId(req.getParameter("id")), userId, active)) throw new IllegalArgumentException("Recurring expense not found.");
                audit.record(userId, active ? "RECURRING_RESUMED" : "RECURRING_PAUSED", active ? "Recurring expense reminder resumed" : "Recurring expense reminder paused");
                return active ? "Recurring reminder resumed." : "Recurring reminder paused.";
            }
            case "delete" -> {
                if (!dao.delete(WebSupport.parseId(req.getParameter("id")), userId)) throw new IllegalArgumentException("Recurring expense not found.");
                audit.record(userId, "RECURRING_DELETED", "Recurring expense reminder deleted");
                return "Recurring expense reminder deleted. Previously recorded expenses were kept.";
            }
            default -> throw new IllegalArgumentException("Unknown recurring expense action.");
        }
    }

    private void saveBudget(HttpServletRequest req, long userId, String action) throws Exception {
        BudgetDao dao = new BudgetDao(WebSupport.database(req), WebSupport.cipher(req));
        AuditDao audit = new AuditDao(WebSupport.database(req));
        if ("create".equals(action) || "update".equals(action)) {
            String category = category(req.getParameter("category"));
            long limit = Validation.amountCents(req.getParameter("limit"), "Budget limit");
            LocalDate start = Validation.date(req.getParameter("startsOn"), "Start date");
            LocalDate end = Validation.date(req.getParameter("endsOn"), "End date");
            if (end.isBefore(start)) throw new IllegalArgumentException("End date must be on or after start date.");
            if ("create".equals(action)) { dao.create(userId, category, limit, start.toString(), end.toString()); audit.record(userId, "BUDGET_CREATED", "Budget created"); }
            else { if (!dao.update(WebSupport.parseId(req.getParameter("id")), userId, category, limit, start.toString(), end.toString())) throw new IllegalArgumentException("Budget not found."); audit.record(userId, "BUDGET_UPDATED", "Budget updated"); }
        } else if ("delete".equals(action)) {
            if (!dao.delete(WebSupport.parseId(req.getParameter("id")), userId)) throw new IllegalArgumentException("Budget not found.");
            audit.record(userId, "BUDGET_DELETED", "Budget removed");
        } else throw new IllegalArgumentException("Unknown budget action.");
    }

    private void saveGoal(HttpServletRequest req, long userId, String action) throws Exception {
        GoalDao dao = new GoalDao(WebSupport.database(req), WebSupport.cipher(req));
        AuditDao audit = new AuditDao(WebSupport.database(req));
        if ("create".equals(action) || "update".equals(action)) {
            String title = Validation.requiredText(req.getParameter("title"), "Goal name", 100);
            long target = Validation.amountCents(req.getParameter("target"), "Target amount");
            long saved = req.getParameter("saved") == null || req.getParameter("saved").isBlank() ? 0 : Validation.nonNegativeAmountCents(req.getParameter("saved"), "Saved amount");
            if (saved > target) throw new IllegalArgumentException("Saved amount cannot exceed the target.");
            String deadline = Validation.date(req.getParameter("deadline"), "Deadline").toString();
            if ("create".equals(action)) { dao.create(userId, title, target, saved, deadline); audit.record(userId, "GOAL_CREATED", "Financial goal created"); }
            else { if (!dao.update(WebSupport.parseId(req.getParameter("id")), userId, title, target, saved, deadline)) throw new IllegalArgumentException("Goal not found."); audit.record(userId, "GOAL_UPDATED", "Financial goal updated"); }
        } else if ("delete".equals(action)) {
            if (!dao.delete(WebSupport.parseId(req.getParameter("id")), userId)) throw new IllegalArgumentException("Goal not found.");
            audit.record(userId, "GOAL_DELETED", "Financial goal removed");
        } else throw new IllegalArgumentException("Unknown goal action.");
    }

    private static String category(String value) {
        if (value == null || !CATEGORIES.contains(value)) throw new IllegalArgumentException("Choose a valid category.");
        return value;
    }
    private static String optionalDate(String value, String label) {
        return value == null || value.isBlank() ? null : Validation.date(value, label).toString();
    }
}
