package forge.game.decision;

import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;

public class DeterminismTraceHasherTest {

    @Test
    public void hashesVersionedCanonicalRecordsWithAnExplicitLineBoundary() {
        final String record = "DECISION_TRACE_V1|0|1|MAIN1|0|PRIORITY_ACTION||0|false|"
                + "PRIORITY_ACTION|PASS";

        assertEquals(DeterminismTraceHasher.sha256(List.of(record)),
                "466660a01fe4a56c01773821948a2fb1829a2ffd1a81a88121a0ab9a44c751b4");
    }

    @Test
    public void firstDivergenceReportsTheFirstUnequalOrMissingRecord() {
        final List<String> left = List.of("same-0", "same-1", "left-2");
        final List<String> right = List.of("same-0", "same-1", "right-2", "right-3");

        assertEquals(DeterminismTraceHasher.firstDivergence(left, right), 2);
        assertEquals(DeterminismTraceHasher.firstDivergence(left, left), -1);
        assertEquals(DeterminismTraceHasher.firstDivergence(left.subList(0, 2), left), 2);
    }
}
