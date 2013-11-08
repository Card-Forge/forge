package forge.card.ability.effects;

import forge.card.ability.SpellAbilityEffect;
import forge.card.spellability.SpellAbility;

public class FogEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        return sa.getSourceCard().getController() + " prevents all combat damage this turn.";
    }


    @Override
    public void resolve(SpellAbility sa) {
        // Expand Fog keyword here depending on what we need out of it.
        sa.getActivatingPlayer().getGame().getPhaseHandler().setPreventCombatDamageThisTurn(true);
    }
}
