<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/_taglibs.jspf" %>
<c:set var="pageTitle" value="RoomRent | Chi tiết hoá đơn" scope="request" />
<c:set var="activeNav" value="/tenant/invoices" scope="request" />
<%@ include file="/WEB-INF/views/common/_top.jspf" %>
<div class="d-flex flex-wrap align-items-center gap-2 mb-3">
    <h1 class="h3 fw-bold mb-0">Hoá đơn <c:out value="${invoice.invoiceCode}" /></h1>
    <ui:statusBadge value="${invoice.status}" />
</div>
<dl class="row">
    <dt class="col-sm-4">Phòng</dt><dd class="col-sm-8"><c:out value="${invoice.roomCode}" /></dd>
    <dt class="col-sm-4">Kỳ hoá đơn</dt><dd class="col-sm-8">Tháng <c:out value="${invoice.month}" />/<c:out value="${invoice.year}" /></dd>
    <dt class="col-sm-4">Hạn thanh toán</dt><dd class="col-sm-8"><c:out value="${invoice.dueDateDisplay}" /></dd>
    <c:if test="${not empty invoice.paidAt}">
        <dt class="col-sm-4">Thời điểm thanh toán</dt><dd class="col-sm-8"><c:out value="${invoice.paidAtDisplay}" /></dd>
    </c:if>
</dl>
<div class="table-responsive">
    <table class="table table-bordered align-middle">
        <thead class="table-light">
            <tr><th>Khoản</th><th>Số lượng</th><th class="text-end">Thành tiền (VNĐ)</th></tr>
        </thead>
        <tbody>
            <tr><td>Tiền phòng</td><td>1 tháng</td><td class="text-end"><fmt:formatNumber value="${invoice.roomFee}" pattern="#,##0" /></td></tr>
            <tr><td>Điện</td><td><c:out value="${invoice.electricUsed}" /> kWh</td><td class="text-end"><fmt:formatNumber value="${invoice.electricFee}" pattern="#,##0" /></td></tr>
            <tr><td>Nước</td><td><c:out value="${invoice.waterUsed}" /> m³</td><td class="text-end"><fmt:formatNumber value="${invoice.waterFee}" pattern="#,##0" /></td></tr>
            <tr><td>Phí dịch vụ</td><td></td><td class="text-end"><fmt:formatNumber value="${invoice.serviceFee}" pattern="#,##0" /></td></tr>
        </tbody>
        <tfoot>
            <tr class="fw-bold"><td colspan="2">Tổng cộng</td><td class="text-end"><fmt:formatNumber value="${invoice.total}" pattern="#,##0" /></td></tr>
        </tfoot>
    </table>
</div>
<c:if test="${invoice.status eq 'UNPAID'}">
    <c:url var="payUrl" value="/tenant/pay"><c:param name="id" value="${invoice.invoiceId}" /></c:url>
    <a class="btn btn-primary" href="<c:out value='${payUrl}' />">Thanh toán hoá đơn</a>
</c:if>
<%@ include file="/WEB-INF/views/common/_bottom.jspf" %>
