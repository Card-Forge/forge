package forge.card.abilityfactory.effects;

import forge.Singletons;
import forge.card.abilityfactory.SpellEffect;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;

public class FogEffect extends SpellEffect {
    

    @Override
    protected String getStackDescription(java.util.Map<String,String> params, SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
    
        if (!(sa instanceof AbilitySub)) {
            sb.append(sa.getSourceCard().getName()).append(" - ");
        } else {
            sb.append(" ");
        }
    
        sb.append(sa.getSourceCard().getController());
        sb.append(" prevents all combat damage this turn.");
        return sb.toString();
    }


    @Override
    public void resolve(java.util.Map<String,String> params, SpellAbility sa) {
        // Expand Fog keyword here depending on what we need out of it.
        Singletons.getModel().getGame().getPhaseHandler().setPreventCombatDamageThisTurn(true);
    }
}