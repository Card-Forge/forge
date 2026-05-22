/**
 * LLM-assisted AI for Forge — the <strong>Forge adapter</strong> for the
 * deck-recognition agent.
 *
 * <p>During a game the AI guesses which deck/archetype its opponent is playing
 * and writes the guess to the game log. The actual reasoning runs in a separate
 * Python service (the LangGraph "sidecar"); this package is the Forge-specific
 * glue that observes the game and talks to it.</p>
 *
 * <h2>The adapter model</h2>
 *
 * <p>The sidecar exposes a client-agnostic HTTP contract: given observations of
 * an opponent's plays it returns an archetype guess. An <em>adapter</em> is the
 * client-specific code that (1) observes a game and (2) speaks that contract.
 * This package is the reference adapter, for Forge. Other MTG clients (e.g.
 * XMage) could provide their own adapter against the same contract — see
 * {@code docs/ADAPTERS.md} in the sidecar repository.</p>
 *
 * <h2>Classes</h2>
 * <ul>
 *   <li>{@link forge.ai.llm.DeckRecognitionManager} — fail-soft attach point,
 *       called from {@code AiController}.</li>
 *   <li>{@link forge.ai.llm.DeckRecognitionObserver} — the Forge-specific part:
 *       subscribes to the game event bus and builds requests.</li>
 *   <li>{@link forge.ai.llm.DeckRecognitionClient} — generic HTTP client for
 *       the sidecar (uses {@code HttpURLConnection} for Android compatibility).</li>
 *   <li>{@link forge.ai.llm.RecognitionRequest} / {@link forge.ai.llm.Observation}
 *       / {@link forge.ai.llm.RecognitionResult} — the wire contract.</li>
 *   <li>{@link forge.ai.llm.DeckRecognitionFeature} — feature-flag resolution.</li>
 * </ul>
 *
 * <p>The feature is <strong>off by default</strong> and entirely fail-soft: if
 * the sidecar is unavailable the game is unaffected, and nothing here changes
 * how the heuristic AI chooses its plays.</p>
 */
package forge.ai.llm;
