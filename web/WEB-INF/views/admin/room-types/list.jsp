<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/_taglibs.jspf" %>
<c:set var="pageTitle" value="Quản lý Loại phòng" scope="request" />
<c:set var="activeNav" value="/admin/room-types" scope="request" />
<%@ include file="/WEB-INF/views/common/_top.jspf" %>

<div class="container mt-4">
    <div class="d-flex justify-content-between align-items-center mb-3">
        <h1 class="h3 fw-bold">Danh sách Loại phòng</h1>
        <a href="<c:url value='/admin/room-types?action=create'/>" class="btn btn-primary">Thêm loại phòng</a>
    </div>

    <c:if test="${not empty errorMessage}">
        <div class="alert alert-danger"><c:out value="${errorMessage}" /></div>
    </c:if>
    <c:if test="${param.msg == 'saved'}">
        <div class="alert alert-success">Lưu dữ liệu thành công!</div>
    </c:if>
    <c:if test="${param.msg == 'deleted'}">
        <div class="alert alert-success">Xóa dữ liệu thành công!</div>
    </c:if>

    <table class="table table-bordered table-striped">
        <thead class="table-dark">
            <tr>
                <th>ID</th>
                <th>Tên loại phòng</th>
                <th>Sức chứa</th>
                <th class="text-center" style="width: 150px;">Thao tác</th>
            </tr>
        </thead>
        <tbody>
            <c:choose>
                <c:when test="${empty roomTypes}">
                    <tr><td colspan="4" class="text-center">Chưa có dữ liệu</td></tr>
                </c:when>
                <c:otherwise>
                    <c:forEach var="rt" items="${roomTypes}">
                        <tr>
                            <td><c:out value="${rt.roomTypeId}" /></td>
                            <td><c:out value="${rt.name}" /></td>
                            <td><c:out value="${rt.capacity}" /></td>
                            <td class="text-center">
                                <a href="<c:url value='/admin/room-types?action=edit&id=${rt.roomTypeId}'/>" class="btn btn-sm btn-warning">Sửa</a>
                                <form action="<c:url value='/admin/room-types'/>" method="POST" style="display:inline;" onsubmit="return confirm('Bạn có chắc muốn xóa loại phòng này?');">
                                    <input type="hidden" name="action" value="delete" />
                                    <input type="hidden" name="roomTypeId" value="${rt.roomTypeId}" />
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

<%@ include file="/WEB-INF/views/common/_bottom.jspf" %>