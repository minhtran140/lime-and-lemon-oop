package com.clothingstore.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

/**
 * Cấp JDBC connection. Khi chạy trên Tomcat, connection lấy từ pool khai báo trong META-INF/context.xml (tên JNDI
 * jdbc/LimeAndLemon); các test JUnit không qua pool mà kết nối bằng {@link #direct}.
 */
public final class Database {
    private static final String POOL_NAME = "java:comp/env/jdbc/LimeAndLemon";

    private final DataSource pool;
    private final String url;
    private final String user;
    private final String password;

    private Database(DataSource pool, String url, String user, String password) {
        this.pool = pool;
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public static Database load() {
        try {
            return new Database((DataSource) new InitialContext().lookup(POOL_NAME), null, null, null);
        } catch (NamingException exception) {
            throw new IllegalStateException("Pool jdbc/LimeAndLemon not found: the app runs under Tomcat with META-INF/context.xml.", exception);
        }
    }

    /** Kết nối thẳng tới database được chỉ định, không qua pool (dùng cho các test PostgreSQL). */
    public static Database direct(String url, String user, String password) {
        return new Database(null, url, user, password);
    }

    public Connection open() throws SQLException {
        return pool != null ? pool.getConnection() : DriverManager.getConnection(url, user, password);
    }
}
