<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/_taglibs.jspf" %>
<%@ taglib prefix="nt" uri="/WEB-INF/nt" %>
<c:set var="pageTitle" value="RoomRent | Hóa đơn của tôi" scope="request" />
<c:set var="activeNav" value="/tenant/invoices" scope="request" />
<%@ include file="/WEB-INF/views/common/_top.jspf" %>

<div class="mb-4">
    <h1 class="h3 fw-bold mb-1">Hóa đơn của tôi</h1>
    <p class="text-secondary mb-0">Hóa đơn hằng tháng của các hợp đồng thuê phòng của bạn, kỳ mới nhất trước.</p>
</div>

<div class="card shadow-sm border-0">
    <div class="card-body p-4">
        <div class="table-responsive" tabindex="0" role="region" aria-label="Danh sách hóa đơn">
            <table class="table table-bordered table-striped align-middle mb-0">
                <thead class="table-dark">
                    <tr>
                        <th>Mã HĐơn</th>
                        <th>Kỳ</th>
                        <th>Phòng</th>
                        <th class="text-end">Tiền phòng</th>
                        <th class="text-end">Tiền điện</th>
                        <th class="text-end">Tiền nước</th>
                        <th class="text-end">Tổng tiền</th>
                        <th>Hạn trả</th>
                        <th>Trạng thái</th>
                        <th></th>
                    </tr>
                </thead>
                <tbody>
                    <c:choose>
                        <c:when test="${empty invoices}">
                            <tr><td colspan="10" class="text-center text-muted">Bạn chưa có hóa đơn nào.</td></tr>
                        </c:when>
                        <c:otherwise>
                            <c:forEach var="inv" items="${invoices}">
                                <c:url var="detailUrl" value="/tenant/invoices/detail">
                                    <c:param name="id" value="${inv.invoiceId}" />
                                </c:url>
                                <tr>
                                    <td><strong><c:out value="${inv.code}" /></strong></td>
                                    <td><c:out value="${inv.periodDisplay}" /></td>
                                    <td><c:out value="${inv.roomCode}" /></td>
                                    <td class="text-end text-nowrap"><ui:money value="${inv.roomFee}" /></td>
                                    <td class="text-end text-nowrap"><ui:money value="${inv.electricFee}" /></td>
                                    <td class="text-end text-nowrap"><ui:money value="${inv.waterFee}" /></td>
                                    <td class="text-end text-nowrap fw-bold"><ui:money value="${inv.total}" /></td>
                                    <td><c:out value="${inv.dueDateDisplay}" /></td>
                                    <td class="text-nowrap">
                                        <ui:statusBadge value="${inv.status}" />
                                        <c:if test="${inv.overdue}"><span class="badge text-bg-danger">Quá hạn</span></c:if>
                                    </td>
                                    <td><a class="btn btn-outline-primary btn-sm" href="<c:out value='${detailUrl}' />">Xem</a></td>
                                </tr>
                            </c:forEach>
                        </c:otherwise>
                    </c:choose>
                </tbody>
            </table>
        </div>

        <c:url var="basePagerUrl" value="/tenant/invoices" />
        <nt:pager page="${currentPage}" totalPages="${totalPages}" url="${basePagerUrl}" />
    </div>
</div>

<%@ include file="/WEB-INF/views/common/_bottom.jspf" %>
