package com.clothingstore.domain.cart;

import com.clothingstore.domain.product.ProductSize;
import com.clothingstore.exception.OutOfStockException;
import com.clothingstore.exception.ValidationException;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Giỏ hàng của khách, lưu trong session. Giỏ tự kiểm tra tồn kho, nên không bao giờ đòi nhiều hơn số hàng đang có. */
public final class Cart implements Serializable {
    private static final long serialVersionUID = 1L;
    // Các method đều synchronized vì một giỏ trong session có thể bị hai request gọi cùng lúc (hai tab, bấm đúp).
    private final Map<Long, CartItem> items = new LinkedHashMap<>();

    public synchronized void addItem(ProductSize size, int quantity) {
        CartItem existing = items.get(size.getSizeId());
        put(size, (existing == null ? 0 : existing.getQuantity()) + positive(quantity));
    }

    public synchronized void updateQuantity(ProductSize size, int quantity) {
        if (!items.containsKey(size.getSizeId())) {
            throw new ValidationException("Sản phẩm này không còn trong giỏ hàng của bạn.");
        }
        put(size, positive(quantity));
    }

    public synchronized void removeItem(long sizeId) { items.remove(sizeId); }
    public synchronized void clear() { items.clear(); }
    public synchronized boolean isEmpty() { return items.isEmpty(); }
    // List.copyOf trả bản chỉ đọc, code bên ngoài không sửa được danh sách của giỏ (đóng gói).
    public synchronized List<CartItem> getItems() { return List.copyOf(items.values()); }

    public synchronized BigDecimal getSubtotal() {
        return items.values().stream().map(CartItem::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void put(ProductSize size, int quantity) {
        if (size.getStock() < quantity) {
            throw new OutOfStockException(size.getSizeId(), size.getProduct().getName(), size.getSize(), quantity,
                    size.getStock());
        }
        items.put(size.getSizeId(), new CartItem(size.getSizeId(), size.getProduct().getProductId(),
                size.getProduct().getName(), size.getSize(),
                size.getProduct().getColor(), size.getProduct().getPrice(), quantity));
    }

    private static int positive(int quantity) {
        if (quantity <= 0) throw new ValidationException("Số lượng phải lớn hơn 0.");
        if (quantity > 1000) throw new ValidationException("Số lượng yêu cầu quá lớn.");
        return quantity;
    }
}
