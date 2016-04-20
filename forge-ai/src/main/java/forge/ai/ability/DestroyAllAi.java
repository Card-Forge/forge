package forge.ai.ability;

import com.google.common.base.Predicate;

import forge.ai.*;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.cost.Cost;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.game.combat.Combat;

public class DestroyAllAi extends SpellAbilityAi {

    private static final Predicate<Card> predicate = new Predicate<Card>() {
        @Override
        public boolean apply(final Card c) {
            return !(c.hasKeyword("Indestructible") || c.getSVar("SacMe").length() > 0);
        }
    };

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#doTriggerAINoCost(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility, boolean)
     */
    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {
        final Card source = sa.getHostCard();
        String valid = "";
        if (mandatory) {
            return true;
        }
        if (sa.hasParam("ValidCards")) {
            valid = sa.getParam("ValidCards");
        }
        CardCollection humanlist = CardLists.getValidCards(ai.getOpponent().getCardsIn(ZoneType.Battlefield),
                valid.split(","), source.getController(), source, sa);
        CardCollection computerlist = CardLists.getValidCards(ai.getCardsIn(ZoneType.Battlefield), valid.split(","),
                source.getController(), source, sa);
        
        if (sa.usesTargeting()) {
            sa.resetTargets();
            if (sa.canTarget(ai.getOpponent())) {
                sa.getTargets().add(ai.getOpponent());
                computerlist.clear();
            } else {
                return false;
            }
        }

        humanlist = CardLists.filter(humanlist, predicate);
        computerlist = CardLists.filter(computerlist, predicate);
        if (humanlist.isEmpty() && !computerlist.isEmpty()) {
            return false;
        }

        // if only creatures are affected evaluate both lists and pass only if
        // human creatures are more valuable
        if ((CardLists.getNotType(humanlist, "Creature").size() == 0) && (CardLists.getNotType(computerlist, "Creature").size() == 0)) {
            if (ComputerUtilCard.evaluateCreatureList(computerlist) >= ComputerUtilCard.evaluateCreatureList(humanlist)
                    && !computerlist.isEmpty()) {
                return false;
            }
        } // otherwise evaluate both lists by CMC and pass only if human
          // permanents are more valuable
        else if (ComputerUtilCard.evaluatePermanentList(computerlist) >= ComputerUtilCard.evaluatePermanentList(humanlist)) {
            return false;
        }
        return true;
    }

    @Override
    public boolean chkAIDrawback(SpellAbility sa, Player aiPlayer) {
        //TODO: Check for bad outcome
        return true;
    }

    @Override
    protected boolean canPlayAI(final Player ai, SpellAbility sa) {
        // AI needs to be expanded, since this function can be pretty complex
        // based on what the expected targets could be
        final Cost abCost = sa.getPayCosts();
        final Card source = sa.getHostCard();
        String valid = "";

        if (sa.hasParam("ValidCards")) {
            valid = sa.getParam("ValidCards");
        }

        if (valid.contains("X") && source.getSVar("X").equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            final int xPay = ComputerUtilMana.determineLeftoverMana(sa, ai);
            source.setSVar("PayX", Integer.toString(xPay));
            valid = valid.replace("X", Integer.toString(xPay));
        }

        CardCollection humanlist = CardLists.getValidCards(ai.getOpponent().getCardsIn(ZoneType.Battlefield),
                valid.split(","), source.getController(), source, sa);
        CardCollection computerlist = CardLists.getValidCards(ai.getCardsIn(ZoneType.Battlefield), valid.split(","),
                source.getController(), source, sa);
        if (sa.usesTargeting()) {
            sa.resetTargets();
            sa.getTargets().add(ai.getOpponent());
            computerlist.clear();
        }

        humanlist = CardLists.filter(humanlist, predicate);
        computerlist = CardLists.filter(computerlist, predicate);

        if (abCost != null) {
            // AI currently disabled for some costs

            if (!ComputerUtilCost.checkLifeCost(ai, abCost, source, 4, null)) {
                return false;
            }
        }

        // prevent run-away activations - first time will always return true
        if (ComputerUtil.preventRunAwayActivations(sa)) {
            return false;
        }
        
        if (humanlist.isEmpty()) {
        	return false;
        }

        // if only creatures are affected evaluate both lists and pass only if
        // human creatures are more valuable
        if (CardLists.getNotType(humanlist, "Creature").isEmpty() && CardLists.getNotType(computerlist, "Creature").isEmpty()) {
            if (ComputerUtilCard.evaluateCreatureList(computerlist) + 200 < ComputerUtilCard.evaluateCreatureList(humanlist)) {
                return true;
            }
            
            if (ai.getGame().getPhaseHandler().getPhase().isBefore(PhaseType.MAIN2)) {
            	return false;
            }
            
            // test whether the human can kill the ai next turn
            Combat combat = new Combat(ai.getOpponent());
            boolean containsAttacker = false;
            for (Card att : ai.getOpponent().getCreaturesInPlay()) {
            	if (ComputerUtilCombat.canAttackNextTurn(att, ai)) {
            		combat.addAttacker(att, ai);
            		containsAttacker = containsAttacker | humanlist.contains(att);
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
        else if (CardLists.getNotType(humanlist, "Land").isEmpty() && CardLists.getNotType(computerlist, "Land").isEmpty()) {
        	if (ai.isCardInPlay("Crucible of Worlds") && !ai.getOpponent().isCardInPlay("Crucible of Worlds") && !humanlist.isEmpty()) {
        		return true;
        	}
            if (ComputerUtilCard.evaluatePermanentList(computerlist) + 1 >= ComputerUtilCard.evaluatePermanentList(humanlist)) {
                return false;
            }
        } // otherwise evaluate both lists by CMC and pass only if human permanents are more valuable
        else if ((ComputerUtilCard.evaluatePermanentList(computerlist) + 3) >= ComputerUtilCard.evaluatePermanentList(humanlist)) {
            return false;
        }

        return true;
    }

}
