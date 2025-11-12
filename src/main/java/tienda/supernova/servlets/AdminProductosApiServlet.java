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
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.math.BigDecimal;
import java.sql.DatabaseMetaData;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.LinkedHashMap;

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
        String[] queries = new String[] {
            "SELECT id_producto AS id, nombre AS nombre, codigo_barra, sku, descripcion, marca, unidad_medida, precio, categoria_id, activo, categoria, stock FROM producto",
            "SELECT producto_id AS id, nombre AS nombre, codigo_barras AS codigo_barra, sku, descripcion, marca, unidad_medida, precio, categoria_id, activo, categoria, stock FROM producto",
            "SELECT id_producto AS id, nombre AS nombre, codigo_barra, sku, descripcion, marca, unidad_medida, precio, categoria_id, activo, categoria, stock FROM Producto",
            "SELECT producto_id AS id, nombre AS nombre, codigo_barras AS codigo_barra, sku, descripcion, marca, unidad_medida, precio, categoria_id, activo, categoria, stock FROM Producto",
            "SELECT id AS id, nombre AS nombre, codigo_barra, sku, descripcion, marca, unidad_medida, precio, categoria_id, activo, categoria, stock FROM productos",
            "SELECT id AS id, nombre_producto AS nombre, codigo_barra, sku, descripcion, marca, unidad_medida, precio, categoria_id, activo, categoria, stock FROM productos"
        };

        StringBuilder sb = new StringBuilder();
        sb.append('[');
        boolean first = true;
        try (Connection conn = DBConnection.getConnection()) {
            boolean anyExecuted = false;
            Set<String> seen = new HashSet<>();
            for (String q : queries) {
                try (PreparedStatement ps = conn.prepareStatement(q); ResultSet rs = ps.executeQuery()) {
                    anyExecuted = true;
                    while (rs.next()) {
                        ResultSetMetaData md = rs.getMetaData();
                        int cols = md.getColumnCount();
                        Map<String,String> row = new LinkedHashMap<>();
                        for (int c = 1; c <= cols; c++) {
                            String colLabel = md.getColumnLabel(c);
                            Object o = null;
                            try { o = rs.getObject(c); } catch (Exception ex) { o = null; }
                            String val = o == null ? "" : o.toString();
                            row.put(colLabel == null ? ("col"+c) : colLabel, val);
                        }
                        String idVal = firstNonEmpty(row, "id","producto_id","productoid","productoId","product_id","producto");
                        if (idVal == null) idVal = "";
                        String uniqueKey = idVal.isEmpty() ? row.toString() : idVal;
                        if (seen.contains(uniqueKey)) continue;
                        seen.add(uniqueKey);
                        if (!first) sb.append(',');
                        first = false;
                        Map<String,String> out = new LinkedHashMap<>();
                        out.put("id", idVal);
                        out.put("nombre", firstNonEmpty(row, "nombre","nombre_producto","nombreproducto","name"));
                        out.put("codigo_barra", firstNonEmpty(row, "codigo_barra","codigo_barras","codigo","codigo_barras"));
                        out.put("categoria", firstNonEmpty(row, "categoria","categoria_nombre","category"));
                        out.put("stock", firstNonEmpty(row, "stock"));
                        for (Map.Entry<String,String> e : row.entrySet()) {
                            String k = e.getKey();
                            if (k == null) continue;
                            String kl = k.toLowerCase();
                            if (kl.equals("id") || kl.equals("producto_id") || kl.equals("productoid") || kl.equals("product_id") || kl.equals("nombre") || kl.contains("nombre") || kl.contains("codigo") || kl.contains("categoria") || kl.equals("stock")) continue;
                            out.put(k, e.getValue());
                        }
                        sb.append('{');
                        int i = 0;
                        for (Map.Entry<String,String> e : out.entrySet()) {
                            String k = e.getKey();
                            String v = e.getValue() == null ? "" : e.getValue();
                            sb.append('"').append(escape(k)).append('"').append(':');
                            if (isNumeric(v) && !k.equalsIgnoreCase("codigo_barra") && !k.toLowerCase().contains("codigo")) {
                                sb.append(v);
                            } else {
                                sb.append('"').append(escape(v)).append('"');
                            }
                            if (++i < out.size()) sb.append(',');
                        }
                        sb.append('}');
                    }
                } catch (SQLException ex) {
                    String msg = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
                    if (msg.contains("unknown table") || msg.contains("doesn't exist") || msg.contains("no such table")
                            || msg.contains("unknown column") || msg.contains("no such column") || msg.contains("column not found")
                            || msg.contains("references invalid") || (msg.contains("view") && msg.contains("invalid"))) {
                        continue;
                    }
                    throw ex;
                } catch (Exception ex) {
                    continue;
                }
            }
            if (!first || anyExecuted) {
                sb.append(']');
                resp.getWriter().print(sb.toString());
                return;
            }
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            String msg = e.getMessage() == null ? "" : e.getMessage();
            resp.getWriter().print("{\"error\":\"db\",\"message\":\"" + escape(msg) + "\"}");
            e.printStackTrace();
            return;
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            String msg = e.getMessage() == null ? "" : e.getMessage();
            resp.getWriter().print("{\"error\":\"internal\",\"message\":\"" + escape(msg) + "\"}");
            e.printStackTrace();
            return;
        }

        sb.append(']');
        resp.getWriter().print(sb.toString());
    }

    private String[] findTableAndIdColumn(Connection conn, String[] tableNames, String[] idCols, String id) throws SQLException {
        if (conn == null || tableNames == null || idCols == null) return null;
        DatabaseMetaData meta = conn.getMetaData();
        for (String tbl : tableNames) {
            if (tbl == null) continue;
            List<String> cols = new ArrayList<>();
            try (ResultSet crs = meta.getColumns(null, null, tbl, null)) {
                while (crs.next()) {
                    String cn = crs.getString("COLUMN_NAME");
                    if (cn != null) cols.add(cn.toLowerCase());
                }
            } catch (SQLException ex) {
                continue;
            }
            if (cols.isEmpty()) continue;
            for (String idc : idCols) {
                if (idc == null) continue;
                if (!cols.contains(idc.toLowerCase())) continue;
                String sql = "SELECT 1 FROM " + tbl + " WHERE " + idc + " = ? LIMIT 1";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, id);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs != null && rs.next()) {
                            return new String[]{tbl, idc};
                        }
                    }
                } catch (SQLException ex) {
                    continue;
                }
            }
        }
        return null;
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","\\r");
    }

    private boolean isNumeric(String s) {
        if (s == null || s.length() == 0) return false;
        try {
            new BigDecimal(s);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String firstNonEmpty(Map<String,String> map, String... keys) {
        if (map == null || keys == null) return "";
        for (String k : keys) {
            if (k == null) continue;
            String v = map.get(k);
            if (v != null && v.length() > 0) return v;
            v = map.get(k.toLowerCase());
            if (v != null && v.length() > 0) return v;
            v = map.get(k.toUpperCase());
            if (v != null && v.length() > 0) return v;
        }
        return "";
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
                String[] tableNames = new String[]{"producto","productos","Producto","Productos"};
                String[] idCols = new String[]{"id_producto","producto_id","id","productoid","product_id"};
                String foundTable = null;
                List<String> tableCols = new ArrayList<>();
                DatabaseMetaData meta = conn.getMetaData();
                for (String tbl : tableNames) {
                    if (tbl == null) continue;
                    tableCols.clear();
                    try (ResultSet crs = meta.getColumns(null, null, tbl, null)) {
                        while (crs.next()) {
                            String cn = crs.getString("COLUMN_NAME");
                            if (cn != null) tableCols.add(cn);
                        }
                    } catch (SQLException ex) {
                        continue;
                    }
                    if (!tableCols.isEmpty()) { foundTable = tbl; break; }
                }
                if (foundTable == null) {
                    resp.getWriter().print("{\"ok\":false,\"error\":\"no_table\"}");
                    return;
                }
                List<String> toInsert = new ArrayList<>();
                for (String col : tableCols) {
                    if (col == null) continue;
                    String cl = col.toLowerCase();
                    boolean isId = false;
                    for (String idc : idCols) { if (idc != null && idc.equalsIgnoreCase(cl)) { isId = true; break; } }
                    if (isId) continue;
                    String v = req.getParameter(col);
                    if (v == null) v = req.getParameter(cl);
                    if (v != null) {
                        // validate numeric non-negative fields
                        try {
                            if ((cl.contains("stock") || cl.contains("cantidad")) && v.trim().length() > 0) {
                                int n = Integer.parseInt(v.trim());
                                if (n < 0) {
                                    resp.getWriter().print("{\"ok\":false,\"error\":\"invalid_value\",\"field\":\""+escape(col)+"\",\"message\":\"Stock debe ser >= 0\"}");
                                    return;
                                }
                            }
                            if ((cl.contains("precio") || cl.contains("price") || cl.contains("cost")) && v.trim().length() > 0) {
                                double f = Double.parseDouble(v.trim());
                                if (f < 0) {
                                    resp.getWriter().print("{\"ok\":false,\"error\":\"invalid_value\",\"field\":\""+escape(col)+"\",\"message\":\"Precio debe ser >= 0\"}");
                                    return;
                                }
                            }
                        } catch (NumberFormatException nfe) {
                            resp.getWriter().print("{\"ok\":false,\"error\":\"invalid_value\",\"field\":\""+escape(col)+"\",\"message\":\"Valor numérico inválido\"}");
                            return;
                        }
                        toInsert.add(col);
                    }
                }
                if (toInsert.isEmpty()) {
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
                }
                StringBuilder colsPart = new StringBuilder();
                StringBuilder valsPart = new StringBuilder();
                for (int i = 0; i < toInsert.size(); i++) {
                    if (i > 0) { colsPart.append(','); valsPart.append(','); }
                    colsPart.append(toInsert.get(i)); valsPart.append('?');
                }
                String insertSql = "INSERT INTO " + foundTable + " (" + colsPart.toString() + ") VALUES (" + valsPart.toString() + ")";
                try (PreparedStatement ps = conn.prepareStatement(insertSql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                    int idx = 1;
                    for (String colName : toInsert) {
                        String v = req.getParameter(colName);
                        if (v == null) v = req.getParameter(colName.toLowerCase());
                        ps.setString(idx++, v == null ? "" : v);
                    }
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
                String[] tableNames = new String[]{"producto","productos","Producto","Productos"};
                String[] idCols = new String[]{"id_producto","producto_id","id","productoid","product_id"};
                try {
                    String[] found = findTableAndIdColumn(conn, tableNames, idCols, id);
                    if (found == null) {
                        resp.getWriter().print("{\"ok\":false,\"error\":\"not_found\"}");
                        return;
                    }
                    String tbl = found[0];
                    String idCol = found[1];
                    DatabaseMetaData meta = conn.getMetaData();
                    java.util.Map<String,String> colsMap = new java.util.HashMap<>();
                    try (ResultSet crs = meta.getColumns(null, null, tbl, null)) {
                        while (crs.next()) {
                            String cn = crs.getString("COLUMN_NAME");
                            if (cn != null) colsMap.put(cn.toLowerCase(), cn);
                        }
                    }
                    List<String> toUpdate = new ArrayList<>();
                    java.util.Map<String,String[]> params = req.getParameterMap();
                    for (String p : params.keySet()) {
                        if (p == null) continue;
                        String pl = p.toLowerCase();
                        if (pl.equals("action") || pl.equals("id")) continue;
                        if (colsMap.containsKey(pl)) {
                            toUpdate.add(colsMap.get(pl));
                        }
                    }
                    // validate numeric non-negative values for update
                    for (String colName : toUpdate) {
                        if (colName == null) continue;
                        String cl = colName.toLowerCase();
                        String paramVal = req.getParameter(colName);
                        if (paramVal == null) paramVal = req.getParameter(colName.toLowerCase());
                        if (paramVal == null) continue;
                        try {
                            if ((cl.contains("stock") || cl.contains("cantidad")) && paramVal.trim().length() > 0) {
                                int n = Integer.parseInt(paramVal.trim());
                                if (n < 0) {
                                    resp.getWriter().print("{\"ok\":false,\"error\":\"invalid_value\",\"field\":\""+escape(colName)+"\",\"message\":\"Stock debe ser >= 0\"}");
                                    return;
                                }
                            }
                            if ((cl.contains("precio") || cl.contains("price") || cl.contains("cost")) && paramVal.trim().length() > 0) {
                                double f = Double.parseDouble(paramVal.trim());
                                if (f < 0) {
                                    resp.getWriter().print("{\"ok\":false,\"error\":\"invalid_value\",\"field\":\""+escape(colName)+"\",\"message\":\"Precio debe ser >= 0\"}");
                                    return;
                                }
                            }
                        } catch (NumberFormatException nfe) {
                            resp.getWriter().print("{\"ok\":false,\"error\":\"invalid_value\",\"field\":\""+escape(colName)+"\",\"message\":\"Valor numérico inválido\"}");
                            return;
                        }
                    }
                    if (toUpdate.isEmpty()) {
                        resp.getWriter().print("{\"ok\":false,\"error\":\"not_supported\"}");
                        return;
                    }
                    StringBuilder set = new StringBuilder();
                    for (int i = 0; i < toUpdate.size(); i++) {
                        if (i > 0) set.append(",");
                        set.append(toUpdate.get(i)).append(" = ?");
                    }
                    String update = "UPDATE " + tbl + " SET " + set.toString() + " WHERE " + idCol + " = ?";
                    try (PreparedStatement ps = conn.prepareStatement(update)) {
                        int idx = 1;
                        for (String colName : toUpdate) {
                            String paramVal = req.getParameter(colName);
                            if (paramVal == null) paramVal = req.getParameter(colName.toLowerCase());
                            ps.setString(idx++, paramVal == null ? "" : paramVal);
                        }
                        ps.setString(idx, id);
                        int affected = ps.executeUpdate();
                        if (affected > 0) {
                            resp.getWriter().print("{\"ok\":true,\"table\":\""+tbl+"\"}");
                            return;
                        }
                        resp.getWriter().print("{\"ok\":false,\"error\":\"not_found\"}");
                        return;
                    }
                } catch (SQLException ex) {
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    resp.getWriter().print("{\"ok\":false,\"error\":\"db\"}");
                    return;
                }
            } else if ("delete".equalsIgnoreCase(action)) {
                String id = req.getParameter("id");
                String[] tableNames = new String[]{"producto","productos","Producto","Productos"};
                String[] idCols = new String[]{"id_producto","producto_id","id","productoid","product_id"};
                try {
                    String[] found = findTableAndIdColumn(conn, tableNames, idCols, id);
                    if (found != null) {
                        String tbl = found[0];
                        String idCol = found[1];
                        String del = "DELETE FROM " + tbl + " WHERE " + idCol + " = ?";
                        try (PreparedStatement ps = conn.prepareStatement(del)) {
                            ps.setString(1, id);
                            int affected = ps.executeUpdate();
                            if (affected > 0) {
                                resp.getWriter().print("{\"ok\":true,\"table\":\""+tbl+"\"}");
                                return;
                            }
                            resp.getWriter().print("{\"ok\":false,\"error\":\"not_found\"}");
                            return;
                        }
                    } else {
                        resp.getWriter().print("{\"ok\":false,\"error\":\"not_found\"}");
                        return;
                    }
                } catch (SQLException ex) {
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    resp.getWriter().print("{\"ok\":false,\"error\":\"db\"}");
                    return;
                }
            } else if ("toggle".equalsIgnoreCase(action)) {
                String id = req.getParameter("id");
                String[] flagCols = new String[]{"activo","enabled","activo_producto","estado"};
                String[] tableNames = new String[]{"producto","productos","Producto","Productos"};
                String[] idCols = new String[]{"id_producto","producto_id","id","productoid","product_id"};
                try {
                    String[] found = findTableAndIdColumn(conn, tableNames, idCols, id);
                    if (found == null) {
                        resp.getWriter().print("{\"ok\":false,\"error\":\"not_supported\"}");
                        return;
                    }
                    String tbl = found[0];
                    String idCol = found[1];
                    DatabaseMetaData meta = conn.getMetaData();
                    List<String> cols = new ArrayList<>();
                    try (ResultSet crs = meta.getColumns(null, null, tbl, null)) {
                        while (crs.next()) {
                            String cn = crs.getString("COLUMN_NAME");
                            if (cn != null) cols.add(cn.toLowerCase());
                        }
                    }
                    String foundFlag = null;
                    for (String fc : flagCols) {
                        if (cols.contains(fc.toLowerCase())) { foundFlag = fc; break; }
                    }
                    if (foundFlag == null) {
                        resp.getWriter().print("{\"ok\":false,\"error\":\"not_supported\"}");
                        return;
                    }
                    String sql = "UPDATE " + tbl + " SET " + foundFlag + " = CASE WHEN " + foundFlag + " = 1 THEN 0 ELSE 1 END WHERE " + idCol + " = ?";
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        if (isNumeric(id)) {
                            try { ps.setLong(1, Long.parseLong(id)); } catch (Exception ex) { ps.setString(1, id); }
                        } else {
                            ps.setString(1, id);
                        }
                        int affected = ps.executeUpdate();
                        if (affected > 0) {
                            resp.getWriter().print("{\"ok\":true,\"table\":\""+tbl+"\"}");
                            return;
                        }
                        resp.getWriter().print("{\"ok\":false,\"error\":\"not_found\"}");
                        return;
                    }
                } catch (SQLException ex) {
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    resp.getWriter().print("{\"ok\":false,\"error\":\"db\"}");
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
