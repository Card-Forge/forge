package forge.planarconquest;

import java.util.ArrayList;
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

    public static enum AEtherFilter implements IHasSkinProp {
        NONE  (null, null, "(None)"),

        WHITE     (FSkinProp.IMG_MANA_W, new ColorFilter(MagicColor.WHITE), "White"),
        BLUE      (FSkinProp.IMG_MANA_U, new ColorFilter(MagicColor.BLUE), "Blue"),
        BLACK     (FSkinProp.IMG_MANA_B, new ColorFilter(MagicColor.BLACK), "Black"),
        RED       (FSkinProp.IMG_MANA_R, new ColorFilter(MagicColor.RED), "Red"),
        GREEN     (FSkinProp.IMG_MANA_G, new ColorFilter(MagicColor.GREEN), "Green"),
        COLORLESS (FSkinProp.IMG_MANA_COLORLESS, new ColorFilter(MagicColor.COLORLESS), "Colorless"),

        CREATURE             (FSkinProp.IMG_CREATURE, new TypeFilter(EnumSet.of(CoreType.Creature)), "Creature"),
        ARTIFACT_ENCHANTMENT (FSkinProp.IMG_ENCHANTMENT, new TypeFilter(EnumSet.of(CoreType.Artifact, CoreType.Enchantment, CoreType.Planeswalker)), "Artifact, Enchantment, or Planeswalker"),
        INSTANT_SORCERY      (FSkinProp.IMG_SORCERY, new TypeFilter(EnumSet.of(CoreType.Instant, CoreType.Sorcery)), "Instant or Sorcery"),
        LAND                 (FSkinProp.IMG_LAND, new TypeFilter(EnumSet.of(CoreType.Land)), "Land"),

        COMMON   (FSkinProp.IMG_PW_BADGE_COMMON, new RarityFilter(CardRarity.Common), "Common"),
        UNCOMMON (FSkinProp.IMG_PW_BADGE_UNCOMMON, new RarityFilter(CardRarity.Uncommon), "Uncommon"),
        RARE     (FSkinProp.IMG_PW_BADGE_RARE, new RarityFilter(CardRarity.Rare), "Rare"),
        MYTHIC   (FSkinProp.IMG_PW_BADGE_MYTHIC, new RarityFilter(CardRarity.MythicRare), "Mythic Rare"),

        CMC_LOW      (FSkinProp.IMG_CMC_LOW, new CMCFilter(0, 3), "CMC 0-3"),
        CMC_LOW_MID  (FSkinProp.IMG_CMC_LOW_MID, new CMCFilter(2, 5), "CMC 2-5"),
        CMC_MID_HIGH (FSkinProp.IMG_CMC_MID_HIGH, new CMCFilter(4, 7), "CMC 4-7"),
        CMC_HIGH     (FSkinProp.IMG_CMC_HIGH, new CMCFilter(6, -1), "CMC 6+");

        public final FSkinProp skinProp;
        public final Predicate<PaperCard> predicate;
        public final String caption;

        private AEtherFilter(final FSkinProp skinProp0, final Predicate<PaperCard> predicate0, final String caption0) {
            skinProp = skinProp0;
            predicate = predicate0;
            caption = caption0;
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

    public static final AEtherFilter[] COLOR_FILTERS = new AEtherFilter[] {
        AEtherFilter.NONE,
        AEtherFilter.WHITE,
        AEtherFilter.BLUE,
        AEtherFilter.BLACK,
        AEtherFilter.RED,
        AEtherFilter.GREEN,
        AEtherFilter.COLORLESS };

    public static final AEtherFilter[] TYPE_FILTERS = new AEtherFilter[] {
        AEtherFilter.NONE,
        AEtherFilter.CREATURE,
        AEtherFilter.ARTIFACT_ENCHANTMENT,
        AEtherFilter.INSTANT_SORCERY,
        AEtherFilter.LAND };

    public static final AEtherFilter[] RARITY_FILTERS = new AEtherFilter[] {
        AEtherFilter.NONE,
        AEtherFilter.COMMON,
        AEtherFilter.UNCOMMON,
        AEtherFilter.RARE,
        AEtherFilter.MYTHIC };

    public static final AEtherFilter[] CMC_FILTERS = new AEtherFilter[] {
        AEtherFilter.NONE,
        AEtherFilter.CMC_LOW,
        AEtherFilter.CMC_LOW_MID,
        AEtherFilter.CMC_MID_HIGH,
        AEtherFilter.CMC_HIGH };

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
