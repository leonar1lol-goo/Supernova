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
import java.util.Date;

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

            String sql = "SELECT " + productIdCol + " as pid, nombre" + (stockCol != null ? (", " + stockCol + " as stock") : "") + (hasPrecio ? ", precio" : "") + " FROM Producto ORDER BY nombre";

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

            int cols = 2 + (stockCol != null ? 1 : 0) + (hasPrecio ? 1 : 0);
            PdfPTable table = new PdfPTable(cols);
            table.setWidthPercentage(100);
            float[] widths;
            if (cols == 4) widths = new float[]{0.6f, 5f, 1.2f, 1.6f};
            else if (cols == 3) widths = new float[]{0.6f, 6f, 1.4f};
            else widths = new float[]{0.6f, 7f};
            try { table.setWidths(widths); } catch (Exception _e) {}

            PdfPCell ch1 = new PdfPCell(new Phrase("#", labelFont)); ch1.setGrayFill(0.9f); ch1.setPadding(4);
            PdfPCell ch2 = new PdfPCell(new Phrase("Producto", labelFont)); ch2.setGrayFill(0.9f); ch2.setPadding(4);
            table.addCell(ch1); table.addCell(ch2);
            if (stockCol != null) { PdfPCell ch3 = new PdfPCell(new Phrase("Stock", labelFont)); ch3.setGrayFill(0.9f); ch3.setPadding(4); table.addCell(ch3); }
            if (hasPrecio) { PdfPCell ch4 = new PdfPCell(new Phrase("Precio", labelFont)); ch4.setGrayFill(0.9f); ch4.setPadding(4); table.addCell(ch4); }

            try (PreparedStatement ps = con.prepareStatement(sql)){
                try (ResultSet rs = ps.executeQuery()){
                    int idx = 1; boolean alt = false;
                    while (rs.next()){
                        PdfPCell c1 = new PdfPCell(new Phrase(String.valueOf(idx++), normalFont)); c1.setPadding(4);
                        PdfPCell c2 = new PdfPCell(new Phrase(rs.getString("nombre"), normalFont)); c2.setPadding(4);
                        if (alt) { c1.setGrayFill(0.98f); c2.setGrayFill(0.98f); }
                        table.addCell(c1); table.addCell(c2);
                        if (stockCol != null) {
                            PdfPCell c3 = new PdfPCell(new Phrase(String.valueOf(rs.getObject("stock")!=null?rs.getObject("stock"):""), normalFont)); c3.setPadding(4); if (alt) c3.setGrayFill(0.98f); table.addCell(c3);
                        }
                        if (hasPrecio) {
                            BigDecimal p = null; try { p = rs.getBigDecimal("precio"); } catch (Exception _e) { p = null; }
                            PdfPCell c4 = new PdfPCell(new Phrase(p!=null?"S/."+p.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString():"", normalFont)); c4.setPadding(4); if (alt) c4.setGrayFill(0.98f); table.addCell(c4);
                        }
                        alt = !alt;
                    }
                }
            }

            doc.add(table);
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
