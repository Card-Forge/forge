package forge.quest.data;

import java.util.HashMap;

import forge.Singletons;
import forge.deck.Deck;
import forge.item.CardPrinted;
import forge.item.InventoryItem;
import forge.item.ItemPool;
import forge.item.ItemPoolView;
import forge.quest.data.QuestPreferences.QPref;
import forge.quest.data.item.QuestInventory;
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
    /** The inventory. */
    final QuestInventory inventory = new QuestInventory(); // different
    /** The life. */
    int life; // for fantasy mode, how much life bought at shop to start
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
    /**
     * Adds n life to maximum.
     * 
     * @param n
     *            &emsp; int
     */
    public void addLife(final int n) {
        this.life += n;
    }
    public QuestAssets(QuestMode mode) {
        this.life = mode.equals(QuestMode.Fantasy) ? 15 : 20;
        

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
    // All belongings
    /**
     * Gets the inventory.
     * 
     * @return the inventory
     */
    public QuestInventory getInventory() {
        return this.inventory;
    }
    // Life (only fantasy)
    /**
     * Gets the life.
     * 
     * @return the life
     */
    public int getLife() {
        return this.life;
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
     * Removes n life from maximum.
     * 
     * @param n
     *            &emsp; int
     */
    public void removeLife(final int n) {
        this.life -= n;
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
    
}
