package forge.card.ability.ai;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.base.Predicate;

import forge.Card;
import forge.CardLists;
import forge.card.ability.AbilityUtils;
import forge.card.ability.SpellAbilityAi;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.TargetRestrictions;
import forge.game.ai.ComputerUtilCard;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;

public class PowerExchangeAi extends SpellAbilityAi {

/* (non-Javadoc)
 * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility)
 */
    @Override
    protected boolean canPlayAI(Player ai, final SpellAbility sa) {
        Card c1 = null;
        Card c2 = null;
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        sa.resetTargets();

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
        CardLists.sortByPowerAsc(list);
        c1 = list.isEmpty() ? null : list.get(0);
        if (sa.hasParam("Defined")) {
            c2 = AbilityUtils.getDefinedCards(sa.getSourceCard(), sa.getParam("Defined"), sa).get(0);
        } else if (tgt.getMinTargets(sa.getSourceCard(), sa) > 1) {
            List<Card> list2 = ai.getCardsIn(ZoneType.Battlefield);
            list2 = CardLists.getValidCards(list2, tgt.getValidTgts(), ai, sa.getSourceCard());
            CardLists.sortByPowerAsc(list2);
            Collections.reverse(list2);
            c2 = list2.isEmpty() ? null : list2.get(0);
            sa.getTargets().add(c2);
        }
        if (c1 == null || c2 == null) {
            return false;
        }
        if (ComputerUtilCard.evaluateCreature(c1) > ComputerUtilCard.evaluateCreature(c2) + 40) {
            sa.getTargets().add(c1);
            return MyRandom.getRandom().nextFloat() <= Math.pow(.6667, sa.getActivationsThisTurn());
        }
        return false;
    }

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#doTriggerAINoCost(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility, boolean)
     */
    @Override
    protected boolean doTriggerAINoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        if (sa.getTargetRestrictions() == null) {
            if (mandatory) {
                return true;
            }
        } else {
            return canPlayAI(aiPlayer, sa);
        }
        return true;
    }
}
