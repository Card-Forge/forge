package forge.card;

import java.util.EnumSet;

import forge.Card;
import forge.GameEntity;
import forge.card.spellability.SpellAbility;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;

/** 
 * Base class for Triggers and ReplacementEffects.
 * Provides the matchesValid function to both classes.
 * 
 */
public abstract class TriggerReplacementBase {
    
    /** The host card. */
    protected Card hostCard;
    
    /**
     * <p>
     * Getter for the field <code>hostCard</code>.
     * </p>
     * 
     * @return a {@link forge.Card} object.
     */
    public final Card getHostCard() {
        return this.hostCard;
    }

    /**
     * <p>
     * Setter for the field <code>hostCard</code>.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     */
    public final void setHostCard(final Card c) {
        this.hostCard = c;
    }
    
    protected EnumSet<ZoneType> validHostZones;
    
    public void setActiveZone(EnumSet<ZoneType> zones) {
        validHostZones = zones;
    }
    
    /**
     * <p>
     * zonesCheck.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean zonesCheck(PlayerZone hostCardZone) {
        return !this.hostCard.isPhasedOut()
                && (validHostZones == null || validHostZones.isEmpty()
                || (hostCardZone != null && validHostZones.contains(hostCardZone.getZoneType()))
              );
    }
    
    /** The overriding ability. */
    private SpellAbility overridingAbility = null;
    
    /**
     * Gets the overriding ability.
     * 
     * @return the overridingAbility
     */
    public SpellAbility getOverridingAbility() {
        return this.overridingAbility;
    }

    /**
     * Sets the overriding ability.
     * 
     * @param overridingAbility0
     *            the overridingAbility to set
     */
    public void setOverridingAbility(final SpellAbility overridingAbility0) {
        this.overridingAbility = overridingAbility0;
    }
    
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
