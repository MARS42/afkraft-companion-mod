package com.mars4two.ikang.client.core.auth.dtos;

import java.util.UUID;

public record HasJoinedRequest(String name, String serverId, UUID profileId) {
}
