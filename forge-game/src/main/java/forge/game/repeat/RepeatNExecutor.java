package forge.game.repeat;

import forge.game.Game;
import forge.game.spellability.SpellAbility;
import forge.game.state.GameStateFingerprint;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Engine core for the repeat-N shortcut (issue #50, doc marker 11f).
 *
 * <p>This class owns the loop policy only: it executes a supplied ability N
 * times, verifies determinism by comparing the game-state fingerprint before
 * each iteration, and honors an interruption flag. It does NOT decide what to
 * repeat or how to pay for it — that is the UI/controller layer (deferred).</p>
 *
 * <p>The UI is responsible for: capturing the player's performed activation as a
 * fresh {@link SpellAbility} per iteration (via the {@code abilityFactory}),
 * rendering the count dialog + presets (10/50/100/1000), auto-paying any cost
 * and auto-resolving any choices inside the factory, and driving the
 * {@code interrupt} flag from an Esc/Stop control.</p>
 *
 * <p>Determinism gate: before resolving iteration i (i &gt; 0) the snapshot must
 * match the snapshot taken before iteration i-1. If it diverges, automatic
 * repetition is unsafe and we abort — the player must resume manually.</p>
 */
// doc:11f PARTIAL
public final class RepeatNExecutor {

    private RepeatNExecutor() {
    }

    /** Outcome of a repeat-N run. */
    public static final class RepeatResult {
        public final int completed;
        public final boolean aborted;
        public final String abortReason;

        public RepeatResult(final int completed, final boolean aborted, final String abortReason) {
            this.completed = completed;
            this.aborted = aborted;
            this.abortReason = abortReason;
        }
    }

    /**
     * Static analysis: can this ability be auto-repeated without any further
     * player decision? Conservative — returns false on doubt.
     *
     * <p>Rejects triggered abilities (not manually repeatable) and any ability
     * that uses targeting (needs a target choice each iteration). Abilities with
     * a non-zero cost are allowed here only if the caller's factory auto-pays;
     * the executor never pays.</p>
     */
    public static boolean canRepeatDeterministically(final SpellAbility sa) {
        if (sa == null) {
            return false;
        }
        if (sa.isTrigger()) {
            return false;
        }
        if (sa.usesTargeting()) {
            // No isTargetingForced signal on SpellAbility; treat any targeting
            // as a per-iteration choice we cannot make safely.
            return false;
        }
        return true;
    }

    /**
     * Repeat {@code total} iterations of the ability produced by
     * {@code abilityFactory}. Returns how many actually resolved and whether it
     * aborted early (interrupt or determinism divergence).
     */
    public static RepeatResult execute(final Game game, final int total,
            final Supplier<SpellAbility> abilityFactory,
            final Consumer<Integer> progress, final BooleanSupplier interrupt) {
        int done = 0;
        String prevBefore = null;
        for (int i = 0; i < total; i++) {
            if (interrupt.getAsBoolean()) {
                return new RepeatResult(done, true, "interrupted");
            }
            final String before = GameStateFingerprint.compute(game);
            if (i > 0 && prevBefore != null && !before.equals(prevBefore)) {
                return new RepeatResult(done, true, "state diverged, cannot verify determinism");
            }
            final SpellAbility sa = abilityFactory.get();
            if (sa == null) {
                return new RepeatResult(done, true, "ability unavailable");
            }
            game.getStack().addAndUnfreeze(sa);
            game.getStack().resolveStack();
            if (game.isGameOver()) {
                // A loop detector (#48/#49) may have ended the game mid-repeat.
                return new RepeatResult(done, true, "game ended");
            }
            prevBefore = before;
            done++;
            if (progress != null) {
                progress.accept(done);
            }
        }
        return new RepeatResult(done, false, null);
    }
}
