<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/_taglibs.jspf" %>
<%@ taglib prefix="nt" uri="/WEB-INF/nt" %>
<c:set var="pageTitle" value="RoomRent | Phòng trống" scope="request" />
<c:set var="activeNav" value="/" scope="request" />
<%@ include file="/WEB-INF/views/common/_top.jspf" %>

<div class="mb-4">
    <h1 class="h3 fw-bold mb-1">Phòng trống</h1>
    <p class="text-secondary mb-0">Danh sách các phòng còn trống tại nhà trọ.</p>
</div>

<%-- ===== Thông báo lỗi ===== --%>
<c:if test="${not empty errorMessage}">
    <div class="alert alert-danger" role="alert">
        <c:out value="${errorMessage}" />
    </div>
</c:if>

<%-- ===== Bộ lọc ===== --%>
<section class="card roomrent-card mb-4" aria-labelledby="filter-title">
    <div class="card-body p-4">
        <h2 id="filter-title" class="h6 fw-bold mb-3">Bộ lọc</h2>
        <form method="get" action="<c:url value='/rooms' />" class="row g-3 align-items-end">
            <div class="col-md-3 col-sm-6">
                <label class="form-label" for="filterRoomTypeId">Loại phòng</label>
                <select class="form-select" id="filterRoomTypeId" name="type">
                    <option value="">Tất cả</option>
                    <c:choose>
                        <c:when test="${not empty roomTypes}">
                            <c:forEach var="rt" items="${roomTypes}">
                                <option value="${rt.roomTypeId}" ${filterType eq rt.roomTypeId ? 'selected' : ''}>
                                    <c:out value="${rt.name}"/>
                                </option>
                            </c:forEach>
                        </c:when>
                        <c:otherwise>
                            <option value="1" ${filterType eq 1 ? 'selected' : ''}>Phòng đơn</option>
                            <option value="2" ${filterType eq 2 ? 'selected' : ''}>Phòng đôi</option>
                            <option value="3" ${filterType eq 3 ? 'selected' : ''}>Phòng 4 người</option>
                            <option value="4" ${filterType eq 4 ? 'selected' : ''}>Phòng gác lửng</option>
                        </c:otherwise>
                    </c:choose>
                </select>
            </div>
            <div class="col-md-2 col-sm-6">
                <label class="form-label" for="filterMinPrice">Giá tối thiểu (₫)</label>
                <input type="number" class="form-control" id="filterMinPrice" name="minPrice"
                       min="0" step="100000" placeholder="0"
                       value="<c:out value='${filterMinPrice}' />" />
            </div>
            <div class="col-md-2 col-sm-6">
                <label class="form-label" for="filterMaxPrice">Giá tối đa (₫)</label>
                <input type="number" class="form-control" id="filterMaxPrice" name="maxPrice"
                       min="0" step="100000" placeholder="Không giới hạn"
                       value="<c:out value='${filterMaxPrice}' />" />
            </div>
            <div class="col-md-2 col-sm-6">
                <label class="form-label" for="filterFloor">Tầng</label>
                <select class="form-select" id="filterFloor" name="floor">
                    <option value="">Tất cả</option>
                    <option value="1" ${filterFloor eq 1 ? 'selected' : ''}>Tầng 1</option>
                    <option value="2" ${filterFloor eq 2 ? 'selected' : ''}>Tầng 2</option>
                    <option value="3" ${filterFloor eq 3 ? 'selected' : ''}>Tầng 3</option>
                </select>
            </div>
            <div class="col-md-3 col-sm-12 d-flex gap-2">
                <button type="submit" class="btn btn-primary">Lọc</button>
                <a href="<c:url value='/rooms' />" class="btn btn-outline-secondary">Xóa lọc</a>
            </div>
        </form>
    </div>
</section>

<%-- ===== Bảng danh sách phòng ===== --%>
<c:choose>
    <c:when test="${empty rooms}">
        <c:if test="${empty errorMessage}">
            <div class="alert alert-info" role="status">
                Không tìm thấy phòng nào phù hợp với bộ lọc.
            </div>
        </c:if>
    </c:when>
    <c:otherwise>
        <section class="card roomrent-card mb-4">
            <div class="table-responsive">
                <table class="table roomrent-table align-middle mb-0">
                    <thead>
                        <tr>
                            <th scope="col">Mã phòng</th>
                            <th scope="col">Loại phòng</th>
                            <th scope="col">Tầng</th>
                            <th scope="col" class="text-end">Giá/tháng (₫)</th>
                            <th scope="col" class="text-center">Chi tiết</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="r" items="${rooms}">
                            <tr>
                                <td class="fw-semibold"><c:out value="${r.roomCode}" /></td>
                                <td><c:out value="${r.roomTypeName}" /></td>
                                <td>Tầng <c:out value="${r.floor}" /></td>
                                <td class="text-end">
                                    <fmt:formatNumber value="${r.monthlyPrice}" type="number" groupingUsed="true" />
                                </td>
                                <td class="text-center">
                                    <a class="btn btn-sm roomrent-btn-soft"
                                       href="<c:url value='/rooms/detail'>
                                                 <c:param name='id' value='${r.roomCode}' />
                                             </c:url>">
                                        Xem
                                    </a>
                                </td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </div>
        </section>
    </c:otherwise>
</c:choose>

<%-- ===== Phân trang (giữ bộ lọc qua URL) ===== --%>
<c:url var="basePagerUrl" value="/rooms">
    <c:if test="${not empty filterType}">
        <c:param name="type" value="${filterType}" />
    </c:if>
    <c:if test="${not empty filterMinPrice}">
        <c:param name="minPrice" value="${filterMinPrice}" />
    </c:if>
    <c:if test="${not empty filterMaxPrice}">
        <c:param name="maxPrice" value="${filterMaxPrice}" />
    </c:if>
    <c:if test="${not empty filterFloor}">
        <c:param name="floor" value="${filterFloor}" />
    </c:if>
</c:url>

<nt:pager page="${currentPage}" totalPages="${totalPages}" url="${basePagerUrl}" />

<%@ include file="/WEB-INF/views/common/_bottom.jspf" %>