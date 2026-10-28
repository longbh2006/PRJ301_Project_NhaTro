<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/_taglibs.jspf" %>
<c:set var="pageTitle" value="RoomRent | Trang thử giao diện" scope="request" />
<%@ include file="/WEB-INF/views/common/_top.jspf" %>

<div class="mb-4">
    <h1 class="h3 fw-bold mb-2">Trang thử giao diện</h1>
    <p class="text-secondary mb-0">Trang mẫu sử dụng đủ ba mảnh giao diện dùng chung của RoomRent.</p>
</div>

<div class="alert alert-success" role="status">
    <c:out value="${requestScope.demoMessage}" />
</div>

<section class="card roomrent-card mb-4" aria-labelledby="demo-role-title">
    <div class="card-body p-4">
        <h2 id="demo-role-title" class="h5 fw-bold">Menu theo vai trò</h2>
        <p class="mb-0">Vai trò hiện tại: <strong><c:out value="${layoutRoleLabel}" /></strong>.
            Menu chung tự thay đổi theo vai trò trong session sau khi đăng nhập.</p>
    </div>
</section>

<div class="row g-3">
    <div class="col-md-4">
        <section class="card roomrent-card h-100" aria-labelledby="demo-taglibs-title">
            <div class="card-body p-4">
                <h2 id="demo-taglibs-title" class="h6 fw-bold">1. Khai báo taglib</h2>
                <p class="small text-secondary mb-0"><code>_taglibs.jspf</code> cung cấp JSTL
                    <code>c</code>, <code>fmt</code>, <code>fn</code> và tag nhóm <code>rr</code>.</p>
            </div>
        </section>
    </div>
    <div class="col-md-4">
        <section class="card roomrent-card h-100" aria-labelledby="demo-top-title">
            <div class="card-body p-4">
                <h2 id="demo-top-title" class="h6 fw-bold">2. Phần đầu trang</h2>
                <p class="small text-secondary mb-0"><code>_top.jspf</code> cung cấp Bootstrap,
                    CSS, header và menu theo vai trò.</p>
            </div>
        </section>
    </div>
    <div class="col-md-4">
        <section class="card roomrent-card h-100" aria-labelledby="demo-bottom-title">
            <div class="card-body p-4">
                <h2 id="demo-bottom-title" class="h6 fw-bold">3. Phần cuối trang</h2>
                <p class="small text-secondary mb-0"><code>_bottom.jspf</code> cung cấp footer
                    và script Bootstrap dùng chung.</p>
            </div>
        </section>
    </div>
</div>

<%@ include file="/WEB-INF/views/common/_bottom.jspf" %>
