
package com.helpdesk.dao;

import com.helpdesk.model.User;
import com.helpdesk.util.DBConnection;
import com.helpdesk.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDAO {

    // PBKDF2 login: retrieve user first, then verify password.
    public User login(String email, String password)
            throws SQLException {

        String sql = "SELECT USER_ID, FULL_NAME, EMAIL, PASSWORD_HASH, "
                + "ROLE, DEPARTMENT, IS_ACTIVE "
                + "FROM USERS WHERE EMAIL = ? AND IS_ACTIVE = TRUE";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, email.trim().toLowerCase());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User user = mapUser(rs);

                    if (PasswordUtil.verifyPassword(
                            password, user.getPasswordHash())) {
                        return user;
                    }
                }
            }
        }

        return null;
    }

    public boolean emailExists(String email) throws SQLException {
        String sql = "SELECT USER_ID FROM USERS WHERE EMAIL = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, email.trim().toLowerCase());

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean createUser(User user) throws SQLException {
        String sql = "INSERT INTO USERS "
                + "(FULL_NAME, EMAIL, PASSWORD_HASH, ROLE, DEPARTMENT, IS_ACTIVE) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, user.getFullName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPasswordHash());
            ps.setString(4, user.getRole());
            ps.setString(5, user.getDepartment());
            ps.setBoolean(6, user.isActive());

            return ps.executeUpdate() == 1;
        }
    }

    public User findById(int userId) throws SQLException {
        String sql = "SELECT USER_ID, FULL_NAME, EMAIL, PASSWORD_HASH, "
                + "ROLE, DEPARTMENT, IS_ACTIVE "
                + "FROM USERS WHERE USER_ID = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
            }
        }

        return null;
    }

    private User mapUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getInt("USER_ID"));
        user.setFullName(rs.getString("FULL_NAME"));
        user.setEmail(rs.getString("EMAIL"));
        user.setPasswordHash(rs.getString("PASSWORD_HASH"));
        user.setRole(rs.getString("ROLE"));
        user.setDepartment(rs.getString("DEPARTMENT"));
        user.setActive(rs.getBoolean("IS_ACTIVE"));
        return user;
    }
}
