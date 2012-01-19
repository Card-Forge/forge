package forge.card;

import forge.Card;
import forge.GameEntity;

/** 
 * Base class for Triggers and ReplacementEffects.
 * Provides the matchesValid function to both classes.
 * 
 */
public abstract class TriggerReplacementBase {
    /**
     * <p>
     * matchesValid.
     * </p>
     * 
     * @param o
     *            a {@link java.lang.Object} object.
     * @param valids
     *            an array of {@link java.lang.String} objects.
     * @param srcCard
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public static boolean matchesValid(final Object o, final String[] valids, final Card srcCard) {
        if (o instanceof GameEntity) {
            final GameEntity c = (GameEntity) o;
            return c.isValid(valids, srcCard.getController(), srcCard);
        }

        return false;
    }

    /** The suppressed. */
    private boolean suppressed = false;

    /** The temporarily suppressed. */
    private boolean temporarilySuppressed = false;

    /**
     * Sets the suppressed.
     * 
     * @param supp
     *            the new suppressed
     */
    public final void setSuppressed(final boolean supp) {
        this.suppressed = supp;
    }

    /**
     * Sets the temporarily suppressed.
     * 
     * @param supp
     *            the new temporarily suppressed
     */
    public final void setTemporarilySuppressed(final boolean supp) {
        this.temporarilySuppressed = supp;
    }

    /**
     * Checks if is suppressed.
     * 
     * @return true, if is suppressed
     */
    public final boolean isSuppressed() {
        return (this.suppressed || this.temporarilySuppressed);
    }
}
