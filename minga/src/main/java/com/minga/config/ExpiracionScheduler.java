package com.minga.config;

import com.minga.service.IntercambioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Cada hora marca como EXPIRADO lo que lleva demasiados dias sin respuesta. */
@Component
public class ExpiracionScheduler {

    private static final Logger log = LoggerFactory.getLogger(ExpiracionScheduler.class);

    private final IntercambioService intercambios;

    public ExpiracionScheduler(IntercambioService intercambios) {
        this.intercambios = intercambios;
    }

    @Scheduled(cron = "0 0 * * * *")
    public void expirar() {
        int total = intercambios.expirarVencidos();
        if (total > 0) {
            log.info("Solicitudes expiradas: {}", total);
        }
    }
}
