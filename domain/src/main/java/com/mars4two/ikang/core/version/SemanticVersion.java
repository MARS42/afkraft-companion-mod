package com.mars4two.ikang.core.version;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Immutable Semantic Version representation (SemVer 2.0.0 compliant for ordering).
 * Build metadata (+build) is preserved for display but ignored during comparison.
 */
public record SemanticVersion(
        int major,
        int minor,
        int patch,
        String preRelease,
        String buildMetadata
) implements Comparable<SemanticVersion> {

    private static final Pattern SEMVER_PATTERN = Pattern.compile(
            "^[vV]?(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?(?:-([0-9A-Za-z.-]+))?(?:\\+([0-9A-Za-z.-]+))?$"
    );

    public SemanticVersion {
        if (major < 0 || minor < 0 || patch < 0) {
            throw new IllegalArgumentException("Version numbers must be non-negative");
        }
    }

    public static SemanticVersion of(int major, int minor, int patch) {
        return new SemanticVersion(major, minor, patch, null, null);
    }

    public static SemanticVersion of(int major, int minor, int patch, String preRelease) {
        return new SemanticVersion(major, minor, patch, preRelease, null);
    }

    public static SemanticVersion parse(String versionStr) {
        if (versionStr == null || versionStr.isBlank()) {
            return new SemanticVersion(0, 0, 0, null, null);
        }

        String trimmed = versionStr.trim();
        Matcher matcher = SEMVER_PATTERN.matcher(trimmed);
        if (!matcher.matches()) {
            // Fallback: extract any digits available
            return parseFallback(trimmed);
        }

        int major = Integer.parseInt(matcher.group(1));
        int minor = matcher.group(2) != null ? Integer.parseInt(matcher.group(2)) : 0;
        int patch = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 0;
        String preRelease = matcher.group(4);
        String buildMeta = matcher.group(5);

        return new SemanticVersion(major, minor, patch, preRelease, buildMeta);
    }

    private static SemanticVersion parseFallback(String raw) {
        Pattern digitsPattern = Pattern.compile("(\\d+)");
        Matcher m = digitsPattern.matcher(raw);
        int major = m.find() ? Integer.parseInt(m.group(1)) : 0;
        int minor = m.find() ? Integer.parseInt(m.group(1)) : 0;
        int patch = m.find() ? Integer.parseInt(m.group(1)) : 0;
        return new SemanticVersion(major, minor, patch, "unknown", null);
    }

    public boolean isPreRelease() {
        return preRelease != null && !preRelease.isBlank();
    }

    @Override
    public int compareTo(SemanticVersion other) {
        if (other == null) {
            return 1;
        }

        if (this.major != other.major) {
            return Integer.compare(this.major, other.major);
        }
        if (this.minor != other.minor) {
            return Integer.compare(this.minor, other.minor);
        }
        if (this.patch != other.patch) {
            return Integer.compare(this.patch, other.patch);
        }

        // SemVer 2.0.0 Rule 11: A normal version has greater precedence than a pre-release version.
        if (this.preRelease == null && other.preRelease != null) {
            return 1;
        }
        if (this.preRelease != null && other.preRelease == null) {
            return -1;
        }
        if (this.preRelease != null && other.preRelease != null) {
            return comparePreReleaseIdentifiers(this.preRelease, other.preRelease);
        }

        return 0;
    }

    private static int comparePreReleaseIdentifiers(String pre1, String pre2) {
        String[] parts1 = pre1.split("\\.");
        String[] parts2 = pre2.split("\\.");
        int minLength = Math.min(parts1.length, parts2.length);

        for (int i = 0; i < minLength; i++) {
            String p1 = parts1[i];
            String p2 = parts2[i];

            boolean p1Numeric = isNumeric(p1);
            boolean p2Numeric = isNumeric(p2);

            if (p1Numeric && p2Numeric) {
                int cmp = Integer.compare(Integer.parseInt(p1), Integer.parseInt(p2));
                if (cmp != 0) return cmp;
            } else if (p1Numeric) {
                return -1; // numeric identifiers have lower precedence than non-numeric
            } else if (p2Numeric) {
                return 1;
            } else {
                int cmp = p1.compareTo(p2);
                if (cmp != 0) return cmp;
            }
        }

        return Integer.compare(parts1.length, parts2.length);
    }

    private static boolean isNumeric(String str) {
        return str != null && str.matches("^\\d+$");
    }

    public String toCleanString() {
        StringBuilder sb = new StringBuilder();
        sb.append(major).append('.').append(minor).append('.').append(patch);
        if (isPreRelease()) {
            sb.append('-').append(preRelease);
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(major).append('.').append(minor).append('.').append(patch);
        if (isPreRelease()) {
            sb.append('-').append(preRelease);
        }
        if (buildMetadata != null && !buildMetadata.isBlank()) {
            sb.append('+').append(buildMetadata);
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SemanticVersion that)) return false;
        // Build metadata ignored for equality as per SemVer 2.0.0 spec
        return major == that.major && minor == that.minor && patch == that.patch
                && Objects.equals(preRelease, that.preRelease);
    }

    @Override
    public int hashCode() {
        return Objects.hash(major, minor, patch, preRelease);
    }
}
