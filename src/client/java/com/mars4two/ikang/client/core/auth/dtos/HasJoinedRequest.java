package com.mars4two.ikang.client.core.auth.dtos;

import java.util.UUID;

public record HasJoinedRequest(String username, String serverId, UUID profileId) {
}
