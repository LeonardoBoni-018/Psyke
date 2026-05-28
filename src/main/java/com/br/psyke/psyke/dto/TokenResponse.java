package com.br.psyke.psyke.dto;

import java.util.List;

public record TokenResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    long expiresIn,
    String userId,
    String name,
    List<String> roles
) {
    public TokenResponse {
        if (tokenType == null) tokenType = "Bearer";
    }
}
