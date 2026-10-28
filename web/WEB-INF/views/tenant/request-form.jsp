<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/_taglibs.jspf" %>
<c:set var="pageTitle" value="RoomRent | Đăng ký thuê phòng" scope="request" />
<c:set var="rentalDesign" value="${true}" scope="request" />
<c:set var="activeNav" value="/tenant/requests" scope="request" />
<%@ include file="/WEB-INF/views/common/_top.jspf" %>

<h1 class="rental-page-title">Gửi yêu cầu thuê phòng</h1>
<p class="rental-page-subtitle">Kiểm tra thông tin phòng và điền thông tin thuê trước khi gửi yêu cầu.</p>
<c:if test="${not empty errorMessage}"><div class="alert alert-danger" role="alert"><c:out value="${errorMessage}" /></div></c:if>
<div class="rental-form-layout">
    <section class="rental-summary" aria-labelledby="summary-title">
        <span class="rental-eyebrow">PHÒNG ĐÃ CHỌN</span>
        <h2 id="summary-title">Phòng <c:out value="${room.roomCode}" /></h2>
        <p class="rental-summary-price"><fmt:formatNumber value="${room.monthlyPrice}" pattern="#,##0" />đ <span>/ tháng</span></p>
        <dl class="rental-summary-facts">
            <div><dt>Loại phòng</dt><dd><c:out value="${room.roomTypeName}" /></dd></div>
            <div><dt>Diện tích</dt><dd><fmt:formatNumber value="${room.area}" pattern="0.#" /> m²</dd></div>
            <div><dt>Sức chứa</dt><dd><c:out value="${room.capacity}" /> người</dd></div>
        </dl>
        <p class="rental-help">Yêu cầu sẽ ở trạng thái chờ duyệt sau khi gửi thành công.</p>
    </section>
    <form method="post" action="<c:url value='/tenant/requests' />" class="rental-request-form">
        <h2>Thông tin thuê phòng</h2>
        <input type="hidden" name="roomId" value="${room.roomId}">
        <div class="rental-form-field">
            <label for="moveInDate">Ngày dọn vào <span aria-hidden="true">*</span></label>
            <input class="form-control" id="moveInDate" name="moveInDate" type="date" required
                   min="${today}" value="${fn:escapeXml(param.moveInDate)}" aria-describedby="move-in-help">
            <small id="move-in-help">Chọn ngày từ hôm nay trở đi.</small>
        </div>
        <div class="rental-form-row">
            <div class="rental-form-field"><label for="months">Số tháng thuê <span aria-hidden="true">*</span></label>
                <input class="form-control" id="months" name="months" type="number" min="1" max="24" required
                       value="${empty param.months ? '1' : fn:escapeXml(param.months)}" aria-describedby="months-help">
                <small id="months-help">Từ 1 đến 24 tháng.</small></div>
            <div class="rental-form-field"><label for="numPeople">Số người <span aria-hidden="true">*</span></label>
                <input class="form-control" id="numPeople" name="numPeople" type="number" min="1" max="${room.capacity}" required
                       value="${empty param.numPeople ? '1' : fn:escapeXml(param.numPeople)}" aria-describedby="people-help">
                <small id="people-help">Tối đa <c:out value="${room.capacity}" /> người.</small></div>
        </div>
        <div class="rental-form-field"><label for="note">Ghi chú</label>
            <textarea class="form-control" id="note" name="note" rows="4" maxlength="500" placeholder="Thông tin bạn muốn gửi đến quản lý..."><c:out value="${param.note}" /></textarea></div>
        <div class="rental-form-actions">
            <button class="btn btn-primary" type="submit">Xác nhận gửi yêu cầu</button>
            <c:url var="roomUrl" value="/rooms/detail"><c:param name="roomId" value="${room.roomId}" /></c:url>
            <a class="btn btn-outline-secondary" href="${fn:escapeXml(roomUrl)}">Quay lại</a>
        </div>
    </form>
</div>
<%@ include file="/WEB-INF/views/common/_bottom.jspf" %>
