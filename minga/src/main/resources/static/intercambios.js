// Intercambios: lo que pedí y lo que me pidieron, con los botones según el estado
(async function () {
    const yo = await iniciarPagina("intercambios");
    if (!yo) return;

    let vista = "curso";
    let todos = [];
    const lista = $("#lista");

    $("#segVista").addEventListener("click", (e) => {
        const boton = e.target.closest("button[data-vista]");
        if (!boton) return;
        document.querySelectorAll("#segVista button").forEach(b => b.classList.toggle("activo", b === boton));
        vista = boton.dataset.vista;
        pintar();
    });

    async function cargar() {
        const res = await api("/api/intercambios/mios");
        if (!res.ok) {
            lista.replaceChildren(el("div", { class: "vacio" }, el("strong", {}, "No pudimos cargar tus intercambios"), mensajeDe(res)));
            return;
        }
        todos = res.data;
        pintar();
    }

    const enCurso = (i) => i.estado === "SOLICITADO" || i.estado === "ACEPTADO";

    function pintar() {
        const visibles = todos.filter(i => vista === "curso" ? enCurso(i) : !enCurso(i));
        if (visibles.length === 0) {
            lista.replaceChildren(el("div", { class: "vacio" },
                el("strong", {}, vista === "curso" ? "No tienes tratos en curso" : "Todavía no hay historial"),
                vista === "curso" ? "Entra a la Plaza y alza la mano en algo que te sirva." : "Cuando termines un trato, aparecerá aquí."));
            return;
        }
        lista.replaceChildren(...visibles.map(tarjeta));
    }

    const ETIQUETAS = {
        SOLICITADO: "Esperando respuesta", ACEPTADO: "Aceptado", REALIZADO: "Realizado",
        RECHAZADO: "Rechazado", CANCELADO: "Cancelado", EXPIRADO: "Expirado",
    };

    // Satisfaccion de las dos partes: lo que yo dije y lo que la otra persona dijo de mi
    function bloqueResenas(i) {
        if (i.estado !== "REALIZADO") return null;
        const otro = i.otraPersonaNombre.split(" ")[0];
        const caja = (titulo, r, vacio) => el("div", { class: "resena-caja" },
            el("strong", {}, titulo),
            r ? [el("span", { class: "estrellas", "aria-label": r.puntaje + " de 5" }, estrellasTexto(r.puntaje)),
                 r.comentario ? el("p", {}, "“" + r.comentario + "”") : null]
              : el("span", { class: "ayuda" }, vacio));
        return el("div", { class: "resenas-trato" },
            caja("Tu reseña para " + otro, i.resenaMia, "Aún no dejas tu reseña."),
            caja(otro + " te calificó", i.resenaOtra, otro + " aún no deja su reseña."));
    }

    function tarjeta(i) {
        const doy = i.miRol === "PROVEEDOR";
        const resumen = doy
            ? `Tú ayudas a ${i.otraPersonaNombre} y ganas ${horasTexto(i.horas)}.`
            : `${i.otraPersonaNombre} te ayuda y se descuentan ${horasTexto(i.horas)} de tu saldo.`;

        const otraConfirmo = doy ? i.confirmaBeneficiario : i.confirmaProveedor;
        let avance = null;
        if (i.estado === "ACEPTADO") {
            avance = el("div", { class: "nota" },
                i.yaConfirme ? "Ya confirmaste. " : "Cuando se haga el trato, confirma aquí. ",
                otraConfirmo ? `${i.otraPersonaNombre.split(" ")[0]} ya confirmó.` : `Falta que ${i.otraPersonaNombre.split(" ")[0]} confirme.`);
        }

        const botones = i.acciones.map(accion => {
            switch (accion) {
                case "aceptar":   return el("button", { type: "button", class: "primario chico", onclick: () => ejecutar(i, "aceptar") }, "Aceptar");
                case "confirmar": return el("button", { type: "button", class: "primario chico", onclick: () => ejecutar(i, "confirmar") }, "Confirmar que se hizo");
                case "resenar":   return el("button", { type: "button", class: "indigo chico", onclick: () => dialogoResena(i) }, "Dejar reseña");
                case "rechazar":  return el("button", { type: "button", class: "peligro chico", onclick: () => ejecutar(i, "rechazar") }, "Rechazar");
                case "cancelar":  return el("button", { type: "button", class: "peligro chico", onclick: () => ejecutar(i, "cancelar") }, "Cancelar trato");
                default: return null;
            }
        });

        return el("article", { class: "intercambio" },
            el("div", { class: "intercambio-cab" },
                el("div", {},
                    el("h3", {}, i.servicioTitulo),
                    el("div", { class: "persona", style: "margin-top:.4rem" },
                        avatar(i.otraPersonaNombre, i.otraPersona),
                        el("a", { href: "perfil.html?u=" + encodeURIComponent(i.otraPersona) }, i.otraPersonaNombre))),
                el("span", { class: "estado " + i.estado }, ETIQUETAS[i.estado] || i.estado)),
            el("p", {}, resumen),
            el("div", { class: "datos" },
                el("span", {}, "Pedido el ", el("strong", {}, fechaCorta(i.fechaSolicitud))),
                i.fechaPropuesta ? el("span", {}, "Día del encuentro: ", el("strong", {}, fechaCorta(i.fechaPropuesta))) : null,
                el("span", {}, "Horario: ", chipFranja(i.franja)),
                i.fechaCierre ? el("span", {}, "Cerrado el ", el("strong", {}, fechaCorta(i.fechaCierre))) : null),
            i.mensaje ? el("div", { class: "nota" }, "“" + i.mensaje + "”") : null,
            avance,
            bloqueResenas(i),
            botones.some(Boolean) ? el("div", { class: "acciones" }, botones) : null);
    }

    async function actualizarSaldo() {
        const res = await api("/api/me/saldo");
        const chip = $("#chipSaldo strong");
        if (res.ok && chip) chip.textContent = res.data.saldo;
    }

    const MENSAJES = {
        aceptar: "Trato hecho. Falta ponerse de acuerdo en el día.",
        rechazar: "Rechazaste la solicitud.",
        cancelar: "Cancelaste el trato.",
        confirmar: "Confirmaste. Si la otra persona también confirma, las horas se mueven solas.",
    };

    async function ejecutar(i, accion) {
        if (accion === "rechazar" && !(await confirmar("Rechazar solicitud", "La otra persona verá que no aceptaste este trato.", "Rechazar"))) return;
        if (accion === "cancelar" && !(await confirmar("Cancelar trato", "El trato se cerrará y no se moverán horas.", "Cancelar trato"))) return;

        const res = await api(`/api/intercambios/${i.id}/${accion}`, { method: "POST" });
        if (!res.ok) { aviso(mensajeDe(res), "error"); return; }

        if (accion === "confirmar" && res.data.estado === "REALIZADO") {
            aviso("¡Trato realizado! Las horas ya se movieron en tu libro.", "exito");
            actualizarSaldo();
        } else {
            aviso(MENSAJES[accion], accion === "rechazar" || accion === "cancelar" ? "info" : "exito");
        }
        cargar();
    }

    function dialogoResena(i) {
        let puntaje = 0;
        const estrellas = [1, 2, 3, 4, 5].map(n => el("button", {
            type: "button", "aria-label": n + (n === 1 ? " estrella" : " estrellas"),
            onclick: () => { puntaje = n; estrellas.forEach((b, idx) => b.classList.toggle("on", idx < n)); },
        }, "★"));
        const comentario = el("textarea", { maxlength: 300, placeholder: "Cuenta cómo te fue (opcional)." });

        const formulario = el("form", { novalidate: true },
            el("div", {}, el("h2", {}, "¿Cómo te fue con " + i.otraPersonaNombre.split(" ")[0] + "?"),
                el("p", { class: "bajada" }, i.servicioTitulo)),
            el("div", { class: "estrellas-input", role: "group", "aria-label": "Puntaje" }, estrellas),
            el("div", { class: "campo" }, el("label", {}, "Comentario"), comentario),
            el("div", { class: "dlg-acciones" },
                el("button", { type: "button", onclick: () => dlg.close() }, "Cancelar"),
                el("button", { type: "submit", class: "primario" }, "Enviar reseña")));
        const dlg = el("dialog", {}, formulario);

        formulario.addEventListener("submit", async (e) => {
            e.preventDefault();
            if (!puntaje) { aviso("Elige un puntaje de 1 a 5 estrellas.", "error"); return; }
            const res = await api(`/api/intercambios/${i.id}/resena`, { method: "POST", body: { puntaje, comentario: comentario.value.trim() } });
            if (res.ok) { dlg.close(); aviso("Gracias por tu reseña.", "exito"); cargar(); }
            else aviso(mensajeDe(res), "error");
        });
        dlg.addEventListener("close", () => dlg.remove());
        document.body.append(dlg);
        dlg.showModal();
    }

    cargar();
})();
