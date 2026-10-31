<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/_taglibs.jspf" %>
<c:set var="pageTitle" value="Thêm Đơn Giá" scope="request" />
<c:set var="activeNav" value="/admin/prices" scope="request" />
<%@ include file="/WEB-INF/views/common/_top.jspf" %>

<div class="container mt-4">
    <h1 class="h3 fw-bold mb-3">Tạo Bảng Giá Điện Nước Mới</h1>

    <div class="alert alert-info">
        <strong>Lưu ý:</strong> Việc thêm bảng giá mới sẽ áp dụng cho các hóa đơn tạo từ ngày hiệu lực trở đi. Bảng giá cũ sẽ được lưu trữ và không thể chỉnh sửa để bảo vệ dữ liệu lịch sử hóa đơn.
    </div>

    <c:if test="${not empty errorMessage}">
        <div class="alert alert-danger" role="alert">
            <c:out value="${errorMessage}" />
        </div>
    </c:if>

    <form action="<c:url value='/admin/prices' />" method="post" class="card p-4">
        <input type="hidden" name="action" value="save" />

        <div class="row">
            <div class="col-md-6 mb-3">
                <label class="form-label" for="electricPrice">Giá điện (VNĐ/kWh)</label>
                <input type="number" class="form-control" id="electricPrice" name="electricPrice" min="1" step="any" required
                       value="<c:out value='${inputPrice.electricPrice}' />" />
            </div>

            <div class="col-md-6 mb-3">
                <label class="form-label" for="waterPrice">Giá nước (VNĐ/khối)</label>
                <input type="number" class="form-control" id="waterPrice" name="waterPrice" min="1" step="any" required
                       value="<c:out value='${inputPrice.waterPrice}' />" />
            </div>
        </div>

        <div class="row">
            <div class="col-md-6 mb-3">
                <label class="form-label" for="serviceFeePerPerson">Phí dịch vụ chung (VNĐ/người/tháng)</label>
                <input type="number" class="form-control" id="serviceFeePerPerson" name="serviceFeePerPerson" min="0" step="any" required
                       value="<c:out value='${inputPrice.serviceFeePerPerson}' />" />
            </div>

            <div class="col-md-6 mb-3">
                <label class="form-label" for="effectiveFrom">Ngày bắt đầu áp dụng</label>
                <input type="date" class="form-control" id="effectiveFrom" name="effectiveFrom" required
                       value="<c:out value='${not empty inputDateStr ? inputDateStr : inputPrice.effectiveFrom}' />" />
            </div>
        </div>

        <div class="d-flex gap-2">
            <button type="submit" class="btn btn-primary">Xác nhận tạo bảng giá</button>
            <a href="<c:url value='/admin/prices' />" class="btn btn-outline-secondary">Hủy</a>
        </div>
    </form>
</div>

<%@ include file="/WEB-INF/views/common/_bottom.jspf" %>