// Panel de la Minga: cifras de la comunidad + administración de categorías
(async function () {
    const yo = await iniciarPagina("panel");
    if (!yo) return;

    const formato = (n) => Number(n).toLocaleString("es-CO");

    function barras(contenedor, filas, clase) {
        if (filas.length === 0) {
            contenedor.replaceChildren(el("p", { class: "ayuda" }, "Todavía no hay datos para mostrar."));
            return;
        }
        const maximo = Math.max(...filas.map(f => f.total));
        contenedor.replaceChildren(...filas.map(f => el("div", { class: "barra-fila" },
            el("span", { title: f.nombre, style: "overflow:hidden;text-overflow:ellipsis;white-space:nowrap" }, f.nombre),
            el("div", { class: "barra-pista" },
                el("div", { class: "barra-relleno " + (clase || ""), style: "width:" + Math.max(6, Math.round(f.total / maximo * 100)) + "%" })),
            el("b", {}, f.total))));
    }

    const res = await api("/api/comunidad/impacto");
    if (res.ok) {
        const d = res.data;
        $("#cMiembros").textContent = formato(d.miembros);
        $("#cServicios").textContent = formato(d.serviciosActivos);
        $("#cRealizados").textContent = formato(d.intercambiosRealizados);
        $("#cHoras").textContent = formato(d.horasIntercambiadas);
        barras($("#barCategorias"), d.categorias, "");
        barras($("#barBarrios"), d.barrios, "verde");
    } else {
        aviso(mensajeDe(res), "error");
    }

    // Solo el administrador ve el formulario de categorías
    if (yo.rol === "ADMIN") {
        $("#admin").hidden = false;
        $("#formCategoria").addEventListener("submit", async (e) => {
            e.preventDefault();
            const r = await api("/api/categorias", {
                method: "POST",
                body: { nombre: $("#catNombre").value, descripcion: $("#catDescripcion").value },
            });
            if (r.ok) {
                aviso("Categoría creada: " + r.data.nombre, "exito");
                $("#catNombre").value = "";
                $("#catDescripcion").value = "";
            } else {
                aviso(mensajeDe(r), "error");
            }
        });
    }
})();
