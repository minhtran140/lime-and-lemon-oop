<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="isRegister" value="${accountMode == 'register'}"/>
<c:set var="formErrorText" value="${isRegister ? registrationError : loginError}"/>
<c:set var="emailValue" value="${isRegister ? registrationEmail : loginEmail}"/>
<%-- Id cho aria-describedby của ô mật khẩu: gợi ý mật khẩu (chỉ khi đăng ký) cộng với lỗi mật khẩu (nếu có), cách nhau bằng dấu cách. --%>
<c:set var="passwordDescribedBy" value="${isRegister ? 'password-hint' : ''}${errorField eq 'password' ? (isRegister ? ' password-error' : 'password-error') : ''}"/>
<!doctype html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><c:choose><c:when test="${isRegister}">Tạo tài khoản</c:when><c:otherwise>Đăng nhập</c:otherwise></c:choose> · Lime &amp; Lemon</title>
    <%@ include file="/WEB-INF/views/fragments/font-preload.jspf" %>
    <link rel="stylesheet" href="${ctx}/assets/store.css?v=${assetVersion}">
    <link rel="stylesheet" href="${ctx}/assets/account.css?v=${assetVersion}">
</head>
<body class="storefront">
<%@ include file="/WEB-INF/views/fragments/header.jspf" %>
<main id="main-content" tabindex="-1" class="container account-page">
    <%@ include file="/WEB-INF/views/fragments/flash.jspf" %>
    <div class="account-layout">
        <%-- Form đứng trước trong DOM (thứ tự Tab, thứ tự đọc); panel giới thiệu được CSS đặt bên cạnh form. --%>
        <section class="account-card" aria-labelledby="account-title">
        <span class="account-mark" aria-hidden="true"><%@ include file="/WEB-INF/views/fragments/logo-mark.jspf" %></span>
        <h1 id="account-title"><c:choose><c:when test="${isRegister}">Tạo tài khoản</c:when><c:otherwise>Chào mừng trở lại</c:otherwise></c:choose></h1>
        <p class="account-lead"><c:choose>
            <c:when test="${isRegister}">Chỉ cần họ tên, email và mật khẩu để bắt đầu đặt hàng.</c:when>
            <c:otherwise>Đăng nhập để đặt hàng và theo dõi đơn của bạn.</c:otherwise>
        </c:choose></p>

        <c:if test="${not empty formErrorText}">
            <div class="form-error-summary" tabindex="-1" autofocus data-focus-error-summary role="alert" aria-labelledby="account-error-title">
                <h2 id="account-error-title"><c:choose><c:when test="${isRegister}">Vui lòng kiểm tra thông tin đăng ký</c:when><c:otherwise>Chưa đăng nhập được</c:otherwise></c:choose></h2>
                <p><c:out value="${formErrorText}"/></p>
            </div>
        </c:if>

        <form class="account-form" method="post" action="${ctx}/account/${accountMode}">
            <p class="form-required-note">Tất cả các mục đều bắt buộc.</p>
            <c:if test="${isRegister}">
                <label for="fullName">Họ và tên
                    <input id="fullName" name="fullName" required maxlength="120" autocomplete="name" value="<c:out value='${registrationFullName}'/>"<c:if test="${errorField eq 'fullName'}"> aria-invalid="true" aria-describedby="fullName-error"</c:if>>
                </label>
                <c:if test="${errorField eq 'fullName'}"><p class="field-error" id="fullName-error"><c:out value="${formErrorText}"/></p></c:if>
            </c:if>
            <label for="email">Email
                <input id="email" type="email" name="email" required maxlength="254" autocomplete="email" value="<c:out value='${emailValue}'/>"<c:if test="${errorField eq 'email'}"> aria-invalid="true" aria-describedby="email-error"</c:if>>
            </label>
            <c:if test="${errorField eq 'email'}"><p class="field-error" id="email-error"><c:out value="${formErrorText}"/></p></c:if>
            <label for="password">Mật khẩu
                <input id="password" type="password" name="password" required <c:if test="${isRegister}">minlength="6"</c:if><c:if test="${not isRegister}">minlength="1"</c:if> maxlength="128" autocomplete="${isRegister ? 'new-password' : 'current-password'}"<c:if test="${not empty passwordDescribedBy}"> aria-describedby="${passwordDescribedBy}"</c:if><c:if test="${errorField eq 'password'}"> aria-invalid="true"</c:if>>
                <c:if test="${isRegister}"><span class="field-hint" id="password-hint">Ít nhất 6 ký tự.</span></c:if>
            </label>
            <c:if test="${errorField eq 'password'}"><p class="field-error" id="password-error"><c:out value="${formErrorText}"/></p></c:if>
            <button class="button-primary" type="submit" <c:choose><c:when test="${isRegister}">data-busy-label="Đang tạo tài khoản…">Tạo tài khoản</c:when><c:otherwise>data-busy-label="Đang đăng nhập…">Đăng nhập</c:otherwise></c:choose></button>
        </form>
        <p class="account-switch"><c:choose>
            <c:when test="${isRegister}">Đã có tài khoản? <a href="${ctx}/account/login">Đăng nhập</a></c:when>
            <c:otherwise>Chưa có tài khoản? <a href="${ctx}/account/register">Tạo tài khoản</a></c:otherwise>
        </c:choose></p>
        </section>
        <aside class="account-story" aria-labelledby="account-story-title">
            <div class="account-story-brand"><span class="account-story-mark" aria-hidden="true"><%@ include file="/WEB-INF/views/fragments/logo-mark.jspf" %></span><span class="brand-name">Lime <em>&amp;</em> Lemon</span></div>
            <div class="account-story-copy">
                <p class="account-story-kicker">GỌN GÀNG, TỰ NHIÊN</p>
                <p class="account-story-line" id="account-story-title">Một chút xanh cho ngày thường.</p>
                <p>Trang phục quen thuộc, lựa chọn theo cách riêng của bạn. Đăng nhập để đặt hàng và theo dõi đơn tại Lime &amp; Lemon.</p>
            </div>
            <a class="account-story-link" href="${ctx}/products">Khám phá trang phục <span aria-hidden="true">↗</span></a>
            <span class="account-story-watermark" aria-hidden="true"><%@ include file="/WEB-INF/views/fragments/logo-mark.jspf" %></span>
        </aside>
    </div>
</main>
<%@ include file="/WEB-INF/views/fragments/footer.jspf" %>
</body>
</html>
