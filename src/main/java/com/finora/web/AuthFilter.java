package com.finora.web;

import com.finora.dao.UserDao;
import com.finora.dao.SettingsDao;
import com.finora.model.User;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

@WebFilter({"/app/*", "/admin/*"})
public class AuthFilter implements Filter {
    @Override public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        HttpSession session = req.getSession(false);
        User current = session == null ? null : (User) session.getAttribute("user");
        if (current == null) { res.sendRedirect(req.getContextPath() + "/login"); return; }
        try {
            User fresh = new UserDao(WebSupport.database(req)).findById(current.getId()).orElse(null);
            if (fresh == null || !fresh.isActive()) {
                session.invalidate(); res.sendRedirect(req.getContextPath() + "/login?disabled=1"); return;
            }
            session.setAttribute("user", fresh);
            String servletPath = req.getServletPath();
            if (("/admin".equals(servletPath) || servletPath.startsWith("/admin/")) && !"ADMIN".equals(fresh.getRole())) { res.sendError(403); return; }
            if ("USER".equals(fresh.getRole()) && new SettingsDao(WebSupport.database(req)).getBoolean("maintenance_mode", false)) {
                res.sendRedirect(req.getContextPath() + "/maintenance"); return;
            }
            chain.doFilter(request, response);
        } catch (Exception exception) {
            throw new ServletException("Could not verify your account", exception);
        }
    }
}
