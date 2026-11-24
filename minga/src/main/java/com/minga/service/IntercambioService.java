package com.minga.service;

import com.minga.dto.Peticiones.SolicitudRequest;
import com.minga.dto.Respuestas.IntercambioDto;
import com.minga.dto.Respuestas.ResenaResumen;
import com.minga.model.EstadoIntercambio;
import com.minga.model.Intercambio;
import com.minga.model.Resena;
import com.minga.model.Servicio;
import com.minga.model.TipoNotificacion;
import com.minga.model.TipoServicio;
import com.minga.model.Usuario;
import com.minga.repository.IntercambioRepository;
import com.minga.repository.ResenaRepository;
import com.minga.repository.ServicioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Maquina de estados del intercambio:
 * SOLICITADO -> ACEPTADO -> REALIZADO, con salidas a RECHAZADO, CANCELADO y EXPIRADO.
 * Cada evento importante deja una notificacion para la otra persona (la campana).
 */
@Service
public class IntercambioService {

    private static final List<EstadoIntercambio> ESTADOS_VIVOS =
            List.of(EstadoIntercambio.SOLICITADO, EstadoIntercambio.ACEPTADO);
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("d 'de' MMMM", Locale.forLanguageTag("es-CO"));
    private static final String PAGINA_TRATOS = "intercambios.html";

    private final IntercambioRepository intercambios;
    private final ServicioRepository servicios;
    private final ResenaRepository resenas;
    private final UsuarioService usuarios;
    private final LibroHorasService libro;
    private final NotificacionService notificaciones;
    private final int diasExpiracion;

    public IntercambioService(IntercambioRepository intercambios, ServicioRepository servicios,
                              ResenaRepository resenas, UsuarioService usuarios, LibroHorasService libro,
                              NotificacionService notificaciones,
                              @Value("${app.minga.dias-expiracion:7}") int diasExpiracion) {
        this.intercambios = intercambios;
        this.servicios = servicios;
        this.resenas = resenas;
        this.usuarios = usuarios;
        this.libro = libro;
        this.notificaciones = notificaciones;
        this.diasExpiracion = diasExpiracion;
    }

    // ------------------------------------------------------------------ consultas

    @Transactional(readOnly = true)
    public List<IntercambioDto> mios(String username) {
        Usuario yo = usuarios.buscar(username);
        return intercambios.buscarDe(yo.getId()).stream().map(i -> vista(i, yo)).toList();
    }

    @Transactional(readOnly = true)
    public IntercambioDto obtener(String username, Long id) {
        Usuario yo = usuarios.buscar(username);
        Intercambio i = cargar(id);
        exigirParticipante(i, yo);
        return vista(i, yo);
    }

    /** Dias ya tomados (solicitados o aceptados) de un servicio, para que nadie los vuelva a pedir. */
    @Transactional(readOnly = true)
    public List<LocalDate> diasOcupados(Long servicioId) {
        return intercambios.diasOcupados(servicioId, ESTADOS_VIVOS, LocalDate.now());
    }

    // ------------------------------------------------------------------ transiciones

    /** "Alzar la mano": crea el intercambio en estado SOLICITADO. */
    @Transactional
    public IntercambioDto solicitar(String username, Long servicioId, SolicitudRequest req) {
        Usuario yo = usuarios.buscar(username);
        // Se bloquea el servicio mientras se agenda: dos solicitudes al mismo tiempo se atienden una por una
        Servicio servicio = servicios.buscarParaActualizar(servicioId)
                .orElseThrow(() -> NegocioException.noEncontrado("Ese servicio no existe."));

        if (!servicio.isActivo()) {
            throw NegocioException.conflicto("Este servicio ya no está disponible.");
        }
        if (servicio.getUsuario().getId().equals(yo.getId())) {
            throw NegocioException.invalido("No puedes pedir tu propio servicio.");
        }
        if (intercambios.existsByServicioIdAndSolicitanteIdAndEstadoIn(servicioId, yo.getId(), ESTADOS_VIVOS)) {
            throw NegocioException.conflicto("Ya tienes una solicitud en curso para este servicio.");
        }

        String mensaje = req == null || req.mensaje() == null ? "" : req.mensaje().trim();
        if (mensaje.length() > 300) {
            throw NegocioException.invalido("El mensaje no puede superar los 300 caracteres.");
        }

        // El dia es obligatorio y no puede estar tomado por otro trato del mismo servicio
        LocalDate fecha = req == null ? null : req.fechaPropuesta();
        if (fecha == null) {
            throw NegocioException.invalido("Elige el día en que te gustaría el encuentro.");
        }
        if (fecha.isBefore(LocalDate.now())) {
            throw NegocioException.invalido("La fecha propuesta no puede ser anterior a hoy.");
        }
        if (fecha.isAfter(LocalDate.now().plusDays(120))) {
            throw NegocioException.invalido("Elige una fecha dentro de los próximos 120 días.");
        }
        if (intercambios.existsByServicioIdAndFechaPropuestaAndEstadoIn(servicioId, fecha, ESTADOS_VIVOS)) {
            throw NegocioException.conflicto("Ese día ya está reservado para este servicio. Elige otra fecha.");
        }

        Intercambio i = new Intercambio();
        i.setServicio(servicio);
        i.setSolicitante(yo);
        i.setHorasAcordadas(servicio.getHorasEstimadas());
        i.setMensaje(mensaje.isEmpty() ? null : mensaje);
        i.setFechaPropuesta(fecha);

        // Quien da y quien recibe depende de si el servicio es una oferta o una necesidad
        if (servicio.getTipo() == TipoServicio.OFERTA) {
            i.setProveedor(servicio.getUsuario());
            i.setBeneficiario(yo);
            exigirSaldo(yo, i.getHorasAcordadas(), true);
        } else {
            i.setProveedor(yo);
            i.setBeneficiario(servicio.getUsuario());
        }
        i = intercambios.save(i);

        notificaciones.crear(servicio.getUsuario(), TipoNotificacion.SOLICITUD_RECIBIDA,
                NotificacionService.primerNombre(yo) + " alzó la mano en «" + servicio.getTitulo()
                        + "» para el " + DIA.format(fecha) + ".", PAGINA_TRATOS);
        return vista(i, yo);
    }

    @Transactional
    public IntercambioDto aceptar(String username, Long id) {
        Usuario yo = usuarios.buscar(username);
        Intercambio i = cargar(id);
        exigirParticipante(i, yo);
        exigirEstado(i, EstadoIntercambio.SOLICITADO);
        if (!i.esDuenoDelServicio(yo.getId())) {
            throw NegocioException.prohibido("Solo quien publicó el servicio puede aceptar la solicitud.");
        }
        exigirSaldo(i.getBeneficiario(), i.getHorasAcordadas(), i.getBeneficiario().getId().equals(yo.getId()));
        i.setEstado(EstadoIntercambio.ACEPTADO);
        i = intercambios.save(i);

        notificaciones.crear(i.getSolicitante(), TipoNotificacion.SOLICITUD_ACEPTADA,
                NotificacionService.primerNombre(yo) + " aceptó tu solicitud de «" + i.getServicio().getTitulo()
                        + "» para el " + DIA.format(i.getFechaPropuesta()) + ".", PAGINA_TRATOS);
        return vista(i, yo);
    }

    @Transactional
    public IntercambioDto rechazar(String username, Long id) {
        Usuario yo = usuarios.buscar(username);
        Intercambio i = cargar(id);
        exigirParticipante(i, yo);
        exigirEstado(i, EstadoIntercambio.SOLICITADO);
        if (!i.esDuenoDelServicio(yo.getId())) {
            throw NegocioException.prohibido("Solo quien publicó el servicio puede rechazar la solicitud.");
        }
        cerrar(i, EstadoIntercambio.RECHAZADO);
        i = intercambios.save(i);

        notificaciones.crear(i.getSolicitante(), TipoNotificacion.SOLICITUD_RECHAZADA,
                NotificacionService.primerNombre(yo) + " no pudo aceptar tu solicitud de «"
                        + i.getServicio().getTitulo() + "».", PAGINA_TRATOS);
        return vista(i, yo);
    }

    @Transactional
    public IntercambioDto cancelar(String username, Long id) {
        Usuario yo = usuarios.buscar(username);
        Intercambio i = cargar(id);
        exigirParticipante(i, yo);
        if (i.getEstado() == EstadoIntercambio.SOLICITADO) {
            if (!i.getSolicitante().getId().equals(yo.getId())) {
                throw NegocioException.prohibido("Si no quieres aceptar la solicitud, usa Rechazar.");
            }
        } else {
            exigirEstado(i, EstadoIntercambio.ACEPTADO);
        }
        cerrar(i, EstadoIntercambio.CANCELADO);
        i = intercambios.save(i);

        notificaciones.crear(otraPersona(i, yo), TipoNotificacion.TRATO_CANCELADO,
                NotificacionService.primerNombre(yo) + " canceló el trato «" + i.getServicio().getTitulo() + "».",
                PAGINA_TRATOS);
        return vista(i, yo);
    }

    /** Cada participante confirma por su lado; cuando ambos confirmaron, las horas se transfieren. */
    @Transactional
    public IntercambioDto confirmar(String username, Long id) {
        Usuario yo = usuarios.buscar(username);
        Intercambio i = cargar(id);
        exigirParticipante(i, yo);
        exigirEstado(i, EstadoIntercambio.ACEPTADO);

        boolean soyProveedor = i.getProveedor().getId().equals(yo.getId());
        if (soyProveedor ? i.isConfirmaProveedor() : i.isConfirmaBeneficiario()) {
            throw NegocioException.conflicto("Ya confirmaste este trato. Falta que la otra persona confirme.");
        }
        if (soyProveedor) {
            i.setConfirmaProveedor(true);
        } else {
            i.setConfirmaBeneficiario(true);
        }

        String titulo = i.getServicio().getTitulo();
        if (i.isConfirmaProveedor() && i.isConfirmaBeneficiario()) {
            libro.transferir(i);                       // debito + credito en la misma transaccion
            cerrar(i, EstadoIntercambio.REALIZADO);
            notificaciones.crear(otraPersona(i, yo), TipoNotificacion.TRATO_REALIZADO,
                    "¡Trato realizado! «" + titulo + "»: las horas ya se movieron en tu libro.", "libro.html");
        } else {
            notificaciones.crear(otraPersona(i, yo), TipoNotificacion.CONFIRMACION_PENDIENTE,
                    NotificacionService.primerNombre(yo) + " confirmó que «" + titulo + "» se hizo. Falta tu confirmación.",
                    PAGINA_TRATOS);
        }
        return vista(intercambios.save(i), yo);
    }

    /** Tarea programada: las solicitudes sin respuesta pasan a EXPIRADO. */
    @Transactional
    public int expirarVencidos() {
        LocalDateTime limite = LocalDateTime.now().minusDays(diasExpiracion);
        List<Intercambio> vencidos = intercambios.findByEstadoAndFechaSolicitudBefore(EstadoIntercambio.SOLICITADO, limite);
        for (Intercambio i : vencidos) {
            cerrar(i, EstadoIntercambio.EXPIRADO);
            notificaciones.crear(i.getSolicitante(), TipoNotificacion.SOLICITUD_EXPIRADA,
                    "Tu solicitud de «" + i.getServicio().getTitulo() + "» expiró por falta de respuesta.", PAGINA_TRATOS);
        }
        intercambios.saveAll(vencidos);
        return vencidos.size();
    }

    // ------------------------------------------------------------------ ayudas

    private Intercambio cargar(Long id) {
        return intercambios.findById(id)
                .orElseThrow(() -> NegocioException.noEncontrado("Ese intercambio no existe."));
    }

    private static Usuario otraPersona(Intercambio i, Usuario yo) {
        return i.getProveedor().getId().equals(yo.getId()) ? i.getBeneficiario() : i.getProveedor();
    }

    private void exigirParticipante(Intercambio i, Usuario yo) {
        if (!i.esParticipante(yo.getId())) {
            throw NegocioException.prohibido("Este intercambio no es tuyo.");
        }
    }

    private void exigirEstado(Intercambio i, EstadoIntercambio esperado) {
        if (i.getEstado() != esperado) {
            throw NegocioException.conflicto("Este intercambio ya no admite esa acción (estado actual: "
                    + i.getEstado().name().toLowerCase() + ").");
        }
    }

    private void exigirSaldo(Usuario beneficiario, int horas, boolean esMismaPersona) {
        int saldo = libro.saldo(beneficiario.getId());
        if (saldo < horas) {
            throw NegocioException.conflicto(esMismaPersona
                    ? "Te faltan " + (horas - saldo) + " horas. Ofrece algo de lo que sabes y vuelve."
                    : "La otra persona no tiene horas suficientes en este momento.");
        }
    }

    private void cerrar(Intercambio i, EstadoIntercambio estadoFinal) {
        i.setEstado(estadoFinal);
        i.setFechaCierre(LocalDateTime.now());
    }

    /** Vista del trato con las dos reseñas (la mia y la de la otra persona), para ver la satisfaccion de ambos. */
    private IntercambioDto vista(Intercambio i, Usuario yo) {
        ResenaResumen mia = null;
        ResenaResumen otra = null;
        if (i.getId() != null) {
            for (Resena r : resenas.findByIntercambioId(i.getId())) {
                if (r.getAutor().getId().equals(yo.getId())) {
                    mia = ResenaResumen.de(r);
                } else {
                    otra = ResenaResumen.de(r);
                }
            }
        }
        return IntercambioDto.de(i, yo.getId(), mia, otra);
    }
}
