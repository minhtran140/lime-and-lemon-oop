package com.clothingstore.repository;

import com.clothingstore.config.Database;
import com.clothingstore.domain.product.Product;
import com.clothingstore.domain.product.ProductSize;
import com.clothingstore.exception.EntityNotFoundException;
import com.clothingstore.exception.ValidationException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Toàn bộ câu SQL của sản phẩm và các size của chúng. */
public final class ProductRepository extends JdbcSupport {
    private static final String PRODUCT_COLUMNS =
            "p.product_id,p.name AS product_name,p.description,p.color,p.price,p.image_url,p.active AS product_active,"
            + "p.featured," + CategoryRepository.COLUMNS;
    private static final String PRODUCT_FROM =
            " FROM products p JOIN categories c ON c.category_id=p.category_id ";
    private static final String SIZE_SELECT = "SELECT " + PRODUCT_COLUMNS + ",s.size_id,s.size,s.stock" + PRODUCT_FROM
            + "JOIN product_sizes s ON s.product_id=p.product_id ";

    /** Người mua chỉ thấy các sản phẩm mà admin chưa tắt. */
    private static final String VISIBLE = "WHERE p.active=TRUE ";

    public ProductRepository(Database database) {
        super(database);
    }

    public Optional<Product> findById(long productId) {
        List<Product> found = queryProducts("find product by ID",
                "SELECT " + PRODUCT_COLUMNS + PRODUCT_FROM + "WHERE p.product_id=?", List.of(productId));
        return found.stream().findFirst();
    }

    /** Sản phẩm đang bán có tên chứa từ khóa (không phân biệt hoa thường, bỏ qua dấu), có thể lọc theo một danh mục, theo thứ tự đã chọn. */
    public List<Product> search(String keyword, Long categoryId, String sort) {
        String order = switch (sort == null ? "" : sort.toUpperCase()) {
            case "PRICE_ASC" -> "ORDER BY p.price ASC,p.product_id DESC";
            case "PRICE_DESC" -> "ORDER BY p.price DESC,p.product_id DESC";
            default -> "ORDER BY p.created_at DESC,p.product_id DESC";
        };
        return search("search products", VISIBLE, keyword, categoryId, order);
    }

    /** Cùng bộ lọc đó cho danh sách sản phẩm của admin: gồm cả sản phẩm đã tắt, id mới nhất xếp đầu. */
    public List<Product> searchForAdmin(String keyword, Long categoryId) {
        // "WHERE TRUE" để mọi điều kiện lọc phía sau đều có thể bắt đầu bằng AND.
        return search("list products for administration", "WHERE TRUE ", keyword, categoryId,
                "ORDER BY p.product_id DESC");
    }

    public List<Product> findAllForAdmin() { return searchForAdmin(null, null); }

    private List<Product> search(String operation, String where, String keyword, Long categoryId, String order) {
        String sql = "SELECT " + PRODUCT_COLUMNS + PRODUCT_FROM + where;
        List<Object> parameters = new ArrayList<>();
        String term = likeTerm(keyword);
        if (term != null) {
            // unaccent bỏ dấu ở cả hai phía; ILIKE là LIKE không phân biệt hoa thường; % hai đầu nghĩa là "chứa";
            // ESCAPE cho \% và \_ do likeTerm tạo ra được hiểu là ký tự thật. Trong mã Java, '\\' là một dấu gạch chéo ngược thật.
            sql += "AND unaccent(p.name) ILIKE ('%' || unaccent(?) || '%') ESCAPE '\\' ";
            parameters.add(term);
        }
        if (categoryId != null) {
            sql += "AND p.category_id=? ";
            parameters.add(categoryId);
        }
        return queryProducts(operation, sql + order, parameters);
    }

    /**
     * Giá trị cần gán sau LIKE cho từ khóa người dùng gõ, hoặc null khi không có gì để tìm: chuẩn hóa NFC, giữa
     * các từ chỉ một dấu cách, tối đa 100 ký tự, và \ % _ được escape để chúng khớp đúng chính ký tự đó.
     */
    public static String likeTerm(String keyword) {
        if (keyword == null) return null;
        String term = Normalizer.normalize(keyword, Normalizer.Form.NFC).trim().replaceAll("\\s+", " ");
        if (term.length() > 100) term = term.substring(0, 100).trim();
        if (term.isEmpty()) return null;
        return term.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    /** Id của các sản phẩm có trong ít nhất một đơn hàng, để admin được đề nghị "Ngừng bán" thay vì xóa. */
    public Set<Long> productIdsWithOrders() {
        return idSet("list products with orders", "SELECT DISTINCT s.product_id FROM order_items i "
                + "JOIN product_sizes s ON s.size_id=i.size_id");
    }

    /** Id của các sản phẩm mà cả ba size đều hết hàng. */
    public Set<Long> soldOutProductIds() {
        return idSet("list sold-out products",
                "SELECT product_id FROM product_sizes GROUP BY product_id HAVING SUM(stock)=0");
    }

    private Set<Long> idSet(String operation, String sql) {
        return withConnection(operation, connection -> {
            Set<Long> ids = new HashSet<>();
            try (PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet rows = statement.executeQuery()) {
                while (rows.next()) ids.add(rows.getLong(1));
            }
            return ids;
        });
    }

    /** Số sản phẩm vừa nổi bật vừa đang bán (trang chủ chỉ hiện tối đa một số lượng cố định trong đó). */
    public int countFeatured() {
        return withConnection("count featured products", connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT COUNT(*) FROM products WHERE featured=TRUE AND active=TRUE");
                 ResultSet row = statement.executeQuery()) {
                row.next();
                return row.getInt(1);
            }
        });
    }

    public List<Product> findFeatured(int limit) {
        return queryProducts("list featured products", "SELECT " + PRODUCT_COLUMNS + PRODUCT_FROM + VISIBLE
                + "AND p.featured=TRUE ORDER BY p.created_at DESC,p.product_id DESC LIMIT ?", List.of(limit));
    }

    public List<Product> findNewest(int limit) {
        return queryProducts("list newest products", "SELECT " + PRODUCT_COLUMNS + PRODUCT_FROM + VISIBLE
                + "ORDER BY p.created_at DESC,p.product_id DESC LIMIT ?", List.of(limit));
    }

    public Optional<ProductSize> findSize(long sizeId) {
        return querySizes("find product size", SIZE_SELECT + "WHERE s.size_id=?", sizeId).stream().findFirst();
    }

    /** Nhiều size trong một câu truy vấn, khóa theo size id (id không tồn tại thì đơn giản là không có trong map). */
    public Map<Long, ProductSize> findSizesByIds(Collection<Long> sizeIds) {
        return withConnection("find product sizes", connection -> {
            Map<Long, ProductSize> found = new HashMap<>();
            try (PreparedStatement statement = connection.prepareStatement(SIZE_SELECT + "WHERE s.size_id=ANY(?)")) {
                // Gửi mọi id thành một mảng SQL duy nhất, dùng như IN (...).
                statement.setArray(1, connection.createArrayOf("bigint", sizeIds.toArray()));
                try (ResultSet rows = statement.executeQuery()) {
                    while (rows.next()) {
                        ProductSize size = new ProductSize(rows.getLong("size_id"), mapProduct(rows),
                                rows.getString("size"), rows.getInt("stock"));
                        found.put(size.getSizeId(), size);
                    }
                }
            }
            return found;
        });
    }

    /** Các size của sản phẩm theo thứ tự S, M, L (chúng luôn được thêm vào theo thứ tự đó). */
    public List<ProductSize> findSizes(long productId) {
        return querySizes("list product sizes", SIZE_SELECT + "WHERE p.product_id=? ORDER BY s.size_id", productId);
    }

    /** Mỗi sản phẩm một dòng kèm tồn kho S, M, L, cho bảng tồn kho của admin. */
    public List<StockRow> findStockRows() {
        return withConnection("list stock", connection -> {
            List<StockRow> rows = new ArrayList<>();
            // SUM(CASE WHEN ...) xoay ba dòng S/M/L của mỗi sản phẩm thành ba cột trên một dòng.
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT p.product_id,p.name,"
                            + "SUM(CASE WHEN s.size='S' THEN s.stock ELSE 0 END) AS stock_s,"
                            + "SUM(CASE WHEN s.size='M' THEN s.stock ELSE 0 END) AS stock_m,"
                            + "SUM(CASE WHEN s.size='L' THEN s.stock ELSE 0 END) AS stock_l "
                            + "FROM products p JOIN product_sizes s ON s.product_id=p.product_id "
                            + "GROUP BY p.product_id,p.name ORDER BY p.product_id DESC");
                 ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    rows.add(new StockRow(result.getLong("product_id"), result.getString("name"),
                            result.getInt("stock_s"), result.getInt("stock_m"), result.getInt("stock_l")));
                }
            }
            return rows;
        });
    }

    /** Lưu sản phẩm mới và ba size của nó trong một transaction. */
    public Product insert(Product product, int stockS, int stockM, int stockL) {
        return withTransaction("insert product", connection -> {
            String sql = "INSERT INTO products(category_id,name,description,color,price,image_url,active,featured) "
                    + "VALUES(?,?,?,?,?,?,?,?)";
            try (PreparedStatement statement = connection.prepareStatement(sql, new String[]{"product_id"})) {
                bindProduct(statement, product);
                statement.executeUpdate();
                product.assignId(generatedId(statement, "product"));
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO product_sizes(product_id,size,stock) VALUES(?,?,?)")) {
                int[] stocks = {stockS, stockM, stockL};
                for (int index = 0; index < 3; index++) {
                    statement.setLong(1, product.getProductId());
                    statement.setString(2, ProductSize.SIZES.get(index));
                    statement.setInt(3, stocks[index]);
                    statement.addBatch();
                }
                statement.executeBatch();
            }
            return product;
        });
    }

    public void update(Product product) {
        withConnection("update product", connection -> {
            String sql = "UPDATE products SET category_id=?,name=?,description=?,color=?,price=?,image_url=?,active=?,"
                    + "featured=? WHERE product_id=?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                bindProduct(statement, product);
                statement.setLong(9, product.getProductId());
                if (statement.executeUpdate() != 1) throw new EntityNotFoundException("Không tìm thấy sản phẩm.");
            }
            return null;
        });
    }

    /**
     * Đặt tồn kho S, M, L của một sản phẩm. Chỉ lưu khi tồn kho hiện tại vẫn bằng giá trị admin đã thấy lúc mở trang
     * (điều kiện AND stock=?); nếu khác thì ném lỗi và rollback cả ba size, để form cũ không ghi đè số hàng đã bán trong lúc đó.
     */
    public void updateStock(long productId, int stockS, int stockM, int stockL,
                            int expectedS, int expectedM, int expectedL) {
        withTransaction("update stock", connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE product_sizes SET stock=? WHERE product_id=? AND size=? AND stock=?")) {
                int[] stocks = {stockS, stockM, stockL};
                int[] expected = {expectedS, expectedM, expectedL};
                for (int index = 0; index < 3; index++) {
                    statement.setInt(1, stocks[index]);
                    statement.setLong(2, productId);
                    statement.setString(3, ProductSize.SIZES.get(index));
                    statement.setInt(4, expected[index]);
                    if (statement.executeUpdate() != 1) {
                        throw new ValidationException("Tồn kho đã thay đổi hoặc sản phẩm không còn tồn tại. Vui lòng tải lại trang.");
                    }
                }
            }
            return null;
        });
    }

    /** Xóa sản phẩm (các size đi theo). Sản phẩm đã có trong đơn hàng thì không xóa được. */
    public void delete(long productId) {
        withConnection("delete product", connection -> {
            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM products WHERE product_id=?")) {
                statement.setLong(1, productId);
                if (statement.executeUpdate() != 1) throw new EntityNotFoundException("Không tìm thấy sản phẩm.");
            } catch (SQLException exception) {
                if (hasState(exception, FOREIGN_KEY_VIOLATION)) {
                    throw new ValidationException(
                            "Sản phẩm đã có trong đơn hàng nên không xóa được. Hãy chọn “Ngừng bán” để ẩn sản phẩm khỏi cửa hàng.");
                }
                throw exception;
            }
            return null;
        });
    }

    private List<Product> queryProducts(String operation, String sql, List<Object> parameters) {
        return withConnection(operation, connection -> {
            List<Product> products = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                bind(statement, parameters);
                try (ResultSet rows = statement.executeQuery()) {
                    while (rows.next()) products.add(mapProduct(rows));
                }
            }
            return products;
        });
    }

    private List<ProductSize> querySizes(String operation, String sql, long id) {
        return withConnection(operation, connection -> {
            List<ProductSize> sizes = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, id);
                try (ResultSet rows = statement.executeQuery()) {
                    while (rows.next()) {
                        sizes.add(new ProductSize(rows.getLong("size_id"), mapProduct(rows), rows.getString("size"),
                                rows.getInt("stock")));
                    }
                }
            }
            return sizes;
        });
    }

    private static void bindProduct(PreparedStatement statement, Product product) throws SQLException {
        statement.setLong(1, product.getCategory().getCategoryId());
        statement.setString(2, product.getName());
        statement.setString(3, product.getDescription());
        statement.setString(4, product.getColor());
        statement.setBigDecimal(5, product.getPrice());
        statement.setString(6, product.getImageUrl());
        statement.setBoolean(7, product.isActive());
        statement.setBoolean(8, product.isFeatured());
    }

    private static void bind(PreparedStatement statement, List<Object> parameters) throws SQLException {
        for (int index = 0; index < parameters.size(); index++) statement.setObject(index + 1, parameters.get(index));
    }

    private static Product mapProduct(ResultSet row) throws SQLException {
        Product product = Product.restore(CategoryRepository.map(row), row.getString("product_name"),
                row.getString("description"), row.getString("color"), row.getBigDecimal("price"),
                row.getString("image_url"));
        product.assignId(row.getLong("product_id"));
        product.changeFeatured(row.getBoolean("featured"));
        if (!row.getBoolean("product_active")) product.deactivate();
        return product;
    }

    /** Tồn kho của một sản phẩm theo từng size; các getter được admin.jsp đọc. */
    public record StockRow(long productId, String name, int stockS, int stockM, int stockL) {
        public long getProductId() { return productId; }
        public String getName() { return name; }
        public int getStockS() { return stockS; }
        public int getStockM() { return stockM; }
        public int getStockL() { return stockL; }
        public int getTotal() { return stockS + stockM + stockL; }
    }
}
