<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="app" tagdir="/WEB-INF/tags" %>
<!doctype html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><c:choose><c:when test="${not empty order}">Đơn hàng #<c:out value="${order.orderId}"/></c:when><c:otherwise>Lịch sử đơn hàng</c:otherwise></c:choose> · Lime &amp; Lemon</title>
    <%@ include file="/WEB-INF/views/fragments/font-preload.jspf" %>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/store.css?v=${assetVersion}">
</head>
<body class="storefront">
<%@ include file="/WEB-INF/views/fragments/header.jspf" %>
<main id="main-content" tabindex="-1" class="container orders-page">
    <%@ include file="/WEB-INF/views/fragments/flash.jspf" %>
    <c:choose>
        <c:when test="${not empty order}">
            <header class="order-heading">
                <a class="back-link" href="${pageContext.request.contextPath}/orders">← Lịch sử đơn hàng</a>
                <p class="eyebrow">ĐƠN #<c:out value="${order.orderId}"/><c:if test="${not empty order.createdAt and not empty dateTimeFormat}"> · Đặt ngày <time datetime="${order.createdAt}"><c:out value="${order.createdAt.format(dateTimeFormat)}"/></time></c:if></p>
                <h1>Chi tiết đơn hàng</h1>
            </header>
            <div class="order-detail-layout">
                <section class="form-card order-items-card" aria-labelledby="order-items-title">
                    <h2 id="order-items-title">Sản phẩm</h2>
                    <div class="order-item-list">
                        <c:forEach items="${order.items}" var="i">
                            <article class="order-item-row">
                                <p class="wrap-anywhere"><strong><c:out value="${i.productName}"/></strong><span><c:out value="${i.size}"/> · <c:out value="${i.color}"/> · SL <c:out value="${i.quantity}"/></span></p>
                                <strong><app:money amount="${i.lineTotal}"/></strong>
                            </article>
                        </c:forEach>
                    </div>
                    <div class="order-totals">
                        <p><span>Tạm tính</span><span><app:money amount="${order.subtotal}"/></span></p>
                        <p><span>Giảm giá</span><span>- <app:money amount="${order.discount}"/></span></p>
                        <p class="order-total"><strong>Tổng thanh toán</strong><strong><app:money amount="${order.total}"/></strong></p>
                    </div>
                </section>
                <aside class="summary-card order-shipping-card" aria-labelledby="order-shipping-title">
                    <h2 id="order-shipping-title">Giao hàng</h2>
                    <dl class="order-shipping-details">
                        <div><dt>Người nhận</dt><dd class="wrap-anywhere"><c:out value="${order.shippingAddress.recipientName}"/></dd></div>
                        <div><dt>Số điện thoại</dt><dd class="wrap-anywhere"><c:out value="${order.shippingAddress.phone}"/></dd></div>
                        <div><dt>Địa chỉ</dt><dd class="wrap-anywhere"><c:out value="${order.shippingAddress.address}"/></dd></div>
                        <div><dt>Thanh toán</dt><dd>Thanh toán khi nhận hàng</dd></div>
                        <div><dt>Trạng thái</dt><dd><span class="status-badge status-${order.status}"><app:enumLabel value="${order.status}"/></span></dd></div>
                    </dl>
                </aside>
            </div>
        </c:when>
        <c:otherwise>
            <header class="orders-heading"><p class="eyebrow">TÀI KHOẢN CỦA BẠN</p><h1>Lịch sử đơn hàng</h1></header>
            <c:choose>
                <c:when test="${empty orders}">
                    <div class="empty-state">
                        <h2>Bạn chưa có đơn hàng</h2>
                        <p>Những đơn hàng của bạn sẽ xuất hiện ở đây.</p>
                        <a class="button" href="${pageContext.request.contextPath}/products">Xem sản phẩm</a>
                    </div>
                </c:when>
                <c:otherwise>
                    <p class="muted orders-note">Tất cả đơn thanh toán khi nhận hàng (COD).</p>
                    <div class="table-wrap orders-table-wrap">
                        <table class="orders-table" aria-label="Lịch sử đơn hàng">
                            <thead><tr><th scope="col">Mã</th><th scope="col">Ngày đặt</th><th scope="col">Trạng thái</th><th scope="col" class="numeric">Tổng</th><th scope="col"><span class="visually-hidden">Thao tác</span></th></tr></thead>
                            <tbody>
                                <c:forEach items="${orders}" var="o">
                                    <tr>
                                        <td><a class="order-link" href="${pageContext.request.contextPath}/orders/detail?id=${o.orderId}">#<c:out value="${o.orderId}"/></a></td>
                                        <td><c:if test="${not empty o.createdAt and not empty dateTimeFormat}"><time datetime="${o.createdAt}"><c:out value="${o.createdAt.format(dateTimeFormat)}"/></time></c:if></td>
                                        <td><span class="status-badge status-${o.status}"><app:enumLabel value="${o.status}"/></span></td>
                                        <td class="numeric"><app:money amount="${o.total}"/></td>
                                        <td><a class="order-link" href="${pageContext.request.contextPath}/orders/detail?id=${o.orderId}">Chi tiết<span class="visually-hidden"> đơn #<c:out value="${o.orderId}"/></span></a></td>
                                    </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </c:otherwise>
            </c:choose>
        </c:otherwise>
    </c:choose>
</main>
<%@ include file="/WEB-INF/views/fragments/footer.jspf" %>
</body>
</html>