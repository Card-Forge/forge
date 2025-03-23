package forge.ai.ability;

import java.util.List;
import java.util.function.Predicate;

import forge.ai.AiAttackController;
import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilCard;
import forge.ai.SpellAbilityAi;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.combat.CombatUtil;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class DetainAi extends SpellAbilityAi {

    Predicate<Card> CREATURE_OR_TAP_ABILITY = c -> {
        if (c.isCreature()) {
            return true;
        }

        for (final SpellAbility sa : c.getSpellAbilities()) {
            if (sa.isAbility() && sa.getPayCosts().hasTapCost()) {
                return true;
            }
        }
        return false;
    };

    protected boolean prefTargeting(final Player ai, final Card source, final SpellAbility sa, final boolean mandatory) {
        final Game game = ai.getGame();
        CardCollection list = CardLists.getTargetableCards(ai.getOpponents().getCardsIn(ZoneType.Battlefield), sa);
        list = CardLists.filter(list, CREATURE_OR_TAP_ABILITY);

        // Filter AI-specific targets if provided
        list = ComputerUtil.filterAITgts(sa, ai, list, true);


        if (list.isEmpty()) {
            return false;
        }

        while (sa.canAddMoreTarget()) {
            Card choice = null;
            if (list.isEmpty()) {
                if (!sa.isMinTargetChosen() || sa.isZeroTargets()) {
                    if (!mandatory) {
                        sa.resetTargets();
                    }
                    return false;

                }
            }

            PhaseHandler phase = game.getPhaseHandler();
            final Player opp = AiAttackController.choosePreferredDefenderPlayer(ai);
            Card primeTarget = ComputerUtil.getKilledByTargeting(sa, list);
            if (primeTarget != null) {
                choice = primeTarget;
            } else if (phase.isPlayerTurn(ai) && phase.getPhase().isBefore(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
                // Tap creatures possible blockers before combat during AI's turn.
                List<Card> attackers;
                if (phase.getPhase().isAfter(PhaseType.COMBAT_DECLARE_ATTACKERS)) {
                    //Combat has already started
                    attackers = game.getCombat().getAttackers();
                } else {
                    attackers = CardLists.filter(ai.getCreaturesInPlay(), c -> CombatUtil.canAttack(c, opp));
                    attackers.remove(source);
                }
                Predicate<Card> findBlockers = CardPredicates.possibleBlockerForAtLeastOne(attackers);
                List<Card> creatureList = CardLists.filter(list, findBlockers);

                // TODO check if own creature would be forced to attack and we want to keep it alive

                if (!attackers.isEmpty() && !creatureList.isEmpty()) {
                    choice = ComputerUtilCard.getBestCreatureAI(creatureList);
                } else if (sa.isTrigger() || ComputerUtil.castSpellInMain1(ai, sa)) {
                    choice = ComputerUtilCard.getMostExpensivePermanentAI(list);
                }
            } else if (phase.isPlayerTurn(opp)
                    && phase.getPhase().isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS)) {
                // Tap creatures possible blockers before combat during AI's turn.
                if (list.anyMatch(CardPredicates.CREATURES)) {
                    List<Card> creatureList = CardLists.filter(list, c -> c.isCreature() && CombatUtil.canAttack(c, opp));
                    choice = ComputerUtilCard.getBestCreatureAI(creatureList);
                } else { // no creatures available
                    choice = ComputerUtilCard.getMostExpensivePermanentAI(list);
                }
            } else {
                choice = ComputerUtilCard.getMostExpensivePermanentAI(list);
            }

            if (choice == null) { // can't find anything left
                if (!sa.isMinTargetChosen() || sa.isZeroTargets()) {
                    if (!mandatory) {
                        sa.resetTargets();
                    }
                    return false;
                } else {
                    if (!ComputerUtil.shouldCastLessThanMax(ai, source)) {
                        return false;
                    }
                    break;
                }
            }

            list.remove(choice);
            sa.getTargets().add(choice);
        }

        return true;
    }
}
