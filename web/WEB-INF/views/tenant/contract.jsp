<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/_taglibs.jspf" %>
<c:set var="pageTitle" value="RoomRent | Hợp đồng của tôi" scope="request" />
<c:set var="activeNav" value="/tenant/contract" scope="request" />
<%@ include file="/WEB-INF/views/common/_top.jspf" %>
<h1>Hợp đồng của tôi</h1>
<c:choose>
    <c:when test="${empty contract}">
        <div class="alert alert-info" role="status">Bạn chưa có hợp đồng thuê đang có hiệu lực.</div>
        <a href="<c:url value='/tenant/requests' />">Xem yêu cầu thuê của tôi</a>
    </c:when>
    <c:otherwise>
        <p><ui:statusBadge value="${contract.status}" /></p>
        <dl class="row">
            <dt class="col-sm-4">Mã hợp đồng</dt><dd class="col-sm-8"><c:out value="${contract.contractId}" /></dd>
            <dt class="col-sm-4">Phòng</dt><dd class="col-sm-8"><c:out value="${contract.roomCode}" /></dd>
            <dt class="col-sm-4">Ngày bắt đầu</dt><dd class="col-sm-8"><c:out value="${contract.startDate}" /></dd>
            <dt class="col-sm-4">Ngày kết thúc</dt><dd class="col-sm-8"><c:out value="${contract.endDate}" /></dd>
            <dt class="col-sm-4">Giá thuê mỗi tháng</dt><dd class="col-sm-8"><fmt:formatNumber value="${contract.monthlyRent}" pattern="#,##0.##" /> VNĐ</dd>
            <dt class="col-sm-4">Tiền cọc</dt><dd class="col-sm-8"><fmt:formatNumber value="${contract.deposit}" pattern="#,##0.##" /> VNĐ</dd>
            <dt class="col-sm-4">Số người</dt><dd class="col-sm-8"><c:out value="${contract.numPeople}" /></dd>
        </dl>
    </c:otherwise>
</c:choose>
<%@ include file="/WEB-INF/views/common/_bottom.jspf" %>
