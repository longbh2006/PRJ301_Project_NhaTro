<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/_taglibs.jspf" %>
<c:set var="pageTitle" value="RoomRent | Thanh toán hoá đơn" scope="request" />
<c:set var="activeNav" value="/tenant/invoices" scope="request" />
<%@ include file="/WEB-INF/views/common/_top.jspf" %>
<h1 class="h3 fw-bold">Thanh toán hoá đơn <c:out value="${invoice.invoiceCode}" /></h1>
<dl class="row">
    <dt class="col-sm-4">Phòng</dt><dd class="col-sm-8"><c:out value="${invoice.roomCode}" /></dd>
    <dt class="col-sm-4">Kỳ hoá đơn</dt><dd class="col-sm-8">Tháng <c:out value="${invoice.month}" />/<c:out value="${invoice.year}" /></dd>
    <dt class="col-sm-4">Hạn thanh toán</dt><dd class="col-sm-8"><c:out value="${invoice.dueDateDisplay}" /></dd>
    <dt class="col-sm-4">Số tiền</dt><dd class="col-sm-8 fw-bold"><fmt:formatNumber value="${invoice.total}" pattern="#,##0" /> VNĐ</dd>
    <dt class="col-sm-4">Trạng thái</dt><dd class="col-sm-8"><ui:statusBadge value="${invoice.status}" /></dd>
</dl>
<c:choose>
    <c:when test="${invoice.status eq 'PAID'}">
        <div class="alert alert-success" role="status">Hoá đơn này đã được thanh toán lúc <c:out value="${invoice.paidAtDisplay}" />.</div>
    </c:when>
    <c:otherwise>
        <%-- Bước chuyển sang VNPay thuộc nhiệm vụ thanh toán riêng; trang này chỉ xác nhận số tiền. --%>
        <div class="alert alert-info" role="status">Cổng thanh toán VNPay chưa được kết nối.</div>
    </c:otherwise>
</c:choose>
<c:url var="detailUrl" value="/tenant/invoices/detail"><c:param name="id" value="${invoice.invoiceId}" /></c:url>
<a href="<c:out value='${detailUrl}' />">Xem chi tiết hoá đơn</a>
<%@ include file="/WEB-INF/views/common/_bottom.jspf" %>
