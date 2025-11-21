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
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfWriter;

@WebServlet(name = "AdminOrdersDateReportServlet", urlPatterns = {"/admin/report/orders-range"})
public class AdminOrdersDateReportServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String fromParam = req.getParameter("from");
        String toParam = req.getParameter("to");
        if (fromParam == null || toParam == null) { resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing from/to"); return; }

        try (Connection con = DBConnection.getConnection()) {
            String dateCol = "fecha_pedido";
            try { if (!hasColumn(con, "Pedido", "fecha_pedido") && hasColumn(con, "Pedido", "fecha_creacion")) dateCol = "fecha_creacion"; } catch (Exception _e) {}

            Timestamp tsFrom = parseDayStart(fromParam);
            Timestamp tsTo = parseDayEnd(toParam);

            String sql = "SELECT p.id_pedido, COALESCE(c.nombre,'(sin cliente)') AS cliente, p.estado, p." + dateCol + " AS fecha_pedido, p.direccion_envio, p.costo_envio, p.total FROM Pedido p LEFT JOIN Cliente c ON p.id_cliente = c.id_cliente WHERE p." + dateCol + " BETWEEN ? AND ? ORDER BY p." + dateCol + " ASC";

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document doc = new Document();
            PdfWriter.getInstance(doc, baos);
            doc.open();

            com.lowagie.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            com.lowagie.text.Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            com.lowagie.text.Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 9);

            PdfPTable header = new PdfPTable(new float[]{1f,2f}); header.setWidthPercentage(100);
            try {
                URL logoUrl = getServletContext().getResource("/images/logo.png");
                if (logoUrl != null) {
                    Image logo = Image.getInstance(logoUrl);
                    logo.scaleToFit(72,72);
                    PdfPCell cLogo = new PdfPCell(logo,false); cLogo.setBorder(Rectangle.NO_BORDER); header.addCell(cLogo);
                } else {
                    PdfPCell cEmpty = new PdfPCell(new Phrase("SUPERNOVA", titleFont)); cEmpty.setBorder(Rectangle.NO_BORDER); header.addCell(cEmpty);
                }
            } catch (Exception _e) { PdfPCell cEmpty = new PdfPCell(new Phrase("SUPERNOVA", titleFont)); cEmpty.setBorder(Rectangle.NO_BORDER); header.addCell(cEmpty); }
            PdfPCell right = new PdfPCell(); right.setBorder(Rectangle.NO_BORDER); right.addElement(new Phrase("REPORTE DE PEDIDOS", titleFont));
            right.addElement(new Phrase("Desde: " + fromParam + "  Hasta: " + toParam, normalFont));
            header.addCell(right);
            doc.add(header);
            doc.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(new float[]{0.6f,3f,1.6f,1.8f,1.8f,1.2f});
            table.setWidthPercentage(100);
            table.addCell(new PdfPCell(new Phrase("ID", labelFont)));
            table.addCell(new PdfPCell(new Phrase("Cliente", labelFont)));
            table.addCell(new PdfPCell(new Phrase("Fecha", labelFont)));
            table.addCell(new PdfPCell(new Phrase("Estado", labelFont)));
            table.addCell(new PdfPCell(new Phrase("Dirección envío", labelFont)));
            table.addCell(new PdfPCell(new Phrase("Total (S/.)", labelFont)));

            int totalOrders = 0;
            BigDecimal sumTotals = BigDecimal.ZERO;
            BigDecimal sumShipping = BigDecimal.ZERO;
            long totalUnits = 0L;
            BigDecimal maxOrderTotal = null; int maxOrderId = -1; String maxOrderClient = null;
            BigDecimal minOrderTotal = null; int minOrderId = -1; String minOrderClient = null;

            try (PreparedStatement ps = con.prepareStatement(sql)){
                ps.setTimestamp(1, tsFrom);
                ps.setTimestamp(2, tsTo);
                try (ResultSet rs = ps.executeQuery()){
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    while (rs.next()){
                        totalOrders++;
                        int id = rs.getInt("id_pedido");
                        String cliente = rs.getString("cliente");
                        Timestamp ts = rs.getTimestamp("fecha_pedido");
                        String fecha = ts!=null? sdf.format(new Date(ts.getTime())):"";
                        String estado = rs.getString("estado");
                        String direccion = rs.getString("direccion_envio");
                        BigDecimal costoEnv = null; try { costoEnv = rs.getBigDecimal("costo_envio"); } catch (Exception _e) { costoEnv = BigDecimal.ZERO; }
                        BigDecimal total = null; try { total = rs.getBigDecimal("total"); } catch (Exception _e) { total = null; }
                        if (total == null) total = BigDecimal.ZERO;
                        sumTotals = sumTotals.add(total);
                        if (maxOrderTotal == null || total.compareTo(maxOrderTotal) > 0) { maxOrderTotal = total; maxOrderId = id; maxOrderClient = cliente; }
                        if (minOrderTotal == null || total.compareTo(minOrderTotal) < 0) { minOrderTotal = total; minOrderId = id; minOrderClient = cliente; }
                        if (costoEnv != null) sumShipping = sumShipping.add(costoEnv);

                        PdfPCell c1 = new PdfPCell(new Phrase(String.valueOf(id), normalFont)); c1.setPadding(4);
                        PdfPCell c2 = new PdfPCell(new Phrase(cliente!=null?cliente:"", normalFont)); c2.setPadding(4);
                        PdfPCell c3 = new PdfPCell(new Phrase(fecha, normalFont)); c3.setPadding(4);
                        PdfPCell c4 = new PdfPCell(new Phrase(estado!=null?estado:"", normalFont)); c4.setPadding(4);
                        PdfPCell c5 = new PdfPCell(new Phrase(direccion!=null?direccion:"", normalFont)); c5.setPadding(4);
                        PdfPCell c6 = new PdfPCell(new Phrase("S/." + total.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString(), normalFont)); c6.setPadding(4);
                        table.addCell(c1); table.addCell(c2); table.addCell(c3); table.addCell(c4); table.addCell(c5); table.addCell(c6);
                    }
                }
            }

            try (PreparedStatement psu = con.prepareStatement("SELECT COALESCE(SUM(dp.cantidad_solicitada),0) AS total_units FROM Detalle_Pedido dp JOIN Pedido p ON dp.id_pedido = p.id_pedido WHERE p." + dateCol + " BETWEEN ? AND ?")){
                psu.setTimestamp(1, tsFrom);
                psu.setTimestamp(2, tsTo);
                try (ResultSet ru = psu.executeQuery()){
                    if (ru.next()) {
                        try { totalUnits = ru.getLong("total_units"); } catch (Exception _e) { totalUnits = 0L; }
                    }
                }
            } catch (Exception _e) { totalUnits = 0L; }

            doc.add(table);
            doc.add(new Paragraph(" "));

            PdfPTable summary = new PdfPTable(2);
            summary.setWidthPercentage(50);
            summary.addCell(new PdfPCell(new Phrase("Total pedidos", labelFont))); summary.addCell(new PdfPCell(new Phrase(String.valueOf(totalOrders), normalFont)));
            summary.addCell(new PdfPCell(new Phrase("Suma totales pedidos (incluye envío)", labelFont))); summary.addCell(new PdfPCell(new Phrase("S/." + sumTotals.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString(), normalFont)));
            summary.addCell(new PdfPCell(new Phrase("Total de Unidades Pedidas", labelFont))); summary.addCell(new PdfPCell(new Phrase(String.valueOf(totalUnits), normalFont)));
            summary.addCell(new PdfPCell(new Phrase("Pedido con Mayor Total", labelFont)));
            summary.addCell(new PdfPCell(new Phrase((maxOrderId!=-1?"#"+maxOrderId+" - "+(maxOrderClient!=null?maxOrderClient:"")+" (S/."+ (maxOrderTotal!=null?maxOrderTotal.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString():"0.00") +")":"-"), normalFont)));
            summary.addCell(new PdfPCell(new Phrase("Pedido con Menor Total", labelFont)));
            summary.addCell(new PdfPCell(new Phrase((minOrderId!=-1?"#"+minOrderId+" - "+(minOrderClient!=null?minOrderClient:"")+" (S/."+ (minOrderTotal!=null?minOrderTotal.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString():"0.00") +")":"-"), normalFont)));
            summary.addCell(new PdfPCell(new Phrase("Suma costo de envío", labelFont))); summary.addCell(new PdfPCell(new Phrase("S/." + sumShipping.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString(), normalFont)));

            doc.add(summary);
            doc.close();

            resp.reset();
            resp.setContentType("application/pdf");
            resp.setHeader("Content-Disposition","inline; filename=orders-range-"+fromParam+"-to-"+toParam+".pdf");
            resp.setHeader("Cache-Control","no-store, no-cache");
            byte[] pdfBytes = baos.toByteArray();
            resp.setContentLength(pdfBytes.length);
            try (OutputStream out = resp.getOutputStream()){
                out.write(pdfBytes); out.flush();
            }

        } catch (SQLException e) {
            throw new ServletException(e);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    private Timestamp parseDayStart(String d) {
        try {
            LocalDate ld = LocalDate.parse(d);
            return Timestamp.valueOf(ld.atStartOfDay());
        } catch (Exception e) { return null; }
    }
    private Timestamp parseDayEnd(String d) {
        try {
            LocalDate ld = LocalDate.parse(d);
            return Timestamp.valueOf(ld.atTime(23,59,59));
        } catch (Exception e) { return null; }
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
