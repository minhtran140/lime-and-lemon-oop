package com.clothingstore.domain.discount;
import com.clothingstore.domain.user.User;
import java.math.BigDecimal;
/**
 * Interface cho mẫu Strategy: CheckoutService chỉ biết kiểu DiscountPolicy, nên đổi MemberDiscountPolicy
 * sang NoDiscountPolicy mà không phải sửa CheckoutService (đa hình).
 */
public interface DiscountPolicy {
    BigDecimal calculateDiscount(BigDecimal eligibleAmount, User customer);

    /** Tên hiển thị cạnh dòng giảm giá, ví dụ "Giảm giá thành viên (5%)". */
    String label();
}
