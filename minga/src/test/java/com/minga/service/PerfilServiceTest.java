package com.minga.service;

import com.minga.dto.Respuestas.InsigniaDto;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerfilServiceTest {

    @Test
    void sinActividadNoHayInsignias() {
        assertTrue(PerfilService.calcularInsignias(0, null, 0, List.of()).isEmpty());
    }

    @Test
    void primerIntercambioDaPrimeraMinga() {
        List<InsigniaDto> insignias = PerfilService.calcularInsignias(1, 5.0, 1, List.of(LocalDate.of(2026, 11, 2)));
        assertEquals(1, insignias.size());
        assertEquals("primera-minga", insignias.get(0).codigo());
    }

    @Test
    void manoFirmeExigeCincoIntercambiosYPromedioAlto() {
        assertTrue(PerfilService.calcularInsignias(5, 4.5, 3, List.of()).stream()
                .anyMatch(i -> i.codigo().equals("mano-firme")));
        assertFalse(PerfilService.calcularInsignias(5, 4.4, 3, List.of()).stream()
                .anyMatch(i -> i.codigo().equals("mano-firme")));
        assertFalse(PerfilService.calcularInsignias(4, 5.0, 3, List.of()).stream()
                .anyMatch(i -> i.codigo().equals("mano-firme")));
    }

    @Test
    void vecinoEstrellaRequiereDiezPersonasDistintas() {
        assertTrue(PerfilService.calcularInsignias(10, 4.0, 10, List.of()).stream()
                .anyMatch(i -> i.codigo().equals("vecino-estrella")));
        assertFalse(PerfilService.calcularInsignias(10, 4.0, 9, List.of()).stream()
                .anyMatch(i -> i.codigo().equals("vecino-estrella")));
    }

    @Test
    void cuatroSemanasSeguidasDanConstante() {
        // lunes 2, 9, 16 y 23 de noviembre de 2026
        List<LocalDate> seguidas = List.of(LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 11),
                LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 28));
        assertTrue(PerfilService.haySemanasSeguidas(seguidas, 4));

        List<LocalDate> conHueco = List.of(LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 11),
                LocalDate.of(2026, 11, 28), LocalDate.of(2026, 12, 8));
        assertFalse(PerfilService.haySemanasSeguidas(conHueco, 4));
    }

    @Test
    void variosIntercambiosEnLaMismaSemanaCuentanComoUna() {
        List<LocalDate> mismaSemana = List.of(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 3),
                LocalDate.of(2026, 11, 4), LocalDate.of(2026, 11, 5));
        assertFalse(PerfilService.haySemanasSeguidas(mismaSemana, 4));
    }
}
