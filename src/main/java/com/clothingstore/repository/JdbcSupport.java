package com.clothingstore.repository;

import com.clothingstore.config.Database;
import com.clothingstore.exception.DataAccessException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Mở connection (hoặc transaction) quanh một đoạn việc JDBC và đổi lỗi SQL thành DataAccessException.
 * Các repository kế thừa lớp này để dùng chung phần mở/đóng connection và xử lý lỗi, không phải lặp lại ở từng repository.
 */
abstract class JdbcSupport {
    static final String UNIQUE_VIOLATION = "23505";
    static final String FOREIGN_KEY_VIOLATION = "23503";

    /**
     * Một đơn vị công việc JDBC: nhận Connection và trả về một giá trị kiểu T (kiểu tổng quát, ví dụ List<Product> hay Long).
     * Repository truyền nó dưới dạng lambda {@code connection -> {...}}; việc không có kết quả thì trả null.
     */
    @FunctionalInterface
    interface SqlWork<T> {
        T apply(Connection connection) throws SQLException;
    }

    private final Database database;

    JdbcSupport(Database database) {
        this.database = database;
    }

    /** Mở connection, chạy công việc, tự đóng connection nhờ try-with-resources, và đổi SQLException thành DataAccessException kèm tên thao tác. */
    final <T> T withConnection(String operation, SqlWork<T> work) {
        try (Connection connection = database.open()) {
            return work.apply(connection);
        } catch (SQLException exception) {
            throw new DataAccessException("Lỗi cơ sở dữ liệu: " + operation, exception);
        }
    }

    /** Chạy công việc trong một transaction: hoặc lưu tất cả, hoặc (khi có bất kỳ exception nào) không lưu gì. */
    final <T> T withTransaction(String operation, SqlWork<T> work) {
        return withConnection(operation, connection -> {
            connection.setAutoCommit(false);
            try {
                T result = work.apply(connection);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException failure) {
                connection.rollback();
                throw failure;
            }
        });
    }

    /** Mã lỗi PostgreSQL: 23505 = trùng khóa, 23503 = vẫn còn dòng khác tham chiếu tới. */
    static boolean hasState(SQLException exception, String sqlState) {
        return sqlState.equals(exception.getSQLState());
    }

    static long generatedId(PreparedStatement statement, String entity) throws SQLException {
        try (ResultSet keys = statement.getGeneratedKeys()) {
            if (!keys.next()) throw new SQLException("Database did not return a generated " + entity + " ID.");
            return keys.getLong(1);
        }
    }
}
