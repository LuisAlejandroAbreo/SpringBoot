// La Plaza: ofertas y necesidades con filtros
(async function () {
    const yo = await iniciarPagina("plaza");
    if (!yo) return;

    const rejilla = $("#rejilla");
    const filtros = { tipo: "", categoriaId: "", franja: "", barrio: "", q: "" };
    let ocupados = new Set();
    let pendiente = null;   // servicio sobre el que se alza la mano

    // ----- categorías -----
    const cats = await api("/api/categorias");
    if (cats.ok) {
        cats.data.forEach(c => $("#fCategoria").append(el("option", { value: c.id }, c.nombre)));
    }

    // ----- filtros -----
    $("#segTipo").addEventListener("click", (e) => {
        const boton = e.target.closest("button[data-tipo]");
        if (!boton) return;
        document.querySelectorAll("#segTipo button").forEach(b => b.classList.toggle("activo", b === boton));
        filtros.tipo = boton.dataset.tipo;
        cargar();
    });
    $("#fCategoria").addEventListener("change", (e) => { filtros.categoriaId = e.target.value; cargar(); });
    $("#fFranja").addEventListener("change", (e) => { filtros.franja = e.target.value; cargar(); });

    let espera = null;
    const conRetraso = (campo, clave) => campo.addEventListener("input", () => {
        clearTimeout(espera);
        espera = setTimeout(() => { filtros[clave] = campo.value.trim(); cargar(); }, 300);
    });
    conRetraso($("#fBarrio"), "barrio");
    conRetraso($("#fQ"), "q");

    // ----- listado -----
    async function cargar() {
        const params = new URLSearchParams();
        Object.entries(filtros).forEach(([k, v]) => { if (v) params.set(k, v); });
        const res = await api("/api/servicios?" + params.toString());
        if (!res.ok) {
            rejilla.replaceChildren(el("div", { class: "vacio" }, el("strong", {}, "No pudimos cargar la Plaza"), mensajeDe(res)));
            return;
        }
        if (res.data.length === 0) {
            rejilla.replaceChildren(el("div", { class: "vacio", style: "grid-column:1/-1" },
                el("strong", {}, "La Plaza está tranquila por aquí"),
                "No hay publicaciones con esos filtros. Prueba con otros o publica la primera."));
            return;
        }
        rejilla.replaceChildren(...res.data.map(tarjeta));
    }

    function tarjeta(s) {
        const esMio = s.propietario === yo.username;
        const esOferta = s.tipo === "OFERTA";
        const boton = esMio
            ? el("span", { class: "ayuda" }, "Es tu publicación")
            : el("button", { type: "button", class: "primario chico", onclick: () => abrirSolicitud(s) },
                esOferta ? "Alzar la mano" : "Ofrecer mi ayuda");

        return el("article", { class: "servicio " + (esOferta ? "oferta" : "necesidad") },
            el("div", { class: "servicio-cab" },
                el("span", { class: "chip " + (esOferta ? "oferta" : "necesidad") }, esOferta ? "Oferta" : "Necesidad"),
                el("span", { class: "chip" }, s.categoria),
                chipFranja(s.franja),
                el("span", { class: "horas", title: "Horas estimadas" }, s.horasEstimadas + " h")),
            el("h3", {}, s.titulo),
            el("p", { class: "desc" }, s.descripcion),
            el("div", { class: "servicio-pie" },
                el("div", { class: "persona" },
                    avatar(s.propietarioNombre, s.propietario),
                    el("div", {},
                        el("a", { href: "perfil.html?u=" + encodeURIComponent(s.propietario) }, s.propietarioNombre),
                        el("small", {}, s.barrio + " · " + (s.modalidad === "VIRTUAL" ? "Virtual" : "Presencial")),
                        el("small", { title: "Calificación de las reseñas recibidas" },
                            s.promedioPropietario
                                ? `★ ${String(s.promedioPropietario).replace(".", ",")} (${s.resenasPropietario} ${s.resenasPropietario === 1 ? "reseña" : "reseñas"})`
                                : "Aún sin reseñas"))),
                boton));
    }

    // ----- alzar la mano -----
    const dlg = $("#dlgSolicitar");
    const hoy = new Date().toISOString().slice(0, 10);
    $("#solFecha").min = hoy;

    function abrirSolicitud(s) {
        pendiente = s;
        const esOferta = s.tipo === "OFERTA";
        $("#solTitulo").textContent = esOferta ? "Alzar la mano" : "Ofrecer mi ayuda";
        $("#solResumen").textContent = esOferta
            ? `“${s.titulo}” de ${s.propietarioNombre}. Te costaría ${horasTexto(s.horasEstimadas)} de tu saldo.`
            : `“${s.titulo}” de ${s.propietarioNombre}. Ganarías ${horasTexto(s.horasEstimadas)} si ambos confirman.`;
        const f = FRANJAS[s.franja] || FRANJAS.FLEXIBLE;
        $("#solFranja").textContent = s.franja === "FLEXIBLE"
            ? "Horario: cualquiera, lo acuerdan entre los dos."
            : `Mejor horario: ${f.texto.toLowerCase()} (${f.rango}).`;
        $("#solMensaje").value = "";
        $("#solFecha").value = "";
        $("#solErrorFecha").textContent = "";
        $("#solOcupados").textContent = "";
        $("#solEnviar").disabled = false;
        ocupados = new Set();
        dlg.showModal();

        // Dias que otra persona ya tomo para este mismo servicio
        api(`/api/servicios/${s.id}/ocupados`).then(r => {
            if (!r.ok || !pendiente || pendiente.id !== s.id) return;
            ocupados = new Set(r.data);
            if (r.data.length > 0) {
                $("#solOcupados").textContent = "Días no disponibles: " + r.data.map(fechaCorta).join(", ") + ".";
            }
        });
    }

    // Valida el dia al elegirlo: obligatorio y no puede estar tomado
    $("#solFecha").addEventListener("input", () => {
        const dia = $("#solFecha").value;
        const error = $("#solErrorFecha");
        if (dia && ocupados.has(dia)) {
            error.textContent = "Ese día ya está reservado para este servicio. Elige otra fecha.";
            $("#solEnviar").disabled = true;
        } else {
            error.textContent = "";
            $("#solEnviar").disabled = false;
        }
    });

    $("#solCancelar").addEventListener("click", () => dlg.close());
    $("#formSolicitar").addEventListener("submit", async (e) => {
        e.preventDefault();
        if (!pendiente) return;
        if (!$("#solFecha").value) {
            $("#solErrorFecha").textContent = "Elige el día en que te gustaría el encuentro.";
            $("#solFecha").focus();
            return;
        }
        const boton = $("#solEnviar");
        boton.disabled = true;
        const res = await api(`/api/servicios/${pendiente.id}/solicitar`, {
            method: "POST",
            body: { mensaje: $("#solMensaje").value.trim(), fechaPropuesta: $("#solFecha").value },
        });
        boton.disabled = false;
        if (res.ok) {
            dlg.close();
            aviso(`Alzaste la mano. Ahora falta que ${pendiente.propietarioNombre.split(" ")[0]} te responda.`, "exito");
        } else {
            $("#solErrorFecha").textContent = res.status === 409 ? mensajeDe(res) : "";
            aviso(mensajeDe(res), "error");
        }
    });

    cargar();
})();
