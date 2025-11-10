(function(){
    'use strict';
    function $(sel, ctx){ return (ctx||document).querySelector(sel); }
    function $all(sel, ctx){ return Array.from((ctx||document).querySelectorAll(sel)); }
    function escapeHtml(s){ return (s||'').toString().replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;'); }
    function renderRow(c){
        var tr = document.createElement('tr');
        tr.innerHTML = '<td data-label="ID">'+(c.id||'')+'</td>'+
            '<td data-label="Nombre"><strong>'+escapeHtml(c.nombre||'')+'</strong></td>'+
            '<td data-label="Dirección">'+escapeHtml(c.direccion||'')+'</td>'+
            '<td data-label="Email">'+escapeHtml(c.email||'')+'</td>'+
            '<td data-label="Teléfono">'+escapeHtml(c.telefono||'')+'</td>'+
            '<td class="text-center" data-label="Acciones">'+
            '<div class="action-forms">'+
            '<button class="action-btn btn-edit small-btn" data-id="'+(c.id||'')+'" data-action="view">Ver</button>'+
            '<button class="action-btn btn-edit small-btn" data-id="'+(c.id||'')+'" data-action="edit">Editar</button>'+
            '<button class="action-btn btn-danger small-btn" data-id="'+(c.id||'')+'" data-action="delete">Eliminar</button>'+
            '</div></td>';
        return tr;
    }
    function load(){
        var tbody = $('#clientsTable tbody');
        tbody.innerHTML = '';
        var base = window.APP_CTX || '';
        fetch(base + '/admin/api/clientes', {cache:'no-store'}).then(function(r){
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
                tr.innerHTML = '<td colspan="5" style="text-align:center;color:#666;padding:20px">(No hay clientes cargados)</td>';
                tbody.appendChild(tr);
            } else {
                json.forEach(function(c){ tbody.appendChild(renderRow(c)); });
            }
        }).catch(function(err){
            console.error('clientes load', err);
            var tr = document.createElement('tr');
            tr.innerHTML = '<td colspan="5" style="text-align:center;color:#c02828;padding:20px">Error cargando clientes</td>';
            tbody.appendChild(tr);
        });
    }
    document.addEventListener('DOMContentLoaded', function(){
        load();
        var input = document.getElementById('clientSearch');
        if (input) input.addEventListener('input', function(){ var q = this.value.toLowerCase(); $all('#clientsTable tbody tr').forEach(function(r){ var name = r.children[1] ? r.children[1].textContent.toLowerCase() : ''; var email = r.children[3] ? r.children[3].textContent.toLowerCase() : ''; r.style.display = (name.includes(q) || email.includes(q)) ? '' : 'none'; }); });
        var btnNew = document.getElementById('btnNewClient');
        var cModal = document.getElementById('createClientModal');
        var ccNombre = document.getElementById('ccNombre');
        var ccDireccion = document.getElementById('ccDireccion');
        var ccTelefono = document.getElementById('ccTelefono');
        var ccEmail = document.getElementById('ccEmail');
        var ccCancel = document.getElementById('ccCancel');
        var ccCreate = document.getElementById('ccCreate');
        if (btnNew) btnNew.addEventListener('click', function(e){ e.preventDefault(); ccNombre.value=''; ccDireccion.value=''; ccTelefono.value=''; ccEmail.value=''; cModal.style.display='flex'; });
        ccCancel && ccCancel.addEventListener('click', function(e){ e.preventDefault(); cModal.style.display='none'; });
        ccCreate && ccCreate.addEventListener('click', function(e){ e.preventDefault(); ccCreate.disabled=true; ccCreate.textContent='Creando...'; var p = new URLSearchParams(); p.append('action','create'); p.append('nombre', ccNombre.value || ''); p.append('direccion', ccDireccion.value || ''); p.append('telefono', ccTelefono.value || ''); p.append('email', ccEmail.value || ''); fetch((window.APP_CTX||'') + '/admin/api/clientes', {method:'POST', headers:{'Content-Type':'application/x-www-form-urlencoded; charset=UTF-8'}, body: p.toString()}).then(function(r){ return r.json(); }).then(function(res){ if (res && res.ok){ cModal.style.display='none'; globalShowToast('Cliente creado','success'); load(); } else { globalShowToast('Error al crear cliente: '+(res && res.error?res.error:JSON.stringify(res)),'error'); } }).catch(function(err){ console.error('create client',err); globalShowToast && globalShowToast('Error al crear cliente','error'); }).finally(function(){ ccCreate.disabled=false; ccCreate.textContent='Crear'; });
        });
        document.body.addEventListener('click', function(e){ var btn = e.target.closest('button[data-action]'); if(!btn) return; var action = btn.getAttribute('data-action'); var id = btn.getAttribute('data-id'); if (action === 'delete'){ openGlobalConfirm('Eliminar cliente?', function(){ var p = new URLSearchParams(); p.append('action','delete'); p.append('id', id); fetch((window.APP_CTX||'') + '/admin/api/clientes', {method:'POST', headers:{'Content-Type':'application/x-www-form-urlencoded; charset=UTF-8'}, body: p.toString()}).then(function(r){ return r.json(); }).then(function(res){ if (res && res.ok){ globalShowToast('Cliente eliminado','success'); load(); } else { globalShowToast('Error al eliminar cliente','error'); } }).catch(function(err){ console.error('delete client',err); globalShowToast('Error al eliminar cliente','error'); }); }); } else if (action === 'edit'){ var modal = document.getElementById('editClientModal'); var ecId = document.getElementById('ecId'); var ecNombre = document.getElementById('ecNombre'); var ecDireccion = document.getElementById('ecDireccion'); var ecTelefono = document.getElementById('ecTelefono'); var ecEmail = document.getElementById('ecEmail'); var ecCancel = document.getElementById('ecCancel'); var ecSave = document.getElementById('ecSave'); var tr = btn.closest('tr'); ecId.value = id; ecNombre.value = tr.children[1].textContent.trim(); ecDireccion.value = tr.children[2].textContent.trim(); ecEmail.value = tr.children[3].textContent.trim(); ecTelefone = tr.children[4] ? tr.children[4].textContent.trim() : ''; document.getElementById('ecTelefono').value = ecTelefone; modal.style.display = 'flex'; ecCancel.onclick = function(ev){ ev.preventDefault(); modal.style.display='none'; }; ecSave.onclick = function(ev){ ev.preventDefault(); ecSave.disabled=true; ecSave.textContent='Guardando...'; var p = new URLSearchParams(); p.append('action','update'); p.append('id', ecId.value || ''); p.append('nombre', ecNombre.value || ''); p.append('direccion', ecDireccion.value || ''); p.append('telefono', ecTelefono || ''); p.append('email', ecEmail.value || ''); fetch((window.APP_CTX||'') + '/admin/api/clientes', {method:'POST', headers:{'Content-Type':'application/x-www-form-urlencoded; charset=UTF-8'}, body: p.toString()}).then(function(r){ return r.json(); }).then(function(res){ if (res && res.ok){ modal.style.display='none'; globalShowToast('Cliente actualizado','success'); load(); } else { globalShowToast('Error al actualizar cliente','error'); } }).catch(function(err){ console.error('update client',err); globalShowToast('Error al actualizar cliente','error'); }).finally(function(){ ecSave.disabled=false; ecSave.textContent='Guardar'; }); }; } });
    });
})();
