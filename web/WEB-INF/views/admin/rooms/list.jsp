<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/_taglibs.jspf" %>
<c:set var="pageTitle" value="Quản lý Phòng" scope="request" />
<c:set var="activeNav" value="/admin/rooms" scope="request" />
<%@ include file="/WEB-INF/views/common/_top.jspf" %>

<div class="container mt-4">
    <div class="d-flex flex-wrap justify-content-between align-items-center gap-3 mb-3">
        <h1 class="h3 fw-bold">Danh sách Phòng</h1>
        <a href="<c:url value='/admin/rooms?action=create'/>" class="btn btn-primary">Thêm phòng mới</a>
    </div>

    <c:if test="${not empty errorMessage}">
        <div class="alert alert-danger"><c:out value="${errorMessage}" /></div>
    </c:if>
    <c:if test="${param.msg == 'saved'}">
        <div class="alert alert-success">Lưu dữ liệu phòng thành công!</div>
    </c:if>
    <c:if test="${param.msg == 'deleted'}">
        <div class="alert alert-success">Xóa phòng thành công!</div>
    </c:if>

    <div class="table-responsive" role="region" aria-label="Danh sách phòng" tabindex="0">
        <table class="table table-bordered table-striped align-middle">
            <thead class="table-dark">
                <tr>
                    <th>Mã phòng</th>
                    <th>Loại phòng</th>
                    <th>Tầng</th>
                    <th>Diện tích</th>
                    <th>Giá tháng</th>
                    <th>Trạng thái</th>
                    <th class="text-center" style="width: 150px;">Thao tác</th>
                </tr>
            </thead>
            <tbody>
                <c:choose>
                    <c:when test="${empty rooms}">
                        <tr><td colspan="7" class="text-center">Chưa có dữ liệu</td></tr>
                    </c:when>
                    <c:otherwise>
                        <c:forEach var="room" items="${rooms}">
                            <tr>
                                <td><strong><c:out value="${room.roomCode}" /></strong></td>
                                <td><c:out value="${room.roomTypeName}" /></td>
                                <td><c:out value="${room.floor}" /></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${room.area > 0}"><c:out value="${room.area}" /> m²</c:when>
                                        <c:otherwise><span class="text-muted">N/A</span></c:otherwise>
                                    </c:choose>
                                </td>
                                <td>
                                    <fmt:formatNumber value="${room.monthlyPrice}" type="currency" currencySymbol="₫" maxFractionDigits="0"/>
                                </td>
                                <td>
                                    <ui:statusBadge value="${room.status}" />
                                </td>
                                <td class="text-center">
                                    <a href="<c:url value='/admin/rooms?action=edit&id=${room.roomId}'/>" class="btn btn-sm btn-warning">Sửa</a>
                                    <form action="<c:url value='/admin/rooms'/>" method="POST" style="display:inline;" onsubmit="return confirm('Bạn có chắc muốn xóa/ẩn phòng này?');">
                                        <input type="hidden" name="action" value="delete" />
                                        <input type="hidden" name="roomId" value="${room.roomId}" />
                                        <button type="submit" class="btn btn-sm btn-danger">Xóa</button>
                                    </form>
                                </td>
                            </tr>
                        </c:forEach>
                    </c:otherwise>
                </c:choose>
            </tbody>
        </table>
    </div>
</div>

<%@ include file="/WEB-INF/views/common/_bottom.jspf" %>
