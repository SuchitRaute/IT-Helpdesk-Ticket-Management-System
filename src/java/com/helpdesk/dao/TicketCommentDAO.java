package com.helpdesk.dao;

import com.helpdesk.model.TicketComment;
import com.helpdesk.util.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TicketCommentDAO {

    public boolean addComment(TicketComment comment)
            throws SQLException {

        String sql = "INSERT INTO TICKET_COMMENTS "
                + "(TICKET_ID, USER_ID, COMMENT_TEXT) VALUES (?, ?, ?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, comment.getTicketId());
            ps.setInt(2, comment.getUserId());
            ps.setString(3, comment.getCommentText());

            return ps.executeUpdate() == 1;
        }
    }

    public List<TicketComment> getCommentsByTicket(int ticketId)
            throws SQLException {

        List<TicketComment> comments = new ArrayList<>();

        String sql = "SELECT COMMENT_ID, TICKET_ID, USER_ID, "
                + "COMMENT_TEXT, CREATED_AT FROM TICKET_COMMENTS "
                + "WHERE TICKET_ID = ? ORDER BY CREATED_AT ASC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, ticketId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TicketComment comment = new TicketComment();

                    comment.setCommentId(rs.getInt("COMMENT_ID"));
                    comment.setTicketId(rs.getInt("TICKET_ID"));
                    comment.setUserId(rs.getInt("USER_ID"));
                    comment.setCommentText(rs.getString("COMMENT_TEXT"));
                    comment.setCreatedAt(rs.getTimestamp("CREATED_AT"));

                    comments.add(comment);
                }
            }
        }

        return comments;
    }
}