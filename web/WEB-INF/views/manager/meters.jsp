<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/_taglibs.jspf" %>
<c:set var="pageTitle" value="RoomRent | Ghi chỉ số điện nước" scope="request" />
<c:set var="activeNav" value="/manager/meters" scope="request" />
<%@ include file="/WEB-INF/views/common/_top.jspf" %>

<c:set var="periodLabel" value="${sheet.month lt 10 ? '0' : ''}${sheet.month}/${sheet.year}" />
<div class="mb-4">
    <h1 class="h3 fw-bold">Ghi chỉ số điện nước</h1>
    <p class="text-secondary">Nhập kỳ, lưu chỉ số; chỉ tạo hóa đơn cho phòng có hợp đồng hiệu lực.</p>
</div>
<c:if test="${not empty requestScope.successMessage}">
    <div class="alert alert-success" role="status"><c:out value="${requestScope.successMessage}" /></div>
</c:if>
<c:if test="${not empty requestScope.warningMessage}">
    <div class="alert alert-warning" role="status"><c:out value="${requestScope.warningMessage}" /></div>
</c:if>
<c:if test="${not empty requestScope.errorMessages}">
    <div class="alert alert-danger" role="alert">
        <c:choose>
            <c:when test="${fn:length(requestScope.errorMessages) eq 1}">
                <c:out value="${requestScope.errorMessages[0]}" />
            </c:when>
            <c:otherwise>
                <p class="fw-semibold mb-2">Chưa lưu được chỉ số:</p>
                <ul class="mb-0">
                    <c:forEach var="message" items="${requestScope.errorMessages}">
                        <li><c:out value="${message}" /></li>
                    </c:forEach>
                </ul>
            </c:otherwise>
        </c:choose>
    </div>
</c:if>

<div class="card roomrent-card mb-4">
    <div class="card-body p-4">
        <form method="get" action="<c:url value='/manager/meters' />" class="row g-3 align-items-end" aria-label="Chọn kỳ ghi số">
            <div class="col-md-4 col-lg-3">
                <span class="form-label d-block" id="period-label">Kỳ ghi số</span>
                <div class="input-group" role="group" aria-labelledby="period-label">
                    <select id="period-month" name="month" class="form-select" aria-label="Tháng">
                        <c:forEach var="m" begin="1" end="12">
                            <option value="${m}" ${m eq sheet.month ? 'selected' : ''}>Tháng <c:out value="${m}" /></option>
                        </c:forEach>
                    </select>
                    <input id="period-year" name="year" type="number" class="form-control" aria-label="Năm"
                           min="${requestScope.minYear}" max="${requestScope.maxYear}" step="1" required
                           value="${fn:escapeXml(sheet.year)}">
                </div>
            </div>
            <div class="col-md-3">
                <label for="room-filter" class="form-label">Phòng</label>
                <select id="room-filter" name="roomId" class="form-select">
                    <option value="">Tất cả</option>
                    <c:forEach var="row" items="${sheet.rows}">
                        <option value="${row.contract.roomId}" ${row.contract.roomId eq requestScope.roomId ? 'selected' : ''}>
                            <c:out value="${row.contract.roomCode}" />
                        </option>
                    </c:forEach>
                </select>
            </div>
            <div class="col-6 col-md-2">
                <button type="submit" class="btn btn-primary w-100">Lọc</button>
            </div>
            <div class="col-6 col-md">
                <button type="submit" form="meter-form" class="btn btn-primary w-100"
                        ${sheet.editable ? '' : 'disabled'}>Lưu chỉ số</button>
            </div>
        </form>
    </div>
</div>

<p class="small text-secondary">
    Tháng <c:out value="${periodLabel}" />: <c:out value="${fn:length(sheet.rows)}" /> phòng có hợp đồng hiệu lực,
    đã ghi <c:out value="${sheet.recordedCount}" />, đã lập hoá đơn <c:out value="${sheet.invoicedCount}" />.
</p>
<form id="meter-form" method="post" action="<c:url value='/manager/meters' />" novalidate>
    <input type="hidden" name="csrfToken" value="${fn:escapeXml(requestScope.csrfToken)}">
    <input type="hidden" name="action" value="save">
    <input type="hidden" name="year" value="${fn:escapeXml(sheet.year)}">
    <input type="hidden" name="month" value="${fn:escapeXml(sheet.month)}">
    <input type="hidden" name="roomId" value="${fn:escapeXml(requestScope.roomId)}">
    <div class="table-responsive">
        <table class="table roomrent-table align-middle">
            <caption class="visually-hidden">Chỉ số điện nước tháng <c:out value="${periodLabel}" /></caption>
            <thead>
                <tr>
                    <th scope="col">Phòng</th>
                    <th scope="col">Điện cũ</th>
                    <th scope="col">Điện mới *</th>
                    <th scope="col">Tiêu thụ</th>
                    <th scope="col">Nước cũ</th>
                    <th scope="col">Nước mới *</th>
                    <th scope="col">Tiêu thụ</th>
                </tr>
            </thead>
            <tbody>
                <c:set var="visibleRows" value="0" />
                <c:forEach var="row" items="${sheet.rows}">
                    <c:if test="${empty requestScope.roomId or row.contract.roomId eq requestScope.roomId}">
                        <c:set var="visibleRows" value="${visibleRows + 1}" />
                        <c:set var="roomKey" value="${row.contract.roomId}" />
                        <tr>
                            <th scope="row">
                                <c:out value="${row.contract.roomCode}" />
                                <div class="small fw-normal text-secondary"><c:out value="${row.contract.tenantName}" /></div>
                                <c:choose>
                                    <c:when test="${row.invoiced}"><span class="badge text-bg-success mt-1">Đã lập hoá đơn</span></c:when>
                                    <c:when test="${row.recorded}"><span class="badge text-bg-info mt-1">Đã ghi</span></c:when>
                                </c:choose>
                            </th>
                            <c:choose>
                                <c:when test="${row.recorded}">
                                    <td><c:out value="${row.oldElectric}" default="—" /></td>
                                    <td><c:out value="${row.current.electricIndex}" /></td>
                                    <td><c:out value="${row.electricUsed}" default="—" /> kWh</td>
                                    <td><c:out value="${row.oldWater}" default="—" /></td>
                                    <td><c:out value="${row.current.waterIndex}" /></td>
                                    <td><c:out value="${row.waterUsed}" default="—" /> m³</td>
                                </c:when>
                                <c:when test="${row.oldIndexKnown}">
                                    <c:set var="electricName" value="electric_${roomKey}" />
                                    <c:set var="waterName" value="water_${roomKey}" />
                                    <td><c:out value="${row.oldElectric}" /></td>
                                    <td>
                                        <input type="hidden" name="meterRoomId" value="${roomKey}">
                                        <input type="number" class="form-control form-control-sm" name="${electricName}"
                                               min="${row.oldElectric}" max="999999999" step="1" inputmode="numeric"
                                               aria-label="Chỉ số điện mới phòng ${fn:escapeXml(row.contract.roomCode)}"
                                               value="${fn:escapeXml(param[electricName])}">
                                    </td>
                                    <td class="text-secondary">—</td>
                                    <td><c:out value="${row.oldWater}" /></td>
                                    <td>
                                        <input type="number" class="form-control form-control-sm" name="${waterName}"
                                               min="${row.oldWater}" max="999999999" step="1" inputmode="numeric"
                                               aria-label="Chỉ số nước mới phòng ${fn:escapeXml(row.contract.roomCode)}"
                                               value="${fn:escapeXml(param[waterName])}">
                                    </td>
                                    <td class="text-secondary">—</td>
                                </c:when>
                                <c:otherwise>
                                    <td colspan="6" class="text-secondary">
                                        Chưa có chỉ số tháng trước. Hãy ghi bù tháng trước rồi mới ghi tháng này.
                                    </td>
                                </c:otherwise>
                            </c:choose>
                        </tr>
                    </c:if>
                </c:forEach>
                <c:if test="${visibleRows eq 0}">
                    <tr><td colspan="7" class="text-center text-secondary py-4">Không có phòng nào có hợp đồng hiệu lực trong kỳ này.</td></tr>
                </c:if>
            </tbody>
        </table>
    </div>
</form>

<div class="card roomrent-card bg-light mb-5">
    <div class="card-body p-4">
        <p class="fw-semibold mb-3">
            <c:choose>
                <c:when test="${not empty sheet.price}">
                    Giá điện: <fmt:formatNumber value="${sheet.price.electricPrice}" pattern="#,##0" /> đ/kWh;
                    nước: <fmt:formatNumber value="${sheet.price.waterPrice}" pattern="#,##0" /> đ/m³;
                    dịch vụ: <fmt:formatNumber value="${sheet.price.serviceFeePerPerson}" pattern="#,##0" /> đ/người
                    (áp dụng từ <c:out value="${sheet.price.effectiveFrom}" />).
                </c:when>
                <c:otherwise>Chưa có bảng giá điện nước hiệu lực cho tháng <c:out value="${periodLabel}" />.</c:otherwise>
            </c:choose>
            Chỉ số mới ≥ cũ; mỗi phòng/kỳ chỉ lưu 1 bản. Phòng trống không lập hóa đơn.
        </p>
        <form method="post" action="<c:url value='/manager/meters' />" class="m-0"
              onsubmit="return confirm('Lập hoá đơn tháng ${periodLabel} cho các phòng đã đủ chỉ số?');">
            <input type="hidden" name="csrfToken" value="${fn:escapeXml(requestScope.csrfToken)}">
            <input type="hidden" name="action" value="generate">
            <input type="hidden" name="year" value="${fn:escapeXml(sheet.year)}">
            <input type="hidden" name="month" value="${fn:escapeXml(sheet.month)}">
            <input type="hidden" name="roomId" value="${fn:escapeXml(requestScope.roomId)}">
            <button type="submit" class="btn roomrent-btn-soft">Lập hoá đơn tháng <c:out value="${periodLabel}" /></button>
        </form>
    </div>
</div>

<h2 class="h5 fw-bold">Hoá đơn tháng <c:out value="${periodLabel}" /></h2>
<c:choose>
    <c:when test="${empty sheet.invoices}">
        <div class="card roomrent-card"><div class="card-body p-4 text-secondary">Chưa lập hoá đơn cho tháng này.</div></div>
    </c:when>
    <c:otherwise>
        <div class="table-responsive">
            <table class="table roomrent-table align-middle">
                <caption class="visually-hidden">Hoá đơn tháng <c:out value="${periodLabel}" /></caption>
                <thead>
                    <tr>
                        <th scope="col">Mã</th>
                        <th scope="col">Phòng</th>
                        <th scope="col">Tiền phòng</th>
                        <th scope="col">Điện</th>
                        <th scope="col">Nước</th>
                        <th scope="col">Dịch vụ</th>
                        <th scope="col">Tổng tiền</th>
                        <th scope="col">Hạn trả</th>
                        <th scope="col">Trạng thái</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach var="invoice" items="${sheet.invoices}">
                        <tr>
                            <th scope="row">INV<c:out value="${invoice.invoiceId}" /></th>
                            <td>
                                <c:out value="${invoice.roomCode}" />
                                <div class="small text-secondary"><c:out value="${invoice.tenantName}" /></div>
                            </td>
                            <td><fmt:formatNumber value="${invoice.roomFee}" pattern="#,##0" /> đ</td>
                            <td>
                                <fmt:formatNumber value="${invoice.electricFee}" pattern="#,##0" /> đ
                                <div class="small text-secondary"><c:out value="${invoice.electricUsed}" /> kWh</div>
                            </td>
                            <td>
                                <fmt:formatNumber value="${invoice.waterFee}" pattern="#,##0" /> đ
                                <div class="small text-secondary"><c:out value="${invoice.waterUsed}" /> m³</div>
                            </td>
                            <td><fmt:formatNumber value="${invoice.serviceFee}" pattern="#,##0" /> đ</td>
                            <td class="fw-semibold"><fmt:formatNumber value="${invoice.total}" pattern="#,##0" /> đ</td>
                            <td><c:out value="${invoice.dueDate}" /></td>
                            <td><ui:statusBadge value="${invoice.status}" /></td>
                        </tr>
                    </c:forEach>
                    <tr>
                        <th scope="row" colspan="6" class="text-end">Tổng cộng <c:out value="${fn:length(sheet.invoices)}" /> hoá đơn</th>
                        <td class="fw-bold"><fmt:formatNumber value="${sheet.invoiceTotal}" pattern="#,##0" /> đ</td>
                        <td colspan="2"></td>
                    </tr>
                </tbody>
            </table>
        </div>
    </c:otherwise>
</c:choose>

<%@ include file="/WEB-INF/views/common/_bottom.jspf" %>
