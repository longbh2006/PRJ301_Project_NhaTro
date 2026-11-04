<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/_taglibs.jspf" %>
<c:set var="pageTitle" value="RoomRent | Thanh toán hóa đơn" scope="request" />
<c:set var="activeNav" value="/tenant/invoices" scope="request" />
<%@ include file="/WEB-INF/views/common/_top.jspf" %>

<div class="d-flex flex-wrap align-items-center gap-2 mb-4">
    <h1 class="h3 fw-bold mb-0">Thanh toán hóa đơn #<c:out value="${invoice.invoiceId}" /></h1>
    <ui:statusBadge value="${invoice.status}" />
</div>

<%-- Hóa đơn đã trả thì thông báo xanh bên dưới đã đủ, không lặp thêm lỗi "đã thanh toán". --%>
<c:if test="${not empty errorMessage and invoice.status eq 'UNPAID'}">
    <div class="alert alert-danger" role="alert"><c:out value="${errorMessage}" /></div>
</c:if>

<div class="card shadow-sm border-0 mb-4">
    <div class="card-body p-4">
        <dl class="row mb-0">
            <dt class="col-sm-4">Kỳ</dt>
            <dd class="col-sm-8">Tháng <c:out value="${invoice.month}" />/<c:out value="${invoice.year}" /></dd>
            <dt class="col-sm-4">Hạn thanh toán</dt>
            <dd class="col-sm-8"><c:out value="${invoice.dueDate}" /></dd>
            <dt class="col-sm-4">Tổng cộng</dt>
            <dd class="col-sm-8 fw-bold"><fmt:formatNumber value="${invoice.total}" pattern="#,##0" /> VNĐ</dd>
            <c:if test="${not empty invoice.paidAt}">
                <dt class="col-sm-4">Đã thanh toán lúc</dt>
                <dd class="col-sm-8"><c:out value="${invoice.paidAt}" /></dd>
            </c:if>
        </dl>
    </div>
</div>

<c:choose>
    <c:when test="${invoice.status eq 'UNPAID'}">
        <form method="post" action="<c:url value='/tenant/pay' />">
            <input type="hidden" name="invoiceId" value="${invoice.invoiceId}" />
            <fieldset class="card shadow-sm border-0 mb-3">
                <div class="card-body p-4">
                    <legend class="h6 fw-bold mb-3">Phương thức thanh toán</legend>
                    <%-- Danh sách do máy chủ quyết định theo cấu hình cổng; phương thức chưa chạy được thì không hiện. --%>
                    <c:forEach var="code" items="${bankCodes}" varStatus="loop">
                        <div class="form-check${loop.last ? '' : ' mb-2'}">
                            <input class="form-check-input" type="radio" name="bankCode" id="bank-${loop.index}"
                                   value="${code}"${loop.first ? ' checked' : ''}>
                            <label class="form-check-label" for="bank-${loop.index}">
                                <c:choose>
                                    <c:when test="${code eq 'VNPAYQR'}">
                                        Quét mã VNPAY-QR
                                        <span class="d-block small text-secondary">Mở ứng dụng ngân hàng hoặc ví VNPAY để quét mã QR trên trang VNPay.</span>
                                    </c:when>
                                    <c:when test="${code eq 'VNBANK'}">Thẻ ATM / tài khoản ngân hàng nội địa</c:when>
                                    <c:when test="${code eq 'INTCARD'}">Thẻ quốc tế (Visa, MasterCard, JCB)</c:when>
                                    <c:otherwise>Chọn trên trang VNPay</c:otherwise>
                                </c:choose>
                            </label>
                        </div>
                    </c:forEach>
                </div>
            </fieldset>
            <div class="d-flex flex-wrap align-items-center gap-3">
                <button type="submit" class="btn btn-primary px-4">Thanh toán qua VNPay</button>
                <span class="small text-secondary">Bạn sẽ được chuyển sang cổng VNPay để thanh toán.</span>
            </div>
        </form>
    </c:when>
    <c:otherwise>
        <div class="alert alert-success" role="status">Hóa đơn này đã được thanh toán.</div>
    </c:otherwise>
</c:choose>

<%@ include file="/WEB-INF/views/common/_bottom.jspf" %>
