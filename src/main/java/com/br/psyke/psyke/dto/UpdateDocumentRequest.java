package com.br.psyke.psyke.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request to update document notes")
public record UpdateDocumentRequest(
    @Schema(description = "Document notes") String notes
) {}
