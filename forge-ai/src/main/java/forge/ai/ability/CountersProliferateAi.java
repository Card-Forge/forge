package forge.ai.ability;

import java.util.List;
import java.util.Map;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;

import forge.ai.ComputerUtil;
import forge.ai.SpellAbilityAi;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.card.CounterType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class CountersProliferateAi extends SpellAbilityAi {

    @Override
    protected boolean checkApiLogic(Player ai, SpellAbility sa) {

        final List<Card> cperms = Lists.newArrayList();
        final List<Player> allies = ai.getAllies();
        allies.add(ai);
        boolean allyExpOrEnergy = false;

        for (final Player p : allies) {
        	// player has experience or energy counter
            if (p.getCounters(CounterType.EXPERIENCE) + p.getCounters(CounterType.ENERGY) >= 1) {
                allyExpOrEnergy = true;
            }
            cperms.addAll(CardLists.filter(p.getCardsIn(ZoneType.Battlefield), new Predicate<Card>() {
                @Override
                public boolean apply(final Card crd) {
                    if (crd.hasCounters()) {
                        return false;
                    }

                    // iterate only over existing counters
                    for (final Map.Entry<CounterType, Integer> e : crd.getCounters().entrySet()) {
                        if (e.getValue() >= 1 && !ComputerUtil.isNegativeCounter(e.getKey(), crd)) {
                            return true;
                        }
                    }
                    return false;
                }
            }));
        }

        final List<Card> hperms = Lists.newArrayList();
        boolean opponentPoison = false;

        for (final Player o : ai.getOpponents()) {
            opponentPoison |= o.getPoisonCounters() >= 1;
            hperms.addAll(CardLists.filter(o.getCardsIn(ZoneType.Battlefield), new Predicate<Card>() {
                @Override
                public boolean apply(final Card crd) {
                    if (crd.hasCounters()) {
                        return false;
                    }

                    // iterate only over existing counters
                    for (final Map.Entry<CounterType, Integer> e : crd.getCounters().entrySet()) {
                        if (e.getValue() >= 1 && ComputerUtil.isNegativeCounter(e.getKey(), crd)) {
                            return true;
                        }
                    }
                    return false;
                }
            }));
        }
        

        if (cperms.isEmpty() && hperms.isEmpty() && !opponentPoison && !allyExpOrEnergy) {
            return false;
        }
        return true;
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
