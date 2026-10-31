<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/_taglibs.jspf" %>
<c:set var="pageTitle" value="Tạo tài khoản Quản lý" scope="request" />
<c:set var="activeNav" value="/admin/accounts" scope="request" />
<%@ include file="/WEB-INF/views/common/_top.jspf" %>

<div class="container mt-4">
    <h1 class="h3 fw-bold mb-3">Tạo tài khoản Quản lý (MANAGER)</h1>

    <c:if test="${not empty errorMessage}">
        <div class="alert alert-danger" role="alert">
            <c:out value="${errorMessage}" />
        </div>
    </c:if>

    <form action="<c:url value='/admin/accounts' />" method="post" class="card p-4">
        <input type="hidden" name="action" value="create" />

        <div class="row">
            <div class="col-md-6 mb-3">
                <label class="form-label" for="username">Tên đăng nhập <span class="text-danger">*</span></label>
                <input type="text" class="form-control" id="username" name="username" minlength="4" maxlength="30" required
                       value="<c:out value='${inputUsername}' />" />
                <small class="text-muted">Từ 4–30 ký tự, gồm chữ cái không dấu, số, dấu chấm, gạch dưới hoặc gạch ngang.</small>
            </div>

            <div class="col-md-6 mb-3">
                <label class="form-label" for="password">Mật khẩu <span class="text-danger">*</span></label>
                <input type="password" class="form-control" id="password" name="password" minlength="8" maxlength="1024" required />
                <small class="text-muted">Tối thiểu 8 ký tự. Sẽ được băm bảo mật khi lưu.</small>
            </div>
        </div>

        <div class="row">
            <div class="col-md-6 mb-3">
                <label class="form-label" for="fullName">Họ và tên <span class="text-danger">*</span></label>
                <input type="text" class="form-control" id="fullName" name="fullName" maxlength="100" required
                       value="<c:out value='${inputFullName}' />" />
            </div>

            <div class="col-md-6 mb-3">
                <label class="form-label" for="phone">Số điện thoại</label>
                <input type="text" class="form-control" id="phone" name="phone" maxlength="10"
                       value="<c:out value='${inputPhone}' />" />
                <small class="text-muted">Tùy chọn (nếu nhập: bắt đầu bằng số 0, gồm đúng 10 chữ số).</small>
            </div>
        </div>

        <div class="row">
            <div class="col-md-6 mb-3">
                <label class="form-label" for="email">Email</label>
                <input type="email" class="form-control" id="email" name="email" maxlength="100"
                       value="<c:out value='${inputEmail}' />" />
            </div>

            <div class="col-md-6 mb-3">
                <label class="form-label">Vai trò</label>
                <input type="text" class="form-control bg-light" value="MANAGER (Quản lý)" readonly disabled />
                <small class="text-muted">Vai trò được ấn định cố định bởi hệ thống.</small>
            </div>
        </div>

        <div class="d-flex gap-2 mt-2">
            <button type="submit" class="btn btn-primary">Tạo tài khoản</button>
            <a href="<c:url value='/admin/accounts' />" class="btn btn-outline-secondary">Hủy</a>
        </div>
    </form>
</div>

<%@ include file="/WEB-INF/views/common/_bottom.jspf" %>