package com.mars4two.ikang.client.core.auth.dtos;

public record ModCredentialsResponse(String access_token, Integer expires_in, Integer refresh_expires_in, String refresh_token, String token_type, String scope) {
}
