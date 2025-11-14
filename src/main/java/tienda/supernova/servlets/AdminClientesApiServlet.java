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
        if (role == null) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            resp.setContentType("application/json;charset=UTF-8");
            resp.getWriter().print("{\"error\":\"forbidden\"}");
            return;
        }

        resp.setContentType("application/json;charset=UTF-8");
        String[] queries = new String[]{
            "SELECT * FROM cliente",
            "SELECT * FROM Cliente",
            "SELECT * FROM clientes",
            "SELECT * FROM Clientes"
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
                        String idStr = null;
                        try { idStr = rs.getString("id_cliente"); } catch (Exception _e) { try { idStr = rs.getString("id"); } catch (Exception __e) { try { idStr = rs.getString("id_client"); } catch (Exception ___e) { try { idStr = rs.getString("idcliente"); } catch (Exception ____e) { idStr = null; } } } }
                        int id = 0;
                        try { id = idStr != null ? Integer.parseInt(idStr) : rs.getInt("id"); } catch (Exception e1) { try { id = rs.getInt(1); } catch (Exception e2) { id = 0; } }
                        String nombre = null;
                        try { nombre = rs.getString("nombre"); } catch (Exception exn) { try { nombre = rs.getString("nombre_cliente"); } catch (Exception exn2) { nombre = null; } }
                        String direccion = null;
                        try { direccion = rs.getString("direccion"); } catch (Exception exd) { direccion = null; }
                        String email = null;
                        try { email = rs.getString("email"); } catch (Exception exe) { try { email = rs.getString("correo"); } catch (Exception exe2) { try { email = rs.getString("mail"); } catch (Exception exe3) { email = null; } } }
                        String telefono = null;
                        try { telefono = rs.getString("telefono"); } catch (Exception ext) { try { telefono = rs.getString("telefono_cliente"); } catch (Exception ext2) { try { telefono = rs.getString("tel"); } catch (Exception ext3) { telefono = null; } } }
                        String dni = null;
                        try { dni = rs.getString("dni"); } catch (Exception exd1) { try { dni = rs.getString("ruc"); } catch (Exception exd2) { try { dni = rs.getString("dni_ruc"); } catch (Exception exd3) { try { dni = rs.getString("documento"); } catch (Exception exd4) { dni = null; } } } }
                        sb.append('{');
                        sb.append("\"id\":").append(id).append(',');
                        sb.append("\"nombre\":\"").append(escape(nombre)).append("\",");
                        sb.append("\"dni\":\"").append(escape(dni)).append("\",");
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
        if (role == null) {
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
                String dni = req.getParameter("dni");
                if (email == null) email = "";
                String emailTrim = email.trim();
                if (emailTrim.isEmpty() || !emailTrim.contains("@") || !emailTrim.contains(".")) {
                    resp.getWriter().print("{\"ok\":false,\"error\":\"invalid_email\"}");
                    return;
                }
                if (dni == null || !dni.trim().matches("\\d{1,11}")) {
                    resp.getWriter().print("{\"ok\":false,\"error\":\"invalid_dni\"}");
                    return;
                }
                String insertWithDni = "INSERT INTO cliente (nombre, dni, direccion, telefono, email) VALUES (?,?,?,?,?)";
                String insertNoDni = "INSERT INTO cliente (nombre, direccion, telefono, email) VALUES (?,?,?,?)";
                        if (dni != null && !dni.trim().isEmpty()) {
                    try (PreparedStatement ps = conn.prepareStatement(insertWithDni, PreparedStatement.RETURN_GENERATED_KEYS)) {
                        ps.setString(1, nombre);
                        ps.setString(2, dni.trim());
                        ps.setString(3, direccion);
                        ps.setString(4, telefono);
                        ps.setString(5, emailTrim);
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
                    } catch (SQLException ex) {
                        String msg = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
                        if (msg.contains("unknown column") || msg.contains("doesn't exist") || msg.contains("no such column")) {
                        } else {
                            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                            resp.getWriter().print("{\"ok\":false,\"error\":\"db\"}");
                            return;
                        }
                    }
                }
                try (PreparedStatement ps = conn.prepareStatement(insertNoDni, PreparedStatement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, nombre);
                    ps.setString(2, direccion);
                    ps.setString(3, telefono);
                    ps.setString(4, emailTrim);
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
                // Only admin/supervisor can update clients
                if (!(role.equalsIgnoreCase("admin") || role.equalsIgnoreCase("supervisor"))) {
                    resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    resp.setContentType("application/json;charset=UTF-8");
                    resp.getWriter().print("{\"error\":\"forbidden\"}");
                    return;
                }
                String id = req.getParameter("id");
                String dni = req.getParameter("dni");
                String telefono = req.getParameter("telefono");
                String email = req.getParameter("email");
                    if (email == null) email = "";
                    String emailTrim = email.trim();
                    if (emailTrim.isEmpty() || !emailTrim.contains("@") || !emailTrim.contains(".")) {
                        resp.getWriter().print("{\"ok\":false,\"error\":\"invalid_email\"}");
                        return;
                    }
                    if (dni != null && !dni.trim().isEmpty()) {
                        if (!dni.trim().matches("\\d{1,11}")) {
                            resp.getWriter().print("{\"ok\":false,\"error\":\"invalid_dni\"}");
                            return;
                        }
                    }
                String[] tables = new String[]{"cliente","Cliente","clientes","Clientes"};
                String[] phoneCols = new String[]{"telefono","telefono_cliente","tel"};
                String[] emailCols = new String[]{"email","correo","mail"};
                String[] dniCols = new String[]{"dni","ruc","dni_ruc","documento"};
                String[] idCols = new String[]{"id_cliente","id","id_client","idcliente"};
                boolean updated = false;
                for (String t : tables) {
                    if (updated) break;
                    for (String pcol : phoneCols) {
                        if (updated) break;
                        for (String ecol : emailCols) {
                            if (updated) break;
                            for (String dnicol : dniCols) {
                                if (updated) break;
                                for (String idc : idCols) {
                                    String sql = "UPDATE " + t + " SET " + pcol + " = ?, " + ecol + " = ?, " + dnicol + " = ? WHERE " + idc + " = ?";
                                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                                        ps.setString(1, telefono);
                                        ps.setString(2, emailTrim);
                                        ps.setString(3, dni != null ? dni : "");
                                        ps.setString(4, id);
                                        int affected = ps.executeUpdate();
                                        if (affected > 0) {
                                            resp.getWriter().print("{\"ok\":true}");
                                            updated = true;
                                            break;
                                        }
                                    } catch (SQLException ex) {
                                        String msg = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
                                        if (msg.contains("unknown column") || msg.contains("columna desconocida") || msg.contains("doesn't exist") || msg.contains("no such column") || msg.contains("unknown table") || msg.contains("doesn't exist")) {
                                            continue;
                                        }
                                        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                                        resp.getWriter().print("{\"ok\":false,\"error\":\"db\"}");
                                        return;
                                    }
                                }
                            }
                        }
                    }
                }
                if (updated) return;
                resp.getWriter().print("{\"ok\":false,\"error\":\"not_found\"}");
                return;
            } else if ("delete".equalsIgnoreCase(action)) {
                // Only admin/supervisor can delete clients
                if (!(role.equalsIgnoreCase("admin") || role.equalsIgnoreCase("supervisor"))) {
                    resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    resp.setContentType("application/json;charset=UTF-8");
                    resp.getWriter().print("{\"error\":\"forbidden\"}");
                    return;
                }
                String id = req.getParameter("id");
                String[] tables = new String[]{"cliente","Cliente","clientes","Clientes"};
                String[] idCols = new String[]{"id_cliente","id","id_client","idcliente"};
                boolean deleted = false;
                for (String t : tables) {
                    if (deleted) break;
                    for (String idc : idCols) {
                        String sql = "DELETE FROM " + t + " WHERE " + idc + " = ?";
                        try (PreparedStatement ps = conn.prepareStatement(sql)) {
                            ps.setString(1, id);
                            int affected = ps.executeUpdate();
                            if (affected > 0) {
                                resp.getWriter().print("{\"ok\":true}");
                                deleted = true;
                                break;
                            }
                        } catch (SQLException ex) {
                            String msg = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
                            if (msg.contains("unknown table") || msg.contains("doesn't exist") || msg.contains("no such table") || msg.contains("unknown column") || msg.contains("columna desconocida") || msg.contains("doesn't exist")) {
                                continue;
                            }
                            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                            resp.getWriter().print("{\"ok\":false,\"error\":\"db\"}");
                            return;
                        }
                    }
                }
                if (deleted) return;
                resp.getWriter().print("{\"ok\":false,\"error\":\"not_found\"}");
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
