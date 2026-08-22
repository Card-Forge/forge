package forge.view;

import forge.game.Match;

/**
 * Compatibility facade for the simulation runner, which now lives in the headless module.
 *
 * @deprecated Use {@link forge.headless.SimulateMatch}.
 */
@Deprecated
public final class SimulateMatch {
    private SimulateMatch() {
    }

    /**
     * @deprecated Use {@link forge.headless.SimulateMatch#simulate(String[])}.
     */
    @Deprecated
    public static void simulate(String[] args) {
        forge.headless.SimulateMatch.simulate(args);
    }

    /**
     * @deprecated Use {@link forge.headless.SimulateMatch#simulateSingleMatch(Match, int, boolean)}.
     */
    @Deprecated
    public static void simulateSingleMatch(Match match, int gameNumber, boolean outputGameLog) {
        forge.headless.SimulateMatch.simulateSingleMatch(match, gameNumber, outputGameLog);
    }
}
