package com.minga.service;

import com.minga.dto.Peticiones.PerfilRequest;
import com.minga.dto.RegistroRequest;
import com.minga.dto.Respuestas.MeDto;
import com.minga.model.Usuario;
import com.minga.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

@Service
public class UsuarioService {

    private static final Pattern USERNAME = Pattern.compile("^[a-z0-9._-]{4,20}$");
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final UsuarioRepository repository;
    private final PasswordEncoder encoder;
    private final LibroHorasService libro;

    public UsuarioService(UsuarioRepository repository, PasswordEncoder encoder, LibroHorasService libro) {
        this.repository = repository;
        this.encoder = encoder;
        this.libro = libro;
    }

    @Transactional
    public Usuario registrar(RegistroRequest req) {
        String username = Usuario.normalizarUsername(req.username());
        String nombre = limpiar(req.nombreCompleto());
        String email = req.email() == null ? "" : req.email().trim().toLowerCase(Locale.ROOT);
        String barrio = limpiar(req.barrio());

        Map<String, List<String>> errores = new LinkedHashMap<>();

        if (username.isEmpty()) {
            errores.put("username", List.of("El usuario es obligatorio."));
        } else if (!USERNAME.matcher(username).matches()) {
            errores.put("username", List.of(
                    "El usuario debe tener entre 4 y 20 caracteres y solo puede incluir letras, números, punto, guion y guion bajo."));
        }
        if (nombre.isEmpty()) {
            errores.put("nombreCompleto", List.of("Tu nombre es obligatorio."));
        } else if (nombre.length() < 3 || nombre.length() > 60) {
            errores.put("nombreCompleto", List.of("El nombre debe tener entre 3 y 60 caracteres."));
        }
        if (email.isEmpty()) {
            errores.put("email", List.of("El correo es obligatorio."));
        } else if (email.length() > 100 || !EMAIL.matcher(email).matches()) {
            errores.put("email", List.of("Escribe un correo válido (ejemplo: nombre@dominio.com)."));
        }
        if (barrio.isEmpty()) {
            errores.put("barrio", List.of("Cuéntanos de qué barrio eres."));
        } else if (barrio.length() < 2 || barrio.length() > 40) {
            errores.put("barrio", List.of("El barrio debe tener entre 2 y 40 caracteres."));
        }

        List<String> erroresPassword = PasswordValidator.validar(req.password(), username);
        if (!erroresPassword.isEmpty()) {
            errores.put("password", erroresPassword);
        }
        if (!Objects.equals(req.password(), req.confirmarPassword())) {
            errores.put("confirmarPassword", List.of("Las contraseñas no coinciden."));
        }
        if (!errores.isEmpty()) {
            throw new RegistroException(HttpStatus.BAD_REQUEST, errores);
        }

        Map<String, List<String>> duplicados = new LinkedHashMap<>();
        if (repository.existsByUsername(username)) {
            duplicados.put("username", new ArrayList<>(List.of("Ese nombre de usuario ya está en uso.")));
        }
        if (repository.existsByEmail(email)) {
            duplicados.put("email", new ArrayList<>(List.of("Ese correo ya está registrado.")));
        }
        if (!duplicados.isEmpty()) {
            throw new RegistroException(HttpStatus.CONFLICT, duplicados);
        }

        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setNombreCompleto(nombre);
        usuario.setEmail(email);
        usuario.setBarrio(barrio);
        usuario.setPassword(encoder.encode(req.password()));
        usuario = repository.save(usuario);

        libro.darBienvenida(usuario);   // las horas de bienvenida quedan en el libro
        return usuario;
    }

    @Transactional(readOnly = true)
    public Usuario buscar(String username) {
        return repository.findByUsername(Usuario.normalizarUsername(username))
                .orElseThrow(() -> NegocioException.noEncontrado("No encontramos a esa persona."));
    }

    @Transactional(readOnly = true)
    public MeDto yo(String username) {
        Usuario u = buscar(username);
        return new MeDto(u.getUsername(), u.getNombreCompleto(), u.getBarrio(), u.getRol(), libro.saldo(u.getId()));
    }

    @Transactional
    public void actualizarPerfil(String username, PerfilRequest req) {
        Usuario u = buscar(username);
        String nombre = limpiar(req.nombreCompleto());
        String barrio = limpiar(req.barrio());
        String bio = limpiar(req.biografia());

        if (nombre.length() < 3 || nombre.length() > 60) {
            throw NegocioException.invalido("El nombre debe tener entre 3 y 60 caracteres.");
        }
        if (barrio.length() < 2 || barrio.length() > 40) {
            throw NegocioException.invalido("El barrio debe tener entre 2 y 40 caracteres.");
        }
        if (bio.length() > 200) {
            throw NegocioException.invalido("La biografía no puede superar los 200 caracteres.");
        }
        u.setNombreCompleto(nombre);
        u.setBarrio(barrio);
        u.setBiografia(bio.isEmpty() ? null : bio);
        repository.save(u);
    }

    private static String limpiar(String texto) {
        return texto == null ? "" : texto.trim().replaceAll("\\s+", " ");
    }
}
