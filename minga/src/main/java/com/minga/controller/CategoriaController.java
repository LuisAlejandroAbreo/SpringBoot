package com.minga.controller;

import com.minga.dto.Peticiones.CategoriaRequest;
import com.minga.dto.Respuestas.CategoriaDto;
import com.minga.service.CategoriaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {

    private final CategoriaService categorias;

    public CategoriaController(CategoriaService categorias) {
        this.categorias = categorias;
    }

    @GetMapping
    public List<CategoriaDto> listar() {
        return categorias.listar();
    }

    // Solo ADMIN (se exige en SecurityConfig)
    @PostMapping
    public ResponseEntity<CategoriaDto> crear(@RequestBody CategoriaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categorias.crear(request));
    }
}
