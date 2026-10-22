
package com.helpdesk.servlet;

import com.helpdesk.model.User;
import com.helpdesk.util.DBConnection;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.*;

@WebServlet("/portal-data")
public class PortalDataServlet extends HttpServlet {

    private String esc(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }

    private void send(HttpServletResponse response, String json)
            throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.getWriter().write(json);
    }

    private User getLoggedInUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return null;
        Object value = session.getAttribute("user");
        return value instanceof User ? (User) value : null;
    }

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws IOException {

        User user = getLoggedInUser(request);

        if (user == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            send(response, "{\"error\":\"Please log in again.\"}");
            return;
        }

        String type = request.getParameter("type");
        if (type == null) type = "tickets";

        try (Connection connection = DBConnection.getConnection()) {

            if ("employees".equals(type) || "technicians".equals(type)) {
                loadUsers(connection, response,
                        "technicians".equals(type) ? "TECHNICIAN" : "EMPLOYEE");
                return;
            }

            if ("reports".equals(type)) {
                loadReports(connection, response, user);
                return;
            }

            if ("tickets".equals(type)) {
                loadTickets(connection, response, user);
                return;
            }

            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            send(response, "{\"error\":\"Unknown data type.\"}");

        } catch (Exception e) {
            getServletContext().log("Portal data error", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            send(response, "{\"error\":\"Unable to load database records.\"}");
        }
    }

    private void loadUsers(Connection connection,
                           HttpServletResponse response,
                           String role) throws Exception {

        String sql =
            "SELECT USER_ID, FULL_NAME, EMAIL, DEPARTMENT, ROLE, IS_ACTIVE " +
            "FROM APP.USERS WHERE ROLE = ? ORDER BY FULL_NAME";

        StringBuilder json = new StringBuilder("{\"items\":[");
        boolean first = true;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, role);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if (!first) json.append(",");
                    first = false;

                    json.append("{")
                        .append("\"id\":").append(rs.getInt("USER_ID"))
                        .append(",\"name\":\"").append(esc(rs.getString("FULL_NAME"))).append("\"")
                        .append(",\"email\":\"").append(esc(rs.getString("EMAIL"))).append("\"")
                        .append(",\"department\":\"").append(esc(rs.getString("DEPARTMENT"))).append("\"")
                        .append(",\"role\":\"").append(esc(rs.getString("ROLE"))).append("\"")
                        .append(",\"active\":").append(rs.getBoolean("IS_ACTIVE"))
                        .append("}");
                }
            }
        }

        json.append("]}");
        send(response, json.toString());
    }

    private void loadTickets(Connection connection,
                             HttpServletResponse response,
                             User user) throws Exception {

        String role = user.getRole();

        String sql =
            "SELECT T.TICKET_ID, T.TITLE, T.CATEGORY, T.PRIORITY, " +
            "T.STATUS, T.CREATED_AT, U.FULL_NAME AS CREATOR " +
            "FROM APP.TICKETS T " +
            "LEFT JOIN APP.USERS U ON T.CREATED_BY = U.USER_ID ";

        if ("EMPLOYEE".equals(role)) {
            sql += "WHERE T.CREATED_BY = ? ";
        } else if ("TECHNICIAN".equals(role)) {
            sql += "WHERE T.ASSIGNED_TO = ? ";
        }

        sql += "ORDER BY T.CREATED_AT DESC";

        StringBuilder json = new StringBuilder("{\"items\":[");
        boolean first = true;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            if (!"ADMIN".equals(role)) {
                ps.setInt(1, user.getUserId());
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if (!first) json.append(",");
                    first = false;

                    json.append("{")
                        .append("\"id\":").append(rs.getInt("TICKET_ID"))
                        .append(",\"title\":\"").append(esc(rs.getString("TITLE"))).append("\"")
                        .append(",\"category\":\"").append(esc(rs.getString("CATEGORY"))).append("\"")
                        .append(",\"priority\":\"").append(esc(rs.getString("PRIORITY"))).append("\"")
                        .append(",\"status\":\"").append(esc(rs.getString("STATUS"))).append("\"")
                        .append(",\"creator\":\"").append(esc(rs.getString("CREATOR"))).append("\"")
                        .append("}");
                }
            }
        }

        json.append("]}");
        send(response, json.toString());
    }

    private void loadReports(Connection connection,
                             HttpServletResponse response,
                             User user) throws Exception {

        String role = user.getRole();

        String sql =
            "SELECT COUNT(*) AS TOTAL, " +
            "SUM(CASE WHEN STATUS='OPEN' THEN 1 ELSE 0 END) AS OPEN_COUNT, " +
            "SUM(CASE WHEN STATUS='ASSIGNED' THEN 1 ELSE 0 END) AS ASSIGNED_COUNT, " +
            "SUM(CASE WHEN STATUS='IN_PROGRESS' THEN 1 ELSE 0 END) AS PROGRESS_COUNT, " +
            "SUM(CASE WHEN STATUS='RESOLVED' THEN 1 ELSE 0 END) AS RESOLVED_COUNT, " +
            "SUM(CASE WHEN STATUS='CLOSED' THEN 1 ELSE 0 END) AS CLOSED_COUNT " +
            "FROM APP.TICKETS ";

        if ("EMPLOYEE".equals(role)) {
            sql += "WHERE CREATED_BY = ?";
        } else if ("TECHNICIAN".equals(role)) {
            sql += "WHERE ASSIGNED_TO = ?";
        }

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            if (!"ADMIN".equals(role)) {
                ps.setInt(1, user.getUserId());
            }

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();

                String json =
                    "{\"total\":" + rs.getInt("TOTAL") +
                    ",\"open\":" + rs.getInt("OPEN_COUNT") +
                    ",\"assigned\":" + rs.getInt("ASSIGNED_COUNT") +
                    ",\"inProgress\":" + rs.getInt("PROGRESS_COUNT") +
                    ",\"resolved\":" + rs.getInt("RESOLVED_COUNT") +
                    ",\"closed\":" + rs.getInt("CLOSED_COUNT") + "}";

                send(response, json);
            }
        }
    }
}
