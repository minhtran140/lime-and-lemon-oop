package com.clothingstore.web.servlet;

import com.clothingstore.domain.cart.Cart;
import com.clothingstore.exception.OutOfStockException;
import com.clothingstore.exception.ValidationException;
import com.clothingstore.domain.user.User;
import com.clothingstore.exception.EntityNotFoundException;
import com.clothingstore.service.CheckoutService;
import com.clothingstore.service.ProductService;
import com.clothingstore.web.WebSupport;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/cart/*")
public final class CartServlet extends HttpServlet {
    private ProductService products;
    private CheckoutService checkout;

    @Override
    public void init() {
        products = (ProductService) getServletContext().getAttribute("productService");
        checkout = (CheckoutService) getServletContext().getAttribute("checkoutService");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Cart cart = WebSupport.cart(request);
        request.setAttribute("cartItems", cart.getItems());
        request.setAttribute("cartSubtotal", cart.getSubtotal());
        request.setAttribute("discountLabel", checkout.discountLabel());
        request.setAttribute("flashMessage", WebSupport.consumeFlash(request));
        request.getRequestDispatcher("/WEB-INF/views/cart.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String action = request.getPathInfo();
        Cart cart = WebSupport.cart(request);
        String back = "/cart";
        String productId = request.getParameter("productId");
        // Regex: số nguyên dương tối đa 18 chữ số, không bắt đầu bằng 0 (vừa trong kiểu long).
        if ("/add".equals(action) && productId != null && productId.matches("[1-9][0-9]{0,17}")) {
            back = "/product?id=" + productId; // chỉ gồm chữ số: không bao giờ là URL do client tự đưa vào
        }
        String target = "/cart"; // thành công luôn về giỏ hàng; chỉ khi lỗi mới quay lại trang trước
        User user = WebSupport.currentUser(request);
        if ("/add".equals(action) && user != null && !user.canCheckout()) {
            WebSupport.flashInfo(request, "Tài khoản quản trị không đặt hàng. Hãy dùng tài khoản khách để mua hàng.");
            response.sendRedirect(WebSupport.path(request, back));
            return;
        }
        try {
            update(request, response, cart, action);
        } catch (ValidationException | OutOfStockException exception) {
            WebSupport.flashError(request, exception.getMessage());
            target = withSize(back, request);
        }
        if (!response.isCommitted()) response.sendRedirect(WebSupport.path(request, target));
    }

    /** Trang sản phẩm khi thêm vào giỏ thất bại, vẫn giữ cỡ mà khách đã chọn. */
    private String withSize(String back, HttpServletRequest request) {
        Long sizeId = WebSupport.optionalId(request, "sizeId");
        if (!back.startsWith("/product") || sizeId == null) return back;
        try {
            return back + "&size=" + products.sellableSize(sizeId).getSize(); // một chữ cái S, M hoặc L lấy từ database
        } catch (EntityNotFoundException gone) {
            return back;
        }
    }

    private void update(HttpServletRequest request, HttpServletResponse response, Cart cart, String action)
            throws IOException {
        if ("/add".equals(action)) {
            cart.addItem(products.sellableSize(WebSupport.longParameter(request, "sizeId")),
                    WebSupport.intParameter(request, "quantity"));
            WebSupport.flash(request, "Đã thêm sản phẩm vào giỏ hàng.");
        } else if ("/update".equals(action)) {
            cart.updateQuantity(products.sellableSize(WebSupport.longParameter(request, "sizeId")),
                    WebSupport.intParameter(request, "quantity"));
            WebSupport.flash(request, "Đã cập nhật giỏ hàng.");
        } else if ("/remove".equals(action)) {
            cart.removeItem(WebSupport.longParameter(request, "sizeId"));
            WebSupport.flash(request, "Đã xóa sản phẩm khỏi giỏ hàng.");
        } else if ("/clear".equals(action)) {
            cart.clear();
            WebSupport.flash(request, "Đã xóa toàn bộ giỏ hàng.");
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }
}
