package com.minga.dto;

import com.minga.model.Categoria;
import com.minga.model.EstadoIntercambio;
import com.minga.model.Franja;
import com.minga.model.Intercambio;
import com.minga.model.Modalidad;
import com.minga.model.MovimientoHoras;
import com.minga.model.Notificacion;
import com.minga.model.Resena;
import com.minga.model.Servicio;
import com.minga.model.TipoMovimiento;
import com.minga.model.TipoNotificacion;
import com.minga.model.TipoServicio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Respuestas JSON de la API (nunca se exponen las entidades: asi no se filtra la contrasena). */
public final class Respuestas {

    private Respuestas() {
    }

    public record MeDto(String username, String nombreCompleto, String barrio, String rol, int saldo) {
    }

    public record CategoriaDto(Long id, String nombre, String descripcion) {
        public static CategoriaDto de(Categoria c) {
            return new CategoriaDto(c.getId(), c.getNombre(), c.getDescripcion());
        }
    }

    public record ServicioDto(Long id, String titulo, String descripcion, TipoServicio tipo, int horasEstimadas,
                              Modalidad modalidad, Franja franja, boolean activo, LocalDateTime fechaPublicacion,
                              Long categoriaId, String categoria,
                              String propietario, String propietarioNombre, String barrio,
                              Double promedioPropietario, long resenasPropietario) {

        public static ServicioDto de(Servicio s) {
            return de(s, null, 0);
        }

        /** Incluye la calificacion de quien publica, para elegir con confianza en la Plaza. */
        public static ServicioDto de(Servicio s, Double promedio, long resenas) {
            return new ServicioDto(s.getId(), s.getTitulo(), s.getDescripcion(), s.getTipo(), s.getHorasEstimadas(),
                    s.getModalidad(), s.getFranja(), s.isActivo(), s.getFechaPublicacion(),
                    s.getCategoria().getId(), s.getCategoria().getNombre(),
                    s.getUsuario().getUsername(), s.getUsuario().getNombreCompleto(), s.getUsuario().getBarrio(),
                    promedio, resenas);
        }
    }

    /** Lo que una persona dijo del trato (para ver la satisfaccion de ambas partes). */
    public record ResenaResumen(int puntaje, String comentario, LocalDateTime fecha, String autorNombre) {
        public static ResenaResumen de(Resena r) {
            return new ResenaResumen(r.getPuntaje(), r.getComentario(), r.getFecha(), r.getAutor().getNombreCompleto());
        }
    }

    public record IntercambioDto(Long id, EstadoIntercambio estado, int horas,
                                 Long servicioId, String servicioTitulo, Franja franja,
                                 String miRol, String otraPersona, String otraPersonaNombre,
                                 boolean soyDueno, String mensaje, LocalDate fechaPropuesta,
                                 LocalDateTime fechaSolicitud, LocalDateTime fechaCierre,
                                 boolean confirmaProveedor, boolean confirmaBeneficiario,
                                 boolean yaConfirme, boolean yaReseno,
                                 ResenaResumen resenaMia, ResenaResumen resenaOtra, List<String> acciones) {

        /**
         * Arma la vista del intercambio desde el punto de vista de la persona que lo consulta.
         * resenaMia = la que yo escribi; resenaOtra = la que la otra persona escribio sobre mi.
         */
        public static IntercambioDto de(Intercambio i, Long yoId, ResenaResumen resenaMia, ResenaResumen resenaOtra) {
            boolean soyProveedor = i.getProveedor().getId().equals(yoId);
            var otra = soyProveedor ? i.getBeneficiario() : i.getProveedor();
            boolean soyDueno = i.esDuenoDelServicio(yoId);
            boolean soySolicitante = i.getSolicitante().getId().equals(yoId);
            boolean yaConfirme = soyProveedor ? i.isConfirmaProveedor() : i.isConfirmaBeneficiario();
            boolean yaReseno = resenaMia != null;

            List<String> acciones = new ArrayList<>();
            switch (i.getEstado()) {
                case SOLICITADO -> {
                    if (soyDueno) {
                        acciones.add("aceptar");
                        acciones.add("rechazar");
                    }
                    if (soySolicitante) {
                        acciones.add("cancelar");
                    }
                }
                case ACEPTADO -> {
                    if (!yaConfirme) {
                        acciones.add("confirmar");
                    }
                    acciones.add("cancelar");
                }
                case REALIZADO -> {
                    if (!yaReseno) {
                        acciones.add("resenar");
                    }
                }
                default -> {
                }
            }

            return new IntercambioDto(i.getId(), i.getEstado(), i.getHorasAcordadas(),
                    i.getServicio().getId(), i.getServicio().getTitulo(), i.getServicio().getFranja(),
                    soyProveedor ? "PROVEEDOR" : "BENEFICIARIO", otra.getUsername(), otra.getNombreCompleto(),
                    soyDueno, i.getMensaje(), i.getFechaPropuesta(), i.getFechaSolicitud(), i.getFechaCierre(),
                    i.isConfirmaProveedor(), i.isConfirmaBeneficiario(), yaConfirme, yaReseno,
                    resenaMia, resenaOtra, acciones);
        }
    }

    public record MovimientoDto(Long id, TipoMovimiento tipo, int horas, int saldoResultante,
                                String descripcion, LocalDateTime fecha) {
        public static MovimientoDto de(MovimientoHoras m) {
            return new MovimientoDto(m.getId(), m.getTipo(), m.getHoras(), m.getSaldoResultante(),
                    m.getDescripcion(), m.getFecha());
        }
    }

    /** Resena recibida; comoProveedor indica si la persona reseñada dio la ayuda o la recibio. */
    public record ResenaDto(Long id, int puntaje, String comentario, LocalDateTime fecha,
                            String autor, String autorNombre, String servicioTitulo, boolean comoProveedor) {
        public static ResenaDto de(Resena r) {
            boolean comoProveedor = r.getIntercambio().getProveedor().getId().equals(r.getDestinatario().getId());
            return new ResenaDto(r.getId(), r.getPuntaje(), r.getComentario(), r.getFecha(),
                    r.getAutor().getUsername(), r.getAutor().getNombreCompleto(),
                    r.getIntercambio().getServicio().getTitulo(), comoProveedor);
        }
    }

    public record InsigniaDto(String codigo, String nombre, String descripcion) {
    }

    /** distribucion: cantidad de resenas de 1, 2, 3, 4 y 5 estrellas (en ese orden). */
    public record PerfilDto(String username, String nombreCompleto, String barrio, String biografia,
                            LocalDateTime miembroDesde, Double promedio, int totalResenas,
                            Double promedioComoProveedor, Double promedioComoBeneficiario, List<Long> distribucion,
                            int intercambiosRealizados, List<InsigniaDto> insignias, List<ResenaDto> resenas) {
    }

    public record NotificacionDto(Long id, TipoNotificacion tipo, String mensaje, String enlace,
                                  boolean leida, LocalDateTime fecha) {
        public static NotificacionDto de(Notificacion n) {
            return new NotificacionDto(n.getId(), n.getTipo(), n.getMensaje(), n.getEnlace(), n.isLeida(), n.getFecha());
        }
    }

    public record ConteoDto(String nombre, long total) {
    }

    public record ImpactoDto(long miembros, long serviciosActivos, long intercambiosRealizados,
                             long horasIntercambiadas, List<ConteoDto> categorias, List<ConteoDto> barrios) {
    }
}
