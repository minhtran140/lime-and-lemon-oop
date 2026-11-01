package com.clothingstore.web.filter;

import com.clothingstore.domain.user.User;
import com.clothingstore.web.WebSupport;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Bảo vệ /checkout, /orders và /admin. Chưa đăng nhập: chuyển tới trang đăng nhập và nhớ trang đã xin (chỉ với request GET).
 * Đã đăng nhập nhưng không được phép: khách hàng vào /admin nhận 403; admin vào /checkout hoặc /orders bị đưa
 * về /admin kèm lời giải thích. Quyền của từng loại người dùng do chính đối tượng user quyết định
 * (canCheckout / canManageStore), nên filter này không bao giờ kiểm tra lớp cụ thể. Kiểm tra quyền trong một
 * filter để không phải lặp lại ở từng servlet.
 */
public final class AuthorizationFilter implements Filter {
    /** Session attribute giữ trang mà khách đã xin; AccountServlet đưa họ tới đó sau khi đăng nhập. */
    public static final String AFTER_LOGIN = "afterLogin";

    @Override
    public void doFilter(ServletRequest rawRequest, ServletResponse rawResponse, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) rawRequest;
        HttpServletResponse response = (HttpServletResponse) rawResponse;
        User user = WebSupport.currentUser(request);
        if (user == null) {
            if ("GET".equals(request.getMethod())) {
                String query = request.getQueryString();
                String target = WebSupport.localPath(request.getRequestURI().substring(request.getContextPath().length())
                        + (query == null ? "" : "?" + query));
                if (target != null) request.getSession(true).setAttribute(AFTER_LOGIN, target);
            }
            WebSupport.flashInfo(request, "Vui lòng đăng nhập để tiếp tục.");
            response.sendRedirect(WebSupport.path(request, "/account/login"));
            return;
        }
        boolean adminArea = request.getServletPath().startsWith("/admin");
        boolean allowed = adminArea ? user.canManageStore() : user.canCheckout();
        if (!allowed) {
            if (adminArea) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
            } else {
                WebSupport.flashInfo(request, "Tài khoản quản trị không đặt hàng. Hãy dùng tài khoản khách để mua hàng.");
                response.sendRedirect(WebSupport.path(request, "/admin"));
            }
            return;
        }
        chain.doFilter(request, response);
    }
}
