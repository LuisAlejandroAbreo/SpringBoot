package com.minga.controller;

import com.minga.service.NegocioException;
import com.minga.service.RegistroException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;

/** Convierte las excepciones en respuestas JSON con mensajes claros. */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(NegocioException.class)
    public ResponseEntity<Map<String, Object>> negocio(NegocioException e) {
        return ResponseEntity.status(e.getStatus()).body(Map.of("mensaje", e.getMessage()));
    }

    @ExceptionHandler(RegistroException.class)
    public ResponseEntity<Map<String, Object>> registro(RegistroException e) {
        return ResponseEntity.status(e.getStatus()).body(Map.of("errores", e.getErrores()));
    }

    // Dos personas tocaron el mismo intercambio a la vez (@Version)
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, Object>> concurrencia(ObjectOptimisticLockingFailureException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("mensaje",
                "Otra persona modificó este intercambio al mismo tiempo. Recarga la página e inténtalo de nuevo."));
    }

    // Respaldo para restricciones UNIQUE de la base de datos
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> integridad(DataIntegrityViolationException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("errores",
                Map.of("username", List.of("Ese usuario o correo ya está registrado."))));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> malFormada(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(Map.of("mensaje", "Los datos enviados no son válidos."));
    }
}
