package forge.game.ability.effects;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.mutable.MutableBoolean;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;

import forge.GameCommand;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.GameObject;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardFactory;
import forge.game.card.CardUtil;
import forge.game.card.CardZoneTable;
import forge.game.card.CounterType;
import forge.game.card.TokenCreateTable;
import forge.game.card.token.TokenInfo;
import forge.game.event.GameEventCardStatsChanged;
import forge.game.player.Player;
import forge.game.replacement.ReplacementType;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.zone.ZoneType;

public abstract class TokenEffectBase extends SpellAbilityEffect {

    protected TokenCreateTable createTokenTable(Iterable<Player> players, String[] tokenScripts, final int finalAmount, final SpellAbility sa) {

        TokenCreateTable tokenTable = new TokenCreateTable();
        for (final Player owner : players) {
            for (String script : tokenScripts) {
                final Card result = TokenInfo.getProtoType(script, sa, owner);

                if (result == null) {
                    throw new RuntimeException("don't find Token for TokenScript: " + script);
                }
                // set owner
                result.setOwner(owner);
                tokenTable.put(owner, result, finalAmount);
            }
        }
        return tokenTable;
    }

    protected TokenCreateTable makeTokenTableInternal(Player owner, String script, final int finalAmount, final SpellAbility sa) {
        TokenCreateTable tokenTable = new TokenCreateTable();
        final Card result = TokenInfo.getProtoType(script, sa, owner, false);

        if (result == null) {
            throw new RuntimeException("don't find Token for TokenScript: " + script);
        }
        // set owner
        result.setOwner(owner);
        tokenTable.put(owner, result, finalAmount);

        return tokenTable;
    }

    protected TokenCreateTable makeTokenTable(Iterable<Player> players, String[] tokenScripts, final int finalAmount, final boolean clone,
            CardZoneTable triggerList, MutableBoolean combatChanged, final SpellAbility sa) {
        return makeTokenTable(createTokenTable(players, tokenScripts, finalAmount, sa), clone, triggerList, combatChanged, sa);
    }

    protected TokenCreateTable makeTokenTable(TokenCreateTable tokenTable, final boolean clone, CardZoneTable triggerList, MutableBoolean combatChanged, final SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Game game = host.getGame();
        long timestamp = game.getNextTimestamp();

        // support PlayerCollection for affected
        Set<Player> toRemove = Sets.newHashSet();
        for (Player p : tokenTable.rowKeySet()) {

            final Map<AbilityKey, Object> repParams = AbilityKey.mapFromAffected(p);
            repParams.put(AbilityKey.Token, tokenTable);
            repParams.put(AbilityKey.EffectOnly, true); // currently only effects can create tokens?

            switch (game.getReplacementHandler().run(ReplacementType.CreateToken, repParams)) {
            case NotReplaced:
                break;
            case Updated: {
                tokenTable = (TokenCreateTable) repParams.get(AbilityKey.Token);
                break;
            }
            default:
                toRemove.add(p);
            }
        }
        tokenTable.rowKeySet().removeAll(toRemove);

        final List<String> pumpKeywords = Lists.newArrayList();
        if (sa.hasParam("PumpKeywords")) {
            pumpKeywords.addAll(Arrays.asList(sa.getParam("PumpKeywords").split(" & ")));
        }
        List<Card> allTokens = Lists.newArrayList();

        CardCollectionView lastStateBattlefield = game.copyLastStateBattlefield();
        CardCollectionView lastStateGraveyard = game.copyLastStateGraveyard();

        Map<AbilityKey, Object> moveParams = Maps.newEnumMap(AbilityKey.class);
        moveParams.put(AbilityKey.LastStateBattlefield, lastStateBattlefield);
        moveParams.put(AbilityKey.LastStateGraveyard, lastStateGraveyard);

        for (final Table.Cell<Player, Card, Integer> c : tokenTable.cellSet()) {
            Card prototype = c.getColumnKey();
            Player creator = c.getRowKey();
            Player controller = prototype.getController();
            int cellAmount = c.getValue();
            for (int i = 0; i < cellAmount; i++) {
                Card tok = CardFactory.copyCard(prototype, true);
                // Crafty Cutpurse would change under which control it does enter,
                // but it shouldn't change who creates the token
                tok.setOwner(creator);
                if (creator != controller) {
                    tok.setController(controller, timestamp);
                }
                tok.setTimestamp(timestamp);
                tok.setToken(true);

                // do effect stuff with the token
                if (sa.hasParam("TokenTapped")) {
                    tok.setTapped(true);
                }

                if (!sa.hasParam("AttachAfter") && sa.hasParam("AttachedTo") && !attachTokenTo(tok, sa)) {
                    continue;
                }
                if (sa.hasParam("WithCounters")) {
                    String[] parse = sa.getParam("WithCounters").split("_");
                    tok.addEtbCounter(CounterType.getType(parse[0]), Integer.parseInt(parse[1]), creator);
                }

                if (sa.hasParam("WithCountersType")) {
                    CounterType cType = CounterType.getType(sa.getParam("WithCountersType"));
                    int cAmount = AbilityUtils.calculateAmount(host, sa.getParamOrDefault("WithCountersAmount", "1"), sa);
                    tok.addEtbCounter(cType, cAmount, creator);
                }

                if (sa.hasParam("AddTriggersFrom")) {
                    final List<Card> cards = AbilityUtils.getDefinedCards(host, sa.getParam("AddTriggersFrom"), sa);
                    for (final Card card : cards) {
                        for (final Trigger trig : card.getTriggers()) {
                            tok.addTrigger(trig.copy(tok, false));
                        }
                    }
                }

                if (clone) {
                    tok.setCopiedPermanent(prototype);
                }

                // Should this be catching the Card that's returned?
                Card moved = game.getAction().moveToPlay(tok, sa, moveParams);
                if (moved == null || moved.getZone() == null) {
                    // in case token can't enter the battlefield, it isn't created
                    triggerList.put(ZoneType.None, ZoneType.None, moved);
                    continue;
                }
                triggerList.put(ZoneType.None, moved.getZone().getZoneType(), moved);

                creator.addTokensCreatedThisTurn();

                if (clone) {
                    moved.setCloneOrigin(host);
                }
                if (!pumpKeywords.isEmpty()) {
                    moved.addChangedCardKeywords(pumpKeywords, Lists.newArrayList(), false, timestamp, 0);
                    addPumpUntil(sa, moved, timestamp);
                }

                if (sa.hasParam("AtEOTTrig")) {
                    addSelfTrigger(sa, sa.getParam("AtEOTTrig"), moved);
                }

                if (addToCombat(moved, tok.getController(), sa, "TokenAttacking", "TokenBlocking")) {
                    combatChanged.setTrue();
                }

                if (sa.hasParam("AttachAfter") && sa.hasParam("AttachedTo")) {
                    attachTokenTo(tok, sa);
                }

                moved.updateStateForView();

                if (sa.hasParam("RememberTokens")) {
                    host.addRemembered(moved);
                }
                if (sa.hasParam("ImprintTokens")) {
                    host.addImprintedCard(moved);
                }
                if (sa.hasParam("RememberSource")) {
                    moved.addRemembered(host);
                }
                if (sa.hasParam("TokenRemembered")) {
                    final String remembered = sa.getParam("TokenRemembered");
                    for (final Object o : AbilityUtils.getDefinedObjects(host, remembered, sa)) {
                        moved.addRemembered(o);
                    }
                }
                allTokens.add(moved);
            }
        }

        if (sa.hasParam("AtEOT")) {
            registerDelayedTrigger(sa, sa.getParam("AtEOT"), allTokens);
        }
        return tokenTable;
    }

    private boolean attachTokenTo(Card tok, SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Game game = host.getGame();

        GameObject aTo = Iterables.getFirst(
                AbilityUtils.getDefinedObjects(host, sa.getParam("AttachedTo"), sa), null);

        if (aTo instanceof GameEntity) {
            GameEntity ge = (GameEntity)aTo;
            // check what the token would be on the battlefield
            Card lki = CardUtil.getLKICopy(tok);

            lki.setLastKnownZone(tok.getController().getZone(ZoneType.Battlefield));

            // double freeze tracker, so it doesn't update view
            game.getTracker().freeze();
            CardCollection preList = new CardCollection(lki);
            game.getAction().checkStaticAbilities(false, Sets.newHashSet(lki), preList);

            boolean canAttach = lki.isAttachment();

            if (canAttach && !ge.canBeAttached(lki)) {
                canAttach = false;
            }

            // reset static abilities
            game.getAction().checkStaticAbilities(false);
            // clear delayed changes, this check should not have updated the view
            game.getTracker().clearDelayed();
            // need to unfreeze tracker
            game.getTracker().unfreeze();

            if (!canAttach) {
                // Token can't attach to it
                return false;
            }

            tok.attachToEntity(ge);
            return true;
        }
        // not a GameEntity, cant be attach
        return false;
    }

    protected void addPumpUntil(SpellAbility sa, final Card c, long timestamp) {
        if (!sa.hasParam("PumpDuration")) {
            return;
        }
        final String duration = sa.getParam("PumpDuration");
        final Card host = sa.getHostCard();
        final Game game = host.getGame();
        final GameCommand untilEOT = new GameCommand() {
            private static final long serialVersionUID = -42244224L;

            @Override
            public void run() {
                c.removeChangedCardKeywords(timestamp, 0);
                game.fireEvent(new GameEventCardStatsChanged(c));
            }
        };

        if ("UntilYourNextTurn".equals(duration)) {
            game.getCleanup().addUntil(sa.getActivatingPlayer(), untilEOT);
        } else {
            game.getEndOfTurn().addUntil(untilEOT);
        }
    }
}
