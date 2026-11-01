<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="app" tagdir="/WEB-INF/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="adminSection" value="orders"/>
<%-- Sau khi đổi trạng thái, quay lại danh sách này với bộ lọc hiện tại. --%>
<c:url var="ordersUrlFull" value="/admin/orders">
    <c:if test="${not empty statusFilter}"><c:param name="status" value="${statusFilter}"/></c:if>
</c:url>
<%-- returnTo là đường dẫn bên trong ứng dụng: phía back tự thêm context path. --%>
<c:set var="ordersUrl" value="${fn:substringAfter(ordersUrlFull, ctx)}"/>
<!doctype html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Đơn hàng · Quản trị · Lime &amp; Lemon</title>
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
            <p>Theo dõi đơn hàng và cập nhật trạng thái.</p>
        </div>
    </div>
    <%@ include file="/WEB-INF/views/fragments/admin-nav.jspf" %>

    <section class="admin-section" id="don-hang" aria-labelledby="don-hang-title">
        <div class="admin-section-head">
            <h2 id="don-hang-title">Đơn hàng</h2>
            <p>Hủy đơn sẽ hoàn hàng trong đơn vào kho.</p>
        </div>
        <nav class="admin-chips" aria-label="Lọc đơn theo trạng thái">
            <a class="chip" href="${ctx}/admin/orders#don-hang"<c:if test="${empty statusFilter}"> aria-current="true"</c:if>>Tất cả</a>
            <c:forEach items="${orderStatuses}" var="status">
                <a class="chip" href="${ctx}/admin/orders?status=${status}#don-hang"<c:if test="${statusFilter eq status.name()}"> aria-current="true"</c:if>><app:enumLabel value="${status}"/></a>
            </c:forEach>
        </nav>
        <div class="admin-table-wrap">
            <table class="admin-table admin-table-orders">
                <caption class="visually-hidden">Đơn hàng</caption>
                <thead><tr><th scope="col">Mã đơn</th><th scope="col">Ngày đặt</th><th scope="col">Người nhận</th><th scope="col" class="admin-numeric">Tổng</th><th scope="col">Trạng thái</th><th scope="col">Cập nhật trạng thái</th><th scope="col" class="admin-col-actions admin-col-details"><span class="visually-hidden">Chi tiết</span></th></tr></thead>
                <tbody>
                <c:forEach items="${orders}" var="order">
                    <c:set var="canMove" value="false"/>
                    <c:set var="canCancel" value="false"/>
                    <c:forEach items="${orderStatuses}" var="status"><c:if test="${order.status.allows(status)}"><c:choose>
                        <c:when test="${status.name() eq 'CANCELLED'}"><c:set var="canCancel" value="true"/></c:when>
                        <c:otherwise><c:set var="canMove" value="true"/></c:otherwise>
                    </c:choose></c:if></c:forEach>
                    <tr class="data-row" id="order-row-${order.orderId}">
                        <td data-label="Mã đơn" class="admin-half"><a href="${ctx}/admin/order/detail?id=${order.orderId}">#<c:out value="${order.orderId}"/></a></td>
                        <td data-label="Ngày đặt" class="admin-half"><c:choose><c:when test="${empty order.createdAt}">—</c:when><c:otherwise>${order.createdAt.format(dateTimeFormat)}</c:otherwise></c:choose></td>
                        <td data-label="Người nhận" class="admin-half"><c:out value="${order.shippingAddress.recipientName}"/></td>
                        <td class="admin-numeric admin-stock admin-half" data-label="Tổng"><app:money amount="${order.total}"/></td>
                        <td data-label="Trạng thái"><span class="status-badge status-${order.status}"><app:enumLabel value="${order.status}"/></span></td>
                        <td data-label="Cập nhật trạng thái">
                            <div class="admin-actions admin-actions-start">
                                <c:if test="${canMove}">
                                    <form class="admin-inline status-control" method="post" action="${ctx}/admin/order/status">
                                        <input type="hidden" name="orderId" value="${order.orderId}">
                                        <input type="hidden" name="returnTo" value="<c:out value='${ordersUrl}'/>">
                                        <label class="visually-hidden" for="order-status-${order.orderId}">Trạng thái đơn #<c:out value="${order.orderId}"/></label>
                                        <select id="order-status-${order.orderId}" name="status"><c:forEach items="${orderStatuses}" var="status"><c:if test="${status == order.status or (order.status.allows(status) and status.name() ne 'CANCELLED')}">
                                            <option value="${status}"<c:if test="${status == order.status}"> selected</c:if>><app:enumLabel value="${status}"/></option>
                                        </c:if></c:forEach></select>
                                        <button class="button secondary button-small" type="submit" data-save>Cập nhật<span class="visually-hidden"> đơn #<c:out value="${order.orderId}"/></span></button>
                                    </form>
                                </c:if>
                                <c:if test="${canCancel}">
                                    <form class="admin-inline" method="post" action="${ctx}/admin/order/status">
                                        <input type="hidden" name="orderId" value="${order.orderId}">
                                        <input type="hidden" name="status" value="CANCELLED">
                                        <input type="hidden" name="returnTo" value="<c:out value='${ordersUrl}'/>">
                                        <details class="confirm-inline">
                                            <summary class="button link-button danger">Hủy đơn…<span class="visually-hidden"> #<c:out value="${order.orderId}"/></span></summary>
                                            <span class="field-hint" id="cancel-hint-${order.orderId}">Hàng được hoàn vào kho.</span>
                                            <button class="button danger button-small" type="submit" aria-describedby="cancel-hint-${order.orderId}">Hủy đơn #<c:out value="${order.orderId}"/></button>
                                        </details>
                                    </form>
                                </c:if>
                                <c:if test="${not canMove and not canCancel}"><span class="field-hint">Không thể đổi trạng thái</span></c:if>
                            </div>
                        </td>
                        <td class="admin-col-actions admin-col-details"><a class="button link-button" href="${ctx}/admin/order/detail?id=${order.orderId}">Chi tiết<span class="visually-hidden"> đơn #<c:out value="${order.orderId}"/></span></a></td>
                    </tr>
                </c:forEach>
                <c:if test="${empty orders}"><tr><td colspan="7" class="admin-empty"><c:choose><c:when test="${not empty statusFilter}">Không có đơn hàng nào ở trạng thái này. <a href="${ctx}/admin/orders#don-hang">Xem tất cả đơn</a></c:when><c:otherwise>Chưa có đơn hàng.</c:otherwise></c:choose></td></tr></c:if>
                </tbody>
            </table>
        </div>
    </section>
</main>
<%@ include file="/WEB-INF/views/fragments/footer.jspf" %>
</body>
</html>
