package com.minga.service;

import com.minga.dto.Respuestas.InsigniaDto;
import com.minga.dto.Respuestas.PerfilDto;
import com.minga.dto.Respuestas.ResenaDto;
import com.minga.model.Intercambio;
import com.minga.model.Usuario;
import com.minga.repository.IntercambioRepository;
import com.minga.repository.ResenaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Perfil publico: reputacion (promedio de reseñas) e insignias calculadas con la actividad real. */
@Service
public class PerfilService {

    private final UsuarioService usuarios;
    private final ResenaRepository resenas;
    private final IntercambioRepository intercambios;

    public PerfilService(UsuarioService usuarios, ResenaRepository resenas, IntercambioRepository intercambios) {
        this.usuarios = usuarios;
        this.resenas = resenas;
        this.intercambios = intercambios;
    }

    @Transactional(readOnly = true)
    public PerfilDto perfil(String username) {
        Usuario u = usuarios.buscar(username);
        List<ResenaDto> lista = resenas.findByDestinatarioIdOrderByFechaDesc(u.getId())
                .stream().map(ResenaDto::de).toList();

        // Satisfaccion: promedio general, promedio segun el rol que tuvo y cuantas resenas de cada puntaje
        Double promedio = redondear(resenas.promedioDe(u.getId()));
        Double comoProveedor = redondear(resenas.promedioComoProveedor(u.getId()));
        Double comoBeneficiario = redondear(resenas.promedioComoBeneficiario(u.getId()));
        long[] conteo = new long[5];
        for (Object[] fila : resenas.distribucionDe(u.getId())) {
            int puntaje = ((Number) fila[0]).intValue();
            if (puntaje >= 1 && puntaje <= 5) {
                conteo[puntaje - 1] = ((Number) fila[1]).longValue();
            }
        }
        List<Long> distribucion = new ArrayList<>();
        for (long c : conteo) {
            distribucion.add(c);
        }

        List<Intercambio> realizados = intercambios.realizadosDe(u.getId());
        Set<Long> personasAyudadas = new HashSet<>();
        List<LocalDate> fechasCierre = new ArrayList<>();
        for (Intercambio i : realizados) {
            if (i.getProveedor().getId().equals(u.getId())) {
                personasAyudadas.add(i.getBeneficiario().getId());
            }
            if (i.getFechaCierre() != null) {
                fechasCierre.add(i.getFechaCierre().toLocalDate());
            }
        }

        List<InsigniaDto> insignias = calcularInsignias(realizados.size(), promedio, personasAyudadas.size(), fechasCierre);
        return new PerfilDto(u.getUsername(), u.getNombreCompleto(), u.getBarrio(), u.getBiografia(),
                u.getFechaRegistro(), promedio, lista.size(), comoProveedor, comoBeneficiario, distribucion,
                realizados.size(), insignias, lista);
    }

    private static Double redondear(Double valor) {
        return valor == null ? null : Math.round(valor * 10.0) / 10.0;
    }

    /** Reglas de las insignias (logica pura: se puede probar sin base de datos). */
    public static List<InsigniaDto> calcularInsignias(int realizados, Double promedio,
                                                      long personasAyudadas, Collection<LocalDate> fechasCierre) {
        List<InsigniaDto> lista = new ArrayList<>();
        if (realizados >= 1) {
            lista.add(new InsigniaDto("primera-minga", "Primera minga", "Completó su primer intercambio."));
        }
        if (realizados >= 5 && promedio != null && promedio >= 4.5) {
            lista.add(new InsigniaDto("mano-firme", "Mano firme", "5 intercambios realizados con promedio de 4,5 o más."));
        }
        if (personasAyudadas >= 10) {
            lista.add(new InsigniaDto("vecino-estrella", "Vecino estrella", "Ha ayudado a 10 personas distintas."));
        }
        if (haySemanasSeguidas(fechasCierre, 4)) {
            lista.add(new InsigniaDto("constante", "Constante", "Realizó intercambios durante 4 semanas seguidas."));
        }
        return lista;
    }

    /** True si hay actividad en al menos {@code semanas} semanas consecutivas. */
    public static boolean haySemanasSeguidas(Collection<LocalDate> fechas, int semanas) {
        Set<LocalDate> lunes = new HashSet<>();
        for (LocalDate f : fechas) {
            lunes.add(f.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)));
        }
        for (LocalDate inicio : lunes) {
            boolean seguidas = true;
            for (int k = 1; k < semanas; k++) {
                if (!lunes.contains(inicio.plusWeeks(k))) {
                    seguidas = false;
                    break;
                }
            }
            if (seguidas) {
                return true;
            }
        }
        return false;
    }
}
