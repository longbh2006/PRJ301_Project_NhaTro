<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/_taglibs.jspf" %>
<%-- Trang riêng, không dùng layout RoomRent: mô phỏng việc rời website sang cổng VNPay. --%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Cổng VNPay giả (môi trường local)</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.8/dist/css/bootstrap.min.css"
          rel="stylesheet" crossorigin="anonymous"
          integrity="sha384-sRIl4kxILFvY47J16cr9ZwB07vP4J8+LH7qKQnuqkuIAvNWLzeN8tE5YBujZqJLB">
</head>
<body class="bg-light">
<main class="container py-5" style="max-width: 640px;">
    <div class="alert alert-warning small" role="note">
        Cổng giả chỉ dùng khi chạy local (VNPay sandbox không gọi được IPN về máy dev).
        Không có tiền thật được chuyển.
    </div>
    <h1 class="h4 fw-bold mb-4">VNPay (giả lập)</h1>

    <c:choose>
        <c:when test="${signatureError}">
            <div class="alert alert-danger" role="alert">
                Sai chữ ký (mã lỗi 70): dữ liệu thanh toán đã bị thay đổi. Giao dịch bị từ chối.
            </div>
        </c:when>
        <c:when test="${not empty returnUrl}">
            <h2 class="h5">Phản hồi IPN của máy chủ</h2>
            <ol class="small">
                <c:forEach var="line" items="${ipnLog}">
                    <li><code><c:out value="${line}" /></code></li>
                </c:forEach>
            </ol>
            <div class="d-flex flex-wrap gap-2">
                <a class="btn btn-primary" href="<c:out value='${returnUrl}' />">Quay về website</a>
                <form method="post" action="<c:url value='/payment/fake-vnpay' />" class="m-0">
                    <input type="hidden" name="action" value="resend">
                    <button type="submit" class="btn btn-outline-secondary">Gửi lại IPN (giống VNPay gọi lại)</button>
                </form>
            </div>
        </c:when>
        <c:otherwise>
            <div class="card border-0 shadow-sm mb-4">
                <div class="card-body">
                    <dl class="row mb-0">
                        <dt class="col-sm-4">Mã đơn hàng</dt>
                        <dd class="col-sm-8"><c:out value="${paymentParams.vnp_TxnRef}" /></dd>
                        <dt class="col-sm-4">Nội dung</dt>
                        <dd class="col-sm-8"><c:out value="${paymentParams.vnp_OrderInfo}" /></dd>
                        <dt class="col-sm-4">Số tiền</dt>
                        <dd class="col-sm-8 fw-bold"><fmt:formatNumber value="${amount}" pattern="#,##0" /> VND</dd>
                        <dt class="col-sm-4">Phương thức</dt>
                        <dd class="col-sm-8"><c:out value="${empty qrToken ? 'NCB (thẻ test)' : 'VNPAY-QR'}" /></dd>
                    </dl>
                </div>
            </div>
            <c:choose>
                <c:when test="${not empty qrToken}">
                    <c:url var="statusUrl" value="/payment/fake-vnpay/qr-status"><c:param name="token" value="${qrToken}" /></c:url>
                    <div class="card border-0 shadow-sm mb-3">
                        <div class="card-body text-center">
                            <p class="fw-semibold mb-3">Quét mã bằng camera điện thoại để thanh toán</p>
                            <div id="fake-qr" class="d-inline-block p-3 bg-white border rounded" role="img"
                                 aria-label="Mã QR thanh toán"
                                 data-qr-url="${fn:escapeXml(qrUrl)}" data-status-url="${fn:escapeXml(statusUrl)}"></div>
                            <p class="small text-secondary mt-3 mb-1">Điện thoại phải dùng chung Wi‑Fi với máy này. Không quét được thì mở link:</p>
                            <p class="small text-break mb-2"><a href="<c:out value='${qrUrl}' />" target="_blank" rel="noopener"><c:out value="${qrUrl}" /></a></p>
                            <p id="qr-status" class="small text-secondary mb-0" role="status" aria-live="polite">Đang chờ quét mã…</p>
                        </div>
                    </div>
                    <form method="post" action="<c:url value='/payment/fake-vnpay/qr' />" class="m-0">
                        <input type="hidden" name="token" value="${fn:escapeXml(qrToken)}">
                        <input type="hidden" name="from" value="desktop">
                        <button type="submit" name="action" value="cancel" class="btn btn-outline-danger">Hủy giao dịch</button>
                    </form>
                    <%-- qrcode.min.js: qrcodejs 1.0.0 (MIT, davidshimjs), lấy từ cdnjs, khớp SRI cdnjs công bố. --%>
                    <script src="<c:url value='/js/qrcode.min.js' />"></script>
                    <script>
                        (function () {
                            var box = document.getElementById('fake-qr');
                            var statusText = document.getElementById('qr-status');
                            new QRCode(box, { text: box.dataset.qrUrl, width: 220, height: 220,
                                correctLevel: QRCode.CorrectLevel.M });
                            // Điện thoại xác nhận xong thì trang này tự quay về website, giống VNPay thật.
                            var timer = setInterval(function () {
                                fetch(box.dataset.statusUrl, { cache: 'no-store' })
                                    .then(function (r) { return r.json(); })
                                    .then(function (s) {
                                        if (s.found !== 'true') {
                                            clearInterval(timer);
                                            statusText.textContent = 'Mã QR đã hết hạn. Hãy tạo giao dịch mới.';
                                        } else if (s.done === 'true') {
                                            clearInterval(timer);
                                            statusText.textContent = 'Đã nhận kết quả, đang quay về website…';
                                            window.location.href = s.returnUrl;
                                        }
                                    })
                                    .catch(function () { /* mạng chập chờn: thử lại ở lượt sau */ });
                            }, 2000);
                        })();
                    </script>
                </c:when>
                <c:otherwise>
                    <form method="post" action="<c:url value='/payment/fake-vnpay' />" class="d-flex flex-wrap gap-2">
                        <c:forEach var="field" items="${paymentParams}">
                            <input type="hidden" name="${fn:escapeXml(field.key)}" value="${fn:escapeXml(field.value)}">
                        </c:forEach>
                        <button type="submit" name="action" value="pay" class="btn btn-primary">Thanh toán thành công</button>
                        <button type="submit" name="action" value="cancel" class="btn btn-outline-danger">Hủy giao dịch</button>
                    </form>
                </c:otherwise>
            </c:choose>
        </c:otherwise>
    </c:choose>
</main>
</body>
</html>
