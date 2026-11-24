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
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

/**
 * Libro de horas: cada fila es un credito, un debito o la bienvenida.
 * Es de SOLO INSERCION: no tiene setters, es @Immutable y su repositorio no expone borrar ni editar.
 */
@Entity
@Immutable
@Table(name = "movimientos_horas")
public class MovimientoHoras {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 12)
    private TipoMovimiento tipo;

    @Column(nullable = false, updatable = false)
    private int horas;

    /** Saldo del usuario justo despues de este movimiento (sirve para auditar). */
    @Column(nullable = false, updatable = false)
    private int saldoResultante;

    @Column(nullable = false, updatable = false, length = 160)
    private String descripcion;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fecha = LocalDateTime.now();

    @ManyToOne(optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, updatable = false)
    private Usuario usuario;

    /** Nulo solo en la bienvenida. */
    @ManyToOne
    @JoinColumn(name = "intercambio_id", updatable = false)
    private Intercambio intercambio;

    protected MovimientoHoras() {
    }

    public MovimientoHoras(Usuario usuario, Intercambio intercambio, TipoMovimiento tipo,
                           int horas, int saldoResultante, String descripcion) {
        this.usuario = usuario;
        this.intercambio = intercambio;
        this.tipo = tipo;
        this.horas = horas;
        this.saldoResultante = saldoResultante;
        this.descripcion = descripcion;
    }

    public Long getId() { return id; }
    public TipoMovimiento getTipo() { return tipo; }
    public int getHoras() { return horas; }
    public int getSaldoResultante() { return saldoResultante; }
    public String getDescripcion() { return descripcion; }
    public LocalDateTime getFecha() { return fecha; }
    public Usuario getUsuario() { return usuario; }
    public Intercambio getIntercambio() { return intercambio; }
}
