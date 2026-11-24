package com.minga.repository;

import com.minga.model.EstadoIntercambio;
import com.minga.model.Intercambio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface IntercambioRepository extends JpaRepository<Intercambio, Long> {

    @Query("select i from Intercambio i where i.proveedor.id = :uid or i.beneficiario.id = :uid "
            + "order by i.fechaSolicitud desc")
    List<Intercambio> buscarDe(@Param("uid") Long usuarioId);

    @Query("select i from Intercambio i where i.estado = :estado "
            + "and (i.proveedor.id = :uid or i.beneficiario.id = :uid) order by i.fechaCierre")
    List<Intercambio> buscarPorEstadoDe(@Param("estado") EstadoIntercambio estado, @Param("uid") Long usuarioId);

    default List<Intercambio> realizadosDe(Long usuarioId) {
        return buscarPorEstadoDe(EstadoIntercambio.REALIZADO, usuarioId);
    }

    /** Evita pedir dos veces el mismo servicio mientras hay una solicitud viva. */
    boolean existsByServicioIdAndSolicitanteIdAndEstadoIn(Long servicioId, Long solicitanteId,
                                                          List<EstadoIntercambio> estados);

    /** Un servicio no puede tener dos tratos vivos para el mismo dia. */
    boolean existsByServicioIdAndFechaPropuestaAndEstadoIn(Long servicioId, LocalDate fecha,
                                                           List<EstadoIntercambio> estados);

    @Query("select i.fechaPropuesta from Intercambio i where i.servicio.id = :sid "
            + "and i.estado in :estados and i.fechaPropuesta >= :hoy order by i.fechaPropuesta")
    List<LocalDate> diasOcupados(@Param("sid") Long servicioId, @Param("estados") List<EstadoIntercambio> estados,
                                 @Param("hoy") LocalDate hoy);

    List<Intercambio> findByEstadoAndFechaSolicitudBefore(EstadoIntercambio estado, LocalDateTime limite);

    long countByEstado(EstadoIntercambio estado);

    @Query("select sum(i.horasAcordadas) from Intercambio i where i.estado = :estado")
    Long sumarHoras(@Param("estado") EstadoIntercambio estado);

    @Query("select s.categoria.nombre, count(i) from Intercambio i join i.servicio s "
            + "where i.estado = :estado group by s.categoria.nombre order by count(i) desc")
    List<Object[]> contarPorCategoria(@Param("estado") EstadoIntercambio estado);
}
