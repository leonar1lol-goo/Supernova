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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
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

@WebServlet(name = "AdminDeletedRecordsReportServlet", urlPatterns = {"/admin/report/deleted"})
public class AdminDeletedRecordsReportServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        jakarta.servlet.http.HttpSession s = req.getSession(false);
        String role = s != null ? (String) s.getAttribute("role") : null;
        if (role == null || !role.equalsIgnoreCase("admin")) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            resp.setContentType("text/plain;charset=UTF-8");
            resp.getWriter().print("forbidden");
            return;
        }

        try (Connection con = DBConnection.getConnection()) {
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
                    logo.scaleToFit(72, 72);
                    PdfPTable header = new PdfPTable(new float[]{1f, 3f});
                    header.setWidthPercentage(100);
                    PdfPCell cLogo = new PdfPCell(logo, false); cLogo.setBorder(Rectangle.NO_BORDER);
                    header.addCell(cLogo);
                    PdfPCell cRight = new PdfPCell(); cRight.setBorder(Rectangle.NO_BORDER);
                    cRight.addElement(new Phrase("REPORTE: REGISTROS ELIMINADOS (LÓGICOS)", titleFont));
                    cRight.addElement(new Phrase("Generado: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()), normalFont));
                    header.addCell(cRight);
                    doc.add(header);
                } else {
                    doc.add(new Paragraph("REPORTE: REGISTROS ELIMINADOS (LÓGICOS)", titleFont));
                    doc.add(new Paragraph("Generado: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()), normalFont));
                }
            } catch (Exception e) {
                doc.add(new Paragraph("REPORTE: REGISTROS ELIMINADOS (LÓGICOS)", titleFont));
            }

            doc.add(new Paragraph(" "));

            String[][] groups = new String[][]{
                {"cliente","Cliente","clientes","Clientes"},
                {"producto","Producto","productos","Productos"},
                {"usuario","Usuario","usuarios","Usuarios"}
            };

            for (String[] names : groups) {
                String foundTable = null;
                Set<String> cols = new HashSet<>();
                for (String t : names) {
                    if (tableExists(con, t)) {
                        foundTable = t;
                        cols = getColumns(con, t);
                        break;
                    }
                }
                if (foundTable == null) continue;

                List<String> conditions = new ArrayList<>();
                String colActivo = pickExisting(cols, new String[]{"activo","activo_cliente","enabled","is_active"});
                String colFechaBaja = pickExisting(cols, new String[]{"fecha_baja","deleted_at","fecha_eliminacion","fecha_eliminado"});
                String colDeleted = pickExisting(cols, new String[]{"deleted","is_deleted","eliminado"});

                if (colActivo != null) conditions.add("COALESCE("+colActivo+",0)=0");
                if (colFechaBaja != null) conditions.add(colFechaBaja + " IS NOT NULL");
                if (colDeleted != null) conditions.add(colDeleted + "=1");

                if (conditions.isEmpty()) continue;

                String where = String.join(" OR ", conditions);

                List<String> showCols = new ArrayList<>();
                String idCol = pickExisting(cols, new String[]{"id_cliente","id_product","id_usuario","id","id_cliente","id_producto","id_usuario","id_user"});
                String nameCol = pickExisting(cols, new String[]{"nombre","name","titulo","title","descripcion","descripcion_corta"});
                String emailCol = pickExisting(cols, new String[]{"email","correo","mail"});
                String codeCol = pickExisting(cols, new String[]{"codigo","sku","codigo_producto"});

                if (idCol != null) showCols.add(idCol); else showCols.add("*");
                if (nameCol != null) showCols.add(nameCol);
                if (emailCol != null) showCols.add(emailCol);
                if (codeCol != null) showCols.add(codeCol);
                if (colActivo != null) showCols.add(colActivo);
                if (colFechaBaja != null) showCols.add(colFechaBaja);
                if (colDeleted != null) showCols.add(colDeleted);

                String selectList;
                if (showCols.size()==1 && "*".equals(showCols.get(0))) selectList = "*";
                else {
                    StringBuilder tmp = new StringBuilder();
                    boolean first = true;
                    for (String sc : showCols) {
                        if (sc==null) continue;
                        if ("*".equals(sc)) continue;
                        if (!first) tmp.append(","); else first=false;
                        tmp.append(sc);
                    }
                    selectList = tmp.length()>0 ? tmp.toString() : "*";
                }

                String sql = "SELECT " + selectList + " FROM " + foundTable + " WHERE (" + where + ") ORDER BY "+ (nameCol!=null?nameCol:(idCol!=null?idCol:"1")) + " ASC";

                PdfPTable table = new PdfPTable(selectList.split(",").length);
                table.setWidthPercentage(100);

                PdfPCell h = new PdfPCell(new Phrase("Tabla: " + foundTable, labelFont));
                h.setColspan(table.getNumberOfColumns());
                h.setBackgroundColor(null);
                table.addCell(h);

                String[] headers = selectList.split(",");
                for (String hc : headers) {
                    table.addCell(new PdfPCell(new Phrase(hc.trim(), labelFont)));
                }

                int count = 0;
                try (PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                    ResultSetMetaData md = rs.getMetaData();
                    while (rs.next()) {
                        count++;
                        for (int i=1;i<=md.getColumnCount();i++) {
                            Object v = rs.getObject(i);
                            String txt = v==null?"": String.valueOf(v);
                            table.addCell(new PdfPCell(new Phrase(txt, normalFont)));
                        }
                    }
                } catch (SQLException e) {
                    continue;
                }

                if (count==0) {
                    PdfPCell noCell = new PdfPCell(new Phrase("(No hay registros eliminados)", normalFont));
                    noCell.setColspan(table.getNumberOfColumns());
                    table.addCell(noCell);
                }

                PdfPCell totalCell = new PdfPCell(new Phrase("Total registros: " + count, labelFont));
                totalCell.setColspan(table.getNumberOfColumns());
                totalCell.setHorizontalAlignment(PdfPCell.ALIGN_RIGHT);
                table.addCell(totalCell);

                doc.add(table);
                doc.add(new Paragraph(" "));
            }

            doc.close();

            resp.reset();
            resp.setContentType("application/pdf");
            resp.setHeader("Content-Disposition","inline; filename=deleted-records.pdf");
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
}
