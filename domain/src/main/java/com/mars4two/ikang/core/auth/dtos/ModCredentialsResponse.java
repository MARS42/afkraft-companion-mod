package com.mars4two.ikang.core.auth.dtos;

import java.util.UUID;

public record ModCredentialsResponse(UUID serverId, UUID adminProfileId, TokenData tokenData) {
    public record TokenData(String access_token, Integer expires_in, Integer refresh_expires_in, String refresh_token, String token_type, String scope) {}
}
