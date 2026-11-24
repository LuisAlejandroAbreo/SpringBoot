package com.minga.repository;

import com.minga.model.Servicio;
import com.minga.model.TipoServicio;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServicioRepository extends JpaRepository<Servicio, Long> {

    List<Servicio> findByActivoTrueOrderByFechaPublicacionDesc();

    List<Servicio> findByUsuarioIdOrderByFechaPublicacionDesc(Long usuarioId);

    long countByActivoTrue();

    /** Detecta publicaciones repetidas (mismo usuario, mismo tipo y mismo titulo normalizado). */
    boolean existsByUsuarioIdAndTipoAndTituloNormalizado(Long usuarioId, TipoServicio tipo, String tituloNormalizado);

    boolean existsByUsuarioIdAndTipoAndTituloNormalizadoAndIdNot(Long usuarioId, TipoServicio tipo,
                                                                 String tituloNormalizado, Long id);

    /** Bloquea el servicio mientras se agenda: dos personas no pueden tomar el mismo dia a la vez. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Servicio s where s.id = :id")
    Optional<Servicio> buscarParaActualizar(@Param("id") Long id);
}
