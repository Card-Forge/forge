package forge.adventure.player;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Null;
import com.google.common.collect.Lists;
import forge.adventure.data.DifficultyData;
import forge.adventure.data.EffectData;
import forge.adventure.data.HeroListData;
import forge.adventure.data.ItemData;
import forge.adventure.util.*;
import forge.adventure.world.WorldSave;
import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckProxy;
import forge.deck.DeckSection;
import forge.item.InventoryItem;
import forge.item.PaperCard;
import forge.util.ItemPool;
import forge.util.MyRandom;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Class that represents the player (not the player sprite)
 */
public class AdventurePlayer implements Serializable, SaveFileContent {
    public static final int NUMBER_OF_DECKS=10;
    // Player profile data.
    private String name;
    private int heroRace;
    private int avatarIndex;
    private boolean isFemale;
    private ColorSet colorIdentity = ColorSet.ALL_COLORS;

    // Deck data
    private Deck deck;
    private final Deck[] decks = new Deck[NUMBER_OF_DECKS];
    private int selectedDeckIndex = 0;
    private final DifficultyData difficultyData = new DifficultyData();

    // Game data.
    private float worldPosX;
    private float worldPosY;
    private int gold   =  0;
    private int maxLife= 20;
    private int life   = 20;
    private EffectData blessing; //Blessing to apply for next battle.
    private final PlayerStatistic statistic    = new PlayerStatistic();
    private final Map<String, Byte> questFlags = new HashMap<>();

    private final Array<String> inventoryItems=new Array<>();
    private final HashMap<String,String> equippedItems=new HashMap<>();

    // Fantasy/Chaos mode settings.
    private boolean fantasyMode     = false;
    private boolean announceFantasy = false;

    // Signals
    SignalList onLifeTotalChangeList = new SignalList();
    SignalList onGoldChangeList      = new SignalList();
    SignalList onPlayerChangeList    = new SignalList();
    SignalList onEquipmentChange     = new SignalList();
    SignalList onBlessing            = new SignalList();

    public AdventurePlayer() { clear(); }

    public PlayerStatistic getStatistic(){ return statistic; }

    private void clearDecks() {
        for(int i=0; i < NUMBER_OF_DECKS; i++) decks[i] = new Deck("Empty Deck");
        deck              = decks[0];
        selectedDeckIndex = 0;
    }

    private void clear() {
        //Ensure sensitive gameplay data is properly reset between games.
        //Reset all properties HERE.
        fantasyMode       = false;
        announceFantasy   = false;
        blessing          = null;
        gold              = 0;
        maxLife           = 20;
        life              = 20;
        clearDecks();
        inventoryItems.clear();
        equippedItems.clear();
        questFlags.clear();
        cards.clear();
        statistic.clear();
        newCards.clear();
    }


    static public AdventurePlayer current() {
        return WorldSave.getCurrentSave().getPlayer();
    }

    private final CardPool cards=new CardPool();
    private final ItemPool<InventoryItem> newCards=new ItemPool<>(InventoryItem.class);

    public void create(String n,   Deck startingDeck, boolean male, int race, int avatar, boolean isFantasy, DifficultyData difficultyData) {
        clear();
        announceFantasy = fantasyMode = isFantasy; //Set Chaos mode first.

        deck     = startingDeck;
        decks[0] = deck;

        cards.addAllFlat(deck.getAllCardsInASinglePool().toFlatList());

        this.difficultyData.startingLife       = difficultyData.startingLife;
        this.difficultyData.staringMoney       = difficultyData.staringMoney;
        this.difficultyData.startingDifficulty = difficultyData.startingDifficulty;
        this.difficultyData.name               = difficultyData.name;
        this.difficultyData.spawnRank          = difficultyData.spawnRank;
        this.difficultyData.enemyLifeFactor    = difficultyData.enemyLifeFactor;
        this.difficultyData.sellFactor         = difficultyData.sellFactor;

        gold        = difficultyData.staringMoney;
        name        = n;
        heroRace    = race;
        avatarIndex = avatar;
        isFemale    = !male;

        setColorIdentity(DeckProxy.getColorIdentity(deck));

        life = maxLife = difficultyData.startingLife;

        inventoryItems.addAll(difficultyData.startItems);
        onGoldChangeList.emit();
        onLifeTotalChangeList.emit();
    }

    public void setSelectedDeckSlot(int slot) {
        if(slot>=0&&slot<NUMBER_OF_DECKS) {
            selectedDeckIndex = slot;
            deck = decks[selectedDeckIndex];
            setColorIdentity(DeckProxy.getColorIdentity(deck));
        }
    }
    public void updateDifficulty(DifficultyData diff) {
        maxLife = diff.startingLife;
        this.difficultyData.startingLife = diff.startingLife;
        this.difficultyData.staringMoney = diff.staringMoney;
        this.difficultyData.startingDifficulty = diff.startingDifficulty;
        this.difficultyData.name = diff.name;
        this.difficultyData.spawnRank = diff.spawnRank;
        this.difficultyData.enemyLifeFactor = diff.enemyLifeFactor;
        this.difficultyData.sellFactor = diff.sellFactor;
        fullHeal();
    }

    //Getters
    public int getSelectedDeckIndex()     { return selectedDeckIndex; }
    public Deck getSelectedDeck()         { return deck;              }
    public Array<String> getItems()       { return inventoryItems;    }
    public Deck getDeck(int index)        { return decks[index];      }
    public CardPool getCards()            { return cards;             }
    public String getName()               { return name;              }
    public float getWorldPosX()           { return worldPosX;         }
    public float getWorldPosY()           { return worldPosY;         }
    public int getGold()                  { return gold;              }
    public int getLife()                  { return life;              }
    public int getMaxLife()               { return maxLife;           }
    public @Null EffectData getBlessing() { return blessing;          }

    public Collection<String> getEquippedItems() { return equippedItems.values(); }
    public ItemPool<InventoryItem> getNewCards() { return newCards;               }

    public ColorSet getColorIdentity(){
        return colorIdentity;
    }

    public String getColorIdentityLong(){
        return colorIdentity.toString();
    }


    //Setters
    public void setWorldPosX(float worldPosX) {
        this.worldPosX = worldPosX;
    }
    public void setWorldPosY(float worldPosY) {
        this.worldPosY = worldPosY;
    }

    public void setColorIdentity(String C){
        colorIdentity= ColorSet.fromNames(C.toCharArray());
    }

    public void setColorIdentity(ColorSet set){
        this.colorIdentity = set;
    }




    @Override
    public void load(SaveFileData data) {
        clear(); //Reset player data.
        this.statistic.load(data.readSubData("statistic"));
        this.difficultyData.startingLife=data.readInt("startingLife");
        this.difficultyData.staringMoney=data.readInt("staringMoney");
        this.difficultyData.startingDifficulty=data.readBool("startingDifficulty");
        this.difficultyData.name=data.readString("difficultyName");
        this.difficultyData.enemyLifeFactor=data.readFloat("enemyLifeFactor");
        this.difficultyData.sellFactor=data.readFloat("sellFactor");
        if(this.difficultyData.sellFactor==0)
            this.difficultyData.sellFactor=0.2f;

        name        = data.readString("name");
        heroRace    = data.readInt("heroRace");
        avatarIndex = data.readInt("avatarIndex");
        isFemale    = data.readBool("isFemale");
        if(data.containsKey("colorIdentity"))
            setColorIdentity(data.readString("colorIdentity"));
        else
            colorIdentity = ColorSet.ALL_COLORS;

        gold        = data.readInt("gold");
        maxLife     = data.readInt("maxLife");
        life        = data.readInt("life");
        worldPosX   = data.readFloat("worldPosX");
        worldPosY   = data.readFloat("worldPosY");

        if(data.containsKey("blessing")) blessing = (EffectData)data.readObject("blessing");

        if(data.containsKey("inventory")) {
            String[] inv=(String[])data.readObject("inventory");
            //Prevent items with wrong names from getting through. Hell breaks loose if it causes null pointers.
            //This only needs to be done on load.
            for(String i : inv){
                if(ItemData.getItem(i) != null) inventoryItems.add(i);
                else {
                    System.err.printf("Cannot find item name %s\n", i);
                    //Allow official© permission for the player to get a refund. We will allow it this time.
                    //TODoooo: Divine retribution if the player refunds too much. Use the orbital laser cannon.
                    System.out.println("Developers have blessed you! You are allowed to cheat the cost of the item back!");
                }
            }
        }
        if(data.containsKey("equippedSlots") && data.containsKey("equippedItems")) {
            String[] slots=(String[])data.readObject("equippedSlots");
            String[] items=(String[])data.readObject("equippedItems");

            assert(slots.length==items.length);
            //Like above, prevent items with wrong names. If it triggered in inventory it'll trigger here as well.
            for(int i=0;i<slots.length;i++) {
                if(ItemData.getItem(items[i]) != null)
                    equippedItems.put(slots[i],items[i]);
                else {
                    System.err.printf("Cannot find equip name %s\n", items[i]);
                }
            }
        }

        deck = new Deck(data.readString("deckName"));
        deck.getMain().addAll(CardPool.fromCardList(Lists.newArrayList((String[])data.readObject("deckCards"))));
        if(data.containsKey("sideBoardCards"))
            deck.getOrCreate(DeckSection.Sideboard).addAll(CardPool.fromCardList(Lists.newArrayList((String[])data.readObject("sideBoardCards"))));

        if(data.containsKey("questFlagsKey") && data.containsKey("questFlagsValue")){
            String[] keys = (String[]) data.readObject("questFlagsKey");
            Byte[] values = (Byte[]) data.readObject("questFlagsValue");
            assert( keys.length == values.length );
            for( int i = 0; i < keys.length; i++){
                questFlags.put(keys[i], values[i]);
            }
        }

        for(int i=0;i<NUMBER_OF_DECKS;i++) {
            if(!data.containsKey("deck_name_" + i)) {
                if(i==0) decks[i] = deck;
                else     decks[i] = new Deck("Empty Deck");
                continue;
            }
            decks[i] = new Deck(data.readString("deck_name_"+i));
            decks[i].getMain().addAll(CardPool.fromCardList(Lists.newArrayList((String[])data.readObject("deck_"+i))));
            if(data.containsKey("sideBoardCards_"+i))
                decks[i].getOrCreate(DeckSection.Sideboard).addAll(CardPool.fromCardList(Lists.newArrayList((String[])data.readObject("sideBoardCards_"+i))));
        }
        setSelectedDeckSlot(data.readInt("selectedDeckIndex"));
        cards.addAll(CardPool.fromCardList(Lists.newArrayList((String[])data.readObject("cards"))));

        fantasyMode     = data.containsKey("fantasyMode")     ? data.readBool("fantasyMode")     : false;
        announceFantasy = data.containsKey("announceFantasy") ? data.readBool("announceFantasy") : false;

        onLifeTotalChangeList.emit();
        onGoldChangeList.emit();
        onBlessing.emit();
    }

    @Override
    public SaveFileData save() {
        SaveFileData data= new SaveFileData();

        data.store("statistic",this.statistic.save());
        data.store("startingLife",this.difficultyData.startingLife);
        data.store("staringMoney",this.difficultyData.staringMoney);
        data.store("startingDifficulty",this.difficultyData.startingDifficulty);
        data.store("difficultyName",this.difficultyData.name);
        data.store("enemyLifeFactor",this.difficultyData.enemyLifeFactor);
        data.store("sellFactor",this.difficultyData.sellFactor);

        data.store("name",name);
        data.store("heroRace",heroRace);
        data.store("avatarIndex",avatarIndex);
        data.store("isFemale",isFemale);
        data.store("colorIdentity", colorIdentity.getColor());

        data.store("fantasyMode",fantasyMode);
        data.store("announceFantasy",announceFantasy);

        data.store("worldPosX",worldPosX);
        data.store("worldPosY",worldPosY);
        data.store("gold",gold);
        data.store("life",life);
        data.store("maxLife",maxLife);
        data.store("deckName",deck.getName());

        data.storeObject("inventory",inventoryItems.toArray(String.class));

        ArrayList<String> slots=new ArrayList<>();
        ArrayList<String> items=new ArrayList<>();
        for (Map.Entry<String,String>  entry : equippedItems.entrySet()) {
            slots.add(entry.getKey());
            items.add(entry.getValue());
        }
        data.storeObject("equippedSlots",slots.toArray(new String[0]));
        data.storeObject("equippedItems",items.toArray(new String[0]));

        data.storeObject("blessing", blessing);

        //Save quest flags.
        ArrayList<String> questFlagsKey = new ArrayList<>();
        ArrayList<Byte> questFlagsValue  = new ArrayList<>();
        for(Map.Entry<String, Byte> entry : questFlags.entrySet()){
            questFlagsKey.add(entry.getKey());
            questFlagsValue.add(entry.getValue());
        }
        data.storeObject("questFlagsKey", questFlagsKey.toArray(new String[0]));
        data.storeObject("questFlagsValue", questFlagsValue.toArray(new Byte[0]));

        data.storeObject("deckCards",deck.getMain().toCardList("\n").split("\n"));
        if(deck.get(DeckSection.Sideboard)!=null)
            data.storeObject("sideBoardCards",deck.get(DeckSection.Sideboard).toCardList("\n").split("\n"));
        for(int i=0;i<NUMBER_OF_DECKS;i++) {
            data.store("deck_name_"+i,decks[i].getName());
            data.storeObject("deck_"+i,decks[i].getMain().toCardList("\n").split("\n"));
            if(decks[i].get(DeckSection.Sideboard)!=null)
                data.storeObject("sideBoardCards_"+i,decks[i].get(DeckSection.Sideboard).toCardList("\n").split("\n"));
        }
        data.store("selectedDeckIndex",selectedDeckIndex);
        data.storeObject("cards",cards.toCardList("\n").split("\n"));

        return data;
    }

    public String spriteName() {
        return HeroListData.getHero(heroRace, isFemale);
    }

    public FileHandle sprite() {
        return Config.instance().getFile(HeroListData.getHero(heroRace, isFemale));
    }

    public TextureRegion avatar() {
        return HeroListData.getAvatar(heroRace, isFemale, avatarIndex);
    }

    public void addCard(PaperCard card) {
        cards.add(card);
        newCards.add(card);
    }

    public void addReward(Reward reward) {
        switch (reward.getType()) {
            case Card:
                cards.add(reward.getCard());
                newCards.add(reward.getCard());
                break;
            case Gold:
                addGold(reward.getCount());
                break;
            case Item:
                inventoryItems.add(reward.getItem().name);
                break;
            case Life:
                addMaxLife(reward.getCount());
                break;
        }
    }

    private void addGold(int goldCount) {
        gold+=goldCount;
        onGoldChangeList.emit();
    }

    public void onLifeChange(Runnable  o) {
        onLifeTotalChangeList.add(o);
        o.run();
    }
    public void onPlayerChanged(Runnable  o) {
        onPlayerChangeList.add(o);
        o.run();
    }

    public void onEquipmentChanged(Runnable  o) {
        onEquipmentChange.add(o);
        o.run();
    }

    public void onGoldChange(Runnable  o) {
        onGoldChangeList.add(o);
        o.run();
    }

    public void onBlessing(Runnable o) {
        onBlessing.add(o);
        o.run();
    }

    public void heal(int amount) {
        life = Math.min(life + amount, maxLife);
        onLifeTotalChangeList.emit();
    }

    public void fullHeal() {
        life = maxLife;
        onLifeTotalChangeList.emit();
    }
    public void defeated() {
        gold= (int) (gold-(gold*difficultyData.goldLoss));
        life=Math.max(1,(int)(life-(maxLife*difficultyData.lifeLoss)));
        onLifeTotalChangeList.emit();
        onGoldChangeList.emit();
    }
    public void addMaxLife(int count) {
        maxLife += count;
        life    += count;
        onLifeTotalChangeList.emit();
    }
    public void giveGold(int price) {
        takeGold(-price);
    }
    public void takeGold(int price) {
        gold -= price;
        onGoldChangeList.emit();
    }

    public void addBlessing(EffectData bless){
        blessing = bless;
        onBlessing.emit();
    }

    public void clearBlessing() {
        blessing = null;
        onBlessing.emit();
    }

    public boolean hasBlessing(String name){ //Checks for a named blessing.
        //It is not necessary to name all blessings, only the ones you'd want to check for.
        if(blessing == null) return false;
        if(blessing.name.equals(name)) return true;
        return false;
    }

    public boolean isFantasyMode(){
        return fantasyMode;
    }

    public boolean hasAnnounceFantasy(){
        return announceFantasy;
    }

    public void clearAnnounceFantasy(){
        announceFantasy = false;
    }

    public boolean hasColorView() {
        for(String name:equippedItems.values()) {
            ItemData data=ItemData.getItem(name);
            if(data != null && data.effect.colorView) return true;
        }
        if(blessing != null) {
            if(blessing.colorView) return true;
        }
        return false;
    }

    public int bonusDeckCards() {
        int result = 0;
        for(String name:equippedItems.values()) {
            ItemData data=ItemData.getItem(name);
            if(data != null && data.effect.cardRewardBonus > 0) result += data.effect.cardRewardBonus;
        }
        if(blessing != null) {
            if(blessing.cardRewardBonus > 0) result += blessing.cardRewardBonus;
        }
        return Math.max(result, 3);
    }

    public DifficultyData getDifficulty() {
        return difficultyData;
    }

    public void renameDeck( String text) {
        deck = (Deck)deck.copyTo(text);
        decks[selectedDeckIndex]=deck;
    }

    public int cardSellPrice(PaperCard card) {
        return (int)(CardUtil.getCardPrice(card)*difficultyData.sellFactor);
    }

    public void sellCard(PaperCard card, Integer result) {
        float price = CardUtil.getCardPrice(card) * result;
        price *= difficultyData.sellFactor;
        cards.remove(card, result);
        addGold((int)price);
    }

    public void removeItem(String name) {
        if(name == null || name.equals("")) return;
        inventoryItems.removeValue(name,false);
        if(equippedItems.values().contains(name) && !inventoryItems.contains(name,false)) {
            equippedItems.values().remove(name);
        }
    }

    public void equip(ItemData item) {
        if(equippedItems.get(item.equipmentSlot) != null && equippedItems.get(item.equipmentSlot).equals(item.name)) {
            equippedItems.remove(item.equipmentSlot);
        } else {
            equippedItems.put(item.equipmentSlot,item.name);
        }
        onEquipmentChange.emit();
    }

    public String itemInSlot(String key) { return equippedItems.get(key); }

    public float equipmentSpeed() {
        float factor=1.0f;
        for(String name:equippedItems.values()) {
            ItemData data=ItemData.getItem(name);
            if(data != null && data.effect.moveSpeed > 0.0)  //Avoid negative speeds. It would be silly.
                factor*=data.effect.moveSpeed;
        }
        if(blessing != null) { //If a blessing gives speed, take it into account.
            if(blessing.moveSpeed > 0.0)
                factor *= blessing.moveSpeed;
        }
        return factor;
    }

    public float goldModifier(boolean sale) {
        float factor = 1.0f;
        for(String name:equippedItems.values()) {
            ItemData data=ItemData.getItem(name);
            if(data != null && data.effect.goldModifier > 0.0)  //Avoid negative modifiers.
                factor *= data.effect.goldModifier;
        }
        if(blessing != null) { //If a blessing gives speed, take it into account.
            if(blessing.goldModifier > 0.0)
                factor *= blessing.goldModifier;
        }
        if(sale) return Math.max(1.0f + (1.0f - factor), 2.5f);
        return Math.max(factor, 0.25f);
    }
    public float goldModifier(){
        return goldModifier(false);
    }

    public boolean hasItem(String name) {
        return inventoryItems.contains(name, false);
    }

    public boolean addItem(String name) {
        ItemData item=ItemData.getItem(name);
        if(item==null)
            return false;
        inventoryItems.add(name);
        return true;
    }


    // Quest functions.
    public void setQuestFlag(String key, int value){
        questFlags.put(key, (byte) value);
    }
    public void advanceQuestFlag(String key){
        if(questFlags.get(key) != null){
            questFlags.put(key, (byte) (questFlags.get(key) + 1));
        } else {
            questFlags.put(key, (byte) 1);
        }
    }
    public boolean checkQuestFlag(String key){
        return questFlags.get(key) != null;
    }
    public int getQuestFlag(String key){
        return (int) questFlags.getOrDefault(key, (byte) 0);
    }
    public void resetQuestFlags(){
        questFlags.clear();
    }
}
