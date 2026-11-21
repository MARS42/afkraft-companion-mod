package com.mars4two.ikang.core.preferences.dtos;

import java.util.UUID;

public record ToggleSnapshotPreferenceRequest(
        UUID serverId,
        UUID adminProfileId,
        String snapshotName,
        Boolean enabled
) {
}
