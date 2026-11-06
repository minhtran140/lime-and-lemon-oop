package com.clothingstore.domain.order;
public enum OrderStatus {
    PENDING("Chờ xác nhận"), CONFIRMED("Đã xác nhận"), SHIPPING("Đang giao"), COMPLETED("Hoàn thành"), CANCELLED("Đã hủy");

    private final String label;

    OrderStatus(String label) { this.label = label; }

    /** Tên tiếng Việt hiển thị cho người dùng. */
    public String getLabel() { return label; }

    public boolean allows(OrderStatus next) {
        if (next == null) return false;
        return switch (this) {
            case PENDING -> next == CONFIRMED || next == CANCELLED;
            case CONFIRMED -> next == SHIPPING || next == CANCELLED;
            case SHIPPING -> next == COMPLETED;
            case COMPLETED, CANCELLED -> false;
        };
    }
}
