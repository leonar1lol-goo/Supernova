<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="jakarta.servlet.http.*,jakarta.servlet.*" %>
<%
    String username = (String) session.getAttribute("username");
    if (username == null) {
        response.sendRedirect(request.getContextPath() + "/Login.jsp");
        return;
    }
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Mi cuenta - Cambiar contraseña</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/css/style.css">
    <style>
        .account-outer { width:360px; border-radius:12px; padding:18px; background:linear-gradient(180deg,#ffffff,#ffffff); box-shadow:0 18px 40px rgba(2,6,23,0.06); margin:0; position:fixed; left:50%; top:50%; transform:translate(-50%,-50%); z-index:1100; }
        .account-inner { background:#fff; border-radius:10px; padding:20px; box-shadow:0 6px 18px rgba(2,6,23,0.04); }
        .account-inner h2 { text-align:center; font-size:1.15rem; margin-bottom:6px }
        .account-inner p { font-size:0.95rem }
        .account-inner input[type=password] { width:100%; padding:8px 10px; margin:8px 0 6px 0; border:1px solid #eef2f7; border-radius:6px }
        .password-rules { display:flex; flex-direction:column; gap:8px; margin:8px 0 }
        .password-rules label{ display:flex; gap:8px; align-items:center; color:#64748b; font-size:0.92rem }
        .password-rules input[type=checkbox]{ width:16px; height:16px }
        .btn-save{ background:#7c3aed; color:#fff; border:0; padding:8px 12px; border-radius:8px; cursor:pointer }
        .btn-save:hover{ filter:brightness(.95) }
        .btn-ghost{ background:#fff; border:1px solid #e6e9ee; color:#111; padding:8px 10px; border-radius:8px; text-decoration:none }
    </style>
</head>
<body>
<jsp:include page="/header.jsp" />
    <div class="account-page-wrapper" style="min-height:calc(100vh - 60px); display:flex; align-items:center; justify-content:center; padding:24px 12px;">
        <div class="account-outer">
            <div class="account-inner">
                <h2>Mi cuenta</h2>
                <p style="color:#374151;margin:0 0 14px 0;text-align:center">Usuario: <strong><%= username %></strong></p>
                <form id="changePwdForm">
                    <div class="form-row">
                        <label class="label-muted">Contraseña actual</label>
                        <input type="password" id="currentPassword" name="currentPassword" required>
                    </div>
                    <div class="form-row">
                        <label class="label-muted">Nueva contraseña</label>
                        <input type="password" id="newPassword" name="newPassword" required>
                    </div>
                    <div class="form-row">
                        <label class="label-muted">Confirmar nueva contraseña</label>
                        <input type="password" id="confirmPassword" name="confirmPassword" required>
                    </div>

                    <div class="password-rules">
                        <label><input type="checkbox" id="chkLen" disabled> Al menos 8 caracteres</label>
                        <label><input type="checkbox" id="chkUpper" disabled> Al menos 1 mayúscula</label>
                        <label><input type="checkbox" id="chkLower" disabled> Al menos 1 minúscula</label>
                        <label><input type="checkbox" id="chkDigit" disabled> Al menos 1 número</label>
                        <label><input type="checkbox" id="chkSpecial" disabled> Al menos 1 carácter especial</label>
                    </div>

                    <div style="margin-top:12px; display:flex; gap:8px; justify-content:flex-start;">
                        <button type="submit" class="btn-save">Cambiar contraseña</button>
                        <a href="<%= request.getContextPath() %>/admin/dashboard" class="btn-ghost" style="align-self:center; padding:8px 12px;">Cancelar</a>
                    </div>
                </form>
            </div>
        </div>
    </div>
<script>
    const newPwd = document.getElementById('newPassword');
    const confirmPwd = document.getElementById('confirmPassword');
    const reqs = {
        len: document.getElementById('chkLen'),
        upper: document.getElementById('chkUpper'),
        lower: document.getElementById('chkLower'),
        digit: document.getElementById('chkDigit'),
        special: document.getElementById('chkSpecial')
    };
    function testPwd(p) {
        reqs.len.checked = p.length >= 8;
        reqs.upper.checked = /[A-Z]/.test(p);
        reqs.lower.checked = /[a-z]/.test(p);
        reqs.digit.checked = /[0-9]/.test(p);
        reqs.special.checked = /[^A-Za-z0-9]/.test(p);
    }
    newPwd.addEventListener('input', () => testPwd(newPwd.value));

    function showToast(msg, ok) {
        try { globalShowToast(msg, ok ? 'success' : 'error'); } catch(e) { console.log(msg); }
    }

    document.getElementById('changePwdForm').addEventListener('submit', async function(e){
        e.preventDefault();
        const cur = document.getElementById('currentPassword').value;
        const nw = newPwd.value;
        const conf = confirmPwd.value;
        if (nw !== conf) { showToast('La nueva contraseña y su confirmación no coinciden', false); return; }
        const ok = nw.length>=8 && /[A-Z]/.test(nw) && /[a-z]/.test(nw) && /[0-9]/.test(nw) && /[^A-Za-z0-9]/.test(nw);
        if (!ok) { showToast('La contraseña no cumple los requisitos', false); return; }
        try {
            const form = new URLSearchParams();
            form.append('action','changePassword');
            form.append('currentPassword', cur);
            form.append('newPassword', nw);
            const resp = await fetch('<%= request.getContextPath() %>/admin/api/account', {
                method: 'POST', body: form
            });
            const j = await resp.json();
            if (j.ok) {
                showToast('Contraseña cambiada correctamente', true);
                document.getElementById('changePwdForm').reset();
            } else {
                showToast(j.error || j.message || 'Error cambiando contraseña', false);
            }
        } catch (err) {
            showToast('Error de red', false);
        }
    });
</script>
</body>
</html>
