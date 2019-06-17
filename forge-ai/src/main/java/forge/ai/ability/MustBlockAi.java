package forge.ai.ability;

import com.google.common.base.Predicate;

import com.google.common.collect.Lists;
import forge.ai.*;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.ZoneType;

import java.util.List;

public class MustBlockAi extends SpellAbilityAi {

    @Override
    protected boolean canPlayAI(Player aiPlayer, SpellAbility sa) {
        final Card source = sa.getHostCard();
        final Game game = aiPlayer.getGame();
        final Combat combat = game.getCombat();
        final PhaseHandler ph = game.getPhaseHandler();
        final boolean onlyLethal = !"AllowNonLethal".equals(sa.getParam("AILogic"));

        if (combat == null || !combat.isAttacking(source)) {
            return false;
        } else if (AiCardMemory.isRememberedCard(aiPlayer, source, AiCardMemory.MemorySet.ACTIVATED_THIS_TURN)) {
            // The AI can meaningfully do it only to one creature per card yet, trying to do it to multiple cards
            // may result in overextending and losing the attacker
            return false;
        }

        final TargetRestrictions abTgt = sa.getTargetRestrictions();
        final List<Card> list = determineGoodBlockers(source, aiPlayer, combat.getDefenderPlayerByAttacker(source), sa, onlyLethal,false);

        if (!list.isEmpty()) {
            final Card blocker = ComputerUtilCard.getBestCreatureAI(list);
            if (blocker == null) {
                return false;
            }
            sa.getTargets().add(blocker);
            AiCardMemory.rememberCard(aiPlayer, source, AiCardMemory.MemorySet.ACTIVATED_THIS_TURN);
            return true;
        }

        return false;
    }

    @Override
    public boolean chkAIDrawback(SpellAbility sa, Player aiPlayer) {
        if (sa.hasParam("DefinedAttacker")) {
            // The AI can't handle "target creature blocks another target creature" abilities yet
            return false;
        }

        // Otherwise it's a standard targeted "target creature blocks CARDNAME" ability, so use the main canPlayAI code path
        return canPlayAI(aiPlayer, sa);
    }

    @Override
    protected boolean doTriggerAINoCost(final Player ai, SpellAbility sa, boolean mandatory) {
        final Card source = sa.getHostCard();
        final TargetRestrictions abTgt = sa.getTargetRestrictions();

        // only use on creatures that can attack
        if (!ai.getGame().getPhaseHandler().getPhase().isBefore(PhaseType.MAIN2)) {
            return false;
        }

        Card attacker = null;
        if (sa.hasParam("DefinedAttacker")) {
            final List<Card> cards = AbilityUtils.getDefinedCards(sa.getHostCard(), sa.getParam("DefinedAttacker"), sa);
            if (cards.isEmpty()) {
                return false;
            }

            attacker = cards.get(0);
        }

        if (attacker == null) {
            attacker = source;
        }

        final Card definedAttacker = attacker;

        boolean chance = false;

        if (abTgt != null) {
            final List<Card> list = determineGoodBlockers(definedAttacker, ai, ai.getWeakestOpponent(), sa, true,true);
            if (list.isEmpty()) {
                return false;
            }
            final Card blocker = ComputerUtilCard.getBestCreatureAI(list);
            if (blocker == null) {
                return false;
            }

            if (source.hasKeyword(Keyword.PROVOKE) && blocker.isTapped()) {
                // Don't provoke if the attack is potentially lethal
                Combat combat = ai.getGame().getCombat();
                if (combat != null) {
                    Player defender = combat.getDefenderPlayerByAttacker(source);
                    if (defender != null && combat.getAttackingPlayer().equals(ai)
                            && defender.canLoseLife() && !defender.cantLoseForZeroOrLessLife()
                            && ComputerUtilCombat.lifeThatWouldRemain(defender, combat) <= 0) {
                        return false;
                    }
                }
            }

            sa.getTargets().add(blocker);
            chance = true;
        } else {
            return false;
        }

        return chance;
    }

    private List<Card> determineGoodBlockers(final Card attacker, final Player ai, Player defender, SpellAbility sa,
            final boolean onlyLethal, final boolean testTapped) {
        final Card source = sa.getHostCard();
        final TargetRestrictions abTgt = sa.getTargetRestrictions();

        List<Card> list = Lists.newArrayList();
        list = CardLists.filter(defender.getCardsIn(ZoneType.Battlefield), CardPredicates.Presets.CREATURES);
        list = CardLists.getTargetableCards(list, sa);
        list = CardLists.getValidCards(list, abTgt.getValidTgts(), source.getController(), source, sa);
        list = CardLists.filter(list, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                boolean tapped = c.isTapped();
                if (testTapped) {
                    c.setTapped(false);
                }
                if (!CombatUtil.canBlock(attacker, c)) {
                    return false;
                }
                if (ComputerUtilCombat.canDestroyAttacker(ai, attacker, c, null, false)) {
                    return false;
                }
                if (onlyLethal && !ComputerUtilCombat.canDestroyBlocker(ai, c, attacker, null, false)) {
                    return false;
                }
                if (testTapped) {
                    c.setTapped(tapped);
                }
                return true;
            }

        });

        return list;
    }
}
