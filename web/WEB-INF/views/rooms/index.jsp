<%-- 
    Document   : index
    Created on : Oct 28, 2026, 4:32:43 PM
    Author     : ADMIN
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib prefix="nt" uri="/WEB-INF/nt" %>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>JSP Page</title>
    </head>
    <body>
        <nt:pager page="${currentPage}" totalPages="${totalPages}" url="/rooms"/>
        
    </body>
</html>
