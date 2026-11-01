package com.clothingstore.web.servlet;

import com.clothingstore.domain.order.Order;
import com.clothingstore.domain.order.OrderItem;
import com.clothingstore.domain.order.OrderStatus;
import com.clothingstore.domain.product.Product;
import com.clothingstore.domain.user.User;
import com.clothingstore.exception.EntityNotFoundException;
import com.clothingstore.exception.ValidationException;
import com.clothingstore.service.OrderService;
import com.clothingstore.service.ProductService;
import com.clothingstore.web.WebSupport;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/admin/*")
public final class AdminServlet extends HttpServlet {
    private ProductService catalog;
    private OrderService orders;

    @Override
    public void init() {
        catalog = (ProductService) getServletContext().getAttribute("productService");
        orders = (OrderService) getServletContext().getAttribute("orderService");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String page = request.getPathInfo();
        if (page == null || "/".equals(page)) {
            showPage(request, response, "/WEB-INF/views/admin.jsp");
        } else if ("/order/detail".equals(page) || "/orders".equals(page) || "/categories".equals(page)) {
            showPage(request, response, null);
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /** Nạp dữ liệu mà trang admin của đường dẫn này cần rồi hiển thị (flash message, nếu có, đi kèm). */
    private void showPage(HttpServletRequest request, HttpServletResponse response, String view)
            throws ServletException, IOException {
        String page = request.getPathInfo();
        if ("/order/detail".equals(page)) {
            Order order = orders.adminOrder(WebSupport.longParameter(request, "id"));
            User customer = orders.customerOf(order);
            request.setAttribute("order", order);
            request.setAttribute("customerName", customer == null ? null : customer.getFullName());
            request.setAttribute("customerEmail", customer == null ? null : customer.getEmail());
            request.setAttribute("dateTimeFormat", WebSupport.DATE_TIME_FORMAT);
            request.setAttribute("orderStatuses", Arrays.asList(OrderStatus.values()));
            view = "/WEB-INF/views/admin-order-detail.jsp";
        } else if ("/orders".equals(page)) {
            OrderStatus status = statusFilter(request);
            request.setAttribute("orders", orders.allOrdersForAdmin(status));
            request.setAttribute("statusFilter", status == null ? null : status.name());
            request.setAttribute("orderStatuses", Arrays.asList(OrderStatus.values()));
            request.setAttribute("dateTimeFormat", WebSupport.DATE_TIME_FORMAT);
            view = "/WEB-INF/views/admin-orders.jsp";
        } else if ("/categories".equals(page)) {
            request.setAttribute("categoryRows", catalog.categoryRows());
            view = "/WEB-INF/views/admin-categories.jsp";
        } else {
            loadProducts(request, request.getParameter("q"), WebSupport.optionalId(request, "category"));
        }
        request.setAttribute("flashMessage", WebSupport.consumeFlash(request));
        request.getRequestDispatcher(view).forward(request, response);
    }

    /** Dữ liệu của trang sản phẩm: danh sách lọc theo q và category (để trống nghĩa là không lọc), danh mục cho form, bảng tồn kho. */
    private void loadProducts(HttpServletRequest request, String keyword, Long categoryId) {
        if (keyword != null && keyword.isBlank()) keyword = null;
        request.setAttribute("filterQ", keyword);
        request.setAttribute("filterCategoryId", categoryId);
        request.setAttribute("categories", catalog.allCategories());
        request.setAttribute("products", catalog.allProducts(keyword, categoryId));
        request.setAttribute("stockRows", catalog.stockRows(keyword, categoryId));
        request.setAttribute("productIdsWithOrders", catalog.productIdsWithOrders());
        request.setAttribute("featuredCount", catalog.featuredCount());
        request.setAttribute("featuredLimit", ProductService.FEATURED_LIMIT);
    }

    /** Các cặp name=value của query string đã giải mã (giá trị đầu tiên thắng). */
    private static Map<String, String> queryOf(String query) {
        Map<String, String> values = new HashMap<>();
        for (String pair : query.split("&")) {
            int equals = pair.indexOf('=');
            if (equals > 0) {
                values.putIfAbsent(URLDecoder.decode(pair.substring(0, equals), StandardCharsets.UTF_8),
                        URLDecoder.decode(pair.substring(equals + 1), StandardCharsets.UTF_8));
            }
        }
        return values;
    }

    private static OrderStatus statusFilter(HttpServletRequest request) {
        try {
            return OrderStatus.valueOf(request.getParameter("status"));
        } catch (IllegalArgumentException | NullPointerException unknownOrMissing) {
            return null;
        }
    }

    /**
     * Kết quả của một thao tác admin: message là nội dung hiển thị; info = true nếu chỉ là thông báo trung tính
     * (không phải "đã lưu"); anchor là #fragment để cuộn tới sau khi redirect.
     */
    private record Done(String message, boolean info, String anchor) {
        static Done saved(String message) { return new Done(message, false, ""); }
        static Done saved(String message, String anchor) { return new Done(message, false, anchor); }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getPathInfo();
        Done done;
        try {
            done = apply(request, action);
            if (done == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
        } catch (ValidationException exception) {
            String kind = formKind(action);
            Long productId = WebSupport.optionalId(request, "productId");
            // Sửa sản phẩm mà không có id dùng được thì không có dòng nào để hiện lỗi, nên báo lỗi ở trang danh sách.
            if ("update-product".equals(kind) && productId == null) kind = null;
            if (kind == null) {
                WebSupport.flashError(request, exception.getMessage());
                String row = "/stock".equals(action) && productId != null ? "#stock-row-" + productId : "";
                response.sendRedirect(WebSupport.path(request, backTo(request, action) + row));
                return;
            }
            // Tạo hoặc sửa bị từ chối thì hiện lại đúng form đó với những gì đã gõ và ô nhập nào sai.
            WebSupport.formError(request, exception);
            request.setAttribute("submittedKind", kind);
            if (kind.equals("update-product")) request.setAttribute("submittedId", productId);
            // 422 chứ không phải 400: Tomcat đóng kết nối sau khi trả 400, và kết nối bị đóng có thể cắt mất trang lớn
            // này (các trang nhỏ đăng nhập, đăng ký, thanh toán không bị ảnh hưởng nên giữ 400).
            response.setStatus(422);
            request.setAttribute("flashMessage", null);
            if (kind.equals("create-category")) {
                request.setAttribute("categoryRows", catalog.categoryRows());
                request.getRequestDispatcher("/WEB-INF/views/admin-categories.jsp").forward(request, response);
            } else {
                // Giữ danh sách được lọc như admin đang xem: bộ lọc nằm trong returnTo của form.
                String returnTo = WebSupport.adminReturnTo(request);
                Map<String, String> filter = returnTo == null || !returnTo.contains("?") ? Map.of()
                        : queryOf(returnTo.substring(returnTo.indexOf('?') + 1));
                // Regex: số nguyên dương tối đa 18 chữ số, không bắt đầu bằng 0, nên Long.valueOf không thể lỗi.
                String category = filter.get("category");
                Long categoryId = category != null && category.matches("[1-9][0-9]{0,17}") ? Long.valueOf(category) : null;
                loadProducts(request, filter.get("q"), categoryId);
                request.getRequestDispatcher("/WEB-INF/views/admin.jsp").forward(request, response);
            }
            return;
        } catch (EntityNotFoundException exception) {
            WebSupport.flashError(request, exception.getMessage());
            response.sendRedirect(WebSupport.path(request, backTo(request, action)));
            return;
        }
        if (done.info()) WebSupport.flashInfo(request, done.message()); else WebSupport.flash(request, done.message());
        response.sendRedirect(WebSupport.path(request, backTo(request, action) + done.anchor()));
    }

    /** Form nào đã gửi POST bị từ chối, khi form đó được hiện lại với giá trị đã gõ; null với các thao tác thường. */
    private static String formKind(String action) {
        return switch (action == null ? "" : action) {
            case "/product/create" -> "create-product";
            case "/product/update" -> "update-product";
            case "/category/create" -> "create-category";
            default -> null;
        };
    }

    /** Trang mà POST quay về: returnTo của form (khi là đường dẫn /admin an toàn), nếu không thì trang mà thao tác thuộc về. */
    private static String backTo(HttpServletRequest request, String action) {
        String returnTo = WebSupport.adminReturnTo(request);
        return returnTo != null ? returnTo : pageAfter(action);
    }

    /** Trang admin mà POST quay về khi form không nói rõ, để kết quả (hoặc lỗi) hiện đúng chỗ có form. */
    static String pageAfter(String action) {
        String path = action == null ? "" : action;
        if (path.startsWith("/category")) return "/admin/categories";
        if (path.startsWith("/order")) return "/admin/orders";
        return "/admin";
    }

    /** Chạy thao tác; trả về null khi không có thao tác như vậy. */
    private Done apply(HttpServletRequest request, String action) {
        switch (action == null ? "" : action) {
            case "/category/create" -> {
                return Done.saved("Đã thêm danh mục “" + catalog.createCategory(request.getParameter("name")).getName() + "”.");
            }
            case "/category/delete" -> {
                return Done.saved("Đã xóa danh mục “"
                        + catalog.deleteCategory(WebSupport.longParameter(request, "categoryId")).getName() + "”.");
            }
            case "/product/create" -> {
                Product product = catalog.createProduct(
                        WebSupport.longParameter(request, "categoryId", "Vui lòng chọn danh mục."),
                        request.getParameter("name"), request.getParameter("description"), request.getParameter("color"),
                        requiredPrice(request), request.getParameter("imageUrl"), WebSupport.checkbox(request, "featured"),
                        stock(request, "stockS", "S"), stock(request, "stockM", "M"), stock(request, "stockL", "L"));
                return Done.saved("Đã tạo sản phẩm “" + product.getName() + "”." + featuredNote(product));
            }
            case "/product/update" -> {
                Product product = catalog.updateProduct(WebSupport.longParameter(request, "productId"),
                        WebSupport.longParameter(request, "categoryId", "Vui lòng chọn danh mục."),
                        request.getParameter("name"), request.getParameter("description"), request.getParameter("color"),
                        requiredPrice(request), request.getParameter("imageUrl"), WebSupport.checkbox(request, "active"),
                        WebSupport.checkbox(request, "featured"));
                return Done.saved("Đã lưu sản phẩm “" + product.getName() + "”." + featuredNote(product),
                        "#product-row-" + product.getProductId());
            }
            case "/product/deactivate" -> {
                Product product = catalog.deactivateProduct(WebSupport.longParameter(request, "productId"));
                return Done.saved("Đã ngừng bán “" + product.getName() + "”.", "#product-row-" + product.getProductId());
            }
            case "/product/delete" -> {
                return Done.saved("Đã xóa sản phẩm “"
                        + catalog.deleteProduct(WebSupport.longParameter(request, "productId")).getName() + "”.");
            }
            case "/stock" -> {
                Product product = catalog.updateStock(WebSupport.longParameter(request, "productId"),
                        WebSupport.intParameter(request, "stockS", "Tồn cỡ S"),
                        WebSupport.intParameter(request, "stockM", "Tồn cỡ M"),
                        WebSupport.intParameter(request, "stockL", "Tồn cỡ L"),
                        WebSupport.intParameter(request, "expectedS"), WebSupport.intParameter(request, "expectedM"),
                        WebSupport.intParameter(request, "expectedL"));
                return Done.saved("Đã cập nhật tồn kho của “" + product.getName() + "”.",
                        "#stock-row-" + product.getProductId());
            }
            case "/order/status" -> {
                return orderStatusDone(orders.changeStatus(WebSupport.longParameter(request, "orderId"),
                        WebSupport.enumParameter(request, "status", OrderStatus.class)));
            }
            default -> {
                return null;
            }
        }
    }

    private static Done orderStatusDone(OrderService.StatusChange change) {
        Order order = change.order();
        if (!change.changed()) {
            return new Done("Đơn #" + order.getOrderId() + " đã ở trạng thái “" + order.getStatus().getLabel()
                    + "”, không có thay đổi.", true, "");
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            int items = order.getItems().stream().mapToInt(OrderItem::getQuantity).sum();
            return Done.saved("Đã hủy đơn #" + order.getOrderId() + " và hoàn " + items + " sản phẩm vào kho.");
        }
        return Done.saved("Đã chuyển đơn #" + order.getOrderId() + " sang “" + order.getStatus().getLabel() + "”.");
    }

    /**
     * Ghi chú thêm khi sản phẩm vừa lưu đang nổi bật và đang bán, và số sản phẩm nổi bật vượt quá
     * FEATURED_LIMIT; trả về "" trong các trường hợp còn lại.
     */
    private String featuredNote(Product product) {
        if (!product.isFeatured() || !product.isActive() || catalog.featuredCount() <= ProductService.FEATURED_LIMIT) return "";
        return " Trang chủ chỉ hiện " + ProductService.FEATURED_LIMIT + " sản phẩm nổi bật, ưu tiên sản phẩm mới nhất.";
    }

    /** Ô tồn kho bỏ trống được tính là 0. */
    private static int stock(HttpServletRequest request, String name, String size) {
        String value = request.getParameter(name);
        return value == null || value.isBlank() ? 0 : WebSupport.intParameter(request, name, "Tồn cỡ " + size);
    }

    private static BigDecimal requiredPrice(HttpServletRequest request) {
        String value = request.getParameter("price");
        try {
            return new BigDecimal(value == null ? "" : value.trim());
        } catch (NumberFormatException exception) {
            throw new ValidationException("Giá không hợp lệ.", "price", exception);
        }
    }
}
