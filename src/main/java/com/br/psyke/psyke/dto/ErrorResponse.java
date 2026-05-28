package com.br.psyke.psyke.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record ErrorResponse(int status, String error, String message, String path, OffsetDateTime timestamp, List<String> details) {}
