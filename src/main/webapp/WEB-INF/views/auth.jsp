<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title><c:choose><c:when test="${registerMode}">Create account</c:when><c:otherwise>Sign in</c:otherwise></c:choose> · Finora</title><link rel="stylesheet" href="${pageContext.request.contextPath}/assets/app.css?v=4"></head>
<body class="auth-page"><main class="auth-shell"><a class="brand auth-brand" href="${pageContext.request.contextPath}/login"><span class="brand-mark">f</span><span>finora</span></a>
    <section class="auth-card"><div class="eyebrow">YOUR MONEY, IN FOCUS</div><h1><c:choose><c:when test="${registerMode}">Create your account</c:when><c:otherwise>Welcome back</c:otherwise></c:choose></h1>
        <p class="muted"><c:choose><c:when test="${registerMode}">A calmer way to track spending and make progress.</c:when><c:otherwise>Sign in to see your financial picture.</c:otherwise></c:choose></p>
        <%@ include file="flash.jspf" %>
        <c:if test="${not empty error}"><div class="notice notice-error" role="alert"><c:out value="${error}"/></div></c:if>
        <form method="post" action="${pageContext.request.contextPath}${registerMode ? '/register' : '/login'}" class="form-stack">
            <input type="hidden" name="csrfToken" value="${csrfToken}">
            <c:if test="${registerMode}"><label>Full name<input name="name" type="text" maxlength="80" autocomplete="name" required></label></c:if>
            <label>Email address<input name="email" type="email" maxlength="160" autocomplete="email" required></label>
            <label>Password<input name="password" type="password" minlength="8" maxlength="200" autocomplete="${registerMode ? 'new-password' : 'current-password'}" required><small>Use at least 8 characters.</small></label>
            <button class="button button-primary button-wide" type="submit"><c:choose><c:when test="${registerMode}">Create account</c:when><c:otherwise>Sign in</c:otherwise></c:choose><span aria-hidden="true">→</span></button>
        </form>
        <c:choose><c:when test="${registerMode}"><p class="auth-switch">Already have an account? <a href="${pageContext.request.contextPath}/login">Sign in</a></p></c:when>
            <c:when test="${registrationEnabled}"><p class="auth-switch">New to Finora? <a href="${pageContext.request.contextPath}/register">Create an account</a></p></c:when></c:choose>
    </section><p class="auth-foot">Your financial data belongs to you.</p></main></body></html>
