<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    String ctx = request.getContextPath();
%>
<!doctype html>
<html>
<head>
    <meta charset="utf-8" />
    <title>Gestión de Pedidos</title>
    <link rel="stylesheet" href="<%= ctx %>/css/style.css" />
    <meta name="viewport" content="width=device-width,initial-scale=1" />
</head>
<body>
<jsp:include page="../header.jsp" />
<div class="admin-layout">
    <jsp:include page="../includes/admin-sidebar.jsp" />
    <main class="admin-main">
        <div class="admin-container">
            <div class="dashboard-hero">
                <h1>Pantalla de Gestión de Pedidos</h1>
                <p>Lista de pedidos con buscador y filtros por estado (pendiente, en preparación, completado).</p>
            </div>

            <div class="panel orders-panel">
                <div style="display:flex;gap:8px;align-items:center;margin-bottom:12px">
                    <input id="orderSearch" class="search-input" placeholder="Buscar pedido..." style="flex:1;" />
                    <button id="btnNewOrder" class="btn-save" style="white-space:nowrap">+ Nuevo Pedido</button>
                </div>
                <div class="filter-group">
                    <button class="filter-btn active" data-state="all">Todos</button>
                    <button class="filter-btn" data-state="pendiente">Pendiente</button>
                    <button class="filter-btn" data-state="preparacion">En preparación</button>
                    <button class="filter-btn" data-state="completado">Completado</button>
                </div>

                <div class="admin-table table-scroll panel">
                    <table id="ordersTable">
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>Cliente</th>
                                <th>Estado</th>
                                <th class="text-center">Total</th>
                                <th class="text-center">Acciones</th>
                            </tr>
                        </thead>
                        <tbody>
                        </tbody>
                    </table>
                </div>
            </div>

            <div id="newOrderModal" class="modal-backdrop" aria-hidden="true">
                <div class="modal" role="dialog" aria-modal="true" aria-labelledby="moTitle">
                    <h3 id="moTitle">Crear Nuevo Pedido</h3>
                    <div class="form-row">
                        <label for="moClienteSelect">Seleccionar cliente existente:</label>
                        <select id="moClienteSelect">
                            <option value="">-- seleccionar cliente --</option>
                        </select>
                    </div>
                    <div class="form-row" style="margin-bottom:8px;display:flex;gap:12px;align-items:center">
                        <div style="flex:1">
                            <p class="label-muted" style="margin:0">Si el cliente no está registrado aún</p>
                        </div>
                        <div style="min-width:140px;display:flex;flex-direction:column;gap:6px">
                            <button id="moOpenClients" class="btn-ghost">Crear cliente</button>
                        </div>
                    </div>
                    <div class="form-row">
                        <label for="moProductoSelect">Agregar productos al pedido:</label>
                        <div class="product-select-row">
                            <select id="moProductoSelect">
                                <option value="">-- seleccionar producto --</option>
                            </select>
                            <div class="product-controls">
                                <input id="moProductoCantidad" type="number" min="1" placeholder="cantidad" />
                                <div id="moProductoInfo"></div>
                                <button id="moAddProducto" class="btn-ghost">Agregar</button>
                            </div>
                        </div>
                        <div id="moItemsList"></div>
                    </div>
                    <div class="actions">
                        <button id="moCancel" class="btn-ghost">Cancelar</button>
                        <button id="moSubmit" class="btn-save">Crear</button>
                    </div>
                </div>
            </div>

            <div id="editOrderModal" class="modal-backdrop" aria-hidden="true">
                <div class="modal" role="dialog" aria-modal="true" aria-labelledby="editTitle">
                    <h3 id="editTitle">Editar Pedido</h3>
                    <div class="form-row">
                        <label for="moEditEstado">Estado:</label>
                        <select id="moEditEstado">
                            <option value="pendiente">pendiente</option>
                            <option value="preparacion">en preparacion</option>
                            <option value="completado">completado</option>
                        </select>
                    </div>
                    <div class="form-row">
                        <label for="moEditPrioridad">Prioridad (opcional):</label>
                        <input id="moEditPrioridad" type="text" placeholder="Prioridad" />
                    </div>
                    <div class="form-row">
                        <label for="moEditClienteEmail">Email cliente (opcional):</label>
                        <input id="moEditClienteEmail" type="email" placeholder="cliente@ejemplo.com" />
                    </div>
                    <div class="actions">
                        <button id="moEditCancel" class="btn-ghost">Cancelar</button>
                        <button id="moEditSubmit" class="btn-save">Guardar</button>
                    </div>
                </div>
            </div>

            <div id="itemsModal" class="modal-backdrop" aria-hidden="true">
                <div class="modal" role="dialog" aria-modal="true" aria-labelledby="itemsTitle">
                    <h3 id="itemsTitle">Items del Pedido <span id="itemsOrderId"></span></h3>
                    <div class="form-row">
                        <div id="itemsList" style="max-height:180px;overflow:auto;padding:6px;border:1px solid #eef; border-radius:6px;background:#fafafa"></div>
                    </div>
                    <hr />
                    <div class="form-row">
                        <label for="itProducto">ID producto:</label>
                        <input id="itProducto" type="number" placeholder="id producto" />
                    </div>
                    <div class="form-row">
                        <label for="itCantidad">Cantidad:</label>
                        <input id="itCantidad" type="number" placeholder="cantidad" />
                    </div>
                    <div class="form-row">
                        <label for="itPrecio">Precio unitario (opcional):</label>
                        <input id="itPrecio" type="text" placeholder="0.00" />
                    </div>
                    <div class="actions">
                        <button id="itClose" class="btn-ghost">Cerrar</button>
                        <button id="itAdd" class="btn-save">Añadir Item</button>
                    </div>
                </div>
            </div>

            <div id="toastContainer" class="toast-container" aria-live="polite"></div>

            <div id="deleteModal" class="modal-backdrop" aria-hidden="true">
                <div class="modal" role="dialog" aria-modal="true" aria-labelledby="delTitle">
                    <h3 id="delTitle">Eliminar pedido</h3>
                    <div class="form-row">
                        <p id="delText">¿Eliminar pedido?</p>
                    </div>
                    <div class="actions">
                        <button id="delCancel" class="btn-ghost">Cancelar</button>
                        <button id="delConfirm" class="btn-danger">Eliminar</button>
                    </div>
                </div>
            </div>
        </div>
    </main>

    <script>window.APP_CTX = '<%= ctx %>';</script>
    <script>
        function showToast(msg, kind){
            try{
                var container = document.getElementById('toastContainer'); if(!container) return; var d = document.createElement('div'); d.className = 'toast '+(kind||''); d.textContent = msg; container.appendChild(d);
                setTimeout(function(){ d.style.transition='opacity 300ms'; d.style.opacity='0'; setTimeout(function(){ try{ container.removeChild(d); }catch(e){} },350); }, 3500);
            }catch(e){ try{ console.log('toast',msg); }catch(_){} }
        }

        (function(){
            function $(sel, ctx){ return (ctx||document).querySelector(sel); }
            function $all(sel, ctx){ return Array.from((ctx||document).querySelectorAll(sel)); }

            function escapeHtml(s){ return (s||'').toString().replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;'); }

            function renderRow(o){
                var tr = document.createElement('tr');
                var stateSlug = (o.estado||'').toLowerCase().replace(/\s+/g,'');
                tr.setAttribute('data-state', stateSlug);
                var totalDisplay = o.totalIsItems ? (o.total + ' items') : ('$' + (Number(o.total||0).toFixed(2)));
                tr.innerHTML = '<td>'+o.id+'</td>'+
                               '<td>'+escapeHtml(o.cliente)+'</td>'+
                               '<td>'+escapeHtml(o.estado)+'</td>'+
                               '<td class="text-center">'+totalDisplay+'</td>'+
                               '<td class="text-center">'
                               +'<button class="action-btn btn-edit small-btn" data-action="edit" data-id="'+o.id+'">Editar</button>'
                               +'<button class="action-btn btn-ghost small-btn" data-action="items" data-id="'+o.id+'">Items</button>'
                               +'<button class="action-btn btn-danger small-btn" data-action="delete" data-id="'+o.id+'">Eliminar</button>'
                               +'</td>';
                return tr;
            }

            function load(){
                var base = window.APP_CTX || '';
                fetch(base + '/admin/api/orders', {cache:'no-store'}).then(r=>r.json()).then(list=>{
                    var tbody = document.querySelector('#ordersTable tbody'); tbody.innerHTML = '';
                    list.forEach(function(o){ tbody.appendChild(renderRow(o)); });
                    applyFilter(currentFilter);
                }).catch(function(err){ console.error('orders load',err); });
            }

            var currentFilter = 'all';
            function applyFilter(state){
                currentFilter = state;
                $all('.filter-btn').forEach(function(b){ b.classList.toggle('active', b.getAttribute('data-state')===state); });
                $all('#ordersTable tbody tr').forEach(function(tr){ tr.style.display = (state==='all' || tr.getAttribute('data-state')===state) ? '' : 'none'; });
            }

            document.addEventListener('DOMContentLoaded', function(){
                var search = document.getElementById('orderSearch');
                $all('.filter-btn').forEach(function(btn){ btn.addEventListener('click', function(){ applyFilter(btn.getAttribute('data-state')); }); });

                var btnNew = document.getElementById('btnNewOrder');
                var modal = document.getElementById('newOrderModal');
                var moEmail = null;
                var moNombre = null;
                var moCancel = document.getElementById('moCancel');
                var moSubmit = document.getElementById('moSubmit');
                var moProductoSelect = document.getElementById('moProductoSelect');
                var moProductoCantidad = document.getElementById('moProductoCantidad');
                var moProductoInfo = document.getElementById('moProductoInfo');
                var moAddProducto = document.getElementById('moAddProducto');
                var moItemsList = document.getElementById('moItemsList');
                var moItems = [];
                var moProductosList = [];

                function openModal(){ if(!modal) return; modal.classList.add('modal-show'); modal.setAttribute('aria-hidden','false'); setTimeout(function(){ try{ if(moProductoSelect) moProductoSelect.focus(); }catch(e){} },50); document.addEventListener('keydown', escHandler); }
                function closeModal(){ if(!modal) return; modal.classList.remove('modal-show'); modal.setAttribute('aria-hidden','true'); document.removeEventListener('keydown', escHandler); }
                function escHandler(e){ if(e.key === 'Escape'){ closeModal(); } }

                if (modal){
                    modal.addEventListener('click', function(e){ if (e.target === modal) closeModal(); });
                }

                if (btnNew){ btnNew.addEventListener('click', function(){
                    if(moItemsList) moItemsList.innerHTML = '<i>(sin items)</i>';
                    if(moProductoInfo) moProductoInfo.innerHTML = '';
                    moItems = []; if(moItemsList) moItemsList.innerHTML = '<i>(sin items)</i>';
                    var sel = document.getElementById('moClienteSelect');
                    if (sel) {
                        sel.innerHTML = '<option value="">-- seleccionar cliente --</option>';
                        fetch(window.APP_CTX + '/admin/api/clientes', {cache:'no-store'}).then(function(r){ return r.json(); }).then(function(list){ if(Array.isArray(list)){ list.forEach(function(c){ var opt = document.createElement('option'); opt.value = c.id; opt.textContent = (c.nombre||'') + (c.email ? ' <' + c.email + '>' : ''); sel.appendChild(opt); }); } }).catch(function(){});
                    }
                    if (moProductoSelect){
                        moProductoSelect.innerHTML = '<option value="">-- seleccionar producto --</option>';
                        fetch(window.APP_CTX + '/admin/api/productos', {cache:'no-store'}).then(function(r){ return r.json(); }).then(function(list){ moProductosList = Array.isArray(list) ? list : []; if(Array.isArray(list)){ list.forEach(function(p){ try{ var opt = document.createElement('option'); opt.value = p.id || p.ID || p.Id || p.ID_PRODUCTO || ''; var display = p.nombre || p.producto || p.name || p.titulo || Object.keys(p).filter(function(k){ return k.toLowerCase()!=='id' && k.toLowerCase()!=='precio'; }).map(function(k){ return p[k]; }).join(' '); opt.textContent = display || opt.value; moProductoSelect.appendChild(opt); }catch(e){} }); } }).catch(function(){});
                        if (moProductoSelect) {
                            moProductoSelect.onchange = function(){
                                var pid = moProductoSelect.value || '';
                                if(!pid){ if(moProductoInfo) moProductoInfo.innerHTML=''; return; }
                                var prod = moProductosList.find(function(x){ return (x.id||x.ID||x.Id||x.ID_PRODUCTO||'')+'' === pid+''; });
                                if(!prod){ if(moProductoInfo) moProductoInfo.innerHTML=''; return; }
                                var unidad = prod.unidad || prod.unidad_medida || prod.uom || prod.u || prod.medida || prod.medida_unidad || '';
                                var precio = prod.precio || prod.price || prod.PRECIO || '';
                                var stock = prod.stock || prod.cantidad || prod.existencia || prod.stock_actual || '';
                                if(moProductoInfo) moProductoInfo.innerHTML = 'Unidad de medida: '+escapeHtml(unidad||'u') + (precio ? ' | Precio: $'+(Number(precio).toFixed?Number(precio).toFixed(2):precio) : '') + (stock ? ' | Stock: '+escapeHtml(stock) : '');
                                if(moProductoCantidad) moProductoCantidad.placeholder = unidad ? 'cantidad ('+unidad+')' : 'cantidad';
                            };
                        }
                    }
                    if (moAddProducto){
                        moAddProducto.onclick = function(ev){ ev.preventDefault(); var pid = moProductoSelect && moProductoSelect.value || ''; var qty = moProductoCantidad && moProductoCantidad.value || ''; if(!pid || !qty || Number(qty) <= 0){ showToast('Seleccione producto y cantidad válida','error'); return; } var label = (moProductoSelect.options[moProductoSelect.selectedIndex] && moProductoSelect.options[moProductoSelect.selectedIndex].text) || pid; var prod = moProductosList.find(function(x){ return (x.id||x.ID||x.Id||x.ID_PRODUCTO||'')+'' === pid+''; }) || {}; var precio = prod.precio || prod.price || prod.PRECIO || ''; var unidad = prod.unidad || prod.unidad_medida || prod.uom || prod.u || prod.medida || prod.medida_unidad || ''; moItems.push({id: pid, cantidad: qty, label: label, precio: precio, unidad: unidad}); renderMoItems(); moProductoSelect.value=''; moProductoCantidad.value=''; if(moProductoInfo) moProductoInfo.innerHTML=''; };
                    }
                    function renderMoItems(){
                        if(!moItemsList) return;
                        if(!moItems || !moItems.length){ moItemsList.innerHTML = '<i>(sin items)</i>'; return; }
                        var html = '<ul class="mo-items">';
                        moItems.forEach(function(it, idx){
                            var meta = 'qty:'+escapeHtml(it.cantidad)+(it.unidad?(' '+escapeHtml(it.unidad)):('')) + (it.precio?(' | $'+(Number(it.precio).toFixed?Number(it.precio).toFixed(2):it.precio)):(''));
                            html += '<li class="mo-item">'
                                 + '<div><strong>#'+escapeHtml(it.id)+'</strong> - '+escapeHtml(it.label)+' <span class="mo-item-meta">'+meta+'</span></div>'
                                 + '<div><button data-idx="'+idx+'" class="btn-ghost small-btn mo-remove">Quitar</button></div>'
                                 + '</li>';
                        });
                        html += '</ul>';
                        moItemsList.innerHTML = html;
                        Array.from(moItemsList.querySelectorAll('.mo-remove')).forEach(function(b){ b.addEventListener('click', function(ev){ var i = Number(b.getAttribute('data-idx')); if(!isNaN(i)){ moItems.splice(i,1); renderMoItems(); } }); });
                    }
                    renderMoItems();
                    openModal();
                }); }
                if (moCancel){ moCancel.addEventListener('click', function(e){ e.preventDefault(); closeModal(); }); }

                if (moSubmit){ moSubmit.addEventListener('click', function(e){
                    e.preventDefault();
                    var sel = document.getElementById('moClienteSelect');
                    var selectedId = sel ? (sel.value || '') : '';
                    if (!selectedId){ showToast('Seleccione un cliente o cree uno nuevo','error'); if(sel) sel.focus(); return; }
                    var params = new URLSearchParams(); params.append('action','create');
                    params.append('cliente_id', selectedId);

                    moSubmit.disabled = true; moSubmit.textContent = 'Creando...';

                    fetch(window.APP_CTX + '/admin/api/orders', {method:'POST', headers: {'Content-Type':'application/x-www-form-urlencoded; charset=UTF-8'}, body: params.toString()}).then(function(resp){
                        if (!resp.ok){ return resp.text().then(function(t){ throw new Error('HTTP '+resp.status+': '+(t||resp.statusText)); }); }
                        return resp.json().catch(function(){ throw new Error('Respuesta no JSON desde el servidor'); });
                    }).then(function(res){
                        if(res && res.ok){
                            var orderId = res.id;
                            if (moItems && moItems.length){
                                var promises = moItems.map(function(it){ var p2 = new URLSearchParams(); p2.append('action','addItem'); p2.append('id_pedido', orderId); p2.append('id_producto', it.id); p2.append('cantidad', it.cantidad); if(it.precio) p2.append('precio_unitario', it.precio); return fetch(window.APP_CTX + '/admin/api/orders', {method:'POST', headers:{'Content-Type':'application/x-www-form-urlencoded; charset=UTF-8'}, body: p2.toString()}).then(function(r){ return r.json().catch(function(){ return {ok:false}; }); }); });
                                Promise.all(promises).then(function(results){ closeModal(); showToast('Pedido creado ID='+orderId,'success'); load(); }).catch(function(e){ console.error('add items error',e); closeModal(); showToast('Pedido creado pero error al agregar items','warn'); load(); });
                            } else { closeModal(); showToast('Pedido creado ID='+orderId,'success'); load(); }
                        }
                        else {
                            var msg = (res && (res.message || JSON.stringify(res))) || 'Error al crear pedido';
                            throw new Error(msg);
                        }
                    }).catch(function(e){
                        console.error('Crear pedido error:', e);
                        showToast('Error al crear pedido: ' + (e && e.message ? e.message : 'ver consola'),'error');
                    }).finally(function(){ moSubmit.disabled = false; moSubmit.textContent = 'Crear'; });
                }); }

                var moOpenClientsBtn = document.getElementById('moOpenClients');
                if (moOpenClientsBtn) {
                    moOpenClientsBtn.addEventListener('click', function(e){ e.preventDefault(); window.location.href = window.APP_CTX + '/admin/clientes.jsp?new=1'; });
                }

                search.addEventListener('input', function(){ var q = this.value.toLowerCase(); $all('#ordersTable tbody tr').forEach(function(tr){ var id = tr.children[0].textContent.toLowerCase(); var client = tr.children[1].textContent.toLowerCase(); var total = tr.children[3].textContent.toLowerCase(); var visible = id.indexOf(q)!==-1 || client.indexOf(q)!==-1 || total.indexOf(q)!==-1; tr.style.display = visible ? '' : 'none'; }); });

                document.querySelector('#ordersTable tbody').addEventListener('click', function(e){
                    var btn = e.target.closest('button[data-action]'); if(!btn) return;
                    var act = btn.getAttribute('data-action'); var id = btn.getAttribute('data-id');
                    if (act === 'delete'){
                        var deleteModal = document.getElementById('deleteModal');
                        var delText = document.getElementById('delText');
                        var delCancel = document.getElementById('delCancel');
                        var delConfirm = document.getElementById('delConfirm');
                        delText.textContent = 'Eliminar pedido ' + id + ' ?';
                        if (!deleteModal) return;
                        deleteModal.classList.add('modal-show'); deleteModal.setAttribute('aria-hidden','false');
                        function closeDel(){ deleteModal.classList.remove('modal-show'); deleteModal.setAttribute('aria-hidden','true'); delConfirm.disabled=false; delConfirm.textContent='Eliminar'; }
                        delCancel.onclick = function(ev){ ev.preventDefault(); closeDel(); };
                        delConfirm.onclick = function(ev){ ev.preventDefault(); delConfirm.disabled=true; delConfirm.textContent='Eliminando...';
                            var p = new URLSearchParams(); p.append('action','delete'); p.append('id', id);
                            fetch(window.APP_CTX + '/admin/api/orders', {method:'POST', headers:{'Content-Type':'application/x-www-form-urlencoded; charset=UTF-8'}, body: p.toString()}).then(function(r){ return r.json(); }).then(function(res){ if(res && res.ok){ closeDel(); showToast('Eliminado','success'); load(); } else { closeDel(); showToast('Error al eliminar: '+(res && res.error?res.error:JSON.stringify(res)),'error'); } }).catch(function(e){ console.error('delete error',e); showToast('Error al eliminar','error'); closeDel(); });
                        };
                    } else if (act === 'edit'){
                        var editModal = document.getElementById('editOrderModal');
                        var moEditEstado = document.getElementById('moEditEstado');
                        var moEditPrioridad = document.getElementById('moEditPrioridad');
                        var moEditClienteEmail = document.getElementById('moEditClienteEmail');
                        var moEditCancel = document.getElementById('moEditCancel');
                        var moEditSubmit = document.getElementById('moEditSubmit');
                        try { var tr = btn.closest('tr'); var currentEstado = tr.children[2].textContent.trim().toLowerCase(); moEditEstado.value = currentEstado==='en preparación' ? 'preparacion' : currentEstado; } catch(e){}
                        moEditPrioridad.value = '';
                        moEditClienteEmail.value = '';
                        editModal.classList.add('modal-show'); editModal.setAttribute('aria-hidden','false');
                        function closeEdit(){ editModal.classList.remove('modal-show'); editModal.setAttribute('aria-hidden','true'); moEditSubmit.disabled=false; moEditSubmit.textContent='Guardar'; }
                        if (moEditCancel) moEditCancel.onclick = function(ev){ ev.preventDefault(); closeEdit(); };
                        if (moEditSubmit) {
                            moEditSubmit.onclick = function(ev){ ev.preventDefault(); var estado = (moEditEstado && moEditEstado.value||'').trim(); var prioridad = (moEditPrioridad && moEditPrioridad.value||'').trim(); var clienteEmail = (moEditClienteEmail && moEditClienteEmail.value||'').trim(); if (!estado){ alert('Ingrese un estado válido'); return; }
                                var p = new URLSearchParams(); p.append('action','update'); p.append('id', id); p.append('estado', estado); if (prioridad) p.append('prioridad', prioridad); if (clienteEmail) p.append('cliente_email', clienteEmail);
                                moEditSubmit.disabled = true; moEditSubmit.textContent='Guardando...';
                                fetch(window.APP_CTX + '/admin/api/orders', {method:'POST', headers:{'Content-Type':'application/x-www-form-urlencoded; charset=UTF-8'}, body: p.toString()}).then(function(r){ return r.json(); }).then(function(res){ if(res && res.ok){ closeEdit(); alert('Actualizado'); load(); } else { alert('Error al actualizar: '+(res && res.error?res.error:JSON.stringify(res))); } }).catch(function(e){ console.error('update error',e); alert('Error al actualizar'); }).finally(function(){ moEditSubmit.disabled=false; moEditSubmit.textContent='Guardar'; }); };
                        }
                    } else if (act === 'items'){
                        var itemsModal = document.getElementById('itemsModal');
                        var itemsList = document.getElementById('itemsList');
                        var itemsOrderId = document.getElementById('itemsOrderId');
                        var itProducto = document.getElementById('itProducto');
                        var itCantidad = document.getElementById('itCantidad');
                        var itPrecio = document.getElementById('itPrecio');
                        var itAdd = document.getElementById('itAdd');
                        var itClose = document.getElementById('itClose');
                        itemsOrderId.textContent = id;
                        itemsList.innerHTML = 'Cargando...';
                        itemsModal.classList.add('modal-show'); itemsModal.setAttribute('aria-hidden','false');
                        function closeItems(){ itemsModal.classList.remove('modal-show'); itemsModal.setAttribute('aria-hidden','true'); }
                        if (itClose) itClose.onclick = function(ev){ ev.preventDefault(); closeItems(); };

                        function loadItems(){ fetch(window.APP_CTX + '/admin/api/orders?id='+encodeURIComponent(id)).then(function(r){ return r.json(); }).then(function(items){ if(!items || !items.length){ itemsList.innerHTML = '<i>(sin items)</i>'; return; } var html = '<ul style="margin:0;padding-left:16px">'; items.forEach(function(it){ html += '<li>#'+it.id_detalle+' - '+escapeHtml(it.producto)+' qty:'+it.cantidad_solicitada + (it.precio_unitario?(' precio:'+it.precio_unitario):'') + '</li>'; }); html += '</ul>'; itemsList.innerHTML = html; }).catch(function(e){ console.error('items load',e); itemsList.innerHTML = '<span style="color:#a00">Error cargando items</span>'; }); }
                        loadItems();

                        if (itAdd) {
                            itAdd.onclick = function(ev){ ev.preventDefault(); var pid = (itProducto && itProducto.value||'').trim(); var qty = (itCantidad && itCantidad.value||'').trim(); var pr = (itPrecio && itPrecio.value||'').trim(); if(!pid || !qty){ alert('Producto y cantidad son requeridos'); return; }
                                var p = new URLSearchParams(); p.append('action','addItem'); p.append('id_pedido', id); p.append('id_producto', pid); p.append('cantidad', qty); if(pr) p.append('precio_unitario', pr);
                                itAdd.disabled = true; itAdd.textContent = 'Añadiendo...';
                                fetch(window.APP_CTX + '/admin/api/orders', {method:'POST', headers:{'Content-Type':'application/x-www-form-urlencoded; charset=UTF-8'}, body: p.toString()}).then(function(r){ return r.json(); }).then(function(res){ if(res && res.ok){ itProducto.value=''; itCantidad.value=''; itPrecio.value=''; loadItems(); load(); } else { alert('Error al añadir item'); } }).catch(function(e){ console.error('addItem',e); alert('Error al añadir item'); }).finally(function(){ itAdd.disabled=false; itAdd.textContent='Añadir Item'; }); };
                        }
                    }
                });

                load();
            });
        })();
    </script>
</div>
</body>
</html>
