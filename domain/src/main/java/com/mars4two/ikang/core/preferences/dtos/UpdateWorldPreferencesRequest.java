package com.mars4two.ikang.core.preferences.dtos;

import java.util.Map;
import java.util.UUID;

public record UpdateWorldPreferencesRequest(
        UUID serverId,
        UUID adminProfileId,
        Map<String, Boolean> snapshots
) {
}
