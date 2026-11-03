

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>

<!DOCTYPE html>
<html lang="vi">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Đăng nhập - Nhà Trọ</title>

        <style>
            * {
                margin: 0;
                padding: 0;
                box-sizing: border-box;
            }

            body {
                font-family: Arial, sans-serif;
                background: linear-gradient(135deg, #dbeafe, #eff6ff);
                min-height: 100vh;
                display: flex;
                justify-content: center;
                align-items: center;
            }

            .login-container {
                background: white;
                width: 400px;
                padding: 35px;
                border-radius: 15px;
                box-shadow: 0 8px 25px rgba(0,0,0,0.1);
            }

            .login-container h2 {
                text-align: center;
                color: #2563eb;
                margin-bottom: 10px;
            }

            .subtitle {
                text-align: center;
                color: #777;
                margin-bottom: 30px;
                font-size: 14px;
            }

            .form-group {
                margin-bottom: 20px;
            }

            .form-group label {
                display: block;
                margin-bottom: 8px;
                font-weight: bold;
                color: #333;
            }

            .form-group input {
                width: 100%;
                padding: 12px;
                border: 1px solid #ddd;
                border-radius: 7px;
                font-size: 15px;
                outline: none;
            }

            .form-group input:focus {
                border-color: #2563eb;
            }

            .btn-login {
                width: 100%;
                padding: 13px;
                background: #2563eb;
                color: white;
                border: none;
                border-radius: 7px;
                font-size: 16px;
                font-weight: bold;
                cursor: pointer;
            }

            .btn-login:hover {
                background: #1d4ed8;
            }

            .error-message {
                background: #fee2e2;
                color: #dc2626;
                padding: 12px;
                border-radius: 7px;
                margin-bottom: 20px;
                text-align: center;
                font-size: 14px;
            }

            .register-link {
                text-align: center;
                margin-top: 20px;
                font-size: 14px;
            }

            .register-link a {
                color: #2563eb;
                text-decoration: none;
                font-weight: bold;
            }

            .register-link a:hover {
                text-decoration: underline;
            }
        </style>
    </head>

    <body>

        <div class="login-container">

            <h2>ĐĂNG NHẬP</h2>

            <p class="subtitle">
                Chào mừng bạn đến với hệ thống Nhà Trọ
            </p>

            <!-- Hiển thị lỗi đăng nhập -->
            <c:if test="${not empty error}">
                <div class="error-message">
                    <c:out value="${error}"/>
                </div>
            </c:if>

            <!-- Form đăng nhập -->
            <form action="<c:url value='/login' />" method="post">

                <!-- Giữ đường dẫn quay lại -->
                <input type="hidden" name="next"
                       value="${fn:escapeXml(param.next)}">

                <div class="form-group">
                    <label for="username">Tên đăng nhập</label>

                    <input type="text"
                           id="username"
                           name="username"
                           placeholder="Nhập tên đăng nhập"
                           value="${fn:escapeXml(param.username)}"
                           required>
                </div>

                <div class="form-group">
                    <label for="password">Mật khẩu</label>

                    <input type="password"
                           id="password"
                           name="password"
                           placeholder="Nhập mật khẩu"
                           required>
                </div>

                <button type="submit" class="btn-login">
                    ĐĂNG NHẬP
                </button>

            </form>

            <div class="register-link">
                Chưa có tài khoản?
                <a href="<c:url value='/register' />">
                    Đăng ký ngay
                </a>
            </div>

        </div>

    </body>
</html>
