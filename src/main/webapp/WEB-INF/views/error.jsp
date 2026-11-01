<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="status" value="${requestScope['jakarta.servlet.error.status_code']}"/>
<%-- Trang khách định vào: servlet path được forward (khi filter forward), nếu không có thì lấy request URI lỗi của container (các trường hợp sendError). --%>
<c:set var="origin" value="${requestScope['jakarta.servlet.forward.servlet_path']}"/>
<c:if test="${empty origin}">
    <c:set var="origin" value="${requestScope['jakarta.servlet.error.request_uri']}"/>
    <c:if test="${not empty pageContext.request.contextPath and fn:startsWith(origin, pageContext.request.contextPath)}"><c:set var="origin" value="${fn:substringAfter(origin, pageContext.request.contextPath)}"/></c:if>
</c:if>
<c:set var="isAdmin" value="${sessionScope.user.role == 'ADMIN'}"/>
<%-- Tiêu đề và câu mặc định theo mã trạng thái; errorMessage (được set khi có exception) làm câu rõ hơn. --%>
<c:choose>
    <c:when test="${status == 404}">
        <c:set var="heading" value="Không tìm thấy trang"/>
        <c:set var="fallback" value="Đường dẫn này không tồn tại hoặc đã được thay đổi."/>
    </c:when>
    <c:when test="${status == 403}">
        <c:set var="heading" value="Bạn không có quyền xem trang này"/>
        <%-- Câu mặc định cho 403: chưa đăng nhập thì mời đăng nhập; admin thì báo tài khoản quản trị không dùng được trang này; khách hàng thì báo trang chỉ dành cho quản trị. --%>
        <c:set var="fallback" value="${empty sessionScope.user ? 'Vui lòng đăng nhập bằng tài khoản phù hợp để xem trang này.' : (isAdmin ? 'Tài khoản quản trị không dùng được trang này.' : 'Trang này chỉ dành cho tài khoản quản trị.')}"/>
    </c:when>
    <c:when test="${status == 400}">
        <c:set var="heading" value="Yêu cầu không hợp lệ"/>
        <c:set var="fallback" value="Đường dẫn hoặc thông tin gửi lên chưa đúng. Vui lòng kiểm tra lại."/>
    </c:when>
    <c:when test="${status == 409}">
        <c:set var="heading" value="Chưa hoàn tất được thao tác"/>
        <c:set var="fallback" value="Dữ liệu vừa thay đổi. Vui lòng thử lại."/>
    </c:when>
    <c:otherwise>
        <c:set var="heading" value="Đã có lỗi xảy ra"/>
        <c:set var="fallback" value="Chúng tôi chưa xử lý được yêu cầu này. Vui lòng thử lại sau."/>
    </c:otherwise>
</c:choose>
<!doctype html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><c:out value="${heading}"/> · Lime &amp; Lemon</title>
    <%@ include file="/WEB-INF/views/fragments/font-preload.jspf" %>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/store.css?v=${assetVersion}">
</head>
<body class="storefront">
<%@ include file="/WEB-INF/views/fragments/header.jspf" %>
<main id="main-content" tabindex="-1" class="narrow container error-page">
    <h1><c:out value="${heading}"/></h1>
    <div class="notice error" role="alert">
        <strong class="notice-label">Lỗi<c:if test="${not empty status}"> ${status}</c:if>.</strong>
        <c:choose>
            <c:when test="${not empty errorMessage}"><c:out value="${errorMessage}"/></c:when>
            <c:otherwise><c:out value="${fallback}"/></c:otherwise>
        </c:choose>
    </div>
    <div class="error-actions">
        <c:choose>
            <c:when test="${outOfStockError}">
                <a class="button" href="${pageContext.request.contextPath}/cart">Quay lại giỏ hàng</a>
            </c:when>
            <%-- Chỉ admin mới được đưa về khu quản trị; người khác không được đưa link dẫn lại chính trang 403 đó. --%>
            <c:when test="${isAdmin and (origin eq '/admin' or fn:startsWith(origin, '/admin/'))}">
                <a class="button" href="${pageContext.request.contextPath}/admin">Quay lại trang quản trị</a>
            </c:when>
            <c:otherwise>
                <a class="button" href="${pageContext.request.contextPath}/products">Về cửa hàng</a>
            </c:otherwise>
        </c:choose>
        <c:if test="${not empty sessionScope.user and not isAdmin and fn:startsWith(origin, '/orders')}">
            <a class="button secondary" href="${pageContext.request.contextPath}/orders">Xem đơn hàng của bạn</a>
        </c:if>
    </div>
</main>
<%@ include file="/WEB-INF/views/fragments/footer.jspf" %>
</body>
</html>
