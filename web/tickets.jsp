
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List" %>
<%@ page import="com.helpdesk.model.Ticket" %>
<%@ page import="com.helpdesk.model.User" %>

<%
    User currentUser = (User) session.getAttribute("user");

    if (currentUser == null) {
        response.sendRedirect(request.getContextPath() + "/login.html");
        return;
    }

    List<Ticket> tickets = (List<Ticket>) request.getAttribute("tickets");

    if (tickets == null) {
        tickets = java.util.Collections.emptyList();
    }
%>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>My Tickets | NexDesk</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/style.css">
    <link rel="stylesheet"
          href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.2/css/all.min.css">
</head>
<body>
<div class="app-shell">

    <aside class="sidebar" id="sidebar">
        <div class="brand"><i class="fa-solid fa-headset"></i>Nex<span>Desk</span></div>

        <div class="nav-section">WORKSPACE</div>
        <a class="nav-link" href="index.html"><i class="fa-solid fa-house"></i> Dashboard</a>
        <a class="nav-link active" href="tickets"><i class="fa-solid fa-ticket"></i> Tickets</a>
        <a class="nav-link" href="create-ticket.html"><i class="fa-solid fa-plus-circle"></i> Create Ticket</a>

        <% if ("ADMIN".equals(currentUser.getRole())) { %>
            <div class="nav-section">ADMINISTRATION</div>
            <a class="nav-link" href="employees.html"><i class="fa-solid fa-users"></i> Employees</a>
            <a class="nav-link" href="technicians.html"><i class="fa-solid fa-user-gear"></i> Technicians</a>
            <a class="nav-link" href="reports.html"><i class="fa-solid fa-chart-line"></i> Reports</a>
            <a class="nav-link" href="settings.html"><i class="fa-solid fa-gear"></i> Settings</a>
        <% } %>

        <div class="sidebar-footer">
            <strong><%= currentUser.getFullName() %></strong>
            <div><%= currentUser.getRole() %></div>
            <a class="nav-link" href="logout" style="margin-top:12px;">
                <i class="fa-solid fa-right-from-bracket"></i> Logout
            </a>
        </div>
    </aside>

    <main class="main">
        <header class="topbar">
            <button class="btn btn-secondary" id="menuToggle" type="button"
                    aria-label="Toggle navigation">
                <i class="fa-solid fa-bars"></i>
            </button>

            <div class="search-box">
                <input type="search" id="ticketSearch"
                       placeholder="Search tickets..." aria-label="Search tickets">
            </div>

            <a class="btn btn-primary" href="create-ticket.html">
                <i class="fa-solid fa-plus"></i> New Ticket
            </a>
        </header>

        <section class="page-content">
            <div style="margin-bottom:24px;">
                <h1 style="font-size:30px;font-weight:800;">Tickets</h1>
                <p style="color:var(--muted);">
                    View and track your support requests.
                </p>
            </div>

            <% if ("1".equals(request.getParameter("created"))) { %>
                <div style="padding:14px 18px;margin-bottom:20px;border-radius:12px;
                            background:#d1fae5;color:#047857;">
                    <i class="fa-solid fa-circle-check"></i>
                    Ticket created successfully!
                </div>
            <% } %>

            <div class="panel">
                <div class="panel-head">
                    <div>
                        <h2>All Accessible Tickets</h2>
                        <div class="panel-subtitle">
                            Total tickets: <%= tickets.size() %>
                        </div>
                    </div>
                    <span class="badge badge-open"><%= tickets.size() %> records</span>
                </div>

                <div class="table-wrap">
                    <table id="ticketTable">
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>Title</th>
                                <th>Category</th>
                                <th>Priority</th>
                                <th>Status</th>
                                <th>Created</th>
                            </tr>
                        </thead>
                        <tbody>
                        <% if (tickets.isEmpty()) { %>
                            <tr>
                                <td colspan="6" style="text-align:center;padding:35px;">
                                    <i class="fa-solid fa-inbox"
                                       style="font-size:28px;color:var(--muted);"></i>
                                    <p style="margin-top:10px;">No tickets found.</p>
                                </td>
                            </tr>
                        <% } else {
                            for (Ticket ticket : tickets) {
                                String status = ticket.getStatus() == null
                                        ? "" : ticket.getStatus();
                                String priority = ticket.getPriority() == null
                                        ? "" : ticket.getPriority();

                                String statusClass = "badge-open";
                                if ("RESOLVED".equalsIgnoreCase(status)
                                        || "CLOSED".equalsIgnoreCase(status)) {
                                    statusClass = "badge-resolved";
                                } else if ("IN_PROGRESS".equalsIgnoreCase(status)
                                        || "ASSIGNED".equalsIgnoreCase(status)) {
                                    statusClass = "badge-progress";
                                }

                                String priorityClass = "badge-low";
                                if ("CRITICAL".equalsIgnoreCase(priority)) {
                                    priorityClass = "badge-critical";
                                } else if ("HIGH".equalsIgnoreCase(priority)) {
                                    priorityClass = "badge-high";
                                } else if ("MEDIUM".equalsIgnoreCase(priority)) {
                                    priorityClass = "badge-medium";
                                }
                        %>
                            <tr>
                                <td>#<%= ticket.getTicketId() %></td>
                                <td>
                                    <strong><%= ticket.getTitle() %></strong>
                                </td>
                                <td><%= ticket.getCategory() %></td>
                                <td>
                                    <span class="badge <%= priorityClass %>">
                                        <%= priority %>
                                    </span>
                                </td>
                                <td>
                                    <span class="badge <%= statusClass %>">
                                        <%= status.replace('_', ' ') %>
                                    </span>
                                </td>
                                <td>
                                    <%= ticket.getCreatedAt() == null
                                            ? "—" : ticket.getCreatedAt() %>
                                </td>
                            </tr>
                        <%  }
                           } %>
                        </tbody>
                    </table>
                </div>
            </div>
        </section>
    </main>
</div>

<script>
document.addEventListener("DOMContentLoaded", function () {
    const search = document.getElementById("ticketSearch");
    const rows = document.querySelectorAll("#ticketTable tbody tr");

    if (search) {
        search.addEventListener("input", function () {
            const query = search.value.toLowerCase().trim();

            rows.forEach(function (row) {
                row.style.display =
                    row.textContent.toLowerCase().includes(query) ? "" : "none";
            });
        });
    }

    const menuToggle = document.getElementById("menuToggle");
    const sidebar = document.getElementById("sidebar");

    if (menuToggle && sidebar) {
        menuToggle.addEventListener("click", function () {
            sidebar.classList.toggle("sidebar-open");
        });
    }
});
</script>
</body>
</html>
