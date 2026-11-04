<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/_taglibs.jspf" %>
<%-- Trang điện thoại mở sau khi quét QR của cổng giả: mô phỏng ứng dụng ngân hàng xác nhận thanh toán. --%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Xác nhận VNPAY-QR (giả lập)</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.8/dist/css/bootstrap.min.css"
          rel="stylesheet" crossorigin="anonymous"
          integrity="sha384-sRIl4kxILFvY47J16cr9ZwB07vP4J8+LH7qKQnuqkuIAvNWLzeN8tE5YBujZqJLB">
</head>
<body class="bg-light">
<main class="container py-4" style="max-width: 480px;">
    <div class="alert alert-warning small" role="note">Giả lập VNPAY-QR khi chạy local. Không có tiền thật được chuyển.</div>
    <h1 class="h5 fw-bold mb-3">Xác nhận thanh toán</h1>
    <div class="card border-0 shadow-sm mb-3">
        <div class="card-body">
            <dl class="row mb-0">
                <dt class="col-5">Mã đơn hàng</dt>
                <dd class="col-7 text-break"><c:out value="${ticket.txnRef}" /></dd>
                <dt class="col-5">Nội dung</dt>
                <dd class="col-7"><c:out value="${ticket.orderInfo}" /></dd>
                <dt class="col-5">Số tiền</dt>
                <dd class="col-7 fw-bold"><ui:money value="${ticket.amount}" /></dd>
            </dl>
        </div>
    </div>
    <c:choose>
        <c:when test="${ticket.done and ticket.paid}">
            <div class="alert alert-success" role="status">
                Thanh toán thành công. Máy tính sẽ tự quay về website.
            </div>
        </c:when>
        <c:when test="${ticket.done}">
            <div class="alert alert-secondary" role="status">Giao dịch đã bị hủy.</div>
        </c:when>
        <c:otherwise>
            <form method="post" action="<c:url value='/payment/fake-vnpay/qr' />" class="d-grid gap-2">
                <input type="hidden" name="token" value="${fn:escapeXml(token)}">
                <button type="submit" name="action" value="pay" class="btn btn-primary btn-lg">Xác nhận thanh toán</button>
                <button type="submit" name="action" value="cancel" class="btn btn-outline-danger">Hủy giao dịch</button>
            </form>
        </c:otherwise>
    </c:choose>
    <c:if test="${ticket.done}">
        <details class="small mt-3">
            <summary>Phản hồi IPN của máy chủ</summary>
            <code><c:out value="${ticket.ipnLog}" /></code>
        </details>
    </c:if>
</main>
</body>
</html>
