package com.minga.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EstadoIntercambioTest {

    @Test
    void solicitadoYAceptadoSiguenAbiertos() {
        assertFalse(EstadoIntercambio.SOLICITADO.esFinal());
        assertFalse(EstadoIntercambio.ACEPTADO.esFinal());
    }

    @Test
    void losDemasEstadosSonFinales() {
        assertTrue(EstadoIntercambio.REALIZADO.esFinal());
        assertTrue(EstadoIntercambio.RECHAZADO.esFinal());
        assertTrue(EstadoIntercambio.CANCELADO.esFinal());
        assertTrue(EstadoIntercambio.EXPIRADO.esFinal());
    }
}
