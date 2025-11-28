<%@ page contentType="text/html;charset=UTF-8" language="java" %> <% String ctx
= request.getContextPath(); jakarta.servlet.http.HttpSession _s =
request.getSession(false); String _role = _s != null ? (String)
_s.getAttribute("role") : null; if (_role == null ||
!_role.equalsIgnoreCase("admin")) { response.sendRedirect(ctx +
"/Login.jsp?admin=required"); return; } %>
<!DOCTYPE html>
<html>
  <head>
    <meta charset="utf-8" />
    <title>Admin - Reportes</title>
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
              <h2>Reportes</h2>
              <p class="muted">
                Genera reportes administrativos, incluyendo registros eliminados
                lógicamente.
              </p>
            </div>
            <div class="admin-table table-scroll">
              <div
                style="
                  display: flex;
                  justify-content: space-between;
                  align-items: center;
                "
              >
                <h2 style="margin: 0">Reportes Disponibles</h2>
              </div>

              <div style="margin-top: 16px">
                <div
                  style="
                    display: grid;
                    grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
                    gap: 12px;
                  "
                >
                  <div
                    style="
                      padding: 16px;
                      background: #fff;
                      border-radius: 8px;
                      box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
                    "
                  >
                    <h3>Reporte de pedidos</h3>
                    <p class="muted">Ir a la pantalla de gestión de pedidos.</p>
                    <div style="margin-top: 12px">
                      <button id="btnOpenOrders" class="btn-ghost">
                        Ir a Pedidos
                      </button>
                    </div>
                  </div>

                  <div
                    style="
                      padding: 16px;
                      background: #fff;
                      border-radius: 8px;
                      box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
                    "
                  >
                    <h3>Reporte por fechas</h3>
                    <p class="muted">
                      Abrir el selector de rango en la pantalla de pedidos.
                    </p>
                    <div style="margin-top: 12px">
                      <button id="btnOpenOrdersDate" class="btn-save">
                        Abrir selector de fechas
                      </button>
                    </div>
                  </div>

                  <div
                    style="
                      padding: 16px;
                      background: #fff;
                      border-radius: 8px;
                      box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
                    "
                  >
                    <h3>Registros eliminados (lógicos)</h3>
                    <p class="muted">
                      Genera un PDF con registros eliminados en clientes,
                      productos y usuarios.
                    </p>
                    <div style="margin-top: 12px">
                      <button id="btnGenerateDeletedReport" class="btn-save">
                        Generar reporte de eliminados
                      </button>
                    </div>
                  </div>

                  <div
                    style="
                      padding: 16px;
                      background: #fff;
                      border-radius: 8px;
                      box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
                    "
                  >
                    <h3>Reporte de stock</h3>
                    <p class="muted">
                      Genera un PDF con el stock de productos.
                    </p>
                    <div style="margin-top: 12px">
                      <button id="btnGenerateStockReport" class="btn-ghost">
                        Generar reporte de stock
                      </button>
                    </div>
                  </div>

                  <div
                    style="
                      padding: 16px;
                      background: #fff;
                      border-radius: 8px;
                      box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
                    "
                  >
                    <h3>Máximo / Mínimo / Promedio (Stock)</h3>
                    <p class="muted">
                      Calcula máximo, mínimo y promedio sobre columnas numéricas
                      de productos.
                    </p>
                    <div style="margin-top: 12px">
                      <button id="btnGenerateStockStats" class="btn-save">
                        Generar reporte de estadísticas
                      </button>
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
        document.addEventListener("DOMContentLoaded", function () {
          var base = window.APP_CTX || "";

          var b1 = document.getElementById("btnOpenOrders");
          if (b1) {
            b1.addEventListener("click", function (ev) {
              ev.preventDefault();
              window.location.href = base + "/admin/orders.jsp";
            });
          }

          var b2 = document.getElementById("btnOpenOrdersDate");
          if (b2) {
            b2.addEventListener("click", function (ev) {
              ev.preventDefault();
              window.location.href = base + "/admin/orders.jsp?openDate=1";
            });
          }

          var b3 = document.getElementById("btnGenerateDeletedReport");
          if (b3) {
            b3.addEventListener("click", function (ev) {
              ev.preventDefault();
              var url = base + "/admin/report/deleted";
              window.open(url, "_blank");
            });
          }

          var b4 = document.getElementById("btnGenerateStockReport");
          if (b4) {
            b4.addEventListener("click", function (ev) {
              ev.preventDefault();
              var url = base + "/admin/report/stock";
              window.open(url, "_blank");
            });
          }

          var b5 = document.getElementById("btnGenerateStockStats");
          if (b5) {
            b5.addEventListener("click", function (ev) {
              ev.preventDefault();
              var url = base + "/admin/report/stock-stats";
              window.open(url, "_blank");
            });
          }
        });
      </script>
    </div>
  </body>
</html>
