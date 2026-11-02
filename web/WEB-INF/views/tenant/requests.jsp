<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/_taglibs.jspf" %>
<c:set var="pageTitle" value="RoomRent | Yêu cầu thuê của tôi" scope="request" />
<c:set var="rentalDesign" value="${true}" scope="request" />
<c:set var="activeNav" value="/tenant/requests" scope="request" />
<%@ include file="/WEB-INF/views/common/_top.jspf" %>

<h1 class="rental-page-title">Yêu cầu thuê của tôi</h1>
<p class="rental-page-subtitle">Chỉ hiển thị yêu cầu của tài khoản<c:if test="${not empty sessionScope.fullName}"> <c:out value="${sessionScope.fullName}" /></c:if> đang đăng nhập.</p>
<c:if test="${not empty successMessage}"><div class="alert alert-success" role="status"><c:out value="${successMessage}" /></div></c:if>

<form class="rental-filters" method="get" action="<c:url value='/tenant/requests' />" aria-label="Lọc yêu cầu thuê">
    <div><label for="request-status">Trạng thái</label>
        <select class="form-select" id="request-status" name="status">
            <option value="" ${empty selectedStatus ? 'selected' : ''}>Tất cả</option>
            <option value="PENDING" ${selectedStatus eq 'PENDING' ? 'selected' : ''}>Chờ duyệt</option>
            <option value="APPROVED" ${selectedStatus eq 'APPROVED' ? 'selected' : ''}>Đã duyệt</option>
            <option value="REJECTED" ${selectedStatus eq 'REJECTED' ? 'selected' : ''}>Từ chối</option>
            <option value="CANCELLED" ${selectedStatus eq 'CANCELLED' ? 'selected' : ''}>Đã hủy</option>
        </select></div>
    <div><label for="request-query">Tìm theo mã / phòng</label>
        <input class="form-control" id="request-query" name="q" maxlength="100" placeholder="Nhập mã YC / phòng..." value="${fn:escapeXml(searchQuery)}"></div>
    <button class="btn btn-primary" type="submit">Lọc</button>
</form>

<div class="rental-table-wrap table-responsive" role="region" aria-label="Yêu cầu thuê của tôi" tabindex="0"><table class="table roomrent-table rental-requests-table mb-0">
    <thead><tr><th scope="col">Mã yêu cầu</th><th scope="col">Phòng</th><th scope="col">Ngày gửi</th><th scope="col">Trạng thái</th><th scope="col">Thao tác</th></tr></thead>
    <tbody>
    <c:forEach var="rentalRequest" items="${rentalRequests}"><tr>
        <td><c:out value="${rentalRequest.requestCode}" /></td>
        <td><c:out value="${rentalRequest.roomCode}" /></td>
        <td><c:out value="${rentalRequest.createdAtDisplay}" /></td>
        <td><ui:statusBadge value="${rentalRequest.status}" /></td>
        <td><c:url var="detailUrl" value="/tenant/requests">
                <c:param name="requestId" value="${rentalRequest.requestId}" />
                <c:param name="status" value="${selectedStatus}" /><c:param name="q" value="${searchQuery}" />
            </c:url><a href="${fn:escapeXml(detailUrl)}#request-detail">Xem</a>
            <c:if test="${rentalRequest.status eq 'PENDING'}">
                <form method="post" action="<c:url value='/tenant/requests/cancel' />" class="d-inline ms-2">
                    <input type="hidden" name="requestId" value="${rentalRequest.requestId}">
                    <input type="hidden" name="csrfToken" value="${fn:escapeXml(sessionScope.rentalCsrfToken)}">
                    <button class="btn btn-outline-danger btn-sm" type="submit">Hủy yêu cầu</button>
                </form>
            </c:if>
        </td>
    </tr></c:forEach>
    <c:if test="${empty rentalRequests}"><tr><td colspan="5" class="rental-empty-state">
        <c:choose><c:when test="${empty selectedStatus and empty searchQuery}">Bạn chưa gửi yêu cầu thuê phòng nào.</c:when>
            <c:otherwise>Không tìm thấy yêu cầu phù hợp với bộ lọc.</c:otherwise></c:choose>
    </td></tr></c:if>
    </tbody>
</table></div>

<section class="rental-request-detail" id="request-detail" aria-labelledby="request-detail-title">
    <h2 id="request-detail-title">Chi tiết yêu cầu thuê</h2>
    <c:choose>
        <c:when test="${not empty selectedRequest}">
            <p class="rental-detail-title"><c:out value="${selectedRequest.requestCode}" /> · Phòng <c:out value="${selectedRequest.roomCode}" /></p>
            <dl class="rental-request-facts">
                <div><dt>Ngày dọn vào</dt><dd><c:out value="${selectedRequest.moveInDate}" /></dd></div>
                <div><dt>Thời hạn thuê</dt><dd><c:out value="${selectedRequest.months}" /> tháng</dd></div>
                <div><dt>Số người</dt><dd><c:out value="${selectedRequest.numPeople}" /> người</dd></div>
            </dl>
            <p class="rental-detail-note">Ghi chú: <c:out value="${selectedRequest.note}" default="Không có ghi chú." /></p>
        </c:when>
        <c:otherwise><p class="rental-help mb-0">Chọn “Xem” ở yêu cầu để kiểm tra ngày dọn vào, thời hạn thuê, số người và ghi chú.</p></c:otherwise>
    </c:choose>
</section>
<%@ include file="/WEB-INF/views/common/_bottom.jspf" %>
