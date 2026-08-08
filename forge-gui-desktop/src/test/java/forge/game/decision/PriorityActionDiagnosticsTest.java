package forge.game.decision;

import org.testng.annotations.Test;
import forge.game.cost.CostAdjustmentPreview;

import static org.testng.Assert.assertEquals;

public class PriorityActionDiagnosticsTest {

    @Test
    public void feasibilityRecordsKeepPreviewAndFeasibilityMeasurementsInTheirHeaderColumns() {
        final String row = PriorityActionDiagnostics.formatFeasibilityRecord(42L, 3, 1,
                PriorityCostFeasibility.Result.UNSUPPORTED,
                PriorityCostFeasibility.UnsupportedReason.COST_ADJUSTMENT_CHOICE_REQUIRED, 123L,
                CostAdjustmentPreview.Status.CHOICE_REQUIRED, CostAdjustmentPreview.Reason.REDUCTION_ORDER, 45L);

        final String[] fields = row.split(",", -1);
        assertEquals(fields.length, 17);
        assertEquals(fields[0], "FEASIBILITY");
        assertEquals(fields[11], "UNSUPPORTED");
        assertEquals(fields[12], "COST_ADJUSTMENT_CHOICE_REQUIRED");
        assertEquals(fields[13], "123");
        assertEquals(fields[14], "CHOICE_REQUIRED");
        assertEquals(fields[15], "REDUCTION_ORDER");
        assertEquals(fields[16], "45");
    }
}
