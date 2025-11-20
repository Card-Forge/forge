package forge.ai.ability;

import forge.ai.*;
import forge.card.MagicColor;
import forge.game.card.*;
import forge.game.combat.Combat;
import forge.game.cost.Cost;
import forge.game.cost.CostDamage;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.collect.FCollectionView;

import java.util.function.Predicate;

public class DestroyAllAi extends SpellAbilityAi {

    private static final Predicate<Card> predicate = c -> !(c.hasKeyword(Keyword.INDESTRUCTIBLE) || c.getCounters(CounterEnumType.SHIELD) > 0 || c.hasSVar("SacMe"));

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#doTriggerAINoCost(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility, boolean)
     */
    @Override
    protected AiAbilityDecision doTriggerNoCost(Player ai, SpellAbility sa, boolean mandatory) {
        if (mandatory) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }

        return doMassRemovalLogic(ai, sa);
    }

    @Override
    public AiAbilityDecision chkDrawback(Player aiPlayer, SpellAbility sa) {
        return doMassRemovalLogic(aiPlayer, sa);
    }

    @Override
    protected AiAbilityDecision checkApiLogic(final Player ai, SpellAbility sa) {
        // AI needs to be expanded, since this function can be pretty complex
        // based on what the expected targets could be
        final String aiLogic = sa.getParamOrDefault("AILogic", "");

        if ("FellTheMighty".equals(aiLogic)) {
            return SpecialCardAi.FellTheMighty.consider(ai, sa);
        }

        return doMassRemovalLogic(ai, sa);
    }

    public static AiAbilityDecision doMassRemovalLogic(Player ai, SpellAbility sa) {
        final Card source = sa.getHostCard();
        final String logic = sa.getParamOrDefault("AILogic", "");

        // if we hit the whole board, the other opponents who are not the reason to cast this probably still suffer a bit too
        final int CREATURE_EVAL_THRESHOLD = 200 / (!sa.usesTargeting() ? ai.getOpponents().size() : 1);

        if (logic.equals("Always")) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }

        String valid = sa.getParamOrDefault("ValidCards", "");

        if (valid.contains("X") && sa.getSVar("X").equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            final int xPay = ComputerUtilCost.getMaxXValue(sa, ai, sa.isTrigger());
            sa.setXManaCostPaid(xPay);
            valid = valid.replace("X", Integer.toString(xPay));
        }

        // TODO should probably sort results when targeted to use on biggest threat instead of first match
        for (Player opponent: ai.getOpponents()) {
            CardCollection opplist = CardLists.getValidCards(opponent.getCardsIn(ZoneType.Battlefield), valid, source.getController(), source, sa);
            CardCollection ailist = CardLists.getValidCards(ai.getCardsIn(ZoneType.Battlefield), valid, source.getController(), source, sa);

            opplist = CardLists.filter(opplist, predicate);
            ailist = CardLists.filter(ailist, predicate);
            if (opplist.isEmpty()) {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }

            if (sa.usesTargeting()) {
                sa.resetTargets();
                if (sa.canTarget(opponent)) {
                    sa.getTargets().add(opponent);
                    ailist.clear();
                } else {
                    return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                }
            }

            // Special handling for Raiding Party
            if (logic.equals("RaidingParty")) {
                int numAiCanSave = Math.min(CardLists.count(ai.getCreaturesInPlay(), CardPredicates.isColor(MagicColor.WHITE).and(CardPredicates.UNTAPPED)) * 2, ailist.size());
                int numOppsCanSave = Math.min(CardLists.count(ai.getOpponents().getCreaturesInPlay(), CardPredicates.isColor(MagicColor.WHITE).and(CardPredicates.UNTAPPED)) * 2, opplist.size());

                if (numOppsCanSave < opplist.size() && (ailist.size() - numAiCanSave < opplist.size() - numOppsCanSave)) {
                    return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                } else if (numAiCanSave < ailist.size() && (opplist.size() - numOppsCanSave < ailist.size() - numAiCanSave)) {
                    return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                }
            }

            // If effect is destroying creatures and AI is about to lose, activate effect anyway no matter what!
            if ((!CardLists.getType(opplist, "Creature").isEmpty()) && (ai.getGame().getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS))
                    && (ai.getGame().getCombat() != null && ComputerUtilCombat.lifeInSeriousDanger(ai, ai.getGame().getCombat()))) {
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            }

            // If effect is destroying creatures and AI is about to get low on life, activate effect anyway if difference in lost permanents not very much
            if ((!CardLists.getType(opplist, "Creature").isEmpty()) && (ai.getGame().getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS))
                    && (ai.getGame().getCombat() != null && ComputerUtilCombat.lifeInDanger(ai, ai.getGame().getCombat()))
                    && ((ComputerUtilCard.evaluatePermanentList(ailist) - 6) >= ComputerUtilCard.evaluatePermanentList(opplist))) {
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            }

            // if only creatures are affected evaluate both lists and pass only if human creatures are more valuable
            if (CardLists.getNotType(opplist, "Creature").isEmpty() && CardLists.getNotType(ailist, "Creature").isEmpty()) {
                if (ComputerUtilCard.evaluateCreatureList(ailist) + CREATURE_EVAL_THRESHOLD < ComputerUtilCard.evaluateCreatureList(opplist)) {
                    return new AiAbilityDecision(100, AiPlayDecision.WillPlay);

                }

                if (ai.getGame().getPhaseHandler().getPhase().isBefore(PhaseType.MAIN2)) {
                    return new AiAbilityDecision(0, AiPlayDecision.WaitForMain2);
                }

                // test whether the human can kill the ai next turn
                Combat combat = new Combat(opponent);
                boolean containsAttacker = false;
                for (Card att : opponent.getCreaturesInPlay()) {
                    if (ComputerUtilCombat.canAttackNextTurn(att, ai)) {
                        combat.addAttacker(att, ai);
                        containsAttacker = containsAttacker || opplist.contains(att);
                    }
                }
                if (!containsAttacker) {
                    return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                }
                AiBlockController block = new AiBlockController(ai, false);
                block.assignBlockersForCombat(combat);

                if (ComputerUtilCombat.lifeInSeriousDanger(ai, combat)) {
                    return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                }
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            } // only lands involved
            else if (CardLists.getNotType(opplist, "Land").isEmpty() && CardLists.getNotType(ailist, "Land").isEmpty()) {
                if (ai.isCardInPlay("Crucible of Worlds") && !opponent.isCardInPlay("Crucible of Worlds")) {
                    // TODO Should care about any land recursion, not just Crucible of Worlds
                    return new AiAbilityDecision(100, AiPlayDecision.WillPlay);

                }
                // evaluate the situation with creatures on the battlefield separately, as that's where the AI typically makes mistakes
                CardCollection aiCreatures = ai.getCreaturesInPlay();
                CardCollection oppCreatures = opponent.getCreaturesInPlay();
                if (!oppCreatures.isEmpty()) {
                    if (ComputerUtilCard.evaluateCreatureList(aiCreatures) < ComputerUtilCard.evaluateCreatureList(oppCreatures) + CREATURE_EVAL_THRESHOLD) {
                        return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                    }
                }
                // check if the AI would lose more lands than the opponent would
                if (ComputerUtilCard.evaluatePermanentList(ailist) > ComputerUtilCard.evaluatePermanentList(opplist) + 1) {
                    return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                }
            } // otherwise evaluate both lists by CMC and pass only if human permanents are more valuable
            else if ((ComputerUtilCard.evaluatePermanentList(ailist) + 3) >= ComputerUtilCard.evaluatePermanentList(opplist)) {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);

        }
        return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
    }
    

    @Override
    public boolean willPayUnlessCost(Player payer, SpellAbility sa, Cost cost, boolean alreadyPaid, FCollectionView<Player> payers) {
        final Card source = sa.getHostCard();
        if (payers.size() > 1) {
            if (alreadyPaid) {
                return false;
            }
        }
        String valid = sa.getParamOrDefault("ValidCards", "");

        CardCollection ailist = CardLists.getValidCards(payer.getCardsIn(ZoneType.Battlefield), valid, source.getController(), source, sa);
        ailist = CardLists.filter(ailist, predicate);

        if (ailist.isEmpty()) {
            return false;
        }

        if (cost.hasSpecificCostType(CostDamage.class)) {
            if (!payer.canLoseLife()) {
                return false;
            }
            final CostDamage pay = cost.getCostPartByType(CostDamage.class);
            int realDamage = ComputerUtilCombat.predictDamageTo(payer, pay.getAbilityAmount(sa), source, false);
            if (realDamage > payer.getLife()) {
                return false;
            }
            if (realDamage > ailist.size() * 3) { // three life points per one creature
                return false;
            }
        }

        return super.willPayUnlessCost(payer, sa, cost, alreadyPaid, payers);
    }
}