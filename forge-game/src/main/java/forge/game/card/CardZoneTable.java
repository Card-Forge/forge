/**
 * 
 */
package forge.game.card;

import java.util.Map;

import com.google.common.collect.*;

import forge.game.CardTraitBase;
import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.player.PlayerCollection;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;

public class CardZoneTable extends ForwardingTable<ZoneType, ZoneType, CardCollection> {
    // TODO use EnumBasedTable if exist
    private Table<ZoneType, ZoneType, CardCollection> dataMap = HashBasedTable.create();

    private CardCollection createdTokens = new CardCollection();
    private PlayerCollection firstTimeTokenCreators = new PlayerCollection();

    private CardCollectionView lastStateBattlefield;
    private CardCollectionView lastStateGraveyard;
    
    public CardZoneTable(CardZoneTable cardZoneTable) {
        this.putAll(cardZoneTable);
        lastStateBattlefield = cardZoneTable.getLastStateBattlefield();
        lastStateGraveyard = cardZoneTable.getLastStateGraveyard();
    }

    public CardZoneTable() {
        this(CardCollection.EMPTY, CardCollection.EMPTY);
    }

    public CardZoneTable(CardCollectionView lastStateBattlefield, CardCollectionView lastStateGraveyard) {
        this.lastStateBattlefield = lastStateBattlefield;
        this.lastStateGraveyard = lastStateGraveyard;
    }

    public CardCollectionView getLastStateBattlefield() {
        return lastStateBattlefield;
    }
    public CardCollectionView getLastStateGraveyard() {
        return lastStateGraveyard;
    }
    public void setLastStateBattlefield(CardCollectionView lastState) {
        this.lastStateBattlefield = lastState;
    }
    public void setLastStateGraveyard(CardCollectionView lastState) {
        this.lastStateGraveyard = lastState;
    }

    /**
     * special put logic, add Card to Card Collection
     */
    public CardCollection put(ZoneType rowKey, ZoneType columnKey, Card value) {
        if (rowKey == null) {
            rowKey = ZoneType.None;
        }
        if (columnKey == null) {
            columnKey = ZoneType.None;
        }
        CardCollection old;
        if (contains(rowKey, columnKey)) {
            old = get(rowKey, columnKey);
            old.add(value);
        } else {
            old = new CardCollection(value);
            delegate().put(rowKey, columnKey, old);
        }
        return old;
    }

    @Override
    protected Table<ZoneType, ZoneType, CardCollection> delegate() {
        return dataMap;
    }

    public void triggerChangesZoneAll(final Game game, final SpellAbility cause) {
        triggerTokenCreatedOnce(game);
        if (!isEmpty()) {
            final Map<AbilityKey, Object> runParams = AbilityKey.newMap();
            runParams.put(AbilityKey.Cards, new CardZoneTable(this));
            runParams.put(AbilityKey.Cause, cause);
            game.getTriggerHandler().runTrigger(TriggerType.ChangesZoneAll, runParams, false);
        }
        final CardZoneTable untilTable = game.getUntilHostLeavesPlayTriggerList();
        if (this != untilTable) {
            untilTable.triggerChangesZoneAll(game, null);
            untilTable.clear();
        }
    }

    private void triggerTokenCreatedOnce(Game game) {
        if (!createdTokens.isEmpty()) {
            final Map<AbilityKey, Object> runParams = AbilityKey.newMap();
            runParams.put(AbilityKey.Cards, createdTokens);
            runParams.put(AbilityKey.FirstTime, firstTimeTokenCreators);
            game.getTriggerHandler().runTrigger(TriggerType.TokenCreatedOnce, runParams, false);
        }
    }

    public CardCollection filterCards(Iterable<ZoneType> origin, ZoneType destination, String valid, Card host, CardTraitBase sa) {
        CardCollection allCards = new CardCollection();
        if (destination != null) {
            if (!containsColumn(destination)) {
                return allCards;
            }
        }
        if (origin != null) {
            for (ZoneType z : origin) {
                CardCollectionView lkiLookup = CardCollection.EMPTY;
                if (z == ZoneType.Battlefield) {
                    lkiLookup = lastStateBattlefield;
                }
                if (containsRow(z)) {
                    if (destination != null) {
                        for (Card c : row(z).get(destination)) {
                            allCards.add(lkiLookup.get(c));
                        }
                    } else {
                        for (CardCollection cc : row(z).values()) {
                            for (Card c : cc) {
                                allCards.add(lkiLookup.get(c));
                            }
                        }
                    }
                }
            }
        } else if (destination != null) {
            for (CardCollection c : column(destination).values()) {
                allCards.addAll(c);
            }
        } else {
            for (CardCollection c : values()) {
                allCards.addAll(c);
            }
        }

        if (valid != null) {
            allCards = CardLists.getValidCards(allCards, valid, host.getController(), host, sa);
        }
        return allCards;
    }

    public Iterable<Card> allCards() {
        return Iterables.concat(values());
    }

    public void addToken(Card c, boolean firstTime) {
        createdTokens.add(c);
        if (firstTime) {
            firstTimeTokenCreators.add(c.getOwner());
        }
    }
}
