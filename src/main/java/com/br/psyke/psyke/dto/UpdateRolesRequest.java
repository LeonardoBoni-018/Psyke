package com.br.psyke.psyke.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@Schema(description = "Request to update user roles")
public record UpdateRolesRequest(
    @NotEmpty @Schema(description = "List of role names to assign", example = "[\"ROLE_PROFESSIONAL\", \"ROLE_RECEPTIONIST\"]") List<String> roles
) {}
