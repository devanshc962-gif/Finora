<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>Expenses · Finora</title><link rel="stylesheet" href="${pageContext.request.contextPath}/assets/app.css?v=4"></head><body>
<%@ include file="header.jspf" %><main class="page-shell"><%@ include file="flash.jspf" %>
    <section class="page-heading"><div><div class="eyebrow">YOUR MONEY, IN FOCUS</div><h1>Expenses</h1><p class="muted">Record spending and keep a searchable history.</p></div></section>
    <section class="panel form-panel"><div class="panel-heading"><div><div class="eyebrow">NEW RECORD</div><h2>Add an expense</h2></div></div>
        <div class="quick-entry" data-quick-entry>
            <label for="expense-quick-entry">Quick entry</label>
            <div class="quick-entry-row">
                <textarea id="expense-quick-entry" rows="2" maxlength="240" placeholder="e.g. Milk, 36 rupees, food, today" aria-describedby="quick-entry-help quick-entry-status"></textarea>
                <button class="button button-secondary" type="button" data-quick-entry-fill>Fill expense</button>
            </div>
            <p id="quick-entry-help">Try “Milk, food, 70, 09/09/2026” or say “today” / “yesterday.” Dates use DD/MM/YYYY. On Mac, you can use macOS Dictation while this box is focused. Review the filled details before saving.</p>
            <p class="quick-entry-status" id="quick-entry-status" data-quick-entry-status role="status" aria-live="polite"></p>
        </div>
        <form method="post" action="${pageContext.request.contextPath}/app/expenses/create" class="form-grid" data-expense-form>
            <input type="hidden" name="csrfToken" value="${csrfToken}">
            <label>Description<input name="description" maxlength="120" placeholder="e.g. Weekly groceries" required></label>
            <label>Category<select name="category" required><c:forEach var="category" items="${categories}"><option value="${category}"><c:out value="${category}"/></option></c:forEach></select></label>
            <label>Amount (INR)<input name="amount" type="number" min="0.01" step="0.01" placeholder="0.00" required></label>
            <label>Date<input name="spentOn" type="date" value="${today}" required></label>
            <div class="form-action"><button class="button button-primary" type="submit">Save expense</button></div>
        </form>
    </section>
    <section class="panel recurring-panel">
        <div class="panel-heading"><div><div class="eyebrow">PLAN AHEAD</div><h2>Recurring expenses</h2></div><span class="count-pill"><c:out value="${recurringExpenses.size()}"/> reminders</span></div>
        <p class="muted recurring-intro">Set reminders for regular bills. Finora only records an expense when you choose <strong>Record expense</strong>; it never creates one automatically.</p>
        <form method="post" action="${pageContext.request.contextPath}/app/recurring/create" class="form-grid recurring-create-form">
            <input type="hidden" name="csrfToken" value="${csrfToken}">
            <label>Description<input name="description" maxlength="120" placeholder="e.g. Internet bill" required></label>
            <label>Category<select name="category"><c:forEach var="category" items="${categories}"><option value="${category}"><c:out value="${category}"/></option></c:forEach></select></label>
            <label>Amount (INR)<input name="amount" type="number" min="0.01" step="0.01" placeholder="0.00" required></label>
            <label>Repeat<select name="frequency"><option value="WEEKLY">Weekly</option><option value="MONTHLY" selected>Monthly</option><option value="YEARLY">Yearly</option></select></label>
            <label>First due date<input name="nextDueOn" type="date" value="${today}" required></label>
            <div class="form-action"><button class="button button-primary" type="submit">Add reminder</button></div>
        </form>
        <c:choose><c:when test="${empty recurringExpenses}"><div class="empty-state compact"><h3>No recurring reminders yet</h3><p>Add a regular bill above, such as rent, internet, or a subscription.</p></div></c:when><c:otherwise>
            <div class="record-list recurring-list"><c:forEach var="recurring" items="${recurringExpenses}"><article class="record-card recurring-card">
                <div class="recurring-summary"><div><h3><c:out value="${recurring.description}"/></h3><p class="muted"><c:out value="${recurring.category}"/> · <c:out value="${recurring.frequencyLabel}"/> · Next due <c:out value="${recurring.nextDueOn}"/></p>
                    <c:choose><c:when test="${not recurring.active}"><span class="count-pill recurring-paused">Paused</span></c:when><c:when test="${recurring.due}"><span class="count-pill recurring-due">Due now</span></c:when><c:otherwise><span class="count-pill">Upcoming</span></c:otherwise></c:choose>
                </div><strong class="amount"><c:out value="${recurring.amount}"/></strong></div>
                <div class="recurring-actions">
                    <c:choose><c:when test="${recurring.active and recurring.due}"><form method="post" action="${pageContext.request.contextPath}/app/recurring/record"><input type="hidden" name="csrfToken" value="${csrfToken}"><input type="hidden" name="id" value="${recurring.id}"><button class="button button-primary button-small" type="submit">Record expense</button></form></c:when><c:when test="${recurring.active}"><button class="button button-secondary button-small" type="button" disabled>Not due yet</button></c:when><c:otherwise><button class="button button-secondary button-small" type="button" disabled>Paused</button></c:otherwise></c:choose>
                    <form method="post" action="${pageContext.request.contextPath}/app/recurring/${recurring.active ? 'pause' : 'resume'}"><input type="hidden" name="csrfToken" value="${csrfToken}"><input type="hidden" name="id" value="${recurring.id}"><button class="button button-secondary button-small" type="submit"><c:choose><c:when test="${recurring.active}">Pause</c:when><c:otherwise>Resume</c:otherwise></c:choose></button></form>
                    <form method="post" action="${pageContext.request.contextPath}/app/recurring/delete" data-confirm="Delete this reminder? Previously recorded expenses will be kept."><input type="hidden" name="csrfToken" value="${csrfToken}"><input type="hidden" name="id" value="${recurring.id}"><button class="button-danger-text" type="submit">Delete reminder</button></form>
                </div>
            </article></c:forEach></div>
        </c:otherwise></c:choose>
    </section>
    <section class="panel"><div class="panel-heading"><div><div class="eyebrow">HISTORY</div><h2>Tracked expenses</h2></div><span class="count-pill"><c:out value="${expenses.size()}"/> records · <c:out value="${filteredExpenseTotal}"/></span></div>
        <form method="get" class="filter-row"><label>Category<select name="category"><option value="">All categories</option><c:forEach var="category" items="${categories}"><option value="${category}" ${selectedCategory eq category ? 'selected' : ''}><c:out value="${category}"/></option></c:forEach></select></label><label>From<input type="date" name="from" value="${from}"></label><label>To<input type="date" name="to" value="${to}"></label><button class="button button-secondary" type="submit">Filter</button><a class="text-link" href="${pageContext.request.contextPath}/app/expenses">Clear</a></form>
        <div class="expense-search-row"><label>Search expenses<input type="search" data-expense-search placeholder="Search description, category, or date" autocomplete="off" aria-describedby="expense-search-count"></label><span id="expense-search-count" data-expense-count aria-live="polite"><c:out value="${expenses.size()}"/> shown</span></div>
        <c:choose><c:when test="${empty expenses}"><div class="empty-state"><span class="empty-icon">↗</span><h3>No matching expenses</h3><p>Try changing the filters or add a new record above.</p></div></c:when><c:otherwise>
            <div class="record-list" data-expense-list><c:forEach var="expense" items="${expenses}"><article class="record-card" data-expense-row data-expense-search="<c:out value='${expense.description}'/> <c:out value='${expense.category}'/> <c:out value='${expense.amount}'/> <c:out value='${expense.spentOn}'/>"><form method="post" action="${pageContext.request.contextPath}/app/expenses/update" class="record-form">
                <input type="hidden" name="csrfToken" value="${csrfToken}"><input type="hidden" name="id" value="${expense.id}">
                <label>Description<input name="description" value="<c:out value='${expense.description}'/>" maxlength="120" required></label>
                <label>Category<select name="category"><c:forEach var="category" items="${categories}"><option value="${category}" ${expense.category eq category ? 'selected' : ''}><c:out value="${category}"/></option></c:forEach></select></label>
                <label>Amount<input name="amount" type="number" min="0.01" step="0.01" value="${expense.amountCents / 100.0}" required></label>
                <label>Date<input name="spentOn" type="date" value="${expense.spentOn}" required></label>
                <div class="record-actions"><button class="button button-secondary button-small" type="submit">Save</button></div>
            </form><form method="post" action="${pageContext.request.contextPath}/app/expenses/delete" class="delete-form" data-confirm="Delete this expense? This action cannot be undone."><input type="hidden" name="csrfToken" value="${csrfToken}"><input type="hidden" name="id" value="${expense.id}"><button class="button-danger-text" type="submit" aria-label="Delete expense">Delete</button></form></article></c:forEach></div><div class="empty-state compact search-empty" data-expense-no-results hidden><h3>No matches</h3><p>Try another description, category, or date.</p></div>
        </c:otherwise></c:choose>
    </section><footer class="site-footer">Finora · Your money, in focus</footer>
</main><script src="${pageContext.request.contextPath}/assets/quick-entry.js?v=2" defer></script></body></html>
