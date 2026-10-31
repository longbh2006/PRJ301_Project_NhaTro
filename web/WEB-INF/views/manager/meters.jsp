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
            table {
                border-collapse: collapse;
                width: 100%;
            }
            th, td {
                border: 1px solid #ccc;
                padding: 8px;
                text-align: left;
            }
            th {
                background:#f0f0f0;
            }
        </style>
    </head>
    <body>

        <h2>Danh sách hợp đồng ACTIVE</h2>
        <c:if test="${param.trung == '1'}">

            <p style="color: red;">Tháng này đã lập hoá đơn, không lập lại.</p>
        </c:if>
        <c:if test="${not empty loi}">
            <ul>
                <c:forEach items="${loi}" var="e">
                    <li><c:out value="${e}"/></li>
                    </c:forEach>
            </ul>
        </c:if>
        <form method="post" action="${pageContext.request.contextPath}/manager/meters">
            <input type="hidden" name="year" value="${year}">
            <input type="hidden" name="month" value="${month}">
            <table>

                <thead>
                    <tr>
                        <th>Mã hợp đồng</th>
                        <th>Mã phòng</th>
                        <th>Chỉ số điện cũ</th>
                        <th>Chỉ số điện mới</th>
                        <th>Chỉ số nước cũ</th>
                        <th>Chỉ số nước mới</th>
                    </tr>
                </thead>
                <tbody>
                    <!-- 2. Lặp qua danh sách hợp đồng -->
                    <c:forEach items="${contracts}" var="ct">
                        <tr>
                            <td>${ct.contractId}</td>
                            <td>${ct.roomId}</td>

                            <td>${oldElectric[ct.contractId]}</td>
                            <td>
                                <input type="number" name="electric_${ct.contractId}" min="0" required>
                            </td>   
                            <td>${oldWater[ct.contractId]}</td>
                            <td>
                                <input type="number" name="water_${ct.contractId}" min="0" required>
                            </td>

                        </tr>
                    </c:forEach>
                </tbody>
            </table>
            <button type="submit">Lưu chỉ số</button>
        </form>
        <form method="post" action="${pageContext.request.contextPath}/manager/invoices">
            <input type="hidden" name="year" value="${year}">
            <input type="hidden" name="month" value="${month}">
            <button type="submit">Lập hoá đơn tháng ${month}/${year}</button>
        </form>
    </body>
</html>
