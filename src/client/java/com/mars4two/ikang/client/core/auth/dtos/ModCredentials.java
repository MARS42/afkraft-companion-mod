package com.mars4two.ikang.client.core.auth.dtos;

public record ModCredentials(String accessToken, Integer expiresIn, Integer refreshExpiresIn, String refreshToken, String tokenType, String scope) {
}
