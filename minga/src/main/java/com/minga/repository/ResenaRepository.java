package com.minga.repository;

import com.minga.model.Resena;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResenaRepository extends JpaRepository<Resena, Long> {

    boolean existsByIntercambioIdAndAutorId(Long intercambioId, Long autorId);

    List<Resena> findByDestinatarioIdOrderByFechaDesc(Long destinatarioId);

    @Query("select avg(r.puntaje) from Resena r where r.destinatario.id = :uid")
    Double promedioDe(@Param("uid") Long usuarioId);

    List<Resena> findByIntercambioId(Long intercambioId);

    /** Filas [puntaje, cantidad] de las resenas recibidas. */
    @Query("select r.puntaje, count(r) from Resena r where r.destinatario.id = :uid group by r.puntaje")
    List<Object[]> distribucionDe(@Param("uid") Long usuarioId);

    /** Satisfaccion cuando la persona dio la ayuda (era la proveedora del trato). */
    @Query("select avg(r.puntaje) from Resena r where r.destinatario.id = :uid and r.intercambio.proveedor.id = :uid")
    Double promedioComoProveedor(@Param("uid") Long usuarioId);

    /** Satisfaccion cuando la persona recibio la ayuda (era la beneficiaria del trato). */
    @Query("select avg(r.puntaje) from Resena r where r.destinatario.id = :uid and r.intercambio.beneficiario.id = :uid")
    Double promedioComoBeneficiario(@Param("uid") Long usuarioId);

    /** Filas [usuarioId, promedio, cantidad] para mostrar la calificacion en la Plaza. */
    @Query("select r.destinatario.id, avg(r.puntaje), count(r) from Resena r group by r.destinatario.id")
    List<Object[]> resumenPorUsuario();
}
