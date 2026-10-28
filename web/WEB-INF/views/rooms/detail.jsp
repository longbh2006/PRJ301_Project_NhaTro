<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/_taglibs.jspf" %>
<c:set var="pageTitle" value="RoomRent | Chi tiết phòng" scope="request" />
<c:set var="layoutVariant" value="public" scope="request" />
<c:set var="rentalDesign" value="${true}" scope="request" />
<%@ include file="/WEB-INF/views/common/_top.jspf" %>

<h1 class="rental-page-title">Chi tiết phòng</h1>
<section class="rental-room-card" aria-labelledby="room-title">
    <div class="rental-gallery">
        <div class="rental-room-image">
            <c:choose>
                <c:when test="${not empty room.imageUrl}">
                    <img src="${fn:escapeXml(room.imageUrl)}" alt="Phòng ${fn:escapeXml(room.roomCode)}">
                </c:when>
                <c:otherwise><span>ẢNH PHÒNG TRỌ</span></c:otherwise>
            </c:choose>
        </div>
        <div class="rental-image-previews" aria-hidden="true">
            <span>Ảnh 1</span><span>Ảnh 2</span><span>Ảnh 3</span>
        </div>
    </div>
    <div class="rental-room-info">
        <div class="rental-room-heading">
            <h2 id="room-title">Phòng <c:out value="${room.roomCode}" /></h2>
            <c:choose>
                <c:when test="${room.occupied}"><span class="rental-status rental-status-muted">Đã có người thuê</span></c:when>
                <c:when test="${room.status ne 'ACTIVE'}"><span class="rental-status rental-status-muted">Tạm ngừng</span></c:when>
                <c:otherwise><span class="rental-status rental-status-approved">Còn trống</span></c:otherwise>
            </c:choose>
        </div>
        <p class="rental-price"><fmt:formatNumber value="${room.monthlyPrice}" pattern="#,##0" />đ <span>/ tháng</span></p>
        <dl class="rental-room-facts">
            <div><dt>Diện tích:</dt><dd><fmt:formatNumber value="${room.area}" pattern="0.#" /> m²</dd></div>
            <div><dt>Sức chứa:</dt><dd><c:out value="${room.capacity}" /> người</dd></div>
            <div><dt>Tiền cọc:</dt><dd>Theo hợp đồng</dd></div>
            <div><dt>Loại phòng:</dt><dd><c:out value="${room.roomTypeName}" /></dd></div>
        </dl>
        <p class="rental-description">Mô tả: <c:out value="${room.description}" default="Chưa có mô tả." /></p>
        <c:if test="${room.occupied or room.status ne 'ACTIVE'}">
            <p class="rental-notice" role="status"><c:choose>
                <c:when test="${room.occupied}">Phòng đã có người thuê.</c:when>
                <c:otherwise>Phòng hiện không mở cho thuê.</c:otherwise>
            </c:choose></p>
        </c:if>
        <div class="rental-room-actions">
            <c:choose>
                <c:when test="${room.occupied or room.status ne 'ACTIVE'}">
                    <button class="btn btn-primary" type="button" disabled>Gửi yêu cầu thuê phòng này</button>
                </c:when>
                <c:when test="${sessionScope.role eq 'TENANT'}">
                    <c:url var="requestUrl" value="/tenant/requests"><c:param name="roomId" value="${room.roomId}" /></c:url>
                    <a class="btn btn-primary" href="${fn:escapeXml(requestUrl)}">Gửi yêu cầu thuê phòng này</a>
                </c:when>
                <c:when test="${empty sessionScope.accountId}">
                    <a class="btn btn-primary" href="<c:url value='/login' />">Gửi yêu cầu thuê phòng này</a>
                </c:when>
                <c:otherwise><button class="btn btn-primary" type="button" disabled>Dành cho người thuê</button></c:otherwise>
            </c:choose>
            <a class="btn btn-outline-secondary" href="<c:url value='/rooms' />">Quay lại</a>
        </div>
        <c:if test="${empty sessionScope.accountId}"><p class="rental-help">Vui lòng đăng nhập bằng tài khoản người thuê để đăng ký thuê phòng.</p></c:if>
    </div>
</section>
<%@ include file="/WEB-INF/views/common/_bottom.jspf" %>