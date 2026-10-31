<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/_taglibs.jspf" %>
<c:set var="pageTitle" value="Quản lý Đơn Giá" scope="request" />
<c:set var="activeNav" value="/admin/prices" scope="request" />
<%@ include file="/WEB-INF/views/common/_top.jspf" %>

<div class="container mt-4">
    <div class="d-flex justify-content-between align-items-center mb-3">
        <h1 class="h3 fw-bold">Lịch sử Bảng giá điện nước</h1>
        <a href="<c:url value='/admin/prices?action=create'/>" class="btn btn-primary">Thêm Bảng giá mới</a>
    </div>

    <c:if test="${not empty errorMessage}">
        <div class="alert alert-danger"><c:out value="${errorMessage}" /></div>
    </c:if>
    <c:if test="${param.msg == 'saved'}">
        <div class="alert alert-success">Thêm bảng giá mới thành công!</div>
    </c:if>

    <table class="table table-bordered table-striped align-middle">
        <thead class="table-dark">
            <tr>
                <th>ID</th>
                <th class="text-end">Giá Điện (₫/kWh)</th>
                <th class="text-end">Giá Nước (₫/khối)</th>
                <th class="text-end">Phí Dịch vụ (₫/người)</th>
                <th>Ngày hiệu lực</th>
                <th>Trạng thái (Tham khảo)</th>
            </tr>
        </thead>
        <tbody>
            <c:choose>
                <c:when test="${empty prices}">
                    <tr><td colspan="6" class="text-center">Chưa có dữ liệu bảng giá</td></tr>
                </c:when>
                <c:otherwise>
                    <%-- Biến cờ đánh dấu đã tìm thấy bảng giá hiện tại --%>
                    <c:set var="foundCurrent" value="false" />
                    
                    <c:forEach var="up" items="${prices}">
                        <tr>
                            <td><c:out value="${up.priceId}" /></td>
                            <td class="text-end">
                                <fmt:formatNumber value="${up.electricPrice}" type="currency" currencySymbol="₫" maxFractionDigits="0"/>
                            </td>
                            <td class="text-end">
                                <fmt:formatNumber value="${up.waterPrice}" type="currency" currencySymbol="₫" maxFractionDigits="0"/>
                            </td>
                            <td class="text-end">
                                <fmt:formatNumber value="${up.serviceFeePerPerson}" type="currency" currencySymbol="₫" maxFractionDigits="0"/>
                            </td>
                            <td>
                                <strong><c:out value="${up.effectiveFrom}" /></strong>
                            </td>
                            <td>
                                <c:choose>
                                    <%-- Nếu ngày của bảng giá > hôm nay --%>
                                    <c:when test="${up.effectiveFrom > today}">
                                        <ui:statusBadge value="SCHEDULED" />
                                    </c:when>
                                    <%-- Nếu <= hôm nay VÀ chưa tìm thấy bảng hiện tại (vì DS đang sort DESC) --%>
                                    <c:when test="${not foundCurrent}">
                                        <c:set var="foundCurrent" value="true" />
                                        <ui:statusBadge value="CURRENT" />
                                    </c:when>
                                    <%-- Các dòng còn lại --%>
                                    <c:otherwise>
                                        <ui:statusBadge value="HISTORICAL" />
                                    </c:otherwise>
                                </c:choose>
                            </td>
                        </tr>
                    </c:forEach>
                </c:otherwise>
            </c:choose>
        </tbody>
    </table>
</div>

<%@ include file="/WEB-INF/views/common/_bottom.jspf" %>
