package forge.game.ability.effects;

import java.util.ArrayList;
import java.util.List;

import forge.game.ability.SpellAbilityEffect;
import forge.game.mana.Mana;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.Lang;

public class DrainManaEffect extends SpellAbilityEffect {
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        sb.append(Lang.joinHomogenous(getTargetPlayers(sa)));

        sb.append(" loses all unspent mana.");

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        List<Mana> drained = new ArrayList<>();

        for (final Player p : getTargetPlayers(sa)) {
            if (!p.isInGame()) {
                continue;
            }
            drained.addAll(p.getManaPool().clearPool(false));
        }

        if (sa.hasParam("DrainMana")) {
            sa.getActivatingPlayer().getManaPool().add(drained);
        }
        if (sa.hasParam("RememberDrainedMana")) {
            sa.getHostCard().addRemembered(Integer.valueOf(drained.size()));
        }
    }

}
