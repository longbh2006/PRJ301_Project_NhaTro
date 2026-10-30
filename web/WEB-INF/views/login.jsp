<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <title>Đăng nhập</title>
</head>
<body>
   

    <div>
        <h2>Login</h2>
        <form action="<c:url value='/login' />" method="post">
            <input type="hidden" name="next" value="${fn:escapeXml(param.next)}"/>
            <table>
                <tr>
                    <td>UserName:</td>
                    <td><input type="text" name="username" required/></td>
                </tr>
                <tr>
                    <td>Password:</td>
                    <td><input type="password" name="password" required/></td>
                </tr>
                <tr>
                    <td colspan="2">
                        <button type="submit">login</button>
                    </td>
                </tr>
            </table>
        </form>
    </div>
             <!-- 5️⃣ Khối hiển thị lỗi -->
    <c:if test="${not empty error}">
        <p style="color:red;"><c:out value="${error}" /></p>
    </c:if>
            
            
</body>
</html>