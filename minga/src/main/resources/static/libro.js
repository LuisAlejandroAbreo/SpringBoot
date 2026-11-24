// Libro de horas: saldo y movimientos (solo lectura)
(async function () {
    const yo = await iniciarPagina("libro");
    if (!yo) return;
    $("#saldo").textContent = yo.saldo;
    $("#saldoEtiqueta").textContent = yo.saldo === 1 ? "hora disponible" : "horas disponibles";

    const res = await api("/api/me/movimientos");
    const cuerpo = $("#movimientos");
    if (!res.ok) {
        cuerpo.replaceChildren(el("tr", {}, el("td", { colspan: 4 }, mensajeDe(res))));
        return;
    }
    if (res.data.length === 0) {
        cuerpo.replaceChildren(el("tr", {}, el("td", { colspan: 4, style: "text-align:center;color:#5a6080" }, "Aún no hay movimientos.")));
        return;
    }
    cuerpo.replaceChildren(...res.data.map(m => {
        const resta = m.tipo === "DEBITO";
        return el("tr", {},
            el("td", {}, fechaCorta(m.fecha)),
            el("td", {}, m.descripcion),
            el("td", { class: resta ? "menos" : "mas" }, (resta ? "− " : "+ ") + m.horas + " h"),
            el("td", {}, m.saldoResultante + " h"));
    }));
})();
