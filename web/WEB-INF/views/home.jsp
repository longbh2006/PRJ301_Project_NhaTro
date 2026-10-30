<%@ page pageEncoding="UTF-8" %>
<%-- Include file _taglibs.jspf để nạp JSTL (c, fmt, fn) và custom tag chung (rr) --%>
<%@ include file="/WEB-INF/views/common/_taglibs.jspf" %>

<%-- Thêm scope="request" để _top.jspf có thể đọc được thông qua requestScope --%>
<c:set var="pageTitle" value="Trang chủ - Hệ thống quản lý Nhà trọ" scope="request" />
<c:set var="activeNav" value="/" scope="request" />
<%@ include file="/WEB-INF/views/common/_top.jspf" %>

<div class="container mt-4">
    <h2 class="mb-4">Phòng trống nổi bật</h2>

    <%-- Hiện errorMessage nếu có (khi servlet bắt lỗi CSDL) --%>
    <c:if test="${not empty requestScope.errorMessage}">
        <div class="alert alert-danger" role="alert">
            <strong>Lỗi:</strong> <c:out value="${requestScope.errorMessage}" />
        </div>
    </c:if>

    <%-- Xử lý hiển thị danh sách phòng hoặc thông báo rỗng --%>
    <c:choose>
        <%-- Nếu danh sách rỗng (và không phải do lỗi hệ thống) --%>
        <c:when test="${empty requestScope.rooms and empty requestScope.errorMessage}">
            <div class="alert alert-info" role="alert">
                Hiện tại hệ thống chưa có phòng trống nào.
            </div>
        </c:when>

        <%-- Nếu có dữ liệu phòng --%>
        <c:otherwise>
            <div class="row">
                <%-- Lặp qua danh sách rooms bằng c:forEach và hiển thị dạng Card Bootstrap --%>
                <c:forEach var="room" items="${requestScope.rooms}">
                    <div class="col-md-4 mb-4">
                        <div class="card h-100 shadow-sm">
                            <div class="card-body">
                                <h5 class="card-title text-primary">
                                    Phòng <c:out value="${room.roomCode}" />
                                </h5>
                                <p class="card-text">
                                    <strong>Loại phòng:</strong> <c:out value="${room.roomTypeName}" /><br/>
                                    <strong>Tầng:</strong> <c:out value="${room.floor}" /><br/>
                                    <strong>Giá thuê:</strong> 
                                    <span class="text-danger fw-bold">
                                        <fmt:formatNumber value="${room.monthlyPrice}" type="currency" currencySymbol="VNĐ" maxFractionDigits="0"/>
                                    </span>
                                </p>
                                
                                <%-- Nút Xem chi tiết tạo bằng c:url và c:param --%>
                                <c:url var="detailUrl" value="/rooms/detail">
                                    <c:param name="id" value="${room.roomCode}" />
                                </c:url>
                                <a href="${detailUrl}" class="btn btn-outline-primary w-100">Xem chi tiết</a>
                            </div>
                        </div>
                    </div>
                </c:forEach>
            </div>
        </c:otherwise>
    </c:choose>
</div>

<%@ include file="/WEB-INF/views/common/_bottom.jspf" %>