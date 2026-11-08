package com.mars4two.ikang.client.core.auth.dtos;

import java.util.UUID;

public record AuthorizeServerRequest(UUID profileId, String targetServerId, String challengeServerId, String joinOtp, String serverName) {
}
