package forge.ai.ability;

import java.util.List;
import java.util.Map;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.ai.AiCardMemory;
import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilCombat;
import forge.ai.SpellAbilityAi;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class MustBlockAi extends SpellAbilityAi {

    @Override
    protected boolean canPlayAI(Player aiPlayer, SpellAbility sa) {
        final Card source = sa.getHostCard();
        final Game game = aiPlayer.getGame();
        final Combat combat = game.getCombat();
        final boolean onlyLethal = !"AllowNonLethal".equals(sa.getParam("AILogic"));

        if (combat == null || !combat.isAttacking(source)) {
            return false;
        } else if (AiCardMemory.isRememberedCard(aiPlayer, source, AiCardMemory.MemorySet.ACTIVATED_THIS_TURN)) {
            // The AI can meaningfully do it only to one creature per card yet, trying to do it to multiple cards
            // may result in overextending and losing the attacker
            return false;
        }

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

        // only use on creatures that can attack
        if (!ai.getGame().getPhaseHandler().getPhase().isBefore(PhaseType.MAIN2)) {
            return false;
        }

        Card attacker = null;
        if (sa.hasParam("DefinedAttacker")) {
            final List<Card> cards = AbilityUtils.getDefinedCards(source, sa.getParam("DefinedAttacker"), sa);
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

        if (sa.usesTargeting()) {
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
        } else if (sa.hasParam("Choices")) {
            // currently choice is attacked player
            return true;
        } else {
            return false;
        }

        return chance;
    }

    private List<Card> determineBlockerFromList(final Card attacker, final Player ai, Iterable<Card> options, SpellAbility sa,
            final boolean onlyLethal, final boolean testTapped) {
        List<Card> list = CardLists.filter(options, new Predicate<Card>() {
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

    private List<Card> determineGoodBlockers(final Card attacker, final Player ai, Player defender, SpellAbility sa,
            final boolean onlyLethal, final boolean testTapped) {
        List<Card> list = Lists.newArrayList();
        list = CardLists.filter(defender.getCardsIn(ZoneType.Battlefield), CardPredicates.Presets.CREATURES);

        if (sa.usesTargeting()) {
            list = CardLists.getTargetableCards(list, sa);
        }
        return determineBlockerFromList(attacker, ai, list, sa, onlyLethal, testTapped);
    }

    @Override
    protected Card chooseSingleCard(Player ai, SpellAbility sa, Iterable<Card> options, boolean isOptional,
            Player targetedPlayer, Map<String, Object> params) {
        final Card host = sa.getHostCard();

        Card attacker = host;

        if (sa.hasParam("DefinedAttacker")) {
            List<Card> attackers = AbilityUtils.getDefinedCards(host, sa.getParam("DefinedAttacker"), sa);
            attacker = Iterables.getFirst(attackers, null);
        }
        if (attacker == null) {
            return Iterables.getFirst(options, null);
        }

        List<Card> better = determineBlockerFromList(attacker, ai, options, sa, false, false);

        if (!better.isEmpty()) {
            return Iterables.getFirst(options, null);
        }

        return Iterables.getFirst(options, null);
    }
}
