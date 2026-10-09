<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>Goals · Finora</title><link rel="stylesheet" href="${pageContext.request.contextPath}/assets/app.css?v=4"></head><body>
<%@ include file="header.jspf" %><main class="page-shell"><%@ include file="flash.jspf" %>
    <section class="page-heading"><div><div class="eyebrow">PLAN FOR WHAT MATTERS</div><h1>Financial goals</h1><p class="muted">Choose a target, update your progress, and keep moving.</p></div></section>
    <section class="panel form-panel"><div class="panel-heading"><div><div class="eyebrow">NEW MILESTONE</div><h2>Set a goal</h2></div></div>
        <form method="post" action="${pageContext.request.contextPath}/app/goals/create" class="form-grid">
            <input type="hidden" name="csrfToken" value="${csrfToken}">
            <label>Goal name<input name="title" maxlength="100" placeholder="e.g. Emergency fund" required></label>
            <label>Target (INR)<input name="target" type="number" min="0.01" step="0.01" placeholder="0.00" required></label>
            <label>Already saved (INR)<input name="saved" type="number" min="0" step="0.01" value="0.00"></label>
            <label>Deadline<input name="deadline" type="date" required></label>
            <div class="form-action"><button class="button button-primary" type="submit">Add goal</button></div>
        </form>
    </section>
    <section class="panel"><div class="panel-heading"><div><div class="eyebrow">YOUR FUTURE</div><h2>Goal progress</h2></div><span class="count-pill"><c:out value="${goals.size()}"/> goals</span></div>
        <c:choose><c:when test="${empty goals}"><div class="empty-state"><span class="empty-icon">✳</span><h3>No goals yet</h3><p>Add a goal to make your progress visible.</p></div></c:when><c:otherwise><div class="record-list"><c:forEach var="goal" items="${goals}"><article class="record-card stacked-card"><div class="budget-summary"><div><div class="eyebrow"><c:choose><c:when test="${goal.completed}">GOAL ACHIEVED</c:when><c:otherwise>TARGET BY <c:out value="${goal.deadline}"/></c:otherwise></c:choose></div><h3><c:out value="${goal.title}"/></h3><p class="muted"><c:out value="${goal.saved}"/> saved of <c:out value="${goal.target}"/></p></div><span class="budget-percent"><c:out value="${goal.progressPercent}"/>%</span></div>
                <div class="bar-track"><span class="bar-fill bar-green" style="width:${goal.progressPercent}%"></span></div>
                <form id="goal-update-${goal.id}" method="post" action="${pageContext.request.contextPath}/app/goals/update" class="record-form record-edit-form"><input type="hidden" name="csrfToken" value="${csrfToken}"><input type="hidden" name="id" value="${goal.id}">
                    <label>Goal name<input name="title" value="<c:out value='${goal.title}'/>" maxlength="100" required></label><label>Target<input name="target" type="number" min="0.01" step="0.01" value="${goal.targetCents / 100.0}" required></label><label>Saved so far<input name="saved" type="number" min="0" step="0.01" value="${goal.savedCents / 100.0}" required></label><label>Deadline<input name="deadline" type="date" value="${goal.deadline}" required></label>
                </form><div class="record-action-row"><button class="button button-secondary button-small" type="submit" form="goal-update-${goal.id}">Update progress</button><form method="post" action="${pageContext.request.contextPath}/app/goals/delete" class="delete-form" data-confirm="Delete this goal? This action cannot be undone."><input type="hidden" name="csrfToken" value="${csrfToken}"><input type="hidden" name="id" value="${goal.id}"><button class="button-danger-text" type="submit">Delete goal</button></form></div>
            </article></c:forEach></div></c:otherwise></c:choose>
    </section><footer class="site-footer">Finora · Your money, in focus</footer>
</main></body></html>
