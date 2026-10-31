<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/_taglibs.jspf" %>
<c:set var="pageTitle" value="Báo cáo sự cố" scope="request" />
<c:set var="activeNav" value="/tenant/issues" scope="request" />
<%@ include file="/WEB-INF/views/common/_top.jspf" %>


<div class="container mt-4">
    <div class="mb-4">
        <h1 class="h3 fw-bold mb-1">Báo cáo sự cố bảo trì</h1>
        <p class="text-secondary mb-0">Quản lý và gửi yêu cầu hỗ trợ sửa chữa phòng của bạn.</p>
    </div>

    <c:if test="${not empty errorMessage}">
        <div class="alert alert-danger"><c:out value="${errorMessage}" /></div>
    </c:if>
    <c:if test="${param.msg == 'created'}">
        <div class="alert alert-success">Ghi nhận sự cố thành công! Ban quản lý sẽ phản hồi và xử lý sớm nhất.</div>
    </c:if>

    <%-- ===== Form Báo Cáo ===== --%>
    <div class="card mb-4 shadow-sm border-0">
        <div class="card-body p-4">
            <h2 class="h5 fw-bold mb-3">Gửi yêu cầu mới</h2>
            <c:choose>
                <c:when test="${hasActiveContract}">
                    <form action="<c:url value='/tenant/issues'/>" method="POST">
                        <input type="hidden" name="action" value="create"/>
                        <div class="mb-3">
                            <label for="title" class="form-label">Tiêu đề (Tóm tắt sự cố) <span class="text-danger">*</span></label>
                            <input type="text" class="form-control" id="title" name="title" maxlength="150" required 
                                   value="<c:out value='${inputTitle}'/>" placeholder="Ví dụ: Vòi nước nhà vệ sinh bị rỉ">
                        </div>
                        <div class="mb-3">
                            <label for="description" class="form-label">Mô tả chi tiết</label>
                            <textarea class="form-control" id="description" name="description" rows="3" maxlength="1000"
                                      placeholder="Mô tả cụ thể vị trí, tình trạng... để thợ dễ chuẩn bị đồ nghề (tùy chọn)"><c:out value='${inputDescription}'/></textarea>
                        </div>
                        <button type="submit" class="btn btn-primary px-4">Gửi báo cáo</button>
                    </form>
                </c:when>
                <c:otherwise>
                    <div class="alert alert-warning mb-0 border-0">
                        <i class="bi bi-info-circle me-1"></i> Bạn hiện không có hợp đồng thuê phòng nào đang ở trạng thái <strong>Đang hoạt động</strong> nên không thể gửi yêu cầu hỗ trợ sự cố.
                    </div>
                </c:otherwise>
            </c:choose>
        </div>
    </div>

    <%-- ===== Danh Sách Sự Cố ===== --%>
    <div class="card shadow-sm border-0">
        <div class="card-body p-4">
            <h2 class="h5 fw-bold mb-3">Lịch sử sự cố của bạn</h2>
            <div class="table-responsive">
                <table class="table table-bordered table-striped align-middle mb-0">
                    <thead class="table-dark">
                        <tr>
                            <th>Mã YC</th>
                            <th>Phòng</th>
                            <th style="min-width: 250px;">Tiêu đề & Mô tả</th>
                            <th>Ngày gửi</th>
                            <th>Trạng thái</th>
                            <th>Ghi chú của QL</th>
                            <th>Ngày xử lý xong</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:choose>
                            <c:when test="${empty issues}">
                                <tr><td colspan="7" class="text-center text-muted">Chưa có sự cố nào được ghi nhận.</td></tr>
                            </c:when>
                            <c:otherwise>
                                <c:forEach var="issue" items="${issues}">
                                    <tr>
                                        <td><strong><c:out value="${issue.ticketCode}" /></strong></td>
                                        <td><strong><c:out value="${issue.roomCode}" /></strong></td>
                                        <td>
                                            <strong><c:out value="${issue.title}" /></strong>
                                            <c:if test="${not empty issue.description}">
                                                <div class="text-secondary small mt-1"><c:out value="${issue.description}" /></div>
                                            </c:if>
                                        </td>
                                        <td><c:out value="${issue.createdAtDisplay}" /></td>
                                        <td>
                                            <ui:statusBadge value="${issue.status}" />
                                        </td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${empty issue.managerNote}"><span class="text-muted">-</span></c:when>
                                                <c:otherwise><span class="text-danger small"><c:out value="${issue.managerNote}" /></span></c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td><c:out value="${issue.resolvedAtDisplay}" /></td>
                                    </tr>
                                </c:forEach>
                            </c:otherwise>
                        </c:choose>
                    </tbody>
                </table>
            </div>
        </div>
    </div>
</div>

<%@ include file="/WEB-INF/views/common/_bottom.jspf" %>