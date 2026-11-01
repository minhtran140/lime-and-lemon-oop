package com.clothingstore.domain.order;

import java.math.BigDecimal;

/** Một dòng của đơn hàng. Tên, size, màu và giá được sao chép lúc thanh toán, nên sửa sản phẩm sau này không làm đổi đơn cũ. */
public final class OrderItem {
    private final long sizeId;
    private final String productName;
    private final String size;
    private final String color;
    private final int quantity;
    private final BigDecimal unitPrice;

    public OrderItem(long sizeId, String productName, String size, String color, int quantity, BigDecimal unitPrice) {
        if (sizeId <= 0) throw new IllegalArgumentException("sizeId must be positive");
        if (productName == null || productName.isBlank() || size == null || color == null) {
            throw new IllegalArgumentException("product name, size and colour are required");
        }
        if (quantity <= 0) throw new IllegalArgumentException("quantity must be positive, got " + quantity);
        if (unitPrice == null || unitPrice.signum() < 0) throw new IllegalArgumentException("unit price must be 0 or more");
        this.sizeId = sizeId;
        this.productName = productName;
        this.size = size;
        this.color = color;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public long getSizeId() { return sizeId; }
    public String getProductName() { return productName; }
    public String getSize() { return size; }
    public String getColor() { return color; }
    public int getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getLineTotal() { return unitPrice.multiply(BigDecimal.valueOf(quantity)); }
}
