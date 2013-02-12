package forge.card.ability.effects;

import forge.Singletons;
import forge.card.ability.SpellEffect;
import forge.card.spellability.SpellAbility;

public class FogEffect extends SpellEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        return sa.getSourceCard().getController() + " prevents all combat damage this turn.";
    }


    @Override
    public void resolve(SpellAbility sa) {
        // Expand Fog keyword here depending on what we need out of it.
        Singletons.getModel().getGame().getPhaseHandler().setPreventCombatDamageThisTurn(true);
    }
}
