package controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import service.RegistrationIntegrationTest;

/** Chỉ triển khai trong ứng dụng kiểm thử tạm trên loopback. */
@WebServlet("/checks")
public final class RegistrationChecksServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/plain;charset=UTF-8");
        try {
            String username = request.getParameter("username");
            if (username != null) {
                if (!username.matches("nt30_http_[a-f0-9]{12}")) {
                    response.sendError(400);
                    return;
                }
                RegistrationIntegrationTest.checkHttpRegistration(username);
                response.getWriter().println("PASS HTTP registration: TENANT role, Unicode and password hash");
                return;
            }
            RegistrationIntegrationTest.main(new String[0]);
            response.getWriter().println("PASS all registration integration checks (see Tomcat log)");
        } catch (Exception | AssertionError ex) {
            response.setStatus(500);
            response.getWriter().println("FAIL: " + ex.getClass().getSimpleName());
            getServletContext().log("Registration integration check failed", ex);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String username = request.getParameter("username");
        if (username == null || !username.matches("nt30_http_[a-f0-9]{12}")) {
            response.sendError(400);
            return;
        }
        try {
            RegistrationIntegrationTest.cleanupHttpRegistration(username);
            response.sendRedirect(request.getContextPath() + "/register");
        } catch (Exception ex) {
            response.sendError(500);
            getServletContext().log("Fixture cleanup failed", ex);
        }
    }
}
