package forge.game.ability.effects;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import forge.game.ability.SpellAbilityEffect;
import forge.game.mana.Mana;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;

public class DrainManaEffect extends SpellAbilityEffect {
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        final List<Player> tgtPlayers = getTargetPlayers(sa);

        sb.append(StringUtils.join(tgtPlayers, ", "));
        sb.append(" loses all unspent mana.");

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        List<Mana> drained = new ArrayList<>();

        for (final Player p : getTargetPlayers(sa)) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                drained.addAll(p.getManaPool().clearPool(false));
            }
        }

        if (sa.hasParam("DrainMana")) {
            for (Mana mana : drained) {
                sa.getActivatingPlayer().getManaPool().addMana(mana);
            }
        }
        if (sa.hasParam("RememberDrainedMana")) {
            sa.getHostCard().addRemembered(Integer.valueOf(drained.size()));
        }
    }

}
