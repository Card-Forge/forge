package forge.card.ability.ai;

import forge.card.ability.AbilityFactory;
import forge.card.ability.SpellAiLogic;
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
            return ((AbilitySub) trigsa).chkAIDrawback(ai);
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
            return trigsa.doTrigger(true, ai);
        } else {
            return trigsa.doTrigger(!sa.getParam("OptionalDecider").equals("You"), ai);
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
