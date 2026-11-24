package com.minga.service;

import com.minga.dto.Peticiones.ServicioRequest;
import com.minga.dto.Respuestas.ServicioDto;
import com.minga.model.Categoria;
import com.minga.model.Franja;
import com.minga.model.Servicio;
import com.minga.model.TipoServicio;
import com.minga.model.Usuario;
import com.minga.repository.CategoriaRepository;
import com.minga.repository.ResenaRepository;
import com.minga.repository.ServicioRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ServicioService {

    private final ServicioRepository servicios;
    private final CategoriaRepository categorias;
    private final ResenaRepository resenas;
    private final UsuarioService usuarios;

    public ServicioService(ServicioRepository servicios, CategoriaRepository categorias,
                           ResenaRepository resenas, UsuarioService usuarios) {
        this.servicios = servicios;
        this.categorias = categorias;
        this.resenas = resenas;
        this.usuarios = usuarios;
    }

    /** La Plaza: servicios activos, con filtros opcionales (cualquiera puede venir nulo o vacio). */
    @Transactional(readOnly = true)
    public List<ServicioDto> plaza(TipoServicio tipo, Long categoriaId, String barrio, String texto, Franja franja) {
        String b = normalizar(barrio);
        String q = normalizar(texto);
        Map<Long, double[]> notas = calificaciones();
        return servicios.findByActivoTrueOrderByFechaPublicacionDesc().stream()
                .filter(s -> tipo == null || s.getTipo() == tipo)
                .filter(s -> categoriaId == null || s.getCategoria().getId().equals(categoriaId))
                // Quien busca "tarde" tambien ve lo que sirve en cualquier horario
                .filter(s -> franja == null || franja == Franja.FLEXIBLE
                        || s.getFranja() == franja || s.getFranja() == Franja.FLEXIBLE)
                .filter(s -> b.isEmpty() || normalizar(s.getUsuario().getBarrio()).contains(b))
                .filter(s -> q.isEmpty() || normalizar(s.getTitulo()).contains(q)
                        || normalizar(s.getDescripcion()).contains(q))
                .map(s -> conNota(s, notas))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ServicioDto> mios(String username) {
        Usuario u = usuarios.buscar(username);
        Map<Long, double[]> notas = calificaciones();
        return servicios.findByUsuarioIdOrderByFechaPublicacionDesc(u.getId()).stream()
                .map(s -> conNota(s, notas)).toList();
    }

    @Transactional
    public ServicioDto crear(String username, ServicioRequest req) {
        Usuario dueno = usuarios.buscar(username);
        Categoria categoria = validar(req);
        String normalizado = normalizarTitulo(req.titulo());

        if (servicios.existsByUsuarioIdAndTipoAndTituloNormalizado(dueno.getId(), req.tipo(), normalizado)) {
            throw repetida(req.tipo());
        }
        Servicio s = new Servicio();
        s.setUsuario(dueno);
        aplicar(s, req, categoria);
        try {
            return ServicioDto.de(servicios.save(s));
        } catch (DataIntegrityViolationException e) {
            throw repetida(req.tipo());   // respaldo: la base de datos tambien lo impide
        }
    }

    @Transactional
    public ServicioDto actualizar(String username, Long id, ServicioRequest req) {
        Servicio s = propio(username, id);
        Categoria categoria = validar(req);
        String normalizado = normalizarTitulo(req.titulo());

        if (servicios.existsByUsuarioIdAndTipoAndTituloNormalizadoAndIdNot(
                s.getUsuario().getId(), req.tipo(), normalizado, id)) {
            throw repetida(req.tipo());
        }
        aplicar(s, req, categoria);
        try {
            return ServicioDto.de(servicios.save(s));
        } catch (DataIntegrityViolationException e) {
            throw repetida(req.tipo());
        }
    }

    @Transactional
    public ServicioDto cambiarActivo(String username, Long id, boolean activo) {
        Servicio s = propio(username, id);
        s.setActivo(activo);
        return ServicioDto.de(servicios.save(s));
    }

    // ------------------------------------------------------------------ ayudas

    /** Quita tildes, signos y mayusculas: "¡Clases de Guitarra!" y "clases de guitarra" son lo mismo. */
    public static String normalizarTitulo(String titulo) {
        if (titulo == null) {
            return "";
        }
        String sinTildes = Normalizer.normalize(titulo, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return sinTildes.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }

    private static NegocioException repetida(TipoServicio tipo) {
        String que = tipo == TipoServicio.NECESIDAD ? "una necesidad" : "una oferta";
        return NegocioException.conflicto("Ya tienes " + que + " con ese título. "
                + "Edítala o reanúdala en lugar de publicarla otra vez.");
    }

    private ServicioDto conNota(Servicio s, Map<Long, double[]> notas) {
        double[] nota = notas.get(s.getUsuario().getId());
        return nota == null ? ServicioDto.de(s)
                : ServicioDto.de(s, Math.round(nota[0] * 10.0) / 10.0, (long) nota[1]);
    }

    /** Promedio y cantidad de reseñas de cada usuario, en una sola consulta. */
    private Map<Long, double[]> calificaciones() {
        Map<Long, double[]> mapa = new HashMap<>();
        for (Object[] fila : resenas.resumenPorUsuario()) {
            mapa.put(((Number) fila[0]).longValue(),
                    new double[]{((Number) fila[1]).doubleValue(), ((Number) fila[2]).doubleValue()});
        }
        return mapa;
    }

    private Servicio propio(String username, Long id) {
        Usuario u = usuarios.buscar(username);
        Servicio s = servicios.findById(id)
                .orElseThrow(() -> NegocioException.noEncontrado("Ese servicio no existe."));
        if (!s.getUsuario().getId().equals(u.getId())) {
            throw NegocioException.prohibido("Solo puedes modificar tus propias publicaciones.");
        }
        return s;
    }

    private Categoria validar(ServicioRequest r) {
        String titulo = r.titulo() == null ? "" : r.titulo().trim();
        String descripcion = r.descripcion() == null ? "" : r.descripcion().trim();
        if (titulo.length() < 5 || titulo.length() > 80) {
            throw NegocioException.invalido("El título debe tener entre 5 y 80 caracteres.");
        }
        if (descripcion.length() < 10 || descripcion.length() > 500) {
            throw NegocioException.invalido("La descripción debe tener entre 10 y 500 caracteres.");
        }
        if (r.tipo() == null) {
            throw NegocioException.invalido("Indica si es una oferta o una necesidad.");
        }
        if (r.modalidad() == null) {
            throw NegocioException.invalido("Indica si es presencial o virtual.");
        }
        if (r.franja() == null) {
            throw NegocioException.invalido("Elige el mejor horario: mañana, tarde, noche o cualquiera.");
        }
        if (r.horasEstimadas() == null || r.horasEstimadas() < 1 || r.horasEstimadas() > 8) {
            throw NegocioException.invalido("Las horas estimadas deben estar entre 1 y 8.");
        }
        if (r.categoriaId() == null) {
            throw NegocioException.invalido("Elige una categoría.");
        }
        Categoria c = categorias.findById(r.categoriaId())
                .orElseThrow(() -> NegocioException.invalido("Esa categoría no existe."));
        if (!c.isActiva()) {
            throw NegocioException.invalido("Esa categoría ya no está disponible.");
        }
        return c;
    }

    private void aplicar(Servicio s, ServicioRequest r, Categoria categoria) {
        s.setTitulo(r.titulo().trim());
        s.setTituloNormalizado(normalizarTitulo(r.titulo()));
        s.setDescripcion(r.descripcion().trim());
        s.setTipo(r.tipo());
        s.setModalidad(r.modalidad());
        s.setFranja(r.franja());
        s.setHorasEstimadas(r.horasEstimadas());
        s.setCategoria(categoria);
    }

    private static String normalizar(String texto) {
        return texto == null ? "" : texto.trim().toLowerCase(Locale.ROOT);
    }
}
