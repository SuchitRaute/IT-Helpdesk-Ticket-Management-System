package com.helpdesk.servlet;

import com.helpdesk.dao.TicketDAO;
import com.helpdesk.dao.TicketHistoryDAO;
import com.helpdesk.model.Ticket;
import com.helpdesk.model.TicketHistory;
import com.helpdesk.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

@WebServlet("/update-ticket-status")
public class UpdateTicketStatusServlet extends HttpServlet {

    private static final List<String> VALID_STATUSES =
            Arrays.asList("OPEN", "ASSIGNED", "IN_PROGRESS",
                    "ON_HOLD", "RESOLVED", "CLOSED");

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

        if (!"ADMIN".equals(user.getRole())
                && !"TECHNICIAN".equals(user.getRole())) {
            response.sendError(403);
            return;
        }

        int ticketId;
        try {
            ticketId = Integer.parseInt(
                    request.getParameter("ticketId"));
        } catch (NumberFormatException ex) {
            response.sendError(400, "Invalid ticket ID.");
            return;
        }

        String newStatus = request.getParameter("status");
        if (newStatus == null || !VALID_STATUSES.contains(newStatus)) {
            response.sendError(400, "Invalid status.");
            return;
        }

        try {
            TicketDAO dao = new TicketDAO();
            Ticket ticket = dao.getTicketById(ticketId);

            if (ticket == null) {
                response.sendError(404, "Ticket not found.");
                return;
            }

            if ("TECHNICIAN".equals(user.getRole())
        && "OPEN".equals(newStatus)) {
    response.sendError(403, "Technicians cannot reopen tickets as OPEN.");
    return;
}

            String oldStatus = ticket.getStatus();

            if (dao.updateStatus(ticketId, newStatus)) {
                TicketHistory history = new TicketHistory();
                history.setTicketId(ticketId);
                history.setChangedBy(user.getUserId());
                history.setOldStatus(oldStatus);
                history.setNewStatus(newStatus);
                history.setActionDescription(
                        "Status changed from " + oldStatus
                        + " to " + newStatus);

                new TicketHistoryDAO().addHistory(history);

                response.sendRedirect(
                        request.getContextPath() + "/tickets?updated=1");
            } else {
                response.sendError(500, "Status update failed.");
            }

        } catch (SQLException ex) {
            throw new ServletException("Unable to update status.", ex);
        }
    }
}