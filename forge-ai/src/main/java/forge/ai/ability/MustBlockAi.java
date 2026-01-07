package forge.ai.ability;

import com.google.common.collect.Iterables;
import forge.ai.*;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.card.CardUtil;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

import java.util.List;
import java.util.Map;

public class MustBlockAi extends SpellAbilityAi {

    @Override
    protected AiAbilityDecision canPlay(Player aiPlayer, SpellAbility sa) {
        final Card source = sa.getHostCard();
        final Game game = aiPlayer.getGame();
        final Combat combat = game.getCombat();
        final boolean onlyLethal = !"AllowNonLethal".equals(sa.getParam("AILogic"));

        if (combat == null || !combat.isAttacking(source)) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        if (source.getAbilityActivatedThisTurn().getActivators(sa).contains(aiPlayer)) {
            // The AI can meaningfully do it only to one creature per card yet, trying to do it to multiple cards
            // may result in overextending and losing the attacker
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }

        final List<Card> list = determineGoodBlockers(source, aiPlayer, combat.getDefenderPlayerByAttacker(source), sa, onlyLethal,false);

        if (!list.isEmpty()) {
            final Card blocker = ComputerUtilCard.getBestCreatureAI(list);
            sa.getTargets().add(blocker);
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }

        return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
    }

    @Override
    public AiAbilityDecision chkDrawback(Player aiPlayer, SpellAbility sa) {
        if (sa.hasParam("DefinedAttacker")) {
            // The AI can't handle "target creature blocks another target creature" abilities yet
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }

        // Otherwise it's a standard targeted "target creature blocks CARDNAME" ability, so use the main canPlayAI code path
        return canPlay(aiPlayer, sa);
    }

    @Override
    protected AiAbilityDecision doTriggerNoCost(final Player ai, SpellAbility sa, boolean mandatory) {
        final Card source = sa.getHostCard();

        Card attacker = source;
        if (sa.hasParam("DefinedAttacker")) {
            final List<Card> cards = AbilityUtils.getDefinedCards(source, sa.getParam("DefinedAttacker"), sa);
            if (cards.isEmpty()) {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }

            attacker = cards.get(0);
        }

        boolean chance = false;

        if (sa.usesTargeting()) {
            List<Card> list = determineGoodBlockers(attacker, ai, ai.getWeakestOpponent(), sa, true, true);
            if (list.isEmpty() && mandatory) {
                list = CardUtil.getValidCardsToTarget(sa);
            }
            final Card blocker = ComputerUtilCard.getBestCreatureAI(list);
            if (blocker == null) {
                if (sa.isTargetNumberValid()) {
                    return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                } else {
                    return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
                }
            }

            if (!mandatory && sa.isKeyword(Keyword.PROVOKE) && blocker.isTapped()) {
                // Don't provoke if the attack is potentially lethal
                Combat combat = ai.getGame().getCombat();
                if (combat != null) {
                    Player defender = combat.getDefenderPlayerByAttacker(source);
                    if (defender != null && combat.getAttackingPlayer().equals(ai)
                            && defender.canLoseLife() && !defender.cantLoseForZeroOrLessLife()
                            && ComputerUtilCombat.lifeThatWouldRemain(defender, combat) <= 0) {
                        return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                    }
                }
            }

            sa.getTargets().add(blocker);
            chance = true;
        } else if (sa.hasParam("Choices")) {
            // currently choice is attacked player
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        } else {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }

        if (chance) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }

        return new AiAbilityDecision(0, AiPlayDecision.StopRunawayActivations);
    }

    private List<Card> determineBlockerFromList(final Card attacker, final Player ai, Iterable<Card> options, SpellAbility sa,
            final boolean onlyLethal, final boolean testTapped) {
        List<Card> list = CardLists.filter(options, c -> {
            if (!CombatUtil.canBlock(attacker, c, testTapped)) {
                return false;
            }
            if (ComputerUtilCombat.canDestroyAttacker(ai, attacker, c, null, false)) {
                return false;
            }
            if (onlyLethal && !ComputerUtilCombat.canDestroyBlocker(ai, c, attacker, null, false)) {
                return false;
            }
            return true;
        });

        return list;
    }

    private List<Card> determineGoodBlockers(final Card attacker, final Player ai, Player defender, SpellAbility sa,
            final boolean onlyLethal, final boolean testTapped) {
        List<Card> list = defender.getCreaturesInPlay();

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
            return Iterables.getFirst(better, null);
        }

        return Iterables.getFirst(options, null);
    }
}
