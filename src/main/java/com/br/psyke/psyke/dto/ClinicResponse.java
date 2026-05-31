package com.br.psyke.psyke.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

@Schema(description = "Clinic response")
public record ClinicResponse(
    String id,
    String name,
    String legalName,
    String cnpj,
    String crp,
    String address,
    String phone,
    String logoUrl,
    String settings,
    boolean active,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
