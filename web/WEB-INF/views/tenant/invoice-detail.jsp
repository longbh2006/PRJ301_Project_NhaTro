<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/_taglibs.jspf" %>
<c:set var="pageTitle" value="RoomRent | Chi tiết hóa đơn ${invoice.code}" scope="request" />
<c:set var="activeNav" value="/tenant/invoices" scope="request" />
<%@ include file="/WEB-INF/views/common/_top.jspf" %>
<c:set var="price" value="${invoice.utilityPrice}" />

<div class="mb-3">
    <a href="<c:url value='/tenant/invoices' />">&larr; Danh sách hóa đơn</a>
</div>

<div class="d-flex flex-wrap align-items-center gap-2 mb-4">
    <h1 class="h3 fw-bold mb-0">Chi tiết hóa đơn <c:out value="${invoice.code}" /></h1>
    <ui:statusBadge value="${invoice.status}" />
    <c:if test="${invoice.overdue}"><span class="badge text-bg-danger">Quá hạn</span></c:if>
</div>

<div class="card shadow-sm border-0 mb-4">
    <div class="card-body p-4">
        <dl class="row mb-0">
            <dt class="col-sm-4">Phòng</dt><dd class="col-sm-8"><c:out value="${invoice.roomCode}" /></dd>
            <dt class="col-sm-4">Kỳ</dt><dd class="col-sm-8"><c:out value="${invoice.periodDisplay}" /></dd>
            <dt class="col-sm-4">Hạn thanh toán</dt><dd class="col-sm-8"><c:out value="${invoice.dueDateDisplay}" /></dd>
            <c:if test="${not empty invoice.paidAt}">
                <dt class="col-sm-4">Đã thanh toán lúc</dt><dd class="col-sm-8"><c:out value="${invoice.paidAtDisplay}" /></dd>
            </c:if>
        </dl>
    </div>
</div>

<div class="card shadow-sm border-0 mb-4">
    <div class="card-body p-4">
        <h2 class="h5 fw-bold mb-3">Chi tiết từng khoản</h2>
        <div class="table-responsive" tabindex="0" role="region" aria-label="Chi tiết từng khoản">
            <table class="table table-bordered align-middle mb-0">
                <thead class="table-light">
                    <tr>
                        <th>Khoản</th>
                        <th>Số lượng</th>
                        <th class="text-end">Đơn giá</th>
                        <th class="text-end">Thành tiền</th>
                    </tr>
                </thead>
                <tbody>
                    <tr>
                        <td>Tiền phòng</td>
                        <td>1 tháng</td>
                        <td class="text-end text-nowrap"><ui:money value="${invoice.roomFee}" /></td>
                        <td class="text-end text-nowrap"><ui:money value="${invoice.roomFee}" /></td>
                    </tr>
                    <tr>
                        <td>Tiền điện</td>
                        <td>
                            <c:out value="${invoice.electricUsed}" /> kWh
                            <c:if test="${not empty invoice.electricIndex}">
                                <div class="small text-secondary">
                                    Chỉ số <c:out value="${invoice.electricIndex - invoice.electricUsed}" /> &rarr; <c:out value="${invoice.electricIndex}" />
                                </div>
                            </c:if>
                        </td>
                        <td class="text-end text-nowrap"><ui:money value="${price.electricPrice}" />/kWh</td>
                        <td class="text-end text-nowrap"><ui:money value="${invoice.electricFee}" /></td>
                    </tr>
                    <tr>
                        <td>Tiền nước</td>
                        <td>
                            <c:out value="${invoice.waterUsed}" /> m³
                            <c:if test="${not empty invoice.waterIndex}">
                                <div class="small text-secondary">
                                    Chỉ số <c:out value="${invoice.waterIndex - invoice.waterUsed}" /> &rarr; <c:out value="${invoice.waterIndex}" />
                                </div>
                            </c:if>
                        </td>
                        <td class="text-end text-nowrap"><ui:money value="${price.waterPrice}" />/m³</td>
                        <td class="text-end text-nowrap"><ui:money value="${invoice.waterFee}" /></td>
                    </tr>
                    <tr>
                        <td>Phí dịch vụ</td>
                        <td><c:out value="${invoice.numPeople}" /> người</td>
                        <td class="text-end text-nowrap"><ui:money value="${price.serviceFeePerPerson}" />/người</td>
                        <td class="text-end text-nowrap"><ui:money value="${invoice.serviceFee}" /></td>
                    </tr>
                </tbody>
                <tfoot>
                    <tr class="fw-bold">
                        <td colspan="3">Tổng cộng</td>
                        <td class="text-end text-nowrap"><ui:money value="${invoice.total}" /></td>
                    </tr>
                </tfoot>
            </table>
        </div>
    </div>
</div>

<c:if test="${invoice.unpaid}">
    <%-- UC07 (NT-44): POST /tenant/pay tạo lần thử PENDING rồi chuyển sang cổng VNPay. --%>
    <form method="post" action="<c:url value='/tenant/pay' />" class="d-flex flex-wrap align-items-center gap-3">
        <input type="hidden" name="invoiceId" value="${invoice.invoiceId}" />
        <button type="submit" class="btn btn-primary px-4" aria-describedby="pay-note">Thanh toán VNPay</button>
        <span id="pay-note" class="small text-secondary">Bạn sẽ được chuyển sang cổng VNPay để thanh toán.</span>
    </form>
</c:if>

<%@ include file="/WEB-INF/views/common/_bottom.jspf" %>
