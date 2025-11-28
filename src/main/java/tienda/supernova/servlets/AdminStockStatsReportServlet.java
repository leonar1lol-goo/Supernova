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
