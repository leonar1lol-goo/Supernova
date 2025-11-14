<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    String ctx = request.getContextPath();
%>
<%
    jakarta.servlet.http.HttpSession _s = request.getSession(false);
    String _role = _s != null ? (String) _s.getAttribute("role") : null;
    // Allow any authenticated user to view clientes page. Redirect to login if not authenticated.
    if (_role == null) {
        response.sendRedirect(ctx + "/Login.jsp?admin=required");
        return;
    }
%>
<!doctype html>
<html>
<head>
    <meta charset="utf-8" />
    <title>Admin - Clientes</title>
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
                    <h2>Clientes</h2>
                    <p class="muted">Listado de clientes registrado en el sistema.</p>
                    <div style="margin-top:12px">
                        <input id="clientSearch" class="search-input" placeholder="Buscar clientes (nombre, email, teléfono)" />
                    </div>
                </div>

                <div class="admin-table table-scroll">
                    <div style="display:flex;justify-content:space-between;align-items:center;gap:12px">
                        <h2 style="margin:0">Lista de clientes</h2>
                        <div>
                            <button id="btnNewClient" class="btn-primary" style="margin-left:8px">Nuevo Cliente</button>
                        </div>
                    </div>

                    <table id="clientsTable" class="admin-table" style="margin-top:12px">
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>Nombre</th>
                                <th>DNI / RUC</th>
                                <th>Dirección</th>
                                <th>Email</th>
                                <th>Teléfono</th>
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
    <script src="<%= ctx %>/js/admin-clientes.js"></script>
    <div id="createClientModal" class="modal-backdrop" style="display:none;align-items:center;justify-content:center;z-index:1200">
        <div class="modal" role="dialog" aria-modal="true" style="width:520px;max-width:calc(100% - 32px);padding:22px;border-radius:10px">
            <h3>Crear nuevo cliente</h3>
            <div class="register-container">
                <div class="form-row">
                    <label for="ccNombre">Nombre</label>
                    <input id="ccNombre" type="text" />
                </div>
                <div class="form-row">
                    <label for="ccDireccion">Dirección</label>
                    <input id="ccDireccion" type="text" />
                </div>
                <div class="form-row">
                    <label for="ccTelefono">Teléfono</label>
                    <input id="ccTelefono" type="tel" inputmode="numeric" pattern="\\d{1,9}" maxlength="9" />
                </div>
                <div class="form-row">
                    <label for="ccDni">DNI / RUC</label>
                    <input id="ccDni" type="text" inputmode="numeric" pattern="\\d{1,11}" maxlength="11" />
                </div>
                <div class="form-row">
                    <label for="ccEmail">Email</label>
                    <input id="ccEmail" type="email" />
                </div>
                <div class="actions" style="margin-top:14px">
                    <button id="ccCancel" class="btn-ghost">Cancelar</button>
                    <button id="ccCreate" class="btn-save" style="background:#f97316;border:0;color:#fff;padding:10px 14px;border-radius:8px">Crear</button>
                </div>
            </div>
        </div>
    </div>
    <div id="editClientModal" class="modal-backdrop" style="display:none;align-items:center;justify-content:center;z-index:1200">
        <div class="modal" role="dialog" aria-modal="true" style="width:520px;max-width:calc(100% - 32px);padding:22px;border-radius:10px">
            <h3>Editar cliente</h3>
            <div class="register-container">
                <input id="ecId" type="hidden" />
                
                <div class="form-row">
                    <label for="ecTelefono">Teléfono</label>
                    <input id="ecTelefono" type="tel" inputmode="numeric" pattern="\\d{1,9}" maxlength="9" />
                </div>
                <div class="form-row">
                    <label for="ecDireccion">Dirección</label>
                    <input id="ecDireccion" type="text" />
                </div>
                <div class="form-row">
                    <label for="ecEmail">Email</label>
                    <input id="ecEmail" type="email" />
                </div>
                <div class="actions" style="margin-top:14px">
                    <button id="ecCancel" class="btn-ghost">Cancelar</button>
                    <button id="ecSave" class="btn-save">Guardar</button>
                </div>
            </div>
        </div>
    </div>
</div>
</body>
</html>
