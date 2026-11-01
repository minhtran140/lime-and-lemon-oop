package com.clothingstore.web.servlet;

import com.clothingstore.domain.cart.Cart;
import com.clothingstore.domain.user.User;
import com.clothingstore.exception.ValidationException;
import com.clothingstore.service.AuthenticationService;
import com.clothingstore.web.WebSupport;
import com.clothingstore.web.filter.AuthorizationFilter;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet("/account/*")
public final class AccountServlet extends HttpServlet {
    private AuthenticationService authentication;

    @Override
    public void init() {
        authentication = (AuthenticationService) getServletContext().getAttribute("authenticationService");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getPathInfo();
        if (!"/login".equals(action) && !"/register".equals(action)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        User signedIn = WebSupport.currentUser(request);
        if (signedIn != null) { // đã đăng nhập rồi: hai trang này không còn việc gì để làm
            redirectByRole(request, response, signedIn);
            return;
        }
        request.setAttribute("accountMode", "/register".equals(action) ? "register" : "login");
        request.setAttribute("flashMessage", WebSupport.consumeFlash(request));
        request.getRequestDispatcher("/WEB-INF/views/account.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getPathInfo();
        if ("/login".equals(action)) {
            login(request, response);
        } else if ("/register".equals(action)) {
            register(request, response);
        } else if ("/logout".equals(action)) {
            logout(request, response);
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void login(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            User user = authentication.login(request.getParameter("email"), request.getParameter("password"));
            WebSupport.signIn(request, user);
            WebSupport.flash(request, "Đăng nhập thành công.");
            redirectByRole(request, response, user);
        } catch (ValidationException exception) {
            // Hiện lại form kèm lý do và email đã gõ (không bao giờ gồm mật khẩu).
            request.setAttribute("accountMode", "login");
            request.setAttribute("loginError", exception.getMessage());
            request.setAttribute("loginEmail", request.getParameter("email"));
            WebSupport.formError(request, exception);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            request.getRequestDispatcher("/WEB-INF/views/account.jsp").forward(request, response);
        }
    }

    private void register(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            User user = authentication.register(request.getParameter("fullName"), request.getParameter("email"),
                    request.getParameter("password"));
            WebSupport.signIn(request, user);
            WebSupport.flash(request, "Tạo tài khoản thành công.");
            redirectByRole(request, response, user);
        } catch (ValidationException exception) {
            // Hiện lại form kèm lý do và những gì khách đã gõ (không bao giờ gồm mật khẩu).
            request.setAttribute("accountMode", "register");
            request.setAttribute("registrationError", exception.getMessage());
            request.setAttribute("registrationFullName", request.getParameter("fullName"));
            request.setAttribute("registrationEmail", request.getParameter("email"));
            WebSupport.formError(request, exception);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            request.getRequestDispatcher("/WEB-INF/views/account.jsp").forward(request, response);
        }
    }

    private void logout(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        // Giỏ hàng nằm trong session nên đăng xuất sẽ làm trống nó; báo điều đó khi giỏ đang có hàng.
        boolean hadCart = false;
        if (session != null && session.getAttribute("shoppingCart") instanceof Cart cart) {
            hadCart = !cart.isEmpty();
        }
        if (session != null) session.invalidate();
        WebSupport.flash(request, hadCart ? "Bạn đã đăng xuất. Giỏ hàng đã được làm trống." : "Bạn đã đăng xuất.");
        response.sendRedirect(WebSupport.path(request, "/products"));
    }

    /**
     * Sau khi đăng nhập: quay lại trang khách đã xin (nếu loại người dùng này được mở), nếu không thì về trang
     * chủ admin hoặc cửa hàng.
     */
    private static void redirectByRole(HttpServletRequest request, HttpServletResponse response, User user)
            throws IOException {
        HttpSession session = request.getSession(false);
        String wanted = null;
        if (session != null) {
            wanted = WebSupport.localPath((String) session.getAttribute(AuthorizationFilter.AFTER_LOGIN));
            session.removeAttribute(AuthorizationFilter.AFTER_LOGIN);
        }
        boolean adminPath = wanted != null && (wanted.equals("/admin") || wanted.startsWith("/admin/")
                || wanted.startsWith("/admin?"));
        boolean mayOpen = wanted != null && (adminPath ? user.canManageStore() : user.canCheckout());
        String home = user.canManageStore() ? "/admin" : "/products";
        String target = mayOpen ? wanted : home;
        response.sendRedirect(WebSupport.path(request, target));
    }
}
