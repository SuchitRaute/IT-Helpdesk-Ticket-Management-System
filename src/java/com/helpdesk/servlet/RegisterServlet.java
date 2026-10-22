
package com.helpdesk.servlet;

import com.helpdesk.dao.UserDAO;
import com.helpdesk.model.User;
import com.helpdesk.util.PasswordUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/register")
public class RegisterServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        String fullName = request.getParameter("fullName");
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        String department = request.getParameter("department");

        if (fullName == null || email == null
                || password == null || department == null
                || fullName.trim().isEmpty()
                || email.trim().isEmpty()
                || password.length() < 8
                || department.trim().isEmpty()) {

            response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Complete all fields. Password must contain at least 8 characters."
            );
            return;
        }

        email = email.trim().toLowerCase();

        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Enter a valid email address."
            );
            return;
        }

        User user = new User();
        user.setFullName(fullName.trim());
        user.setEmail(email);
        user.setPasswordHash(
                PasswordUtil.hashPassword(password)
        );

        // Public registration can create EMPLOYEE accounts only.
        user.setRole("EMPLOYEE");
        user.setDepartment(department.trim());
        user.setActive(true);

        try {
            UserDAO dao = new UserDAO();

            if (dao.emailExists(email)) {
                response.sendRedirect(
                        request.getContextPath()
                        + "/login.html?error=email-exists"
                );
                return;
            }

            if (dao.createUser(user)) {
                response.sendRedirect(
                        request.getContextPath()
                        + "/login.html?registered=1"
                );
            } else {
                response.sendError(
                        HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Registration failed."
                );
            }

        } catch (SQLException ex) {
            throw new ServletException(
                    "Unable to register the user.", ex
            );
        }
    }
}
