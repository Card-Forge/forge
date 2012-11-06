package forge.card.abilityfactory.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import forge.Card;
import forge.CardLists;
import forge.Singletons;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellAiLogic;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.cost.Cost;
import forge.card.cost.CostUtil;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;

public class PlayAi extends SpellAiLogic {
    
    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#chkAIDrawback(java.util.Map, forge.card.spellability.SpellAbility, forge.game.player.Player)
     */
    @Override
    public boolean chkAIDrawback(Map<String, String> params, SpellAbility sa, Player aiPlayer) {
        return true;
    }
    
    @Override
    protected boolean canPlayAI(Player ai, java.util.Map<String,String> params, SpellAbility sa) {
        final Cost abCost = sa.getAbilityFactory().getAbCost();
        final Card source = sa.getAbilityFactory().getHostCard();

        final Random r = MyRandom.getRandom();

        if (abCost != null) {
            // AI currently disabled for these costs
            if (!CostUtil.checkSacrificeCost(ai, abCost, source)) {
                return false;
            }

            if (!CostUtil.checkLifeCost(ai, abCost, source, 4, null)) {
                return false;
            }

            if (!CostUtil.checkDiscardCost(ai, abCost, source)) {
                return false;
            }

            if (!CostUtil.checkRemoveCounterCost(abCost, source)) {
                return false;
            }
        }

        // don't use this as a response
        if (!Singletons.getModel().getGame().getStack().isEmpty()) {
            return false;
        }

        // prevent run-away activations - first time will always return true
        boolean chance = r.nextFloat() <= Math.pow(.6667, sa.getRestrictions().getNumberTurnActivations());

        List<Card> cards;
        final Target tgt = sa.getTarget();
        if (tgt != null) {
            ZoneType zone = tgt.getZone().get(0);
            cards = CardLists.getValidCards(Singletons.getModel().getGame().getCardsIn(zone), tgt.getValidTgts(), ai, source);
            if (cards.isEmpty()) {
                return false;
            }
            tgt.addTarget(CardFactoryUtil.getBestAI(cards));
        } else if (!params.containsKey("Valid")) {
            cards = new ArrayList<Card>(AbilityFactory.getDefinedCards(sa.getSourceCard(), params.get("Defined"), sa));
            if (cards.isEmpty()) {
                return false;
            }
        }
        return chance;
    }
}