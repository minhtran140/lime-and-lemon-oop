package com.clothingstore;

import com.clothingstore.config.Database;
import com.clothingstore.domain.cart.Cart;
import com.clothingstore.domain.discount.MemberDiscountPolicy;
import com.clothingstore.domain.order.*;
import com.clothingstore.domain.product.*;
import com.clothingstore.domain.user.*;
import com.clothingstore.exception.OutOfStockException;
import com.clothingstore.exception.ValidationException;
import com.clothingstore.repository.*;
import com.clothingstore.service.CheckoutService;
import com.clothingstore.service.OrderService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeNotNull;

/** Test PostgreSQL chạy khi được bật riêng (-Dtest.db.url). Mỗi test dùng một schema mới; không bao giờ đụng bảng có sẵn. */
public class DatabaseTest {
    private Connection owner;
    private String schema;
    private ProductRepository products;
    private OrderRepository orders;
    private OrderService orderService;
    private CheckoutService checkout;
    private Category category;
    private User customer;
    private static final ShippingAddress ADDRESS = new ShippingAddress("Test", "0901234567", "Test address");

    @Before public void createIsolatedSchema() throws Exception {
        String url = System.getProperty("test.db.url");
        assumeNotNull(url);
        String user = System.getProperty("test.db.user", "postgres");
        String password = System.getProperty("test.db.password", "");
        owner = DriverManager.getConnection(url, user, password);
        schema = "test_" + UUID.randomUUID().toString().replace("-", "");
        try (var statement = owner.createStatement()) {
            statement.execute("CREATE SCHEMA " + schema);
            statement.execute("SET search_path TO " + schema);
            statement.execute(Files.readString(Path.of("src/main/resources/db/schema.sql")));
        }
        Database database = Database.direct(url + (url.contains("?") ? "&" : "?") + "currentSchema=" + schema,
                user, password);
        products = new ProductRepository(database);
        orders = new OrderRepository(database);
        orderService = new OrderService(orders, new UserRepository(database));
        checkout = new CheckoutService(products, orders, new MemberDiscountPolicy());
        category = new CategoryRepository(database).insert(Category.create("Test"));
        customer = new UserRepository(database).insert(Customer.create("Test", "test@example.test", "hash"));
    }

    @After public void dropOnlyOurSchema() throws Exception {
        if (owner == null) return;
        try (Connection connection = owner; var statement = connection.createStatement()) {
            if (schema != null) statement.execute("DROP SCHEMA " + schema + " CASCADE");
        }
    }

    // Màn hình admin cũ (đã lỗi thời) hủy đơn lần nữa thì không được cộng lại tồn kho lần hai.
    @Test public void staleCancellationCannotRestockTwice() {
        Product product = product("100", 10, 10, 10);
        Order order = buy(product, 2, ADDRESS);
        Order stale = orderService.adminOrder(order.getOrderId());
        orderService.changeStatus(order.getOrderId(), OrderStatus.CANCELLED);
        stale.transitionTo(OrderStatus.CANCELLED);
        assertThrows(ValidationException.class, () -> orders.updateStatus(stale, OrderStatus.PENDING));
        assertEquals(10, size(product, "S").getStock());
        assertEquals(OrderStatus.CANCELLED, orderService.adminOrder(order.getOrderId()).getStatus());
    }

    // Hai admin hủy cùng một đơn cùng lúc: chỉ một người thành công, tồn kho cộng lại đúng một lần.
    @Test public void concurrentCancellationsRestockExactlyOnce() throws Exception {
        Product product = product("100", 30, 10, 10);
        for (int round = 0; round < 15; round++) {
            Order order = buy(product, 2, ADDRESS);
            Runnable cancel = () -> orderService.changeStatus(order.getOrderId(), OrderStatus.CANCELLED);
            List<Throwable> failures = runTogether(cancel, cancel);
            assertEquals("exactly one of two cancellations may win", 1, failures.size());
            assertTrue(failures.get(0).toString(), failures.get(0) instanceof ValidationException);
            assertEquals(30, size(product, "S").getStock());
        }
    }

    // Hai người cùng mua món cuối cùng: chỉ một người mua được, người kia nhận lỗi hết hàng.
    @Test public void lastItemGoesToExactlyOneBuyer() throws Exception {
        Product product = product("100", 1, 0, 0);
        Runnable buyLast = () -> buy(product, 1, ADDRESS);
        List<Throwable> failures = runTogether(buyLast, buyLast);
        assertEquals(1, failures.size());
        assertTrue(failures.get(0).toString(), failures.get(0) instanceof OutOfStockException);
        assertEquals(0, size(product, "S").getStock());
        assertEquals(1, orders.findAll().size());
    }

    /** Thanh toán theo hai thứ tự dòng hàng và việc hủy đơn cùng chạm vào hai dòng tồn kho một lúc; không được xảy ra deadlock. */
    @Test public void reverseOrderCheckoutsAndCancellationsDoNotDeadlock() throws Exception {
        int rounds = 100;
        Product product = product("100", 1000, 1000, 0);
        ProductSize s = size(product, "S"), m = size(product, "M");
        List<Order> toCancel = new ArrayList<>();
        for (int i = 0; i < rounds; i++) toCancel.add(checkout.checkout(customer, cart(s, m), ADDRESS));
        List<Throwable> failures = runTogether(
                () -> { for (int i = 0; i < rounds; i++) checkout.checkout(customer, cart(s, m), ADDRESS); },
                () -> { for (int i = 0; i < rounds; i++) checkout.checkout(customer, cart(m, s), ADDRESS); },
                () -> toCancel.forEach(o -> orderService.changeStatus(o.getOrderId(), OrderStatus.CANCELLED)));
        assertEquals(List.of(), failures.stream().map(f -> f + " caused by " + f.getCause()).toList());
        assertEquals(1000 - 2 * rounds, size(product, "S").getStock());
        assertEquals(1000 - 2 * rounds, size(product, "M").getStock());
    }

    // Màn hình cũ bấm xác nhận không được làm sống lại đơn đã hủy.
    @Test public void staleConfirmationCannotReopenCancelledOrder() {
        Product product = product("100", 10, 10, 10);
        Order order = buy(product, 2, ADDRESS);
        Order stale = orderService.adminOrder(order.getOrderId());
        orderService.changeStatus(order.getOrderId(), OrderStatus.CANCELLED);
        stale.transitionTo(OrderStatus.CONFIRMED);
        assertThrows(ValidationException.class, () -> orders.updateStatus(stale, OrderStatus.PENDING));
        assertEquals(OrderStatus.CANCELLED, orderService.adminOrder(order.getOrderId()).getStatus());
        assertEquals(10, size(product, "S").getStock());
    }

    // Các bước chuyển trạng thái hợp lệ vẫn đi được tới hoàn tất; đơn hoàn tất thì không hủy được nữa.
    @Test public void allowedStatusChangesStillCompleteAnOrder() {
        Order order = buy(product("100", 10, 10, 10), 1, ADDRESS);
        orderService.changeStatus(order.getOrderId(), OrderStatus.CONFIRMED);
        orderService.changeStatus(order.getOrderId(), OrderStatus.SHIPPING);
        orderService.changeStatus(order.getOrderId(), OrderStatus.COMPLETED);
        assertEquals(OrderStatus.COMPLETED, orderService.adminOrder(order.getOrderId()).getStatus());
        assertThrows(ValidationException.class,
                () -> orderService.changeStatus(order.getOrderId(), OrderStatus.CANCELLED));
    }

    // Form sửa tồn kho cũ không được ghi đè lên số hàng đã bán.
    @Test public void staleStockFormCannotUndoASale() {
        Product product = product("100", 10, 10, 10);
        buy(product, 1, ADDRESS);
        assertThrows(ValidationException.class, () -> products.updateStock(product.getProductId(), 10, 11, 10, 10, 10, 10));
        assertEquals(9, size(product, "S").getStock());
        assertEquals(10, size(product, "M").getStock());
    }

    // Sửa tồn kho bị xung đột ở size cuối thì rollback cả các size trước đó.
    @Test public void conflictOnLastSizeRollsBackEarlierSizeChanges() {
        Product product = product("100", 10, 10, 10);
        products.updateStock(product.getProductId(), 10, 10, 9, 10, 10, 10);
        assertThrows(ValidationException.class, () -> products.updateStock(product.getProductId(), 11, 12, 13, 10, 10, 10));
        assertEquals(10, size(product, "S").getStock());
        assertEquals(10, size(product, "M").getStock());
        assertEquals(9, size(product, "L").getStock());
    }

    // Form tồn kho còn khớp dữ liệu hiện tại thì cập nhật đủ cả ba size.
    @Test public void matchingStockFormUpdatesAllSizes() {
        Product product = product("100", 10, 10, 10);
        products.updateStock(product.getProductId(), 1, 2, 3, 10, 10, 10);
        assertEquals(1, size(product, "S").getStock());
        assertEquals(2, size(product, "M").getStock());
        assertEquals(3, size(product, "L").getStock());
    }

    // Số điện thoại sai thì không tạo đơn và không trừ tồn kho.
    @Test public void invalidPhoneCannotCreateAnOrder() {
        Product product = product("100", 10, 10, 10);
        assertThrows(ValidationException.class,
                () -> buy(product, 1, new ShippingAddress("Test", "x", "Test address")));
        assertEquals(10, size(product, "S").getStock());
        assertTrue(orders.findAll().isEmpty());
    }

    // Số điện thoại có dấu cách, dấu chấm, +84 vẫn được nhận; đơn cũ có số lạ vẫn đọc được.
    @Test public void formattedPhonesAndOldOrdersRemainReadable() {
        Product product = product("100", 10, 10, 10);
        for (String phone : new String[]{"0901234567", "+84901234567", "090.123.4567", "+84 90-123-4567"}) {
            Order order = buy(product, 1, new ShippingAddress("Test", phone, "Test address"));
            assertEquals(phone, orderService.adminOrder(order.getOrderId()).getShippingAddress().phone());
        }
        Order legacy = new Order(customer.getUserId(), new ShippingAddress("Test", "x", "Test address"));
        legacy.addItem(new OrderItem(size(product, "S").getSizeId(), "Legacy", "S", "Black", 1,
                new java.math.BigDecimal("100")));
        orders.placeOrder(legacy);
        assertEquals("x", orderService.adminOrder(legacy.getOrderId()).getShippingAddress().phone());
    }

    // Tổng tiền vượt giới hạn cột thì báo lỗi validation, giỏ hàng và tồn kho giữ nguyên.
    @Test public void amountOverflowIsAValidationErrorAndLeavesCartAndStock() {
        Product product = product("6000000000", 2, 0, 0);
        Cart cart = new Cart();
        cart.addItem(size(product, "S"), 2);
        assertThrows(ValidationException.class, () -> checkout.checkout(customer, cart, ADDRESS));
        assertEquals(2, size(product, "S").getStock());
        assertEquals(2, cart.getItems().get(0).getQuantity());
        assertTrue(orders.findAll().isEmpty());
    }

    // Một dòng hết hàng thì rollback cả đơn (transaction), giỏ hàng vẫn còn.
    @Test public void outOfStockRollsBackAllLinesAndRetainsCart() {
        Product product = product("100", 2, 1, 0);
        Cart cart = new Cart();
        cart.addItem(size(product, "S"), 2);
        cart.addItem(size(product, "M"), 1);
        products.updateStock(product.getProductId(), 2, 0, 0, 2, 1, 0);
        assertThrows(OutOfStockException.class, () -> checkout.checkout(customer, cart, ADDRESS));
        assertEquals(2, size(product, "S").getStock());
        assertEquals(2, cart.getItems().size());
        assertTrue(orders.findAll().isEmpty());
    }

    // Dữ liệu demo chỉ dùng ảnh .webp có thật trong thư mục webapp.
    @Test public void seedCatalogUsesExistingWebpFiles() throws Exception {
        try (var statement = owner.createStatement()) {
            statement.execute(Files.readString(Path.of("src/main/resources/db/demo-catalog.sql")));
            try (var rows = statement.executeQuery("SELECT image_url FROM products WHERE image_url IS NOT NULL")) {
                while (rows.next()) {
                    String image = rows.getString(1);
                    assertTrue(image, image.endsWith(".webp"));
                    assertTrue(image, Files.isRegularFile(Path.of("src/main/webapp" + image)));
                }
            }
        }
    }

    // Migration ảnh chỉ đổi đúng các đường dẫn cũ đã biết, giữ link tùy chỉnh, và chạy lại nhiều lần vẫn an toàn.
    @Test public void imageMigrationOnlyReplacesKnownPathsAndIsRepeatable() throws Exception {
        Product old = product("100", 1, 0, 0);
        old.changeImageUrl("/assets/products/ao-hoodie-xam-ai.png");
        products.update(old);
        Product custom = product("100", 1, 0, 0);
        custom.changeImageUrl("https://example.test/custom.png");
        products.update(custom);
        Map<Long, String> before = new HashMap<>();
        products.findAllForAdmin().forEach(p -> before.put(p.getProductId(), p.getName()));
        String sql = Files.readString(Path.of("src/main/resources/db/migrations/20261027-product-webp.sql"));
        try (var statement = owner.createStatement()) {
            statement.execute(sql);
            statement.execute(sql);
        }
        assertEquals("/assets/products/home/ao-hoodie-xam-20261007.webp",
                products.findById(old.getProductId()).orElseThrow().getImageUrl());
        assertEquals("https://example.test/custom.png", products.findById(custom.getProductId()).orElseThrow().getImageUrl());
        assertEquals(before.size(), products.findAllForAdmin().size());
        products.findAllForAdmin().forEach(p -> assertEquals(before.get(p.getProductId()), p.getName()));
    }

    // Tìm kiếm không phân biệt dấu ("ao" ra "áo"); % và _ được xem là ký tự bình thường (không phải ký tự đại diện).
    @Test public void searchIgnoresAccentsAndTreatsWildcardsAsText() {
        products.insert(Product.create(category, "Áo thun đen", null, "Đen", new java.math.BigDecimal("100"), null), 1, 0, 0);
        products.insert(Product.create(category, "Đầm midi", null, "Be", new java.math.BigDecimal("100"), null), 1, 0, 0);
        products.insert(Product.create(category, "Sale 50% off_ok", null, "Be", new java.math.BigDecimal("100"), null), 1, 0, 0);
        assertEquals(1, products.search("ao", null, null).size());
        assertEquals(1, products.search("ÁO  thun", null, null).size());
        assertEquals(1, products.search("dam", null, null).size());
        assertEquals(1, products.search("ĐẦM", null, null).size());
        assertEquals(1, products.search("50%", null, null).size());
        assertEquals(1, products.search("_ok", null, null).size());
        assertEquals(0, products.search("%", null, null).stream().filter(p -> p.getName().equals("Áo thun đen")).count());
        assertEquals(3, products.searchForAdmin(null, null).size());
    }

    // Danh sách sản phẩm hết hàng và sản phẩm đã có đơn để các trang hiển thị.
    @Test public void soldOutAndOrderedProductsAreListedForTheViews() {
        Product sold = product("100", 1, 0, 0);
        Product stocked = product("100", 5, 0, 0);
        buy(sold, 1, ADDRESS);
        assertTrue(products.soldOutProductIds().contains(sold.getProductId()));
        assertFalse(products.soldOutProductIds().contains(stocked.getProductId()));
        assertTrue(products.productIdsWithOrders().contains(sold.getProductId()));
        assertFalse(products.productIdsWithOrders().contains(stocked.getProductId()));
    }

    // Danh sách đơn đọc các dòng hàng bằng một truy vấn và gán đúng dòng vào đúng đơn, đơn mới nhất lên đầu.
    @Test public void ordersListReadsLinesInOneQueryAndKeepsThemWithTheirOrder() {
        Product product = product("100", 5, 0, 0);
        Order first = buy(product, 1, ADDRESS);
        Order second = buy(product, 2, ADDRESS);
        List<Order> all = orders.findAll();
        assertEquals(2, all.size());
        assertEquals(second.getOrderId(), all.get(0).getOrderId());
        assertEquals(2, all.get(0).getItems().get(0).getQuantity());
        assertEquals(1, all.get(1).getItems().get(0).getQuantity());
        assertEquals(first.getOrderId(), orders.findByCustomerId(customer.getUserId()).get(1).getOrderId());
        assertTrue(orders.findAll().stream().allMatch(o -> o.getCreatedAt() != null));
    }

    // Đổi sang đúng trạng thái đang có thì không làm gì.
    @Test public void changingToTheSameStatusDoesNothing() {
        Order order = buy(product("100", 5, 0, 0), 1, ADDRESS);
        assertFalse(orderService.changeStatus(order.getOrderId(), OrderStatus.PENDING).changed());
        assertTrue(orderService.changeStatus(order.getOrderId(), OrderStatus.CONFIRMED).changed());
        assertFalse(orderService.changeStatus(order.getOrderId(), OrderStatus.CONFIRMED).changed());
    }

    private Product product(String price, int s, int m, int l) {
        return products.insert(Product.create(category, "Product", null, "Black", new java.math.BigDecimal(price), null), s, m, l);
    }

    private ProductSize size(Product product, String name) {
        return products.findSizes(product.getProductId()).stream().filter(s -> s.getSize().equals(name)).findFirst().orElseThrow();
    }

    private Order buy(Product product, int quantity, ShippingAddress address) {
        Cart cart = new Cart();
        cart.addItem(size(product, "S"), quantity);
        return checkout.checkout(customer, cart, address);
    }

    /** Mỗi size một cái, theo đúng thứ tự dòng hàng được truyền vào. */
    private static Cart cart(ProductSize... sizes) {
        Cart cart = new Cart();
        for (ProductSize size : sizes) cart.addItem(size, 1);
        return cart;
    }

    /** Cho mỗi task chạy trên một thread riêng, bắt đầu cùng một lúc, rồi trả về các lỗi mà task ném ra. */
    private static List<Throwable> runTogether(Runnable... tasks) throws InterruptedException {
        CountDownLatch start = new CountDownLatch(1);
        List<Throwable> failures = Collections.synchronizedList(new ArrayList<>());
        List<Thread> threads = new ArrayList<>();
        for (Runnable task : tasks) {
            Thread thread = new Thread(() -> {
                try {
                    start.await();
                    task.run();
                } catch (Throwable failure) {
                    failures.add(failure);
                }
            });
            thread.start();
            threads.add(thread);
        }
        start.countDown();
        for (Thread thread : threads) thread.join();
        return failures;
    }
}
