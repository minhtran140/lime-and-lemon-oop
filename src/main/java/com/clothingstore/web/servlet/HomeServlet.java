package com.clothingstore.web.servlet;

import com.clothingstore.service.ProductService;
import com.clothingstore.web.WebSupport;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/home")
public final class HomeServlet extends HttpServlet {
    private ProductService products;

    @Override public void init() {
        products = (ProductService) getServletContext().getAttribute("productService");
    }

    @Override protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("categories", products.categories());
        request.setAttribute("featuredProducts", products.featuredProducts());
        request.setAttribute("newestProducts", products.newestProducts());
        request.setAttribute("soldOutProductIds", products.soldOutProductIds());
        request.setAttribute("homePage", true);
        request.setAttribute("flashMessage", WebSupport.consumeFlash(request));
        request.getRequestDispatcher("/WEB-INF/views/home.jsp").forward(request, response);
    }
}
