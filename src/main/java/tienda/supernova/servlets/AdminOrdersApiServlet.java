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
            String productIdCol = "id_producto";
            try {
                if (hasColumn(con, "Producto", "id_producto")) productIdCol = "id_producto";
                else if (hasColumn(con, "Producto", "producto_id")) productIdCol = "producto_id";
                else if (hasColumn(con, "Producto", "id")) productIdCol = "id";
            } catch (Exception _ex) {}
            String orderIdParam = req.getParameter("id");
            if (orderIdParam == null) orderIdParam = req.getParameter("orderId");
            if (orderIdParam != null && !orderIdParam.isEmpty()){
                int orderId = Integer.parseInt(orderIdParam);
                boolean hasPrecioUnitario = hasColumn(con, "Detalle_Pedido", "precio_unitario");
        String sqlItems = hasPrecioUnitario ?
            "SELECT dp.id_detalle, dp.id_producto, prod.nombre AS producto, dp.cantidad_solicitada, dp.cantidad_preparada, dp.precio_unitario FROM Detalle_Pedido dp LEFT JOIN Producto prod ON dp.id_producto = prod." + productIdCol + " WHERE dp.id_pedido = ?" :
            "SELECT dp.id_detalle, dp.id_producto, prod.nombre AS producto, dp.cantidad_solicitada, dp.cantidad_preparada FROM Detalle_Pedido dp LEFT JOIN Producto prod ON dp.id_producto = prod." + productIdCol + " WHERE dp.id_pedido = ?";
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

        String dateCol = "fecha_pedido";
        try {
        if (!hasColumn(con, "Pedido", "fecha_pedido") && hasColumn(con, "Pedido", "fecha_creacion")) dateCol = "fecha_creacion";
        } catch (Exception _e) {  }

    String sql = "SELECT p.id_pedido, p.id_cliente, p.id_usuario_gestiono, COALESCE(c.nombre, '(sin cliente)') AS cliente, p.estado, p." + dateCol + " AS fecha_pedido, p.fecha_entrega_estimada, p.fecha_entrega_real, p.direccion_envio, p.costo_envio, p.metodo_pago, p.prioridad, p.notas, "
        + totalExpr + " AS total "
            + "FROM Pedido p "
            + "LEFT JOIN Cliente c ON p.id_cliente = c.id_cliente "
        + "LEFT JOIN Detalle_Pedido dp ON dp.id_pedido = p.id_pedido "
        + "LEFT JOIN Producto prod ON dp.id_producto = prod." + productIdCol + " "
        + "GROUP BY p.id_pedido, p.id_cliente, p.id_usuario_gestiono, c.nombre, p.estado, p." + dateCol + ", p.fecha_entrega_estimada, p.fecha_entrega_real, p.direccion_envio, p.costo_envio, p.metodo_pago, p.prioridad, p.notas "
        + "ORDER BY p." + dateCol + " DESC";

            try (PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                StringBuilder sb = new StringBuilder();
                sb.append("[");
                boolean first = true;
                    while (rs.next()) {
                    if (!first) sb.append(','); first = false;
                    int id = rs.getInt("id_pedido");
                    int idCliente = rs.getInt("id_cliente"); if (rs.wasNull()) idCliente = -1;
                    int idUsuarioGestiono = -1; try { idUsuarioGestiono = rs.getInt("id_usuario_gestiono"); if (rs.wasNull()) idUsuarioGestiono = -1; } catch (Exception _e) {}
                    String cliente = rs.getString("cliente");
                    String estado = rs.getString("estado");
                    Timestamp ts = rs.getTimestamp("fecha_pedido");
                    String fecha = ts != null ? ts.toString() : null;
                    Timestamp tsEst = null; try { tsEst = rs.getTimestamp("fecha_entrega_estimada"); } catch (Exception _e) {}
                    String fechaEst = tsEst != null ? tsEst.toString() : null;
                    Timestamp tsReal = null; try { tsReal = rs.getTimestamp("fecha_entrega_real"); } catch (Exception _e) {}
                    String fechaReal = tsReal != null ? tsReal.toString() : null;
                    String direccion = null; try { direccion = rs.getString("direccion_envio"); } catch (Exception _e) {}
                    BigDecimal costoEnvio = null; try { costoEnvio = rs.getBigDecimal("costo_envio"); } catch (Exception _e) {}
                    String metodoPago = null; try { metodoPago = rs.getString("metodo_pago"); } catch (Exception _e) {}
                    String prioridadVal = null; try { prioridadVal = rs.getString("prioridad"); } catch (Exception _e) {}
                    String notas = null; try { notas = rs.getString("notas"); } catch (Exception _e) {}
                    BigDecimal total = null; try { total = rs.getBigDecimal("total"); } catch (Exception _e) {}

                    sb.append('{');
                    sb.append("\"id\":").append(id).append(',');
                    sb.append("\"id_cliente\":").append(idCliente).append(',');
                    sb.append("\"id_usuario_gestiono\":").append(idUsuarioGestiono).append(',');
                    sb.append("\"cliente\":").append(quote(cliente)).append(',');
                    sb.append("\"estado\":").append(quote(estado)).append(',');
                    sb.append("\"fecha\":").append(quote(fecha)).append(',');
                    sb.append("\"fecha_entrega_estimada\":").append(quote(fechaEst)).append(',');
                    sb.append("\"fecha_entrega_real\":").append(quote(fechaReal)).append(',');
                    sb.append("\"direccion_envio\":").append(quote(direccion)).append(',');
                    sb.append("\"costo_envio\":").append(costoEnvio==null?"0":costoEnvio).append(',');
                    sb.append("\"metodo_pago\":").append(quote(metodoPago)).append(',');
                    sb.append("\"prioridad\":").append(quote(prioridadVal)).append(',');
                    sb.append("\"notas\":").append(quote(notas)).append(',');
                    if (total == null) sb.append("\"total\":0"); else sb.append("\"total\":").append(total);
                    sb.append(',');
                    sb.append("\"totalIsItems\":").append(totalIsItems);
                    sb.append('}');
                }
                sb.append("]");
                if (!first) {
                    resp.getWriter().write(sb.toString());
                    return;
                }
            }

            try (PreparedStatement fps = con.prepareStatement("SELECT p.id_pedido, p.id_cliente, p.id_usuario_gestiono, COALESCE(c.nombre, '(sin cliente)') AS cliente, p.estado, p."+ (hasColumn(con, "Pedido", "fecha_pedido")?"fecha_pedido":"fecha_creacion") +" AS fecha_pedido, p.fecha_entrega_estimada, p.fecha_entrega_real, p.direccion_envio, p.costo_envio, p.metodo_pago, p.prioridad, p.notas FROM Pedido p LEFT JOIN Cliente c ON p.id_cliente = c.id_cliente ORDER BY fecha_pedido DESC");
                 ResultSet frs = fps.executeQuery()) {
                StringBuilder fsb = new StringBuilder(); fsb.append('[');
                boolean ffirst = true;
                while (frs.next()){
                    if (!ffirst) fsb.append(','); ffirst = false;
                    int id = frs.getInt("id_pedido");
                    int idCliente = frs.getInt("id_cliente"); if (frs.wasNull()) idCliente = -1;
                    int idUsuarioGestiono = -1; try { idUsuarioGestiono = frs.getInt("id_usuario_gestiono"); if (frs.wasNull()) idUsuarioGestiono = -1; } catch (Exception _e) {}
                    String cliente = frs.getString("cliente");
                    String estado = frs.getString("estado");
                    Timestamp ts = frs.getTimestamp("fecha_pedido");
                    String fecha = ts != null ? ts.toString() : null;
                    String fechaEst = null; try { Timestamp te = frs.getTimestamp("fecha_entrega_estimada"); if (te!=null) fechaEst = te.toString(); } catch (Exception _e) {}
                    String fechaReal = null; try { Timestamp tr = frs.getTimestamp("fecha_entrega_real"); if (tr!=null) fechaReal = tr.toString(); } catch (Exception _e) {}
                    String direccion = null; try { direccion = frs.getString("direccion_envio"); } catch (Exception _e) {}
                    BigDecimal costoEnv = null; try { costoEnv = frs.getBigDecimal("costo_envio"); } catch (Exception _e) {}
                    String metodoPago = null; try { metodoPago = frs.getString("metodo_pago"); } catch (Exception _e) {}
                    String prioridadVal = null; try { prioridadVal = frs.getString("prioridad"); } catch (Exception _e) {}
                    String notas = null; try { notas = frs.getString("notas"); } catch (Exception _e) {}
                    fsb.append('{');
                    fsb.append("\"id\":").append(id).append(',');
                    fsb.append("\"id_cliente\":").append(idCliente).append(',');
                    fsb.append("\"id_usuario_gestiono\":").append(idUsuarioGestiono).append(',');
                    fsb.append("\"cliente\":").append(quote(cliente)).append(',');
                    fsb.append("\"estado\":").append(quote(estado)).append(',');
                    fsb.append("\"fecha\":").append(quote(fecha)).append(',');
                    fsb.append("\"fecha_entrega_estimada\":").append(quote(fechaEst)).append(',');
                    fsb.append("\"fecha_entrega_real\":").append(quote(fechaReal)).append(',');
                    fsb.append("\"direccion_envio\":").append(quote(direccion)).append(',');
                    fsb.append("\"costo_envio\":").append(costoEnv==null?"0":costoEnv).append(',');
                    fsb.append("\"metodo_pago\":").append(quote(metodoPago)).append(',');
                    fsb.append("\"prioridad\":").append(quote(prioridadVal)).append(',');
                    fsb.append("\"notas\":").append(quote(notas)).append(',');
                    fsb.append("\"total\":0,");
                    fsb.append("\"totalIsItems\":true");
                    fsb.append('}');
                }
                fsb.append(']');
                resp.getWriter().write(fsb.toString());
                return;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.setContentType("application/json; charset=utf-8");
            resp.getWriter().write("[]");
            return;
        } catch (Exception e) {
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.setContentType("application/json; charset=utf-8");
            resp.getWriter().write("[]");
            return;
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json; charset=utf-8");
        req.setCharacterEncoding("utf-8");
        try (Connection con = DBConnection.getConnection()) {
            String action = req.getParameter("action");
            if (action == null) action = "";
            String productIdCol = "id_producto";
            try {
                if (hasColumn(con, "Producto", "id_producto")) productIdCol = "id_producto";
                else if (hasColumn(con, "Producto", "producto_id")) productIdCol = "producto_id";
                else if (hasColumn(con, "Producto", "id")) productIdCol = "id";
            } catch (Exception _ex) {}
            String stockCol = null;
            try {
                if (hasColumn(con, "Producto", "stock")) stockCol = "stock";
                else if (hasColumn(con, "Producto", "cantidad")) stockCol = "cantidad";
                else if (hasColumn(con, "Producto", "existencia")) stockCol = "existencia";
                else if (hasColumn(con, "Producto", "stock_actual")) stockCol = "stock_actual";
            } catch (Exception _ex) {}


            if ("create".equalsIgnoreCase(action)) {
                String clienteIdStr = req.getParameter("cliente_id");
                String estado = req.getParameter("estado"); if (estado==null) estado = "pendiente";
                String prioridad = req.getParameter("prioridad");
                String fechaEst = req.getParameter("fecha_entrega_estimada");
                String direccion = req.getParameter("direccion_envio");
                String costoEnv = req.getParameter("costo_envio");
                String metodoPago = req.getParameter("metodo_pago");
                String notas = req.getParameter("notas");
                Integer idCliente = null;
                if (clienteIdStr != null && !clienteIdStr.isEmpty()) {
                    try { idCliente = Integer.parseInt(clienteIdStr); } catch (Exception ex) { idCliente = null; }
                }
                if ((direccion == null || direccion.trim().isEmpty()) && idCliente != null) {
                    try (PreparedStatement p = con.prepareStatement("SELECT direccion FROM Cliente WHERE id_cliente = ?")){
                        p.setInt(1, idCliente);
                        try (ResultSet r = p.executeQuery()){ if (r.next()){ String d = r.getString(1); if (d != null && !d.trim().isEmpty()) direccion = d; } }
                    } catch (SQLException _ex) {  }
                }
                String sql = "INSERT INTO Pedido (id_cliente, estado, prioridad, fecha_entrega_estimada, direccion_envio, costo_envio, metodo_pago, notas) VALUES (?,?,?,?,?,?,?,?)";
                try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)){
                    if (idCliente != null) ps.setInt(1, idCliente); else ps.setNull(1, Types.INTEGER);
                    ps.setString(2, estado);
                    ps.setString(3, prioridad);
                    Timestamp tsFechaEst = parseTimestampInput(fechaEst);
                    if (tsFechaEst != null) ps.setTimestamp(4, tsFechaEst); else ps.setNull(4, Types.TIMESTAMP);
                    if (direccion != null && !direccion.isEmpty()) ps.setString(5, direccion); else ps.setNull(5, Types.VARCHAR);
                    if (costoEnv != null && !costoEnv.isEmpty()) { try { ps.setBigDecimal(6, new BigDecimal(costoEnv)); } catch(Exception _e){ ps.setNull(6, Types.DECIMAL); } } else ps.setNull(6, Types.DECIMAL);
                    if (metodoPago != null && !metodoPago.isEmpty()) ps.setString(7, metodoPago); else ps.setNull(7, Types.VARCHAR);
                    if (notas != null && !notas.isEmpty()) ps.setString(8, notas); else ps.setNull(8, Types.VARCHAR);
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
            } else if ("createWithItems".equalsIgnoreCase(action)) {
                String clienteIdStr = req.getParameter("cliente_id");
                String estado = req.getParameter("estado"); if (estado==null) estado = "pendiente";
                String prioridad = req.getParameter("prioridad");
                String fechaEst = req.getParameter("fecha_entrega_estimada");
                String direccion = req.getParameter("direccion_envio");
                String costoEnv = req.getParameter("costo_envio");
                String metodoPago = req.getParameter("metodo_pago");
                String notas = req.getParameter("notas");
                String[] itemIds = req.getParameterValues("item_id");
                String[] itemQtys = req.getParameterValues("item_cantidad");
                String[] itemPrices = req.getParameterValues("item_precio");
                Integer idCliente = null;
                if (clienteIdStr != null && !clienteIdStr.isEmpty()) {
                    try { idCliente = Integer.parseInt(clienteIdStr); } catch (Exception ex) { idCliente = null; }
                }
                try {
                    con.setAutoCommit(false);
                    String insertPedido = "INSERT INTO Pedido (id_cliente, estado, prioridad, fecha_entrega_estimada, direccion_envio, costo_envio, metodo_pago, notas) VALUES (?,?,?,?,?,?,?,?)";
                    try (PreparedStatement ps = con.prepareStatement(insertPedido, Statement.RETURN_GENERATED_KEYS)){
                        boolean costoNotNullable = false;
                        try (PreparedStatement pc = con.prepareStatement("SELECT costo_envio FROM Pedido LIMIT 1")){
                            try (ResultSet rc = pc.executeQuery()){
                                ResultSetMetaData rmd = rc.getMetaData();
                                if (rmd != null && rmd.getColumnCount()>=1){ int idx=1; int nullable = rmd.isNullable(idx); if (nullable==ResultSetMetaData.columnNoNulls) costoNotNullable = true; }
                            }
                        } catch (SQLException _ex) {  }
                        if (costoNotNullable && (costoEnv==null || costoEnv.trim().isEmpty())){
                            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                            resp.getWriter().write("{\"ok\":false,\"error\":\"validation\",\"message\":\"Complete los campos restantes: costo_envio\"}");
                            return;
                        }
                        if (idCliente != null) ps.setInt(1, idCliente); else ps.setNull(1, Types.INTEGER);
                        ps.setString(2, estado);
                        ps.setString(3, prioridad);
                        Timestamp tsFechaEst2 = parseTimestampInput(fechaEst);
                        if (tsFechaEst2 != null) ps.setTimestamp(4, tsFechaEst2); else ps.setNull(4, Types.TIMESTAMP);
                        if ((direccion == null || direccion.trim().isEmpty()) && idCliente != null) {
                            try (PreparedStatement p = con.prepareStatement("SELECT direccion FROM Cliente WHERE id_cliente = ?")){
                                p.setInt(1, idCliente);
                                try (ResultSet r = p.executeQuery()){ if (r.next()){ String d = r.getString(1); if (d != null && !d.trim().isEmpty()) direccion = d; } }
                            } catch (SQLException _ex) { }
                        }
                        if (direccion != null && !direccion.isEmpty()) ps.setString(5, direccion); else ps.setNull(5, Types.VARCHAR);
                        if (costoEnv != null && !costoEnv.isEmpty()) { try { ps.setBigDecimal(6, new BigDecimal(costoEnv)); } catch(Exception _e){ ps.setNull(6, Types.DECIMAL); } } else ps.setNull(6, Types.DECIMAL);
                        if (metodoPago != null && !metodoPago.isEmpty()) ps.setString(7, metodoPago); else ps.setNull(7, Types.VARCHAR);
                        if (notas != null && !notas.isEmpty()) ps.setString(8, notas); else ps.setNull(8, Types.VARCHAR);
                        ps.executeUpdate();
                        int orderId = -1;
                        try (ResultSet keys = ps.getGeneratedKeys()){ if (keys.next()) orderId = keys.getInt(1); }
                        if (orderId <= 0) throw new SQLException("no id generated for pedido");
                        if (itemIds != null && itemIds.length > 0) {
                            boolean detalleHasPrecio = false;
                            try (PreparedStatement pcheck = con.prepareStatement("SELECT * FROM Detalle_Pedido LIMIT 1")){
                                try (ResultSet r = pcheck.executeQuery()){
                                    ResultSetMetaData md = r.getMetaData();
                                    for (int i=1;i<=md.getColumnCount();i++){ if ("precio_unitario".equalsIgnoreCase(md.getColumnLabel(i))){ detalleHasPrecio = true; break; } }
                                }
                            } catch (SQLException ignore) { detalleHasPrecio = true; }

                            String insertItemSql = detalleHasPrecio ?
                                    "INSERT INTO Detalle_Pedido (id_pedido, id_producto, cantidad_solicitada, precio_unitario) VALUES (?,?,?,?)" :
                                    "INSERT INTO Detalle_Pedido (id_pedido, id_producto, cantidad_solicitada) VALUES (?,?,?)";
                            try (PreparedStatement pis = con.prepareStatement(insertItemSql)){
                                for (int i=0;i<itemIds.length;i++){
                                    String pid = itemIds[i];
                                    String qty = (itemQtys != null && itemQtys.length>i) ? itemQtys[i] : "1";
                                    String pr = (itemPrices != null && itemPrices.length>i) ? itemPrices[i] : null;
                                    int pidInt = -1; int qtyInt = 1;
                                    try { pidInt = Integer.parseInt(pid); } catch (Exception _e) { throw new SQLException("invalid product id: " + pid); }
                                    try { qtyInt = Integer.parseInt(qty); } catch (Exception _e) { qtyInt = 1; }
                                    if (stockCol != null) {
                                        try (PreparedStatement psStock = con.prepareStatement("SELECT " + stockCol + " FROM Producto WHERE " + productIdCol + " = ? FOR UPDATE")){
                                            psStock.setInt(1, pidInt);
                                            try (ResultSet rs2 = psStock.executeQuery()){
                                                if (!rs2.next()) throw new SQLException("product not found: " + pid);
                                                int cur = rs2.getInt(1); if (rs2.wasNull()) cur = 0;
                                                if (cur < qtyInt) throw new SQLException("insufficient stock for product " + pid);
                                            }
                                        }
                                        try (PreparedStatement psUpd = con.prepareStatement("UPDATE Producto SET " + stockCol + " = " + stockCol + " - ? WHERE " + productIdCol + " = ?")){
                                            psUpd.setInt(1, qtyInt); psUpd.setInt(2, pidInt); psUpd.executeUpdate();
                                        }
                                    }
                                    if (detalleHasPrecio){ pis.setInt(1, orderId); pis.setInt(2, pidInt); pis.setInt(3, qtyInt); if (pr==null || pr.isEmpty()) pis.setBigDecimal(4, null); else pis.setBigDecimal(4, new java.math.BigDecimal(pr)); pis.executeUpdate(); }
                                    else { pis.setInt(1, orderId); pis.setInt(2, pidInt); pis.setInt(3, qtyInt); pis.executeUpdate(); }
                                }
                            }
                        }
                        con.commit();
                        resp.getWriter().write("{\"ok\":true,\"id\":"+orderId+"}");
                        return;
                    }
                } catch (Exception ex) {
                    try { con.rollback(); } catch (Exception _e) {}
                    String msg = ex.getMessage()!=null?ex.getMessage() : "error";
                    if (msg.toLowerCase().contains("insufficient stock")){
                        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        try { resp.getWriter().write("{\"ok\":false,\"error\":\"insufficient_stock\",\"message\":\""+msg.replace('"','\'')+"\"}"); } catch (IOException _io){}
                    } else if (msg.toLowerCase().contains("product not found")){
                        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        try { resp.getWriter().write("{\"ok\":false,\"error\":\"product_not_found\",\"message\":\""+msg.replace('"','\'')+"\"}"); } catch (IOException _io){}
                    } else {
                        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        try { resp.getWriter().write("{\"ok\":false,\"error\":\"db\",\"message\":\""+msg.replace('"','\'')+"\"}"); } catch (IOException _io){}
                    }
                    ex.printStackTrace();
                    return;
                } finally {
                    try { con.setAutoCommit(true); } catch (Exception _e) {}
                }
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
                try {
                    con.setAutoCommit(false);
                    if (stockCol != null) {
                        String detProdCol = null, detQtyCol = null;
                        try {
                            if (hasColumn(con, "Detalle_Pedido", "id_producto")) detProdCol = "id_producto";
                            else if (hasColumn(con, "Detalle_Pedido", "producto_id")) detProdCol = "producto_id";
                            else if (hasColumn(con, "Detalle_Pedido", "id")) detProdCol = "id";
                            if (hasColumn(con, "Detalle_Pedido", "cantidad_solicitada")) detQtyCol = "cantidad_solicitada";
                            else if (hasColumn(con, "Detalle_Pedido", "cantidad")) detQtyCol = "cantidad";
                            else if (hasColumn(con, "Detalle_Pedido", "qty")) detQtyCol = "qty";
                        } catch (Exception _ex) { }
                        if (detProdCol != null && detQtyCol != null) {
                            String sel = "SELECT " + detProdCol + ", " + detQtyCol + " FROM Detalle_Pedido WHERE id_pedido = ?";
                            try (PreparedStatement psGet = con.prepareStatement(sel)){
                                psGet.setInt(1, id);
                                try (ResultSet rs = psGet.executeQuery()){
                                    try (PreparedStatement psUpd = con.prepareStatement("UPDATE Producto SET " + stockCol + " = COALESCE(" + stockCol + ",0) + ? WHERE " + productIdCol + " = ?")){
                                        while (rs.next()){
                                            int pid = rs.getInt(detProdCol); int qty = rs.getInt(detQtyCol); psUpd.setInt(1, qty); psUpd.setInt(2, pid); psUpd.addBatch();
                                        }
                                        psUpd.executeBatch();
                                    }
                                }
                            }
                        }
                    }
                    try (PreparedStatement psDelDet = con.prepareStatement("DELETE FROM Detalle_Pedido WHERE id_pedido = ?")){
                        psDelDet.setInt(1, id); psDelDet.executeUpdate();
                    }
                    int deleted = 0;
                    try (PreparedStatement ps = con.prepareStatement("DELETE FROM Pedido WHERE id_pedido = ?")){
                        ps.setInt(1, id); deleted = ps.executeUpdate();
                    }
                    con.commit();
                    resp.getWriter().write("{\"ok\":true,\"deleted\":"+deleted+"}"); return;
                } catch (Exception ex) {
                    try { con.rollback(); } catch (Exception _e) {}
                    String msg = ex.getMessage()!=null?ex.getMessage() : "error";
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    try { resp.getWriter().write("{\"ok\":false,\"error\":\"delete_failed\",\"message\":\"" + msg.replace('"','\'') + "\"}"); } catch (IOException _io) {}
                    ex.printStackTrace();
                    return;
                } finally {
                    try { con.setAutoCommit(true); } catch (Exception _e) {}
                }
            } else if ("addItem".equalsIgnoreCase(action)) {
                String idPedidoStr = req.getParameter("id_pedido"); String idProductoStr = req.getParameter("id_producto"); String cantStr = req.getParameter("cantidad");
                if (idPedidoStr==null || idProductoStr==null || cantStr==null){ resp.getWriter().write("{\"ok\":false,\"error\":\"missing params\"}"); return; }
                int idPedido = Integer.parseInt(idPedidoStr); int idProducto = Integer.parseInt(idProductoStr); int cantidad = Integer.parseInt(cantStr);
                boolean hasPrecioUnitario = hasColumn(con, "Detalle_Pedido", "precio_unitario");
                if (hasPrecioUnitario) {
                    String precioStr = req.getParameter("precio_unitario"); BigDecimal precio = precioStr!=null && !precioStr.isEmpty() ? new BigDecimal(precioStr) : BigDecimal.ZERO;
                    if (stockCol != null) {
                        try (PreparedStatement psStock = con.prepareStatement("SELECT " + stockCol + " FROM Producto WHERE " + productIdCol + " = ? FOR UPDATE")){
                            psStock.setInt(1, idProducto);
                            try (ResultSet rs2 = psStock.executeQuery()){
                                if (!rs2.next()) { resp.getWriter().write("{\"ok\":false,\"error\":\"product_not_found\"}"); return; }
                                int cur = rs2.getInt(1); if (rs2.wasNull()) cur = 0;
                                if (cur < cantidad) { resp.setStatus(HttpServletResponse.SC_BAD_REQUEST); resp.getWriter().write("{\"ok\":false,\"error\":\"insufficient_stock\",\"message\":\"stock available: " + cur + "\"}"); return; }
                            }
                        }
                        try (PreparedStatement psUpd = con.prepareStatement("UPDATE Producto SET " + stockCol + " = " + stockCol + " - ? WHERE " + productIdCol + " = ?")){
                            psUpd.setInt(1, cantidad); psUpd.setInt(2, idProducto); psUpd.executeUpdate();
                        }
                    }
                    try (PreparedStatement ps = con.prepareStatement("INSERT INTO Detalle_Pedido (id_pedido, id_producto, cantidad_solicitada, precio_unitario) VALUES (?,?,?,?)", Statement.RETURN_GENERATED_KEYS)){
                        ps.setInt(1, idPedido); ps.setInt(2, idProducto); ps.setInt(3, cantidad); ps.setBigDecimal(4, precio); ps.executeUpdate(); try (ResultSet k=ps.getGeneratedKeys()){ if(k.next()){ resp.getWriter().write("{\"ok\":true,\"id\":"+k.getInt(1)+"}"); return; } }
                    }
                } else {
                    if (stockCol != null) {
                        try (PreparedStatement psStock = con.prepareStatement("SELECT " + stockCol + " FROM Producto WHERE " + productIdCol + " = ? FOR UPDATE")){
                            psStock.setInt(1, idProducto);
                            try (ResultSet rs2 = psStock.executeQuery()){
                                if (!rs2.next()) { resp.getWriter().write("{\"ok\":false,\"error\":\"product_not_found\"}"); return; }
                                int cur = rs2.getInt(1); if (rs2.wasNull()) cur = 0;
                                if (cur < cantidad) { resp.setStatus(HttpServletResponse.SC_BAD_REQUEST); resp.getWriter().write("{\"ok\":false,\"error\":\"insufficient_stock\",\"message\":\"stock available: " + cur + "\"}"); return; }
                            }
                        }
                        try (PreparedStatement psUpd = con.prepareStatement("UPDATE Producto SET " + stockCol + " = " + stockCol + " - ? WHERE " + productIdCol + " = ?")){
                            psUpd.setInt(1, cantidad); psUpd.setInt(2, idProducto); psUpd.executeUpdate();
                        }
                    }
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

    private Timestamp parseTimestampInput(String s) {
        if (s == null) return null;
        s = s.trim(); if (s.isEmpty()) return null;
        String f = s;
        try {
            if (f.indexOf('T') != -1) {
                f = f.replace('T', ' ');
                if (f.length() == 16) f = f + ":00";
            } else if (f.length() == 10) {
                f = f + " 00:00:00";
            }
            return Timestamp.valueOf(f);
        } catch (Exception e) {
            return null;
        }
    }

    private String quote(String s) {
        if (s == null) return "\"\"";
        String safe = s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
        return "\"" + safe + "\"";
    }
}
