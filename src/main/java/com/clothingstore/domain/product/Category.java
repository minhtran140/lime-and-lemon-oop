package com.clothingstore.domain.product;

import com.clothingstore.exception.ValidationException;

import java.text.Normalizer;
import java.util.Locale;

/** Danh mục sản phẩm. Slug (dùng trong đường dẫn) được tạo từ tên, ví dụ "Váy & chân váy" → "vay-chan-vay". */
public final class Category {
    private long categoryId;
    private final String name;
    private final String slug;

    private Category(String name, String slug) {
        this.name = name;
        this.slug = slug;
    }

    public static Category create(String name) {
        if (name == null || name.isBlank() || name.trim().length() > 100) {
            throw new ValidationException("Tên danh mục cần từ 1 đến 100 ký tự.", "name");
        }
        String slug = slugOf(name);
        if (slug.isEmpty()) throw new ValidationException("Tên danh mục cần có chữ hoặc số.", "name");
        return new Category(name.trim(), slug);
    }

    /** Dựng lại danh mục đọc từ database. */
    public static Category restore(long categoryId, String name, String slug) {
        Category category = new Category(name, slug);
        category.categoryId = categoryId;
        return category;
    }

    /** Các từ ASCII viết thường nối bằng "-": bỏ dấu, đ → d, mọi ký tự khác thành dấu phân cách. */
    public static String slugOf(String name) {
        // NFD tách "á" thành "a" + dấu; \p{M} xóa các dấu đó; cuối cùng cắt "-" thừa ở đầu và cuối.
        String noAccents = Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String ascii = noAccents.replace('đ', 'd').replace('Đ', 'D').toLowerCase(Locale.ROOT);
        return ascii.replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
    }

    /** Bằng 0 cho đến khi danh mục được lưu. */
    public long getCategoryId() { return categoryId; }
    public String getName() { return name; }
    public String getSlug() { return slug; }

    public void assignId(long id) { categoryId = id; }
}
