package forge.game.ability.effects;

import forge.game.ability.SpellAbilityEffect;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

public class DecreaseSpeedEffect extends SpellAbilityEffect {
    @Override
    public void resolve(SpellAbility sa) {
        for (Player p : getTargetPlayers(sa)) {
            if (!p.isInGame() || p.noSpeed()) continue;
            p.decreaseSpeed();
        }
    }
}
