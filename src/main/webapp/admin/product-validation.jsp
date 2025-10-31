<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    String ctx = request.getContextPath();
%>
<!doctype html>
<html>
<head>
    <meta charset="utf-8" />
    <title>Validación de Productos</title>
    <link rel="stylesheet" href="<%= ctx %>/css/style.css" />
</head>
<body>
<jsp:include page="../header.jsp" />
<div class="admin-layout">
    <jsp:include page="../includes/admin-sidebar.jsp" />
    <main class="admin-main">
        <div class="admin-container">
            <div class="dashboard-hero">
                <h1>Validación de Productos</h1>
                <p>Panel para revisar nuevos productos, validar imágenes y descripciones antes de publicar.</p>
            </div>

            <div class="panel">
                <h3>Productos pendientes de validación</h3>
                <p class="muted">No hay productos pendientes en este momento. En una futura versión mostraremos miniaturas y acciones rápidas.</p>
            </div>
        </div>
    </main>
</div>
</body>
</html>
