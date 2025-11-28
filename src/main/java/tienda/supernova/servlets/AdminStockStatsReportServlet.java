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
import java.util.List;
import java.util.ArrayList;

import com.lowagie.text.Document;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.Image;

@WebServlet(name = "AdminStockStatsReportServlet", urlPatterns = {"/admin/report/stock-stats"})
public class AdminStockStatsReportServlet extends HttpServlet {

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

        try (Connection con = DBConnection.getConnection()) {
            String[] candidates = new String[] {"producto","productos","product","products"};
            String foundTable = null;
            for (String t : candidates) {
                if (tableExists(con, t)) { foundTable = t; break; }
            }
            if (foundTable == null) {
                resp.setContentType("text/plain;charset=UTF-8");
                resp.getWriter().print("No se encontró tabla de productos");
                return;
            }

            Set<String> cols = getColumns(con, foundTable);

            String[] stockCandidates = new String[]{"stock","stock_actual","cantidad","existencia","cantidad_disponible"};
            String[] priceCandidates = new String[]{"precio","precio_unitario","price","precio_venta","precio_lista"};

            String stockCol = pickExisting(cols, stockCandidates);
            String priceCol = pickExisting(cols, priceCandidates);
            String[] categoryCandidates = new String[]{"categoria","categoria_id","categoria_nombre","category","category_id","type","tipo"};
            String categoryCol = pickExisting(cols, categoryCandidates);

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
                    cRight.addElement(new Phrase("REPORTE: MÁXIMO / MÍNIMO / PROMEDIO - STOCK", titleFont));
                    cRight.addElement(new Phrase("Generado: "+new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()), normalFont));
                    header.addCell(cRight);
                    doc.add(header);
                } else {
                    doc.add(new Paragraph("REPORTE: MÁXIMO / MÍNIMO / PROMEDIO - STOCK", titleFont));
                    doc.add(new Paragraph("Generado: "+new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()), normalFont));
                }
            } catch (Exception e) {
                doc.add(new Paragraph("REPORTE: MÁXIMO / MÍNIMO / PROMEDIO - STOCK", titleFont));
            }

            doc.add(new Paragraph(" "));

            DecimalFormat df2 = new DecimalFormat("#,##0.00");
            DecimalFormat dfInt = new DecimalFormat("#,##0");

            String[] nameCandidates = new String[]{"nombre","producto","name","title","titulo","descripcion"};
            String nameCol = pickExisting(cols, nameCandidates);

            PdfPTable resumen = new PdfPTable(new float[]{3f,2f});
            resumen.setWidthPercentage(80);
            resumen.setSpacingBefore(6f);
            resumen.setSpacingAfter(6f);

            java.util.function.BiConsumer<String,String> addResumen = (label, val) -> {
                PdfPCell cl = new PdfPCell(new Phrase(label, labelFont)); cl.setBorder(Rectangle.NO_BORDER);
                PdfPCell cv = new PdfPCell(new Phrase(val, normalFont)); cv.setBorder(Rectangle.NO_BORDER);
                resumen.addCell(cl); resumen.addCell(cv);
            };

            if (stockCol != null) {
                try (PreparedStatement ps = con.prepareStatement("SELECT SUM(COALESCE("+stockCol+",0)) AS total_units FROM "+foundTable)) {
                    try (ResultSet rs = ps.executeQuery()) {
                        long totalUnits = 0;
                        if (rs.next()) totalUnits = rs.getLong("total_units");
                        addResumen.accept("Total de Unidades en Stock:", dfInt.format(totalUnits));
                    }
                }
            } else {
                addResumen.accept("Total de Unidades en Stock:", "N/A");
            }

            if (stockCol != null && priceCol != null) {
                try (PreparedStatement ps = con.prepareStatement("SELECT SUM(COALESCE("+stockCol+",0) * COALESCE("+priceCol+",0)) AS valor_total, SUM(COALESCE("+stockCol+",0)) AS total_units FROM "+foundTable)) {
                    try (ResultSet rs = ps.executeQuery()) {
                        double valorTotal = 0; long totalUnits = 0;
                        if (rs.next()) {
                            valorTotal = rs.getDouble("valor_total");
                            totalUnits = rs.getLong("total_units");
                        }
                        addResumen.accept("Valor Total del Inventario:", "S/."+df2.format(valorTotal));
                        String ponderado = totalUnits>0? ("S/."+df2.format(valorTotal/ (double) totalUnits)) : "N/A";
                        addResumen.accept("Precio Promedio Ponderado por Unidad:", ponderado);
                    }
                }
            } else {
                addResumen.accept("Valor Total del Inventario:", "N/A");
                addResumen.accept("Precio Promedio Ponderado por Unidad:", "N/A");
            }

            if (stockCol != null) {
                String labelName = (nameCol!=null?nameCol:"id");
                String sqlMax = "SELECT "+(nameCol!=null?nameCol:stockCol)+", "+stockCol+" AS s FROM "+foundTable+" ORDER BY COALESCE("+stockCol+",0) DESC LIMIT 1";
                try (PreparedStatement ps = con.prepareStatement(sqlMax); ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String nm = nameCol!=null? rs.getString(nameCol) : "#"+rs.getObject(1);
                        long s = rs.getLong("s");
                        addResumen.accept("Ítem con Más Stock:", (nm==null?"(sin nombre)":nm) + " ("+s+")");
                    }
                } catch (SQLException ex) { addResumen.accept("Ítem con Más Stock:", "N/A"); }

                String sqlMin = "SELECT "+(nameCol!=null?nameCol:stockCol)+", "+stockCol+" AS s FROM "+foundTable+" ORDER BY COALESCE("+stockCol+",0) ASC LIMIT 1";
                try (PreparedStatement ps = con.prepareStatement(sqlMin); ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String nm = nameCol!=null? rs.getString(nameCol) : "#"+rs.getObject(1);
                        long s = rs.getLong("s");
                        addResumen.accept("Ítem con Menos Stock:", (nm==null?"(sin nombre)":nm) + " ("+s+")");
                    }
                } catch (SQLException ex) { addResumen.accept("Ítem con Menos Stock:", "N/A"); }

                try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) AS c FROM "+foundTable+" WHERE COALESCE("+stockCol+",0)=0")) {
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            addResumen.accept("Número de Productos con Stock Cero:", String.valueOf(rs.getLong("c")));
                        }
                    }
                } catch (SQLException ex) { addResumen.accept("Número de Productos con Stock Cero:", "N/A"); }

                if (nameCol!=null) {
                    try (PreparedStatement ps = con.prepareStatement("SELECT "+nameCol+" FROM "+foundTable+" WHERE COALESCE("+stockCol+",0)=0 LIMIT 20")) {
                        try (ResultSet rs = ps.executeQuery()) {
                            StringBuilder sb = new StringBuilder(); int cnt=0;
                            while (rs.next()) {
                                if (cnt>0) sb.append(", ");
                                sb.append(rs.getString(1)); cnt++; if (cnt>=20) break;
                            }
                            addResumen.accept("Productos agotados:", sb.length()>0?sb.toString():"(ninguno)");
                        }
                    } catch (SQLException ex) { addResumen.accept("Productos agotados:", "N/A"); }
                }
            } else {
                addResumen.accept("Ítem con Más Stock:", "N/A");
                addResumen.accept("Ítem con Menos Stock:", "N/A");
                addResumen.accept("Número de Productos con Stock Cero:", "N/A");
                addResumen.accept("Productos agotados:", "N/A");
            }

            if (priceCol != null) {
                try (PreparedStatement ps = con.prepareStatement("SELECT MAX("+priceCol+") AS maxp, MIN("+priceCol+") AS minp FROM "+foundTable)) {
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            Object maxp = rs.getObject("maxp");
                            Object minp = rs.getObject("minp");
                            addResumen.accept("Precio de Mayor:", maxp==null?"":("S/."+df2.format(rs.getDouble("maxp"))));
                            addResumen.accept("Precio de Menor:", minp==null?"":("S/."+df2.format(rs.getDouble("minp"))));
                        }
                    }
                } catch (SQLException ex) { addResumen.accept("Precio de Mayor:", "N/A"); addResumen.accept("Precio de Menor:", "N/A"); }
            } else {
                addResumen.accept("Precio de Mayor:", "N/A");
                addResumen.accept("Precio de Menor:", "N/A");
            }

            String from = req.getParameter("from");
            String to = req.getParameter("to");
            boolean useOrdersRange = (from!=null && !from.trim().isEmpty() && to!=null && !to.trim().isEmpty());

            if (useOrdersRange) {
                String[] orderCandidates = new String[]{"pedido","pedidos","order","orders"};
                String[] itemCandidates = new String[]{"detalle_pedido","detalle_pedidos","detallepedido","order_item","order_items","detalle"};
                String orderTable = null, itemTable = null;
                for (String t: orderCandidates) if (tableExists(con,t)) { orderTable = t; break; }
                for (String t: itemCandidates) if (tableExists(con,t)) { itemTable = t; break; }

                if (orderTable!=null && itemTable!=null) {
                    Set<String> orderCols = getColumns(con, orderTable);
                    Set<String> itemCols = getColumns(con, itemTable);
                    String[] dateCandidates = new String[]{"fecha","fecha_pedido","fecha_creacion","created_at","fecha_registro","fecha_envio","fecha_entrega"};
                    String orderDateCol = pickExisting(orderCols, dateCandidates);
                    String[] idCandidates = new String[]{"id_pedido","id","order_id","id_order"};
                    String orderIdCol = pickExisting(orderCols, idCandidates);
                    String itemOrderIdCol = pickExisting(itemCols, idCandidates);
                    String itemQtyCol = pickExisting(itemCols, new String[]{"cantidad_solicitada","cantidad","qty","quantity","cantidad"});
                    String itemPriceCol = pickExisting(itemCols, new String[]{"precio_unitario","precio","unit_price","price","precio"});
                    String itemProdIdCol = pickExisting(itemCols, new String[]{"id_producto","producto_id","product_id","id_product","id"});

                    if (orderDateCol!=null && orderIdCol!=null && itemOrderIdCol!=null && (itemQtyCol!=null || itemPriceCol!=null)) {
                        try {
                            PdfPTable resumenOrders = new PdfPTable(new float[]{3f,2f});
                            resumenOrders.setWidthPercentage(80);
                            resumenOrders.setSpacingBefore(6f);
                            resumenOrders.setSpacingAfter(6f);

                            if (itemQtyCol!=null) {
                                String sumSql = "SELECT SUM(COALESCE(i."+itemQtyCol+",0)) AS total_units" +
                                        (itemPriceCol!=null? ", SUM(COALESCE(i."+itemQtyCol+",0) * COALESCE(i."+itemPriceCol+",0)) AS valor_total" : "") +
                                        " FROM "+itemTable+" i JOIN "+orderTable+" o ON i."+itemOrderIdCol+" = o."+orderIdCol+" WHERE o."+orderDateCol+" BETWEEN ? AND ?";
                                try (PreparedStatement psSum = con.prepareStatement(sumSql)) {
                                    psSum.setString(1, from); psSum.setString(2, to);
                                    try (ResultSet rsSum = psSum.executeQuery()) {
                                        long totalUnits = 0; double valorTotal = 0d;
                                        if (rsSum.next()) {
                                            totalUnits = rsSum.getLong("total_units");
                                            if (itemPriceCol!=null) valorTotal = rsSum.getDouble("valor_total");
                                        }
                                        PdfPCell cl1 = new PdfPCell(new Phrase("Total de Unidades en Pedidos Seleccionados:", labelFont)); cl1.setBorder(Rectangle.NO_BORDER);
                                        PdfPCell cv1 = new PdfPCell(new Phrase(dfInt.format(totalUnits), normalFont)); cv1.setBorder(Rectangle.NO_BORDER);
                                        resumenOrders.addCell(cl1); resumenOrders.addCell(cv1);

                                        PdfPCell cl2 = new PdfPCell(new Phrase("Valor Total de Pedidos (cantidad*precio):", labelFont)); cl2.setBorder(Rectangle.NO_BORDER);
                                        PdfPCell cv2 = new PdfPCell(new Phrase(itemPriceCol!=null? ("S/."+df2.format(valorTotal)) : "N/A", normalFont)); cv2.setBorder(Rectangle.NO_BORDER);
                                        resumenOrders.addCell(cl2); resumenOrders.addCell(cv2);

                                        PdfPCell cl3 = new PdfPCell(new Phrase("Precio Promedio Ponderado por Unidad (rango):", labelFont)); cl3.setBorder(Rectangle.NO_BORDER);
                                        PdfPCell cv3 = new PdfPCell(new Phrase((totalUnits>0 && itemPriceCol!=null)? ("S/."+df2.format(valorTotal/(double)totalUnits)) : "N/A", normalFont)); cv3.setBorder(Rectangle.NO_BORDER);
                                        resumenOrders.addCell(cl3); resumenOrders.addCell(cv3);
                                    }
                                }
                            } else {
                                PdfPCell cl1 = new PdfPCell(new Phrase("Total de Unidades en Pedidos Seleccionados:", labelFont)); cl1.setBorder(Rectangle.NO_BORDER);
                                PdfPCell cv1 = new PdfPCell(new Phrase("N/A", normalFont)); cv1.setBorder(Rectangle.NO_BORDER);
                                resumenOrders.addCell(cl1); resumenOrders.addCell(cv1);
                            }

                            if (itemQtyCol!=null) {
                                String joinProd = "";
                                String nameSel = "i."+itemProdIdCol;
                                String prodTableForName = null;
                                if (itemProdIdCol!=null) {
                                    String[] prodCandidatesLocal = new String[]{"producto","productos","product","products"};
                                    for (String t: prodCandidatesLocal) if (tableExists(con,t)) { prodTableForName = t; break; }
                                    if (prodTableForName!=null) {
                                        Set<String> prodColsLocal = getColumns(con, prodTableForName);
                                        String prodIdLocal = pickExisting(prodColsLocal, new String[]{"id","id_producto","id_product","producto_id"});
                                        String prodNameLocal = pickExisting(prodColsLocal, new String[]{"nombre","producto","name","title","titulo","descripcion","producto_nombre","nombre_producto"});
                                        if (prodIdLocal!=null && prodNameLocal!=null) {
                                            joinProd = " JOIN "+prodTableForName+" p ON p."+prodIdLocal+" = i."+itemProdIdCol;
                                            nameSel = "p."+prodNameLocal;
                                        }
                                    }
                                }

                                String sqlMost = "SELECT "+nameSel+" AS name, SUM(COALESCE(i."+itemQtyCol+",0)) AS s FROM "+itemTable+" i JOIN "+orderTable+" o ON i."+itemOrderIdCol+" = o."+orderIdCol + joinProd + " WHERE o."+orderDateCol+" BETWEEN ? AND ? GROUP BY " + nameSel + " ORDER BY s DESC LIMIT 1";
                                try (PreparedStatement psM = con.prepareStatement(sqlMost)) {
                                    psM.setString(1, from); psM.setString(2, to);
                                    try (ResultSet rm = psM.executeQuery()) {
                                        if (rm.next()) {
                                            String name = rm.getString("name"); long s = rm.getLong("s");
                                            PdfPCell cl = new PdfPCell(new Phrase("Ítem con Más (rango):", labelFont)); cl.setBorder(Rectangle.NO_BORDER);
                                            PdfPCell cv = new PdfPCell(new Phrase((name==null?"(sin nombre)":name)+" ("+dfInt.format(s)+")", normalFont)); cv.setBorder(Rectangle.NO_BORDER);
                                            resumenOrders.addCell(cl); resumenOrders.addCell(cv);
                                        } else { PdfPCell cl = new PdfPCell(new Phrase("Ítem con Más (rango):", labelFont)); cl.setBorder(Rectangle.NO_BORDER); PdfPCell cv = new PdfPCell(new Phrase("(ninguno)", normalFont)); cv.setBorder(Rectangle.NO_BORDER); resumenOrders.addCell(cl); resumenOrders.addCell(cv); }
                                    }
                                } catch (Exception ex) { PdfPCell cl = new PdfPCell(new Phrase("Ítem con Más (rango):", labelFont)); cl.setBorder(Rectangle.NO_BORDER); PdfPCell cv = new PdfPCell(new Phrase("N/A", normalFont)); cv.setBorder(Rectangle.NO_BORDER); resumenOrders.addCell(cl); resumenOrders.addCell(cv); }

                                String sqlMin = "SELECT "+nameSel+" AS name, SUM(COALESCE(i."+itemQtyCol+",0)) AS s FROM "+itemTable+" i JOIN "+orderTable+" o ON i."+itemOrderIdCol+" = o."+orderIdCol + joinProd + " WHERE o."+orderDateCol+" BETWEEN ? AND ? GROUP BY " + nameSel + " ORDER BY s ASC LIMIT 1";
                                try (PreparedStatement psm = con.prepareStatement(sqlMin)) {
                                    psm.setString(1, from); psm.setString(2, to);
                                    try (ResultSet rmin = psm.executeQuery()) {
                                        if (rmin.next()) { String name = rmin.getString("name"); long s = rmin.getLong("s"); PdfPCell cl = new PdfPCell(new Phrase("Ítem con Menos (rango):", labelFont)); cl.setBorder(Rectangle.NO_BORDER); PdfPCell cv = new PdfPCell(new Phrase((name==null?"(sin nombre)":name)+" ("+dfInt.format(s)+")", normalFont)); cv.setBorder(Rectangle.NO_BORDER); resumenOrders.addCell(cl); resumenOrders.addCell(cv); }
                                        else { PdfPCell cl = new PdfPCell(new Phrase("Ítem con Menos (rango):", labelFont)); cl.setBorder(Rectangle.NO_BORDER); PdfPCell cv = new PdfPCell(new Phrase("(ninguno)", normalFont)); cv.setBorder(Rectangle.NO_BORDER); resumenOrders.addCell(cl); resumenOrders.addCell(cv); }
                                    }
                                } catch (Exception ex) { PdfPCell cl = new PdfPCell(new Phrase("Ítem con Menos (rango):", labelFont)); cl.setBorder(Rectangle.NO_BORDER); PdfPCell cv = new PdfPCell(new Phrase("N/A", normalFont)); cv.setBorder(Rectangle.NO_BORDER); resumenOrders.addCell(cl); resumenOrders.addCell(cv); }
                            } else {
                                PdfPCell cl = new PdfPCell(new Phrase("Ítem con Más (rango):", labelFont)); cl.setBorder(Rectangle.NO_BORDER); PdfPCell cv = new PdfPCell(new Phrase("N/A", normalFont)); cv.setBorder(Rectangle.NO_BORDER); resumenOrders.addCell(cl); resumenOrders.addCell(cv);
                                PdfPCell clm = new PdfPCell(new Phrase("Ítem con Menos (rango):", labelFont)); clm.setBorder(Rectangle.NO_BORDER); PdfPCell cvm = new PdfPCell(new Phrase("N/A", normalFont)); cvm.setBorder(Rectangle.NO_BORDER); resumenOrders.addCell(clm); resumenOrders.addCell(cvm);
                            }

                            if (itemPriceCol!=null) {
                                String sqlP = "SELECT MAX(i."+itemPriceCol+") AS maxp, MIN(i."+itemPriceCol+") AS minp FROM "+itemTable+" i JOIN "+orderTable+" o ON i."+itemOrderIdCol+" = o."+orderIdCol+" WHERE o."+orderDateCol+" BETWEEN ? AND ?";
                                try (PreparedStatement psp = con.prepareStatement(sqlP)) {
                                    psp.setString(1, from); psp.setString(2, to);
                                    try (ResultSet rp = psp.executeQuery()) {
                                        if (rp.next()) { Object maxp = rp.getObject("maxp"); Object minp = rp.getObject("minp"); PdfPCell cmax = new PdfPCell(new Phrase("Precio de Mayor (rango):", labelFont)); cmax.setBorder(Rectangle.NO_BORDER); PdfPCell vmax = new PdfPCell(new Phrase(maxp==null?"":"S/."+df2.format(rp.getDouble("maxp")), normalFont)); vmax.setBorder(Rectangle.NO_BORDER); resumenOrders.addCell(cmax); resumenOrders.addCell(vmax);
                                            PdfPCell cmin = new PdfPCell(new Phrase("Precio de Menor (rango):", labelFont)); cmin.setBorder(Rectangle.NO_BORDER); PdfPCell vmin = new PdfPCell(new Phrase(minp==null?"":"S/."+df2.format(rp.getDouble("minp")), normalFont)); vmin.setBorder(Rectangle.NO_BORDER); resumenOrders.addCell(cmin); resumenOrders.addCell(vmin); }
                                    }
                                } catch (Exception ex) { PdfPCell cmax = new PdfPCell(new Phrase("Precio de Mayor (rango):", labelFont)); cmax.setBorder(Rectangle.NO_BORDER); PdfPCell vmax = new PdfPCell(new Phrase("N/A", normalFont)); vmax.setBorder(Rectangle.NO_BORDER); resumenOrders.addCell(cmax); resumenOrders.addCell(vmax); PdfPCell cmin = new PdfPCell(new Phrase("Precio de Menor (rango):", labelFont)); cmin.setBorder(Rectangle.NO_BORDER); PdfPCell vmin = new PdfPCell(new Phrase("N/A", normalFont)); vmin.setBorder(Rectangle.NO_BORDER); resumenOrders.addCell(cmin); resumenOrders.addCell(vmin); }
                            } else {
                                PdfPCell cmax = new PdfPCell(new Phrase("Precio de Mayor (rango):", labelFont)); cmax.setBorder(Rectangle.NO_BORDER); PdfPCell vmax = new PdfPCell(new Phrase("N/A", normalFont)); vmax.setBorder(Rectangle.NO_BORDER); resumenOrders.addCell(cmax); resumenOrders.addCell(vmax);
                                PdfPCell cmin = new PdfPCell(new Phrase("Precio de Menor (rango):", labelFont)); cmin.setBorder(Rectangle.NO_BORDER); PdfPCell vmin = new PdfPCell(new Phrase("N/A", normalFont)); vmin.setBorder(Rectangle.NO_BORDER); resumenOrders.addCell(cmin); resumenOrders.addCell(vmin);
                            }

                            if (itemProdIdCol!=null) {
                                String sqlDistinct = "SELECT COUNT(DISTINCT i."+itemProdIdCol+") AS cnt FROM "+itemTable+" i JOIN "+orderTable+" o ON i."+itemOrderIdCol+" = o."+orderIdCol+" WHERE o."+orderDateCol+" BETWEEN ? AND ?";
                                try (PreparedStatement pd = con.prepareStatement(sqlDistinct)) { pd.setString(1, from); pd.setString(2, to); try (ResultSet rd = pd.executeQuery()) { if (rd.next()) { PdfPCell c = new PdfPCell(new Phrase("Número de Productos distintos en rango:", labelFont)); c.setBorder(Rectangle.NO_BORDER); PdfPCell v = new PdfPCell(new Phrase(String.valueOf(rd.getLong("cnt")), normalFont)); v.setBorder(Rectangle.NO_BORDER); resumenOrders.addCell(c); resumenOrders.addCell(v); } } }
                            } else { PdfPCell c = new PdfPCell(new Phrase("Número de Productos distintos en rango:", labelFont)); c.setBorder(Rectangle.NO_BORDER); PdfPCell v = new PdfPCell(new Phrase("N/A", normalFont)); v.setBorder(Rectangle.NO_BORDER); resumenOrders.addCell(c); resumenOrders.addCell(v); }

                            doc.add(new Paragraph("Resumen:", titleFont));
                            doc.add(resumenOrders);
                        } catch (Exception ex) {
                            doc.add(new Paragraph("Resumen:", titleFont));
                            doc.add(resumen);
                        }

                        PdfPTable table = new PdfPTable(4);
                        table.setWidthPercentage(80);
                        table.setSpacingBefore(6f);
                        table.setSpacingAfter(6f);
                        table.addCell(new PdfPCell(new Phrase("Campo", labelFont)));
                        table.addCell(new PdfPCell(new Phrase("Mínimo", labelFont)));
                        table.addCell(new PdfPCell(new Phrase("Máximo", labelFont)));
                        table.addCell(new PdfPCell(new Phrase("Promedio", labelFont)));

                        String where = " o."+orderDateCol+" BETWEEN ? AND ? ";

                        if (itemQtyCol!=null) {
                            String sql = "SELECT COUNT(*) AS total, COUNT(i."+itemQtyCol+") AS nonnull, MAX(i."+itemQtyCol+") AS maxv, MIN(i."+itemQtyCol+") AS minv, AVG(i."+itemQtyCol+") AS avgv FROM "+itemTable+" i JOIN "+orderTable+" o ON i."+itemOrderIdCol+" = o."+orderIdCol+" WHERE "+where;
                            try (PreparedStatement ps = con.prepareStatement(sql)) {
                                ps.setString(1, from); ps.setString(2, to);
                                try (ResultSet rs = ps.executeQuery()) {
                                    if (rs.next()) {
                                        long nonnull = rs.getLong("nonnull");
                                        Object maxo = rs.getObject("maxv");
                                        Object mino = rs.getObject("minv");
                                        Object avgo = rs.getObject("avgv");
                                        table.addCell(new PdfPCell(new Phrase("Cantidad Pedida", normalFont)));
                                        table.addCell(new PdfPCell(new Phrase(formatNumber(mino, df2, dfInt), normalFont)));
                                        table.addCell(new PdfPCell(new Phrase(formatNumber(maxo, df2, dfInt), normalFont)));
                                        table.addCell(new PdfPCell(new Phrase(formatAverage(avgo, dfInt) + " (count="+nonnull+")", normalFont)));
                                    }
                                }
                            }
                        }

                        if (itemPriceCol!=null) {
                            String sql = "SELECT COUNT(*) AS total, COUNT(i."+itemPriceCol+") AS nonnull, MAX(i."+itemPriceCol+") AS maxv, MIN(i."+itemPriceCol+") AS minv, AVG(i."+itemPriceCol+") AS avgv FROM "+itemTable+" i JOIN "+orderTable+" o ON i."+itemOrderIdCol+" = o."+orderIdCol+" WHERE "+where;
                            try (PreparedStatement ps = con.prepareStatement(sql)) {
                                ps.setString(1, from); ps.setString(2, to);
                                try (ResultSet rs = ps.executeQuery()) {
                                    if (rs.next()) {
                                        long nonnull = rs.getLong("nonnull");
                                        Object maxo = rs.getObject("maxv");
                                        Object mino = rs.getObject("minv");
                                        Object avgo = rs.getObject("avgv");
                                        table.addCell(new PdfPCell(new Phrase("precio por unidad", normalFont)));
                                        table.addCell(new PdfPCell(new Phrase(formatNumber(mino, df2, dfInt), normalFont)));
                                        table.addCell(new PdfPCell(new Phrase(formatNumber(maxo, df2, dfInt), normalFont)));
                                        table.addCell(new PdfPCell(new Phrase(formatAverage(avgo, dfInt) + " (count="+nonnull+")", normalFont)));
                                    }
                                }
                            }
                        }

                        try {
                            String orderTotalColLocal = null;
                            if (orderCols != null) {
                                orderTotalColLocal = pickExisting(orderCols, new String[]{"total","total_pedido","monto","importe","amount","order_total"});
                            }

                            Double minv = null, maxv = null, avgv = null;
                            long cntOrders = 0;

                            if (orderTotalColLocal != null) {
                                String sql = "SELECT MIN(o."+orderTotalColLocal+") AS minv, MAX(o."+orderTotalColLocal+") AS maxv, AVG(o."+orderTotalColLocal+") AS avgv, COUNT(o."+orderTotalColLocal+") AS cnt FROM "+orderTable+" o WHERE "+where;
                                try (PreparedStatement ps = con.prepareStatement(sql)) {
                                    ps.setString(1, from); ps.setString(2, to);
                                    try (ResultSet rs = ps.executeQuery()) {
                                        if (rs.next()) {
                                            Object mn = rs.getObject("minv");
                                            Object mx = rs.getObject("maxv");
                                            Object av = rs.getObject("avgv");
                                            minv = mn==null?null:((Number)mn).doubleValue();
                                            maxv = mx==null?null:((Number)mx).doubleValue();
                                            avgv = av==null?null:((Number)av).doubleValue();
                                            cntOrders = rs.getLong("cnt");
                                        }
                                    }
                                }
                            } else if (itemQtyCol!=null && itemPriceCol!=null) {
                                String sql = "SELECT MIN(t.order_sum) AS minv, MAX(t.order_sum) AS maxv, AVG(t.order_sum) AS avgv, COUNT(*) AS cnt FROM (SELECT o."+orderIdCol+" AS oid, SUM(COALESCE(i."+itemQtyCol+",0) * COALESCE(i."+itemPriceCol+",0)) AS order_sum FROM "+itemTable+" i JOIN "+orderTable+" o ON i."+itemOrderIdCol+" = o."+orderIdCol+" WHERE o."+orderDateCol+" BETWEEN ? AND ? GROUP BY o."+orderIdCol+") t";
                                try (PreparedStatement ps = con.prepareStatement(sql)) {
                                    ps.setString(1, from); ps.setString(2, to);
                                    try (ResultSet rs = ps.executeQuery()) {
                                        if (rs.next()) {
                                            Object mn = rs.getObject("minv");
                                            Object mx = rs.getObject("maxv");
                                            Object av = rs.getObject("avgv");
                                            minv = mn==null?null:((Number)mn).doubleValue();
                                            maxv = mx==null?null:((Number)mx).doubleValue();
                                            avgv = av==null?null:((Number)av).doubleValue();
                                            cntOrders = rs.getLong("cnt");
                                        }
                                    }
                                }
                            }

                            if (minv!=null || maxv!=null || avgv!=null) {
                                String minDisplay = minv==null?"":"S/."+df2.format(minv);
                                String maxDisplay = maxv==null?"":"S/."+df2.format(maxv);
                                String avgDisplay = avgv==null?"":"S/."+dfInt.format(Math.round(avgv));
                                table.addCell(new PdfPCell(new Phrase("precio total", normalFont)));
                                table.addCell(new PdfPCell(new Phrase(minDisplay, normalFont)));
                                table.addCell(new PdfPCell(new Phrase(maxDisplay, normalFont)));
                                table.addCell(new PdfPCell(new Phrase((avgDisplay.isEmpty()?"":("S/."+avgDisplay)) + " (count="+cntOrders+")", normalFont)));
                            }
                        } catch (Exception ex) {}

                        String[] prodCandidates = new String[]{"producto","productos","product","products"};
                        String prodTable = null;
                        for (String t: prodCandidates) if (tableExists(con,t)) { prodTable = t; break; }
                        if (prodTable!=null && itemProdIdCol!=null) {
                            Set<String> prodCols = getColumns(con, prodTable);
                            String catCol = pickExisting(prodCols, categoryCandidates);
                            String prodIdCol = pickExisting(prodCols, new String[]{"id","id_producto","id_product"});
                            if (catCol!=null && prodIdCol!=null) {
                                String sql = "SELECT p."+catCol+" AS cat, COUNT(*) AS cnt FROM "+itemTable+" i JOIN "+orderTable+" o ON i."+itemOrderIdCol+" = o."+orderIdCol+" JOIN "+prodTable+" p ON p."+prodIdCol+" = i."+itemProdIdCol+" WHERE "+where+" GROUP BY p."+catCol;
                                try (PreparedStatement ps = con.prepareStatement(sql)) {
                                    ps.setString(1, from); ps.setString(2, to);
                                    try (ResultSet rs = ps.executeQuery()) {
                                        String minCat=null,maxCat=null; long minCnt=Long.MAX_VALUE,maxCnt=Long.MIN_VALUE; long totalCount=0; int categories=0;
                                        while (rs.next()) {
                                            String cat = rs.getString("cat"); long cnt = rs.getLong("cnt"); totalCount+=cnt; categories++; if (cnt<minCnt){minCnt=cnt;minCat=cat;} if(cnt>maxCnt){maxCnt=cnt;maxCat=cat;}
                                        }
                                        if (categories>0) {
                                            double avgPerCat = (double) totalCount / (double) categories;
                                            String minDisplay = (minCat==null?"":minCat)+" ("+minCnt+")";
                                            String maxDisplay = (maxCat==null?"":maxCat)+" ("+maxCnt+")";
                                            String avgDisplay = formatAverage(Double.valueOf(avgPerCat), df2)+" (categories="+categories+")";
                                            table.addCell(new PdfPCell(new Phrase(catCol, normalFont)));
                                            table.addCell(new PdfPCell(new Phrase(minDisplay, normalFont)));
                                            table.addCell(new PdfPCell(new Phrase(maxDisplay, normalFont)));
                                            table.addCell(new PdfPCell(new Phrase(avgDisplay, normalFont)));
                                        }
                                    }
                                }
                            }
                        }

                        doc.add(table);

                        String clientCol = pickExisting(orderCols, new String[]{"cliente_id","cliente","cliente_nombre","cliente_nombre_completo","cliente_id","cliente_nombre","cliente_email","email","nombre_cliente"});
                        String totalCol = pickExisting(orderCols, new String[]{"total","total_pedido","monto","importe","amount","order_total"});
                        PdfPTable ordersTable = new PdfPTable(new float[]{1f,2f,3f,2f});
                        ordersTable.setWidthPercentage(100);
                        ordersTable.addCell(new PdfPCell(new Phrase("ID Pedido", labelFont)));
                        ordersTable.addCell(new PdfPCell(new Phrase("Fecha", labelFont)));
                        ordersTable.addCell(new PdfPCell(new Phrase("Cliente", labelFont)));
                        ordersTable.addCell(new PdfPCell(new Phrase("Total", labelFont)));

                        String selectCols = orderIdCol + ", " + orderDateCol;
                        if (clientCol!=null) selectCols += ", "+clientCol;
                        if (totalCol!=null) selectCols += ", "+totalCol;
                        String sqlOrders = "SELECT " + selectCols + " FROM "+orderTable+" o WHERE " + where + " ORDER BY o."+orderDateCol+" ASC";

                        List<Object> selectedOrderIds = new ArrayList<>();
                        try (PreparedStatement ps2 = con.prepareStatement(sqlOrders)) {
                            ps2.setString(1, from); ps2.setString(2, to);
                            try (ResultSet rs2 = ps2.executeQuery()) {
                                boolean any = false;
                                while (rs2.next()) {
                                    any = true;
                                    Object oid = rs2.getObject(orderIdCol);
                                    selectedOrderIds.add(oid);
                                    Object ofe = rs2.getObject(orderDateCol);
                                    String cli = clientCol!=null? String.valueOf(rs2.getObject(clientCol)) : "";
                                    String tot = totalCol!=null? (rs2.getObject(totalCol)==null?"":("S/."+df2.format(rs2.getDouble(totalCol)))) : "";
                                    ordersTable.addCell(new PdfPCell(new Phrase(String.valueOf(oid), normalFont)));
                                    ordersTable.addCell(new PdfPCell(new Phrase(String.valueOf(ofe), normalFont)));
                                    ordersTable.addCell(new PdfPCell(new Phrase(cli, normalFont)));
                                    ordersTable.addCell(new PdfPCell(new Phrase(tot, normalFont)));
                                }
                                if (!any) { PdfPCell c = new PdfPCell(new Phrase("(No se encontraron pedidos en el rango)", normalFont)); c.setColspan(4); ordersTable.addCell(c); }
                            }
                        }

                        doc.add(new Paragraph(" "));
                        doc.add(new Paragraph("Pedidos seleccionados:", labelFont));
                        doc.add(ordersTable);

                        String[] prodCandidates2 = new String[]{"producto","productos","product","products"};
                        String prodTable2 = null;
                        for (String t: prodCandidates2) if (tableExists(con,t)) { prodTable2 = t; break; }
                        Set<String> prodCols2 = prodTable2!=null ? getColumns(con, prodTable2) : new HashSet<>();
                        String prodIdCol2 = prodTable2!=null ? pickExisting(prodCols2, new String[]{"id","id_producto","id_product","producto_id"}) : null;
                        String prodNameCol2 = prodTable2!=null ? pickExisting(prodCols2, new String[]{"nombre","producto","name","title","titulo","descripcion","nombre_completo","nombre_corto","producto_nombre","nombre_producto","descripcion_corta"}) : null;

                        for (Object soid : selectedOrderIds) {
                            doc.add(new Paragraph(" "));
                            doc.add(new Paragraph("Productos del pedido: " + String.valueOf(soid), labelFont));
                            PdfPTable itemsTable = new PdfPTable(new float[]{5f,1f,2f});
                            itemsTable.setWidthPercentage(100);
                            itemsTable.addCell(new PdfPCell(new Phrase("Producto", labelFont)));
                            itemsTable.addCell(new PdfPCell(new Phrase("Cantidad", labelFont)));
                            itemsTable.addCell(new PdfPCell(new Phrase("Precio", labelFont)));

                            String itemsSql;
                            if (prodTable2!=null && prodIdCol2!=null && itemProdIdCol!=null) {
                                String pname = prodNameCol2!=null? prodNameCol2 : prodIdCol2;
                                String qtySel = itemQtyCol!=null? ("i."+itemQtyCol+" AS qty") : "NULL AS qty";
                                String prSel = itemPriceCol!=null? ("i."+itemPriceCol+" AS pr") : "NULL AS pr";
                                itemsSql = "SELECT p."+pname+" AS pname, "+qtySel+", "+prSel+" FROM "+itemTable+" i LEFT JOIN "+prodTable2+" p ON p."+prodIdCol2+" = i."+itemProdIdCol+" WHERE i."+itemOrderIdCol+" = ?";
                            } else {
                                String pidSel = itemProdIdCol!=null? ("i."+itemProdIdCol+" AS pid") : "NULL AS pid";
                                String qtySel = itemQtyCol!=null? ("i."+itemQtyCol+" AS qty") : "NULL AS qty";
                                String prSel = itemPriceCol!=null? ("i."+itemPriceCol+" AS pr") : "NULL AS pr";
                                itemsSql = "SELECT "+pidSel+", "+qtySel+", "+prSel+" FROM "+itemTable+" i WHERE i."+itemOrderIdCol+" = ?";
                            }

                            try (PreparedStatement psi = con.prepareStatement(itemsSql)) {
                                psi.setObject(1, soid);
                                try (ResultSet rsi = psi.executeQuery()) {
                                    boolean anyItem = false;
                                    while (rsi.next()) {
                                        anyItem = true;
                                        String prodDisp = "";
                                        Object pnameObj = null;
                                        try { pnameObj = rsi.getObject("pname"); } catch (Exception ex) { }
                                        if (pnameObj!=null) {
                                            prodDisp = String.valueOf(pnameObj);
                                            boolean looksLikeId = prodDisp.matches("^\\d+$");
                                            if (looksLikeId && prodTable2!=null && prodIdCol2!=null && prodNameCol2!=null) {
                                                try (PreparedStatement pLookup = con.prepareStatement("SELECT " + prodNameCol2 + " FROM " + prodTable2 + " WHERE " + prodIdCol2 + " = ? LIMIT 1")) {
                                                    pLookup.setObject(1, prodDisp);
                                                    try (ResultSet rLookup = pLookup.executeQuery()) {
                                                        if (rLookup.next()) {
                                                            Object foundName = rLookup.getObject(1);
                                                            if (foundName!=null) prodDisp = String.valueOf(foundName);
                                                        }
                                                    }
                                                } catch (Exception ex) { }
                                            }
                                        } else if (itemProdIdCol!=null) {
                                            Object pid = null;
                                            try { pid = rsi.getObject("pid"); } catch (Exception ex) { }
                                            if (pid!=null) {
                                                prodDisp = String.valueOf(pid);
                                                if (prodTable2!=null && prodIdCol2!=null && prodNameCol2!=null) {
                                                    try (PreparedStatement pLookup = con.prepareStatement("SELECT " + prodNameCol2 + " FROM " + prodTable2 + " WHERE " + prodIdCol2 + " = ? LIMIT 1")) {
                                                        pLookup.setObject(1, pid);
                                                        try (ResultSet rLookup = pLookup.executeQuery()) {
                                                            if (rLookup.next()) {
                                                                Object foundName = rLookup.getObject(1);
                                                                if (foundName!=null) prodDisp = String.valueOf(foundName);
                                                            }
                                                        }
                                                    } catch (Exception ex) { }
                                                }
                                            } else prodDisp = "(producto)";
                                        } else prodDisp = "(producto)";

                                        String qtyS = ""; try { Object q = rsi.getObject("qty"); qtyS = formatNumber(q, df2, dfInt); } catch (Exception ex) { qtyS = ""; }
                                        String prS = ""; try { Object p = rsi.getObject("pr"); prS = p==null? "" : ("S/."+df2.format(rsi.getDouble("pr"))); } catch (Exception ex) { prS = ""; }

                                        itemsTable.addCell(new PdfPCell(new Phrase(prodDisp, normalFont)));
                                        itemsTable.addCell(new PdfPCell(new Phrase(qtyS, normalFont)));
                                        itemsTable.addCell(new PdfPCell(new Phrase(prS, normalFont)));
                                    }
                                    if (!anyItem) { PdfPCell c = new PdfPCell(new Phrase("(Sin productos)", normalFont)); c.setColspan(3); itemsTable.addCell(c); }
                                }
                            }

                            doc.add(itemsTable);
                        }

                        doc.close();
                        resp.reset();
                        resp.setContentType("application/pdf");
                        resp.setHeader("Content-Disposition","inline; filename=stock-stats-range.pdf");
                        byte[] pdfBytes2 = baos.toByteArray();
                        resp.setContentLength(pdfBytes2.length);
                        try (OutputStream out2 = resp.getOutputStream()) { out2.write(pdfBytes2); out2.flush(); }
                        return;
                    }
                }
            }


            doc.add(new Paragraph("Resumen:", titleFont));
            doc.add(resumen);

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(80);
            table.setSpacingBefore(6f);
            table.setSpacingAfter(6f);

            table.addCell(new PdfPCell(new Phrase("Campo", labelFont)));
            table.addCell(new PdfPCell(new Phrase("Mínimo", labelFont)));
            table.addCell(new PdfPCell(new Phrase("Máximo", labelFont)));
            table.addCell(new PdfPCell(new Phrase("Promedio", labelFont)));

            if (stockCol != null) {
                String sql = "SELECT COUNT(*) AS total, COUNT("+stockCol+") AS nonnull, MAX("+stockCol+") AS maxv, MIN("+stockCol+") AS minv, AVG("+stockCol+") AS avgv FROM "+foundTable;
                try (PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        long total = rs.getLong("total");
                        long nonnull = rs.getLong("nonnull");
                        Object maxo = rs.getObject("maxv");
                        Object mino = rs.getObject("minv");
                        Object avgo = rs.getObject("avgv");
                        String maxs = formatNumber(maxo, df2, dfInt);
                        String mins = formatNumber(mino, df2, dfInt);
                        String avgs = formatAverage(avgo, df2);
                        table.addCell(new PdfPCell(new Phrase(stockCol, normalFont)));
                        table.addCell(new PdfPCell(new Phrase(mins, normalFont)));
                        table.addCell(new PdfPCell(new Phrase(maxs, normalFont)));
                        table.addCell(new PdfPCell(new Phrase(avgs + " (count="+nonnull+")", normalFont)));
                    }
                } catch (SQLException ex) {
                    table.addCell(new PdfPCell(new Phrase(stockCol, normalFont)));
                    table.addCell(new PdfPCell(new Phrase("error", normalFont)));
                    table.addCell(new PdfPCell(new Phrase("error", normalFont)));
                    table.addCell(new PdfPCell(new Phrase("error", normalFont)));
                }
            }

            if (priceCol != null) {
                String sql = "SELECT COUNT(*) AS total, COUNT("+priceCol+") AS nonnull, MAX("+priceCol+") AS maxv, MIN("+priceCol+") AS minv, AVG("+priceCol+") AS avgv FROM "+foundTable;
                try (PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        long total = rs.getLong("total");
                        long nonnull = rs.getLong("nonnull");
                        Object maxo = rs.getObject("maxv");
                        Object mino = rs.getObject("minv");
                        Object avgo = rs.getObject("avgv");
                        String maxs = formatNumber(maxo, df2, dfInt);
                        String mins = formatNumber(mino, df2, dfInt);
                        String avgs = formatAverage(avgo, df2);
                        table.addCell(new PdfPCell(new Phrase(priceCol, normalFont)));
                        table.addCell(new PdfPCell(new Phrase(mins, normalFont)));
                        table.addCell(new PdfPCell(new Phrase(maxs, normalFont)));
                        table.addCell(new PdfPCell(new Phrase(avgs + " (count="+nonnull+")", normalFont)));
                    }
                } catch (SQLException ex) {
                    table.addCell(new PdfPCell(new Phrase(priceCol, normalFont)));
                    table.addCell(new PdfPCell(new Phrase("error", normalFont)));
                    table.addCell(new PdfPCell(new Phrase("error", normalFont)));
                    table.addCell(new PdfPCell(new Phrase("error", normalFont)));
                }
            }

            if (categoryCol != null) {
                String sql = "SELECT "+categoryCol+" AS cat, COUNT(*) AS cnt FROM "+foundTable+" GROUP BY "+categoryCol;
                try (PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                    String minCat = null, maxCat = null;
                    long minCnt = Long.MAX_VALUE, maxCnt = Long.MIN_VALUE;
                    long totalCount = 0;
                    int categories = 0;
                    while (rs.next()) {
                        Object catObj = rs.getObject("cat");
                        String cat = catObj==null?"(sin categoría)":String.valueOf(catObj);
                        long cnt = rs.getLong("cnt");
                        totalCount += cnt;
                        categories++;
                        if (cnt < minCnt) { minCnt = cnt; minCat = cat; }
                        if (cnt > maxCnt) { maxCnt = cnt; maxCat = cat; }
                    }
                    if (categories>0) {
                        double avgPerCat = (double) totalCount / (double) categories;
                        String minDisplay = (minCat==null?"":minCat) + " (" + minCnt + ")";
                        String maxDisplay = (maxCat==null?"":maxCat) + " (" + maxCnt + ")";
                        String avgDisplay = formatAverage(Double.valueOf(avgPerCat), df2) + " (categories="+categories+")";
                        table.addCell(new PdfPCell(new Phrase(categoryCol, normalFont)));
                        table.addCell(new PdfPCell(new Phrase(minDisplay, normalFont)));
                        table.addCell(new PdfPCell(new Phrase(maxDisplay, normalFont)));
                        table.addCell(new PdfPCell(new Phrase(avgDisplay, normalFont)));
                    } else {
                        PdfPCell c = new PdfPCell(new Phrase("(No hay categorías con productos)", normalFont));
                        c.setColspan(4);
                        table.addCell(c);
                    }
                } catch (SQLException ex) {
                    table.addCell(new PdfPCell(new Phrase(categoryCol, normalFont)));
                    table.addCell(new PdfPCell(new Phrase("error", normalFont)));
                    table.addCell(new PdfPCell(new Phrase("error", normalFont)));
                    table.addCell(new PdfPCell(new Phrase("error", normalFont)));
                }
            }

            if ((stockCol==null) && (priceCol==null)) {
                PdfPCell c = new PdfPCell(new Phrase("No se encontraron columnas numéricas relevantes en "+foundTable, normalFont));
                c.setColspan(4);
                table.addCell(c);
            }

            doc.add(table);
            doc.close();

            resp.reset();
            resp.setContentType("application/pdf");
            resp.setHeader("Content-Disposition","inline; filename=stock-stats.pdf");
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
        for (String c: candidates) {
            if (c==null) continue;
            if (cols.contains(c.toLowerCase())) return c;
        }
        return null;
    }

    private static String formatNumber(Object o, DecimalFormat df2, DecimalFormat dfInt) {
        if (o==null) return "";
        try {
            if (o instanceof Number) {
                double d = ((Number)o).doubleValue();
                if (Math.abs(d - Math.rint(d)) < 1e-9) return dfInt.format(Math.rint(d));
                return df2.format(d);
            } else {
                String s = String.valueOf(o);
                double d = Double.parseDouble(s);
                if (Math.abs(d - Math.rint(d)) < 1e-9) return dfInt.format(Math.rint(d));
                return df2.format(d);
            }
        } catch (Exception e) { return String.valueOf(o); }
    }

    private static String formatAverage(Object o, DecimalFormat df2) {
        if (o==null) return "";
        try {
            double d = ((Number)o).doubleValue();
            return df2.format(d);
        } catch (Exception e) {
            try { return df2.format(Double.parseDouble(String.valueOf(o))); } catch (Exception ex) { return String.valueOf(o); }
        }
    }
}
