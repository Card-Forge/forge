package forge.card.abilityfactory.ai;

import forge.card.abilityfactory.SpellAiLogic;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.game.player.Player;
import forge.util.MyRandom;

public class ChooseColorAi extends SpellAiLogic {
        
    
    /**
     * <p>
     * chooseColorCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
        @Override
        public boolean canPlayAI(Player aiPlayer, java.util.Map<String,String> params, SpellAbility sa) {
        if (!params.containsKey("AILogic")) {
            return false;
        }
        boolean chance = MyRandom.getRandom().nextFloat() <= Math.pow(.6667, sa.getActivationsThisTurn());

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            chance &= subAb.chkAIDrawback();
        }
        return chance;
    }

    @Override
    public boolean chkAIDrawback(java.util.Map<String,String> params, SpellAbility sa, Player aiPlayer) {
        return true;
    }
    

    /**
     * <p>
     * chooseColorTriggerAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    @Override
    public boolean doTriggerAINoCost(Player ai, java.util.Map<String,String> params, SpellAbility sa, boolean mandatory) {
        return mandatory || canPlayAI(ai, params, sa);
    }

    /**
     * <p>
     * chooseColorStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
}