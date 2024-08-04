package forge.game.ability.effects;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;

import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardZoneTable;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.collect.FCollectionView;

/**
 * TODO: Write javadoc for this type.
 *
 */
public class BalanceEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.card.ability.SpellAbilityEffect#resolve(forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        Player activator = sa.getActivatingPlayer();
        Card source = sa.getHostCard();
        Game game = activator.getGame();
        String valid = sa.getParamOrDefault("Valid", "Card");
        ZoneType zone = sa.hasParam("Zone") ? ZoneType.smartValueOf(sa.getParam("Zone")) : ZoneType.Battlefield;

        int min = Integer.MAX_VALUE;

        final FCollectionView<Player> players = game.getPlayersInTurnOrder();
        final List<CardCollection> validCards = new ArrayList<>(players.size());
        Map<Player, CardCollectionView> discardedMap = Maps.newHashMap();

        for (int i = 0; i < players.size(); i++) {
            // Find the minimum of each Valid per player
            validCards.add(CardLists.getValidCards(players.get(i).getCardsIn(zone), valid, activator, source, sa));
            min = Math.min(min, validCards.get(i).size());
        }

        Map<AbilityKey, Object> params = AbilityKey.newMap();
        CardZoneTable table = AbilityKey.addCardZoneTableParams(params, sa);

        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            int numToBalance = validCards.get(i).size() - min;
            if (numToBalance == 0) {
                continue;
            }
            if (zone.equals(ZoneType.Hand)) {
                discardedMap.put(p, p.getController().chooseCardsToDiscardFrom(p, sa, validCards.get(i), numToBalance, numToBalance));
            } else { // Battlefield
                CardCollectionView list = p.getController().choosePermanentsToSacrifice(sa, numToBalance, numToBalance, validCards.get(i), valid);
                game.getAction().sacrifice(list, sa, true, params);
            }
        }

        if (zone.equals(ZoneType.Hand)) {
            discard(sa, true, discardedMap, params);
        }

        table.triggerChangesZoneAll(game, sa);
    }
}
