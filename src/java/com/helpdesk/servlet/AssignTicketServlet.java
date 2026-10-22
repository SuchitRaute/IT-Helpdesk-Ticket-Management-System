package com.helpdesk.servlet;

import com.helpdesk.dao.TicketDAO;
import com.helpdesk.dao.TicketHistoryDAO;
import com.helpdesk.dao.UserDAO;
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

@WebServlet("/assign-ticket")
public class AssignTicketServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        User admin = session == null ? null
                : (User) session.getAttribute("user");

        if (admin == null) {
            response.sendRedirect(
                    request.getContextPath() + "/login.html");
            return;
        }

        if (!"ADMIN".equals(admin.getRole())) {
            response.sendError(403);
            return;
        }

        int ticketId;
        int technicianId;

        try {
            ticketId = Integer.parseInt(
                    request.getParameter("ticketId"));
            technicianId = Integer.parseInt(
                    request.getParameter("technicianId"));
        } catch (NumberFormatException ex) {
            response.sendError(400, "Invalid ID.");
            return;
        }

        try {
            UserDAO userDAO = new UserDAO();
            User technician = userDAO.findById(technicianId);

            if (technician == null
                    || !"TECHNICIAN".equals(technician.getRole())
                    || !technician.isActive()) {
                response.sendError(400, "Invalid technician.");
                return;
            }

            TicketDAO ticketDAO = new TicketDAO();
            Ticket ticket = ticketDAO.getTicketById(ticketId);

            if (ticket == null) {
                response.sendError(404, "Ticket not found.");
                return;
            }

            String oldStatus = ticket.getStatus();

            if (ticketDAO.assignTicket(ticketId, technicianId)) {
                TicketHistory history = new TicketHistory();
                history.setTicketId(ticketId);
                history.setChangedBy(admin.getUserId());
                history.setOldStatus(oldStatus);
                history.setNewStatus("ASSIGNED");
                history.setActionDescription(
                        "Assigned to " + technician.getFullName());

                new TicketHistoryDAO().addHistory(history);

                response.sendRedirect(
                        request.getContextPath() + "/tickets?assigned=1");
            } else {
                response.sendError(500, "Assignment failed.");
            }

        } catch (SQLException ex) {
            throw new ServletException("Unable to assign ticket.", ex);
        }
    }
}