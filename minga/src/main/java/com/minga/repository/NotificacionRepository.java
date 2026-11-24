package com.minga.repository;

import com.minga.model.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    List<Notificacion> findTop30ByUsuarioIdOrderByFechaDescIdDesc(Long usuarioId);

    long countByUsuarioIdAndLeidaFalse(Long usuarioId);

    List<Notificacion> findByUsuarioIdAndLeidaFalse(Long usuarioId);

    Optional<Notificacion> findByIdAndUsuarioId(Long id, Long usuarioId);
}
