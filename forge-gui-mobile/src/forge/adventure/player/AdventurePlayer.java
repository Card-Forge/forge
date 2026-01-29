package forge.adventure.player;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Null;
import com.github.tommyettinger.textra.TextraLabel;
import com.google.common.collect.Lists;

import forge.Forge;
import forge.adventure.data.*;
import forge.adventure.pointofintrest.PointOfInterestChanges;
import forge.adventure.scene.AdventureDeckEditor;
import forge.adventure.scene.DeckEditScene;
import forge.adventure.stage.GameStage;
import forge.adventure.stage.MapStage;
import forge.adventure.stage.WorldStage;
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
import java.util.function.Predicate;

/**
 * Class that represents the player (not the player sprite)
 */
public class AdventurePlayer implements Serializable, SaveFileContent {
    public static final int MIN_DECK_COUNT = 10;
    // this is a purely arbitrary limit, could be higher or lower; just meant as some sort of reasonable limit for the user
    public static final int MAX_DECK_COUNT = 20;
    // Player profile data.
    private String name;
    private int heroRace;
    private int avatarIndex;
    private boolean isFemale;
    private ColorSet colorIdentity = ColorSet.WUBRG;

    // Deck data
    private Deck deck;
    private final ArrayList<Deck> decks = new ArrayList<>(MIN_DECK_COUNT);
    private int selectedDeckIndex = 0;
    private final DifficultyData difficultyData = new DifficultyData();

    // Commander mode
    private AdventureModes adventureMode;

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

    private final ArrayList<ItemData> inventoryItems = new ArrayList<>();
    private final Array<Deck> boostersOwned = new Array<>();
    private final HashMap<String, Long> equippedItems = new HashMap<>();
    private final HashMap<Integer, HashMap<String, Long>> deckLoadouts = new HashMap<>();
    private final List<AdventureQuestData> quests = new ArrayList<>();
    private final List<AdventureEventData> events = new ArrayList<>();
    private final Set<PaperCard> unsupportedCards = new HashSet<>();
    private final Predicate<PaperCard> isUnsupported = pc -> pc != null && pc.getRules() != null && pc.getRules().isUnsupported();
    private final Predicate<PaperCard> isValid = pc -> pc != null && pc.getRules() != null && !pc.getRules().isUnsupported();

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

    public int getDeckCount() { return decks.size(); }

    private void clearDecks() {
        decks.clear();
        for (int i = 0; i < MIN_DECK_COUNT; i++)
            decks.add(new Deck(Forge.getLocalizer().getMessage("lblEmptyDeck")));
        deck = decks.get(0);
        selectedDeckIndex = 0;
    }

    private void clear() {
        //Ensure sensitive gameplay data is properly reset between games.
        //Reset all properties HERE.
        fantasyMode = false;
        announceFantasy = false;
        usingCustomDeck = false;
        adventureMode = null;
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
        favoriteCards.clear();
        AdventureEventController.clear();
        AdventureQuestController.clear();
        unsupportedCards.clear();
    }

    static public AdventurePlayer current() {
        return WorldSave.getCurrentSave().getPlayer();
    }

    private final CardPool cards = new CardPool();

    public final ItemPool<PaperCard> newCards = new ItemPool<>(PaperCard.class);
    public final ItemPool<PaperCard> autoSellCards = new ItemPool<>(PaperCard.class);
    public final Set<PaperCard> favoriteCards = new HashSet<>();

    public void create(String n, Deck startingDeck, boolean male, int race, int avatar, boolean isFantasy,
                       boolean isUsingCustomDeck, DifficultyData difficultyData, AdventureModes adventureMode) {
        clear();
        this.adventureMode = adventureMode;
        announceFantasy = fantasyMode = isFantasy; //Set Chaos mode first.
        announceCustom = usingCustomDeck = isUsingCustomDeck;

        clearDecks(); // Reset the empty decks to now already have the commander in the command zone.
        deck = startingDeck;
        decks.set(0, deck);

        cards.addAllFlat(deck.getAllCardsInASinglePool(true, true).toFlatList());

        this.difficultyData.startingLife = difficultyData.startingLife;
        this.difficultyData.startingMoney = difficultyData.startingMoney;
        this.difficultyData.startingDifficulty = difficultyData.startingDifficulty;
        this.difficultyData.name = difficultyData.name;
        this.difficultyData.spawnRank = difficultyData.spawnRank;
        this.difficultyData.enemyLifeFactor = difficultyData.enemyLifeFactor;
        this.difficultyData.sellFactor = difficultyData.sellFactor;
        this.difficultyData.shardSellRatio = difficultyData.shardSellRatio;
        this.difficultyData.goldLoss = difficultyData.goldLoss;
        this.difficultyData.lifeLoss = difficultyData.lifeLoss;

        gold = difficultyData.startingMoney;
        name = n;
        heroRace = race;
        avatarIndex = avatar;
        isFemale = !male;

        setColorIdentity(DeckProxy.getColorIdentity(deck));

        life = maxLife = difficultyData.startingLife;
        shards = difficultyData.startingShards;

        for (String s : difficultyData.startItems) {
            ItemData i = ItemListData.getItem(s);
            if (i == null)
                continue;
            inventoryItems.add(i);
        }

        onGoldChangeList.emit();
        onLifeTotalChangeList.emit();
        onShardsChangeList.emit();
    }

    public void setSelectedDeckSlot(int slot) {
        setSelectedDeckSlot(slot, true);
    }

    public void setSelectedDeckSlot(int slot, boolean switchLoadout) {
        if (slot >= 0 && slot < getDeckCount()) {
            boolean bindLoadouts = Config.instance().getSettingData().bindEquipmentLoadoutsToDecks;
            if (switchLoadout && bindLoadouts && slot != selectedDeckIndex) {
                // Save current loadout to old deck
                deckLoadouts.put(selectedDeckIndex, new HashMap<>(equippedItems));

                // Clear current equipment
                for (ItemData item : inventoryItems) {
                    if (item != null) {
                        item.isEquipped = false;
                    }
                }
                equippedItems.clear();

                // Restore loadout for new deck (if any)
                if (deckLoadouts.containsKey(slot)) {
                    HashMap<String, Long> newLoadout = deckLoadouts.get(slot);
                    for (Map.Entry<String, Long> entry : newLoadout.entrySet()) {
                        ItemData item = getItemFromInventory(entry.getValue());
                        if (item != null) {
                            item.isEquipped = true;
                            equippedItems.put(entry.getKey(), entry.getValue());
                        }
                    }
                }

                onEquipmentChange.emit();
            }

            selectedDeckIndex = slot;
            deck = decks.get(selectedDeckIndex);
            setColorIdentity(DeckProxy.getColorIdentity(deck));
        }
    }

    public void updateDifficulty(DifficultyData diff) {
        maxLife = diff.startingLife;
        this.difficultyData.startingShards = diff.startingShards;
        this.difficultyData.startingLife = diff.startingLife;
        this.difficultyData.startingMoney = diff.startingMoney;
        this.difficultyData.startingDifficulty = diff.startingDifficulty;
        this.difficultyData.name = diff.name;
        this.difficultyData.spawnRank = diff.spawnRank;
        this.difficultyData.enemyLifeFactor = diff.enemyLifeFactor;
        this.difficultyData.sellFactor = diff.sellFactor;
        this.difficultyData.shardSellRatio = diff.shardSellRatio;
        this.difficultyData.goldLoss = diff.goldLoss;
        this.difficultyData.lifeLoss = diff.lifeLoss;
        resetToMaxLife();
    }

    //Getters
    public int getSelectedDeckIndex() {
        return selectedDeckIndex;
    }

    public Deck getSelectedDeck() {
        return deck;
    }

    public ArrayList<ItemData> getItems() {
        return inventoryItems;
    }

    public ItemData getItemFromInventory(Long id) {
        if (id == null)
            return null;
        for (ItemData data : inventoryItems) {
            if (data == null)
                continue;
            if (id.equals(data.longID))
                return data;
        }
        return null;
    }

    public ItemData getEquippedItem(Long id) {
        if (id == null)
            return null;
        for (ItemData data : inventoryItems) {
            if (data == null)
                continue;
            if (id.equals(data.longID) && data.isEquipped)
                return data;
        }
        return null;
    }

    public Array<Deck> getBoostersOwned() {
        return boostersOwned;
    }

    public Deck getDeck(int index) {
        return decks.get(index);
    }

    public CardPool getCards() {
        return cards;
    }

    public String getName() {
        return name;
    }

    public Boolean isFemale() {
        return isFemale;
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

    public AdventureModes getAdventureMode(){
        return adventureMode;
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

    public Collection<Long> getEquippedItems() {
        return equippedItems.values();
    }

    public ColorSet getColorIdentity() {
        return colorIdentity;
    }

    public String getColorIdentityLong() {
        return colorIdentity.toString();
    }

    public Collection<PaperCard> getUnsupportedCards() {
        return unsupportedCards;
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
        boolean migration = false;
        clear(); // Reset player data.
        this.statistic.load(data.readSubData("statistic"));
        this.difficultyData.startingLife = data.readInt("startingLife");
        // Support for old typo
        if (data.containsKey("staringMoney")) {
            this.difficultyData.startingMoney = data.readInt("staringMoney");
        } else {
            this.difficultyData.startingMoney = data.readInt("startingMoney");
        }
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

        String _mode = data.readString("adventure_mode");
        if (_mode == null)
            adventureMode = AdventureModes.Standard;
        else
            adventureMode = AdventureModes.valueOf(_mode);

        if (data.containsKey("colorIdentity")) {
            String temp = data.readString("colorIdentity");
            if (temp != null)
                setColorIdentity(temp);
            else
                colorIdentity = ColorSet.WUBRG;
        } else
            colorIdentity = ColorSet.WUBRG;

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
            try {
                ItemData[] inv = (ItemData[]) data.readObject("inventory");
                for (int i = 0; i < inv.length; i++) {
                    ItemData itemData = inv[i];
                    if (itemData != null) {
                        inventoryItems.add(itemData);
                    }
                }
            } catch (Exception ignored) {
                migration = true;
                // migrate from string..
                try {
                    String[] inv = (String[]) data.readObject("inventory");
                    // Prevent items with wrong names from getting through. Hell breaks loose if it causes null pointers.
                    // This only needs to be done on load.
                    for (int j = 0; j < inv.length; j++) {
                        String i = inv[j];
                        ItemData itemData = ItemListData.getItem(i);
                        if (itemData != null) {
                            inventoryItems.add(itemData);
                        } else {
                            System.err.printf("Cannot find item name %s\n", i);
                            // Allow official© permission for the player to get a refund. We will allow it this time.
                            // TODO: Divine retribution if the player refunds too much. Use the orbital laser cannon.
                            System.out.println("Developers have blessed you! You are allowed to cheat the cost of the item back!");
                        }
                    }
                } catch (Exception e) {
                    //shouldn't crash if coming from string...
                    e.printStackTrace();
                }
            }
        }
        if (data.containsKey("equippedSlots") && data.containsKey("equippedItems")) {
            try {
                String[] slots = (String[]) data.readObject("equippedSlots");
                Long[] items = (Long[]) data.readObject("equippedItems");

                assert (slots.length == items.length);
                // Prevent items with wrong names. If it triggered in inventory, it'll trigger here as well.
                for (int i = 0; i < slots.length; i++) {
                    ItemData itemData = getItemFromInventory(items[i]);
                    if (itemData != null) {
                        if (itemData.longID == null)
                            itemData = itemData.clone();
                        if (itemData.longID != null) {
                            itemData.isEquipped = true;
                            equippedItems.put(slots[i], itemData.longID);
                        } else {
                            itemData.isEquipped = false;
                            System.err.println("Missing ID: " + itemData.name);
                        }
                    }
                }
            } catch (Exception ignored) {}
        }
        if (data.containsKey("boosters")) {
            Deck[] decks = (Deck[]) data.readObject("boosters");
            if (decks != null) {
                for (Deck d : decks) {
                    if (d != null && !d.isEmpty()) {
                        boostersOwned.add(d);
                    } else {
                        System.err.printf("Null or empty booster %s\n", d);
                        System.out.println("You have an empty booster pack in your inventory.");
                    }
                }
            } else {
                System.err.println("Deck[] is null! [boosters]");
            }
        }

        deck = new Deck(data.readString("deckName"));
        CardPool deckCards = CardPool.fromCardList(Lists.newArrayList((String[]) data.readObject("deckCards")));
        deck.getMain().addAll(deckCards.getFilteredPool(isValid));
        unsupportedCards.addAll(deckCards.getFilteredPool(isUnsupported).toFlatList());
        if (data.containsKey("sideBoardCards")) {
            CardPool sideBoardCards = CardPool.fromCardList(Lists.newArrayList((String[]) data.readObject("sideBoardCards")));
            deck.getOrCreate(DeckSection.Sideboard).addAll(sideBoardCards.getFilteredPool(isValid));
            unsupportedCards.addAll(sideBoardCards.getFilteredPool(isUnsupported).toFlatList());
        }
        if (data.containsKey("attractionDeckCards")) {
            CardPool attractionDeckCards = CardPool.fromCardList(List.of((String[]) data.readObject("attractionDeckCards")));
            deck.getOrCreate(DeckSection.Attractions).addAll(attractionDeckCards.getFilteredPool(isValid));
            unsupportedCards.addAll(attractionDeckCards.getFilteredPool(isUnsupported).toFlatList());
        }
        if (data.containsKey("contraptionDeckCards")) {//TODO: Generalize this. Can't we just serialize the whole deck?
            CardPool contraptionDeckCards = CardPool.fromCardList(List.of((String[]) data.readObject("contraptionDeckCards")));
            deck.getOrCreate(DeckSection.Contraptions).addAll(contraptionDeckCards.getFilteredPool(isValid));
            unsupportedCards.addAll(contraptionDeckCards.getFilteredPool(isUnsupported).toFlatList());
        }
        if (data.containsKey("commanderCards")) {
            CardPool commanderCards = CardPool.fromCardList(List.of((String[]) data.readObject("commanderCards")));
            deck.getOrCreate(DeckSection.Commander).addAll(commanderCards.getFilteredPool(isValid));
            unsupportedCards.addAll(commanderCards.getFilteredPool(isUnsupported).toFlatList());
        }
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

        // Load decks
        // Check if this save has dynamic deck count, use set-count load if not
        boolean hasDynamicDeckCount = data.containsKey("deckCount");
        if (hasDynamicDeckCount) {
            int dynamicDeckCount = data.readInt("deckCount");
            // In case the save had previously saved more decks than the current version allows (in case of the max being lowered)
            dynamicDeckCount = Math.min(MAX_DECK_COUNT, dynamicDeckCount);
            for (int i = 0; i < dynamicDeckCount; i++){
                // The first x elements are pre-created
                if (i < MIN_DECK_COUNT) {
                    decks.set(i, new Deck(data.readString("deck_name_" + i)));
                }
                else {
                    decks.add(new Deck(data.readString("deck_name_" + i)));
                }
                CardPool mainCards = CardPool.fromCardList(Lists.newArrayList((String[]) data.readObject("deck_" + i)));
                decks.get(i).getMain().addAll(mainCards.getFilteredPool(isValid));
                unsupportedCards.addAll(mainCards.getFilteredPool(isUnsupported).toFlatList());
                if (data.containsKey("sideBoardCards_" + i)) {
                    CardPool sideBoardCards = CardPool.fromCardList(Lists.newArrayList((String[]) data.readObject("sideBoardCards_" + i)));
                    decks.get(i).getOrCreate(DeckSection.Sideboard).addAll(sideBoardCards.getFilteredPool(isValid));
                    unsupportedCards.addAll(sideBoardCards.getFilteredPool(isUnsupported).toFlatList());
                }
                if (data.containsKey("attractionDeckCards_" + i)) {
                    CardPool attractionCards = CardPool.fromCardList(Lists.newArrayList((String[]) data.readObject("attractionDeckCards_" + i)));
                    decks.get(i).getOrCreate(DeckSection.Attractions).addAll(attractionCards.getFilteredPool(isValid));
                    unsupportedCards.addAll(attractionCards.getFilteredPool(isUnsupported).toFlatList());
                }
                if (data.containsKey("contraptionDeckCards_" + i)) {
                    CardPool contraptionCards = CardPool.fromCardList(Lists.newArrayList((String[]) data.readObject("contraptionDeckCards_" + i)));
                    decks.get(i).getOrCreate(DeckSection.Contraptions).addAll(contraptionCards.getFilteredPool(isValid));
                    unsupportedCards.addAll(contraptionCards.getFilteredPool(isUnsupported).toFlatList());
                }
                if (data.containsKey("commanderCards_" + i)) {
                    CardPool commanderCards = CardPool.fromCardList(List.of((String[]) data.readObject("commanderCards_" + i)));
                    decks.get(i).getOrCreate(DeckSection.Commander).addAll(commanderCards.getFilteredPool(isValid));
                    unsupportedCards.addAll(commanderCards.getFilteredPool(isUnsupported).toFlatList());
                }
            }
            // In case we allow removing decks from the deck selection GUI, populate up to the minimum
            for (int i = dynamicDeckCount++; i < MIN_DECK_COUNT; i++) {
                decks.set(i, new Deck(Forge.getLocalizer().getMessage("lblEmptyDeck")));
            }
        // Legacy load
        } else {
            for (int i = 0; i < MIN_DECK_COUNT; i++) {
                if (!data.containsKey("deck_name_" + i)) {
                    if (i == 0) decks.set(i, deck);
                    else decks.set(i, new Deck(Forge.getLocalizer().getMessage("lblEmptyDeck")));
                    continue;
                }
                decks.set(i, new Deck(data.readString("deck_name_" + i)));
                CardPool mainCards = CardPool.fromCardList(Lists.newArrayList((String[]) data.readObject("deck_" + i)));
                decks.get(i).getMain().addAll(mainCards.getFilteredPool(isValid));
                unsupportedCards.addAll(mainCards.getFilteredPool(isUnsupported).toFlatList());
                if (data.containsKey("sideBoardCards_" + i)) {
                    CardPool sideBoardCards = CardPool.fromCardList(Lists.newArrayList((String[]) data.readObject("sideBoardCards_" + i)));
                    decks.get(i).getOrCreate(DeckSection.Sideboard).addAll(sideBoardCards.getFilteredPool(isValid));
                    unsupportedCards.addAll(sideBoardCards.getFilteredPool(isUnsupported).toFlatList());
                }
                if (data.containsKey("commanderCards_" + i)) {
                    CardPool commanderCards = CardPool.fromCardList(List.of((String[]) data.readObject("commanderCards_" + i)));
                    decks.get(i).getOrCreate(DeckSection.Commander).addAll(commanderCards.getFilteredPool(isValid));
                    unsupportedCards.addAll(commanderCards.getFilteredPool(isUnsupported).toFlatList());
                }
            }
        }

        // Load deck loadouts (equipment tied to each deck)
        for (int i = 0; i < getDeckCount(); i++) {
            if (data.containsKey("deckLoadout_slots_" + i) && data.containsKey("deckLoadout_items_" + i)) {
                try {
                    String[] loadoutSlots = (String[]) data.readObject("deckLoadout_slots_" + i);
                    Long[] loadoutItems = (Long[]) data.readObject("deckLoadout_items_" + i);
                    if (loadoutSlots.length == loadoutItems.length) {
                        HashMap<String, Long> loadout = new HashMap<>();
                        for (int j = 0; j < loadoutSlots.length; j++) {
                            loadout.put(loadoutSlots[j], loadoutItems[j]);
                        }
                        deckLoadouts.put(i, loadout);
                    }
                } catch (Exception ignored) {}
            }
        }

        // Use false to skip loadout switching during load (equippedItems already loaded correctly above)
        setSelectedDeckSlot(data.readInt("selectedDeckIndex"), false);
        CardPool cardPool = CardPool.fromCardList(Lists.newArrayList((String[]) data.readObject("cards")));
        cards.addAll(cardPool.getFilteredPool(isValid));
        unsupportedCards.addAll(cardPool.getFilteredPool(isUnsupported).toFlatList());

        if (data.containsKey("newCards")) {
            InventoryItem[] items = (InventoryItem[]) data.readObject("newCards");
            for (InventoryItem item : items) {
                if (item instanceof PaperCard pc) {
                    if (isUnsupported.test(pc))
                        unsupportedCards.add(pc);
                    else
                        newCards.add(pc);
                }
            }
        }
        if (data.containsKey("noSellCards")) {
            // Legacy list of unsellable cards. Now done via CardRequest flags. Convert the corresponding cards.
            PaperCard[] items = (PaperCard[]) data.readObject("noSellCards");
            CardPool noSellPool = new CardPool();
            for (PaperCard pc : items) {
                if (isUnsupported.test(pc))
                    unsupportedCards.add(pc);
                else
                    noSellPool.add(pc);
            }
            for (Map.Entry<PaperCard, Integer> noSellEntry : noSellPool) {
                PaperCard item = noSellEntry.getKey();
                if (item == null)
                    continue;
                int totalCopies = cards.count(item);
                int noSellCopies = Math.min(noSellEntry.getValue(), totalCopies);
                if (!cards.remove(item, noSellCopies)) {
                    System.err.printf("Failed to update noSellValue flag - %s%n", item);
                    continue;
                }

                int remainingSellableCopies = totalCopies - noSellCopies;

                PaperCard noSellVersion = item.getNoSellVersion();
                cards.add(noSellVersion, noSellCopies);

                System.out.printf("Converted legacy noSellCards item - %s (%d / %d copies)%n", item, noSellCopies, totalCopies);

                // Also go through their decks and update cards there.
                for (Deck deck : decks) {
                    int inUse = 0;
                    for (Map.Entry<DeckSection, CardPool> section : deck) {
                        CardPool pool = section.getValue();
                        inUse += pool.count(item);
                        if(inUse > remainingSellableCopies) {
                            int toConvert = inUse - remainingSellableCopies;
                            pool.remove(item, toConvert);
                            pool.add(noSellVersion, toConvert);
                            System.out.printf("- Converted %d copies in deck - %s/%s%n", toConvert, deck.getName(), section.getKey());
                        }
                    }
                }

            }
        }
        if (data.containsKey("autoSellCards")) {
            PaperCard[] items = (PaperCard[]) data.readObject("autoSellCards");
            for (PaperCard pc : items) {
                if (isUnsupported.test(pc))
                    unsupportedCards.add(pc);
                else
                    autoSellCards.add(pc);
            }
        }
        if (data.containsKey("favoriteCards")) {
            PaperCard[] items = (PaperCard[]) data.readObject("favoriteCards");
            for (PaperCard pc : items) {
                if (isUnsupported.test(pc))
                    unsupportedCards.add(pc);
                else
                    favoriteCards.add(pc);
            }
        }

        fantasyMode = data.containsKey("fantasyMode") && data.readBool("fantasyMode");
        announceFantasy = data.containsKey("announceFantasy") && data.readBool("announceFantasy");
        usingCustomDeck = data.containsKey("usingCustomDeck") && data.readBool("usingCustomDeck");
        announceCustom = data.containsKey("announceCustom") && data.readBool("announceCustom");
        if (migration) {
            getCurrentGameStage().setExtraAnnouncement(Forge.getLocalizer().getMessage("lblDataMigrationMsg"));
        }

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
        data.store("startingMoney", this.difficultyData.startingMoney);
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

        data.store("adventure_mode", adventureMode.toString());

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

        data.storeObject("inventory", inventoryItems.toArray(new ItemData[0]));

        ArrayList<String> slots = new ArrayList<>();
        ArrayList<Long> items = new ArrayList<>();
        for (Map.Entry<String, Long> entry : equippedItems.entrySet()) {
            slots.add(entry.getKey());
            items.add(entry.getValue());
        }
        data.storeObject("equippedSlots", slots.toArray(new String[0]));
        data.storeObject("equippedItems", items.toArray(new Long[0]));

        data.storeObject("boosters", boostersOwned.toArray(Deck.class));

        data.storeObject("blessing", blessing);

        // Save character flags.
        ArrayList<String> characterFlagsKey = new ArrayList<>();
        ArrayList<Byte> characterFlagsValue = new ArrayList<>();
        for (Map.Entry<String, Byte> entry : characterFlags.entrySet()) {
            characterFlagsKey.add(entry.getKey());
            characterFlagsValue.add(entry.getValue());
        }
        data.storeObject("characterFlagsKey", characterFlagsKey.toArray(new String[0]));
        data.storeObject("characterFlagsValue", characterFlagsValue.toArray(new Byte[0]));

        // Save quest flags.
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
        if (deck.get(DeckSection.Attractions) != null)
            data.storeObject("attractionDeckCards", deck.get(DeckSection.Attractions).toCardList("\n").split("\n"));
        if (deck.get(DeckSection.Contraptions) != null)
            data.storeObject("contraptionDeckCards", deck.get(DeckSection.Contraptions).toCardList("\n").split("\n"));
        if (deck.get(DeckSection.Commander) != null)
            data.storeObject("commanderCards", deck.get(DeckSection.Commander).toCardList("\n").split("\n"));

        // save decks dynamically
        data.store("deckCount", getDeckCount());
        for (int i = 0; i < getDeckCount(); i++) {
            data.store("deck_name_" + i, decks.get(i).getName());
            data.storeObject("deck_" + i, decks.get(i).getMain().toCardList("\n").split("\n"));
            if (decks.get(i).get(DeckSection.Sideboard) != null)
                data.storeObject("sideBoardCards_" + i, decks.get(i).get(DeckSection.Sideboard).toCardList("\n").split("\n"));
            if (decks.get(i).get(DeckSection.Attractions) != null)
                data.storeObject("attractionDeckCards_" + i, decks.get(i).get(DeckSection.Attractions).toCardList("\n").split("\n"));
            if (decks.get(i).get(DeckSection.Contraptions) != null)
                data.storeObject("contraptionDeckCards_" + i, decks.get(i).get(DeckSection.Contraptions).toCardList("\n").split("\n"));
            if (decks.get(i).get(DeckSection.Commander) != null)
                data.storeObject("commanderCards_" + i, decks.get(i).get(DeckSection.Commander).toCardList("\n").split("\n"));
        }

        // Save deck loadouts (equipment tied to each deck)
        // First, save current equipment to current deck's loadout
        deckLoadouts.put(selectedDeckIndex, new HashMap<>(equippedItems));
        for (int i = 0; i < getDeckCount(); i++) {
            if (deckLoadouts.containsKey(i)) {
                HashMap<String, Long> loadout = deckLoadouts.get(i);
                ArrayList<String> loadoutSlots = new ArrayList<>();
                ArrayList<Long> loadoutItems = new ArrayList<>();
                for (Map.Entry<String, Long> entry : loadout.entrySet()) {
                    loadoutSlots.add(entry.getKey());
                    loadoutItems.add(entry.getValue());
                }
                data.storeObject("deckLoadout_slots_" + i, loadoutSlots.toArray(new String[0]));
                data.storeObject("deckLoadout_items_" + i, loadoutItems.toArray(new Long[0]));
            }
        }

        data.store("selectedDeckIndex", selectedDeckIndex);
        data.storeObject("cards", cards.toCardList("\n").split("\n"));

        data.storeObject("newCards", newCards.toFlatList().toArray(new PaperCard[0]));
        data.storeObject("autoSellCards", autoSellCards.toFlatList().toArray(new PaperCard[0]));
        data.storeObject("favoriteCards", favoriteCards.toArray(new PaperCard[0]));

        return data;
    }

    public String spriteName() {
        return HeroListData.instance().getHero(heroRace, isFemale);
    }

    public FileHandle sprite() {
        return Config.instance().getFile(HeroListData.instance().getHero(heroRace, isFemale));
    }

    public TextureRegion avatar() {
        return HeroListData.instance().getAvatar(heroRace, isFemale, avatarIndex);
    }

    public String raceName() {
        return HeroListData.instance().getRaces().get(Current.player().heroRace);
    }

    public GameStage getCurrentGameStage() {
        if (MapStage.getInstance().isInMap())
            return MapStage.getInstance();
        return WorldStage.getInstance();
    }

    public void addStatusMessage(String iconName, String message, Integer itemCount, float x, float y) {
        String symbol = itemCount == null || itemCount < 0 ? "" : " +";
        String icon = iconName == null ? "" : "[+" + iconName + "]";
        String count = itemCount == null ? "" : String.valueOf(itemCount);
        TextraLabel actor = Controls.newTextraLabel("[%95]" + icon + "[WHITE]" + symbol + count + " " + message);
        actor.setPosition(x, y);
        actor.addAction(Actions.sequence(
                Actions.parallel(Actions.moveBy(0f, 5f, 3f), Actions.fadeIn(2f)),
                Actions.hide(),
                Actions.removeActor())
        );
        getCurrentGameStage().addActor(actor);
    }

    public void addCard(PaperCard card) {
        addCard(card, 1);
    }

    public void addCard(PaperCard card, int amount) {
        cards.add(card, amount);
        newCards.add(card, amount);
    }

    public void addCards(ItemPool<PaperCard> cardPool) {
        cards.addAll(cardPool);
        newCards.addAll(cardPool);
    }

    public void addReward(Reward reward) {
        switch (reward.getType()) {
            case Card:
                cards.add(reward.getCard());
                newCards.add(reward.getCard());
                if (reward.isAutoSell()) {
                    autoSellCards.add(reward.getCard());
                    refreshEditor();
                }
                break;
            case Gold:
                addGold(reward.getCount());
                break;
            case Item:
                if (reward.getItem() != null)
                    addItem(reward.getItem().name);
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

    private void refreshEditor() {
        AdventureDeckEditor editor = ((AdventureDeckEditor) DeckEditScene.getInstance().getScreen());
        if (editor != null)
            editor.refresh();
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
        life = (int) (life - (maxLife * difficultyData.lifeLoss));
        onLifeTotalChangeList.emit();
        onGoldChangeList.emit();
        return life < 1;
        // If true, the player would have had 0 or less, and thus is actually "defeated" if the caller cares about it
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
        boolean changed = shards != number;
        if (changed) {
            shards = number;
            onShardsChangeList.emit();
        }
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
        for (Long id : equippedItems.values()) {
            ItemData data = getEquippedItem(id);
            if (data != null && data.effect != null && data.effect.colorView) return true;
        }
        if (blessing != null) {
            return blessing.colorView;
        }
        return false;
    }

    public ItemData getRandomEquippedItem() {
        Array<ItemData> items = new Array<>();
        for (Long id : equippedItems.values()) {
            ItemData item = getEquippedItem(id);
            if (item == null)
                continue;
            if (isHardorInsaneDifficulty()) {
                items.add(item);
            } else {
                switch (item.equipmentSlot) {
                    // limit to these for easy and normal
                    case "Boots", "Body", "Neck" -> items.add(item);
                }
            }
        }
        return items.random();
    }

    public boolean hasEquippedItem() {
        for (Long id : equippedItems.values()) {
            ItemData item = getEquippedItem(id);
            if (item == null)
                continue;
            if (isHardorInsaneDifficulty()) {
                return true;
            } else {
                switch (item.equipmentSlot) {
                    // limit to these for easy and normal
                    case "Boots", "Body", "Neck" -> {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public ItemData getEquippedAbility1() {
        for (Long id : equippedItems.values()) {
            ItemData data = getEquippedItem(id);
            if (data != null && "Ability1".equalsIgnoreCase(data.equipmentSlot)) {
                return data;
            }
        }
        return null;
    }

    public ItemData getEquippedAbility2() {
        for (Long id : equippedItems.values()) {
            ItemData data = getEquippedItem(id);
            if (data != null && "Ability2".equalsIgnoreCase(data.equipmentSlot)) {
                return data;
            }
        }
        return null;
    }

    public int bonusDeckCards() {
        int result = 0;
        for (Long id : equippedItems.values()) {
            ItemData data = getEquippedItem(id);
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

    public boolean isHardorInsaneDifficulty() {
        return "Hard".equalsIgnoreCase(difficultyData.name) || "Insane".equalsIgnoreCase(difficultyData.name);
    }

    public void renameDeck(String text) {
        deck = (Deck) deck.copyTo(text);
        decks.set(selectedDeckIndex, deck);
    }

    public int cardSellPrice(PaperCard card) {
        if (card.hasNoSellValue()) {
            return 0;
        }

        int basePrice = (int) (CardUtil.getCardPrice(card) * difficultyData.sellFactor);

        float townPriceModifier = currentLocationChanges == null ? 1f : currentLocationChanges.getTownPriceModifier();
        return (int) (basePrice * (2.0f - townPriceModifier));
    }

    /**
     * Sells a number of copies of a card.
     * @return the number of copies successfully sold.
     */
    public int sellCard(PaperCard card, Integer amount) {
        if (amount == null || amount < 1)
            return 0;

        int amountToSell = Math.min(amount, cards.count(card));
        int earned = performSale(card, amountToSell);

        if(earned > 0)
            giveGold(earned);
        return amountToSell;
    }

    /**
     * Sells all cards in the given card pool and adds the resulting amount of gold.
     * The given card pool will be emptied.
     */
    public void doBulkSell(ItemPool<PaperCard> cards) {
        int profit = 0;
        for (PaperCard cardToSell : cards.toFlatList()) {
            profit += AdventurePlayer.current().performSale(cardToSell, 1);
            cards.remove(cardToSell);
        }
        giveGold(profit); //do this as one transaction so as not to get multiple copies of sound effect
    }

    /**
     * Removes a number of copies of a card from the player's inventory and returns the amount of gold they sold for.
     * Does *not* update the player's gold. Can be used as part of bulk-sell operations that update the amount all at once.
     */
    private int performSale(PaperCard card, int amount) {
        int amountToSell = Math.min(amount, cards.count(card));
        if(!cards.remove(card, amountToSell))
            return 0; //Failed to sell?
        return cardSellPrice(card) * amountToSell;
    }

    public void removeItem(String name) {
        inventoryItems.stream().filter(itemData -> name.equalsIgnoreCase(itemData.name)).findFirst().ifPresent(this::removeItem);
    }

    public void removeItem(ItemData item) {
        if (item == null)
            return;
        inventoryItems.remove(item);
        if (getEquippedItems().contains(item.longID) && !inventoryItems.contains(item)) {
            item.isEquipped = false;
            getEquippedItems().remove(item.longID);
        }
    }

    public void equip(ItemData item) {
        Long itemID = equippedItems.get(item.equipmentSlot);
        if (itemID != null && itemID.equals(item.longID)) {
            item.isEquipped = false;
            equippedItems.remove(item.equipmentSlot);
        } else {
            item.isEquipped = true;
            equippedItems.put(item.equipmentSlot, item.longID);
        }
        onEquipmentChange.emit();
    }

    public Long itemInSlot(String key) {
        return equippedItems.get(key);
    }

    public float equipmentSpeed() {
        float factor = 1.0f;
        for (Long id : equippedItems.values()) {
            ItemData data = getEquippedItem(id);
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
        for (Long id: equippedItems.values()) {
            ItemData data = getEquippedItem(id);
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
        return inventoryItems.stream().anyMatch(itemData -> name.equalsIgnoreCase(itemData.name));
    }

    public int countItem(String name) {
        return (int) inventoryItems.stream().filter(Objects::nonNull).filter(i -> i.name.equals(name)).count();
    }

    public boolean addItem(String name) {
        return addItem(name, true);
    }
    public boolean addItem(String name, boolean updateEvent) {
        ItemData item = ItemListData.getItem(name);
        if (item == null)
            return false;
        inventoryItems.add(item);
        if (updateEvent)
            AdventureQuestController.instance().updateItemReceived(item);
        return true;
    }

    public void removeAllQuestItems(){
        inventoryItems.removeIf(data -> data != null && data.questItem);
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

    public void addQuest(String questID, boolean isNewGame) {
        int id = Integer.parseInt(questID);
        addQuest(id, isNewGame);
    }

    public void addQuest(int questID, boolean isNewGame) {
        AdventureQuestData toAdd = AdventureQuestController.instance().generateQuest(questID);

        if (toAdd != null) {
            addQuest(toAdd, isNewGame);
        }
    }

    public void addQuest(AdventureQuestData q, boolean isNewGame) {
        //TODO: add a config flag for this
        boolean noTrackedQuests = true;
        for (AdventureQuestData existing : quests) {
            if (noTrackedQuests && existing.isTracked) {
                noTrackedQuests = false;
                break;
            }
        }
        quests.add(q);
        if (noTrackedQuests || q.autoTrack)
            AdventureQuestController.trackQuest(q);
        q.activateNextStages();
        if (!isNewGame)
            AdventureQuestController.instance().showQuestDialogs(MapStage.getInstance());
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
     * Clears a deck by replacing the current selected deck with a new deck
     */
    public void clearDeck() {
        deck = decks.set(selectedDeckIndex, new Deck(Forge.getLocalizer().getMessage("lblEmptyDeck")));
    }

    /**
     * Actually removes the deck from the list of decks.
     */
    public void deleteDeck(){
        int oldIndex = selectedDeckIndex;
        this.setSelectedDeckSlot(0);
        decks.remove(oldIndex);
    }

    public void addDeck(){
        decks.add(new Deck(Forge.getLocalizer().getMessage("lblEmptyDeck")));
    }

    /**
     * Attempts to copy a deck to an empty slot.
     *
     * @return int - index of new copy slot, or -1 if no slot was available
     */
    public int copyDeck() {
        for (int i = 0; i < MAX_DECK_COUNT; i++) {
            if (i >= getDeckCount()) addDeck();
            if (isEmptyDeck(i)) {
                decks.set(i, (Deck) deck.copyTo(deck.getName() + " (" + Forge.getLocalizer().getMessage("lblCopy") + ")"));
                return i;
            }
        }

        return -1;
    }

    public boolean isEmptyDeck(int deckIndex) {
        return decks.get(deckIndex).isEmpty() && decks.get(deckIndex).getName().equals(Forge.getLocalizer().getMessage("lblEmptyDeck"));
    }

    public void removeEvent(AdventureEventData completedEvent) {
        events.remove(completedEvent);
    }

    public ItemPool<PaperCard> getAutoSellCards() {
        return autoSellCards;
    }

    /**
     * Gets a list of cards that can be safely sold without taking copies out of the player's decks.
     */
    public ItemPool<PaperCard> getSellableCards() {
        ItemPool<PaperCard> sellableCards = new ItemPool<>(PaperCard.class);
        sellableCards.addAllFlat(cards.toFlatList());

        // Nosell cards used to be filtered out here. Instead we're going to replace their value with 0

        // 1a. Potentially return here if we want to give config option to sell cards from decks
        // but would need to update the decks on sell, not just the catalog

        // 2. Count max cards across all decks in excess of unsellable
        Map<PaperCard, Integer> maxCardCounts = new HashMap<>();

        for (Deck deck : decks) {
            for (final Map.Entry<PaperCard, Integer> cp : deck.getAllCardsInASinglePool(true, true)) {
                int count = cp.getValue();
                if (count > maxCardCounts.getOrDefault(cp.getKey(), 0)) {
                    maxCardCounts.put(cp.getKey(), count);
                }
            }
        }

        // 3. Remove the highest use count of each card, remainder can be sold safely
        for (PaperCard card : maxCardCounts.keySet()) {
            sellableCards.remove(card, maxCardCounts.get(card));
        }

        return sellableCards;
    }

    /**
     * Gets the number of copies of this card that the player needs to keep for their decks to remain valid.
     * Copies are shared between decks, so if one deck uses 1 copy and another deck uses 2, the player needs 2 copies.
     */
    public int getCopiesUsedInDecks(PaperCard card) {
        int copiesUsed = 0;
        for(Deck deck : decks) {
            copiesUsed = Math.max(copiesUsed, deck.count(card));
        }
        return copiesUsed;
    }

    public void removeLostCardFromPools(PaperCard card) {
        if (card.isVeryBasicLand() && !card.isFoil()) {
            return;
        }

        int leftInPool = Current.player().getCards().count(card) - 1;

        for (final Deck deck : decks) {
            int cntInDeck = deck.count(card);
            int nToRemoveFromThisDeck = cntInDeck - leftInPool;
            if (nToRemoveFromThisDeck <= 0) {
                continue;
            }

            for(DeckSection section : DeckSection.values()) {
                if (section == DeckSection.Main || deck.get(section) == null) {
                    continue;
                }
                int cntInSection = deck.get(section).count(card);
                int nToRemoveFromSection = Math.min(cntInSection, nToRemoveFromThisDeck);
                if (nToRemoveFromSection > 0) {
                    deck.get(section).remove(card, nToRemoveFromSection);
                    nToRemoveFromThisDeck -= nToRemoveFromSection;
                    if (nToRemoveFromThisDeck <= 0) {
                        break;
                    }
                }
            }

            if (nToRemoveFromThisDeck <= 0) {
                continue;
            }

            deck.getMain().remove(card, nToRemoveFromThisDeck);
        }
        Current.player().getCards().remove(card, 1);
    }

    public CardPool getCollectionCards(boolean allCards) {
        CardPool collectionCards = new CardPool();
        collectionCards.addAll(cards);
        if (!allCards) {
            collectionCards.removeAll(autoSellCards);
        }

        return collectionCards;
    }

    public void loadChanges(PointOfInterestChanges changes) {
        this.currentLocationChanges = changes;
    }
}
