package com.br.psyke.psyke.dto;

import java.util.List;

public record UserResponse(
    String id, String fullName, String email,
    String cpf, String phone, boolean active,
    boolean emailVerified, List<String> roles
) {}
