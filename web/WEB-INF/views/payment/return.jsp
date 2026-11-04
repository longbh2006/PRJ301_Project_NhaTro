<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/_taglibs.jspf" %>
<c:set var="pageTitle" value="RoomRent | Kết quả thanh toán" scope="request" />
<c:set var="activeNav" value="/tenant/invoices" scope="request" />
<%@ include file="/WEB-INF/views/common/_top.jspf" %>
<%-- Trang này chỉ HIỂN THỊ. Trạng thái hóa đơn chỉ đổi theo IPN của VNPay hoặc phản hồi querydr máy chủ tự hỏi VNPay
     (chạy local với sandbox có thể bật vnp_TrustReturnUrl, xem docs/NT-44-validation.md). --%>
<c:set var="payment" value="${result.payment}" />

<h1 class="h3 fw-bold mb-4">Kết quả thanh toán</h1>

<c:choose>
    <c:when test="${not result.validSignature}">
        <div class="alert alert-danger" role="alert">
            Chữ ký không hợp lệ: dữ liệu trên đường dẫn đã bị thay đổi hoặc không đến từ VNPay.
            Kết quả này không được ghi nhận.
        </div>
    </c:when>
    <c:when test="${empty payment}">
        <div class="alert alert-warning" role="alert">Không tìm thấy giao dịch tương ứng.</div>
    </c:when>
    <c:otherwise>
        <c:choose>
            <c:when test="${result.responseCode eq '00'}">
                <c:set var="vnpMessage" value="VNPay báo giao dịch thành công." />
            </c:when>
            <c:when test="${result.responseCode eq '24'}">
                <c:set var="vnpMessage" value="Bạn đã hủy giao dịch." />
            </c:when>
            <c:when test="${result.responseCode eq '51'}">
                <c:set var="vnpMessage" value="Tài khoản không đủ số dư." />
            </c:when>
            <c:when test="${result.responseCode eq '11'}">
                <c:set var="vnpMessage" value="Hết thời gian chờ thanh toán." />
            </c:when>
            <c:otherwise>
                <c:set var="vnpMessage" value="VNPay báo giao dịch không thành công (mã ${result.responseCode})." />
            </c:otherwise>
        </c:choose>

        <div class="card shadow-sm border-0 mb-4">
            <div class="card-body p-4">
                <p class="mb-3"><c:out value="${vnpMessage}" /></p>
                <dl class="row mb-0">
                    <dt class="col-sm-4">Mã giao dịch</dt>
                    <dd class="col-sm-8"><c:out value="${payment.txnRef}" /></dd>
                    <dt class="col-sm-4">Hóa đơn</dt>
                    <dd class="col-sm-8">#<c:out value="${payment.invoiceId}" /></dd>
                    <dt class="col-sm-4">Số tiền</dt>
                    <dd class="col-sm-8"><fmt:formatNumber value="${payment.amount}" pattern="#,##0" /> VNĐ</dd>
                    <dt class="col-sm-4">Trạng thái trên hệ thống</dt>
                    <dd class="col-sm-8">
                        <c:choose>
                            <c:when test="${payment.status eq 'SUCCESS'}">
                                <span class="badge text-bg-success">Đã ghi nhận thanh toán</span>
                            </c:when>
                            <c:when test="${payment.status eq 'FAILED'}">
                                <span class="badge text-bg-danger">Không thành công</span>
                            </c:when>
                            <c:otherwise>
                                <span class="badge text-bg-warning">Đang chờ VNPay xác nhận</span>
                            </c:otherwise>
                        </c:choose>
                    </dd>
                </dl>
                <c:if test="${payment.status eq 'PENDING'}">
                    <p class="small text-secondary mt-3 mb-0">
                        Hóa đơn chỉ chuyển sang "Đã trả" khi VNPay xác nhận với máy chủ. Hãy tải lại trang sau ít phút.
                    </p>
                </c:if>
            </div>
        </div>
        <a class="btn btn-outline-secondary" href="<c:url value='/tenant/pay'><c:param name='invoiceId' value='${payment.invoiceId}' /></c:url>">Xem hóa đơn</a>
    </c:otherwise>
</c:choose>

<%@ include file="/WEB-INF/views/common/_bottom.jspf" %>
