# Finora — Personal Finance Management Platform

Finora is a Java web application for tracking personal expenses, planning budgets, and following financial goals. It provides separate user and administrator areas and stores application data in SQLite through JDBC.

## Features

### User area

- Register and sign in with salted PBKDF2 password hashing.
- Add, edit, delete, categorize, and date-filter expenses.
- Fill expenses from a quick-entry sentence; review details before saving. macOS Dictation can be used in the text field.
- Set weekly, monthly, or yearly expense reminders.
- Create category budgets and track spending and remaining balances.
- Set financial goals with target amounts, saved amounts, deadlines, and progress.
- View monthly summaries, category breakdowns, recent expenses, budgets, and goals.

### Administrator area

- Search and filter accounts; create, edit, disable, delete, and reset passwords.
- Change account roles and active status, with safeguards against disabling the last active administrator.
- Open or close registration, enable maintenance mode, and manage platform features.
- Review account and security activity, failed sign-in reports, and export audit CSV.
- Review finance-data encryption and rotate the data-encryption key.

## Technology

- Java 17 or newer
- Jakarta Servlet 6, JSP, and JSTL
- JDBC with SQLite
- Maven WAR packaging
- Responsive HTML and CSS

## Run locally

You need JDK 17 or newer, Maven 3.9 or newer, and Apache Tomcat 10.1 or newer.

1. Set the first administrator credentials and a persistent database path before starting Tomcat. Use a unique password of at least 12 characters:

```sh
export FINORA_ADMIN_EMAIL="admin@example.com"
export FINORA_ADMIN_PASSWORD="replace-with-a-long-unique-password"
export FINORA_DB_PATH="/absolute/path/to/finora/data/finora.db"
```

Finora provisions the administrator only if that email does not already exist. If the admin variables are missing, visitors can register as regular users, but no administrator account is created.

2. Build the WAR:

```sh
mvn clean package
```

3. Copy target/finora.war to Tomcat's webapps directory and start Tomcat.
4. Open http://localhost:8080/finora/ and create a user account or sign in with the configured administrator.

FINORA_DB_PATH is optional. It defaults to data/finora.db relative to the Tomcat process working directory. Choose a persistent location and back it up. The application creates a keyring beside the database; back up that keyring securely and separately. Do not commit the database, keyring, or real credentials.

## Security notes

Passwords use PBKDF2-HMAC-SHA-256 with a per-account random salt. Finance data uses authenticated AES-256-GCM encryption. Finora also uses parameterized SQL, server-side validation, session ID rotation at login, CSRF tokens, and restrictive response headers.

The local keyring suits a classroom demonstration. Production deployments should use managed key storage, HTTPS, rate limiting, account recovery, and a reviewed privacy policy. If the keyring is lost, encrypted finance records cannot be recovered.