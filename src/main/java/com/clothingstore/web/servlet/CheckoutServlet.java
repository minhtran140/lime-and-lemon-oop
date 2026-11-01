package com.clothingstore.web.servlet;

import com.clothingstore.domain.cart.Cart;
import com.clothingstore.domain.order.Order;
import com.clothingstore.domain.order.ShippingAddress;
import com.clothingstore.domain.user.User;
import com.clothingstore.exception.EntityNotFoundException;
import com.clothingstore.exception.OutOfStockException;
import com.clothingstore.exception.ValidationException;
import com.clothingstore.service.CheckoutService;
import com.clothingstore.web.WebSupport;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/checkout")
public final class CheckoutServlet extends HttpServlet {
    private CheckoutService checkout;

    @Override
    public void init() {
        checkout = (CheckoutService) getServletContext().getAttribute("checkoutService");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setAttribute("flashMessage", WebSupport.consumeFlash(request));
        showForm(request, response, WebSupport.currentUser(request), WebSupport.cart(request));
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        User customer = WebSupport.currentUser(request);
        Cart cart = WebSupport.cart(request);
        try {
            ShippingAddress address = new ShippingAddress(request.getParameter("recipientName"),
                    request.getParameter("phone"), request.getParameter("address"));
            Order order = checkout.checkout(customer, cart, address);
            WebSupport.flash(request, "Đặt hàng thành công.");
            response.sendRedirect(WebSupport.path(request, "/orders/detail?id=" + order.getOrderId()));
        } catch (OutOfStockException exception) {
            // Có người mua hết hàng trước: hiện lại form với những gì đã gõ và dòng nào bị thiếu hàng.
            request.setAttribute("checkoutError", exception.getMessage());
            request.setAttribute("checkoutErrorSizeId", exception.getSizeId());
            response.setStatus(HttpServletResponse.SC_CONFLICT);
            showForm(request, response, customer, cart);
        } catch (ValidationException exception) {
            // Địa chỉ sai hoặc giỏ trống: hiện lại form kèm lý do.
            request.setAttribute("checkoutError", exception.getMessage());
            WebSupport.formError(request, exception);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            showForm(request, response, customer, cart);
        }
    }

    /** Trang thanh toán: địa chỉ đã gửi (nếu có) và giỏ hàng được tính giá từ database. */
    private void showForm(HttpServletRequest request, HttpServletResponse response, User customer, Cart cart)
            throws ServletException, IOException {
        Order summary = null;
        try {
            // Giỏ trống thì không có gì để hiện: trang có liên kết quay về cửa hàng.
            if (!cart.isEmpty()) summary = checkout.preview(customer, cart);
        } catch (ValidationException | EntityNotFoundException problem) {
            // Một dòng trong giỏ không còn mua được nữa (sản phẩm đã bị tắt): giỏ hàng là nơi để sửa.
            WebSupport.flashError(request, problem.getMessage());
            response.sendRedirect(WebSupport.path(request, "/cart"));
            return;
        }
        request.setAttribute("checkoutSummary", summary);
        request.setAttribute("discountLabel", checkout.discountLabel());
        request.setAttribute("recipientName", request.getParameter("recipientName"));
        request.setAttribute("phone", request.getParameter("phone"));
        request.setAttribute("address", request.getParameter("address"));
        request.getRequestDispatcher("/WEB-INF/views/checkout.jsp").forward(request, response);
    }
}
