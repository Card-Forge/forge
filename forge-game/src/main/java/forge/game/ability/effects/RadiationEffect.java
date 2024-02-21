package forge.game.ability.effects;

import com.google.common.collect.Maps;
import forge.game.Game;
import forge.game.GameEntityCounterTable;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.event.GameEventPlayerRadiation;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

import java.util.Map;

public class RadiationEffect extends SpellAbilityEffect {

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Game game = host.getGame();
        final int toAdd = AbilityUtils.calculateAmount(host, sa.getParamOrDefault("Add", "0"), sa);
        final int toRem = AbilityUtils.calculateAmount(host, sa.getParamOrDefault("Remove", "0"), sa);
        final Map<Player, Integer> list = Maps.newHashMap();

        GameEntityCounterTable table = new GameEntityCounterTable();

        for (final Player p : getTargetPlayers(sa)) {
            if (!p.isInGame()) continue;

            list.put(p, p.getCounters(CounterEnumType.RAD));
            if (toAdd >= 1) p.addRadCounters(toAdd, host, table);
            else if (toRem >= 1) p.removeRadCounters(toRem, host);
        }
        table.replaceCounterEffect(game, sa, true);
        for (final Player p : list.keySet()) {
            int oldCount = list.get(p);
            int newCount = p.getCounters(CounterEnumType.RAD);
            if (newCount > 0 && !p.hasRadiationEffect()) p.createRadiationEffect(host.getSetCode());
            if (oldCount < newCount) game.fireEvent(new GameEventPlayerRadiation(p, host, newCount - oldCount));
        }
    }
}
