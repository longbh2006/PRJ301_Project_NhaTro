<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/common/_taglibs.jspf" %>
<%@ taglib prefix="nt" uri="/WEB-INF/nt" %>
<c:set var="pageTitle" value="Quản lý Tài khoản" scope="request" />
<c:set var="activeNav" value="/admin/accounts" scope="request" />
<%@ include file="/WEB-INF/views/common/_top.jspf" %>

<div class="container mt-4">
    <div class="d-flex justify-content-between align-items-center mb-3">
        <div>
            <h1 class="h3 fw-bold mb-1">Quản lý Tài khoản</h1>
            <p class="text-secondary mb-0">Danh sách tài khoản hệ thống, phân quyền và trạng thái hoạt động.</p>
        </div>
        <a href="<c:url value='/admin/accounts?action=create' />" class="btn btn-primary">Tạo tài khoản Quản lý</a>
    </div>

    <c:if test="${not empty errorMessage}">
        <div class="alert alert-danger" role="alert">
            <c:out value="${errorMessage}" />
        </div>
    </c:if>
    <c:if test="${param.msg == 'created'}">
        <div class="alert alert-success" role="alert">Tạo tài khoản Quản lý (MANAGER) thành công!</div>
    </c:if>
    <c:if test="${param.msg == 'locked'}">
        <div class="alert alert-warning" role="alert">Đã khóa tài khoản thành công.</div>
    </c:if>
    <c:if test="${param.msg == 'unlocked'}">
        <div class="alert alert-success" role="alert">Đã mở khóa tài khoản thành công.</div>
    </c:if>

    <%-- ===== Bộ lọc tìm kiếm ===== --%>
    <section class="card roomrent-card mb-4">
        <div class="card-body p-4">
            <form method="get" action="<c:url value='/admin/accounts' />" class="row g-3 align-items-end">
                <div class="col-md-5 col-sm-12">
                    <label class="form-label" for="keyword">Tìm kiếm (Username / Họ tên)</label>
                    <input type="text" class="form-control" id="keyword" name="keyword"
                           placeholder="Nhập tên đăng nhập hoặc họ tên..."
                           value="<c:out value='${filterKeyword}' />" />
                </div>

                <div class="col-md-4 col-sm-6">
                    <label class="form-label" for="role">Vai trò</label>
                    <select class="form-select" id="role" name="role">
                        <option value="">Tất cả vai trò</option>
                        <option value="ADMIN" ${filterRole == 'ADMIN' ? 'selected' : ''}>ADMIN</option>
                        <option value="MANAGER" ${filterRole == 'MANAGER' ? 'selected' : ''}>MANAGER</option>
                        <option value="TENANT" ${filterRole == 'TENANT' ? 'selected' : ''}>TENANT</option>
                    </select>
                </div>

                <div class="col-md-3 col-sm-6 d-flex gap-2">
                    <button type="submit" class="btn btn-primary">Tìm kiếm</button>
                    <a href="<c:url value='/admin/accounts' />" class="btn btn-outline-secondary">Xóa lọc</a>
                </div>
            </form>
        </div>
    </section>

    <%-- ===== Bảng danh sách tài khoản ===== --%>
    <c:choose>
        <c:when test="${empty accounts}">
            <div class="alert alert-info" role="status">
                Không tìm thấy tài khoản nào phù hợp với điều kiện lọc.
            </div>
        </c:when>
        <c:otherwise>
            <div class="table-responsive mb-4">
                <table class="table table-bordered table-striped align-middle mb-0">
                    <thead class="table-dark">
                        <tr>
                            <th>ID</th>
                            <th>Username</th>
                            <th>Họ và tên</th>
                            <th>Điện thoại</th>
                            <th>Email</th>
                            <th>Vai trò</th>
                            <th>Trạng thái</th>
                            <th class="text-center" style="width: 150px;">Thao tác</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="acc" items="${accounts}">
                            <tr>
                                <td><c:out value="${acc.accountId}" /></td>
                                <td class="fw-semibold"><c:out value="${acc.username}" /></td>
                                <td><c:out value="${acc.fullName}" /></td>
                                <td><c:out value="${not empty acc.phone ? acc.phone : '-'}" /></td>
                                <td><c:out value="${not empty acc.email ? acc.email : '-'}" /></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${acc.role == 'ADMIN'}"><span class="badge bg-danger">ADMIN</span></c:when>
                                        <c:when test="${acc.role == 'MANAGER'}"><span class="badge bg-primary">MANAGER</span></c:when>
                                        <c:otherwise><span class="badge bg-secondary">TENANT</span></c:otherwise>
                                    </c:choose>
                                </td>
                                <td>
                                    <c:choose>
                                        <c:when test="${acc.active}">
                                            <span class="badge bg-success">Đang hoạt động</span>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="badge bg-secondary">Đã khóa</span>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td class="text-center">
                                    <c:choose>
                                        <c:when test="${sessionScope.account.accountId == acc.accountId}">
                                            <span class="text-muted small">Tài khoản của bạn</span>
                                        </c:when>
                                        <c:otherwise>
                                            <form action="<c:url value='/admin/accounts' />" method="post" style="display:inline;"
                                                  onsubmit="return confirm('${acc.active ? 'Bạn có chắc chắn muốn khóa tài khoản này?' : 'Bạn có chắc chắn muốn mở khóa tài khoản này?'}');">
                                                <input type="hidden" name="action" value="toggleStatus" />
                                                <input type="hidden" name="accountId" value="<c:out value='${acc.accountId}' />" />
                                                <input type="hidden" name="active" value="${acc.active ? 0 : 1}" />
                                                <button type="submit" class="btn btn-sm ${acc.active ? 'btn-outline-danger' : 'btn-outline-success'}">
                                                    ${acc.active ? 'Khóa' : 'Mở khóa'}
                                                </button>
                                            </form>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </div>

            <%-- ===== Phân trang (Giữ nguyên keyword và role khi sang trang 2) ===== --%>
            <c:url var="basePagerUrl" value="/admin/accounts">
                <c:if test="${not empty filterKeyword}">
                    <c:param name="keyword" value="${filterKeyword}" />
                </c:if>
                <c:if test="${not empty filterRole}">
                    <c:param name="role" value="${filterRole}" />
                </c:if>
            </c:url>

            <nt:pager page="${currentPage}" totalPages="${totalPages}" url="${basePagerUrl}" />
        </c:otherwise>
    </c:choose>
</div>

<%@ include file="/WEB-INF/views/common/_bottom.jspf" %>