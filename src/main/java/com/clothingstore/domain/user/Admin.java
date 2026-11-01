package com.clothingstore.domain.user;

/** Quản trị viên: không được đặt hàng nhưng quản lý cửa hàng (override canCheckout/canManageStore, đa hình). Chỉ tạo qua Admin.create vì constructor là private. */
public final class Admin extends User {
    private Admin(String fullName, String email, String passwordHash) { super(fullName, email, passwordHash); }

    public static Admin create(String fullName, String email, String passwordHash) { return new Admin(fullName, email, passwordHash); }

    @Override public UserRole getRole() { return UserRole.ADMIN; }
    @Override public boolean canCheckout() { return false; }
    @Override public boolean canManageStore() { return true; }
}
