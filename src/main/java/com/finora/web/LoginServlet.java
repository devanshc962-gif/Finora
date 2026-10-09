package com.finora.web;

import com.finora.dao.AuditDao;
import com.finora.dao.SettingsDao;
import com.finora.dao.UserDao;
import com.finora.model.User;
import com.finora.security.PasswordHasher;
import com.finora.security.Validation;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet(urlPatterns = {"/login", "/register", "/logout", "/maintenance"})
public class LoginServlet extends HttpServlet {
    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if ("/logout".equals(req.getServletPath())) { resp.sendRedirect(req.getContextPath() + "/login"); return; }
        boolean maintenance;
        try { maintenance = new SettingsDao(WebSupport.database(req)).getBoolean("maintenance_mode", false); }
        catch (Exception exception) { throw new ServletException(exception); }
        if ("/maintenance".equals(req.getServletPath()) && !maintenance && req.getSession(false) == null) {
            resp.sendRedirect(req.getContextPath() + "/login"); return;
        }
        if (req.getSession(false) != null && req.getSession(false).getAttribute("user") instanceof User user) {
            if ("/maintenance".equals(req.getServletPath()) && "USER".equals(user.getRole()) && maintenance) {
                WebSupport.forward(req, resp, "maintenance"); return;
            }
            resp.sendRedirect(req.getContextPath() + ("ADMIN".equals(user.getRole()) ? "/admin" : maintenance ? "/maintenance" : "/app/dashboard"));
            return;
        }
        if ("/maintenance".equals(req.getServletPath()) || (maintenance && "/register".equals(req.getServletPath()))) { WebSupport.forward(req, resp, "maintenance"); return; }
        boolean registrationEnabled;
        try {
            registrationEnabled = new SettingsDao(WebSupport.database(req)).registrationEnabled();
            req.setAttribute("registrationEnabled", registrationEnabled);
        }
        catch (Exception exception) { throw new ServletException(exception); }
        boolean registerMode = "/register".equals(req.getServletPath());
        if (registerMode && !registrationEnabled) {
            req.setAttribute("registerMode", false);
            req.setAttribute("error", "New account registration is currently closed.");
            WebSupport.forward(req, resp, "auth");
            return;
        }
        req.setAttribute("registerMode", registerMode);
        if (req.getParameter("disabled") != null) req.setAttribute("error", "This account has been disabled. Contact an administrator.");
        WebSupport.forward(req, resp, "auth");
    }

    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            if ("/maintenance".equals(req.getServletPath())) { resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED); return; }
            UserDao users = new UserDao(WebSupport.database(req));
            AuditDao audit = new AuditDao(WebSupport.database(req));
            if ("/logout".equals(req.getServletPath())) {
                HttpSession session = req.getSession(false);
                Long actorId = session != null && session.getAttribute("user") instanceof User user ? user.getId() : null;
                audit.record(actorId, "LOGOUT", "User signed out");
                if (session != null) session.invalidate();
                resp.sendRedirect(req.getContextPath() + "/login?loggedOut=1"); return;
            }
            String email = Validation.requiredText(req.getParameter("email"), "Email", 160).toLowerCase(java.util.Locale.ROOT);
            String password = req.getParameter("password");
            if (password == null || password.length() < 8 || password.length() > 200) throw new IllegalArgumentException("Password must be between 8 and 200 characters.");
            if ("/register".equals(req.getServletPath())) {
                if (new SettingsDao(WebSupport.database(req)).getBoolean("maintenance_mode", false)) { WebSupport.forward(req, resp, "maintenance"); return; }
                if (!new SettingsDao(WebSupport.database(req)).registrationEnabled()) throw new IllegalArgumentException("New account registration is currently closed.");
                email = Validation.email(email);
                String name = Validation.requiredText(req.getParameter("name"), "Name", 80);
                users.createUser(name, email, PasswordHasher.hash(password));
                audit.record(null, "ACCOUNT_CREATED", "A new user account was registered");
                WebSupport.flash(req, "success", "Your account is ready. Sign in to continue.");
                resp.sendRedirect(req.getContextPath() + "/login"); return;
            }
            UserDao.Credentials credentials = users.findCredentials(email).orElse(null);
            if (credentials == null || !PasswordHasher.verify(password, credentials.passwordHash()) || !credentials.user().isActive()) {
                audit.record(credentials == null ? null : credentials.user().getId(), "LOGIN_FAILED", "Unsuccessful sign-in attempt");
                throw new IllegalArgumentException("Email or password is incorrect.");
            }
            if ("USER".equals(credentials.user().getRole()) && new SettingsDao(WebSupport.database(req)).getBoolean("maintenance_mode", false)) {
                audit.record(credentials.user().getId(), "LOGIN_BLOCKED_MAINTENANCE", "User login held during maintenance");
                WebSupport.forward(req, resp, "maintenance"); return;
            }
            HttpSession session = req.getSession(true);
            req.changeSessionId();
            session.setAttribute("user", credentials.user());
            audit.record(credentials.user().getId(), "LOGIN", "User signed in");
            resp.sendRedirect(req.getContextPath() + ("ADMIN".equals(credentials.user().getRole()) ? "/admin" : "/app/dashboard"));
        } catch (IllegalArgumentException exception) {
            boolean registerMode = "/register".equals(req.getServletPath());
            boolean registrationEnabled;
            try { registrationEnabled = new SettingsDao(WebSupport.database(req)).registrationEnabled(); }
            catch (Exception ignored) { registrationEnabled = false; }
            req.setAttribute("registrationEnabled", registrationEnabled);
            req.setAttribute("registerMode", registerMode && registrationEnabled);
            req.setAttribute("error", registerMode && !registrationEnabled
                    ? "New account registration is currently closed."
                    : exception.getMessage());
            WebSupport.forward(req, resp, "auth");
        } catch (java.sql.SQLException exception) {
            if (exception.getMessage() != null && exception.getMessage().toLowerCase().contains("unique")) {
                req.setAttribute("error", "An account with that email already exists.");
                req.setAttribute("registerMode", true);
                req.setAttribute("registrationEnabled", true);
                WebSupport.forward(req, resp, "auth");
            } else throw new ServletException(exception);
        } catch (Exception exception) { throw new ServletException(exception); }
    }
}
