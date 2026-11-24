package com.minga.repository;

import com.minga.model.Usuario;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    /** Bloquea la fila hasta terminar la transaccion: asi dos transferencias no gastan las mismas horas. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where u.id = :id")
    Optional<Usuario> buscarParaActualizar(@Param("id") Long id);

    @Query("select u.barrio, count(u) from Usuario u group by u.barrio order by count(u) desc")
    List<Object[]> contarPorBarrio();
}
