package forge.adventure.data;

import com.badlogic.gdx.utils.Array;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import forge.StaticData;
import forge.adventure.util.*;
import forge.adventure.world.WorldSave;
import forge.card.CardEdition;
import forge.deck.Deck;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.util.IterableUtil;
import forge.util.StreamUtil;

import java.io.Serializable;
import java.util.*;
import java.util.function.Predicate;


/**
 * Data class that will be used to read Json configuration files
 * BiomeData
 * contains the information for a "reward"
 * that can be a random card, gold or items.
 * Also used for deck generation and shops
 */
public class RewardData implements Serializable {
    private static final long serialVersionUID = 3158932532013393718L;
    public String type;
    public float probability;
    public int count;
    public int addMaxCount;
    public String cardName;
    public String itemName;
    public String[] itemNames;
    public String[] editions;
    public String[] colors;
    public int startDate;
    public int endDate;
    public String[] rarity;
    public String[] subTypes;
    public String[] cardTypes;
    public String[] superTypes;
    public int[] manaCosts;
    public String[] keyWords;
    public String colorType;
    public String cardText;
    public boolean matchAllSubTypes;
    public boolean matchAllColors;
    public RewardData[] cardUnion;
    public String[] deckNeeds;
    public RewardData[] rotation;
    public Deck cardPack;
    public String sourceDeck;
    public String minDate;

    public RewardData() { }

    public RewardData(RewardData rewardData) {
        if (rewardData == null)
            return;

        type        =rewardData.type;
        probability =rewardData.probability;
        count       =rewardData.count;
        addMaxCount =rewardData.addMaxCount;
        cardName    =rewardData.cardName;
        itemName    =rewardData.itemName;
        startDate   =rewardData.startDate;
        endDate     =rewardData.endDate;
        itemNames    =rewardData.itemNames==null?null:rewardData.itemNames.clone();
        editions    =rewardData.editions==null?null:rewardData.editions.clone();
        colors      =rewardData.colors==null?null:rewardData.colors.clone();
        rarity      =rewardData.rarity==null?null:rewardData.rarity.clone();
        subTypes    =rewardData.subTypes==null?null:rewardData.subTypes.clone();
        cardTypes   =rewardData.cardTypes==null?null:rewardData.cardTypes.clone();
        superTypes  =rewardData.superTypes==null?null:rewardData.superTypes.clone();
        manaCosts   =rewardData.manaCosts==null?null:rewardData.manaCosts.clone();
        keyWords    =rewardData.keyWords==null?null:rewardData.keyWords.clone();
        colorType   =rewardData.colorType;
        cardText    =rewardData.cardText;
        matchAllSubTypes    =rewardData.matchAllSubTypes;
        matchAllColors =rewardData.matchAllColors;
        cardUnion         =rewardData.cardUnion==null?null:rewardData.cardUnion.clone();
        rotation          =rewardData.rotation==null?null:rewardData.rotation.clone();
        deckNeeds         =rewardData.deckNeeds==null?null:rewardData.deckNeeds.clone();
        cardPack          = rewardData.cardPack;
        sourceDeck  =rewardData.sourceDeck;
        minDate     =rewardData.minDate;
    }

    private static Iterable<PaperCard> allCards;
    private static Iterable<PaperCard> allEnemyCards;

    static private void initializeAllCards(){
        ConfigData configData = Config.instance().getConfigData();
        RewardData legals = configData.legalCards;

        if(legals==null)
            allCards = CardUtil.getFullCardPool(false); // we need unique cards only here, so that a unique card can be chosen before a set variant is determined
        else
            allCards = IterableUtil.filter(CardUtil.getFullCardPool(false), new CardUtil.CardPredicate(legals, true));
        //Filter out specific cards.
        allCards = IterableUtil.filter(allCards, input -> {
            if(input == null)
                return false;
            if (Iterables.contains(input.getRules().getMainPart().getKeywords(), "Remove CARDNAME from your deck before playing if you're not playing for ante."))
                return false;
            if(input.getRules().getAiHints().getRemNonCommanderDecks())
                return false;
            if(configData.allowedEditions != null) {
                if (!Arrays.asList(configData.allowedEditions).contains(input.getEdition()))
                    return false;
            } else if(Arrays.asList(configData.restrictedEditions).contains(input.getEdition()))
                return false;
            if(input.getRules().isCustom())
                return false;
            return !Arrays.asList(configData.restrictedCards).contains(input.getName());
        });
        //Filter AI cards for enemies.
        allEnemyCards= IterableUtil.filter(allCards, input -> {
            if (input == null) return false;
            return !input.getRules().getAiHints().getRemAIDecks();
        });
    }

    static public Iterable<PaperCard> getAllCards() {
        if(allCards == null) initializeAllCards();
        return allCards;
    }

    public Array<Reward> generate(boolean isForEnemy, boolean useSeedlessRandom) {
        return generate(isForEnemy, null, useSeedlessRandom);
    }

    public Array<Reward> generate(boolean isForEnemy, boolean useSeedlessRandom, boolean isNoSell) {
        return generate(isForEnemy, null, useSeedlessRandom, isNoSell);
    }

    public Array<Reward> generate(boolean isForEnemy, Iterable<PaperCard> cards, boolean useSeedlessRandom){
        return generate(isForEnemy, cards, useSeedlessRandom, false);
    }

    public Array<Reward> generate(boolean isForEnemy, Iterable<PaperCard> cards, boolean useSeedlessRandom, boolean isNoSell) {

        boolean allCardVariants = Config.instance().getSettingData().useAllCardVariants;
        Random rewardRandom = useSeedlessRandom?new Random():WorldSave.getCurrentSave().getWorld().getRandom();
        //Keep using same generation method for shop rewards, but fully randomize loot drops by not using the instance pre-seeded by the map

        if(allCards==null) initializeAllCards();
        Array<Reward> ret=new Array<>();


        if(probability == 0 || rewardRandom.nextFloat() <= probability) {
            if(type==null || type.isEmpty())
                type="randomCard";
            int maxCount=Math.round(addMaxCount*Current.player().getDifficulty().rewardMaxFactor);
            int addedCount = (maxCount > 0 ? rewardRandom.nextInt(maxCount) : 0);

            switch(type) {
                case "Union":
                    HashSet<PaperCard> pool = new HashSet<>();
                    for (RewardData r : cardUnion) {
                        if( r.cardName != null && !r.cardName.isEmpty() ) {
                            PaperCard pc = allCardVariants ? CardUtil.getCardByName(r.cardName)
                                    : StaticData.instance().getCommonCards().getCard(r.cardName);
                            if (pc != null)
                                pool.add(pc);
                        } else if( r.sourceDeck != null && ! r.sourceDeck.isEmpty() ) {
                            pool.addAll(CardUtil.getDeck(r.sourceDeck, false, false, "", false, false).getAllCardsInASinglePool().toFlatList());
                        } else {
                            pool.addAll(CardUtil.getPredicateResult(allCards, r));
                        }
                    }
                    ArrayList<PaperCard> finalPool = new ArrayList(pool);

                    if (finalPool.size() > 0){
                        for (int i = 0; i < count; i++) {
                            if (allCardVariants) {
                                PaperCard cardTemplate = finalPool.get(rewardRandom.nextInt(finalPool.size()));
                                PaperCard finalCard = CardUtil.getCardByName(cardTemplate.getCardName());
                                ret.add(new Reward(finalCard, isNoSell));
                            } else {
                                ret.add(new Reward(finalPool.get(rewardRandom.nextInt(finalPool.size())), isNoSell));
                            }
                        }
                    }
                    break;
                case "card":
                case "randomCard":
                    if( cardName != null && !cardName.isEmpty() ) {
                        if (allCardVariants) {
                            PaperCard card = CardUtil.getCardByName(cardName);
                            for (int i = 0; i < count + addedCount; i++) {
                                ret.add(new Reward(CardUtil.getCardByNameAndEdition(cardName, card.getEdition()), isNoSell));
                            }
                        } else {
                            for (int i = 0; i < count + addedCount; i++) {
                                ret.add(new Reward(StaticData.instance().getCommonCards().getCard(cardName), isNoSell));
                            }
                        }
                    } else if( sourceDeck != null && ! sourceDeck.isEmpty() ) {
                        for(PaperCard card:CardUtil.generateCards(CardUtil.getDeck(sourceDeck, false, false, "", false, false).getAllCardsInASinglePool().toFlatList() ,this, count+addedCount, rewardRandom)) {
                            ret.add(new Reward(card, isNoSell));
                        }
                    } else {
                        for(PaperCard card:CardUtil.generateCards(isForEnemy ? allEnemyCards:allCards,this, count+addedCount, rewardRandom)) {
                            ret.add(new Reward(card, isNoSell));
                        }
                    }
                    break;
                case "item":
                    if(itemNames!=null)
                    {
                        for(int i=0;i<count+addedCount;i++) {
                            ret.add(new Reward(ItemData.getItem(itemNames[WorldSave.getCurrentSave().getWorld().getRandom().nextInt(itemNames.length)])));
                        }
                    }
                    else if(itemName!=null&&!itemName.isEmpty()) {
                        for(int i=0;i<count+addedCount;i++) {
                            ret.add(new Reward(ItemData.getItem(itemName)));
                        }
                    }
                    break;
                case "cardPackShop":
                {
                    if(colors == null) {
                        CardEdition.Collection editions = FModel.getMagicDb().getEditions();
                        Predicate<CardEdition> filter = CardEdition.Predicates.CAN_MAKE_BOOSTER;
                        List<CardEdition> allEditions = new ArrayList<>();
                        StreamUtil.stream(editions)
                                .filter(filter)
                                .filter(CardEdition::hasBoosterTemplate)
                                .forEach(allEditions::add);
                        ConfigData configData = Config.instance().getConfigData();

                        for (String restricted : configData.restrictedEditions) {
                            allEditions.removeIf(q -> q.getCode().equals(restricted));
                        }
                        for(String restrictedCard: configData.restrictedCards)
                        {
                            allEditions.removeIf(
                                    cardEdition -> cardEdition.getAllCardsInSet().stream().anyMatch(
                                            o -> o.name().equals(restrictedCard))
                            );
                        }

                        endDate = endDate == 0 ? 9999 : endDate;
                        allEditions.removeIf(q -> q.getDate().getYear()+1900 < startDate || q.getDate().getYear()+1900 > endDate);
                        for(int i=0;i<count+addedCount;i++) {
                            ret.add(new Reward(AdventureEventController.instance().generateBooster(allEditions.get(WorldSave.getCurrentSave().getWorld().getRandom().nextInt(allEditions.size())).getCode())));
                        }
                    }
                    else
                    {
                        for(int i=0;i<count+addedCount;i++) {
                            ret.add(new Reward(AdventureEventController.instance().generateBoosterByColor(colors[0])));

                        }
                    }


                    break;
                }
                case "landSketchbookShop":
                    List<ItemData> sketchbookItems = Lists.newArrayList(ItemData.getAllItems());
                    sketchbookItems.removeIf(new Predicate<ItemData>() {
                        @Override
                        public boolean test(ItemData itemData) {
                            return itemData.questItem || !itemData.getName().contains("Landscape Sketchbook");
                        }
                    });

                    for(int i=0; i < count + addedCount; i++) {
                        ItemData item = sketchbookItems.get(WorldSave.getCurrentSave().getWorld().getRandom().nextInt(sketchbookItems.size()));
                        ret.add(new Reward(item));
                    }

                    break;
                case "cardPack":
                    if(cardPack!=null)
                    {
                        if (isNoSell){
                            cardPack.getTags().add("noSell");
                        }
                        ret.add(new Reward(cardPack, isNoSell));
                    }
                    break;
                case "deckCard":
                    if(cards == null) return ret;
                    for(PaperCard card: CardUtil.generateCards(cards,this, count + addedCount + Current.player().bonusDeckCards() ,rewardRandom)) {
                        ret.add(new Reward(card, isNoSell));
                    }
                    break;
                case "gold":
                    ret.add(new Reward(count + addedCount));
                    break;
                case "life":
                    ret.add(new Reward(Reward.Type.Life, count + addedCount));
                    break;
                case "mana": //backwards compatibility for reward data
                case "shards":
                    ret.add(new Reward(Reward.Type.Shards, count + addedCount));
                    break;
            }
        }
        return ret;
    }

    static public List<PaperCard> generateAllCards(Iterable<RewardData> dataList, boolean isForEnemy) {
        return rewardsToCards(generateAll(dataList, isForEnemy));
    }
    static public Iterable<Reward> generateAll(Iterable<RewardData> dataList, boolean isForEnemy) {
        Array<Reward> ret=new Array<Reward>();
        for (RewardData data:dataList)
            ret.addAll(data.generate(isForEnemy, false));
        return ret;
    }
    static public List<PaperCard> rewardsToCards(Iterable<Reward> dataList) {
        ArrayList<PaperCard> ret=new ArrayList<PaperCard>();

        boolean allCardVariants = Config.instance().getSettingData().useAllCardVariants;

        if (allCardVariants) {
            String basicLandEdition = "";
            for (Reward data : dataList) {
                PaperCard card = data.getCard();
                if (card.isVeryBasicLand()) {
                    // ensure that all basic lands share the same edition so the deck doesn't look odd
                    if (basicLandEdition.isEmpty()) {
                        basicLandEdition = card.getEdition();
                    }
                    ret.add(CardUtil.getCardByNameAndEdition(card.getName(), basicLandEdition));
                } else {
                    ret.add(card);
                }
            }
        } else {
            for (Reward data : dataList) {
                ret.add(data.getCard());
            }
        }
        return ret;
    }

}
