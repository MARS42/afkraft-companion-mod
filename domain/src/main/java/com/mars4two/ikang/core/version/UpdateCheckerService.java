package com.mars4two.ikang.core.version;

import com.mars4two.ikang.core.config.GsonConfig;
import com.mars4two.ikang.core.config.HttpConfig;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Asynchronous, non-blocking service that queries Forge/NeoForge update manifest
 * and computes version difference using pure Java.
 */
public class UpdateCheckerService {

    public static final URI DEFAULT_UPDATE_URI = URI.create(
            "https://raw.githubusercontent.com/MARS42/afkraft-companion-mod/main/update.json"
    );

    private static final Duration TIMEOUT = Duration.ofSeconds(3);

    private final URI updateUri;

    public UpdateCheckerService() {
        this(DEFAULT_UPDATE_URI);
    }

    public UpdateCheckerService(URI updateUri) {
        this.updateUri = updateUri != null ? updateUri : DEFAULT_UPDATE_URI;
    }

    public CompletableFuture<UpdateCheckResult> checkAsync(String rawCurrentModVer, String rawMcVer) {
        SemanticVersion currentVer = SemanticVersion.parse(rawCurrentModVer);

        HttpRequest request = HttpRequest.newBuilder(updateUri)
                .timeout(TIMEOUT)
                .header("User-Agent", "AFKraftCompanion/" + rawCurrentModVer)
                .GET()
                .build();

        return HttpConfig.getHttpClient()
                .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        return UpdateCheckResult.error(currentVer, "HTTP " + response.statusCode());
                    }
                    return parseAndCompare(response.body(), currentVer, rawMcVer);
                })
                .exceptionally(ex -> UpdateCheckResult.error(currentVer, ex.getMessage()));
    }

    public UpdateCheckResult parseAndCompare(String jsonBody, SemanticVersion currentVer, String mcVer) {
        try {
            ForgeUpdateManifest manifest = GsonConfig.fromJson(jsonBody, ForgeUpdateManifest.class);
            if (manifest == null) {
                return UpdateCheckResult.error(currentVer, "Empty update manifest");
            }

            Optional<String> latestStrOpt = manifest.resolveLatest(mcVer);
            if (latestStrOpt.isEmpty()) {
                return UpdateCheckResult.upToDate(currentVer, manifest.getHomepage());
            }

            SemanticVersion latestVer = SemanticVersion.parse(latestStrOpt.get());
            int comparison = latestVer.compareTo(currentVer);

            if (comparison > 0) {
                return UpdateCheckResult.outdated(currentVer, latestVer, manifest.getHomepage());
            } else if (comparison < 0) {
                return UpdateCheckResult.ahead(currentVer, latestVer, manifest.getHomepage());
            } else {
                return UpdateCheckResult.upToDate(currentVer, manifest.getHomepage());
            }
        } catch (Exception e) {
            return UpdateCheckResult.error(currentVer, "Failed to parse manifest: " + e.getMessage());
        }
    }
}
