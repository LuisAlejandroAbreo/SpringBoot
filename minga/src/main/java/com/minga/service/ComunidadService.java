package com.minga.service;

import com.minga.dto.Respuestas.ConteoDto;
import com.minga.dto.Respuestas.ImpactoDto;
import com.minga.model.EstadoIntercambio;
import com.minga.repository.IntercambioRepository;
import com.minga.repository.ServicioRepository;
import com.minga.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Cifras del Panel de la Minga (consultas agregadas). */
@Service
public class ComunidadService {

    private final UsuarioRepository usuarios;
    private final ServicioRepository servicios;
    private final IntercambioRepository intercambios;

    public ComunidadService(UsuarioRepository usuarios, ServicioRepository servicios, IntercambioRepository intercambios) {
        this.usuarios = usuarios;
        this.servicios = servicios;
        this.intercambios = intercambios;
    }

    @Transactional(readOnly = true)
    public ImpactoDto impacto() {
        Long horas = intercambios.sumarHoras(EstadoIntercambio.REALIZADO);
        return new ImpactoDto(
                usuarios.count(),
                servicios.countByActivoTrue(),
                intercambios.countByEstado(EstadoIntercambio.REALIZADO),
                horas == null ? 0L : horas,
                aConteos(intercambios.contarPorCategoria(EstadoIntercambio.REALIZADO)),
                aConteos(usuarios.contarPorBarrio()));
    }

    /** Convierte filas [nombre, total] en objetos y se queda con las 5 primeras. */
    private static List<ConteoDto> aConteos(List<Object[]> filas) {
        return filas.stream()
                .limit(5)
                .map(f -> new ConteoDto(String.valueOf(f[0]), ((Number) f[1]).longValue()))
                .toList();
    }
}
