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
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfWriter;

@WebServlet(name = "AdminStockReportServlet", urlPatterns = {"/admin/report/stock"})
public class AdminStockReportServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try (Connection con = DBConnection.getConnection()) {
            String productIdCol = "id_producto";
            try {
                if (hasColumn(con, "Producto", "id_producto")) productIdCol = "id_producto";
                else if (hasColumn(con, "Producto", "producto_id")) productIdCol = "producto_id";
                else if (hasColumn(con, "Producto", "id")) productIdCol = "id";
            } catch (Exception _ex){}

            String stockCol = null;
            try {
                if (hasColumn(con, "Producto", "stock")) stockCol = "stock";
                else if (hasColumn(con, "Producto", "cantidad")) stockCol = "cantidad";
                else if (hasColumn(con, "Producto", "existencia")) stockCol = "existencia";
                else if (hasColumn(con, "Producto", "stock_actual")) stockCol = "stock_actual";
            } catch (Exception _ex){}

            boolean hasPrecio = false;
            try { hasPrecio = hasColumn(con, "Producto", "precio"); } catch (Exception _ex){}

            boolean hasCodigo = false, hasCategoria = false, hasSku = false, hasDescripcion = false, hasMarca = false, hasUnidad = false;
            try {
                hasCodigo = hasColumn(con, "Producto", "codigo") || hasColumn(con, "Producto", "codigo_barra") || hasColumn(con, "Producto", "codigo_producto");
            } catch (Exception _ex){}
            try { hasCategoria = hasColumn(con, "Producto", "categoria"); } catch (Exception _ex){}
            try { hasSku = hasColumn(con, "Producto", "sku") || hasColumn(con, "Producto", "codigo_sku"); } catch (Exception _ex){}
            try { hasDescripcion = hasColumn(con, "Producto", "descripcion") || hasColumn(con, "Producto", "descripcion_corta"); } catch (Exception _ex){}
            try { hasMarca = hasColumn(con, "Producto", "marca"); } catch (Exception _ex){}
            try { hasUnidad = hasColumn(con, "Producto", "unidad_medida") || hasColumn(con, "Producto", "unidad"); } catch (Exception _ex){}

            StringBuilder sb = new StringBuilder();
            sb.append("SELECT ");
            sb.append(productIdCol).append(" as pid, nombre");
            if (hasCodigo) sb.append(", codigo as codigo");
            if (hasCategoria) sb.append(", categoria as categoria");
            if (stockCol != null) sb.append(", ").append(stockCol).append(" as stock");
            if (hasSku) sb.append(", sku as sku");
            if (hasDescripcion) sb.append(", descripcion as descripcion");
            if (hasMarca) sb.append(", marca as marca");
            if (hasUnidad) sb.append(", unidad_medida as unidad_medida");
            if (hasPrecio) sb.append(", precio");
            sb.append(" FROM Producto ORDER BY nombre");
            String sql = sb.toString();
            System.out.println("[AdminStockReportServlet] SQL: " + sql);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document doc = new Document();
            PdfWriter.getInstance(doc, baos);
            doc.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 9);

            PdfPTable header = new PdfPTable(new float[]{1f, 2f});
            header.setWidthPercentage(100);
            try {
                URL logoUrl = getServletContext().getResource("/images/logo.png");
                if (logoUrl != null) {
                    Image logo = Image.getInstance(logoUrl);
                    logo.scaleToFit(72, 72);
                    PdfPCell cLogo = new PdfPCell(logo, false);
                    cLogo.setBorder(Rectangle.NO_BORDER);
                    header.addCell(cLogo);
                } else {
                    PdfPCell cEmpty = new PdfPCell(new Phrase("SUPERNOVA", sectionFont));
                    cEmpty.setBorder(Rectangle.NO_BORDER);
                    header.addCell(cEmpty);
                }
            } catch (Exception _e) {
                PdfPCell cEmpty = new PdfPCell(new Phrase("SUPERNOVA", sectionFont));
                cEmpty.setBorder(Rectangle.NO_BORDER);
                header.addCell(cEmpty);
            }
            PdfPCell right = new PdfPCell();
            right.setBorder(Rectangle.NO_BORDER);
            right.addElement(new Phrase("REPORTE DE STOCK", titleFont));
            Paragraph datePara = new Paragraph(new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()), normalFont);
            datePara.setAlignment(Element.ALIGN_RIGHT);
            right.addElement(datePara);
            header.addCell(right);
            doc.add(header);

            doc.add(new Paragraph(" "));

            java.util.List<String> headers = new java.util.ArrayList<>();
            headers.add("ID");
            headers.add("Nombre");
            if (hasCodigo) headers.add("Código");
            if (hasCategoria) headers.add("Categoría");
            if (stockCol != null) headers.add("Stock");
            if (hasSku) headers.add("SKU");
            if (hasDescripcion) headers.add("Descripción");
            if (hasMarca) headers.add("Marca");
            if (hasUnidad) headers.add("Unidad de medida");
            if (hasPrecio) headers.add("Precio");

            PdfPTable table = new PdfPTable(headers.size());
            table.setWidthPercentage(100);
            float[] widths = new float[headers.size()];
            for (int i = 0; i < headers.size(); i++) {
                String h = headers.get(i);
                if (h.equals("ID")) widths[i] = 0.6f;
                else if (h.equals("Nombre")) widths[i] = 4.5f;
                else if (h.equals("Descripción")) widths[i] = 3.0f;
                else if (h.equals("Precio")) widths[i] = 1.2f;
                else widths[i] = 1.2f;
            }
            try { table.setWidths(widths); } catch (Exception _e) {}

            for (String hh : headers) {
                PdfPCell ch = new PdfPCell(new Phrase(hh, labelFont)); ch.setGrayFill(0.9f); ch.setPadding(4); table.addCell(ch);
            }

            long totalUnits = 0L;
            java.math.BigDecimal totalValue = BigDecimal.ZERO;
            String itemMostStock = null; long mostStockQty = -1;
            String itemLeastStock = null; long leastStockQty = Long.MAX_VALUE;
            long zeroStockCount = 0L;
            List<String> zeroStockItems = new ArrayList<>();
            java.math.BigDecimal highestPrice = null;
            java.math.BigDecimal lowestPrice = null;

            try (PreparedStatement ps = con.prepareStatement(sql)){
                try (ResultSet rs = ps.executeQuery()){
                    boolean alt = false;
                    while (rs.next()){
                        String nombre = rs.getString("nombre");
                        Long stockVal = null;
                        if (stockCol != null) {
                            Object obj = rs.getObject("stock");
                            if (obj != null) {
                                try { stockVal = ((Number)obj).longValue(); } catch (Exception _e) { try { stockVal = Long.parseLong(obj.toString()); } catch (Exception __e) { stockVal = null; } }
                            }
                        }
                        java.math.BigDecimal precioVal = null;
                        if (hasPrecio) {
                            try { precioVal = rs.getBigDecimal("precio"); } catch (Exception _e) { precioVal = null; }
                        }

                        for (String hh : headers) {
                            String key = hh;
                            String text = "";
                            try {
                                switch (key) {
                                    case "ID": text = String.valueOf(rs.getObject("pid")); break;
                                    case "Nombre": text = nombre; break;
                                    case "Código": text = rs.getObject("codigo")!=null?rs.getObject("codigo").toString():""; break;
                                    case "Categoría": text = rs.getObject("categoria")!=null?rs.getObject("categoria").toString():""; break;
                                    case "Stock": text = stockVal!=null?String.valueOf(stockVal):""; break;
                                    case "SKU": text = rs.getObject("sku")!=null?rs.getObject("sku").toString():""; break;
                                    case "Descripción": text = rs.getObject("descripcion")!=null?rs.getObject("descripcion").toString():""; break;
                                    case "Marca": text = rs.getObject("marca")!=null?rs.getObject("marca").toString():""; break;
                                    case "Unidad de medida": text = rs.getObject("unidad_medida")!=null?rs.getObject("unidad_medida").toString():""; break;
                                    case "Precio": text = precioVal!=null?"S/."+precioVal.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString():""; break;
                                    default: text = "";
                                }
                            } catch (Exception _e) { text = ""; }
                            PdfPCell cell = new PdfPCell(new Phrase(text, normalFont)); cell.setPadding(4); if (alt) cell.setGrayFill(0.98f); table.addCell(cell);
                        }

                        if (stockVal != null) {
                            totalUnits += stockVal;
                            if (stockVal == 0) { zeroStockCount++; zeroStockItems.add(nombre); }
                            if (stockVal > mostStockQty) { mostStockQty = stockVal; itemMostStock = nombre; }
                            if (stockVal < leastStockQty) { leastStockQty = stockVal; itemLeastStock = nombre; }
                            if (precioVal != null) {
                                try { totalValue = totalValue.add(precioVal.multiply(new BigDecimal(stockVal))); } catch (Exception _e) {}
                            }
                        }

                        if (precioVal != null) {
                            if (highestPrice == null || precioVal.compareTo(highestPrice) > 0) highestPrice = precioVal;
                            if (lowestPrice == null || precioVal.compareTo(lowestPrice) < 0) lowestPrice = precioVal;
                        }

                        alt = !alt;
                    }
                    if (mostStockQty == -1) { mostStockQty = 0; itemMostStock = "-"; }
                    if (leastStockQty == Long.MAX_VALUE) { leastStockQty = 0; itemLeastStock = "-"; }
                }
            }

            doc.add(table);

            doc.add(new Paragraph(" "));
            Paragraph resumenTitle = new Paragraph("Resumen:", sectionFont);
            doc.add(resumenTitle);

            PdfPTable summary = new PdfPTable(new float[]{3f, 2f});
            summary.setWidthPercentage(60);
            summary.setSpacingBefore(6f);

            java.util.function.BiConsumer<String, String> addRow = (k, v) -> {
                PdfPCell key = new PdfPCell(new Phrase(k, labelFont)); key.setPadding(6); key.setBorder(Rectangle.NO_BORDER);
                PdfPCell val = new PdfPCell(new Phrase(v, normalFont)); val.setPadding(6); val.setBorder(Rectangle.NO_BORDER);
                summary.addCell(key); summary.addCell(val);
            };

            addRow.accept("Total de Unidades en Stock:", String.valueOf(totalUnits));

            if (hasPrecio) {
                addRow.accept("Valor Total del Inventario:", "S/." + totalValue.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
            } else {
                addRow.accept("Valor Total del Inventario:", "N/A");
            }

            if (hasPrecio && totalUnits > 0) {
                java.math.BigDecimal avg = totalValue.divide(new BigDecimal(totalUnits), 2, java.math.RoundingMode.HALF_UP);
                addRow.accept("Precio Promedio Ponderado por Unidad:", "S/." + avg.toPlainString());
            } else {
                addRow.accept("Precio Promedio Ponderado por Unidad:", "N/A");
            }

            addRow.accept("Ítem con Más Stock:", (itemMostStock!=null?itemMostStock:"-") + " (" + mostStockQty + ")");
            addRow.accept("Ítem con Menos Stock:", (itemLeastStock!=null?itemLeastStock:"-") + " (" + leastStockQty + ")");

            if (hasPrecio) {
                addRow.accept("Precio de Mayor:", highestPrice!=null?"S/."+highestPrice.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString():"N/A");
                addRow.accept("Precio de Menor:", lowestPrice!=null?"S/."+lowestPrice.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString():"N/A");
            } else {
                addRow.accept("Precio de Mayor:", "N/A");
                addRow.accept("Precio de Menor:", "N/A");
            }

            addRow.accept("Número de Productos con Stock Cero:", String.valueOf(zeroStockCount));
            addRow.accept("Productos agotados:", zeroStockItems.size() > 0 ? String.join(", ", zeroStockItems) : "-");

            doc.add(summary);

            doc.close();

            resp.reset();
            resp.setContentType("application/pdf");
            resp.setHeader("Content-Disposition", "inline; filename=stock-report.pdf");
            resp.setHeader("Cache-Control", "no-store, no-cache");
            byte[] pdfBytes = baos.toByteArray();
            resp.setContentLength(pdfBytes.length);
            try (OutputStream out = resp.getOutputStream()){
                out.write(pdfBytes);
                out.flush();
            }

        } catch (SQLException e) {
            throw new ServletException(e);
        } catch (Exception e) {
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
}
