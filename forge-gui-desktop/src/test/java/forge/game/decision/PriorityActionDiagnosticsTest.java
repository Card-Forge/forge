package forge.game.decision;

import org.testng.annotations.Test;
import forge.game.cost.CostAdjustmentPreview;

import static org.testng.Assert.assertEquals;

public class PriorityActionDiagnosticsTest {

    @Test
    public void continuationRecordsKeepSequenceMetadataInDedicatedColumns() {
        final String row = PriorityActionDiagnostics.formatContinuationRecord("DOWNSTREAM", 42L, 481L, 2,
                PriorityActionKind.CAST_SPELL, "42:Drain Life", DownstreamCallbackFamily.TARGET, false,
                3, "MAIN1", "Ada", 5);

        final String[] fields = row.split(",", -1);
        assertEquals(fields.length, 23);
        assertEquals(fields[0], "DOWNSTREAM");
        assertEquals(fields[1], "42");
        assertEquals(fields[2], "481");
        assertEquals(fields[3], "2");
        assertEquals(fields[4], "CAST_SPELL");
        assertEquals(fields[5], "42:Drain Life");
        assertEquals(fields[6], "TARGET");
        assertEquals(fields[7], "false");
        assertEquals(fields[8], "3");
        assertEquals(fields[9], "MAIN1");
        assertEquals(fields[10], "Ada");
        assertEquals(fields[11], "5");
    }

    @Test
    public void feasibilityRecordsKeepPreviewAndFeasibilityMeasurementsInTheirHeaderColumns() {
        final String row = PriorityActionDiagnostics.formatFeasibilityRecord(42L, 3, 1,
                PriorityCostFeasibility.Result.UNSUPPORTED,
                PriorityCostFeasibility.UnsupportedReason.COST_ADJUSTMENT_CHOICE_REQUIRED, 123L,
                CostAdjustmentPreview.Status.CHOICE_REQUIRED, CostAdjustmentPreview.Reason.REDUCTION_ORDER, 45L);

        final String[] fields = row.split(",", -1);
        assertEquals(fields.length, 23);
        assertEquals(fields[0], "FEASIBILITY");
        assertEquals(fields[17], "UNSUPPORTED");
        assertEquals(fields[18], "COST_ADJUSTMENT_CHOICE_REQUIRED");
        assertEquals(fields[19], "123");
        assertEquals(fields[20], "CHOICE_REQUIRED");
        assertEquals(fields[21], "REDUCTION_ORDER");
        assertEquals(fields[22], "45");
    }
}
