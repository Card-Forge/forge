package forge.game.ability.effects;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;

import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.ability.SpellAbilityEffect;
import forge.game.mana.Mana;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbilityUnspentMana;
import forge.game.trigger.TriggerType;
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
        final Game game = sa.getHostCard().getGame();
        final List<Mana> drained = new ArrayList<>();
        final Map<Player, Integer> lossMap = Maps.newHashMap();

        for (final Player p : getTargetPlayers(sa)) {
            if (!p.isInGame()) {
                continue;
            }
            List<Mana> cleared = p.getManaPool().clearPool(false);
            drained.addAll(cleared);
            if (StaticAbilityUnspentMana.hasManaBurn(p)) {
                final int lost = p.loseLife(cleared.size(), false, true);
                if (lost > 0) {
                    lossMap.put(p, lost);
                }
            }
        }

        if (!lossMap.isEmpty()) { // Run triggers if any player actually lost life
            final Map<AbilityKey, Object> runLifeLostParams = AbilityKey.mapFromPIMap(lossMap);
            game.getTriggerHandler().runTrigger(TriggerType.LifeLostAll, runLifeLostParams, false);
        }

        if (sa.hasParam("DrainMana")) {
            sa.getActivatingPlayer().getManaPool().add(drained);
        }
        if (sa.hasParam("RememberDrainedMana")) {
            sa.getHostCard().addRemembered(Integer.valueOf(drained.size()));
        }
    }

}
