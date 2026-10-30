<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/_taglibs.jspf" %>
<c:set var="pageTitle" value="RoomRent | Phòng ${room.roomCode}" scope="request" />
<%@ include file="/WEB-INF/views/common/_top.jspf" %>

<nav aria-label="breadcrumb" class="mb-3">
    <ol class="breadcrumb mb-0">
        <li class="breadcrumb-item"><a href="<c:url value='/rooms' />">Phòng trống</a></li>
        <li class="breadcrumb-item active" aria-current="page"><c:out value="${room.roomCode}" /></li>
    </ol>
</nav>

<c:if test="${not empty errorMessage}">
    <div class="alert alert-danger" role="alert">
        <c:out value="${errorMessage}" />
    </div>
</c:if>

<c:if test="${not empty room}">
    <section class="card roomrent-card mb-4" aria-labelledby="detail-title">
        <div class="card-body p-4">
            <h1 id="detail-title" class="h4 fw-bold mb-4">
                Phòng <c:out value="${room.roomCode}" />
            </h1>

            <dl class="row roomrent-details mb-0">
                <dt class="col-sm-4">Mã phòng</dt>
                <dd class="col-sm-8"><c:out value="${room.roomCode}" /></dd>

                <dt class="col-sm-4">Loại phòng</dt>
                <dd class="col-sm-8"><c:out value="${room.roomTypeName}" /></dd>

                <dt class="col-sm-4">Sức chứa</dt>
                <dd class="col-sm-8"><c:out value="${room.capacity}" /> người</dd>

                <dt class="col-sm-4">Tầng</dt>
                <dd class="col-sm-8">Tầng <c:out value="${room.floor}" /></dd>

                <dt class="col-sm-4">Diện tích</dt>
                <dd class="col-sm-8">
                    <c:choose>
                        <c:when test="${room.area > 0}">
                            <fmt:formatNumber value="${room.area}" maxFractionDigits="1" /> m²
                        </c:when>
                        <c:otherwise>Chưa cập nhật</c:otherwise>
                    </c:choose>
                </dd>

                <dt class="col-sm-4">Giá thuê/tháng</dt>
                <dd class="col-sm-8">
                    <fmt:formatNumber value="${room.monthlyPrice}" type="number" groupingUsed="true" /> ₫
                </dd>

                <dt class="col-sm-4">Trạng thái</dt>
                <dd class="col-sm-8">
                    <c:choose>
                        <c:when test="${room.status eq 'ACTIVE'}">
                            <span class="badge text-bg-success">Đang mở</span>
                        </c:when>
                        <c:when test="${room.status eq 'MAINTENANCE'}">
                            <span class="badge text-bg-warning">Đang bảo trì</span>
                        </c:when>
                        <c:otherwise>
                            <span class="badge text-bg-secondary"><c:out value="${room.status}" /></span>
                        </c:otherwise>
                    </c:choose>
                </dd>

                <c:if test="${not empty room.description}">
                    <dt class="col-sm-4">Mô tả</dt>
                    <dd class="col-sm-8"><c:out value="${room.description}" /></dd>
                </c:if>
            </dl>
        </div>
    </section>

    <c:choose>
        <c:when test="${room.status ne 'ACTIVE'}">
            <button class="btn btn-primary mb-3" type="button" disabled>Đăng ký thuê</button>
            <p class="text-secondary">Phòng hiện không mở cho thuê.</p>
        </c:when>
        <c:when test="${sessionScope.role eq 'TENANT'}">
            <button class="btn btn-primary mb-3" type="button" data-bs-toggle="collapse"
                    data-bs-target="#rental-request-form" aria-expanded="${not empty errorMessage}"
                    aria-controls="rental-request-form">Đăng ký thuê</button>

            <section id="rental-request-form" class="collapse ${not empty errorMessage ? 'show' : ''}">
                <div class="card roomrent-card mb-4">
                    <div class="card-body p-4">
                        <h2 class="h5 fw-bold mb-4">Thông tin đăng ký thuê</h2>
                        <form method="post" action="<c:url value='/tenant/requests' />">
                            <input type="hidden" name="roomId" value="${room.roomId}">

                            <div class="mb-3">
                                <label class="form-label" for="moveInDate">Ngày dọn vào</label>
                                <input class="form-control" id="moveInDate" name="moveInDate" type="date"
                                       value="${fn:escapeXml(param.moveInDate)}" required>
                            </div>

                            <div class="row g-3 mb-3">
                                <div class="col-md-6">
                                    <label class="form-label" for="months">Số tháng thuê</label>
                                    <input class="form-control" id="months" name="months" type="number"
                                           min="1" max="24" value="${empty param.months ? '1' : fn:escapeXml(param.months)}" required>
                                </div>
                                <div class="col-md-6">
                                    <label class="form-label" for="numPeople">Số người</label>
                                    <input class="form-control" id="numPeople" name="numPeople" type="number"
                                           min="1" max="${room.capacity}"
                                           value="${empty param.numPeople ? '1' : fn:escapeXml(param.numPeople)}" required>
                                </div>
                            </div>

                            <div class="mb-3">
                                <label class="form-label" for="note">Ghi chú</label>
                                <textarea class="form-control" id="note" name="note" rows="4" maxlength="500"><c:out value="${param.note}" /></textarea>
                            </div>

                            <button class="btn btn-primary" type="submit">Gửi yêu cầu thuê</button>
                        </form>
                    </div>
                </div>
            </section>
        </c:when>
        <c:when test="${empty sessionScope.accountId}">
            <a class="btn btn-primary mb-3" href="<c:url value='/login' />">Đăng ký thuê</a>
            <p class="text-secondary">Vui lòng đăng nhập bằng tài khoản người thuê để đăng ký.</p>
        </c:when>
        <c:otherwise>
            <button class="btn btn-primary mb-3" type="button" disabled>Đăng ký thuê</button>
            <p class="text-secondary">Chức năng này chỉ dành cho người thuê.</p>
        </c:otherwise>
    </c:choose>

    <a class="btn btn-outline-secondary" href="<c:url value='/rooms' />">← Quay lại danh sách</a>
</c:if>

<%@ include file="/WEB-INF/views/common/_bottom.jspf" %>
