package com.mars4two.ikang.client.core.auth.dtos;

public record ModCredentials(Integer refreshExpiresIn, String refreshToken, String tokenType, String scope) {
}
