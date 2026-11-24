package com.minga.dto;

import com.minga.model.Franja;
import com.minga.model.Modalidad;
import com.minga.model.TipoServicio;

import java.time.LocalDate;

/** Cuerpos JSON que recibe la API. */
public final class Peticiones {

    private Peticiones() {
    }

    public record ServicioRequest(String titulo, String descripcion, TipoServicio tipo,
                                  Integer horasEstimadas, Modalidad modalidad, Franja franja, Long categoriaId) {
    }

    public record SolicitudRequest(String mensaje, LocalDate fechaPropuesta) {
    }

    public record ResenaRequest(Integer puntaje, String comentario) {
    }

    public record PerfilRequest(String nombreCompleto, String barrio, String biografia) {
    }

    public record CategoriaRequest(String nombre, String descripcion) {
    }
}
