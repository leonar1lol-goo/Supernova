(function(){
    'use strict';

    function $(sel, ctx){ return (ctx||document).querySelector(sel); }
    function $all(sel, ctx){ return Array.from((ctx||document).querySelectorAll(sel)); }

    function renderRow(u){
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td data-label="ID">${u.id}</td>
            <td data-label="Nombre"><strong>${escapeHtml(u.nombre||'')}</strong></td>
            <td data-label="Email">${escapeHtml(u.email||'')}</td>
            <td data-label="Rol">${escapeHtml(u.rol||'')}</td>
            <td data-label="Activo">${u.activo ? 'Sí' : 'No'}</td>
            <td class="text-center" data-label="Acciones">
                <div class="action-forms">
                    <button class="action-btn btn-edit small-btn" data-id="${u.id}" data-action="edit" title="Editar">
                        <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04a1.003 1.003 0 0 0 0-1.42l-2.34-2.34a1.003 1.003 0 0 0-1.42 0l-1.83 1.83 3.75 3.75 1.84-1.82z"/></svg>
                        <span>Editar</span>
                    </button>
                    <button class="action-btn btn-danger small-btn" data-id="${u.id}" data-action="delete" title="Eliminar">
                        <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M6 19a2 2 0 0 0 2 2h8a2 2 0 0 0 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"/></svg>
                        <span>Eliminar</span>
                    </button>
                    <button class="action-btn btn-toggle small-btn" data-id="${u.id}" data-action="toggle" title="Activar/Desactivar usuario">
                        <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 12c2.7 0 5-2.3 5-5s-2.3-5-5-5-5 2.3-5 5 2.3 5 5 5zm0 2c-3.3 0-10 1.7-10 5v3h20v-3c0-3.3-6.7-5-10-5z"/></svg>
                        <span>${u.activo ? 'Desactivar' : 'Activar'}</span>
                    </button>
                </div>
            </td>
        `;
        return tr;
    }

    function escapeHtml(s){ return (s||'').toString().replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;'); }

    function load(){
        const base = window.APP_CTX || '';
        fetch(base + '/admin/api/users', {cache:'no-store'}).then(function(r){
            if (!r.ok) return r.text().then(function(t){ throw new Error('HTTP '+r.status+': '+(t||r.statusText)); });
            return r.json();
        }).then(function(json){
            if (!Array.isArray(json)){
                console.error('users load unexpected response', json);
                try{ globalShowToast('Respuesta inesperada del servidor al cargar usuarios','error'); }catch(e){ console.log('toast',e); }
                return;
            }
            const tbody = $('#usersTable tbody');
            tbody.innerHTML = '';
            if (!json.length){
                var tr = document.createElement('tr');
                tr.innerHTML = '<td colspan="6" style="text-align:center;color:#666;padding:20px">(No hay usuarios)</td>';
                tbody.appendChild(tr);
            } else {
                json.forEach(u=> tbody.appendChild(renderRow(u)) );
            }
        }).catch(err=>{ console.error('users load',err); try{ globalShowToast('Error cargando usuarios: '+(err && err.message?err.message:'ver consola'),'error'); }catch(e){ console.log(err); } });
    }

    function postAction(action, id, callback){
        const base = window.APP_CTX || '';
        const params = new URLSearchParams(); params.append('action', action); params.append('id', id);
        fetch(base + '/admin/api/users', {method:'POST', headers:{'Content-Type':'application/x-www-form-urlencoded; charset=UTF-8'}, body: params.toString()}).then(r=>r.json()).then(callback).catch(err=>{ console.error('action',err); });
    }

    document.addEventListener('DOMContentLoaded', function(){
        load();
        function checkRules(v){ return {
            length: v.length >= 8,
            upper: /[A-Z]/.test(v),
            lower: /[a-z]/.test(v),
            special: /[^A-Za-z0-9]/.test(v),
            number: /[0-9]/.test(v)
        }; }
        function updateRulesUIFor(targets, r){ if(!targets) return; targets.length && targets[0].classList.toggle('valid', !!r.length); if(targets[1]) targets[1].classList.toggle('valid', !!r.upper); if(targets[2]) targets[2].classList.toggle('valid', !!r.lower); if(targets[3]) targets[3].classList.toggle('valid', !!r.special); if(targets[4]) targets[4].classList.toggle('valid', !!r.number); }
    var btnNew = document.getElementById('btnNewUser');
        if (btnNew){
            var cModal = document.getElementById('createUserModal');
            var cuFirst = document.getElementById('cuFirstName');
            var cuLast = document.getElementById('cuLastName');
            var cuEmail = document.getElementById('cuEmail');
            var cuRol = document.getElementById('cuRol');
            var cuPassword = document.getElementById('cuPassword');
            var cuCancel = document.getElementById('cuCancel');
            var cuCreate = document.getElementById('cuCreate');
            var cuToggle = document.getElementById('cuTogglePw');
            var ruleLength = document.getElementById('ruleLength');
            var ruleUpper = document.getElementById('ruleUpper');
            var ruleLower = document.getElementById('ruleLower');
            var ruleSpecial = document.getElementById('ruleSpecial');
            var ruleNumber = document.getElementById('ruleNumber');
            function closeCreate(){ cModal.style.display='none'; cuCreate.disabled=false; cuCreate.textContent='Aceptar y registrarse'; }
            function updateRulesUI(r){ updateRulesUIFor([ruleLength, ruleUpper, ruleLower, ruleSpecial, ruleNumber], r); }
            function isValidEmail(v){ if(!v) return false; v = v.trim(); return v.indexOf('@') !== -1 && v.indexOf('.') !== -1; }
            function canCreate(){ var v = cuPassword.value || ''; var r = checkRules(v); return !!(r.length && r.upper && r.lower && r.number && r.special && isValidEmail(cuEmail.value||'')); }
            btnNew.addEventListener('click', function(e){ e.preventDefault(); cuFirst.value=''; cuLast.value=''; cuEmail.value=''; cuRol.value='operario'; cuPassword.value=''; updateRulesUI(checkRules('')); cuCreate.disabled=true; cModal.style.display='flex'; });
            cuCancel.addEventListener('click', function(e){ e.preventDefault(); closeCreate(); });
            cuToggle.addEventListener('click', function(e){ e.preventDefault(); if (cuPassword.type === 'password'){ cuPassword.type = 'text'; } else { cuPassword.type = 'password'; } });
            cuPassword.addEventListener('input', function(){ var r = checkRules(this.value||''); updateRulesUI(r); cuCreate.disabled = !canCreate(); });
            cuEmail.addEventListener('input', function(){ cuCreate.disabled = !canCreate(); });
            cuCreate.addEventListener('click', function(e){ e.preventDefault(); var emailVal = (cuEmail.value||'').trim(); if (!isValidEmail(emailVal)) { try{ globalShowToast('Ingrese un correo electrónico correcto','error'); }catch(e){}; return; } cuCreate.disabled=true; cuCreate.textContent='Creando...'; var nombre = ((cuFirst.value||'') + ' ' + (cuLast.value||'')).trim(); if (!nombre) nombre = cuFirst.value || cuLast.value || ''; var p = new URLSearchParams(); p.append('action','create'); p.append('nombre', nombre); p.append('email', emailVal || ''); p.append('rol', cuRol.value || 'operario'); p.append('password', cuPassword.value || ''); fetch((window.APP_CTX||'') + '/admin/api/users', {method:'POST', headers:{'Content-Type':'application/x-www-form-urlencoded; charset=UTF-8'}, body: p.toString()})
                .then(function(r){ return r.json(); })
                .then(function(res){ if (res && res.ok){ closeCreate(); globalShowToast('Usuario creado','success'); load(); } else { if (res && res.error === 'invalid_email') { try{ globalShowToast('Ingrese un correo electrónico correcto','error'); }catch(e){} } else if (res && res.error === 'invalid_password_special') { try{ globalShowToast('La contraseña debe incluir al menos un carácter especial','error'); }catch(e){} } else { globalShowToast('Error al crear usuario: '+(res && res.error?res.error:JSON.stringify(res)),'error'); } } })
                .catch(function(err){ try{ globalShowToast('Error al crear usuario','error'); }catch(e){} }).finally(function(){ cuCreate.disabled=false; cuCreate.textContent='Aceptar y registrarse'; });
            });


        }
        document.body.addEventListener('click', function(e){
            const btn = e.target.closest('button[data-action]'); if(!btn) return;
            const action = btn.getAttribute('data-action'); const id = btn.getAttribute('data-id');
            if (action === 'delete'){
                openGlobalConfirm('Eliminar usuario?', function(){
                    postAction('delete', id, function(res){
                        if (res && res.ok) {
                            load();
                        } else {
                            try{ globalShowToast('Error al eliminar','error'); }catch(e){ console.log('Error al eliminar'); }
                        }
                    });
                });
            } else if (action === 'toggle'){
                postAction('toggleActive', id, function(res){ if(res && res.ok){ load(); } else { globalShowToast('Error al cambiar estado','error'); } });
            } else if (action === 'edit'){
                    var modal = document.getElementById('editUserModal');
                    var euEmail = document.getElementById('euEmail');
                    var euRol = document.getElementById('euRol');
                    var euPassword = document.getElementById('euPassword');
                    var euCancel = document.getElementById('euCancel');
                    var euSave = document.getElementById('euSave');
                    var euToggle = document.getElementById('euToggleEditPw');
                    var eruleLength = document.getElementById('eruleLength');
                    var eruleUpper = document.getElementById('eruleUpper');
                    var eruleLower = document.getElementById('eruleLower');
                    var eruleSpecial = document.getElementById('eruleSpecial');
                    var eruleNumber = document.getElementById('eruleNumber');
                var tr = btn.closest('tr');
                var full = tr.children[1].textContent.trim();
                euEmail.value = tr.children[2].textContent.trim();
                euEmail.value = tr.children[2].textContent.trim();
                try{
                    var roleVal = (tr.children[3].textContent||'').trim().toLowerCase();
                    var found = Array.from(euRol.options).some(function(o){ return o.value === roleVal; });
                    euRol.value = found ? roleVal : 'operario';
                }catch(err){ euRol.value = 'operario'; }
                euPassword.value = '';
                updateRulesUIFor([eruleLength, eruleUpper, eruleLower, eruleSpecial, eruleNumber], checkRules(''));
                modal.style.display = 'flex';
                function closeModal(){ modal.style.display='none'; euSave.disabled=false; euSave.textContent='Guardar'; }
                euCancel.onclick = function(e){ e.preventDefault(); closeModal(); };
                euToggle && euToggle.addEventListener('click', function(e){ e.preventDefault(); if (euPassword.type === 'password'){ euPassword.type = 'text'; } else { euPassword.type = 'password'; } });
                euPassword.addEventListener('input', function(){ var r = checkRules(this.value||''); updateRulesUIFor([eruleLength, eruleUpper, eruleLower, eruleSpecial, eruleNumber], r); var hasReq = r.length && r.upper && r.lower && r.number && r.special; euSave.disabled = !( (this.value && hasReq) || (!this.value && (euEmail.value||'').trim()) ); });
                euEmail.addEventListener('input', function(){ euSave.disabled = !( (!euPassword.value && (euEmail.value||'').trim()) || (euPassword.value && (function(){ var rr = checkRules(euPassword.value||''); return rr.length && rr.upper && rr.lower && rr.number && rr.special; })()) ); });
                euSave.onclick = function(e){ e.preventDefault(); euSave.disabled=true; euSave.textContent='Guardando...';
                    var emailToSend = (euEmail.value||'').trim();
                    if (!emailToSend || emailToSend.indexOf('@') === -1 || emailToSend.indexOf('.') === -1) {
                        try{ globalShowToast('Ingrese un correo electrónico correcto','error'); }catch(e){}
                        euSave.disabled=false; euSave.textContent='Guardar';
                        return;
                    }
                    var p = new URLSearchParams(); p.append('action','update'); p.append('id', id); p.append('email', emailToSend||''); p.append('rol', euRol.value||''); if (euPassword.value) p.append('password', euPassword.value);
                    fetch((window.APP_CTX||'') + '/admin/api/users', {method:'POST', headers:{'Content-Type':'application/x-www-form-urlencoded; charset=UTF-8'}, body: p.toString()})
                        .then(function(r){ return r.json(); })
                        .then(function(res){ if (res && res.ok){ closeModal(); globalShowToast('Usuario actualizado','success'); load(); } else { if (res && res.error === 'invalid_email') { try{ globalShowToast('Ingrese un correo electrónico correcto','error'); }catch(e){} } else if (res && res.error === 'invalid_password_special') { try{ globalShowToast('La contraseña debe incluir al menos un carácter especial','error'); }catch(e){} } else { globalShowToast('Error al actualizar: '+(res && res.error?res.error:JSON.stringify(res)),'error'); } } })
                        .catch(function(err){ console.error('update user',err); globalShowToast('Error al actualizar usuario','error'); }).finally(function(){ euSave.disabled=false; euSave.textContent='Guardar'; });
                };
            }
        });

        const input = document.getElementById('userSearch');
        if (input) input.addEventListener('input', function(){ const q = this.value.toLowerCase(); $all('#usersTable tbody tr').forEach(r=>{ const name = r.children[1].textContent.toLowerCase(); const email = r.children[2].textContent.toLowerCase(); r.style.display = (name.includes(q) || email.includes(q)) ? '' : 'none'; }); });
    });

})();
