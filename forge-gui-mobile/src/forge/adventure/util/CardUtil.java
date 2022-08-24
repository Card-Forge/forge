package forge.adventure.util;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import forge.StaticData;
import forge.adventure.data.GeneratedDeckData;
import forge.adventure.data.GeneratedDeckTemplateData;
import forge.adventure.data.RewardData;
import forge.adventure.world.WorldSave;
import forge.card.*;
import forge.card.mana.ManaCostShard;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.deck.DeckgenUtil;
import forge.deck.io.DeckSerializer;
import forge.item.PaperCard;
import forge.model.FModel;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import static forge.adventure.data.RewardData.generateAllCards;

/**
 * Utility class to deck generation and card filtering
 */
public class CardUtil {
    public static final class CardPredicate implements Predicate<PaperCard> {
        enum ColorType
        {
            Any,
            Colorless,
            MultiColor,
            MonoColor
        }
        private final List<CardRarity> rarities=new ArrayList<>();
        private final List<String> editions=new ArrayList<>();
        private final List<String> subType=new ArrayList<>();
        private final List<String> keyWords=new ArrayList<>();
        private final List<CardType.CoreType> type=new ArrayList<>();
        private final List<CardType.Supertype> superType=new ArrayList<>();
        private final List<Integer> manaCosts =new ArrayList<>();
        private final Pattern text;
        private final boolean matchAllSubTypes;
        private  int colors;
        private final ColorType colorType;
        private final boolean shouldBeEqual;

        @Override
        public boolean apply(final PaperCard card) {
            if(!this.rarities.isEmpty()&&!this.rarities.contains(card.getRarity()))
                return !this.shouldBeEqual;
            if(!this.editions.isEmpty()&&!this.editions.contains(card.getEdition()))
                return !this.shouldBeEqual;
            if(!this.manaCosts.isEmpty()&&!this.manaCosts.contains(card.getRules().getManaCost().getCMC()))
                return !this.shouldBeEqual;
            if(this.text!=null&& !this.text.matcher(card.getRules().getOracleText()).find())
                return !this.shouldBeEqual;

            if(this.colors!= MagicColor.ALL_COLORS)
            {
                if(!card.getRules().getColor().hasNoColorsExcept(this.colors)||card.getRules().getColor().isColorless())
                    return !this.shouldBeEqual;
            }
            if(colorType!=ColorType.Any)
            {
                switch (colorType)
                {
                    case Colorless:
                        if(!card.getRules().getColor().isColorless())
                            return !this.shouldBeEqual;
                        break;
                    case MonoColor:
                        if(!card.getRules().getColor().isMonoColor())
                            return !this.shouldBeEqual;
                        break;
                    case MultiColor:
                        if(!card.getRules().getColor().isMulticolor())
                            return !this.shouldBeEqual;
                        break;
                }
            }
            if(!this.type.isEmpty())
            {
                boolean found=false;
                for(CardType.CoreType type:card.getRules().getType().getCoreTypes())
                {
                    if(this.type.contains(type))
                    {
                        found=true;
                        break;
                    }
                }
                if(!found)
                    return !this.shouldBeEqual;
            }
            if(!this.superType.isEmpty())
            {
                boolean found=false;
                for(CardType.Supertype type:card.getRules().getType().getSupertypes())
                {
                    if(this.superType.contains(type))
                    {
                        found=true;
                        break;
                    }
                }
                if(!found)
                    return !this.shouldBeEqual;
            }
            if(this.matchAllSubTypes)
            {
                if(!this.subType.isEmpty())
                {
                    if(this.subType.size()!= Iterables.size(card.getRules().getType().getSubtypes()))
                        return !this.shouldBeEqual;
                    for(String subtype:card.getRules().getType().getSubtypes())
                    {
                        if(!this.subType.contains(subtype))
                        {
                            return !this.shouldBeEqual;
                        }
                    }
                }
            }
            else
            {
                if(!this.subType.isEmpty())
                {
                    boolean found=false;
                    for(String subtype:card.getRules().getType().getSubtypes())
                    {
                        if(this.subType.contains(subtype))
                        {
                            found=true;
                            break;
                        }
                    }
                    if(!found)
                        return !this.shouldBeEqual;
                }
            }

            if(!this.keyWords.isEmpty())
            {
                boolean found=false;
                for(String keyWord:this.keyWords)
                {
                    if(card.getRules().hasKeyword(keyWord))
                    {
                        found=true;
                        break;
                    }
                }
                if(!found)
                    return !this.shouldBeEqual;
            }



            return this.shouldBeEqual;
        }

        public CardPredicate(final RewardData type, final boolean wantEqual) {
            this.matchAllSubTypes=type.matchAllSubTypes;
            this.shouldBeEqual = wantEqual;
            for(int i=0;type.manaCosts!=null&&i<type.manaCosts.length;i++)
                manaCosts.add(type.manaCosts[i]);
            text = type.cardText==null||type.cardText.isEmpty()?null:Pattern.compile(type.cardText, Pattern.CASE_INSENSITIVE);
            if(type.colors==null||type.colors.length==0)
            {
                this.colors=MagicColor.ALL_COLORS;
            }
            else
            {
                this.colors=0;
                for(String color:type.colors)
                {
                    colors|=MagicColor.fromName(color.toLowerCase());
                }
            }
            if(type.keyWords!=null&&type.keyWords.length!=0)
            {
                keyWords.addAll(Arrays.asList(type.keyWords));
            }
            if(type.rarity!=null&&type.rarity.length!=0)
            {
                for(String rarity:type.rarity)
                {
                    rarities.add(CardRarity.smartValueOf(rarity));
                }
            }

            if(type.subTypes!=null&&type.subTypes.length!=0)
            {
                subType.addAll(Arrays.asList(type.subTypes));
            }
            if(type.editions!=null&&type.editions.length!=0)
            {
                editions.addAll(Arrays.asList(type.editions));
            }
            if(type.superTypes!=null&&type.superTypes.length!=0)
            {
                for(String string:type.superTypes)
                    superType.add(CardType.Supertype.getEnum(string));
            }
            if(type.cardTypes!=null&&type.cardTypes.length!=0)
            {
                for(String string:type.cardTypes)
                    this.type.add(CardType.CoreType.getEnum(string));
            }
            if(type.colorType!=null&&!type.colorType.isEmpty())
            {
                this.colorType=ColorType.valueOf(type.colorType);
            }
            else
            {
                this.colorType=ColorType.Any;
            }
        }
    }

    public static List<PaperCard> generateCards(Iterable<PaperCard> cards,final RewardData data, final int count)
    {

        final List<PaperCard> result = new ArrayList<>();


        for (int i=0;i<count;i++) {

            CardPredicate pre=new CardPredicate(data, true);
            PaperCard card = null;
            int lowest = Integer.MAX_VALUE;
            for (final PaperCard item : cards)
            {
                if(!pre.apply(item))
                    continue;
                int next = WorldSave.getCurrentSave().getWorld().getRandom().nextInt();
                if(next < lowest) {
                    lowest = next;
                    card = item;
                }
            }
            if (card != null )
                result.add(card);
        }

        return result;
    }
    public static int getCardPrice(PaperCard card)
    {
        if(card==null)
            return 0;
        switch (card.getRarity())
        {
            case BasicLand:
                return 5;
            case Common:
                return 50;
            case Uncommon:
                return 150;
            case Rare:
                return 300;
            case MythicRare:
                return 500;
            default:
                return 600;
        }
    }
    public static int getRewardPrice(Reward reward)
    {
        PaperCard card=reward.getCard();
        if(card!=null)
            return getCardPrice(card);
        if(reward.getItem()!=null)
            return reward.getItem().cost;
        return 1000;
    }

    public static Deck generateDeck(GeneratedDeckData data)
    {
        Deck deck= new Deck(data.name);
        if(data.mainDeck!=null)
        {
            deck.getOrCreate(DeckSection.Main).addAllFlat(generateAllCards(Arrays.asList(data.mainDeck), true));
            if(data.sideBoard!=null)
                deck.getOrCreate(DeckSection.Sideboard).addAllFlat(generateAllCards(Arrays.asList(data.sideBoard), true));
            return deck;
        }
        if(data.jumpstartPacks!=null)
        {
            deck.getOrCreate(DeckSection.Main);
            for(int i=0;i<data.jumpstartPacks.length;i++)
            {

                String targetName;
                switch (MagicColor.fromName(data.jumpstartPacks[i]))
                {
                    default:
                    case MagicColor.WHITE:  targetName = "Plains";  break;
                    case MagicColor.BLUE:   targetName = "Island";  break;
                    case MagicColor.BLACK:  targetName = "Swamp";   break;
                    case MagicColor.RED:    targetName = "Mountain";break;
                    case MagicColor.GREEN:  targetName = "Forest";  break;
                }

                List<PrintSheet> candidates=new ArrayList<>();
                for(PrintSheet sheet : StaticData.instance().getPrintSheets())
                {
                    if(sheet.containsCardNamed(targetName,3)&&sheet.getName().startsWith("JMP"))//dodge the rainbow jumpstart sheet
                    {
                        candidates.add(sheet);
                    }
                }
                deck.getOrCreate(DeckSection.Main).addAllFlat(candidates.get(Current.world().getRandom().nextInt(candidates.size())).all());
            }
            return deck;
        }
       if(data.template!=null)
       {
           float count=data.template.count;
           float lands=count*0.4f;
           float spells=count-lands;
           List<RewardData> dataArray= generateRewards(data.template,spells*0.5f,new int[]{1,2});
           dataArray.addAll(generateRewards(data.template,spells*0.3f,new int[]{3,4,5}));
           dataArray.addAll(generateRewards(data.template,spells*0.2f,new int[]{6,7,8}));
           List<PaperCard>  nonLand= generateAllCards(dataArray, true);

           nonLand.addAll(fillWithLands(nonLand,data.template));
           deck.getOrCreate(DeckSection.Main).addAllFlat(nonLand);
       }
        return deck;
    }

    private static List<PaperCard> fillWithLands(List<PaperCard> nonLands, GeneratedDeckTemplateData template) {
        int red=0;
        int blue=0;
        int green=0;
        int white=0;
        int black=0;
        int colorLess=0;
        int cardCount=nonLands.size();
        List<PaperCard> cards=new ArrayList<>();
        for(PaperCard nonLand:nonLands)
        {
            red+=nonLand.getRules().getManaCost().getShardCount(ManaCostShard.RED);
            green+=nonLand.getRules().getManaCost().getShardCount(ManaCostShard.GREEN);
            white+=nonLand.getRules().getManaCost().getShardCount(ManaCostShard.WHITE);
            blue+=nonLand.getRules().getManaCost().getShardCount(ManaCostShard.BLUE);
            black+=nonLand.getRules().getManaCost().getShardCount(ManaCostShard.BLACK);
            colorLess+=nonLand.getRules().getManaCost().getShardCount(ManaCostShard.GENERIC);
        }
        float sum= red+ blue+ green+ white+ black;
        int neededLands=template.count-cardCount;
        int neededDualLands= Math.round (neededLands*template.rares);
        int neededBase=neededLands-neededDualLands;
        if(sum==0.)
        {
            cards.addAll(generateLands("Wastes",neededLands));
        }
        else
        {
            int mount=Math.round(neededBase*(red/sum));
            int island=Math.round(neededBase*(blue/sum));
            int forest=Math.round(neededBase*(green/sum));
            int plains=Math.round(neededBase*(white/sum));
            int swamp=Math.round(neededBase*(black/sum));
            cards.addAll(generateLands("Plains",plains));
            cards.addAll(generateLands("Island",island));
            cards.addAll(generateLands("Forest",forest));
            cards.addAll(generateLands("Mountain",mount));
            cards.addAll(generateLands("Swamp",swamp));
            List<String> landTypes=new ArrayList<>();
            if(mount>0)
                landTypes.add("Mountain");
            if(island>0)
                landTypes.add("Island");
            if(plains>0)
                landTypes.add("Plains");
            if(swamp>0)
                landTypes.add("Swamp");
            if(forest>0)
                landTypes.add("Forest");
            cards.addAll(generateDualLands(landTypes,neededDualLands));

        }
        return cards;
    }

    private static Collection<PaperCard> generateDualLands(List<String> landName, int count) {
        ArrayList<RewardData> rewards=new ArrayList<>();
        RewardData base= new RewardData();
        rewards.add(base);
        base.cardTypes=new String[]{"Land"};
        base.count=count;
        base.matchAllSubTypes=true;
        if(landName.size()==1)
        {
            base.subTypes=new String[]{landName.get(0)};
        }
        else if(landName.size()==2)
        {
            base.subTypes=new String[]{landName.get(0),landName.get(1)};
        }
        else if(landName.size()==3)
        {
            RewardData sub1= new RewardData(base);
            RewardData sub2= new RewardData(base);
            sub1.count/=3;
            sub2.count/=3;
            base.count-=sub1.count;
            base.count-=sub2.count;

            base.subTypes=new String[]{landName.get(0),landName.get(1)};
            sub1.subTypes=new String[]{landName.get(1),landName.get(2)};
            sub2.subTypes=new String[]{landName.get(0),landName.get(2)};
            rewards.addAll(Arrays.asList(sub1,sub2));
        }
        else if(landName.size()==4)
        {
            RewardData sub1= new RewardData(base);
            RewardData sub2= new RewardData(base);
            RewardData sub3= new RewardData(base);
            RewardData sub4= new RewardData(base);
            sub1.count/=5;
            sub2.count/=5;
            sub3.count/=5;
            sub4.count/=5;
            base.count-=sub1.count;
            base.count-=sub2.count;
            base.count-=sub3.count;
            base.count-=sub4.count;

            base.subTypes = new String[]{landName.get(0),landName.get(1)};
            sub1.subTypes = new String[]{landName.get(0),landName.get(2)};
            sub2.subTypes = new String[]{landName.get(0),landName.get(3)};
            sub3.subTypes = new String[]{landName.get(1),landName.get(2)};
            sub4.subTypes = new String[]{landName.get(1),landName.get(3)};
            rewards.addAll(Arrays.asList(sub1,sub2,sub3,sub4));
        }
        else if(landName.size()==5)
        {
            RewardData sub1= new RewardData(base);
            RewardData sub2= new RewardData(base);
            RewardData sub3= new RewardData(base);
            RewardData sub4= new RewardData(base);
            RewardData sub5= new RewardData(base);
            RewardData sub6= new RewardData(base);
            RewardData sub7= new RewardData(base);
            RewardData sub8= new RewardData(base);
            RewardData sub9= new RewardData(base);
            sub1.count/=10;
            sub2.count/=10;
            sub3.count/=10;
            sub4.count/=10;
            sub5.count/=10;
            sub6.count/=10;
            sub7.count/=10;
            sub8.count/=10;
            sub9.count/=10;
            base.count-=sub1.count;
            base.count-=sub2.count;
            base.count-=sub3.count;
            base.count-=sub4.count;
            base.count-=sub5.count;
            base.count-=sub6.count;
            base.count-=sub7.count;
            base.count-=sub8.count;
            base.count-=sub9.count;

            base.subTypes=new String[]{landName.get(0),landName.get(1)};
            sub1.subTypes=new String[]{landName.get(0),landName.get(2)};
            sub2.subTypes=new String[]{landName.get(0),landName.get(3)};
            sub3.subTypes=new String[]{landName.get(0),landName.get(4)};
            sub4.subTypes=new String[]{landName.get(1),landName.get(2)};
            sub5.subTypes=new String[]{landName.get(1),landName.get(3)};
            sub6.subTypes=new String[]{landName.get(1),landName.get(4)};
            sub7.subTypes=new String[]{landName.get(2),landName.get(3)};
            sub8.subTypes=new String[]{landName.get(2),landName.get(4)};
            sub9.subTypes=new String[]{landName.get(3),landName.get(4)};
            rewards.addAll(Arrays.asList(sub1,sub2,sub3,sub4,sub5,sub6,sub7,sub8,sub9));
        }

        Collection<PaperCard> ret = new ArrayList<>(generateAllCards(rewards, true));
        return ret;
    }

    private static Collection<PaperCard> generateLands(String landName,int count) {
        Collection<PaperCard> ret=new ArrayList<>();
        for(int i=0;i<count;i++)
            ret.add(FModel.getMagicDb().getCommonCards().getCard(landName));

        return ret;
    }

    private static List<RewardData> generateRewards(GeneratedDeckTemplateData template, float count, int[] manaCosts) {
        ArrayList<RewardData> ret=new ArrayList<>();
        ret.addAll(templateGenerate(template,count-(count*template.rares),manaCosts,new String[]{"Uncommon","Common"}));
        ret.addAll(templateGenerate(template,count*template.rares,manaCosts,new String[]{"Rare","Mythic Rare"}));
        return ret;
    }

    private static ArrayList<RewardData> templateGenerate(GeneratedDeckTemplateData template, float count, int[] manaCosts, String[] strings) {
        ArrayList<RewardData> ret=new ArrayList<>();
        RewardData base= new RewardData();
        base.manaCosts=manaCosts;
        base.rarity=strings;
        base.colors=template.colors;
        if(template.tribe!=null&&!template.tribe.isEmpty())
        {
            RewardData caresAbout= new RewardData(base);
            caresAbout.cardText="\\b"+template.tribe+"\\b";
            caresAbout.count=  Math.round(count*template.tribeSynergyCards);
            ret.add(caresAbout);

            base.subTypes=new String[]{template.tribe};
            base.count=  Math.round(count*(1-template.tribeSynergyCards));
        }
        else
        {
            base.count=  Math.round(count);
        }
        ret.add(base);
        return  ret;
    }

    public static Deck getDeck(String path, boolean forAI, boolean isFantasyMode, String colors, boolean isTheme, boolean useGeneticAI)
    {
        if(path.endsWith(".dck"))
            return DeckSerializer.fromFile(new File(Config.instance().getFilePath(path)));

        if(forAI && (isFantasyMode||useGeneticAI)) {
            Deck deck = DeckgenUtil.getRandomOrPreconOrThemeDeck(colors, forAI, isTheme, useGeneticAI);
            if (deck != null)
                return deck;
        }
        Json json = new Json();
        FileHandle handle = Config.instance().getFile(path);
        if (handle.exists())
            return generateDeck(json.fromJson(GeneratedDeckData.class, handle));
        return null;

    }
}
