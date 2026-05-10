package forge.ai.simulation;

/**
 * Tunable coefficients for {@link GameStateEvaluator}. Access via {@link #current()} after {@link #configure(GameStateEvalVariant)}.
 */
public final class GameStateEvalWeights {

    public static final GameStateEvalWeights VARIANT_A = new GameStateEvalWeights(
            1, 5, 4, 2, 2,
            100, 100, 5);

    /** Experimental bundle; initially matches A so behavior is unchanged until tuned. */
    public static final GameStateEvalWeights VARIANT_B = new GameStateEvalWeights(
            1, 5, 4, 2, 2,
            100, 100, 5);

    private static volatile GameStateEvalWeights current = VARIANT_A;

    private final int excessHandPenaltyPerCard;
    private final int handMyCoefficient;
    private final int handTheirCoefficient;
    private final int lifeMyCoefficient;
    private final int lifeOppCoefficient;
    private final int manaColorPipUnit;
    private final int manaMaxCostUnit;
    private final int manaExcessUnit;

    private GameStateEvalWeights(
            final int excessHandPenaltyPerCard,
            final int handMyCoefficient,
            final int handTheirCoefficient,
            final int lifeMyCoefficient,
            final int lifeOppCoefficient,
            final int manaColorPipUnit,
            final int manaMaxCostUnit,
            final int manaExcessUnit) {
        this.excessHandPenaltyPerCard = excessHandPenaltyPerCard;
        this.handMyCoefficient = handMyCoefficient;
        this.handTheirCoefficient = handTheirCoefficient;
        this.lifeMyCoefficient = lifeMyCoefficient;
        this.lifeOppCoefficient = lifeOppCoefficient;
        this.manaColorPipUnit = manaColorPipUnit;
        this.manaMaxCostUnit = manaMaxCostUnit;
        this.manaExcessUnit = manaExcessUnit;
    }

    /**
     * Sets the active weight bundle for this JVM. Intended to run once at startup (see forge-gui bootstrap).
     */
    public static void configure(final GameStateEvalVariant variant) {
        current = variant == GameStateEvalVariant.B ? VARIANT_B : VARIANT_A;
    }

    public static GameStateEvalWeights current() {
        return current;
    }

    public int getExcessHandPenaltyPerCard() {
        return excessHandPenaltyPerCard;
    }

    public int getHandMyCoefficient() {
        return handMyCoefficient;
    }

    public int getHandTheirCoefficient() {
        return handTheirCoefficient;
    }

    public int getLifeMyCoefficient() {
        return lifeMyCoefficient;
    }

    public int getLifeOppCoefficient() {
        return lifeOppCoefficient;
    }

    public int getManaColorPipUnit() {
        return manaColorPipUnit;
    }

    public int getManaMaxCostUnit() {
        return manaMaxCostUnit;
    }

    public int getManaExcessUnit() {
        return manaExcessUnit;
    }
}
