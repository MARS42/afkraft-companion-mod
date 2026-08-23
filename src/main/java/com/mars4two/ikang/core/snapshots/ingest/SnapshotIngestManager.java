package com.mars4two.ikang.core.snapshots.ingest;

import com.mars4two.ikang.core.snapshots.ContainerSnapshot;
import com.mars4two.ikang.core.snapshots.SnapshotBase;
import com.mars4two.ikang.core.util.Conflater;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class SnapshotIngestManager implements AutoCloseable {

    private static final SnapshotIngestManager INSTANCE = new SnapshotIngestManager();

    private static final Logger log = LoggerFactory.getLogger(SnapshotIngestManager.class);

    private Conflater<SnapshotBase> conflater;

    public static SnapshotIngestManager getInstance() {
        return INSTANCE;
    }

    public void start() {
        conflater = new Conflater<>(1000,
                SnapshotBase::key, SnapshotBase::changeToken,
                this::onEmit, this::onEmitError);
        conflater.start();
    }

    public void ingestContainerBlockEntity(BaseContainerBlockEntity containerBlockEntity) {
        conflater.submit(ContainerSnapshot.of(containerBlockEntity));
    }

    private void onEmit(List<SnapshotBase> snapshots) {
        log.info("Emitting {} snapshots...", snapshots.size());
    }

    private void onEmitError(Throwable error) {
        log.error("Error emitting snapshots", error);
    }

    @Override
    public void close() {
        try {
            conflater.close();
            conflater = null;
        } catch (Exception e) {
            log.error("Error closing SnapshotIngestManager", e);
        }
    }
}
