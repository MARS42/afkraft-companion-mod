package com.mars4two.ikang.core.version;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateCheckerServiceTest {

    private static final String SAMPLE_FORGE_JSON = """
            {
              "homepage": "https://github.com/MARS42/afkraft-companion-mod",
              "promos": {
                "1.21.1-latest": "1.0.1",
                "1.21.1-recommended": "1.0.0",
                "26.3-latest": "1.1.0",
                "26.3-recommended": "1.0.0"
              },
              "1.21.1": {
                "1.0.0": "Initial release",
                "1.0.1": "Bug fixes"
              },
              "26.3": {
                "1.0.0": "Initial release",
                "1.1.0": "Feature release"
              }
            }
            """;

    @Test
    void detectsOutdatedVersion() {
        UpdateCheckerService service = new UpdateCheckerService();
        SemanticVersion current = SemanticVersion.parse("1.0.0");

        UpdateCheckResult result = service.parseAndCompare(SAMPLE_FORGE_JSON, current, "26.3");

        assertEquals(UpdateStatus.OUTDATED, result.status());
        assertTrue(result.isOutdated());
        assertEquals("1.1.0", result.latestVersion().toCleanString());
        assertEquals("https://github.com/MARS42/afkraft-companion-mod", result.downloadUrl());
    }

    @Test
    void detectsUpToDateVersion() {
        UpdateCheckerService service = new UpdateCheckerService();
        SemanticVersion current = SemanticVersion.parse("1.1.0");

        UpdateCheckResult result = service.parseAndCompare(SAMPLE_FORGE_JSON, current, "26.3");

        assertEquals(UpdateStatus.UP_TO_DATE, result.status());
        assertFalse(result.isOutdated());
    }

    @Test
    void detectsAheadVersion() {
        UpdateCheckerService service = new UpdateCheckerService();
        SemanticVersion current = SemanticVersion.parse("2.0.0");

        UpdateCheckResult result = service.parseAndCompare(SAMPLE_FORGE_JSON, current, "26.3");

        assertEquals(UpdateStatus.AHEAD, result.status());
        assertFalse(result.isOutdated());
    }

    @Test
    void resolvesClusterFallbackWhenExactPatchNotListed() {
        UpdateCheckerService service = new UpdateCheckerService();
        SemanticVersion current = SemanticVersion.parse("1.0.0");

        // Player is on 1.21 or 1.21.0, promos has 1.21.1-latest
        UpdateCheckResult result = service.parseAndCompare(SAMPLE_FORGE_JSON, current, "1.21");

        assertEquals(UpdateStatus.OUTDATED, result.status());
        assertEquals("1.0.1", result.latestVersion().toCleanString());
    }

    @Test
    void handlesClosedBetaUpToDateAndUpgradeToStable() {
        UpdateCheckerService service = new UpdateCheckerService();
        String betaManifest = """
                {
                  "homepage": "https://github.com/MARS42/afkraft-companion-mod/releases",
                  "promos": {
                    "26.3-latest": "1.0.0-beta.1"
                  }
                }
                """;
        String stableManifest = """
                {
                  "homepage": "https://github.com/MARS42/afkraft-companion-mod/releases",
                  "promos": {
                    "26.3-latest": "1.0.0",
                    "26.3-recommended": "1.0.0"
                  }
                }
                """;

        SemanticVersion installedBeta = SemanticVersion.parse("1.0.0-beta.1+26.3");

        UpdateCheckResult betaResult = service.parseAndCompare(betaManifest, installedBeta, "26.3");
        assertEquals(UpdateStatus.UP_TO_DATE, betaResult.status());
        assertFalse(betaResult.isOutdated());

        UpdateCheckResult stableResult = service.parseAndCompare(stableManifest, installedBeta, "26.3");
        assertEquals(UpdateStatus.OUTDATED, stableResult.status());
        assertTrue(stableResult.isOutdated());
        assertEquals("1.0.0", stableResult.latestVersion().toCleanString());
    }
}
