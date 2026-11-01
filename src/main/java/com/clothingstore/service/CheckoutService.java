package com.clothingstore.service;

import com.clothingstore.domain.cart.Cart;
import com.clothingstore.domain.cart.CartItem;
import com.clothingstore.domain.discount.DiscountPolicy;
import com.clothingstore.domain.order.Order;
import com.clothingstore.domain.order.OrderItem;
import com.clothingstore.domain.order.ShippingAddress;
import com.clothingstore.domain.product.ProductSize;
import com.clothingstore.domain.user.User;
import com.clothingstore.exception.EntityNotFoundException;
import com.clothingstore.exception.ValidationException;
import com.clothingstore.repository.OrderRepository;
import com.clothingstore.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.Map;

/**
 * Biến giỏ hàng thành đơn hàng. Giá luôn lấy từ database, không bao giờ lấy từ trình duyệt.
 * Service giữ các luật nghiệp vụ; repository chỉ lo câu SQL, nhờ vậy hai phần tách riêng và dễ hiểu, dễ kiểm thử và dễ thay đổi.
 */
public final class CheckoutService {
    private final ProductRepository products;
    private final OrderRepository orders;
    private final DiscountPolicy discountPolicy;

    public CheckoutService(ProductRepository products, OrderRepository orders, DiscountPolicy discountPolicy) {
        this.products = products;
        this.orders = orders;
        this.discountPolicy = discountPolicy;
    }

    /** Đơn hàng chưa lưu mà trang thanh toán hiển thị: giá hiện tại, giảm giá và tổng tiền. */
    public Order preview(User customer, Cart cart) {
        return buildOrder(customer, cart, null);
    }

    /** Lưu đơn và làm trống giỏ. OutOfStockException (từ câu UPDATE kho) nghĩa là chưa lưu gì. */
    public Order checkout(User customer, Cart cart, ShippingAddress address) {
        // Bấm đúp sẽ gửi hai lần thanh toán cho cùng một giỏ; lock làm lần thứ hai thấy giỏ đã trống.
        // Giới hạn: lock theo từng giỏ trong một JVM; chạy hai node Tomcat thì cần chốt chặn ở phía database.
        synchronized (cart) {
            // Định dạng số điện thoại là luật cho đơn mới, nên nằm ở đây chứ không ở ShippingAddress: các đơn đã lưu
            // trước khi có luật này có thể mang định dạng khác, và OrderRepository vẫn phải dựng lại chúng thành ShippingAddress.
            // Chấp nhận: dấu cách, chấm, gạch ngang tùy ý ở đầu/cuối và giữa các chữ số; bắt đầu bằng +84 hoặc 0;
            // rồi đúng 9 chữ số nữa (vậy dạng 0xxxxxxxxx là đủ 10 chữ số).
            if (!address.phone().matches("[\\s.\\-]*(\\+84|0)([\\s.\\-]*\\d){9}[\\s.\\-]*")) {
                throw new ValidationException("Số điện thoại cần bắt đầu bằng 0 hoặc +84 và có đủ 10 chữ số.", "phone");
            }
            Order order = buildOrder(customer, cart, address);
            // 9999999999.99 là giá trị lớn nhất của cột NUMERIC(12,2) trong schema.sql.
            if (order.getSubtotal().compareTo(new BigDecimal("9999999999.99")) > 0) {
                throw new ValidationException("Giá trị giỏ hàng quá lớn. Vui lòng giảm số lượng trước khi đặt hàng.");
            }
            orders.placeOrder(order);
            cart.clear();
            return order;
        }
    }

    /** Tên dòng giảm giá, ví dụ "Giảm giá thành viên (5%)". */
    public String discountLabel() { return discountPolicy.label(); }

    private Order buildOrder(User customer, Cart cart, ShippingAddress address) {
        if (cart.isEmpty()) throw new ValidationException("Giỏ hàng của bạn đang trống.");
        // Một câu truy vấn cho mọi dòng của giỏ (không phải mỗi dòng một câu).
        Map<Long, ProductSize> sizes = products.findSizesByIds(
                cart.getItems().stream().map(CartItem::getSizeId).toList());
        Order order = new Order(customer.getUserId(), address);
        for (CartItem cartItem : cart.getItems()) {
            ProductSize size = sizes.get(cartItem.getSizeId());
            if (size == null) throw new EntityNotFoundException("Không tìm thấy sản phẩm.");
            if (!size.getProduct().isSellable()) {
                throw new ValidationException("Sản phẩm " + size.getProduct().getName() + " (size " + size.getSize()
                        + ") đã ngừng bán. Vui lòng xóa khỏi giỏ hàng.");
            }
            order.addItem(new OrderItem(size.getSizeId(), size.getProduct().getName(), size.getSize(),
                    size.getProduct().getColor(), cartItem.getQuantity(), size.getProduct().getPrice()));
        }
        order.applyDiscount(discountPolicy.calculateDiscount(order.getSubtotal(), customer));
        return order;
    }
}
