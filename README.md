# Finora — Personal Finance Management Platform

Finora is a Java web application for tracking personal expenses, planning category budgets, and following financial goals. It includes separate user and administrator areas and uses JDBC with a local SQLite database.

## Features

### User area

- Register and sign in with a password stored as a salted PBKDF2 hash.
- Create, edit, delete, categorize, and date-filter expenses.
- Use quick entry to turn a short sentence into an expense description, amount, category, and today/yesterday date; review the form and save it manually. On Mac, the text box also works with macOS Dictation, avoiding browser speech-recognition and language-pack requirements.
- Schedule weekly, monthly, or yearly expense reminders. Record each due occurrence manually; pausing or deleting a reminder never removes expenses already recorded.
- Create and manage category budgets over a chosen date range, with spending and remaining-balance progress.
- Create and update financial goals with a target, saved amount, deadline, and progress indicator.
- View a monthly spending summary, category breakdown, recent expenses, budgets, and goals.
- Filter expense history by date and category and see the total for the filtered results.

### Administrator area

- View user accounts and change account roles or active status.
- Search and filter accounts; create, edit, disable, reset passwords, and permanently delete accounts with their finance records.
- Prevent an administrator from disabling their own access or removing the last active administrator.
- Open or close registration, enable maintenance mode, and manage budget, goals, and report features.
- Review and filter security/account activity, view failed sign-in reports, and export an audit CSV.
- Review AES-GCM finance-data encryption status and rotate the data-encryption key.

## Technology

- Java 17+
- Jakarta Servlet 6 and JSP/JSTL
- JDBC with SQLite
- Maven WAR packaging
- Responsive HTML and CSS

The code is separated into web controllers, data access objects, models, configuration, and security helpers. Servlet request methods use overrides; DAOs use generic collections; JDBC statements are parameterized; and request validation raises clear exceptions before persistence.

## Run locally

You need JDK 17 or newer, Maven 3.9 or newer, and Apache Tomcat 10.1 or newer. The current workspace has Java 21 and Maven; the project packages as a WAR.

1. Set the first administrator credentials before starting Tomcat. Use a unique password of at least 12 characters:

   ```sh
   export FINORA_ADMIN_EMAIL="admin@example.com"
   export FINORA_ADMIN_PASSWORD="replace-with-a-long-unique-password"
   export FINORA_DB_PATH="/absolute/path/to/finora/data/finora.db"
   ```

   Finora creates the administrator only if the configured email does not already exist. If the admin variables are missing, visitors can register as regular users, but no admin account is provisioned.

2. Build the WAR:

   ```sh
   mvn clean package
   ```

3. Copy `target/finora.war` to Tomcat's `webapps` directory and start Tomcat. Open `http://localhost:8080/finora/`.

4. Create a user account from the sign-in screen, or sign in with the administrator account configured above.

`FINORA_DB_PATH` is optional; it defaults to `data/finora.db` relative to the Tomcat process working directory. Choose a persistent path and back it up. Do not commit the database file or real credentials.

Finora creates `finora.keyring` beside the database on first startup. Back up that file securely. Existing plaintext finance records in an earlier Finora database are encrypted automatically on startup.

## Data model

- `users`: account identity, salted password hash, role, and active status.
- `expenses`: owner, description, category, amount in paise, and date.
- `recurring_expenses`: owner and active status, with the schedule and financial details stored in an encrypted payload.
- `budgets`: owner, category, limit in paise, and date range.
- `goals`: owner, title, target and saved amounts in paise, and deadline.
- `audit_logs`: actor, event, non-financial details, and timestamp.
- `app_settings`: administrator-managed registration preference.

Amounts are represented as integer paise inside the application to avoid floating-point arithmetic. User finance details are stored in authenticated AES-256-GCM encrypted payloads; legacy columns are scrubbed to placeholders. DAO reports decrypt owner-scoped rows in the application before calculating totals. Admin actions are restricted by role checks.

## Security and deployment notes

- Passwords use PBKDF2-HMAC-SHA-256 with a per-account random salt.
- Finance data uses AES-256-GCM. The key ring is stored outside the web root next to the SQLite file by default (`finora.keyring`); set `FINORA_KEY_PATH` to move it elsewhere.
- Prepared statements, server-side input checks, session ID rotation at login, CSRF tokens, and restrictive response headers are used.
- Back up the key ring securely and separately from the database. If it is lost, encrypted finance records cannot be recovered. Admins can rotate keys after backing up the current key ring; old keys remain available to recover from an interrupted rotation.
- The local key ring suits a classroom demonstration. For production, use managed key storage, HTTPS, rate limiting, account recovery, and a reviewed privacy policy.

## Rubric alignment

| Web-project rubric | Finora evidence |
|---|---|
| Problem understanding and solution design — 8 | User/admin roles, finance workflows, schema, and dashboard reporting |
| Core Java concepts — 10 | Servlet inheritance/overrides, model classes, generic collections, validation, exceptions, and layered DAOs |
| Database integration (JDBC) — 8 | SQLite schema, prepared statements, CRUD operations, grouping, and budget aggregation |
| Servlets and web integration — 7 | Registration, login, sessions, route handlers, JSP views, filters, forms, and role checks |

The assignment screenshots also include a Java GUI rubric. Finora is web-based, so the web rubric is the direct match; confirm the intended rubric with the evaluator if the project category is uncertain.

## Suggested presentation flow

1. Problem and intended users
2. User/admin capabilities and scope
3. Architecture and request flow
4. Database entities and ownership rules
5. Expense, budget, and goal screens
6. Security decisions and known limitations
7. Rubric mapping and next steps

The project overview deck is available at [`deliverables/Finora_Project_Overview_v2.pptx`](deliverables/Finora_Project_Overview_v2.pptx).
