package forge.ai.ability;


import java.util.Map;

import com.google.common.collect.Iterables;

import forge.ai.ComputerUtilCard;
import forge.ai.SpellAbilityAi;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.player.Player;
import forge.game.player.PlayerCollection;
import forge.game.player.PlayerPredicates;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class ClashAi extends SpellAbilityAi {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#doTriggerAINoCost(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility, boolean)
     */
    @Override
    protected boolean doTriggerAINoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        boolean legalAction = true;

        if (sa.usesTargeting()) {
            legalAction = selectTarget(aiPlayer, sa);
        }

        return legalAction;
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.ai.SpellAbilityAi#checkApiLogic(forge.game.player.Player,
     * forge.game.spellability.SpellAbility)
     */
    @Override
    protected boolean checkApiLogic(Player ai, SpellAbility sa) {
        boolean legalAction = true;

        if (sa.usesTargeting()) {
            legalAction = selectTarget(ai, sa);
        }

        return legalAction;
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.ai.SpellAbilityAi#chooseSinglePlayer(forge.game.player.Player,
     * forge.game.spellability.SpellAbility, java.lang.Iterable)
     */
    @Override
    protected Player chooseSinglePlayer(Player ai, SpellAbility sa, Iterable<Player> options, Map<String, Object> params) {
        for (Player p : options) {
            if (p.getCardsIn(ZoneType.Library).size() == 0)
                return p;
        }

    	CardCollectionView col = ai.getCardsIn(ZoneType.Library);
        if (!col.isEmpty() && col.getFirst().mayPlayerLook(ai)) {
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

    private boolean selectTarget(Player ai, SpellAbility sa) {
        String valid = sa.getParam("ValidTgts");

        PlayerCollection players = ai.getOpponents().filter(PlayerPredicates.isTargetableBy(sa));
        // use chooseSinglePlayer function to the select player
        Player chosen = chooseSinglePlayer(ai, sa, players, null);
        if (chosen != null) {
            sa.resetTargets();
            sa.getTargets().add(chosen);
        }

        if ("Creature".equals(valid)) {
            // Springjack Knight
            // TODO: Whirlpool Whelm also uses creature targeting but it's trickier to support
            CardCollectionView aiCreats = CardLists.filter(ai.getCardsIn(ZoneType.Battlefield), CardPredicates.Presets.CREATURES);
            CardCollectionView oppCreats = CardLists.filter(ai.getOpponents().getCardsIn(ZoneType.Battlefield), CardPredicates.Presets.CREATURES);

            Card tgt = aiCreats.isEmpty() ? ComputerUtilCard.getWorstCreatureAI(oppCreats) : ComputerUtilCard.getBestCreatureAI(aiCreats);

            if (tgt != null) {
                sa.resetTargets();
                sa.getTargets().add(tgt);
            } else {
                return false; // cut short if this part of the clause is not satisfiable (with current card pool should never get here)
            }
        }

        return sa.getTargets().size() > 0;
    }

}
