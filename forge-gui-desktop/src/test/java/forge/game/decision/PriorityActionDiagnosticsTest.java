package forge.game.decision;

import org.testng.annotations.Test;
import forge.game.cost.CostAdjustmentPreview;

import static org.testng.Assert.assertEquals;

public class PriorityActionDiagnosticsTest {

    @Test
    public void continuationRecordsKeepSequenceMetadataInDedicatedColumns() {
        final String row = PriorityActionDiagnostics.formatContinuationRecord("DOWNSTREAM", 42L, 481L, 2,
                PriorityActionKind.CAST_SPELL, "42:Drain Life", DownstreamCallbackFamily.TARGET, false,
                3, "MAIN1", "Ada", "Bea", 5);

        final String[] fields = row.split(",", -1);
        assertEquals(fields.length, 54);
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
        assertEquals(fields[11], "Bea");
        assertEquals(fields[12], "5");
    }

    @Test
    public void onlySingleAbilitySelectionsCanOpenACorrelatedContinuation() {
        assertEquals(PriorityActionDiagnostics.isSingleActionSelection(1), true);
        assertEquals(PriorityActionDiagnostics.isSingleActionSelection(0), false);
        assertEquals(PriorityActionDiagnostics.isSingleActionSelection(2), false);
    }

    @Test
    public void feasibilityRecordsKeepPreviewAndFeasibilityMeasurementsInTheirHeaderColumns() {
        final String row = PriorityActionDiagnostics.formatFeasibilityRecord(42L, 3, 1,
                PriorityCostFeasibility.Result.UNSUPPORTED,
                PriorityCostFeasibility.UnsupportedReason.COST_ADJUSTMENT_CHOICE_REQUIRED, 123L,
                CostAdjustmentPreview.Status.CHOICE_REQUIRED, CostAdjustmentPreview.Reason.REDUCTION_ORDER, 45L);

        final String[] fields = row.split(",", -1);
        assertEquals(fields.length, 54);
        assertEquals(fields[0], "FEASIBILITY");
        assertEquals(fields[18], "UNSUPPORTED");
        assertEquals(fields[19], "COST_ADJUSTMENT_CHOICE_REQUIRED");
        assertEquals(fields[20], "123");
        assertEquals(fields[21], "CHOICE_REQUIRED");
        assertEquals(fields[22], "REDUCTION_ORDER");
        assertEquals(fields[23], "45");
    }

    @Test
    public void targetRecordsKeepChooserAndTargetMetadataWithoutTargetNames() {
        final String row = PriorityActionDiagnostics.formatTargetRecord(42L, 481L, 1,
                PriorityActionKind.CAST_SPELL, "42:Drain Life", false, 3, "MAIN1", "Ada", "Bea", 5,
                0, TargetDecisionProvider.Status.DECISION, true, 123L, null);

        final String[] fields = row.split(",", -1);
        assertEquals(fields.length, 54);
        assertEquals(fields[0], "TARGET");
        assertEquals(fields[1], "42");
        assertEquals(fields[2], "481");
        assertEquals(fields[3], "1");
        assertEquals(fields[6], "TARGET");
        assertEquals(fields[7], "false");
        assertEquals(fields[11], "Bea");
        assertEquals(fields[12], "5");
        assertEquals(fields[15], "123");
        assertEquals(fields[24], "0");
        assertEquals(fields[25], "DECISION");
        assertEquals(fields[26], "true");
    }

    @Test
    public void xRecordsSeparateRawBoundsFromNeutralCandidateSummary() {
        final String row = PriorityActionDiagnostics.formatXRecord("X_VALUE", 42L, 481L, 1,
                PriorityActionKind.CAST_SPELL, "42:Invoke the Firemind", true, 3, "MAIN1", "Ada", "Ada",
                1, 123L, 0, Integer.MAX_VALUE, 0, 0, XDecisionProvider.Status.DECISION, null);

        final String[] fields = row.split(",", -1);
        assertEquals(fields.length, 54);
        assertEquals(fields[0], "X_VALUE");
        assertEquals(fields[6], "X_VALUE");
        assertEquals(fields[33], "0");
        assertEquals(fields[34], Integer.toString(Integer.MAX_VALUE));
        assertEquals(fields[35], "0");
        assertEquals(fields[36], "0");
        assertEquals(fields[37], "DECISION");
        assertEquals(fields[39], "X");
    }

    @Test
    public void modeRecordsSeparateRawCallbacksNeutralRequestsAndProbeCounts() {
        final String callback = PriorityActionDiagnostics.formatModeRecord("MODE_CALLBACK", 42L, 481L, null,
                PriorityActionKind.CAST_SPELL, "42:Izzet Charm", false, 3, "MAIN1", "Ada", "Ada",
                3, 0L, null, null, 0, 0, "");
        final String request = PriorityActionDiagnostics.formatModeRecord("MODE", 42L, 481L, 1,
                PriorityActionKind.CAST_SPELL, "42:Izzet Charm", false, 3, "MAIN1", "Ada", "Ada",
                2, 123L, ModeDecisionProvider.Status.DECISION, null, 4, 3, "0+2");

        final String[] callbackFields = callback.split(",", -1);
        final String[] requestFields = request.split(",", -1);
        assertEquals(callbackFields.length, 54);
        assertEquals(callbackFields[0], "MODE_CALLBACK");
        assertEquals(callbackFields[3], "");
        assertEquals(callbackFields[6], "MODE");
        assertEquals(requestFields[0], "MODE");
        assertEquals(requestFields[3], "1");
        assertEquals(requestFields[12], "2");
        assertEquals(requestFields[15], "123");
        assertEquals(requestFields[40], "DECISION");
        assertEquals(requestFields[41], "4");
        assertEquals(requestFields[42], "3");
        assertEquals(requestFields[43], "0+2");
    }

    @Test
    public void discardCardSelectionRecordsUseCallbackLocalIdentityWithoutActionContinuation() {
        final String callback = PriorityActionDiagnostics.formatDiscardSelectionRecord(
                "CARD_SELECTION_DISCARD_CALLBACK", 42L, 7, 19L, null, false, 3, "MAIN1", "Ada", "Ada",
                7, 0L, 123L, DiscardCardSelectionAdapter.Status.SUPPORTED, null, 0, 7, 7, 0);
        final String request = PriorityActionDiagnostics.formatDiscardSelectionRecord(
                "CARD_SELECTION", 42L, 7, 19L, 1, false, 3, "MAIN1", "Ada", "Ada",
                6, 456L, 0L, CardSelectionDecisionProvider.Status.DECISION, null, 1, 6, 7, 1);

        final String[] callbackFields = callback.split(",", -1);
        final String[] requestFields = request.split(",", -1);
        assertEquals(callbackFields.length, 54);
        assertEquals(callbackFields[0], "CARD_SELECTION_DISCARD_CALLBACK");
        assertEquals(callbackFields[2], "");
        assertEquals(callbackFields[3], "");
        assertEquals(callbackFields[6], "CARD_SELECTION");
        assertEquals(callbackFields[16], "123");
        assertEquals(callbackFields[44], "DISCARD");
        assertEquals(callbackFields[45], "7");
        assertEquals(callbackFields[46], "19");
        assertEquals(callbackFields[47], "");
        assertEquals(callbackFields[48], "SUPPORTED");
        assertEquals(requestFields[0], "CARD_SELECTION");
        assertEquals(requestFields[2], "");
        assertEquals(requestFields[3], "");
        assertEquals(requestFields[47], "1");
        assertEquals(requestFields[48], "DECISION");
        assertEquals(requestFields[50], "1");
        assertEquals(requestFields[51], "6");
        assertEquals(requestFields[52], "7");
        assertEquals(requestFields[53], "1");
    }

    @Test
    public void legacyCostCardSelectionRemainsADownstreamEventWithoutDiscardAdapterIdentity() {
        final String row = PriorityActionDiagnostics.formatContinuationRecord("DOWNSTREAM", 42L, 481L, 2,
                PriorityActionKind.CAST_SPELL, "42:Cost", DownstreamCallbackFamily.CARD_SELECTION, false,
                3, "MAIN1", "Ada", "Ada", 5);

        final String[] fields = row.split(",", -1);
        assertEquals(fields[0], "DOWNSTREAM");
        assertEquals(fields[6], "CARD_SELECTION");
        assertEquals(fields[44], "");
        assertEquals(fields[45], "");
        assertEquals(fields[46], "");
    }

    @Test
    public void attackRecordsUseTurnBasedSessionColumnsWithoutActionContinuation() {
        final String row = PriorityActionDiagnostics.formatAttackDeclarationRecord(
                "ATTACK", 42L, 19, 7L, 1, false, 3, "COMBAT_DECLARE_ATTACKERS", "Ada", "Ada", 2,
                123L, 456L, AttackDeclarationDecisionProvider.Status.DECISION, null, 1, 1, 2, 1);

        final String[] fields = row.split(",", -1);
        assertEquals(fields.length, 54);
        assertEquals(fields[0], "ATTACK");
        assertEquals(fields[2], "");
        assertEquals(fields[3], "");
        assertEquals(fields[6], "ATTACK");
        assertEquals(fields[16], "456");
        assertEquals(fields[44], "ATTACK");
        assertEquals(fields[45], "19");
        assertEquals(fields[46], "7");
        assertEquals(fields[47], "1");
        assertEquals(fields[48], "DECISION");
        assertEquals(fields[50], "1");
        assertEquals(fields[51], "1");
        assertEquals(fields[52], "2");
        assertEquals(fields[53], "1");
    }
}
