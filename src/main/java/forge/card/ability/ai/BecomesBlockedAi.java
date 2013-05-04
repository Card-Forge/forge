package forge.card.ability.ai;


import java.util.List;

import forge.Card;
import forge.CardLists;
import forge.card.ability.SpellAbilityAi;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.GameState;
import forge.game.ai.ComputerUtilCard;
import forge.game.phase.PhaseType;
import forge.game.player.AIPlayer;
import forge.game.zone.ZoneType;

public class BecomesBlockedAi extends SpellAbilityAi {

    @Override
    protected boolean canPlayAI(AIPlayer aiPlayer, SpellAbility sa) {
        final Card source = sa.getSourceCard();
        final Target tgt = sa.getTarget();
        final GameState game = aiPlayer.getGame();
        
        if (!game.getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS_INSTANT_ABILITY)
                || !game.getPhaseHandler().getPlayerTurn().isOpponentOf(aiPlayer)) {
            return false;
        }

        List<Card> list = game.getCardsIn(ZoneType.Battlefield);
        list = CardLists.filterControlledBy(list, aiPlayer.getOpponents());
        list = CardLists.getValidCards(list, tgt.getValidTgts(), source.getController(), source);
        list = CardLists.getTargetableCards(list, sa);

        while (tgt.getNumTargeted() < tgt.getMaxTargets(source, sa)) {
            Card choice = null;

            if (list.isEmpty()) {
                return false;
            }

            choice = ComputerUtilCard.getBestCreatureAI(list);

            if (choice == null) { // can't find anything left
                return false;
            }

            list.remove(choice);
            tgt.addTarget(choice);
        }
        return true;
    }

    @Override
    public boolean chkAIDrawback(SpellAbility sa, AIPlayer aiPlayer) {

        // TODO - implement AI
        return false;
    }

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#doTriggerAINoCost(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility, boolean)
     */
    @Override
    protected boolean doTriggerAINoCost(AIPlayer aiPlayer, SpellAbility sa, boolean mandatory) {
        boolean chance;

        // TODO - implement AI
        chance = false;

        return chance;
    }
}
