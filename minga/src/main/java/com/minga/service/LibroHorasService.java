package com.minga.service;

import com.minga.dto.Respuestas.MovimientoDto;
import com.minga.model.Intercambio;
import com.minga.model.MovimientoHoras;
import com.minga.model.TipoMovimiento;
import com.minga.model.Usuario;
import com.minga.repository.MovimientoHorasRepository;
import com.minga.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Libro de horas. Aqui vive la parte mas delicada del proyecto: mover horas de una cuenta
 * a otra de forma atomica (todo o nada) y sin que dos operaciones gasten las mismas horas.
 */
@Service
public class LibroHorasService {

    private final MovimientoHorasRepository movimientos;
    private final UsuarioRepository usuarios;
    private final int horasBienvenida;

    public LibroHorasService(MovimientoHorasRepository movimientos, UsuarioRepository usuarios,
                             @Value("${app.minga.horas-bienvenida:3}") int horasBienvenida) {
        this.movimientos = movimientos;
        this.usuarios = usuarios;
        this.horasBienvenida = horasBienvenida;
    }

    private static long ceroSiNulo(Long valor) {
        return valor == null ? 0L : valor;
    }

    /** Saldo = bienvenida + creditos - debitos (se calcula siempre desde el libro). */
    @Transactional(readOnly = true)
    public int saldo(Long usuarioId) {
        long ingresos = ceroSiNulo(movimientos.sumarHoras(usuarioId, TipoMovimiento.BIENVENIDA))
                + ceroSiNulo(movimientos.sumarHoras(usuarioId, TipoMovimiento.CREDITO));
        long egresos = ceroSiNulo(movimientos.sumarHoras(usuarioId, TipoMovimiento.DEBITO));
        return (int) (ingresos - egresos);
    }

    @Transactional
    public void darBienvenida(Usuario usuario) {
        movimientos.save(new MovimientoHoras(usuario, null, TipoMovimiento.BIENVENIDA,
                horasBienvenida, horasBienvenida, "Bienvenida a la Minga"));
    }

    /**
     * Debita al beneficiario y acredita al proveedor en una sola transaccion.
     * Si algo falla, no se guarda ninguno de los dos movimientos.
     */
    @Transactional
    public void transferir(Intercambio intercambio) {
        Usuario proveedor = intercambio.getProveedor();
        Usuario beneficiario = intercambio.getBeneficiario();

        // Se bloquean las dos cuentas SIEMPRE en el mismo orden para evitar bloqueos cruzados
        Long primero = Math.min(proveedor.getId(), beneficiario.getId());
        Long segundo = Math.max(proveedor.getId(), beneficiario.getId());
        usuarios.buscarParaActualizar(primero);
        usuarios.buscarParaActualizar(segundo);

        int horas = intercambio.getHorasAcordadas();
        int saldoBeneficiario = saldo(beneficiario.getId());
        if (saldoBeneficiario < horas) {
            throw NegocioException.conflicto("No hay horas suficientes para completar el trato: se necesitan "
                    + horas + " y hay " + saldoBeneficiario + ".");
        }
        int saldoProveedor = saldo(proveedor.getId());
        String titulo = intercambio.getServicio().getTitulo();

        movimientos.save(new MovimientoHoras(beneficiario, intercambio, TipoMovimiento.DEBITO, horas,
                saldoBeneficiario - horas,
                recortar("Recibiste ayuda: " + titulo + " (con " + proveedor.getNombreCompleto() + ")")));
        movimientos.save(new MovimientoHoras(proveedor, intercambio, TipoMovimiento.CREDITO, horas,
                saldoProveedor + horas,
                recortar("Ayudaste: " + titulo + " (a " + beneficiario.getNombreCompleto() + ")")));
    }

    @Transactional(readOnly = true)
    public List<MovimientoDto> movimientosDe(Long usuarioId) {
        return movimientos.findByUsuarioIdOrderByIdDesc(usuarioId).stream().map(MovimientoDto::de).toList();
    }

    private static String recortar(String texto) {
        return texto.length() <= 160 ? texto : texto.substring(0, 157) + "...";
    }
}
