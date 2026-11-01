<%@ tag body-content="empty" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ attribute name="value" required="true" type="java.lang.Object" %>
<%-- Từ EL 3.0, += là toán tử nối chuỗi: nối chuỗi rỗng '' với value để đổi enum OrderStatus thành tên dạng chuỗi (PENDING, CONFIRMED...) cho các c:when bên dưới so sánh. --%>
<c:set var="key" value="${'' += value}"/>
<c:choose>
    <c:when test="${key eq 'PENDING'}">Chờ xác nhận</c:when>
    <c:when test="${key eq 'CONFIRMED'}">Đã xác nhận</c:when>
    <c:when test="${key eq 'SHIPPING'}">Đang giao</c:when>
    <c:when test="${key eq 'COMPLETED'}">Hoàn thành</c:when>
    <c:when test="${key eq 'CANCELLED'}">Đã hủy</c:when>
    <c:when test="${key eq 'COD'}">Thanh toán khi nhận hàng</c:when>
    <c:otherwise><c:out value="${key}"/></c:otherwise>
</c:choose>
