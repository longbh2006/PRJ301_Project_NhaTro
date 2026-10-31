<%@ tag pageEncoding="UTF-8" body-content="empty" %>
<%@ attribute name="value" required="true" type="java.lang.String" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="status" value="${fn:toUpperCase(fn:trim(value))}" />
<c:set var="badgeColor" value="secondary" />
<c:set var="badgeLabel" value="Không xác định" />
<c:choose>
    <c:when test="${status eq 'PENDING'}">
        <c:set var="badgeColor" value="warning" />
        <c:set var="badgeLabel" value="Chờ duyệt" />
    </c:when>
    <c:when test="${status eq 'APPROVED'}">
        <c:set var="badgeColor" value="success" />
        <c:set var="badgeLabel" value="Đã duyệt" />
    </c:when>
    <c:when test="${status eq 'ACTIVE'}">
        <c:set var="badgeColor" value="success" />
        <c:set var="badgeLabel" value="Đang hiệu lực" />
    </c:when>
    <c:when test="${status eq 'PAID'}">
        <c:set var="badgeColor" value="success" />
        <c:set var="badgeLabel" value="Đã trả" />
    </c:when>
    <c:when test="${status eq 'REJECTED'}">
        <c:set var="badgeColor" value="danger" />
        <c:set var="badgeLabel" value="Từ chối" />
    </c:when>
    <c:when test="${status eq 'CANCELLED'}">
        <c:set var="badgeColor" value="danger" />
        <c:set var="badgeLabel" value="Đã hủy" />
    </c:when>
    <c:when test="${status eq 'UNPAID'}">
        <c:set var="badgeColor" value="unpaid" />
        <c:set var="badgeLabel" value="Chưa trả" />
    </c:when>
    <c:when test="${status eq 'ENDED'}">
        <c:set var="badgeLabel" value="Đã kết thúc" />
    </c:when>
    <c:when test="${status eq 'OPEN'}">
        <c:set var="badgeColor" value="info" />
        <c:set var="badgeLabel" value="Mới tiếp nhận" />
    </c:when>
    <c:when test="${status eq 'IN_PROGRESS'}">
        <c:set var="badgeColor" value="primary" />
        <c:set var="badgeLabel" value="Đang xử lý" />
    </c:when>
    <c:when test="${status eq 'DONE'}">
        <c:set var="badgeColor" value="success" />
        <c:set var="badgeLabel" value="Đã hoàn thành" />
    </c:when>
    <c:when test="${status eq 'MAINTENANCE'}">
        <c:set var="badgeColor" value="warning" />
        <c:set var="badgeLabel" value="Đang bảo trì" />
    </c:when>
    <c:when test="${status eq 'HIDDEN'}">
        <c:set var="badgeLabel" value="Đã ẩn" />
    </c:when>
</c:choose>
<span class="badge text-bg-${badgeColor}"><c:out value="${badgeLabel}" /></span>
