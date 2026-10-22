package com.helpdesk.dao;

import com.helpdesk.model.TicketHistory;
import com.helpdesk.util.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class TicketHistoryDAO {

    public boolean addHistory(TicketHistory history)
            throws SQLException {

        String sql = "INSERT INTO TICKET_HISTORY "
                + "(TICKET_ID, CHANGED_BY, OLD_STATUS, NEW_STATUS, "
                + "ACTION_DESCRIPTION) VALUES (?, ?, ?, ?, ?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, history.getTicketId());
            ps.setInt(2, history.getChangedBy());
            ps.setString(3, history.getOldStatus());
            ps.setString(4, history.getNewStatus());
            ps.setString(5, history.getActionDescription());

            return ps.executeUpdate() == 1;
        }
    }
}