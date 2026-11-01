package com.clothingstore.domain.discount;

import com.clothingstore.domain.user.User;

import java.math.BigDecimal;

/** Chiến lược thứ hai: không ai được giảm giá. Thay vào CheckoutService để tắt giảm giá thành viên. */
public final class NoDiscountPolicy implements DiscountPolicy {
    @Override public String label() { return "Giảm giá"; }

    @Override public BigDecimal calculateDiscount(BigDecimal eligibleAmount, User customer) {
        return BigDecimal.ZERO;
    }
}
