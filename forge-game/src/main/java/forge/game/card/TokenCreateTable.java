package forge.game.card;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ObjectUtils;

import com.google.common.collect.ForwardingTable;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;

import forge.game.CardTraitBase;
import forge.game.GameObjectPredicates;
import forge.game.player.Player;

public class TokenCreateTable extends ForwardingTable<Player, Card, Integer> {

    Table<Player, Card, Integer> dataMap = HashBasedTable.create();
    
    public TokenCreateTable() {
    }

    @Override
    protected Table<Player, Card, Integer> delegate() {
        return dataMap;
    }

    public int add(Player p, Card c, int i) {
        int old = ObjectUtils.defaultIfNull(this.get(p, c), 0);
        int newValue = old + i;
        this.put(p, c, newValue);
        return newValue;
    }

    public int getFilterAmount(String validOwner, String validToken, final CardTraitBase ctb) {
        final Card host = ctb.getHostCard();
        int result = 0;
        List<Card> filteredCards = null;
        List<Player> filteredPlayer = null;

        if (validOwner == null && validToken == null) {
            for (Integer i : values()) {
                result += i;
            }
            return result;
        }

        if (validOwner != null) {
            filteredPlayer = Lists.newArrayList(Iterables.filter(rowKeySet(),
                    GameObjectPredicates.restriction(validOwner.split(","), host.getController(), host, ctb)));
            if (filteredPlayer.isEmpty()) {
                return 0;
            }
        }
        if (validToken != null) {
            filteredCards = CardLists.getValidCardsAsList(columnKeySet(), validToken, host.getController(), host, ctb);
            if (filteredCards.isEmpty()) {
                return 0;
            }
        }

        if (filteredPlayer == null) {
            for (Map.Entry<Card, Map<Player, Integer>> e : columnMap().entrySet()) {
                for (Integer i : e.getValue().values()) {
                    result += i;
                }
            }
            return result;
        }

        if (filteredCards == null) {
            for (Map.Entry<Player, Map<Card, Integer>> e : rowMap().entrySet()) {
                for (Integer i : e.getValue().values()) {
                    result += i;
                }
            }
            return result;
        }

        for (Table.Cell<Player, Card, Integer> c : this.cellSet()) {
            if (!filteredPlayer.contains(c.getRowKey())) {
                continue;
            }
            if (!filteredCards.contains(c.getColumnKey())) {
                continue;
            }
            result += c.getValue();
        }

        return result;
    }
}
