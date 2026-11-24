package com.minga.repository;

import com.minga.model.MovimientoHoras;
import com.minga.model.TipoMovimiento;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Libro de horas de SOLO INSERCION: extiende Repository (no JpaRepository),
 * asi que no expone borrar ni modificar en bloque.
 */
public interface MovimientoHorasRepository extends Repository<MovimientoHoras, Long> {

    MovimientoHoras save(MovimientoHoras movimiento);

    List<MovimientoHoras> findByUsuarioIdOrderByIdDesc(Long usuarioId);

    @Query("select sum(m.horas) from MovimientoHoras m where m.usuario.id = :uid and m.tipo = :tipo")
    Long sumarHoras(@Param("uid") Long usuarioId, @Param("tipo") TipoMovimiento tipo);
}
