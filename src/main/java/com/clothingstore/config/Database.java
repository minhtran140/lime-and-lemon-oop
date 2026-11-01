package com.clothingstore.config;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

/**
 * Cấp JDBC connection. Khi chạy trên Tomcat, connection lấy từ pool khai báo trong META-INF/context.xml (tên JNDI
 * jdbc/LimeAndLemon); ở nơi khác (các test JUnit) thì dùng trực tiếp url, user và password trong db.properties.
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
        } catch (NamingException notUnderTomcat) {
            return fromProperties();
        }
    }

    private static Database fromProperties() {
        Properties properties = new Properties();
        try (InputStream input = Database.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (input == null) throw new IllegalStateException("db.properties is missing from the classpath.");
            properties.load(input);
            Class.forName("org.postgresql.Driver"); // Tomcat không tự đăng ký driver trong WEB-INF/lib
        } catch (IOException | ClassNotFoundException exception) {
            throw new IllegalStateException("Could not set up the database connection.", exception);
        }
        return new Database(null, properties.getProperty("db.url"), properties.getProperty("db.user"),
                properties.getProperty("db.password"));
    }

    /** Kết nối thẳng tới database được chỉ định, không qua pool (dùng cho các test PostgreSQL). */
    public static Database direct(String url, String user, String password) {
        return new Database(null, url, user, password);
    }

    public Connection open() throws SQLException {
        return pool != null ? pool.getConnection() : DriverManager.getConnection(url, user, password);
    }
}
