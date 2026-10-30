package controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(name = "LayoutDemoServlet", urlPatterns = {"/layout-demo"})
public class LayoutDemoServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("demoMessage", "Trang thử đã được tải qua controller thành công.");
        request.getRequestDispatcher("/WEB-INF/views/layout-demo.jsp")
                .forward(request, response);
    }
}
