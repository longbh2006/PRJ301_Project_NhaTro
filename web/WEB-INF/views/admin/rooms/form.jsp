<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/_taglibs.jspf" %>
<c:set var="pageTitle" value="Quản lý Phòng" scope="request" />
<c:set var="activeNav" value="/admin/rooms" scope="request" />
<%@ include file="/WEB-INF/views/common/_top.jspf" %>

<div class="container mt-4">
    <h1 class="h3 fw-bold mb-3">
        ${not empty room.roomId and room.roomId > 0 ? 'Cập nhật Phòng' : 'Thêm Phòng mới'}
    </h1>

    <c:if test="${not empty errorMessage}">
        <div class="alert alert-danger" role="alert">
            <c:out value="${errorMessage}" />
        </div>
    </c:if>

    <form action="<c:url value='/admin/rooms' />" method="post" class="card p-4">
        <input type="hidden" name="action" value="save" />
        <input type="hidden" name="roomId" value="<c:out value='${room.roomId}' />" />

        <div class="row">
            <div class="col-md-6 mb-3">
                <label class="form-label" for="roomCode">Mã phòng</label>
                <input type="text" class="form-control" id="roomCode" name="roomCode" maxlength="10" required
                       value="<c:out value='${room.roomCode}' />" />
            </div>

            <div class="col-md-6 mb-3">
                <label class="form-label" for="roomTypeId">Loại phòng</label>
                <select class="form-select" id="roomTypeId" name="roomTypeId" required>
                    <c:forEach var="rt" items="${roomTypes}">
                        <option value="${rt.roomTypeId}" ${room.roomTypeId == rt.roomTypeId ? 'selected' : ''}>
                            <c:out value="${rt.name} (Sức chứa: ${rt.capacity})" />
                        </option>
                    </c:forEach>
                </select>
            </div>
        </div>

        <div class="row">
            <div class="col-md-4 mb-3">
                <label class="form-label" for="monthlyPrice">Giá thuê (tháng)</label>
                <!-- SỬA: Đổi step="1" để người dùng nhập bất cứ số nào -->
                <input type="number" class="form-control" id="monthlyPrice" name="monthlyPrice" min="0" step="1" required
                       value="<c:out value='${room.monthlyPrice}' />" />
            </div>

            <div class="col-md-4 mb-3">
                <label class="form-label" for="floor">Tầng</label>
                <input type="number" class="form-control" id="floor" name="floor" min="1" required
                       value="<c:out value='${room.floor}' />" />
            </div>

            <div class="col-md-4 mb-3">
                <label class="form-label" for="area">Diện tích (m²)</label>
                <input type="number" class="form-control" id="area" name="area" min="0" step="0.1"
                       value="<c:out value='${not empty inputArea ? inputArea : (room.area > 0 ? room.area : "")}' />" />
                <small class="text-muted">Để trống nếu chưa có thông tin</small>
            </div>
        </div>

        <div class="mb-3">
            <label class="form-label" for="status">Trạng thái</label>
            <select class="form-select" id="status" name="status">
                <option value="ACTIVE" ${room.status == 'ACTIVE' ? 'selected' : ''}>Đang hoạt động (ACTIVE)</option>
                <option value="MAINTENANCE" ${room.status == 'MAINTENANCE' ? 'selected' : ''}>Bảo trì (MAINTENANCE)</option>
                <option value="HIDDEN" ${room.status == 'HIDDEN' ? 'selected' : ''}>Ẩn (HIDDEN)</option>
            </select>
        </div>

        <div class="mb-3">
            <label class="form-label" for="imageUrl">URL Ảnh minh họa</label>
            <input type="url" class="form-control" id="imageUrl" name="imageUrl"
                   value="<c:out value='${room.imageUrl}' />" />
        </div>

        <div class="mb-3">
            <label class="form-label" for="description">Mô tả chi tiết</label>
            <textarea class="form-control" id="description" name="description" rows="3"><c:out value='${room.description}' /></textarea>
        </div>

        <div class="d-flex gap-2">
            <button type="submit" class="btn btn-primary">Lưu lại</button>
            <a href="<c:url value='/admin/rooms' />" class="btn btn-outline-secondary">Hủy</a>
        </div>
    </form>
</div>

<%@ include file="/WEB-INF/views/common/_bottom.jspf" %>