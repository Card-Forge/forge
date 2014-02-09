package forge.ai.ability;

import com.google.common.base.Predicate;
import forge.ai.ComputerUtil;
import forge.ai.SpellAbilityAi;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.card.CounterType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.List;

public class CountersProliferateAi extends SpellAbilityAi {

    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        boolean chance = true;

        List<Card> cperms = CardLists.filter(ai.getCardsIn(ZoneType.Battlefield), new Predicate<Card>() {
            @Override
            public boolean apply(final Card crd) {
                for (final CounterType c1 : CounterType.values()) {
                    if (crd.getCounters(c1) != 0 && !ComputerUtil.isNegativeCounter(c1, crd)) {
                        return true;
                    }
                }
                return false;
            }
        });

        List<Card> hperms = CardLists.filter(ai.getOpponent().getCardsIn(ZoneType.Battlefield), new Predicate<Card>() {
            @Override
            public boolean apply(final Card crd) {
                for (final CounterType c1 : CounterType.values()) {
                    if (crd.getCounters(c1) != 0 && ComputerUtil.isNegativeCounter(c1, crd)) {
                        return true;
                    }
                }
                return false;
            }
        });

        if (cperms.isEmpty() && hperms.isEmpty() && ai.getOpponent().getPoisonCounters() == 0) {
            return false;
        }
        return chance;
    }

    @Override
    protected boolean doTriggerAINoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        boolean chance = true;

        // TODO Make sure Human has poison counters or there are some counters
        // we want to proliferate
        return chance;
    }

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#chkAIDrawback(java.util.Map, forge.card.spellability.SpellAbility, forge.game.player.Player)
     */
    @Override
    public boolean chkAIDrawback(SpellAbility sa, Player ai) {
        return canPlayAI(ai, sa);
    }

}
