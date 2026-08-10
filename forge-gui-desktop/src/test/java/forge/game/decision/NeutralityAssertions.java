package forge.game.decision;

import forge.game.Game;
import forge.util.DeterminismAuditRandom;
import forge.util.MyRandom;

import java.util.Random;
import java.util.function.Supplier;

import static org.testng.Assert.assertEquals;

/** Shared fail-loud gate for neutral decision generation and diagnostic replay. */
final class NeutralityAssertions {
    private NeutralityAssertions() {
    }

    static <T> T assertGameAndRngNeutral(final String family, final Game game, final Supplier<T> probe) {
        final Random previous = MyRandom.getRandom();
        final DeterminismAuditRandom auditRandom = new DeterminismAuditRandom(20260810L);
        MyRandom.setRandom(auditRandom);
        try {
            final String stateBefore = ForgeStateFingerprint.canonical(game);
            final long drawsBefore = auditRandom.getDrawCount();
            final T result = probe.get();
            assertEquals(ForgeStateFingerprint.canonical(game), stateBefore,
                    family + " changed Forge gameplay state");
            assertEquals(auditRandom.getDrawCount(), drawsBefore,
                    family + " consumed gameplay RNG");
            return result;
        } finally {
            MyRandom.setRandom(previous);
        }
    }
}
