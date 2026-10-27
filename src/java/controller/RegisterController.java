package controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import model.RegistrationForm;
import service.AuthService;
import service.RegistrationException;

@WebServlet(name = "RegisterController", urlPatterns = "/register")
public class RegisterController extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(RegisterController.class.getName());
    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setHeader("Cache-Control", "no-store");
        request.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Cache-Control", "no-store");
        RegistrationForm form = new RegistrationForm(request.getParameter("username"),
                request.getParameter("fullName"), request.getParameter("phone"),
                request.getParameter("email"), request.getParameter("idCardNo"),
                request.getParameter("school"), request.getParameter("dateOfBirth"),
                request.getParameter("hometown"));
        try {
            authService.register(form, request.getParameter("password"));
            response.sendRedirect(request.getContextPath() + "/login?registered=1");
            return;
        } catch (RegistrationException ex) {
            request.setAttribute("errors", ex.getErrors());
        } catch (SQLException ex) {
            // Không đưa thông tin truy vấn, tài khoản hay CCCD vào log và giao diện.
            LOGGER.log(Level.SEVERE, "Registration failed; SQLState={0}, code={1}",
                    new Object[]{ex.getSQLState(), ex.getErrorCode()});
            request.setAttribute("errors", Map.of("form", "Chưa thể đăng ký lúc này. Vui lòng thử lại sau."));
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        }
        request.setAttribute("form", form);
        request.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(request, response);
    }
}
