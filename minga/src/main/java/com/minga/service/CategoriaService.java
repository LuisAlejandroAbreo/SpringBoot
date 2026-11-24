package com.minga.service;

import com.minga.dto.Peticiones.CategoriaRequest;
import com.minga.dto.Respuestas.CategoriaDto;
import com.minga.model.Categoria;
import com.minga.repository.CategoriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoriaService {

    private final CategoriaRepository repository;

    public CategoriaService(CategoriaRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<CategoriaDto> listar() {
        return repository.findByActivaTrueOrderByNombreAsc().stream().map(CategoriaDto::de).toList();
    }

    @Transactional
    public CategoriaDto crear(CategoriaRequest req) {
        String nombre = req.nombre() == null ? "" : req.nombre().trim();
        String descripcion = req.descripcion() == null ? "" : req.descripcion().trim();
        if (nombre.length() < 3 || nombre.length() > 40) {
            throw NegocioException.invalido("El nombre de la categoría debe tener entre 3 y 40 caracteres.");
        }
        if (descripcion.length() > 120) {
            throw NegocioException.invalido("La descripción no puede superar los 120 caracteres.");
        }
        if (repository.existsByNombreIgnoreCase(nombre)) {
            throw NegocioException.conflicto("Ya existe una categoría con ese nombre.");
        }
        return CategoriaDto.de(repository.save(new Categoria(nombre, descripcion.isEmpty() ? null : descripcion)));
    }
}
