package com.clothingstore.web;

import com.clothingstore.domain.cart.Cart;
import com.clothingstore.domain.user.User;
import com.clothingstore.exception.AuthorizationException;
import com.clothingstore.exception.ValidationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Các hàm nhỏ dùng chung cho các servlet: giỏ hàng và người dùng trong session, flash message và đọc tham số. */
public final class WebSupport {
    /** Định dạng ngày giờ của đơn hàng khi hiển thị cho người dùng (giờ Việt Nam; Order.getCreatedAt đã đổi sẵn). */
    public static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private WebSupport() {}

    public static Cart cart(HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        if (session.getAttribute("shoppingCart") == null) session.setAttribute("shoppingCart", new Cart());
        return (Cart) session.getAttribute("shoppingCart");
    }

    /** Người dùng đang đăng nhập, hoặc null. */
    public static User currentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (User) session.getAttribute("user");
    }

    public static long currentUserId(HttpServletRequest request) {
        User user = currentUser(request);
        if (user == null) throw new AuthorizationException("Vui lòng đăng nhập để tiếp tục.");
        return user.getUserId();
    }

    public static void signIn(HttpServletRequest request, User user) {
        request.getSession(true).setAttribute("user", user);
        // Đổi session id sau khi đăng nhập để chặn session fixation (kẻ tấn công cài sẵn một session id cũ cho nạn nhân).
        request.changeSessionId();
    }

    /**
     * Thông báo chỉ hiện một lần ở trang kế tiếp (sau redirect). Lưu trong session vì redirect là một request mới;
     * đây là mẫu Post/Redirect/Get: sau khi POST thì redirect để người dùng bấm F5 không gửi lại form.
     */
    public static void flash(HttpServletRequest request, String message) {
        store(request, message, "success");
    }

    public static void flashError(HttpServletRequest request, String message) {
        store(request, message, "error");
    }

    /** Thông báo trung tính: không có gì thất bại và không có gì được lưu (cần đăng nhập trước, không có gì để đổi). */
    public static void flashInfo(HttpServletRequest request, String message) {
        store(request, message, "info");
    }

    private static void store(HttpServletRequest request, String message, String kind) {
        HttpSession session = request.getSession(true);
        session.setAttribute("flashMessage", message);
        session.setAttribute("flashKind", kind);
    }

    /** Trả về nội dung flash (null nếu không có), đặt các request attribute flashError và flashInfo, rồi xóa flash. */
    public static String consumeFlash(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return null;
        Object message = session.getAttribute("flashMessage");
        Object kind = session.getAttribute("flashKind");
        request.setAttribute("flashError", "error".equals(kind));
        request.setAttribute("flashInfo", "info".equals(kind));
        session.removeAttribute("flashMessage");
        session.removeAttribute("flashKind");
        return (String) message;
    }

    /**
     * Trả về đường dẫn nếu an toàn để redirect tới (bắt đầu bằng đúng một dấu "/", không có dấu gạch chéo ngược,
     * khoảng trắng hay ký tự điều khiển, nên không bao giờ trỏ sang site khác), ngược lại trả về null.
     */
    public static String localPath(String path) {
        if (path == null || !path.startsWith("/") || path.startsWith("//")) return null;
        for (int index = 0; index < path.length(); index++) {
            char c = path.charAt(index);
            // c <= ' ' gồm khoảng trắng và ký tự điều khiển (mã 0-32); 127 là DEL.
            if (c == '\\' || c <= ' ' || c == 127) return null;
        }
        return path;
    }

    /** returnTo được gửi lên nếu là đường dẫn nội bộ nằm trong /admin (đã bỏ phần #fragment), ngược lại null. */
    public static String adminReturnTo(HttpServletRequest request) {
        String path = localPath(request.getParameter("returnTo"));
        if (path == null || !(path.equals("/admin") || path.startsWith("/admin/") || path.startsWith("/admin?"))) return null;
        int fragment = path.indexOf('#');
        return fragment < 0 ? path : path.substring(0, fragment);
    }

    /** Hiện lại form sau khi bị từ chối: thông báo lỗi, ô nhập liên quan và những gì đã gõ (không bao giờ gồm mật khẩu). */
    public static void formError(HttpServletRequest request, ValidationException exception) {
        request.setAttribute("formError", exception.getMessage());
        request.setAttribute("errorField", exception.getField());
        Map<String, String> submitted = new LinkedHashMap<>();
        request.getParameterMap().forEach((name, values) -> {
            if (!name.toLowerCase(Locale.ROOT).contains("password") && values.length > 0) submitted.put(name, values[0]);
        });
        request.setAttribute("submitted", submitted);
    }

    public static String path(HttpServletRequest request, String path) {
        return request.getContextPath() + path;
    }

    /** Một id dương, hoặc null khi thiếu tham số hoặc không phải số. */
    public static Long optionalId(HttpServletRequest request, String name) {
        try {
            long id = Long.parseLong(request.getParameter(name));
            return id > 0 ? id : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    public static long longParameter(HttpServletRequest request, String name) {
        Long id = optionalId(request, name);
        if (id == null) throw new ValidationException("Đường dẫn không hợp lệ.");
        return id;
    }

    /** Giống longParameter ở trên, nhưng thông báo lỗi và tên trường là của ô nhập trong form (dùng cho id chọn trong form). */
    public static long longParameter(HttpServletRequest request, String name, String message) {
        Long id = optionalId(request, name);
        if (id == null) throw new ValidationException(message, name);
        return id;
    }

    public static int intParameter(HttpServletRequest request, String name) {
        return intParameter(request, name, "Giá trị số");
    }

    /** Một số nguyên; nhãn (label, ví dụ "Tồn cỡ S") dùng để gọi tên ô nhập trong thông báo lỗi. */
    public static int intParameter(HttpServletRequest request, String name, String label) {
        try {
            return Integer.parseInt(request.getParameter(name));
        } catch (NumberFormatException exception) {
            throw new ValidationException(label + " không hợp lệ.", name);
        }
    }

    public static boolean checkbox(HttpServletRequest request, String name) {
        return "on".equals(request.getParameter(name)) || "true".equals(request.getParameter(name));
    }

    /** T phải là kiểu enum (ví dụ OrderStatus); thiếu tham số (NullPointerException) hay sai tên đều thành ValidationException. */
    public static <T extends Enum<T>> T enumParameter(HttpServletRequest request, String name, Class<T> type) {
        try {
            return Enum.valueOf(type, request.getParameter(name));
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new ValidationException("Lựa chọn không hợp lệ.");
        }
    }
}
