package com.minga.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ServicioServiceTest {

    @Test
    void ignoraTildesMayusculasYSignos() {
        assertEquals("clases de guitarra", ServicioService.normalizarTitulo("¡Clases de GUITARRA!"));
        assertEquals("ayuda con tecnologia", ServicioService.normalizarTitulo("  Ayuda   con Tecnología  "));
    }

    @Test
    void titulosIgualesTrasNormalizarSonRepetidos() {
        assertEquals(ServicioService.normalizarTitulo("Arreglo de llaves"),
                ServicioService.normalizarTitulo("arreglo, de llaves."));
    }

    @Test
    void tituloNuloDaVacio() {
        assertEquals("", ServicioService.normalizarTitulo(null));
    }
}
