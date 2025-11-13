(function(){
    'use strict';
    function $(sel, ctx){ return (ctx||document).querySelector(sel); }
    function $all(sel, ctx){ return Array.from((ctx||document).querySelectorAll(sel)); }
    function escapeHtml(s){ return (s||'').toString().replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;'); }
    function renderRow(c){
        var tr = document.createElement('tr');
        tr.innerHTML = '<td data-label="ID">'+(c.id||'')+'</td>'+
            '<td data-label="Nombre"><strong>'+escapeHtml(c.nombre||'')+'</strong></td>'+
            '<td data-label="DNI">'+escapeHtml(c.dni||'')+'</td>'+
            '<td data-label="Dirección">'+escapeHtml(c.direccion||'')+'</td>'+
            '<td data-label="Email">'+escapeHtml(c.email||'')+'</td>'+
            '<td data-label="Teléfono">'+escapeHtml(c.telefono||'')+'</td>'+
            '<td class="text-center" data-label="Acciones">'+
            '<div class="action-forms">'+
                '<button type="button" class="action-btn btn-edit small-btn" data-id="'+(c.id||'')+'" data-action="edit">Editar</button>'+
                '<button type="button" class="action-btn btn-danger small-btn" data-id="'+(c.id||'')+'" data-action="delete">Eliminar</button>'+
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
                tr.innerHTML = '<td colspan="6" style="text-align:center;color:#666;padding:20px">(Respuesta inesperada del servidor)</td>';
                tbody.appendChild(tr);
                return;
            }
            if (!json.length){
                var tr = document.createElement('tr');
                tr.innerHTML = '<td colspan="6" style="text-align:center;color:#666;padding:20px">(No hay clientes cargados)</td>';
                tbody.appendChild(tr);
            } else {
                json.forEach(function(c){ tbody.appendChild(renderRow(c)); });
            }
        }).catch(function(err){
            console.error('clientes load', err);
            var tr = document.createElement('tr');
            tr.innerHTML = '<td colspan="6" style="text-align:center;color:#c02828;padding:20px">Error cargando clientes</td>';
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
    var ccDni = document.getElementById('ccDni');
    var ccEmail = document.getElementById('ccEmail');
    var ecTelefonoEl = document.getElementById('ecTelefono');
    var ecDniEl = document.getElementById('ecDni');
        var ccCancel = document.getElementById('ccCancel');
        var ccCreate = document.getElementById('ccCreate');

    function isValidEmail(v){ if (!v) return false; v = v.trim(); return v.indexOf('@') !== -1 && v.indexOf('.') !== -1; }
    function sanitizePhone(s){ return (s||'').toString().replace(/\D/g,'').slice(0,9); }
    function sanitizeDni(s){ return (s||'').toString().replace(/\D/g,'').slice(0,11); }
    function isValidDni(s){ var d = sanitizeDni(s); return d.length > 0 && d.length <= 11; }
    function isValidPhone(s){ var d = sanitizePhone(s); return d.length > 0 && d.length <= 9; }

        function updateCreateState(){
            if (!ccCreate) return;
            var n = (ccNombre.value||'').trim();
            var dir = (ccDireccion.value||'').trim();
            var ph = sanitizePhone(ccTelefono.value||'');
            var em = (ccEmail.value||'').trim();
            var dniVal = (ccDni && sanitizeDni(ccDni.value||'')) || '';
            var ok = n.length > 0 && dir.length > 0 && ph.length > 0 && ph.length <= 9 && isValidEmail(em) && dniVal.length > 0 && dniVal.length <= 11;
            ccCreate.disabled = !ok;
        }

        if (ccTelefono) {
            ccTelefono.addEventListener('input', function(){ var v = sanitizePhone(this.value); if (this.value !== v) this.value = v; updateCreateState(); });
            ccTelefono.addEventListener('paste', function(e){ setTimeout(updateCreateState,50); });
        }
        if (ccDni) {
            ccDni.addEventListener('input', function(){ var v = sanitizeDni(this.value); if (this.value !== v) this.value = v; updateCreateState(); });
            ccDni.addEventListener('paste', function(e){ setTimeout(updateCreateState,50); });
        }
        if (ecTelefonoEl) {
            ecTelefonoEl.addEventListener('input', function(){ var v = sanitizePhone(this.value); if (this.value !== v) this.value = v; });
            ecTelefonoEl.addEventListener('paste', function(e){ setTimeout(function(){ if (ecTelefonoEl) ecTelefonoEl.value = sanitizePhone(ecTelefonoEl.value); },50); });
        }
        if (ecDniEl) {
            ecDniEl.addEventListener('input', function(){ var v = sanitizeDni(this.value); if (this.value !== v) this.value = v; });
            ecDniEl.addEventListener('paste', function(e){ setTimeout(function(){ if (ecDniEl) ecDniEl.value = sanitizeDni(ecDniEl.value); },50); });
        }
        [ccNombre, ccDireccion, ccEmail].forEach(function(el){ if (!el) return; el.addEventListener('input', updateCreateState); });

    if (btnNew) btnNew.addEventListener('click', function(e){ e.preventDefault(); ccNombre.value=''; ccDireccion.value=''; ccTelefono.value=''; ccDni && (ccDni.value=''); ccEmail.value=''; if (ccCreate) ccCreate.disabled = true; updateCreateState(); cModal.style.display='flex'; });
        try {
            var sp = new URLSearchParams(window.location.search || '');
            if (sp.get('new') === '1') { if (btnNew) { setTimeout(function(){ try{ btnNew.click(); }catch(e){} }, 50); } }
        } catch(e) {}
        ccCancel && ccCancel.addEventListener('click', function(e){ e.preventDefault(); cModal.style.display='none'; });
        ccCreate && ccCreate.addEventListener('click', function(e){ e.preventDefault();
            var nombreVal = (ccNombre.value||'').trim();
            var direccionVal = (ccDireccion.value||'').trim();
            var telefonoVal = sanitizePhone(ccTelefono.value||'');
            var dniVal = (ccDni && sanitizeDni(ccDni.value||'')) || '';
            var emailVal = (ccEmail.value||'').trim();
            if (!nombreVal || !direccionVal || !telefonoVal || !emailVal || !dniVal) {
                try{ globalShowToast('Complete todos los campos','error'); }catch(e){}
                return;
            }
            if (!/^[0-9]{1,9}$/.test(telefonoVal)) {
                try{ globalShowToast('El teléfono debe contener hasta 9 dígitos numéricos','error'); }catch(e){}
                return;
            }
            if (!/^[0-9]{1,11}$/.test(dniVal)) {
                try{ globalShowToast('DNI/RUC debe contener hasta 11 dígitos numéricos','error'); }catch(e){}
                return;
            }
            if (!isValidEmail(emailVal)) {
                try{ globalShowToast('Ingrese un correo electrónico correcto','error'); }catch(e){}
                return;
            }
            ccCreate.disabled=true; ccCreate.textContent='Creando...'; var p = new URLSearchParams(); p.append('action','create'); p.append('nombre', nombreVal || ''); p.append('dni', dniVal || ''); p.append('direccion', direccionVal || ''); p.append('telefono', telefonoVal || ''); p.append('email', emailVal || ''); fetch((window.APP_CTX||'') + '/admin/api/clientes', {method:'POST', headers:{'Content-Type':'application/x-www-form-urlencoded; charset=UTF-8'}, body: p.toString()})
                .then(function(r){ return r.json(); })
                .then(function(res){
                    if (res && res.ok){
                        cModal.style.display='none';
                        globalShowToast('Cliente creado','success');
                        load();
                    } else {
                        if (res && res.error === 'invalid_email') {
                            try{ globalShowToast('Ingrese un correo electrónico correcto','error'); }catch(e){}
                        } else if (res && res.error === 'invalid_dni') {
                            try{ globalShowToast('DNI/RUC inválido: debe contener sólo números y hasta 11 dígitos','error'); }catch(e){}
                        } else {
                            globalShowToast('Error al crear cliente: '+(res && res.error?res.error:JSON.stringify(res)),'error');
                        }
                    }
                }).catch(function(err){ console.error('create client',err); globalShowToast && globalShowToast('Error al crear cliente','error'); }).finally(function(){ ccCreate.disabled=false; ccCreate.textContent='Crear'; });
        });
        document.body.addEventListener('click', function(e){
            var btn = e.target.closest('button[data-action]');
            if(!btn) return;
            var action = btn.getAttribute('data-action');
            var id = btn.getAttribute('data-id');
            if (action === 'delete'){
                openGlobalConfirm('Eliminar cliente?', function(){
                    var p = new URLSearchParams();
                    p.append('action','delete');
                    p.append('id', id);
                    fetch((window.APP_CTX||'') + '/admin/api/clientes', {method:'POST', headers:{'Content-Type':'application/x-www-form-urlencoded; charset=UTF-8'}, body: p.toString()}).then(function(r){ return r.json(); }).then(function(res){ if (res && res.ok){ globalShowToast('Cliente eliminado','success'); load(); } else { globalShowToast('Error al eliminar cliente','error'); } }).catch(function(err){ console.error('delete client',err); globalShowToast('Error al eliminar cliente','error'); });
                });
            } else if (action === 'edit'){
                var modal = document.getElementById('editClientModal');
                var ecId = document.getElementById('ecId');
                var ecTelefono = document.getElementById('ecTelefono');
                var ecDni = document.getElementById('ecDni');
                var ecDireccion = document.getElementById('ecDireccion');
                var ecEmail = document.getElementById('ecEmail');
                var ecCancel = document.getElementById('ecCancel');
                var ecSave = document.getElementById('ecSave');
                var tr = btn.closest('tr');
                ecId.value = id;
                ecEmail.value = tr.children[4].textContent.trim();
                var telefonoText = tr.children[5] ? tr.children[5].textContent.trim() : '';
                ecTelefono.value = telefonoText;
                var dniText = tr.children[2] ? tr.children[2].textContent.trim() : '';
                if (ecDni) ecDni.value = sanitizeDni(dniText);
                var direccionText = tr.children[3] ? tr.children[3].textContent.trim() : '';
                if (ecDireccion) ecDireccion.value = direccionText;
                modal.style.display = 'flex';
                ecCancel.onclick = function(ev){ ev.preventDefault(); modal.style.display='none'; };
                ecSave.onclick = function(ev){
                    ev.preventDefault();
                    ecSave.disabled=true;
                    ecSave.textContent='Guardando...';
                    var emailVal = (ecEmail.value||'').trim();
                    if (!emailVal || emailVal.indexOf('@') === -1 || emailVal.indexOf('.') === -1) {
                        try{ globalShowToast('Ingrese un correo electrónico correcto','error'); }catch(e){}
                        ecSave.disabled=false;
                        ecSave.textContent='Guardar';
                        return;
                    }
                    var telefonoClean = sanitizePhone(ecTelefono.value||'');
                    var dniValEdit = ecDni ? (ecDni.value||'').trim() : '';
                    var direccionVal = ecDireccion ? (ecDireccion.value||'').trim() : '';
                    if (!/^[0-9]{1,9}$/.test(telefonoClean)) {
                        try{ globalShowToast('El teléfono debe contener hasta 9 dígitos numéricos','error'); }catch(e){}
                        ecSave.disabled=false;
                        ecSave.textContent='Guardar';
                        return;
                    }
                    var p = new URLSearchParams();
                    p.append('action','update');
                    p.append('id', ecId.value || '');
                    p.append('telefono', telefonoClean || '');
                    p.append('dni', dniValEdit || '');
                    p.append('direccion', direccionVal || '');
                    p.append('email', emailVal || '');
                    fetch((window.APP_CTX||'') + '/admin/api/clientes', {method:'POST', headers:{'Content-Type':'application/x-www-form-urlencoded; charset=UTF-8'}, body: p.toString()})
                        .then(function(r){ return r.json(); })
                        .then(function(res){
                            if (res && res.ok){
                                modal.style.display='none';
                                globalShowToast('Cliente actualizado','success');
                                load();
                            } else {
                                    if (res && res.error === 'invalid_email') {
                                        try{ globalShowToast('Ingrese un correo electrónico correcto','error'); }catch(e){}
                                    } else if (res && res.error === 'invalid_dni') {
                                        try{ globalShowToast('DNI/RUC inválido: debe contener sólo números y hasta 11 dígitos','error'); }catch(e){}
                                    } else {
                                        var errMsg = (res && res.error) ? res.error : 'Error al actualizar cliente';
                                        globalShowToast(errMsg,'error');
                                    }
                            }
                        }).catch(function(err){ console.error('update client',err); globalShowToast('Error al actualizar cliente','error'); }).finally(function(){ ecSave.disabled=false; ecSave.textContent='Guardar'; });
                };
            }
        });
    });
})();
