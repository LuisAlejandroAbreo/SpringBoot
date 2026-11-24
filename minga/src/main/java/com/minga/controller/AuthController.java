package com.minga.controller;

import com.minga.dto.RegistroRequest;
import com.minga.dto.Respuestas.MeDto;
import com.minga.service.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UsuarioService usuarios;

    public AuthController(UsuarioService usuarios) {
        this.usuarios = usuarios;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> registrar(@RequestBody RegistroRequest request) {
        usuarios.registrar(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("mensaje", "Bienvenido a la Minga. Te regalamos horas para empezar."));
    }

    /** Datos de la persona con sesion iniciada (el front los usa para la cabecera). */
    @GetMapping("/me")
    public MeDto yo(Authentication authentication) {
        return usuarios.yo(authentication.getName());
    }
}
