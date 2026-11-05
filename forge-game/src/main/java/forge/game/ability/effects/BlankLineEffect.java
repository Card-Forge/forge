package forge.game.ability.effects;

import forge.game.ability.SpellAbilityEffect;
import forge.game.spellability.SpellAbility;

public class BlankLineEffect extends SpellAbilityEffect {
    // this "effect" just allows spacing to look better for certain card displays

    @Override
    protected String getStackDescription(SpellAbility sa) {
        return "\r\n";
    }
}
