<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    String ctx = request.getContextPath();
%>
<!doctype html>
<html>
<head>
    <meta charset="utf-8" />
    <title>Admin Dashboard</title>
    <link rel="stylesheet" href="<%= ctx %>/css/style.css" />
</head>
<body>
<jsp:include page="../header.jsp" />
<div class="admin-layout">
    <jsp:include page="../includes/admin-sidebar.jsp" />

    <main class="admin-main">
        <div class="admin-container">
            <div class="dashboard-hero">
                <h1>Panel de Administración</h1>
                <p>Bienvenido al panel. Desde aquí puedes gestionar los recursos principales de la tienda.</p>
            </div>

            <div class="kpi-grid">
        <div class="kpi-card">
            <div class="num"><%= request.getAttribute("totalUsers") != null ? request.getAttribute("totalUsers") : "--" %></div>
            <div>
                <div class="label">Usuarios</div>
                <div class="muted">Cantidad total de usuarios</div>
            </div>
        </div>
        <div class="kpi-card">
            <div class="num"><%= request.getAttribute("totalProducts") != null ? request.getAttribute("totalProducts") : "--" %></div>
            <div>
                <div class="label">Productos</div>
                <div class="muted">Cantidad total de productos</div>
            </div>
        </div>
        <div class="kpi-card">
            <div class="num"><%= request.getAttribute("pendingOrders") != null ? request.getAttribute("pendingOrders") : "--" %></div>
            <div>
                <div class="label">Órdenes</div>
                <div class="muted">Órdenes pendientes</div>
            </div>
        </div>
    </div>

    <div class="actions-grid">
        <a class="action-card" href="<%= ctx %>/admin/orders.jsp"><div style="width:42px;height:42px;border-radius:8px;background:#6a1b9a;margin-right:10px"></div><div>Gestión de Pedidos</div></a>
        <a class="action-card" href="<%= ctx %>/admin/route-optimization.jsp"><div style="width:42px;height:42px;border-radius:8px;background:#f57c00;margin-right:10px"></div><div>Optimización de Ruta</div></a>
        <a class="action-card" href="<%= ctx %>/admin/product-validation.jsp"><div style="width:42px;height:42px;border-radius:8px;background:#00796b;margin-right:10px"></div><div>Validación de Productos</div></a>
        <a class="action-card" href="<%= ctx %>/admin/notifications.jsp"><div style="width:42px;height:42px;border-radius:8px;background:#455a64;margin-right:10px"></div><div>Notificaciones</div></a>
        <% 
            jakarta.servlet.http.HttpSession _s = request.getSession(false);
            String _role = _s != null ? (String) _s.getAttribute("role") : null;
            if (_role != null && (_role.equalsIgnoreCase("admin") || _role.equalsIgnoreCase("supervisor"))) {
        %>
        <a class="action-card" href="<%= ctx %>/admin/users.jsp"><div style="width:42px;height:42px;border-radius:8px;background:#1976d2;margin-right:10px"></div><div>Gestionar Usuarios</div></a>
        <% } %>
    </div>
        </div>
    </main>

</div>

<script>window.APP_CTX = '<%= ctx %>';</script>
</body>
</html>
