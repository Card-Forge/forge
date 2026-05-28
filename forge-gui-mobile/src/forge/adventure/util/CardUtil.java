package forge.adventure.util;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.google.common.collect.Iterables;
import forge.StaticData;
import forge.adventure.data.ConfigData;
import forge.adventure.data.GeneratedDeckData;
import forge.adventure.data.GeneratedDeckTemplateData;
import forge.adventure.data.RewardData;
import forge.card.*;
import forge.card.DeckHints.Type;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostShard;
import forge.adventure.player.AdventurePlayer;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckRecognizer;
import forge.deck.DeckSection;
import forge.deck.DeckgenUtil;
import forge.deck.io.DeckSerializer;
import forge.util.FileUtil;
import forge.util.ItemPool;
import forge.game.GameFormat;
import forge.game.GameType;
import forge.item.BoosterPack;
import forge.item.PaperCard;
import forge.item.PaperCardPredicates;
import forge.item.SealedTemplate;
import forge.item.generation.UnOpenedProduct;
import forge.model.FModel;
import forge.util.Aggregates;
import forge.util.IterableUtil;
import forge.card.MagicColor;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static forge.adventure.data.RewardData.generateAllCards;

/**
 * Utility class to deck generation and card filtering
 */
public class CardUtil {
    public static final class CardPredicate implements Predicate<PaperCard> {
        enum ColorType {
            Any,
            Colorless,
            MultiColor,
            MonoColor
        }

        private final List<CardRarity> rarities = new ArrayList<>();
        private final List<String> editions = new ArrayList<>();
        private final List<String> subType = new ArrayList<>();
        private final List<String> keyWords = new ArrayList<>();
        private final List<CardType.CoreType> type = new ArrayList<>();
        private final List<CardType.Supertype> superType = new ArrayList<>();
        private final List<Integer> manaCosts = new ArrayList<>();
        private final Pattern text;
        private final boolean matchAllSubTypes;
        private final boolean matchAllColors;
        private int colors;
        private final ColorType colorType;
        private final boolean shouldBeEqual;
        private final List<String> deckNeeds = new ArrayList<>();
        private final String minDate;
        private final static SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

        private static Date parseDate(String date) {
            if (date.length() <= 7)
                date = date + "-01";
            try {
                return formatter.parse(date);
            } catch (Exception e) {
                return new Date();
            }
        }

        @Override
        public boolean test(final PaperCard card) {
            if (!this.rarities.isEmpty() && !this.rarities.contains(card.getRarity()))
                return !this.shouldBeEqual;
            if (!this.editions.isEmpty() && !this.editions.contains(card.getEdition())) {
                boolean found = false;
                List<PaperCard> allPrintings = FModel.getMagicDb().getCommonCards().getAllCards(card);
                for (PaperCard c : allPrintings) {
                    if (this.editions.contains(c.getEdition())) {
                        found = true;
                        break;
                    }
                }
                if (!found)
                    return !this.shouldBeEqual;
            }
            if (!this.minDate.isEmpty()) {
                boolean found = false;
                List<PaperCard> allPrintings = FModel.getMagicDb().getCommonCards().getAllCards(card);
                List<CardEdition> cardEditionList = new ArrayList<>();

                Date d = parseDate(this.minDate);

                for (CardEdition e : FModel.getMagicDb().getEditions()) {
                    if (e.getDate().before(d))
                        continue;
                    cardEditionList.add(e);
                }

                for (PaperCard c : allPrintings) {
                    for (CardEdition e : cardEditionList) {
                        if (e.getCode().equals(c.getEdition())) {
                            found = true;
                            break;
                        }
                    }
                }
                if (!found)
                    return !this.shouldBeEqual;
            }
            if (!this.manaCosts.isEmpty() && !this.manaCosts.contains(card.getRules().getManaCost().getCMC()))
                return !this.shouldBeEqual;
            if (this.text != null && !this.text.matcher(card.getRules().getOracleText()).find())
                return !this.shouldBeEqual;

            if (this.matchAllColors) {
                if (!card.getRules().getColor().hasAllColors(this.colors)) {
                    return !this.shouldBeEqual;
                }
            }

            if (this.colors != MagicColor.ALL_COLORS) {
                if (!card.getRules().getColor().hasNoColorsExcept(this.colors)
                        || (this.colors != MagicColor.COLORLESS && card.getRules().getColor().isColorless()))
                    return !this.shouldBeEqual;
            }
            if (colorType != ColorType.Any) {
                switch (colorType) {
                    case Colorless:
                        if (!card.getRules().getColor().isColorless())
                            return !this.shouldBeEqual;
                        break;
                    case MonoColor:
                        if (!card.getRules().getColor().isMonoColor())
                            return !this.shouldBeEqual;
                        break;
                    case MultiColor:
                        if (!card.getRules().getColor().isMulticolor())
                            return !this.shouldBeEqual;
                        break;
                }
            }
            if (!this.type.isEmpty()) {
                boolean found = false;
                for (CardType.CoreType type : card.getRules().getType().getCoreTypes()) {
                    if (this.type.contains(type)) {
                        found = true;
                        break;
                    }
                }
                if (!found)
                    return !this.shouldBeEqual;
            }
            if (!this.superType.isEmpty()) {
                boolean found = false;
                for (CardType.Supertype type : card.getRules().getType().getSupertypes()) {
                    if (this.superType.contains(type)) {
                        found = true;
                        break;
                    }
                }
                if (!found)
                    return !this.shouldBeEqual;
            }
            if (this.matchAllSubTypes) {
                if (!this.subType.isEmpty()) {
                    if (this.subType.size() != Iterables.size(card.getRules().getType().getSubtypes()))
                        return !this.shouldBeEqual;
                    for (String subtype : card.getRules().getType().getSubtypes()) {
                        if (!this.subType.contains(subtype)) {
                            return !this.shouldBeEqual;
                        }
                    }
                }
            } else {
                if (!this.subType.isEmpty()) {
                    boolean found = false;
                    for (String subtype : card.getRules().getType().getSubtypes()) {
                        if (this.subType.contains(subtype)) {
                            found = true;
                            break;
                        }
                    }
                    if (!found)
                        return !this.shouldBeEqual;
                }
            }

            if (!this.keyWords.isEmpty()) {
                boolean found = false;
                for (String keyWord : this.keyWords) {
                    if (card.getRules().hasKeyword(keyWord)) {
                        found = true;
                        break;
                    }
                }
                if (!found)
                    return !this.shouldBeEqual;
            }

            if (!this.deckNeeds.isEmpty()) {
                boolean found = false;
                for (String need : this.deckNeeds) {
                    // FormatExpected: X$Y, where X is DeckHints.Type and Y is a string descriptor
                    String[] parts = need.split("\\$");

                    if (parts.length != 2) {
                        continue;
                    }
                    DeckHints.Type t = DeckHints.Type.valueOf(parts[0].toUpperCase());

                    DeckHints hints = card.getRules().getAiHints().getDeckHints();
                    if (hints != null && hints.contains(t, parts[1])) {
                        found = true;
                        break;
                    }
                }
                if (!found)
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
            this.matchAllSubTypes = type.matchAllSubTypes;
            this.matchAllColors = type.matchAllColors;
            this.shouldBeEqual = wantEqual;
            for (int i = 0; type.manaCosts != null && i < type.manaCosts.length; i++)
                manaCosts.add(type.manaCosts[i]);
            text = getPattern(type);
            if (type.colors == null || type.colors.length == 0) {
                this.colors = MagicColor.ALL_COLORS;
            } else {
                this.colors = 0;
                for (String color : type.colors) {
                    if ("colorID".equals(color))
                        colors |= Current.player().getColorIdentity().getColor();
                    else
                        colors |= MagicColor.fromName(color.toLowerCase());
                }
            }
            if (type.keyWords != null && type.keyWords.length != 0) {
                keyWords.addAll(Arrays.asList(type.keyWords));
            }
            if (type.rarity != null) {
                for (String rarity : type.rarity) {
                    rarities.add(CardRarity.smartValueOf(rarity));
                }
            }

            if (type.subTypes != null && type.subTypes.length != 0) {
                subType.addAll(Arrays.asList(type.subTypes));
            }
            if (type.editions != null && type.editions.length != 0) {
                editions.addAll(Arrays.asList(type.editions));
            }
            if (type.superTypes != null) {
                for (String string : type.superTypes)
                    superType.add(CardType.Supertype.getEnum(string));
            }
            if (type.cardTypes != null) {
                for (String string : type.cardTypes)
                    this.type.add(CardType.CoreType.getEnum(string));
            }
            if (type.colorType != null && !type.colorType.isEmpty()) {
                this.colorType = ColorType.valueOf(type.colorType);
            } else {
                this.colorType = ColorType.Any;
            }
            if (type.deckNeeds != null && type.deckNeeds.length != 0) {
                deckNeeds.addAll(Arrays.asList(type.deckNeeds));
            }
            if (type.minDate != null && !type.minDate.isEmpty()) {
                this.minDate = type.minDate;
            } else {
                this.minDate = "";
            }
        }
    }

    public static List<PaperCard> getPredicateResult(Iterable<PaperCard> cards, final RewardData data) {
        List<PaperCard> result = new ArrayList<>();
        CardPredicate pre = new CardPredicate(data, true);

        for (final PaperCard item : cards) {
            if (pre.test(item))
                result.add(item);
        }
        return result;
    }

    public static List<PaperCard> generateCards(Iterable<PaperCard> cards, final RewardData data, final int count,
            Random r) {
        boolean allCardVariants = Config.instance().getSettingData().useAllCardVariants;

        final List<PaperCard> result = new ArrayList<>();
        List<PaperCard> pool = getPredicateResult(cards, data);
        if (!pool.isEmpty()) {
            for (int i = 0; i < count; i++) {
                PaperCard candidate = pool.get(r.nextInt(pool.size()));
                if (candidate != null) {
                    if (allCardVariants) {
                        // Get a random variant, preserving edition when specified
                        PaperCard finalCandidate = CardUtil.getCardByNameAndEdition(candidate.getCardName(), candidate.getEdition());
                        result.add(finalCandidate);
                    } else {
                        result.add(candidate);
                    }
                }
            }
        }
        return result;
    }
    private static AdventureReadPriceList.PriceData priceData;

    /**
     * Clear the cached price data. Call this when switching adventures/planes
     * so prices are reloaded from the new adventure's cardprices.txt.
     */
    public static void clearPriceCache() {
        priceData = null;
    }

    private static AdventureReadPriceList.PriceData getPriceData() {
        if (priceData == null) {
            priceData = AdventureReadPriceList.loadPrices();
        }
        return priceData;
    }

    /**
     * Returns the price mode for the current adventure's price list.
     * FORCED means custom prices are always active (toggle disabled).
     * OPTIONAL means the player controls it via the settings toggle.
     */
    public static AdventureReadPriceList.PriceMode getPriceMode() {
        return getPriceData().mode;
    }

    public static int getCardPrice(PaperCard card) {
        if (card == null)
            return 0;
        CardRarity effectiveRarity = card.getRarity();

        if (card.getRarity() == CardRarity.BasicLand
                && !card.isVeryBasicLand()
                && !MagicColor.Constant.SNOW_LANDS.contains(card.getName())
                && !card.getName().equals("Wastes")) {
            effectiveRarity = CardRarity.Common; // adjust for lands which are L rarity but are not basic lands
        }
        AdventureReadPriceList.PriceData data = getPriceData();
        boolean useCustomPrices = data.mode == AdventureReadPriceList.PriceMode.FORCED
                || Config.instance().getConfigData().usePriceListPrices;

        if (useCustomPrices) {
            Integer price = data.prices.get(card.getName());
            if (price != null) {
                return price;
            }
        }    
        return switch (effectiveRarity) {
            case BasicLand -> 5;
            case Common -> 50;
            case Uncommon -> 150;
            case Rare -> 300;
            case MythicRare -> 500;
            default -> 600;
        };
    }
    public static int getBoosterPrice(Deck booster) {
    	if (booster == null)
            return 0;
    	String editionCode = booster.getComment();
        AdventureReadPriceList.PriceData data = getPriceData();
        boolean useCustomPrices = data.mode == AdventureReadPriceList.PriceMode.FORCED
                || Config.instance().getConfigData().usePriceListPrices;

        if (useCustomPrices) {
            Integer price = data.prices.get(editionCode);
            if (price != null) {
                return price;
            }
        }
        return 1000;
     }
    
    public static int getRewardPrice(Reward reward) {
        PaperCard card = reward.getCard();
        Deck booster = reward.getDeck();

        if (card != null)
            return getCardPrice(card);
        if (reward.getItem() != null)
            return reward.getItem().cost;
        if (reward.getType() == Reward.Type.Life)
            return reward.getCount() * 500;
        if (reward.getType() == Reward.Type.Shards)
            return reward.getCount() * 500;
        if (reward.getType() == Reward.Type.Gold)
            return reward.getCount();
		if(reward.getType() == Reward.Type.CardPack)                // TODO: Heitor - Price by card count and type of boosterPack.
         return getBoosterPrice(booster);
		
        return 1000;
    }

    public static Deck generateDeck(GeneratedDeckData data, CardEdition starterEdition, boolean discourageDuplicates) {
        List<String> editionCodes = (starterEdition != null)
                ? Arrays.asList(starterEdition.getCode(), starterEdition.getCode2())
                : Arrays.asList("JMP", "J22", "DMU", "BRO", "ONE", "MOM");
        Deck deck = new Deck(data.name);
        if (data.mainDeck != null) {
            deck.getOrCreate(DeckSection.Main).addAllFlat(generateAllCards(Arrays.asList(data.mainDeck), true));
            if (data.sideBoard != null)
                deck.getOrCreate(DeckSection.Sideboard)
                        .addAllFlat(generateAllCards(Arrays.asList(data.sideBoard), true));
            return deck;
        }
        if (data.jumpstartPacks != null) {
            deck.getOrCreate(DeckSection.Main);

            Map<String, List<PaperCard>> packCandidates = null;
            List<String> usedPackNames = new ArrayList<String>();

            for (int i = 0; i < data.jumpstartPacks.length; i++) {
                final byte targetColor = MagicColor.fromName(data.jumpstartPacks[i]);
                String targetName = switch (targetColor) {
                    case MagicColor.BLUE -> "Island";
                    case MagicColor.BLACK -> "Swamp";
                    case MagicColor.RED -> "Mountain";
                    case MagicColor.GREEN -> "Forest";
                    default -> "Plains";
                };

                packCandidates = new HashMap<>();
                for (SealedTemplate template : StaticData.instance().getSpecialBoosters()) {
                    if (!editionCodes.contains(template.getEdition().split("\\s", 2)[0]))
                        continue;
                    List<PaperCard> packContents = new UnOpenedProduct(template).get();
                    if (packContents.size() < 18 | packContents.size() > 25)
                        continue;
                    if (packContents.stream().filter(x -> x.getName().equals(targetName)).count() >= 3)
                        packCandidates.putIfAbsent(template.getEdition(), packContents);
                }
                List<PaperCard> selectedPack;
                if (discourageDuplicates) {
                    Map<String, List<PaperCard>> filteredPackCandidates = new HashMap<>();
                    for (java.util.Map.Entry<String, List<PaperCard>> entry : packCandidates.entrySet()) {
                        if (!usedPackNames.contains(entry.getKey())) {
                            // Deep copy so that packCandidates can be used if filtered ends up being empty
                            filteredPackCandidates.put(entry.getKey(), entry.getValue());
                        }
                    }
                    // Only re-use a pack if all possibilities have already been chosen
                    if (filteredPackCandidates.isEmpty())
                        filteredPackCandidates = packCandidates;
                    Object[] keys = filteredPackCandidates.keySet().toArray();

                    String keyName = (String) keys[Current.world().getRandom().nextInt(keys.length)];
                    usedPackNames.add(keyName);
                    selectedPack = filteredPackCandidates.remove(keyName);
                } else {
                    Object[] keys = packCandidates.keySet().toArray();
                    selectedPack = packCandidates.get((String) keys[Current.world().getRandom().nextInt(keys.length)]);
                }
                // If the packContents size above is below 20, add random cards
                int size = 20 - selectedPack.size();
                for (int c = 0; c < size; c++) {
                    selectedPack.add(Aggregates.random(selectedPack));
                }
                deck.getOrCreate(DeckSection.Main).addAllFlat(selectedPack);
            }
            return deck;
        }
        if (data.template != null) {
            float count = data.template.count;
            float lands = count * 0.4f;
            float spells = count - lands;
            List<RewardData> dataArray = generateRewards(data.template, spells * 0.5f, new int[] { 1, 2 });
            dataArray.addAll(generateRewards(data.template, spells * 0.3f, new int[] { 3, 4, 5 }));
            dataArray.addAll(generateRewards(data.template, spells * 0.2f, new int[] { 6, 7, 8 }));
            List<PaperCard> nonLand = generateAllCards(dataArray, true);

            nonLand.addAll(fillWithLands(nonLand, data.template));
            deck.getOrCreate(DeckSection.Main).addAllFlat(nonLand);
        }
        return deck;
    }

    private static List<PaperCard> fillWithLands(List<PaperCard> nonLands, GeneratedDeckTemplateData template) {
        int red = 0, blue = 0, green = 0, white = 0, black = 0, colorless = 0;
        int cardCount = nonLands.size();
        List<PaperCard> cards = new ArrayList<>();
        boolean allCardVariants = Config.instance().getSettingData().useAllCardVariants;
        boolean useSnowLands = false;

        for (PaperCard nonLand : nonLands) {
            CardRules rules = nonLand.getRules();
            ManaCost manaCost = rules.getManaCost();

            red += manaCost.getShardCount(ManaCostShard.RED);
            green += manaCost.getShardCount(ManaCostShard.GREEN);
            white += manaCost.getShardCount(ManaCostShard.WHITE);
            blue += manaCost.getShardCount(ManaCostShard.BLUE);
            black += manaCost.getShardCount(ManaCostShard.BLACK);
            colorless += manaCost.getShardCount(ManaCostShard.COLORLESS);

            // Check for Snow lands requirement
            if (!useSnowLands) {
                if (manaCost.getShardCount(ManaCostShard.S) > 0) {
                    useSnowLands = true;
                    continue;
                }

                if (rules.getAiHints() != null && rules.getAiHints().getDeckHints() != null) {
                    useSnowLands = rules.getAiHints().getDeckHints().contains(Type.TYPE, "Snow");
                }
            }
        }

        float sumColoredCost = red + blue + green + white + black;
        int neededLands = template.count - cardCount;
        int neededDualLands = Math.round(neededLands * template.rares);
        int neededBase = neededLands - neededDualLands;
        String edition = "";

        if (allCardVariants) {
            PaperCard templateLand = CardUtil.getCardByName("Plains");
            edition = templateLand.getEdition();
        }

        if (sumColoredCost == 0) {
            cards.addAll(generateLands("Wastes", neededLands));
        } else {
            float sumTotalCost = sumColoredCost + colorless;

            int mountain = Math.round(neededBase * (red / sumTotalCost));
            int island = Math.round(neededBase * (blue / sumTotalCost));
            int forest = Math.round(neededBase * (green / sumTotalCost));
            int plains = Math.round(neededBase * (white / sumTotalCost));
            int swamp = Math.round(neededBase * (black / sumTotalCost));
            int wastes = Math.round(neededBase * (colorless / sumTotalCost));

            cards.addAll(generateLands(useSnowLands ? "Snow-Covered Plains" : "Plains", plains, edition));
            cards.addAll(generateLands(useSnowLands ? "Snow-Covered Island" : "Island", island, edition));
            cards.addAll(generateLands(useSnowLands ? "Snow-Covered Forest" : "Forest", forest, edition));
            cards.addAll(generateLands(useSnowLands ? "Snow-Covered Mountain" : "Mountain", mountain, edition));
            cards.addAll(generateLands(useSnowLands ? "Snow-Covered Swamp" : "Swamp", swamp, edition));
            cards.addAll(generateLands(useSnowLands ? "Snow-Covered Wastes" : "Wastes", wastes, edition));

            List<String> landTypes = new ArrayList<>();
            if (mountain > 0)
                landTypes.add("Mountain");
            if (island > 0)
                landTypes.add("Island");
            if (plains > 0)
                landTypes.add("Plains");
            if (swamp > 0)
                landTypes.add("Swamp");
            if (forest > 0)
                landTypes.add("Forest");

            cards.addAll(generateDualLands(landTypes, neededDualLands));
        }

        return cards;
    }

    private static Collection<PaperCard> generateDualLands(List<String> landName, int count) {
        ArrayList<RewardData> rewards = new ArrayList<>();
        RewardData base = new RewardData();
        rewards.add(base);
        base.cardTypes = new String[] { "Land" };
        base.count = count;
        base.matchAllSubTypes = true;
        if (landName.size() == 1) {
            base.subTypes = new String[] { landName.get(0) };
        } else if (landName.size() == 2) {
            base.subTypes = new String[] { landName.get(0), landName.get(1) };
        } else if (landName.size() == 3) {
            RewardData sub1 = new RewardData(base);
            RewardData sub2 = new RewardData(base);
            sub1.count /= 3;
            sub2.count /= 3;
            base.count -= sub1.count;
            base.count -= sub2.count;

            base.subTypes = new String[] { landName.get(0), landName.get(1) };
            sub1.subTypes = new String[] { landName.get(1), landName.get(2) };
            sub2.subTypes = new String[] { landName.get(0), landName.get(2) };
            rewards.addAll(Arrays.asList(sub1, sub2));
        } else if (landName.size() == 4) {
            RewardData sub1 = new RewardData(base);
            RewardData sub2 = new RewardData(base);
            RewardData sub3 = new RewardData(base);
            RewardData sub4 = new RewardData(base);
            sub1.count /= 5;
            sub2.count /= 5;
            sub3.count /= 5;
            sub4.count /= 5;
            base.count -= sub1.count;
            base.count -= sub2.count;
            base.count -= sub3.count;
            base.count -= sub4.count;

            base.subTypes = new String[] { landName.get(0), landName.get(1) };
            sub1.subTypes = new String[] { landName.get(0), landName.get(2) };
            sub2.subTypes = new String[] { landName.get(0), landName.get(3) };
            sub3.subTypes = new String[] { landName.get(1), landName.get(2) };
            sub4.subTypes = new String[] { landName.get(1), landName.get(3) };
            rewards.addAll(Arrays.asList(sub1, sub2, sub3, sub4));
        } else if (landName.size() == 5) {
            RewardData sub1 = new RewardData(base);
            RewardData sub2 = new RewardData(base);
            RewardData sub3 = new RewardData(base);
            RewardData sub4 = new RewardData(base);
            RewardData sub5 = new RewardData(base);
            RewardData sub6 = new RewardData(base);
            RewardData sub7 = new RewardData(base);
            RewardData sub8 = new RewardData(base);
            RewardData sub9 = new RewardData(base);
            sub1.count /= 10;
            sub2.count /= 10;
            sub3.count /= 10;
            sub4.count /= 10;
            sub5.count /= 10;
            sub6.count /= 10;
            sub7.count /= 10;
            sub8.count /= 10;
            sub9.count /= 10;
            base.count -= sub1.count;
            base.count -= sub2.count;
            base.count -= sub3.count;
            base.count -= sub4.count;
            base.count -= sub5.count;
            base.count -= sub6.count;
            base.count -= sub7.count;
            base.count -= sub8.count;
            base.count -= sub9.count;

            base.subTypes = new String[] { landName.get(0), landName.get(1) };
            sub1.subTypes = new String[] { landName.get(0), landName.get(2) };
            sub2.subTypes = new String[] { landName.get(0), landName.get(3) };
            sub3.subTypes = new String[] { landName.get(0), landName.get(4) };
            sub4.subTypes = new String[] { landName.get(1), landName.get(2) };
            sub5.subTypes = new String[] { landName.get(1), landName.get(3) };
            sub6.subTypes = new String[] { landName.get(1), landName.get(4) };
            sub7.subTypes = new String[] { landName.get(2), landName.get(3) };
            sub8.subTypes = new String[] { landName.get(2), landName.get(4) };
            sub9.subTypes = new String[] { landName.get(3), landName.get(4) };
            rewards.addAll(Arrays.asList(sub1, sub2, sub3, sub4, sub5, sub6, sub7, sub8, sub9));
        }

        return new ArrayList<>(generateAllCards(rewards, true));
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
        ArrayList<RewardData> ret = new ArrayList<>();
        ret.addAll(templateGenerate(template, count - (count * template.rares), manaCosts,
                new String[] { "Uncommon", "Common" }));
        ret.addAll(
                templateGenerate(template, count * template.rares, manaCosts, new String[] { "Rare", "Mythic Rare" }));
        return ret;
    }

    private static ArrayList<RewardData> templateGenerate(GeneratedDeckTemplateData template, float count,
            int[] manaCosts, String[] strings) {
        ArrayList<RewardData> ret = new ArrayList<>();
        RewardData base = new RewardData();
        base.manaCosts = manaCosts;
        base.rarity = strings;
        base.colors = template.colors;
        if (template.tribe != null && !template.tribe.isEmpty()) {
            RewardData caresAbout = new RewardData(base);
            caresAbout.cardText = "\\b" + template.tribe + "\\b";
            caresAbout.count = Math.round(count * template.tribeSynergyCards);
            ret.add(caresAbout);

            base.subTypes = new String[] { template.tribe };
            base.count = Math.round(count * (1 - template.tribeSynergyCards));
        } else {
            base.count = Math.round(count);
        }
        ret.add(base);
        return ret;
    }

    public static Deck getDeck(String path, boolean forAI, boolean isFantasyMode, String colors, boolean isTheme,
            boolean useGeneticAI) {
        return getDeck(path, forAI, isFantasyMode, colors, isTheme, useGeneticAI, null, true);
    }

    public static Deck getDeck(String path, boolean forAI, boolean isFantasyMode, String colors, boolean isTheme,
            boolean useGeneticAI, CardEdition starterEdition, boolean discourageDuplicates) {
        if (path.endsWith(".dck")) {
            FileHandle fileHandle = Config.instance().getFile(path);
            Deck deck = null;
            if (fileHandle != null) {
                deck = DeckSerializer.fromFile(fileHandle.file());
            }
            if (deck == null) {
                deck = DeckgenUtil.getRandomOrPreconOrThemeDeck(colors, true, false, true);
                System.err.println("Error loading Deck: " + path + "\nGenerating random deck: " + deck.getName());
            }
            return deck;
        }

        if (forAI && (isFantasyMode || useGeneticAI)) {
            if (isFantasyMode && "Commander".equalsIgnoreCase(Config.instance().getConfigData().chaosDeckFormat)) {
                return DeckgenUtil.generateCommanderDeck(true, GameType.Commander);
            }
            return DeckgenUtil.getRandomOrPreconOrThemeDeck(colors, forAI, isTheme, useGeneticAI);
        }

        Json json = new Json();
        FileHandle handle = Config.instance().getFile(path);
        if (handle.exists())
            return generateDeck(json.fromJson(GeneratedDeckData.class, handle), starterEdition, discourageDuplicates);
        Deck deck = DeckgenUtil.getRandomOrPreconOrThemeDeck(colors, true, false, true);
        System.err.println("Error loading JSON: " + handle.path() + "\nGenerating random deck: " + deck.getName());
        return deck;
    }

    private static final GameFormat.Collection formats = FModel.getFormats();

    private static final Predicate<CardEdition> filterPioneer = formats.getPioneer().editionLegalPredicate;
    private static final Predicate<CardEdition> filterModern = formats.getModern().editionLegalPredicate;
    private static final Predicate<CardEdition> filterVintage = formats.getVintage().editionLegalPredicate;
    private static final Predicate<CardEdition> filterStandard = formats.getStandard().editionLegalPredicate;

    public static Deck generateStandardBoosterAsDeck() {
        return generateRandomBoosterPackAsDeck(filterStandard);
    }

    public static Deck generatePioneerBoosterAsDeck() {
        return generateRandomBoosterPackAsDeck(filterPioneer);
    }

    public static Deck generateModernBoosterAsDeck() {
        return generateRandomBoosterPackAsDeck(filterModern);
    }

    public static Deck generateVintageBoosterAsDeck() {
        return generateRandomBoosterPackAsDeck(filterVintage);
    }

    public static Deck generateBoosterPackAsDeck(String code) {
        ConfigData configData = Config.instance().getConfigData();
        if (configData.allowedEditions != null) {
            if (!Arrays.asList(configData.allowedEditions).contains(code)) {
                System.err.println("Cannot generate booster pack, '" + code + "' is not an allowed edition");
            }
        } else if (Arrays.asList(configData.restrictedEditions).contains(code)) {
            System.err.println("Cannot generate booster pack, '" + code + "' is a restricted edition");
        }

        CardEdition edition = StaticData.instance().getEditions().get(code);
        if (edition == null) {
            System.err.println("Set code '" + code + "' not found.");
            return new Deck();
        }
        BoosterPack cards = BoosterPack.fromSet(edition);
        return generateBoosterPackAsDeck(edition);
    }

    public static Deck generateBoosterPackAsDeck(CardEdition edition) {
        Deck d = new Deck("Booster pack");
        d.setComment(edition.getCode());
        d.getMain().add(BoosterPack.fromSet(edition).getCards());
        return d;
    }

    public static Deck generateRandomBoosterPackAsDeck(final Predicate<CardEdition> editionFilter) {
        Predicate<CardEdition> filter = CardEdition.Predicates.CAN_MAKE_BOOSTER.and(editionFilter);
        Iterable<CardEdition> possibleEditions = IterableUtil.filter(FModel.getMagicDb().getEditions(), filter);

        if (!possibleEditions.iterator().hasNext()) {
            System.err.println("No sets found matching edition filter that can create boosters.");
            return null;
        }

        CardEdition edition = Aggregates.random(possibleEditions);
        return generateBoosterPackAsDeck(edition);
    }

    private static PaperCard getReplacement(String missingCard, String replacementCard) {
        System.err.println(missingCard + " : Not found in the database.\nReplacement card: " + replacementCard);
        return FModel.getMagicDb().getCommonCards().getCard(replacementCard);
    }

    public static PaperCard getCardByName(String cardName) {
        List<PaperCard> validCards;
        ConfigData configData = Config.instance().getConfigData();
        if (Config.instance().getSettingData().useAllCardVariants) {
            Predicate<PaperCard> editionFilter;
            if (configData.allowedEditions != null && configData.allowedEditions.length > 0) {
                Set<String> allowed = new HashSet<>(Arrays.asList(configData.allowedEditions));
                editionFilter = card -> allowed.contains(card.getEdition());
            } else {
                editionFilter = card -> (!Arrays.asList(configData.restrictedEditions).contains(card.getEdition()));
            }
            Predicate<PaperCard> combined_predicate = editionFilter;
            if (Config.instance().getSettingData().excludeAlchemyVariants) {
                combined_predicate = editionFilter.and(PaperCardPredicates.IS_REBALANCED.negate());
            }
            validCards = FModel.getMagicDb().getCommonCards().getAllCardsNoAlt(cardName, combined_predicate);
        } else {
            validCards = List.of(FModel.getMagicDb().getCommonCards().getUniqueByNameNoAlt(cardName));
            // Filter to allowed editions to prevent showing printings from wrong sets.
            if (configData.allowedEditions != null && configData.allowedEditions.length > 0) {
                Set<String> allowed = new HashSet<>(Arrays.asList(configData.allowedEditions));
                validCards = validCards.stream()
                    .filter(card -> allowed.contains(card.getEdition()))
                    .collect(Collectors.toList());
                if (validCards.isEmpty()) {
                    // Card was from a non-allowed edition, find any printing from an allowed one.
                    validCards = FModel.getMagicDb().getCommonCards().getAllCardsNoAlt(cardName).stream()
                        .filter(card -> allowed.contains(card.getEdition()))
                        .collect(Collectors.toList());
                }
            }
        }
        if (validCards.isEmpty()) {
            return getReplacement(cardName, "Wastes");
        }

        return validCards.get(Current.world().getRandom().nextInt(validCards.size()));
    }

    public static PaperCard getCardByNameAndEdition(String cardName, String edition) {
        ConfigData configData = Config.instance().getConfigData();
        if (configData.allowedEditions != null && configData.allowedEditions.length > 0) {
            if (!Arrays.asList(configData.allowedEditions).contains(edition)) {
                return getCardByName(cardName);
            }
        } else if (configData.restrictedEditions != null && configData.restrictedEditions.length > 0) {
            if (Arrays.asList(configData.restrictedEditions).contains(edition)) {
                return getCardByName(cardName);
            }
        }
        List<PaperCard> cardPool = Config.instance().getSettingData().useAllCardVariants
                ? FModel.getMagicDb().getCommonCards().getAllCardsNoAlt(cardName)
                : List.of(FModel.getMagicDb().getCommonCards().getUniqueByNameNoAlt(cardName));
        List<PaperCard> validCards = cardPool.stream()
                .filter(input -> input.getEdition().equals(edition)).collect(Collectors.toList());

        if (validCards.isEmpty()) {
            System.err.println("Unexpected behavior: tried to call getCardByNameAndEdition for card " + cardName
                    + " from the edition " + edition
                    + ", but didn't find it in the DB. A random existing instance will be returned if found.");
            return getCardByName(cardName);
        }

        return validCards.get(Current.world().getRandom().nextInt(validCards.size()));
    }

    public static Collection<PaperCard> getFullCardPool(boolean allCardVariants) {
        ConfigData configData = Config.instance().getConfigData();
        if (configData.allowedEditions != null && configData.allowedEditions.length > 0) {
            Set<String> allowed = new HashSet<>(Arrays.asList(configData.allowedEditions));
            if (allCardVariants) {
                return FModel.getMagicDb().getCommonCards().getAllCards().stream()
                    .filter(card -> allowed.contains(card.getEdition()))
                    .collect(Collectors.toList());
            }
            // For unique cards, replace non-allowed printings with allowed ones.
            List<PaperCard> filtered = new ArrayList<>();
            for (PaperCard card : FModel.getMagicDb().getCommonCards().getUniqueCards()) {
                if (card == null) continue;
                if (allowed.contains(card.getEdition())) {
                    filtered.add(card);
                } else {
                    for (PaperCard p : FModel.getMagicDb().getCommonCards().getAllCards(card)) {
                        if (allowed.contains(p.getEdition())) {
                            filtered.add(p);
                            break;
                        }
                    }
                }
            }
            return filtered;
        }
        return allCardVariants
            ? FModel.getMagicDb().getCommonCards().getAllCards()
            : FModel.getMagicDb().getCommonCards().getUniqueCards();
    }

    /**
     * Replace all cards in a CardPool that are from non-allowed editions
     * with printings from allowed editions (if configured).
     * Returns the number of cards replaced.
     */
    public static int sanitizeCardPool(forge.deck.CardPool pool) {
        ConfigData configData = Config.instance().getConfigData();
        if (configData.allowedEditions == null || configData.allowedEditions.length == 0)
            return 0;
        Set<String> allowed = new HashSet<>(Arrays.asList(configData.allowedEditions));
        List<Map.Entry<PaperCard, Integer>> toReplace = new ArrayList<>();
        for (Map.Entry<PaperCard, Integer> entry : pool) {
            PaperCard card = entry.getKey();
            if (card != null && !allowed.contains(card.getEdition())) {
                toReplace.add(Map.entry(card, entry.getValue()));
            }
        }
        int replaced = 0;
        for (Map.Entry<PaperCard, Integer> entry : toReplace) {
            PaperCard original = entry.getKey();
            PaperCard replacement = ensureAllowedEdition(original);
            // Preserve noSell flag from the original card.
            if (original.hasNoSellValue() && !replacement.hasNoSellValue()) {
                replacement = replacement.getNoSellVersion();
            }
            int count = entry.getValue();
            pool.remove(original, count);
            pool.add(replacement, count);
            replaced += count;
        }
        return replaced;
    }

    public static PaperCard ensureAllowedEdition(PaperCard card) {
        if (card == null) return null;
        ConfigData configData = Config.instance().getConfigData();
        if (configData.allowedEditions == null || configData.allowedEditions.length == 0)
            return card;
        Set<String> allowed = new HashSet<>(Arrays.asList(configData.allowedEditions));
        if (allowed.contains(card.getEdition()))
            return card;
        for (PaperCard p : FModel.getMagicDb().getCommonCards().getAllCards(card)) {
            if (allowed.contains(p.getEdition()))
                return p;
        }
        return card;
    }

    // ===== Deck Import/Export Feature =====

    public enum ImportMode {
        GIVE_MISSING,
        BUY_MISSING,
        REPORT_ONLY
    }

    public static class MissingCard {
        public final PaperCard card;
        public final int quantity;
        public MissingCard(PaperCard card, int quantity) {
            this.card = card;
            this.quantity = quantity;
        }
    }

    public static class ImportResult {
        public final boolean success;
        public final String message;
        public final String deckName;
        public final int slot;
        public final int cardsAdded;
        public final List<MissingCard> missingCards;

        private ImportResult(boolean success, String message, String deckName, int slot, int cardsAdded, List<MissingCard> missingCards) {
            this.success = success;
            this.message = message;
            this.deckName = deckName;
            this.slot = slot;
            this.cardsAdded = cardsAdded;
            this.missingCards = missingCards != null ? missingCards : new ArrayList<>();
        }

        public static ImportResult success(int slot, String deckName, int cardsAdded) {
            return new ImportResult(true, null, deckName, slot, cardsAdded, null);
        }

        public static ImportResult failure(String message) {
            return new ImportResult(false, message, null, -1, 0, null);
        }

        public static ImportResult reportOnly(String deckName, List<MissingCard> missing) {
            return new ImportResult(true, null, deckName, -1, 0, missing);
        }

        public String formatMissingReport() {
            if (missingCards.isEmpty()) return "You own all cards in this deck!";
            StringBuilder sb = new StringBuilder();
            sb.append("Missing cards for \"").append(deckName != null ? deckName : "Unknown").append("\":\n");
            for (MissingCard mc : missingCards) {
                sb.append(mc.quantity).append("x ").append(mc.card.getName()).append('\n');
            }
            sb.append("(").append(missingCards.size()).append(" unique cards missing)");
            return sb.toString();
        }
    }

    public static class MarkSellResult {
        public final boolean success;
        public final String message;
        public final int marked;
        public final int skipped;

        private MarkSellResult(boolean success, String message, int marked, int skipped) {
            this.success = success;
            this.message = message;
            this.marked = marked;
            this.skipped = skipped;
        }

        public static MarkSellResult success(int marked, int skipped) {
            return new MarkSellResult(true, null, marked, skipped);
        }

        public static MarkSellResult failure(String message) {
            return new MarkSellResult(false, message, 0, 0);
        }
    }

    public static File resolveFilePath(String rawPath) {
        String normalized = rawPath.replace('\\', File.separatorChar)
                                   .replace('/', File.separatorChar);
        if (normalized.startsWith("~" + File.separator) || normalized.equals("~")) {
            normalized = System.getProperty("user.home") + normalized.substring(1);
        }
        File file = new File(normalized);
        if (!file.isAbsolute()) {
            file = new File(System.getProperty("user.home"), normalized);
        }
        return file;
    }

    public static Deck parseDeckFile(File file) {
        if (!file.exists()) return null;
        List<String> lines = FileUtil.readFile(file);
        if (lines == null || lines.isEmpty()) return null;

        boolean isNativeDck = lines.stream().anyMatch(l ->
            l.trim().equals("[metadata]") || l.trim().equals("[Main]"));

        if (isNativeDck) {
            return DeckSerializer.fromFile(file);
        }

        DeckRecognizer recognizer = new DeckRecognizer();
        List<DeckRecognizer.Token> tokens = recognizer.parseCardList(
            lines.toArray(new String[0]));

        Deck deck = new Deck();
        for (DeckRecognizer.Token t : tokens) {
            if (t.getType() == DeckRecognizer.TokenType.DECK_NAME) {
                deck.setName(t.getText());
            } else if (t.isTokenForDeck()) {
                deck.getOrCreate(t.getTokenSection()).add(t.getCard(), t.getQuantity());
            }
        }

        if (deck.getName() == null || deck.getName().isEmpty()) {
            String fname = file.getName();
            int dot = fname.lastIndexOf('.');
            deck.setName(dot > 0 ? fname.substring(0, dot) : fname);
        }
        return deck;
    }

    public static ImportResult importDeckFromFile(File file, AdventurePlayer player, ImportMode mode) {
        Deck importedDeck = parseDeckFile(file);
        if (importedDeck == null) return ImportResult.failure("Could not parse file");

        List<MissingCard> missingCards = new ArrayList<>();
        for (java.util.Map.Entry<DeckSection, CardPool> entry : importedDeck) {
            for (java.util.Map.Entry<PaperCard, Integer> cardEntry : entry.getValue()) {
                PaperCard card = cardEntry.getKey();
                int needed = cardEntry.getValue();
                int owned = player.getCards().count(card);
                if (owned < needed) {
                    missingCards.add(new MissingCard(card, needed - owned));
                }
            }
        }

        if (mode == ImportMode.REPORT_ONLY) {
            return ImportResult.reportOnly(importedDeck.getName(), missingCards);
        }

        if (mode == ImportMode.BUY_MISSING) {
            int totalCost = 0;
            for (MissingCard mc : missingCards) {
                totalCost += player.cardSellPrice(mc.card) * mc.quantity;
            }
            if (player.getGold() < totalCost) {
                return ImportResult.failure(String.format(
                    "Not enough gold. Need %d, have %d. Missing %d cards.",
                    totalCost, player.getGold(), missingCards.size()));
            }
            player.giveGold(-totalCost);
        }

        int cardsAdded = 0;
        for (MissingCard mc : missingCards) {
            player.addCard(mc.card, mc.quantity);
            cardsAdded += mc.quantity;
        }

        int slot = player.findFirstEmptySlot();
        if (slot == -1) return ImportResult.failure("All deck slots full (max 99)");
        player.importIntoSlot(slot, importedDeck);

        return ImportResult.success(slot, importedDeck.getName(), cardsAdded);
    }

    public static MarkSellResult markCardsForSale(File file, AdventurePlayer player) {
        Deck parsed = parseDeckFile(file);
        if (parsed == null) return MarkSellResult.failure("Could not parse file");

        int marked = 0;
        int skipped = 0;
        CardPool collection = player.getCards();
        ItemPool<PaperCard> autoSell = player.getAutoSellCards();

        for (java.util.Map.Entry<DeckSection, CardPool> entry : parsed) {
            for (java.util.Map.Entry<PaperCard, Integer> cardEntry : entry.getValue()) {
                PaperCard card = cardEntry.getKey();
                int requestedQty = cardEntry.getValue();

                int owned = collection.count(card);
                int inDecks = player.getCopiesUsedInDecks(card);
                int alreadyMarked = autoSell.count(card);
                int canMark = owned - inDecks - alreadyMarked;

                if (card.getMarkedFlags() != null && card.getMarkedFlags().noSellValue) {
                    skipped += requestedQty;
                    continue;
                }

                int toMark = Math.min(requestedQty, Math.max(canMark, 0));
                if (toMark > 0) {
                    autoSell.add(card, toMark);
                    marked += toMark;
                }
                skipped += (requestedQty - toMark);
            }
        }

        return MarkSellResult.success(marked, skipped);
    }

    public static String exportCollectionAsArena(AdventurePlayer player) {
        StringBuilder sb = new StringBuilder();
        CardPool collection = player.getCards();
        List<java.util.Map.Entry<PaperCard, Integer>> sorted = new ArrayList<>();
        for (java.util.Map.Entry<PaperCard, Integer> e : collection) sorted.add(e);
        sorted.sort(java.util.Comparator.comparing(e -> e.getKey().getName()));
        for (java.util.Map.Entry<PaperCard, Integer> entry : sorted) {
            PaperCard card = entry.getKey();
            int count = entry.getValue();
            String set = card.getEdition();
            String collNum = card.getCollectorNumber();
            sb.append(count).append(' ').append(card.getName());
            if (set != null && !set.isEmpty()) {
                sb.append(" (").append(set).append(')');
                if (collNum != null && !collNum.isEmpty()) {
                    sb.append(' ').append(collNum);
                }
            }
            sb.append('\n');
        }
        return sb.toString();
    }
}
