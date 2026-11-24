package com.minga.controller;

import com.minga.dto.Peticiones.PerfilRequest;
import com.minga.dto.Respuestas.MeDto;
import com.minga.dto.Respuestas.MovimientoDto;
import com.minga.dto.Respuestas.PerfilDto;
import com.minga.model.Usuario;
import com.minga.service.LibroHorasService;
import com.minga.service.PerfilService;
import com.minga.service.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Libro de horas propio y perfiles. */
@RestController
@RequestMapping("/api")
public class CuentaController {

    private final UsuarioService usuarios;
    private final LibroHorasService libro;
    private final PerfilService perfiles;

    public CuentaController(UsuarioService usuarios, LibroHorasService libro, PerfilService perfiles) {
        this.usuarios = usuarios;
        this.libro = libro;
        this.perfiles = perfiles;
    }

    @GetMapping("/me/saldo")
    public Map<String, Integer> saldo(Authentication auth) {
        MeDto yo = usuarios.yo(auth.getName());
        return Map.of("saldo", yo.saldo());
    }

    @GetMapping("/me/movimientos")
    public List<MovimientoDto> movimientos(Authentication auth) {
        Usuario u = usuarios.buscar(auth.getName());
        return libro.movimientosDe(u.getId());
    }

    @PutMapping("/me/perfil")
    public ResponseEntity<Void> actualizarPerfil(Authentication auth, @RequestBody PerfilRequest request) {
        usuarios.actualizarPerfil(auth.getName(), request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/usuarios/{username}/perfil")
    public PerfilDto perfil(@PathVariable("username") String username) {
        return perfiles.perfil(username);
    }
}
