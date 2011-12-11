package forge.view.home;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.miginfocom.swing.MigLayout;
import forge.AllZone;
import forge.CardList;
import forge.Constant;
import forge.PlayerType;
import forge.deck.Deck;
import forge.deck.generate.Generate2ColorDeck;
import forge.deck.generate.Generate3ColorDeck;
import forge.deck.generate.Generate5ColorDeck;
import forge.deck.generate.GenerateThemeDeck;
import forge.game.GameType;
import forge.view.GuiTopLevel;
import forge.view.toolbox.FSkin;

/** 
 * TODO: Write javadoc for this type.
 *
 */
@SuppressWarnings("serial")
public class ViewConstructed extends JPanel {
    private JList lstColorsHuman, lstColorsAI, lstThemesHuman, lstThemesAI, lstDecksHuman, lstDecksAI;
    private String constraints;
    private FSkin skin;
    private HomeTopLevel parentView;
    private JList currentHumanSelection = null, currentAISelection = null;
    private Timer timer1 = null;
    private int counter;
    private Map<String, String> colorVals;

    /**
     * TODO: Write javadoc for Constructor.
     * @param v0 &emsp; HomeTopLevel parent view
     */
    public ViewConstructed(HomeTopLevel v0) {
        super();
        this.setOpaque(false);
        this.setLayout(new MigLayout("insets 0, gap 0"));
        parentView = v0;
        skin = AllZone.getSkin();

        final GenerateThemeDeck gen = new GenerateThemeDeck();

        colorVals = new HashMap<String, String>();
        colorVals.put("Random", "AI");
        colorVals.put("Black", "black");
        colorVals.put("Blue", "blue");
        colorVals.put("Green", "green");
        colorVals.put("Red", "red");
        colorVals.put("White", "white");

        // Content menu
        String[] colors = {"Random", "Random", "Random",
                "Random", "Black", "Blue", "Green", "Red", "White"};
        String[] decks = objectArrayToStringArray(AllZone.getDeckManager().getDecks().toArray());
        String[] themes = objectArrayToStringArray(gen.getThemeNames().toArray());

        lstColorsHuman = new JList(colors);
        lstColorsAI = new JList(colors);
        lstDecksHuman = new JList(decks);
        lstDecksAI = new JList(decks);
        lstThemesHuman = new JList(themes);
        lstThemesAI = new JList(themes);

        // Human
        JLabel lblHuman = new JLabel("Choose a deck for the human player:");
        lblHuman.setFont(skin.getFont1().deriveFont(Font.BOLD, 16));
        lblHuman.setForeground(skin.getColor("text"));
        this.add(lblHuman, "w 90%!, h 5%!, gap 5% 5% 2% 0, wrap, span 5 1");

        constraints = "w 28%!, h 30%!";
        this.add(new JScrollPane(lstColorsHuman), constraints + ", gapleft 3%");
        this.add(new OrPanel(), "w 5%!, h 30%!");
        this.add(new JScrollPane(lstThemesHuman), constraints);
        this.add(new OrPanel(), "w 5%!, h 30%!");
        this.add(new JScrollPane(lstDecksHuman), constraints + ", wrap");

        this.lstColorsHuman.getSelectionModel().addListSelectionListener(new HumanColorsListener());
        this.lstColorsHuman.setSelectedIndices(new int[] {0, 1});
        this.lstColorsHuman.setName("lstColorsHuman");

        this.lstThemesHuman.getSelectionModel().addListSelectionListener(new HumanThemesListener());
        this.lstThemesHuman.setSelectionMode(DefaultListSelectionModel.SINGLE_SELECTION);
        this.lstThemesHuman.setName("lstThemesHuman");

        this.lstDecksHuman.getSelectionModel().addListSelectionListener(new HumanDecksListener());
        this.lstDecksHuman.setSelectionMode(DefaultListSelectionModel.SINGLE_SELECTION);
        this.lstDecksHuman.setName("lstDecksHuman");

        // AI
        JLabel lblAI = new JLabel("Choose a deck for the AI player:");
        lblAI.setFont(skin.getFont1().deriveFont(Font.BOLD, 16));
        lblAI.setForeground(skin.getColor("text"));
        this.add(lblAI, "w 90%!, h 5%!, gap 5% 5% 2% 0, wrap, span 5 1");

        this.add(new JScrollPane(lstColorsAI), constraints + ", gapleft 3%");
        this.add(new OrPanel(), "w 5%!, h 30%!");
        this.add(new JScrollPane(lstThemesAI), constraints);
        this.add(new OrPanel(), "w 5%!, h 30%!");
        this.add(new JScrollPane(lstDecksAI), constraints + ", wrap");

        this.lstColorsAI.getSelectionModel().addListSelectionListener(new AIColorsListener());
        this.lstColorsAI.setSelectedIndices(new int[] {0, 1});
        this.lstColorsAI.setName("lstColorsAI");

        this.lstThemesAI.getSelectionModel().addListSelectionListener(new AIThemesListener());
        this.lstThemesAI.setSelectionMode(DefaultListSelectionModel.SINGLE_SELECTION);
        this.lstThemesAI.setName("lstThemesAI");

        this.lstDecksAI.getSelectionModel().addListSelectionListener(new AIDecksListener());
        this.lstDecksAI.setSelectionMode(DefaultListSelectionModel.SINGLE_SELECTION);
        this.lstDecksAI.setName("lstDecksAI");

        // Start button
        JButton btnStart = new JButton();
        btnStart.setAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent arg0) { start(); }
        });
        btnStart.setRolloverEnabled(true);
        btnStart.setPressedIcon(parentView.getStartButtonDown());
        btnStart.setRolloverIcon(parentView.getStartButtonOver());
        btnStart.setIcon(parentView.getStartButtonUp());
        btnStart.setOpaque(false);
        btnStart.setContentAreaFilled(false);
        btnStart.setBorder(null);
        btnStart.setBorderPainted(false);
        btnStart.setBounds(10, 476, 205, 84);

        JPanel pnlButtonContainer = new JPanel();
        pnlButtonContainer.setOpaque(false);
        this.add(pnlButtonContainer, "w 100%!, gaptop 2%, span 5 1");

        pnlButtonContainer.setLayout(new BorderLayout());
        pnlButtonContainer.add(btnStart, SwingConstants.CENTER);
    }

    // For some reason, MigLayout has sizing problems with a JLabel next to a JList.
    // So, the "or" label must be nested in a panel.
    private class OrPanel extends JPanel {
        public OrPanel() {
            super();
            setOpaque(false);
            setLayout(new BorderLayout());

            JLabel lblOr = new JLabel("OR");
            lblOr.setHorizontalAlignment(SwingConstants.CENTER);
            lblOr.setForeground(skin.getColor("text"));
            add(lblOr, BorderLayout.CENTER);
        }
    }

    /** @return HomeTopLevel */
    public HomeTopLevel getParentView() {
        return parentView;
    }

    private String[] objectArrayToStringArray(Object[] o0) {
        String[] output = new String[o0.length];

        for (int i = 0; i < o0.length; i++) {
            output[i] = o0[i].toString();
        }

        return output;
    }

    //========== LISTENERS

    /** Listeners for human lists, which pass control directly to regulation logic. */
    private class HumanColorsListener implements ListSelectionListener {
        @Override
        public void valueChanged(final ListSelectionEvent e) { regulateHuman(lstColorsHuman); }
    }

    private class HumanThemesListener implements ListSelectionListener {
        @Override
        public void valueChanged(final ListSelectionEvent e) { regulateHuman(lstThemesHuman); }
    }

    private class HumanDecksListener implements ListSelectionListener {
        @Override
        public void valueChanged(final ListSelectionEvent e) { regulateHuman(lstDecksHuman); }
    }

    /** Listeners for AI lists, which pass control directly to regulation logic. */
    private class AIColorsListener implements ListSelectionListener {
        @Override
        public void valueChanged(final ListSelectionEvent e) { regulateAI(lstColorsAI); }
    }

    private class AIThemesListener implements ListSelectionListener {
        @Override
        public void valueChanged(final ListSelectionEvent e) { regulateAI(lstThemesAI); }
    }

    private class AIDecksListener implements ListSelectionListener {
        @Override
        public void valueChanged(final ListSelectionEvent e) { regulateAI(lstDecksAI); }
    }

    /** Regulates that only one of the three deck type JLists is in use at a time. */
    private void regulateHuman(JList lst0) {
        if (currentHumanSelection != null && lst0 != currentHumanSelection) {
            currentHumanSelection.clearSelection();
        }

        currentHumanSelection = lst0;
    }

    /** Regulates that only one of the three deck type JLists is in use at a time. */
    private void regulateAI(JList lst0) {
        if (currentAISelection != null && lst0 != currentAISelection) {
            currentAISelection.clearSelection();
        }

        currentAISelection = lst0;
    }

    /** Fired when start button is pressed; checks various conditions from lists and starts game. */
    private void start() {
        String[] humanSelected = objectArrayToStringArray(currentHumanSelection.getSelectedValues());
        String[] aiSelected = objectArrayToStringArray(currentAISelection.getSelectedValues());

        // Check color-based deck selection for appropriate length
        if (currentHumanSelection.getName().equals("lstColorsHuman")) {
            if (!checkValidityOfHumanSelectedColors(humanSelected)) { return; }
        }

        if (currentAISelection.getName().equals("lstColorsAI")) {
            if (!checkValidityOfAISelectedColors(aiSelected)) { return; }
        }

        // If deck selection is acceptable, start a new game.
        generateHumanDecks(humanSelected);
        generateAIDecks(aiSelected);

        GuiTopLevel g = ((GuiTopLevel) AllZone.getDisplay());

        g.getController().changeState(1);
        g.getController().getMatchController().initMatch();

        // TODO shirley, there's a better place for this
        //Constant.Runtime.SMOOTH[0] = OldGuiNewGame.getSmoothLandCheckBox().isSelected();

        AllZone.getGameAction().newGame(Constant.Runtime.HUMAN_DECK[0], Constant.Runtime.COMPUTER_DECK[0]);
    }

    /** 
     * Checks lengths of selected values for human color lists
     * to see if a deck generator exists. Alert and visual reminder if fail.
     */
    private boolean checkValidityOfHumanSelectedColors(String[] human0) {
        boolean result = true;

        if (human0.length == 1) {
            JOptionPane.showMessageDialog(null,
                    "Sorry, single color generated decks aren't supported yet."
                    + "\n\rPlease choose at least one more color for the human deck.",
                    "Human deck: 1 color", JOptionPane.ERROR_MESSAGE);
            remind(currentHumanSelection);
            result = false;
        }
        else if (human0.length == 4) {
            JOptionPane.showMessageDialog(null,
                    "Sorry, four color generated decks aren't supported yet."
                    + "\n\rPlease use 2, 3, or 5 colors for the human deck.",
                    "Human deck: 4 colors", JOptionPane.ERROR_MESSAGE);
            remind(currentHumanSelection);
            result = false;
        }
        else if (human0.length > 5) {
            JOptionPane.showMessageDialog(null,
                    "Human deck: maximum five colors!",
                    "Human deck: too many colors", JOptionPane.ERROR_MESSAGE);
            remind(currentHumanSelection);
            result = false;
        }

        return result;
    }

    /** Generates human deck from current list selection(s). */
    private void generateHumanDecks(String[] human0) {
        CardList cards = null;

        // Human: Color-based deck generation
        if (currentHumanSelection.getName().equals("lstColorsHuman")) {
            // Replace "random" with "AI" for deck generation code
            for (int i = 0; i < human0.length; i++) {
                human0[i] = colorVals.get(human0[i]);
            }

            // 2, 3, and 5 colors.
            if (human0.length == 2) {
                Generate2ColorDeck gen = new Generate2ColorDeck(
                        human0[0], human0[1]);
                cards = gen.get2ColorDeck(60, PlayerType.HUMAN);
            }
            else if (human0.length == 3) {
                Generate3ColorDeck gen = new Generate3ColorDeck(
                        human0[0], human0[1], human0[2]);
                cards = gen.get3ColorDeck(60, PlayerType.HUMAN);
            }
            else {
                Generate5ColorDeck gen = new Generate5ColorDeck(
                        human0[0], human0[1], human0[2], human0[3], human0[4]);
                cards = gen.get5ColorDeck(60, PlayerType.HUMAN);
            }

            // After generating card lists, convert to deck and save.
            final Deck deck = new Deck(GameType.Constructed);

            for (int i = 0; i < cards.size(); i++) {
                deck.addMain(cards.get(i).getName());
            }

            Constant.Runtime.HUMAN_DECK[0] = deck;
        }

        // Human: theme deck generation
        else if (currentHumanSelection.getName().equals("lstThemesHuman")) {
            GenerateThemeDeck gen = new GenerateThemeDeck();
            cards = gen.getThemeDeck(human0[0], 60);

            // After generating card lists, convert to deck and save.
            final Deck deck = new Deck(GameType.Constructed);

            for (int i = 0; i < cards.size(); i++) {
                deck.addMain(cards.get(i).getName());
            }

            Constant.Runtime.HUMAN_DECK[0] = deck;
        }

        // Human: deck file
        // TODO random deck selection for theme and deck file
        else {
            Constant.Runtime.HUMAN_DECK[0] = AllZone.getDeckManager().getDeck(human0[0]);
        }
    }

    private void generateAIDecks(String[] ai0) {
        CardList cards = null;
        // AI: Color-based deck generation
        if (currentAISelection.getName().equals("lstColorsAI")) {
            // Replace "random" with "AI" for deck generation code
            for (int i = 0; i < ai0.length; i++) {
                ai0[i] = colorVals.get(ai0[i]);
            }

            // 2, 3, and 5 colors.
            if (ai0.length == 2) {
                Generate2ColorDeck gen = new Generate2ColorDeck(
                        ai0[0], ai0[1]);
                cards = gen.get2ColorDeck(60, PlayerType.COMPUTER);
            }
            else if (ai0.length == 3) {
                Generate3ColorDeck gen = new Generate3ColorDeck(
                        ai0[0], ai0[1], ai0[2]);
                cards = gen.get3ColorDeck(60, PlayerType.COMPUTER);
            }
            else {
                Generate5ColorDeck gen = new Generate5ColorDeck(
                        ai0[0], ai0[1], ai0[2], ai0[3], ai0[4]);
                cards = gen.get5ColorDeck(60, PlayerType.COMPUTER);
            }

            // After generating card lists, convert to deck and save.
            final Deck deck = new Deck(GameType.Constructed);

            for (int i = 0; i < cards.size(); i++) {
                deck.addMain(cards.get(i).getName());
            }

            Constant.Runtime.COMPUTER_DECK[0] = deck;
        }

        // AI: theme deck generation
        else if (currentAISelection.getName().equals("lstThemesAI")) {
            GenerateThemeDeck gen = new GenerateThemeDeck();
            cards = gen.getThemeDeck(ai0[0], 60);

            // After generating card lists, convert to deck and save.
            final Deck deck = new Deck(GameType.Constructed);

            for (int i = 0; i < cards.size(); i++) {
                deck.addMain(cards.get(i).getName());
            }

            Constant.Runtime.COMPUTER_DECK[0] = deck;
        }

        // AI: deck file
        else {
            Constant.Runtime.COMPUTER_DECK[0] = AllZone.getDeckManager().getDeck(ai0[0]);
        }
    }

    /** 
     * Checks lengths of selected values for AI color lists
     * to see if a deck generator exists. Alert and visual reminder if fail.
     */
    private boolean checkValidityOfAISelectedColors(String[] ai0) {
        boolean result = true;

        if (ai0.length == 1) {
            JOptionPane.showMessageDialog(null,
                    "Sorry, single color generated decks aren't supported yet."
                    + "\n\rPlease choose at least one more color for the AI deck.",
                    "AI deck: 1 color", JOptionPane.ERROR_MESSAGE);
            remind(currentAISelection);
            result = false;
        }
        else if (ai0.length == 4) {
            JOptionPane.showMessageDialog(null,
                    "Sorry, four color generated decks aren't supported yet."
                    + "\n\rPlease use 2, 3, or 5 colors for the AI deck.",
                    "AI deck: 4 colors", JOptionPane.ERROR_MESSAGE);
            remind(currentAISelection);
            result = false;
        }
        else if (ai0.length > 5) {
            JOptionPane.showMessageDialog(null,
                    "AI deck: maximum five colors!",
                    "AI deck: Too many colors", JOptionPane.ERROR_MESSAGE);
            remind(currentAISelection);
            result = false;
        }

        return result;
    }

    private void remind(JList lst0) {
        if (timer1 != null) { return; }

        final JList target = lst0;
        final int[] steps = {210, 215, 220, 220, 220, 215, 210};
        final Color oldBG = lst0.getBackground();
        counter = 0;

        ActionListener fader = new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent evt) {
                counter++;
                if (counter != (steps.length - 1)) {
                    setBackground(new Color(255, 0, 0, steps[counter]));
                }
                else {
                    target.setBackground(oldBG);
                    timer1.stop();
                    timer1 = null;
                }
            }
        };

        timer1 = new Timer(100, fader);
        timer1.start();
    }
}
