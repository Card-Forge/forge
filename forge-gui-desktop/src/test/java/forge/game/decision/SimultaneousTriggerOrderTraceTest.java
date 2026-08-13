package forge.game.decision;

import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class SimultaneousTriggerOrderTraceTest {
    @Test
    public void externalOrderChosenIsValidHistoryButNotBc() {
        final DecisionTraceRequestRecord request = orderRequest(0L);
        final DecisionTraceResultRecord result = chosen(request, "RESOLVE_FIRST|1", false, false);

        assertTrue(DecisionTraceTrainingValidator.isHistoryValid(request, result));
        assertFalse(DecisionTraceTrainingValidator.isBCPolicySample(request, result));
    }

    @Test
    public void nativeOrderChosenRemainsTheOnlyBcLabel() {
        final DecisionTraceRequestRecord request = orderRequest(1L);
        final DecisionTraceResultRecord result = chosen(request, "RESOLVE_FIRST|1", true, true);

        assertTrue(DecisionTraceTrainingValidator.isHistoryValid(request, result));
        assertTrue(DecisionTraceTrainingValidator.isBCPolicySample(request, result));
    }

    @Test
    public void terminalExternalAndNativeFailuresAreValidHistoryWithoutLabels() {
        final DecisionTraceRequestRecord externalFailure = orderRequest(2L);
        final DecisionTraceResultRecord invalidExternal = new DecisionTraceResultRecord(
                2L, DecisionTraceResultKind.INVALID_EXTERNAL_CANDIDATE, "", false, false,
                false, false, false);
        final DecisionTraceRequestRecord nativeFailure = orderRequest(3L);
        final DecisionTraceResultRecord callbackFailure = new DecisionTraceResultRecord(
                3L, DecisionTraceResultKind.NATIVE_CALLBACK_FAILURE, "", false, false,
                false, false, false);

        assertTrue(DecisionTraceTrainingValidator.isHistoryValid(externalFailure, invalidExternal));
        assertFalse(DecisionTraceTrainingValidator.isBCPolicySample(externalFailure, invalidExternal));
        assertTrue(DecisionTraceTrainingValidator.isHistoryValid(nativeFailure, callbackFailure));
        assertFalse(DecisionTraceTrainingValidator.isBCPolicySample(nativeFailure, callbackFailure));
    }

    @Test
    public void mappingFailureRetainsNativeReturnedSemantics() {
        final DecisionTraceRequestRecord request = orderRequest(4L);
        final DecisionTraceResultRecord result = new DecisionTraceResultRecord(
                4L, DecisionTraceResultKind.MAPPING_FAILED, "", true, true,
                false, false, false);

        assertTrue(DecisionTraceTrainingValidator.isHistoryValid(request, result));
        assertFalse(DecisionTraceTrainingValidator.isBCPolicySample(request, result));
    }

    private static DecisionTraceRequestRecord orderRequest(final long index) {
        return new DecisionTraceRequestRecord(index, 1, "MAIN", 0, DecisionType.ORDER,
                "SIMULTANEOUS_TRIGGER_ORDER", 0, false,
                List.of("RESOLVE_FIRST|1", "RESOLVE_FIRST|2"), "hash");
    }

    private static DecisionTraceResultRecord chosen(final DecisionTraceRequestRecord request,
            final String selected, final boolean nativeCompleted, final boolean mappingAttempted) {
        return new DecisionTraceResultRecord(request.getTraceRequestIndex(),
                DecisionTraceResultKind.CHOSEN, selected, nativeCompleted, mappingAttempted,
                false, false, false);
    }
}
