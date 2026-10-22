package com.helpdesk.servlet;

import com.helpdesk.model.User;
import com.helpdesk.util.DBConnection;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.*;

@WebServlet("/ticket-comment")
public class TicketCommentServlet extends HttpServlet {
    private void send(HttpServletResponse r,int status,String message)throws IOException{
        r.setStatus(status);r.setContentType("application/json;charset=UTF-8");
        String safe=message.replace("\\","\\\\").replace("\"","\\\"").replace("\n"," ");
        r.getWriter().write("{\"message\":\""+safe+"\"}");
    }
    private User current(HttpServletRequest req){
        HttpSession s=req.getSession(false);Object o=s==null?null:s.getAttribute("user");
        return o instanceof User?(User)o:null;
    }
    @Override protected void doPost(HttpServletRequest req,HttpServletResponse resp)throws IOException{
        User user=current(req);if(user==null){send(resp,401,"Please log in again.");return;}
        int id;try{id=Integer.parseInt(req.getParameter("ticketId"));}catch(Exception e){send(resp,400,"Invalid ticket ID.");return;}
        String text=req.getParameter("comment");
        if(text==null||text.trim().length()<2||text.trim().length()>3000){send(resp,400,"Comment must contain 2 to 3000 characters.");return;}
        try(Connection c=DBConnection.getConnection()){
            boolean allowed=false;
            try(PreparedStatement ps=c.prepareStatement("SELECT CREATED_BY,ASSIGNED_TO FROM TICKETS WHERE TICKET_ID=?")){
                ps.setInt(1,id);try(ResultSet rs=ps.executeQuery()){
                    if(!rs.next()){send(resp,404,"Ticket not found.");return;}
                    int creator=rs.getInt("CREATED_BY");int assignedInt=rs.getInt("ASSIGNED_TO");Integer assigned=rs.wasNull()?null:assignedInt;
                    allowed="ADMIN".equals(user.getRole())||creator==user.getUserId()||(assigned!=null&&assigned.intValue()==user.getUserId());
                }
            }
            if(!allowed){send(resp,403,"You do not have access to this ticket.");return;}
            try(PreparedStatement ps=c.prepareStatement("INSERT INTO TICKET_COMMENTS (TICKET_ID,USER_ID,COMMENT_TEXT) VALUES (?,?,?)")){
                ps.setInt(1,id);ps.setInt(2,user.getUserId());ps.setString(3,text.trim());ps.executeUpdate();
            }
            try(PreparedStatement ps=c.prepareStatement("INSERT INTO TICKET_HISTORY (TICKET_ID,CHANGED_BY,OLD_STATUS,NEW_STATUS,ACTION_DESCRIPTION) "
                    +"SELECT TICKET_ID,?,STATUS,STATUS,? FROM TICKETS WHERE TICKET_ID=?")){
                ps.setInt(1,user.getUserId());ps.setString(2,user.getFullName()+" added a comment.");ps.setInt(3,id);ps.executeUpdate();
            }
            send(resp,200,"Comment saved.");
        }catch(SQLException e){getServletContext().log("Saving ticket comment failed",e);send(resp,500,"Unable to save comment. Check the server log.");}
    }
}
