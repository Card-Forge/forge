package forge.sound;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import forge.Card;
import forge.Singletons;
import forge.card.spellability.SpellAbility;
import forge.game.event.GameEventBlockerAssigned;
import forge.game.event.GameEventCardDamaged;
import forge.game.event.GameEventCardDestroyed;
import forge.game.event.GameEventCardDiscarded;
import forge.game.event.GameEventCardEquipped;
import forge.game.event.GameEventCardRegenerated;
import forge.game.event.GameEventCardSacrificed;
import forge.game.event.GameEventCounterAdded;
import forge.game.event.GameEventCounterRemoved;
import forge.game.event.GameEventDrawCard;
import forge.game.event.GameEventDuelOutcome;
import forge.game.event.GameEventEndOfTurn;
import forge.game.event.GameEvent;
import forge.game.event.GameEventFlipCoin;
import forge.game.event.GameEventLandPlayed;
import forge.game.event.GameEventLifeLoss;
import forge.game.event.GameEventPoisonCounter;
import forge.game.event.GameEventCardTapped;
import forge.game.event.GameEventShuffle;
import forge.game.event.GameEventSpellResolved;
import forge.game.event.GameEventTokenCreated;

/** 
 * This class is in charge of converting any forge.game.event.Event to a SoundEffectType.
 *
 */
public class EventVisualizer {

    static final Map<Class<?>, SoundEffectType> matchTable = new HashMap<Class<?>, SoundEffectType>();

    public EventVisualizer() {
        matchTable.put(GameEventCounterAdded.class, SoundEffectType.AddCounter);
        matchTable.put(GameEventBlockerAssigned.class, SoundEffectType.Block);
        matchTable.put(GameEventCardDamaged.class, SoundEffectType.Damage);
        matchTable.put(GameEventCardDestroyed.class, SoundEffectType.Destroy);
        matchTable.put(GameEventCardDiscarded.class, SoundEffectType.Discard);
        matchTable.put(GameEventDrawCard.class, SoundEffectType.Draw);
        matchTable.put(GameEventEndOfTurn.class, SoundEffectType.EndOfTurn);
        matchTable.put(GameEventCardEquipped.class, SoundEffectType.Equip);
        matchTable.put(GameEventFlipCoin.class, SoundEffectType.FlipCoin);
        matchTable.put(GameEventLifeLoss.class, SoundEffectType.LifeLoss);
        matchTable.put(GameEventPoisonCounter.class, SoundEffectType.Poison);
        matchTable.put(GameEventCardRegenerated.class, SoundEffectType.Regen);
        matchTable.put(GameEventCounterRemoved.class, SoundEffectType.RemoveCounter);
        matchTable.put(GameEventCardSacrificed.class, SoundEffectType.Sacrifice);
        matchTable.put(GameEventShuffle.class, SoundEffectType.Shuffle);
        matchTable.put(GameEventTokenCreated.class, SoundEffectType.Token);
    }


    public final SoundEffectType getSoundForEvent(GameEvent evt) {
        SoundEffectType fromMap = matchTable.get(evt.getClass());

        // call methods copied from Utils here
        if (evt instanceof GameEventSpellResolved) {
            return getSoundEffectForResolve(((GameEventSpellResolved) evt).Source, ((GameEventSpellResolved) evt).Spell);
        }
        if (evt instanceof GameEventLandPlayed) {
            return getSoundEffectForLand(((GameEventLandPlayed) evt).Land);
        }
        if (evt instanceof GameEventCounterAdded) {
            if (((GameEventCounterAdded) evt).Amount == 0) {
                return null;
            }
        }
        if (evt instanceof GameEventCounterRemoved) {
            if (((GameEventCounterRemoved) evt).Amount == 0) {
                return null;
            }
        }
        if (evt instanceof GameEventCardTapped) {
            return getSoundEffectForTapState(((GameEventCardTapped) evt).tapped);
        }
        if (evt instanceof GameEventDuelOutcome) {
            return getSoundEffectForDuelOutcome(((GameEventDuelOutcome) evt).result.getWinner() == Singletons.getControl().getLobby().getGuiPlayer());
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
            
            // Find mana ability if it is somewhere in tail

            String manaColors = sa.getManaPartRecursive().getOrigProduced();

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
    public String getScriptedSoundEffectName(GameEvent evt) {
        Card c = null;

        if (evt instanceof GameEventSpellResolved) {
            c = ((GameEventSpellResolved) evt).Source;
        } else if (evt instanceof GameEventLandPlayed) {
            c = ((GameEventLandPlayed) evt).Land;
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
