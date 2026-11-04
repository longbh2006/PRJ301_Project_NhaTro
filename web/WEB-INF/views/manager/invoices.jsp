<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/_taglibs.jspf" %>
<%@ taglib prefix="nt" uri="/WEB-INF/nt" %>
<c:set var="pageTitle" value="RoomRent | Hóa đơn" scope="request" />
<c:set var="activeNav" value="/manager/invoices" scope="request" />
<%@ include file="/WEB-INF/views/common/_top.jspf" %>
<c:set var="summary" value="${invoicePage.summary}" />

<div class="mb-4">
    <h1 class="h3 fw-bold mb-1">Hóa đơn</h1>
    <p class="text-secondary mb-0">
        Hóa đơn của mọi phòng, kỳ mới nhất trước. Lập hóa đơn mới tại
        <a href="<c:url value='/manager/meters' />">Điện nước</a>.
    </p>
</div>

<div class="card roomrent-card mb-4">
    <div class="card-body p-4">
        <form method="get" action="<c:url value='/manager/invoices' />" class="row g-3 align-items-end" aria-label="Lọc hóa đơn">
            <div class="col-md-4 col-lg-3">
                <span class="form-label d-block" id="invoice-period-label">Kỳ</span>
                <div class="input-group" role="group" aria-labelledby="invoice-period-label">
                    <select id="invoice-month" name="month" class="form-select" aria-label="Tháng">
                        <option value="" ${empty selectedMonth ? 'selected' : ''}>Mọi tháng</option>
                        <c:forEach var="m" begin="1" end="12">
                            <option value="${m}" ${m eq selectedMonth ? 'selected' : ''}>Tháng <c:out value="${m}" /></option>
                        </c:forEach>
                    </select>
                    <input id="invoice-year" name="year" type="number" class="form-control" aria-label="Năm"
                           min="2020" max="2100" step="1" placeholder="Năm" value="${fn:escapeXml(selectedYear)}">
                </div>
            </div>
            <div class="col-md-3 col-lg-2">
                <label for="invoice-status" class="form-label">Trạng thái</label>
                <select id="invoice-status" name="status" class="form-select">
                    <option value="" ${empty selectedStatus ? 'selected' : ''}>Tất cả</option>
                    <option value="UNPAID" ${selectedStatus eq 'UNPAID' ? 'selected' : ''}>Chưa trả</option>
                    <option value="OVERDUE" ${selectedStatus eq 'OVERDUE' ? 'selected' : ''}>Quá hạn</option>
                    <option value="PAID" ${selectedStatus eq 'PAID' ? 'selected' : ''}>Đã trả</option>
                </select>
            </div>
            <div class="col-md-2">
                <label for="invoice-room" class="form-label">Mã phòng</label>
                <input id="invoice-room" name="roomCode" maxlength="10" class="form-control" placeholder="P101"
                       value="${fn:escapeXml(roomCode)}">
            </div>
            <div class="col-md-3">
                <label for="invoice-tenant" class="form-label">Tên người thuê</label>
                <input id="invoice-tenant" name="tenantName" maxlength="100" class="form-control"
                       value="${fn:escapeXml(tenantName)}">
            </div>
            <div class="col-md-12 col-lg-2 d-flex gap-2">
                <button type="submit" class="btn btn-primary">Lọc</button>
                <a href="<c:url value='/manager/invoices' />" class="btn btn-outline-secondary">Xóa lọc</a>
            </div>
        </form>
    </div>
</div>

<p>
    <strong><c:out value="${summary.count}" /></strong> hóa đơn
    · Tổng phải thu <strong><ui:money value="${summary.totalAmount}" /></strong>
    · Chưa thu <strong class="text-danger"><ui:money value="${summary.unpaidAmount}" /></strong>
</p>

<div class="table-responsive" tabindex="0" role="region" aria-label="Danh sách hóa đơn">
    <table class="table roomrent-table align-middle">
        <thead>
            <tr>
                <th scope="col">Mã HĐơn</th>
                <th scope="col">Kỳ</th>
                <th scope="col">Phòng</th>
                <th scope="col">Người thuê</th>
                <th scope="col" class="text-end">Tổng tiền</th>
                <th scope="col">Hạn trả</th>
                <th scope="col">Trạng thái</th>
                <th scope="col"><span class="visually-hidden">Thao tác</span></th>
            </tr>
        </thead>
        <tbody>
            <c:forEach var="inv" items="${invoicePage.invoices}">
                <c:url var="detailUrl" value="/manager/invoices/detail">
                    <c:param name="id" value="${inv.invoiceId}" />
                </c:url>
                <tr>
                    <th scope="row"><c:out value="${inv.code}" /></th>
                    <td><c:out value="${inv.periodDisplay}" /></td>
                    <td><c:out value="${inv.roomCode}" /></td>
                    <td><c:out value="${inv.tenantName}" /></td>
                    <td class="text-end text-nowrap fw-semibold"><ui:money value="${inv.total}" /></td>
                    <td><c:out value="${inv.dueDateDisplay}" /></td>
                    <td class="text-nowrap">
                        <ui:statusBadge value="${inv.status}" />
                        <c:if test="${inv.overdue}"><span class="badge text-bg-danger">Quá hạn</span></c:if>
                        <c:if test="${not empty inv.paidAt}">
                            <div class="small text-secondary"><c:out value="${inv.paidAtDisplay}" /></div>
                        </c:if>
                    </td>
                    <td><a class="btn btn-outline-primary btn-sm" href="<c:out value='${detailUrl}' />">Xem</a></td>
                </tr>
            </c:forEach>
            <c:if test="${empty invoicePage.invoices}">
                <tr><td colspan="8" class="text-center text-secondary py-4">Không tìm thấy hóa đơn phù hợp.</td></tr>
            </c:if>
        </tbody>
    </table>
</div>

<c:url var="basePagerUrl" value="/manager/invoices">
    <c:param name="month" value="${selectedMonth}" />
    <c:param name="year" value="${selectedYear}" />
    <c:param name="status" value="${selectedStatus}" />
    <c:param name="roomCode" value="${roomCode}" />
    <c:param name="tenantName" value="${tenantName}" />
</c:url>
<nt:pager page="${invoicePage.page}" totalPages="${invoicePage.totalPages}" url="${basePagerUrl}" />

<%@ include file="/WEB-INF/views/common/_bottom.jspf" %>
