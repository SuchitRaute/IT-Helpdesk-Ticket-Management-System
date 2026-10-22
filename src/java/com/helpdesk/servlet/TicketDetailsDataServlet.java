package com.helpdesk.servlet;

import com.helpdesk.model.User;
import com.helpdesk.util.DBConnection;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.sql.*;

@WebServlet("/ticket-details-data")
public class TicketDetailsDataServlet extends HttpServlet {

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String q(String s) {
        return "\"" + esc(s) + "\"";
    }

    private String ts(Timestamp t) {
        return t == null ? "" : t.toString();
    }

    private void send(HttpServletResponse r, int status, String body)
            throws IOException {
        r.setStatus(status);
        r.setContentType("application/json;charset=UTF-8");
        r.setHeader("Cache-Control", "no-store");
        r.getWriter().write(body);
    }

    private User current(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        Object o = s == null ? null : s.getAttribute("user");
        return o instanceof User ? (User) o : null;
    }

    /*
     * Checks whether a column exists before using it.
     * This supports older versions of the database schema.
     */
    private boolean hasColumn(Connection con, String tableName,
                              String columnName) throws SQLException {

        DatabaseMetaData meta = con.getMetaData();

        try (ResultSet rs = meta.getColumns(
                null,
                "APP",
                tableName.toUpperCase(),
                columnName.toUpperCase())) {

            return rs.next();
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        User user = current(req);

        if (user == null) {
            send(resp, 401,
                    "{\"error\":\"Please log in again.\"}");
            return;
        }

        int id;

        try {
            id = Integer.parseInt(req.getParameter("id"));

            if (id <= 0) {
                throw new NumberFormatException();
            }

        } catch (Exception ex) {
            send(resp, 400,
                    "{\"error\":\"Missing or invalid ticket id.\"}");
            return;
        }

        try (Connection c = DBConnection.getConnection()) {

            /*
             * Handle databases where USERS.DEPARTMENT is missing.
             */
            boolean hasCreatorDepartment =
                    hasColumn(c, "USERS", "DEPARTMENT");

            String creatorDepartmentSql = hasCreatorDepartment
                    ? "C.DEPARTMENT AS CREATOR_DEPARTMENT"
                    : "CAST(NULL AS VARCHAR(100)) AS CREATOR_DEPARTMENT";

            String ticketSql =
                    "SELECT T.*, C.FULL_NAME AS CREATOR, "
                    + creatorDepartmentSql + ", "
                    + "A.FULL_NAME AS ASSIGNEE "
                    + "FROM TICKETS T "
                    + "JOIN USERS C ON C.USER_ID = T.CREATED_BY "
                    + "LEFT JOIN USERS A ON A.USER_ID = T.ASSIGNED_TO "
                    + "WHERE T.TICKET_ID = ?";

            StringBuilder json = new StringBuilder();

            try (PreparedStatement ps = c.prepareStatement(ticketSql)) {

                ps.setInt(1, id);

                try (ResultSet rs = ps.executeQuery()) {

                    if (!rs.next()) {
                        send(resp, 404,
                                "{\"error\":\"Ticket not found.\"}");
                        return;
                    }

                    int creatorId = rs.getInt("CREATED_BY");

                    int assigned = rs.getInt("ASSIGNED_TO");
                    Integer assignedId =
                            rs.wasNull() ? null : assigned;

                    boolean allowed =
                            "ADMIN".equals(user.getRole())
                            || creatorId == user.getUserId()
                            || ("TECHNICIAN".equals(user.getRole())
                                && assignedId != null
                                && assignedId == user.getUserId());

                    if (!allowed) {
                        send(resp, 403,
                                "{\"error\":\"You do not have access to this ticket.\"}");
                        return;
                    }

                    json.append("{\"role\":")
                            .append(q(user.getRole()))
                            .append(",\"ticket\":{")

                            .append("\"id\":")
                            .append(rs.getInt("TICKET_ID"))

                            .append(",\"title\":")
                            .append(q(rs.getString("TITLE")))

                            .append(",\"description\":")
                            .append(q(rs.getString("DESCRIPTION")))

                            .append(",\"category\":")
                            .append(q(rs.getString("CATEGORY")))

                            .append(",\"priority\":")
                            .append(q(rs.getString("PRIORITY")))

                            .append(",\"status\":")
                            .append(q(rs.getString("STATUS")))

                            .append(",\"creator\":")
                            .append(q(rs.getString("CREATOR")))

                            .append(",\"department\":")
                            .append(q(rs.getString("CREATOR_DEPARTMENT")))

                            .append(",\"assignedTo\":")
                            .append(assignedId == null
                                    ? "null" : assignedId)

                            .append(",\"assignedName\":")
                            .append(q(rs.getString("ASSIGNEE")))

                            .append(",\"createdAt\":")
                            .append(q(ts(rs.getTimestamp("CREATED_AT"))))

                            .append(",\"updatedAt\":")
                            .append(q(ts(rs.getTimestamp("UPDATED_AT"))))

                            .append(",\"resolvedAt\":")
                            .append(q(ts(rs.getTimestamp("RESOLVED_AT"))))

                            .append("},\"comments\":[");
                }
            }

            /*
             * Load saved ticket comments.
             */
            boolean first = true;

            String commentsSql =
                    "SELECT C.COMMENT_TEXT, C.CREATED_AT, "
                    + "U.FULL_NAME, U.ROLE "
                    + "FROM TICKET_COMMENTS C "
                    + "JOIN USERS U ON U.USER_ID = C.USER_ID "
                    + "WHERE C.TICKET_ID = ? "
                    + "ORDER BY C.CREATED_AT";

            try (PreparedStatement ps =
                    c.prepareStatement(commentsSql)) {

                ps.setInt(1, id);

                try (ResultSet rs = ps.executeQuery()) {

                    while (rs.next()) {

                        if (!first) {
                            json.append(",");
                        }

                        first = false;

                        json.append("{\"text\":")
                                .append(q(rs.getString("COMMENT_TEXT")))

                                .append(",\"name\":")
                                .append(q(rs.getString("FULL_NAME")))

                                .append(",\"role\":")
                                .append(q(rs.getString("ROLE")))

                                .append(",\"at\":")
                                .append(q(ts(
                                        rs.getTimestamp("CREATED_AT"))))

                                .append("}");
                    }
                }
            }

            /*
             * Load ticket history.
             * ACTION_DESCRIPTION is optional for older databases.
             */
            json.append("],\"history\":[");
            first = true;

            boolean hasActionDescription =
                    hasColumn(c, "TICKET_HISTORY",
                            "ACTION_DESCRIPTION");

            String descriptionSql = hasActionDescription
                    ? "H.ACTION_DESCRIPTION"
                    : "CAST('' AS VARCHAR(1000))";

          String historySql =
        "SELECT H.OLD_STATUS, H.NEW_STATUS, "
        + descriptionSql + " AS ACTION_DESCRIPTION, "
        + "U.FULL_NAME "
        + "FROM TICKET_HISTORY H "
        + "JOIN USERS U ON U.USER_ID = H.CHANGED_BY "
        + "WHERE H.TICKET_ID = ?";

try (PreparedStatement ps = c.prepareStatement(historySql)) {
    ps.setInt(1, id);

    try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
            if (!first) {
                json.append(",");
            }
            first = false;

            json.append("{\"oldStatus\":")
                    .append(q(rs.getString("OLD_STATUS")))
                    .append(",\"newStatus\":")
                    .append(q(rs.getString("NEW_STATUS")))
                    .append(",\"description\":")
                    .append(q(rs.getString("ACTION_DESCRIPTION")))
                    .append(",\"by\":")
                    .append(q(rs.getString("FULL_NAME")))
                    .append(",\"at\":\"\"")
                    .append("}");
        }
    }
}

            /*
             * Load active technicians.
             * DEPARTMENT is optional for older databases.
             */
            json.append("],\"technicians\":[");
            first = true;

            boolean hasTechnicianDepartment =
                    hasColumn(c, "USERS", "DEPARTMENT");

            String technicianDepartmentSql =
                    hasTechnicianDepartment
                    ? "DEPARTMENT"
                    : "CAST(NULL AS VARCHAR(100)) AS DEPARTMENT";

            String techniciansSql =
                    "SELECT USER_ID, FULL_NAME, "
                    + technicianDepartmentSql
                    + " FROM USERS "
                    + "WHERE ROLE = 'TECHNICIAN' "
                    + "AND IS_ACTIVE = TRUE "
                    + "ORDER BY FULL_NAME";

            try (PreparedStatement ps =
                    c.prepareStatement(techniciansSql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    if (!first) {
                        json.append(",");
                    }

                    first = false;

                    json.append("{\"id\":")
                            .append(rs.getInt("USER_ID"))

                            .append(",\"name\":")
                            .append(q(rs.getString("FULL_NAME")))

                            .append(",\"department\":")
                            .append(q(rs.getString("DEPARTMENT")))

                            .append("}");
                }
            }

            json.append("]}");

            send(resp, 200, json.toString());

        } catch (SQLException ex) {

    getServletContext().log("Ticket details database error", ex);

    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    resp.setContentType("application/json;charset=UTF-8");

    resp.getWriter().write(
        "{\"error\":" + q(ex.getMessage()) + "}"
    );
}
    }
}