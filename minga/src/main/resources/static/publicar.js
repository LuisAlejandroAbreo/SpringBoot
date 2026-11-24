// Publicar y administrar mis publicaciones
(async function () {
    const yo = await iniciarPagina("publicar");
    if (!yo) return;

    const campos = {
        tipo: $("#tipo"), titulo: $("#titulo"), descripcion: $("#descripcion"),
        categoria: $("#categoria"), modalidad: $("#modalidad"), horas: $("#horas"), franja: $("#franja"),
    };
    const mensaje = $("#mensaje");
    let editandoId = null;

    function mostrarError(texto) {
        mensaje.textContent = texto;
        mensaje.className = "mensaje error";
        mensaje.hidden = false;
    }

    const cats = await api("/api/categorias");
    if (cats.ok) cats.data.forEach(c => campos.categoria.append(el("option", { value: c.id }, c.nombre)));

    function modoEdicion(editando) {
        $("#tituloForm").textContent = editando ? "Editar publicación" : "Nueva publicación";
        $("#btnGuardar").textContent = editando ? "Guardar cambios" : "Publicar";
        $("#btnCancelar").hidden = !editando;
        $("#tarjetaForm").scrollIntoView({ block: "nearest", behavior: "smooth" });
    }

    function limpiar() {
        editandoId = null;
        campos.titulo.value = "";
        campos.descripcion.value = "";
        campos.categoria.value = "";
        campos.tipo.value = "OFERTA";
        campos.modalidad.value = "PRESENCIAL";
        campos.horas.value = 1;
        campos.franja.value = "";
        Object.values(campos).forEach(c => c.removeAttribute("aria-invalid"));
        mensaje.hidden = true;
        $("#btnCancelar").hidden = true;
        $("#tituloForm").textContent = "Nueva publicación";
        $("#btnGuardar").textContent = "Publicar";
    }
    $("#btnCancelar").addEventListener("click", limpiar);

    function editar(s) {
        editandoId = s.id;
        campos.tipo.value = s.tipo;
        campos.titulo.value = s.titulo;
        campos.descripcion.value = s.descripcion;
        campos.categoria.value = s.categoriaId;
        campos.modalidad.value = s.modalidad;
        campos.horas.value = s.horasEstimadas;
        campos.franja.value = s.franja;
        mensaje.hidden = true;
        modoEdicion(true);
        campos.titulo.focus({ preventScroll: true });
    }

    $("#formServicio").addEventListener("submit", async (e) => {
        e.preventDefault();
        mensaje.hidden = true;

        const cuerpo = {
            tipo: campos.tipo.value,
            titulo: campos.titulo.value.trim(),
            descripcion: campos.descripcion.value.trim(),
            categoriaId: campos.categoria.value ? Number(campos.categoria.value) : null,
            modalidad: campos.modalidad.value,
            horasEstimadas: Number(campos.horas.value),
            franja: campos.franja.value || null,
        };
        // Marca visual de campos incompletos (el servidor valida todo otra vez)
        const faltan = [["titulo", cuerpo.titulo.length < 5], ["descripcion", cuerpo.descripcion.length < 10],
                        ["categoria", !cuerpo.categoriaId], ["franja", !cuerpo.franja]];
        faltan.forEach(([k, malo]) => malo ? campos[k].setAttribute("aria-invalid", "true") : campos[k].removeAttribute("aria-invalid"));
        const primero = faltan.find(([, malo]) => malo);
        if (primero) {
            mostrarError("Revisa los campos marcados: título (mínimo 5 letras), descripción (mínimo 10), categoría y mejor horario.");
            campos[primero[0]].focus();
            return;
        }

        $("#btnGuardar").disabled = true;
        const res = editandoId
            ? await api("/api/servicios/" + editandoId, { method: "PUT", body: cuerpo })
            : await api("/api/servicios", { method: "POST", body: cuerpo });
        $("#btnGuardar").disabled = false;

        if (res.ok) {
            aviso(editandoId ? "Publicación actualizada." : "Ya estás en la Plaza. ¡Gracias por sumar!", "exito");
            limpiar();
            cargarMias();
        } else {
            mostrarError(mensajeDe(res));
        }
    });

    async function cambiarActivo(s, activar) {
        const res = await api(`/api/servicios/${s.id}/${activar ? "reanudar" : "pausar"}`, { method: "POST" });
        if (res.ok) {
            aviso(activar ? "Tu publicación volvió a la Plaza." : "Pausaste la publicación: ya no aparece en la Plaza.", "info");
            cargarMias();
        } else {
            aviso(mensajeDe(res), "error");
        }
    }

    async function cargarMias() {
        const res = await api("/api/servicios/mios");
        const cont = $("#mias");
        if (!res.ok) { cont.replaceChildren(el("div", { class: "vacio" }, mensajeDe(res))); return; }
        if (res.data.length === 0) {
            cont.replaceChildren(el("div", { class: "vacio" }, el("strong", {}, "Aún no has publicado nada"),
                "Cuéntale a la Minga qué sabes hacer: seguro hay alguien que lo necesita."));
            return;
        }
        cont.replaceChildren(...res.data.map(s => {
            const esOferta = s.tipo === "OFERTA";
            return el("article", { class: "servicio " + (esOferta ? "oferta" : "necesidad") + (s.activo ? "" : " pausado") },
                el("div", { class: "servicio-cab" },
                    el("span", { class: "chip " + (esOferta ? "oferta" : "necesidad") }, esOferta ? "Oferta" : "Necesidad"),
                    el("span", { class: "chip" }, s.categoria),
                    chipFranja(s.franja),
                    s.activo ? null : el("span", { class: "chip", style: "background:#ececf3;color:#5a6080" }, "En pausa"),
                    el("span", { class: "horas" }, s.horasEstimadas + " h")),
                el("h3", {}, s.titulo),
                el("p", { class: "desc" }, s.descripcion),
                el("div", { class: "acciones" },
                    el("button", { type: "button", class: "chico", onclick: () => editar(s) }, "Editar"),
                    el("button", { type: "button", class: "chico " + (s.activo ? "peligro" : ""),
                        onclick: () => cambiarActivo(s, !s.activo) }, s.activo ? "Pausar" : "Reanudar")));
        }));
    }

    cargarMias();
})();
