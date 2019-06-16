package forge.ai.ability;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;

import forge.ai.ComputerUtilCard;
import forge.ai.SpellAbilityAi;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;

public class ControlExchangeAi extends SpellAbilityAi {

/* (non-Javadoc)
 * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility)
 */
    @Override
    protected boolean canPlayAI(Player ai, final SpellAbility sa) {
        Card object1 = null;
        Card object2 = null;
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        sa.resetTargets();

        CardCollection list =
                CardLists.getValidCards(ai.getWeakestOpponent().getCardsIn(ZoneType.Battlefield), tgt.getValidTgts(), ai, sa.getHostCard(), sa);
        // AI won't try to grab cards that are filtered out of AI decks on
        // purpose
        list = CardLists.filter(list, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {                
                return !ComputerUtilCard.isCardRemAIDeck(c) && c.canBeTargetedBy(sa);
            }
        });
        object1 = ComputerUtilCard.getBestAI(list);
        if (sa.hasParam("Defined")) {
            object2 = AbilityUtils.getDefinedCards(sa.getHostCard(), sa.getParam("Defined"), sa).get(0);
        } else if (tgt.getMinTargets(sa.getHostCard(), sa) > 1) {
            CardCollectionView list2 = ai.getCardsIn(ZoneType.Battlefield);
            list2 = CardLists.getValidCards(list2, tgt.getValidTgts(), ai, sa.getHostCard(), sa);
            object2 = ComputerUtilCard.getWorstAI(list2);
            sa.getTargets().add(object2);
        }
        if (object1 == null || object2 == null) {
            return false;
        }
        if (ComputerUtilCard.evaluateCreature(object1) > ComputerUtilCard.evaluateCreature(object2) + 40) {
            sa.getTargets().add(object1);
            return MyRandom.getRandom().nextFloat() <= Math.pow(.6667, sa.getActivationsThisTurn());
        }
        return false;
    }

    @Override
    protected boolean doTriggerAINoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        if (!sa.usesTargeting()) {
            if (mandatory) {
                return true;
            }
        } else {
            if (mandatory) {
                return chkAIDrawback(sa, aiPlayer);
            } else {
                return canPlayAI(aiPlayer, sa);
            }
        }
        return true;
    }

    @Override
    public boolean chkAIDrawback(SpellAbility sa, Player aiPlayer) {
        if (!sa.usesTargeting()) {
            return true;
        }

        final TargetRestrictions tgt = sa.getTargetRestrictions();

        CardCollection list = CardLists.getValidCards(aiPlayer.getGame().getCardsIn(ZoneType.Battlefield),
                tgt.getValidTgts(), aiPlayer, sa.getHostCard(), sa);

        // only select the cards that can be targeted
        list = CardLists.getTargetableCards(list, sa);

        if (list.isEmpty())
            return false;

        Card best = ComputerUtilCard.getBestAI(list);

        // if Param has Defined, check if the best Target is better than the Defined
        if (sa.hasParam("Defined")) {
            final Card object = AbilityUtils.getDefinedCards(sa.getHostCard(), sa.getParam("Defined"), sa).get(0);
            // TODO add evaluate Land if able
            final Card realBest = ComputerUtilCard.getBestAI(Lists.newArrayList(best, object));

            // Defined card is better than this one, try to avoid trade
            if (!best.equals(realBest)) {
                return false;
            }
        }

        // add best Target
        sa.getTargets().add(best);
        return true;
    }   
    
}
