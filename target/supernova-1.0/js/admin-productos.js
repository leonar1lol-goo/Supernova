(function(){
    'use strict';
    function $(sel, ctx){ return (ctx||document).querySelector(sel); }
    function $all(sel, ctx){ return Array.from((ctx||document).querySelectorAll(sel)); }
    function escapeHtml(s){ return (s||'').toString().replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;'); }
    function buildTableHeader(keys){
        var thead = document.querySelector('#productsTable thead');
        if (!thead) return;
        thead.innerHTML = '';
        var tr = document.createElement('tr');
        var labels = {
            id: 'ID',
            nombre: 'Nombre',
            codigo_barra: 'Código',
            categoria: 'Categoría',
            stock: 'Stock',
            sku: 'SKU',
            descripcion: 'Descripción',
            marca: 'Marca',
            unidad_medida: 'Unidad de medida',
            precio: 'Precio (s./)',
            categoria_id: 'Categoria ID',
            activo: 'Acciones'
        };
        keys.forEach(function(k){
            var th = document.createElement('th');
            th.setAttribute('data-key', k);
            var label = labels[k] || k;
            th.textContent = label;
            tr.appendChild(th);
        });
        thead.appendChild(tr);
        window.PRODUCTS_COL_INDEX = {};
        window.PRODUCTS_KEYS = keys.slice();
        Array.from(tr.children).forEach(function(th,i){ window.PRODUCTS_COL_INDEX[th.getAttribute('data-key')] = i; });
    }

    function renderRow(p, keys){
        var tr = document.createElement('tr');
        keys.forEach(function(k){
            var td = document.createElement('td');
            td.setAttribute('data-label', k);
            var val = p[k] === undefined || p[k] === null ? '' : p[k];
            if (k === 'nombre') {
                var strong = document.createElement('strong');
                strong.textContent = String(val);
                td.appendChild(strong);
            } else if (k === 'activo') {
                var wrap = document.createElement('div');
                wrap.style.display = 'flex';
                wrap.style.gap = '6px';
                var btnEdit = document.createElement('button');
                btnEdit.textContent = 'Editar';
                btnEdit.setAttribute('data-action','edit');
                btnEdit.setAttribute('data-id', String(p.id || ''));
                btnEdit.style.cursor = 'pointer';
                btnEdit.style.padding = '6px 10px';
                btnEdit.style.borderRadius = '4px';
                btnEdit.style.border = '1px solid #1976d2';
                btnEdit.style.background = '#1976d2';
                btnEdit.style.color = '#fff';
                var btnDelete = document.createElement('button');
                btnDelete.textContent = 'Eliminar';
                btnDelete.setAttribute('data-action','delete');
                btnDelete.setAttribute('data-id', String(p.id || ''));
                btnDelete.style.cursor = 'pointer';
                btnDelete.style.padding = '6px 10px';
                btnDelete.style.borderRadius = '4px';
                btnDelete.style.border = '1px solid #c02828';
                btnDelete.style.background = '#c02828';
                btnDelete.style.color = '#fff';
                var btnToggle = document.createElement('button');
                var active = String(val).toLowerCase();
                var isOn = (active === '1' || active === 'true' || active === 't');
                btnToggle.textContent = isOn ? 'Activo' : 'Inactivo';
                btnToggle.setAttribute('data-action','toggle');
                btnToggle.setAttribute('data-id', String(p.id || ''));
                btnToggle.style.cursor = 'pointer';
                btnToggle.style.padding = '6px 10px';
                btnToggle.style.borderRadius = '4px';
                btnToggle.style.border = '1px solid #ccc';
                btnToggle.style.background = isOn ? '#2ecc71' : '#e0e0e0';
                btnToggle.style.color = isOn ? '#fff' : '#333';
                wrap.appendChild(btnEdit);
                wrap.appendChild(btnDelete);
                wrap.appendChild(btnToggle);
                td.appendChild(wrap);
            } else {
                td.textContent = String(val);
            }
            tr.appendChild(td);
        });
        return tr;
    }
    function load(){
        var tbody = $('#productsTable tbody');
        tbody.innerHTML = '';
        var base = window.APP_CTX || '';
        fetch(base + '/admin/api/productos', {cache:'no-store'}).then(function(r){
            if (!r.ok) return r.text().then(function(t){
                try { console.error('productos load HTTP', r.status, t); } catch(e){}
                var tr = document.createElement('tr');
                tr.innerHTML = '<td colspan="5" style="text-align:center;color:#c02828;padding:20px">Error cargando productos: '+escapeHtml(t||r.statusText)+'</td>';
                tbody.appendChild(tr);
                throw new Error('HTTP '+r.status+': '+(t||r.statusText));
            });
            return r.json();
        }).then(function(json){
            if (!Array.isArray(json)){
                var tr = document.createElement('tr');
                tr.innerHTML = '<td colspan="5" style="text-align:center;color:#666;padding:20px">(Respuesta inesperada del servidor)</td>';
                tbody.appendChild(tr);
                return;
            }
            if (!json.length){
                var tr = document.createElement('tr');
                tr.innerHTML = '<td colspan="5" style="text-align:center;color:#666;padding:20px">(No hay productos cargados)</td>';
                tbody.appendChild(tr);
            } else {
                var keys = Object.keys(json[0] || {});
                if (keys.length === 0) keys = ['id','nombre','codigo_barra','categoria','stock'];
                var baseKeys = ['id','nombre','codigo_barra','categoria','stock'];
                var ordered = [];
                baseKeys.forEach(function(k){ if (keys.indexOf(k) !== -1) ordered.push(k); });
                keys.forEach(function(k){ if (ordered.indexOf(k) === -1) ordered.push(k); });
                buildTableHeader(ordered);
                json.forEach(function(p){ tbody.appendChild(renderRow(p, ordered)); });
            }
        }).catch(function(err){
            console.error('productos load', err);
            var tr = document.createElement('tr');
            tr.innerHTML = '<td colspan="5" style="text-align:center;color:#c02828;padding:20px">Error cargando productos</td>';
            tbody.appendChild(tr);
        });
    }
    document.addEventListener('DOMContentLoaded', function(){
        load();
    var input = document.getElementById('productSearch');
    if (input) input.addEventListener('input', function(){ var q = this.value.toLowerCase(); $all('#productsTable tbody tr').forEach(function(r){ var text = r.textContent.toLowerCase(); r.style.display = text.indexOf(q) !== -1 ? '' : 'none'; }); });
        var btnNew = document.getElementById('btnNewProduct');
        var pModal = document.getElementById('createProductModal');
        var pcNombre = document.getElementById('pcNombre');
        var pcCodigo = document.getElementById('pcCodigo');
        var pcCategoria = document.getElementById('pcCategoria');
        var pcStock = document.getElementById('pcStock');
        var pcCancel = document.getElementById('pcCancel');
        var pcCreate = document.getElementById('pcCreate');
        if (btnNew) btnNew.addEventListener('click', function(e){ e.preventDefault(); pcNombre.value=''; pcCodigo.value=''; pcCategoria.value=''; pcStock.value='0'; pModal.style.display='flex'; });
        pcCancel && pcCancel.addEventListener('click', function(e){ e.preventDefault(); pModal.style.display='none'; });
        pcCreate && pcCreate.addEventListener('click', function(e){ e.preventDefault(); pcCreate.disabled=true; pcCreate.textContent='Creando...'; var p = new URLSearchParams(); p.append('action','create'); p.append('nombre', pcNombre.value || ''); p.append('codigo_barra', pcCodigo.value || ''); p.append('categoria', pcCategoria.value || ''); p.append('stock', pcStock.value || '0'); fetch((window.APP_CTX||'') + '/admin/api/productos', {method:'POST', headers:{'Content-Type':'application/x-www-form-urlencoded; charset=UTF-8'}, body: p.toString()}).then(function(r){ return r.json(); }).then(function(res){ if (res && res.ok){ pModal.style.display='none'; globalShowToast('Producto creado','success'); load(); } else { globalShowToast('Error al crear producto: '+(res && res.error?res.error:JSON.stringify(res)),'error'); } }).catch(function(err){ console.error('create product',err); globalShowToast && globalShowToast('Error al crear producto','error'); }).finally(function(){ pcCreate.disabled=false; pcCreate.textContent='Crear'; });
        });
    document.body.addEventListener('click', function(e){
        var btn = e.target.closest('button[data-action]');
        if(!btn) return;
        var action = btn.getAttribute('data-action');
        var id = btn.getAttribute('data-id');
        if (action === 'delete'){
            openGlobalConfirm('Eliminar producto?', function(){
                var p = new URLSearchParams(); p.append('action','delete'); p.append('id', id);
                fetch((window.APP_CTX||'') + '/admin/api/productos', {method:'POST', headers:{'Content-Type':'application/x-www-form-urlencoded; charset=UTF-8'}, body: p.toString()}).then(function(r){ return r.json(); }).then(function(res){ if (res && res.ok){ globalShowToast('Producto eliminado','success'); load(); } else { globalShowToast('Error al eliminar producto','error'); } }).catch(function(err){ console.error('delete product',err); globalShowToast('Error al eliminar producto','error'); });
            });
        } else if (action === 'edit'){
            var modal = document.getElementById('editProductModal');
            var container = modal.querySelector('.register-container');
            var tr = btn.closest('tr');
            var keys = window.PRODUCTS_KEYS || Object.keys(window.PRODUCTS_COL_INDEX || {});
            container.innerHTML = '';
            var hidden = document.createElement('input'); hidden.type = 'hidden'; hidden.id = 'epId'; hidden.name = 'id'; hidden.value = id; container.appendChild(hidden);
            var idx = window.PRODUCTS_COL_INDEX || {};
            function cellValue(key){ var i = idx[key]; if (i === undefined) return ''; var c = tr.children[i]; return c ? c.textContent.trim() : ''; }
            var labels = { nombre: 'Nombre', codigo_barra: 'Código', categoria: 'Categoría', stock: 'Stock', sku: 'SKU', descripcion: 'Descripción', marca: 'Marca', unidad_medida: 'Unidad Medida', precio: 'Precio (s./)'};
            keys.forEach(function(k){
                if (k === 'id' || k === 'activo') return;
                var row = document.createElement('div'); row.className = 'form-row';
                var label = document.createElement('label'); label.setAttribute('for','ep_'+k); label.textContent = labels[k] || (k.replace(/_/g,' ').replace(/\b\w/g,function(m){return m.toUpperCase();}));
                var input = document.createElement('input'); input.id = 'ep_'+k; input.name = k; if (k.toLowerCase().indexOf('stock') !== -1 || k.toLowerCase().indexOf('precio') !== -1) input.type = 'number'; else input.type = 'text'; input.value = cellValue(k);
                row.appendChild(label); row.appendChild(input); container.appendChild(row);
            });
            var actions = document.createElement('div'); actions.className = 'actions'; actions.style.marginTop = '14px';
            var btnCancel = document.createElement('button'); btnCancel.id = 'epCancel'; btnCancel.className = 'btn-ghost'; btnCancel.textContent = 'Cancelar';
            var btnSave = document.createElement('button'); btnSave.id = 'epSave'; btnSave.className = 'btn-save'; btnSave.textContent = 'Guardar';
            actions.appendChild(btnCancel); actions.appendChild(btnSave); container.appendChild(actions);
            modal.style.display = 'flex';
            btnCancel.onclick = function(ev){ ev.preventDefault(); modal.style.display='none'; };
            btnSave.onclick = function(ev){ ev.preventDefault(); btnSave.disabled=true; var old = btnSave.textContent; btnSave.textContent='Guardando...'; var p = new URLSearchParams(); p.append('action','update'); p.append('id', id); keys.forEach(function(k){ if (k === 'id') return; var el = document.getElementById('ep_'+k); if (!el) return; p.append(k, el.value || ''); }); fetch((window.APP_CTX||'') + '/admin/api/productos', {method:'POST', headers:{'Content-Type':'application/x-www-form-urlencoded; charset=UTF-8'}, body: p.toString()}).then(function(r){ return r.json(); }).then(function(res){ if (res && res.ok){ modal.style.display='none'; globalShowToast('Producto actualizado','success'); load(); } else { globalShowToast('Error al actualizar producto','error'); } }).catch(function(err){ console.error('update product',err); globalShowToast('Error al actualizar producto','error'); }).finally(function(){ btnSave.disabled=false; btnSave.textContent=old; });
            };
        } else if (action === 'toggle'){
            var p = new URLSearchParams(); p.append('action','toggle'); p.append('id', id);
            fetch((window.APP_CTX||'') + '/admin/api/productos', {method:'POST', headers:{'Content-Type':'application/x-www-form-urlencoded; charset=UTF-8'}, body: p.toString()}).then(function(r){ return r.json(); }).then(function(res){ if (res && res.ok){ globalShowToast('Producto actualizado','success'); load(); } else { globalShowToast('Error al cambiar estado','error'); } }).catch(function(err){ console.error('toggle product',err); globalShowToast('Error al cambiar estado','error'); });
        }
    });
    });
})();