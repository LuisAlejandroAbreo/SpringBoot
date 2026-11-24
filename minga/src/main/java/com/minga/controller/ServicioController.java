package com.minga.controller;

import com.minga.dto.Peticiones.ServicioRequest;
import com.minga.dto.Peticiones.SolicitudRequest;
import com.minga.dto.Respuestas.IntercambioDto;
import com.minga.dto.Respuestas.ServicioDto;
import com.minga.model.Franja;
import com.minga.model.TipoServicio;
import com.minga.service.IntercambioService;
import com.minga.service.ServicioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/servicios")
public class ServicioController {

    private final ServicioService servicios;
    private final IntercambioService intercambios;

    public ServicioController(ServicioService servicios, IntercambioService intercambios) {
        this.servicios = servicios;
        this.intercambios = intercambios;
    }

    /** La Plaza, con filtros opcionales. */
    @GetMapping
    public List<ServicioDto> plaza(@RequestParam(name = "tipo", required = false) TipoServicio tipo,
                                   @RequestParam(name = "categoriaId", required = false) Long categoriaId,
                                   @RequestParam(name = "barrio", required = false) String barrio,
                                   @RequestParam(name = "q", required = false) String q,
                                   @RequestParam(name = "franja", required = false) Franja franja) {
        return servicios.plaza(tipo, categoriaId, barrio, q, franja);
    }

    /** Dias que ya estan tomados para este servicio (el front los bloquea al agendar). */
    @GetMapping("/{id}/ocupados")
    public List<LocalDate> ocupados(@PathVariable("id") Long id) {
        return intercambios.diasOcupados(id);
    }

    @GetMapping("/mios")
    public List<ServicioDto> mios(Authentication auth) {
        return servicios.mios(auth.getName());
    }

    @PostMapping
    public ResponseEntity<ServicioDto> crear(Authentication auth, @RequestBody ServicioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicios.crear(auth.getName(), request));
    }

    @PutMapping("/{id}")
    public ServicioDto actualizar(Authentication auth, @PathVariable("id") Long id,
                                  @RequestBody ServicioRequest request) {
        return servicios.actualizar(auth.getName(), id, request);
    }

    @PostMapping("/{id}/pausar")
    public ServicioDto pausar(Authentication auth, @PathVariable("id") Long id) {
        return servicios.cambiarActivo(auth.getName(), id, false);
    }

    @PostMapping("/{id}/reanudar")
    public ServicioDto reanudar(Authentication auth, @PathVariable("id") Long id) {
        return servicios.cambiarActivo(auth.getName(), id, true);
    }

    /** "Alzar la mano": crea un intercambio SOLICITADO sobre este servicio. */
    @PostMapping("/{id}/solicitar")
    public ResponseEntity<IntercambioDto> solicitar(Authentication auth, @PathVariable("id") Long id,
                                                    @RequestBody(required = false) SolicitudRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(intercambios.solicitar(auth.getName(), id, request));
    }
}
