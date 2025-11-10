(function(){
    'use strict';
    function $(sel, ctx){ return (ctx||document).querySelector(sel); }
    function $all(sel, ctx){ return Array.from((ctx||document).querySelectorAll(sel)); }
    function escapeHtml(s){ return (s||'').toString().replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;'); }
    function renderRow(p){
        var tr = document.createElement('tr');
        tr.innerHTML = '<td data-label="ID">'+(p.id||'')+'</td>'+
            '<td data-label="Nombre"><strong>'+escapeHtml(p.nombre||'')+'</strong></td>'+
            '<td data-label="Código">'+escapeHtml(p.codigo_barra||'')+'</td>'+
            '<td data-label="Categoría">'+escapeHtml(p.categoria||'')+'</td>'+
            '<td data-label="Stock">'+escapeHtml(p.stock||'')+'</td>';
        return tr;
    }
    function load(){
        var tbody = $('#productsTable tbody');
        tbody.innerHTML = '';
        var base = window.APP_CTX || '';
        fetch(base + '/admin/api/productos', {cache:'no-store'}).then(function(r){
            if (!r.ok) return r.text().then(function(t){ throw new Error('HTTP '+r.status+': '+(t||r.statusText)); });
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
                json.forEach(function(p){ tbody.appendChild(renderRow(p)); });
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
        if (input) input.addEventListener('input', function(){ var q = this.value.toLowerCase(); $all('#productsTable tbody tr').forEach(function(r){ var name = r.children[1] ? r.children[1].textContent.toLowerCase() : ''; var cat = r.children[3] ? r.children[3].textContent.toLowerCase() : ''; var code = r.children[2] ? r.children[2].textContent.toLowerCase() : ''; r.style.display = (name.includes(q) || cat.includes(q) || code.includes(q)) ? '' : 'none'; }); });
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
        document.body.addEventListener('click', function(e){ var btn = e.target.closest('button[data-action]'); if(!btn) return; var action = btn.getAttribute('data-action'); var id = btn.getAttribute('data-id'); if (action === 'delete'){ openGlobalConfirm('Eliminar producto?', function(){ var p = new URLSearchParams(); p.append('action','delete'); p.append('id', id); fetch((window.APP_CTX||'') + '/admin/api/productos', {method:'POST', headers:{'Content-Type':'application/x-www-form-urlencoded; charset=UTF-8'}, body: p.toString()}).then(function(r){ return r.json(); }).then(function(res){ if (res && res.ok){ globalShowToast('Producto eliminado','success'); load(); } else { globalShowToast('Error al eliminar producto','error'); } }).catch(function(err){ console.error('delete product',err); globalShowToast('Error al eliminar producto','error'); }); }); } else if (action === 'edit'){ var modal = document.getElementById('editProductModal'); var epId = document.getElementById('epId'); var epNombre = document.getElementById('epNombre'); var epCodigo = document.getElementById('epCodigo'); var epCategoria = document.getElementById('epCategoria'); var epStock = document.getElementById('epStock'); var epCancel = document.getElementById('epCancel'); var epSave = document.getElementById('epSave'); var tr = btn.closest('tr'); epId.value = id; epNombre.value = tr.children[1].textContent.trim(); epCodigo.value = tr.children[2].textContent.trim(); epCategoria.value = tr.children[3].textContent.trim(); epStock.value = tr.children[4] ? tr.children[4].textContent.trim() : ''; modal.style.display = 'flex'; epCancel.onclick = function(ev){ ev.preventDefault(); modal.style.display='none'; }; epSave.onclick = function(ev){ ev.preventDefault(); epSave.disabled=true; epSave.textContent='Guardando...'; var p = new URLSearchParams(); p.append('action','update'); p.append('id', epId.value || ''); p.append('nombre', epNombre.value || ''); p.append('codigo_barra', epCodigo.value || ''); p.append('categoria', epCategoria.value || ''); p.append('stock', epStock.value || '0'); fetch((window.APP_CTX||'') + '/admin/api/productos', {method:'POST', headers:{'Content-Type':'application/x-www-form-urlencoded; charset=UTF-8'}, body: p.toString()}).then(function(r){ return r.json(); }).then(function(res){ if (res && res.ok){ modal.style.display='none'; globalShowToast('Producto actualizado','success'); load(); } else { globalShowToast('Error al actualizar producto','error'); } }).catch(function(err){ console.error('update product',err); globalShowToast('Error al actualizar producto','error'); }).finally(function(){ epSave.disabled=false; epSave.textContent='Guardar'; }); }; } else if (action === 'toggle'){ var p = new URLSearchParams(); p.append('action','toggle'); p.append('id', id); fetch((window.APP_CTX||'') + '/admin/api/productos', {method:'POST', headers:{'Content-Type':'application/x-www-form-urlencoded; charset=UTF-8'}, body: p.toString()}).then(function(r){ return r.json(); }).then(function(res){ if (res && res.ok){ globalShowToast('Producto actualizado','success'); load(); } else { globalShowToast('Error al cambiar estado','error'); } }).catch(function(err){ console.error('toggle product',err); globalShowToast('Error al cambiar estado','error'); }); } });
    });
})();