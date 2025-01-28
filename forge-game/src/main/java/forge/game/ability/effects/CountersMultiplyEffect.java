package forge.game.ability.effects;

import java.util.Map;

import forge.game.Game;
import forge.game.GameEntity;
import forge.game.GameEntityCounterTable;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CounterType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.Lang;

public class CountersMultiplyEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        final CounterType counterType = getCounterType(sa);

        sb.append("Double the number of ");

        if (counterType != null) {
            sb.append(counterType.getName());
            sb.append(" counters");
        } else {
            sb.append("each kind of counter");
        }
        sb.append(" on ");

        sb.append(Lang.joinHomogenous(getTargetEntities(sa)));

        sb.append(".");

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Game game = host.getGame();
        final Player player = sa.getActivatingPlayer();

        final CounterType counterType = getCounterType(sa);
        final int n = Integer.parseInt(sa.getParamOrDefault("Multiplier", "2")) - 1;

        GameEntityCounterTable table = new GameEntityCounterTable();
        for (GameEntity ge : getTargetEntities(sa)) {
            if (ge instanceof Card) {
                Card gameCard = game.getCardState((Card) ge, null);
                // gameCard is LKI in that case, the card is not in game anymore
                // or the timestamp did change
                // this should check Self too
                if (gameCard == null || !((Card) ge).equalsWithGameTimestamp(gameCard)) {
                    continue;
                }
                ge = gameCard;
            }

            if (counterType != null) {
                ge.addCounter(counterType, ge.getCounters(counterType) * n, player, table);
            } else {
                for (Map.Entry<CounterType, Integer> e : ge.getCounters().entrySet()) {
                    ge.addCounter(e.getKey(), e.getValue() * n, player, table);
                }
            }
        }
        table.replaceCounterEffect(game, sa, true);
    }

    private CounterType getCounterType(SpellAbility sa) {
        if (sa.hasParam("CounterType")) {
            try {
                return CounterType.getType(sa.getParam("CounterType"));
            } catch (Exception e) {
                System.out.println("Counter type doesn't match, nor does an SVar exist with the type name.");
                return null;
            }
        }
        return null;
    }
}
