package com.finora.web;

import com.finora.config.Database;
import com.finora.model.User;
import com.finora.security.FinanceCipher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.util.Locale;

final class WebSupport {
    private WebSupport() { }
    static Database database(HttpServletRequest request) { return (Database) request.getServletContext().getAttribute("database"); }
    static FinanceCipher cipher(HttpServletRequest request) { return (FinanceCipher) request.getServletContext().getAttribute("financeCipher"); }
    static User user(HttpServletRequest request) { return (User) request.getSession(false).getAttribute("user"); }
    static long userId(HttpServletRequest request) { return user(request).getId(); }
    static long parseId(String value) {
        try { long id = Long.parseLong(value); if (id < 1) throw new NumberFormatException(); return id; }
        catch (Exception exception) { throw new IllegalArgumentException("That record could not be found."); }
    }
    static String money(long cents) { return String.format(Locale.US, "₹%,.2f", cents / 100.0); }
    static void flash(HttpServletRequest request, String type, String message) {
        HttpSession session = request.getSession();
        session.setAttribute("flashType", type);
        session.setAttribute("flashMessage", message);
    }
    static void exposeFlash(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            request.setAttribute("flashType", session.getAttribute("flashType"));
            request.setAttribute("flashMessage", session.getAttribute("flashMessage"));
            session.removeAttribute("flashType"); session.removeAttribute("flashMessage");
        }
    }
    static void forward(HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response, String view)
            throws jakarta.servlet.ServletException, java.io.IOException {
        exposeFlash(request);
        request.getRequestDispatcher("/WEB-INF/views/" + view + ".jsp").forward(request, response);
    }
}
