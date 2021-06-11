package forge.game.ability.effects;

import java.util.List;

import forge.game.Game;
import forge.game.GameEntity;
import forge.game.GameEntityCounterTable;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.card.CounterType;
import forge.game.player.Player;
import forge.game.player.PlayerController;
import forge.game.player.PlayerPredicates;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Localizer;
import forge.util.collect.FCollection;

public class CountersProliferateEffect extends SpellAbilityEffect {
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        sb.append("Proliferate.");
        sb.append(" (Choose any number of permanents and/or players,");
        sb.append(" then give each another counter of each kind already there.)");

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Player p = sa.getActivatingPlayer();
        final Card host = sa.getHostCard();
        final Game game = host.getGame();

        PlayerController pc = p.getController();

        FCollection<GameEntity> list = new FCollection<>();

        list.addAll(game.getPlayers().filter(PlayerPredicates.hasCounters()));
        list.addAll(CardLists.filter(game.getCardsIn(ZoneType.Battlefield), CardPredicates.hasCounters()));

        List<GameEntity> result = pc.chooseEntitiesForEffect(list, 0, list.size(), null, sa,
                Localizer.getInstance().getMessage("lblChooseProliferateTarget"), p, null);

        GameEntityCounterTable table = new GameEntityCounterTable();
        for (final GameEntity ge : result) {
            for (final CounterType ct : ge.getCounters().keySet()) {
                ge.addCounter(ct, 1, p, sa, true, true, table);
            }
            if (ge instanceof Card) {
                Card c = (Card) ge;
                game.updateLastStateForCard(c);
            }
        }
        table.triggerCountersPutAll(game);
    }
}
