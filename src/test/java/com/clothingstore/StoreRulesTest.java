package com.clothingstore;

import com.clothingstore.domain.cart.Cart;
import com.clothingstore.domain.order.Order;
import com.clothingstore.domain.order.OrderItem;
import com.clothingstore.domain.order.OrderStatus;
import com.clothingstore.domain.product.Category;
import com.clothingstore.domain.product.Product;
import com.clothingstore.domain.product.ProductSize;
import com.clothingstore.domain.user.Customer;
import com.clothingstore.domain.user.User;
import com.clothingstore.exception.OutOfStockException;
import com.clothingstore.exception.ValidationException;
import com.clothingstore.repository.ProductRepository;
import com.clothingstore.web.WebSupport;
import com.clothingstore.web.filter.SameOriginFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class StoreRulesTest {
    // Chạy OopDemo để chắc chắn các ví dụ OOP vẫn đúng.
    @Test public void oopExamplesRunWithAssertions() {
        OopDemo.main(new String[0]);
    }

    // Đăng nhập phải đổi session id (chống session fixation) nhưng giỏ hàng cũ vẫn giữ nguyên.
    @Test public void signInChangesSessionIdAndKeepsCart() {
        Map<String, Object> attributes = new HashMap<>();
        Cart cart = new Cart();
        attributes.put("shoppingCart", cart);
        String[] sessionId = {"anonymous-id"};
        HttpSession session = (HttpSession) Proxy.newProxyInstance(HttpSession.class.getClassLoader(),
                new Class<?>[]{HttpSession.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getId" -> sessionId[0];
                    case "getAttribute" -> attributes.get((String) args[0]);
                    case "setAttribute" -> { attributes.put((String) args[0], args[1]); yield null; }
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        HttpServletRequest request = (HttpServletRequest) Proxy.newProxyInstance(HttpServletRequest.class.getClassLoader(),
                new Class<?>[]{HttpServletRequest.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getSession" -> session;
                    case "changeSessionId" -> sessionId[0] = "signed-in-id";
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        User user = Customer.create("Customer", "customer@example.test", "hash");
        WebSupport.signIn(request, user);
        assertNotEquals("anonymous-id", session.getId());
        assertSame(user, WebSupport.currentUser(request));
        assertSame(cart, WebSupport.cart(request));
    }

    // Chỉ nhận form POST đến từ chính website này (chống CSRF).
    @Test public void onlyPostsFromThisSiteAreAccepted() {
        assertTrue(SameOriginFilter.fromThisSite(request("localhost:8081", "http://localhost:8081", null)));
        assertTrue(SameOriginFilter.fromThisSite(request("localhost:8081", null, "http://localhost:8081/cart")));
        assertTrue("no browser header: not a browser form", SameOriginFilter.fromThisSite(request("localhost:8081", null, null)));
        assertFalse(SameOriginFilter.fromThisSite(request("localhost:8081", "https://evil.example", null)));
        assertFalse(SameOriginFilter.fromThisSite(request("localhost:8081", "http://localhost:9999", null)));
        assertFalse("sandboxed or privacy-stripped origin", SameOriginFilter.fromThisSite(request("localhost:8081", "null", null)));
        assertFalse(SameOriginFilter.fromThisSite(request("localhost:8081", null, "https://evil.example/page")));
        assertFalse(SameOriginFilter.fromThisSite(request("localhost:8081", "http://bad host", null)));
    }

    private static HttpServletRequest request(String host, String origin, String referer) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Host", host);
        headers.put("Origin", origin);
        headers.put("Referer", referer);
        return (HttpServletRequest) Proxy.newProxyInstance(HttpServletRequest.class.getClassLoader(),
                new Class<?>[]{HttpServletRequest.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getHeader" -> headers.get((String) args[0]);
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    // Giá phải nằm trong giới hạn của cột NUMERIC(12,2) trong database.
    @Test public void priceMustFitDatabaseColumn() {
        assertThrows(ValidationException.class, () -> product("10000000000"));
        assertThrows(ValidationException.class, () -> product("9999999999.995"));
        assertThrows(ValidationException.class, () -> product("-0.01"));
        assertEquals(new BigDecimal("9999999999.99"), product("9999999999.99").getPrice());
    }

    // Giá phải lớn hơn 0.
    @Test public void priceMustBeAboveZero() {
        ValidationException zero = assertThrows(ValidationException.class, () -> product("0"));
        assertEquals("price", zero.getField());
        assertEquals(new BigDecimal("0.01"), product("0.01").getPrice());
    }

    // Link ảnh chỉ được để trống, là https, hoặc là đường dẫn /assets của site.
    @Test public void imageUrlIsEmptyHttpsOrAssetsPath() {
        for (String ok : new String[]{null, "", "  ", "https://example.test/a.jpg", "/assets/products/home/x.webp"}) {
            Product.create(Category.create("Test"), "Product", null, "Black", BigDecimal.TEN, ok);
        }
        for (String bad : new String[]{"khong-phai-anh.jpg", "http://example.test/a.jpg", "https://", "//evil.example/a.png", "javascript:alert(1)"}) {
            ValidationException error = assertThrows(bad, ValidationException.class,
                    () -> Product.create(Category.create("Test"), "Product", null, "Black", BigDecimal.TEN, bad));
            assertEquals("imageUrl", error.getField());
        }
    }

    // Sản phẩm cũ trong database vi phạm luật mới vẫn phải đọc lên được.
    @Test public void storedProductsLoadEvenIfTheyBreakTheNewRules() {
        Product old = Product.restore(Category.create("Test"), "Product", null, "Black", BigDecimal.ZERO, "old.jpg");
        assertEquals("old.jpg", old.getImageUrl());
        assertEquals(0, old.getPrice().signum());
    }

    // Lỗi validation phải nói rõ lỗi ở ô nhập nào để form hiển thị đúng chỗ.
    @Test public void validationErrorsNameTheirField() {
        assertEquals("name", assertThrows(ValidationException.class, () -> Category.create(" ")).getField());
        assertEquals("categoryId", assertThrows(ValidationException.class,
                () -> Product.create(null, "P", null, "Black", BigDecimal.TEN, null)).getField());
        assertEquals("phone", assertThrows(ValidationException.class,
                () -> new com.clothingstore.domain.order.ShippingAddress("A", " ", "B")).getField());
        assertNull(new ValidationException("x").getField());
    }

    // Từ khóa tìm kiếm: ký tự đại diện của LIKE (% và _) được coi là ký tự bình thường, không còn tác dụng đặc biệt; khoảng trắng thừa được dọn.
    @Test public void searchTermEscapesWildcardsAndTidiesSpaces() {
        assertNull(ProductRepository.likeTerm(null));
        assertNull(ProductRepository.likeTerm("   "));
        assertEquals("áo thun", ProductRepository.likeTerm("  áo   thun "));
        assertEquals("100\\%", ProductRepository.likeTerm("100%"));
        assertEquals("a\\_b", ProductRepository.likeTerm("a_b"));
        assertEquals("\\\\", ProductRepository.likeTerm("\\"));
        assertEquals("decomposed accent is composed (NFC)", 4, ProductRepository.likeTerm("café").length());
        assertEquals(100, ProductRepository.likeTerm("a".repeat(500)).length());
    }

    // Chỉ chuyển hướng tới đường dẫn nội bộ của site (chống open redirect).
    @Test public void onlyLocalPathsAreRedirectTargets() {
        assertEquals("/checkout", WebSupport.localPath("/checkout"));
        assertEquals("/admin/order/detail?id=4", WebSupport.localPath("/admin/order/detail?id=4"));
        for (String bad : new String[]{null, "", "checkout", "//evil.example", "/\\evil.example", "https://evil.example",
                "/a b", "/a\r\nSet-Cookie: x=1"}) {
            assertNull(String.valueOf(bad), WebSupport.localPath(bad));
        }
    }

    // Làm tròn giá giống cách database làm tròn.
    @Test public void priceUsesTheSameRoundingAsDatabase() {
        assertEquals(new BigDecimal("1.24"), product("1.235").getPrice());
    }

    // Giờ lưu là UTC, khi hiển thị đổi sang giờ Việt Nam (có thể sang ngày hôm sau).
    @Test public void orderDisplayTimeCrossesMidnightInVietnam() {
        Order order = Order.restore(1, 1, null, OrderStatus.PENDING, BigDecimal.ZERO,
                LocalDateTime.of(2026, 10, 26, 18, 30), List.of());
        assertEquals(LocalDateTime.of(2026, 10, 27, 1, 30), order.getCreatedAt());
        assertNull(new Order(1, null).getCreatedAt());
    }

    // Giảm giá phải từ 0 đến tổng tiền hàng, không được làm tổng âm.
    @Test public void discountMustStayBetweenZeroAndSubtotal() {
        Order order = orderWith(line(1, 2, "100"));
        assertThrows(IllegalArgumentException.class, () -> order.applyDiscount(null));
        assertThrows(IllegalArgumentException.class, () -> order.applyDiscount(new BigDecimal("-0.01")));
        assertThrows(IllegalArgumentException.class, () -> order.applyDiscount(new BigDecimal("200.01")));
        order.applyDiscount(new BigDecimal("200"));
        assertEquals(0, order.getTotal().signum());
    }

    // Đơn hàng không nhận dòng null (thiếu), không nhận dòng trùng cùng một size; danh sách dòng không sửa được từ bên ngoài.
    @Test public void orderRefusesMissingOrRepeatedLines() {
        Order order = orderWith(line(1, 1, "100"));
        assertThrows(NullPointerException.class, () -> order.addItem(null));
        assertThrows(IllegalArgumentException.class, () -> order.addItem(line(1, 3, "100")));
        assertEquals(1, order.getItems().size());
        assertThrows(UnsupportedOperationException.class, () -> order.getItems().clear());
    }

    // Đơn đã lưu (đã có mã) thì không được sửa nội dung hay đánh số lại.
    @Test public void savedOrderCannotBeChangedOrRenumbered() {
        Order order = orderWith(line(1, 1, "100"));
        assertThrows(IllegalArgumentException.class, () -> order.assignId(0));
        order.assignId(7);
        assertThrows(IllegalStateException.class, () -> order.assignId(8));
        assertThrows(IllegalStateException.class, () -> order.addItem(line(2, 1, "100")));
        assertThrows(IllegalStateException.class, () -> order.applyDiscount(BigDecimal.ZERO));
        assertEquals(7, order.getOrderId());
    }

    // Đơn phải có khách hàng và chỉ được chuyển trạng thái theo đúng luật.
    @Test public void orderNeedsACustomerAndAllowedTransitions() {
        assertThrows(IllegalArgumentException.class, () -> new Order(0, null));
        Order order = orderWith(line(1, 1, "100"));
        assertThrows(ValidationException.class, () -> order.transitionTo(OrderStatus.COMPLETED));
        assertThrows(ValidationException.class, () -> order.transitionTo(null));
        order.transitionTo(OrderStatus.CANCELLED);
        assertThrows(ValidationException.class, () -> order.transitionTo(OrderStatus.PENDING));
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
    }

    // Dòng hàng trong đơn từ chối giá trị sai (số lượng, giá, tên).
    @Test public void orderLineRejectsInvalidValues() {
        assertThrows(IllegalArgumentException.class, () -> line(0, 1, "100"));
        assertThrows(IllegalArgumentException.class, () -> line(1, 0, "100"));
        assertThrows(IllegalArgumentException.class, () -> line(1, -1, "100"));
        assertThrows(IllegalArgumentException.class, () -> line(1, 1, "-1"));
        assertThrows(IllegalArgumentException.class, () -> new OrderItem(1, "Áo", "S", "Đen", 1, null));
        assertThrows(IllegalArgumentException.class, () -> new OrderItem(1, " ", "S", "Đen", 1, BigDecimal.ONE));
        assertEquals(new BigDecimal("300"), line(1, 3, "100").getLineTotal());
    }

    // Đơn đọc lại từ database cũng phải tuân theo các luật như đơn mới.
    @Test public void restoredOrderObeysTheSameRules() {
        List<OrderItem> twice = List.of(line(1, 1, "100"), line(1, 1, "100"));
        assertThrows(IllegalArgumentException.class, () -> Order.restore(1, 1, null, OrderStatus.PENDING,
                BigDecimal.ZERO, null, twice));
        assertThrows(IllegalArgumentException.class, () -> Order.restore(1, 1, null, OrderStatus.PENDING,
                new BigDecimal("101"), null, List.of(line(1, 1, "100"))));
        assertThrows(NullPointerException.class, () -> Order.restore(1, 1, null, null,
                BigDecimal.ZERO, null, List.of()));
    }

    // Size sản phẩm từ chối tồn kho âm, mã sai, size ngoài S/M/L.
    @Test public void productSizeRejectsInvalidValues() {
        Product product = product("100");
        assertThrows(IllegalArgumentException.class, () -> new ProductSize(1, product, "M", -1));
        assertThrows(IllegalArgumentException.class, () -> new ProductSize(0, product, "M", 1));
        assertThrows(IllegalArgumentException.class, () -> new ProductSize(1, null, "M", 1));
        assertThrows(IllegalArgumentException.class, () -> new ProductSize(1, product, "XL", 1));
        assertEquals(0, new ProductSize(1, product, "M", 0).getStock());
    }

    // Giỏ hàng chỉ có một dòng cho mỗi size và từ chối số lượng sai hoặc vượt tồn kho.
    @Test public void cartKeepsOneLinePerSizeAndRefusesBadQuantities() {
        ProductSize size = new ProductSize(1, product("100"), "M", 5);
        Cart cart = new Cart();
        cart.addItem(size, 2);
        cart.addItem(size, 3);
        assertEquals(1, cart.getItems().size());
        assertEquals(5, cart.getItems().get(0).getQuantity());
        assertThrows(ValidationException.class, () -> cart.addItem(size, 0));
        assertThrows(ValidationException.class, () -> cart.updateQuantity(size, -1));
        assertThrows(OutOfStockException.class, () -> cart.updateQuantity(size, 6));
        assertEquals(5, cart.getItems().get(0).getQuantity());
    }

    private static OrderItem line(long sizeId, int quantity, String price) {
        return new OrderItem(sizeId, "Áo", "S", "Đen", quantity, new BigDecimal(price));
    }

    private static Order orderWith(OrderItem... lines) {
        Order order = new Order(1, null);
        for (OrderItem line : lines) order.addItem(line);
        return order;
    }

    private static Product product(String price) {
        return Product.create(Category.create("Test"), "Product", null, "Black", new BigDecimal(price), null);
    }
}
