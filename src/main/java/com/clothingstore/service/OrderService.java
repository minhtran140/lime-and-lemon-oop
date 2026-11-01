package com.clothingstore.service;

import com.clothingstore.domain.order.Order;
import com.clothingstore.domain.order.OrderStatus;
import com.clothingstore.domain.user.User;
import com.clothingstore.exception.EntityNotFoundException;
import com.clothingstore.repository.OrderRepository;
import com.clothingstore.repository.UserRepository;

import java.util.List;

public final class OrderService {
    private final OrderRepository orders;
    private final UserRepository users;

    public OrderService(OrderRepository orders, UserRepository users) {
        this.orders = orders;
        this.users = users;
    }

    public List<Order> customerHistory(long customerId) {
        return orders.findByCustomerId(customerId);
    }

    public Order customerOrder(long customerId, long orderId) {
        return orders.findByIdForCustomer(orderId, customerId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đơn hàng."));
    }

    public Order adminOrder(long orderId) {
        return orders.findById(orderId).orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đơn hàng."));
    }

    /** Người đã đặt đơn, cho trang đơn hàng của admin; null nếu tài khoản không còn tồn tại. */
    public User customerOf(Order order) {
        return users.findById(order.getCustomerId()).orElse(null);
    }

    /** Mọi đơn hàng, hoặc chỉ những đơn có trạng thái này khi có truyền trạng thái. */
    public List<Order> allOrdersForAdmin(OrderStatus status) {
        List<Order> all = orders.findAll();
        return status == null ? all : all.stream().filter(order -> order.getStatus() == status).toList();
    }

    /** Đơn hàng sau khi đổi và trạng thái trước đó của nó; {@code changed()} là false khi trạng thái được yêu cầu trùng với trạng thái hiện tại. */
    public record StatusChange(Order order, OrderStatus before) {
        public boolean changed() { return order.getStatus() != before; }
    }

    /** Yêu cầu đúng trạng thái đơn đang có thì không đổi gì. Đối tượng đơn hàng kiểm tra bước chuyển có được phép không; hủy đơn sẽ trả hàng về kho. */
    public StatusChange changeStatus(long orderId, OrderStatus nextStatus) {
        Order order = adminOrder(orderId);
        OrderStatus expectedStatus = order.getStatus();
        if (expectedStatus == nextStatus) return new StatusChange(order, expectedStatus);
        order.transitionTo(nextStatus);
        orders.updateStatus(order, expectedStatus);
        return new StatusChange(order, expectedStatus);
    }
}
