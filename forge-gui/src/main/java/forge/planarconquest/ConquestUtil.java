package forge.planarconquest;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import forge.assets.FSkinProp;
import forge.assets.IHasSkinProp;
import forge.card.CardRarity;
import forge.card.CardRules;
import forge.card.CardType;
import forge.card.CardType.CoreType;
import forge.card.mana.ManaCostShard;
import forge.card.ColorSet;
import forge.card.MagicColor;
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
import forge.item.PaperCard;
import forge.model.FModel;
import forge.planarconquest.ConquestPreferences.CQPref;
import forge.properties.ForgeConstants;
import forge.quest.QuestUtil;
import forge.util.FileUtil;
import forge.util.gui.SOptionPane;

public class ConquestUtil {
    private ConquestUtil() {}

    public static Deck generateDeck(PaperCard commander, IDeckGenPool pool, boolean forAi) {
        ColorSet colorID = commander.getRules().getColorIdentity();

        List<String> colors = new ArrayList<String>();
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
        case 5:
            gen = new DeckGenerator5Color(pool, DeckFormat.PlanarConquest);
            break;
        default:
            return null; //shouldn't happen
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
            name = SOptionPane.showInputDialog("Historians will recall your conquest as:", "Conquest Name");
            if (name == null) { return null; }
    
            name = QuestUtil.cleanString(name);
    
            if (name.isEmpty()) {
                SOptionPane.showMessageDialog("Please specify a conquest name.");
                continue;
            }
            if (FileUtil.doesFileExist(ForgeConstants.CONQUEST_SAVE_DIR + name + ".dat")) {
                SOptionPane.showMessageDialog("A conquest already exists with that name. Please pick another quest name.");
                continue;
            }
            break;
        }
        return name;
    }

    public static CardPool getAvailablePool(Deck deck) {
        HashSet<PaperCard> availableCards = new HashSet<PaperCard>();
        ConquestData model = FModel.getConquest().getModel();
        for (PaperCard pc : model.getUnlockedCards()) {
            availableCards.add(pc);
        }

        //remove all exiled cards
        for (PaperCard pc : model.getExiledCards()) {
            availableCards.remove(pc);
        }

        //remove all cards in main deck
        for (Entry<PaperCard, Integer> e : deck.getMain()) {
            availableCards.remove(e.getKey());
        }

        //remove commander
        PaperCard commander = deck.get(DeckSection.Commander).get(0);
        availableCards.remove(commander);

        //remove any cards that aren't allowed in deck due to color identity
        final ColorSet colorIdentity = commander.getRules().getColorIdentity();
        if (!colorIdentity.equals(ColorSet.ALL_COLORS)) {
            List<PaperCard> invalidCards = new ArrayList<PaperCard>();
            for (PaperCard pc : availableCards) {
                if (!pc.getRules().getColorIdentity().hasNoColorsExcept(colorIdentity)) {
                    invalidCards.add(pc);
                }
            }
            availableCards.removeAll(invalidCards);
        }

        //create pool from available cards
        CardPool pool = new CardPool();
        pool.addAllFlat(availableCards);
        return pool;
    }

    public static Iterable<PaperCard> getStartingPlaneswalkerOptions(PaperCard startingCommander) {
        final byte colorIdentity = startingCommander.getRules().getColorIdentity().getColor();
        return Iterables.filter(FModel.getMagicDb().getCommonCards().getUniqueCards(), new Predicate<PaperCard>() {
            @Override
            public boolean apply(PaperCard card) {
                CardRules rules = card.getRules();
                return rules.getType().isPlaneswalker() &&
                        !rules.canBeCommander() && //don't allow picking a commander as a starting planeswalker
                        rules.getColorIdentity().hasNoColorsExcept(colorIdentity);
            }
        });
    }

    public static int getShardValue(PaperCard card, CQPref baseValuePref) {
        return getShardValue(card.getRarity(), baseValuePref);
    }
    public static int getShardValue(CardRarity rarity, CQPref baseValuePref) {
        ConquestPreferences prefs = FModel.getConquestPreferences();
        int baseValue = prefs.getPrefInt(baseValuePref);
        switch (rarity) {
        case Common:
            return baseValue;
        case Uncommon:
            return Math.round((float)baseValue * (float)prefs.getPrefInt(CQPref.AETHER_UNCOMMON_MULTIPLIER));
        case Rare:
        case Special:
            return Math.round((float)baseValue * (float)prefs.getPrefInt(CQPref.AETHER_RARE_MULTIPLIER));
        case MythicRare:
            return Math.round((float)baseValue * (float)prefs.getPrefInt(CQPref.AETHER_MYTHIC_MULTIPLIER));
        default:
            return 0;
        }
    }

    public static enum AEtherFilter implements IHasSkinProp {
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

        WUBRG     (null, new ColorFilter(MagicColor.ALL_COLORS), "Playable in {W}{U}{B}{R}{G}"),

        CREATURE              (FSkinProp.IMG_CREATURE, new TypeFilter(EnumSet.of(CoreType.Creature)), "Creature"),
        NONCREATURE_PERMANENT (FSkinProp.IMG_ENCHANTMENT, new TypeFilter(EnumSet.of(CoreType.Artifact, CoreType.Enchantment, CoreType.Planeswalker, CoreType.Land)), "Noncreature Permanent"),
        INSTANT_SORCERY       (FSkinProp.IMG_SORCERY, new TypeFilter(EnumSet.of(CoreType.Instant, CoreType.Sorcery)), "Instant or Sorcery"),

        COMMON   (FSkinProp.IMG_PW_BADGE_COMMON, new RarityFilter(EnumSet.of(CardRarity.Common, CardRarity.Uncommon, CardRarity.Rare, CardRarity.Special, CardRarity.MythicRare)), "Common"),
        UNCOMMON (FSkinProp.IMG_PW_BADGE_UNCOMMON, new RarityFilter(EnumSet.of(CardRarity.Uncommon, CardRarity.Rare, CardRarity.Special, CardRarity.MythicRare)), "Uncommon"),
        RARE     (FSkinProp.IMG_PW_BADGE_RARE, new RarityFilter(EnumSet.of(CardRarity.Rare, CardRarity.Special, CardRarity.MythicRare)), "Rare"),
        MYTHIC   (FSkinProp.IMG_PW_BADGE_MYTHIC, new RarityFilter(EnumSet.of(CardRarity.MythicRare)), "Mythic Rare (100%)"),

        CMC_LOW      (FSkinProp.IMG_CMC_LOW, new CMCFilter(0, 3), "CMC 0-3"),
        CMC_LOW_MID  (FSkinProp.IMG_CMC_LOW_MID, new CMCFilter(2, 5), "CMC 2-5"),
        CMC_MID_HIGH (FSkinProp.IMG_CMC_MID_HIGH, new CMCFilter(4, 7), "CMC 4-7"),
        CMC_HIGH     (FSkinProp.IMG_CMC_HIGH, new CMCFilter(6, -1), "CMC 6+");

        private final FSkinProp skinProp;
        private final Predicate<PaperCard> predicate;
        private String caption;

        private AEtherFilter(final FSkinProp skinProp0, final Predicate<PaperCard> predicate0, final String caption0) {
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
            return getRarity(0);
        }
        public CardRarity getRarity(double randomSeed) {
            if (predicate instanceof RarityFilter) {
                float total = 0;
                CardRarity rarity = null;
                EnumMap<CardRarity, Double> rarityOdds = ((RarityFilter)predicate).rarityOdds;
                for (Entry<CardRarity, Double> entry : rarityOdds.entrySet()) {
                    rarity = entry.getKey();
                    total += entry.getValue();
                    if (randomSeed < total) {
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
        String name = "";
        for (ManaCostShard s : color.getOrderedShards()) {
            name += s.toString();
        }
        name = name.replaceAll("[{}]", ""); //remove all brackets
        try {
            return AEtherFilter.valueOf(name);
        }
        catch (Exception e) {
            System.err.println("No color filter with name " + name);
            return AEtherFilter.WUBRG; //return 5-color filter as fallback
        }
    }

    public static void updateRarityFilterOdds() {
        ConquestPreferences prefs = FModel.getConquestPreferences();

        EnumMap<CardRarity, Double> odds = new EnumMap<CardRarity, Double>(CardRarity.class);
        double commonsPerBooster = prefs.getPrefInt(CQPref.BOOSTER_COMMONS);
        double uncommonPerBooster = prefs.getPrefInt(CQPref.BOOSTER_UNCOMMONS);
        double raresPerBooster = prefs.getPrefInt(CQPref.BOOSTER_RARES);
        double mythicsPerBooster = raresPerBooster / (double)prefs.getPrefInt(CQPref.BOOSTERS_PER_MYTHIC);

        odds.put(CardRarity.Common, 1d);
        odds.put(CardRarity.Uncommon, uncommonPerBooster / commonsPerBooster);
        odds.put(CardRarity.Rare, raresPerBooster / commonsPerBooster);
        odds.put(CardRarity.MythicRare, mythicsPerBooster / commonsPerBooster);

        for (AEtherFilter filter : RARITY_FILTERS) {
            filter.caption = ((RarityFilter)filter.predicate).updateOdds(odds);
        }
    }

    public static final AEtherFilter[] COLOR_FILTERS = new AEtherFilter[] {
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
        AEtherFilter.WUBRG };

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
        public boolean apply(PaperCard card) {
            return card.getRules().getColorIdentity().hasNoColorsExcept(color);
        }
    }

    private static class TypeFilter implements Predicate<PaperCard> {
        private final EnumSet<CoreType> types;

        private TypeFilter(EnumSet<CoreType> types0) {
            types = types0;
        }

        @Override
        public boolean apply(PaperCard card) {
            CardType cardType = card.getRules().getType();
            for (CoreType type : types) {
                if (cardType.hasType(type)) {
                    return true;
                }
            }
            return false;
        }
    }

    private static class RarityFilter implements Predicate<PaperCard> {
        private final EnumMap<CardRarity, Double> rarityOdds;

        private RarityFilter(EnumSet<CardRarity> rarities0) {
            rarityOdds = new EnumMap<CardRarity, Double>(CardRarity.class);
            for (CardRarity rarity : rarities0) {
                rarityOdds.put(rarity, 0d); //values will be set later
            }
        }

        private String updateOdds(EnumMap<CardRarity, Double> oddsLookup) {
            double baseOdds = 0;
            double remainingOdds = 1;
            CardRarity baseRarity = null;
            String caption = "";

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
                    caption += ", " + rarity.getLongName() + " (" + (Math.round(1000 * odds) / 10) + "%)"; //round to nearest single decimal point
                    rarityOdds.put(rarity, odds);
                }
            }

            //prepend base rarity and odds
            caption = baseRarity.getLongName() + " (" + (Math.round(1000 * remainingOdds) / 10) + "%)" + caption;
            rarityOdds.put(baseRarity, remainingOdds);

            return caption;
        }

        @Override
        public boolean apply(PaperCard card) {
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
        public boolean apply(PaperCard card) {
            int cardCmc = card.getRules().getManaCost().getCMC();
            if (cardCmc < cmcMin) { return false; }
            if (cmcMax != -1 && cardCmc > cmcMax) { return false; }
            return true;
        }
    }
}
