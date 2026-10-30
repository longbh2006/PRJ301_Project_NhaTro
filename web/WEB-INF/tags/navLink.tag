<%@ tag pageEncoding="UTF-8" body-content="empty" %>
<%@ attribute name="path" required="true" type="java.lang.String" %>
<%@ attribute name="label" required="true" type="java.lang.String" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:url var="navUrl" value="${path}" />
<a class="nav-link${requestScope.activeNav eq path ? ' active' : ''}"
   href="<c:out value='${navUrl}' />"${requestScope.activeNav eq path ? ' aria-current="page"' : ''}><c:out value="${label}" /></a>
