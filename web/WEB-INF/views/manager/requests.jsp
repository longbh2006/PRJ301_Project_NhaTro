<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/_taglibs.jspf" %>
<c:set var="pageTitle" value="RoomRent | Duyệt yêu cầu thuê" scope="request" />
<c:set var="activeNav" value="/manager/requests" scope="request" />
<%@ include file="/WEB-INF/views/common/_top.jspf" %>

<div class="mb-4">
    <h1 class="h3 fw-bold">Duyệt yêu cầu thuê</h1>
    <p class="text-secondary">Các yêu cầu đang chờ xử lý. Nhập tiền cọc và chỉ số điện nước khi duyệt.</p>
</div>
<c:if test="${not empty requestScope.successMessage}">
    <div class="alert alert-success" role="status"><c:out value="${requestScope.successMessage}" /></div>
</c:if>
<c:if test="${not empty requestScope.errorMessage}">
    <div class="alert alert-danger" role="alert"><c:out value="${requestScope.errorMessage}" /></div>
</c:if>
<form method="get" action="<c:url value='/manager/requests' />" class="d-flex flex-wrap align-items-end gap-2 mb-4">
    <div>
        <label for="room-filter" class="form-label">Lọc theo mã ID phòng</label>
        <input id="room-filter" name="roomId" type="number" min="1" max="2147483647" step="1"
               value="${fn:escapeXml(requestScope.roomId)}" class="form-control" placeholder="Tất cả phòng">
    </div>
    <button class="btn btn-primary" type="submit">Lọc</button>
    <a href="<c:url value='/manager/requests' />" class="btn btn-outline-secondary">Bỏ lọc</a>
</form>
<p class="small text-secondary">Có <c:out value="${fn:length(requestScope.pendingRequests)}" /> yêu cầu chờ duyệt.</p>
<c:choose>
    <c:when test="${empty requestScope.pendingRequests}">
        <div class="card roomrent-card"><div class="card-body p-4">Không có yêu cầu chờ duyệt trong danh sách này.</div></div>
    </c:when>
    <c:otherwise>
        <div class="d-grid gap-3">
            <c:forEach var="rental" items="${requestScope.pendingRequests}">
                <section class="card roomrent-card" aria-labelledby="request-${rental.requestId}-title">
                    <div class="card-body p-4">
                        <div class="d-flex flex-wrap justify-content-between gap-2 mb-3">
                            <h2 class="h5 mb-0" id="request-${rental.requestId}-title">
                                Yêu cầu #<c:out value="${rental.requestId}" /> · Phòng <c:out value="${rental.roomCode}" />
                            </h2>
                            <span class="badge text-bg-warning align-self-start">Chờ duyệt</span>
                        </div>
                        <dl class="row small mb-2">
                            <dt class="col-sm-3">Người thuê</dt>
                            <dd class="col-sm-9"><c:out value="${rental.tenantName}" /> (ID <c:out value="${rental.tenantId}" />)</dd>
                            <dt class="col-sm-3">Phòng</dt>
                            <dd class="col-sm-9"><c:out value="${rental.roomCode}" /> (ID <c:out value="${rental.roomId}" />)</dd>
                            <dt class="col-sm-3">Ngày nhận phòng</dt><dd class="col-sm-9"><c:out value="${rental.moveInDate}" /></dd>
                            <dt class="col-sm-3">Thời hạn / Số người</dt>
                            <dd class="col-sm-9"><c:out value="${rental.months}" /> tháng / <c:out value="${rental.numPeople}" /> người</dd>
                            <dt class="col-sm-3">Giá thuê hiện tại</dt>
                            <dd class="col-sm-9"><fmt:formatNumber value="${rental.monthlyPrice}" pattern="#,##0" /> đ/tháng</dd>
                            <dt class="col-sm-3">Ghi chú</dt><dd class="col-sm-9 text-break"><c:out value="${rental.note}" default="—" /></dd>
                        </dl>
                        <form method="post" action="<c:url value='/manager/requests' />" class="border-top pt-3">
                            <input type="hidden" name="csrfToken" value="${fn:escapeXml(requestScope.csrfToken)}">
                            <input type="hidden" name="action" value="approve">
                            <input type="hidden" name="requestId" value="${rental.requestId}">
                            <input type="hidden" name="roomId" value="${fn:escapeXml(requestScope.roomId)}">
                            <div class="row g-3 align-items-end">
                                <div class="col-md-4">
                                    <label class="form-label" for="deposit-${rental.requestId}">Tiền cọc (đ)</label>
                                    <input class="form-control" id="deposit-${rental.requestId}" name="deposit"
                                           type="number" min="0" max="999999999999" step="1" required
                                           value="${requestScope.formRequestId eq rental.requestId ? fn:escapeXml(param.deposit) : ''}">
                                </div>
                                <div class="col-md-4">
                                    <label class="form-label" for="electric-${rental.requestId}">Chỉ số điện (kWh)</label>
                                    <input class="form-control" id="electric-${rental.requestId}" name="electricIndex"
                                           type="number" min="0" max="2147483647" step="1" required
                                           value="${requestScope.formRequestId eq rental.requestId ? fn:escapeXml(param.electricIndex) : ''}">
                                </div>
                                <div class="col-md-4">
                                    <label class="form-label" for="water-${rental.requestId}">Chỉ số nước (m³)</label>
                                    <input class="form-control" id="water-${rental.requestId}" name="waterIndex"
                                           type="number" min="0" max="2147483647" step="1" required
                                           value="${requestScope.formRequestId eq rental.requestId ? fn:escapeXml(param.waterIndex) : ''}">
                                </div>
                            </div>
                            <p class="small text-secondary mt-2">Giá thuê được chốt theo giá phòng tại thời điểm duyệt. Những yêu cầu khác cùng phòng sẽ được từ chối.</p>
                            <button type="submit" class="btn btn-primary">Duyệt yêu cầu #<c:out value="${rental.requestId}" /></button>
                        </form>
                        <form method="post" action="<c:url value='/manager/requests' />" class="mt-2">
                            <input type="hidden" name="csrfToken" value="${fn:escapeXml(requestScope.csrfToken)}">
                            <input type="hidden" name="action" value="reject">
                            <input type="hidden" name="requestId" value="${rental.requestId}">
                            <input type="hidden" name="roomId" value="${fn:escapeXml(requestScope.roomId)}">
                            <button type="submit" class="btn btn-outline-danger">Từ chối yêu cầu #<c:out value="${rental.requestId}" /></button>
                        </form>
                    </div>
                </section>
            </c:forEach>
        </div>
    </c:otherwise>
</c:choose>

<%@ include file="/WEB-INF/views/common/_bottom.jspf" %>
