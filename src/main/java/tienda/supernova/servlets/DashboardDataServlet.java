package tienda.supernova.servlets;

import tienda.supernova.db.DBConnection;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

@WebServlet("/admin/api/dashboard/categories")
public class DashboardDataServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        ArrayList<String> labels = new ArrayList<>();
        ArrayList<Integer> values = new ArrayList<>();
        String sql = "SELECT categoria, SUM(stock) AS total FROM Producto GROUP BY categoria ORDER BY total DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String categoria = rs.getString("categoria");
                if (categoria == null || categoria.trim().isEmpty()) categoria = "Sin categoría";
                labels.add(categoria);
                values.add(rs.getInt("total"));
            }

        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().print("{\"error\":\"Database error\"}");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append('{');
        sb.append("\"labels\":[");
        for (int i = 0; i < labels.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append('"').append(escape(labels.get(i))).append('"');
        }
        sb.append("],\"values\":[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(values.get(i));
        }
        sb.append(" ] }");

        resp.getWriter().print(sb.toString());
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
