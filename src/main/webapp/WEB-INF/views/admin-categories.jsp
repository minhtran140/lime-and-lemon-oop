<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="adminSection" value="categories"/>
<c:set var="cc" value="${submittedKind eq 'create-category'}"/>
<!doctype html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Danh mục · Quản trị · Lime &amp; Lemon</title>
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
            <h1>Quản trị</h1>
            <p>Danh mục sản phẩm của cửa hàng.</p>
        </div>
    </div>
    <%@ include file="/WEB-INF/views/fragments/admin-nav.jspf" %>

    <section class="admin-section" id="danh-muc" aria-labelledby="danh-muc-title">
        <div class="admin-section-head">
            <h2 id="danh-muc-title">Danh mục</h2>
            <p>Đường dẫn được tạo tự động từ tên. Danh mục còn sản phẩm không xóa được.</p>
        </div>
        <form class="admin-toolbar" method="post" action="${ctx}/admin/category/create">
            <input type="hidden" name="returnTo" value="/admin/categories">
            <div class="field">
                <label for="category-name">Tên danh mục</label>
                <input id="category-name" name="name" required maxlength="100" placeholder="Phụ kiện" value="<c:out value='${cc ? submitted.name : ""}'/>"<c:if test="${cc and not empty formError}"> aria-invalid="true" aria-describedby="category-name-error"</c:if>>
                <c:if test="${cc and not empty formError}"><span class="field-error" id="category-name-error" tabindex="-1" data-focus-error-summary><c:out value="${formError}"/></span></c:if>
            </div>
            <button class="button" type="submit">Thêm danh mục</button>
        </form>
        <div class="admin-table-wrap">
            <table class="admin-table">
                <caption class="visually-hidden">Danh mục</caption>
                <thead><tr><th scope="col">Tên</th><th scope="col">Đường dẫn</th><th scope="col" class="admin-numeric">Sản phẩm</th><th scope="col" class="admin-col-actions">Thao tác</th></tr></thead>
                <tbody>
                <c:forEach items="${categoryRows}" var="row">
                    <tr class="data-row" id="category-row-${row.category.categoryId}">
                        <td data-label="Tên"><c:out value="${row.category.name}"/></td>
                        <td data-label="Đường dẫn"><span class="admin-mono">/<c:out value="${row.category.slug}"/></span></td>
                        <td class="admin-numeric" data-label="Sản phẩm"><c:out value="${row.productCount}"/></td>
                        <td class="admin-col-actions" data-label="Thao tác">
                            <c:choose>
                                <c:when test="${row.productCount gt 0}"><span class="field-hint">Còn sản phẩm, chưa xóa được</span></c:when>
                                <c:otherwise>
                                    <form class="admin-actions" method="post" action="${ctx}/admin/category/delete">
                                        <input type="hidden" name="categoryId" value="${row.category.categoryId}">
                                        <input type="hidden" name="returnTo" value="/admin/categories">
                                        <details class="confirm-inline">
                                            <summary class="button link-button danger">Xóa…<span class="visually-hidden"> danh mục <c:out value="${row.category.name}"/></span></summary>
                                            <button class="button danger" type="submit">Xác nhận xóa<span class="visually-hidden"> danh mục <c:out value="${row.category.name}"/></span></button>
                                        </details>
                                    </form>
                                </c:otherwise>
                            </c:choose>
                        </td>
                    </tr>
                </c:forEach>
                <c:if test="${empty categoryRows}"><tr><td colspan="4" class="admin-empty">Chưa có danh mục.</td></tr></c:if>
                </tbody>
            </table>
        </div>
    </section>
</main>
<%@ include file="/WEB-INF/views/fragments/footer.jspf" %>
</body>
</html>
