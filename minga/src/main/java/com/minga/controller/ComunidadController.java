package com.minga.controller;

import com.minga.dto.Respuestas.ImpactoDto;
import com.minga.service.ComunidadService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/comunidad")
public class ComunidadController {

    private final ComunidadService comunidad;

    public ComunidadController(ComunidadService comunidad) {
        this.comunidad = comunidad;
    }

    @GetMapping("/impacto")
    public ImpactoDto impacto() {
        return comunidad.impacto();
    }
}
