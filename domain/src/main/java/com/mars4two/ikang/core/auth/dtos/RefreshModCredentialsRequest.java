package com.mars4two.ikang.core.auth.dtos;

import java.util.UUID;

public record RefreshModCredentialsRequest(String refreshToken, UUID currentServerId, UUID adminProfileId) {
}
