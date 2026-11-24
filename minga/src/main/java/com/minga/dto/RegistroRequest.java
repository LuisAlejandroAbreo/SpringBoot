package com.minga.dto;

public record RegistroRequest(String username,
                              String nombreCompleto,
                              String email,
                              String barrio,
                              String password,
                              String confirmarPassword) {
}
