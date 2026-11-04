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
                    <div class="form-check mb-2">
                        <input class="form-check-input" type="radio" name="bankCode" id="bank-qr" value="VNPAYQR" checked>
                        <label class="form-check-label" for="bank-qr">
                            Quét mã VNPAY-QR
                            <span class="d-block small text-secondary">Mở ứng dụng ngân hàng hoặc ví VNPAY để quét mã QR trên trang VNPay.</span>
                        </label>
                    </div>
                    <div class="form-check mb-2">
                        <input class="form-check-input" type="radio" name="bankCode" id="bank-atm" value="VNBANK">
                        <label class="form-check-label" for="bank-atm">Thẻ ATM / tài khoản ngân hàng nội địa</label>
                    </div>
                    <div class="form-check mb-2">
                        <input class="form-check-input" type="radio" name="bankCode" id="bank-intl" value="INTCARD">
                        <label class="form-check-label" for="bank-intl">Thẻ quốc tế (Visa, MasterCard, JCB)</label>
                    </div>
                    <div class="form-check">
                        <input class="form-check-input" type="radio" name="bankCode" id="bank-any" value="">
                        <label class="form-check-label" for="bank-any">Chọn trên trang VNPay</label>
                    </div>
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
