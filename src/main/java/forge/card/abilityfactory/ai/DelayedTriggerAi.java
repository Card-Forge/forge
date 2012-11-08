package forge.card.abilityfactory.ai;

import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellAiLogic;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;

public class DelayedTriggerAi extends SpellAiLogic {
    private static AbilityFactory tempCreator = new AbilityFactory();
    
    @Override
    public boolean chkAIDrawback(SpellAbility sa, forge.game.player.Player aiPlayer) {
        final String svarName = sa.getParam("Execute");
        final SpellAbility trigsa = tempCreator.getAbility(sa.getSourceCard().getSVar(svarName), sa.getSourceCard());

        if (trigsa instanceof AbilitySub) {
            return ((AbilitySub) trigsa).chkAIDrawback();
        } else {
            return trigsa.canPlayAI();
        }
    }

    @Override
    protected boolean doTriggerAINoCost(forge.game.player.Player aiPlayer, SpellAbility sa, boolean mandatory) {
        final String svarName = sa.getParam("Execute");
        final SpellAbility trigsa = tempCreator.getAbility(sa.getSourceCard().getSVar(svarName), sa.getSourceCard());

        if (!sa.hasParam("OptionalDecider")) {
            return trigsa.doTrigger(true);
        } else {
            return trigsa.doTrigger(!sa.getParam("OptionalDecider").equals("You"));
        }
    }

    @Override
    protected boolean canPlayAI(forge.game.player.Player aiPlayer, SpellAbility sa) {
        final String svarName = sa.getParam("Execute");
        final SpellAbility trigsa = tempCreator.getAbility(sa.getSourceCard().getSVar(svarName), sa.getSourceCard());
        return trigsa.canPlayAI();
    }

}