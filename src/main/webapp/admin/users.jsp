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
        <div class="modal" role="dialog" aria-modal="true" style="width:420px;max-width:calc(100% - 32px);padding:22px;border-radius:10px">
            <h3>Editar usuario</h3>
            <div class="register-container">
                <div class="form-row">
                    <label for="euEmail">Correo electrónico</label>
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
                <div class="form-row" style="position:relative">
                    <label for="euPassword">Contraseña (dejar en blanco para no cambiar)</label>
                    <input id="euPassword" type="password" />
                    <button id="euToggleEditPw" type="button" style="position:absolute;right:8px;top:36px;background:transparent;border:0;cursor:pointer" aria-label="Mostrar contraseña">
                        <svg width="20" height="20" viewBox="0 0 24 24" fill="none"><path d="M12 5c-7 0-11 6-11 7s4 7 11 7 11-6 11-7-4-7-11-7z" stroke="#6b7280" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round"/><circle cx="12" cy="12" r="3" stroke="#6b7280" stroke-width="1.2"/></svg>
                    </button>
                </div>
                <div class="password-rules" style="margin-top:6px">
                    <div class="rule" id="eruleLength">Al menos 8 caracteres</div>
                    <div class="rule" id="eruleUpper">Al menos una letra mayúscula</div>
                    <div class="rule" id="eruleLower">Al menos una letra minúscula</div>
                    <div class="rule" id="eruleSpecial">Al menos un carácter especial</div>
                    <div class="rule" id="eruleNumber">Al menos un número</div>
                </div>
                <div class="actions" style="margin-top:14px">
                    <button id="euCancel" class="btn-ghost">Cancelar</button>
                    <button id="euSave" class="btn-save">Guardar</button>
                </div>
            </div>
        </div>
    </div>
    <div id="createUserModal" class="modal-backdrop" style="display:none;align-items:center;justify-content:center;z-index:1200">
        <div class="modal" role="dialog" aria-modal="true" style="width:420px;max-width:calc(100% - 32px);padding:22px;border-radius:10px">
            <h3>Crear nueva cuenta</h3>
            <div class="register-container">
                <div class="form-row">
                    <label for="cuEmail">Correo electrónico</label>
                    <input id="cuEmail" type="email" placeholder="tucorreo@ejemplo.com" />
                </div>
                <div class="form-row" style="display:grid;grid-template-columns:1fr 1fr;gap:8px">
                    <div>
                        <label for="cuFirstName">Nombre(s)</label>
                        <input id="cuFirstName" type="text" />
                    </div>
                    <div>
                        <label for="cuLastName">Apellido(s)</label>
                        <input id="cuLastName" type="text" />
                    </div>
                </div>
                <div class="form-row" style="position:relative">
                    <label for="cuPassword">Contraseña</label>
                    <input id="cuPassword" type="password" />
                    <button id="cuTogglePw" type="button" style="position:absolute;right:8px;top:36px;background:transparent;border:0;cursor:pointer" aria-label="Mostrar contraseña">
                        <svg width="20" height="20" viewBox="0 0 24 24" fill="none"><path d="M12 5c-7 0-11 6-11 7s4 7 11 7 11-6 11-7-4-7-11-7z" stroke="#6b7280" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round"/><circle cx="12" cy="12" r="3" stroke="#6b7280" stroke-width="1.2"/></svg>
                    </button>
                </div>
                <div class="password-rules" style="margin-top:6px">
                    <div class="rule" id="ruleLength">Al menos 8 caracteres</div>
                    <div class="rule" id="ruleUpper">Al menos una letra mayúscula</div>
                    <div class="rule" id="ruleLower">Al menos una letra minúscula</div>
                    <div class="rule" id="ruleSpecial">Al menos un carácter especial</div>
                    <div class="rule" id="ruleNumber">Al menos un número</div>
                </div>
                <div style="margin-top:12px;display:flex;justify-content:space-between;align-items:center;gap:12px">
                    <div style="flex:1">
                        <select id="cuRol" style="width:100%;padding:10px;border:1px solid #e6e9ee;border-radius:8px">
                            <option value="admin">Administrador</option>
                            <option value="operario">Operario</option>
                            <option value="supervisor">Supervisor</option>
                        </select>
                    </div>
                </div>
                <div class="actions" style="margin-top:14px">
                    <button id="cuCancel" class="btn-ghost">Cancelar</button>
                    <button id="cuCreate" class="btn-save" disabled style="background:#f97316;border:0;color:#fff;padding:12px 16px;border-radius:10px">Aceptar y registrarse</button>
                </div>
            </div>
        </div>
    </div>
    <script src="<%= ctx %>/js/admin-users.js"></script>
</div>
</body>
</html>
