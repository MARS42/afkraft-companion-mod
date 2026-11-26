package com.mars4two.ikang.core.version;

public record UpdateCheckResult(
        UpdateStatus status,
        SemanticVersion currentVersion,
        SemanticVersion latestVersion,
        String downloadUrl,
        String errorMessage
) {
    public static UpdateCheckResult upToDate(SemanticVersion current, String url) {
        return new UpdateCheckResult(UpdateStatus.UP_TO_DATE, current, current, url, null);
    }

    public static UpdateCheckResult outdated(SemanticVersion current, SemanticVersion latest, String url) {
        return new UpdateCheckResult(UpdateStatus.OUTDATED, current, latest, url, null);
    }

    public static UpdateCheckResult ahead(SemanticVersion current, SemanticVersion latest, String url) {
        return new UpdateCheckResult(UpdateStatus.AHEAD, current, latest, url, null);
    }

    public static UpdateCheckResult error(SemanticVersion current, String errorMsg) {
        return new UpdateCheckResult(UpdateStatus.ERROR, current, null, null, errorMsg);
    }

    public boolean isOutdated() {
        return status == UpdateStatus.OUTDATED;
    }
}
