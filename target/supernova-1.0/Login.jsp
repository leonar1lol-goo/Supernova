<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <title>Login - Supernova</title>
    <link rel="stylesheet" href="css/style.css">
    
</head>
<body class="login-body">
    <div class="login-container">
        <h2>Login</h2>
        <%
            // Prefer session-based error flag set by LoginServlet to avoid showing stale ?error params
            String sessionErr = null;
            try {
                jakarta.servlet.http.HttpSession _s = request.getSession(false);
                if (_s != null) {
                    Object v = _s.getAttribute("loginError");
                    if (v != null) {
                        sessionErr = v.toString();
                        _s.removeAttribute("loginError");
                    }
                }
            } catch (Exception ignore) { }
            if ("inactive".equals(sessionErr)) {
        %>
            <div class="login-error">Cuenta desactivada. Contacta al Administrador.</div>
        <%
            }
        %>
        <form action="login" method="post">
            <input type="text" name="email" placeholder="Correo Electrónico" required>
            <input type="password" name="password" placeholder="Contraseña" required>
            <input type="submit" value="Ingresar" style="display:block;margin:18px auto 0;padding:10px 18px;border-radius:6px;cursor:pointer;">
        </form>
    </div>
</body>
</html>