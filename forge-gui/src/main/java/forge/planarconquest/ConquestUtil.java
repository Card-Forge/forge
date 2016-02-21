package forge.planarconquest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import forge.assets.FSkinProp;
import forge.card.CardRarity;
import forge.card.CardRules;
import forge.card.CardType.CoreType;
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
        for (PaperCard pc : FModel.getConquest().getModel().getUnlockedCards()) {
            availableCards.add(pc);
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

    public static Iterable<PaperCard> getStartingPlaneswalkerOptions() {
        return Iterables.filter(FModel.getMagicDb().getCommonCards().getUniqueCards(), new Predicate<PaperCard>() {
            @Override
            public boolean apply(PaperCard card) {
                CardRules rules = card.getRules();
                return rules.getType().isPlaneswalker() && !rules.canBeCommander(); //don't allow picking a commander as a starting planeswalker
            }
        });
    }

    public static enum FilterOption {
        NONE  (null, null, "(None)"),

        COLORLESS  (FSkinProp.IMG_MANA_COLORLESS, new ColorFilter(MagicColor.COLORLESS), "Colorless"),
        WHITE      (FSkinProp.IMG_MANA_W, new ColorFilter(MagicColor.WHITE), "White"),
        BLUE       (FSkinProp.IMG_MANA_U, new ColorFilter(MagicColor.BLUE), "Blue"),
        BLACK      (FSkinProp.IMG_MANA_B, new ColorFilter(MagicColor.BLACK), "Black"),
        RED        (FSkinProp.IMG_MANA_R, new ColorFilter(MagicColor.RED), "Red"),
        GREEN      (FSkinProp.IMG_MANA_G, new ColorFilter(MagicColor.GREEN), "Green"),

        LAND         (FSkinProp.IMG_LAND, new TypeFilter(CoreType.Land), "Land"),
        ARTIFACT     (FSkinProp.IMG_ARTIFACT, new TypeFilter(CoreType.Artifact), "Artifact"),
        CREATURE     (FSkinProp.IMG_CREATURE, new TypeFilter(CoreType.Creature), "Creature"),
        ENCHANTMENT  (FSkinProp.IMG_ENCHANTMENT, new TypeFilter(CoreType.Enchantment), "Enchantment"),
        PLANESWALKER (FSkinProp.IMG_PLANESWALKER, new TypeFilter(CoreType.Planeswalker), "Planeswalker"),
        INSTANT      (FSkinProp.IMG_INSTANT, new TypeFilter(CoreType.Instant), "Instant"),
        SORCERY      (FSkinProp.IMG_SORCERY, new TypeFilter(CoreType.Sorcery), "Sorcery"),

        COMMON   (FSkinProp.IMG_PW_BADGE_COMMON, new RarityFilter(CardRarity.Common), "Common"),
        UNCOMMON (FSkinProp.IMG_PW_BADGE_UNCOMMON, new RarityFilter(CardRarity.Uncommon), "Uncommon"),
        RARE     (FSkinProp.IMG_PW_BADGE_RARE, new RarityFilter(CardRarity.Rare), "Rare"),
        MYTHIC   (FSkinProp.IMG_PW_BADGE_MYTHIC, new RarityFilter(CardRarity.MythicRare), "Mythic Rare"),

        CMC_0 (FSkinProp.IMG_MANA_0, new CMCFilter(0, false), "CMC 0"),
        CMC_1 (FSkinProp.IMG_MANA_1, new CMCFilter(1, false), "CMC 1"),
        CMC_2 (FSkinProp.IMG_MANA_2, new CMCFilter(2, false), "CMC 2"),
        CMC_3 (FSkinProp.IMG_MANA_3, new CMCFilter(3, false), "CMC 3"),
        CMC_4 (FSkinProp.IMG_MANA_4, new CMCFilter(4, false), "CMC 4"),
        CMC_5 (FSkinProp.IMG_MANA_5, new CMCFilter(5, false), "CMC 5"),
        CMC_6 (FSkinProp.IMG_MANA_6, new CMCFilter(6, true), "CMC 6+");

        public final FSkinProp skinProp;
        public final Predicate<PaperCard> predicate;
        public final String caption;

        private FilterOption(final FSkinProp skinProp0, final Predicate<PaperCard> predicate0, final String caption0) {
            skinProp = skinProp0;
            predicate = predicate0;
            caption = caption0;
        }

        @Override
        public String toString() {
            return caption;
        }
    }

    public static final FilterOption[] COLOR_FILTERS = new FilterOption[] {
        FilterOption.NONE,
        FilterOption.COLORLESS,
        FilterOption.WHITE,
        FilterOption.BLUE,
        FilterOption.BLACK,
        FilterOption.RED,
        FilterOption.GREEN };

    public static final FilterOption[] TYPE_FILTERS = new FilterOption[] {
        FilterOption.NONE,
        FilterOption.LAND,
        FilterOption.ARTIFACT,
        FilterOption.CREATURE,
        FilterOption.ENCHANTMENT,
        FilterOption.PLANESWALKER,
        FilterOption.INSTANT,
        FilterOption.SORCERY };

    public static final FilterOption[] RARITY_FILTERS = new FilterOption[] {
        FilterOption.NONE,
        FilterOption.COMMON,
        FilterOption.UNCOMMON,
        FilterOption.RARE,
        FilterOption.MYTHIC };

    public static final FilterOption[] CMC_FILTERS = new FilterOption[] {
        FilterOption.NONE,
        FilterOption.CMC_0,
        FilterOption.CMC_1,
        FilterOption.CMC_2,
        FilterOption.CMC_3,
        FilterOption.CMC_4,
        FilterOption.CMC_5,
        FilterOption.CMC_6 };

    private static class ColorFilter implements Predicate<PaperCard> {
        private final byte color;

        private ColorFilter(byte color0) {
            color = color0;
        }

        @Override
        public boolean apply(PaperCard card) {
            //use color identity for lands and color for other cards
            CardRules cardRules = card.getRules();
            ColorSet cardColor = cardRules.getType().isLand() ? cardRules.getColorIdentity() : cardRules.getColor();

            if (color == MagicColor.COLORLESS) {
                return cardColor.isColorless();
            }
            return cardColor.hasAllColors(color);
        }
    }

    private static class TypeFilter implements Predicate<PaperCard> {
        private final CoreType type;

        private TypeFilter(CoreType type0) {
            type = type0;
        }

        @Override
        public boolean apply(PaperCard card) {
            return card.getRules().getType().hasType(type);
        }
    }

    private static class RarityFilter implements Predicate<PaperCard> {
        private final CardRarity rarity;

        private RarityFilter(CardRarity rarity0) {
            rarity = rarity0;
        }

        @Override
        public boolean apply(PaperCard card) {
            CardRarity cardRarity = card.getRarity();
            if (cardRarity == rarity) { return true; }
            if (cardRarity == CardRarity.Special && rarity == CardRarity.Rare) { return true; } //treat specials as rares
            return false;
        }
    }

    private static class CMCFilter implements Predicate<PaperCard> {
        private final int cmc;
        private final boolean allowGreater;

        private CMCFilter(int cmc0, boolean allowGreater0) {
            cmc = cmc0;
            allowGreater = allowGreater0;
        }

        @Override
        public boolean apply(PaperCard card) {
            int cardCmc = card.getRules().getManaCost().getCMC();
            if (cardCmc == cmc) { return true; }
            if (allowGreater && cardCmc > cmc) { return true; }
            return false;
        }
    }
}
