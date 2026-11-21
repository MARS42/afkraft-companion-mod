package com.mars4two.ikang.core.preferences.dtos;

import java.util.Map;
import java.util.UUID;

/**
 * Response payload from {@code /core/mod-worker/preferences}.
 * {@code updatedAt} is stored as an ISO-8601 string to remain compatible with plain Gson.
 */
public record WorldPreferencesResponse(
        UUID worldId,
        UUID serverId,
        boolean editable,
        Map<String, Boolean> snapshots,
        String updatedAt
) {
}
