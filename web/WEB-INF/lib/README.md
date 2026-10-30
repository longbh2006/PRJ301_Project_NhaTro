# Jakarta JSTL

Tomcat provides the Jakarta Servlet/JSP APIs but the application must package
JSTL separately. These runtime dependencies support `jakarta.tags.core` and
`jakarta.tags.fmt` on Tomcat 10.1:

- `jakarta.servlet.jsp.jstl-api-3.0.0.jar`: Jakarta Tags API.
- `jakarta.servlet.jsp.jstl-3.0.1.jar`: GlassFish Jakarta Tags implementation.

Downloaded from Maven Central:

- https://repo.maven.apache.org/maven2/jakarta/servlet/jsp/jstl/jakarta.servlet.jsp.jstl-api/3.0.0/
- https://repo.maven.apache.org/maven2/org/glassfish/web/jakarta.servlet.jsp.jstl/3.0.1/

Both JARs are included automatically when the contents of `web/` are packaged
into the application WAR. Do not add the legacy `javax` JSTL implementation.
