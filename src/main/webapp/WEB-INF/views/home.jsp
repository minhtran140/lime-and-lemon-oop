<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="app" tagdir="/WEB-INF/tags" %>
<!doctype html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="description" content="Khám phá các bộ sưu tập Lime &amp; Lemon, sản phẩm nổi bật và gợi ý phối đồ hằng ngày.">
    <title>Lime &amp; Lemon · Một chút xanh cho ngày thường</title>
    <%@ include file="/WEB-INF/views/fragments/font-preload.jspf" %>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/store.css?v=${assetVersion}">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/home.css?v=${assetVersion}">
</head>
<body class="storefront home-page">
<%@ include file="/WEB-INF/views/fragments/header.jspf" %>
<main id="main-content" tabindex="-1">
    <%@ include file="/WEB-INF/views/fragments/flash.jspf" %>
    <section class="home-hero" aria-labelledby="home-title">
        <div class="home-hero-inner">
            <div class="home-hero-copy">
                <h1 id="home-title">Một chút xanh<br>cho ngày thường.</h1>
                <p>Những kiểu dáng quen thuộc, những cách phối mới. Chọn trang phục cho nhịp sống của bạn.</p>
                <div class="home-actions"><a class="button" href="${pageContext.request.contextPath}/products">Khám phá sản phẩm <span aria-hidden="true">↗</span></a></div>
            </div>
            <img class="home-hero-image" src="${pageContext.request.contextPath}/assets/home/hero-fashion-editorial-20261007.webp" srcset="${pageContext.request.contextPath}/assets/home/hero-fashion-editorial-20261007-small.webp 1000w, ${pageContext.request.contextPath}/assets/home/hero-fashion-editorial-20261007.webp 2000w" sizes="(max-width: 1024px) 100vw, min(54vw, 700px)" width="2000" height="1250" alt="Người mẫu mặc sơ mi linen màu kem và quần sage trong sân vườn ngập nắng" loading="eager" fetchpriority="high" decoding="async">
        </div>
    </section>

    <section id="collections" class="home-section home-collections" aria-labelledby="collections-title">
        <div class="home-section-heading"><div><h2 id="collections-title">Chọn một góc của bạn</h2><p>Từ chiếc áo quen đến một kiểu dáng khác.</p></div><a class="home-text-link" href="${pageContext.request.contextPath}/products">Tất cả sản phẩm <span aria-hidden="true">↗</span></a></div>
        <div class="home-collection-grid">
            <c:forEach items="${categories}" var="category">
                <c:url var="collectionUrl" value="/search"><c:param name="categoryId" value="${category.categoryId}"/></c:url>
                <%-- Ảnh bộ sưu tập là file đặt tên theo slug của danh mục. Chỉ năm slug này có ảnh; danh mục khác
                     (ví dụ danh mục admin vừa tạo) chỉ hiện nhãn thường, không yêu cầu một file không tồn tại. --%>
                <c:set var="slugKey" value="|${category.slug}|"/>
                <%-- Các slug được bọc trong dấu | để chỉ khớp nguyên slug, không khớp một phần của slug khác (nếu không bọc, một slug ngắn như "ao" sẽ khớp nhầm vì nó nằm trong "ao-khoac" và "ao-thun" của danh sách). --%>
                <c:set var="hasArt" value="${fn:contains('|ao-khoac|ao-so-mi|ao-thun|quan|vay-chan-vay|', slugKey)}"/>
                <c:set var="collectionBase" value="${pageContext.request.contextPath}/assets/collections/${category.slug}-20261007"/>
                <a class="home-collection<c:if test='${category.slug eq "ao-khoac"}'> home-collection--large</c:if><c:if test='${not hasArt}'> home-collection--fallback</c:if>" href="<c:out value='${collectionUrl}'/>">
                    <c:if test="${hasArt}"><img src="<c:out value='${collectionBase}'/>.webp" srcset="<c:out value='${collectionBase}'/>-small.webp 600w, <c:out value='${collectionBase}'/>.webp 1200w" sizes="(max-width: 599px) 46vw, (max-width: 1023px) 45vw, 33vw" alt="" width="600" height="750" loading="lazy" decoding="async" onerror="this.onerror=null;this.hidden=true;this.parentNode.classList.add('home-collection--fallback')"></c:if>
                    <span class="home-collection-label"><span><c:out value="${category.name}"/></span><span aria-hidden="true">↗</span></span>
                </a>
            </c:forEach>
        </div>
    </section>

    <c:if test="${not empty featuredProducts}">
    <section class="home-section home-featured" aria-labelledby="featured-title">
        <div class="home-section-heading"><div><h2 id="featured-title">Nổi bật hôm nay</h2><p>Những lựa chọn từ các bộ sưu tập Lime &amp; Lemon.</p></div><a class="home-text-link" href="${pageContext.request.contextPath}/products">Khám phá thêm <span aria-hidden="true">↗</span></a></div>
        <div class="product-grid"><c:forEach items="${featuredProducts}" var="product"><%@ include file="/WEB-INF/views/fragments/product-card.jspf" %></c:forEach></div>
    </section>
    </c:if>

    <section class="home-band home-newest" aria-labelledby="newest-title">
        <img class="home-band-texture" src="${pageContext.request.contextPath}/assets/home/band-leaves-20261007-small.webp" srcset="${pageContext.request.contextPath}/assets/home/band-leaves-20261007-small.webp 1000w, ${pageContext.request.contextPath}/assets/home/band-leaves-20261007.webp 2000w" sizes="100vw" alt="" width="2000" height="1100" loading="lazy" decoding="async">
        <div class="home-section"><div class="home-section-heading"><div><h2 id="newest-title">Mới về</h2><p>Những sản phẩm vừa có mặt trong cửa hàng.</p></div><a class="home-text-link" href="${pageContext.request.contextPath}/products">Xem toàn bộ <span aria-hidden="true">↗</span></a></div><div class="product-grid"><c:forEach items="${newestProducts}" var="product"><%@ include file="/WEB-INF/views/fragments/product-card.jspf" %></c:forEach></div><c:if test="${empty newestProducts}"><div class="empty-state"><h3>Chưa có sản phẩm mới</h3><p>Sản phẩm vừa về sẽ xuất hiện ở đây.</p><a class="button" href="${pageContext.request.contextPath}/products">Xem sản phẩm</a></div></c:if></div>
    </section>
    <section class="home-band home-outfits" aria-labelledby="outfits-title">
        <img class="home-band-texture" src="${pageContext.request.contextPath}/assets/home/band-grass-20261007-small.webp" srcset="${pageContext.request.contextPath}/assets/home/band-grass-20261007-small.webp 1000w, ${pageContext.request.contextPath}/assets/home/band-grass-20261007.webp 2000w" sizes="100vw" alt="" width="2000" height="1100" loading="lazy" decoding="async">
        <div class="home-section">
            <div class="home-section-heading"><div><h2 id="outfits-title">Phối đồ theo nhịp của bạn</h2><p>Đổi một cách kết hợp, thêm một cảm hứng cho ngày mới.</p></div></div>
            <div class="home-outfit-grid">
                <article class="home-outfit"><img src="${pageContext.request.contextPath}/assets/home/outfit-1-20261007.webp" srcset="${pageContext.request.contextPath}/assets/home/outfit-1-20261007-small.webp 700w, ${pageContext.request.contextPath}/assets/home/outfit-1-20261007.webp 1400w" sizes="(max-width: 767px) 90vw, 46vw" width="1400" height="1000" alt="Áo sơ mi xanh nhạt và quần chinos be treo cạnh nhau trong vườn" loading="lazy" decoding="async"><div class="home-outfit-copy"><h3>Một ngày nhẹ nhịp</h3><p>Áo sơ mi xanh nhạt đi cùng quần be. Xắn tay áo, giữ phom quần gọn để thay đổi nét quen thuộc.</p><c:forEach items="${categories}" var="cat"><c:if test="${cat.slug eq 'ao-so-mi'}"><c:set var="shirtCategoryId" value="${cat.categoryId}"/></c:if></c:forEach><a class="home-text-link" href="${pageContext.request.contextPath}/<c:choose><c:when test="${empty shirtCategoryId}">products</c:when><c:otherwise>search?categoryId=${shirtCategoryId}</c:otherwise></c:choose>">Chọn áo sơ mi <span aria-hidden="true">↗</span></a></div></article>
                <article class="home-outfit"><img src="${pageContext.request.contextPath}/assets/home/outfit-2-20261007.webp" srcset="${pageContext.request.contextPath}/assets/home/outfit-2-20261007-small.webp 700w, ${pageContext.request.contextPath}/assets/home/outfit-2-20261007.webp 1400w" sizes="(max-width: 767px) 90vw, 46vw" width="1400" height="1000" alt="Áo thun kem phối cùng chân váy xếp ly xanh navy trên nền vải sáng" loading="lazy" decoding="async"><div class="home-outfit-copy"><h3>Cuối tuần, theo ý bạn</h3><p>Áo thun dáng rộng và chân váy xếp ly: sơ vin nhẹ phía trước để tạo điểm nhấn cho cả bộ.</p><a class="home-text-link" href="${pageContext.request.contextPath}/products">Tìm cách phối của bạn <span aria-hidden="true">↗</span></a></div></article>
            </div>
        </div>
    </section>

</main>
<%@ include file="/WEB-INF/views/fragments/footer.jspf" %>
</body>
</html>
