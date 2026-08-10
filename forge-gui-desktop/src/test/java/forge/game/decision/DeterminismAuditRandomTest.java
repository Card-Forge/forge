package forge.game.decision;

import forge.util.DeterminismAuditRandom;
import org.testng.annotations.Test;

import java.util.Random;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class DeterminismAuditRandomTest {

    @Test
    public void preservesJavaRandomValuesWhileRecordingEveryUnderlyingDraw() {
        final Random expected = new Random(42L);
        final DeterminismAuditRandom actual = new DeterminismAuditRandom(42L);

        assertEquals(actual.nextInt(100), expected.nextInt(100));
        assertEquals(actual.nextLong(), expected.nextLong());
        assertEquals(actual.nextDouble(), expected.nextDouble());
        assertEquals(actual.getDrawCount(), 5L);
        assertEquals(actual.getCanonicalRecords(0L, actual.getDrawCount()).size(), 5);
        assertTrue(actual.getCanonicalRecords(0L, 1L).get(0).startsWith("RNG_TRACE_V1|0|"));
    }

    @Test
    public void readingTraceMetadataConsumesNoAdditionalRandomDraws() {
        final DeterminismAuditRandom random = new DeterminismAuditRandom(20260810L);
        random.nextBoolean();
        final long before = random.getDrawCount();

        random.getCanonicalRecords(0L, before);
        random.getDiagnosticRecords(0L, before);

        assertEquals(random.getDrawCount(), before);
    }
}
