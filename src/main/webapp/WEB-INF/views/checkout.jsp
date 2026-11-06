<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="app" tagdir="/WEB-INF/tags" %>
<!doctype html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Đặt hàng · Lime &amp; Lemon</title>
    <%@ include file="/WEB-INF/views/fragments/font-preload.jspf" %>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/store.css?v=${assetVersion}">
</head>
<body class="storefront">
<%@ include file="/WEB-INF/views/fragments/header.jspf" %>
<main id="main-content" tabindex="-1" class="container checkout-page">
    <%@ include file="/WEB-INF/views/fragments/flash.jspf" %>
    <h1>Thông tin giao hàng</h1>
    <c:choose>
        <c:when test="${empty checkoutSummary}">
            <div class="empty-state">
                <h2>Giỏ hàng đang trống</h2>
                <p>Thêm sản phẩm vào giỏ để tiếp tục đặt hàng.</p>
                <a class="button" href="${pageContext.request.contextPath}/products">Xem sản phẩm</a>
            </div>
        </c:when>
        <c:otherwise>
            <c:if test="${not empty checkoutError}">
                <div class="form-error-summary" tabindex="-1" autofocus data-focus-error-summary role="alert" aria-labelledby="checkout-error-title">
                    <h2 id="checkout-error-title">Chưa đặt được hàng</h2>
                    <p><c:out value="${checkoutError}"/></p>
                    <c:if test="${checkoutErrorSizeId gt 0}"><p><a href="${pageContext.request.contextPath}/cart">Quay lại giỏ hàng để chỉnh số lượng</a></p></c:if>
                </div>
            </c:if>
            <div class="checkout-grid">
                <form class="form-card checkout-form" method="post" action="${pageContext.request.contextPath}/checkout">
                    <%-- quoteId: khóa phía server của giá/số lượng đang hiển thị; giá đổi thì server báo lỗi và hiển thị lại. --%>
                    <input type="hidden" name="quoteId" value="<c:out value='${quoteId}'/>">
                    <h2>Thông tin nhận hàng</h2>
                    <p class="form-required-note">Tất cả các mục đều bắt buộc.</p>
                    <label>Người nhận<input id="recipientName" name="recipientName" required pattern=".*\S.*" title="Vui lòng nhập tên người nhận" maxlength="120" autocomplete="name" value="<c:out value='${param.recipientName}'/>"<c:if test="${errorField eq 'recipientName'}"> aria-invalid="true" aria-describedby="recipientName-error"</c:if>></label>
                    <c:if test="${errorField eq 'recipientName'}"><p class="field-error" id="recipientName-error"><c:out value="${checkoutError}"/></p></c:if>
                    <label>Số điện thoại<input id="phone" type="tel" name="phone" required pattern="[\s.\-]*(\+84|0)([\s.\-]*\d){9}[\s.\-]*" title="Số điện thoại gồm 10 chữ số, bắt đầu bằng 0 hoặc +84" maxlength="30" autocomplete="tel" inputmode="tel" value="<c:out value='${param.phone}'/>"<c:if test="${errorField eq 'phone'}"> aria-invalid="true" aria-describedby="phone-error"</c:if>></label>
                    <c:if test="${errorField eq 'phone'}"><p class="field-error" id="phone-error"><c:out value="${checkoutError}"/></p></c:if>
                    <label>Địa chỉ<textarea id="address" name="address" required maxlength="500" title="Vui lòng nhập địa chỉ" autocomplete="street-address"<c:if test="${errorField eq 'address'}"> aria-invalid="true" aria-describedby="address-error"</c:if>><c:out value="${param.address}"/></textarea></label>
                    <c:if test="${errorField eq 'address'}"><p class="field-error" id="address-error"><c:out value="${checkoutError}"/></p></c:if>
                    <p class="muted">Thanh toán khi nhận hàng (COD).</p>
                    <button class="button-primary" type="submit" data-busy-label="Đang đặt hàng…">Đặt hàng</button>
                </form>
                <aside class="summary-card checkout-summary" aria-labelledby="checkout-summary-title">
                    <h2 id="checkout-summary-title">Đơn của bạn</h2>
                    <div class="checkout-lines">
                        <c:forEach items="${checkoutSummary.items}" var="line">
                            <p<c:if test="${checkoutErrorSizeId gt 0 and line.sizeId eq checkoutErrorSizeId}"> class="is-problem"</c:if>><span class="wrap-anywhere"><c:out value="${line.productName}"/> · <c:out value="${line.size}"/> · <c:out value="${line.color}"/> · <c:out value="${line.quantity}"/></span><span><app:money amount="${line.lineTotal}"/></span></p>
                        </c:forEach>
                    </div>
                    <hr>
                    <p><strong>Tạm tính</strong><strong><app:money amount="${checkoutSummary.subtotal}"/></strong></p>
                    <c:if test="${checkoutSummary.discount.signum() gt 0}"><p><span><c:out value="${empty discountLabel ? 'Giảm giá' : discountLabel}"/></span><span>- <app:money amount="${checkoutSummary.discount}"/></span></p></c:if>
                    <p class="checkout-total"><strong>Tổng thanh toán</strong><strong><app:money amount="${checkoutSummary.total}"/></strong></p>
                </aside>
            </div>
        </c:otherwise>
    </c:choose>
</main>
<%@ include file="/WEB-INF/views/fragments/footer.jspf" %>
</body>
</html>
