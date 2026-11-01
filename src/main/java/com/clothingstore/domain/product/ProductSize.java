package com.clothingstore.domain.product;

import java.util.List;

/** Một size (S, M hoặc L) của sản phẩm và số lượng còn trong kho. */
public final class ProductSize {
    public static final List<String> SIZES = List.of("S", "M", "L");

    private final long sizeId;
    private final Product product;
    private final String size;
    private final int stock;

    public ProductSize(long sizeId, Product product, String size, int stock) {
        if (sizeId <= 0) throw new IllegalArgumentException("sizeId must be positive");
        if (product == null) throw new IllegalArgumentException("product is required");
        if (!SIZES.contains(size)) throw new IllegalArgumentException("size must be S, M or L");
        if (stock < 0) throw new IllegalArgumentException("stock must be 0 or more, got " + stock);
        this.sizeId = sizeId;
        this.product = product;
        this.size = size;
        this.stock = stock;
    }

    public long getSizeId() { return sizeId; }
    public Product getProduct() { return product; }
    public String getSize() { return size; }
    public int getStock() { return stock; }
}
