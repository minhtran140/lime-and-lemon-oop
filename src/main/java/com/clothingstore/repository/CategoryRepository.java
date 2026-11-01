package com.clothingstore.repository;

import com.clothingstore.config.Database;
import com.clothingstore.domain.product.Category;
import com.clothingstore.exception.EntityNotFoundException;
import com.clothingstore.exception.ValidationException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Toàn bộ câu SQL của danh mục. */
public final class CategoryRepository extends JdbcSupport {
    /** Các cột của danh mục, đặt tên bí danh (alias) để map() đọc; các câu truy vấn có JOIN cũng dùng alias c này. */
    static final String COLUMNS = "c.category_id,c.name AS category_name,c.slug AS category_slug";

    public CategoryRepository(Database database) {
        super(database);
    }

    public Optional<Category> findById(long categoryId) {
        return withConnection("find category", connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT " + COLUMNS + " FROM categories c WHERE c.category_id=?")) {
                statement.setLong(1, categoryId);
                try (ResultSet rows = statement.executeQuery()) {
                    return rows.next() ? Optional.of(map(rows)) : Optional.empty();
                }
            }
        });
    }

    public List<Category> findAll() {
        return withConnection("list categories", connection -> {
            List<Category> result = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT " + COLUMNS + " FROM categories c ORDER BY c.name");
                 ResultSet rows = statement.executeQuery()) {
                while (rows.next()) result.add(map(rows));
            }
            return result;
        });
    }

    /** Mọi danh mục kèm số sản phẩm của từng danh mục, cho trang quản trị. */
    public List<CategoryRow> findAllWithProductCount() {
        return withConnection("list categories with product counts", connection -> {
            List<CategoryRow> result = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT " + COLUMNS + ",COUNT(p.product_id) AS product_count FROM categories c "
                            + "LEFT JOIN products p ON p.category_id=c.category_id "
                            + "GROUP BY c.category_id,c.name,c.slug ORDER BY c.name");
                 ResultSet rows = statement.executeQuery()) {
                while (rows.next()) result.add(new CategoryRow(map(rows), rows.getInt("product_count")));
            }
            return result;
        });
    }

    public Category insert(Category category) {
        return withConnection("insert category", connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO categories(name,slug) VALUES(?,?)", new String[]{"category_id"})) {
                statement.setString(1, category.getName());
                statement.setString(2, category.getSlug());
                statement.executeUpdate();
                category.assignId(generatedId(statement, "category"));
                return category;
            } catch (SQLException exception) {
                if (hasState(exception, UNIQUE_VIOLATION)) throw new ValidationException("Danh mục đã tồn tại.", "name");
                throw exception;
            }
        });
    }

    /** Danh mục còn sản phẩm thì không xóa được. */
    public void delete(long categoryId) {
        withConnection("delete category", connection -> {
            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM categories WHERE category_id=?")) {
                statement.setLong(1, categoryId);
                if (statement.executeUpdate() != 1) throw new EntityNotFoundException("Không tìm thấy danh mục.");
            } catch (SQLException exception) {
                if (hasState(exception, FOREIGN_KEY_VIOLATION)) {
                    throw new ValidationException("Danh mục còn sản phẩm nên không xóa được. Hãy chuyển hoặc xóa các sản phẩm đó trước.");
                }
                throw exception;
            }
            return null;
        });
    }

    static Category map(ResultSet row) throws SQLException {
        return Category.restore(row.getLong("category_id"), row.getString("category_name"), row.getString("category_slug"));
    }

    /** Một danh mục và số sản phẩm của nó; các getter được admin-categories.jsp đọc. */
    public record CategoryRow(Category category, int productCount) {
        public Category getCategory() { return category; }
        public int getProductCount() { return productCount; }
    }
}
