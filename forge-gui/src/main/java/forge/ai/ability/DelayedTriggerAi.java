package forge.ai.ability;

import forge.game.ability.AbilityFactory;
import forge.game.ability.SpellAbilityAi;
import forge.game.player.Player;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;

public class DelayedTriggerAi extends SpellAbilityAi {

    @Override
    public boolean chkAIDrawback(SpellAbility sa, Player ai) {
        final String svarName = sa.getParam("Execute");
        final SpellAbility trigsa = AbilityFactory.getAbility(sa.getSourceCard().getSVar(svarName), sa.getSourceCard());
        trigsa.setActivatingPlayer(ai);

        if (trigsa instanceof AbilitySub) {
            return ((AbilitySub) trigsa).getAi().chkDrawbackWithSubs(ai, (AbilitySub)trigsa);
        } else {
            return trigsa.canPlayAI();
        }
    }

    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {
        final String svarName = sa.getParam("Execute");
        final SpellAbility trigsa = AbilityFactory.getAbility(sa.getSourceCard().getSVar(svarName), sa.getSourceCard());
        trigsa.setActivatingPlayer(ai);

        if (!sa.hasParam("OptionalDecider")) {
            return trigsa.doTrigger(true, ai);
        } else {
            return trigsa.doTrigger(!sa.getParam("OptionalDecider").equals("You"), ai);
        }
    }

    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        final String svarName = sa.getParam("Execute");
        final SpellAbility trigsa = AbilityFactory.getAbility(sa.getSourceCard().getSVar(svarName), sa.getSourceCard());
        trigsa.setActivatingPlayer(ai);
        return trigsa.canPlayAI();
    }

}
