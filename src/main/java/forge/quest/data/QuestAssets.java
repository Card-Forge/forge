package forge.quest.data;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import forge.Singletons;
import forge.deck.Deck;
import forge.item.CardPrinted;
import forge.item.InventoryItem;
import forge.item.ItemPool;
import forge.item.ItemPoolView;
import forge.quest.QuestDeckMap;
import forge.quest.QuestUtilCards;
import forge.quest.data.QuestPreferences.QPref;
import forge.quest.data.item.QuestItemType;
import forge.quest.data.pet.QuestPetManager;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class QuestAssets {

    // Cards associated with quest
    /** The card pool. */
    final ItemPool<CardPrinted> cardPool = new ItemPool<CardPrinted>(CardPrinted.class); // player's
    /** The credits. */
    long credits; // this money is good for all modes
    // game
    // with

    // Decks collected by player
    /** The my decks. */
    final HashMap<String, Deck> myDecks = new HashMap<String, Deck>();
    // current
    // shop
    // list
    /** The new card list. */
    final ItemPool<InventoryItem> newCardList = new ItemPool<InventoryItem>(InventoryItem.class); // cards
    // belonging
    /** The shop list. */
    final ItemPool<InventoryItem> shopList = new ItemPool<InventoryItem>(InventoryItem.class); // the
    // gadgets

    /** The pet manager. */
    final QuestPetManager petManager = new QuestPetManager(); // pets

    
    final Map<QuestItemType, QuestItemCondition> inventoryItems = new EnumMap<QuestItemType, QuestItemCondition>(QuestItemType.class);


    public final boolean hasItem(final QuestItemType itemType) {
        return this.inventoryItems.containsKey(itemType) && (this.inventoryItems.get(itemType).getLevel() > 0);
    }


    public final int getItemLevel(final QuestItemType itemType) {
        final QuestItemCondition state = this.inventoryItems.get(itemType);
        return state == null ? 0 : state.getLevel();
    }

    public final QuestItemCondition getItemCondition(final QuestItemType itemType) {
        return this.inventoryItems.get(itemType);
    }
    

    public final void setItemLevel(final QuestItemType itemType, final int level) {
        QuestItemCondition cond = this.inventoryItems.get(itemType);
        if( null == cond ) {
            try { // care to set appropriate state class here
                cond = (QuestItemCondition) itemType.getModelClass().newInstance();
            } catch (Exception e) {
                // TODO Auto-generated catch block ignores the exception, but sends it to System.err and probably forge.log.
                e.printStackTrace();
                cond = new QuestItemCondition();
            } 
            this.inventoryItems.put(itemType, cond); 
        }
        cond.setLevel(level);
    }
    
    public QuestAssets() {
        final QuestPreferences prefs = Singletons.getModel().getQuestPreferences();
        final ItemPoolView<CardPrinted> lands = QuestUtilCards.generateBasicLands(
                prefs.getPreferenceInt(QPref.STARTING_BASIC_LANDS), prefs.getPreferenceInt(QPref.STARTING_SNOW_LANDS));
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
     * @return the life
     */
    public int getLife(QuestMode mode) {
        int base = mode.equals(QuestMode.Fantasy) ? 15 : 20;
        return base + getItemLevel(QuestItemType.ELIXIR_OF_LIFE) - getItemLevel(QuestItemType.POUND_FLESH);
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

    /**
     * Gets the pet manager.
     * 
     * @return the pet manager
     */
    public QuestPetManager getPetManager() {
        return this.petManager;
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
    public ItemPool<CardPrinted> getCardPool() {
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
     * TODO: Write javadoc for this method.
     * @return
     */
    public QuestDeckMap getDeckStorage() {
        return new QuestDeckMap(myDecks);
    }

}
