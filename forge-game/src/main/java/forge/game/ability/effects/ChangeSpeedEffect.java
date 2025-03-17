package forge.game.ability.effects;

import forge.game.ability.SpellAbilityEffect;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

public class ChangeSpeedEffect extends SpellAbilityEffect {
    @Override
    public void resolve(SpellAbility sa) {
        String mode = sa.getParamOrDefault("Mode", "Increase");

        for (Player p : getTargetPlayers(sa)) {
            if (p.isInGame()) {
                if (mode.equals("Increase") && !p.maxSpeed()) p.increaseSpeed();
                else if (mode.equals("Decrease") && p.getSpeed() > 1) p.decreaseSpeed();
            }
        }
    }
}
