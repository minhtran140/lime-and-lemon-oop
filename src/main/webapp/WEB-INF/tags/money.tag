<%@ tag body-content="empty" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ attribute name="amount" required="true" type="java.math.BigDecimal" %>
<fmt:setLocale value="vi_VN"/>
<fmt:formatNumber value="${amount}" type="currency" currencyCode="VND" minFractionDigits="0" maxFractionDigits="0"/>
