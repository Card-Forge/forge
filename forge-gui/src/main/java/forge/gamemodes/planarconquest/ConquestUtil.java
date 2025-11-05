package forge.gamemodes.planarconquest;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import forge.card.*;
import forge.card.CardType.CoreType;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckFormat;
import forge.deck.DeckSection;
import forge.deck.generation.DeckGenerator2Color;
import forge.deck.generation.DeckGenerator3Color;
import forge.deck.generation.DeckGenerator5Color;
import forge.deck.generation.DeckGeneratorBase;
import forge.deck.generation.DeckGeneratorMonoColor;
import forge.deck.generation.IDeckGenPool;
import forge.gamemodes.planarconquest.ConquestPreferences.CQPref;
import forge.gamemodes.quest.QuestUtil;
import forge.gui.util.SOptionPane;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.skin.FSkinProp;
import forge.localinstance.skin.IHasSkinProp;
import forge.model.FModel;
import forge.util.*;

public class ConquestUtil {
    private ConquestUtil() {}

    public static Deck generateDeck(PaperCard commander, IDeckGenPool pool, boolean forAi) {
        ColorSet colorID = commander.getRules().getColorIdentity();

        List<String> colors = Lists.newArrayList();
        if (colorID.hasWhite()) { colors.add("White"); }
        if (colorID.hasBlue())  { colors.add("Blue"); }
        if (colorID.hasBlack()) { colors.add("Black"); }
        if (colorID.hasRed())   { colors.add("Red"); }
        if (colorID.hasGreen()) { colors.add("Green"); }

        DeckGeneratorBase gen;
        switch (colors.size()) {
        case 0:
            gen = new DeckGeneratorMonoColor(pool, DeckFormat.PlanarConquest, "");
            break;
        case 1:
            gen = new DeckGeneratorMonoColor(pool, DeckFormat.PlanarConquest, colors.get(0));
            break;
        case 2:
            gen = new DeckGenerator2Color(pool, DeckFormat.PlanarConquest, colors.get(0), colors.get(1));
            break;
        case 3:
            gen = new DeckGenerator3Color(pool, DeckFormat.PlanarConquest, colors.get(0), colors.get(1), colors.get(2));
            break;
        case 4:
            int colorSubset = MyRandom.getRandom().nextInt(4);
            switch (colorSubset) {
                case 0:
                    gen = new DeckGenerator3Color(pool, DeckFormat.PlanarConquest, colors.get(0), colors.get(1), colors.get(2));
                    break;
                case 1:
                    gen = new DeckGenerator3Color(pool, DeckFormat.PlanarConquest, colors.get(0), colors.get(1), colors.get(3));
                    break;
                case 2:
                    gen = new DeckGenerator3Color(pool, DeckFormat.PlanarConquest, colors.get(0), colors.get(2), colors.get(3));
                    break;
                case 3:
                    gen = new DeckGenerator3Color(pool, DeckFormat.PlanarConquest, colors.get(1), colors.get(2), colors.get(3));
                    break;
                default:
                    // This branch should never hit under the current setup, but this might change if the mechanism is changed
                    gen = new DeckGenerator3Color(pool, DeckFormat.PlanarConquest, colors.get(0), colors.get(1), colors.get(2));
                    break;
            }
            break;
        case 5:
            gen = new DeckGenerator5Color(pool, DeckFormat.PlanarConquest);
            break;
        default:
            return null; // Shouldn't happen
        }

        gen.setSingleton(true);
        CardPool cards = gen.getDeck(40, forAi);

        Deck deck = new Deck(commander.getName());
        deck.setDirectory("generated/conquest");
        deck.getMain().addAll(cards);
        deck.getOrCreate(DeckSection.Commander).add(commander);

        return deck;
    }

    public static String promptForName() {
        String name;
        while (true) {
            name = SOptionPane.showInputDialog(Localizer.getInstance().getMessage("lblHistoriiansWillRecallYourConquestAs"), Localizer.getInstance().getMessage("lblConquestName"));
            if (name == null) { return null; }

            name = QuestUtil.cleanString(name);

            if (name.isEmpty()) {
                SOptionPane.showMessageDialog(Localizer.getInstance().getMessage("lblPleaseSpecifyConquestName"));
                continue;
            }
            if (FileUtil.doesFileExist(ForgeConstants.CONQUEST_SAVE_DIR + name + ".dat")) {
                SOptionPane.showMessageDialog(Localizer.getInstance().getMessage("lblConquestNameExistsPleasePickAnotherName"));
                continue;
            }
            break;
        }
        return name;
    }

    public static CardPool getAvailablePool() {
        Set<PaperCard> availableCards = Sets.newHashSet();
        ConquestData model = FModel.getConquest().getModel();
        for (PaperCard pc : model.getUnlockedCards()) {
            availableCards.add(pc);
        }

        // Remove all exiled cards
        for (PaperCard pc : model.getExiledCards()) {
            availableCards.remove(pc);
        }

        // Create pool from available cards
        CardPool pool = new CardPool();
        pool.addAllFlat(availableCards);
        return pool;
    }

    public static List<CardEdition> getBasicLandSets(Deck currentDeck) {
        ConquestData model = FModel.getConquest().getModel();
        List<ConquestPlane> planes = new ArrayList<>(model.getUnlockedPlanes());
        ConquestPlane currentPlane = model.getCurrentPlane();
        //Move the current plane to the front.
        if(currentPlane != null && planes.contains(currentPlane)) {
            planes.remove(currentPlane);
            planes.add(0, currentPlane);
        }
        //Move editions of cards already in the deck to the front.
        Map<CardEdition, Integer> editionStats = currentDeck.getAllCardsInASinglePool().getCardEditionStatistics(true);
        // use flatMap instead of mapMulti for Android 13 and below
        //https://developer.android.com/reference/java/util/stream/Stream#mapMulti
        List<CardEdition> out = planes.stream()
            .flatMap(p -> p.getEditions().stream())
            .filter(CardEdition::hasBasicLands)
            .sorted(Comparator.comparing(e -> editionStats.getOrDefault(e, 0)))
            .collect(Collectors.toList());
        return out;
    }

    public static ConquestPlane getPlaneByName(String planeName) {
        for (ConquestPlane plane : FModel.getPlanes()) {
            if (plane.getName().equals(planeName)) {
                return plane;
            }
        }
        return null;
    }

    public static void setPlaneTemporarilyAccessible(String planeName, boolean accessible) {
        ConquestPlane plane = getPlaneByName(planeName);
        if (plane != null && accessible == plane.isUnreachable()) {
            plane.setTemporarilyReachable(accessible);
        } else {
            System.err.println("Could not find plane to set the accessibility flag: " + planeName);
        }
    }

    public static Iterable<PaperCard> getStartingPlaneswalkerOptions(final PaperCard startingCommander) {
        final byte colorIdentity = startingCommander.getRules().getColorIdentity().getColor();
        final Set<String> selected = Sets.newHashSet();
        // TODO: Could make this more efficient by streaming unique cards and then mapping them to an acceptable print if they aren't already...
        return IterableUtil.filter(FModel.getMagicDb().getCommonCards().getAllNonPromosNonReprintsNoAlt(), card -> {
            if (selected.contains(card.getName())) {
                return false;
            }
            CardRules rules = card.getRules();
            boolean allowed = rules.getType().isPlaneswalker() &&
                    !card.getName().equals(startingCommander.getName()) && //don't allow picking a commander as a starting planeswalker
                    rules.getColorIdentity().hasNoColorsExcept(colorIdentity);

            if (allowed) {
                selected.add(card.getName());
                return true;
            }

            return false;
        });
    }

    public static int getShardValue(PaperCard card, CQPref baseValuePref) {
        return getShardValue(card.getRarity(), baseValuePref);
    }
    public static int getShardValue(CardRarity rarity, CQPref baseValuePref) {
        ConquestPreferences prefs = FModel.getConquestPreferences();
        int baseValue = prefs.getPrefInt(baseValuePref);
        return switch (rarity) {
            case Common -> baseValue;
            case Uncommon ->
                    Math.round((float) baseValue * (float) prefs.getPrefInt(CQPref.AETHER_UNCOMMON_MULTIPLIER));
            case Rare, Special ->
                    Math.round((float) baseValue * (float) prefs.getPrefInt(CQPref.AETHER_RARE_MULTIPLIER));
            case MythicRare ->
                    Math.round((float) baseValue * (float) prefs.getPrefInt(CQPref.AETHER_MYTHIC_MULTIPLIER));
            default -> 0;
        };
    }

    public enum TypeFilter implements IHasSkinProp, Predicate<PaperCard>
    {
        CREATURE              (FSkinProp.IMG_CREATURE, EnumSet.of(CoreType.Creature), "Creature"),
        NONCREATURE_PERMANENT (FSkinProp.IMG_ENCHANTMENT, EnumSet.of(CoreType.Artifact, CoreType.Enchantment, CoreType.Planeswalker, CoreType.Land), EnumSet.of(CoreType.Creature), "Noncreature Permanent"),
        INSTANT_SORCERY       (FSkinProp.IMG_SORCERY, EnumSet.of(CoreType.Instant, CoreType.Sorcery), "Instant or Sorcery")
        ;
        private final FSkinProp skinProp;
        private final Iterable<CoreType> types;
        private final Iterable<CoreType> nonTypes;
        private final String caption;

        private TypeFilter(FSkinProp skinProp, Iterable<CoreType> types0, final String caption0) {
            this(skinProp, types0, null, caption0);
        }

        private TypeFilter(FSkinProp skinProp0, Iterable<CoreType> types0, Iterable<CoreType> nonTypes0, final String caption0) {
            skinProp = skinProp0;
            types = types0;
            nonTypes = nonTypes0;
            caption = caption0;
        }

        @Override
        public boolean test(PaperCard card) {
            CardType cardType = card.getRules().getType();
            if (nonTypes != null) {
                for (CoreType nonType : nonTypes) {
                    if (cardType.hasType(nonType)) {
                        return false;
                    }
                }
            }
            for (CoreType type : types) {
                if (cardType.hasType(type)) {
                    return true;
                }
            }
            return false;
        }
        @Override
        public FSkinProp getSkinProp() {
            return skinProp;
        }
        @Override
        public String toString() {
            return caption;
        }
    }


    public enum RarityFilter implements IHasSkinProp, Predicate<PaperCard>
    {
        COMMON   (FSkinProp.IMG_PW_BADGE_COMMON, EnumSet.of(CardRarity.Common, CardRarity.Uncommon, CardRarity.Rare, CardRarity.Special, CardRarity.MythicRare)),
        UNCOMMON (FSkinProp.IMG_PW_BADGE_UNCOMMON, EnumSet.of(CardRarity.Uncommon, CardRarity.Rare, CardRarity.Special, CardRarity.MythicRare)),
        RARE     (FSkinProp.IMG_PW_BADGE_RARE, EnumSet.of(CardRarity.Rare, CardRarity.Special, CardRarity.MythicRare)),
        MYTHIC   (FSkinProp.IMG_PW_BADGE_MYTHIC, EnumSet.of(CardRarity.MythicRare)),
        ;
        private final FSkinProp skinProp;
        private final Map<CardRarity, Double> rarityOdds;
        private String caption = "";

        private RarityFilter(FSkinProp skinProp0, Iterable<CardRarity> rarities0) {
            skinProp = skinProp0;
            rarityOdds = Maps.newEnumMap(CardRarity.class);
            for (CardRarity rarity : rarities0) {
                rarityOdds.put(rarity, 0d); //values will be set later
            }
        }

        public CardRarity getRarity() {
            return getRarity(0d);
        }
        public CardRarity getRarity(double random) {
            double total = 0d;
            CardRarity rarity = null;
            for (final Entry<CardRarity, Double> entry : rarityOdds.entrySet()) {
                rarity = entry.getKey();
                total += entry.getValue();
                if (random < total) {
                    return rarity;
                }
            }
            return rarity;
        }

        private void updateOdds(Map<CardRarity, Double> oddsLookup) {
            double baseOdds = 0;
            double remainingOdds = 1;
            CardRarity baseRarity = null;
            StringBuilder sb = new StringBuilder();

            for (CardRarity rarity : rarityOdds.keySet()) {
                Double odds = oddsLookup.get(rarity);
                if (odds == null) { continue; } //skip Special rarity

                if (baseRarity == null) {
                    baseRarity = rarity;
                    baseOdds = odds;
                }
                else {
                    odds /= baseOdds;
                    remainingOdds -= odds;
                    final double rounded = Math.round(1000 * odds) / 10d;
                    final String display = rounded < 1d
                            ? Double.toString(rounded) // Display decimal if < 1%
                            : Long.toString(Math.round(rounded));
                    sb.append(", ").append(rarity.getLongName()).append(" (").append(display).append("%)");
                    rarityOdds.put(rarity, odds);
                }
            }

            //prepend base rarity and odds
            sb.insert(0, baseRarity.getLongName() + " (" + (Math.round(1000 * remainingOdds) / 10) + "%)");
            rarityOdds.put(baseRarity, remainingOdds);

            caption = sb.toString();
        }

        @Override
        public boolean test(PaperCard card) {
            return rarityOdds.containsKey(card.getRarity());
        }
        @Override
        public FSkinProp getSkinProp() {
            return skinProp;
        }
        @Override
        public String toString() {
            return caption;
        }
    }

    public static void updateRarityFilterOdds(ConquestPreferences prefs) {
        Map<CardRarity, Double> odds = Maps.newEnumMap(CardRarity.class);
        if (prefs.getPrefBoolean(CQPref.AETHER_USE_DEFAULT_RARITY_ODDS)) {
            odds.put(CardRarity.Common, 1d);
            odds.put(CardRarity.Uncommon, 0.17);
            odds.put(CardRarity.Rare, 0.03);
            odds.put(CardRarity.MythicRare, 0.005);
        } else {
            double commonsPerBooster = prefs.getPrefInt(CQPref.BOOSTER_COMMONS);
            double uncommonPerBooster = prefs.getPrefInt(CQPref.BOOSTER_UNCOMMONS);
            double raresPerBooster = prefs.getPrefInt(CQPref.BOOSTER_RARES);
            double mythicsPerBooster = raresPerBooster / (double)prefs.getPrefInt(CQPref.BOOSTERS_PER_MYTHIC);

            odds.put(CardRarity.Common, 1d);
            odds.put(CardRarity.Uncommon, uncommonPerBooster / commonsPerBooster);
            odds.put(CardRarity.Rare, raresPerBooster / commonsPerBooster);
            odds.put(CardRarity.MythicRare, mythicsPerBooster / commonsPerBooster);
        }

        for (RarityFilter filter : RarityFilter.values()) {
            filter.updateOdds(odds);
        }
    }

    public enum CMCFilter implements IHasSkinProp, Predicate<PaperCard>
    {
        CMC_LOW      (FSkinProp.IMG_CMC_LOW, 0, 3),
        CMC_LOW_MID  (FSkinProp.IMG_CMC_LOW_MID, 2, 5),
        CMC_MID_HIGH (FSkinProp.IMG_CMC_MID_HIGH, 4, 7),
        CMC_HIGH     (FSkinProp.IMG_CMC_HIGH, 6, -1);

        private final FSkinProp skinProp;
        private final int cmcMin, cmcMax;

        private CMCFilter(FSkinProp skinProp0, int cmcMin0, int cmcMax0) {
            skinProp = skinProp0;
            cmcMin = cmcMin0;
            cmcMax = cmcMax0;
        }

        @Override
        public boolean test(PaperCard card) {
            int cardCmc = card.getRules().getManaCost().getCMC();
            if (cardCmc < cmcMin) { return false; }
            return cmcMax == -1 || cardCmc <= cmcMax;
        }
        @Override
        public FSkinProp getSkinProp() {
            return skinProp;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("Mana Value ");
            sb.append(cmcMin);
            sb.append(cmcMax == -1 ? "+" : "-" + cmcMax);
            return sb.toString();
        }
    }
}
