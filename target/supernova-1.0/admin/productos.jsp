<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
  String ctx = request.getContextPath();
  jakarta.servlet.http.HttpSession _s = request.getSession(false);
  String _role = _s != null ? (String) _s.getAttribute("role") : null;
  /* Allow any authenticated role to view the products page.
     Redirect to login if no session/role. */
  if (_role == null) {
    response.sendRedirect(ctx + "/Login.jsp?admin=required");
    return;
  }
%>
<!DOCTYPE html>
<html>
  <head>
    <meta charset="utf-8" />
    <title>Admin - Productos</title>
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
              <h2>Productos</h2>
              <p class="muted">Listado de productos registrados en el sistema.</p>

            <div class="admin-table table-scroll">
              <div
                style="
                  display: flex;
                  justify-content: space-between;
                  align-items: center;
                  gap: 12px;
                "
              >
                <h2 style="margin: 0">Lista de productos</h2>
                <div style="display: flex; align-items: center; gap: 8px">
                  <% if (_role != null && (_role.equalsIgnoreCase("admin") ||
                  _role.equalsIgnoreCase("supervisor"))) { %>
                  <button
                    id="btnNewProduct"
                    class="btn-primary"
                    style="margin-left: 8px"
                  >
                    Nuevo Producto
                  </button>
                  <% } %>
                  <a
                    href="<%= ctx %>/admin/report/stock"
                    target="_blank"
                    class="btn-ghost"
                    style="margin-left: 6px"
                    >Generar reporte PDF</a
                  >
                </div>
              </div>

              <table
                id="productsTable"
                class="admin-table"
                style="margin-top: 12px"
              >
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Nombre</th>
                    <th>Código</th>
                    <th>Categoría</th>
                    <th>Stock</th>
                  </tr>
                </thead>
                <tbody></tbody>
              </table>

              <div
                id="createProductModal"
                class="modal-backdrop"
                style="
                  display: none;
                  align-items: center;
                  justify-content: center;
                  z-index: 1200;
                "
              >
                <div
                  class="modal"
                  role="dialog"
                  aria-modal="true"
                  style="
                    width: 520px;
                    max-width: calc(100% - 32px);
                    padding: 22px;
                    border-radius: 10px;
                  "
                >
                  <h3>Crear producto</h3>
                  <div class="register-container">
                    <div class="form-row">
                      <label for="pcNombre">Nombre</label>
                      <input id="pcNombre" type="text" />
                    </div>
                    <div class="form-row">
                      <label for="pcCodigo">Código de barra</label>
                      <input id="pcCodigo" type="text" />
                    </div>
                    <div class="form-row">
                      <label for="pcCategoria">Categoría</label>
                      <input id="pcCategoria" type="text" />
                    </div>
                    <div class="form-row">
                      <label for="pcStock">Stock</label>
                      <input id="pcStock" type="number" />
                    </div>
                    <div class="actions" style="margin-top: 14px">
                      <button id="pcCancel" class="btn-ghost">Cancelar</button>
                      <button
                        id="pcCreate"
                        class="btn-save"
                        style="
                          background: #f97316;
                          border: 0;
                          color: #fff;
                          padding: 10px 14px;
                          border-radius: 8px;
                        "
                      >
                        Crear
                      </button>
                    </div>
                  </div>
                </div>
              </div>

              <div
                id="editProductModal"
                class="modal-backdrop"
                style="
                  display: none;
                  align-items: center;
                  justify-content: center;
                  z-index: 1200;
                "
              >
                <div
                  class="modal"
                  role="dialog"
                  aria-modal="true"
                  style="
                    width: 520px;
                    max-width: calc(100% - 32px);
                    padding: 22px;
                    border-radius: 10px;
                  "
                >
                  <h3>Editar producto</h3>
                  <div class="register-container">
                    <input id="epId" type="hidden" />
                    <div class="form-row">
                      <label for="epNombre">Nombre</label>
                      <input id="epNombre" type="text" />
                    </div>
                    <div class="form-row">
                      <label for="epCodigo">Código de barra</label>
                      <input id="epCodigo" type="text" />
                    </div>
                    <div class="form-row">
                      <label for="epCategoria">Categoría</label>
                      <input id="epCategoria" type="text" />
                    </div>
                    <div class="form-row">
                      <label for="epStock">Stock</label>
                      <input id="epStock" type="number" />
                    </div>
                    <div class="actions" style="margin-top: 14px">
                      <button id="epCancel" class="btn-ghost">Cancelar</button>
                      <button id="epSave" class="btn-save">Guardar</button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </main>

      <script>
        window.APP_CTX = "<%= ctx %>";
      </script>
      <script src="<%= ctx %>/js/admin-productos.js"></script>
    </div>
  </body>
</html>
