package forge.sound;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


import forge.Card;
import forge.card.spellability.SpellAbility;
import forge.game.event.CounterAddedEvent;
import forge.game.event.BlockerAssignedEvent;
import forge.game.event.CardDamagedEvent;
import forge.game.event.CardDestroyedEvent;
import forge.game.event.CardDiscardedEvent;
import forge.game.event.CardEquippedEvent;
import forge.game.event.CardRegeneratedEvent;
import forge.game.event.CardSacrificedEvent;
import forge.game.event.DrawCardEvent;
import forge.game.event.DuelOutcomeEvent;
import forge.game.event.EndOfTurnEvent;
import forge.game.event.Event;
import forge.game.event.FlipCoinEvent;
import forge.game.event.LandPlayedEvent;
import forge.game.event.LifeLossEvent;
import forge.game.event.PoisonCounterEvent;
import forge.game.event.CounterRemovedEvent;
import forge.game.event.SetTappedEvent;
import forge.game.event.ShuffleEvent;
import forge.game.event.SpellResolvedEvent;
import forge.game.event.TokenCreatedEvent;

/** 
 * This class is in charge of converting any forge.game.event.Event to a SoundEffectType.
 *
 */
public class EventVisualizer {

    static final Map<Class<?>, SoundEffectType> matchTable = new HashMap<Class<?>, SoundEffectType>();

    public EventVisualizer() {
        matchTable.put(CounterAddedEvent.class, SoundEffectType.AddCounter);
        matchTable.put(BlockerAssignedEvent.class, SoundEffectType.Block);
        matchTable.put(CardDamagedEvent.class, SoundEffectType.Damage);
        matchTable.put(CardDestroyedEvent.class, SoundEffectType.Destroy);
        matchTable.put(CardDiscardedEvent.class, SoundEffectType.Discard);
        matchTable.put(DrawCardEvent.class, SoundEffectType.Draw);
        matchTable.put(EndOfTurnEvent.class, SoundEffectType.EndOfTurn);
        matchTable.put(CardEquippedEvent.class, SoundEffectType.Equip);
        matchTable.put(FlipCoinEvent.class, SoundEffectType.FlipCoin);
        matchTable.put(LifeLossEvent.class, SoundEffectType.LifeLoss);
        matchTable.put(PoisonCounterEvent.class, SoundEffectType.Poison);
        matchTable.put(CardRegeneratedEvent.class, SoundEffectType.Regen);
        matchTable.put(CounterRemovedEvent.class, SoundEffectType.RemoveCounter);
        matchTable.put(CardSacrificedEvent.class, SoundEffectType.Sacrifice);
        matchTable.put(ShuffleEvent.class, SoundEffectType.Shuffle);
        matchTable.put(TokenCreatedEvent.class, SoundEffectType.Token);
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
        if (evt instanceof CounterAddedEvent) {
            if (((CounterAddedEvent) evt).Amount == 0) {
                return null;
            }
        }
        if (evt instanceof CounterRemovedEvent) {
            if (((CounterRemovedEvent) evt).Amount == 0) {
                return null;
            }
        }
        if (evt instanceof SetTappedEvent) {
            return getSoundEffectForTapState(((SetTappedEvent) evt).Tapped);
        }
        if (evt instanceof DuelOutcomeEvent) {
            return getSoundEffectForDuelOutcome(((DuelOutcomeEvent) evt).humanWonTheDuel);
        }

        return fromMap;
    }

    /**
     * Plays the sound corresponding to the outcome of the duel.
     * 
     * @param humanWonTheDuel true if the human won the duel, false otherwise.
     * @return the sound effect played
     */
    public SoundEffectType getSoundEffectForDuelOutcome(boolean humanWonTheDuel) {
        return humanWonTheDuel ? SoundEffectType.WinDuel : SoundEffectType.LoseDuel;
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
            if (getSpecificCardEffect(source)) {
                return SoundEffectType.ScriptedEffect;
            }

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
        if (getSpecificCardEffect(land)) {
            return SoundEffectType.ScriptedEffect;
        }

        final List<SpellAbility> manaProduced = land.getManaAbility();

        for (SpellAbility sa : manaProduced) {
            String manaColors = sa.getManaPart().getOrigProduced();

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
    private static boolean getSpecificCardEffect(final Card c) {
        // Implement sound effects for specific cards here, if necessary.
        String effect = "";
        if (null != c) {
            effect = c.getSVar("SoundEffect");
        }
        return effect.isEmpty() ? false : true;
    }


    /**
     * Returns the value of the SoundEffect SVar of the card that triggered
     * the event, otherwise returns an empty string.
     * 
     * @param evt the event which is the source of the sound effect
     * @return a string containing the SoundEffect SVar, or empty string if
     * SVar:SoundEffect does not exist.
     */
    public String getScriptedSoundEffectName(Event evt) {
        Card c = null;

        if (evt instanceof SpellResolvedEvent) {
            c = ((SpellResolvedEvent) evt).Source;
        } else if (evt instanceof LandPlayedEvent) {
            c = ((LandPlayedEvent) evt).Land;
        }

        return c != null ? c.getSVar("SoundEffect") : "";
    }

    /**
     * Determine if the event will potentially produce a lot of overlapping
     * sounds, in which case only one of the must actually be played and the
     * others must wait on it and only play when the first sound effect is done.
     */
    public boolean isSyncSound(SoundEffectType effect) {

        return effect.getIsSynced();
    }

}
