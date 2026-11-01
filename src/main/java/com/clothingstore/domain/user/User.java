package com.clothingstore.domain.user;

import com.clothingstore.exception.ValidationException;

import java.io.Serializable;
import java.util.Locale;
import java.util.regex.Pattern;

/** Lớp trừu tượng: chỉ có Customer hoặc Admin, không có "người dùng chung" nào để tạo ra (kế thừa + đa hình). */
public abstract class User implements Serializable {
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private long userId;
    private final String fullName;
    private final String email;
    private final String passwordHash;

    protected User(String fullName, String email, String passwordHash) {
        if (fullName == null || fullName.isBlank() || fullName.trim().length() > 120) {
            throw new ValidationException("Họ và tên cần từ 1 đến 120 ký tự.", "fullName");
        }
        this.fullName = fullName.trim();
        this.email = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (!isValidEmail(this.email)) throw new ValidationException("Vui lòng nhập địa chỉ email hợp lệ.", "email");
        this.passwordHash = passwordHash;
    }

    /** Bằng 0 cho đến khi người dùng được lưu. */
    public long getUserId() { return userId; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public abstract UserRole getRole();

    /** Mỗi loại người dùng tự quyết định có được đặt hàng hay không. */
    public abstract boolean canCheckout();

    /** Mỗi loại người dùng tự quyết định có được dùng các trang quản trị hay không. */
    public abstract boolean canManageStore();

    public void assignId(long id) { userId = id; }

    /** Quy tắc email duy nhất: dài tối đa 254 ký tự, có đúng dạng "phần đầu@phần giữa.phần cuối" (không có khoảng trắng, không có thêm dấu @, có ít nhất một dấu chấm sau @). */
    public static boolean isValidEmail(String email) {
        return email.length() <= 254 && EMAIL.matcher(email).matches();
    }
}
