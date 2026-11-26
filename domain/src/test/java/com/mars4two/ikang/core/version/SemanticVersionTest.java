package com.mars4two.ikang.core.version;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SemanticVersionTest {

    @Test
    void parsesStandardVersions() {
        SemanticVersion v = SemanticVersion.parse("1.2.3");
        assertEquals(1, v.major());
        assertEquals(2, v.minor());
        assertEquals(3, v.patch());
        assertFalse(v.isPreRelease());
    }

    @Test
    void parsesVersionWithVPrefix() {
        SemanticVersion v = SemanticVersion.parse("v2.1.0");
        assertEquals(2, v.major());
        assertEquals(1, v.minor());
        assertEquals(0, v.patch());
    }

    @Test
    void parsesVersionWithBuildMetadataAndMinecraftTag() {
        SemanticVersion v = SemanticVersion.parse("1.0.0+26.3");
        assertEquals(1, v.major());
        assertEquals(0, v.minor());
        assertEquals(0, v.patch());
        assertEquals("26.3", v.buildMetadata());
        assertFalse(v.isPreRelease());

        // Build metadata should be ignored in comparison
        SemanticVersion vClean = SemanticVersion.parse("1.0.0");
        assertEquals(0, v.compareTo(vClean));
        assertEquals(vClean, v);
    }

    @Test
    void parsesPreReleaseVersions() {
        SemanticVersion v = SemanticVersion.parse("1.0.0-beta.1");
        assertEquals(1, v.major());
        assertEquals(0, v.minor());
        assertEquals(0, v.patch());
        assertTrue(v.isPreRelease());
        assertEquals("beta.1", v.preRelease());
    }

    @Test
    void normalVersionHasHigherPrecedenceThanPreRelease() {
        SemanticVersion normal = SemanticVersion.parse("1.0.0");
        SemanticVersion beta = SemanticVersion.parse("1.0.0-beta.2");

        assertTrue(normal.compareTo(beta) > 0);
        assertTrue(beta.compareTo(normal) < 0);
    }

    @Test
    void parsesPreReleaseWithMinecraftBuildMetadata() {
        SemanticVersion betaWithMc = SemanticVersion.parse("1.0.0-beta.1+26.3");
        SemanticVersion betaClean = SemanticVersion.parse("1.0.0-beta.1");
        SemanticVersion stable = SemanticVersion.parse("1.0.0");

        assertEquals(1, betaWithMc.major());
        assertEquals(0, betaWithMc.minor());
        assertEquals(0, betaWithMc.patch());
        assertTrue(betaWithMc.isPreRelease());
        assertEquals("beta.1", betaWithMc.preRelease());
        assertEquals("26.3", betaWithMc.buildMetadata());
        assertEquals("1.0.0-beta.1", betaWithMc.toCleanString());
        assertEquals(0, betaWithMc.compareTo(betaClean));
        assertEquals(betaClean, betaWithMc);
        assertTrue(betaWithMc.compareTo(stable) < 0);
    }

    @Test
    void comparesVersionsCorrectly() {
        assertTrue(SemanticVersion.parse("1.0.1").compareTo(SemanticVersion.parse("1.0.0")) > 0);
        assertTrue(SemanticVersion.parse("1.1.0").compareTo(SemanticVersion.parse("1.0.9")) > 0);
        assertTrue(SemanticVersion.parse("2.0.0").compareTo(SemanticVersion.parse("1.9.9")) > 0);
        assertTrue(SemanticVersion.parse("1.0.0").compareTo(SemanticVersion.parse("1.0.1")) < 0);
        assertEquals(0, SemanticVersion.parse("1.0.0").compareTo(SemanticVersion.parse("1.0.0")));
    }

    @Test
    void parsesShortVersions() {
        SemanticVersion v = SemanticVersion.parse("1.2");
        assertEquals(1, v.major());
        assertEquals(2, v.minor());
        assertEquals(0, v.patch());
    }
}
