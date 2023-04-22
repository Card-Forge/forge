package forge.game.ability.effects;

import forge.game.ability.SpellAbilityEffect;
import forge.game.spellability.SpellAbility;

public class BlankLineEffect extends SpellAbilityEffect {
    @Override
    protected String getStackDescription(SpellAbility sa) {
        return "\r\n";
    }

    @Override
    public void resolve(SpellAbility sa) {
        // this "effect" just allows spacing to look better for certain card displays
    }
}
