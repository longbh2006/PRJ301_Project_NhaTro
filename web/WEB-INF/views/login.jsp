<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/_taglibs.jspf" %>
<c:set var="pageTitle" value="RoomRent | Đăng nhập" scope="request" />
<c:set var="layoutVariant" value="public" scope="request" />
<%@ include file="/WEB-INF/views/common/_top.jspf" %>

<section class="roomrent-login card roomrent-card" aria-labelledby="login-title">
    <div class="card-body p-4">
        <h1 id="login-title" class="h3 fw-bold text-center mb-1">Đăng nhập</h1>
        <p class="small text-secondary text-center mb-4">Chào mừng bạn đến với hệ thống nhà trọ RoomRent</p>

        <c:if test="${not empty error}">
            <div class="alert alert-danger" role="alert"><c:out value="${error}" /></div>
        </c:if>

        <form action="<c:url value='/login' />" method="post">
            <%-- Giữ đường dẫn quay lại sau khi đăng nhập --%>
            <input type="hidden" name="next" value="${fn:escapeXml(param.next)}">
            <div class="mb-3">
                <label class="form-label" for="username">Tên đăng nhập</label>
                <%-- Đăng nhập sai thì giữ lại tên đăng nhập, không bao giờ giữ mật khẩu --%>
                <input class="form-control" id="username" type="text" name="username"
                       placeholder="Nhập tên đăng nhập" value="${fn:escapeXml(param.username)}"
                       autocomplete="username" required>
            </div>
            <div class="mb-4">
                <label class="form-label" for="password">Mật khẩu</label>
                <input class="form-control" id="password" type="password" name="password"
                       placeholder="Nhập mật khẩu" autocomplete="current-password" required>
            </div>
            <button class="btn btn-primary w-100" type="submit">Đăng nhập</button>
        </form>

        <p class="small text-center mt-3 mb-0">
            Chưa có tài khoản? <a href="<c:url value='/register' />">Đăng ký ngay</a>
        </p>
    </div>
</section>

<%@ include file="/WEB-INF/views/common/_bottom.jspf" %>
