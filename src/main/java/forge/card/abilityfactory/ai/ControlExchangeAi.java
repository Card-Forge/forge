package forge.card.abilityfactory.ai;

import java.util.List;
import java.util.Map;

import com.google.common.base.Predicate;

import forge.Card;
import forge.CardLists;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellAiLogic;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;

public class ControlExchangeAi extends SpellAiLogic {
    
/* (non-Javadoc)
 * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility)
 */
    @Override
    public boolean canPlayAI(Player ai, Map<String, String> params, final SpellAbility sa) {
        Card object1 = null;
        Card object2 = null;
        final Target tgt = sa.getTarget();
        tgt.resetTargets();

        List<Card> list = 
                CardLists.getValidCards(ai.getOpponent().getCardsIn(ZoneType.Battlefield), tgt.getValidTgts(), ai, sa.getSourceCard());
        // AI won't try to grab cards that are filtered out of AI decks on
        // purpose
        list = CardLists.filter(list, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                final Map<String, String> vars = c.getSVars();
                return !vars.containsKey("RemAIDeck") && c.canBeTargetedBy(sa);
            }
        });
        object1 = CardFactoryUtil.getBestAI(list);
        if (params.containsKey("Defined")) {
            object2 = AbilityFactory.getDefinedCards(sa.getSourceCard(), params.get("Defined"), sa).get(0);
        } else if (tgt.getMinTargets(sa.getSourceCard(), sa) > 1) {
            List<Card> list2 = ai.getCardsIn(ZoneType.Battlefield);
            list2 = CardLists.getValidCards(list2, tgt.getValidTgts(), ai, sa.getSourceCard());
            object2 = CardFactoryUtil.getWorstAI(list2);
            tgt.addTarget(object2);
        }
        if (object1 == null || object2 == null) {
            return false;
        }
        if (CardFactoryUtil.evaluateCreature(object1) > CardFactoryUtil.evaluateCreature(object2) + 40) {
            tgt.addTarget(object1);
            return MyRandom.getRandom().nextFloat() <= Math.pow(.6667, sa.getActivationsThisTurn());
        }
        return false;
    }

    @Override
    public boolean chkAIDrawback(java.util.Map<String,String> params, SpellAbility sa, Player aiPlayer) {
        // check AI life before playing this drawback?
        return true;
    }

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#doTriggerAINoCost(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility, boolean)
     */
    @Override
    public boolean doTriggerAINoCost(Player aiPlayer, Map<String, String> params, SpellAbility sa, boolean mandatory) {
        return false;
    }
}