package forge.adventure.util;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import forge.StaticData;
import forge.adventure.data.GeneratedDeckData;
import forge.adventure.data.GeneratedDeckTemplateData;
import forge.adventure.data.RewardData;
import forge.card.*;
import forge.card.mana.ManaCostShard;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.deck.DeckgenUtil;
import forge.deck.io.DeckSerializer;
import forge.game.GameFormat;
import forge.item.BoosterPack;
import forge.item.PaperCard;
import forge.item.SealedProduct;
import forge.item.generation.UnOpenedProduct;
import forge.model.FModel;
import forge.util.Aggregates;

import java.io.File;
import java.util.*;
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
        private final boolean matchAllColors;
        private  int colors;
        private final ColorType colorType;
        private final boolean shouldBeEqual;
        private final List<String> deckNeeds=new ArrayList<>();

        @Override
        public boolean apply(final PaperCard card) {
            if(!this.rarities.isEmpty()&&!this.rarities.contains(card.getRarity()))
                return !this.shouldBeEqual;
            if(!this.editions.isEmpty()&&!this.editions.contains(card.getEdition())) {
                boolean found = false;
                List<PaperCard> allPrintings = FModel.getMagicDb().getCommonCards().getAllCards(card.getCardName());
                for (PaperCard c : allPrintings){
                    if (this.editions.contains(c.getEdition())) {
                        found = true;
                        break;
                    }
                }
                if (!found)
                    return !this.shouldBeEqual;
            }
            if(!this.manaCosts.isEmpty()&&!this.manaCosts.contains(card.getRules().getManaCost().getCMC()))
                return !this.shouldBeEqual;
            if(this.text!=null&& !this.text.matcher(card.getRules().getOracleText()).find())
                return !this.shouldBeEqual;

            if(this.matchAllColors)
            {
                if(!card.getRules().getColor().hasAllColors(this.colors))
                {
                    return !this.shouldBeEqual;
                }
            }

            if(this.colors!= MagicColor.ALL_COLORS)
            {
                if(!card.getRules().getColor().hasNoColorsExcept(this.colors)||(this.colors != MagicColor.COLORLESS && card.getRules().getColor().isColorless()))
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

            if(!this.deckNeeds.isEmpty())
            {
                boolean found = false;
                for(String need:this.deckNeeds)
                {
                    //FormatExpected: X$Y, where X is DeckHints.Type and Y is a string descriptor
                    String[] parts = need.split("\\$");

                    if (parts.length != 2){
                        continue;
                    }
                    DeckHints.Type t = DeckHints.Type.valueOf(parts[0].toUpperCase());

                    DeckHints hints = card.getRules().getAiHints().getDeckHints();
                    if (hints != null && hints.contains(t, parts[1])){
                        found=true;
                        break;
                    }
                }
                if(!found)
                    return !this.shouldBeEqual;
            }


            return this.shouldBeEqual;
        }
        private Pattern getPattern(RewardData type) {
            if (type.cardText == null || type.cardText.isEmpty())
                return null;
            try {
                return Pattern.compile(type.cardText, Pattern.CASE_INSENSITIVE);
            } catch (Exception e) {
                System.err.println("[" + type.cardName + "|" + type.itemName + "]\n" + e);
                return null;
            }
        }
        public CardPredicate(final RewardData type, final boolean wantEqual) {
            this.matchAllSubTypes=type.matchAllSubTypes;
            this.matchAllColors=type.matchAllColors;
            this.shouldBeEqual = wantEqual;
            for(int i=0;type.manaCosts!=null&&i<type.manaCosts.length;i++)
                manaCosts.add(type.manaCosts[i]);
            text = getPattern(type);
            if(type.colors==null||type.colors.length==0)
            {
                this.colors=MagicColor.ALL_COLORS;
            }
            else
            {
                this.colors=0;
                for(String color:type.colors)
                {
                    if("colorID".equals(color))
                        for (byte c : Current.player().getColorIdentity())
                            colors |= c;
                    else
                        colors |= MagicColor.fromName(color.toLowerCase());
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
            if(type.deckNeeds!=null&&type.deckNeeds.length!=0){
                deckNeeds.addAll(Arrays.asList(type.deckNeeds));
            }
        }
    }

    public static List<PaperCard> getPredicateResult(Iterable<PaperCard> cards,final RewardData data)
    {
        List<PaperCard> result = new ArrayList<>();
        CardPredicate pre = new CardPredicate(data, true);

        for (final PaperCard item : cards)
        {
            if(pre.apply(item))
                result.add(item);
        }
        return result;
    }

    public static List<PaperCard> generateCards(Iterable<PaperCard> cards,final RewardData data, final int count, Random r)
    {

        final List<PaperCard> result = new ArrayList<>();
        List<PaperCard> pool = getPredicateResult(cards, data);
        if (pool.size() > 0) {
            for (int i = 0; i < count; i++) {
                PaperCard candidate = pool.get(r.nextInt(pool.size()));
                if (candidate != null) {
                    result.add(candidate);
                }
            }
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
        if(reward.getType()== Reward.Type.Life)
            return reward.getCount()*500;
        if(reward.getType()== Reward.Type.Shards)
            return reward.getCount()*500;
        if(reward.getType()== Reward.Type.Gold)
            return reward.getCount();
        return 1000;
    }

    public static Deck generateDeck(GeneratedDeckData data, CardEdition starterEdition, boolean discourageDuplicates)
    {
        List<String> editionCodes = (starterEdition != null)?Arrays.asList(starterEdition.getCode(), starterEdition.getCode2()):Arrays.asList("JMP", "J22", "DMU", "BRO", "ONE", "MOM");
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

            Map <String, List<PaperCard>> packCandidates=null;
            List<String> usedPackNames=new ArrayList<String>();

            for(int i=0;i<data.jumpstartPacks.length;i++)
            {

                final byte targetColor = MagicColor.fromName(data.jumpstartPacks[i]);
                String targetName;
                switch (targetColor)
                {
                    default:
                    case MagicColor.WHITE:  targetName = "Plains";  break;
                    case MagicColor.BLUE:   targetName = "Island";  break;
                    case MagicColor.BLACK:  targetName = "Swamp";   break;
                    case MagicColor.RED:    targetName = "Mountain";break;
                    case MagicColor.GREEN:  targetName = "Forest";  break;
                }

                packCandidates=new HashMap<>();
                for(SealedProduct.Template template : StaticData.instance().getSpecialBoosters())
                {
                    if (!editionCodes.contains(template.getEdition().split("\\s",2)[0]))
                        continue;
                    List<PaperCard> packContents = new UnOpenedProduct(template).get();
                    if (packContents.size() < 18 | packContents.size() > 25)
                        continue;
                    if (packContents.stream().filter(x -> x.getName().equals(targetName)).count() >=3)
                        packCandidates.putIfAbsent(template.getEdition(), packContents);
                }
                List<PaperCard> selectedPack;
                if (discourageDuplicates) {
                    Map <String, List<PaperCard>> filteredPackCandidates= new HashMap<>();
                    for (java.util.Map.Entry<String, List<PaperCard>>  entry: packCandidates.entrySet()){
                        if (!usedPackNames.contains(entry.getKey())){
                            filteredPackCandidates.put(entry.getKey(), entry.getValue()); //deep copy so that packCandidates can be used if filtered ends up being empty
                        }
                    }
                    //Only re-use a pack if all possibilities have already been chosen
                    if (filteredPackCandidates.size() == 0)
                        filteredPackCandidates = packCandidates;
                    Object[] keys = filteredPackCandidates.keySet().toArray();

                    String keyName = (String)keys[Current.world().getRandom().nextInt(keys.length)];
                    usedPackNames.add(keyName);
                    selectedPack = filteredPackCandidates.remove(keyName);
                }
                else{
                    Object[] keys = packCandidates.keySet().toArray();
                    selectedPack = packCandidates.get((String)keys[Current.world().getRandom().nextInt(keys.length)]);
                }
                //if the packContents size above is below 20, just get random card
                int size = 20 - selectedPack.size();
                for (int c = 0; c < size; c++) {
                    selectedPack.add(Aggregates.random(selectedPack));
                }
                deck.getOrCreate(DeckSection.Main).addAllFlat(selectedPack);
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
        boolean allCardVariants=Config.instance().getSettingData().useAllCardVariants;

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
        String edition = "";
        if (allCardVariants) {
            PaperCard templateLand = CardUtil.getCardByName("Plains");
            edition = templateLand.getEdition();
        }
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
            cards.addAll(generateLands("Plains",plains,edition));
            cards.addAll(generateLands("Island",island,edition));
            cards.addAll(generateLands("Forest",forest,edition));
            cards.addAll(generateLands("Mountain",mount,edition));
            cards.addAll(generateLands("Swamp",swamp,edition));
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

    private static Collection<PaperCard> generateLands(String landName, int count) {
        return generateLands(landName, count, "");
    }

    private static Collection<PaperCard> generateLands(String landName, int count, String edition) {
        boolean allCardVariants = Config.instance().getSettingData().useAllCardVariants;
        Collection<PaperCard> ret = new ArrayList<>();

        if (allCardVariants) {
            if (edition.isEmpty()) {
                PaperCard templateLand = getCardByName(landName);
                edition = templateLand.getEdition();
            }
            for (int i = 0; i < count; i++) {
                ret.add(getCardByNameAndEdition(landName, edition));
            }
        } else {
            for (int i = 0; i < count; i++)
                ret.add(FModel.getMagicDb().getCommonCards().getCard(landName));
        }

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

    public static Deck getDeck(String path, boolean forAI, boolean isFantasyMode, String colors, boolean isTheme, boolean useGeneticAI) {
        return getDeck(path, forAI, isFantasyMode, colors, isTheme, useGeneticAI, null,true);
    }

    public static Deck getDeck(String path, boolean forAI, boolean isFantasyMode, String colors, boolean isTheme, boolean useGeneticAI, CardEdition starterEdition, boolean discourageDuplicates)
    {
        if(path.endsWith(".dck"))
            return DeckSerializer.fromFile(new File(Config.instance().getCommonFilePath(path)));

        if(forAI && (isFantasyMode||useGeneticAI)) {
            Deck deck = DeckgenUtil.getRandomOrPreconOrThemeDeck(colors, forAI, isTheme, useGeneticAI);
            if (deck != null)
                return deck;
        }
        Json json = new Json();
        FileHandle handle = Config.instance().getFile(path);
        if (handle.exists())
            return generateDeck(json.fromJson(GeneratedDeckData.class, handle), starterEdition, discourageDuplicates);
        Deck deck = DeckgenUtil.getRandomOrPreconOrThemeDeck(colors, true, false, true);
        System.err.println("Error loading JSON: " + handle.path() + "\nGenerating random deck: "+deck.getName());
        return deck;

    }

    private static final GameFormat.Collection  formats   = FModel.getFormats();





    private static final Predicate<CardEdition> filterPioneer = formats.getPioneer().editionLegalPredicate;
    private static final Predicate<CardEdition> filterModern= formats.getModern().editionLegalPredicate;
    private static final Predicate<CardEdition> filterVintage = formats.getVintage().editionLegalPredicate;
    private static final Predicate<CardEdition> filterStandard = formats.getStandard().editionLegalPredicate;
    public static Deck generateStandardBoosterAsDeck(){
        return generateRandomBoosterPackAsDeck(filterStandard);
    }
    public static Deck generatePioneerBoosterAsDeck(){
        return generateRandomBoosterPackAsDeck(filterPioneer);
    }
    public static Deck generateModernBoosterAsDeck(){
        return generateRandomBoosterPackAsDeck(filterModern);
    }
    public static Deck generateVintageBoosterAsDeck(){
        return generateRandomBoosterPackAsDeck(filterVintage);
    }

    public static Deck generateBoosterPackAsDeck(String code){

        if (Arrays.asList(Config.instance().getConfigData().restrictedEditions).contains(code)){
            System.err.println("Cannot generate booster pack, '" + code + "' is a restricted edition");
        }

        CardEdition edition = StaticData.instance().getEditions().get(code);
        if (edition == null){
            System.err.println("Set code '" + code + "' not found.");
            return new Deck();
        }
        BoosterPack cards = BoosterPack.FN_FROM_SET.apply(edition);
        return generateBoosterPackAsDeck(edition);
    }

    public static Deck generateBoosterPackAsDeck(CardEdition edition){
        Deck d = new Deck("Booster pack");
        d.setComment(edition.getCode());
        d.getMain().add(BoosterPack.FN_FROM_SET.apply(edition).getCards());
        return d;
    }

    public static Deck generateRandomBoosterPackAsDeck(final Predicate<CardEdition> editionFilter) {
        Predicate<CardEdition> filter = Predicates.and(CardEdition.Predicates.CAN_MAKE_BOOSTER, editionFilter);
        Iterable<CardEdition> possibleEditions = Iterables.filter(FModel.getMagicDb().getEditions(), filter);

        if (!possibleEditions.iterator().hasNext()) {
            System.err.println("No sets found matching edition filter that can create boosters.");
            return null;
        }

        CardEdition edition = Aggregates.random(possibleEditions);
        return generateBoosterPackAsDeck(edition);
    }

    public static PaperCard getCardByName(String cardName) {
        return Aggregates.random(Iterables.filter(getFullCardPool(), input -> input.getCardName().equals(cardName)));
    }

    public static PaperCard getCardByNameAndEdition(String cardName, String edition) {
        return Aggregates.random(Iterables.filter(getFullCardPool(), input -> input.getCardName().equals(cardName) && input.getEdition().equals(edition)));
    }

    public static Collection<PaperCard> getFullCardPool() {
        return Config.instance().getSettingData().useAllCardVariants ?
                FModel.getMagicDb().getCommonCards().getAllCards() : FModel.getMagicDb().getCommonCards().getUniqueCardsNoAlt();
    }
}

