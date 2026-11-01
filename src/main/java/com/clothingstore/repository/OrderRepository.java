package com.clothingstore.repository;

import com.clothingstore.config.Database;
import com.clothingstore.domain.order.Order;
import com.clothingstore.domain.order.OrderItem;
import com.clothingstore.domain.order.OrderStatus;
import com.clothingstore.domain.order.ShippingAddress;
import com.clothingstore.exception.OutOfStockException;
import com.clothingstore.exception.ValidationException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Toàn bộ câu SQL của đơn hàng. Đặt đơn và hủy đơn mỗi việc chạy trong một transaction. */
public final class OrderRepository extends JdbcSupport {
    private static final String SELECT = "SELECT order_id,customer_id,status,recipient_name,recipient_phone,"
            + "shipping_address,discount_amount,created_at FROM orders ";

    public OrderRepository(Database database) {
        super(database);
    }

    public Optional<Order> findById(long orderId) {
        return withConnection("find order", connection ->
                findMany(connection, SELECT + "WHERE order_id=?", orderId).stream().findFirst());
    }

    public Optional<Order> findByIdForCustomer(long orderId, long customerId) {
        return withConnection("find customer order", connection ->
                findMany(connection, SELECT + "WHERE order_id=? AND customer_id=?", orderId, customerId).stream().findFirst());
    }

    public List<Order> findByCustomerId(long customerId) {
        return withConnection("list customer orders", connection ->
                findMany(connection, SELECT + "WHERE customer_id=? ORDER BY order_id DESC", customerId));
    }

    public List<Order> findAll() {
        return withConnection("list orders", connection -> findMany(connection, SELECT + "ORDER BY order_id DESC"));
    }

    /**
     * Trừ kho cho từng dòng hàng và lưu đơn, tất cả trong một transaction. Câu UPDATE kho chỉ khớp khi còn đủ
     * hàng, nên hai người mua không bao giờ mua được cùng một món cuối cùng: người thứ hai nhận
     * OutOfStockException và rollback hoàn tác mọi thứ.
     */
    public void placeOrder(Order order) {
        withTransaction("place order", connection -> {
            for (OrderItem line : inLockOrder(order)) takeFromStock(connection, line);
            insertOrder(connection, order);
            return null;
        });
    }

    /**
     * Các dòng hàng của đơn, sắp theo size_id. Mọi UPDATE bảng product_sizes (mua và hủy) đều đi theo thứ tự
     * này, nên hai transaction cùng đụng các size đó sẽ lock theo cùng một thứ tự và không thể bị deadlock.
     */
    private static List<OrderItem> inLockOrder(Order order) {
        return order.getItems().stream().sorted(Comparator.comparingLong(OrderItem::getSizeId)).toList();
    }

    private static void takeFromStock(Connection connection, OrderItem line) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE product_sizes SET stock = stock - ? WHERE size_id = ? AND stock >= ?")) {
            statement.setInt(1, line.getQuantity());
            statement.setLong(2, line.getSizeId());
            statement.setInt(3, line.getQuantity());
            if (statement.executeUpdate() == 1) return;
        }
        try (PreparedStatement statement = connection.prepareStatement("SELECT stock FROM product_sizes WHERE size_id=?")) {
            statement.setLong(1, line.getSizeId());
            try (ResultSet row = statement.executeQuery()) {
                int available = row.next() ? row.getInt("stock") : 0;
                throw new OutOfStockException(line.getSizeId(), line.getProductName(), line.getSize(), line.getQuantity(), available);
            }
        }
    }

    private static void insertOrder(Connection connection, Order order) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO orders(customer_id,recipient_name,recipient_phone,shipping_address,subtotal,discount_amount,total) "
                        + "VALUES(?,?,?,?,?,?,?)", new String[]{"order_id"})) {
            statement.setLong(1, order.getCustomerId());
            statement.setString(2, order.getShippingAddress().recipientName());
            statement.setString(3, order.getShippingAddress().phone());
            statement.setString(4, order.getShippingAddress().address());
            statement.setBigDecimal(5, order.getSubtotal());
            statement.setBigDecimal(6, order.getDiscount());
            statement.setBigDecimal(7, order.getTotal());
            statement.executeUpdate();
            order.assignId(generatedId(statement, "order"));
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO order_items(order_id,size_id,product_name,size,color,quantity,unit_price,line_total) "
                        + "VALUES(?,?,?,?,?,?,?,?)")) {
            for (OrderItem item : order.getItems()) {
                statement.setLong(1, order.getOrderId());
                statement.setLong(2, item.getSizeId());
                statement.setString(3, item.getProductName());
                statement.setString(4, item.getSize());
                statement.setString(5, item.getColor());
                statement.setInt(6, item.getQuantity());
                statement.setBigDecimal(7, item.getUnitPrice());
                statement.setBigDecimal(8, item.getLineTotal());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    /**
     * Lưu trạng thái mới của đơn, nhưng chỉ khi trạng thái đang lưu vẫn là {@code expectedStatus} (giá trị nơi
     * gọi đã đọc). Nếu một request khác đổi trước thì không lưu gì. Đơn bị hủy trả hàng về kho trong cùng
     * transaction, nên kho chỉ được cộng lại sau khi việc hủy đơn đã thành công.
     */
    public void updateStatus(Order order, OrderStatus expectedStatus) {
        withTransaction("update order status", connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE orders SET status=? WHERE order_id=? AND status=?")) {
                statement.setString(1, order.getStatus().name());
                statement.setLong(2, order.getOrderId());
                statement.setString(3, expectedStatus.name());
                if (statement.executeUpdate() != 1) {
                    throw new ValidationException("Đơn hàng đã thay đổi. Vui lòng tải lại trang.");
                }
            }
            if (order.getStatus() == OrderStatus.CANCELLED) {
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE product_sizes SET stock = stock + ? WHERE size_id = ?")) {
                    for (OrderItem line : inLockOrder(order)) {
                        statement.setInt(1, line.getQuantity());
                        statement.setLong(2, line.getSizeId());
                        statement.executeUpdate();
                    }
                }
            }
            return null;
        });
    }

    /** Các đơn hàng mà câu truy vấn tìm được, kèm mọi dòng hàng đọc bằng thêm một câu truy vấn (không phải mỗi đơn một câu). */
    private static List<Order> findMany(Connection connection, String sql, long... ids) throws SQLException {
        // Header chỉ là chỗ giữ tạm: Order.restore cần có sẵn mọi dòng hàng, mà các dòng được đọc ở câu truy vấn sau.
        record Header(long orderId, long customerId, ShippingAddress address, OrderStatus status, BigDecimal discount,
                      LocalDateTime createdAt) {}
        List<Header> headers = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int index = 0; index < ids.length; index++) statement.setLong(index + 1, ids[index]);
            try (ResultSet row = statement.executeQuery()) {
                while (row.next()) {
                    ShippingAddress address = new ShippingAddress(row.getString("recipient_name"),
                            row.getString("recipient_phone"), row.getString("shipping_address"));
                    headers.add(new Header(row.getLong("order_id"), row.getLong("customer_id"), address,
                            OrderStatus.valueOf(row.getString("status")), row.getBigDecimal("discount_amount"),
                            row.getObject("created_at", LocalDateTime.class)));
                }
            }
        }
        Map<Long, List<OrderItem>> itemsByOrder = new HashMap<>();
        if (!headers.isEmpty()) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT order_id,size_id,product_name,size,color,quantity,unit_price FROM order_items "
                            + "WHERE order_id=ANY(?) ORDER BY order_item_id")) {
                // Gửi mọi order_id thành một mảng SQL duy nhất, dùng như IN (...).
                statement.setArray(1, connection.createArrayOf("bigint",
                        headers.stream().map(Header::orderId).toArray()));
                try (ResultSet lines = statement.executeQuery()) {
                    while (lines.next()) {
                        // Lấy danh sách dòng hàng của đơn này (chưa có thì tạo mới), rồi thêm dòng vào.
                        itemsByOrder.computeIfAbsent(lines.getLong("order_id"), key -> new ArrayList<>())
                                .add(new OrderItem(lines.getLong("size_id"), lines.getString("product_name"),
                                        lines.getString("size"), lines.getString("color"), lines.getInt("quantity"),
                                        lines.getBigDecimal("unit_price")));
                    }
                }
            }
        }
        List<Order> result = new ArrayList<>();
        for (Header header : headers) {
            result.add(Order.restore(header.orderId(), header.customerId(), header.address(), header.status(),
                    header.discount(), header.createdAt(), itemsByOrder.getOrDefault(header.orderId(), List.of())));
        }
        return result;
    }
}
