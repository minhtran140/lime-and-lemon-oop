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

/** Tạo mọi repository và service một lần lúc khởi động rồi đưa cho các servlet (nối thủ công, không dùng framework). */
@WebListener
public final class AppContextListener implements ServletContextListener {
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
    }
}
