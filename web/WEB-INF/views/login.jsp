<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Login</title>
 <style>
        body {
            font-family: Arial;
            background-color: #eeeeee;
        }

        div {
            width: 350px;
            margin: 100px auto;
            padding: 25px;
            background-color: white;
            border: 1px solid #cccccc;
            border-radius: 8px;
        }

        h2 {
            text-align: center;
            color: blue;
        }

        table {
            width: 100%;
        }

        td {
            padding: 10px 5px;
        }

        input {
            padding: 8px;
            width: 95%;
            border: 1px solid #cccccc;
            border-radius: 4px;
        }

        button {
            width: 100%;
            padding: 10px;
            background-color: blue;
            color: white;
            border: none;
            border-radius: 4px;
            cursor: pointer;
        }

        button:hover {
            background-color: darkblue;
        }
    </style>
</head>
<body>
   

    <div>
        <h2>Login</h2>
        <form action="<c:url value='/login' />" method="post">
            <input type="hidden" name="next" value="${fn:escapeXml(param.next)}"/>
            <table>
                <tr>
                    <td>Username:</td>
                    <td><input type="text" name="username" required/></td>
                </tr>
                <tr>
                    <td>Password:</td>
                    <td><input type="password" name="password" required/></td>
                </tr>
                <tr>
                    <td colspan="2">
                        <button type="submit">Login</button>
                    </td>
                </tr>
                
            </table>
            <c:if test="${not empty error}">
                <p style="color:red;" lang="vi"><c:out value="${error}" /></p>
            </c:if>
        </form>
    </div>
             
   
            
            
</body>
</html>