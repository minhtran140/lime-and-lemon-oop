package com.clothingstore.web.servlet;

import com.clothingstore.exception.EntityNotFoundException;
import com.clothingstore.service.ProductService;
import com.clothingstore.web.WebSupport;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/** /products và /search liệt kê sản phẩm (từ khóa, danh mục, sắp xếp); /product?id= hiện một sản phẩm. */
@WebServlet(urlPatterns = {"/products", "/search", "/product"})
public final class CatalogServlet extends HttpServlet {
    private ProductService products;

    @Override
    public void init() {
        products = (ProductService) getServletContext().getAttribute("productService");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setAttribute("flashMessage", WebSupport.consumeFlash(request));
        if ("/product".equals(request.getServletPath())) {
            Long productId = WebSupport.optionalId(request, "id");
            if (productId == null) throw new EntityNotFoundException("Không tìm thấy sản phẩm.");
            request.setAttribute("details", products.productDetails(productId));
            request.getRequestDispatcher("/WEB-INF/views/product.jsp").forward(request, response);
            return;
        }
        // Bộ lọc không đọc được (chữ làm id danh mục, kiểu sắp xếp không biết) thì bị bỏ qua.
        Long categoryId = WebSupport.optionalId(request, "categoryId");
        request.setAttribute("categories", products.categories());
        request.setAttribute("soldOutProductIds", products.soldOutProductIds());
        request.setAttribute("selectedCategoryId", categoryId);
        request.setAttribute("selectedSort", request.getParameter("sort"));
        request.setAttribute("products",
                products.search(request.getParameter("keyword"), categoryId, request.getParameter("sort")));
        request.getRequestDispatcher("/WEB-INF/views/catalog.jsp").forward(request, response);
    }
}
