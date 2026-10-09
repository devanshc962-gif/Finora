<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>Budgets · Finora</title><link rel="stylesheet" href="${pageContext.request.contextPath}/assets/app.css?v=4"></head><body>
<%@ include file="header.jspf" %><main class="page-shell"><%@ include file="flash.jspf" %>
    <section class="page-heading"><div><div class="eyebrow">MAKE A PLAN</div><h1>Budgets</h1><p class="muted">Set a limit for a category and see how much room remains. Only expenses dated within a budget’s start and end dates count toward it.</p></div></section>
    <section class="panel form-panel"><div class="panel-heading"><div><div class="eyebrow">NEW PLAN</div><h2>Create a budget</h2></div></div>
        <form method="post" action="${pageContext.request.contextPath}/app/budgets/create" class="form-grid">
            <input type="hidden" name="csrfToken" value="${csrfToken}">
            <label>Category<select name="category"><c:forEach var="category" items="${categories}"><option value="${category}"><c:out value="${category}"/></option></c:forEach></select></label>
            <label>Limit (INR)<input name="limit" type="number" min="0.01" step="0.01" placeholder="0.00" required></label>
            <label>Starts<input name="startsOn" type="date" value="${budgetStart}" required></label>
            <label>Ends<input name="endsOn" type="date" value="${budgetEnd}" required></label>
            <div class="form-action"><button class="button button-primary" type="submit">Create budget</button></div>
        </form>
    </section>
    <section class="panel"><div class="panel-heading"><div><div class="eyebrow">YOUR PLAN</div><h2>Budget overview</h2></div><span class="count-pill"><c:out value="${budgets.size()}"/> budgets</span></div>
        <c:choose><c:when test="${empty budgets}"><div class="empty-state"><span class="empty-icon">◷</span><h3>No budgets yet</h3><p>Create a budget to track category spending over a date range.</p></div></c:when><c:otherwise><div class="record-list"><c:forEach var="budget" items="${budgets}"><article class="record-card stacked-card"><div class="budget-summary"><div><div class="eyebrow"><c:out value="${budget.category}"/></div><h3><c:out value="${budget.spent}"/> <span class="muted">of</span> <c:out value="${budget.limit}"/></h3><p class="muted"><c:out value="${budget.startsOn}"/> — <c:out value="${budget.endsOn}"/></p></div><span class="budget-percent"><c:out value="${budget.progressPercent}"/>%</span></div>
                <div class="bar-track"><span class="bar-fill ${budget.progressPercent ge 90 ? 'bar-warning' : ''}" style="width:${budget.progressPercent}%"></span></div><div class="progress-foot"><span>Spent <c:out value="${budget.spent}"/></span><span><c:out value="${budget.remaining}"/> remaining</span></div>
                <form id="budget-update-${budget.id}" method="post" action="${pageContext.request.contextPath}/app/budgets/update" class="record-form record-edit-form"><input type="hidden" name="csrfToken" value="${csrfToken}"><input type="hidden" name="id" value="${budget.id}">
                    <label>Category<select name="category"><c:forEach var="category" items="${categories}"><option value="${category}" ${budget.category eq category ? 'selected' : ''}><c:out value="${category}"/></option></c:forEach></select></label><label>Limit<input name="limit" type="number" min="0.01" step="0.01" value="${budget.limitCents / 100.0}" required></label><label>Starts<input name="startsOn" type="date" value="${budget.startsOn}" required></label><label>Ends<input name="endsOn" type="date" value="${budget.endsOn}" required></label>
                </form><div class="record-action-row"><button class="button button-secondary button-small" type="submit" form="budget-update-${budget.id}">Save</button><form method="post" action="${pageContext.request.contextPath}/app/budgets/delete" class="delete-form" data-confirm="Delete this budget? This action cannot be undone."><input type="hidden" name="csrfToken" value="${csrfToken}"><input type="hidden" name="id" value="${budget.id}"><button class="button-danger-text" type="submit">Delete budget</button></form></div>
            </article></c:forEach></div></c:otherwise></c:choose>
    </section><footer class="site-footer">Finora · Your money, in focus</footer>
</main></body></html>
