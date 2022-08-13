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

    public RewardData() { }

    public RewardData(RewardData rewardData) {
        type        =rewardData.type;
        probability =rewardData.probability;
        count       =rewardData.count;
        addMaxCount =rewardData.addMaxCount;
        cardName    =rewardData.cardName;
        itemName    =rewardData.itemName;
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
    }

    private static Iterable<PaperCard> allCards;
    private static Iterable<PaperCard> allEnemyCards;

    private void initializeAllCards(){
        RewardData legals = Config.instance().getConfigData().legalCards;
        if(legals==null) allCards = FModel.getMagicDb().getCommonCards().getUniqueCardsNoAltNoOnline();
        else             allCards = Iterables.filter(FModel.getMagicDb().getCommonCards().getUniqueCardsNoAltNoOnline(),  new CardUtil.CardPredicate(legals, true));
        //Filter out specific cards.
        allCards = Iterables.filter(allCards,  new Predicate<PaperCard>() {
            @Override
            public boolean apply(PaperCard input){
                if(input == null) return false;
                if(Arrays.asList(Config.instance().getConfigData().restrictedEditions).contains(input.getEdition())) return false;
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

    public Iterable<PaperCard> getAllCards() {
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
            int addedCount = (addMaxCount > 0 ? WorldSave.getCurrentSave().getWorld().getRandom().nextInt(addMaxCount) : 0);
            if( colors != null && colors.length > 0 ) { //Filter special "colorID" case.
                String C = Current.player().getColorIdentityLong();
                for(int i = 0; i < colors.length; i++){
                    if(colors[i].equals("colorID")){
                        if(C.equals("colorless")) { //Colorless nullifies all other possible colors.
                            //A quirk of the filter, but flavorful.
                            colorType = "Colorless";
                            colors = null;
                            break;
                        }
                        else colors[i] = C;
                    }
                }
            }

            switch(type) {
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
                    if(itemName!=null&&!itemName.isEmpty()) {
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
