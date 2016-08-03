package forge.ai.ability;


import com.google.common.collect.Iterables;

import forge.ai.SpellAbilityAi;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.ZoneType;

public class ClashAi extends SpellAbilityAi {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#doTriggerAINoCost(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility, boolean)
     */
    @Override
    protected boolean doTriggerAINoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        return true;
    }

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        final Player opp = ai.getOpponent();
        if (tgt != null) {
            if (!sa.canTarget(opp)) {
                return false;
            }
            sa.resetTargets();
            sa.getTargets().add(opp);
        }
        return true;
    }

    @Override
	protected Player chooseSinglePlayer(Player ai, SpellAbility sa, Iterable<Player> options) {
    	for (Player p : options) {
    		if (p.getCardsIn(ZoneType.Library).size() == 0)
    			return p;
    	}

    	CardCollectionView col = ai.getCardsIn(ZoneType.Library);
    	if (!col.isEmpty() && col.getFirst().getView().mayPlayerLook(ai.getView())) {
    		final Card top = col.get(0);
    		for (Player p : options) {
    			final Card oppTop = p.getCardsIn(ZoneType.Library).getFirst();
    			// TODO add logic for SplitCards
    			if (top.getCMC() > oppTop.getCMC()) {
    				return p;
    			}
    		}
    	}

		return Iterables.getFirst(options, null);
	}

}
