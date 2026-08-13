package forge.game.decision;

/** Controller-local resolver and deterministic ID source for the L1C profile. */
public final class CopySpellResolveFirstOrderDecisionProvider {
    @FunctionalInterface
    public interface Resolver {
        LegalCandidate choose(DecisionRequest request);
    }

    private long nextRequestId = 1L;
    private long nextOrderSessionId = 1L;
    private Resolver resolver;

    public void setResolver(final Resolver resolver0) {
        resolver = resolver0;
    }

    public Resolver getResolver() {
        return resolver;
    }

    public boolean hasResolver() {
        return resolver != null;
    }

    long nextRequestId() {
        return nextRequestId++;
    }

    long nextOrderSessionId() {
        return nextOrderSessionId++;
    }
}
