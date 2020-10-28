package forge.game.ability.effects;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.mutable.MutableBoolean;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import forge.GameCommand;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.GameObject;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardUtil;
import forge.game.card.CardZoneTable;
import forge.game.card.token.TokenInfo;
import forge.game.combat.Combat;
import forge.game.event.GameEventCardStatsChanged;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.CardTranslation;
import forge.util.Localizer;
import forge.util.collect.FCollectionView;

public abstract class TokenEffectBase extends SpellAbilityEffect {

    protected List<Card> makeTokens(final Card prototype, final Player creator, final SpellAbility sa, int finalAmount,
            boolean applyMultiplier, boolean clone, CardZoneTable triggerList, MutableBoolean combatChanged) {
        final Card host = sa.getHostCard();
        final Game game = host.getGame();
        final long timestamp = game.getNextTimestamp();

        final List<String> pumpKeywords = Lists.newArrayList();
        if (sa.hasParam("PumpKeywords")) {
            pumpKeywords.addAll(Arrays.asList(sa.getParam("PumpKeywords").split(" & ")));
        }

        List<Card> allTokens = Lists.newArrayList();
        for (Card tok : TokenInfo.makeTokensFromPrototype(prototype, creator, finalAmount, applyMultiplier)) {
            if (sa.hasParam("TokenTapped")) {
                tok.setTapped(true);
            }

            if (!sa.hasParam("AttachAfter") && sa.hasParam("AttachedTo") && !attachTokenTo(tok, sa)) {
                continue;
            }

            if (clone) {
                tok.setCopiedPermanent(prototype);
            }

            // Should this be catching the Card that's returned?
            Card c = game.getAction().moveToPlay(tok, sa);
            if (c == null || c.getZone() == null) {
                // in case token can't enter the battlefield, it isn't created
                continue;
            }
            triggerList.put(ZoneType.None, c.getZone().getZoneType(), c);

            creator.addTokensCreatedThisTurn();

            if (clone) {
                c.setCloneOrigin(host);
            }
            if (!pumpKeywords.isEmpty()) {
                c.addChangedCardKeywords(pumpKeywords, Lists.newArrayList(), false, false, timestamp);
                addPumpUntil(sa, c, timestamp);
            }

            if (sa.hasParam("AtEOTTrig")) {
                addSelfTrigger(sa, sa.getParam("AtEOTTrig"), c);
            }

            if (addTokenToCombat(game, c, tok.getController(), sa, host)) {
                combatChanged.setTrue();
            }

            if (sa.hasParam("AttachAfter") && sa.hasParam("AttachedTo")) {
                attachTokenTo(tok, sa);
            }

            c.updateStateForView();

            if (sa.hasParam("RememberTokens")) {
                game.getCardState(sa.getHostCard()).addRemembered(c);
            }
            if (sa.hasParam("ImprintTokens")) {
                game.getCardState(sa.getHostCard()).addImprintedCard(c);
            }
            if (sa.hasParam("RememberSource")) {
                game.getCardState(c).addRemembered(host);
            }
            if (sa.hasParam("TokenRemembered")) {
                final Card token = game.getCardState(c);
                final String remembered = sa.getParam("TokenRemembered");
                for (final Object o : AbilityUtils.getDefinedObjects(host, remembered, sa)) {
                    token.addRemembered(o);
                }
            }
            allTokens.add(c);
        }

        if (sa.hasParam("AtEOT")) {
            registerDelayedTrigger(sa, sa.getParam("AtEOT"), allTokens);
        }
        return allTokens;
    }

    private boolean addTokenToCombat(Game game, Card c, Player controller, SpellAbility sa, Card host) {
        if (!game.getPhaseHandler().inCombat()) {
            return false;
        }
        boolean combatChanged = false;
        final Combat combat = game.getCombat();

        if (sa.hasParam("TokenAttacking") && combat.getAttackingPlayer().equals(controller)) {
            String attacking = sa.getParam("TokenAttacking");
            GameEntity defender = null;
            FCollectionView<GameEntity> defs = null;
            if ("True".equalsIgnoreCase(attacking)) {
                defs = combat.getDefenders();
            } else if (sa.hasParam("ChoosePlayerOrPlaneswalker")) {
                Player defendingPlayer = Iterables.getFirst(AbilityUtils.getDefinedPlayers(host, attacking, sa), null);
                if (defendingPlayer != null) {
                    defs = game.getCombat().getDefendersControlledBy(defendingPlayer);
                }
            } else {
                defs = AbilityUtils.getDefinedEntities(host, attacking, sa);
            }

            if (defs != null) {
                Map<String, Object> params = Maps.newHashMap();
                params.put("Attacker", c);
                defender = controller.getController().chooseSingleEntityForEffect(defs, sa,
                        Localizer.getInstance().getMessage("lblChooseDefenderToAttackWithCard", CardTranslation.getTranslatedName(c.getName())), false, params);
            }

            if (defender != null) {
                combat.addAttacker(c, defender);
                combatChanged = true;
            }
        }
        if (sa.hasParam("TokenBlocking")) {
            final Card attacker = Iterables.getFirst(AbilityUtils.getDefinedCards(host, sa.getParam("TokenBlocking"), sa), null);
            if (attacker != null) {
                final boolean wasBlocked = combat.isBlocked(attacker);
                combat.addBlocker(attacker, c);
                combat.orderAttackersForDamageAssignment(c);

                // Run triggers for new blocker and add it to damage assignment order
                if (!wasBlocked) {
                    combat.setBlocked(attacker, true);
                    combat.addBlockerToDamageAssignmentOrder(attacker, c);
                }
                combatChanged = true;
            }
        }
        return combatChanged;
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
        } else {
            // not a GameEntity, cant be attach
            return false;
        }
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
                c.removeChangedCardKeywords(timestamp);
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
