package com.clothingstore.domain.order;

import com.clothingstore.exception.ValidationException;

/** Ai nhận đơn và nhận ở đâu. Cả ba trường đều bắt buộc. */
public record ShippingAddress(String recipientName, String phone, String address) {
    // Compact constructor: chạy trước khi các trường được gán, nên kiểm tra và cắt khoảng trắng từng giá trị ngay tại đây.
    public ShippingAddress {
        recipientName = required(recipientName, "Họ tên người nhận", 120, "recipientName");
        phone = required(phone, "Số điện thoại", 30, "phone");
        address = required(address, "Địa chỉ", 500, "address");
    }

    // Record chỉ tự sinh recipientName(); các getter này để JSP/EL đọc được ${address.recipientName}.
    public String getRecipientName() { return recipientName; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }

    private static String required(String value, String label, int max, String field) {
        if (value == null || value.isBlank() || value.trim().length() > max) {
            throw new ValidationException(label + " cần từ 1 đến " + max + " ký tự.", field);
        }
        return value.trim();
    }
}
