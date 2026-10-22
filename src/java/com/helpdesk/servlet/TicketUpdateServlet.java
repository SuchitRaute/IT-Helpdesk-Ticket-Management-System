package com.helpdesk.servlet;

import com.helpdesk.model.User;
import com.helpdesk.util.DBConnection;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.sql.*;
import java.util.Arrays;
import java.util.List;

@WebServlet("/ticket-update")
public class TicketUpdateServlet extends HttpServlet {
    private static final List<String> STATUSES = Arrays.asList(
            "OPEN","ASSIGNED","IN_PROGRESS","ON_HOLD","RESOLVED","CLOSED");
    private static final List<String> PRIORITIES = Arrays.asList(
            "LOW","MEDIUM","HIGH","CRITICAL");
    private void json(HttpServletResponse r, int code, String message) throws IOException {
        r.setStatus(code); r.setContentType("application/json;charset=UTF-8");
        r.getWriter().write("{\"message\":\"" + message.replace("\\","\\\\").replace("\"","\\\"") + "\"}");
    }
    private User current(HttpServletRequest req) {
        HttpSession s=req.getSession(false); Object o=s==null?null:s.getAttribute("user");
        return o instanceof User?(User)o:null;
    }
    @Override protected void doPost(HttpServletRequest req,HttpServletResponse resp)
            throws ServletException,IOException {
        User user=current(req);
        if(user==null){json(resp,401,"Please log in again.");return;}
        int id;
        try{id=Integer.parseInt(req.getParameter("ticketId"));}
        catch(Exception e){json(resp,400,"Invalid ticket ID.");return;}
        String status=req.getParameter("status");
        String priority=req.getParameter("priority");
        String assigneeRaw=req.getParameter("technicianId");
        String resolutionNote=req.getParameter("resolutionNote");
        if(status==null || !STATUSES.contains(status)){json(resp,400,"Invalid status.");return;}
        if("ADMIN".equals(user.getRole()) && (priority==null || !PRIORITIES.contains(priority))){
            json(resp,400,"Invalid priority.");return;
        }
        if(!"ADMIN".equals(user.getRole()) && !"TECHNICIAN".equals(user.getRole())){
            json(resp,403,"Only administrators and assigned technicians can update ticket status.");return;
        }
        if("TECHNICIAN".equals(user.getRole()) && ("OPEN".equals(status)||"ASSIGNED".equals(status)||"CLOSED".equals(status))){
            json(resp,403,"Technicians can move their assigned ticket to In Progress, On Hold, or Resolved.");return;
        }
        try(Connection c=DBConnection.getConnection()){
            c.setAutoCommit(false);
            try{
                String oldStatus; String oldPriority; int creator; int oldAssignedInt; Integer oldAssigned;
                try(PreparedStatement ps=c.prepareStatement("SELECT STATUS,PRIORITY,CREATED_BY,ASSIGNED_TO FROM TICKETS WHERE TICKET_ID=?")){
                    ps.setInt(1,id);
                    try(ResultSet rs=ps.executeQuery()){
                        if(!rs.next()){c.rollback();json(resp,404,"Ticket not found.");return;}
                        oldStatus=rs.getString("STATUS"); oldPriority=rs.getString("PRIORITY"); creator=rs.getInt("CREATED_BY");
                        oldAssignedInt=rs.getInt("ASSIGNED_TO"); oldAssigned=rs.wasNull()?null:oldAssignedInt;
                    }
                }
                if("TECHNICIAN".equals(user.getRole()) && (oldAssigned==null || oldAssigned.intValue()!=user.getUserId())){
                    c.rollback();json(resp,403,"You can update only tickets assigned to you.");return;
                }
                if("RESOLVED".equals(status) && !"RESOLVED".equals(oldStatus)
                        && (resolutionNote==null || resolutionNote.trim().length()<3)){
                    c.rollback();json(resp,400,"Add a resolution note explaining the work completed before marking the ticket resolved.");return;
                }
                Integer newAssigned=oldAssigned;
                if("ADMIN".equals(user.getRole())){
                    if(assigneeRaw==null || assigneeRaw.trim().isEmpty()) newAssigned=null;
                    else {
                        try{newAssigned=Integer.valueOf(assigneeRaw);}
                        catch(NumberFormatException e){c.rollback();json(resp,400,"Invalid technician.");return;}
                        try(PreparedStatement ps=c.prepareStatement("SELECT USER_ID FROM USERS WHERE USER_ID=? AND ROLE='TECHNICIAN' AND IS_ACTIVE=TRUE")){
                            ps.setInt(1,newAssigned);
                            try(ResultSet rs=ps.executeQuery()){if(!rs.next()){c.rollback();json(resp,400,"Select an active technician.");return;}}
                        }
                    }
                }
                String newPriority="ADMIN".equals(user.getRole())?priority:oldPriority;
                if(!"ADMIN".equals(user.getRole()) && !status.equals(oldStatus) && "CLOSED".equals(oldStatus)){
                    c.rollback();json(resp,409,"This ticket is closed. Ask an administrator to reopen it.");return;
                }
                String sql="UPDATE TICKETS SET STATUS=?, PRIORITY=?, ASSIGNED_TO=?, UPDATED_AT=CURRENT_TIMESTAMP, "
                        +"RESOLVED_AT=CASE WHEN ?='RESOLVED' THEN COALESCE(RESOLVED_AT,CURRENT_TIMESTAMP) "
                        +"WHEN ? IN ('OPEN','ASSIGNED','IN_PROGRESS','ON_HOLD') THEN NULL ELSE RESOLVED_AT END WHERE TICKET_ID=?";
                try(PreparedStatement ps=c.prepareStatement(sql)){
                    ps.setString(1,status);ps.setString(2,newPriority);
                    if(newAssigned==null)ps.setNull(3,Types.INTEGER);else ps.setInt(3,newAssigned);
                    ps.setString(4,status);ps.setString(5,status);ps.setInt(6,id);ps.executeUpdate();
                }
                if(!oldStatus.equals(status)){
                    insertHistory(c,id,user.getUserId(),oldStatus,status,
                            "Status changed from "+oldStatus+" to "+status+" by "+user.getFullName());
                }
                if(!oldPriority.equals(newPriority)){
                    insertHistory(c,id,user.getUserId(),oldStatus,status,
                            "Priority changed from "+oldPriority+" to "+newPriority);
                }
                if(oldAssigned==null ? newAssigned!=null : !oldAssigned.equals(newAssigned)){
                    String techName="Unassigned";
                    if(newAssigned!=null){
                        try(PreparedStatement ps=c.prepareStatement("SELECT FULL_NAME FROM USERS WHERE USER_ID=?")){
                            ps.setInt(1,newAssigned);try(ResultSet rs=ps.executeQuery()){if(rs.next())techName=rs.getString(1);}
                        }
                    }
                    insertHistory(c,id,user.getUserId(),oldStatus,status,"Technician assignment changed to "+techName);
                }
                if("RESOLVED".equals(status) && !"RESOLVED".equals(oldStatus)){
                    try(PreparedStatement ps=c.prepareStatement("INSERT INTO TICKET_COMMENTS (TICKET_ID,USER_ID,COMMENT_TEXT) VALUES (?,?,?)")){
                        ps.setInt(1,id);ps.setInt(2,user.getUserId());
                        ps.setString(3,"Resolution report: "+resolutionNote.trim());ps.executeUpdate();
                    }
                    insertHistory(c,id,user.getUserId(),oldStatus,status,"Resolution reported by "+user.getFullName()+": "+resolutionNote.trim());
                }
                c.commit();
                json(resp,200,"Ticket changes saved to the database.");
            }catch(SQLException e){c.rollback();throw e;}
            finally{c.setAutoCommit(true);}
        }catch(SQLException e){
            getServletContext().log("Ticket update failed",e);
            json(resp,500,"Database update failed. Check the GlassFish server log.");
        }
    }
    private void insertHistory(Connection c,int ticketId,int userId,String oldStatus,String newStatus,String description)
            throws SQLException{
        try(PreparedStatement ps=c.prepareStatement("INSERT INTO TICKET_HISTORY "
                +"(TICKET_ID,CHANGED_BY,OLD_STATUS,NEW_STATUS,ACTION_DESCRIPTION) VALUES (?,?,?,?,?)")){
            ps.setInt(1,ticketId);ps.setInt(2,userId);ps.setString(3,oldStatus);ps.setString(4,newStatus);ps.setString(5,description);ps.executeUpdate();
        }
    }
}
