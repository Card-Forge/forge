package forge.ai.ability;


import java.util.List;

import forge.ai.ComputerUtilCard;
import forge.game.Game;
import forge.game.ability.SpellAbilityAi;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.ZoneType;

public class BecomesBlockedAi extends SpellAbilityAi {

    @Override
    protected boolean canPlayAI(Player aiPlayer, SpellAbility sa) {
        final Card source = sa.getSourceCard();
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        final Game game = aiPlayer.getGame();
        
        if (!game.getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS)
                || !game.getPhaseHandler().getPlayerTurn().isOpponentOf(aiPlayer)) {
            return false;
        }

        List<Card> list = game.getCardsIn(ZoneType.Battlefield);
        list = CardLists.filterControlledBy(list, aiPlayer.getOpponents());
        list = CardLists.getValidCards(list, tgt.getValidTgts(), source.getController(), source);
        list = CardLists.getTargetableCards(list, sa);
        list = CardLists.getNotKeyword(list, "Trample");

        while (sa.getTargets().getNumTargeted() < tgt.getMaxTargets(source, sa)) {
            Card choice = null;

            if (list.isEmpty()) {
                return false;
            }

            choice = ComputerUtilCard.getBestCreatureAI(list);

            if (choice == null) { // can't find anything left
                return false;
            }

            list.remove(choice);
            sa.getTargets().add(choice);
        }
        return true;
    }

    @Override
    public boolean chkAIDrawback(SpellAbility sa, Player aiPlayer) {

        // TODO - implement AI
        return false;
    }

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#doTriggerAINoCost(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility, boolean)
     */
    @Override
    protected boolean doTriggerAINoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        boolean chance;

        // TODO - implement AI
        chance = false;

        return chance;
    }
}
