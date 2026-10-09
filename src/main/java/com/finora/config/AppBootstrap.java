package com.finora.config;

import com.finora.dao.UserDao;
import com.finora.dao.AuditDao;
import com.finora.security.FinanceCipher;
import com.finora.security.FinanceEncryptionMigrator;
import com.finora.security.PasswordHasher;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

/** Initializes persistence and optionally provisions the first administrator from environment variables. */
@WebListener
public class AppBootstrap implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent event) {
        try {
            Database database = new Database();
            FinanceCipher cipher = new FinanceCipher(database);
            new FinanceEncryptionMigrator(database, cipher).migrateAndRekey();
            event.getServletContext().setAttribute("database", database);
            event.getServletContext().setAttribute("financeCipher", cipher);
            String adminEmail = System.getenv("FINORA_ADMIN_EMAIL");
            String adminPassword = System.getenv("FINORA_ADMIN_PASSWORD");
            if (adminEmail != null && !adminEmail.isBlank() && adminPassword != null && adminPassword.length() >= 12) {
                new UserDao(database).createInitialAdmin("Finora Admin", adminEmail.trim(), PasswordHasher.hash(adminPassword));
            }
            new AuditDao(database).record(null, "SYSTEM_STARTED", "Finora application started");
        } catch (Exception exception) {
            throw new IllegalStateException("Finora could not initialize its database", exception);
        }
    }
}
