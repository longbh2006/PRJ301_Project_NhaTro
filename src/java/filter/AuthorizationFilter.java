/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import model.Account;

/**
 *
 * @author Lenovo
 */
public class AuthorizationFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        // ① ép kiểu sang HttpServletRequest
        HttpServletRequest httpReq = (HttpServletRequest) request;

        String path = httpReq.getRequestURI().substring(httpReq.getContextPath().length());
        HttpSession session = httpReq.getSession(false);

        Account account = null;
        if (session != null) {
            account = (Account) session.getAttribute("account");
        }
    HttpServletResponse httpResp = (HttpServletResponse) response;

    
    if (path.startsWith("/tenant/")) {
        
        if (account == null) {
            String redirectUrl = httpReq.getContextPath()
                                        + "/login?next=" + path;
            httpResp.sendRedirect(redirectUrl);
            return;  
        }

        
        if (!"TENANT".equals(account.getRole())) {
            httpResp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;   
        }
    }
    
    if (path.startsWith("/manager/")) {
        
        if (account == null) {
            String redirectUrl = httpReq.getContextPath()
                                        + "/login?next=" + path;
            httpResp.sendRedirect(redirectUrl);
            return;   
        }

        
        if (!"MANAGER".equals(account.getRole())) {
            httpResp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;   
        }
    }
    
    if (path.startsWith("/admin/")) {
        
        if (account == null) {
            String redirectUrl = httpReq.getContextPath()
                                        + "/login?next=" + path;
            httpResp.sendRedirect(redirectUrl);
            return;   
        }

        
        if (!"ADMIN".equals(account.getRole())) {
            httpResp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;   
        }
    }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        Filter.super.destroy(); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody
    }

}
