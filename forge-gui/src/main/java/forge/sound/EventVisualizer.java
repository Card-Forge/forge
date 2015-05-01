package forge.sound;

import java.util.Collection;

import forge.LobbyPlayer;
import forge.events.IUiEventVisitor;
import forge.events.UiEventAttackerDeclared;
import forge.events.UiEventBlockerAssigned;
import forge.events.UiEventNextGameDecision;
import forge.game.card.Card;
import forge.game.event.GameEvent;
import forge.game.event.GameEventBlockersDeclared;
import forge.game.event.GameEventCardAttachment;
import forge.game.event.GameEventCardChangeZone;
import forge.game.event.GameEventCardCounters;
import forge.game.event.GameEventCardDamaged;
import forge.game.event.GameEventCardDestroyed;
import forge.game.event.GameEventCardPhased;
import forge.game.event.GameEventCardRegenerated;
import forge.game.event.GameEventCardSacrificed;
import forge.game.event.GameEventCardTapped;
import forge.game.event.GameEventFlipCoin;
import forge.game.event.GameEventGameOutcome;
import forge.game.event.GameEventLandPlayed;
import forge.game.event.GameEventPlayerLivesChanged;
import forge.game.event.GameEventPlayerPoisoned;
import forge.game.event.GameEventShuffle;
import forge.game.event.GameEventSpellResolved;
import forge.game.event.GameEventTokenCreated;
import forge.game.event.GameEventTurnEnded;
import forge.game.event.IGameEventVisitor;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.maps.MapOfLists;

/**
 * This class is in charge of converting any forge.game.event.Event to a SoundEffectType.
 *
 */
public class EventVisualizer extends IGameEventVisitor.Base<SoundEffectType> implements IUiEventVisitor<SoundEffectType> {

    final LobbyPlayer player;
    public EventVisualizer(final LobbyPlayer lobbyPlayer) {
        this.player = lobbyPlayer;
    }

    @Override
    public SoundEffectType visit(final GameEventCardDamaged event) { return SoundEffectType.Damage; }
    @Override
    public SoundEffectType visit(final GameEventCardDestroyed event) { return SoundEffectType.Destroy; }
    @Override
    public SoundEffectType visit(final GameEventCardAttachment event) { return SoundEffectType.Equip; }
    @Override
    public SoundEffectType visit(final GameEventCardChangeZone event) {
        final ZoneType from = event.from == null ? null : event.from.getZoneType();
        final ZoneType to = event.to.getZoneType();
        if( from == ZoneType.Library && to == ZoneType.Hand) {
            return SoundEffectType.Draw;
        }
        if( from == ZoneType.Hand && (to == ZoneType.Graveyard || to == ZoneType.Library) ) {
            return SoundEffectType.Discard;
        }

        return to == ZoneType.Exile ? SoundEffectType.Exile : null;
    }
    @Override
    public SoundEffectType visit(final GameEventCardRegenerated event) { return SoundEffectType.Regen; }
    @Override
    public SoundEffectType visit(final GameEventCardSacrificed event) { return SoundEffectType.Sacrifice; }
    @Override
    public SoundEffectType visit(final GameEventCardCounters event) { return event.newValue > event.oldValue ? SoundEffectType.AddCounter : event.newValue < event.oldValue ? SoundEffectType.RemoveCounter : null; }
    @Override
    public SoundEffectType visit(final GameEventTurnEnded event) { return SoundEffectType.EndOfTurn; }
    @Override
    public SoundEffectType visit(final GameEventFlipCoin event) { return SoundEffectType.FlipCoin; }
    @Override
    public SoundEffectType visit(final GameEventPlayerLivesChanged event) { return event.newLives < event.oldLives ? SoundEffectType.LifeLoss : SoundEffectType.LifeGain; }
    @Override
    public SoundEffectType visit(final GameEventPlayerPoisoned event) { return SoundEffectType.Poison; }
    @Override
    public SoundEffectType visit(final GameEventShuffle event) { return SoundEffectType.Shuffle; }
    @Override
    public SoundEffectType visit(final GameEventTokenCreated event) { return SoundEffectType.Token; }
    @Override
    public SoundEffectType visit(final GameEventBlockersDeclared event) {
        final boolean isLocalHuman = event.defendingPlayer.getLobbyPlayer() == player;
        if (isLocalHuman) {
            return null; // already played sounds in interactive mode
        }

        for (final MapOfLists<Card, Card> ab : event.blockers.values()) {
            for(final Collection<Card> bb : ab.values()) {
                if ( !bb.isEmpty() ) {
                    // hasAnyBlocker = true;
                    return SoundEffectType.Block;
                }
            }
        }
        return null;
    }

    /**
     * Plays the sound corresponding to the outcome of the duel.
     */
    @Override
    public SoundEffectType visit(final GameEventGameOutcome event) {
        final boolean humanWonTheDuel = event.result.getWinningLobbyPlayer() == player;
        return humanWonTheDuel ? SoundEffectType.WinDuel : SoundEffectType.LoseDuel;
    }

    /**
     * Plays the sound corresponding to the card type/color when the card
     * ability resolves on the stack.
     */
    @Override
    public SoundEffectType visit(final GameEventSpellResolved evt) {
        if (evt.spell == null ) {
            return null;
        }

        final Card source = evt.spell.getHostCard();
        if (evt.spell.isSpell()) {
            // if there's a specific effect for this particular card, play it and
            // we're done.
            if (hasSpecificCardEffect(source)) {
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
    @Override
    public SoundEffectType visit(final GameEventCardTapped event) {
        return event.tapped ? SoundEffectType.Tap : SoundEffectType.Untap;
    }

    /**
     * Plays the sound corresponding to the land type when the land is played.
     *
     * @param land the land card that was played
     * @return the sound effect type
     */
    @Override
    public SoundEffectType visit(final GameEventLandPlayed event) {
        if (event.land == null) {
            return null;
        }

        // if there's a specific effect for this particular card, play it and
        // we're done.
        if (hasSpecificCardEffect(event.land)) {
            return SoundEffectType.ScriptedEffect;
        }

        for (final SpellAbility sa : event.land.getManaAbilities()) {
            final String manaColors = sa.getManaPartRecursive().getOrigProduced();

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
    private static boolean hasSpecificCardEffect(final Card c) {
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
    public String getScriptedSoundEffectName(final GameEvent evt) {
        Card c = null;

        if (evt instanceof GameEventSpellResolved) {
            c = ((GameEventSpellResolved) evt).spell.getHostCard();
        } else if (evt instanceof GameEventLandPlayed) {
            c = ((GameEventLandPlayed) evt).land;
        }

        return c != null ? c.getSVar("SoundEffect") : "";
    }


    @Override
    public SoundEffectType visit(final UiEventBlockerAssigned event) {
        return event.attackerBeingBlocked == null ? null : SoundEffectType.Block;
    }
    @Override
    public SoundEffectType visit(final UiEventAttackerDeclared event) {
        return null;
    }
    @Override
    public SoundEffectType visit(final UiEventNextGameDecision event) {
        return null;
    }
    @Override
    public SoundEffectType visit(final GameEventCardPhased event) {
        return SoundEffectType.Phasing;
    }
}
