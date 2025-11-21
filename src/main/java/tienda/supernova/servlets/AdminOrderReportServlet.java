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
import java.util.HashSet;

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

@WebServlet(name = "AdminOrderReportServlet", urlPatterns = {"/admin/report/order"})
public class AdminOrderReportServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String idParam = req.getParameter("id");
        if (idParam == null) { resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing id"); return; }
        int id = Integer.parseInt(idParam);

        try (Connection con = DBConnection.getConnection()) {
            String dateCol = hasColumn(con, "Pedido", "fecha_pedido") ? "fecha_pedido" : (hasColumn(con, "Pedido", "fecha_creacion") ? "fecha_creacion" : "fecha_pedido");

            String productIdCol = "id_producto";
            try {
                if (hasColumn(con, "Producto", "id_producto")) productIdCol = "id_producto";
                else if (hasColumn(con, "Producto", "producto_id")) productIdCol = "producto_id";
                else if (hasColumn(con, "Producto", "id")) productIdCol = "id";
            } catch (Exception _ex) {}

            String sql = "SELECT id_pedido, id_cliente, estado, " + dateCol + " as fecha_pedido, fecha_entrega_estimada, prioridad, costo_envio, metodo_pago, notas, total FROM Pedido WHERE id_pedido = ?";
            String sqlItems = "SELECT dp.id_detalle as id_detalle, prod.nombre as producto, dp.cantidad_solicitada, dp.precio_unitario, prod.precio as producto_precio FROM Detalle_Pedido dp LEFT JOIN Producto prod ON dp.id_producto = prod." + productIdCol + " WHERE dp.id_pedido = ?";

            System.out.println("[AdminOrderReportServlet] SQL: " + sql);
            System.out.println("[AdminOrderReportServlet] SQL items: " + sqlItems);

            String cliente = null, direccion = null, metodoPago = null, notas = null, estado = null, prioridad = null;
            java.util.Date fecha = null, fechaEst = null;
            BigDecimal costoEnv = BigDecimal.ZERO, total = null;
            boolean detalleHasPrecio = false;
            Integer clienteId = null;

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, id);
                try (ResultSet r = ps.executeQuery()) {
                    if (r.next()) {
                        metodoPago = r.getString("metodo_pago");
                        notas = r.getString("notas");
                        estado = r.getString("estado");
                        prioridad = r.getString("prioridad");
                        fecha = r.getTimestamp("fecha_pedido");
                        fechaEst = r.getTimestamp("fecha_entrega_estimada");
                        costoEnv = r.getBigDecimal("costo_envio") != null ? r.getBigDecimal("costo_envio") : BigDecimal.ZERO;
                        total = r.getBigDecimal("total");
                        clienteId = r.getObject("id_cliente") != null ? r.getInt("id_cliente") : null;
                    }
                }
            }

            if (clienteId != null) {
                try (PreparedStatement pcs = con.prepareStatement("SELECT nombre, direccion FROM Cliente WHERE id_cliente = ?")){
                    pcs.setInt(1, clienteId);
                    try (ResultSet cr = pcs.executeQuery()){
                        if (cr.next()){
                            cliente = cr.getString("nombre");
                            direccion = cr.getString("direccion");
                        }
                    }
                }
            }

            try (PreparedStatement pis = con.prepareStatement("SELECT precio_unitario FROM Detalle_Pedido WHERE id_pedido = ? LIMIT 1")){
                pis.setInt(1, id);
                try (ResultSet rr = pis.executeQuery()){
                    if (rr.next()) {
                        detalleHasPrecio = rr.getObject(1) != null;
                    }
                }
            }

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
            right.addElement(new Phrase("RESUMEN DE PEDIDO", titleFont));
            Paragraph badgePara = new Paragraph("PEDIDO #" + id, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10));
            badgePara.setAlignment(Element.ALIGN_RIGHT);
            badgePara.setSpacingBefore(6);
            right.addElement(badgePara);
            header.addCell(right);
            doc.add(header);

            doc.add(new Paragraph(" "));

            PdfPTable infoRow = new PdfPTable(new float[]{1f, 1f});
            infoRow.setWidthPercentage(100);
            PdfPCell clientBox = new PdfPCell(); clientBox.setPadding(8);
            clientBox.addElement(new Phrase("Datos del cliente", sectionFont));
            clientBox.addElement(new Phrase("Cliente: ", labelFont)); clientBox.addElement(new Phrase(cliente != null ? cliente : "", normalFont));
            clientBox.addElement(new Phrase("Dirección: ", labelFont)); clientBox.addElement(new Phrase(direccion != null ? direccion : "", normalFont));
            clientBox.addElement(new Phrase("Método pago: ", labelFont)); clientBox.addElement(new Phrase(metodoPago != null ? metodoPago : "", normalFont));
            clientBox.addElement(new Phrase("Costo envío: ", labelFont)); clientBox.addElement(new Phrase(costoEnv != null ? ("S/." + costoEnv.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()) : "S/.0.00", normalFont));
            clientBox.setBorder(Rectangle.NO_BORDER);
            clientBox.setGrayFill(0.96f);
            infoRow.addCell(clientBox);

            PdfPCell statusBox = new PdfPCell(); statusBox.setPadding(8);
            statusBox.addElement(new Phrase("Estado y envío", sectionFont));
            statusBox.addElement(new Phrase("Estado: ", labelFont)); statusBox.addElement(new Phrase(estado != null ? estado : "", normalFont));
            statusBox.addElement(new Phrase("Fecha: ", labelFont)); statusBox.addElement(new Phrase(fecha != null ? fecha.toString() : "", normalFont));
            statusBox.addElement(new Phrase("Fecha entrega estimada: ", labelFont)); statusBox.addElement(new Phrase(fechaEst != null ? fechaEst.toString() : "", normalFont));
            statusBox.addElement(new Phrase("Prioridad: ", labelFont)); statusBox.addElement(new Phrase(prioridad != null ? prioridad : "", normalFont));
            statusBox.setBorder(Rectangle.NO_BORDER);
            statusBox.setGrayFill(0.96f);
            infoRow.addCell(statusBox);

            doc.add(infoRow);

            doc.add(new Paragraph(" "));

            Paragraph prodTitle = new Paragraph("PRODUCTOS", sectionFont);
            prodTitle.setSpacingBefore(6);

            PdfPTable itemsTable = new PdfPTable(detalleHasPrecio ? 4 : 3);
            itemsTable.setWidthPercentage(100);
            itemsTable.setWidths(detalleHasPrecio ? new float[]{0.6f, 4f, 1.2f, 1.6f} : new float[]{0.6f, 5f, 1.5f});
            PdfPCell h1 = new PdfPCell(new Phrase("#", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
            PdfPCell h2 = new PdfPCell(new Phrase("Producto", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
            PdfPCell h3 = new PdfPCell(new Phrase("Cantidad", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
            h1.setGrayFill(0.9f); h2.setGrayFill(0.9f); h3.setGrayFill(0.9f);
            h1.setPadding(4); h2.setPadding(4); h3.setPadding(4);
            itemsTable.addCell(h1); itemsTable.addCell(h2); itemsTable.addCell(h3);
            if (detalleHasPrecio) { PdfPCell h4 = new PdfPCell(new Phrase("Precio unitario", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10))); h4.setGrayFill(0.9f); h4.setPadding(4); itemsTable.addCell(h4); }

            BigDecimal subtotal = BigDecimal.ZERO;
            int totalItems = 0;
            int distinctItems = 0;
            BigDecimal highestUnitPrice = BigDecimal.ZERO;
            HashSet<Integer> seenItemIds = new HashSet<>();

            try (PreparedStatement pis = con.prepareStatement(sqlItems)){
                pis.setInt(1, id);
                try (ResultSet r = pis.executeQuery()){
                    int idx = 1;
                    boolean alt = false;
                    while (r.next()){
                        String prodName = r.getString("producto");
                        int qty = 0; try { qty = r.getInt("cantidad_solicitada"); } catch (Exception _e) { qty = 0; }
                        BigDecimal unitPrice = null;
                        try { unitPrice = r.getBigDecimal("precio_unitario"); } catch (Exception _e) { unitPrice = null; }
                        if (unitPrice == null) {
                            try { unitPrice = r.getBigDecimal("producto_precio"); } catch (Exception _e) { unitPrice = null; }
                        }
                        if (unitPrice == null) unitPrice = BigDecimal.ZERO;

                        PdfPCell c1 = new PdfPCell(new Phrase(String.valueOf(idx++)));
                        PdfPCell c2 = new PdfPCell(new Phrase(prodName != null ? prodName : ""));
                        PdfPCell c3 = new PdfPCell(new Phrase(String.valueOf(qty)));
                        c1.setPadding(4); c2.setPadding(4); c3.setPadding(4);
                        if (detalleHasPrecio) {
                            PdfPCell c4 = new PdfPCell(new Phrase(unitPrice.compareTo(BigDecimal.ZERO)!=0?"S/."+unitPrice.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString():""));
                            c4.setPadding(4);
                            if (alt) { c1.setGrayFill(0.98f); c2.setGrayFill(0.98f); c3.setGrayFill(0.98f); c4.setGrayFill(0.98f); }
                            itemsTable.addCell(c1); itemsTable.addCell(c2); itemsTable.addCell(c3); itemsTable.addCell(c4);
                        } else {
                            if (alt) { c1.setGrayFill(0.98f); c2.setGrayFill(0.98f); c3.setGrayFill(0.98f); }
                            itemsTable.addCell(c1); itemsTable.addCell(c2); itemsTable.addCell(c3);
                        }

                        alt = !alt;
                        totalItems += qty;
                        subtotal = subtotal.add(unitPrice.multiply(new BigDecimal(qty)));
                        if (unitPrice.compareTo(highestUnitPrice) > 0) highestUnitPrice = unitPrice;
                        int detId = -1; try { detId = r.getInt("id_detalle"); } catch (Exception _e) { detId = -1; }
                        if (detId != -1 && !seenItemIds.contains(detId)) { seenItemIds.add(detId); distinctItems++; }
                    }
                }
            }

            PdfPTable summaryInner = new PdfPTable(2);
            summaryInner.setWidthPercentage(100);
            summaryInner.addCell(new PdfPCell(new Phrase("Items distintos", labelFont))); summaryInner.addCell(new PdfPCell(new Phrase(String.valueOf(distinctItems), normalFont)));
            summaryInner.addCell(new PdfPCell(new Phrase("Total de unidades", labelFont))); summaryInner.addCell(new PdfPCell(new Phrase(String.valueOf(totalItems), normalFont)));
            summaryInner.addCell(new PdfPCell(new Phrase("Subtotal (sin envío)", labelFont))); summaryInner.addCell(new PdfPCell(new Phrase("S/." + subtotal.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString(), normalFont)));
            summaryInner.addCell(new PdfPCell(new Phrase("Precio unitario mayor", labelFont))); summaryInner.addCell(new PdfPCell(new Phrase("S/." + highestUnitPrice.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString(), normalFont)));
            summaryInner.addCell(new PdfPCell(new Phrase("Costo envío", labelFont))); summaryInner.addCell(new PdfPCell(new Phrase(costoEnv != null ? ("S/." + costoEnv.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()) : "S/.0.00", normalFont)));
            PdfPCell totalLabel = new PdfPCell(new Phrase("Total pedido", labelFont));
            PdfPCell totalValue = new PdfPCell(new Phrase(total != null ? ("S/." + total.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()) : ("S/." + subtotal.add(costoEnv != null ? costoEnv : BigDecimal.ZERO).setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()), normalFont));
            totalLabel.setGrayFill(0.85f); totalValue.setGrayFill(0.85f);
            totalLabel.setPadding(6); totalValue.setPadding(6);
            summaryInner.addCell(totalLabel);
            summaryInner.addCell(totalValue);

            PdfPTable summaryBox = new PdfPTable(1);
            summaryBox.setWidthPercentage(100);
            PdfPCell sumCell = new PdfPCell(summaryInner);
            sumCell.setBorder(Rectangle.BOX);
            sumCell.setPadding(6);
            summaryBox.addCell(sumCell);

            PdfPTable parent = new PdfPTable(new float[]{0.68f, 0.32f});
            parent.setWidthPercentage(100);
            PdfPCell left = new PdfPCell(); left.setBorder(Rectangle.NO_BORDER);
            left.addElement(prodTitle);
            left.addElement(itemsTable);
            left.addElement(new Paragraph(" "));
            left.addElement(new Paragraph("Notas: " + (notas != null ? notas : ""), normalFont));
            parent.addCell(left);
            PdfPCell rightSummary = new PdfPCell(); rightSummary.setBorder(Rectangle.NO_BORDER); rightSummary.addElement(summaryBox);
            parent.addCell(rightSummary);

            doc.add(parent);

            doc.close();

            try {
                resp.reset();
                resp.setContentType("application/pdf");
                resp.setHeader("Content-Disposition", "inline; filename=pedido-" + id + ".pdf");
                resp.setHeader("Cache-Control", "no-store, no-cache");
                byte[] pdfBytes = baos.toByteArray();
                resp.setContentLength(pdfBytes.length);
                try (OutputStream out = resp.getOutputStream()){
                    out.write(pdfBytes);
                    out.flush();
                }
            } finally {
                try { baos.close(); } catch (Exception _e){}
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
