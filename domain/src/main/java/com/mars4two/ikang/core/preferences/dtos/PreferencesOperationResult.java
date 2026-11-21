package com.mars4two.ikang.core.preferences.dtos;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record PreferencesOperationResult(
        Status status,
        Map<String, Boolean> snapshots
) {

    public enum Status {
        SUCCESS,
        NOT_AUTHENTICATED,
        FORBIDDEN,
        RATE_LIMITED,
        NOT_FOUND,
        INVALID_REQUEST,
        SAVE_IN_PROGRESS,
        NETWORK_ERROR
    }

    public PreferencesOperationResult {
        snapshots = snapshots == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(snapshots));
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    public static PreferencesOperationResult success(Map<String, Boolean> snapshots) {
        return new PreferencesOperationResult(Status.SUCCESS, snapshots);
    }

    public static PreferencesOperationResult failure(Status status, Map<String, Boolean> fallbackSnapshots) {
        return new PreferencesOperationResult(status, fallbackSnapshots);
    }
}
