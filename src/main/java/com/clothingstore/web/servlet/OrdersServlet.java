package com.clothingstore.web.servlet;

import com.clothingstore.service.OrderService;
import com.clothingstore.web.WebSupport;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet(urlPatterns = {"/orders", "/orders/detail"})
public final class OrdersServlet extends HttpServlet {
    private OrderService orders;

    @Override
    public void init() {
        orders = (OrderService) getServletContext().getAttribute("orderService");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        long customerId = WebSupport.currentUserId(request);
        if ("/orders/detail".equals(request.getServletPath())) {
            request.setAttribute("order", orders.customerOrder(customerId, WebSupport.longParameter(request, "id")));
        } else {
            request.setAttribute("orders", orders.customerHistory(customerId));
        }
        request.setAttribute("dateTimeFormat", WebSupport.DATE_TIME_FORMAT);
        request.setAttribute("flashMessage", WebSupport.consumeFlash(request));
        request.getRequestDispatcher("/WEB-INF/views/orders.jsp").forward(request, response);
    }
}