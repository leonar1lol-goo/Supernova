package tienda.supernova.servlets;

import tienda.supernova.db.DBConnection;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@WebServlet(name = "AccountServlet", urlPatterns = {"/admin/api/account"})
public class AccountServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("username") == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"ok\":false,\"error\":\"not_logged_in\"}");
            return;
        }


        String action = req.getParameter("action");
        if (action == null || !action.equals("changePassword")) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"ok\":false,\"error\":\"invalid_action\"}");
            return;
        }

        String current = req.getParameter("currentPassword");
        String nw = req.getParameter("newPassword");
        if (current == null || nw == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"ok\":false,\"error\":\"missing_params\"}");
            return;
        }

        if (nw.length() < 8 || !nw.matches(".*[A-Z].*") || !nw.matches(".*[a-z].*") || !nw.matches(".*[0-9].*") || nw.matches("[A-Za-z0-9]*")) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"ok\":false,\"error\":\"weak_password\",\"message\":\"La nueva contraseña no cumple los requisitos\"}");
            return;
        }

        String username = (String) session.getAttribute("username");

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();

            String select1 = "SELECT id_usuario, nombre_usuario, `contraseña` FROM Usuario WHERE nombre_usuario = ? OR email = ? LIMIT 1";
            try (PreparedStatement ps = conn.prepareStatement(select1)) {
                ps.setString(1, username);
                ps.setString(2, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String stored = rs.getString(3);
                        if (stored == null || !stored.equals(current)) {
                            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                            out.print("{\"ok\":false,\"error\":\"wrong_current\",\"message\":\"Contraseña actual incorrecta\"}");
                            return;
                        }
                        String upd = "UPDATE Usuario SET `contraseña` = ? WHERE id_usuario = ?";
                        try (PreparedStatement u = conn.prepareStatement(upd)) {
                            u.setString(1, nw);
                            u.setInt(2, rs.getInt(1));
                            int updated = u.executeUpdate();
                            if (updated > 0) {
                                out.print("{\"ok\":true}");
                                return;
                            } else {
                                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                                out.print("{\"ok\":false,\"error\":\"db_update\"}");
                                return;
                            }
                        }
                    }
                }
            } catch (SQLException ex) {
            }

            String select2 = "SELECT id, nombre, password FROM usuarios WHERE nombre = ? OR email = ? LIMIT 1";
            try (PreparedStatement ps2 = conn.prepareStatement(select2)) {
                ps2.setString(1, username);
                ps2.setString(2, username);
                try (ResultSet rs2 = ps2.executeQuery()) {
                    if (rs2.next()) {
                        String stored2 = rs2.getString(3);
                        if (stored2 == null || !stored2.equals(current)) {
                            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                            out.print("{\"ok\":false,\"error\":\"wrong_current\",\"message\":\"Contraseña actual incorrecta\"}");
                            return;
                        }
                        String upd2 = "UPDATE usuarios SET password = ? WHERE id = ?";
                        try (PreparedStatement u2 = conn.prepareStatement(upd2)) {
                            u2.setString(1, nw);
                            u2.setInt(2, rs2.getInt(1));
                            int updated2 = u2.executeUpdate();
                            if (updated2 > 0) {
                                out.print("{\"ok\":true}");
                                return;
                            } else {
                                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                                out.print("{\"ok\":false,\"error\":\"db_update\"}");
                                return;
                            }
                        }
                    }
                }
            }

            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"ok\":false,\"error\":\"user_not_found\"}");

        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"ok\":false,\"error\":\"db_error\",\"message\":\"" + e.getMessage().replaceAll("\"","\\\"") + "\"}");
        } finally {
            try { if (conn != null) conn.close(); } catch (SQLException ignored) {}
        }
    }
}
