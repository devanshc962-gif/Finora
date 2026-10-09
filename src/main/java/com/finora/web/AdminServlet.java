package com.finora.web;

import com.finora.config.Database;
import com.finora.dao.AuditDao;
import com.finora.dao.SettingsDao;
import com.finora.dao.UserDao;
import com.finora.model.User;
import com.finora.security.FinanceCipher;
import com.finora.security.FinanceEncryptionMigrator;
import com.finora.security.PasswordHasher;
import com.finora.security.Validation;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

@WebServlet("/admin/*")
public class AdminServlet extends HttpServlet {
    private static final Set<String> MANAGED_SETTINGS = Set.of("registration_enabled", "maintenance_mode", "budgets_enabled", "goals_enabled", "reports_enabled");

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo() == null ? "/" : req.getPathInfo();
        if ("/reports/audit.csv".equals(path)) { exportAudit(req, resp); return; }
        try {
            UserDao users = new UserDao(WebSupport.database(req));
            SettingsDao settings = new SettingsDao(WebSupport.database(req));
            AuditDao audit = new AuditDao(WebSupport.database(req));
            String userQuery = cleanFilter(req.getParameter("userQuery"), 80);
            String role = "USER".equals(req.getParameter("role")) || "ADMIN".equals(req.getParameter("role")) ? req.getParameter("role") : null;
            String requestedStatus = req.getParameter("status");
            String status = "active".equals(requestedStatus) || "inactive".equals(requestedStatus) ? requestedStatus : null;
            String auditQuery = cleanFilter(req.getParameter("auditQuery"), 100);
            String auditEvent = cleanFilter(req.getParameter("event"), 48);
            String from = safeDate(req.getParameter("from"));
            String to = safeDate(req.getParameter("to"));
            req.setAttribute("users", users.findAll(userQuery, role, status));
            req.setAttribute("userQuery", userQuery == null ? "" : userQuery);
            req.setAttribute("selectedRole", role == null ? "" : role);
            req.setAttribute("selectedStatus", status == null ? "" : status);
            req.setAttribute("auditQuery", auditQuery == null ? "" : auditQuery);
            req.setAttribute("selectedEvent", auditEvent == null ? "" : auditEvent);
            req.setAttribute("auditFrom", from == null ? "" : from);
            req.setAttribute("auditTo", to == null ? "" : to);
            List<Map<String, Object>> auditRows = audit.search(auditQuery, auditEvent, from, to, 100);
            req.setAttribute("auditLogs", auditRows);
            req.setAttribute("totalAccounts", users.countAll());
            req.setAttribute("activeAccounts", users.countActive());
            req.setAttribute("inactiveAccounts", users.countInactive());
            req.setAttribute("activeAdmins", users.countAdmins());
            req.setAttribute("registrationEnabled", settings.getBoolean("registration_enabled", true));
            req.setAttribute("maintenanceMode", settings.getBoolean("maintenance_mode", false));
            req.setAttribute("budgetsEnabled", settings.getBoolean("budgets_enabled", true));
            req.setAttribute("goalsEnabled", settings.getBoolean("goals_enabled", true));
            req.setAttribute("reportsEnabled", settings.getBoolean("reports_enabled", true));
            req.setAttribute("securityEvents30d", audit.countAllInLastDays(30));
            req.setAttribute("failedLogins30d", audit.countEventInLastDays("LOGIN_FAILED", 30));
            req.setAttribute("encryptionLabel", WebSupport.cipher(req).getEncryptionLabel());
            req.setAttribute("keyBackupPath", WebSupport.cipher(req).getKeyringPath().toString());
            req.setAttribute("today", LocalDate.now().toString());
            WebSupport.forward(req, resp, "admin");
        } catch (Exception exception) { throw new ServletException("Unable to load the admin dashboard", exception); }
    }

    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action");
        try {
            User current = WebSupport.user(req);
            UserDao users = new UserDao(WebSupport.database(req));
            AuditDao audit = new AuditDao(WebSupport.database(req));
            SettingsDao settings = new SettingsDao(WebSupport.database(req));
            if ("create-user".equals(action)) {
                String name = Validation.requiredText(req.getParameter("name"), "Name", 80);
                String email = Validation.email(req.getParameter("email"));
                String password = adminPassword(req.getParameter("password"));
                String role = validRole(req.getParameter("role"));
                users.createAdminManagedUser(name, email, PasswordHasher.hash(password), role);
                audit.record(current.getId(), "USER_CREATED_BY_ADMIN", "Created " + role.toLowerCase() + " account");
                WebSupport.flash(req, "success", "Account created.");
            } else if ("update-user".equals(action)) {
                long id = WebSupport.parseId(req.getParameter("id"));
                String name = Validation.requiredText(req.getParameter("name"), "Name", 80);
                String email = Validation.email(req.getParameter("email"));
                String role = validRole(req.getParameter("role"));
                boolean active = "on".equals(req.getParameter("active"));
                if (id == current.getId() && (!active || !"ADMIN".equals(role))) throw new IllegalArgumentException("You cannot disable or demote your own administrator account.");
                User target = users.findById(id).orElseThrow(() -> new IllegalArgumentException("Account not found."));
                if (isLastAdminBeingRemoved(users, target, role, active)) throw new IllegalArgumentException("Keep at least one active administrator account.");
                String newPassword = req.getParameter("password");
                String passwordHash = newPassword != null && !newPassword.isBlank() ? PasswordHasher.hash(adminPassword(newPassword)) : null;
                if (!users.updateProfileAndAccess(id, name, email, role, active)) throw new IllegalArgumentException("Account not found.");
                if (passwordHash != null) users.updatePassword(id, passwordHash);
                audit.record(current.getId(), "USER_ACCOUNT_UPDATED", "Account " + id + " profile or access updated");
                WebSupport.flash(req, "success", "Account updated.");
            } else if ("delete-user".equals(action)) {
                long id = WebSupport.parseId(req.getParameter("id"));
                if (id == current.getId()) throw new IllegalArgumentException("You cannot delete your own administrator account.");
                if (!"DELETE".equals(req.getParameter("confirmation"))) throw new IllegalArgumentException("Confirm permanent deletion before continuing.");
                User target = users.findById(id).orElseThrow(() -> new IllegalArgumentException("Account not found."));
                if ("ADMIN".equals(target.getRole()) && target.isActive() && users.countActiveAdmins() <= 1) throw new IllegalArgumentException("Keep at least one active administrator account.");
                audit.record(current.getId(), "USER_ACCOUNT_DELETED", "Deleted account " + id + " and its finance records");
                users.delete(id);
                WebSupport.flash(req, "success", "Account and its associated finance records were deleted.");
            } else if ("save-settings".equals(action)) {
                StringBuilder changed = new StringBuilder();
                for (String key : MANAGED_SETTINGS) {
                    boolean enabled = "on".equals(req.getParameter(key));
                    settings.setBoolean(key, enabled);
                    if (!changed.isEmpty()) changed.append(", ");
                    changed.append(key).append('=').append(enabled);
                }
                audit.record(current.getId(), "SYSTEM_SETTINGS_UPDATED", changed.toString());
                WebSupport.flash(req, "success", "Platform settings saved.");
            } else if ("rotate-encryption-key".equals(action)) {
                if (!"yes".equals(req.getParameter("confirmRotation"))) throw new IllegalArgumentException("Confirm that you backed up the current key ring before rotating it.");
                FinanceCipher cipher = WebSupport.cipher(req);
                cipher.activateNewKey();
                new FinanceEncryptionMigrator(WebSupport.database(req), cipher).migrateAndRekey();
                audit.record(current.getId(), "ENCRYPTION_KEY_ROTATED", "Finance data re-encrypted with a new AES-GCM key");
                WebSupport.flash(req, "success", "Encryption key rotated and finance records re-encrypted. Back up the updated key ring.");
            } else throw new IllegalArgumentException("Unknown admin action.");
        } catch (IllegalArgumentException exception) {
            WebSupport.flash(req, "error", exception.getMessage());
        } catch (SQLException exception) {
            String message = exception.getMessage() == null ? "" : exception.getMessage().toLowerCase(java.util.Locale.ROOT);
            if (exception.getErrorCode() == 19 || message.contains("unique constraint")) WebSupport.flash(req, "error", "That email address is already in use.");
            else throw new ServletException("Unable to apply the admin change", exception);
        } catch (Exception exception) { throw new ServletException("Unable to apply the admin change", exception); }
        resp.sendRedirect(req.getContextPath() + "/admin");
    }

    private void exportAudit(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String query = cleanFilter(req.getParameter("auditQuery"), 100);
            String event = cleanFilter(req.getParameter("event"), 48);
            String from = safeDate(req.getParameter("from"));
            String to = safeDate(req.getParameter("to"));
            List<Map<String, Object>> rows = new AuditDao(WebSupport.database(req)).search(query, event, from, to, 5000);
            resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
            resp.setContentType("text/csv; charset=UTF-8");
            resp.setHeader("Content-Disposition", "attachment; filename=\"finora-audit-report.csv\"");
            try (PrintWriter out = resp.getWriter()) {
                out.println("Event,Actor,Details,Time");
                for (Map<String, Object> row : rows) out.println(csv(row.get("event")) + "," + csv(row.get("actorEmail")) + "," + csv(row.get("details")) + "," + csv(row.get("occurredAt")));
            }
        } catch (Exception exception) { throw new IOException("Could not generate the audit report", exception); }
    }

    private static boolean isLastAdminBeingRemoved(UserDao users, User target, String newRole, boolean active) throws Exception {
        return "ADMIN".equals(target.getRole()) && target.isActive() && (!"ADMIN".equals(newRole) || !active) && users.countActiveAdmins() <= 1;
    }
    private static String validRole(String role) {
        if (!"USER".equals(role) && !"ADMIN".equals(role)) throw new IllegalArgumentException("Choose a valid role.");
        return role;
    }
    private static String adminPassword(String password) {
        if (password == null || password.length() < 12 || password.length() > 200) throw new IllegalArgumentException("Administrator-created passwords must be 12 to 200 characters.");
        return password;
    }
    private static String cleanFilter(String value, int limit) {
        if (value == null || value.isBlank()) return null;
        return value.trim().substring(0, Math.min(value.trim().length(), limit));
    }
    private static String safeDate(String value) {
        if (value == null || value.isBlank()) return null;
        try { return Validation.date(value, "Report date").toString(); }
        catch (IllegalArgumentException exception) { return null; }
    }
    private static String csv(Object value) {
        String text = value == null ? "" : value.toString();
        String trimmed = text.stripLeading();
        if (!trimmed.isEmpty() && "=+-@".indexOf(trimmed.charAt(0)) >= 0) text = "'" + text;
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }
}
