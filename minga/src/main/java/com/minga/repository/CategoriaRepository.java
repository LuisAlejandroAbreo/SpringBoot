package com.minga.repository;

import com.minga.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    List<Categoria> findByActivaTrueOrderByNombreAsc();

    boolean existsByNombreIgnoreCase(String nombre);
}
