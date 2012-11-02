package forge.card.abilityfactory.ai;

import java.util.List;
import java.util.Map;
import java.util.Random;

import com.google.common.base.Predicate;

import forge.Card;
import forge.CardLists;
import forge.card.abilityfactory.SpellAiLogic;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.cost.Cost;
import forge.card.cost.CostUtil;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.ComputerUtil;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;

public class DestroyAllAi extends SpellAiLogic {

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
    public boolean doTriggerAINoCost(Player ai, Map<String, String> params, SpellAbility sa, boolean mandatory) {
        final Card source = sa.getSourceCard();
        final Target tgt = sa.getTarget();
        String valid = "";
        if (mandatory) {
            return true;
        }
        if (params.containsKey("ValidCards")) {
            valid = params.get("ValidCards");
        }
        List<Card> humanlist = 
                CardLists.getValidCards(ai.getOpponent().getCardsIn(ZoneType.Battlefield), valid.split(","), source.getController(), source);
        List<Card> computerlist = 
                CardLists.getValidCards(ai.getCardsIn(ZoneType.Battlefield), valid.split(","), source.getController(), source);
        if (sa.getTarget() != null) {
            tgt.resetTargets();
            sa.getTarget().addTarget(ai.getOpponent());
            computerlist.clear();
        }

        humanlist = CardLists.filter(humanlist, predicate);
        computerlist = CardLists.filter(computerlist, predicate);
        if (humanlist.isEmpty() && !computerlist.isEmpty()) {
            return false;
        }

        // if only creatures are affected evaluate both lists and pass only if
        // human creatures are more valuable
        if ((CardLists.getNotType(humanlist, "Creature").size() == 0) && (CardLists.getNotType(computerlist, "Creature").size() == 0)) {
            if (CardFactoryUtil.evaluateCreatureList(computerlist) >= CardFactoryUtil.evaluateCreatureList(humanlist)
                    && !computerlist.isEmpty()) {
                return false;
            }
        } // otherwise evaluate both lists by CMC and pass only if human
          // permanents are more valuable
        else if (CardFactoryUtil.evaluatePermanentList(computerlist) >= CardFactoryUtil.evaluatePermanentList(humanlist)) {
            return false;
        }

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null && !subAb.chkAIDrawback()) {
            return false;
        }
        return true;
    }

    @Override
    public boolean chkAIDrawback(java.util.Map<String,String> params, SpellAbility sa, Player aiPlayer) {
        return true;
    }

    @Override
    public boolean canPlayAI(final Player ai, java.util.Map<String,String> params, SpellAbility sa) {
        // AI needs to be expanded, since this function can be pretty complex
        // based on what the expected targets could be
        final Random r = MyRandom.getRandom();
        final Cost abCost = sa.getPayCosts();
        final Card source = sa.getSourceCard();
        String valid = "";

        if (params.containsKey("ValidCards")) {
            valid = params.get("ValidCards");
        }

        if (valid.contains("X") && source.getSVar("X").equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            final int xPay = ComputerUtil.determineLeftoverMana(sa, ai);
            source.setSVar("PayX", Integer.toString(xPay));
            valid = valid.replace("X", Integer.toString(xPay));
        }

        final Target tgt = sa.getTarget();

        List<Card> humanlist = 
                CardLists.getValidCards(ai.getOpponent().getCardsIn(ZoneType.Battlefield), valid.split(","), source.getController(), source);
        List<Card> computerlist = 
                CardLists.getValidCards(ai.getCardsIn(ZoneType.Battlefield), valid.split(","), source.getController(), source);
        if (sa.getTarget() != null) {
            tgt.resetTargets();
            sa.getTarget().addTarget(ai.getOpponent());
            computerlist.clear();
        }

        humanlist = CardLists.filter(humanlist, predicate);
        computerlist = CardLists.filter(computerlist, predicate);

        if (abCost != null) {
            // AI currently disabled for some costs

            if (!CostUtil.checkLifeCost(ai, abCost, source, 4, null)) {
                return false;
            }
        }

        // prevent run-away activations - first time will always return true
        boolean chance = r.nextFloat() <= Math.pow(.6667, sa.getActivationsThisTurn());

        // if only creatures are affected evaluate both lists and pass only if
        // human creatures are more valuable
        if ((CardLists.getNotType(humanlist, "Creature").size() == 0) && (CardLists.getNotType(computerlist, "Creature").size() == 0)) {
            if ((CardFactoryUtil.evaluateCreatureList(computerlist) + 200) >= CardFactoryUtil
                    .evaluateCreatureList(humanlist)) {
                return false;
            }
        } // only lands involved
        else if ((CardLists.getNotType(humanlist, "Land").size() == 0) && (CardLists.getNotType(computerlist, "Land").size() == 0)) {
            if ((CardFactoryUtil.evaluatePermanentList(computerlist) + 1) >= CardFactoryUtil
                    .evaluatePermanentList(humanlist)) {
                return false;
            }
        } // otherwise evaluate both lists by CMC and pass only if human
          // permanents are more valuable
        else if ((CardFactoryUtil.evaluatePermanentList(computerlist) + 3) >= CardFactoryUtil
                .evaluatePermanentList(humanlist)) {
            return false;
        }

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            chance &= subAb.chkAIDrawback();
        }

        return chance;
    }

}