package forge.card.abilityfactory.ai;

import java.util.List;
import java.util.Map;

import com.google.common.base.Predicate;

import forge.Card;
import forge.CardLists;
import forge.Counters;
import forge.card.abilityfactory.SpellAiLogic;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.spellability.SpellAbility;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class CountersProliferateAi extends SpellAiLogic {
    
    @Override
    public boolean canPlayAI(Player ai, java.util.Map<String,String> params, SpellAbility sa) {
        boolean chance = true;
        
        List<Card> cperms = CardLists.filter(ai.getCardsIn(ZoneType.Battlefield), new Predicate<Card>() {
            @Override
            public boolean apply(final Card crd) {
                for (final Counters c1 : Counters.values()) {
                    if (crd.getCounters(c1) != 0 && !CardFactoryUtil.isNegativeCounter(c1)) {
                        return true;
                    }
                }
                return false;
            }
        });

        List<Card> hperms = CardLists.filter(ai.getOpponent().getCardsIn(ZoneType.Battlefield), new Predicate<Card>() {
            @Override
            public boolean apply(final Card crd) {
                for (final Counters c1 : Counters.values()) {
                    if (crd.getCounters(c1) != 0 && CardFactoryUtil.isNegativeCounter(c1)) {
                        return true;
                    }
                }
                return false;
            }
        });

        if ((cperms.size() == 0) && (hperms.size() == 0) && (ai.getOpponent().getPoisonCounters() == 0)) {
            return false;
        }
        return chance;
    }

    @Override
    protected boolean doTriggerAINoCost(Player aiPlayer, java.util.Map<String,String> params, SpellAbility sa, boolean mandatory) {
        boolean chance = true;

        // TODO Make sure Human has poison counters or there are some counters
        // we want to proliferate
        return chance;
    }
    
    
    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#chkAIDrawback(java.util.Map, forge.card.spellability.SpellAbility, forge.game.player.Player)
     */
    @Override
    public boolean chkAIDrawback(Map<String, String> params, SpellAbility sa, Player ai) {
        return canPlayAI(ai, params, sa);
    }

}