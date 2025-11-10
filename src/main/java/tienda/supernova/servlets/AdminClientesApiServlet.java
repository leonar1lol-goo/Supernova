package tienda.supernova.servlets;

import tienda.supernova.db.DBConnection;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@WebServlet("/admin/api/clientes")
public class AdminClientesApiServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        jakarta.servlet.http.HttpSession s = req.getSession(false);
        String role = s != null ? (String) s.getAttribute("role") : null;
        if (role == null || !(role.equalsIgnoreCase("admin") || role.equalsIgnoreCase("supervisor"))) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            resp.setContentType("application/json;charset=UTF-8");
            resp.getWriter().print("{\"error\":\"forbidden\"}");
            return;
        }

        resp.setContentType("application/json;charset=UTF-8");
        String[] queries = new String[]{
            "SELECT id_cliente AS id, nombre AS nombre, direccion, email, telefono FROM cliente",
            "SELECT id_cliente AS id, nombre AS nombre, direccion, email, telefono FROM Cliente",
            "SELECT id_cliente AS id, nombre_cliente AS nombre, direccion, email, telefono FROM cliente",
            "SELECT id_cliente AS id, nombre_cliente AS nombre, direccion, email, telefono FROM Cliente",
            "SELECT id AS id, nombre AS nombre, direccion, email, telefono FROM clientes",
            "SELECT id AS id, nombre_cliente AS nombre, direccion, email, telefono FROM clientes"
        };

        StringBuilder sb = new StringBuilder();
        sb.append('[');
        boolean first = true;
        try (Connection conn = DBConnection.getConnection()) {
            boolean executed = false;
            for (String q : queries) {
                try (PreparedStatement ps = conn.prepareStatement(q); ResultSet rs = ps.executeQuery()) {
                    executed = true;
                    while (rs.next()) {
                        if (!first) sb.append(',');
                        first = false;
                        int id = rs.getInt("id");
                        String nombre = rs.getString("nombre");
                        String direccion = null;
                        try { direccion = rs.getString("direccion"); } catch (Exception exd) { direccion = null; }
                        String email = rs.getString("email");
                        String telefono = rs.getString("telefono");
                        sb.append('{');
                        sb.append("\"id\":").append(id).append(',');
                        sb.append("\"nombre\":\"").append(escape(nombre)).append("\",");
                        sb.append("\"direccion\":\"").append(escape(direccion)).append("\",");
                        sb.append("\"email\":\"").append(escape(email)).append("\",");
                        sb.append("\"telefono\":\"").append(escape(telefono)).append("\"");
                        sb.append('}');
                    }
                    break;
                } catch (SQLException ex) {
                    String msg = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
                    if (msg.contains("unknown table") || msg.contains("doesn't exist") || msg.contains("no such table")) {
                        continue;
                    }
                    throw ex;
                }
            }
            if (!first || executed) {
                sb.append(']');
                resp.getWriter().print(sb.toString());
                return;
            }
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().print("{\"error\":\"db\"}");
            return;
        }

        sb.append(']');
        resp.getWriter().print(sb.toString());
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","\\r");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        jakarta.servlet.http.HttpSession s = req.getSession(false);
        String role = s != null ? (String) s.getAttribute("role") : null;
        if (role == null || !(role.equalsIgnoreCase("admin") || role.equalsIgnoreCase("supervisor"))) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            resp.setContentType("application/json;charset=UTF-8");
            resp.getWriter().print("{\"error\":\"forbidden\"}");
            return;
        }

        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");
        String action = req.getParameter("action");
        if (action == null) {
            resp.getWriter().print("{\"ok\":false,\"error\":\"no action\"}");
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            if ("create".equalsIgnoreCase(action)) {
                String nombre = req.getParameter("nombre");
                String direccion = req.getParameter("direccion");
                String telefono = req.getParameter("telefono");
                String email = req.getParameter("email");
                String insert = "INSERT INTO cliente (nombre, direccion, telefono, email) VALUES (?,?,?,?)";
                try (PreparedStatement ps = conn.prepareStatement(insert, PreparedStatement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, nombre);
                    ps.setString(2, direccion);
                    ps.setString(3, telefono);
                    ps.setString(4, email);
                    int affected = ps.executeUpdate();
                    if (affected > 0) {
                        try (ResultSet gk = ps.getGeneratedKeys()) {
                            if (gk != null && gk.next()) {
                                int id = gk.getInt(1);
                                resp.getWriter().print("{\"ok\":true,\"id\":"+id+"}");
                                return;
                            }
                        }
                        resp.getWriter().print("{\"ok\":true}");
                        return;
                    }
                    resp.getWriter().print("{\"ok\":false,\"error\":\"no_insert\"}");
                    return;
                }
            } else if ("update".equalsIgnoreCase(action)) {
                String id = req.getParameter("id");
                String nombre = req.getParameter("nombre");
                String direccion = req.getParameter("direccion");
                String telefono = req.getParameter("telefono");
                String email = req.getParameter("email");
                String update = "UPDATE cliente SET nombre = ?, direccion = ?, telefono = ?, email = ? WHERE id_cliente = ? OR id = ?";
                try (PreparedStatement ps = conn.prepareStatement(update)) {
                    ps.setString(1, nombre);
                    ps.setString(2, direccion);
                    ps.setString(3, telefono);
                    ps.setString(4, email);
                    ps.setString(5, id);
                    ps.setString(6, id);
                    int affected = ps.executeUpdate();
                    if (affected > 0) {
                        resp.getWriter().print("{\"ok\":true}");
                        return;
                    }
                    resp.getWriter().print("{\"ok\":false,\"error\":\"not_found\"}");
                    return;
                }
            } else if ("delete".equalsIgnoreCase(action)) {
                String id = req.getParameter("id");
                String del = "DELETE FROM cliente WHERE id_cliente = ? OR id = ?";
                try (PreparedStatement ps = conn.prepareStatement(del)) {
                    ps.setString(1, id);
                    ps.setString(2, id);
                    int affected = ps.executeUpdate();
                    if (affected > 0) {
                        resp.getWriter().print("{\"ok\":true}");
                        return;
                    }
                    resp.getWriter().print("{\"ok\":false,\"error\":\"not_found\"}");
                    return;
                }
            } else {
                resp.getWriter().print("{\"ok\":false,\"error\":\"unknown action\"}");
                return;
            }
        } catch (SQLException ex) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().print("{\"ok\":false,\"error\":\"db\"}");
            return;
        }
    }
}
