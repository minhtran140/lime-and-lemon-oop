package com.clothingstore.domain.product;

import com.clothingstore.exception.ValidationException;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Product {
    private Long productId;
    private Category category;
    private String name;
    private String description;
    private String color;
    private BigDecimal price;
    private String imageUrl;
    private boolean active = true;
    private boolean featured;
    private long version;

    private Product(Category category, String name, String description, String color) {
        changeCategory(category);
        rename(name);
        changeDescription(description);
        changeColor(color);
    }

    public static Product create(Category category, String name, String description, String color,
                                 BigDecimal price, String imageUrl) {
        Product product = new Product(category, name, description, color);
        product.changePrice(price);
        product.changeImageUrl(imageUrl);
        return product;
    }

    /** Dựng lại sản phẩm đọc từ database. Quy tắc về giá và ảnh chỉ áp dụng cho dữ liệu nhập mới, nên các dòng cũ vẫn tải được. */
    public static Product restore(Category category, String name, String description, String color,
                                  BigDecimal price, String imageUrl) {
        Product product = new Product(category, name, description, color);
        product.price = price;
        product.imageUrl = imageUrl;
        return product;
    }

    /** Bằng 0 cho đến khi sản phẩm được lưu. */
    public long getProductId() { return productId == null ? 0 : productId; }
    public Category getCategory() { return category; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getColor() { return color; }
    public BigDecimal getPrice() { return price; }
    public String getImageUrl() { return imageUrl; }
    public boolean isActive() { return active; }

    /** Sản phẩm nổi bật được hiển thị ở trang chủ. */
    public boolean isFeatured() { return featured; }

    public boolean isSellable() { return active; }

    /** Đổi mỗi khi dòng được cập nhật trong database (dùng để phát hiện form sửa đã cũ); bằng 0 cho tới khi đọc từ database. */
    public long getVersion() { return version; }

    public void assignId(long id) { productId = id; }
    public void assignVersion(long value) { version = value; }

    public void rename(String value) { name = required(value, "Tên sản phẩm", 160, "name"); }
    public void changeColor(String value) { color = required(value, "Màu", 40, "color"); }
    public void changeCategory(Category value) {
        if (value == null) throw new ValidationException("Vui lòng chọn danh mục.", "categoryId");
        category = value;
    }
    public void changeDescription(String value) { description = value == null || value.isBlank() ? null : value.trim(); }

    /** Rỗng (không có ảnh), địa chỉ https://, hoặc đường dẫn dưới /assets/. */
    public void changeImageUrl(String value) {
        String url = value == null ? "" : value.trim();
        // Cột image_url là VARCHAR(1000): kiểm tra ở đây để admin nhận thông báo rõ ràng thay vì lỗi cơ sở dữ liệu.
        if (url.length() > 1000) throw new ValidationException("Địa chỉ ảnh không được dài quá 1000 ký tự.", "imageUrl");
        boolean ok = url.isEmpty() || url.startsWith("/assets/") || (url.startsWith("https://") && url.length() > 8);
        if (!ok) throw new ValidationException("Địa chỉ ảnh cần bắt đầu bằng https:// hoặc /assets/.", "imageUrl");
        imageUrl = url.isEmpty() ? null : url;
    }

    public void changePrice(BigDecimal value) {
        if (value == null) throw new ValidationException("Giá phải lớn hơn 0.", "price");
        // Làm tròn trước rồi mới kiểm tra: 0.004 thành 0.00 nên bị từ chối, 9999999999.995 thành 10000000000.00 nên vượt giới hạn.
        BigDecimal rounded = value.setScale(2, RoundingMode.HALF_UP);
        if (rounded.signum() <= 0) throw new ValidationException("Giá phải lớn hơn 0.", "price");
        // Giới hạn này khớp cột price NUMERIC(12,2) trong schema.sql (tối đa 10 chữ số phần nguyên).
        if (rounded.compareTo(new BigDecimal("9999999999.99")) > 0) {
            throw new ValidationException("Giá không được vượt quá 9.999.999.999 đồng.", "price");
        }
        price = rounded;
    }
    public void changeFeatured(boolean value) { featured = value; }
    public void deactivate() { active = false; }
    public void activate() { active = true; }

    private static String required(String value, String label, int max, String field) {
        if (value == null || value.isBlank() || value.trim().length() > max) {
            throw new ValidationException(label + " cần từ 1 đến " + max + " ký tự.", field);
        }
        return value.trim();
    }
}
