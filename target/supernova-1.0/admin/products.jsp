<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.*" %>
<%@ page import="tienda.supernova.utils.PriceUtils" %>
<%@ page import="java.sql.Connection,java.sql.PreparedStatement,java.sql.ResultSet,java.sql.SQLException" %>
<%@ page import="tienda.supernova.db.DBConnection" %>
<%
    List<Map<String, Object>> products = (List<Map<String, Object>>) request.getAttribute("products");
    if (products == null) products = new ArrayList<>();
    Map<String, Object> editProduct = (Map<String, Object>) request.getAttribute("editProduct");
    String ctx = request.getContextPath();
%>
<!doctype html>
<html>
<head>
    <meta charset="utf-8" />
    <title>Admin - Productos</title>
    <link rel="stylesheet" href="<%= ctx %>/css/style.css" />
</head>
<body>
<jsp:include page="../header.jsp" />
<div class="admin-layout">
    <jsp:include page="../includes/admin-sidebar.jsp" />

    <main class="admin-main">
        <div class="admin-container">

            <div class="admin-grid">
        <div class="admin-form users-panel">
            <h2><%= (editProduct != null) ? "Editar producto" : "Agregar producto" %></h2>
            <form class="admin-form-inner admin-form" action="<%= ctx %>/admin/products" method="post" enctype="multipart/form-data">
        <input type="hidden" name="action" value="<%= (editProduct != null) ? "update" : "create" %>" />
        <% if (editProduct != null) { %>
            <input type="hidden" name="id" value="<%= editProduct.get("id") %>" />
        <% } %>
        <div class="form-grid">
            <div>
                <label class="small">Nombre</label>
                <input type="text" name="nombre" required value="<%= (editProduct != null ? editProduct.get("nombre") : "") %>"/>
            </div>
            <div>
                <label class="small">Marca</label>
                <input type="text" name="marca" value="<%= (editProduct != null ? editProduct.get("marca") : "") %>"/>
            </div>
            <div>
                <label class="small">Modelo</label>
                <input type="text" name="modelo" value="<%= (editProduct != null ? editProduct.get("modelo") : "") %>"/>
            </div>
            <div>
                <label class="small">Categoria</label>
                <select name="categoria">
                    <%
                        String selectedCat = (editProduct != null && editProduct.get("categoria") != null) ? String.valueOf(editProduct.get("categoria")) : "";
                        // canonical categories we want to show even if DB currently has none
                        List<String> canonical = Arrays.asList("procesador", "tarjeta_grafica", "memoria", "almacenamiento", "fuentes", "gabinetes", "placa_madre");
                        java.util.Set<String> categorySet = new java.util.LinkedHashSet<>();
                        categorySet.addAll(canonical);
                        try (Connection dbc = DBConnection.getConnection();
                             PreparedStatement cps = dbc.prepareStatement("SELECT DISTINCT categoria FROM productos ORDER BY categoria");
                             ResultSet crs = cps.executeQuery()) {
                            while (crs.next()) {
                                String c = crs.getString(1);
                                if (c != null && !c.trim().isEmpty()) categorySet.add(c.trim());
                            }
                        } catch (SQLException e) {
                            // ignore and fall back to canonical only
                        }

                        for (String val : categorySet) {
                            String label = val.replace('_', ' ');
                            // human friendly mapping
                            if ("procesador".equalsIgnoreCase(val) || "procesadores".equalsIgnoreCase(val)) label = "Procesadores";
                            else if (val.toLowerCase().contains("tarjeta") || val.toLowerCase().contains("grafica") || val.toLowerCase().contains("tarjeta_grafica")) label = "Tarjetas Gráficas";
                            else if (val.toLowerCase().contains("memoria") || val.toLowerCase().contains("memorias") ) label = "Memorias RAM";
                            else if (val.toLowerCase().contains("almacenamiento")) label = "Almacenamiento";
                            else if (val.toLowerCase().contains("fuente")) label = "Fuentes de Poder";
                            else if (val.toLowerCase().contains("gabinete")) label = "Gabinetes";
                            else if (val.toLowerCase().contains("placa") || val.toLowerCase().contains("madre")) label = "Placas Madre";
                    %>
                    <option value="<%= val %>" <%= val.equals(selectedCat) ? "selected" : "" %>><%= label %></option>
                    <%
                        }
                    %>
                </select>
            </div>
            <div>
                <label class="small">Precio</label>
                <input type="number" step="0.01" name="precio" required value="<%= (editProduct != null ? editProduct.get("precio") : "0.00") %>"/>
            </div>
            <div>
                <label class="small">Stock</label>
                <input type="number" name="stock" value="<%= (editProduct != null ? editProduct.get("stock") : "0") %>"/>
            </div>

            <div class="full-width">
                <label class="small">Imagen</label>
                <input type="file" name="imagen" accept="image/*" />
                <% if (editProduct != null && editProduct.get("ruta_imagen") != null) { %>
                    <div class="current-image">Imagen actual: <img src="<%= ctx %>/<%= editProduct.get("ruta_imagen") %>" style="max-height:80px;"/></div>
                <% } %>
            </div>

            <div class="full-width">
                <label class="small">Descripcion</label>
                <textarea name="descripcion" rows="4"><%= (editProduct != null ? editProduct.get("descripcion") : "") %></textarea>
            </div>
        </div>

        <div class="controls-right save-row" style="margin-top:8px">
            <button type="submit" class="btn-save"> <%= (editProduct != null) ? "Actualizar" : "Crear" %></button>
            <% if (editProduct != null) { %>
                <a href="<%= ctx %>/admin/products" class="btn-ghost">Cancelar</a>
            <% } %>
        </div>
            </form>
        </div>

        <div class="admin-table table-scroll">
            <div style="display:flex;justify-content:space-between;align-items:center;gap:12px">
                <h2 style="margin:0">Lista de productos</h2>
                <div style="display:flex;gap:8px;align-items:center">
                    <input id="userSearch" class="search-input" placeholder="Buscar usuarios (nombre, email)" />
                    <select id="userSort" class="sort-select">
                        <option value="id">Orden: ID</option>
                        <option value="nombre">Orden: Nombre</option>
                        <option value="precio">Orden: Precio (asc)</option>
                        <option value="precio_desc">Orden: Precio (desc)</option>
                    </select>
                </div>
            </div>

            <table id="productsTable" class="admin-table" style="margin-top:12px" cellpadding="6">
        <thead>
        <tr>
            <th>Imagen</th>
            <th>ID</th>
            <th>Nombre</th>
            <th>Marca</th>
            <th>Modelo</th>
            <th>Categoria</th>
            <th>Precio</th>
            <th>Stock</th>
            <th>Activo</th>
            <th class="text-center">Acciones</th>
        </tr>
        </thead>
        <tbody>
        <% for (Map<String, Object> p : products) { %>
            <tr>
                <td class="image-col">
                    <% if (p.get("ruta_imagen") != null) { %>
                        <img src="<%= ctx %>/<%= p.get("ruta_imagen") %>" alt="" />
                    <% } else { %>
                        <div class="no-image">No image</div>
                    <% } %>
                </td>
                <td class="id-col"><%= p.get("id") %></td>
                <td class="name-col"><strong><%= p.get("nombre") %></strong></td>
                <td class="marca-col"><%= p.get("marca") %></td>
                <td class="model-col"><%= p.get("modelo") %></td>
                <td class="categoria-col"><%= p.get("categoria") %></td>
                <td class="text-right price-col"><%= (p.get("precio") instanceof java.math.BigDecimal) ? PriceUtils.formatSoles((java.math.BigDecimal)p.get("precio")) : ("S/ " + String.valueOf(p.get("precio"))) %></td>
                <td class="text-right stock-col"><%= p.get("stock") %></td>
                <td class="text-center activo-col"><%= (Boolean.TRUE.equals(p.get("activo")) ? "Sí" : "No") %></td>
                <td class="text-center actions-col">
                    <div class="action-forms">
                        <form action="<%= ctx %>/admin/products" method="post" style="display:inline-block;">
                            <input type="hidden" name="action" value="toggle" />
                            <input type="hidden" name="id" value="<%= p.get("id") %>" />
                            <button class="action-btn btn-toggle small-btn" title="<%= (Boolean.TRUE.equals(p.get("activo"))) ? "Desactivar" : "Activar" %>" type="submit">
                                
                                <span class="label"><%= (Boolean.TRUE.equals(p.get("activo"))) ? "Desactivar" : "Activar" %></span>
                            </button>
                        </form>
                        <a class="action-btn btn-edit small-btn" title="Editar" href="<%= ctx %>/admin/products?edit=<%= p.get("id") %>"> Editar</a>
                        <form action="<%= ctx %>/admin/products" method="post" onsubmit="return confirm('Eliminar?')" style="display:inline-block;">
                            <input type="hidden" name="action" value="delete" />
                            <input type="hidden" name="id" value="<%= p.get("id") %>" />
                            <button class="action-btn btn-danger small-btn" title="Eliminar" type="submit"> Eliminar</button>
                        </form>
                    </div>
                </td>
            </tr>
        <% } %>
        </tbody>
            </table>
            </div>
    </div>

    <script>
        (function(){
            const input = document.getElementById('userSearch');
            const sort = document.getElementById('userSort');
            const table = document.getElementById('productsTable');
            if (!table) return;
            const tbody = table.querySelector('tbody');
            const rows = Array.from(tbody.querySelectorAll('tr'));

            function normalize(t){ return (t||'').toString().toLowerCase(); }

            if (input) input.addEventListener('input', function(){
                const q = normalize(this.value);
                rows.forEach(r=>{
                    const cells = Array.from(r.querySelectorAll('td'));
                    const hay = [cells[2], cells[3], cells[4]].some(c => normalize(c && c.textContent).includes(q));
                    r.style.display = hay ? '' : 'none';
                });
            });

            if (sort) sort.addEventListener('change', function(){
                const v = this.value;
                const sorted = rows.slice().sort((a,b)=>{
                    if (v==='id') return Number(a.children[1].textContent) - Number(b.children[1].textContent);
                    if (v==='nombre') return a.children[2].textContent.localeCompare(b.children[2].textContent);
                    if (v==='precio') return Number(a.children[6].textContent.replace(/[^0-9.-]+/g, '')) - Number(b.children[6].textContent.replace(/[^0-9.-]+/g, ''));
                    if (v==='precio_desc') return Number(b.children[6].textContent.replace(/[^0-9.-]+/g, '')) - Number(a.children[6].textContent.replace(/[^0-9.-]+/g, ''));
                    return 0;
                });
                sorted.forEach(r=> tbody.appendChild(r));
            });
        })();
    </script>
    
</div>
</body>
</html>

