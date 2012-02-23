package forge.gui.home.constructed;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;

import net.miginfocom.swing.MigLayout;
import forge.AllZone;
import forge.CardList;
import forge.Command;
import forge.Player;
import forge.PlayerType;
import forge.Singletons;
import forge.deck.Deck;
import forge.deck.generate.Generate2ColorDeck;
import forge.deck.generate.Generate3ColorDeck;
import forge.deck.generate.Generate5ColorDeck;
import forge.deck.generate.GenerateThemeDeck;
import forge.game.GameNew;
import forge.item.CardPrinted;
import forge.properties.ForgePreferences.FPref;
import forge.view.toolbox.FCheckBox;
import forge.view.toolbox.FLabel;
import forge.view.toolbox.FList;
import forge.view.toolbox.FOverlay;
import forge.view.toolbox.FPanel;
import forge.view.toolbox.FScrollPane;
import forge.view.toolbox.FSkin;



/** 
 * Utilities for the constructed submenu, all over the MVC spectrum.
 *
 */
public class SubmenuConstructedUtil {
    /**
     * @param pnlParent &emsp; JPanel, where to put the finished lists
     * @param btnStart &emsp; Start button for the panel
     * @return List<JList> completed collection of JLists
     */
    public static List<JList> populateConstructedSubmenuView(final JPanel pnlParent, final JButton btnStart) {
        final List<JList> generatedJLists  = new ArrayList<JList>();
        final String constraintsLst = "w 41%!, h 320px, gap 6% 0 4% 2%";
        final String constraintsBtn = "newline, ax center, gap 6% 0 0 0, span 2 1";
        final List<Player> players = AllZone.getPlayersInGame();
        pnlParent.removeAll();

        pnlParent.setOpaque(false);
        pnlParent.setLayout(new MigLayout("insets 0, gap 0"));

        final List<CustomSelectPanel> storage = new ArrayList<CustomSelectPanel>();
        for (int i = 0; i < players.size(); i++) {
            storage.add(new CustomSelectPanel(players.get(i)));
            generatedJLists.add(storage.get(storage.size() - 1).getList());

            if (i % 2 == 1) {
                pnlParent.add(storage.get(storage.size() - 1), constraintsLst + ", wrap");
            }
            else {
                pnlParent.add(storage.get(storage.size() - 1), constraintsLst);
            }
        }
        storage.clear();

        if (pnlParent.getName().equals(ESubmenuConstructedTypes.COLORS.toString())
                || pnlParent.getName() .equals(ESubmenuConstructedTypes.THEMES.toString())) {
            final JPanel pnlOptions = new JPanel();
            pnlOptions.setOpaque(false);
            SubmenuConstructedUtil.populateOptionsPanel(pnlOptions);
            pnlParent.add(pnlOptions, "span 2 1, align center, gap 6% 0 0 0");
        }

        pnlParent.add(btnStart, constraintsBtn);

        return generatedJLists;
    }

    /** Shows decklist dialog for a given deck.
     * @param d0 &emsp; {@link forge.deck.Deck} */
    public static void showDecklist(final Deck d0) {
        final HashMap<String, Integer> deckMap = new HashMap<String, Integer>();

        for (final Entry<CardPrinted, Integer> s : d0.getMain()) {
            deckMap.put(s.getKey().getName(), s.getValue());
        }

        final String nl = System.getProperty("line.separator");
        final StringBuilder deckList = new StringBuilder();
        final String dName = d0.getName();

        deckList.append(dName == null ? "" : dName + nl + nl);

        final ArrayList<String> dmKeys = new ArrayList<String>();
        for (final String s : deckMap.keySet()) {
            dmKeys.add(s);
        }

        Collections.sort(dmKeys);

        for (final String s : dmKeys) {
            deckList.append(deckMap.get(s) + " x " + s + nl);
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
     * Exhaustively converts object array to string array.
     * Probably a much easier way to do this.
     * And, there must be a better place for this.
     * 
     * @param o0 &emsp; Object[]
     * @return String[]
     */
    public static String[] oa2sa(final Object[] o0) {
        final String[] output = new String[o0.length];

        for (int i = 0; i < o0.length; i++) {
            output[i] = o0[i].toString();
        }

        return output;
    }

    /** @param lst0 &emsp; {@link javax.swing.JList} */
    public static void randomSelect(final JList lst0) {
        if (lst0.getName().equals(ESubmenuConstructedTypes.COLORS.toString())) {
            // Color select algorithm
            int x = -1;
            // HACK because 1 and 4 color decks are not supported yet. :(
            while (x == -1 || x == 1 || x == 4) {
                x = (int) Math.ceil(Math.random() * 5);
            }
            final int colorCount = x;

            final int maxCount = lst0.getModel().getSize();
            int[] selectedIndices = new int[colorCount];

            x = -1;
            for (int i = 0; i < colorCount; i++) {
                while (x == -1) {
                    x = (int) Math.floor(Math.random() * maxCount);
                    if (Arrays.asList(selectedIndices).contains(x)) { x = -1; }
                    else { selectedIndices[i] = x; }
                }
                x = -1;
            }
            lst0.setSelectedIndices(selectedIndices);
            selectedIndices = null;
        }
        else {
            final int size = lst0.getModel().getSize();

            if (size > 0) {
                final Random r = new Random();
                final int i = r.nextInt(size);

                lst0.setSelectedIndex(i);
                lst0.ensureIndexIsVisible(lst0.getSelectedIndex());
            }
        }
    }

    /** Generates deck from current list selection(s). */
    private static Deck generateDeck(final JList lst0, final PlayerType player0) {
        CardList cards = null;
        final String[] selection = oa2sa(lst0.getSelectedValuesList().toArray());
        final Deck deck;

        // Color deck
        if (lst0.getName().equals("lstColor") && colorCheck(selection)) {
            final Map<String, String> colorVals = new HashMap<String, String>();
            colorVals.put("Random 1", "AI");
            colorVals.put("Random 2", "AI");
            colorVals.put("Random 3", "AI");
            colorVals.put("Random 4", "AI");
            colorVals.put("Black", "black");
            colorVals.put("Blue", "blue");
            colorVals.put("Green", "green");
            colorVals.put("Red", "red");
            colorVals.put("White", "white");

            // Replace "random" with "AI" for deck generation code
            for (int i = 0; i < selection.length; i++) {
                selection[i] = colorVals.get(selection[i]);
            }

            // 2, 3, and 5 colors.
            if (selection.length == 2) {
                final Generate2ColorDeck gen = new Generate2ColorDeck(
                        selection[0], selection[1]);
                cards = gen.get2ColorDeck(60, player0);
            }
            else if (selection.length == 3) {
                final Generate3ColorDeck gen = new Generate3ColorDeck(
                        selection[0], selection[1], selection[2]);
                cards = gen.get3ColorDeck(60, player0);
            }
            else {
                final Generate5ColorDeck gen = new Generate5ColorDeck(
                        selection[0], selection[1], selection[2], selection[3], selection[4]);
                cards = gen.get5ColorDeck(60, player0);
            }

            // After generating card lists, build deck.
            deck = new Deck();
            deck.getMain().add(cards);
            colorVals.clear();
        }

        // Theme deck
        else if (lst0.getName().equals(ESubmenuConstructedTypes.THEMES.toString())) {
            final GenerateThemeDeck gen = new GenerateThemeDeck();
            cards = gen.getThemeDeck(selection[0], 60);

            // After generating card lists, build deck.
            deck = new Deck();
            deck.getMain().add(cards);
        }
        else if (lst0.getName().equals(ESubmenuConstructedTypes.QUESTEVENTS.toString())) {
            deck = Singletons.getModel().getQuestEventManager().getEvent(selection[0]).getEventDeck();
        }
        // Custom deck
        else {
            deck = Singletons.getModel().getDecks().getConstructed().get(selection[0]);
        }

        return deck;
    }

    @SuppressWarnings("serial")
    private static void populateOptionsPanel(final JPanel pnlParent) {
        final FCheckBox cbSingletons = new FCheckBox("Singleton Mode");
        final FCheckBox cbArtifacts = new FCheckBox("Remove Artifacts");
        final FCheckBox cbRemoveSmall = new FCheckBox("Remove Small Creatures");
        final String constraints = "ax center, gap 0 0 0 5px";

        cbSingletons.setSelected(Singletons.getModel()
                .getPreferences().getPrefBoolean(FPref.DECKGEN_SINGLETONS));
        cbArtifacts.setSelected(Singletons.getModel()
                .getPreferences().getPrefBoolean(FPref.DECKGEN_ARTIFACTS));
        cbRemoveSmall.setSelected(Singletons.getModel()
                .getPreferences().getPrefBoolean(FPref.DECKGEN_NOSMALL));

        // Event handling must occur here, unfortunately.
        cbSingletons.setCommand(new Command() {
            @Override
            public void execute() {
                Singletons.getModel().getPreferences().setPref(
                        FPref.DECKGEN_SINGLETONS, String.valueOf(cbSingletons.isSelected()));
            }
        });

        cbArtifacts.setCommand(new Command() {
            @Override
            public void execute() {
                Singletons.getModel().getPreferences().setPref(
                        FPref.DECKGEN_ARTIFACTS, String.valueOf(cbArtifacts.isSelected()));
            }
        });

        cbRemoveSmall.setCommand(new Command() {
            @Override
            public void execute() {
                Singletons.getModel().getPreferences().setPref(
                        FPref.DECKGEN_NOSMALL, String.valueOf(cbRemoveSmall.isSelected()));
            }
        });

        pnlParent.removeAll();
        pnlParent.setLayout(new MigLayout("inset 0, gap 0, wrap"));
        pnlParent.add(cbSingletons, constraints);
        pnlParent.add(cbArtifacts, constraints);
        pnlParent.add(cbRemoveSmall, constraints);
    }

    /** 
     * Checks lengths of selected values for color lists
     * to see if a deck generator exists. Alert and visual reminder if fail.
     * 
     * @param colors0 &emsp; String[] of color names
     * @return boolean
     */
    private static boolean colorCheck(final String[] colors0) {
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

    /** @param lists0 &emsp; {@link java.util.List}<{@link javax.swing.JList}> */
    public static void startGame(final List<JList> lists0) {
        final Deck deckHuman = generateDeck(lists0.get(0), PlayerType.HUMAN);
        final Deck deckAI = generateDeck(lists0.get(1), PlayerType.COMPUTER);

        final FOverlay overlay = Singletons.getView().getOverlay();
        overlay.setLayout(new MigLayout("insets 0, gap 0, align center"));

        final FPanel pnl = new FPanel();
        pnl.setLayout(new MigLayout("insets 0, gap 0, ax center, wrap"));
        pnl.setBackground(FSkin.getColor(FSkin.Colors.CLR_ACTIVE));

        pnl.add(new FLabel.Builder().icon(FSkin.getIcon(FSkin.ForgeIcons.ICO_LOGO)).build(),
                "h 200px!, align center");
        pnl.add(new FLabel.Builder().text("Loading new game...")
                .fontScaleAuto(false).fontSize(22).build(), "h 40px!, align center");

        overlay.add(pnl, "h 300px!, w 400px!");

        overlay.showOverlay();
        GameNew.newGame(deckHuman, deckAI);
        overlay.hideOverlay();
    }

    @SuppressWarnings("serial")
    private static class CustomSelectPanel extends JPanel {
        private final FList lst;
        private final Command cmd = new Command() { @Override
            public void execute() { randomSelect(lst); } };

        public CustomSelectPanel(final Player p0) {
            lst = new FList();
            lst.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

            this.setOpaque(false);
            this.setLayout(new MigLayout("insets 0, gap 0, wrap"));
            this.add(new FLabel.Builder().text(p0.getName()).fontSize(14)
                    .fontScaleAuto(false).build(),
                    "w 100%!, h 25px!, gap 0 0 0 8px");
            this.add(new FScrollPane(lst), "w 100%!, pushy, growy");
            this.add(new FLabel.Builder().text("Random").fontSize(14).opaque(true)
                    .cmdClick(cmd).hoverable(true).fontScaleAuto(false).build(),
                    "w 100%!, h 25px!, gap 0 0 8px 0");
        }

        public FList getList() {
            return lst;
        }
    }
}
