/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package controller;

import java.io.IOException;
import java.io.PrintWriter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.Account;
import service.AuthService;
import java.sql.SQLException;

/**
 *
 * @author Lenovo
 */
public class LoginController extends HttpServlet {

    private AuthService authService = new AuthService();

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String username = request.getParameter("username");
        String password = request.getParameter("password");
        String next = request.getParameter("next");

        if (username != null) {
            username = username.trim();
        }

        if (isNullOrEmpty(username) || isNullOrEmpty(password)) {

            request.setAttribute("error",
                    "Sai tên đăng nhập hoặc mật khẩu");

            request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
            return;
        }

        Account account;
        try {
            account = authService.login(username, password);
        } catch (SQLException e) {
            throw new ServletException("Lỗi CSDL khi đăng nhập", e);
        }
        if (account != null) {

            HttpSession session = request.getSession();
            request.changeSessionId();

            session.setAttribute("account", account);
            session.setAttribute("accountId", account.getAccountId());
            session.setAttribute("role", account.getRole());
            session.setAttribute("fullName", account.getFullName());
            String target = "/home";
            String role = account.getRole();

            if ("ADMIN".equals(role)) {
                target = "/admin/rooms";
            } else if ("MANAGER".equals(role)) {
                target = "/manager/requests";
            } else if ("TENANT".equals(role)) {
                target = "/tenant/requests";
            }
            if (isSafeNext(next)) {
                target = next;

            }
            response.sendRedirect(request.getContextPath() + target);
        } else {
            request.setAttribute("error", "Sai tên đăng nhập hoặc mật khẩu");
            request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
        }
    }

    private boolean isNullOrEmpty(String s) {
        return s == null || s.isEmpty();
    }

    private boolean isSafeNext(String next) {
        if (next == null || next.isEmpty()) {
            return false;
        }

        if (next.contains("\t") || next.contains("\n") || next.contains("\r")) {
            return false;
        }

        if (!next.startsWith("/")) {
            return false;
        }

        if (next.startsWith("//") || next.startsWith("/\\")) {
            return false;
        }
        return true;
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}
