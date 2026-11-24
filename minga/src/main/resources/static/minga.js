// ============================================================
// Minga - utilidades compartidas (JavaScript puro)
// ============================================================
const $ = (selector, raiz = document) => raiz.querySelector(selector);

// Spring guarda el token CSRF en la cookie XSRF-TOKEN; hay que reenviarlo en POST/PUT
function tokenCsrf() {
    const m = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]+)/);
    return m ? decodeURIComponent(m[1]) : "";
}

// Llama a la API y devuelve { ok, status, data }. Si la sesión venció, vuelve al login.
async function api(url, opciones = {}) {
    const metodo = (opciones.method || "GET").toUpperCase();
    const headers = {};
    if (opciones.body !== undefined) headers["Content-Type"] = "application/json";
    if (metodo !== "GET") headers["X-XSRF-TOKEN"] = tokenCsrf();

    const respuesta = await fetch(url, {
        method: metodo,
        headers,
        body: opciones.body !== undefined ? JSON.stringify(opciones.body) : undefined,
    });
    if (respuesta.status === 401) {
        location.replace("/login.html");
        return new Promise(() => {});
    }
    let data = null;
    const texto = await respuesta.text();
    try { data = texto ? JSON.parse(texto) : null; } catch (e) { data = null; }
    return { ok: respuesta.ok, status: respuesta.status, data };
}

// Saca el mensaje de error de una respuesta de la API
function mensajeDe(res, porDefecto) {
    if (res && res.data) {
        if (res.data.mensaje) return res.data.mensaje;
        if (res.data.errores) {
            const primero = Object.values(res.data.errores).flat()[0];
            if (primero) return primero;
        }
    }
    if (res && res.status === 403) return "No tienes permiso para hacer esto.";
    return porDefecto || "Algo salió mal. Inténtalo de nuevo.";
}

// Constructor de elementos: usa textContent, así nunca se inyecta HTML
function el(etiqueta, atributos, ...hijos) {
    const e = document.createElement(etiqueta);
    for (const [clave, valor] of Object.entries(atributos || {})) {
        if (valor === null || valor === undefined || valor === false) continue;
        if (clave === "class") e.className = valor;
        else if (clave.startsWith("on") && typeof valor === "function") e.addEventListener(clave.slice(2), valor);
        else if (valor === true) e.setAttribute(clave, "");
        else e.setAttribute(clave, valor);
    }
    for (const hijo of hijos.flat()) {
        if (hijo === null || hijo === undefined || hijo === false) continue;
        e.append(typeof hijo === "number" ? String(hijo) : hijo);
    }
    return e;
}

// ---------- Avisos y confirmaciones ----------
function aviso(texto, tipo = "info") {
    let caja = document.getElementById("avisos");
    if (!caja) {
        caja = el("div", { id: "avisos", "aria-live": "polite" });
        document.body.append(caja);
    }
    const nodo = el("div", { class: "mensaje " + tipo, role: "status" }, texto);
    caja.append(nodo);
    setTimeout(() => nodo.remove(), tipo === "error" ? 7000 : 4500);
}

function confirmar(titulo, texto, boton = "Confirmar") {
    return new Promise(resolver => {
        const dlg = el("dialog", {},
            el("form", { method: "dialog" },
                el("div", {}, el("h2", {}, titulo), el("p", { class: "bajada" }, texto)),
                el("div", { class: "dlg-acciones" },
                    el("button", { type: "submit", value: "no" }, "Cancelar"),
                    el("button", { type: "submit", value: "si", class: "indigo" }, boton))));
        dlg.addEventListener("close", () => {
            const acepto = dlg.returnValue === "si";
            dlg.remove();
            resolver(acepto);
        });
        document.body.append(dlg);
        dlg.showModal();
    });
}

// ---------- Formato ----------
function fechaCorta(valor) {
    if (!valor) return "";
    const iso = valor.length === 10 ? valor + "T00:00:00" : valor;
    return new Date(iso).toLocaleDateString("es-CO", { day: "numeric", month: "short", year: "numeric" });
}

function iniciales(nombre) {
    const p = (nombre || "").trim().split(/\s+/).filter(Boolean);
    if (p.length === 0) return "?";
    return (p[0][0] + (p.length > 1 ? p[p.length - 1][0] : "")).toUpperCase();
}

function tonoDe(texto) {
    let h = 0;
    for (const c of (texto || "").toLowerCase()) h = (h * 31 + c.codePointAt(0)) >>> 0;
    return "t" + (h % 4);
}

function avatar(nombre, usuario, extra) {
    return el("span", { class: "avatar " + tonoDe(usuario || nombre) + (extra ? " " + extra : "") }, iniciales(nombre));
}

function horasTexto(n) {
    return n + (n === 1 ? " hora" : " horas");
}

// ---------- Horarios y estrellas ----------
const FRANJAS = {
    MANANA: { texto: "Mañana", rango: "6 a. m. – 12 m." },
    TARDE: { texto: "Tarde", rango: "12 m. – 6 p. m." },
    NOCHE: { texto: "Noche", rango: "6 – 10 p. m." },
    FLEXIBLE: { texto: "Cualquier horario", rango: "" },
};

function chipFranja(franja) {
    const d = FRANJAS[franja] || FRANJAS.FLEXIBLE;
    return el("span", { class: "chip franja", title: d.rango || "Sin horario fijo" }, d.texto);
}

function estrellasTexto(n) {
    const llenas = Math.round(n || 0);
    return "★".repeat(llenas) + "☆".repeat(5 - llenas);
}

function tiempoRelativo(iso) {
    const minutos = Math.round((Date.now() - new Date(iso).getTime()) / 60000);
    if (minutos < 1) return "ahora mismo";
    if (minutos < 60) return `hace ${minutos} min`;
    const horas = Math.round(minutos / 60);
    if (horas < 24) return `hace ${horas} ${horas === 1 ? "hora" : "horas"}`;
    const dias = Math.round(horas / 24);
    return dias === 1 ? "ayer" : fechaCorta(iso);
}

// ---------- Campana de notificaciones ----------
const ICONO_CAMPANA = '<svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M6 8a6 6 0 1 1 12 0c0 7 3 9 3 9H3s3-2 3-9"/><path d="M10.3 21a1.94 1.94 0 0 0 3.4 0"/></svg>';
const COLOR_NOTIF = {
    SOLICITUD_RECIBIDA: "maiz", RESENA_RECIBIDA: "maiz", SOLICITUD_ACEPTADA: "hoja", TRATO_REALIZADO: "hoja",
    CONFIRMACION_PENDIENTE: "indigo", SOLICITUD_RECHAZADA: "tierra", TRATO_CANCELADO: "tierra", SOLICITUD_EXPIRADA: "tierra",
};

function crearCampana() {
    const icono = el("span", { class: "campana-icono" });
    icono.innerHTML = ICONO_CAMPANA;   // constante propia: no lleva datos de usuarios
    const insignia = el("span", { class: "campana-badge", hidden: true }, "0");
    const boton = el("button", { type: "button", class: "campana", "aria-label": "Notificaciones", "aria-expanded": "false" }, icono, insignia);
    const panel = el("div", { class: "panel-notif", hidden: true, role: "dialog", "aria-label": "Notificaciones" });
    const caja = el("div", { class: "campana-caja" }, boton, panel);
    let noLeidas = 0;

    function pintarInsignia(n) {
        insignia.hidden = n === 0;
        insignia.textContent = n > 9 ? "9+" : String(n);
        boton.setAttribute("aria-label", n ? `Notificaciones (${n} sin leer)` : "Notificaciones");
    }

    async function refrescarConteo(avisarSiHayNuevas) {
        const r = await api("/api/notificaciones/conteo");
        if (!r.ok) return;
        const n = r.data.noLeidas;
        if (avisarSiHayNuevas && n > noLeidas) {
            aviso(n - noLeidas === 1 ? "Tienes una notificación nueva." : `Tienes ${n - noLeidas} notificaciones nuevas.`, "info");
        }
        noLeidas = n;
        pintarInsignia(n);
        if (!panel.hidden) cargarLista();
    }

    async function cargarLista() {
        const r = await api("/api/notificaciones");
        const cabecera = el("div", { class: "panel-notif-cab" },
            el("strong", {}, "Notificaciones"),
            el("button", { type: "button", class: "chico", onclick: async () => {
                await api("/api/notificaciones/leer-todas", { method: "POST" });
                noLeidas = 0; pintarInsignia(0); cargarLista();
            } }, "Marcar todas como leídas"));
        if (!r.ok) { panel.replaceChildren(cabecera, el("p", { class: "ayuda", style: "padding:1rem" }, mensajeDe(r))); return; }
        if (r.data.length === 0) {
            panel.replaceChildren(cabecera, el("p", { class: "ayuda", style: "padding:1.25rem" },
                "Todo al día. Aquí te avisaremos cuando alguien alce la mano, acepte un trato o te deje una reseña."));
            return;
        }
        panel.replaceChildren(cabecera, el("div", { class: "panel-notif-lista" }, r.data.map(n =>
            el("button", { type: "button", class: "notif" + (n.leida ? "" : " nueva"), onclick: async () => {
                if (!n.leida) await api(`/api/notificaciones/${n.id}/leer`, { method: "POST" });
                location.href = n.enlace || "index.html";
            } },
                el("span", { class: "notif-punto " + (COLOR_NOTIF[n.tipo] || "indigo") }),
                el("span", { class: "notif-texto" }, el("span", {}, n.mensaje), el("small", {}, tiempoRelativo(n.fecha)))))));
    }

    function cerrar() { panel.hidden = true; boton.setAttribute("aria-expanded", "false"); }
    boton.addEventListener("click", (e) => {
        e.stopPropagation();
        panel.hidden = !panel.hidden;
        boton.setAttribute("aria-expanded", String(!panel.hidden));
        if (!panel.hidden) cargarLista();
    });
    document.addEventListener("click", (e) => { if (!caja.contains(e.target)) cerrar(); });
    document.addEventListener("keydown", (e) => { if (e.key === "Escape") cerrar(); });

    refrescarConteo(false);
    setInterval(() => refrescarConteo(true), 30000);   // se actualiza solo cada 30 segundos
    return caja;
}

// ---------- Cabecera común ----------
function logoMarca() {
    return el("span", { class: "logo-marca", "aria-hidden": "true" }, el("i"), el("i"), el("i"));
}

// Dibuja la barra superior y devuelve los datos de la persona (o null si no hay sesión)
async function iniciarPagina(activa) {
    const cabecera = document.getElementById("cabecera");
    const enlaces = [
        ["index.html", "La Plaza", "plaza"],
        ["publicar.html", "Publicar", "publicar"],
        ["intercambios.html", "Intercambios", "intercambios"],
        ["libro.html", "Libro de horas", "libro"],
        ["panel.html", "Panel", "panel"],
    ];

    const res = await api("/api/auth/me");
    if (!res.ok) return null;
    const yo = res.data;

    const btnSalir = el("button", { type: "button", class: "sobre-oscuro chico" }, "Salir");
    btnSalir.addEventListener("click", async () => {
        try {
            await fetch("/logout", { method: "POST", headers: { "X-XSRF-TOKEN": tokenCsrf() } });
        } finally {
            location.replace("/login.html?salio=1");
        }
    });

    cabecera.className = "topbar";
    cabecera.replaceChildren(el("div", { class: "topbar-interior" },
        el("a", { class: "logo", href: "index.html" }, logoMarca(), "Minga"),
        el("nav", { class: "nav", "aria-label": "Principal" },
            enlaces.map(([href, texto, clave]) => el("a", { href, class: clave === activa ? "activo" : null }, texto))),
        el("div", { class: "sesion" },
            crearCampana(),
            el("a", { class: "chip-saldo", href: "libro.html", title: "Tu saldo de horas", id: "chipSaldo" },
                el("strong", {}, yo.saldo), yo.saldo === 1 ? "hora" : "horas"),
            el("a", { class: "avatar-usuario", href: "perfil.html", title: yo.nombreCompleto }, iniciales(yo.nombreCompleto)),
            btnSalir)));
    return yo;
}
