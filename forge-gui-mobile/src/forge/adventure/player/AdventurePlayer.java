package forge.adventure.player;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.google.common.collect.Lists;
import forge.adventure.data.DifficultyData;
import forge.adventure.data.HeroListData;
import forge.adventure.data.ItemData;
import forge.adventure.util.*;
import forge.adventure.world.WorldSave;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.item.InventoryItem;
import forge.item.PaperCard;
import forge.util.ItemPool;

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
    private  Deck deck;
    private  int avatarIndex;
    private  int heroRace;
    private  boolean isFemale;
    private float worldPosX;
    private float worldPosY;
    private String name;
    private int gold=0;
    private int maxLife=20;
    private int life=20;
    private int selectedDeckIndex=0;
    private PlayerStatistic statistic=new PlayerStatistic();
    private  Deck[] decks=new Deck[NUMBER_OF_DECKS];
    private final DifficultyData difficultyData=new DifficultyData();

    private final Array<String> inventoryItems=new Array<>();
    private final HashMap<String,String> equippedItems=new HashMap<>();

    public AdventurePlayer()
    {

        for(int i=0;i<NUMBER_OF_DECKS;i++)
        {
            decks[i]=new Deck("Empty Deck");
        }
    }

    public PlayerStatistic getStatistic(){return statistic;}

    static public AdventurePlayer current()
    {
        return WorldSave.getCurrentSave().getPlayer();
    }
    private final CardPool cards=new CardPool();
    private final ItemPool<InventoryItem> newCards=new ItemPool<>(InventoryItem.class);

    public void create(String n, Deck startingDeck, boolean male, int race, int avatar,DifficultyData difficultyData) {
        inventoryItems.clear();
        equippedItems.clear();
        deck = startingDeck;
        decks[0]=deck;
        gold =difficultyData.staringMoney;
        cards.clear();
        cards.addAllFlat(deck.getAllCardsInASinglePool().toFlatList());
        maxLife=difficultyData.startingLife;
        this.difficultyData.startingLife=difficultyData.startingLife;
        this.difficultyData.staringMoney=difficultyData.staringMoney;
        this.difficultyData.startingDifficulty=difficultyData.startingDifficulty;
        this.difficultyData.name=difficultyData.name;
        this.difficultyData.enemyLifeFactor=difficultyData.enemyLifeFactor;
        this.difficultyData.sellFactor=difficultyData.sellFactor;
        life=maxLife;
        avatarIndex = avatar;
        heroRace = race;
        isFemale = !male;
        name = n;
        statistic.clear();
        newCards.clear();
        onGoldChangeList.emit();
        onLifeTotalChangeList.emit();

        inventoryItems.addAll(difficultyData.startItems);
    }

    public void setSelectedDeckSlot(int slot) {
        if(slot>=0&&slot<NUMBER_OF_DECKS)
        {
            selectedDeckIndex=slot;
            deck=decks[selectedDeckIndex];
        }
    }
    public int getSelectedDeckIndex() {
        return selectedDeckIndex;
    }
    public Deck getSelectedDeck() {
        return deck;
    }
    public Array<String> getItems() {
        return inventoryItems;
    }
    public Deck getDeck(int index) {
        return decks[index];
    }
    public CardPool getCards() {
        return cards;
    }


    public String getName() {
        return name;
    }

    public float getWorldPosX() {
        return worldPosX;
    }

    public void setWorldPosX(float worldPosX) {
        this.worldPosX = worldPosX;
    }

    public float getWorldPosY() {
        return worldPosY;
    }

    public void setWorldPosY(float worldPosY) {
        this.worldPosY = worldPosY;
    }



    @Override
    public void load(SaveFileData data) {


        this.statistic.load(data.readSubData("statistic"));
        this.difficultyData.startingLife=data.readInt("startingLife");
        this.difficultyData.staringMoney=data.readInt("staringMoney");
        this.difficultyData.startingDifficulty=data.readBool("startingDifficulty");
        this.difficultyData.name=data.readString("difficultyName");
        this.difficultyData.enemyLifeFactor=data.readFloat("enemyLifeFactor");
        this.difficultyData.sellFactor=data.readFloat("sellFactor");
        if(this.difficultyData.sellFactor==0)
            this.difficultyData.sellFactor=0.2f;


        name = data.readString("name");
        worldPosX = data.readFloat("worldPosX");
        worldPosY = data.readFloat("worldPosY");

        avatarIndex = data.readInt("avatarIndex");
        heroRace = data.readInt("heroRace");
        isFemale = data.readBool("isFemale");
        gold = data.readInt("gold");
        life = data.readInt("life");
        maxLife = data.readInt("maxLife");

        inventoryItems.clear();
        equippedItems.clear();
        if(data.containsKey("inventory"))
        {
            String[] inv=(String[])data.readObject("inventory");
            inventoryItems.addAll(inv);
        }
        if(data.containsKey("equippedSlots")&&data.containsKey("equippedItems"))
        {
            String[] slots=(String[])data.readObject("equippedSlots");
            String[] items=(String[])data.readObject("equippedItems");

            assert(slots.length==items.length);
            for(int i=0;i<slots.length;i++)
            {
                equippedItems.put(slots[i],items[i]);
            }
        }
        deck = new Deck(data.readString("deckName"));
        deck.getMain().addAll(CardPool.fromCardList(Lists.newArrayList((String[])data.readObject("deckCards"))));
        if(data.containsKey("sideBoardCards"))
            deck.getOrCreate(DeckSection.Sideboard).addAll(CardPool.fromCardList(Lists.newArrayList((String[])data.readObject("sideBoardCards"))));

        for(int i=0;i<NUMBER_OF_DECKS;i++)
        {
            if(!data.containsKey("deck_name_"+i))
            {
                if(i==0)
                    decks[i]=deck;
                else
                    decks[i]=new Deck("Empty Deck");
                continue;
            }
            decks[i] = new Deck(data.readString("deck_name_"+i));
            decks[i].getMain().addAll(CardPool.fromCardList(Lists.newArrayList((String[])data.readObject("deck_"+i))));
            if(data.containsKey("sideBoardCards_"+i))
                decks[i].getOrCreate(DeckSection.Sideboard).addAll(CardPool.fromCardList(Lists.newArrayList((String[])data.readObject("sideBoardCards_"+i))));
        }
        setSelectedDeckSlot(data.readInt("selectedDeckIndex"));

        cards.clear();
        cards.addAll(CardPool.fromCardList(Lists.newArrayList((String[])data.readObject("cards"))));

        newCards.clear();
        onLifeTotalChangeList.emit();
        onGoldChangeList.emit();
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
        data.store("worldPosX",worldPosX);
        data.store("worldPosY",worldPosY);
        data.store("avatarIndex",avatarIndex);
        data.store("heroRace",heroRace);
        data.store("isFemale",isFemale);
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


        data.storeObject("deckCards",deck.getMain().toCardList("\n").split("\n"));
        if(deck.get(DeckSection.Sideboard)!=null)
            data.storeObject("sideBoardCards",deck.get(DeckSection.Sideboard).toCardList("\n").split("\n"));
        for(int i=0;i<NUMBER_OF_DECKS;i++)
        {

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

        switch (reward.getType())
        {
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

    SignalList onLifeTotalChangeList=new SignalList();
    SignalList onGoldChangeList=new SignalList();
    SignalList onPlayerChangeList=new SignalList();
    SignalList onEquipmentChange=new SignalList();

    private void addGold(int goldCount) {
        gold+=goldCount;
        onGoldChangeList.emit();
    }

    public int getGold() {
        return gold;
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

    public int getLife() {
        return life;
    }

    public int getMaxLife() {
        return maxLife;
    }

    public void heal() {
        life=maxLife;
        onLifeTotalChangeList.emit();
    }
    public void defeated() {
        gold=gold/2;
        life=Math.max(1,(int)(life-(maxLife*0.2f)));
        onLifeTotalChangeList.emit();
        onGoldChangeList.emit();
    }
    public void addMaxLife(int count) {
        maxLife+=count;
        life+=count;
        onLifeTotalChangeList.emit();
    }
    public void giveGold(int price) {
        takeGold(-price);
    }
    public void takeGold(int price) {
        gold-=price;
        onGoldChangeList.emit();
    }

    public DifficultyData getDifficulty() {
        return difficultyData;
    }

    public void renameDeck( String text) {

        deck = (Deck)deck.copyTo(text);
        decks[selectedDeckIndex]=deck;
    }

    public ItemPool<InventoryItem> getNewCards() {
        return newCards;
    }
    public int cardSellPrice(PaperCard card)
    {
        return (int) (CardUtil.getCardPrice(card)*difficultyData.sellFactor);
    }

    public void sellCard(PaperCard card, Integer result) {
        float price= CardUtil.getCardPrice(card)*result;
        price=difficultyData.sellFactor*price;
        cards.remove(card,  result);
        addGold((int) price);
    }

    public void removeItem(String name) {
        if(name==null||name.equals(""))return;
        inventoryItems.removeValue(name,false);
        if(equippedItems.values().contains(name)&&!inventoryItems.contains(name,false))
        {
            equippedItems.values().remove(name);
        }
    }

    public void equip(ItemData item) {
        if(equippedItems.get(item.equipmentSlot)!=null&&equippedItems.get(item.equipmentSlot).equals(item.name))
        {
            equippedItems.remove(item.equipmentSlot);
        }
        else
        {
            equippedItems.put(item.equipmentSlot,item.name);
        }
        onEquipmentChange.emit();
    }

    public String itemInSlot(String key) {
        return equippedItems.get(key);
    }

    public Collection<String> getEquippedItems() {
        return  equippedItems.values();
    }

    public float equipmentSpeed() {
        float factor=1.0f;
        for(String name:equippedItems.values()) {
            ItemData data=ItemData.getItem(name);
            if(data.effect.moveSpeed > 0.0) { //Avoid negative speeds. It would be silly.
                factor*=data.effect.moveSpeed;
            }
        }
        return factor;
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
}
