<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/_taglibs.jspf" %>
<c:set var="pageTitle" value="Quản lý Loại phòng" scope="request" />
<c:set var="activeNav" value="/admin/room-types" scope="request" />
<%@ include file="/WEB-INF/views/common/_top.jspf" %>

<div class="container mt-4">
    <h1 class="h3 fw-bold mb-3">
        ${not empty roomType.roomTypeId and roomType.roomTypeId > 0 ? 'Cập nhật Loại phòng' : 'Thêm Loại phòng mới'}
    </h1>

    <c:if test="${not empty errorMessage}">
        <div class="alert alert-danger" role="alert">
            <c:out value="${errorMessage}" />
        </div>
    </c:if>

    <form action="<c:url value='/admin/room-types' />" method="post" class="card p-4">
        <input type="hidden" name="action" value="save" />
        <input type="hidden" name="roomTypeId" value="<c:out value='${roomType.roomTypeId}' />" />

        <div class="mb-3">
            <label class="form-label" for="name">Tên loại phòng</label>
            <!-- Nếu có lỗi (inputName != null), điền lại chữ bị lỗi. Nếu ko, điền tên từ DB (roomType.name) -->
            <input type="text" class="form-control" id="name" name="name" maxlength="50" required
                   value="<c:out value='${not empty inputName ? inputName : roomType.name}' />" />
        </div>

        <div class="mb-3">
            <label class="form-label" for="capacity">Sức chứa (1 - 8 người)</label>
            <!-- Tương tự cho Sức chứa -->
            <input type="number" class="form-control" id="capacity" name="capacity" min="1" max="8" required
                   value="<c:out value='${not empty inputCapacity ? inputCapacity : (roomType.capacity > 0 ? roomType.capacity : "")}' />" />
        </div>

        <div class="d-flex gap-2">
            <button type="submit" class="btn btn-primary">Lưu lại</button>
            <a href="<c:url value='/admin/room-types' />" class="btn btn-outline-secondary">Hủy</a>
        </div>
    </form>
</div>

<%@ include file="/WEB-INF/views/common/_bottom.jspf" %>