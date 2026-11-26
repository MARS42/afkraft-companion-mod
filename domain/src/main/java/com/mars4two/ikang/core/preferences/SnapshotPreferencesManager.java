package com.mars4two.ikang.core.preferences;

import com.mars4two.ikang.core.auth.AuthService;
import com.mars4two.ikang.core.preferences.dtos.PreferencesOperationResult;
import com.mars4two.ikang.core.snapshots.SnapshotType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Server-side source of truth for active snapshot preferences.
 * Reads are lock-free for hot ingest paths; writes synchronize with the backend asynchronously.
 */
public class SnapshotPreferencesManager {

    public record AuthContext(UUID serverId, UUID adminProfileId, String accessToken) {
    }

    private static final Logger log = LoggerFactory.getLogger(SnapshotPreferencesManager.class);
    private static final SnapshotPreferencesManager INSTANCE = new SnapshotPreferencesManager();

    private final WorldPreferencesService remoteService;
    private final Supplier<Optional<AuthContext>> authContextSupplier;
    private final List<Consumer<Set<String>>> changeListeners = new CopyOnWriteArrayList<>();
    private final AtomicBoolean saveInFlight = new AtomicBoolean(false);

    private volatile Map<String, Boolean> activePreferences = SnapshotType.defaultPreferencesMap();

    public SnapshotPreferencesManager() {
        this(new WorldPreferencesService(), SnapshotPreferencesManager::resolveDefaultAuthContext);
    }

    public SnapshotPreferencesManager(
            WorldPreferencesService remoteService,
            Supplier<Optional<AuthContext>> authContextSupplier
    ) {
        this.remoteService = Objects.requireNonNull(remoteService, "remoteService");
        this.authContextSupplier = Objects.requireNonNull(authContextSupplier, "authContextSupplier");
    }

    public static SnapshotPreferencesManager getInstance() {
        return INSTANCE;
    }

    private static Optional<AuthContext> resolveDefaultAuthContext() {
        Optional<String> tokenOpt = AuthService.getAccessToken();
        if (tokenOpt.isEmpty()) {
            return Optional.empty();
        }
        Optional<UUID> serverId = AuthService.findCurrentServerId();
        Optional<UUID> adminProfileId = AuthService.findAdminProfileId();
        if (serverId.isEmpty() || adminProfileId.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new AuthContext(serverId.get(), adminProfileId.get(), tokenOpt.get()));
    }

    public void addChangeListener(Consumer<Set<String>> listener) {
        if (listener != null) {
            changeListeners.add(listener);
        }
    }

    public boolean isEnabled(SnapshotType type) {
        if (type == null) {
            return true;
        }
        return isEnabled(type.snapshotName());
    }

    public boolean isEnabled(String snapshotName) {
        if (snapshotName == null) {
            return true;
        }
        Boolean enabled = activePreferences.get(snapshotName);
        return enabled == null || enabled;
    }

    public Map<String, Boolean> getEffectivePreferences() {
        return activePreferences;
    }

    public CompletableFuture<PreferencesOperationResult> refreshFromBackendAsync() {
        return CompletableFuture.supplyAsync(this::refreshFromBackend);
    }

    public PreferencesOperationResult refreshFromBackend() {
        Optional<AuthContext> authOpt = authContextSupplier.get();
        if (authOpt.isEmpty()) {
            return PreferencesOperationResult.failure(
                    PreferencesOperationResult.Status.NOT_AUTHENTICATED,
                    activePreferences
            );
        }

        AuthContext auth = authOpt.get();
        PreferencesOperationResult result = remoteService.fetchPreferences(
                auth.serverId(),
                auth.adminProfileId(),
                auth.accessToken(),
                activePreferences
        );
        if (result.isSuccess()) {
            applyPreferences(result.snapshots());
        }
        return result;
    }

    public CompletableFuture<PreferencesOperationResult> saveToBackendAsync(Map<String, Boolean> requestedSnapshots) {
        Optional<Map<String, Boolean>> validated = validateRequestedMap(requestedSnapshots);
        if (validated.isEmpty()) {
            return CompletableFuture.completedFuture(
                    PreferencesOperationResult.failure(
                            PreferencesOperationResult.Status.INVALID_REQUEST,
                            activePreferences
                    )
            );
        }

        if (!saveInFlight.compareAndSet(false, true)) {
            return CompletableFuture.completedFuture(
                    PreferencesOperationResult.failure(
                            PreferencesOperationResult.Status.SAVE_IN_PROGRESS,
                            activePreferences
                    )
            );
        }

        Map<String, Boolean> sanitized = validated.get();
        return CompletableFuture.supplyAsync(() -> {
            try {
                Optional<AuthContext> authOpt = authContextSupplier.get();
                if (authOpt.isEmpty()) {
                    return PreferencesOperationResult.failure(
                            PreferencesOperationResult.Status.NOT_AUTHENTICATED,
                            activePreferences
                    );
                }

                AuthContext auth = authOpt.get();
                PreferencesOperationResult result = remoteService.replacePreferences(
                        auth.serverId(),
                        auth.adminProfileId(),
                        sanitized,
                        auth.accessToken(),
                        activePreferences
                );
                if (result.isSuccess()) {
                    applyPreferences(result.snapshots());
                }
                return result;
            } finally {
                saveInFlight.set(false);
            }
        });
    }

    public void applyLocalPreferencesForTest(Map<String, Boolean> snapshots) {
        applyPreferences(SnapshotType.mergeWithDefaults(snapshots));
    }

    public synchronized void reset() {
        Map<String, Boolean> previous = this.activePreferences;
        Map<String, Boolean> defaults = SnapshotType.defaultPreferencesMap();
        this.activePreferences = defaults;
        this.saveInFlight.set(false);
        Set<String> changed = diffKeys(previous, defaults);
        if (!changed.isEmpty()) {
            notifyListeners(changed);
        }
    }

    private synchronized void applyPreferences(Map<String, Boolean> nextSnapshots) {
        Map<String, Boolean> previous = this.activePreferences;
        Map<String, Boolean> merged = SnapshotType.mergeWithDefaults(nextSnapshots);
        this.activePreferences = merged;

        Set<String> changed = diffKeys(previous, merged);
        if (!changed.isEmpty()) {
            log.info("Snapshot preferences updated ({} changed: {})", changed.size(), changed);
            notifyListeners(changed);
        }
    }

    private void notifyListeners(Set<String> changedSnapshotNames) {
        Set<String> immutableChanged = Collections.unmodifiableSet(changedSnapshotNames);
        for (Consumer<Set<String>> listener : changeListeners) {
            try {
                listener.accept(immutableChanged);
            } catch (Exception e) {
                log.warn("Error notifying snapshot preference change listener", e);
            }
        }
    }

    private static Optional<Map<String, Boolean>> validateRequestedMap(Map<String, Boolean> raw) {
        if (raw == null || raw.isEmpty()) {
            return Optional.empty();
        }
        Map<String, Boolean> cleaned = new LinkedHashMap<>();
        for (Map.Entry<String, Boolean> entry : raw.entrySet()) {
            String key = entry.getKey();
            Boolean value = entry.getValue();
            if (key == null || value == null || !SnapshotType.isKnownSnapshotName(key)) {
                return Optional.empty();
            }
            cleaned.put(key.trim(), value);
        }
        return Optional.of(SnapshotType.mergeWithDefaults(cleaned));
    }

    private static Set<String> diffKeys(Map<String, Boolean> previous, Map<String, Boolean> next) {
        Set<String> changed = new HashSet<>();
        for (SnapshotType type : SnapshotType.values()) {
            String key = type.snapshotName();
            boolean prevVal = previous == null || !Boolean.FALSE.equals(previous.get(key));
            boolean nextVal = next == null || !Boolean.FALSE.equals(next.get(key));
            if (prevVal != nextVal) {
                changed.add(key);
            }
        }
        return changed;
    }
}
