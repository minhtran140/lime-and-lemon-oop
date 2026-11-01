package com.clothingstore.domain.user;

/** Khách hàng: được đặt hàng nhưng không vào trang quản trị (override canCheckout/canManageStore, đa hình). Chỉ tạo qua Customer.create vì constructor là private. */
public final class Customer extends User {
    private Customer(String fullName, String email, String passwordHash) { super(fullName, email, passwordHash); }

    public static Customer create(String fullName, String email, String passwordHash) { return new Customer(fullName, email, passwordHash); }

    @Override public UserRole getRole() { return UserRole.CUSTOMER; }
    @Override public boolean canCheckout() { return true; }
    @Override public boolean canManageStore() { return false; }
}
