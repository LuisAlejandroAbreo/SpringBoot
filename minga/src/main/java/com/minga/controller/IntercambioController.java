package com.minga.controller;

import com.minga.dto.Peticiones.ResenaRequest;
import com.minga.dto.Respuestas.IntercambioDto;
import com.minga.dto.Respuestas.ResenaDto;
import com.minga.service.IntercambioService;
import com.minga.service.ResenaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/intercambios")
public class IntercambioController {

    private final IntercambioService intercambios;
    private final ResenaService resenas;

    public IntercambioController(IntercambioService intercambios, ResenaService resenas) {
        this.intercambios = intercambios;
        this.resenas = resenas;
    }

    @GetMapping("/mios")
    public List<IntercambioDto> mios(Authentication auth) {
        return intercambios.mios(auth.getName());
    }

    @GetMapping("/{id}")
    public IntercambioDto obtener(Authentication auth, @PathVariable("id") Long id) {
        return intercambios.obtener(auth.getName(), id);
    }

    @PostMapping("/{id}/aceptar")
    public IntercambioDto aceptar(Authentication auth, @PathVariable("id") Long id) {
        return intercambios.aceptar(auth.getName(), id);
    }

    @PostMapping("/{id}/rechazar")
    public IntercambioDto rechazar(Authentication auth, @PathVariable("id") Long id) {
        return intercambios.rechazar(auth.getName(), id);
    }

    @PostMapping("/{id}/cancelar")
    public IntercambioDto cancelar(Authentication auth, @PathVariable("id") Long id) {
        return intercambios.cancelar(auth.getName(), id);
    }

    @PostMapping("/{id}/confirmar")
    public IntercambioDto confirmar(Authentication auth, @PathVariable("id") Long id) {
        return intercambios.confirmar(auth.getName(), id);
    }

    @PostMapping("/{id}/resena")
    public ResponseEntity<ResenaDto> resenar(Authentication auth, @PathVariable("id") Long id,
                                             @RequestBody ResenaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(resenas.crear(auth.getName(), id, request));
    }
}
