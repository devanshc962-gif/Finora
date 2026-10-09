# Finora — Personal Finance Management Platform

Finora is a Java web application for tracking personal expenses, planning category budgets, and following financial goals. It includes separate user and administrator areas and uses JDBC with a local SQLite database.

## Features

### User area

- Register and sign in with salted PBKDF2 password hashing.
- Create, edit, delete, categorize, and date-filter expenses.
- Use quick entry to fill an expense from a sentence; review the details before saving. On Mac, the text box supports macOS Dictation.
- Schedule weekly, monthly, or yearly expense reminders.
- Create category budgets and track spending and remaining balances.
- Create financial goals with targets, saved amounts, deadlines, and progress.
- View monthly spending summaries, category breakdowns, recent expenses, budgets, and goals.

### Administrator area

- View, search, filter, create, edit, disable, and delete user accounts; manage roles and reset passwords.
- Prevent an administrator from disabling their own access or removing the last active administrator.
- Open or close registration, enable maintenance mode, and manage platform features.
- Review account and security activity, failed sign-in reports, and export audit CSV.
- Review finance-data encryption status and rotate the data-encryption key.

## Technology

- Java 17+
- Jakarta Servlet 6 and JSP/JSTL
- JDBC with SQLite
- Maven WAR packaging
- Responsive HTML and CSS

## Run locally

You need JDK 17 or newer, Maven 3.9 or newer, and Apache Tomcat 10.1 or newer.

1. Set the first administrator credentials and a persistent database path before starting Tomcat. Use a unique password of at least 12 characters:

   export FINORA_ADMIN_EMAIL="admin@example.com"
   export FINORA_ADMIN_PASSWORD="replace-with-a-long-unique-password"
   export FINORA_DB_PATH="/absolute/path/to/finora/data/finora.db"

2. Build the WAR with Maven: mvn clean package
3. Copy target/finora.war to Tomcat's webapps directory and start Tomcat.
4. Open http://localhost:8080/finora/ and create a user account or sign in as the configured administrator.

FINORA_DB_PATH is optional and defaults to data/finora.db relative to the Tomcat process working directory. Choose a persistent path and back it up. Do not commit database files or real credentials. Finora creates a keyring beside the database; back it up securely and separately. If the keyring is lost, encrypted finance records cannot be recovered.

## Security notes

Passwords use PBKDF2-HMAC-SHA-256 with a per-account salt. Finance data uses authenticated AES-256-GCM encryption. The project also uses parameterized SQL, server-side validation, session ID rotation at login, CSRF tokens, and restrictive response headers. The local keyring suits a classroom demonstration; production deployments should use managed key storage, HTTPS, rate limiting, account recovery, and a reviewed privacy policy.

## Project overview

The project overview deck is available at deliverables/Finora_Project_Overview_v2.pptx.
