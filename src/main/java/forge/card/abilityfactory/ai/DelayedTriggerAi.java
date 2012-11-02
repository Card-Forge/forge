package forge.card.abilityfactory.ai;

import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellAiLogic;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;

public class DelayedTriggerAi extends SpellAiLogic {
    private static AbilityFactory tempCreator = new AbilityFactory();
    
    @Override
    public boolean chkAIDrawback(java.util.Map<String,String> params, SpellAbility sa, forge.game.player.Player aiPlayer) {
        final String svarName = params.get("Execute");
        final SpellAbility trigsa = tempCreator.getAbility(sa.getAbilityFactory().getHostCard()
                .getSVar(svarName), sa.getAbilityFactory().getHostCard());

        if (trigsa instanceof AbilitySub) {
            return ((AbilitySub) trigsa).chkAIDrawback();
        } else {
            return trigsa.canPlayAI();
        }
    }

    @Override
    public boolean doTriggerAI(forge.game.player.Player aiPlayer, java.util.Map<String,String> params, SpellAbility sa, boolean mandatory) {
        final String svarName = params.get("Execute");
        final SpellAbility trigsa = tempCreator.getAbility(sa.getAbilityFactory().getHostCard()
                .getSVar(svarName), sa.getAbilityFactory().getHostCard());

        if (!params.containsKey("OptionalDecider")) {
            return trigsa.doTrigger(true);
        } else {
            return trigsa.doTrigger(!params.get("OptionalDecider").equals("You"));
        }
    }

    @Override
    public boolean canPlayAI(forge.game.player.Player aiPlayer, java.util.Map<String,String> params, SpellAbility sa) {
        final String svarName = params.get("Execute");
        final SpellAbility trigsa = tempCreator.getAbility(sa.getAbilityFactory().getHostCard().getSVar(svarName), sa.getAbilityFactory().getHostCard());
        return trigsa.canPlayAI();
    }

}