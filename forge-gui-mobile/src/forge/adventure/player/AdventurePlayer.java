package forge.adventure.player;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Null;
import com.google.common.collect.Lists;
import forge.Forge;
import forge.adventure.data.*;
import forge.adventure.pointofintrest.PointOfInterestChanges;
import forge.adventure.scene.AdventureDeckEditor;
import forge.adventure.scene.DeckEditScene;
import forge.adventure.util.*;
import forge.adventure.world.WorldSave;
import forge.card.ColorSet;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckProxy;
import forge.deck.DeckSection;
import forge.item.InventoryItem;
import forge.item.PaperCard;
import forge.sound.SoundEffectType;
import forge.sound.SoundSystem;
import forge.util.ItemPool;

import java.io.Serializable;
import java.util.*;

/**
 * Class that represents the player (not the player sprite)
 */
public class AdventurePlayer implements Serializable, SaveFileContent {
    public static final int NUMBER_OF_DECKS = 10;
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
    private int gold = 0;
    private int maxLife = 20;
    private int life = 20;
    private int shards = 0;
    private EffectData blessing; //Blessing to apply for next battle.
    private final PlayerStatistic statistic = new PlayerStatistic();
    private final Map<String, Byte> questFlags = new HashMap<>();
    private final Map<String, Byte> characterFlags = new HashMap<>();
    private final Map<String, Byte> tutorialFlags = new HashMap<>();

    private final Array<String> inventoryItems = new Array<>();
    private final Array<Deck> boostersOwned = new Array<>();
    private final HashMap<String, String> equippedItems = new HashMap<>();
    private final List<AdventureQuestData> quests = new ArrayList<>();
    private final List<AdventureEventData> events = new ArrayList<>();

    // Fantasy/Chaos mode settings.
    private boolean fantasyMode = false;
    private boolean announceFantasy = false;
    private boolean usingCustomDeck = false;
    private boolean announceCustom = false;

    // Signals
    final SignalList onLifeTotalChangeList = new SignalList();
    final SignalList onShardsChangeList = new SignalList();
    final SignalList onGoldChangeList = new SignalList();
    final SignalList onPlayerChangeList = new SignalList();
    final SignalList onEquipmentChange = new SignalList();
    final SignalList onBlessing = new SignalList();
    private PointOfInterestChanges currentLocationChanges;

    public AdventurePlayer() {
        clear();
    }

    public PlayerStatistic getStatistic() {
        return statistic;
    }

    private void clearDecks() {
        for (int i = 0; i < NUMBER_OF_DECKS; i++) decks[i] = new Deck(Forge.getLocalizer().getMessage("lblEmptyDeck"));
        deck = decks[0];
        selectedDeckIndex = 0;
    }

    private void clear() {
        //Ensure sensitive gameplay data is properly reset between games.
        //Reset all properties HERE.
        fantasyMode = false;
        announceFantasy = false;
        usingCustomDeck = false;
        blessing = null;
        gold = 0;
        maxLife = 20;
        life = 20;
        shards = 0;
        clearDecks();
        inventoryItems.clear();
        boostersOwned.clear();
        equippedItems.clear();
        characterFlags.clear();
        questFlags.clear();
        quests.clear();
        events.clear();
        cards.clear();
        statistic.clear();
        newCards.clear();
        autoSellCards.clear();
        noSellCards.clear();
        AdventureEventController.clear();
        AdventureQuestController.clear();
    }


    static public AdventurePlayer current() {
        return WorldSave.getCurrentSave().getPlayer();
    }

    private final CardPool cards = new CardPool();

    private final ItemPool<PaperCard> newCards = new ItemPool<>(PaperCard.class);
    public final ItemPool<PaperCard> autoSellCards = new ItemPool<>(PaperCard.class);
    public final ItemPool<PaperCard> noSellCards = new ItemPool<>(PaperCard.class);

    public void create(String n, Deck startingDeck, boolean male, int race, int avatar, boolean isFantasy, boolean isUsingCustomDeck, DifficultyData difficultyData) {
        clear();
        announceFantasy = fantasyMode = isFantasy; //Set Chaos mode first.
        announceCustom = usingCustomDeck = isUsingCustomDeck;

        deck = startingDeck;
        decks[0] = deck;

        cards.addAllFlat(deck.getAllCardsInASinglePool().toFlatList());

        this.difficultyData.startingLife = difficultyData.startingLife;
        this.difficultyData.staringMoney = difficultyData.staringMoney;
        this.difficultyData.startingDifficulty = difficultyData.startingDifficulty;
        this.difficultyData.name = difficultyData.name;
        this.difficultyData.spawnRank = difficultyData.spawnRank;
        this.difficultyData.enemyLifeFactor = difficultyData.enemyLifeFactor;
        this.difficultyData.sellFactor = difficultyData.sellFactor;
        this.difficultyData.shardSellRatio = difficultyData.shardSellRatio;

        gold = difficultyData.staringMoney;
        name = n;
        heroRace = race;
        avatarIndex = avatar;
        isFemale = !male;

        setColorIdentity(DeckProxy.getColorIdentity(deck));

        life = maxLife = difficultyData.startingLife;
        shards = difficultyData.startingShards;

        inventoryItems.addAll(difficultyData.startItems);
        onGoldChangeList.emit();
        onLifeTotalChangeList.emit();
        onShardsChangeList.emit();
    }

    public void setSelectedDeckSlot(int slot) {
        if (slot >= 0 && slot < NUMBER_OF_DECKS) {
            selectedDeckIndex = slot;
            deck = decks[selectedDeckIndex];
            setColorIdentity(DeckProxy.getColorIdentity(deck));
        }
    }

    public void updateDifficulty(DifficultyData diff) {
        maxLife = diff.startingLife;
        this.difficultyData.startingShards = diff.startingShards;
        this.difficultyData.startingLife = diff.startingLife;
        this.difficultyData.staringMoney = diff.staringMoney;
        this.difficultyData.startingDifficulty = diff.startingDifficulty;
        this.difficultyData.name = diff.name;
        this.difficultyData.spawnRank = diff.spawnRank;
        this.difficultyData.enemyLifeFactor = diff.enemyLifeFactor;
        this.difficultyData.sellFactor = diff.sellFactor;
        this.difficultyData.shardSellRatio = diff.shardSellRatio;
        resetToMaxLife();
    }

    //Getters
    public int getSelectedDeckIndex() {
        return selectedDeckIndex;
    }

    public Deck getSelectedDeck() {
        return deck;
    }

    public Array<String> getItems() {
        return inventoryItems;
    }

    public Array<Deck> getBoostersOwned() {
        return boostersOwned;
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

    public float getWorldPosY() {
        return worldPosY;
    }

    public int getGold() {
        return gold;
    }

    public int getLife() {
        return life;
    }

    public int getMaxLife() {
        return maxLife;
    }

    public int getShards() {
        return shards;
    }

    public @Null EffectData getBlessing() {
        return blessing;
    }

    public Collection<String> getEquippedItems() {
        return equippedItems.values();
    }

    public ItemPool<PaperCard> getNewCards() {
        return newCards;
    }

    public ColorSet getColorIdentity() {
        return colorIdentity;
    }

    public String getColorIdentityLong() {
        return colorIdentity.toString();
    }


    //Setters
    public void setWorldPosX(float worldPosX) {
        this.worldPosX = worldPosX;
    }

    public void setWorldPosY(float worldPosY) {
        this.worldPosY = worldPosY;
    }

    public void setColorIdentity(String C) {
        colorIdentity = ColorSet.fromNames(C.toCharArray());
    }

    public void setColorIdentity(ColorSet set) {
        this.colorIdentity = set;
    }


    @Override
    public void load(SaveFileData data) {
        clear(); //Reset player data.
        this.statistic.load(data.readSubData("statistic"));
        this.difficultyData.startingLife = data.readInt("startingLife");
        this.difficultyData.staringMoney = data.readInt("staringMoney");
        this.difficultyData.startingDifficulty = data.readBool("startingDifficulty");
        this.difficultyData.name = data.readString("difficultyName");
        this.difficultyData.enemyLifeFactor = data.readFloat("enemyLifeFactor");
        this.difficultyData.sellFactor = data.readFloat("sellFactor");
        if (this.difficultyData.sellFactor == 0)
            this.difficultyData.sellFactor = 0.2f;

        //BEGIN SPECIAL CASES
        //Previously these were not being read from or written to save files, causing defaults to appear after reload
        //Pull from config if appropriate
        DifficultyData configuredDifficulty = null;
        for (DifficultyData candidate : Config.instance().getConfigData().difficulties) {
            if (candidate.name.equals(this.difficultyData.name)) {
                configuredDifficulty = candidate;
                break;
            }
        }

        if (configuredDifficulty != null && (this.difficultyData.shardSellRatio == data.readFloat("shardSellRatio") || data.readFloat("shardSellRatio") == 0))
            this.difficultyData.shardSellRatio = configuredDifficulty.shardSellRatio;
        else
            this.difficultyData.shardSellRatio = data.readFloat("shardSellRatio");
        if (configuredDifficulty != null && !data.containsKey("goldLoss"))
            this.difficultyData.goldLoss = configuredDifficulty.goldLoss;
        else
            this.difficultyData.goldLoss = data.readFloat("goldLoss");
        if (configuredDifficulty != null && !data.containsKey("lifeLoss"))
            this.difficultyData.lifeLoss = configuredDifficulty.lifeLoss;
        else
            this.difficultyData.lifeLoss = data.readFloat("lifeLoss");
        if (configuredDifficulty != null && !data.containsKey("spawnRank"))
            this.difficultyData.spawnRank = configuredDifficulty.spawnRank;
        else
            this.difficultyData.spawnRank = data.readInt("spawnRank");
        if (configuredDifficulty != null && !data.containsKey("rewardMaxFactor"))
            this.difficultyData.rewardMaxFactor = configuredDifficulty.rewardMaxFactor;
        else
            this.difficultyData.rewardMaxFactor = data.readFloat("rewardMaxFactor");
        // END SPECIAL CASES

        name = data.readString("name");
        heroRace = data.readInt("heroRace");
        avatarIndex = data.readInt("avatarIndex");
        isFemale = data.readBool("isFemale");
        if (data.containsKey("colorIdentity")) {
            String temp = data.readString("colorIdentity");
            if (temp != null)
                setColorIdentity(temp);
            else
                colorIdentity = ColorSet.ALL_COLORS;
        }
        else
            colorIdentity = ColorSet.ALL_COLORS;

        gold = data.readInt("gold");
        maxLife = data.readInt("maxLife");
        life = data.readInt("life");
        shards = data.containsKey("shards") ? data.readInt("shards") : 0;
        worldPosX = data.readFloat("worldPosX");
        worldPosY = data.readFloat("worldPosY");

        if (data.containsKey("blessing")) {
            EffectData temp = (EffectData) data.readObject("blessing");
            if (temp != null)
                blessing = temp;
        }

        if (data.containsKey("inventory")) {
            String[] inv = (String[]) data.readObject("inventory");
            //Prevent items with wrong names from getting through. Hell breaks loose if it causes null pointers.
            //This only needs to be done on load.
            for (String i : inv) {
                if (ItemData.getItem(i) != null) inventoryItems.add(i);
                else {
                    System.err.printf("Cannot find item name %s\n", i);
                    //Allow official© permission for the player to get a refund. We will allow it this time.
                    //TODoooo: Divine retribution if the player refunds too much. Use the orbital laser cannon.
                    System.out.println("Developers have blessed you! You are allowed to cheat the cost of the item back!");
                }
            }
        }
        if (data.containsKey("equippedSlots") && data.containsKey("equippedItems")) {
            String[] slots = (String[]) data.readObject("equippedSlots");
            String[] items = (String[]) data.readObject("equippedItems");

            assert (slots.length == items.length);
            //Like above, prevent items with wrong names. If it triggered in inventory it'll trigger here as well.
            for (int i = 0; i < slots.length; i++) {
                if (ItemData.getItem(items[i]) != null)
                    equippedItems.put(slots[i], items[i]);
                else {
                    System.err.printf("Cannot find equip name %s\n", items[i]);
                }
            }
        }
        if (data.containsKey("boosters")) {
            Deck[] decks = (Deck[]) data.readObject("boosters");
            for (Deck d : decks) {
                if (d != null && !d.isEmpty()) {
                    boostersOwned.add(d);
                } else {
                    System.err.printf("Null or empty booster %s\n", d);
                    System.out.println("You have an empty booster pack in your inventory.");
                }
            }
        }

        deck = new Deck(data.readString("deckName"));
        deck.getMain().addAll(CardPool.fromCardList(Lists.newArrayList((String[]) data.readObject("deckCards"))));
        if (data.containsKey("sideBoardCards"))
            deck.getOrCreate(DeckSection.Sideboard).addAll(CardPool.fromCardList(Lists.newArrayList((String[]) data.readObject("sideBoardCards"))));

        if (data.containsKey("characterFlagsKey") && data.containsKey("characterFlagsValue")) {
            String[] keys = (String[]) data.readObject("characterFlagsKey");
            Byte[] values = (Byte[]) data.readObject("characterFlagsValue");
            assert (keys.length == values.length);
            for (int i = 0; i < keys.length; i++) {
                characterFlags.put(keys[i], values[i]);
            }
        }

        if (data.containsKey("questFlagsKey") && data.containsKey("questFlagsValue")) {
            String[] keys = (String[]) data.readObject("questFlagsKey");
            Byte[] values = (Byte[]) data.readObject("questFlagsValue");
            assert (keys.length == values.length);
            for (int i = 0; i < keys.length; i++) {
                questFlags.put(keys[i], values[i]);
            }
        }
        if (data.containsKey("quests")) {
            quests.clear();
            Object[] q = (Object[]) data.readObject("quests");
            if (q != null) {
                for (Object itsReallyAQuest : q)
                    quests.add((AdventureQuestData) itsReallyAQuest);
            }
        }
        if (data.containsKey("events")) {
            events.clear();
            Object[] q = (Object[]) data.readObject("events");
            if (q != null) {
                for (Object itsReallyAnEvent : q) {
                    events.add((AdventureEventData) itsReallyAnEvent);
                }
            }
        }

        for (int i = 0; i < NUMBER_OF_DECKS; i++) {
            if (!data.containsKey("deck_name_" + i)) {
                if (i == 0) decks[i] = deck;
                else decks[i] = new Deck(Forge.getLocalizer().getMessage("lblEmptyDeck"));
                continue;
            }
            decks[i] = new Deck(data.readString("deck_name_" + i));
            decks[i].getMain().addAll(CardPool.fromCardList(Lists.newArrayList((String[]) data.readObject("deck_" + i))));
            if (data.containsKey("sideBoardCards_" + i))
                decks[i].getOrCreate(DeckSection.Sideboard).addAll(CardPool.fromCardList(Lists.newArrayList((String[]) data.readObject("sideBoardCards_" + i))));
        }
        setSelectedDeckSlot(data.readInt("selectedDeckIndex"));
        cards.addAll(CardPool.fromCardList(Lists.newArrayList((String[]) data.readObject("cards"))));

        //newCards.addAll(InventoryItem data.readObject("cards"))));
        data.storeObject("newCards", newCards.toFlatList().toArray(new InventoryItem[0]));
        data.storeObject("autoSellCards", autoSellCards.toFlatList().toArray(new InventoryItem[0]));
        data.storeObject("noSellCards", noSellCards.toFlatList().toArray(new InventoryItem[0]));

//        if (data.containsKey("newCards")) {
//            InventoryItem[] items = (InventoryItem[]) data.readObject("newCards");
//            for (InventoryItem item : items){
//                newCards.add((PaperCard)item);
//            }
//        }
//        if (data.containsKey("noSellCards")) {
//            PaperCard[] items = (PaperCard[]) data.readObject("noSellCards");
//            for (PaperCard item : items){
//                noSellCards.add(item);
//            }
//        }
//        if (data.containsKey("autoSellCards")) {
//            PaperCard[] items = (PaperCard[]) data.readObject("autoSellCards");
//            for (PaperCard item : items){
//                autoSellCards.add(item);
//            }
//        }

        fantasyMode = data.containsKey("fantasyMode") && data.readBool("fantasyMode");
        announceFantasy = data.containsKey("announceFantasy") && data.readBool("announceFantasy");
        usingCustomDeck = data.containsKey("usingCustomDeck") && data.readBool("usingCustomDeck");
        announceCustom = data.containsKey("announceCustom") && data.readBool("announceCustom");

        onLifeTotalChangeList.emit();
        onShardsChangeList.emit();
        onGoldChangeList.emit();
        onBlessing.emit();
    }

    @Override
    public SaveFileData save() {
        SaveFileData data = new SaveFileData();

        data.store("statistic", this.statistic.save());
        data.store("startingLife", this.difficultyData.startingLife);
        data.store("staringMoney", this.difficultyData.staringMoney);
        data.store("startingDifficulty", this.difficultyData.startingDifficulty);
        data.store("difficultyName", this.difficultyData.name);
        data.store("enemyLifeFactor", this.difficultyData.enemyLifeFactor);
        data.store("sellFactor", this.difficultyData.sellFactor);
        data.store("shardSellRatio", this.difficultyData.shardSellRatio);
        data.store("goldLoss", this.difficultyData.goldLoss);
        data.store("lifeLoss", this.difficultyData.lifeLoss);
        data.store("spawnRank", this.difficultyData.spawnRank);
        data.store("rewardMaxFactor", this.difficultyData.rewardMaxFactor);

        data.store("name", name);
        data.store("heroRace", heroRace);
        data.store("avatarIndex", avatarIndex);
        data.store("isFemale", isFemale);
        data.store("colorIdentity", colorIdentity.getColor());

        data.store("fantasyMode", fantasyMode);
        data.store("announceFantasy", announceFantasy);
        data.store("usingCustomDeck", usingCustomDeck);
        data.store("announceCustom", announceCustom);

        data.store("worldPosX", worldPosX);
        data.store("worldPosY", worldPosY);
        data.store("gold", gold);
        data.store("life", life);
        data.store("maxLife", maxLife);
        data.store("shards", shards);
        data.store("deckName", deck.getName());

        data.storeObject("inventory", inventoryItems.toArray(String.class));

        ArrayList<String> slots = new ArrayList<>();
        ArrayList<String> items = new ArrayList<>();
        for (Map.Entry<String, String> entry : equippedItems.entrySet()) {
            slots.add(entry.getKey());
            items.add(entry.getValue());
        }
        data.storeObject("equippedSlots", slots.toArray(new String[0]));
        data.storeObject("equippedItems", items.toArray(new String[0]));

        data.storeObject("boosters", boostersOwned.toArray(Deck.class));

        data.storeObject("blessing", blessing);

        //Save character flags.
        ArrayList<String> characterFlagsKey = new ArrayList<>();
        ArrayList<Byte> characterFlagsValue = new ArrayList<>();
        for (Map.Entry<String, Byte> entry : characterFlags.entrySet()) {
            characterFlagsKey.add(entry.getKey());
            characterFlagsValue.add(entry.getValue());
        }
        data.storeObject("characterFlagsKey", characterFlagsKey.toArray(new String[0]));
        data.storeObject("characterFlagsValue", characterFlagsValue.toArray(new Byte[0]));

        //Save quest flags.
        ArrayList<String> questFlagsKey = new ArrayList<>();
        ArrayList<Byte> questFlagsValue = new ArrayList<>();
        for (Map.Entry<String, Byte> entry : questFlags.entrySet()) {
            questFlagsKey.add(entry.getKey());
            questFlagsValue.add(entry.getValue());
        }
        data.storeObject("questFlagsKey", questFlagsKey.toArray(new String[0]));
        data.storeObject("questFlagsValue", questFlagsValue.toArray(new Byte[0]));
        data.storeObject("quests", quests.toArray());
        data.storeObject("events", events.toArray());

        data.storeObject("deckCards", deck.getMain().toCardList("\n").split("\n"));
        if (deck.get(DeckSection.Sideboard) != null)
            data.storeObject("sideBoardCards", deck.get(DeckSection.Sideboard).toCardList("\n").split("\n"));
        for (int i = 0; i < NUMBER_OF_DECKS; i++) {
            data.store("deck_name_" + i, decks[i].getName());
            data.storeObject("deck_" + i, decks[i].getMain().toCardList("\n").split("\n"));
            if (decks[i].get(DeckSection.Sideboard) != null)
                data.storeObject("sideBoardCards_" + i, decks[i].get(DeckSection.Sideboard).toCardList("\n").split("\n"));
        }
        data.store("selectedDeckIndex", selectedDeckIndex);
        data.storeObject("cards", cards.toCardList("\n").split("\n"));

        data.storeObject("newCards", newCards.toFlatList().toArray(new PaperCard[0]));
        data.storeObject("autoSellCards", autoSellCards.toFlatList().toArray(new PaperCard[0]));
        data.storeObject("noSellCards", noSellCards.toFlatList().toArray(new PaperCard[0]));

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
                if (reward.isNoSell()) {
                    noSellCards.add(reward.getCard());
                    AdventureDeckEditor editor = ((AdventureDeckEditor) DeckEditScene.getInstance().getScreen());
                    if (editor != null)
                        editor.refresh();
                }
                break;
            case Gold:
                addGold(reward.getCount());
                break;
            case Item:
                if (reward.getItem() != null)
                    inventoryItems.add(reward.getItem().name);
                break;
            case CardPack:
                if (reward.getDeck() != null) {
                    boostersOwned.add(reward.getDeck());
                }
                break;
            case Life:
                addMaxLife(reward.getCount());
                break;
            case Shards:
                addShards(reward.getCount());
                break;
        }
    }

    private void addGold(int goldCount) {
        gold += goldCount;
        onGoldChangeList.emit();
    }

    public void onShardsChange(Runnable o) {
        onShardsChangeList.add(o);
        o.run();
    }

    public void onLifeChange(Runnable o) {
        onLifeTotalChangeList.add(o);
        o.run();
    }

    public void onPlayerChanged(Runnable o) {
        onPlayerChangeList.add(o);
        o.run();
    }

    public void onEquipmentChanged(Runnable o) {
        onEquipmentChange.add(o);
        o.run();
    }

    public void onGoldChange(Runnable o) {
        onGoldChangeList.add(o);
        o.run();
    }

    public void onBlessing(Runnable o) {
        onBlessing.add(o);
        o.run();
    }

    public boolean fullHeal() {
        if (life < maxLife) {
            resetToMaxLife();
            return true;
        }
        return false;
    }

    public void resetToMaxLife() {
        life = maxLife;
        onLifeTotalChangeList.emit();
    }

    public boolean potionOfFalseLife() {
        if (gold >= falseLifeCost() && life == maxLife) {
            life = maxLife + 2;
            gold -= falseLifeCost();
            onLifeTotalChangeList.emit();
            onGoldChangeList.emit();
            return true;
        } else {
            System.out.println("Can't afford cost of false life " + falseLifeCost());
            System.out.println("Only has this much gold " + gold);
        }
        return false;
    }

    public int falseLifeCost() {
        int ret = 200 + (int) (50 * getStatistic().winLossRatio());
        return ret < 0 ? 250 : ret;
    }

    public void heal(int amount) {
        life = Math.min(life + amount, maxLife);
        onLifeTotalChangeList.emit();
    }

    public void heal(float percent) {
        life = Math.min(life + (int) (maxLife * percent), maxLife);
        onLifeTotalChangeList.emit();
    }

    public boolean defeated() {
        gold = (int) (gold - (gold * difficultyData.goldLoss));
        int newLife = (int) (life - (maxLife * difficultyData.lifeLoss));
        life = Math.max(1, newLife);
        onLifeTotalChangeList.emit();
        onGoldChangeList.emit();
        return newLife < 1;
        //If true, the player would have had 0 or less, and thus is actually "defeated" if the caller cares about it
    }

    public void win() {
        Current.player().addShards(1);
    }

    public void addMaxLife(int count) {
        maxLife += count;
        life += count;
        onLifeTotalChangeList.emit();
    }

    public void giveGold(int price) {
        takeGold(-price);
    }

    public void takeGold(int price) {
        gold -= price;
        onGoldChangeList.emit();
        //play sfx
        SoundSystem.instance.play(SoundEffectType.CoinsDrop, false);
    }

    public void addShards(int number) {
        takeShards(-number);
    }

    public void takeShards(int number) {
        shards -= number;
        onShardsChangeList.emit();
        //play sfx
        SoundSystem.instance.play(SoundEffectType.TakeShard, false);
    }

    public void setShards(int number) {
        shards = number;
        onShardsChangeList.emit();
    }

    public void addBlessing(EffectData bless) {
        blessing = bless;
        onBlessing.emit();
    }

    public void clearBlessing() {
        blessing = null;
        onBlessing.emit();
    }

    public boolean hasBlessing(String name) { //Checks for a named blessing.
        //It is not necessary to name all blessings, only the ones you'd want to check for.
        if (blessing == null) return false;
        return blessing.name.equals(name);
    }

    public boolean isFantasyMode() {
        return fantasyMode;
    }

    public boolean isUsingCustomDeck() {
        return usingCustomDeck;
    }

    public boolean hasAnnounceFantasy() {
        return announceFantasy;
    }

    public void clearAnnounceFantasy() {
        announceFantasy = false;
    }

    public boolean hasAnnounceCustom() {
        return announceCustom;
    }

    public void clearAnnounceCustom() {
        announceCustom = false;
    }

    public boolean hasColorView() {
        for (String name : equippedItems.values()) {
            ItemData data = ItemData.getItem(name);
            if (data != null && data.effect != null && data.effect.colorView) return true;
        }
        if (blessing != null) {
            return blessing.colorView;
        }
        return false;
    }

    public ItemData getEquippedAbility1() {
        for (String name : equippedItems.values()) {
            ItemData data = ItemData.getItem(name);
            if (data != null && "Ability1".equalsIgnoreCase(data.equipmentSlot)) {
                return data;
            }
        }
        return null;
    }

    public ItemData getEquippedAbility2() {
        for (String name : equippedItems.values()) {
            ItemData data = ItemData.getItem(name);
            if (data != null && "Ability2".equalsIgnoreCase(data.equipmentSlot)) {
                return data;
            }
        }
        return null;
    }

    public int bonusDeckCards() {
        int result = 0;
        for (String name : equippedItems.values()) {
            ItemData data = ItemData.getItem(name);
            if (data != null && data.effect != null && data.effect.cardRewardBonus > 0)
                result += data.effect.cardRewardBonus;
        }
        if (blessing != null) {
            if (blessing.cardRewardBonus > 0) result += blessing.cardRewardBonus;
        }
        return Math.min(result, 3);
    }

    public DifficultyData getDifficulty() {
        return difficultyData;
    }

    public void renameDeck(String text) {
        deck = (Deck) deck.copyTo(text);
        decks[selectedDeckIndex] = deck;
    }

    public int cardSellPrice(PaperCard card) {
        return (int) (CardUtil.getCardPrice(card) * difficultyData.sellFactor * (2.0f - currentLocationChanges.getTownPriceModifier()));
    }

    public void sellCard(PaperCard card, Integer result) {
        float price = CardUtil.getCardPrice(card) * result;
        price *= difficultyData.sellFactor;
        cards.remove(card, result);
        addGold((int) price);
    }

    public void removeItem(String name) {
        if (name == null || name.equals("")) return;
        inventoryItems.removeValue(name, false);
        if (equippedItems.values().contains(name) && !inventoryItems.contains(name, false)) {
            equippedItems.values().remove(name);
        }
    }

    public void equip(ItemData item) {
        if (equippedItems.get(item.equipmentSlot) != null && equippedItems.get(item.equipmentSlot).equals(item.name)) {
            equippedItems.remove(item.equipmentSlot);
        } else {
            equippedItems.put(item.equipmentSlot, item.name);
        }
        onEquipmentChange.emit();
    }

    public String itemInSlot(String key) {
        return equippedItems.get(key);
    }

    public float equipmentSpeed() {
        float factor = 1.0f;
        for (String name : equippedItems.values()) {
            ItemData data = ItemData.getItem(name);
            if (data != null && data.effect != null && data.effect.moveSpeed > 0.0)  //Avoid negative speeds. It would be silly.
                factor *= data.effect.moveSpeed;
        }
        if (blessing != null) { //If a blessing gives speed, take it into account.
            if (blessing.moveSpeed > 0.0)
                factor *= blessing.moveSpeed;
        }
        return factor;
    }

    public float goldModifier(boolean sale) {
        float factor = 1.0f;
        for (String name : equippedItems.values()) {
            ItemData data = ItemData.getItem(name);
            if (data != null && data.effect != null && data.effect.goldModifier > 0.0)  //Avoid negative modifiers.
                factor *= data.effect.goldModifier;
        }
        if (blessing != null) { //If a blessing gives speed, take it into account.
            if (blessing.goldModifier > 0.0)
                factor *= blessing.goldModifier;
        }
        if (sale) return Math.max(1.0f + (1.0f - factor), 2.5f);
        return Math.max(factor, 0.25f);
    }

    public float goldModifier() {
        return goldModifier(false);
    }

    public boolean hasItem(String name) {
        return inventoryItems.contains(name, false);
    }

    public int countItem(String name) {
        int count = 0;
        if (!hasItem(name))
            return count;
        for (String s : inventoryItems) {
            if (s.equals(name))
                count++;
        }
        return count;
    }

    public boolean addItem(String name) {
        ItemData item = ItemData.getItem(name);
        if (item == null)
            return false;
        inventoryItems.add(name);
        return true;
    }

    public boolean addBooster(Deck booster) {
        if (booster == null || booster.isEmpty())
            return false;
        boostersOwned.add(booster);
        return true;
    }

    public void removeBooster(Deck booster) {
        boostersOwned.removeValue(booster, true);
    }

    //Permanent character flags
    public void setCharacterFlag(String key, int value) {
        if (value != 0)
            characterFlags.put(key, (byte) value);
        else
            characterFlags.remove(key);
        AdventureQuestController.instance().updateQuestsCharacterFlag(key, value);
    }

    public void advanceCharacterFlag(String key) {
        if (characterFlags.get(key) != null) {
            characterFlags.put(key, (byte) (characterFlags.get(key) + 1));
        } else {
            characterFlags.put(key, (byte) 1);
        }
    }

    public boolean checkCharacterFlag(String key) {
        return characterFlags.get(key) != null;
    }

    public int getCharacterFlag(String key) {
        return (int) characterFlags.getOrDefault(key, (byte) 0);
    }

    // Quest functions.
    public void setQuestFlag(String key, int value) {
        if (value != 0)
            questFlags.put(key, (byte) value);
        else
            questFlags.remove(key);
        AdventureQuestController.instance().updateQuestsQuestFlag(key, value);
    }

    public void advanceQuestFlag(String key) {
        if (questFlags.get(key) != null) {
            questFlags.put(key, (byte) (questFlags.get(key) + 1));
        } else {
            questFlags.put(key, (byte) 1);
        }
    }

    public boolean checkQuestFlag(String key) {
        return questFlags.get(key) != null;
    }

    public int getQuestFlag(String key) {
        return (int) questFlags.getOrDefault(key, (byte) 0);
    }

    public void resetQuestFlags() {
        questFlags.clear();
    }

    public void addQuest(String questID) {
        int id = Integer.parseInt(questID);
        addQuest(id);
    }

    public void addQuest(int questID) {
        AdventureQuestData toAdd = AdventureQuestController.instance().generateQuest(questID);

        if (toAdd != null) {
            addQuest(toAdd);
        }
    }

    public void addQuest(AdventureQuestData q) {
        //TODO: add a config flag for this
        boolean autoTrack = true;
        for (AdventureQuestData existing : quests) {
            if (autoTrack && existing.isTracked) {
                autoTrack = false;
                break;
            }
        }
        q.isTracked = autoTrack;
        quests.add(q);
    }

    public List<AdventureQuestData> getQuests() {
        return quests;
    }

    public void addEvent(AdventureEventData e) {
        events.add(e);
    }

    public List<AdventureEventData> getEvents() {
        return events;
    }

    public int getEnemyDeckNumber(String enemyName, int maxDecks) {
        int deckNumber = 0;
        if (statistic.getWinLossRecord().get(enemyName) != null) {
            int playerWins = statistic.getWinLossRecord().get(enemyName).getKey();
            int enemyWins = statistic.getWinLossRecord().get(enemyName).getValue();
            if (playerWins > enemyWins) {
                int deckNumberAfterAlgorithmOutput = (int) ((playerWins - enemyWins) * (difficultyData.enemyLifeFactor / 3));
                if (deckNumberAfterAlgorithmOutput < maxDecks) {
                    deckNumber = deckNumberAfterAlgorithmOutput;
                } else {
                    deckNumber = maxDecks - 1;
                }
            }
        }
        return deckNumber;
    }

    public void removeQuest(AdventureQuestData quest) {
        quests.remove(quest);
    }

    /**
     * Deletes a deck by replacing the current selected deck with a new deck
     */
    public void deleteDeck() {
        deck = decks[selectedDeckIndex] = new Deck(Forge.getLocalizer().getMessage("lblEmptyDeck"));
    }

    /**
     * Attempts to copy a deck to an empty slot.
     *
     * @return int - index of new copy slot, or -1 if no slot was available
     */
    public int copyDeck() {
        for (int i = 0; i < decks.length; i++) {
            if (isEmptyDeck(i)) {
                decks[i] = (Deck) deck.copyTo(deck.getName() + " (" + Forge.getLocalizer().getMessage("lblCopy") + ")");
                return i;
            }
        }

        return -1;
    }

    public boolean isEmptyDeck(int deckIndex) {
        return decks[deckIndex].isEmpty() && decks[deckIndex].getName().equals(Forge.getLocalizer().getMessage("lblEmptyDeck"));
    }

    public void removeEvent(AdventureEventData completedEvent) {
        events.remove(completedEvent);
    }

    public ItemPool<PaperCard> getAutoSellCards() {
        return autoSellCards;
    }

    public ItemPool<PaperCard> getNoSellCards() {
        return noSellCards;
    }

    public ItemPool<PaperCard> getSellableCards() {
        ItemPool<PaperCard> sellableCards = new ItemPool<>(PaperCard.class);
        sellableCards.addAllFlat(cards.toFlatList());

        // 1. Remove cards you can't sell
        sellableCards.removeAll(noSellCards);
        // 1a. Potentially return here if we want to give config option to sell cards from decks
        // but would need to update the decks on sell, not just the catalog

        // 2. Count max cards across all decks in excess of unsellable
        Map<PaperCard, Integer> maxCardCounts = new HashMap<>();
        for (int i = 0; i < NUMBER_OF_DECKS; i++) {
            for (final Map.Entry<PaperCard, Integer> cp : decks[i].getAllCardsInASinglePool()) {

                int count = cp.getValue() - noSellCards.count(cp.getKey());

                if (count <= 0) continue;

                if (count > maxCardCounts.getOrDefault(cp.getKey(), 0)) {
                    maxCardCounts.put(cp.getKey(), cp.getValue());
                }
            }
        }

        // 3. Remove the highest use count of each card, remainder can be sold safely
        for (PaperCard card : maxCardCounts.keySet()) {
            sellableCards.remove(card, maxCardCounts.get(card));
        }

        return sellableCards;
    }


    public CardPool getCollectionCards(boolean allCards) {
        CardPool collectionCards = new CardPool();
        collectionCards.addAll(cards);
        if (!allCards) {
            collectionCards.removeAll(autoSellCards);
            collectionCards.removeAll(noSellCards);
        }

        return collectionCards;
    }

    public void loadChanges(PointOfInterestChanges changes) {
        this.currentLocationChanges = changes;
    }

    public void doAutosell() {
        int profit = 0;
        for (PaperCard cardToSell : autoSellCards.toFlatList()) {
            profit += AdventurePlayer.current().cardSellPrice(cardToSell);
            autoSellCards.remove(cardToSell);
            cards.remove(cardToSell, 1);
        }
        addGold(profit); //do this as one transaction so as not to get multiple copies of sound effect
    }
}
