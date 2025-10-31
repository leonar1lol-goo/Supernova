<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    String ctx = request.getContextPath();
%>
<%
    // Prevent non-admin/supervisor from accessing this page directly
    jakarta.servlet.http.HttpSession _s = request.getSession(false);
    String _role = _s != null ? (String) _s.getAttribute("role") : null;
    if (_role == null || !(_role.equalsIgnoreCase("admin") || _role.equalsIgnoreCase("supervisor"))) {
        response.sendRedirect(ctx + "/admin/dashboard");
        return;
    }
%>
<!doctype html>
<html>
<head>
    <meta charset="utf-8" />
    <title>Admin - Usuarios</title>
    <link rel="stylesheet" href="<%= ctx %>/css/style.css" />
    <meta name="viewport" content="width=device-width,initial-scale=1" />
</head>
<body>
<jsp:include page="../header.jsp" />
<div class="admin-layout">
    <jsp:include page="../includes/admin-sidebar.jsp" />

    <main class="admin-main">
        <div class="admin-container">
            <div class="admin-grid">
                <div class="admin-form users-panel">
                    <h2>Usuarios</h2>
                    <p class="muted">Listado de usuarios cargado desde la base de datos (local).</p>
                    <div style="margin-top:12px">
                        <input id="userSearch" class="search-input" placeholder="Buscar usuarios (nombre, email)" />
                    </div>
                </div>

                <div class="admin-table table-scroll">
                    <div style="display:flex;justify-content:space-between;align-items:center;gap:12px">
                        <h2 style="margin:0">Lista de usuarios</h2>
                        <div>
                            <button id="btnNewUser" class="btn-primary" style="margin-left:8px">Nuevo Usuario</button>
                        </div>
                    </div>

                    <table id="usersTable" class="admin-table" style="margin-top:12px">
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>Nombre</th>
                                <th>Email</th>
                                <th>Rol</th>
                                <th>Activo</th>
                                <th class="text-center">Acciones</th>
                            </tr>
                        </thead>
                        <tbody>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </main>

    <script>window.APP_CTX = '<%= ctx %>';</script>
    <div id="editUserModal" class="modal-backdrop" style="display:none;align-items:center;justify-content:center;z-index:1200">
        <div class="modal" role="dialog" aria-modal="true" style="width:420px;max-width:calc(100% - 32px);">
            <h3>Editar Usuario</h3>
            <div class="form-row">
                <label for="euNombre">Nombre</label>
                <input id="euNombre" type="text" />
            </div>
            <div class="form-row">
                <label for="euEmail">Email</label>
                <input id="euEmail" type="email" />
            </div>
            <div class="form-row">
                <label for="euRol">Rol</label>
                <select id="euRol">
                    <option value="admin">Administrador</option>
                    <option value="operario">Operario</option>
                    <option value="supervisor">Supervisor</option>
                </select>
            </div>
            <div class="form-row">
                <label for="euPassword">Contraseña (dejar en blanco para no cambiar)</label>
                <input id="euPassword" type="password" />
            </div>
            <div class="actions">
                <button id="euCancel" class="btn-ghost">Cancelar</button>
                <button id="euSave" class="btn-save">Guardar</button>
            </div>
        </div>
    </div>
    <div id="createUserModal" class="modal-backdrop" style="display:none;align-items:center;justify-content:center;z-index:1200">
        <div class="modal" role="dialog" aria-modal="true" style="width:420px;max-width:calc(100% - 32px);">
            <h3>Nuevo Usuario</h3>
            <div class="form-row">
                <label for="cuNombre">Nombre</label>
                <input id="cuNombre" type="text" />
            </div>
            <div class="form-row">
                <label for="cuEmail">Email</label>
                <input id="cuEmail" type="email" />
            </div>
            <div class="form-row">
                <label for="cuRol">Rol</label>
                <select id="cuRol">
                    <option value="admin">Administrador</option>
                    <option value="operario">Operario</option>
                    <option value="supervisor">Supervisor</option>
                </select>
            </div>
            <div class="form-row">
                <label for="cuPassword">Contraseña</label>
                <input id="cuPassword" type="password" />
            </div>
            <div class="actions">
                <button id="cuCancel" class="btn-ghost">Cancelar</button>
                <button id="cuCreate" class="btn-save">Crear</button>
            </div>
        </div>
    </div>
    <script src="<%= ctx %>/js/admin-users.js"></script>
</div>
</body>
</html>
