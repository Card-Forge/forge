package forge.gui;

import forge.ai.simulation.GameStateEvalVariant;
import forge.ai.simulation.GameStateEvalWeights;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;

/**
 * Selects {@link GameStateEvalWeights} once per JVM for AI simulation scoring.
 * <p>
 * Precedence: {@code -Dforge.eval.variant=A|B} overrides the {@link FPref#UI_GAME_STATE_EVAL_VARIANT}
 * preference when the property is non-blank.
 * </p>
 */
public final class GameStateEvalVariantBootstrap {

    private GameStateEvalVariantBootstrap() {
    }

    /**
     * Reads {@code forge.eval.variant} or prefs and configures {@link GameStateEvalWeights}.
     * Call before {@link forge.model.FModel#initialize} when {@link GuiBase#getForgePrefs()} is valid.
     */
    public static void apply() {
        final String prop = System.getProperty("forge.eval.variant");
        if (prop != null && !prop.isBlank()) {
            final GameStateEvalVariant fromProp = parseStrictVariant(prop.trim());
            if (fromProp != null) {
                GameStateEvalWeights.configure(fromProp);
                System.out.println("Game state eval weights: variant " + fromProp + " (from forge.eval.variant)");
            } else {
                System.err.println("Invalid forge.eval.variant (use A or B): " + prop);
                GameStateEvalWeights.configure(GameStateEvalVariant.A);
                System.out.println("Game state eval weights: variant A (default after invalid forge.eval.variant)");
            }
            return;
        }

        final ForgePreferences prefs = GuiBase.getForgePrefs();
        final String pref = prefs.getPref(FPref.UI_GAME_STATE_EVAL_VARIANT);
        final String trimmed = pref != null ? pref.trim() : "";
        if (trimmed.isEmpty()) {
            GameStateEvalWeights.configure(GameStateEvalVariant.A);
            System.out.println("Game state eval weights: variant A (default, empty preference)");
            return;
        }
        final GameStateEvalVariant fromPref = parseStrictVariant(trimmed);
        if (fromPref != null) {
            GameStateEvalWeights.configure(fromPref);
            System.out.println("Game state eval weights: variant " + fromPref + " (from preferences)");
            return;
        }
        System.err.println("Invalid UI_GAME_STATE_EVAL_VARIANT preference (use A or B): " + pref);
        GameStateEvalWeights.configure(GameStateEvalVariant.A);
        System.out.println("Game state eval weights: variant A (default after invalid preference)");
    }

    /** Non-empty strings only: A/a or B/b, else null. */
    private static GameStateEvalVariant parseStrictVariant(final String trimmedNonEmpty) {
        if (trimmedNonEmpty.equalsIgnoreCase("A")) {
            return GameStateEvalVariant.A;
        }
        if (trimmedNonEmpty.equalsIgnoreCase("B")) {
            return GameStateEvalVariant.B;
        }
        return null;
    }
}
