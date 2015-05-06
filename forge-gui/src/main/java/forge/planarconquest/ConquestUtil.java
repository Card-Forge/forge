package forge.planarconquest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

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
        CardPool cards = gen.getDeck(60, forAi);

        Deck deck = new Deck(commander.getName());
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
        HashSet<PaperCard> availableCards = new HashSet<PaperCard>(FModel.getConquest().getModel().getCollection());

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

        //create pool from available cards and allowed basic lands
        CardPool pool = new CardPool();
        pool.addAllFlat(availableCards);

        String setCode = FModel.getConquest().getModel().getCurrentPlane().getEditions().get(0).getCode();
        for (int i = 0; i < MagicColor.WUBRG.length; i++) {
            if (colorIdentity.hasAnyColor(MagicColor.WUBRG[i])) {
                pool.add(MagicColor.Constant.BASIC_LANDS.get(i), setCode, 50);
            }
        }
        return pool;
    }
}
