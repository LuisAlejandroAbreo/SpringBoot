package com.minga.model;

public enum EstadoIntercambio {
    SOLICITADO, ACEPTADO, REALIZADO, RECHAZADO, CANCELADO, EXPIRADO;

    /** Un estado final ya no admite ningun cambio. */
    public boolean esFinal() {
        return this != SOLICITADO && this != ACEPTADO;
    }
}
