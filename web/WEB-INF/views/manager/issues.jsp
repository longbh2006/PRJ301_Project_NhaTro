<%@page pageEncoding="UTF-8"%>
<%@include file="/WEB-INF/views/common/_taglibs.jspf" %>

<c:set var="pageTitle" value="RoomRent | Xử lý sự cố" scope="request" />
<c:set var="activeNav" value="/manager/issues" scope="request" />
<%@include file="/WEB-INF/views/common/_top.jspf" %>

<div class="container-fluid mt-4">
    <h2 class="mb-4">Quản lý sự cố</h2>

    <c:if test="${not empty sessionScope.successMessage}">
        <div class="alert alert-success alert-dismissible fade show" role="alert">
            <c:out value="${sessionScope.successMessage}" />
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        </div>
        <c:remove var="successMessage" scope="session" />
    </c:if>
    <c:if test="${not empty errorMessage}">
        <div class="alert alert-danger alert-dismissible fade show" role="alert">
            <c:out value="${errorMessage}" />
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        </div>
    </c:if>

    <%-- Đã sửa class form-inline thành d-flex của Bootstrap 5 --%>
    <form action="<c:url value='/manager/issues'/>" method="GET" class="d-flex align-items-center gap-2 mb-4">
        <select name="status" class="form-select w-auto">
            <option value="">-- Tất cả trạng thái --</option>
            <option value="OPEN" ${param.status == 'OPEN' ? 'selected' : ''}>Mới tiếp nhận</option>
            <option value="IN_PROGRESS" ${param.status == 'IN_PROGRESS' ? 'selected' : ''}>Đang xử lý</option>
            <option value="DONE" ${param.status == 'DONE' ? 'selected' : ''}>Đã hoàn thành</option>
        </select>
        <input type="number" name="roomId" class="form-control w-auto" placeholder="Mã phòng (ID)..." value="${fn:escapeXml(param.roomId)}" />
        <button type="submit" class="btn btn-primary">Lọc</button>
    </form>

    <c:choose>
        <c:when test="${empty issues and empty errorMessage}">
            <div class="alert alert-info">Không tìm thấy sự cố nào phù hợp.</div>
        </c:when>
        <c:when test="${not empty issues}">
            <%-- Thêm table-responsive để bảng có thể cuộn ngang nếu màn hình nhỏ, tránh bóp chữ --%>
            <div class="table-responsive">
                <table class="table table-bordered table-striped table-hover align-middle">
                    <%-- Sửa thead-dark thành table-dark --%>
                    <thead class="table-dark">
                        <tr>
                            <%-- Thêm text-nowrap để các cột nhỏ không bị rớt dòng --%>
                            <th class="text-nowrap">Mã SC</th>
                            <th class="text-nowrap">Phòng</th>
                            <th class="text-nowrap">Người thuê</th>
                            <th>Tiêu đề / Mô tả</th>
                            <th class="text-nowrap">Trạng thái</th>
                            <th class="text-nowrap">Ngày tạo</th>
                            <th style="min-width: 150px;">Ghi chú QL</th>
                            <th style="min-width: 140px;">Thao tác</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="issue" items="${issues}">
                            <tr>
                                <td class="text-nowrap"><c:out value="${issue.ticketCode}" /></td>
                                <td class="text-nowrap"><c:out value="${issue.roomCode}" /></td>
                                <td class="text-nowrap"><c:out value="${issue.tenantName}" /></td>
                                <td>
                                    <strong><c:out value="${issue.title}" /></strong><br/>
                                    <small class="text-muted"><c:out value="${issue.description}" /></small>
                                </td>
                                <td class="text-nowrap"><ui:statusBadge value="${issue.status}" /></td>
                                <td class="text-nowrap"><c:out value="${issue.createdAtDisplay}" /></td>
                                <td><c:out value="${issue.managerNote}" /></td>
                                
                                <td>
                                    <c:choose>
                                        <c:when test="${issue.status == 'OPEN'}">
                                            <form action="<c:url value='/manager/issues'/>" method="POST">
                                                <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}" />
                                                <input type="hidden" name="ticketId" value="${issue.ticketId}" />
                                                <input type="hidden" name="currentStatus" value="${issue.status}" />
                                                <input type="hidden" name="newStatus" value="IN_PROGRESS" />
                                                <textarea name="managerNote" class="form-control form-control-sm mb-2" maxlength="500" placeholder="Ghi chú (tuỳ chọn)"></textarea>
                                                <button type="submit" class="btn btn-warning btn-sm w-100">Chuyển Đang xử lý</button>
                                            </form>
                                        </c:when>
                                        <c:when test="${issue.status == 'IN_PROGRESS'}">
                                            <form action="<c:url value='/manager/issues'/>" method="POST">
                                                <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}" />
                                                <input type="hidden" name="ticketId" value="${issue.ticketId}" />
                                                <input type="hidden" name="currentStatus" value="${issue.status}" />
                                                <input type="hidden" name="newStatus" value="DONE" />
                                                <textarea name="managerNote" class="form-control form-control-sm mb-2" maxlength="500" placeholder="Ghi chú (bắt buộc)" required></textarea>
                                                <button type="submit" class="btn btn-success btn-sm w-100">Hoàn thành</button>
                                            </form>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="text-muted fst-italic">Không có thao tác</span>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:when>
    </c:choose>
</div>

<%@include file="/WEB-INF/views/common/_bottom.jspf" %>