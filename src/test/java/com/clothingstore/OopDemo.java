package com.clothingstore;

import com.clothingstore.domain.cart.Cart;
import com.clothingstore.domain.discount.DiscountPolicy;
import com.clothingstore.domain.discount.MemberDiscountPolicy;
import com.clothingstore.domain.discount.NoDiscountPolicy;
import com.clothingstore.domain.order.OrderStatus;
import com.clothingstore.domain.product.Category;
import com.clothingstore.domain.product.Product;
import com.clothingstore.domain.product.ProductSize;
import com.clothingstore.domain.user.Admin;
import com.clothingstore.domain.user.Customer;
import com.clothingstore.domain.user.User;
import com.clothingstore.exception.ApplicationException;
import com.clothingstore.exception.OutOfStockException;
import com.clothingstore.service.Passwords;

import java.math.BigDecimal;

/**
 * Minh họa các ý tưởng OOP của dự án đang chạy thật, không cần database hay server.
 * Chạy sau "mvn package":  java -ea -cp "target/classes;target/test-classes" com.clothingstore.OopDemo
 * (trên macOS/Linux dùng ":" thay cho ";").
 */
public final class OopDemo {
    public static void main(String[] args) {
        // Kiểm tra xem đã chạy với -ea chưa. Câu lệnh assert chỉ được thực thi khi bật -ea.
        // "assert assertionsOn = true;" là phép GÁN (một dấu =), không phải so sánh: biến assertionsOn
        // chỉ thành true khi assert thật sự chạy, tức là khi -ea đang bật. Nếu vẫn false thì báo lỗi ngay.
        boolean assertionsOn = false;
        assert assertionsOn = true;
        if (!assertionsOn) throw new IllegalStateException("Run with java -ea, otherwise the checks are skipped.");

        // Kế thừa + đa hình: cùng một câu hỏi nhưng mỗi loại người dùng (Admin, Customer) trả lời khác nhau.
        String hash = Passwords.sha256("123456");
        User admin = Admin.create("Quản trị", "admin@example.vn", hash);
        User customer = Customer.create("Khách", "khach@example.vn", hash);
        assert admin.canManageStore() && !admin.canCheckout();
        assert customer.canCheckout() && !customer.canManageStore();
        assert hash.length() == 64;

        // Trừu tượng + đa hình (mẫu Strategy): CheckoutService chỉ biết DiscountPolicy, nên cắm cách tính giảm giá nào vào cũng được.
        BigDecimal subtotal = new BigDecimal("200000.00");
        DiscountPolicy member = new MemberDiscountPolicy();
        DiscountPolicy none = new NoDiscountPolicy();
        assert member.calculateDiscount(subtotal, customer).compareTo(new BigDecimal("10000.00")) == 0;
        assert none.calculateDiscount(subtotal, customer).signum() == 0;

        // Enum có hành vi (đóng gói): chính trạng thái đơn hàng quyết định được chuyển sang trạng thái nào.
        assert OrderStatus.PENDING.allows(OrderStatus.CONFIRMED);
        assert OrderStatus.CONFIRMED.allows(OrderStatus.CANCELLED);
        assert !OrderStatus.COMPLETED.allows(OrderStatus.PENDING);

        // Đóng gói: giỏ hàng tự kiểm tra tồn kho. Kế thừa exception: OutOfStockException là một ApplicationException.
        Product tee = Product.create(Category.create("Áo thun"), "Áo thun cơ bản", null, "Đen",
                new BigDecimal("199000"), null);
        ProductSize sizeM = new ProductSize(1, tee, "M", 2);
        Cart cart = new Cart();
        cart.addItem(sizeM, 2);
        try {
            cart.addItem(sizeM, 1);
            throw new AssertionError("adding more than the stock must fail");
        } catch (OutOfStockException expected) {
            assert expected instanceof ApplicationException;
            assert cart.getItems().get(0).getQuantity() == 2 : "a refused add must leave the cart unchanged";
        }

        // Slug của danh mục được tạo từ tên tiếng Việt (bỏ dấu).
        assert Category.slugOf("Váy & chân váy").equals("vay-chan-vay");
        assert Category.slugOf("Đầm").equals("dam");

        System.out.println("OopDemo: all checks passed");
    }
}
