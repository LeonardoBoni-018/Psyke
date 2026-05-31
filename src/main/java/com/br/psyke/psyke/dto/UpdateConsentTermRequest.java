package com.br.psyke.psyke.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request to update a consent term")
public record UpdateConsentTermRequest(
    @Schema(description = "Term title") String title,
    @Schema(description = "Full term content") String content,
    @Schema(description = "Whether the patient has signed") Boolean signedByPatient,
    @Schema(description = "Whether the professional has signed") Boolean signedByProfessional,
    @Schema(description = "Whether the term is active") Boolean active
) {}
