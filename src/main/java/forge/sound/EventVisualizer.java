package forge.sound;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


import forge.Card;
import forge.card.spellability.SpellAbility;
import forge.game.event.AddCounterEvent;
import forge.game.event.CardDamagedEvent;
import forge.game.event.CardDestroyedEvent;
import forge.game.event.EndOfTurnEvent;
import forge.game.event.Event;
import forge.game.event.FlipCoinEvent;
import forge.game.event.LandPlayedEvent;
import forge.game.event.LifeLossEvent;
import forge.game.event.PoisonCounterEvent;
import forge.game.event.RemoveCounterEvent;
import forge.game.event.SetTappedEvent;
import forge.game.event.ShuffleEvent;
import forge.game.event.SpellResolvedEvent;

/** 
 * This class is in charge of converting any forge.game.event.Event to a SoundEffectType
 *
 */
public class EventVisualizer {

    final static Map<Class<?>, SoundEffectType> matchTable = new HashMap<Class<?>, SoundEffectType>();
    
    public EventVisualizer() { 
        matchTable.put(PoisonCounterEvent.class, SoundEffectType.Poison);
        matchTable.put(AddCounterEvent.class, SoundEffectType.AddCounter);
        matchTable.put(RemoveCounterEvent.class, SoundEffectType.RemoveCounter);
        matchTable.put(ShuffleEvent.class, SoundEffectType.Shuffle);
        matchTable.put(FlipCoinEvent.class, SoundEffectType.FlipCoin);
        matchTable.put(EndOfTurnEvent.class, SoundEffectType.EndOfTurn);
        matchTable.put(CardDestroyedEvent.class, SoundEffectType.Destroy);
        matchTable.put(CardDamagedEvent.class, SoundEffectType.Damage);
        matchTable.put(LifeLossEvent.class, SoundEffectType.LifeLoss);
    }
    
    
    public final SoundEffectType getSoundForEvent(Event evt) {
        SoundEffectType fromMap = matchTable.get(evt.getClass());

        // call methods copied from Utils here
        if (evt instanceof SpellResolvedEvent) {
            return getSoundEffectForResolve(((SpellResolvedEvent) evt).Source, ((SpellResolvedEvent) evt).Spell);
        }
        if (evt instanceof LandPlayedEvent) {
            return getSoundEffectForLand(((LandPlayedEvent) evt).Land);
        }
        if (evt instanceof AddCounterEvent) {
            if (((AddCounterEvent) evt).Amount == 0) {
                return null;
            }
        }
        if (evt instanceof RemoveCounterEvent) {
            if (((RemoveCounterEvent) evt).Amount == 0) {
                return null;
            }
        }
        if (evt instanceof SetTappedEvent) {
            return getSoundEffectForTapState(((SetTappedEvent) evt).Tapped);
        }

        return fromMap;
    }
    
    /**
     * Plays the sound corresponding to the card type/color when the card
     * ability resolves on the stack.
     *
     * @param source the card to play the sound for.
     * @param sa the spell ability that was resolving.
     */
    public SoundEffectType getSoundEffectForResolve(final Card source, final SpellAbility sa) {
        if (sa == null || source == null) {
            return null;
        }

        if (sa.isSpell()) {
            // if there's a specific effect for this particular card, play it and
            // we're done.
            SoundEffectType specialEffect = getSpecificCardEffect(source);
            if( specialEffect != null ) return specialEffect;

            if (source.isCreature() && source.isArtifact()) {
                return SoundEffectType.ArtifactCreature;
            } else if (source.isCreature()) {
                return SoundEffectType.Creature;
            } else if (source.isArtifact()) {
                return SoundEffectType.Artifact;
            } else if (source.isInstant()) {
                return SoundEffectType.Instant;
            } else if (source.isPlaneswalker()) {
                return SoundEffectType.Planeswalker;
            } else if (source.isSorcery()) {
                return SoundEffectType.Sorcery;
            } else if (source.isEnchantment()) {
                return SoundEffectType.Enchantment;
            }
        }
        return null;
    }

    /**
     * Plays the sound corresponding to the change of the card's tapped state
     * (when a card is tapped or untapped).
     * 
     * @param tapped_state if true, the "tap" sound is played; otherwise, the
     * "untap" sound is played
     * @return the sound effect type
     */
    public static SoundEffectType getSoundEffectForTapState(boolean tapped_state) {
        return tapped_state ? SoundEffectType.Tap : SoundEffectType.Untap;
    }
    
    /**
     * Plays the sound corresponding to the land type when the land is played.
     *
     * @param land the land card that was played
     * @return the sound effect type
     */
    public static SoundEffectType getSoundEffectForLand(final Card land) {
        if (land == null) {
            return null;
        }

        // if there's a specific effect for this particular card, play it and
        // we're done.
        SoundEffectType specialEffect = getSpecificCardEffect(land);
        if( specialEffect != null ) return specialEffect;


        final List<SpellAbility> manaProduced = land.getManaAbility();

        for (SpellAbility sa : manaProduced) {
            String manaColors = sa.getManaPart().getManaProduced();

            if (manaColors.contains("B")) {
                return SoundEffectType.BlackLand;
            }
            if (manaColors.contains("U")) {
                return SoundEffectType.BlueLand;
            }
            if (manaColors.contains("G")) {
                return SoundEffectType.GreenLand;
            }
            if (manaColors.contains("R")) {
                return SoundEffectType.RedLand;
            }
            if (manaColors.contains("W")) {
                return SoundEffectType.WhiteLand;
            }
        }

        // play a generic land sound if no other sound corresponded to it.
        return SoundEffectType.OtherLand;
    }

    /**
     * Play a specific sound effect based on card's name.
     *
     * @param c the card to play the sound effect for.
     * @return the sound effect type
     */
    private static SoundEffectType getSpecificCardEffect(final Card c) {
        // Implement sound effects for specific cards here, if necessary.
        return null;
    }


    /**
     * TODO: Choose is the special type of event produces a single or lot of overlapping sounds (?)
     */
    public boolean isSyncSound(SoundEffectType effect) {

        return effect.getIsSynced();
    }    

}
