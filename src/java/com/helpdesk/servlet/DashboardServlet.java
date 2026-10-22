
package com.helpdesk.servlet;

import com.helpdesk.util.DBConnection;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

@WebServlet("/dashboard-data")
public class DashboardServlet extends HttpServlet {

    private String escape(String value) {
        if (value == null) return "";

        return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws IOException {

        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");

        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("user") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(
                "{\"error\":\"Please log in again.\"}"
            );
            return;
        }

        int total = 0;
        int open = 0;
        int inProgress = 0;
        int resolved = 0;
        int closed = 0;

        StringBuilder tickets = new StringBuilder();
        boolean first = true;

        try (Connection con = DBConnection.getConnection()) {

            String statsSql =
                "SELECT COUNT(*) AS TOTAL, " +
                "SUM(CASE WHEN STATUS = 'OPEN' THEN 1 ELSE 0 END) AS OPEN_COUNT, " +
                "SUM(CASE WHEN STATUS = 'IN_PROGRESS' THEN 1 ELSE 0 END) AS PROGRESS_COUNT, " +
                "SUM(CASE WHEN STATUS = 'RESOLVED' THEN 1 ELSE 0 END) AS RESOLVED_COUNT, " +
                "SUM(CASE WHEN STATUS = 'CLOSED' THEN 1 ELSE 0 END) AS CLOSED_COUNT " +
                "FROM APP.TICKETS";

            try (Statement statement = con.createStatement();
                 ResultSet rs = statement.executeQuery(statsSql)) {

                if (rs.next()) {
                    total = rs.getInt("TOTAL");
                    open = rs.getInt("OPEN_COUNT");
                    inProgress = rs.getInt("PROGRESS_COUNT");
                    resolved = rs.getInt("RESOLVED_COUNT");
                    closed = rs.getInt("CLOSED_COUNT");
                }
            }

            String ticketSql =
                "SELECT TICKET_ID, TITLE, CATEGORY, PRIORITY, STATUS " +
                "FROM APP.TICKETS " +
                "ORDER BY CREATED_AT DESC";

            try (PreparedStatement ps = con.prepareStatement(ticketSql)) {
                ps.setMaxRows(10);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        if (!first) {
                            tickets.append(",");
                        }
                        first = false;

                        tickets.append("{")
                            .append("\"ticketId\":")
                            .append(rs.getInt("TICKET_ID"))
                            .append(",\"title\":\"")
                            .append(escape(rs.getString("TITLE")))
                            .append("\",\"category\":\"")
                            .append(escape(rs.getString("CATEGORY")))
                            .append("\",\"priority\":\"")
                            .append(escape(rs.getString("PRIORITY")))
                            .append("\",\"status\":\"")
                            .append(escape(rs.getString("STATUS")))
                            .append("\"}");
                    }
                }
            }

            String json =
                "{\"totalTickets\":" + total +
                ",\"openTickets\":" + open +
                ",\"inProgressTickets\":" + inProgress +
                ",\"resolvedTickets\":" + resolved +
                ",\"closedTickets\":" + closed +
                ",\"recentTickets\":[" + tickets + "]}";

            response.getWriter().write(json);

        } catch (Exception e) {
            getServletContext().log("Dashboard data error", e);

            response.setStatus(
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR
            );

            response.getWriter().write(
                "{\"error\":\"Unable to load dashboard data.\"}"
            );
        }
    }
}
