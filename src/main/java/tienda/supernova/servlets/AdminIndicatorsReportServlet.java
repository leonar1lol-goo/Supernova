package tienda.supernova.servlets;

import tienda.supernova.db.DBConnection;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import com.lowagie.text.Document;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.Image;

@WebServlet(name = "AdminIndicatorsReportServlet", urlPatterns = {"/admin/report/indicators"})
public class AdminIndicatorsReportServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        jakarta.servlet.http.HttpSession session = req.getSession(false);
        String role = session != null ? (String) session.getAttribute("role") : null;
        if (role == null || !role.equalsIgnoreCase("admin")) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            resp.setContentType("text/plain;charset=UTF-8");
            resp.getWriter().print("forbidden");
            return;
        }

        String from = req.getParameter("from");
        String to = req.getParameter("to");
        if (from == null || to == null || from.trim().isEmpty() || to.trim().isEmpty()) {
            resp.setContentType("text/plain;charset=UTF-8");
            resp.getWriter().print("Se requiere parámetros 'from' y 'to' (YYYY-MM-DD...)");
            return;
        }

        try (Connection con = DBConnection.getConnection()) {
            String[] orderCandidates = new String[]{"pedido","pedidos","order","orders"};
            String orderTable = null;
            for (String t : orderCandidates) if (tableExists(con, t)) { orderTable = t; break; }
            if (orderTable == null) {
                resp.setContentType("text/plain;charset=UTF-8");
                resp.getWriter().print("No se encontró tabla de pedidos");
                return;
            }

            Set<String> orderCols = getColumns(con, orderTable);

            String[] dateCandidates = new String[]{"fecha","fecha_pedido","fecha_creacion","created_at","fecha_registro","fecha_envio","fecha_entrega"};
            String orderDateCol = pickExisting(orderCols, dateCandidates);

            String[] estCandidates = new String[]{"fecha_estimada","fecha_prometida","fecha_prometida_cliente","fecha_entrega_estimada","promised_date","fecha_promesa"};
            String fechaEstCol = pickExisting(orderCols, estCandidates);

            String[] realCandidates = new String[]{"fecha_real","fecha_entrega","fecha_entregado","delivered_at","fecha_entrega_real"};
            String fechaRealCol = pickExisting(orderCols, realCandidates);

            String statusCol = pickExisting(orderCols, new String[]{"estado","status","estado_pedido","order_status","state"});
            String totalCol = pickExisting(orderCols, new String[]{"total","total_pedido","monto","importe","amount","order_total"});
            String idCol = pickExisting(orderCols, new String[]{"id_pedido","id","order_id","id_order"});

            DecimalFormat df2 = new DecimalFormat("#,##0.00");
            DecimalFormat dfInt = new DecimalFormat("#,##0");

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document doc = new Document();
            PdfWriter.getInstance(doc, baos);
            doc.open();

            com.lowagie.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            com.lowagie.text.Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            com.lowagie.text.Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 9);

            try {
                URL logoUrl = getServletContext().getResource("/images/logo.png");
                if (logoUrl != null) {
                    Image logo = Image.getInstance(logoUrl);
                    logo.scaleToFit(72,72);
                    PdfPTable header = new PdfPTable(new float[]{1f,3f});
                    header.setWidthPercentage(100);
                    PdfPCell cLogo = new PdfPCell(logo, false); cLogo.setBorder(Rectangle.NO_BORDER);
                    header.addCell(cLogo);
                    PdfPCell cRight = new PdfPCell(); cRight.setBorder(Rectangle.NO_BORDER);
                    cRight.addElement(new Phrase("REPORTE: INDICADORES - PEDIDOS", titleFont));
                    cRight.addElement(new Phrase("Rango: "+from+"  -  "+to, normalFont));
                    cRight.addElement(new Phrase("Generado: "+new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()), normalFont));
                    header.addCell(cRight);
                    doc.add(header);
                } else {
                    doc.add(new Paragraph("REPORTE: INDICADORES - PEDIDOS", titleFont));
                    doc.add(new Paragraph("Rango: "+from+"  -  "+to, normalFont));
                }
            } catch (Exception e) { doc.add(new Paragraph("REPORTE: INDICADORES - PEDIDOS", titleFont)); }

            doc.add(new Paragraph(" "));

            doc.add(new Paragraph("1. Porcentaje de Pedidos Entregados a Tiempo", titleFont));
            doc.add(new Paragraph("Este indicador mide la eficiencia comparando fecha real vs fecha estimada.", normalFont));

            String pctDisplay = "N/A";
            if (fechaRealCol!=null && fechaEstCol!=null && orderDateCol!=null) {
                String sql = "SELECT COUNT(*) AS delivered_count, SUM(CASE WHEN o."+fechaRealCol+" <= o."+fechaEstCol+" THEN 1 ELSE 0 END) AS ontime_count FROM "+orderTable+" o WHERE o."+orderDateCol+" BETWEEN ? AND ? AND o."+fechaRealCol+" IS NOT NULL";
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, from); ps.setString(2, to);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            long delivered = rs.getLong("delivered_count");
                            long ontime = rs.getLong("ontime_count");
                            if (delivered>0) {
                                long pct = Math.round((ontime * 100.0d) / (double) delivered);
                                pctDisplay = pct+"% ("+ontime+"/"+delivered+")";
                            } else pctDisplay = "N/A (no hay pedidos entregados en el rango)";
                        }
                    }
                } catch (Exception ex) { pctDisplay = "N/A"; }
            } else {
                pctDisplay = "N/A (falta columna fecha estimada/real/fecha pedido)";
            }

            PdfPTable t1 = new PdfPTable(new float[]{6f,4f}); t1.setWidthPercentage(90);
            t1.addCell(new PdfPCell(new Phrase("Indicador", labelFont)));
            t1.addCell(new PdfPCell(new Phrase("Valor", labelFont)));
            t1.addCell(new PdfPCell(new Phrase("Porcentaje de Pedidos Entregados a Tiempo", normalFont)));
            t1.addCell(new PdfPCell(new Phrase(pctDisplay, normalFont)));
            doc.add(t1);

            doc.add(new Paragraph(" "));

            doc.add(new Paragraph("2. Valor Promedio del Pedido (VPP)", titleFont));
            doc.add(new Paragraph("Promedio del total por pedido en el rango.", normalFont));

            String vppDisplay = "N/A";
            if (orderDateCol!=null) {
                if (totalCol!=null) {
                    String sql = "SELECT AVG(o."+totalCol+") AS avgv, COUNT(*) AS cnt FROM "+orderTable+" o WHERE o."+orderDateCol+" BETWEEN ? AND ?";
                    try (PreparedStatement ps = con.prepareStatement(sql)) {
                        ps.setString(1, from); ps.setString(2, to);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                Object av = rs.getObject("avgv"); long cnt = rs.getLong("cnt");
                                if (av!=null) vppDisplay = "S/."+df2.format(((Number)av).doubleValue())+" (count="+cnt+")";
                                else vppDisplay = "N/A";
                            }
                        }
                    } catch (Exception ex) { vppDisplay = "N/A"; }
                } else {
                    String[] itemCandidates = new String[]{"detalle_pedido","detalle_pedidos","detallepedido","order_item","order_items","detalle"};
                    String itemTable = null;
                    for (String t: itemCandidates) if (tableExists(con,t)) { itemTable = t; break; }
                    if (itemTable!=null) {
                        Set<String> itemCols = getColumns(con, itemTable);
                        String[] idCandidates = new String[]{"id_pedido","id","order_id","id_order"};
                        String itemOrderIdCol = pickExisting(itemCols, idCandidates);
                        String itemQtyCol = pickExisting(itemCols, new String[]{"cantidad_solicitada","cantidad","qty","quantity"});
                        String itemPriceCol = pickExisting(itemCols, new String[]{"precio_unitario","precio","unit_price","price"});

                        if (itemOrderIdCol!=null && itemQtyCol!=null && itemPriceCol!=null && idCol!=null) {
                            String sql = "SELECT AVG(t.order_sum) AS avgv, COUNT(*) AS cnt FROM (SELECT o."+idCol+" AS oid, SUM(COALESCE(i."+itemQtyCol+",0) * COALESCE(i."+itemPriceCol+",0)) AS order_sum FROM "+itemTable+" i JOIN "+orderTable+" o ON i."+itemOrderIdCol+" = o."+idCol+" WHERE o."+orderDateCol+" BETWEEN ? AND ? GROUP BY o."+idCol+") t";
                            try (PreparedStatement ps = con.prepareStatement(sql)) {
                                ps.setString(1, from); ps.setString(2, to);
                                try (ResultSet rs = ps.executeQuery()) {
                                    if (rs.next()) {
                                        Object av = rs.getObject("avgv"); long cnt = rs.getLong("cnt");
                                        if (av!=null) vppDisplay = "S/."+df2.format(((Number)av).doubleValue())+" (count="+cnt+")";
                                    }
                                }
                            } catch (Exception ex) { vppDisplay = "N/A"; }
                        }
                    }
                }
            }

            PdfPTable t2 = new PdfPTable(new float[]{6f,4f}); t2.setWidthPercentage(90);
            t2.addCell(new PdfPCell(new Phrase("Indicador", labelFont)));
            t2.addCell(new PdfPCell(new Phrase("Valor", labelFont)));
            t2.addCell(new PdfPCell(new Phrase("Valor Promedio del Pedido (VPP)", normalFont)));
            t2.addCell(new PdfPCell(new Phrase(vppDisplay, normalFont)));
            doc.add(t2);

            doc.add(new Paragraph(" "));

            doc.add(new Paragraph("3. Tasa de Pedidos Pendientes", titleFont));
            doc.add(new Paragraph("Porcentaje de pedidos en estado 'Pendiente' respecto al total en el rango.", normalFont));

            String pendDisplay = "N/A";
            if (orderDateCol!=null) {
                try {
                    long totalOrders = 0; long pendCount = 0;
                    String sqlTotal = "SELECT COUNT(*) AS cnt FROM "+orderTable+" o WHERE o."+orderDateCol+" BETWEEN ? AND ?";
                    try (PreparedStatement ps = con.prepareStatement(sqlTotal)) { ps.setString(1, from); ps.setString(2, to); try (ResultSet rs = ps.executeQuery()) { if (rs.next()) totalOrders = rs.getLong("cnt"); } }

                    if (totalOrders>0) {
                        if (statusCol!=null) {
                            String sqlPend = "SELECT COUNT(*) AS cnt FROM "+orderTable+" o WHERE o."+orderDateCol+" BETWEEN ? AND ? AND LOWER(COALESCE(o."+statusCol+",'')) = 'pendiente'";
                            try (PreparedStatement ps = con.prepareStatement(sqlPend)) { ps.setString(1, from); ps.setString(2, to); try (ResultSet rs = ps.executeQuery()) { if (rs.next()) pendCount = rs.getLong("cnt"); } }
                        } else if (fechaRealCol!=null) {
                            String sqlPend = "SELECT COUNT(*) AS cnt FROM "+orderTable+" o WHERE o."+orderDateCol+" BETWEEN ? AND ? AND o."+fechaRealCol+" IS NULL";
                            try (PreparedStatement ps = con.prepareStatement(sqlPend)) { ps.setString(1, from); ps.setString(2, to); try (ResultSet rs = ps.executeQuery()) { if (rs.next()) pendCount = rs.getLong("cnt"); } }
                        }
                        long pct = Math.round((pendCount * 100.0d) / (double) totalOrders);
                        pendDisplay = pct+"% ("+pendCount+"/"+totalOrders+")";
                    } else pendDisplay = "N/A (no hay pedidos en el rango)";
                } catch (Exception ex) { pendDisplay = "N/A"; }
            }

            PdfPTable t3 = new PdfPTable(new float[]{6f,4f}); t3.setWidthPercentage(90);
            t3.addCell(new PdfPCell(new Phrase("Indicador", labelFont)));
            t3.addCell(new PdfPCell(new Phrase("Valor", labelFont)));
            t3.addCell(new PdfPCell(new Phrase("Tasa de Pedidos Pendientes", normalFont)));
            t3.addCell(new PdfPCell(new Phrase(pendDisplay, normalFont)));
            doc.add(t3);

            doc.close();

            resp.reset();
            resp.setContentType("application/pdf");
            resp.setHeader("Content-Disposition","inline; filename=indicators-report.pdf");
            byte[] pdfBytes = baos.toByteArray();
            resp.setContentLength(pdfBytes.length);
            try (OutputStream out = resp.getOutputStream()) { out.write(pdfBytes); out.flush(); }

        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private boolean tableExists(Connection con, String table) {
        try (PreparedStatement p = con.prepareStatement("SELECT * FROM " + table + " LIMIT 1")) {
            try (ResultSet r = p.executeQuery()) { return true; }
        } catch (SQLException ex) { return false; }
    }

    private Set<String> getColumns(Connection con, String table) {
        Set<String> cols = new HashSet<>();
        try (PreparedStatement p = con.prepareStatement("SELECT * FROM " + table + " LIMIT 1")) {
            try (ResultSet r = p.executeQuery()) {
                ResultSetMetaData md = r.getMetaData();
                for (int i=1;i<=md.getColumnCount();i++) cols.add(md.getColumnLabel(i).toLowerCase());
            }
        } catch (SQLException ex) {}
        return cols;
    }

    private String pickExisting(Set<String> cols, String[] candidates) {
        if (cols==null) return null;
        for (String c: candidates) {
            if (c==null) continue;
            if (cols.contains(c.toLowerCase())) return c;
        }
        return null;
    }
}
