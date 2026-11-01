package com.clothingstore.domain.order;

import com.clothingstore.exception.ValidationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Đơn hàng của khách. Thanh toán luôn là tiền mặt khi nhận hàng. */
public final class Order {
    private long orderId;
    private final long customerId;
    private final ShippingAddress shippingAddress;
    private final List<OrderItem> items = new ArrayList<>();
    private BigDecimal discount = BigDecimal.ZERO;
    private OrderStatus status = OrderStatus.PENDING;
    private LocalDateTime createdAt;

    /** Địa chỉ có thể là null khi trang thanh toán mới chỉ xem trước số tiền. */
    public Order(long customerId, ShippingAddress shippingAddress) {
        if (customerId <= 0) throw new IllegalArgumentException("customerId must be positive");
        this.customerId = customerId;
        this.shippingAddress = shippingAddress;
    }

    /** Dựng lại đơn hàng đọc từ database; các dòng hàng và giảm giá được kiểm tra giống như đơn mới. */
    public static Order restore(long orderId, long customerId, ShippingAddress address, OrderStatus status,
                                BigDecimal discount, LocalDateTime createdAt, List<OrderItem> items) {
        Order order = new Order(customerId, address);
        // Phải thêm dòng hàng và giảm giá trước assignId, vì đơn đã có ID thì bị khóa (requireUnsaved).
        items.forEach(order::addItem);
        order.applyDiscount(discount);
        order.assignId(orderId);
        order.status = Objects.requireNonNull(status, "status");
        order.createdAt = createdAt;
        return order;
    }

    /** Bằng 0 cho đến khi đơn được lưu. */
    public long getOrderId() { return orderId; }
    public long getCustomerId() { return customerId; }
    public ShippingAddress getShippingAddress() { return shippingAddress; }
    public OrderStatus getStatus() { return status; }
    // List.copyOf trả bản chỉ đọc, code bên ngoài không sửa được danh sách dòng hàng (đóng gói).
    public List<OrderItem> getItems() { return List.copyOf(items); }

    /** Giờ hiển thị theo Việt Nam, đổi từ giá trị UTC đã lưu; null cho đến khi được tải từ database. */
    public LocalDateTime getCreatedAt() {
        return createdAt == null ? null : createdAt.atOffset(ZoneOffset.UTC)
                .atZoneSameInstant(ZoneId.of("Asia/Ho_Chi_Minh")).toLocalDateTime();
    }

    /** Chỉ gọi một lần, với ID do database sinh ra. */
    public void assignId(long id) {
        if (id <= 0) throw new IllegalArgumentException("order ID must be positive");
        if (orderId != 0) throw new IllegalStateException("order already has ID " + orderId);
        orderId = id;
    }

    /** Mỗi size một dòng; chỉ thêm được dòng trước khi đơn được lưu. */
    public void addItem(OrderItem item) {
        Objects.requireNonNull(item, "item");
        requireUnsaved();
        if (items.stream().anyMatch(line -> line.getSizeId() == item.getSizeId())) {
            throw new IllegalArgumentException("size " + item.getSizeId() + " is already in this order");
        }
        items.add(item);
    }

    /** Giảm giá nằm trong khoảng từ 0 đến tạm tính, nên tổng tiền không bao giờ âm. */
    public void applyDiscount(BigDecimal amount) {
        requireUnsaved();
        if (amount == null || amount.signum() < 0 || amount.compareTo(getSubtotal()) > 0) {
            throw new IllegalArgumentException("discount must be between 0 and the subtotal, got " + amount);
        }
        discount = amount;
    }

    private void requireUnsaved() {
        if (orderId != 0) throw new IllegalStateException("order " + orderId + " is already saved");
    }

    public BigDecimal getSubtotal() {
        return items.stream().map(OrderItem::getLineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getDiscount() { return discount; }
    public BigDecimal getTotal() { return getSubtotal().subtract(discount); }

    /** OrderStatus quyết định những bước chuyển nào được phép, ví dụ PENDING → CONFIRMED nhưng không bao giờ COMPLETED → PENDING. */
    public void transitionTo(OrderStatus next) {
        if (!status.allows(next)) throw new ValidationException("Không thể chuyển đơn hàng sang trạng thái này.");
        status = next;
    }
}
