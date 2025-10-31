<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    String ctx = request.getContextPath();
%>
<!doctype html>
<html>
<head>
    <meta charset="utf-8" />
    <title>Notificaciones</title>
    <link rel="stylesheet" href="<%= ctx %>/css/style.css" />
</head>
<body>
<jsp:include page="../header.jsp" />
<div class="admin-layout">
    <jsp:include page="../includes/admin-sidebar.jsp" />
    <main class="admin-main">
        <div class="admin-container">
            <div class="dashboard-hero">
                <h1>Notificaciones</h1>
                <p>Centro de notificaciones del sistema.</p>
            </div>

            <div class="panel">
                <h3>Últimas notificaciones</h3>
                <ul>
                    <li class="muted">[Ejemplo] Pedido 001 cambiado a En preparación</li>
                    <li class="muted">[Ejemplo] Usuario nuevo registrado: ana@ejemplo.local</li>
                </ul>
            </div>
        </div>
    </main>
</div>
</body>
</html>
