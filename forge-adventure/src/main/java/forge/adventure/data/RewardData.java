package forge.adventure.data;

import com.badlogic.gdx.utils.Array;
import forge.StaticData;
import forge.adventure.util.CardUtil;
import forge.adventure.util.Reward;
import forge.adventure.world.WorldSave;
import forge.item.PaperCard;
import forge.model.FModel;

public class RewardData {
    public String type;
    public float probability;
    public int count;
    public int addMaxCount;
    public String cardName;
    public Array<String> editions;
    public Array<String> colors;
    public Array<String> rarity;
    public Array<String> subTypes;
    public Array<String> cardTypes;
    public Array<String> superTypes;
    public String colorType;

    private static Iterable<PaperCard> allCards;
    public Array<Reward> generate()
    {
        return generate(null);
    }
    public Array<Reward> generate(Iterable<PaperCard> cards)
    {
        if(allCards==null)
            allCards = FModel.getMagicDb().getCommonCards().getAllNonPromosNonReprintsNoAlt();
        Array<Reward> ret=new Array<>();
        if(probability==0|| WorldSave.getCurrentSave().getWorld().getRandom().nextFloat()<=probability)
        {
            if(type==null||type=="")
                type="randomCard";
            int addedCount=(int)((float)(addMaxCount)* WorldSave.getCurrentSave().getWorld().getRandom().nextFloat());

            switch(type)
            {
                case "randomCard":
                    for(PaperCard card:CardUtil.generateCards(allCards,this, count+addedCount))
                    {
                        ret.add(new Reward(card));
                    }
                    break;
                case "deckCard":
                    if(cards==null)return ret;
                    for(PaperCard card: CardUtil.generateCards(cards,this, count+addedCount))
                    {
                        ret.add(new Reward(card));
                    }
                    break;
                case "card":
                    for(int i=0;i<count+addedCount;i++)
                    {
                        ret.add(new Reward(StaticData.instance().getCommonCards().getCard(cardName)));
                    }
                    break;
                case "gold":
                    ret.add(new Reward(count+addedCount));
                    break;
                case "life":
                    ret.add(new Reward(Reward.Type.Life, count+addedCount));
                    break;
            }
        }
        return ret;
    }

}
