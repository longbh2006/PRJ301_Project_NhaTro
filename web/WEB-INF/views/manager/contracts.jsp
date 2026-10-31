<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/_taglibs.jspf" %>
<c:set var="pageTitle" value="RoomRent | Quản lý hợp đồng" scope="request" />
<c:set var="activeNav" value="/manager/contracts" scope="request" />
<%@ include file="/WEB-INF/views/common/_top.jspf" %>

<h1>Quản lý hợp đồng</h1>
<p class="text-secondary">Tìm hợp đồng theo trạng thái, phòng và tên người thuê. Chỉ kết thúc khi đã thanh toán hết hóa đơn.</p>
<c:if test="${not empty successMessage}"><div class="alert alert-success" role="status"><c:out value="${successMessage}" /></div></c:if>
<c:if test="${not empty errorMessage}"><div class="alert alert-danger" role="alert"><c:out value="${errorMessage}" /></div></c:if>

<form method="get" action="<c:url value='/manager/contracts' />" class="row g-3 mb-4" aria-label="Lọc hợp đồng">
    <div class="col-md-3"><label for="contract-status" class="form-label">Trạng thái</label>
        <select id="contract-status" name="status" class="form-select">
            <option value="" ${empty selectedStatus ? 'selected' : ''}>Tất cả</option>
            <option value="ACTIVE" ${selectedStatus eq 'ACTIVE' ? 'selected' : ''}>Đang hiệu lực</option>
            <option value="ENDED" ${selectedStatus eq 'ENDED' ? 'selected' : ''}>Đã kết thúc</option>
        </select>
    </div>
    <div class="col-md-3"><label for="contract-room" class="form-label">Mã phòng (ID)</label>
        <input id="contract-room" name="roomId" type="number" min="1" max="2147483647" class="form-control" value="${fn:escapeXml(selectedRoomId)}">
    </div>
    <div class="col-md-4"><label for="contract-tenant" class="form-label">Tên người thuê</label>
        <input id="contract-tenant" name="tenantName" maxlength="100" class="form-control" value="${fn:escapeXml(tenantName)}">
    </div>
    <div class="col-md-2 d-flex align-items-end gap-2"><button type="submit" class="btn btn-primary">Tìm / lọc</button>
        <a href="<c:url value='/manager/contracts' />" class="btn btn-outline-secondary">Xóa lọc</a>
    </div>
</form>

<p>Tổng cộng <strong><c:out value="${contractPage.total}" /></strong> hợp đồng.</p>
<div class="table-responsive"><table class="table roomrent-table align-middle">
    <thead><tr><th scope="col">Hợp đồng</th><th scope="col">Phòng</th><th scope="col">Người thuê</th>
        <th scope="col">Thời hạn</th><th scope="col">Tiền thuê / tháng</th><th scope="col">Trạng thái</th><th scope="col">Thao tác</th></tr></thead>
    <tbody>
    <c:forEach var="contract" items="${contractPage.contracts}"><tr>
        <td>HĐ-<c:out value="${contract.contractId}" /></td>
        <td><c:out value="${contract.roomCode}" /></td>
        <td><c:out value="${contract.tenantName}" /></td>
        <td><c:out value="${contract.startDate}" /> → <c:out value="${contract.endDate}" /></td>
        <td><fmt:formatNumber value="${contract.monthlyRent}" pattern="#,##0" /> đ</td>
        <td><c:choose>
            <c:when test="${contract.status eq 'ACTIVE'}"><span class="badge text-bg-success">Đang hiệu lực</span></c:when>
            <c:otherwise><span class="badge text-bg-secondary">Đã kết thúc</span><br><small><c:out value="${contract.endedAt}" /></small></c:otherwise>
        </c:choose></td>
        <td><c:if test="${contract.status eq 'ACTIVE'}">
            <form method="post" action="<c:url value='/manager/contracts' />" onsubmit="return confirm('Kết thúc hợp đồng này? Chỉ thực hiện khi người thuê đã thanh toán hết hóa đơn.');">
                <input type="hidden" name="action" value="end">
                <input type="hidden" name="contractId" value="${fn:escapeXml(contract.contractId)}">
                <input type="hidden" name="csrfToken" value="${fn:escapeXml(sessionScope.contractCsrfToken)}">
                <input type="hidden" name="status" value="${fn:escapeXml(selectedStatus)}">
                <input type="hidden" name="roomId" value="${fn:escapeXml(selectedRoomId)}">
                <input type="hidden" name="tenantName" value="${fn:escapeXml(tenantName)}">
                <input type="hidden" name="page" value="${fn:escapeXml(contractPage.page)}">
                <button type="submit" class="btn btn-outline-danger btn-sm">Kết thúc hợp đồng</button>
            </form>
        </c:if></td>
    </tr></c:forEach>
    <c:if test="${empty contractPage.contracts}"><tr><td colspan="7" class="text-center text-secondary py-4">Không tìm thấy hợp đồng phù hợp.</td></tr></c:if>
    </tbody>
</table></div>

<nav aria-label="Phân trang hợp đồng" class="d-flex align-items-center justify-content-between gap-3">
    <span>Trang <c:out value="${contractPage.page}" /> / <c:out value="${contractPage.totalPages}" /></span>
    <div class="d-flex gap-2">
        <c:if test="${contractPage.page gt 1}">
            <c:url var="previousUrl" value="/manager/contracts">
                <c:param name="status" value="${selectedStatus}" /><c:param name="roomId" value="${selectedRoomId}" />
                <c:param name="tenantName" value="${tenantName}" /><c:param name="page" value="${contractPage.page - 1}" />
            </c:url><a class="btn btn-outline-secondary" href="${fn:escapeXml(previousUrl)}">Trang trước</a>
        </c:if>
        <c:if test="${contractPage.page lt contractPage.totalPages}">
            <c:url var="nextUrl" value="/manager/contracts">
                <c:param name="status" value="${selectedStatus}" /><c:param name="roomId" value="${selectedRoomId}" />
                <c:param name="tenantName" value="${tenantName}" /><c:param name="page" value="${contractPage.page + 1}" />
            </c:url><a class="btn btn-outline-secondary" href="${fn:escapeXml(nextUrl)}">Trang sau</a>
        </c:if>
    </div>
</nav>
<%@ include file="/WEB-INF/views/common/_bottom.jspf" %>
