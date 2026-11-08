package com.mars4two.ikang.core.util;

import com.mars4two.ikang.client.core.util.QRMatrixGenerator;
import com.mars4two.ikang.core.snapshots.model.RawPlayerAttackCadence;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CadenceConflaterAndQrTest {

    record SampleEvent(String id, int value) {
    }

    @Test
    void attackCadenceTrackerClassifiesIdleOptimalTooFastAndNoSweep() {
        AttackCadenceTracker tracker = new AttackCadenceTracker();

        assertEquals(RawPlayerAttackCadence.STATUS_IDLE, tracker.snapshot(0, 12.5f, true).status());

        // Optimal sword attacks with full charge and sweep
        tracker.recordAttack(10, 20, 12.5f, 1.0f, true, true, true, true);
        tracker.recordAttack(23, 13, 12.5f, 1.0f, true, true, true, true);
        RawPlayerAttackCadence optimal = tracker.snapshot(25, 12.5f, true);
        assertEquals(RawPlayerAttackCadence.STATUS_OPTIMAL, optimal.status());
        assertEquals(13.0f, optimal.avgIntervalTicks());
        assertEquals(100, optimal.fullChargeRate());
        assertEquals(100, optimal.sweepRate());

        // Idle timeout resets window
        assertEquals(RawPlayerAttackCadence.STATUS_IDLE, tracker.snapshot(200, 12.5f, true).status());

        // Sword attacks without sweep trigger NO_SWEEP
        tracker.recordAttack(210, 20, 12.5f, 1.0f, true, true, false, true);
        assertEquals(RawPlayerAttackCadence.STATUS_NO_SWEEP, tracker.snapshot(215, 12.5f, true).status());

        // Spam clicking triggers TOO_FAST
        tracker.recordAttack(216, 1, 12.5f, 0.2f, false, true, false, true);
        assertEquals(RawPlayerAttackCadence.STATUS_TOO_FAST, tracker.snapshot(217, 12.5f, true).status());
    }

    @Test
    void conflaterEmitsOnlyLatestDistinctStatePerKey() throws Exception {
        List<List<SampleEvent>> batches = new ArrayList<>();
        try (Conflater<SampleEvent> conflater = new Conflater<>(
                60_000,
                SampleEvent::id,
                SampleEvent::value,
                batches::add,
                null
        )) {
            conflater.submit(new SampleEvent("k1", 1));
            conflater.submit(new SampleEvent("k1", 2));
            conflater.flush();

            assertEquals(1, batches.size());
            assertEquals(List.of(new SampleEvent("k1", 2)), batches.get(0));

            // Returning to an intermediate value then back to already-emitted value (2) produces no emission
            conflater.submit(new SampleEvent("k1", 3));
            conflater.submit(new SampleEvent("k1", 2));
            conflater.flush();

            assertEquals(1, batches.size());
        }
    }

    @Test
    void qrMatrixGeneratorProducesSquareQrBitGridWithDarkFinderPatterns() {
        QRMatrixGenerator.QrBitGrid grid = QRMatrixGenerator.encode("https://afkraft.com/connect?otp=123456");
        assertTrue(grid.size() >= 23, "QR grid with margin=1 must be at least 23x23 modules");
        assertEquals(grid.size() * grid.size(), grid.modules().length);

        int darkModules = 0;
        for (int row = 0; row < grid.size(); row++) {
            for (int col = 0; col < grid.size(); col++) {
                if (grid.isDark(col, row)) {
                    darkModules++;
                }
            }
        }
        assertTrue(darkModules > 0 && darkModules < grid.modules().length);
    }
}
