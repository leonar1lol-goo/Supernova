<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    String ctx = request.getContextPath();
%>
<!doctype html>
<html>
<head>
    <meta charset="utf-8" />
    <title>Optimización de Ruta</title>
    <link rel="stylesheet" href="<%= ctx %>/css/style.css" />
</head>
<body>
<jsp:include page="../header.jsp" />
<div class="admin-layout">
    <jsp:include page="../includes/admin-sidebar.jsp" />
    <main class="admin-main">
        <div class="admin-container">
            <div class="dashboard-hero">
                <h1>Optimización de Ruta</h1>
                <p>Página para optimizar rutas de reparto. Aquí puedes ver rutas propuestas y tiempos estimados.</p>
            </div>

            <div class="panel">
                <h3>Listado de rutas (placeholder)</h3>
                <p class="muted">Esta es una página de ejemplo. Puedo integrar algoritmo de optimización y mapa en una siguiente iteración.</p>
                <div style="margin-top:12px">
                    <table class="admin-table">
                        <thead><tr><th>ID</th><th>Ruta</th><th>Paradas</th><th class="text-center">Tiempo estimado</th></tr></thead>
                        <tbody>
                            <tr><td>R-001</td><td>Zona Norte</td><td>5</td><td class="text-center">45 min</td></tr>
                            <tr><td>R-002</td><td>Centro</td><td>8</td><td class="text-center">78 min</td></tr>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </main>
</div>
</body>
</html>
