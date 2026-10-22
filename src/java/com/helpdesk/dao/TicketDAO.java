package com.helpdesk.dao;

import com.helpdesk.model.Ticket;
import com.helpdesk.util.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class TicketDAO {

    public boolean createTicket(Ticket ticket) throws SQLException {
        String sql = "INSERT INTO TICKETS "
                + "(TITLE, DESCRIPTION, CATEGORY, PRIORITY, STATUS, CREATED_BY, ASSIGNED_TO) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                Integer technicianId = findMatchingTechnician(con, ticket.getCategory());
                String initialStatus = technicianId == null ? "OPEN" : "ASSIGNED";

                try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, ticket.getTitle());
                    ps.setString(2, ticket.getDescription());
                    ps.setString(3, ticket.getCategory());
                    ps.setString(4, ticket.getPriority());
                    ps.setString(5, initialStatus);
                    ps.setInt(6, ticket.getCreatedBy());
                    if (technicianId == null) ps.setNull(7, java.sql.Types.INTEGER);
                    else ps.setInt(7, technicianId);

                    if (ps.executeUpdate() != 1) {
                        con.rollback();
                        return false;
                    }
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) ticket.setTicketId(keys.getInt(1));
                    }
                }

                ticket.setStatus(initialStatus);
                ticket.setAssignedTo(technicianId);
                con.commit();
                return ticket.getTicketId() > 0;
            } catch (SQLException ex) {
                con.rollback();
                throw ex;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    /*
     * Fixed technician routing by ticket category.
     * The technician account must exist in USERS with ROLE='TECHNICIAN'
     * and the exact FULL_NAME shown in this mapping.
     */
    private Integer findMatchingTechnician(Connection con, String category)
            throws SQLException {
        String technicianName = technicianNameForCategory(category);
        if (technicianName == null) {
            return null;
        }

        String sql = "SELECT USER_ID FROM USERS "
                + "WHERE ROLE = 'TECHNICIAN' AND IS_ACTIVE = TRUE "
                + "AND UPPER(TRIM(FULL_NAME)) = UPPER(TRIM(?))";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, technicianName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("USER_ID") : null;
            }
        }
    }

    private String technicianNameForCategory(String category) {
        if (category == null) return null;
        switch (category.trim().toUpperCase(java.util.Locale.ROOT)) {
            case "SOFTWARE":
                return "Rohit Sharma";
            case "HARDWARE":
                return "Amit Patil";
            case "NETWORK":
                return "Akash Verma";
            case "EMAIL":
                return "Priya Deshmukh";
            case "ACCOUNT ACCESS":
                return "Sneha Joshi";
            case "SECURITY":
                return "Rahul Kulkarni";
            default:
                return null;
        }
    }

    public List<Ticket> getAllTickets() throws SQLException {
        List<Ticket> tickets = new ArrayList<>();

        String sql = "SELECT * FROM TICKETS ORDER BY CREATED_AT DESC";

        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                tickets.add(mapTicket(rs));
            }
        }

        return tickets;
    }

    public Ticket getTicketById(int ticketId) throws SQLException {
        String sql = "SELECT * FROM TICKETS WHERE TICKET_ID = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, ticketId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapTicket(rs);
                }
            }
        }

        return null;
    }

    public List<Ticket> getTicketsByCreator(int userId) throws SQLException {
        List<Ticket> tickets = new ArrayList<>();

        String sql = "SELECT * FROM TICKETS "
                   + "WHERE CREATED_BY = ? ORDER BY CREATED_AT DESC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tickets.add(mapTicket(rs));
                }
            }
        }

        return tickets;
    }

    public List<Ticket> getTicketsByTechnician(int technicianId)
            throws SQLException {

        List<Ticket> tickets = new ArrayList<>();

        String sql = "SELECT * FROM TICKETS "
                   + "WHERE ASSIGNED_TO = ? ORDER BY CREATED_AT DESC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, technicianId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tickets.add(mapTicket(rs));
                }
            }
        }

        return tickets;
    }

    public boolean assignTicket(int ticketId, int technicianId)
            throws SQLException {

        String sql = "UPDATE TICKETS SET ASSIGNED_TO = ?, "
                   + "STATUS = 'ASSIGNED', UPDATED_AT = CURRENT_TIMESTAMP "
                   + "WHERE TICKET_ID = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, technicianId);
            ps.setInt(2, ticketId);

            return ps.executeUpdate() == 1;
        }
    }

    public boolean updateStatus(int ticketId, String status)
            throws SQLException {

        String sql = "UPDATE TICKETS SET STATUS = ?, "
                   + "UPDATED_AT = CURRENT_TIMESTAMP, "
                   + "RESOLVED_AT = CASE WHEN ? = 'RESOLVED' "
                   + "THEN CURRENT_TIMESTAMP ELSE RESOLVED_AT END "
                   + "WHERE TICKET_ID = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, status);
            ps.setString(2, status);
            ps.setInt(3, ticketId);

            return ps.executeUpdate() == 1;
        }
    }

    public boolean deleteTicket(int ticketId) throws SQLException {
        String sql = "DELETE FROM TICKETS WHERE TICKET_ID = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, ticketId);

            return ps.executeUpdate() == 1;
        }
    }

    private Ticket mapTicket(ResultSet rs) throws SQLException {
        Ticket ticket = new Ticket();

        ticket.setTicketId(rs.getInt("TICKET_ID"));
        ticket.setTitle(rs.getString("TITLE"));
        ticket.setDescription(rs.getString("DESCRIPTION"));
        ticket.setCategory(rs.getString("CATEGORY"));
        ticket.setPriority(rs.getString("PRIORITY"));
        ticket.setStatus(rs.getString("STATUS"));
        ticket.setCreatedBy(rs.getInt("CREATED_BY"));

        int assignedTo = rs.getInt("ASSIGNED_TO");
        ticket.setAssignedTo(rs.wasNull() ? null : assignedTo);

        ticket.setCreatedAt(rs.getTimestamp("CREATED_AT"));
        ticket.setUpdatedAt(rs.getTimestamp("UPDATED_AT"));
        ticket.setResolvedAt(rs.getTimestamp("RESOLVED_AT"));

        return ticket;
    }
}