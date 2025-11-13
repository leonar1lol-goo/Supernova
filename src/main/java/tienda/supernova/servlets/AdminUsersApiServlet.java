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

@WebServlet("/admin/api/users")
public class AdminUsersApiServlet extends HttpServlet {

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
        String sqlWithActivo = "SELECT id_usuario AS id, nombre_usuario AS nombre, rol, email, activo FROM Usuario";
        String sqlWithoutActivo = "SELECT id_usuario AS id, nombre_usuario AS nombre, rol, email FROM Usuario";
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        boolean first = true;
        try (Connection conn = DBConnection.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(sqlWithActivo); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if (!first) sb.append(',');
                    first = false;
                    int id = rs.getInt("id");
                    String nombre = rs.getString("nombre");
                    String rol = rs.getString("rol");
                    String email = rs.getString("email");
                    boolean activo = false;
                    try { activo = rs.getBoolean("activo"); } catch (Exception x) { activo = false; }
                    sb.append('{');
                    sb.append("\"id\":").append(id).append(',');
                    sb.append("\"nombre\":\"").append(escape(nombre)).append("\",");
                    sb.append("\"rol\":\"").append(escape(rol)).append("\",");
                    sb.append("\"email\":\"").append(escape(email)).append("\",");
                    sb.append("\"activo\":").append(activo ? "true" : "false");
                    sb.append('}');
                }
            } catch (SQLException ex) {
                String msg = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
                if (msg.contains("activo") || msg.contains("unknown column") || msg.contains("column") ) {
                    try (PreparedStatement ps2 = conn.prepareStatement(sqlWithoutActivo); ResultSet rs2 = ps2.executeQuery()) {
                        while (rs2.next()) {
                            if (!first) sb.append(',');
                            first = false;
                            int id = rs2.getInt("id");
                            String nombre = rs2.getString("nombre");
                            String rol = rs2.getString("rol");
                            String email = rs2.getString("email");
                            boolean activo = false;
                            sb.append('{');
                            sb.append("\"id\":").append(id).append(',');
                            sb.append("\"nombre\":\"").append(escape(nombre)).append("\",");
                            sb.append("\"rol\":\"").append(escape(rol)).append("\",");
                            sb.append("\"email\":\"").append(escape(email)).append("\",");
                            sb.append("\"activo\":").append(activo ? "true" : "false");
                            sb.append('}');
                        }
                    }
                } else {
                    throw ex;
                }
            }
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().print("{\"error\":\"db\"}");
            return;
        }
        sb.append(']');
        resp.getWriter().print(sb.toString());
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
        String action = req.getParameter("action");
        resp.setContentType("application/json;charset=UTF-8");
        if (action == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().print("{\"error\":\"missing_action\"}");
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            if ("create".equals(action)) {
                String nombre = req.getParameter("nombre");
                String email = req.getParameter("email");
                String rol = req.getParameter("rol");
                String password = req.getParameter("password");
                if (nombre == null) nombre = "";
                if (email == null) email = "";
                String emailTrim = email.trim();
                if (emailTrim.isEmpty() || !emailTrim.contains("@") || !emailTrim.contains(".")) {
                    resp.getWriter().print("{\"ok\":false,\"error\":\"invalid_email\"}");
                    return;
                }
                if (password == null) password = "";
                if (password.isEmpty() || !password.matches(".*[^A-Za-z0-9].*")) {
                    resp.getWriter().print("{\"ok\":false,\"error\":\"invalid_password_special\"}");
                    return;
                }
                if (rol == null) rol = "user";
                try (PreparedStatement ps = conn.prepareStatement("INSERT INTO Usuario (nombre_usuario, rol, email, `contraseña`) VALUES (?,?,?,?)", java.sql.Statement.RETURN_GENERATED_KEYS)){
                    ps.setString(1, nombre);
                    ps.setString(2, rol);
                    ps.setString(3, emailTrim);
                    ps.setString(4, password);
                    ps.executeUpdate();
                    try (ResultSet gk = ps.getGeneratedKeys()){
                        if (gk.next()){
                            int newId = gk.getInt(1);
                            resp.getWriter().print("{\"ok\":true,\"id\":"+newId+"}");
                        } else {
                            resp.getWriter().print("{\"ok\":true}");
                        }
                    }
                    return;
                }
            }

            String idParam = req.getParameter("id");
            if (idParam == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().print("{\"error\":\"missing_params\"}");
                return;
            }
            int id;
            try { id = Integer.parseInt(idParam); } catch (NumberFormatException e) { resp.setStatus(HttpServletResponse.SC_BAD_REQUEST); resp.getWriter().print("{\"error\":\"bad_id\"}"); return; }

            if ("delete".equals(action)) {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM Usuario WHERE id_usuario = ?")) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                }
                resp.getWriter().print("{\"ok\":true}");
                return;
            } else if ("toggleAdmin".equals(action)) {
                String cur = null;
                try (PreparedStatement ps = conn.prepareStatement("SELECT rol FROM Usuario WHERE id_usuario = ?")) {
                    ps.setInt(1, id);
                    try (ResultSet rs = ps.executeQuery()) { if (rs.next()) cur = rs.getString("rol"); }
                }
                String next = (cur != null && cur.equalsIgnoreCase("admin")) ? "user" : "admin";
                try (PreparedStatement ps2 = conn.prepareStatement("UPDATE Usuario SET rol = ? WHERE id_usuario = ?")) {
                    ps2.setString(1, next);
                    ps2.setInt(2, id);
                    ps2.executeUpdate();
                }
                resp.getWriter().print("{\"ok\":true,\"role\":\""+escape(next)+"\"}");
                return;
            } else if ("toggleActive".equals(action) || "toggleActivo".equals(action)) {
                try {
                    try (PreparedStatement ps = conn.prepareStatement("UPDATE Usuario SET activo = NOT activo WHERE id_usuario = ?")) {
                        ps.setInt(1, id);
                        ps.executeUpdate();
                    }
                    boolean newActivo = false;
                    try (PreparedStatement ps2 = conn.prepareStatement("SELECT activo FROM Usuario WHERE id_usuario = ?")){
                        ps2.setInt(1, id);
                        try (ResultSet rs = ps2.executeQuery()){ if (rs.next()) newActivo = rs.getBoolean("activo"); }
                    }
                    resp.getWriter().print("{\"ok\":true,\"activo\":" + (newActivo?"true":"false") + "}");
                    return;
                } catch (SQLException ex) {
                    String msg = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
                    if (msg.contains("activo") || msg.contains("unknown column") || msg.contains("column")) {
                        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        resp.getWriter().print("{\"ok\":false,\"error\":\"no_activo_column\"}");
                        return;
                    }
                    throw ex;
                }
            } else if ("update".equals(action)) {
                String nombre = req.getParameter("nombre");
                String email = req.getParameter("email");
                String rol = req.getParameter("rol");
                String password = req.getParameter("password");
                if (email != null) {
                    String emailTrim = email.trim();
                    if (emailTrim.isEmpty() || !emailTrim.contains("@") || !emailTrim.contains(".")) {
                        resp.getWriter().print("{\"ok\":false,\"error\":\"invalid_email\"}");
                        return;
                    }
                    email = emailTrim;
                }
                if (password != null) {
                    if (password.isEmpty() || !password.matches(".*[^A-Za-z0-9].*")) {
                        resp.getWriter().print("{\"ok\":false,\"error\":\"invalid_password_special\"}");
                        return;
                    }
                }
                java.util.List<String> sets = new java.util.ArrayList<>();
                java.util.List<Object> paramsList = new java.util.ArrayList<>();
                if (nombre != null) { sets.add("nombre_usuario = ?"); paramsList.add(nombre); }
                if (email != null) { sets.add("email = ?"); paramsList.add(email); }
                if (rol != null) { sets.add("rol = ?"); paramsList.add(rol); }
                if (password != null) { sets.add("`contraseña` = ?"); paramsList.add(password); }
                if (sets.isEmpty()){
                    resp.getWriter().print("{\"ok\":false,\"error\":\"nothing_to_update\"}");
                    return;
                }
                StringBuilder sb = new StringBuilder("UPDATE Usuario SET ");
                for (int i=0;i<sets.size();i++){ sb.append(sets.get(i)); if (i<sets.size()-1) sb.append(", "); }
                sb.append(" WHERE id_usuario = ?");
                try (PreparedStatement ps = conn.prepareStatement(sb.toString())){
                    int idx = 1;
                    for (Object p : paramsList) ps.setObject(idx++, p);
                    ps.setInt(idx, id);
                    int u = ps.executeUpdate();
                    resp.getWriter().print("{\"ok\":true,\"updated\":"+u+"}");
                    return;
                }
            }
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().print("{\"error\":\"db\"}");
            return;
        }

        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        resp.getWriter().print("{\"error\":\"unknown_action\"}");
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","\\r");
    }
}
