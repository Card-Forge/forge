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
        List<CardEdition> out = planes.stream()
                .<CardEdition>mapMulti((p, c) -> p.getEditions().forEach(c))
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

    public enum AEtherFilter implements IHasSkinProp {
        C (null, new ColorFilter(MagicColor.COLORLESS), "Playable in {C}"),
        W (null, new ColorFilter(MagicColor.WHITE), "Playable in {W}"),
        U (null, new ColorFilter(MagicColor.BLUE), "Playable in {U}"),
        B (null, new ColorFilter(MagicColor.BLACK), "Playable in {B}"),
        R (null, new ColorFilter(MagicColor.RED), "Playable in {R}"),
        G (null, new ColorFilter(MagicColor.GREEN), "Playable in {G}"),

        WU (null, new ColorFilter(MagicColor.WHITE | MagicColor.BLUE), "Playable in {W}{U}"),
        WB (null, new ColorFilter(MagicColor.WHITE | MagicColor.BLACK), "Playable in {W}{B}"),
        UB (null, new ColorFilter(MagicColor.BLUE | MagicColor.BLACK), "Playable in {U}{B}"),
        UR (null, new ColorFilter(MagicColor.BLUE | MagicColor.RED), "Playable in {U}{R}"),
        BR (null, new ColorFilter(MagicColor.BLACK | MagicColor.RED), "Playable in {B}{R}"),
        BG (null, new ColorFilter(MagicColor.BLACK | MagicColor.GREEN), "Playable in {B}{G}"),
        RG (null, new ColorFilter(MagicColor.RED | MagicColor.GREEN), "Playable in {R}{G}"),
        RW (null, new ColorFilter(MagicColor.RED | MagicColor.WHITE), "Playable in {R}{W}"),
        GW (null, new ColorFilter(MagicColor.GREEN | MagicColor.WHITE), "Playable in {G}{W}"),
        GU (null, new ColorFilter(MagicColor.GREEN | MagicColor.BLUE), "Playable in {G}{U}"),

        WUB (null, new ColorFilter(MagicColor.WHITE | MagicColor.BLUE | MagicColor.BLACK), "Playable in {W}{U}{B}"),
        WBG (null, new ColorFilter(MagicColor.WHITE | MagicColor.BLACK | MagicColor.GREEN), "Playable in {W}{B}{G}"),
        UBR (null, new ColorFilter(MagicColor.BLUE | MagicColor.BLACK | MagicColor.RED), "Playable in {U}{B}{R}"),
        URW (null, new ColorFilter(MagicColor.BLUE | MagicColor.RED | MagicColor.WHITE), "Playable in {U}{R}{W}"),
        BRG (null, new ColorFilter(MagicColor.BLACK | MagicColor.RED | MagicColor.GREEN), "Playable in {B}{R}{G}"),
        BGU (null, new ColorFilter(MagicColor.BLACK | MagicColor.GREEN | MagicColor.BLUE), "Playable in {B}{G}{U}"),
        RGW (null, new ColorFilter(MagicColor.RED | MagicColor.GREEN | MagicColor.WHITE), "Playable in {R}{G}{W}"),
        RWB (null, new ColorFilter(MagicColor.RED | MagicColor.WHITE | MagicColor.BLACK), "Playable in {R}{W}{B}"),
        GWU (null, new ColorFilter(MagicColor.GREEN | MagicColor.WHITE | MagicColor.BLUE), "Playable in {G}{W}{U}"),
        GUR (null, new ColorFilter(MagicColor.GREEN | MagicColor.BLUE | MagicColor.RED), "Playable in {G}{U}{R}"),

        WUBR (null, new ColorFilter(MagicColor.WHITE | MagicColor.BLUE | MagicColor.BLACK | MagicColor.RED), "Playable in {W}{U}{B}{R}"),
        WUBG (null, new ColorFilter(MagicColor.WHITE | MagicColor.BLUE | MagicColor.BLACK | MagicColor.GREEN), "Playable in {W}{U}{B}{G}"),
        WURG (null, new ColorFilter(MagicColor.WHITE | MagicColor.BLUE | MagicColor.RED | MagicColor.GREEN), "Playable in {W}{U}{R}{G}"),
        WBRG (null, new ColorFilter(MagicColor.WHITE | MagicColor.BLACK | MagicColor.RED | MagicColor.GREEN), "Playable in {W}{B}{R}{G}"),
        UBRG (null, new ColorFilter(MagicColor.BLUE | MagicColor.BLACK | MagicColor.RED | MagicColor.GREEN), "Playable in {U}{B}{R}{G}"),

        WUBRG     (null, new ColorFilter(MagicColor.ALL_COLORS), "Playable in {W}{U}{B}{R}{G}"),

        CREATURE              (FSkinProp.IMG_CREATURE, new TypeFilter(EnumSet.of(CoreType.Creature)), "Creature"),
        NONCREATURE_PERMANENT (FSkinProp.IMG_ENCHANTMENT, new TypeFilter(EnumSet.of(CoreType.Artifact, CoreType.Enchantment, CoreType.Planeswalker, CoreType.Land), EnumSet.of(CoreType.Creature)), "Noncreature Permanent"),
        INSTANT_SORCERY       (FSkinProp.IMG_SORCERY, new TypeFilter(EnumSet.of(CoreType.Instant, CoreType.Sorcery)), "Instant or Sorcery"),

        COMMON   (FSkinProp.IMG_PW_BADGE_COMMON, new RarityFilter(EnumSet.of(CardRarity.Common, CardRarity.Uncommon, CardRarity.Rare, CardRarity.Special, CardRarity.MythicRare)), "Common"),
        UNCOMMON (FSkinProp.IMG_PW_BADGE_UNCOMMON, new RarityFilter(EnumSet.of(CardRarity.Uncommon, CardRarity.Rare, CardRarity.Special, CardRarity.MythicRare)), "Uncommon"),
        RARE     (FSkinProp.IMG_PW_BADGE_RARE, new RarityFilter(EnumSet.of(CardRarity.Rare, CardRarity.Special, CardRarity.MythicRare)), "Rare"),
        MYTHIC   (FSkinProp.IMG_PW_BADGE_MYTHIC, new RarityFilter(EnumSet.of(CardRarity.MythicRare)), "Mythic Rare (100%)"),

        CMC_LOW      (FSkinProp.IMG_CMC_LOW, new CMCFilter(0, 3), "Mana Value 0-3"),
        CMC_LOW_MID  (FSkinProp.IMG_CMC_LOW_MID, new CMCFilter(2, 5), "Mana Value 2-5"),
        CMC_MID_HIGH (FSkinProp.IMG_CMC_MID_HIGH, new CMCFilter(4, 7), "Mana Value 4-7"),
        CMC_HIGH     (FSkinProp.IMG_CMC_HIGH, new CMCFilter(6, -1), "Mana Value 6+");

        private final FSkinProp skinProp;
        private final Predicate<PaperCard> predicate;
        private String caption;

        AEtherFilter(final FSkinProp skinProp0, final Predicate<PaperCard> predicate0, final String caption0) {
            skinProp = skinProp0;
            predicate = predicate0;
            caption = caption0;
        }

        @Override
        public FSkinProp getSkinProp() {
            return skinProp;
        }

        public Predicate<PaperCard> getPredicate() {
            return predicate;
        }

        public ColorSet getColor() {
            if (predicate instanceof ColorFilter) {
                return ((ColorFilter)predicate).color;
            }
            return null;
        }

        public CardRarity getRarity() {
            return getRarity(0d);
        }
        public CardRarity getRarity(double random) {
            if (predicate instanceof RarityFilter) {
                double total = 0d;
                CardRarity rarity = null;
                Map<CardRarity, Double> rarityOdds = ((RarityFilter)predicate).rarityOdds;
                for (final Entry<CardRarity, Double> entry : rarityOdds.entrySet()) {
                    rarity = entry.getKey();
                    total += entry.getValue();
                    if (random < total) {
                        return rarity;
                    }
                }
                return rarity;
            }
            return null;
        }

        @Override
        public String toString() {
            return caption;
        }
    }

    public static AEtherFilter getColorFilter(ColorSet color) {
        StringBuilder name = new StringBuilder();
        for (MagicColor.Color s : color.getOrderedColors()) {
            name.append(s.getShortName());
        }
        try {
            return AEtherFilter.valueOf(name.toString());
        }
        catch (Exception e) {
            System.err.println("No color filter with name " + name);
            return AEtherFilter.WUBRG; //return 5-color filter as fallback
        }
    }

    public static void updateRarityFilterOdds() {
        ConquestPreferences prefs = FModel.getConquestPreferences();

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

        for (AEtherFilter filter : RARITY_FILTERS) {
            filter.caption = ((RarityFilter)filter.predicate).updateOdds(odds);
        }
    }

    public static final AEtherFilter[] COLOR_FILTERS = new AEtherFilter[] {
        AEtherFilter.C,
        AEtherFilter.W,
        AEtherFilter.U,
        AEtherFilter.B,
        AEtherFilter.R,
        AEtherFilter.G,
        AEtherFilter.WU,
        AEtherFilter.WB,
        AEtherFilter.UB,
        AEtherFilter.UR,
        AEtherFilter.BR,
        AEtherFilter.BG,
        AEtherFilter.RG,
        AEtherFilter.RW,
        AEtherFilter.GW,
        AEtherFilter.GU,
        AEtherFilter.WUB,
        AEtherFilter.WBG,
        AEtherFilter.UBR,
        AEtherFilter.URW,
        AEtherFilter.BRG,
        AEtherFilter.BGU,
        AEtherFilter.RGW,
        AEtherFilter.RWB,
        AEtherFilter.GWU,
        AEtherFilter.GUR,
        AEtherFilter.WUBR,
        AEtherFilter.WUBG,
        AEtherFilter.WURG,
        AEtherFilter.WBRG,
        AEtherFilter.UBRG,
        AEtherFilter.WUBRG};

    public static final AEtherFilter[] TYPE_FILTERS = new AEtherFilter[] {
        AEtherFilter.CREATURE,
        AEtherFilter.NONCREATURE_PERMANENT,
        AEtherFilter.INSTANT_SORCERY };

    public static final AEtherFilter[] RARITY_FILTERS = new AEtherFilter[] {
        AEtherFilter.COMMON,
        AEtherFilter.UNCOMMON,
        AEtherFilter.RARE,
        AEtherFilter.MYTHIC };

    public static final AEtherFilter[] CMC_FILTERS = new AEtherFilter[] {
        AEtherFilter.CMC_LOW,
        AEtherFilter.CMC_LOW_MID,
        AEtherFilter.CMC_MID_HIGH,
        AEtherFilter.CMC_HIGH };

    private static class ColorFilter implements Predicate<PaperCard> {
        private final ColorSet color;

        private ColorFilter(int colorMask0) {
            color = ColorSet.fromMask(colorMask0);
        }

        @Override
        public boolean test(PaperCard card) {
            return card.getRules().getColorIdentity().hasNoColorsExcept(color);
        }
    }

    private static class TypeFilter implements Predicate<PaperCard> {
        private final Iterable<CoreType> types;
        private final Iterable<CoreType> nonTypes;

        private TypeFilter(Iterable<CoreType> types0) {
            types = types0;
            nonTypes = null;
        }

        private TypeFilter(Iterable<CoreType> types0, Iterable<CoreType> nonTypes0) {
            types = types0;
            nonTypes = nonTypes0;
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
    }

    private static class RarityFilter implements Predicate<PaperCard> {
        private final Map<CardRarity, Double> rarityOdds;

        private RarityFilter(Iterable<CardRarity> rarities0) {
            rarityOdds = Maps.newEnumMap(CardRarity.class);
            for (CardRarity rarity : rarities0) {
                rarityOdds.put(rarity, 0d); //values will be set later
            }
        }

        private String updateOdds(Map<CardRarity, Double> oddsLookup) {
            double baseOdds = 0;
            double remainingOdds = 1;
            CardRarity baseRarity = null;
            StringBuilder caption = new StringBuilder();

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
                    caption.append(", ").append(rarity.getLongName()).append(" (").append(display).append("%)");
                    rarityOdds.put(rarity, odds);
                }
            }

            //prepend base rarity and odds
            caption.insert(0, baseRarity.getLongName() + " (" + (Math.round(1000 * remainingOdds) / 10) + "%)");
            rarityOdds.put(baseRarity, remainingOdds);

            return caption.toString();
        }

        @Override
        public boolean test(PaperCard card) {
            return rarityOdds.containsKey(card.getRarity());
        }
    }

    private static class CMCFilter implements Predicate<PaperCard> {
        private final int cmcMin, cmcMax;

        private CMCFilter(int cmcMin0, int cmcMax0) {
            cmcMin = cmcMin0;
            cmcMax = cmcMax0;
        }

        @Override
        public boolean test(PaperCard card) {
            int cardCmc = card.getRules().getManaCost().getCMC();
            if (cardCmc < cmcMin) { return false; }
            return cmcMax == -1 || cardCmc <= cmcMax;
        }
    }
}
