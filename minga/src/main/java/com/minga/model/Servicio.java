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
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

/** Oferta ("puedo...") o necesidad ("necesito...") publicada en la Plaza. */
@Entity
@Table(name = "servicios",
        uniqueConstraints = @UniqueConstraint(name = "uk_servicio_usuario_tipo_titulo",
                columnNames = {"usuario_id", "tipo", "titulo_normalizado"}))
public class Servicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String titulo;

    @Column(nullable = false, length = 500)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private TipoServicio tipo;

    @Column(nullable = false)
    private int horasEstimadas;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private Modalidad modalidad;

    /** Mejor horario para el encuentro. */
    @Enumerated(EnumType.STRING)
    @Column(length = 12)
    private Franja franja = Franja.FLEXIBLE;

    /** Titulo sin tildes, mayusculas ni signos: sirve para detectar publicaciones repetidas. */
    @Column(length = 80)
    private String tituloNormalizado;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaPublicacion = LocalDateTime.now();

    @ManyToOne(optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    public Servicio() {
    }

    public Long getId() { return id; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public TipoServicio getTipo() { return tipo; }
    public void setTipo(TipoServicio tipo) { this.tipo = tipo; }
    public int getHorasEstimadas() { return horasEstimadas; }
    public void setHorasEstimadas(int horasEstimadas) { this.horasEstimadas = horasEstimadas; }
    public Modalidad getModalidad() { return modalidad; }
    public void setModalidad(Modalidad modalidad) { this.modalidad = modalidad; }
    public Franja getFranja() { return franja == null ? Franja.FLEXIBLE : franja; }
    public void setFranja(Franja franja) { this.franja = franja; }
    public String getTituloNormalizado() { return tituloNormalizado; }
    public void setTituloNormalizado(String tituloNormalizado) { this.tituloNormalizado = tituloNormalizado; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public LocalDateTime getFechaPublicacion() { return fechaPublicacion; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public Categoria getCategoria() { return categoria; }
    public void setCategoria(Categoria categoria) { this.categoria = categoria; }
}
