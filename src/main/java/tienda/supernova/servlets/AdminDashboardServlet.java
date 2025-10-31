package tienda.supernova.servlets;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/admin/dashboard")
public class AdminDashboardServlet extends HttpServlet {

    @Override

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        int totalUsers = 0;
        int totalProducts = 0;
        int pendingOrders = 0;
        try (java.sql.Connection con = tienda.supernova.db.DBConnection.getConnection()) {
            try (java.sql.PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) AS cnt FROM usuarios")) {
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) totalUsers = rs.getInt("cnt");

                }

            } catch (Exception ignore) {  }



            try (java.sql.PreparedStatement ps2 = con.prepareStatement("SELECT COUNT(*) AS cnt FROM productos")) {

                try (java.sql.ResultSet rs2 = ps2.executeQuery()) {

                    if (rs2.next()) totalProducts = rs2.getInt("cnt");

                }

            } catch (Exception ignore) {  }


            try (java.sql.PreparedStatement pts = con.prepareStatement(

                    "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = DATABASE() " +

                            "AND (TABLE_NAME LIKE '%orden%' OR TABLE_NAME LIKE '%pedido%' OR TABLE_NAME LIKE '%order%' OR TABLE_NAME LIKE '%venta%') LIMIT 1")) {

                try (java.sql.ResultSet rts = pts.executeQuery()) {

                    if (rts.next()) {

                        String ordersTable = rts.getString("TABLE_NAME");


                        String statusCol = null;

                        try (java.sql.PreparedStatement pcs = con.prepareStatement(

                                "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?")) {

                            pcs.setString(1, ordersTable);

                            try (java.sql.ResultSet rcs = pcs.executeQuery()) {

                                while (rcs.next()) {

                                    String col = rcs.getString("COLUMN_NAME");

                                    String lc = col.toLowerCase();

                                    if (lc.contains("estado") || lc.contains("status") || lc.contains("estado_pedido") || lc.contains("is_pending") || lc.contains("pending")) {

                                        statusCol = col;

                                        break;

                                    }

                                }

                            }

                        }



                        if (statusCol != null) {


                            String q = "SELECT COUNT(*) AS cnt FROM " + ordersTable + " WHERE " + statusCol + " IN ('pendiente','PENDIENTE','pending','PENDING', 'por procesar')";

                            try (java.sql.PreparedStatement ps3 = con.prepareStatement(q); java.sql.ResultSet rs3 = ps3.executeQuery()) {

                                if (rs3.next()) pendingOrders = rs3.getInt("cnt");

                            } catch (Exception ex) {


                                try (java.sql.PreparedStatement ps4 = con.prepareStatement("SELECT COUNT(*) AS cnt FROM " + ordersTable + " WHERE " + statusCol + " = 1"); java.sql.ResultSet rs4 = ps4.executeQuery()) {

                                    if (rs4.next()) pendingOrders = rs4.getInt("cnt");

                                } catch (Exception ignore2) { }

                            }

                        }

                    }

                }

            } catch (Exception ignore) {  }

        } catch (java.sql.SQLException e) {

            throw new ServletException(e);

        }



        req.setAttribute("totalUsers", totalUsers);

        req.setAttribute("totalProducts", totalProducts);

        req.setAttribute("pendingOrders", pendingOrders);


        req.getRequestDispatcher("/admin/dashboard.jsp").forward(req, resp);

    }

}

