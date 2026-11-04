package com.mars4two.ikang.client.core.auth.dtos;

import java.util.UUID;

public record ConnectRequest(String serverId, UUID profileId, String joinOtp) {
}
