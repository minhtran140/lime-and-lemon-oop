package com.clothingstore.service;

import com.clothingstore.domain.product.Category;
import com.clothingstore.domain.product.Product;
import com.clothingstore.domain.product.ProductSize;
import com.clothingstore.exception.EntityNotFoundException;
import com.clothingstore.exception.ValidationException;
import com.clothingstore.repository.CategoryRepository;
import com.clothingstore.repository.CategoryRepository.CategoryRow;
import com.clothingstore.repository.ProductRepository;
import com.clothingstore.repository.ProductRepository.StockRow;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Danh mục hàng cho người mua và việc quản lý sản phẩm/danh mục cho admin. */
public final class ProductService {
    public static final int FEATURED_LIMIT = 8;
    private static final int NEWEST_LIMIT = 4;

    private final CategoryRepository categories;
    private final ProductRepository products;

    public ProductService(CategoryRepository categories, ProductRepository products) {
        this.categories = categories;
        this.products = products;
    }

    // Phía cửa hàng

    public List<Category> categories() { return categories.findAll(); }

    public List<Product> search(String keyword, Long categoryId, String sort) {
        return products.search(keyword, categoryId, sort);
    }

    /** Các sản phẩm admin đánh dấu nổi bật, cho trang chủ. */
    public List<Product> featuredProducts() { return products.findFeatured(FEATURED_LIMIT); }
    public List<Product> newestProducts() { return products.findNewest(NEWEST_LIMIT); }

    /** Id của các sản phẩm hết hàng ở mọi size, để hiện nhãn "Hết hàng" trên thẻ sản phẩm. */
    public Set<Long> soldOutProductIds() { return products.soldOutProductIds(); }

    public ProductDetails productDetails(long productId) {
        Product product = products.findById(productId).filter(Product::isSellable)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sản phẩm."));
        return new ProductDetails(product, products.findSizes(productId));
    }

    /** Size mà người mua đã chọn, nếu sản phẩm của nó vẫn đang bán. */
    public ProductSize sellableSize(long sizeId) {
        return products.findSize(sizeId).filter(size -> size.getProduct().isSellable())
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sản phẩm."));
    }

    /** Một sản phẩm kèm các size của nó; các getter được product.jsp đọc. */
    public record ProductDetails(Product product, List<ProductSize> sizes) {
        public Product getProduct() { return product; }
        public List<ProductSize> getSizes() { return sizes; }
    }

    // Phía quản trị

    public List<CategoryRow> categoryRows() { return categories.findAllWithProductCount(); }

    /** Danh sách sản phẩm của admin, có thể thu hẹp theo tên (bỏ qua dấu và hoa thường) và danh mục. */
    public List<Product> allProducts(String keyword, Long categoryId) {
        return products.searchForAdmin(keyword, categoryId);
    }

    /** Các dòng tồn kho của đúng những sản phẩm mà {@link #allProducts} trả về với các bộ lọc này. */
    public List<StockRow> stockRows(String keyword, Long categoryId) {
        List<StockRow> rows = products.findStockRows();
        if (keyword == null && categoryId == null) return rows;
        Set<Long> shown = allProducts(keyword, categoryId).stream().map(Product::getProductId)
                .collect(Collectors.toSet());
        return rows.stream().filter(row -> shown.contains(row.productId())).toList();
    }

    public Set<Long> productIdsWithOrders() { return products.productIdsWithOrders(); }
    public int featuredCount() { return products.countFeatured(); }

    public Category createCategory(String name) { return categories.insert(Category.create(name)); }

    public Category deleteCategory(long categoryId) {
        Category category = findCategory(categoryId);
        categories.delete(categoryId);
        return category;
    }

    public Product createProduct(long categoryId, String name, String description, String color, BigDecimal price,
                                 String imageUrl, boolean featured, int stockS, int stockM, int stockL) {
        Product product = Product.create(findCategory(categoryId), name, description, color, price, imageUrl);
        product.changeFeatured(featured);
        return products.insert(product, checkStock(stockS, "stockS", "S"), checkStock(stockM, "stockM", "M"),
                checkStock(stockL, "stockL", "L"));
    }

    public Product updateProduct(long productId, long categoryId, String name, String description, String color,
                                 BigDecimal price, String imageUrl, boolean active, boolean featured,
                                 long expectedVersion) {
        Product product = findProduct(productId);
        product.changeCategory(findCategory(categoryId));
        product.rename(name);
        product.changeDescription(description);
        product.changeColor(color);
        product.changePrice(price);
        product.changeImageUrl(imageUrl);
        product.changeFeatured(featured);
        if (active) product.activate(); else product.deactivate();
        // Phiên bản là của form admin đã mở, không phải của bản vừa đọc ở trên: so sánh với form mới phát hiện được sửa chồng.
        products.update(product, expectedVersion);
        return product;
    }

    /** Tắt một sản phẩm: người mua không còn thấy nó, các đơn cũ vẫn giữ nó. */
    public Product deactivateProduct(long productId) {
        Product product = findProduct(productId);
        products.deactivate(productId);
        product.deactivate();
        return product;
    }

    public Product updateStock(long productId, int stockS, int stockM, int stockL,
                               int expectedS, int expectedM, int expectedL) {
        Product product = findProduct(productId);
        products.updateStock(productId, checkStock(stockS, "stockS", "S"), checkStock(stockM, "stockM", "M"),
                checkStock(stockL, "stockL", "L"), checkStock(expectedS, "stockS", "S"),
                checkStock(expectedM, "stockM", "M"), checkStock(expectedL, "stockL", "L"));
        return product;
    }

    public Product deleteProduct(long productId) {
        Product product = findProduct(productId);
        products.delete(productId);
        return product;
    }

    private static int checkStock(int stock, String field, String size) {
        if (stock < 0) throw new ValidationException("Tồn cỡ " + size + " không được âm.", field);
        return stock;
    }

    private Category findCategory(long categoryId) {
        return categories.findById(categoryId).orElseThrow(() -> new EntityNotFoundException("Không tìm thấy danh mục."));
    }

    private Product findProduct(long productId) {
        return products.findById(productId).orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sản phẩm."));
    }
}
