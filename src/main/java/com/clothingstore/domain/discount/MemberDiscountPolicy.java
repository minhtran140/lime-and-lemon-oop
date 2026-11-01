package com.clothingstore.domain.discount;

import com.clothingstore.domain.user.User;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Thành viên (khách đã đăng nhập và được phép thanh toán) được giảm 5%. */
public final class MemberDiscountPolicy implements DiscountPolicy {
    private static final BigDecimal RATE = new BigDecimal("0.05");

    // movePointRight(2): 0.05 thành 5 để hiện "5%" trong nhãn.
    @Override public String label() { return "Giảm giá thành viên (" + RATE.movePointRight(2).intValue() + "%)"; }

    @Override public BigDecimal calculateDiscount(BigDecimal eligibleAmount, User customer) {
        if (!customer.canCheckout()) return BigDecimal.ZERO;
        return eligibleAmount.multiply(RATE).setScale(2, RoundingMode.HALF_UP);
    }
}
