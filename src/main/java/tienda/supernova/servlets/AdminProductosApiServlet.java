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

@WebServlet("/admin/api/productos")
public class AdminProductosApiServlet extends HttpServlet {

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
            "SELECT id_producto AS id, nombre AS nombre, codigo_barra, categoria, stock FROM producto",
            "SELECT id_producto AS id, nombre AS nombre, codigo_barra, categoria, stock FROM Producto",
            "SELECT id AS id, nombre AS nombre, codigo_barra, categoria, stock FROM productos",
            "SELECT id AS id, nombre_producto AS nombre, codigo_barra, categoria, stock FROM productos"
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
                        String codigo = null;
                        try { codigo = rs.getString("codigo_barra"); } catch (Exception ex) { codigo = null; }
                        String categoria = null;
                        try { categoria = rs.getString("categoria"); } catch (Exception ex) { categoria = null; }
                        String stock = null;
                        try { stock = rs.getString("stock"); } catch (Exception ex) { stock = null; }
                        sb.append('{');
                        sb.append("\"id\":").append(id).append(',');
                        sb.append("\"nombre\":\"").append(escape(nombre)).append("\",");
                        sb.append("\"codigo_barra\":\"").append(escape(codigo)).append("\",");
                        sb.append("\"categoria\":\"").append(escape(categoria)).append("\",");
                        sb.append("\"stock\":\"").append(escape(stock)).append("\"");
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
                String codigo = req.getParameter("codigo_barra");
                String categoria = req.getParameter("categoria");
                String stock = req.getParameter("stock");
                String insert = "INSERT INTO producto (nombre, codigo_barra, categoria, stock) VALUES (?,?,?,?)";
                try (PreparedStatement ps = conn.prepareStatement(insert, PreparedStatement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, nombre);
                    ps.setString(2, codigo);
                    ps.setString(3, categoria);
                    ps.setString(4, stock);
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
                String codigo = req.getParameter("codigo_barra");
                String categoria = req.getParameter("categoria");
                String stock = req.getParameter("stock");
                String update = "UPDATE producto SET nombre = ?, codigo_barra = ?, categoria = ?, stock = ? WHERE id_producto = ? OR id = ?";
                try (PreparedStatement ps = conn.prepareStatement(update)) {
                    ps.setString(1, nombre);
                    ps.setString(2, codigo);
                    ps.setString(3, categoria);
                    ps.setString(4, stock);
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
                String del = "DELETE FROM producto WHERE id_producto = ? OR id = ?";
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
            } else if ("toggle".equalsIgnoreCase(action)) {
                String id = req.getParameter("id");
                String[] flagCols = new String[]{"activo","enabled","activo_producto","estado"};
                boolean toggled = false;
                for (String col : flagCols) {
                    String sql = "UPDATE producto SET " + col + " = CASE WHEN " + col + " = 1 THEN 0 ELSE 1 END WHERE id_producto = ? OR id = ?";
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setString(1, id);
                        ps.setString(2, id);
                        int affected = ps.executeUpdate();
                        if (affected > 0) {
                            resp.getWriter().print("{\"ok\":true}");
                            toggled = true;
                            break;
                        }
                    } catch (SQLException ex) {
                        String msg = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
                        if (msg.contains("unknown column") || msg.contains("doesn't exist") || msg.contains("no such column") || msg.contains("unknown table")) {
                            continue;
                        }
                        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        resp.getWriter().print("{\"ok\":false,\"error\":\"db\"}");
                        return;
                    }
                }
                if (toggled) return;
                resp.getWriter().print("{\"ok\":false,\"error\":\"not_supported\"}");
                return;
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
