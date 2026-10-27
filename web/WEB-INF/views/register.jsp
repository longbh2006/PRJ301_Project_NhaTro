<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Đăng ký tài khoản | Nhà trọ sinh viên</title>
    <link rel="stylesheet" href="<c:url value='/css/register.css'/>">
</head>
<body>
    <header class="site-header">
        <a class="brand" href="<c:url value='/'/>">Nhà trọ <span>sinh viên</span></a>
        <a href="<c:url value='/login'/>">Đăng nhập</a>
    </header>
    <main class="registration">
        <div class="intro">
            <p class="eyebrow">BẮT ĐẦU HÀNH TRÌNH MỚI</p>
            <h1>Tạo tài khoản người thuê</h1>
            <p>Đăng ký để gửi yêu cầu thuê phòng và quản lý thông tin lưu trú của bạn.</p>
        </div>
        <form method="post" action="<c:url value='/register'/>" class="registration-card">
            <c:if test="not empty errors">
                <div class="error-summary" role="alert" tabindex="-1">
                    <strong>Chưa thể hoàn tất đăng ký.</strong>
                    <ul><c:forEach var="error" items="${errors}"><li><c:out value="${error.value}"/></li></c:forEach></ul>
                </div>
            </c:if>
            <p class="form-hint">Các trường có dấu <span class="required">*</span> là bắt buộc.</p>
            <fieldset>
                <legend>Thông tin tài khoản</legend>
                <div class="fields">
                    <div class="field">
                        <label for="username">Tên đăng nhập <span class="required">*</span></label>
                        <input id="username" name="username" value="${fn:escapeXml(form.username)}" autocomplete="username" minlength="4" maxlength="30" pattern="[A-Za-z0-9_.\-]{4,30}" required aria-describedby="username-hint username-error" aria-invalid="${not empty errors.username}">
                        <small id="username-hint">4–30 ký tự: chữ không dấu, số, dấu chấm, gạch dưới hoặc gạch ngang.</small>
                        <span id="username-error" class="field-error"><c:out value="${errors.username}"/></span>
                    </div>
                    <div class="field">
                        <label for="password">Mật khẩu <span class="required">*</span></label>
                        <input id="password" name="password" type="password" autocomplete="new-password" minlength="8" maxlength="1024" required aria-describedby="password-hint password-error" aria-invalid="${not empty errors.password}">
                        <small id="password-hint">Từ 8 ký tự. Hãy dùng mật khẩu riêng cho tài khoản này.</small>
                        <span id="password-error" class="field-error"><c:out value="${errors.password}"/></span>
                    </div>
                </div>
            </fieldset>
            <fieldset>
                <legend>Thông tin cá nhân</legend>
                <div class="fields">
                    <div class="field">
                        <label for="fullName">Họ và tên <span class="required">*</span></label>
                        <input id="fullName" name="fullName" value="${fn:escapeXml(form.fullName)}" autocomplete="name" maxlength="100" required aria-describedby="fullName-error" aria-invalid="${not empty errors.fullName}">
                        <span id="fullName-error" class="field-error"><c:out value="${errors.fullName}"/></span>
                    </div>
                    <div class="field">
                        <label for="idCardNo">Số CCCD <span class="required">*</span></label>
                        <input id="idCardNo" name="idCardNo" value="${fn:escapeXml(form.idCardNo)}" inputmode="numeric" pattern="[0-9]{12}" minlength="12" maxlength="12" required aria-describedby="idCardNo-hint idCardNo-error" aria-invalid="${not empty errors.idCardNo}">
                        <small id="idCardNo-hint">Nhập đủ 12 chữ số, bao gồm số 0 ở đầu.</small>
                        <span id="idCardNo-error" class="field-error"><c:out value="${errors.idCardNo}"/></span>
                    </div>
                    <div class="field">
                        <label for="phone">Số điện thoại</label>
                        <input id="phone" name="phone" type="tel" value="${fn:escapeXml(form.phone)}" autocomplete="tel" inputmode="numeric" pattern="[0-9]{10}" maxlength="10" aria-describedby="phone-error" aria-invalid="${not empty errors.phone}">
                        <span id="phone-error" class="field-error"><c:out value="${errors.phone}"/></span>
                    </div>
                    <div class="field">
                        <label for="email">Email</label>
                        <input id="email" name="email" type="email" value="${fn:escapeXml(form.email)}" autocomplete="email" maxlength="100" aria-describedby="email-error" aria-invalid="${not empty errors.email}">
                        <span id="email-error" class="field-error"><c:out value="${errors.email}"/></span>
                    </div>
                    <div class="field">
                        <label for="dateOfBirth">Ngày sinh</label>
                        <input id="dateOfBirth" name="dateOfBirth" type="date" value="${fn:escapeXml(form.dateOfBirth)}" autocomplete="bday" aria-describedby="dateOfBirth-error" aria-invalid="${not empty errors.dateOfBirth}">
                        <span id="dateOfBirth-error" class="field-error"><c:out value="${errors.dateOfBirth}"/></span>
                    </div>
                    <div class="field">
                        <label for="school">Trường đang học</label>
                        <input id="school" name="school" value="${fn:escapeXml(form.school)}" autocomplete="organization" maxlength="100" aria-describedby="school-error" aria-invalid="${not empty errors.school}">
                        <span id="school-error" class="field-error"><c:out value="${errors.school}"/></span>
                    </div>
                    <div class="field full-width">
                        <label for="hometown">Quê quán</label>
                        <input id="hometown" name="hometown" value="${fn:escapeXml(form.hometown)}" maxlength="100" aria-describedby="hometown-error" aria-invalid="${not empty errors.hometown}">
                        <span id="hometown-error" class="field-error"><c:out value="${errors.hometown}"/></span>
                    </div>
                </div>
            </fieldset>
            <button type="submit">Tạo tài khoản</button>
            <p class="login-link">Đã có tài khoản? <a href="<c:url value='/login'/>">Đăng nhập</a></p>
        </form>
    </main>
    <footer>Nhà trọ sinh viên · An tâm học tập, thuận tiện lưu trú</footer>
</body>
</html>
