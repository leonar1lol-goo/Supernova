package tienda.supernova.servlets;

import tienda.supernova.db.DBConnection;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.*;

@WebServlet("/admin/api/orders")
public class AdminOrdersApiServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json; charset=utf-8");
        resp.setCharacterEncoding("utf-8");
        try (Connection con = DBConnection.getConnection()) {
            String orderIdParam = req.getParameter("id");
            if (orderIdParam == null) orderIdParam = req.getParameter("orderId");
            if (orderIdParam != null && !orderIdParam.isEmpty()){
                int orderId = Integer.parseInt(orderIdParam);
                boolean hasPrecioUnitario = hasColumn(con, "Detalle_Pedido", "precio_unitario");
                String sqlItems = hasPrecioUnitario ?
                        "SELECT dp.id_detalle, dp.id_producto, prod.nombre AS producto, dp.cantidad_solicitada, dp.cantidad_preparada, dp.precio_unitario FROM Detalle_Pedido dp LEFT JOIN Producto prod ON dp.id_producto = prod.id_producto WHERE dp.id_pedido = ?" :
                        "SELECT dp.id_detalle, dp.id_producto, prod.nombre AS producto, dp.cantidad_solicitada, dp.cantidad_preparada FROM Detalle_Pedido dp LEFT JOIN Producto prod ON dp.id_producto = prod.id_producto WHERE dp.id_pedido = ?";
                try (PreparedStatement ps = con.prepareStatement(sqlItems)){
                    ps.setInt(1, orderId);
                    try (ResultSet rs = ps.executeQuery()){
                        StringBuilder sb = new StringBuilder(); sb.append('[');
                        boolean first = true;
                        while (rs.next()){
                            if (!first) sb.append(','); first = false;
                            sb.append('{');
                            sb.append("\"id_detalle\":").append(rs.getInt("id_detalle")).append(',');
                            sb.append("\"id_producto\":").append(rs.getInt("id_producto")).append(',');
                            sb.append("\"producto\":").append(quote(rs.getString("producto"))).append(',');
                            sb.append("\"cantidad_solicitada\":").append(rs.getInt("cantidad_solicitada")).append(',');
                            sb.append("\"cantidad_preparada\":").append(rs.getInt("cantidad_preparada"));
                            if (hasPrecioUnitario) sb.append(',').append("\"precio_unitario\":").append(rs.getBigDecimal("precio_unitario")!=null?rs.getBigDecimal("precio_unitario"):"0");
                            sb.append('}');
                        }
                        sb.append(']');
                        resp.getWriter().write(sb.toString());
                        return;
                    }
                }
            }
            boolean hasPrecioUnitario = false;
            boolean hasPrecioProducto = false;

            try (PreparedStatement p = con.prepareStatement("SELECT * FROM Detalle_Pedido LIMIT 1")) {
                try (ResultSet r = p.executeQuery()) {
                    ResultSetMetaData md = r.getMetaData();
                    int cols = md.getColumnCount();
                    for (int i = 1; i <= cols; i++) {
                        String col = md.getColumnLabel(i);
                        if (col == null) continue;
                        if ("precio_unitario".equalsIgnoreCase(col)) hasPrecioUnitario = true;
                    }
                }
            } catch (SQLException ignore) {
            }

            try (PreparedStatement p2 = con.prepareStatement("SELECT * FROM Producto LIMIT 1")) {
                try (ResultSet r2 = p2.executeQuery()) {
                    ResultSetMetaData md2 = r2.getMetaData();
                    int cols2 = md2.getColumnCount();
                    for (int i = 1; i <= cols2; i++) {
                        String col = md2.getColumnLabel(i);
                        if (col == null) continue;
                        if ("precio".equalsIgnoreCase(col)) hasPrecioProducto = true;
                    }
                }
            } catch (SQLException ignore) {
            }

            String totalExpr;
            boolean totalIsItems = false;
            if (hasPrecioUnitario) {
                totalExpr = "COALESCE(SUM(dp.cantidad_solicitada * dp.precio_unitario),0)";
            } else if (hasPrecioProducto) {
                totalExpr = "COALESCE(SUM(dp.cantidad_solicitada * prod.precio),0)";
            } else {
                totalExpr = "COALESCE(SUM(dp.cantidad_solicitada),0)";
                totalIsItems = true;
            }

            String sql = "SELECT p.id_pedido, COALESCE(c.nombre, '(sin cliente)') AS cliente, p.estado, p.fecha_pedido, "
                    + totalExpr + " AS total "
                    + "FROM Pedido p "
                    + "LEFT JOIN Cliente c ON p.id_cliente = c.id_cliente "
                    + "LEFT JOIN Detalle_Pedido dp ON dp.id_pedido = p.id_pedido "
                    + "LEFT JOIN Producto prod ON dp.id_producto = prod.id_producto "
                    + "GROUP BY p.id_pedido, c.nombre, p.estado, p.fecha_pedido "
                    + "ORDER BY p.fecha_pedido DESC";

            try (PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                StringBuilder sb = new StringBuilder();
                sb.append("[");
                boolean first = true;
                while (rs.next()) {
                    if (!first) sb.append(','); first = false;
                    int id = rs.getInt("id_pedido");
                    String cliente = rs.getString("cliente");
                    String estado = rs.getString("estado");
                    Timestamp ts = rs.getTimestamp("fecha_pedido");
                    String fecha = ts != null ? ts.toString() : null;
                    BigDecimal total = rs.getBigDecimal("total");

                    sb.append('{');
                    sb.append("\"id\":").append(id).append(',');
                    sb.append("\"cliente\":").append(quote(cliente)).append(',');
                    sb.append("\"estado\":").append(quote(estado)).append(',');
                    sb.append("\"fecha\":").append(quote(fecha)).append(',');
                    if (total == null) sb.append("\"total\":0"); else sb.append("\"total\":").append(total);
                    sb.append(',');
                    sb.append("\"totalIsItems\":").append(totalIsItems);
                    sb.append('}');
                }
                sb.append("]");
                resp.getWriter().write(sb.toString());
            }

        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json; charset=utf-8");
        req.setCharacterEncoding("utf-8");
        try (Connection con = DBConnection.getConnection()) {
            String action = req.getParameter("action");
            if (action == null) action = "";


            if ("create".equalsIgnoreCase(action)) {
                String clienteEmail = req.getParameter("cliente_email");
                String clienteNombre = req.getParameter("cliente_nombre");
                String estado = req.getParameter("estado"); if (estado==null) estado = "pendiente";
                String prioridad = req.getParameter("prioridad");
                int idCliente = getOrCreateClient(con, clienteEmail, clienteNombre);
                String sql = "INSERT INTO Pedido (id_cliente, estado, prioridad) VALUES (?, ?, ?)";
                try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)){
                    if (idCliente>0) ps.setInt(1, idCliente); else ps.setNull(1, Types.INTEGER);
                    ps.setString(2, estado);
                    ps.setString(3, prioridad);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()){
                        if (keys.next()){
                            int id = keys.getInt(1);
                            resp.getWriter().write("{\"ok\":true,\"id\":"+id+"}");
                            return;
                        }
                    }
                }
                resp.getWriter().write("{\"ok\":false}");
                return;
            } else if ("update".equalsIgnoreCase(action)) {
                String idStr = req.getParameter("id"); if (idStr==null){ resp.getWriter().write("{\"ok\":false,\"error\":\"missing id\"}"); return; }
                int id = Integer.parseInt(idStr);
                String estado = req.getParameter("estado");
                String prioridad = req.getParameter("prioridad");
                String clienteEmail = req.getParameter("cliente_email");
                String clienteNombre = req.getParameter("cliente_nombre");
                Integer idCliente = null;
                if (clienteEmail != null && !clienteEmail.isEmpty()) idCliente = getOrCreateClient(con, clienteEmail, clienteNombre);
                StringBuilder sb = new StringBuilder("UPDATE Pedido SET ");
                java.util.List<Object> params = new java.util.ArrayList<>();
                if (estado != null) { sb.append("estado = ?, "); params.add(estado); }
                if (prioridad != null) { sb.append("prioridad = ?, "); params.add(prioridad); }
                if (idCliente != null) { sb.append("id_cliente = ?, "); params.add(idCliente); }
                if (params.isEmpty()) { resp.getWriter().write("{\"ok\":false,\"error\":\"nothing to update\"}"); return; }
                int last = sb.lastIndexOf(","); if (last!=-1) sb.deleteCharAt(last);
                sb.append(" WHERE id_pedido = ?");
                try (PreparedStatement ps = con.prepareStatement(sb.toString())){
                    for (int i=0;i<params.size();i++) ps.setObject(i+1, params.get(i));
                    ps.setInt(params.size()+1, id);
                    int updated = ps.executeUpdate();
                    resp.getWriter().write("{\"ok\":true,\"updated\":"+updated+"}"); return;
                }
            } else if ("delete".equalsIgnoreCase(action)) {
                String idStr = req.getParameter("id"); if (idStr==null){ resp.getWriter().write("{\"ok\":false,\"error\":\"missing id\"}"); return; }
                int id = Integer.parseInt(idStr);
                try (PreparedStatement ps = con.prepareStatement("DELETE FROM Pedido WHERE id_pedido = ?")){
                    ps.setInt(1, id); int d = ps.executeUpdate(); resp.getWriter().write("{\"ok\":true,\"deleted\":"+d+"}"); return;
                }
            } else if ("addItem".equalsIgnoreCase(action)) {
                String idPedidoStr = req.getParameter("id_pedido"); String idProductoStr = req.getParameter("id_producto"); String cantStr = req.getParameter("cantidad");
                if (idPedidoStr==null || idProductoStr==null || cantStr==null){ resp.getWriter().write("{\"ok\":false,\"error\":\"missing params\"}"); return; }
                int idPedido = Integer.parseInt(idPedidoStr); int idProducto = Integer.parseInt(idProductoStr); int cantidad = Integer.parseInt(cantStr);
                boolean hasPrecioUnitario = hasColumn(con, "Detalle_Pedido", "precio_unitario");
                if (hasPrecioUnitario) {
                    String precioStr = req.getParameter("precio_unitario"); BigDecimal precio = precioStr!=null && !precioStr.isEmpty() ? new BigDecimal(precioStr) : BigDecimal.ZERO;
                    try (PreparedStatement ps = con.prepareStatement("INSERT INTO Detalle_Pedido (id_pedido, id_producto, cantidad_solicitada, precio_unitario) VALUES (?,?,?,?)", Statement.RETURN_GENERATED_KEYS)){
                        ps.setInt(1, idPedido); ps.setInt(2, idProducto); ps.setInt(3, cantidad); ps.setBigDecimal(4, precio); ps.executeUpdate(); try (ResultSet k=ps.getGeneratedKeys()){ if(k.next()){ resp.getWriter().write("{\"ok\":true,\"id\":"+k.getInt(1)+"}"); return; } }
                    }
                } else {
                    try (PreparedStatement ps = con.prepareStatement("INSERT INTO Detalle_Pedido (id_pedido, id_producto, cantidad_solicitada) VALUES (?,?,?)", Statement.RETURN_GENERATED_KEYS)){
                        ps.setInt(1, idPedido); ps.setInt(2, idProducto); ps.setInt(3, cantidad); ps.executeUpdate(); try (ResultSet k=ps.getGeneratedKeys()){ if(k.next()){ resp.getWriter().write("{\"ok\":true,\"id\":"+k.getInt(1)+"}"); return; } }
                    }
                }
                resp.getWriter().write("{\"ok\":false}"); return;
            } else if ("removeItem".equalsIgnoreCase(action)) {
                String idStr = req.getParameter("id_detalle"); if (idStr==null){ resp.getWriter().write("{\"ok\":false,\"error\":\"missing id_detalle\"}"); return; }
                int id = Integer.parseInt(idStr);
                try (PreparedStatement ps = con.prepareStatement("DELETE FROM Detalle_Pedido WHERE id_detalle = ?")){
                    ps.setInt(1, id); int d = ps.executeUpdate(); resp.getWriter().write("{\"ok\":true,\"deleted\":"+d+"}"); return;
                }
            } else if ("updateItem".equalsIgnoreCase(action)) {
                String idStr = req.getParameter("id_detalle"); if (idStr==null){ resp.getWriter().write("{\"ok\":false,\"error\":\"missing id_detalle\"}"); return; }
                int id = Integer.parseInt(idStr);
                String cantStr = req.getParameter("cantidad_solicitada"); String cantPrepStr = req.getParameter("cantidad_preparada"); String precioStr = req.getParameter("precio_unitario");
                StringBuilder sb = new StringBuilder("UPDATE Detalle_Pedido SET "); java.util.List<Object> params = new java.util.ArrayList<>();
                if (cantStr!=null){ sb.append("cantidad_solicitada = ?, "); params.add(Integer.parseInt(cantStr)); }
                if (cantPrepStr!=null){ sb.append("cantidad_preparada = ?, "); params.add(Integer.parseInt(cantPrepStr)); }
                if (precioStr!=null && hasColumn(con, "Detalle_Pedido", "precio_unitario")){ sb.append("precio_unitario = ?, "); params.add(new BigDecimal(precioStr)); }
                if (params.isEmpty()) { resp.getWriter().write("{\"ok\":false,\"error\":\"nothing to update\"}"); return; }
                int last = sb.lastIndexOf(","); if (last!=-1) sb.deleteCharAt(last); sb.append(" WHERE id_detalle = ?");
                try (PreparedStatement ps = con.prepareStatement(sb.toString())){
                    for (int i=0;i<params.size();i++) ps.setObject(i+1, params.get(i)); ps.setInt(params.size()+1, id); int u = ps.executeUpdate(); resp.getWriter().write("{\"ok\":true,\"updated\":"+u+"}"); return;
                }
            }

            resp.getWriter().write("{\"ok\":false,\"error\":\"unknown action\"}");
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private boolean hasColumn(Connection con, String table, String col) {
        try (PreparedStatement p = con.prepareStatement("SELECT * FROM " + table + " LIMIT 1")){
            try (ResultSet r = p.executeQuery()){
                ResultSetMetaData md = r.getMetaData();
                for (int i=1;i<=md.getColumnCount();i++) if (col.equalsIgnoreCase(md.getColumnLabel(i))) return true;
            }
        } catch (SQLException ex) {}
        return false;
    }

    private int getOrCreateClient(Connection con, String email, String nombre) throws SQLException {
        if (email != null && !email.isEmpty()){
            try (PreparedStatement ps = con.prepareStatement("SELECT id_cliente FROM Cliente WHERE email = ?")){
                ps.setString(1, email);
                try (ResultSet rs = ps.executeQuery()){ if (rs.next()) return rs.getInt(1); }
            }
            try (PreparedStatement ins = con.prepareStatement("INSERT INTO Cliente (nombre, email) VALUES (?,?)", Statement.RETURN_GENERATED_KEYS)){
                ins.setString(1, nombre != null && !nombre.isEmpty() ? nombre : email);
                ins.setString(2, email);
                ins.executeUpdate(); try (ResultSet k = ins.getGeneratedKeys()){ if (k.next()) return k.getInt(1); }
            }
        }
        return -1;
    }

    private String quote(String s) {
        if (s == null) return "\"\"";
        String safe = s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
        return "\"" + safe + "\"";
    }
}
