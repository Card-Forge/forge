package forge.ai.ability;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import forge.ai.*;
import forge.card.MagicColor;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.combat.Combat;
import forge.game.cost.Cost;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class DestroyAllAi extends SpellAbilityAi {

    private static final Predicate<Card> predicate = new Predicate<Card>() {
        @Override
        public boolean apply(final Card c) {
            return !(c.hasKeyword(Keyword.INDESTRUCTIBLE) || c.getSVar("SacMe").length() > 0);
        }
    };

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#doTriggerAINoCost(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility, boolean)
     */
    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {
        if (mandatory) {
            return true;
        }

        return doMassRemovalLogic(ai, sa);
    }

    @Override
    public boolean chkAIDrawback(SpellAbility sa, Player aiPlayer) {
        return doMassRemovalLogic(aiPlayer, sa);
    }

    @Override
    protected boolean canPlayAI(final Player ai, SpellAbility sa) {
        // AI needs to be expanded, since this function can be pretty complex
        // based on what the expected targets could be
        final Cost abCost = sa.getPayCosts();
        final Card source = sa.getHostCard();

        if (abCost != null) {
            // AI currently disabled for some costs
            if (!ComputerUtilCost.checkLifeCost(ai, abCost, source, 4, sa)) {
                return false;
            }
        }

        // prevent run-away activations - first time will always return true
        if (ComputerUtil.preventRunAwayActivations(sa)) {
            return false;
        }

        final String aiLogic = sa.getParamOrDefault("AILogic", "");

        if ("FellTheMighty".equals(aiLogic)) {
            return SpecialCardAi.FellTheMighty.consider(ai, sa);
        }

        return doMassRemovalLogic(ai, sa);
    }

    public static boolean doMassRemovalLogic(Player ai, SpellAbility sa) {
        final Card source = sa.getHostCard();
        final String logic = sa.getParamOrDefault("AILogic", "");

        // if we hit the whole board, the other opponents who are not the reason to cast this probably still suffer a bit too
        final int CREATURE_EVAL_THRESHOLD = 200 / (!sa.usesTargeting() ? ai.getOpponents().size() : 1);

        if (logic.equals("Always")) {
            return true; // e.g. Tetzimoc, Primal Death, where we want to cast the permanent even if the removal trigger does nothing
        }

        String valid = sa.getParamOrDefault("ValidCards", "");

        if (valid.contains("X") && sa.getSVar("X").equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            final int xPay = ComputerUtilCost.getMaxXValue(sa, ai);
            sa.setXManaCostPaid(xPay);
            valid = valid.replace("X", Integer.toString(xPay));
        }

        // TODO should probably sort results when targeted to use on biggest threat instead of first match
        for (Player opponent: ai.getOpponents()) {
            CardCollection opplist = CardLists.getValidCards(opponent.getCardsIn(ZoneType.Battlefield), valid.split(","), source.getController(), source, sa);
            CardCollection ailist = CardLists.getValidCards(ai.getCardsIn(ZoneType.Battlefield), valid.split(","), source.getController(), source, sa);

            opplist = CardLists.filter(opplist, predicate);
            ailist = CardLists.filter(ailist, predicate);
            if (opplist.isEmpty()) {
                return false;
            }

            if (sa.usesTargeting()) {
                sa.resetTargets();
                if (sa.canTarget(opponent)) {
                    sa.getTargets().add(opponent);
                    ailist.clear();
                } else {
                    return false;
                }
            }

            // Special handling for Raiding Party
            if (logic.equals("RaidingParty")) {
                int numAiCanSave = Math.min(CardLists.filter(ai.getCreaturesInPlay(), Predicates.and(CardPredicates.isColor(MagicColor.WHITE), CardPredicates.Presets.UNTAPPED)).size() * 2, ailist.size());
                int numOppsCanSave = Math.min(CardLists.filter(ai.getOpponents().getCreaturesInPlay(), Predicates.and(CardPredicates.isColor(MagicColor.WHITE), CardPredicates.Presets.UNTAPPED)).size() * 2, opplist.size());

                return numOppsCanSave < opplist.size() && (ailist.size() - numAiCanSave < opplist.size() - numOppsCanSave);
            }

            // If effect is destroying creatures and AI is about to lose, activate effect anyway no matter what!
            if ((!CardLists.getType(opplist, "Creature").isEmpty()) && (ai.getGame().getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS))
                    && (ai.getGame().getCombat() != null && ComputerUtilCombat.lifeInSeriousDanger(ai, ai.getGame().getCombat()))) {
                return true;
            }

            // If effect is destroying creatures and AI is about to get low on life, activate effect anyway if difference in lost permanents not very much
            if ((!CardLists.getType(opplist, "Creature").isEmpty()) && (ai.getGame().getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS))
                    && (ai.getGame().getCombat() != null && ComputerUtilCombat.lifeInDanger(ai, ai.getGame().getCombat()))
                    && ((ComputerUtilCard.evaluatePermanentList(ailist) - 6) >= ComputerUtilCard.evaluatePermanentList(opplist))) {
                return true;
            }

            // if only creatures are affected evaluate both lists and pass only if human creatures are more valuable
            if (CardLists.getNotType(opplist, "Creature").isEmpty() && CardLists.getNotType(ailist, "Creature").isEmpty()) {
                if (ComputerUtilCard.evaluateCreatureList(ailist) + CREATURE_EVAL_THRESHOLD < ComputerUtilCard.evaluateCreatureList(opplist)) {
                    return true;
                }

                if (ai.getGame().getPhaseHandler().getPhase().isBefore(PhaseType.MAIN2)) {
                    return false;
                }

                // test whether the human can kill the ai next turn
                Combat combat = new Combat(opponent);
                boolean containsAttacker = false;
                for (Card att : opponent.getCreaturesInPlay()) {
                    if (ComputerUtilCombat.canAttackNextTurn(att, ai)) {
                        combat.addAttacker(att, ai);
                        containsAttacker = containsAttacker | opplist.contains(att);
                    }
                }
                if (!containsAttacker) {
                    return false;
                }
                AiBlockController block = new AiBlockController(ai);
                block.assignBlockersForCombat(combat);

                if (ComputerUtilCombat.lifeInSeriousDanger(ai, combat)) {
                    return true;
                }
                return false;
            } // only lands involved
            else if (CardLists.getNotType(opplist, "Land").isEmpty() && CardLists.getNotType(ailist, "Land").isEmpty()) {
                if (ai.isCardInPlay("Crucible of Worlds") && !opponent.isCardInPlay("Crucible of Worlds")) {
                    return true;
                }
                // evaluate the situation with creatures on the battlefield separately, as that's where the AI typically makes mistakes
                CardCollection aiCreatures = ai.getCreaturesInPlay();
                CardCollection oppCreatures = opponent.getCreaturesInPlay();
                if (!oppCreatures.isEmpty()) {
                    if (ComputerUtilCard.evaluateCreatureList(aiCreatures) < ComputerUtilCard.evaluateCreatureList(oppCreatures) + CREATURE_EVAL_THRESHOLD) {
                        return false;
                    }
                }
                // check if the AI would lose more lands than the opponent would
                if (ComputerUtilCard.evaluatePermanentList(ailist) > ComputerUtilCard.evaluatePermanentList(opplist) + 1) {
                    return false;
                }
            } // otherwise evaluate both lists by CMC and pass only if human permanents are more valuable
            else if ((ComputerUtilCard.evaluatePermanentList(ailist) + 3) >= ComputerUtilCard.evaluatePermanentList(opplist)) {
                return false;
            }
            return true;
        }
        return false;
    }
    
}