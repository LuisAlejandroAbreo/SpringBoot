package com.minga.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** Aviso para la campana: solicitudes, aceptaciones, confirmaciones y resenas. */
@Entity
@Table(name = "notificaciones")
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private TipoNotificacion tipo;

    @Column(nullable = false, length = 240)
    private String mensaje;

    /** Pagina a la que lleva el aviso (por ejemplo intercambios.html). */
    @Column(length = 80)
    private String enlace;

    @Column(nullable = false)
    private boolean leida = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fecha = LocalDateTime.now();

    public Notificacion() {
    }

    public Notificacion(Usuario usuario, TipoNotificacion tipo, String mensaje, String enlace) {
        this.usuario = usuario;
        this.tipo = tipo;
        this.mensaje = mensaje.length() > 240 ? mensaje.substring(0, 237) + "..." : mensaje;
        this.enlace = enlace;
    }

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public TipoNotificacion getTipo() { return tipo; }
    public String getMensaje() { return mensaje; }
    public String getEnlace() { return enlace; }
    public boolean isLeida() { return leida; }
    public void setLeida(boolean leida) { this.leida = leida; }
    public LocalDateTime getFecha() { return fecha; }
}
