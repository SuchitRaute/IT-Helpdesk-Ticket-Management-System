
package com.helpdesk.servlet;

import com.helpdesk.dao.UserDAO;
import com.helpdesk.model.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        String email = request.getParameter("email");
        String password = request.getParameter("password");

        if (email == null || password == null
                || email.trim().isEmpty() || password.isEmpty()) {

            response.sendRedirect(
                    request.getContextPath()
                    + "/login.html?error=missing");
            return;
        }

        try {
            UserDAO userDAO = new UserDAO();

            // UserDAO verifies the PBKDF2 password.
            User user = userDAO.login(
                    email.trim().toLowerCase(), password);

            if (user == null) {
                response.sendRedirect(
                        request.getContextPath()
                        + "/login.html?error=invalid");
                return;
            }

            // Prevent session fixation.
            HttpSession oldSession = request.getSession(false);

            if (oldSession != null) {
                oldSession.invalidate();
            }

            HttpSession session = request.getSession(true);
            session.setAttribute("user", user);
            session.setAttribute("userId", user.getUserId());
            session.setAttribute("userRole", user.getRole());
            session.setMaxInactiveInterval(1800);

            response.sendRedirect(
                    request.getContextPath() + "/tickets");

        } catch (SQLException ex) {
            throw new ServletException("Login failed.", ex);
        }
    }
}
