package com.clothingstore.domain.cart;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Một dòng trong giỏ: một size sản phẩm, giá lúc thêm vào và số lượng.
 * Giá chỉ để hiển thị trong giỏ; khi thanh toán, CheckoutService tải lại giá từ database.
 */
public final class CartItem implements Serializable {
    private static final long serialVersionUID = 1L;

    private final long sizeId;
    private final String productName;
    private final String size;
    private final String color;
    private final BigDecimal unitPrice;
    private final int quantity;

    private final long productId;

    CartItem(long sizeId, long productId, String productName, String size, String color, BigDecimal unitPrice,
             int quantity) {
        this.sizeId = sizeId;
        this.productId = productId;
        this.productName = productName;
        this.size = size;
        this.color = color;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
    }

    public long getSizeId() { return sizeId; }
    public long getProductId() { return productId; }
    public String getProductName() { return productName; }
    public String getSize() { return size; }
    public String getColor() { return color; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public int getQuantity() { return quantity; }
    public BigDecimal getSubtotal() { return unitPrice.multiply(BigDecimal.valueOf(quantity)); }
}
