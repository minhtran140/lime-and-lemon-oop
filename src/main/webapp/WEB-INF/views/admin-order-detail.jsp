<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="app" tagdir="/WEB-INF/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="adminSection" value="orders"/>
<c:set var="canMove" value="false"/>
<c:set var="canCancel" value="false"/>
<c:forEach items="${orderStatuses}" var="status"><c:if test="${order.status.allows(status)}"><c:choose>
    <c:when test="${status.name() eq 'CANCELLED'}"><c:set var="canCancel" value="true"/></c:when>
    <c:otherwise><c:set var="canMove" value="true"/></c:otherwise>
</c:choose></c:if></c:forEach>
<!doctype html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Đơn hàng #<c:out value="${order.orderId}"/> · Quản trị · Lime &amp; Lemon</title>
    <%@ include file="/WEB-INF/views/fragments/font-preload.jspf" %>
    <link rel="stylesheet" href="${ctx}/assets/store.css?v=${assetVersion}">
    <link rel="stylesheet" href="${ctx}/assets/admin.css?v=${assetVersion}">
    <script src="${ctx}/assets/admin.js?v=${assetVersion}" defer></script>
</head>
<body class="admin-shell">
<%@ include file="/WEB-INF/views/fragments/header.jspf" %>
<main id="main-content" tabindex="-1" class="container admin-page">
    <%@ include file="/WEB-INF/views/fragments/flash.jspf" %>
    <div class="admin-header">
        <div>
            <h1>Đơn hàng #<c:out value="${order.orderId}"/></h1>
            <p>Ngày đặt <c:choose><c:when test="${empty order.createdAt}">—</c:when><c:otherwise>${order.createdAt.format(dateTimeFormat)}</c:otherwise></c:choose></p>
        </div>
        <div class="admin-header-status">
            <span class="status-badge status-${order.status}"><c:out value="${order.status.label}"/></span>
            <c:if test="${canMove}">
                <form class="admin-inline status-control" method="post" action="${ctx}/admin/order/status">
                    <input type="hidden" name="orderId" value="${order.orderId}">
                    <input type="hidden" name="returnTo" value="/admin/order/detail?id=${order.orderId}">
                    <label class="visually-hidden" for="order-status">Trạng thái đơn #<c:out value="${order.orderId}"/></label>
                    <select id="order-status" name="status"><c:forEach items="${orderStatuses}" var="status"><c:if test="${status == order.status or (order.status.allows(status) and status.name() ne 'CANCELLED')}">
                        <option value="${status}"<c:if test="${status == order.status}"> selected</c:if>><c:out value="${status.label}"/></option>
                    </c:if></c:forEach></select>
                    <button class="button secondary button-small" type="submit" data-save>Cập nhật<span class="visually-hidden"> đơn #<c:out value="${order.orderId}"/></span></button>
                </form>
            </c:if>
            <c:if test="${canCancel}">
                <form class="admin-inline" method="post" action="${ctx}/admin/order/status">
                    <input type="hidden" name="orderId" value="${order.orderId}">
                    <input type="hidden" name="status" value="CANCELLED">
                    <input type="hidden" name="returnTo" value="/admin/order/detail?id=${order.orderId}">
                    <details class="confirm-inline">
                        <summary class="button link-button danger">Hủy đơn…<span class="visually-hidden"> #<c:out value="${order.orderId}"/></span></summary>
                        <span class="field-hint" id="cancel-hint">Hàng được hoàn vào kho.</span>
                        <button class="button danger button-small" type="submit" aria-describedby="cancel-hint">Hủy đơn #<c:out value="${order.orderId}"/></button>
                    </details>
                </form>
            </c:if>
            <c:if test="${not canMove and not canCancel}"><span class="field-hint">Không thể đổi trạng thái</span></c:if>
        </div>
    </div>
    <%@ include file="/WEB-INF/views/fragments/admin-nav.jspf" %>

    <a class="back-link" href="${ctx}/admin/orders">← Quay lại danh sách đơn hàng</a>

    <div class="admin-two-col">
        <section class="admin-card" aria-labelledby="giao-hang-title">
            <h2 id="giao-hang-title">Khách hàng và giao hàng</h2>
            <dl class="admin-dl">
                <dt>Khách hàng</dt><dd class="wrap-anywhere"><c:choose>
                    <c:when test="${not empty customerName}"><c:out value="${customerName}"/></c:when>
                    <c:otherwise>Mã khách <c:out value="${order.customerId}"/></c:otherwise>
                </c:choose></dd>
                <c:if test="${not empty customerEmail}"><dt>Email</dt><dd class="wrap-anywhere"><a href="mailto:<c:out value='${customerEmail}'/>"><c:out value="${customerEmail}"/></a></dd></c:if>
                <dt>Người nhận</dt><dd class="wrap-anywhere"><c:out value="${order.shippingAddress.recipientName}"/></dd>
                <dt>Điện thoại</dt><dd class="wrap-anywhere"><c:out value="${order.shippingAddress.phone}"/></dd>
                <dt>Địa chỉ</dt><dd class="wrap-anywhere"><c:out value="${order.shippingAddress.address}"/></dd>
            </dl>
        </section>
        <section class="admin-card" aria-labelledby="thanh-toan-title">
            <h2 id="thanh-toan-title">Thanh toán</h2>
            <dl class="admin-dl">
                <dt>Phương thức</dt><dd>Thanh toán khi nhận hàng</dd>
                <dt>Tổng cộng</dt><dd class="admin-stock"><app:money amount="${order.total}"/></dd>
            </dl>
        </section>
    </div>

    <section class="admin-section" aria-labelledby="san-pham-title">
        <div class="admin-section-head"><h2 id="san-pham-title">Sản phẩm trong đơn</h2></div>
        <div class="admin-table-wrap">
            <table class="admin-table">
                <caption class="visually-hidden">Sản phẩm trong đơn</caption>
                <thead><tr><th scope="col">Sản phẩm</th><th scope="col">Phân loại</th><th scope="col" class="admin-numeric">Số lượng</th><th scope="col" class="admin-numeric">Đơn giá</th><th scope="col" class="admin-numeric">Thành tiền</th></tr></thead>
                <tbody>
                <c:forEach items="${order.items}" var="item">
                    <tr class="data-row">
                        <td class="admin-cell-product" data-label="Sản phẩm"><c:out value="${item.productName}"/></td>
                        <td class="wrap-anywhere" data-label="Phân loại"><c:out value="${item.size}"/> · <c:out value="${item.color}"/></td>
                        <td class="admin-numeric" data-label="Số lượng"><c:out value="${item.quantity}"/></td>
                        <td class="admin-numeric" data-label="Đơn giá"><app:money amount="${item.unitPrice}"/></td>
                        <td class="admin-numeric" data-label="Thành tiền"><app:money amount="${item.lineTotal}"/></td>
                    </tr>
                </c:forEach>
                </tbody>
                <tfoot>
                    <c:if test="${order.discount.signum() gt 0}"><tr><th colspan="4" scope="row" class="admin-numeric">Giảm giá thành viên</th><td class="admin-numeric">- <app:money amount="${order.discount}"/></td></tr></c:if>
                    <tr><th colspan="4" scope="row" class="admin-numeric">Tổng cộng</th><th class="admin-numeric"><app:money amount="${order.total}"/></th></tr>
                </tfoot>
            </table>
        </div>
    </section>
</main>
<%@ include file="/WEB-INF/views/fragments/footer.jspf" %>
</body>
</html>
