package forge.adventure.data;

import com.badlogic.gdx.utils.Array;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import forge.StaticData;
import forge.adventure.util.CardUtil;
import forge.adventure.util.Config;
import forge.adventure.util.Current;
import forge.adventure.util.Reward;
import forge.adventure.world.WorldSave;
import forge.item.PaperCard;
import forge.model.FModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * Data class that will be used to read Json configuration files
 * BiomeData
 * contains the information for a "reward"
 * that can be a random card, gold or items.
 * Also used for deck generation and shops
 */
public class RewardData {
    public String type;
    public float probability;
    public int count;
    public int addMaxCount;
    public String cardName;
    public String itemName;
    public String[] itemNames;
    public String[] editions;
    public String[] colors;
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

    public RewardData() { }

    public RewardData(RewardData rewardData) {
        type        =rewardData.type;
        probability =rewardData.probability;
        count       =rewardData.count;
        addMaxCount =rewardData.addMaxCount;
        cardName    =rewardData.cardName;
        itemName    =rewardData.itemName;
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
    }

    private static Iterable<PaperCard> allCards;
    private static Iterable<PaperCard> allEnemyCards;

    static private void initializeAllCards(){
        RewardData legals = Config.instance().getConfigData().legalCards;
        if(legals==null)
            allCards = FModel.getMagicDb().getCommonCards().getUniqueCardsNoAlt();
        else
            allCards = Iterables.filter(FModel.getMagicDb().getCommonCards().getUniqueCardsNoAlt(),  new CardUtil.CardPredicate(legals, true));
        //Filter out specific cards.
        allCards = Iterables.filter(allCards,  new Predicate<PaperCard>() {
            @Override
            public boolean apply(PaperCard input){
                if(input == null)
                    return false;
                if (Iterables.contains(input.getRules().getMainPart().getKeywords(), "Remove CARDNAME from your deck before playing if you're not playing for ante."))
                   return false;
                if(input.getRules().getAiHints().getRemNonCommanderDecks())
                    return false;
                if(Arrays.asList(Config.instance().getConfigData().restrictedEditions).contains(input.getEdition()))
                    return false;

                return !Arrays.asList(Config.instance().getConfigData().restrictedCards).contains(input.getName());
            }
        });
        //Filter AI cards for enemies.
        allEnemyCards=Iterables.filter(allCards, new Predicate<PaperCard>() {
            @Override
            public boolean apply(PaperCard input) {
                if (input == null) return false;
                return !input.getRules().getAiHints().getRemAIDecks();
            }
        });
    }

    static public Iterable<PaperCard> getAllCards() {
        if(allCards == null) initializeAllCards();
        return allCards;
    }

    public Array<Reward> generate(boolean isForEnemy) {
        return generate(isForEnemy, null);
    }
    public Array<Reward> generate(boolean isForEnemy, Iterable<PaperCard> cards) {
        if(allCards==null) initializeAllCards();
        Array<Reward> ret=new Array<>();
        if(probability == 0 || WorldSave.getCurrentSave().getWorld().getRandom().nextFloat() <= probability) {
            if(type==null || type.isEmpty())
                type="randomCard";
            int maxCount=Math.round(addMaxCount*Current.player().getDifficulty().rewardMaxFactor);
            int addedCount = (maxCount > 0 ? WorldSave.getCurrentSave().getWorld().getRandom().nextInt(maxCount) : 0);

            switch(type) {
                case "Union":
                    HashSet<PaperCard> pool = new HashSet<>();
                    for (RewardData r : cardUnion) {
                        pool.addAll(CardUtil.getPredicateResult(allCards, r));
                    }
                    ArrayList<PaperCard> finalPool = new ArrayList(pool);

                    if (finalPool.size() > 0){
                        for (int i = 0; i < count; i++) {
                            ret.add(new Reward(finalPool.get(WorldSave.getCurrentSave().getWorld().getRandom().nextInt(finalPool.size()))));
                        }
                    }
                    break;
                case "card":
                case "randomCard":
                    if( cardName != null && !cardName.isEmpty() ) {
                        for(int i = 0; i < count + addedCount; i++) {
                            ret.add(new Reward(StaticData.instance().getCommonCards().getCard(cardName)));
                        }
                    } else {
                        for(PaperCard card:CardUtil.generateCards(isForEnemy ? allEnemyCards:allCards,this, count+addedCount)) {
                            ret.add(new Reward(card));
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
                case "deckCard":
                    if(cards == null) return ret;
                    for(PaperCard card: CardUtil.generateCards(cards,this, count + addedCount + Current.player().bonusDeckCards() )) {
                        ret.add(new Reward(card));
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
            ret.addAll(data.generate(isForEnemy));
        return ret;
    }
    static public List<PaperCard> rewardsToCards(Iterable<Reward> dataList) {
        ArrayList<PaperCard> ret=new ArrayList<PaperCard>();
        for (Reward data:dataList) {
            ret.add(data.getCard());
        }
        return ret;
    }

}
