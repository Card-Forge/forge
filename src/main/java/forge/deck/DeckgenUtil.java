package forge.deck;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeMap;

import javax.swing.JList;
import javax.swing.JOptionPane;

import org.apache.commons.lang3.ArrayUtils;

import com.google.common.collect.Iterables;

import forge.Singletons;
import forge.deck.generate.Generate2ColorDeck;
import forge.deck.generate.Generate3ColorDeck;
import forge.deck.generate.Generate5ColorDeck;
import forge.deck.generate.GenerateThemeDeck;
import forge.game.player.PlayerType;
import forge.item.CardDb;
import forge.item.CardPrinted;
import forge.item.ItemPool;
import forge.item.ItemPoolView;
import forge.quest.QuestEvent;
import forge.quest.QuestEventManager;
import forge.util.IStorage;
import forge.util.MyRandom;

/** 
 * Utility collection for various types of decks.
 * - Builders (builds or retrieves deck based on a selection)
 * - Randomizers (retrieves random deck of selected type)
 * - Color checker (see javadoc)
 * - Decklist display-er
 */
// TODO This class can be used for home menu constructed deck generation as well.
public class DeckgenUtil {
    /** */
    public static final Map<String, String> COLOR_VALS = new HashMap<String, String>();

    static {
        COLOR_VALS.clear();
        COLOR_VALS.put("Random 1", "AI");
        COLOR_VALS.put("Random 2", "AI");
        COLOR_VALS.put("Random 3", "AI");
        COLOR_VALS.put("Random 4", "AI");
        COLOR_VALS.put("Black", "black");
        COLOR_VALS.put("Blue", "blue");
        COLOR_VALS.put("Green", "green");
        COLOR_VALS.put("Red", "red");
        COLOR_VALS.put("White", "white");
    }

    /** */
    public enum DeckTypes { /** */
        COLORS, /** */
        THEMES, /** */
        CUSTOM, /** */
        QUESTEVENTS
    }

    /**
     * @param selection {@link java.lang.String} array
     * @return {@link forge.deck.Deck}
     */
    public static Deck buildColorDeck(final String[] selection, PlayerType pt) {
        ItemPoolView<CardPrinted> cards = null;
        final Deck deck;

        // Replace "random" with "AI" for deck generation code
        for (int i = 0; i < selection.length; i++) {
            selection[i] = COLOR_VALS.get(selection[i]);
        }

        // 2, 3, and 5 colors.
        if (selection.length == 2) {
            final Generate2ColorDeck gen = new Generate2ColorDeck(
                    selection[0], selection[1]);
            cards = gen.get2ColorDeck(60, pt);
        }
        else if (selection.length == 3) {
            final Generate3ColorDeck gen = new Generate3ColorDeck(
                    selection[0], selection[1], selection[2]);
            cards = gen.get3ColorDeck(60, pt);
        }
        else {
            final Generate5ColorDeck gen = new Generate5ColorDeck();
            cards = gen.get5ColorDeck(60, pt);
        }

        // After generating card lists, build deck.
        deck = new Deck();
        deck.getMain().addAll(cards);

        return deck;
    }

    /**
     * @param selection {@link java.lang.String}
     * @return {@link forge.deck.Deck}
     */
    public static Deck buildThemeDeck(final String[] selection) {
        final GenerateThemeDeck gen = new GenerateThemeDeck();
        final Deck deck = new Deck();
        deck.getMain().addAll(gen.getThemeDeck(selection[0], 60));

        return deck;
    }

    /**
     * Gets a user deck.
     *
     * @param selection {java.lang.String}
     * @return {@link forge.deck.Deck}
     */
    public static Deck buildCustomDeck(final String[] selection) {
        return Singletons.getModel().getDecks().getConstructed().get(selection[0]);
    }

    /**
     * Gets a quest deck.
     * 
     * @param selection {java.lang.String}
     * @return {@link forge.deck.Deck}
     */
    public static Deck buildQuestDeck(final String[] selection) {
        return Singletons.getModel().getQuest().getDuelsManager().getEvent(selection[0]).getEventDeck();
    }

    /** @return {@link forge.deck.Deck} */
    public static Deck getRandomColorDeck(PlayerType pt) {
        final int[] colorCount = new int[] {2, 3, 5};
        final int count = colorCount[(int) (Math.round(Math.random() * 2))];
        final String[] selection = new String[count];

        // A simulated selection of "random 1" will trigger the AI selection process.
        for (int i = 0; i < count; i++) { selection[i] = "Random 1"; }

        return DeckgenUtil.buildColorDeck(selection, pt);
    }

    /** @return {@link forge.deck.Deck} */
    public static Deck getRandomThemeDeck() {
        final List<String> themeNames = new ArrayList<String>();
        for (final String s : GenerateThemeDeck.getThemeNames()) { themeNames.add(s); }
        final int rand = (int) (Math.floor(Math.random() * themeNames.size()));
        return DeckgenUtil.buildThemeDeck(new String[] {themeNames.get(rand)});
    }

    /** @return {@link forge.deck.Deck} */
    public static Deck getRandomCustomDeck() {
        final IStorage<Deck> allDecks = Singletons.getModel().getDecks().getConstructed();
        final int rand = (int) (Math.floor(Math.random() * allDecks.getCount()));
        final String name = allDecks.getNames().toArray(new String[0])[rand];
        return allDecks.get(name);
    }

    /** @return {@link forge.deck.Deck} */
    public static Deck getRandomQuestDeck() {
        final List<Deck> allQuestDecks = new ArrayList<Deck>();
        final QuestEventManager manager = Singletons.getModel().getQuest().getDuelsManager();

        for (final QuestEvent e : manager.getAllDuels()) {
            allQuestDecks.add(e.getEventDeck());
        }

        for (final QuestEvent e : manager.getAllChallenges()) {
            allQuestDecks.add(e.getEventDeck());
        }

        final int rand = (int) (Math.floor(Math.random() * allQuestDecks.size()));
        return allQuestDecks.get(rand);
    }

    /**
     * Returns random selection of colors from a nine-value list
     * (BGRUW, rand 1-4) within Forge support limitations (only 2, 3, 5 color deckgen).
     * Used for random color deck generation.
     * @return int[] */
    public static int[] randomSelectColors() {
        // Color select algorithm
        int x = -1;
        // HACK because 1 and 4 color decks are not supported yet. :(
        while (x == -1 || x == 1 || x == 4) {
            x = (int) Math.ceil(Math.random() * 5);
        }
        final Integer colorCount = x;
        final int maxCount = 9;
        Integer[] selectedIndices = new Integer[colorCount];

        x = -1;
        for (int i = 0; i < colorCount; i++) {
            while (x == -1) {
                x = (int) Math.floor(Math.random() * maxCount);
                if (Arrays.asList(selectedIndices).contains(x)) { x = -1; }
                else { selectedIndices[i] = x; }
            }
            x = -1;
        }

       return ArrayUtils.toPrimitive(selectedIndices);
    }

    /** @param lst0 {@link javax.swing.JList} */
    public static void randomSelect(final JList lst0) {
        final int size = lst0.getModel().getSize();

        if (size > 0) {
            final Random r = new Random();
            final int i = r.nextInt(size);

            lst0.setSelectedIndex(i);
            lst0.ensureIndexIsVisible(lst0.getSelectedIndex());
        }
    }

    /** Shows decklist dialog for a given deck.
     * @param lst0 {@link javax.swing.JList}
     */
    public static void showDecklist(final JList lst0) {
        final String deckName = lst0.getSelectedValue().toString();
        final Deck deck;

        // Retrieve from custom or quest deck maps
        if (lst0.getName().equals(DeckTypes.CUSTOM.toString())) {
            deck = Singletons.getModel().getDecks().getConstructed().get(deckName);
        }
        else {
            deck = Singletons.getModel().getQuest().getDuelsManager().getEvent(deckName).getEventDeck();
        }

        // Dump into map and display.
        final Map<String, Integer> deckMap = new TreeMap<String, Integer>();

        for (final Entry<CardPrinted, Integer> s : deck.getMain()) {
            deckMap.put(s.getKey().getName(), s.getValue());
        }

        final String nl = System.getProperty("line.separator");
        final StringBuilder deckList = new StringBuilder();
        final String dName = deck.getName();

        deckList.append(dName == null ? "" : dName + nl + nl);

        if (deck.getAvatar() != null) {
            deckList.append("Avatar: " + deck.getAvatar().getName() + nl + nl);
        }
        if (deck.getCommander() != null) {
            deckList.append("Commander: " + deck.getCommander().getName() + nl + nl);
        }

        for (final Entry<String, Integer> ev : deckMap.entrySet()) {
            deckList.append(ev.getValue() + " x " + ev.getKey() + nl);
        }

        final StringBuilder msg = new StringBuilder();
        if (deckMap.keySet().size() <= 32) {
            msg.append(deckList.toString() + nl);
        } else {
            msg.append("Decklist too long for dialog." + nl + nl);
        }

        msg.append("Copy Decklist to Clipboard?");

        // Output
        final int rcMsg = JOptionPane.showConfirmDialog(null, msg, "Decklist", JOptionPane.OK_CANCEL_OPTION);
        if (rcMsg == JOptionPane.OK_OPTION) {
            final StringSelection ss = new StringSelection(deckList.toString());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, null);
        }
    } // End showDecklist

    /** 
     * Checks lengths of selected values for color lists
     * to see if a deck generator exists. Alert and visual reminder if fail.
     * 
     * @param colors0 String[]
     * @return boolean
     */
    public static boolean colorCheck(final String[] colors0) {
        boolean result = true;

        if (colors0.length == 1) {
            JOptionPane.showMessageDialog(null,
                    "Sorry, single color generated decks aren't supported yet."
                    + "\n\rPlease choose at least one more color for this deck.",
                    "Generate deck: 1 color", JOptionPane.ERROR_MESSAGE);
            result = false;
        }
        else if (colors0.length == 4) {
            JOptionPane.showMessageDialog(null,
                    "Sorry, four color generated decks aren't supported yet."
                    + "\n\rPlease use 2, 3, or 5 colors for this deck.",
                    "Generate deck: 4 colors", JOptionPane.ERROR_MESSAGE);
            result = false;
        }
        else if (colors0.length > 5) {
            JOptionPane.showMessageDialog(null,
                    "Generate deck: maximum five colors!",
                    "Generate deck: too many colors", JOptionPane.ERROR_MESSAGE);
            result = false;
        }
        return result;
    }

    public static Deck generateSchemeDeck() {
        Deck res = new Deck();

        Iterable<CardPrinted> allSchemes = Iterables.filter(CardDb.instance().getAllNonTraditionalCards(), CardPrinted.Predicates.type("Scheme"));

        for (int i = 0; i < 20; i++) {
            CardPrinted cp = Iterables.get(allSchemes, MyRandom.getRandom().nextInt(Iterables.size(allSchemes)));
            int appearances = 0;
            for (CardPrinted added : res.getSchemes().toFlatList()) {
                if (added.getName().equals(cp.getName())) {

                    appearances++;
                }
            }

            if (appearances < 2) {

                res.getSchemes().add(cp);
            } else {

                i--;
            }
        }

        return res;
    }
}
