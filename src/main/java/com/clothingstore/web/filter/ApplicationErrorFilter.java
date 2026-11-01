package com.clothingstore.web.filter;

import com.clothingstore.exception.ApplicationException;
import com.clothingstore.exception.AuthorizationException;
import com.clothingstore.exception.DataAccessException;
import com.clothingstore.exception.EntityNotFoundException;
import com.clothingstore.exception.OutOfStockException;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Biến exception mà servlet không xử lý thành trang lỗi. Loại exception quyết định HTTP status (đây là chỗ
 * phân cấp ApplicationException phát huy tác dụng); mọi lỗi bất ngờ khác là 500. Dùng một filter để xử lý lỗi
 * ở một chỗ, thay vì try/catch lặp lại trong từng servlet.
 */
public final class ApplicationErrorFilter implements Filter {
    private static final Logger LOG = Logger.getLogger(ApplicationErrorFilter.class.getName());

    @Override
    public void doFilter(ServletRequest rawRequest, ServletResponse rawResponse, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) rawRequest;
        HttpServletResponse response = (HttpServletResponse) rawResponse;
        try {
            chain.doFilter(request, response);
        } catch (Exception exception) {
            if (response.isCommitted()) throw new ServletException(exception);
            // Pattern switch (Java 21): case khớp đầu tiên được chọn, nên các lớp con phải đứng trước ApplicationException.
            int status = switch (exception) {
                case EntityNotFoundException e -> HttpServletResponse.SC_NOT_FOUND;
                case AuthorizationException e -> HttpServletResponse.SC_FORBIDDEN;
                case OutOfStockException e -> HttpServletResponse.SC_CONFLICT;
                case DataAccessException e -> HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
                case ApplicationException e -> HttpServletResponse.SC_BAD_REQUEST;
                default -> HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            };
            String message;
            if (status == HttpServletResponse.SC_INTERNAL_SERVER_ERROR) {
                // Lỗi phía server (lỗi SQL, bug) được ghi vào log trên console, xem bằng `docker compose logs web`;
                // người truy cập chỉ thấy một thông báo chung.
                LOG.log(Level.SEVERE, "Unhandled server error on " + request.getMethod() + " " + request.getRequestURI(),
                        exception);
                message = "Đã có lỗi xảy ra. Vui lòng thử lại sau.";
            } else {
                message = exception.getMessage();
            }
            request.setAttribute("outOfStockError", exception instanceof OutOfStockException); // trang lỗi dùng cờ này để hiện liên kết về giỏ hàng
            request.setAttribute("errorMessage", message);
            // Attribute mà container đặt cho các trang lỗi của sendError, để error.jsp chỉ đọc một chỗ cho mọi status.
            request.setAttribute("jakarta.servlet.error.status_code", status);
            response.setStatus(status);
            request.getRequestDispatcher("/WEB-INF/views/error.jsp").forward(request, response);
        }
    }
}
