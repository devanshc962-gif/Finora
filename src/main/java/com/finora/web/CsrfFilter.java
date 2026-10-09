package com.finora.web;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@WebFilter("/*")
public class CsrfFilter implements Filter {
    private final SecureRandom random = new SecureRandom();
    @Override public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        HttpSession session = httpRequest.getSession(true);
        String token = (String) session.getAttribute("csrfToken");
        if (token == null) {
            byte[] bytes = new byte[32]; random.nextBytes(bytes); token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            session.setAttribute("csrfToken", token);
        }
        httpRequest.setAttribute("csrfToken", token);
        if ("POST".equalsIgnoreCase(httpRequest.getMethod())) {
            String submitted = httpRequest.getParameter("csrfToken");
            if (submitted == null || !MessageDigest.isEqual(token.getBytes(java.nio.charset.StandardCharsets.UTF_8), submitted.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
                httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "The form expired. Reload the page and try again.");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
