package forge.game.ability.effects;

import forge.game.ability.SpellAbilityEffect;
import forge.game.spellability.SpellAbility;

public class FogEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        return sa.getHostCard().getController() + " prevents all combat damage this turn.";
    }


    @Override
    public void resolve(SpellAbility sa) {
        // Expand Fog keyword here depending on what we need out of it.
        sa.getActivatingPlayer().getGame().getPhaseHandler().setPreventCombatDamageThisTurn();
    }
}
