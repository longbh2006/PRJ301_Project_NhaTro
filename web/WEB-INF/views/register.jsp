<%@ page pageEncoding="UTF-8" session="false" %>
<%@ include file="/WEB-INF/views/common/_taglibs.jspf" %>
<c:set var="pageTitle" value="RoomRent | Đăng ký tài khoản" scope="request" />
<c:set var="layoutVariant" value="public" scope="request" />
<%@ include file="/WEB-INF/views/common/_top.jspf" %>

<section class="roomrent-register" aria-labelledby="register-title">
    <div class="mb-4">
        <h1 id="register-title" class="h3 fw-bold mb-1">Tạo tài khoản người thuê</h1>
        <p class="text-secondary mb-0">Đăng ký để gửi yêu cầu thuê phòng và quản lý thông tin lưu trú của bạn.</p>
    </div>

    <form method="post" action="<c:url value='/register' />" class="card roomrent-card">
        <div class="card-body p-4">
            <c:if test="${not empty errors}">
                <div class="alert alert-danger" role="alert" tabindex="-1">
                    <strong>Chưa thể hoàn tất đăng ký.</strong>
                    <ul class="mb-0 mt-2"><c:forEach var="error" items="${errors}"><li><c:out value="${error.value}" /></li></c:forEach></ul>
                </div>
            </c:if>
            <p class="small text-secondary mb-4">Các trường có dấu <span class="text-danger">*</span> là bắt buộc.</p>

            <fieldset class="mb-4">
                <legend class="h6 fw-bold border-bottom pb-2 mb-3">Thông tin tài khoản</legend>
                <div class="row g-3">
                    <div class="col-md-6">
                        <label class="form-label" for="username">Tên đăng nhập <span class="text-danger">*</span></label>
                        <input class="form-control ${not empty errors.username ? 'is-invalid' : ''}" id="username" name="username" value="${fn:escapeXml(form.username)}" autocomplete="username" minlength="4" maxlength="30" pattern="[A-Za-z0-9_.\-]{4,30}" required aria-describedby="username-hint username-error" aria-invalid="${not empty errors.username}">
                        <div id="username-hint" class="form-text">4–30 ký tự: chữ không dấu, số, dấu chấm, gạch dưới hoặc gạch ngang.</div>
                        <c:if test="${not empty errors.username}"><div id="username-error" class="invalid-feedback"><c:out value="${errors.username}" /></div></c:if>
                    </div>
                    <div class="col-md-6">
                        <label class="form-label" for="password">Mật khẩu <span class="text-danger">*</span></label>
                        <input class="form-control ${not empty errors.password ? 'is-invalid' : ''}" id="password" name="password" type="password" autocomplete="new-password" minlength="8" maxlength="1024" required aria-describedby="password-hint password-error" aria-invalid="${not empty errors.password}">
                        <div id="password-hint" class="form-text">Từ 8 ký tự. Hãy dùng mật khẩu riêng cho tài khoản này.</div>
                        <c:if test="${not empty errors.password}"><div id="password-error" class="invalid-feedback"><c:out value="${errors.password}" /></div></c:if>
                    </div>
                </div>
            </fieldset>

            <fieldset class="mb-4">
                <legend class="h6 fw-bold border-bottom pb-2 mb-3">Thông tin cá nhân</legend>
                <div class="row g-3">
                    <div class="col-md-6">
                        <label class="form-label" for="fullName">Họ và tên <span class="text-danger">*</span></label>
                        <input class="form-control ${not empty errors.fullName ? 'is-invalid' : ''}" id="fullName" name="fullName" value="${fn:escapeXml(form.fullName)}" autocomplete="name" maxlength="100" required aria-describedby="fullName-error" aria-invalid="${not empty errors.fullName}">
                        <c:if test="${not empty errors.fullName}"><div id="fullName-error" class="invalid-feedback"><c:out value="${errors.fullName}" /></div></c:if>
                    </div>
                    <div class="col-md-6">
                        <label class="form-label" for="idCardNo">Số CCCD <span class="text-danger">*</span></label>
                        <input class="form-control ${not empty errors.idCardNo ? 'is-invalid' : ''}" id="idCardNo" name="idCardNo" value="${fn:escapeXml(form.idCardNo)}" inputmode="numeric" pattern="[0-9]{12}" minlength="12" maxlength="12" required aria-describedby="idCardNo-hint idCardNo-error" aria-invalid="${not empty errors.idCardNo}">
                        <div id="idCardNo-hint" class="form-text">Nhập đủ 12 chữ số, bao gồm số 0 ở đầu.</div>
                        <c:if test="${not empty errors.idCardNo}"><div id="idCardNo-error" class="invalid-feedback"><c:out value="${errors.idCardNo}" /></div></c:if>
                    </div>
                    <div class="col-md-6">
                        <label class="form-label" for="phone">Số điện thoại</label>
                        <input class="form-control ${not empty errors.phone ? 'is-invalid' : ''}" id="phone" name="phone" type="tel" value="${fn:escapeXml(form.phone)}" autocomplete="tel" inputmode="numeric" pattern="[0-9]{10}" maxlength="10" aria-describedby="phone-error" aria-invalid="${not empty errors.phone}">
                        <c:if test="${not empty errors.phone}"><div id="phone-error" class="invalid-feedback"><c:out value="${errors.phone}" /></div></c:if>
                    </div>
                    <div class="col-md-6">
                        <label class="form-label" for="email">Email</label>
                        <input class="form-control ${not empty errors.email ? 'is-invalid' : ''}" id="email" name="email" type="email" value="${fn:escapeXml(form.email)}" autocomplete="email" maxlength="100" aria-describedby="email-error" aria-invalid="${not empty errors.email}">
                        <c:if test="${not empty errors.email}"><div id="email-error" class="invalid-feedback"><c:out value="${errors.email}" /></div></c:if>
                    </div>
                    <div class="col-md-6">
                        <label class="form-label" for="dateOfBirth">Ngày sinh</label>
                        <input class="form-control ${not empty errors.dateOfBirth ? 'is-invalid' : ''}" id="dateOfBirth" name="dateOfBirth" type="date" value="${fn:escapeXml(form.dateOfBirth)}" autocomplete="bday" aria-describedby="dateOfBirth-error" aria-invalid="${not empty errors.dateOfBirth}">
                        <c:if test="${not empty errors.dateOfBirth}"><div id="dateOfBirth-error" class="invalid-feedback"><c:out value="${errors.dateOfBirth}" /></div></c:if>
                    </div>
                    <div class="col-md-6">
                        <label class="form-label" for="school">Trường đang học</label>
                        <input class="form-control ${not empty errors.school ? 'is-invalid' : ''}" id="school" name="school" value="${fn:escapeXml(form.school)}" autocomplete="organization" maxlength="100" aria-describedby="school-error" aria-invalid="${not empty errors.school}">
                        <c:if test="${not empty errors.school}"><div id="school-error" class="invalid-feedback"><c:out value="${errors.school}" /></div></c:if>
                    </div>
                    <div class="col-12">
                        <label class="form-label" for="hometown">Quê quán</label>
                        <input class="form-control ${not empty errors.hometown ? 'is-invalid' : ''}" id="hometown" name="hometown" value="${fn:escapeXml(form.hometown)}" maxlength="100" aria-describedby="hometown-error" aria-invalid="${not empty errors.hometown}">
                        <c:if test="${not empty errors.hometown}"><div id="hometown-error" class="invalid-feedback"><c:out value="${errors.hometown}" /></div></c:if>
                    </div>
                </div>
            </fieldset>

            <button class="btn btn-primary w-100" type="submit">Tạo tài khoản</button>
            <p class="small text-center mt-3 mb-0">Đã có tài khoản? <a href="<c:url value='/login' />">Đăng nhập</a></p>
        </div>
    </form>
</section>

<%@ include file="/WEB-INF/views/common/_bottom.jspf" %>
