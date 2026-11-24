package com.minga.service;

import com.minga.dto.Peticiones.ResenaRequest;
import com.minga.dto.Respuestas.ResenaDto;
import com.minga.model.EstadoIntercambio;
import com.minga.model.Intercambio;
import com.minga.model.Resena;
import com.minga.model.TipoNotificacion;
import com.minga.model.Usuario;
import com.minga.repository.IntercambioRepository;
import com.minga.repository.ResenaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResenaService {

    private final ResenaRepository resenas;
    private final IntercambioRepository intercambios;
    private final UsuarioService usuarios;
    private final NotificacionService notificaciones;

    public ResenaService(ResenaRepository resenas, IntercambioRepository intercambios,
                         UsuarioService usuarios, NotificacionService notificaciones) {
        this.resenas = resenas;
        this.intercambios = intercambios;
        this.usuarios = usuarios;
        this.notificaciones = notificaciones;
    }

    @Transactional
    public ResenaDto crear(String username, Long intercambioId, ResenaRequest req) {
        Usuario yo = usuarios.buscar(username);
        Intercambio i = intercambios.findById(intercambioId)
                .orElseThrow(() -> NegocioException.noEncontrado("Ese intercambio no existe."));

        if (!i.esParticipante(yo.getId())) {
            throw NegocioException.prohibido("Solo quienes participaron pueden dejar una reseña.");
        }
        if (i.getEstado() != EstadoIntercambio.REALIZADO) {
            throw NegocioException.conflicto("Solo puedes reseñar un intercambio que ya se realizó.");
        }
        if (req == null || req.puntaje() == null || req.puntaje() < 1 || req.puntaje() > 5) {
            throw NegocioException.invalido("El puntaje debe estar entre 1 y 5.");
        }
        String comentario = req.comentario() == null ? "" : req.comentario().trim();
        if (comentario.length() > 300) {
            throw NegocioException.invalido("El comentario no puede superar los 300 caracteres.");
        }
        if (resenas.existsByIntercambioIdAndAutorId(intercambioId, yo.getId())) {
            throw NegocioException.conflicto("Ya dejaste tu reseña para este intercambio.");
        }

        Usuario destinatario = i.getProveedor().getId().equals(yo.getId()) ? i.getBeneficiario() : i.getProveedor();
        Resena r = new Resena();
        r.setIntercambio(i);
        r.setAutor(yo);
        r.setDestinatario(destinatario);
        r.setPuntaje(req.puntaje());
        r.setComentario(comentario.isEmpty() ? null : comentario);
        r = resenas.save(r);

        // Aviso en la campana de la persona reseñada
        notificaciones.crear(destinatario, TipoNotificacion.RESENA_RECIBIDA,
                NotificacionService.primerNombre(yo) + " te dejó una reseña de " + req.puntaje()
                        + (req.puntaje() == 1 ? " estrella" : " estrellas") + " por «" + i.getServicio().getTitulo() + "».",
                "perfil.html");
        return ResenaDto.de(r);
    }
}
