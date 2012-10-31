package forge.card.abilityfactory.ai;

import forge.card.abilityfactory.SpellAiLogic;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.game.player.Player;

public class UntapAllAi extends SpellAiLogic {
    
    /**
     * <p>
     * untapAllPlayDrawbackAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    @Override
    public boolean chkAIDrawback(java.util.Map<String,String> params, SpellAbility sa, Player aiPlayer) {
        return true;
    }

    /**
     * <p>
     * untapAllCanPlayAI.
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
        // check SubAbilities DoTrigger?
        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null && abSub.chkAIDrawback()) {
            return true;
        }
        return false;
    }

    /**
     * <p>
     * untapAllTrigger.
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
    public boolean doTriggerAINoCost(Player aiPlayer, java.util.Map<String,String> params, SpellAbility sa, boolean mandatory) {
        return mandatory;
    }

    /**
     * <p>
     * untapAllStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
}