package forge.game;

import forge.game.card.Card;
import forge.game.card.CardFactoryUtil;
import forge.game.spellability.SpellAbility;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;

import java.util.EnumSet;
import java.util.Set;

/**
 * Created by Hellfish on 2014-02-09.
 */
public abstract class TriggerReplacementBase extends CardTraitBase {
    protected EnumSet<ZoneType> validHostZones;

    /** The overriding ability. */
    private SpellAbility overridingAbility = null;

    @Override
    public void setHostCard(final Card c) {
        this.hostCard = c;

        if (overridingAbility != null) {
            CardFactoryUtil.correctAbilityChainSourceCard(overridingAbility, c);
        }
    }

    public Set<ZoneType> getActiveZone() {
        return validHostZones;
    }
    
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
    public final boolean zonesCheck(Zone hostCardZone) {
        return !this.hostCard.isPhasedOut()
                && (validHostZones == null || validHostZones.isEmpty()
                || (hostCardZone != null && validHostZones.contains(hostCardZone.getZoneType()))
        );
    }

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

}
