<%@ page isErrorPage="true" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/_taglibs.jspf" %>
<c:set var="pageTitle" value="RoomRent | 404 - Không tìm thấy trang" scope="request" />
<c:remove var="activeNav" scope="request" />
<%@ include file="/WEB-INF/views/common/_top.jspf" %>
<section class="card roomrent-card roomrent-error text-center" aria-labelledby="error-title">
    <div class="card-body p-4 p-md-5">
        <p class="roomrent-error-code text-primary">404</p>
        <h1 id="error-title" class="h3 fw-bold">Không tìm thấy trang</h1>
        <p class="text-secondary">Trang bạn tìm không tồn tại hoặc đã được di chuyển.</p>
        <a class="btn btn-primary" href="<c:url value='/' />">Về trang chủ</a>
    </div>
</section>
<%@ include file="/WEB-INF/views/common/_bottom.jspf" %>
