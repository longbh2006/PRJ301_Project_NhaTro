<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/_taglibs.jspf" %>
<c:set var="pageTitle" value="RoomRent | Chi tiết sự cố" scope="request" />
<c:set var="activeNav" value="/tenant/issues" scope="request" />
<%@ include file="/WEB-INF/views/common/_top.jspf" %>
<div class="d-flex flex-wrap align-items-center gap-2 mb-3">
    <h1 class="h3 fw-bold mb-0">Sự cố <c:out value="${issue.ticketCode}" /></h1>
    <ui:statusBadge value="${issue.status}" />
</div>
<dl class="row">
    <dt class="col-sm-4">Phòng</dt><dd class="col-sm-8"><c:out value="${issue.roomCode}" /></dd>
    <dt class="col-sm-4">Tiêu đề</dt><dd class="col-sm-8"><c:out value="${issue.title}" /></dd>
    <dt class="col-sm-4">Mô tả</dt>
    <dd class="col-sm-8">
        <c:choose>
            <c:when test="${empty issue.description}"><span class="text-muted">-</span></c:when>
            <c:otherwise><c:out value="${issue.description}" /></c:otherwise>
        </c:choose>
    </dd>
    <dt class="col-sm-4">Ngày gửi</dt><dd class="col-sm-8"><c:out value="${issue.createdAtDisplay}" /></dd>
    <dt class="col-sm-4">Ghi chú của quản lý</dt>
    <dd class="col-sm-8">
        <c:choose>
            <c:when test="${empty issue.managerNote}"><span class="text-muted">-</span></c:when>
            <c:otherwise><c:out value="${issue.managerNote}" /></c:otherwise>
        </c:choose>
    </dd>
    <c:if test="${not empty issue.resolvedAt}">
        <dt class="col-sm-4">Ngày xử lý xong</dt><dd class="col-sm-8"><c:out value="${issue.resolvedAtDisplay}" /></dd>
    </c:if>
</dl>
<a href="<c:url value='/tenant/issues' />">Quay lại danh sách sự cố</a>
<%@ include file="/WEB-INF/views/common/_bottom.jspf" %>
