/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Nate
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.gamemodes.quest.data;

import java.lang.reflect.InvocationTargetException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import forge.deck.Deck;
import forge.deck.DeckGroup;
import forge.gamemodes.quest.QuestDeckGroupMap;
import forge.gamemodes.quest.QuestDeckMap;
import forge.gamemodes.quest.QuestMode;
import forge.gamemodes.quest.QuestUtilCards;
import forge.gamemodes.quest.bazaar.QuestItemType;
import forge.gamemodes.quest.data.QuestPreferences.QPref;
import forge.item.InventoryItem;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.util.ItemPool;

/** */
public class QuestAssets {

    // Cards associated with quest
    /** The card pool. */
    private final ItemPool<PaperCard> cardPool = new ItemPool<>(PaperCard.class); // player's
    /** The credits. */
    private long credits; // this money is good for all modes
    // game
    // with

    // Decks collected by player
    /** The my decks. */
    private final HashMap<String, Deck> myDecks = new HashMap<>();
    // current
    // shop
    // list
    /** The new card list. */
    private final ItemPool<InventoryItem> newCardList = new ItemPool<>(InventoryItem.class); // cards
    // belonging
    /** The shop list. */
    private final ItemPool<InventoryItem> shopList = new ItemPool<>(InventoryItem.class); // the
    // gadgets

    /** The inventory items. */
    private final Map<QuestItemType, QuestItemCondition> inventoryItems = new EnumMap<>(
            QuestItemType.class);

    // Much the same like other map, but keyed by string (to support a lot of custom pets)
    private final Map<String, QuestItemCondition> combatPets = new HashMap<>();
    
    private final HashMap<String, DeckGroup> draftDecks = new HashMap<>();
    /**
     * Checks for item.
     *
     * @param itemType the item type
     * @return true, if successful
     */
    public final boolean hasItem(final QuestItemType itemType) {
        return this.inventoryItems.containsKey(itemType) && (this.inventoryItems.get(itemType).getLevel() > 0);
    }

    /**
     * Gets the item level.
     *
     * @param itemType the item type
     * @return the item level
     */
    public final int getItemLevel(final QuestItemType itemType) {
        final QuestItemCondition state = this.inventoryItems.get(itemType);
        return state == null ? 0 : state.getLevel();
    }

    /**
     * Gets the item condition.
     *
     * @param itemType the item type
     * @param <T> extends QuestItemCondition
     * @return T the item condition
     */
    @SuppressWarnings("unchecked")
    public final <T extends QuestItemCondition> T getItemCondition(final QuestItemType itemType) {
        QuestItemCondition current = this.inventoryItems.get(itemType);
        if (!current.getClass().equals(itemType.getModelClass())) {
            try {
                QuestItemCondition modern = itemType.getModelClass().getDeclaredConstructor().newInstance();
                modern.takeDataFrom(current);
                current = modern;
                inventoryItems.put(itemType, modern);
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }

        }
        return (T) current;
    }

    /**
     * Sets the item level.
     *
     * @param itemType the item type
     * @param level the level
     */
    public final void setItemLevel(final QuestItemType itemType, final int level) {
        QuestItemCondition cond = this.inventoryItems.get(itemType);
        if (null == cond) {
            try { // care to set appropriate state class here
                cond = itemType.getModelClass().getDeclaredConstructor().newInstance();
            } catch (final Exception e) {
                e.printStackTrace();
                cond = new QuestItemCondition();
            }
            this.inventoryItems.put(itemType, cond);
        }
        cond.setLevel(level);
    }

    /**
     * @param name String
     * @return int
     */
    public final int getPetLevel(final String name) {
        final QuestItemCondition state = this.combatPets.get(name);
        return state == null ? 0 : state.getLevel();
    }

    /**
     * @param name &emsp; String
     * @param <T> extends QuestItemCondition
     * @return <T>
     */
    @SuppressWarnings("unchecked")
    public final <T extends QuestItemCondition> T getPetCondition(final String name) {
        return (T) this.combatPets.get(name);
    }

    /**
     * @param name String
     * @param level int
     */
    public final void setPetLevel(final String name, final int level) {
        QuestItemCondition cond = this.combatPets.get(name);
        if (null == cond) {
            cond = new QuestItemCondition(); // pets have only level that should be serialized for now
            this.combatPets.put(name, cond);
        }
        cond.setLevel(level);
    }

    public QuestAssets() { //needed for XML serialization
    }

    /**
     * Instantiates a new quest assets.
     */
    public QuestAssets(GameFormatQuest useFormat) {
        final QuestPreferences prefs = FModel.getQuestPreferences();
        int snowLands = prefs.getPrefInt(QPref.STARTING_SNOW_LANDS);
        if (useFormat != null && !useFormat.hasSnowLands()) {
            snowLands = 0;
        }
        // Non-snow basic lands are no longer generated (we use Add Basic Lands)
        final ItemPool<PaperCard> lands = QuestUtilCards.generateBasicLands(
                /*prefs.getPrefInt(QPref.STARTING_BASIC_LANDS)*/0,  snowLands, useFormat);
        this.getCardPool().addAll(lands);
    }

    /**
     * Gets the credits.
     * 
     * @return the credits
     */
    public long getCredits() {
        return this.credits;
    }

    // Life (only fantasy)
    /**
     * Gets the life.
     *
     * @param mode the mode
     * @return the life
     */
    public int getLife(final QuestMode mode) {
        int base = mode.equals(QuestMode.Fantasy) ? 15 : 20;

        //Modify life for the quest's sub-format, e.g.: Commander adds 20
        switch(FModel.getQuest().getDeckConstructionRules()){
            case Default: break;
            case Commander: base += 20;
        }

        return (base + this.getItemLevel(QuestItemType.ELIXIR_OF_LIFE)) - this.getItemLevel(QuestItemType.POUND_FLESH);
    }

    /**
     * Gets the new card list.
     * 
     * @return the newCardList
     */
    public ItemPool<InventoryItem> getNewCardList() {
        return this.newCardList;
    }

    /**
     * Gets the shop list.
     * 
     * @return the shopList
     */
    public ItemPool<InventoryItem> getShopList() {
        return this.shopList;
    }

    /**
     * Sets the credits.
     * 
     * @param credits0
     *            the credits to set
     */
    public void setCredits(final long credits0) {
        this.credits = credits0;
    }

    // Credits
    /**
     * Adds the credits.
     * 
     * @param c
     *            the c
     */
    public void addCredits(final long c) {
        this.setCredits(this.getCredits() + c);
    }

    /**
     * Gets the card pool.
     * 
     * @return the cardPool
     */
    public ItemPool<PaperCard> getCardPool() {
        return this.cardPool;
    }

    /**
     * Subtract credits.
     * 
     * @param c
     *            the c
     */
    public void subtractCredits(final long c) {
        this.setCredits(this.getCredits() > c ? this.getCredits() - c : 0);
    }

    /**
     * @return the deck storage
     */
    public QuestDeckMap getDeckStorage() {
        return new QuestDeckMap(this.myDecks);
    }

    /**
     * @return the tournament deck storage
     */
    public QuestDeckGroupMap getDraftDeckStorage() {
        return new QuestDeckGroupMap(this.draftDecks);
    }

}
