package forge.sound;

import forge.LobbyPlayer;
import forge.game.card.Card;
import forge.game.event.*;
import forge.game.spellability.AbilityManaPart;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.gui.events.IUiEventVisitor;
import forge.gui.events.UiEventAttackerDeclared;
import forge.gui.events.UiEventBlockerAssigned;
import forge.gui.events.UiEventNextGameDecision;
import forge.util.TextUtil;

import java.util.Collection;
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
        SoundEffectType resultSound = null;
        if(zoneEventMode == EventValueChangeType.Added && zoneTo == ZoneType.Battlefield) {
            if(card.isLand()) {
                resultSound = getLandSound(card);
            }
        }
        return resultSound;
    }

    private SoundEffectType getLandSound(Card land) {
        SoundEffectType resultSound = null;

        // if there's a specific effect for this particular card, play it and
        // we're done.
        if (hasSpecificCardEffect(land)) {
            resultSound = SoundEffectType.ScriptedEffect;
        } else {
            // I want to get all real colors this land can produce - no interest in colorless or devoid
            StringBuilder fullManaColors = new StringBuilder();
            for (final SpellAbility sa : land.getManaAbilities()) {
                for (AbilityManaPart mp : sa.getAllManaParts()) {
                    String currManaColor = mp.getOrigProduced();
                    if(!"C".equals(currManaColor)) {
                        fullManaColors.append(currManaColor);
                    }
                }

            }
            // No interest if "colors together" or "alternative colors" - only interested in colors themselves
            fullManaColors = new StringBuilder(TextUtil.fastReplace(fullManaColors.toString()," ", ""));

            int fullManaColorsLength = fullManaColors.length();

            if(fullManaColorsLength >= 3) {
                // three color land
                fullManaColors = new StringBuilder(fullManaColors.substring(0, 3));
                if (fullManaColors.toString().contains("W") && fullManaColors.toString().contains("U") && fullManaColors.toString().contains("B") && SoundSystem.instance.hasResource(SoundEffectType.WhiteBlueBlackLand)) {
                    resultSound = SoundEffectType.WhiteBlueBlackLand;
                } else if (fullManaColors.toString().contains("W") && fullManaColors.toString().contains("G") && fullManaColors.toString().contains("U") && SoundSystem.instance.hasResource(SoundEffectType.WhiteGreenBlueLand)) {
                    resultSound = SoundEffectType.WhiteGreenBlueLand;
                } else if (fullManaColors.toString().contains("W") && fullManaColors.toString().contains("R") && fullManaColors.toString().contains("B") && SoundSystem.instance.hasResource(SoundEffectType.WhiteRedBlackLand)) {
                    resultSound = SoundEffectType.WhiteRedBlackLand;
                } else if (fullManaColors.toString().contains("B") && fullManaColors.toString().contains("W") && fullManaColors.toString().contains("G") && SoundSystem.instance.hasResource(SoundEffectType.BlackWhiteGreenLand)) {
                    resultSound = SoundEffectType.BlackWhiteGreenLand;
                } else if (fullManaColors.toString().contains("B") && fullManaColors.toString().contains("R") && fullManaColors.toString().contains("G") && SoundSystem.instance.hasResource(SoundEffectType.BlackRedGreenLand)) {
                    resultSound = SoundEffectType.BlackRedGreenLand;
                } else if (fullManaColors.toString().contains("U") && fullManaColors.toString().contains("B") && fullManaColors.toString().contains("R") && SoundSystem.instance.hasResource(SoundEffectType.BlueBlackRedLand)) {
                    resultSound = SoundEffectType.BlueBlackRedLand;
                } else if (fullManaColors.toString().contains("G") && fullManaColors.toString().contains("U") && fullManaColors.toString().contains("R") && SoundSystem.instance.hasResource(SoundEffectType.GreenBlueRedLand)) {
                    resultSound = SoundEffectType.GreenBlueRedLand;
                } else if (fullManaColors.toString().contains("G") && fullManaColors.toString().contains("B") && fullManaColors.toString().contains("U") && SoundSystem.instance.hasResource(SoundEffectType.GreenBlackBlueLand)) {
                    resultSound = SoundEffectType.GreenBlackBlueLand;
                } else if (fullManaColors.toString().contains("G") && fullManaColors.toString().contains("R") && fullManaColors.toString().contains("W")  && SoundSystem.instance.hasResource(SoundEffectType.GreenRedWhiteLand)) {
                    resultSound = SoundEffectType.GreenRedWhiteLand;
                } else if (fullManaColors.toString().contains("R") && fullManaColors.toString().contains("U") && fullManaColors.toString().contains("W")  && SoundSystem.instance.hasResource(SoundEffectType.RedBlueWhiteLand)) {
                    resultSound = SoundEffectType.RedBlueWhiteLand;
                }
            }

            if(resultSound == null && fullManaColorsLength >= 2) {
                // three color land without sounds installed, or two color land
                // lets try
                fullManaColors = new StringBuilder(fullManaColors.substring(0, 2));
                if (fullManaColors.toString().contains("W") && (fullManaColors.toString().contains("U")) && SoundSystem.instance.hasResource(SoundEffectType.WhiteBlueLand)) {
                    resultSound = SoundEffectType.WhiteBlueLand;
                } else if (fullManaColors.toString().contains("W") && (fullManaColors.toString().contains("G")) && SoundSystem.instance.hasResource(SoundEffectType.WhiteGreenLand)) {
                    resultSound = SoundEffectType.WhiteGreenLand;
                } else if (fullManaColors.toString().contains("W") && (fullManaColors.toString().contains("R")) && SoundSystem.instance.hasResource(SoundEffectType.WhiteRedLand)) {
                    resultSound = SoundEffectType.WhiteRedLand;
                } else if (fullManaColors.toString().contains("B") && (fullManaColors.toString().contains("W")) && SoundSystem.instance.hasResource(SoundEffectType.BlackWhiteLand)) {
                    resultSound = SoundEffectType.BlackWhiteLand;
                } else if (fullManaColors.toString().contains("B") && (fullManaColors.toString().contains("R")) && SoundSystem.instance.hasResource(SoundEffectType.BlackRedLand)) {
                    resultSound = SoundEffectType.BlackRedLand;
                } else if (fullManaColors.toString().contains("U") && (fullManaColors.toString().contains("B")) && SoundSystem.instance.hasResource(SoundEffectType.BlueBlackLand)) {
                    resultSound = SoundEffectType.BlueBlackLand;
                } else if (fullManaColors.toString().contains("G") && (fullManaColors.toString().contains("U")) && SoundSystem.instance.hasResource(SoundEffectType.GreenBlueLand)) {
                    resultSound = SoundEffectType.GreenBlueLand;
                } else if (fullManaColors.toString().contains("G") && (fullManaColors.toString().contains("B")) && SoundSystem.instance.hasResource(SoundEffectType.GreenBlackLand)) {
                    resultSound = SoundEffectType.GreenBlackLand;
                } else if (fullManaColors.toString().contains("G") && (fullManaColors.toString().contains("R")) && SoundSystem.instance.hasResource(SoundEffectType.GreenRedLand)) {
                    resultSound = SoundEffectType.GreenRedLand;
                } else if (fullManaColors.toString().contains("R") && (fullManaColors.toString().contains("U")) && SoundSystem.instance.hasResource(SoundEffectType.RedBlueLand)) {
                    resultSound = SoundEffectType.RedBlueLand;
                }
            }

            if(resultSound == null) {
                // multicolor land without sounds installed, or single mana land, or colorless/devoid land
                // in case of multicolor, lets take only the 1st color of the list, it sure has sound
                if(fullManaColorsLength >= 2) {
                    fullManaColors = new StringBuilder(fullManaColors.substring(0, 1));
                }
                if (fullManaColors.toString().contains("B")) {
                    resultSound = SoundEffectType.BlackLand;
                } else if (fullManaColors.toString().contains("U")) {
                    resultSound = SoundEffectType.BlueLand;
                } else if (fullManaColors.toString().contains("G")) {
                    resultSound = SoundEffectType.GreenLand;
                } else if (fullManaColors.toString().contains("R")) {
                    resultSound = SoundEffectType.RedLand;
                } else if (fullManaColors.toString().contains("W")) {
                    resultSound = SoundEffectType.WhiteLand;
                } else {
                    resultSound = SoundEffectType.OtherLand;
                }
            }
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
