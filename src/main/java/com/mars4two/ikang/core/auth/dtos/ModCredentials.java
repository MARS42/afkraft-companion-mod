package com.mars4two.ikang.core.auth.dtos;

import java.util.UUID;

public record ModCredentials(Integer refreshExpiresIn, String refreshToken, String tokenType, String scope, UUID serverId, UUID adminProfileId) {
}
