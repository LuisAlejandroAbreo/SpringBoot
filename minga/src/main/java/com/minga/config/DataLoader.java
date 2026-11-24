package com.minga.config;

import com.minga.dto.Peticiones.ServicioRequest;
import com.minga.dto.Peticiones.SolicitudRequest;
import com.minga.dto.RegistroRequest;
import com.minga.dto.Respuestas.IntercambioDto;
import com.minga.dto.Respuestas.ServicioDto;
import com.minga.model.Categoria;
import com.minga.model.Franja;
import com.minga.model.Modalidad;
import com.minga.model.TipoServicio;
import com.minga.repository.CategoriaRepository;
import com.minga.repository.UsuarioRepository;
import com.minga.service.IntercambioService;
import com.minga.service.ResenaService;
import com.minga.service.ServicioService;
import com.minga.service.UsuarioService;
import com.minga.dto.Peticiones.ResenaRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Carga datos iniciales: categorias (siempre que falten) y, si app.demo-data=true,
 * usuarios, servicios y un intercambio de ejemplo para que la demo nunca arranque vacia.
 */
@Component
public class DataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);
    private static final String CLAVE_DEMO = "Minga#2026";

    private final CategoriaRepository categorias;
    private final UsuarioRepository usuariosRepo;
    private final UsuarioService usuarios;
    private final ServicioService servicios;
    private final IntercambioService intercambios;
    private final ResenaService resenas;
    private final boolean demo;

    public DataLoader(CategoriaRepository categorias, UsuarioRepository usuariosRepo, UsuarioService usuarios,
                      ServicioService servicios, IntercambioService intercambios, ResenaService resenas,
                      @Value("${app.demo-data:false}") boolean demo) {
        this.categorias = categorias;
        this.usuariosRepo = usuariosRepo;
        this.usuarios = usuarios;
        this.servicios = servicios;
        this.intercambios = intercambios;
        this.resenas = resenas;
        this.demo = demo;
    }

    @Override
    public void run(String... args) {
        if (categorias.count() == 0) {
            categorias.saveAll(List.of(
                    new Categoria("Hogar", "Arreglos, jardinería, mudanzas y mantenimiento"),
                    new Categoria("Clases", "Idiomas, música, tareas y refuerzo escolar"),
                    new Categoria("Tecnología", "Computadores, celulares, redes y software"),
                    new Categoria("Cocina", "Recetas, repostería y comida para el día a día"),
                    new Categoria("Cuidado", "Compañía, mascotas y cuidado de niños o mayores"),
                    new Categoria("Trámites", "Ayuda con documentos, formularios y papeleo"),
                    new Categoria("Arte y oficios", "Manualidades, costura, pintura y carpintería")));
        }
        if (demo && usuariosRepo.count() == 0) {
            cargarDemo();
        }
    }

    private void cargarDemo() {
        registrar("admin", "Administración Minga", "Cabecera");
        usuariosRepo.findByUsername("admin").ifPresent(u -> {
            u.setRol("ADMIN");
            usuariosRepo.save(u);
        });
        registrar("ana", "Ana María Rojas", "Cabecera");
        registrar("luis", "Luis Fernando Pérez", "Centro");
        registrar("marta", "Marta Quintero", "Cabecera");

        Long hogar = idCategoria("Hogar");
        Long clases = idCategoria("Clases");
        Long tecno = idCategoria("Tecnología");
        Long cocina = idCategoria("Cocina");

        ServicioDto guitarra = servicios.crear("ana", new ServicioRequest("Clases de guitarra para principiantes",
                "Te enseño los primeros acordes y tu primera canción en una hora tranquila.",
                TipoServicio.OFERTA, 2, Modalidad.PRESENCIAL, Franja.TARDE, clases));
        servicios.crear("luis", new ServicioRequest("Arreglo de llaves y goteras",
                "Cambio empaques, sifones y arreglo goteras pequeñas en casa.",
                TipoServicio.OFERTA, 1, Modalidad.PRESENCIAL, Franja.MANANA, hogar));
        servicios.crear("marta", new ServicioRequest("Ayuda con Excel y hojas de cálculo",
                "Fórmulas, tablas dinámicas y orden de datos, por videollamada.",
                TipoServicio.OFERTA, 2, Modalidad.VIRTUAL, Franja.NOCHE, tecno));
        servicios.crear("ana", new ServicioRequest("Necesito ayuda para instalar una impresora",
                "Mi impresora no se conecta al wifi del apartamento. ¿Alguien me echa una mano?",
                TipoServicio.NECESIDAD, 1, Modalidad.PRESENCIAL, Franja.FLEXIBLE, tecno));
        servicios.crear("luis", new ServicioRequest("Busco quien me enseñe a hacer arepas",
                "Quiero aprender la masa y el punto de la plancha. Llevo yo los ingredientes.",
                TipoServicio.NECESIDAD, 2, Modalidad.PRESENCIAL, Franja.TARDE, cocina));

        // Un intercambio ya realizado (Luis toma las clases de Ana) para que el libro y el panel tengan datos
        IntercambioDto trato = intercambios.solicitar("luis", guitarra.id(),
                new SolicitudRequest("Quiero aprender para tocarle una canción a mi mamá.", LocalDate.now().plusDays(1)));
        intercambios.aceptar("ana", trato.id());
        intercambios.confirmar("ana", trato.id());
        intercambios.confirmar("luis", trato.id());
        resenas.crear("luis", trato.id(), new ResenaRequest(5, "Muy paciente y clara. ¡Ya me sé una canción!"));
        resenas.crear("ana", trato.id(), new ResenaRequest(5, "Luis llegó puntual y con muchas ganas."));

        log.info("Datos de ejemplo cargados. Usuarios: admin, ana, luis, marta (clave: {})", CLAVE_DEMO);
    }

    private void registrar(String username, String nombre, String barrio) {
        usuarios.registrar(new RegistroRequest(username, nombre, username + "@minga.test", barrio,
                CLAVE_DEMO, CLAVE_DEMO));
    }

    private Long idCategoria(String nombre) {
        return categorias.findAll().stream()
                .filter(c -> c.getNombre().equals(nombre)).findFirst().orElseThrow().getId();
    }
}
