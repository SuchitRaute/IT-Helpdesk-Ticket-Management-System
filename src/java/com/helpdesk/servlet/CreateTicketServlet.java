package com.helpdesk.servlet;

import com.helpdesk.dao.TicketDAO;
import com.helpdesk.dao.TicketHistoryDAO;
import com.helpdesk.dao.UserDAO;
import com.helpdesk.model.Ticket;
import com.helpdesk.model.User;
import com.helpdesk.model.TicketHistory;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/create-ticket")
public class CreateTicketServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request,
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

        String title = request.getParameter("title");
        String description = request.getParameter("description");
        String category = request.getParameter("category");
        String priority = request.getParameter("priority");

        if (title == null || description == null
                || category == null || priority == null
                || title.trim().isEmpty()
                || description.trim().isEmpty()
                || category.trim().isEmpty()) {
            response.sendError(400, "Complete all required fields.");
            return;
        }

        if (!priority.matches("LOW|MEDIUM|HIGH|CRITICAL")) {
            response.sendError(400, "Invalid priority.");
            return;
        }

        Ticket ticket = new Ticket();
        ticket.setTitle(title.trim());
        ticket.setDescription(description.trim());
        ticket.setCategory(category.trim());
        ticket.setPriority(priority);
        ticket.setCreatedBy(user.getUserId());

        try {
            if (new TicketDAO().createTicket(ticket)) {
                TicketHistory history = new TicketHistory();
                history.setTicketId(ticket.getTicketId());
                history.setChangedBy(user.getUserId());
                history.setOldStatus(null);
                history.setNewStatus(ticket.getStatus());

                if (ticket.getAssignedTo() != null) {
                    User technician = new UserDAO().findById(ticket.getAssignedTo());
                    history.setActionDescription("Ticket created and automatically assigned to "
                            + (technician == null ? "the matching domain technician"
                                                  : technician.getFullName())
                            + " for domain: " + ticket.getCategory());
                } else {
                    history.setActionDescription("Ticket created. No active technician is configured for domain: "
                            + ticket.getCategory() + "; administrator assignment is required.");
                }
                new TicketHistoryDAO().addHistory(history);

                response.sendRedirect(request.getContextPath()
                        + "/ticket-details.html?id=" + ticket.getTicketId());
            } else {
                response.sendError(500, "Ticket creation failed.");
            }
        } catch (SQLException ex) {
            throw new ServletException("Unable to create ticket.", ex);
        }
    }
}