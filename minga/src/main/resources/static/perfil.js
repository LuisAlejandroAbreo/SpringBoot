// Perfil de un vecino (el propio por defecto): reputación, insignias y reseñas
(async function () {
    const yo = await iniciarPagina(null);
    if (!yo) return;

    const usuario = new URLSearchParams(location.search).get("u") || yo.username;
    const contenido = $("#contenido");
    const res = await api("/api/usuarios/" + encodeURIComponent(usuario) + "/perfil");
    if (!res.ok) {
        contenido.replaceChildren(el("div", { class: "vacio" }, el("strong", {}, "No encontramos a esa persona"), mensajeDe(res)));
        return;
    }
    pintar(res.data);

    function estrellas(promedio) {
        const llenas = Math.round(promedio || 0);
        return "★".repeat(llenas) + "☆".repeat(5 - llenas);
    }

    function pintar(p) {
        const esMio = p.username === yo.username;
        const reputacion = p.promedio === null || p.promedio === undefined
            ? el("span", { class: "ayuda" }, "Aún sin reseñas")
            : el("span", {}, el("span", { class: "estrellas", "aria-hidden": "true" }, estrellas(p.promedio)), " ",
                el("strong", {}, String(p.promedio).replace(".", ",")), ` (${p.totalResenas} ${p.totalResenas === 1 ? "reseña" : "reseñas"})`);

        contenido.replaceChildren(
            el("section", { class: "tarjeta perfil-cab" },
                avatar(p.nombreCompleto, p.username, "grande"),
                el("div", { style: "flex:1;min-width:14rem" },
                    el("h1", {}, p.nombreCompleto),
                    el("p", { class: "bajada", style: "margin-top:.1rem" },
                        "@" + p.username + " · " + p.barrio + " · miembro desde " + fechaCorta(p.miembroDesde)),
                    el("div", { style: "margin-top:.5rem" }, reputacion),
                    p.biografia ? el("p", { style: "margin-top:.6rem" }, p.biografia) : null),
                esMio ? el("button", { type: "button", onclick: () => dialogoEditar(p) }, "Editar perfil") : null),

            el("div", { class: "cifras" },
                el("div", { class: "cifra-tarjeta" }, el("strong", {}, p.intercambiosRealizados), el("span", {}, "intercambios realizados")),
                el("div", { class: "cifra-tarjeta" }, el("strong", {}, p.totalResenas), el("span", {}, "reseñas recibidas"))),

            satisfaccion(p),

            el("section", { class: "tarjeta", style: "margin-bottom:1.5rem" },
                el("h2", {}, "Insignias"),
                p.insignias.length === 0
                    ? el("p", { class: "ayuda" }, "Todavía no hay insignias. La primera llega con el primer intercambio.")
                    : el("div", { class: "insignias" },
                        p.insignias.map(i => el("span", { class: "insignia", title: i.descripcion }, i.nombre)))),

            el("section", { class: "tarjeta" },
                el("h2", {}, "Reseñas"),
                p.resenas.length === 0
                    ? el("p", { class: "ayuda" }, "Aún no hay reseñas.")
                    : p.resenas.map(r => el("div", { class: "resena" },
                        el("div", { style: "display:flex;gap:.6rem;align-items:center;flex-wrap:wrap" },
                            el("span", { class: "estrellas", "aria-label": r.puntaje + " de 5" }, estrellas(r.puntaje)),
                            el("a", { href: "perfil.html?u=" + encodeURIComponent(r.autor), style: "font-weight:600;text-decoration:none" }, r.autorNombre),
                            el("small", { class: "ayuda" }, fechaCorta(r.fecha))),
                        el("small", { class: "ayuda" }, `Por «${r.servicioTitulo}» · cuando ${r.comoProveedor ? "ayudó" : "recibió ayuda"}`),
                        r.comentario ? el("p", { style: "margin-top:.3rem" }, r.comentario) : null))));
    }

    // Nivel de satisfaccion: promedio, desglose por estrellas y por rol (quien da y quien recibe la ayuda)
    function satisfaccion(p) {
        if (p.totalResenas === 0) return document.createDocumentFragment();
        const nota = (valor) => valor === null || valor === undefined ? "sin datos" : "★ " + String(valor).replace(".", ",");
        const maximo = Math.max(...p.distribucion, 1);
        const filas = [5, 4, 3, 2, 1].map(n => el("div", { class: "dist-fila" },
            el("span", {}, n + " ★"),
            el("div", { class: "barra-pista" }, el("div", { class: "barra-relleno", style: "width:" + Math.round(p.distribucion[n - 1] / maximo * 100) + "%" })),
            el("b", {}, p.distribucion[n - 1])));
        return el("section", { class: "tarjeta", style: "margin-bottom:1.5rem" },
            el("h2", {}, "Nivel de satisfacción"),
            el("div", { class: "satisfaccion" },
                el("div", {}, el("div", { class: "grande" }, String(p.promedio).replace(".", ",")),
                    el("div", { class: "estrellas" }, estrellas(p.promedio)),
                    el("small", { class: "ayuda" }, `${p.totalResenas} ${p.totalResenas === 1 ? "reseña" : "reseñas"}`)),
                el("div", { class: "barras", style: "gap:.4rem" }, filas)),
            el("div", { class: "rol-prom" },
                el("span", { class: "chip oferta", title: "Reseñas recibidas cuando dio la ayuda" }, "Como quien ayuda: " + nota(p.promedioComoProveedor)),
                el("span", { class: "chip necesidad", title: "Reseñas recibidas cuando recibió la ayuda" }, "Como quien recibe ayuda: " + nota(p.promedioComoBeneficiario))));
    }

    function dialogoEditar(p) {
        const nombre = el("input", { type: "text", maxlength: 60, value: p.nombreCompleto });
        const barrio = el("input", { type: "text", maxlength: 40, value: p.barrio });
        const bio = el("textarea", { maxlength: 200, placeholder: "Cuéntale a la Minga quién eres y qué te gusta hacer." });
        bio.value = p.biografia || "";

        const formulario = el("form", { novalidate: true },
            el("h2", {}, "Editar perfil"),
            el("div", { class: "campo" }, el("label", {}, "Nombre completo"), nombre),
            el("div", { class: "campo" }, el("label", {}, "Barrio"), barrio),
            el("div", { class: "campo" }, el("label", {}, "Sobre ti"), bio),
            el("div", { class: "dlg-acciones" },
                el("button", { type: "button", onclick: () => dlg.close() }, "Cancelar"),
                el("button", { type: "submit", class: "primario" }, "Guardar")));
        const dlg = el("dialog", {}, formulario);

        formulario.addEventListener("submit", async (e) => {
            e.preventDefault();
            const r = await api("/api/me/perfil", {
                method: "PUT",
                body: { nombreCompleto: nombre.value, barrio: barrio.value, biografia: bio.value },
            });
            if (r.ok) { dlg.close(); aviso("Perfil actualizado.", "exito"); setTimeout(() => location.reload(), 500); }
            else aviso(mensajeDe(r), "error");
        });
        dlg.addEventListener("close", () => dlg.remove());
        document.body.append(dlg);
        dlg.showModal();
    }
})();
