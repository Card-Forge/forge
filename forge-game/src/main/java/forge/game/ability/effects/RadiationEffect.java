package forge.game.ability.effects;

import forge.game.Game;
import forge.game.GameEntityCounterTable;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

public class RadiationEffect extends SpellAbilityEffect {

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Player player = sa.getActivatingPlayer();
        final Game game = host.getGame();
        final int toAdd = AbilityUtils.calculateAmount(host, sa.getParamOrDefault("Add", "0"), sa);
        final int toRem = AbilityUtils.calculateAmount(host, sa.getParamOrDefault("Remove", "0"), sa);

        GameEntityCounterTable table = new GameEntityCounterTable();

        for (final Player p : getTargetPlayers(sa)) {
            if (!p.isInGame()) continue;

            if (toAdd >= 1) p.addCounter(CounterEnumType.RAD, toAdd, player, table);
            else if (toRem >= 1) p.subtractCounter(CounterEnumType.RAD, toRem);
        }
        table.replaceCounterEffect(game, sa, true);
    }
}
