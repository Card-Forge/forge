package forge.card.abilityfactory.effects;

import forge.Singletons;
import forge.card.abilityfactory.SpellEffect;
import forge.card.spellability.SpellAbility;

public class FogEffect extends SpellEffect {
    

    @Override
    protected String getStackDescription(java.util.Map<String,String> params, SpellAbility sa) {
        return sa.getSourceCard().getController() + " prevents all combat damage this turn.";
    }


    @Override
    public void resolve(java.util.Map<String,String> params, SpellAbility sa) {
        // Expand Fog keyword here depending on what we need out of it.
        Singletons.getModel().getGame().getPhaseHandler().setPreventCombatDamageThisTurn(true);
    }
}