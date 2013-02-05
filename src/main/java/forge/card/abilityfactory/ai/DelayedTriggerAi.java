package forge.card.abilityfactory.ai;

import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellAiLogic;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.game.player.AIPlayer;

public class DelayedTriggerAi extends SpellAiLogic {
    private static AbilityFactory tempCreator = new AbilityFactory();

    @Override
    public boolean chkAIDrawback(SpellAbility sa, AIPlayer ai) {
        final String svarName = sa.getParam("Execute");
        final SpellAbility trigsa = tempCreator.getAbility(sa.getSourceCard().getSVar(svarName), sa.getSourceCard());
        trigsa.setActivatingPlayer(ai);

        if (trigsa instanceof AbilitySub) {
            return ((AbilitySub) trigsa).chkAIDrawback();
        } else {
            return trigsa.canPlayAI();
        }
    }

    @Override
    protected boolean doTriggerAINoCost(AIPlayer ai, SpellAbility sa, boolean mandatory) {
        final String svarName = sa.getParam("Execute");
        final SpellAbility trigsa = tempCreator.getAbility(sa.getSourceCard().getSVar(svarName), sa.getSourceCard());
        trigsa.setActivatingPlayer(ai);

        if (!sa.hasParam("OptionalDecider")) {
            return trigsa.doTrigger(true);
        } else {
            return trigsa.doTrigger(!sa.getParam("OptionalDecider").equals("You"));
        }
    }

    @Override
    protected boolean canPlayAI(AIPlayer ai, SpellAbility sa) {
        final String svarName = sa.getParam("Execute");
        final SpellAbility trigsa = tempCreator.getAbility(sa.getSourceCard().getSVar(svarName), sa.getSourceCard());
        trigsa.setActivatingPlayer(ai);
        return trigsa.canPlayAI();
    }

}
