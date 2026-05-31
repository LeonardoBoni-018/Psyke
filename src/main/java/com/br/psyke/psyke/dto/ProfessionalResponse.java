package com.br.psyke.psyke.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Schema(description = "Professional profile response")
public record ProfessionalResponse(
    String id,
    String fullName,
    String crp,
    String specialty,
    String approach,
    String resume,
    BigDecimal sessionValue,
    int sessionDuration,
    boolean acceptsInsurance,
    boolean active,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
