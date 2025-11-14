package tienda.supernova.filters;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

@WebFilter("/admin/*")
public class AdminFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        HttpSession session = req.getSession(false);
        String ctx = req.getContextPath();
        String uri = req.getRequestURI();
        String role = session != null ? (String) session.getAttribute("role") : null;

        if (session == null || role == null) {
            res.sendRedirect(ctx + "/Login.jsp?admin=required");
            return;
        }

        if ("admin".equalsIgnoreCase(role) || "supervisor".equalsIgnoreCase(role)) {
            chain.doFilter(request, response);
            return;
        }

        if ("operario".equalsIgnoreCase(role) || "user".equalsIgnoreCase(role)) {

            if (uri.startsWith(ctx + "/admin/dashboard")
                    || uri.startsWith(ctx + "/admin/orders")
                    || uri.startsWith(ctx + "/admin/route-optimization")
                    || uri.startsWith(ctx + "/admin/product-validation")
                    || uri.startsWith(ctx + "/admin/notifications")
                    || uri.startsWith(ctx + "/admin/api/orders")
                    || uri.startsWith(ctx + "/admin/api/productos")
                    || uri.startsWith(ctx + "/admin/api/products")
                    || uri.startsWith(ctx + "/admin/account")
                    || uri.startsWith(ctx + "/admin/api/account")
                    || uri.contains(ctx + "/admin/product")
                    || uri.contains(ctx + "/admin/api/product")) {
                chain.doFilter(request, response);
                return;
            } else {
                res.sendRedirect(ctx + "/admin/dashboard");
                return;
            }
        }

        res.sendRedirect(ctx + "/Login.jsp?admin=required");
    }
}
