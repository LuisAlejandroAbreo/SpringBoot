package com.minga.service;

import com.minga.dto.Respuestas.NotificacionDto;
import com.minga.model.Notificacion;
import com.minga.model.TipoNotificacion;
import com.minga.model.Usuario;
import com.minga.repository.NotificacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Avisos de la campana. Se crean dentro de la misma transaccion del evento que los origina. */
@Service
public class NotificacionService {

    private final NotificacionRepository repository;
    private final UsuarioService usuarios;

    public NotificacionService(NotificacionRepository repository, UsuarioService usuarios) {
        this.repository = repository;
        this.usuarios = usuarios;
    }

    /** Primer nombre para mensajes cortos ("Ana alzo la mano..."). */
    public static String primerNombre(Usuario u) {
        String nombre = u.getNombreCompleto() == null ? "" : u.getNombreCompleto().trim();
        int espacio = nombre.indexOf(' ');
        return espacio > 0 ? nombre.substring(0, espacio) : nombre;
    }

    @Transactional
    public void crear(Usuario destinatario, TipoNotificacion tipo, String mensaje, String enlace) {
        repository.save(new Notificacion(destinatario, tipo, mensaje, enlace));
    }

    @Transactional(readOnly = true)
    public List<NotificacionDto> recientes(String username) {
        Usuario u = usuarios.buscar(username);
        return repository.findTop30ByUsuarioIdOrderByFechaDescIdDesc(u.getId()).stream().map(NotificacionDto::de).toList();
    }

    @Transactional(readOnly = true)
    public long noLeidas(String username) {
        return repository.countByUsuarioIdAndLeidaFalse(usuarios.buscar(username).getId());
    }

    @Transactional
    public void marcarLeida(String username, Long id) {
        Usuario u = usuarios.buscar(username);
        Notificacion n = repository.findByIdAndUsuarioId(id, u.getId())
                .orElseThrow(() -> NegocioException.noEncontrado("Esa notificación no existe."));
        if (!n.isLeida()) {
            n.setLeida(true);
            repository.save(n);
        }
    }

    @Transactional
    public void marcarTodas(String username) {
        Usuario u = usuarios.buscar(username);
        List<Notificacion> pendientes = repository.findByUsuarioIdAndLeidaFalse(u.getId());
        pendientes.forEach(n -> n.setLeida(true));
        repository.saveAll(pendientes);
    }
}
