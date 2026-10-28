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

<%-- ===== Thông báo lỗi ===== --%>
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

    <a class="btn btn-outline-secondary" href="<c:url value='/rooms' />">← Quay lại danh sách</a>
</c:if>

<%@ include file="/WEB-INF/views/common/_bottom.jspf" %>
