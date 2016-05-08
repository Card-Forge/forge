package forge.ai.ability;


import forge.ai.SpellAbilityAi;
import forge.game.GlobalRuleChange;
import forge.game.card.Card;
import forge.game.card.CardState;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

public class SetStateAi extends SpellAbilityAi {
    @Override
    protected boolean canPlayAI(Player aiPlayer, SpellAbility sa) {
        // Prevent transform into legendary creature if copy already exists
        final Card source = sa.getHostCard();
        
        // Check first if Legend Rule does still apply
        if (!aiPlayer.getGame().getStaticEffects().getGlobalRuleChange(GlobalRuleChange.noLegendRule)) {

        	// check if the other side is legendary and if such Card already is in Play
            final CardState other = source.getAlternateState();
            if (other.getType().isLegendary() && aiPlayer.isCardInPlay(other.getName())) {
            	return false;
            }
        }
        
        if (sa.getTargetRestrictions() == null && "Transform".equals(sa.getParam("Mode"))) {
            return true;
        }
        return false;
    }

    @Override
    public boolean chkAIDrawback(SpellAbility sa, Player aiPlayer) {
        // Gross generalization, but this always considers alternate
        // states more powerful
        return !sa.getHostCard().isInAlternateState();
    }


    /* (non-Javadoc)
    * @see forge.card.abilityfactory.SpellAiLogic#doTriggerAINoCost(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility, boolean)
    */
    @Override
    protected boolean doTriggerAINoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        return true;
    }
}
