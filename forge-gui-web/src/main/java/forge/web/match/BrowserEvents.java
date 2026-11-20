package forge.web.match;

import com.google.common.collect.Multimap;
import forge.game.GameEntityView;
import forge.game.card.CardView;
import forge.game.event.GameEvent;
import forge.game.event.GameEventAttackersDeclared;
import forge.game.event.GameEventBlockersDeclared;
import forge.game.event.GameEventCardAttachment;
import forge.game.event.GameEventCardChangeZone;
import forge.game.event.GameEventCardCounters;
import forge.game.event.GameEventCardDamaged;
import forge.game.event.GameEventFlipCoin;
import forge.game.event.GameEventGameStarted;
import forge.game.event.GameEventPlayerCounters;
import forge.game.event.GameEventPlayerDamaged;
import forge.game.event.GameEventPlayerLivesChanged;
import forge.game.event.GameEventShuffle;
import forge.game.event.GameEventSpellAbilityCast;
import forge.game.event.GameEventSpellResolved;
import forge.game.player.PlayerView;
import forge.game.zone.ZoneType;
import forge.game.zone.ZoneView;
import forge.trackable.TrackableTypes;
import forge.trackable.Tracker;
import forge.web.protocol.ToBrowser.Attack;
import forge.web.protocol.ToBrowser.AttackersDeclared;
import forge.web.protocol.ToBrowser.Block;
import forge.web.protocol.ToBrowser.BlockersDeclared;
import forge.web.protocol.ToBrowser.CardAttached;
import forge.web.protocol.ToBrowser.CardCounters;
import forge.web.protocol.ToBrowser.CardDamaged;
import forge.web.protocol.ToBrowser.CardMoved;
import forge.web.protocol.ToBrowser.CoinTossed;
import forge.web.protocol.ToBrowser.GameStarted;
import forge.web.protocol.ToBrowser.LifeChanged;
import forge.web.protocol.ToBrowser.Place;
import forge.web.protocol.ToBrowser.PlayerCounters;
import forge.web.protocol.ToBrowser.PlayerDamaged;
import forge.web.protocol.ToBrowser.Ref;
import forge.web.protocol.ToBrowser.Shuffled;
import forge.web.protocol.ToBrowser.StackAdded;
import forge.web.protocol.ToBrowser.StackResolved;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** The game events the browser can animate. Each is sent with the state change it caused. */
final class BrowserEvents {
    private BrowserEvents() {
    }

    /** Returns null for an event the browser does not animate. The state update already shows its result. */
    static Record forwarded(final GameEvent event, final Tracker tracker) {
        if (event instanceof GameEventCardChangeZone e && e.card() != null) {
            PlayerView caster = null;
            if (e.to() != null && e.to().zoneType() == ZoneType.Stack) {
                caster = e.card().getController();
                // A card the game copied as it moved has no controller, so the controller is read from the tracker's copy
                final CardView tracked = caster == null && tracker != null ? tracker.getObj(TrackableTypes.CardViewType, e.card().getId()) : null;
                caster = tracked == null ? caster : tracked.getController();
            }
            return new CardMoved(Ref.card(e.card().getId()), place(e.from()), place(e.to()),
                    caster == null ? null : Ref.player(caster.getId()));
        }
        if (event instanceof GameEventCardDamaged e && e.card() != null) {
            return new CardDamaged(Ref.card(e.card().getId()), e.source() == null ? null : Ref.card(e.source().getId()), e.amount());
        }
        if (event instanceof GameEventPlayerDamaged e && e.target() != null) {
            return new PlayerDamaged(Ref.player(e.target().getId()), e.source() == null ? null : Ref.card(e.source().getId()),
                    e.amount(), e.combat());
        }
        if (event instanceof GameEventAttackersDeclared e && e.player() != null) {
            final List<Attack> attacks = new ArrayList<>();
            for (final Map.Entry<GameEntityView, CardView> attack : e.attackersMap().entries()) {
                attacks.add(new Attack(Ref.card(attack.getValue().getId()), entity(attack.getKey())));
            }
            return new AttackersDeclared(Ref.player(e.player().getId()), attacks);
        }
        if (event instanceof GameEventBlockersDeclared e && e.defendingPlayer() != null) {
            final List<Block> blocks = new ArrayList<>();
            for (final Multimap<CardView, CardView> blockersByAttacker : e.blockers().values()) {
                for (final Map.Entry<CardView, CardView> block : blockersByAttacker.entries()) {
                    // The engine lists an unblocked attacker as blocked by itself
                    if (!block.getKey().equals(block.getValue())) {
                        blocks.add(new Block(Ref.card(block.getValue().getId()), Ref.card(block.getKey().getId())));
                    }
                }
            }
            return new BlockersDeclared(Ref.player(e.defendingPlayer().getId()), blocks);
        }
        if (event instanceof GameEventSpellAbilityCast e && e.si() != null) {
            return new StackAdded(player(e.si().getActivatingPlayer()));
        }
        if (event instanceof GameEventSpellResolved e && e.spell() != null) {
            final CardView host = e.spell().getHostCard();
            return new StackResolved(host == null ? null : player(host.getController()),
                    host == null ? null : Ref.card(host.getId()), e.hasFizzled());
        }
        if (event instanceof GameEventPlayerLivesChanged e && e.player() != null) {
            return new LifeChanged(Ref.player(e.player().getId()), e.oldLives(), e.newLives());
        }
        // The engine also fires this with no counter type when it only clears a player's counters. That case is skipped
        if (event instanceof GameEventPlayerCounters e && e.receiver() != null && e.type() != null) {
            return new PlayerCounters(Ref.player(e.receiver().getId()));
        }
        if (event instanceof GameEventCardCounters e && e.card() != null) {
            return new CardCounters(Ref.card(e.card().getId()));
        }
        if (event instanceof GameEventCardAttachment e && e.equipment() != null) {
            return new CardAttached(Ref.card(e.equipment().getId()), entity(e.newTarget()));
        }
        if (event instanceof GameEventShuffle e && e.player() != null) {
            return new Shuffled(Ref.player(e.player().getId()));
        }
        if (event instanceof GameEventGameStarted e && e.firstTurn() != null) {
            return new GameStarted(Ref.player(e.firstTurn().getId()));
        }
        if (event instanceof GameEventFlipCoin e && e.startingToss() && e.flipper() != null) {
            return new CoinTossed(Ref.player(e.flipper().getId()));
        }
        return null;
    }

    private static Ref player(final PlayerView player) {
        return player == null ? null : Ref.player(player.getId());
    }

    private static Ref entity(final GameEntityView entity) {
        return entity instanceof CardView c ? Ref.card(c.getId()) : entity instanceof PlayerView p ? Ref.player(p.getId()) : null;
    }

    static Place place(final ZoneView zone) {
        return zone == null || zone.zoneType() == null ? null
                : new Place(zone.zoneType(), zone.player() == null ? null : Ref.player(zone.player().getId()));
    }
}
