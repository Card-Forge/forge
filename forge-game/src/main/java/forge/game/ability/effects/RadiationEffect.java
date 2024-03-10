package forge.game.ability.effects;

import forge.game.Game;
import forge.game.GameEntityCounterTable;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

public class RadiationEffect extends SpellAbilityEffect {

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Player player = sa.getActivatingPlayer();
        final Game game = host.getGame();
        final int num = AbilityUtils.calculateAmount(host, sa.getParamOrDefault("Num", "0"), sa);

        GameEntityCounterTable table = new GameEntityCounterTable();

        for (final Player p : getTargetPlayers(sa)) {
            if (!p.isInGame()) continue;

            if (num >= 1) {
                p.addRadCounters(num, player, table);
            } else {
                p.removeRadCounters(-num);
            }
        }
        table.replaceCounterEffect(game, sa, true);
    }
}
