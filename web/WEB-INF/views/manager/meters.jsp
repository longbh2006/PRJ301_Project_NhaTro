<%-- 
    Document   : meters
    Created on : Oct 31, 2026, 9:46:40 AM
    Author     : Lenovo
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %> 
<!DOCTYPE html>
<html>
    <head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <title>Quản lý chỉ số điện</title>
    <style>
        table { border-collapse: collapse; width: 100%; }
        th, td { border: 1px solid #ccc; padding: 8px; text-align: left; }
        th { background:#f0f0f0; }
    </style>
</head>
<body>

<h2>Danh sách hợp đồng ACTIVE</h2>

<table>
    <thead>
        <tr>
            <th>Mã hợp đồng</th>
            <th>Mã phòng</th>
            <th>Chỉ số điện cũ</th>
            <th>Chỉ số nước cũ</th>
        </tr>
    </thead>
    <tbody>
        <!-- 2. Lặp qua danh sách hợp đồng -->
        <c:forEach items="${contracts}" var="ct">
            <tr>
                <td>${ct.contractId}</td>
                <td>${ct.roomId}</td>
                 <td>${oldElectric[ct.contractId]}</td>
                 <td>${oldWater[ct.contractId]}</td>
            </tr>
        </c:forEach>
    </tbody>
</table>

</body>
</html>
