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
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
            // Con số khách đã xem nằm trong session, tra bằng quoteId của đúng trang đã gửi form (mỗi tab một quoteId).
            Map<String, String> quotes = quotesOf(request);
            String quoteId = request.getParameter("quoteId");
            String confirmedQuote = quoteId == null ? null : quotes.get(quoteId);
            Order order = checkout.checkout(customer, cart, address, confirmedQuote);
            if (quoteId != null) quotes.remove(quoteId);
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

    /** Các bản ghi (quoteId → nội dung đơn khách đã xem) của những trang thanh toán mà session này đã mở. */
    @SuppressWarnings("unchecked")
    private static Map<String, String> quotesOf(HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        Map<String, String> quotes = (Map<String, String>) session.getAttribute("checkoutQuotes");
        if (quotes == null) {
            quotes = new ConcurrentHashMap<>();
            session.setAttribute("checkoutQuotes", quotes);
        }
        return quotes;
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
        if (summary != null) {
            // Mỗi lần hiện trang là một quoteId mới, để nhiều tab mở cùng lúc vẫn đúng: tab nào hiện giá cũ thì bị từ chối.
            String quoteId = UUID.randomUUID().toString();
            Map<String, String> quotes = quotesOf(request);
            // Quá nhiều trang cũ thì xóa hết: tab cũ chỉ cần bấm Đặt hàng lần nữa để xác nhận lại.
            if (quotes.size() > 20) quotes.clear();
            quotes.put(quoteId, CheckoutService.quoteOf(summary));
            request.setAttribute("quoteId", quoteId);
        }
        request.setAttribute("checkoutSummary", summary);
        request.setAttribute("discountLabel", checkout.discountLabel());
        request.getRequestDispatcher("/WEB-INF/views/checkout.jsp").forward(request, response);
    }
}
