<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="adminSection" value="products"/>
<%-- Nơi admin quay lại sau khi lưu: danh sách này với bộ lọc hiện tại. --%>
<c:url var="listUrlFull" value="/admin">
    <c:if test="${not empty filterQ}"><c:param name="q" value="${filterQ}"/></c:if>
    <c:if test="${not empty filterCategoryId}"><c:param name="category" value="${filterCategoryId}"/></c:if>
</c:url>
<%-- returnTo là đường dẫn bên trong ứng dụng: phía back tự thêm context path. --%>
<c:set var="listUrl" value="${fn:substringAfter(listUrlFull, ctx)}"/>
<c:set var="filtered" value="${not empty filterQ or not empty filterCategoryId}"/>
<%-- Tạo mới thất bại thì giữ nội dung đã nhập; lưu một dòng thất bại thì giữ các chỉnh sửa của dòng đó.
     cp = form tạo sản phẩm vừa bị từ chối; cpField = ô có lỗi trong form đó. --%>
<c:set var="cp" value="${submittedKind eq 'create-product'}"/>
<c:set var="cpField" value="${cp ? errorField : ''}"/>
<%-- inactiveIds gom id các sản phẩm đã tắt bán; gán vào biến ignored chỉ để gọi được add() bên trong EL (kết quả không dùng). --%>
<jsp:useBean id="inactiveIds" class="java.util.HashSet" scope="page"/>
<c:forEach items="${products}" var="product"><c:if test="${not product.active}"><c:set var="ignored" value="${inactiveIds.add(product.productId)}"/></c:if></c:forEach>
<!doctype html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Sản phẩm và tồn kho · Quản trị · Lime &amp; Lemon</title>
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
            <p>Sản phẩm và tồn kho của cửa hàng.</p>
        </div>
    </div>
    <%@ include file="/WEB-INF/views/fragments/admin-nav.jspf" %>

    <section class="admin-section" id="san-pham" aria-labelledby="san-pham-title">
        <div class="admin-section-head">
            <h2 id="san-pham-title">Sản phẩm</h2>
            <p>Mỗi sản phẩm có một màu và ba cỡ S, M, L. Sản phẩm đã có trong đơn hàng chỉ có thể ngừng bán, không xóa được.</p>
        </div>

        <details class="admin-disclosure" id="tao-san-pham"<c:if test="${cp}"> open</c:if>>
            <summary class="button">Tạo sản phẩm</summary>
            <form class="admin-form-grid" method="post" action="${ctx}/admin/product/create">
                <input type="hidden" name="returnTo" value="<c:out value='${listUrl}'/>">
                <c:if test="${cp and not empty formError}">
                    <div class="form-error-summary span-2" id="cp-error" tabindex="-1" data-focus-error-summary role="alert">
                        <h3>Chưa tạo được sản phẩm</h3>
                        <p><c:out value="${formError}"/></p>
                    </div>
                </c:if>
                <div class="field">
                    <label for="cp-category">Danh mục</label>
                    <c:set var="cpCat" value="${cp ? submitted.categoryId : ''}"/>
                    <select id="cp-category" name="categoryId" required<c:if test="${cpField eq 'categoryId'}"> aria-invalid="true" aria-describedby="cp-categoryId-error"</c:if>>
                        <option value="" disabled<c:if test="${empty cpCat}"> selected</c:if>>Chọn danh mục</option>
                        <c:forEach items="${categories}" var="category">
                            <option value="${category.categoryId}"<c:if test="${not empty cpCat and cpCat eq category.categoryId.toString()}"> selected</c:if>><c:out value="${category.name}"/></option>
                        </c:forEach>
                    </select>
                    <c:if test="${cpField eq 'categoryId'}"><span class="field-error" id="cp-categoryId-error"><c:out value="${formError}"/></span></c:if>
                </div>
                <div class="field">
                    <label for="cp-name">Tên</label>
                    <input id="cp-name" name="name" required maxlength="160" value="<c:out value='${cp ? submitted.name : ""}'/>"<c:if test="${cpField eq 'name'}"> aria-invalid="true" aria-describedby="cp-name-error"</c:if>>
                    <c:if test="${cpField eq 'name'}"><span class="field-error" id="cp-name-error"><c:out value="${formError}"/></span></c:if>
                </div>
                <div class="field">
                    <label for="cp-price">Giá (đồng)</label>
                    <input id="cp-price" name="price" type="number" min="1" step="1" required inputmode="numeric" value="<c:out value='${cp ? submitted.price : ""}'/>"<c:if test="${cpField eq 'price'}"> aria-invalid="true" aria-describedby="cp-price-error"</c:if>>
                    <c:if test="${cpField eq 'price'}"><span class="field-error" id="cp-price-error"><c:out value="${formError}"/></span></c:if>
                </div>
                <div class="field">
                    <label for="cp-color">Màu</label>
                    <input id="cp-color" name="color" required maxlength="40" value="<c:out value='${cp ? submitted.color : ""}'/>"<c:if test="${cpField eq 'color'}"> aria-invalid="true" aria-describedby="cp-color-error"</c:if>>
                    <c:if test="${cpField eq 'color'}"><span class="field-error" id="cp-color-error"><c:out value="${formError}"/></span></c:if>
                </div>
                <div class="field span-2">
                    <label for="cp-image">Ảnh (tùy chọn)</label>
                    <input id="cp-image" name="imageUrl" type="text" inputmode="url" maxlength="1000" placeholder="https://… hoặc /assets/products/…" pattern="(https://|/assets/).+" title="Nhập địa chỉ bắt đầu bằng https:// hoặc đường dẫn /assets/…" value="<c:out value='${cp ? submitted.imageUrl : ""}'/>" aria-describedby="cp-image-hint<c:if test='${cpField eq "imageUrl"}'> cp-imageUrl-error</c:if>"<c:if test="${cpField eq 'imageUrl'}"> aria-invalid="true"</c:if>>
                    <span class="field-hint" id="cp-image-hint">Địa chỉ bắt đầu bằng https:// hoặc đường dẫn nội bộ /assets/…</span>
                    <c:if test="${cpField eq 'imageUrl'}"><span class="field-error" id="cp-imageUrl-error"><c:out value="${formError}"/></span></c:if>
                </div>
                <fieldset class="admin-stock-fields span-2">
                    <legend>Tồn kho ban đầu</legend>
                    <c:forEach items="${['S', 'M', 'L']}" var="size">
                        <div class="field">
                            <label for="cp-stock${size}">Cỡ ${size}</label>
                            <c:set var="stockKey" value="stock${size}"/>
                            <input id="cp-stock${size}" name="stock${size}" type="number" min="0" step="1" inputmode="numeric" value="<c:out value='${cp ? submitted[stockKey] : "0"}'/>"<c:if test="${cpField eq stockKey}"> aria-invalid="true" aria-describedby="cp-${stockKey}-error"</c:if>>
                            <c:if test="${cpField eq stockKey}"><span class="field-error" id="cp-${stockKey}-error"><c:out value="${formError}"/></span></c:if>
                        </div>
                    </c:forEach>
                </fieldset>
                <div class="field span-2">
                    <label for="cp-description">Mô tả</label>
                    <textarea id="cp-description" name="description" maxlength="4000"<c:if test="${cpField eq 'description'}"> aria-invalid="true" aria-describedby="cp-description-error"</c:if>><c:out value="${cp ? submitted.description : ''}"/></textarea>
                    <c:if test="${cpField eq 'description'}"><span class="field-error" id="cp-description-error"><c:out value="${formError}"/></span></c:if>
                </div>
                <label class="check span-2"><input type="checkbox" name="featured"<c:if test="${cp and not empty submitted.featured}"> checked</c:if>> Hiển thị ở mục nổi bật trên trang chủ</label>
                <button class="button" type="submit">Tạo sản phẩm</button>
            </form>
        </details>

        <form class="admin-filter" method="get" action="${ctx}/admin" role="search" aria-label="Lọc sản phẩm">
            <div class="field">
                <label for="filter-q">Tìm theo tên</label>
                <input id="filter-q" type="search" name="q" maxlength="100" value="<c:out value='${filterQ}'/>" placeholder="Ví dụ: áo khoác">
            </div>
            <div class="field">
                <label for="filter-category">Danh mục</label>
                <select id="filter-category" name="category">
                    <option value="">Tất cả danh mục</option>
                    <c:forEach items="${categories}" var="category">
                        <option value="${category.categoryId}"<c:if test="${not empty filterCategoryId and filterCategoryId eq category.categoryId}"> selected</c:if>><c:out value="${category.name}"/></option>
                    </c:forEach>
                </select>
            </div>
            <div class="admin-filter-actions">
                <button class="button secondary" type="submit">Lọc</button>
                <c:if test="${filtered}"><a class="button link-button" href="${ctx}/admin#san-pham">Bỏ lọc</a></c:if>
                <a class="button link-button admin-jump" href="#ton-kho">Đến tồn kho<span aria-hidden="true"> ↓</span></a>
            </div>
        </form>
        <p class="admin-meta" aria-live="polite">
            <c:choose>
                <c:when test="${filtered}">Có <strong><c:out value="${products.size()}"/></strong> sản phẩm khớp bộ lọc.</c:when>
                <c:otherwise><strong><c:out value="${products.size()}"/></strong> sản phẩm.</c:otherwise>
            </c:choose>
            <c:if test="${not empty featuredLimit}">
                Trang chủ hiển thị tối đa <c:out value="${featuredLimit}"/> sản phẩm nổi bật đang bán, hiện có
                <span class="badge<c:if test='${featuredCount gt featuredLimit}'> badge-warn</c:if>">Nổi bật <c:out value="${featuredCount}"/>/<c:out value="${featuredLimit}"/></span>.
            </c:if>
        </p>

        <div class="admin-table-wrap">
            <table class="admin-table admin-table-products">
                <caption class="visually-hidden">Sản phẩm</caption>
                <thead><tr><th scope="col">Tên</th><th scope="col">Danh mục</th><th scope="col">Giá</th><th scope="col">Màu</th><th scope="col">Đang bán</th><th scope="col">Nổi bật</th><th scope="col" class="admin-col-actions">Thao tác</th></tr></thead>
                <tbody>
                <c:forEach items="${products}" var="product">
                    <c:set var="pid" value="${product.productId}"/>
                    <c:set var="formId" value="product-form-${pid}"/>
                    <c:set var="failed" value="${submittedKind eq 'update-product' and submittedId eq pid}"/>
                    <c:set var="rf" value="${failed ? errorField : ''}"/>
                    <c:set var="hasOrders" value="${not empty productIdsWithOrders and productIdsWithOrders.contains(pid)}"/>
                    <c:set var="thumb" value="${product.imageUrl}"/>
                    <c:if test="${not empty thumb and fn:startsWith(thumb, '/assets/')}"><c:url var="thumb" value="${product.imageUrl}"/></c:if>
                    <c:set var="stockTotal" value="${0}"/>
                    <c:set var="stockText" value=""/>
                    <c:forEach items="${stockRows}" var="sr"><c:if test="${sr.productId eq pid}"><c:set var="stockTotal" value="${sr.total}"/><c:set var="stockText" value="S ${sr.stockS} · M ${sr.stockM} · L ${sr.stockL}"/></c:if></c:forEach>
                    <tr class="data-row" id="product-row-${pid}"<c:if test="${not product.active}"> data-inactive</c:if><c:if test="${failed}"> data-unsaved</c:if>>
                        <td data-label="Tên" class="admin-cell-name">
                            <div class="admin-name-line">
                                <span class="admin-thumb" aria-hidden="true"><c:if test="${not empty thumb}"><img src="<c:out value='${thumb}'/>" alt="" loading="lazy" width="40" height="50" onerror="this.hidden=true"></c:if></span>
                                <label class="visually-hidden" for="product-name-${pid}">Tên sản phẩm <c:out value="${product.name}"/></label>
                                <input id="product-name-${pid}" class="admin-input-name" name="name" form="${formId}" value="<c:out value='${failed ? submitted.name : product.name}'/>" required maxlength="160"<c:if test="${rf eq 'name'}"> aria-invalid="true" aria-describedby="row-error-${pid}"</c:if>>
                            </div>
                            <c:if test="${failed and not empty formError}"><p class="field-error admin-row-error" id="row-error-${pid}" tabindex="-1" data-focus-error-summary><c:out value="${formError}"/></p></c:if>
                            <div class="admin-row-meta">
                                <c:if test="${not product.active}"><span class="badge badge-muted">Ngừng bán</span></c:if>
                                <a class="admin-stock-link" href="#stock-row-${pid}">Tồn <strong><c:out value="${stockTotal}"/></strong><span class="admin-stock-detail"> (<c:out value="${stockText}"/>)</span><span class="visually-hidden">: sửa tồn kho <c:out value="${product.name}"/></span></a>
                                <details class="row-editor"<c:if test="${rf eq 'description' or rf eq 'imageUrl'}"> open</c:if>>
                                    <summary>Mô tả và ảnh<span class="visually-hidden"> của <c:out value="${product.name}"/></span></summary>
                                    <div class="admin-form-grid">
                                        <div class="field span-2">
                                            <label for="product-description-${pid}">Mô tả</label>
                                            <textarea id="product-description-${pid}" name="description" form="${formId}" maxlength="4000"<c:if test="${rf eq 'description'}"> aria-invalid="true" aria-describedby="row-error-${pid}"</c:if>><c:out value="${failed ? submitted.description : product.description}"/></textarea>
                                        </div>
                                        <div class="field span-2">
                                            <label for="product-image-${pid}">Ảnh (tùy chọn)</label>
                                            <input id="product-image-${pid}" name="imageUrl" type="text" inputmode="url" maxlength="1000" form="${formId}" placeholder="https://… hoặc /assets/products/…" pattern="(https://|/assets/).+" title="Nhập địa chỉ bắt đầu bằng https:// hoặc đường dẫn /assets/…" value="<c:out value='${failed ? submitted.imageUrl : product.imageUrl}'/>"<c:if test="${rf eq 'imageUrl'}"> aria-invalid="true" aria-describedby="row-error-${pid}"</c:if>>
                                        </div>
                                    </div>
                                </details>
                            </div>
                        </td>
                        <td data-label="Danh mục">
                            <label class="visually-hidden" for="product-category-${pid}">Danh mục của <c:out value="${product.name}"/></label>
                            <c:set var="catVal" value="${failed ? submitted.categoryId : product.category.categoryId}"/>
                            <select id="product-category-${pid}" name="categoryId" form="${formId}"<c:if test="${rf eq 'categoryId'}"> aria-invalid="true" aria-describedby="row-error-${pid}"</c:if>>
                                <c:forEach items="${categories}" var="category">
                                    <option value="${category.categoryId}"<c:if test="${category.categoryId.toString() eq catVal.toString()}"> selected</c:if>><c:out value="${category.name}"/></option>
                                </c:forEach>
                            </select>
                        </td>
                        <td data-label="Giá" class="admin-half">
                            <label class="visually-hidden" for="product-price-${pid}">Giá của <c:out value="${product.name}"/></label>
                            <input id="product-price-${pid}" class="admin-input-price" name="price" type="number" min="1" step="1" inputmode="numeric" form="${formId}" value="<c:out value='${failed ? submitted.price : product.price.stripTrailingZeros().toPlainString()}'/>" required<c:if test="${rf eq 'price'}"> aria-invalid="true" aria-describedby="row-error-${pid}"</c:if>>
                        </td>
                        <td data-label="Màu" class="admin-half">
                            <label class="visually-hidden" for="product-color-${pid}">Màu của <c:out value="${product.name}"/></label>
                            <input id="product-color-${pid}" class="admin-input-short" name="color" form="${formId}" value="<c:out value='${failed ? submitted.color : product.color}'/>" required maxlength="40"<c:if test="${rf eq 'color'}"> aria-invalid="true" aria-describedby="row-error-${pid}"</c:if>>
                        </td>
                        <td data-label="Đang bán" class="admin-half">
                            <label class="check" for="product-active-${pid}">
                                <input id="product-active-${pid}" type="checkbox" name="active" form="${formId}"<c:if test="${failed ? not empty submitted.active : product.active}"> checked</c:if>>
                                <span class="visually-hidden">Đang bán <c:out value="${product.name}"/></span>
                            </label>
                        </td>
                        <td data-label="Nổi bật" class="admin-half">
                            <label class="check" for="product-featured-${pid}">
                                <input id="product-featured-${pid}" type="checkbox" name="featured" form="${formId}"<c:if test="${failed ? not empty submitted.featured : product.featured}"> checked</c:if>>
                                <span class="visually-hidden">Nổi bật <c:out value="${product.name}"/></span>
                            </label>
                        </td>
                        <td class="admin-col-actions" data-label="Thao tác">
                            <form id="${formId}" class="admin-actions" method="post" action="${ctx}/admin/product/update">
                                <input type="hidden" name="productId" value="${pid}">
                                <%-- Lỗi nhập thường: giữ version lúc admin bắt đầu sửa để lần lưu sau vẫn phát hiện sửa đồng thời.
                                     Lỗi xung đột (rf = version): mang version mới nhất để bấm "Lưu" lần nữa là ghi đè có chủ ý. --%>
                                <input type="hidden" name="version" value="<c:out value='${failed and rf ne "version" ? submitted.version : product.version}'/>">
                                <input type="hidden" name="returnTo" value="<c:out value='${listUrl}'/>">
                                <button class="button secondary" type="submit" data-save>Lưu<span class="visually-hidden"> <c:out value="${product.name}"/></span></button>
                                <c:choose>
                                    <c:when test="${hasOrders}">
                                        <c:if test="${product.active}"><button class="button link-button" type="submit" formaction="${ctx}/admin/product/deactivate" formnovalidate title="Sản phẩm đã có trong đơn hàng nên không xóa được">Ngừng bán<span class="visually-hidden"> <c:out value="${product.name}"/></span></button></c:if>
                                    </c:when>
                                    <c:otherwise>
                                        <details class="confirm-inline">
                                            <summary class="button link-button danger">Xóa…<span class="visually-hidden"> <c:out value="${product.name}"/></span></summary>
                                            <button class="button danger" type="submit" formaction="${ctx}/admin/product/delete" formnovalidate>Xác nhận xóa<span class="visually-hidden"> <c:out value="${product.name}"/></span></button>
                                        </details>
                                    </c:otherwise>
                                </c:choose>
                            </form>
                        </td>
                    </tr>
                </c:forEach>
                <c:if test="${empty products}"><tr><td colspan="7" class="admin-empty"><c:choose><c:when test="${filtered}">Không có sản phẩm nào khớp bộ lọc. <a href="${ctx}/admin#san-pham">Bỏ lọc</a></c:when><c:otherwise>Chưa có sản phẩm.</c:otherwise></c:choose></td></tr></c:if>
                </tbody>
            </table>
        </div>
    </section>

    <section class="admin-section" id="ton-kho" aria-labelledby="ton-kho-title">
        <div class="admin-section-head">
            <h2 id="ton-kho-title">Tồn kho</h2>
            <p>Nhập số lượng còn lại cho từng cỡ rồi bấm Lưu.<c:if test="${filtered}"> Danh sách theo bộ lọc ở trên.</c:if></p>
        </div>
        <div class="admin-table-wrap">
            <table class="admin-table admin-table-stock">
                <caption class="visually-hidden">Tồn kho theo cỡ</caption>
                <thead><tr><th scope="col">Sản phẩm</th><th scope="col" class="admin-numeric">Cỡ S</th><th scope="col" class="admin-numeric">Cỡ M</th><th scope="col" class="admin-numeric">Cỡ L</th><th scope="col" class="admin-numeric">Tổng</th><th scope="col" class="admin-col-actions">Thao tác</th></tr></thead>
                <tbody>
                <c:forEach items="${stockRows}" var="row">
                    <c:set var="stockFormId" value="stock-form-${row.productId}"/>
                    <c:set var="rowInactive" value="${inactiveIds.contains(row.productId)}"/>
                    <tr class="data-row" id="stock-row-${row.productId}"<c:if test="${rowInactive}"> data-inactive</c:if>>
                        <td data-label="Sản phẩm" class="admin-cell-name"><span class="admin-stock-name"><c:out value="${row.name}"/></span><c:if test="${rowInactive}"> <span class="badge badge-muted">Ngừng bán</span></c:if></td>
                        <c:forEach items="${['S', 'M', 'L']}" var="size">
                            <td class="admin-numeric admin-half" data-label="Cỡ ${size}">
                                <label class="visually-hidden" for="stock-${size}-${row.productId}">Tồn cỡ ${size} của <c:out value="${row.name}"/></label>
                                <%-- Lấy tồn kho của cỡ đang vẽ: S thì stockS, M thì stockM, còn lại (L) thì stockL. --%>
                                <input id="stock-${size}-${row.productId}" class="admin-input-adjust" type="number" min="0" step="1" inputmode="numeric" required name="stock${size}" form="${stockFormId}" value="${size eq 'S' ? row.stockS : size eq 'M' ? row.stockM : row.stockL}">
                            </td>
                        </c:forEach>
                        <td class="admin-numeric admin-stock admin-half" data-label="Tổng"><c:out value="${row.total}"/></td>
                        <td class="admin-col-actions" data-label="Thao tác">
                            <form id="${stockFormId}" class="admin-actions" method="post" action="${ctx}/admin/stock">
                                <input type="hidden" name="productId" value="${row.productId}">
                                <%-- Tồn kho lúc trang được tạo; server chỉ lưu nếu tồn kho vẫn bằng các số này (xem ProductRepository.updateStock). --%>
                                <input type="hidden" name="expectedS" value="${row.stockS}">
                                <input type="hidden" name="expectedM" value="${row.stockM}">
                                <input type="hidden" name="expectedL" value="${row.stockL}">
                                <input type="hidden" name="returnTo" value="<c:out value='${listUrl}'/>">
                                <button class="button secondary button-small" type="submit" data-save>Lưu<span class="visually-hidden"> tồn kho <c:out value="${row.name}"/></span></button>
                                <a class="button link-button button-small" href="#product-row-${row.productId}">Sản phẩm<span class="visually-hidden"> <c:out value="${row.name}"/></span></a>
                            </form>
                        </td>
                    </tr>
                </c:forEach>
                <c:if test="${empty stockRows}"><tr><td colspan="6" class="admin-empty">Chưa có sản phẩm.</td></tr></c:if>
                </tbody>
            </table>
        </div>
    </section>

</main>
<%@ include file="/WEB-INF/views/fragments/footer.jspf" %>
</body>
</html>
