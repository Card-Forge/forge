package forge.game.ability.effects;

import com.google.common.collect.Maps;

import forge.game.Game;
import forge.game.GameEntityCounterTable;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardPredicates;
import forge.game.card.CounterEnumType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.Lang;
import forge.util.Localizer;

import java.util.List;

public class BlightEffect extends SpellAbilityEffect {
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final Card card = sa.getHostCard();
        final StringBuilder sb = new StringBuilder();

        List<Player> tgt = getTargetPlayers(sa);
        final int amount = AbilityUtils.calculateAmount(card, sa.getParamOrDefault("Num", "1"), sa);

        sb.append(Lang.joinHomogenous(tgt));
        sb.append(" ");
        sb.append(tgt.size() > 1 ? "blights" : "blight");
        sb.append(" ").append(amount).append(". ");

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Game game = host.getGame();
        GameEntityCounterTable table = new GameEntityCounterTable();

        final int amount = AbilityUtils.calculateAmount(host, sa.getParamOrDefault("Num", "1"), sa);

        for (final Player p : getTargetPlayers(sa)) {
            CardCollection options = p.getCreaturesInPlay()
                    .filter(CardPredicates.canReceiveCounters(CounterEnumType.M1M1));
            Card tgt = p.getController().chooseSingleEntityForEffect(options, sa,
                    Localizer.getInstance().getMessage("lblChooseaCard"), false, Maps.newHashMap());
            if (tgt == null) {
                continue;
            }

            tgt.addCounter(CounterEnumType.M1M1, amount, p, table);
        }

        table.replaceCounterEffect(game, sa, true);
    }
}
