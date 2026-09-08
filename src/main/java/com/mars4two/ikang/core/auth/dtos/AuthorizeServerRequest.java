package com.mars4two.ikang.core.auth.dtos;

import java.util.UUID;

public record AuthorizeServerRequest(UUID profileId, String serverId, String joinOtp, String serverName) {
}
