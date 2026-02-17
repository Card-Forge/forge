package forge.sound;

import forge.LobbyPlayer;
import forge.game.card.Card;
import forge.game.card.CardView.CardStateView;
import forge.game.event.*;
import forge.game.zone.ZoneType;
import forge.gui.events.IUiEventVisitor;
import forge.gui.events.UiEventAttackerDeclared;
import forge.gui.events.UiEventBlockerAssigned;
import forge.gui.events.UiEventNextGameDecision;
import forge.util.TextUtil;

import java.util.Objects;

import com.google.common.collect.Multimap;

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
    public SoundEffectType visit(GameEventGameStarted event) {
        return SoundEffectType.StartDuel;
    }

    @Override
    public SoundEffectType visit(final GameEventCardDamaged event) { return SoundEffectType.Damage; }
    @Override
    public SoundEffectType visit(final GameEventCardDestroyed event) { return SoundEffectType.Destroy; }
    @Override
    public SoundEffectType visit(final GameEventCardAttachment event) { return SoundEffectType.Equip; }
    @Override
    public SoundEffectType visit(final GameEventCardChangeZone event) {
        final ZoneType from = event.from() == null ? null : event.from().getZoneType();
        final ZoneType to = event.to().getZoneType();
        if( from == ZoneType.Library && to == ZoneType.Hand) {
            return SoundEffectType.Draw;
        }
        if( from == ZoneType.Hand && (to == ZoneType.Graveyard || to == ZoneType.Library) ) {
            return SoundEffectType.Discard;
        }

        return to == ZoneType.Exile ? SoundEffectType.Exile : null;
    }

    @Override
    public SoundEffectType visit(GameEventCardStatsChanged event) {
        return event.transform() ? SoundEffectType.FlipCard : null ;
    }

    @Override
    public SoundEffectType visit(final GameEventCardRegenerated event) { return SoundEffectType.Regen; }
    @Override
    public SoundEffectType visit(final GameEventCardSacrificed event) { return SoundEffectType.Sacrifice; }
    @Override
    public SoundEffectType visit(final GameEventCardCounters event) { return event.newValue() > event.oldValue() ? SoundEffectType.AddCounter : event.newValue() < event.oldValue() ? SoundEffectType.RemoveCounter : null; }
    @Override
    public SoundEffectType visit(final GameEventTurnEnded event) { return SoundEffectType.EndOfTurn; }
    @Override
    public SoundEffectType visit(final GameEventFlipCoin event) { return SoundEffectType.FlipCoin; }
    @Override
    public SoundEffectType visit(final GameEventRollDie event) { return SoundEffectType.RollDie; }
    @Override
    public SoundEffectType visit(final GameEventPlayerLivesChanged event) { return event.newLives() < event.oldLives() ? SoundEffectType.LifeLoss : SoundEffectType.LifeGain; }
    @Override
    public SoundEffectType visit(final GameEventPlayerShardsChanged event) { return SoundEffectType.TakeShard; }
    @Override
    public SoundEffectType visit(final GameEventManaBurn event) { return SoundEffectType.ManaBurn; }
    @Override
    public SoundEffectType visit(final GameEventPlayerPoisoned event) { return SoundEffectType.Poison; }
    @Override
    public SoundEffectType visit(final GameEventShuffle event) { return SoundEffectType.Shuffle; }
    @Override
    public SoundEffectType visit(final GameEventSpeedChanged event) { return event.newValue() > event.oldValue() ? SoundEffectType.SpeedUp : null; }
    @Override
    public SoundEffectType visit(final GameEventTokenCreated event) { return SoundEffectType.Token; }
    @Override
    public SoundEffectType visit(final GameEventSprocketUpdate event) {
        if(event.oldSprocket() == event.sprocket() || event.sprocket() <= 0)
            return null;
        return SoundEffectType.Sprocket;
    }
    @Override
    public SoundEffectType visit(final GameEventDayTimeChanged event) {
        return event.daytime() ? SoundEffectType.Daytime : SoundEffectType.Nighttime;
    }
    @Override
    public SoundEffectType visit(final GameEventBlockersDeclared event) {
        final boolean isLocalHuman = Objects.equals(event.defendingPlayer().getLobbyPlayer(), player);
        if (isLocalHuman) {
            return null; // already played sounds in interactive mode
        }

        if (event.blockers().values().stream().noneMatch(Multimap::isEmpty)) {
            // hasAnyBlocker = true;
            return SoundEffectType.Block;
        }
        return null;
    }

    /**
     * Plays the sound corresponding to the outcome of the duel.
     */
    @Override
    public SoundEffectType visit(final GameEventGameOutcome event) {
        final boolean humanWonTheDuel = Objects.equals(event.result().getWinningLobbyPlayer(), player);
        return humanWonTheDuel ? SoundEffectType.WinDuel : SoundEffectType.LoseDuel;
    }

    /**
     * Plays the sound corresponding to the card type/color when the card
     * ability resolves on the stack.
     */
    @Override
    public SoundEffectType visit(final GameEventSpellResolved evt) {
        if (evt.spell() == null ) {
            return null;
        }

        final Card source = evt.spell().getHostCard();
        if (evt.spell().isSpell()) {
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
     * @param event if true, the "tap" sound is played; otherwise, the
     * "untap" sound is played
     * @return the sound effect type
     */
    @Override
    public SoundEffectType visit(final GameEventCardTapped event) {
        return event.tapped() ? SoundEffectType.Tap : SoundEffectType.Untap;
    }

    /**
     * Plays the sound corresponding to the land type when the land is played.
     *
     * @param event the land card that was played
     * @return the sound effect type
     */
    @Override
    public SoundEffectType visit(final GameEventLandPlayed event) {
        SoundEffectType resultSound = null;
        return resultSound;
    }


    @Override
    public SoundEffectType visit(GameEventZone event) {
        Card card = event.card();
        ZoneType zoneTo = event.zoneType();
        EventValueChangeType zoneEventMode = event.mode();
        if (zoneEventMode != EventValueChangeType.Added || zoneTo != ZoneType.Battlefield || !card.isLand()) {
            return null;
        }
        if (hasSpecificCardEffect(card)) {
            return SoundEffectType.ScriptedEffect;
        }
        CardStateView state = card.getView().getCurrentState();
        SoundEffectType resultSound = switch(state.origProduceMana()) {
            case W -> SoundEffectType.WhiteLand;
            case U -> SoundEffectType.BlueLand;
            case B -> SoundEffectType.BlackLand;
            case R -> SoundEffectType.RedLand;
            case G -> SoundEffectType.GreenLand;

            case WU -> SoundEffectType.WhiteBlueLand;
            case GW -> SoundEffectType.WhiteGreenLand;
            case RW -> SoundEffectType.WhiteRedLand;
            case WB -> SoundEffectType.BlackWhiteLand;
            case BR -> SoundEffectType.BlackRedLand;
            case UB -> SoundEffectType.BlueBlackLand;
            case GU -> SoundEffectType.GreenBlueLand;
            case BG -> SoundEffectType.GreenBlackLand;
            case RG -> SoundEffectType.GreenRedLand;
            case UR -> SoundEffectType.RedBlueLand;

            case WUB -> SoundEffectType.WhiteBlueBlackLand;
            case GWU -> SoundEffectType.WhiteGreenBlueLand;
            case RWB -> SoundEffectType.WhiteRedBlackLand;
            case WBG -> SoundEffectType.BlackWhiteGreenLand;
            case BRG -> SoundEffectType.BlackRedGreenLand;
            case UBR -> SoundEffectType.BlueBlackRedLand;
            case GUR -> SoundEffectType.GreenBlueRedLand;
            case BGU -> SoundEffectType.GreenBlackBlueLand;
            case RGW -> SoundEffectType.GreenRedWhiteLand;
            case URW -> SoundEffectType.RedBlueWhiteLand;

            default -> null;
        };
        if (resultSound == null || state.origProduceAnyMana() || !SoundSystem.instance.hasResource(resultSound)) {
            resultSound = SoundEffectType.OtherLand;
        }
        return resultSound;
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
            if (c.hasSVar("SoundEffect")) {
                effect = c.getSVar("SoundEffect");
            } else {
                effect = TextUtil.fastReplace(TextUtil.fastReplace(
                        TextUtil.fastReplace(c.getName(), ",", ""),
                        " ", "_"), "'", "").toLowerCase();

            }
        }

        // Only proceed if the file actually exists
        return SoundSystem.instance.getSoundResource(effect) != null;
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

        if (evt instanceof GameEventSpellResolved evSpell) {
            c = evSpell.spell().getHostCard();
        } else if (evt instanceof GameEventZone evZone) {
            if (evZone.zoneType() == ZoneType.Battlefield && evZone.mode() == EventValueChangeType.Added && evZone.card().isLand()) {
                c = evZone.card(); // assuming a land is played or otherwise put on the battlefield
            }
        }

        if (c == null) {
            return "";
        } else {
            if (c.hasSVar("SoundEffect")) {
                return c.getSVar("SoundEffect");
            } else {
                return TextUtil.fastReplace(TextUtil.fastReplace(
                        TextUtil.fastReplace(c.getName(), ",", ""),
                        " ", "_"), "'", "").toLowerCase();
            }
        }
    }


    @Override
    public SoundEffectType visit(final UiEventBlockerAssigned event) {
        return event.attackerBeingBlocked() == null ? null : SoundEffectType.Block;
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

    @Override
    public SoundEffectType visit(final GameEventSnapshotRestored gameEventSnapshotRestored) {
        SoundSystem.instance.setIgnorePlayRequests(gameEventSnapshotRestored.start());

        // How often do people cancel/undo is a rewind noise too repetitive?
        return SoundEffectType.SnapshotRestored;
    }
}
