package com.clothingstore.web.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.Objects;

/**
 * Chặn tấn công giả mạo request chéo site (CSRF): mọi thay đổi trong shop đều là POST, và POST có Origin (hoặc,
 * nếu không có, Referer) thuộc host khác sẽ bị từ chối với 403. Trình duyệt luôn gửi Origin kèm POST, nên form
 * ở site khác không qua được; request không có header nào (curl, smoke test) không phải form của trình duyệt
 * nên được cho qua. Cookie session còn có SameSite=Lax (web.xml) làm lớp bảo vệ thứ hai.
 */
public final class SameOriginFilter implements Filter {
    @Override
    public void doFilter(ServletRequest rawRequest, ServletResponse rawResponse, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) rawRequest;
        if ("POST".equals(request.getMethod()) && !fromThisSite(request)) {
            ((HttpServletResponse) rawResponse).sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        chain.doFilter(rawRequest, rawResponse);
    }

    /** True khi trang gửi request nằm trên host này, hoặc khi không có header nào của trình duyệt cho thấy ngược lại. */
    public static boolean fromThisSite(HttpServletRequest request) {
        String source = request.getHeader("Origin");
        if (source == null) source = request.getHeader("Referer");
        if (source == null) return true;
        try {
            // Authority là phần host:port của URL (ví dụ localhost:8081), đem so với header Host của request.
            return Objects.equals(URI.create(source).getRawAuthority(), request.getHeader("Host"));
        } catch (IllegalArgumentException malformed) {
            return false;
        }
    }
}
