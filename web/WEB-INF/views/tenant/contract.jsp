<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/_taglibs.jspf" %>
<c:set var="pageTitle" value="RoomRent | Hợp đồng của tôi" scope="request" />
<c:set var="activeNav" value="/tenant/contract" scope="request" />
<%@ include file="/WEB-INF/views/common/_top.jspf" %>
<h1>Hợp đồng của tôi</h1>
<c:choose>
    <c:when test="${empty contracts}">
        <div class="alert alert-info" role="status">Bạn chưa có hợp đồng thuê nào.</div>
        <a href="<c:url value='/tenant/requests' />">Xem yêu cầu thuê của tôi</a>
    </c:when>
    <c:otherwise>
        <c:if test="${contracts[0].status ne 'ACTIVE'}">
            <div class="alert alert-info" role="status">Bạn chưa có hợp đồng thuê đang có hiệu lực. Các hợp đồng đã kết thúc được liệt kê bên dưới.</div>
        </c:if>
        <c:forEach var="contract" items="${contracts}">
            <section class="card mb-3">
                <div class="card-body">
                    <h2 class="h5">Hợp đồng #<c:out value="${contract.contractId}" /> <ui:statusBadge value="${contract.status}" /></h2>
                    <dl class="row mb-0">
                        <dt class="col-sm-4">Phòng</dt><dd class="col-sm-8"><c:out value="${contract.roomCode}" /></dd>
                        <dt class="col-sm-4">Ngày bắt đầu</dt><dd class="col-sm-8"><c:out value="${contract.startDate}" /></dd>
                        <dt class="col-sm-4">Ngày kết thúc</dt><dd class="col-sm-8"><c:out value="${contract.endDate}" /></dd>
                        <c:if test="${contract.status eq 'ENDED'}">
                            <dt class="col-sm-4">Đã kết thúc lúc</dt><dd class="col-sm-8"><c:out value="${contract.endedAt}" /></dd>
                        </c:if>
                        <dt class="col-sm-4">Giá thuê mỗi tháng</dt><dd class="col-sm-8"><fmt:formatNumber value="${contract.monthlyRent}" pattern="#,##0.##" /> VNĐ</dd>
                        <dt class="col-sm-4">Tiền cọc</dt><dd class="col-sm-8"><fmt:formatNumber value="${contract.deposit}" pattern="#,##0.##" /> VNĐ</dd>
                        <dt class="col-sm-4">Số người</dt><dd class="col-sm-8"><c:out value="${contract.numPeople}" /></dd>
                    </dl>
                </div>
            </section>
        </c:forEach>
    </c:otherwise>
</c:choose>
<%@ include file="/WEB-INF/views/common/_bottom.jspf" %>
