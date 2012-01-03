package forge.card;

import forge.Card;
import forge.Player;

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
        if (o instanceof Card) {
            final Card c = (Card) o;
            return c.isValid(valids, srcCard.getController(), srcCard);
        }

        if (o instanceof Player) {
            for (final String v : valids) {
                if (v.equalsIgnoreCase("Player") || v.equalsIgnoreCase("Each")) {
                    return true;
                }
                if (v.equalsIgnoreCase("Opponent")) {
                    if (o.equals(srcCard.getController().getOpponent())) {
                        return true;
                    }
                }
                if (v.equalsIgnoreCase("You")) {
                    return o.equals(srcCard.getController());
                }
                if (v.equalsIgnoreCase("EnchantedController")) {
                    return ((Player) o).isPlayer(srcCard.getEnchantingCard().getController());
                }
                if (v.equalsIgnoreCase("EnchantedPlayer")) {
                    return o.equals(srcCard.getEnchanting());
                }
            }
        }

        return false;
    }
}
