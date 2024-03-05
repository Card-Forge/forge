/**
 * 
 */
package forge.game.card;

import java.util.Map;

import org.apache.commons.lang3.ObjectUtils;

import com.google.common.collect.*;

import forge.game.CardTraitBase;
import forge.game.Game;
import forge.game.GameAction;
import forge.game.ability.AbilityKey;
import forge.game.player.PlayerCollection;
import forge.game.replacement.ReplacementType;
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
    
    public CardZoneTable() {
        this(null, null);
    }

    public CardZoneTable(CardCollectionView lastStateBattlefield, CardCollectionView lastStateGraveyard) {
        setLastStateBattlefield(ObjectUtils.firstNonNull(lastStateBattlefield, CardCollection.EMPTY));
        setLastStateGraveyard(ObjectUtils.firstNonNull(lastStateGraveyard, CardCollection.EMPTY));
    }

    public CardZoneTable(CardZoneTable cardZoneTable) {
        this.putAll(cardZoneTable);
        lastStateBattlefield = cardZoneTable.getLastStateBattlefield();
        lastStateGraveyard = cardZoneTable.getLastStateGraveyard();
    }

    public static CardZoneTable getSimultaneousInstance(SpellAbility sa) {
        if (sa.isReplacementAbility() && sa.getReplacementEffect().getMode() == ReplacementType.Moved
                && sa.getReplacingObject(AbilityKey.InternalTriggerTable) != null) {
            // if a RE changes the destination zone try to make it simultaneous
            return (CardZoneTable) sa.getReplacingObject(AbilityKey.InternalTriggerTable);    
        }
        GameAction ga = sa.getHostCard().getGame().getAction();
        return new CardZoneTable(
                ga.getLastState(AbilityKey.LastStateBattlefield, sa, null, true),
                ga.getLastState(AbilityKey.LastStateGraveyard, sa, null, true));
    }

    public CardCollectionView getLastStateBattlefield() {
        return lastStateBattlefield;
    }
    public CardCollectionView getLastStateGraveyard() {
        return lastStateGraveyard;
    }
    public void setLastStateBattlefield(CardCollectionView lastState) {
        // store it in a new object, it might be from Game which can also refresh itself
        this.lastStateBattlefield = new CardCollection(lastState);
    }
    public void setLastStateGraveyard(CardCollectionView lastState) {
        this.lastStateGraveyard = new CardCollection(lastState);
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
        if (cause != null && cause.getReplacingObject(AbilityKey.InternalTriggerTable) == this) {
            // will be handled by original "cause" instead
            return;
        }
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
        if (destination != null && !containsColumn(destination)) {
            return allCards;
        }
        if (origin != null) {
            for (ZoneType z : origin) {
                if (containsRow(z)) {
                    CardCollectionView lkiLookup = CardCollection.EMPTY;
                    // CR 603.10a
                    if (z == ZoneType.Battlefield) {
                        lkiLookup = lastStateBattlefield;
                    }
                    if (z == ZoneType.Graveyard && destination == null) {
                        lkiLookup = lastStateGraveyard;
                    }
                    if (destination != null) {
                        if (row(z).containsKey(destination)) {
                            for (Card c : row(z).get(destination)) {
                                allCards.add(lkiLookup.get(c));
                            }
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
