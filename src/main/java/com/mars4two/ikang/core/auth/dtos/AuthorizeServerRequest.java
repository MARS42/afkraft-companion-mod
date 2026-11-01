package com.mars4two.ikang.core.auth.dtos;

import java.util.UUID;

public record AuthorizeServerRequest(UUID profileId, String targetServerId, String challengeServerId, String joinOtp) {
}
