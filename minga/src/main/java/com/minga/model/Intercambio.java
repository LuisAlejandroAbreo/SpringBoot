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
import jakarta.persistence.Version;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Trato entre dos usuarios sobre un servicio. Guarda quien da la ayuda (proveedor)
 * y quien la recibe (beneficiario); las horas viajan del segundo al primero.
 */
@Entity
@Table(name = "intercambios")
public class Intercambio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private EstadoIntercambio estado = EstadoIntercambio.SOLICITADO;

    @Column(nullable = false)
    private int horasAcordadas;

    @Column(length = 300)
    private String mensaje;

    private LocalDate fechaPropuesta;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaSolicitud = LocalDateTime.now();

    private LocalDateTime fechaCierre;

    @Column(nullable = false)
    private boolean confirmaProveedor = false;

    @Column(nullable = false)
    private boolean confirmaBeneficiario = false;

    /** Control de concurrencia: evita que dos confirmaciones simultaneas transfieran dos veces. */
    @Version
    private Long version;

    @ManyToOne(optional = false)
    @JoinColumn(name = "servicio_id", nullable = false)
    private Servicio servicio;

    /** Quien alzo la mano (el otro participante es el dueno del servicio). */
    @ManyToOne(optional = false)
    @JoinColumn(name = "solicitante_id", nullable = false)
    private Usuario solicitante;

    @ManyToOne(optional = false)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private Usuario proveedor;

    @ManyToOne(optional = false)
    @JoinColumn(name = "beneficiario_id", nullable = false)
    private Usuario beneficiario;

    public Intercambio() {
    }

    public boolean esParticipante(Long usuarioId) {
        return proveedor.getId().equals(usuarioId) || beneficiario.getId().equals(usuarioId);
    }

    public boolean esDuenoDelServicio(Long usuarioId) {
        return servicio.getUsuario().getId().equals(usuarioId);
    }

    public Long getId() { return id; }
    public EstadoIntercambio getEstado() { return estado; }
    public void setEstado(EstadoIntercambio estado) { this.estado = estado; }
    public int getHorasAcordadas() { return horasAcordadas; }
    public void setHorasAcordadas(int horasAcordadas) { this.horasAcordadas = horasAcordadas; }
    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
    public LocalDate getFechaPropuesta() { return fechaPropuesta; }
    public void setFechaPropuesta(LocalDate fechaPropuesta) { this.fechaPropuesta = fechaPropuesta; }
    public LocalDateTime getFechaSolicitud() { return fechaSolicitud; }
    public LocalDateTime getFechaCierre() { return fechaCierre; }
    public void setFechaCierre(LocalDateTime fechaCierre) { this.fechaCierre = fechaCierre; }
    public boolean isConfirmaProveedor() { return confirmaProveedor; }
    public void setConfirmaProveedor(boolean confirmaProveedor) { this.confirmaProveedor = confirmaProveedor; }
    public boolean isConfirmaBeneficiario() { return confirmaBeneficiario; }
    public void setConfirmaBeneficiario(boolean confirmaBeneficiario) { this.confirmaBeneficiario = confirmaBeneficiario; }
    public Long getVersion() { return version; }
    public Servicio getServicio() { return servicio; }
    public void setServicio(Servicio servicio) { this.servicio = servicio; }
    public Usuario getSolicitante() { return solicitante; }
    public void setSolicitante(Usuario solicitante) { this.solicitante = solicitante; }
    public Usuario getProveedor() { return proveedor; }
    public void setProveedor(Usuario proveedor) { this.proveedor = proveedor; }
    public Usuario getBeneficiario() { return beneficiario; }
    public void setBeneficiario(Usuario beneficiario) { this.beneficiario = beneficiario; }
}
