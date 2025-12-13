package forge.game.ability.effects;

import com.google.common.collect.Maps;

import forge.game.Game;
import forge.game.GameEntityCounterTable;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
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
        sb.append(" ");
        sb.append(amount);
        sb.append(". ");

        return sb.toString();
	}

    @Override
    public void resolve(SpellAbility sa) {
        final Card card = sa.getHostCard();
        final Game game = card.getGame();
        GameEntityCounterTable table = new GameEntityCounterTable();

        final int amount = AbilityUtils.calculateAmount(card, sa.getParamOrDefault("Num", "1"), sa);
		
		for (final Player p : getTargetPlayers(sa)) {
			Card tgt = p.getController().chooseSingleEntityForEffect(p.getCreaturesInPlay(), sa, Localizer.getInstance().getMessage("lblChooseaCard"), false, Maps.newHashMap());
			if (tgt == null) {
				continue;
			}
			
			tgt.addCounter(CounterEnumType.M1M1, amount, p, table);
		}

        table.replaceCounterEffect(game, sa, true);
	}
}
