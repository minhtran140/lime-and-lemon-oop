package com.clothingstore.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Lưu mật khẩu dưới dạng hex SHA-256. Cố ý làm đơn giản cho bài demo môn học; cửa hàng thật dùng hash chậm có salt. */
public final class Passwords {
    private Passwords() {}

    public static String sha256(String password) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(password.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
