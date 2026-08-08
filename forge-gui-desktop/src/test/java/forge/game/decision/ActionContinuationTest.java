package forge.game.decision;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class ActionContinuationTest {

    @Test
    public void retainsPriorityRequestIdentityAndAssignsSequentialSubdecisionIndexes() {
        final ActionContinuation continuation = new ActionContinuation(481L,
                PriorityActionKind.CAST_SPELL, "42:Drain Life");

        assertEquals(continuation.getDecisionSequenceId(), 481L);
        assertEquals(continuation.getTopLevelSubdecisionIndex(), 0);
        assertEquals(continuation.nextSubdecisionIndex(), 1);
        assertEquals(continuation.nextSubdecisionIndex(), 2);
        assertEquals(continuation.getTopLevelCandidateKind(), PriorityActionKind.CAST_SPELL);
        assertEquals(continuation.getTopLevelSource(), "42:Drain Life");
    }
}
