<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/_taglibs.jspf" %>
<c:set var="pageTitle" value="RoomRent | Báo cáo doanh thu" scope="request" />
<c:set var="activeNav" value="/manager/report" scope="request" />
<%@ include file="/WEB-INF/views/common/_top.jspf" %>

<h1>Báo cáo doanh thu</h1>
<p class="text-secondary">Tiền đã thu tính theo ngày thanh toán (paidAt) của hóa đơn PAID. Nợ quá hạn là hóa đơn UNPAID có hạn trả trước hôm nay, gom theo tháng của hạn trả.</p>
<c:if test="${not empty errorMessage}"><div class="alert alert-danger" role="alert"><c:out value="${errorMessage}" /></div></c:if>

<form method="get" action="<c:url value='/manager/report' />" class="row g-3 mb-4" aria-label="Chọn năm báo cáo">
    <div class="col-sm-4 col-md-3"><label for="report-year" class="form-label">Năm</label>
        <input id="report-year" name="year" type="number" min="${minYear}" max="${maxYear}" required class="form-control" value="${fn:escapeXml(selectedYear)}">
    </div>
    <div class="col-sm-8 col-md-9 d-flex align-items-end flex-wrap gap-2">
        <button type="submit" class="btn btn-primary">Xem báo cáo</button>
        <c:if test="${not empty report}">
            <c:if test="${report.year gt minYear}">
                <c:url var="previousYearUrl" value="/manager/report"><c:param name="year" value="${report.year - 1}" /></c:url>
                <a class="btn btn-outline-secondary" href="${fn:escapeXml(previousYearUrl)}">Năm <c:out value="${report.year - 1}" /></a>
            </c:if>
            <c:if test="${report.year lt maxYear}">
                <c:url var="nextYearUrl" value="/manager/report"><c:param name="year" value="${report.year + 1}" /></c:url>
                <a class="btn btn-outline-secondary" href="${fn:escapeXml(nextYearUrl)}">Năm <c:out value="${report.year + 1}" /></a>
            </c:if>
        </c:if>
    </div>
</form>

<c:if test="${not empty report}">
<section class="row g-3 mb-4" aria-label="Thẻ số liệu">
    <div class="col-6 col-lg"><div class="card roomrent-card h-100"><div class="card-body">
        <p class="small text-secondary mb-1">Phòng đang thuê</p>
        <p class="fs-3 fw-semibold mb-0"><c:out value="${report.occupancy.rentedRooms}" /></p>
    </div></div></div>
    <div class="col-6 col-lg"><div class="card roomrent-card h-100"><div class="card-body">
        <p class="small text-secondary mb-1">Phòng trống</p>
        <p class="fs-3 fw-semibold mb-0"><c:out value="${report.occupancy.vacantRooms}" /></p>
    </div></div></div>
    <div class="col-6 col-lg"><div class="card roomrent-card h-100"><div class="card-body">
        <p class="small text-secondary mb-1">Tỉ lệ lấp đầy</p>
        <p class="fs-3 fw-semibold mb-0"><fmt:formatNumber value="${report.occupancy.occupancyRate}" pattern="0.0" /> %</p>
        <p class="small text-secondary mb-0"><c:out value="${report.occupancy.rentedRooms}" /> / <c:out value="${report.occupancy.openRooms}" /> phòng đang mở cho thuê</p>
    </div></div></div>
    <div class="col-6 col-lg"><div class="card roomrent-card h-100"><div class="card-body">
        <p class="small text-secondary mb-1">Đã thu năm <c:out value="${report.year}" /></p>
        <p class="fs-5 fw-semibold mb-0"><fmt:formatNumber value="${report.paidTotal}" pattern="#,##0" /> đ</p>
    </div></div></div>
    <div class="col-12 col-lg"><div class="card roomrent-card h-100"><div class="card-body">
        <p class="small text-secondary mb-1">Nợ quá hạn năm <c:out value="${report.year}" /></p>
        <p class="fs-5 fw-semibold mb-0 text-danger"><fmt:formatNumber value="${report.overdueTotal}" pattern="#,##0" /> đ</p>
    </div></div></div>
</section>

<div class="card roomrent-card mb-4"><div class="card-body">
    <h2 class="h6 mb-3">Biểu đồ theo tháng — năm <c:out value="${report.year}" /></h2>
    <div style="position: relative; height: 320px;"><canvas id="revenue-chart" role="img" aria-label="Biểu đồ cột tiền đã thu và nợ quá hạn theo tháng"></canvas></div>
</div></div>

<div class="table-responsive"><table id="revenue-table" class="table roomrent-table align-middle">
    <caption class="visually-hidden">Doanh thu và nợ quá hạn 12 tháng năm <c:out value="${report.year}" /></caption>
    <thead><tr><th scope="col">Tháng</th><th scope="col" class="text-end">Đã thu</th><th scope="col" class="text-end">Số hóa đơn đã thu</th>
        <th scope="col" class="text-end">Nợ quá hạn</th><th scope="col" class="text-end">Số hóa đơn quá hạn</th></tr></thead>
    <tbody>
    <c:forEach var="row" items="${report.months}">
        <tr data-month="${row.month}" data-paid="${fn:escapeXml(row.paidTotal)}" data-overdue="${fn:escapeXml(row.overdueTotal)}">
            <th scope="row">Tháng <c:out value="${row.month}" /></th>
            <td class="text-end"><fmt:formatNumber value="${row.paidTotal}" pattern="#,##0" /> đ</td>
            <td class="text-end"><c:out value="${row.paidCount}" /></td>
            <td class="text-end ${row.overdueTotal.signum() gt 0 ? 'text-danger' : ''}"><fmt:formatNumber value="${row.overdueTotal}" pattern="#,##0" /> đ</td>
            <td class="text-end"><c:out value="${row.overdueCount}" /></td>
        </tr>
    </c:forEach>
    </tbody>
    <tfoot class="fw-semibold"><tr>
        <th scope="row">Tổng năm</th>
        <td class="text-end"><fmt:formatNumber value="${report.paidTotal}" pattern="#,##0" /> đ</td>
        <td class="text-end"><c:out value="${report.paidCount}" /></td>
        <td class="text-end"><fmt:formatNumber value="${report.overdueTotal}" pattern="#,##0" /> đ</td>
        <td class="text-end"><c:out value="${report.overdueCount}" /></td>
    </tr></tfoot>
</table></div>
<p class="small text-secondary">Số phòng tính trên phòng có trạng thái ACTIVE; phòng đang thuê là phòng có hợp đồng ACTIVE. Nợ quá hạn tính đến ngày <c:out value="${today}" />.</p>

<script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.6/dist/chart.umd.min.js"
        crossorigin="anonymous"
        integrity="sha384-Sse/HDqcypGpyTDpvZOJNnG0TT3feGQUkF9H+mnRvic+LjR+K1NhTt8f51KIQ3v3"></script>
<script>
    // Biểu đồ đọc số liệu từ chính bảng để hai bên luôn khớp; không có Chart.js thì bảng vẫn đủ thông tin.
    (function () {
        if (typeof Chart === 'undefined') { return; }
        var rows = document.querySelectorAll('#revenue-table tbody tr');
        var labels = [], paid = [], overdue = [];
        rows.forEach(function (row) {
            labels.push('T' + row.dataset.month);
            paid.push(Number(row.dataset.paid));
            overdue.push(Number(row.dataset.overdue));
        });
        var money = new Intl.NumberFormat('vi-VN');
        new Chart(document.getElementById('revenue-chart'), {
            type: 'bar',
            data: {
                labels: labels,
                datasets: [
                    { label: 'Đã thu', data: paid, backgroundColor: '#2563eb' },
                    { label: 'Nợ quá hạn', data: overdue, backgroundColor: '#dc2626' }
                ]
            },
            options: {
                maintainAspectRatio: false,
                scales: { y: { beginAtZero: true, ticks: { callback: function (value) { return money.format(value); } } } },
                plugins: { tooltip: { callbacks: { label: function (ctx) {
                    return ctx.dataset.label + ': ' + money.format(ctx.parsed.y) + ' đ';
                } } } }
            }
        });
    })();
</script>
</c:if>
<%@ include file="/WEB-INF/views/common/_bottom.jspf" %>
