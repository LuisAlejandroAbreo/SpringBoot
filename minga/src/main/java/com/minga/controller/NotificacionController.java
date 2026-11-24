package com.minga.controller;

import com.minga.dto.Respuestas.NotificacionDto;
import com.minga.service.NotificacionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Campana de notificaciones. */
@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {

    private final NotificacionService notificaciones;

    public NotificacionController(NotificacionService notificaciones) {
        this.notificaciones = notificaciones;
    }

    @GetMapping
    public List<NotificacionDto> recientes(Authentication auth) {
        return notificaciones.recientes(auth.getName());
    }

    /** Liviano: el front lo consulta cada cierto tiempo para actualizar el numerito rojo. */
    @GetMapping("/conteo")
    public Map<String, Long> conteo(Authentication auth) {
        return Map.of("noLeidas", notificaciones.noLeidas(auth.getName()));
    }

    @PostMapping("/{id}/leer")
    public ResponseEntity<Void> leer(Authentication auth, @PathVariable("id") Long id) {
        notificaciones.marcarLeida(auth.getName(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/leer-todas")
    public ResponseEntity<Void> leerTodas(Authentication auth) {
        notificaciones.marcarTodas(auth.getName());
        return ResponseEntity.noContent().build();
    }
}
