package forge.ai.ability;


import forge.ai.ComputerUtilCard;
import forge.ai.SpellAbilityAi;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.ZoneType;

public class BecomesBlockedAi extends SpellAbilityAi {
    @Override
    protected boolean canPlayAI(Player aiPlayer, SpellAbility sa) {
        final Card source = sa.getHostCard();
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        final Game game = aiPlayer.getGame();

        if (!game.getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS)
                || !game.getPhaseHandler().getPlayerTurn().isOpponentOf(aiPlayer)) {
            return false;
        }

        if (tgt != null) {
        	sa.resetTargets();
	        CardCollection list = CardLists.filterControlledBy(game.getCardsIn(ZoneType.Battlefield), aiPlayer.getOpponents());
	        list = CardLists.getValidCards(list, tgt.getValidTgts(), source.getController(), source, sa);
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
        }
        return true;
    }

    @Override
    public boolean chkAIDrawback(SpellAbility sa, Player aiPlayer) {
        // TODO - implement AI
        return false;
    }

    @Override
    protected boolean doTriggerAINoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        boolean chance;

        // TODO - implement AI
        chance = false;

        return chance;
    }
}
