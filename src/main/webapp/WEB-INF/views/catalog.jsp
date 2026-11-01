<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="app" tagdir="/WEB-INF/tags" %>
<!doctype html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><c:choose><c:when test="${not empty param.keyword}">Kết quả cho “<c:out value="${param.keyword}"/>” · Lime &amp; Lemon</c:when><c:otherwise>Lime &amp; Lemon · Quần áo mặc hằng ngày</c:otherwise></c:choose></title>
    <meta name="description" content="Áo khoác, áo sơ mi, áo thun, quần và váy dễ mặc hằng ngày tại Lime &amp; Lemon. Lọc theo danh mục và sắp xếp theo giá.">
    <%@ include file="/WEB-INF/views/fragments/font-preload.jspf" %>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/store.css?v=${assetVersion}">
</head>
<body class="storefront">
<%@ include file="/WEB-INF/views/fragments/header.jspf" %>
<main id="main-content" tabindex="-1" class="container catalog-page">
    <%@ include file="/WEB-INF/views/fragments/flash.jspf" %>
    <div class="catalog-head">
        <h1 id="catalog-title"><c:choose><c:when test="${not empty param.keyword}">Kết quả cho “<c:out value="${param.keyword}"/>”</c:when><c:otherwise>Sản phẩm</c:otherwise></c:choose></h1>
        <span class="catalog-result-count"><c:out value="${fn:length(products)}"/> sản phẩm</span>
    </div>

    <c:url var="allCategoriesUrl" value="/search">
        <c:if test="${not empty param.keyword}"><c:param name="keyword" value="${param.keyword}"/></c:if>
        <c:if test="${not empty param.sort}"><c:param name="sort" value="${param.sort}"/></c:if>
    </c:url>
    <nav class="category-pills" aria-label="Lọc theo danh mục">
        <c:choose>
            <c:when test="${empty selectedCategoryId}"><a class="category-pill is-active" href="<c:out value='${allCategoriesUrl}'/>" aria-current="page">Tất cả</a></c:when>
            <c:otherwise><a class="category-pill" href="<c:out value='${allCategoriesUrl}'/>">Tất cả</a></c:otherwise>
        </c:choose>
        <c:forEach items="${categories}" var="category">
            <c:url var="categoryUrl" value="/search">
                <c:param name="categoryId" value="${category.categoryId}"/>
                <c:if test="${not empty param.keyword}"><c:param name="keyword" value="${param.keyword}"/></c:if>
                <c:if test="${not empty param.sort}"><c:param name="sort" value="${param.sort}"/></c:if>
            </c:url>
            <c:choose>
                <c:when test="${selectedCategoryId eq category.categoryId}"><a class="category-pill is-active" href="<c:out value='${categoryUrl}'/>" aria-current="page"><c:out value="${category.name}"/></a></c:when>
                <c:otherwise><a class="category-pill" href="<c:out value='${categoryUrl}'/>"><c:out value="${category.name}"/></a></c:otherwise>
            </c:choose>
        </c:forEach>
    </nav>

    <form class="catalog-filter" method="get" action="${pageContext.request.contextPath}/search" aria-label="Bộ lọc sản phẩm">
        <label class="catalog-filter-keyword" for="keyword">Từ khóa<input id="keyword" name="keyword" maxlength="100" value="<c:out value='${param.keyword}'/>" placeholder="Áo thun, quần…"></label>
        <label class="catalog-filter-select" for="categoryId">Danh mục<select id="categoryId" name="categoryId">
            <option value="" <c:if test="${empty selectedCategoryId}">selected</c:if>>Tất cả</option>
            <c:forEach items="${categories}" var="category"><option value="${category.categoryId}" <c:if test="${selectedCategoryId eq category.categoryId}">selected</c:if>><c:out value="${category.name}"/></option></c:forEach>
        </select></label>
        <c:set var="sortValue" value="${fn:toUpperCase(selectedSort)}"/>
        <label class="catalog-filter-select" for="sort">Sắp xếp<select id="sort" name="sort">
            <option value="NEWEST" <c:if test="${empty sortValue or sortValue eq 'NEWEST'}">selected</c:if>>Mới nhất</option>
            <option value="PRICE_ASC" <c:if test="${sortValue eq 'PRICE_ASC'}">selected</c:if>>Giá tăng dần</option>
            <option value="PRICE_DESC" <c:if test="${sortValue eq 'PRICE_DESC'}">selected</c:if>>Giá giảm dần</option>
        </select></label>
        <button type="submit">Tìm sản phẩm</button>
    </form>

    <c:if test="${not empty param.keyword or not empty param.categoryId or not empty param.sort}">
        <p class="clear-filters"><a href="${pageContext.request.contextPath}/products">Xóa bộ lọc</a></p>
    </c:if>

    <section id="products" class="product-section" aria-labelledby="catalog-title">
        <div class="product-grid">
            <c:set var="cardHeading" value="h2"/>
            <c:forEach items="${products}" var="product" varStatus="card">
                <c:set var="cardEager" value="${card.index lt 4}"/>
                <c:set var="cardFirst" value="${card.index eq 0}"/>
                <%@ include file="/WEB-INF/views/fragments/product-card.jspf" %>
            </c:forEach>
        </div>

        <c:if test="${empty products}"><div class="empty-state"><h2>Chưa tìm thấy sản phẩm phù hợp</h2><p>Thử thay đổi bộ lọc để xem thêm lựa chọn.</p><a class="button" href="${pageContext.request.contextPath}/products">Xem tất cả sản phẩm</a></div></c:if>
    </section>
</main>
<%@ include file="/WEB-INF/views/fragments/footer.jspf" %>
</body>
</html>
