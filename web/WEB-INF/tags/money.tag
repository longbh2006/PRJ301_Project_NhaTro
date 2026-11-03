<%@ tag pageEncoding="UTF-8" body-content="empty" trimDirectiveWhitespaces="true" %>
<%@ attribute name="value" required="true" type="java.lang.Object" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%-- Cố định locale vi_VN để luôn ra "1.250.000 ₫", không phụ thuộc ngôn ngữ trình duyệt. --%>
<fmt:setLocale value="vi_VN" />
<fmt:formatNumber value="${value}" type="currency" currencySymbol="₫" maxFractionDigits="0" />