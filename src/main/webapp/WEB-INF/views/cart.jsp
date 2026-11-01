<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="app" tagdir="/WEB-INF/tags" %>
<!doctype html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Giỏ hàng · Lime &amp; Lemon</title>
    <%@ include file="/WEB-INF/views/fragments/font-preload.jspf" %>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/store.css?v=${assetVersion}">
</head>
<body class="storefront">
<%@ include file="/WEB-INF/views/fragments/header.jspf" %>
<main id="main-content" tabindex="-1" class="container cart-page">
    <%@ include file="/WEB-INF/views/fragments/flash.jspf" %>
    <a class="back-link" href="${pageContext.request.contextPath}/products">← Tiếp tục mua sắm</a>
    <h1>Giỏ hàng</h1>
    <c:choose>
        <c:when test="${empty cartItems}">
            <div class="empty-state">
                <h2>Giỏ hàng đang trống</h2>
                <p>Những sản phẩm bạn thêm vào sẽ nằm ở đây.</p>
                <a class="button" href="${pageContext.request.contextPath}/products">Xem sản phẩm</a>
            </div>
        </c:when>
        <c:otherwise>
            <div class="cart-layout">
                <section class="cart-items" aria-labelledby="cart-items-title">
                    <h2 id="cart-items-title">Sản phẩm trong giỏ</h2>
                    <div class="cart-line-list">
                        <c:forEach items="${cartItems}" var="i">
                            <article class="cart-line">
                                <div class="cart-line-product">
                                    <h3><a href="${pageContext.request.contextPath}/product?id=${i.productId}&amp;size=<c:out value='${i.size}'/>"><c:out value="${i.productName}"/></a></h3>
                                    <p><span><c:out value="${i.size}"/></span> · <span><c:out value="${i.color}"/></span></p>
                                </div>
                                <div class="cart-line-unit-price">
                                    <span class="cart-field-label">Đơn giá</span>
                                    <strong><app:money amount="${i.unitPrice}"/></strong>
                                </div>
                                <form class="cart-line-update" method="post" action="${pageContext.request.contextPath}/cart/update">
                                    <input type="hidden" name="sizeId" value="${i.sizeId}">
                                    <label for="cart-quantity-${i.sizeId}">Số lượng <span class="visually-hidden"><c:out value="${i.productName}"/></span>
                                        <input id="cart-quantity-${i.sizeId}" type="number" name="quantity" min="1" value="${i.quantity}">
                                    </label>
                                    <button class="secondary" type="submit" data-busy-label="Đang cập nhật…">Cập nhật<span class="visually-hidden"> <c:out value="${i.productName}"/> (<c:out value="${i.size}"/>)</span></button>
                                </form>
                                <div class="cart-line-subtotal">
                                    <span class="cart-field-label">Tạm tính</span>
                                    <strong><app:money amount="${i.subtotal}"/></strong>
                                </div>
                                <form class="cart-line-remove" method="post" action="${pageContext.request.contextPath}/cart/remove">
                                    <input type="hidden" name="sizeId" value="${i.sizeId}">
                                    <button class="link-button danger" type="submit"><svg aria-hidden="true" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M6 6l12 12M18 6 6 18"></path></svg><span class="visually-hidden">Xóa <c:out value="${i.productName}"/> khỏi giỏ</span></button>
                                </form>
                            </article>
                        </c:forEach>
                    </div>
                </section>
                <aside class="cart-summary" aria-labelledby="cart-summary-title">
                    <h2 id="cart-summary-title">Tóm tắt đơn hàng</h2>
                    <p class="cart-summary-total"><span>Tạm tính</span><strong><app:money amount="${cartSubtotal}"/></strong></p>
                    <c:choose>
                        <c:when test="${empty sessionScope.user}"><p class="cart-summary-note">Đăng nhập để được giảm 5% khi đặt hàng. <a href="${pageContext.request.contextPath}/account/login">Đăng nhập</a></p></c:when>
                        <c:when test="${not empty discountLabel}"><p class="cart-summary-note"><c:out value="${discountLabel}"/> được áp dụng ở bước đặt hàng.</p></c:when>
                    </c:choose>
                    <div class="cart-summary-actions">
                        <c:choose>
                            <c:when test="${sessionScope.user.role == 'ADMIN'}"><p class="admin-order-note">Tài khoản quản trị không đặt hàng. Hãy dùng tài khoản khách để mua hàng.</p></c:when>
                            <c:otherwise><a class="button button-primary" href="${pageContext.request.contextPath}/checkout">Tiến hành đặt hàng</a></c:otherwise>
                        </c:choose>
                        <form method="post" action="${pageContext.request.contextPath}/cart/clear">
                            <details class="confirm-inline">
                                <summary class="button link-button">Xóa giỏ hàng</summary>
                                <p class="confirm-inline-text">Xóa tất cả sản phẩm khỏi giỏ hàng?</p>
                                <button class="button danger" type="submit" data-busy-label="Đang xóa…">Xác nhận xóa giỏ hàng</button>
                            </details>
                        </form>
                    </div>
                </aside>
            </div>
        </c:otherwise>
    </c:choose>
</main>
<%@ include file="/WEB-INF/views/fragments/footer.jspf" %>
</body>
</html>