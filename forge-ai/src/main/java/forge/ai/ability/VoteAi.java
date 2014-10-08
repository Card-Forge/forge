package forge.ai.ability;


import forge.ai.SpellAbilityAi;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class VoteAi extends SpellAbilityAi {
    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected boolean canPlayAI(Player aiPlayer, SpellAbility sa) {
        // TODO: add ailogic
        String logic = sa.getParam("AILogic");
        final Card host = sa.getHostCard();
        if ("Always".equals(logic)) {
            return true;
        } else if ("Judgment".equals(logic)) {
            return !CardLists.getValidCards(host.getGame().getCardsIn(ZoneType.Battlefield),
                    sa.getParam("VoteCard"), host.getController(), host).isEmpty();
        } else if ("Torture".equals(logic)) {
            return aiPlayer.getGame().getPhaseHandler().getPhase().isAfter(PhaseType.MAIN1);
        }
        return false;
    }

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#chkAIDrawback(java.util.Map, forge.card.spellability.SpellAbility, forge.game.player.Player)
     */
    @Override
    public boolean chkAIDrawback(SpellAbility sa, Player aiPlayer) {
        return canPlayAI(aiPlayer, sa);
    }

    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {
        return true;
    }
}
