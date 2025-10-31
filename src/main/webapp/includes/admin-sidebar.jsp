<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    String ctx = request.getContextPath();
%>
<aside class="admin-sidebar">
    <div class="brand">
        <div class="logo">SN</div>
        <div class="title">SuperNova</div>
    </div>
    <nav>
    <a class="nav-link" href="<%= ctx %>/admin/dashboard.jsp" data-slug="dashboard">
            <svg viewBox="0 0 24 24" aria-hidden="true"><rect x="3" y="3" width="18" height="18" rx="3"/></svg>
            <span>Dashboard</span>
        </a>
    <a class="nav-link" href="<%= ctx %>/admin/orders.jsp" data-slug="orders">
            <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M3 6h18v2H3zM5 10h14v10H5z"/></svg>
            <span>Gestión de Pedidos</span>
        </a>
    <%
        jakarta.servlet.http.HttpSession _s = request.getSession(false);
        String _role = _s != null ? (String) _s.getAttribute("role") : null;
        if (_role != null && (_role.equalsIgnoreCase("admin") || _role.equalsIgnoreCase("supervisor"))) {
    %>
    <a class="nav-link" href="<%= ctx %>/admin/users.jsp" data-slug="users">
            <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 12c2.7 0 5-2.3 5-5s-2.3-5-5-5-5 2.3-5 5 2.3 5 5 5zm0 2c-3.3 0-10 1.7-10 5v2h20v-2c0-3.3-6.7-5-10-5z"/></svg>
            <span>Gestionar Usuarios</span>
        </a>
    <% } %>
    <a class="nav-link" href="<%= ctx %>/admin/route-optimization.jsp" data-slug="route-optimization">
            <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 2v6l4 2v6l-4 2v6"/></svg>
            <span>Optimización de Ruta</span>
        </a>
    <a class="nav-link" href="<%= ctx %>/admin/product-validation.jsp" data-slug="product-validation">
            <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M9 11l3 3L22 4"/></svg>
            <span>Validación de Productos</span>
        </a>
    <a class="nav-link" href="<%= ctx %>/admin/notifications.jsp" data-slug="notifications">
            <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 2a6 6 0 0 0-6 6v5l-2 2h16l-2-2V8a6 6 0 0 0-6-6z"/></svg>
            <span>Notificaciones</span>
        </a>
    </nav>
</aside>

<script>
    (function(){
        try{
            var path = location.pathname || '';
            var parts = path.split('/');
            var last = parts.length ? parts[parts.length-1] : '';
            var links = document.querySelectorAll('.admin-sidebar .nav-link');
            links.forEach(function(a){
                a.classList.remove('active');
                var slug = a.getAttribute('data-slug') || '';
                if (slug) {
                    if (path.indexOf('/admin/'+slug) !== -1 || path.indexOf('/admin/'+slug+'.jsp') !== -1 || last === slug || last === slug + '.jsp') {
                        a.classList.add('active');
                    }
                }
            });
        }catch(e){}
    })();
</script>
