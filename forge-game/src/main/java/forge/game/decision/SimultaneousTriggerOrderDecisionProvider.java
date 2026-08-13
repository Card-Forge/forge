package forge.game.decision;

/** Controller-local owner and deterministic counter source for ORDER sessions. */
public final class SimultaneousTriggerOrderDecisionProvider {
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
