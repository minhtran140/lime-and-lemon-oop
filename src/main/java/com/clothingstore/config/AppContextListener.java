package com.clothingstore.config;

import com.clothingstore.domain.discount.MemberDiscountPolicy;
import com.clothingstore.repository.CategoryRepository;
import com.clothingstore.repository.OrderRepository;
import com.clothingstore.repository.ProductRepository;
import com.clothingstore.repository.UserRepository;
import com.clothingstore.service.AuthenticationService;
import com.clothingstore.service.CheckoutService;
import com.clothingstore.service.OrderService;
import com.clothingstore.service.ProductService;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Tạo mọi repository và service một lần lúc khởi động rồi đưa cho các servlet (nối thủ công, không dùng framework). */
@WebListener
public final class AppContextListener implements ServletContextListener {
    private static final String PRODUCT_IMAGES = "/assets/products/";

    @Override
    public void contextInitialized(ServletContextEvent event) {
        ServletContext context = event.getServletContext();
        Database database = Database.load();
        CategoryRepository categories = new CategoryRepository(database);
        ProductRepository products = new ProductRepository(database);
        UserRepository users = new UserRepository(database);
        OrderRepository orders = new OrderRepository(database);

        context.setAttribute("authenticationService", new AuthenticationService(users));
        context.setAttribute("productService", new ProductService(categories, products));
        context.setAttribute("checkoutService", new CheckoutService(products, orders, new MemberDiscountPolicy()));
        context.setAttribute("orderService", new OrderService(orders, users));
        context.setAttribute("smallProductImages", smallImageTwins(webappFilesUnder(context, PRODUCT_IMAGES)));
    }

    /**
     * Với mỗi ảnh .webp có sẵn ảnh "-small.webp" cùng thư mục, trả về đường dẫn ảnh -> đường dẫn ảnh nhỏ. Thẻ sản phẩm
     * chỉ tạo srcset cho ảnh có trong bảng này, nên ảnh admin tự nhập (không có bản nhỏ) không bị trỏ vào file không tồn tại.
     */
    public static Map<String, String> smallImageTwins(Set<String> files) {
        Map<String, String> twins = new HashMap<>();
        for (String file : files) {
            if (!file.endsWith(".webp") || file.endsWith("-small.webp")) continue;
            String small = file.substring(0, file.length() - ".webp".length()) + "-small.webp";
            if (files.contains(small)) twins.put(file, small);
        }
        return Map.copyOf(twins);
    }

    /** Mọi file trong một thư mục của webapp, kể cả thư mục con (getResourcePaths trả về thư mục với dấu "/" ở cuối). */
    private static Set<String> webappFilesUnder(ServletContext context, String folder) {
        Set<String> files = new HashSet<>();
        Set<String> entries = context.getResourcePaths(folder);
        if (entries == null) return files;
        for (String entry : entries) {
            if (entry.endsWith("/")) files.addAll(webappFilesUnder(context, entry)); else files.add(entry);
        }
        return files;
    }
}
