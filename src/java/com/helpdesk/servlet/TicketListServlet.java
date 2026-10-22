package com.helpdesk.servlet;

import com.helpdesk.dao.TicketDAO;
import com.helpdesk.model.Ticket;
import com.helpdesk.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@WebServlet("/tickets")
public class TicketListServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        User user = session == null ? null
                : (User) session.getAttribute("user");

        if (user == null) {
            response.sendRedirect(
                    request.getContextPath() + "/login.html");
            return;
        }

        try {
            TicketDAO dao = new TicketDAO();
            List<Ticket> tickets;

            if ("ADMIN".equals(user.getRole())) {
                tickets = dao.getAllTickets();
            } else if ("TECHNICIAN".equals(user.getRole())) {
                tickets = dao.getTicketsByTechnician(user.getUserId());
            } else {
                tickets = dao.getTicketsByCreator(user.getUserId());
            }

            request.setAttribute("tickets", tickets);
            request.getRequestDispatcher("/tickets.jsp")
                   .forward(request, response);

        } catch (SQLException ex) {
            throw new ServletException("Unable to load tickets.", ex);
        }
    }
}