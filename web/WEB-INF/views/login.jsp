<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/_taglibs.jspf" %>
<c:set var="pageTitle" value="Login | RoomRent" scope="request" />
<c:set var="layoutVariant" value="public" scope="request" />
<%@ include file="/WEB-INF/views/common/_top.jspf" %>

<section class="roomrent-login card roomrent-card" aria-labelledby="login-title">
    <div class="card-body p-4">
        <h1 id="login-title" class="h3 fw-bold text-center mb-4">Login</h1>
        <form action="<c:url value='/login' />" method="post">
            <input type="hidden" name="next" value="${fn:escapeXml(param.next)}">
            <div class="row g-3">
                <div class="col-12">
                    <label class="form-label" for="login-username">Username:</label>
                    <input class="form-control" id="login-username" type="text" name="username"
                           autocomplete="username" required>
                </div>
                <div class="col-12">
                    <label class="form-label" for="login-password">Password:</label>
                    <input class="form-control" id="login-password" type="password" name="password"
                           autocomplete="current-password" required>
                </div>
                <c:if test="${not empty error}">
                    <div class="col-12"><p class="alert alert-danger mb-0" role="alert"><c:out value="${error}" /></p></div>
                </c:if>
                <div class="col-12"><button class="btn btn-primary w-100" type="submit">Login</button></div>
            </div>
        </form>
        <p class="small text-center mt-3 mb-0">Chưa có tài khoản? <a href="<c:url value='/register' />">Đăng ký</a></p>
    </div>
</section>

<%@ include file="/WEB-INF/views/common/_bottom.jspf" %>
