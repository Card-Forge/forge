package forge.ai.ability;


import forge.ai.SpellAbilityAi;
import forge.game.ability.AbilityFactory;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;

public class RepeatAi extends SpellAbilityAi {

    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        final Player opp = ai.getOpponent();
        if (tgt != null) {
            if (!opp.canBeTargetedBy(sa)) {
                return false;
            }
            sa.resetTargets();
            sa.getTargets().add(opp);
        }
        return true;
    }
    
    @Override
    public boolean confirmAction(Player player, SpellAbility sa, PlayerActionConfirmMode mode, String message) {
      //TODO add logic to have computer make better choice (ArsenalNut)
        return false;
    }

    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {
    	// setup subability to repeat
        final SpellAbility repeat = AbilityFactory.getAbility(sa.getSourceCard().getSVar(sa.getParam("RepeatSubAbility")), sa.getSourceCard());

        if (repeat == null) {
        	return false;
        }

        repeat.setActivatingPlayer(sa.getActivatingPlayer());
        ((AbilitySub) repeat).setParent(sa);
        return repeat.doTrigger(mandatory, ai);
    }
}
