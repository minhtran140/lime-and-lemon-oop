<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="app" tagdir="/WEB-INF/tags" %>
<c:set var="productImageUrl" value="${details.product.imageUrl}"/>
<c:if test="${not empty details.product.imageUrl and fn:startsWith(details.product.imageUrl, '/assets/')}">
    <c:url var="productImageUrl" value="${details.product.imageUrl}"/>
</c:if>
<%-- Chọn size: lấy size từ ?size= nếu còn hàng; không có ?size= thì lấy size còn hàng đầu tiên.
     Size trong URL không hợp lệ hoặc hết hàng thì để không chọn size nào, không tự thay bằng size khác. --%>
<c:forEach items="${details.sizes}" var="s">
    <c:if test="${empty firstInStock and s.stock > 0}"><c:set var="firstInStock" value="${s}"/></c:if>
    <c:if test="${s.size eq param.size and s.stock > 0}"><c:set var="pick" value="${s}"/></c:if>
</c:forEach>
<c:if test="${empty param.size}"><c:set var="pick" value="${firstInStock}"/></c:if>
<c:set var="pickInCart" value="0"/>
<c:forEach items="${sessionScope.shoppingCart.items}" var="item"><c:if test="${not empty pick and item.sizeId eq pick.sizeId}"><c:set var="pickInCart" value="${item.quantity}"/></c:if></c:forEach>
<c:set var="pickRemaining" value="${empty pick ? 0 : pick.stock - pickInCart}"/>
<%-- Tài khoản admin xem cửa hàng nhưng không đặt hàng: size chỉ để tham khảo, có ghi chú thay cho form mua. --%>
<c:set var="isAdmin" value="${sessionScope.user.role == 'ADMIN'}"/>
<c:set var="canBuy" value="${not empty firstInStock and not isAdmin}"/>
<!doctype html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><c:out value="${details.product.name}"/> · Lime &amp; Lemon</title>
    <meta name="description" content="<c:out value='${fn:length(details.product.description) gt 155 ? fn:substring(details.product.description, 0, 155) : details.product.description}'/>">
    <%@ include file="/WEB-INF/views/fragments/font-preload.jspf" %>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/store.css?v=${assetVersion}">
</head>
<body class="storefront">
<%@ include file="/WEB-INF/views/fragments/header.jspf" %>
<main id="main-content" tabindex="-1" class="container product-page">
    <%@ include file="/WEB-INF/views/fragments/flash.jspf" %>
    <c:url var="categoryUrl" value="/search"><c:param name="categoryId" value="${details.product.category.categoryId}"/></c:url>
    <nav class="breadcrumb" aria-label="Đường dẫn">
        <ol>
            <li><a href="${pageContext.request.contextPath}/">Trang chủ</a></li>
            <li><a href="<c:out value='${categoryUrl}'/>"><c:out value="${details.product.category.name}"/></a></li>
            <li aria-current="page"><c:out value="${details.product.name}"/></li>
        </ol>
    </nav>
    <div class="detail-grid<c:if test='${empty details.product.imageUrl}'> detail-grid--no-image</c:if>">
        <div class="product-art product-art--detail<c:if test='${empty details.product.imageUrl}'> product-art--empty</c:if>">
            <c:choose>
                <c:when test="${not empty details.product.imageUrl}"><img src="<c:out value='${productImageUrl}'/>" alt="<c:out value='${details.product.name}'/>" width="800" height="1000" decoding="async" onerror="this.onerror=null;this.hidden=true;this.nextElementSibling.hidden=false"><span class="product-art-note image-fallback" role="status" aria-live="polite" hidden>Không thể tải ảnh</span></c:when>
                <c:otherwise><span class="product-art-note">Chưa có ảnh</span></c:otherwise>
            </c:choose>
        </div>
        <section class="product-info">
            <h1><c:out value="${details.product.name}"/></h1>
            <p class="price price--large"><app:money amount="${details.product.price}"/></p>
            <p class="product-description"><c:out value="${details.product.description}"/></p>
            <c:if test="${not empty details.sizes}">
                <div class="pdp-select" data-pdp>
                    <p class="pdp-option pdp-option-single"><span class="pdp-option-name">Màu sắc:</span> <span class="pdp-option-value"><c:out value="${details.product.color}"/></span></p>

                    <%-- Form chỉ tồn tại khi còn sản phẩm có thể mua. --%>
                    <c:if test="${canBuy}"><form class="pdp-form" method="post" action="${pageContext.request.contextPath}/cart/add" data-pdp-form></c:if>
                    <c:if test="${not canBuy}"><div class="pdp-form"></c:if>
                        <c:if test="${canBuy}">
                            <input type="hidden" name="productId" value="<c:out value='${details.product.productId}'/>">
                        </c:if>
                        <fieldset class="pdp-option pdp-sizes" aria-describedby="pdp-size-error">
                            <legend><span class="pdp-option-name">Kích cỡ:</span> <span class="pdp-option-value" data-size-label><c:out value="${pick.size}"/></span></legend>
                            <%-- Mỗi size một nút. Giá trị của radio được chọn là sizeId được POST tới /cart/add. --%>
                            <div class="pdp-size-group is-active">
                                <div class="pdp-size-list">
                                    <c:forEach items="${details.sizes}" var="s">
                                        <c:set var="inCart" value="0"/>
                                        <c:forEach items="${sessionScope.shoppingCart.items}" var="item"><c:if test="${item.sizeId eq s.sizeId}"><c:set var="inCart" value="${item.quantity}"/></c:if></c:forEach>
                                        <label class="pdp-size">
                                            <c:choose>
                                                <c:when test="${s.stock > 0}"><input type="radio" name="sizeId" value="${s.sizeId}" data-size="${s.size}" data-stock="${s.stock}" data-in-cart="${inCart}" <c:if test="${s.sizeId eq pick.sizeId}">checked</c:if>></c:when>
                                                <c:otherwise><input type="radio" disabled data-size="${s.size}"></c:otherwise>
                                            </c:choose>
                                            <span class="pdp-size-face<c:if test='${s.stock le 0}'> is-sold-out</c:if>"<c:if test="${s.stock le 0}"> title="Hết hàng"</c:if>><c:out value="${s.size}"/><c:if test="${s.stock le 0}"><span class="visually-hidden"> (hết hàng)</span></c:if></span>
                                        </label>
                                    </c:forEach>
                                </div>
                            </div>
                        </fieldset>
                        <p class="field-error pdp-size-error" id="pdp-size-error" hidden>Vui lòng chọn kích cỡ.</p>

                        <c:choose>
                            <c:when test="${empty firstInStock}"><p class="pdp-soldout"><span class="stock-badge stock-badge--empty">Hết hàng</span> Sản phẩm tạm hết ở mọi kích cỡ.</p></c:when>
                            <c:when test="${isAdmin}"><p class="admin-order-note">Tài khoản quản trị không đặt hàng. Hãy dùng tài khoản khách để mua hàng.</p></c:when>
                            <c:otherwise>
                                <p class="pdp-stock" role="status" data-stock-status><c:choose>
                                    <c:when test="${empty pick}">Vui lòng chọn kích cỡ.</c:when>
                                    <c:when test="${pickRemaining le 0}">Bạn đã có <c:out value="${pickInCart}"/> sản phẩm này trong giỏ, bằng số lượng còn lại.</c:when>
                                    <c:otherwise>Còn <c:out value="${pick.stock}"/> sản phẩm<c:if test="${pickInCart gt 0}"> · Đã có <c:out value="${pickInCart}"/> trong giỏ</c:if></c:otherwise>
                                </c:choose></p>
                                <div class="pdp-quantity">
                                    <label class="pdp-option-name" for="pdp-quantity">Số lượng</label>
                                    <div class="pdp-qty">
                                        <button class="pdp-qty-step" type="button" data-step="-1" aria-controls="pdp-quantity" aria-label="Giảm số lượng">−</button>
                                        <input id="pdp-quantity" type="number" name="quantity" min="1" max="${pickRemaining gt 0 ? pickRemaining : 1}" value="1" step="1" inputmode="numeric" required>
                                        <button class="pdp-qty-step" type="button" data-step="1" aria-controls="pdp-quantity" aria-label="Tăng số lượng">+</button>
                                    </div>
                                </div>
                                <button class="button button-primary pdp-add" type="submit" data-add data-busy-label="Đang thêm…">Thêm vào giỏ hàng</button>
                            </c:otherwise>
                        </c:choose>
                    <c:if test="${canBuy}"></form></c:if>
                    <c:if test="${not canBuy}"></div></c:if>
                </div>
                <c:if test="${not isAdmin}"><p class="pdp-note">Thanh toán khi nhận hàng (COD).</p></c:if>
                <details class="pdp-details">
                    <summary>Hướng dẫn chọn size</summary>
                    <table class="pdp-size-table">
                        <thead><tr><th scope="col">Size</th><th scope="col">Chiều cao (cm)</th><th scope="col">Cân nặng (kg)</th></tr></thead>
                        <tbody>
                            <tr><th scope="row">S</th><td>150–160</td><td>45–52</td></tr>
                            <tr><th scope="row">M</th><td>160–168</td><td>52–60</td></tr>
                            <tr><th scope="row">L</th><td>168–175</td><td>60–70</td></tr>
                        </tbody>
                    </table>
                    <p class="pdp-note">Số đo tham khảo, có thể chênh lệch theo kiểu dáng.</p>
                </details>
            </c:if>
            <c:if test="${empty details.sizes}"><div class="empty-state"><h2>Chưa có lựa chọn kích cỡ</h2><p>Sản phẩm hiện chưa có lựa chọn còn hoạt động.</p><a class="button" href="${pageContext.request.contextPath}/products">Xem sản phẩm khác</a></div></c:if>
        </section>
    </div>
</main>
<script src="${pageContext.request.contextPath}/assets/product-detail.js?v=${assetVersion}" defer></script>
<%@ include file="/WEB-INF/views/fragments/footer.jspf" %>
</body>
</html>
