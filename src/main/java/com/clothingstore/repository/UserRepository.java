package com.clothingstore.repository;

import com.clothingstore.config.Database;
import com.clothingstore.domain.user.Admin;
import com.clothingstore.domain.user.Customer;
import com.clothingstore.domain.user.User;
import com.clothingstore.domain.user.UserRole;
import com.clothingstore.exception.DataAccessException;
import com.clothingstore.exception.ValidationException;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public final class UserRepository extends JdbcSupport {
    public UserRepository(Database database) {
        super(database);
    }

    public Optional<User> findByEmail(String email) {
        return withConnection("find user by email", connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT user_id,full_name,email,password_hash,role FROM users WHERE lower(email)=lower(?)")) {
                statement.setString(1, email.trim());
                try (ResultSet rows = statement.executeQuery()) {
                    return rows.next() ? Optional.of(map(rows)) : Optional.empty();
                }
            }
        });
    }

    public Optional<User> findById(long userId) {
        return withConnection("find user by ID", connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT user_id,full_name,email,password_hash,role FROM users WHERE user_id=?")) {
                statement.setLong(1, userId);
                try (ResultSet rows = statement.executeQuery()) {
                    return rows.next() ? Optional.of(map(rows)) : Optional.empty();
                }
            }
        });
    }

    public User insert(User user) {
        if (user.getUserId() != 0) throw new IllegalArgumentException("new user must not already have an ID");
        try {
            return withConnection("insert user", connection -> {
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO users(full_name,email,password_hash,role) VALUES(?,?,?,?)",
                        new String[]{"user_id"})) {
                    statement.setString(1, user.getFullName());
                    statement.setString(2, user.getEmail());
                    statement.setString(3, user.getPasswordHash());
                    statement.setString(4, user.getRole().name());
                    statement.executeUpdate();
                    user.assignId(generatedId(statement, "user"));
                    return user;
                }
            });
        } catch (DataAccessException failure) {
            throw translateDuplicateEmail(failure);
        }
    }

    static RuntimeException translateDuplicateEmail(DataAccessException failure) {
        if (failure.getCause() instanceof SQLException sqlFailure
                && hasState(sqlFailure, UNIQUE_VIOLATION)) {
            return new ValidationException("Email này đã được đăng ký. Vui lòng đăng nhập hoặc dùng email khác.", "email", sqlFailure);
        }
        return failure;
    }

    private static User map(ResultSet row) throws java.sql.SQLException {
        String name = row.getString("full_name");
        String email = row.getString("email");
        String hash = row.getString("password_hash");
        // Đọc cột role để dựng đúng lớp con: Customer hoặc Admin.
        User user = switch (UserRole.valueOf(row.getString("role"))) {
            case CUSTOMER -> Customer.create(name, email, hash);
            case ADMIN -> Admin.create(name, email, hash);
        };
        user.assignId(row.getLong("user_id"));
        return user;
    }
}
