<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>Overview · Finora</title><link rel="stylesheet" href="${pageContext.request.contextPath}/assets/app.css?v=4"></head><body>
<%@ include file="header.jspf" %><main class="page-shell"><%@ include file="flash.jspf" %>
    <section class="page-heading"><div><div class="eyebrow">PERSONAL FINANCE</div><h1>Your overview</h1><p class="muted">A clear view of what you spent and what you’re working toward.</p></div><a class="button button-primary" href="${pageContext.request.contextPath}/app/expenses">Add expense <span aria-hidden="true">＋</span></a></section>
    <section class="summary-grid">
        <a class="summary-card summary-highlight summary-card-link" href="${pageContext.request.contextPath}/app/expenses"><div class="summary-label">Spent this month</div><div class="summary-value"><c:out value="${monthTotal}"/></div><div class="summary-foot">Open your expenses →</div></a>
        <article class="summary-card"><div class="summary-label">Budgets</div><div class="summary-value"><c:out value="${budgetCount}"/></div><div class="summary-foot">Keep spending within your plan</div></article>
        <article class="summary-card"><div class="summary-label">Financial goals</div><div class="summary-value"><c:out value="${goalCount}"/></div><div class="summary-foot">Small steps add up</div></article>
    </section>
    <div class="content-grid">
        <section class="panel"><div class="panel-heading"><div><div class="eyebrow">LAST 30 DAYS</div><h2>Recent expenses</h2></div><a class="text-link" href="${pageContext.request.contextPath}/app/expenses">View all →</a></div>
            <c:choose><c:when test="${empty recentExpenses}"><div class="empty-state"><span class="empty-icon">↗</span><h3>No expenses yet</h3><p>Add your first expense to start seeing patterns.</p><a class="text-link" href="${pageContext.request.contextPath}/app/expenses">Track an expense →</a></div></c:when>
                <c:otherwise><div class="expense-list"><c:forEach var="expense" items="${recentExpenses}"><div class="expense-row"><div class="category-icon"><c:out value="${expense.category.substring(0,1)}"/></div><div class="expense-main"><strong><c:out value="${expense.description}"/></strong><span><c:out value="${expense.category}"/> · <c:out value="${expense.spentOn}"/></span></div><strong class="amount"><c:out value="${expense.amount}"/></strong></div></c:forEach></div></c:otherwise></c:choose>
        </section>
        <c:if test="${reportsEnabled}"><section class="panel"><div class="panel-heading"><div><div class="eyebrow">THIS MONTH</div><h2>Spending by category</h2></div></div>
            <c:choose><c:when test="${empty categoryRows}"><div class="empty-state compact"><p>Your category breakdown will appear here.</p></div></c:when><c:otherwise><div class="chart-list"><c:forEach var="row" items="${categoryRows}"><c:url var="categoryExpensesUrl" value="/app/expenses" context="${pageContext.request.contextPath}"><c:param name="category" value="${row.category}"/></c:url><a class="chart-item chart-link" href="${categoryExpensesUrl}" aria-label="View ${row.category} expenses"><div class="chart-meta"><span><c:out value="${row.category}"/></span><strong><c:out value="${row.amount}"/></strong></div><div class="bar-track"><span class="bar-fill" style="width:${row.width}%"></span></div></a></c:forEach></div></c:otherwise></c:choose>
        </section></c:if>
    </div>
    <div class="content-grid lower-grid">
        <c:if test="${budgetsEnabled}">
        <section class="panel"><div class="panel-heading"><div><div class="eyebrow">SPENDING PLAN</div><h2>Budgets</h2></div><a class="text-link" href="${pageContext.request.contextPath}/app/budgets">Manage →</a></div>
            <c:choose><c:when test="${empty budgetRows}"><div class="empty-state compact"><p>No budgets yet. Set a limit for a category and date range.</p><a class="text-link" href="${pageContext.request.contextPath}/app/budgets">Create a budget →</a></div></c:when><c:otherwise><c:forEach var="budget" items="${budgetRows}"><div class="progress-item"><div class="progress-meta"><strong><c:out value="${budget.category}"/></strong><span><c:out value="${budget.spent}"/> of <c:out value="${budget.limit}"/></span></div><div class="bar-track"><span class="bar-fill ${budget.progressPercent ge 90 ? 'bar-warning' : ''}" style="width:${budget.progressPercent}%"></span></div><div class="progress-foot"><span><c:out value="${budget.progressPercent}"/>% used</span><span><c:out value="${budget.remaining}"/> left</span></div></div></c:forEach></c:otherwise></c:choose>
        </section>
        </c:if>
        <c:if test="${goalsEnabled}">
        <section class="panel"><div class="panel-heading"><div><div class="eyebrow">FUTURE YOU</div><h2>Financial goals</h2></div><a class="text-link" href="${pageContext.request.contextPath}/app/goals">Manage →</a></div>
            <c:choose><c:when test="${empty goalRows}"><div class="empty-state compact"><p>Choose something you’re saving toward and track your progress.</p><a class="text-link" href="${pageContext.request.contextPath}/app/goals">Set a goal →</a></div></c:when><c:otherwise><c:forEach var="goal" items="${goalRows}"><div class="progress-item"><div class="progress-meta"><strong><c:out value="${goal.title}"/></strong><span><c:out value="${goal.saved}"/> of <c:out value="${goal.target}"/></span></div><div class="bar-track"><span class="bar-fill bar-green" style="width:${goal.progressPercent}%"></span></div><div class="progress-foot"><span><c:out value="${goal.progressPercent}"/>% complete</span><span>Due <c:out value="${goal.deadline}"/></span></div></div></c:forEach></c:otherwise></c:choose>
        </section>
        </c:if>
    </div>
    <footer class="site-footer">Finora · Your money, in focus</footer>
</main></body></html>
