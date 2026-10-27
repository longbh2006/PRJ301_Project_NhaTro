<%@ page isErrorPage="true" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/_taglibs.jspf" %>
<c:set var="pageTitle" value="RoomRent | 403 - Không có quyền truy cập" scope="request" />
<c:remove var="activeNav" scope="request" />
<%@ include file="/WEB-INF/views/common/_top.jspf" %>
<section class="card roomrent-card roomrent-error text-center" aria-labelledby="error-title">
    <div class="card-body p-4 p-md-5">
        <p class="roomrent-error-code text-primary">403</p>
        <h1 id="error-title" class="h3 fw-bold">Không có quyền truy cập</h1>
        <p class="text-secondary">Tài khoản của bạn không có quyền truy cập trang này.</p>
        <a class="btn btn-primary" href="<c:url value='/' />">Về trang chủ</a>
    </div>
</section>
<%@ include file="/WEB-INF/views/common/_bottom.jspf" %>
