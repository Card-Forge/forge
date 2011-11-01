package forge.view.swing;

import static net.slightlymagic.braids.util.UtilFunctions.safeToString;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.border.TitledBorder;

import net.miginfocom.swing.MigLayout;

import com.esotericsoftware.minlog.Log;

import forge.AllZone;
import forge.Command;
import forge.Constant;
import forge.Constant_StringArrayList;
import forge.FileUtil;
import forge.GUI_ImportPicture;
import forge.GuiDisplay4;
import forge.GuiDownloadQuestImages;
import forge.Gui_DownloadPictures_LQ;
import forge.Gui_DownloadPrices;
import forge.Gui_DownloadSetPictures_LQ;
import forge.ImageCache;
import forge.MyRandom;
import forge.PlayerType;
import forge.Singletons;
import forge.deck.Deck;
import forge.deck.DeckGeneration;
import forge.deck.DeckManager;
import forge.error.BugzReporter;
import forge.error.ErrorViewer;
import forge.game.GameType;
import forge.game.limited.BoosterDraft_1;
import forge.game.limited.CardPoolLimitation;
import forge.game.limited.SealedDeck;
import forge.gui.GuiUtils;
import forge.gui.ListChooser;
import forge.gui.deckeditor.DeckEditorCommon;
import forge.gui.deckeditor.DeckEditorDraft;
import forge.item.CardPrinted;
import forge.item.ItemPool;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.CardSizeType;
import forge.properties.ForgePreferences.StackOffsetType;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;
import forge.properties.NewConstants.LANG.OldGuiNewGame.MENU_BAR.MENU;
import forge.properties.NewConstants.LANG.OldGuiNewGame.MENU_BAR.OPTIONS;
import forge.quest.gui.QuestOptions;

/*CHOPPIC*/

/**
 * <p>
 * OldGuiNewGame class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class OldGuiNewGame extends JFrame implements NewConstants, NewConstants.LANG.OldGuiNewGame {
    /** Constant <code>serialVersionUID=-2437047615019135648L</code>. */
    private static final long serialVersionUID = -2437047615019135648L;

    // private final DeckManager deckManager = new
    // DeckManager(ForgeProps.getFile(NEW_DECKS));
    private final DeckManager deckManager = AllZone.getDeckManager();
    // with the new IO, there's no reason to use different instances
    private List<Deck> allDecks;

    private JLabel titleLabel = new JLabel();
    private JLabel jLabel2 = new JLabel();
    private JLabel jLabel3 = new JLabel();
    private JComboBox humanComboBox = new JComboBox();
    private JComboBox computerComboBox = new JComboBox();
    private JButton deckEditorButton = new JButton();
    private JButton startButton = new JButton();
    private ButtonGroup buttonGroup1 = new ButtonGroup();
    private JRadioButton sealedRadioButton = new JRadioButton();
    private JRadioButton singleRadioButton = new JRadioButton();

    private JRadioButton draftRadioButton = new JRadioButton();

    /* CHOPPIC */
    private CustomPanel jPanel1 = new CustomPanel(10);
    private CustomPanel jPanel2 = new CustomPanel(10);
    private CustomPanel jPanel3 = new CustomPanel(10);
    /* CHOPPIC */

    // @SuppressWarnings("unused")
    // titledBorder2
    /** Constant <code>newGuiCheckBox</code>. */
    // private static JCheckBox newGuiCheckBox = new JCheckBox("", true);
    /** Constant <code>smoothLandCheckBox</code>. */
    static JCheckBox smoothLandCheckBox = new JCheckBox("", false);
    /** Constant <code>devModeCheckBox</code>. */
    static JCheckBox devModeCheckBox = new JCheckBox("", true);

    /** The upld drft check box. */
    static JCheckBox upldDrftCheckBox = new JCheckBox("", true);

    /** The foil random check box. */
    static JCheckBox foilRandomCheckBox = new JCheckBox("", true);

    // GenerateConstructedDeck.get2Colors() and GenerateSealedDeck.get2Colors()
    // use these two variables
    /** Constant <code>removeSmallCreatures</code>. */
    public static JCheckBoxMenuItem removeSmallCreatures = new JCheckBoxMenuItem(
            ForgeProps.getLocalized(MENU_BAR.OPTIONS.GENERATE.REMOVE_SMALL));

    /** Constant <code>removeArtifacts</code>. */
    public static JCheckBoxMenuItem removeArtifacts = new JCheckBoxMenuItem(
            ForgeProps.getLocalized(MENU_BAR.OPTIONS.GENERATE.REMOVE_ARTIFACTS));
    /** Constant <code>useLAFFonts</code>. */
    public static JCheckBoxMenuItem useLAFFonts = new JCheckBoxMenuItem(ForgeProps.getLocalized(MENU_BAR.OPTIONS.FONT));
    /** Constant <code>cardOverlay</code>. */
    public static JCheckBoxMenuItem cardOverlay = new JCheckBoxMenuItem(
            ForgeProps.getLocalized(MENU_BAR.OPTIONS.CARD_OVERLAY));
    /** Constant <code>cardScale</code>. */
    public static JCheckBoxMenuItem cardScale = new JCheckBoxMenuItem(
            ForgeProps.getLocalized(MENU_BAR.OPTIONS.CARD_SCALE));
    private JButton questButton = new JButton();

    private final Action LOOK_AND_FEEL_ACTION = new LookAndFeelAction(this);
    // private Action DOWNLOAD_ACTION = new DownloadAction();
    private final Action DOWNLOAD_ACTION_LQ = new DownloadActionLQ();
    private final Action DOWNLOAD_ACTION_SETLQ = new DownloadActionSetLQ();
    private final Action DOWNLOAD_ACTION_QUEST = new DownloadActionQuest();
    private final Action IMPORT_PICTURE = new ImportPictureAction();
    private final Action CARD_SIZES_ACTION = new CardSizesAction();
    private final Action CARD_STACK_ACTION = new CardStackAction();
    private final Action CARD_STACK_OFFSET_ACTION = new CardStackOffsetAction();
    private final Action ABOUT_ACTION = new AboutAction();
    private final Action HOW_TO_PLAY_ACTION = new HowToPlayAction();
    private final Action DNLD_PRICES_ACTION = new DownloadPriceAction();
    private final Action BUGZ_REPORTER_ACTION = new BugzReporterAction();
    private final Action EXIT_ACTION = new ExitAction();

    /**
     * <p>
     * Constructor for OldGuiNewGame.
     * </p>
     */
    public OldGuiNewGame() {

        AllZone.setQuestData(null);

        if (Constant.Runtime.WIDTH[0] == 0) {
            Constant.Runtime.WIDTH[0] = 300;
        }

        if (Constant.Runtime.HEIGHT[0] == 0) {
            Constant.Runtime.HEIGHT[0] = 98;
        }

        if (Constant.Runtime.STACK_SIZE[0] == 0) {
            Constant.Runtime.STACK_SIZE[0] = 4;
        }

        if (Constant.Runtime.STACK_OFFSET[0] == 0) {
            Constant.Runtime.STACK_OFFSET[0] = 10;
        }

        try {
            jbInit();
        } catch (Exception ex) {
            ErrorViewer.showError(ex);
        }

        if (Constant.Runtime.getGameType().equals(GameType.Constructed)) {
            singleRadioButton.setSelected(true);
            updateDeckComboBoxes();
        }
        if (Constant.Runtime.getGameType().equals(GameType.Sealed)) {
            sealedRadioButton.setSelected(true);
            updateDeckComboBoxes();
        }
        if (Constant.Runtime.getGameType().equals(GameType.Draft)) {
            draftRadioButton.setSelected(true);
            draftRadioButtonActionPerformed(null);
        }

        addListeners();

        this.setSize(550, 565);
        GuiUtils.centerFrame(this);

        setTitle(ForgeProps.getLocalized(LANG.PROGRAM_NAME));
        setupMenu();
        setVisible(true);

        Log.WARN(); // set logging level to warn
        SwingUtilities.updateComponentTreeUI(this);
    }

    // init()

    /**
     * <p>
     * setupMenu.
     * </p>
     */
    private void setupMenu() {
        Action[] actions = {
                // Remove the option to download HQ pics since the HQ pics
                // server appears to be offline.
                // LOOK_AND_FEEL_ACTION, DNLD_PRICES_ACTION, DOWNLOAD_ACTION,
                // DOWNLOAD_ACTION_LQ, DOWNLOAD_ACTION_SETLQ, IMPORT_PICTURE,
                // CARD_SIZES_ACTION,
                LOOK_AND_FEEL_ACTION, DNLD_PRICES_ACTION, DOWNLOAD_ACTION_LQ, DOWNLOAD_ACTION_SETLQ,
                DOWNLOAD_ACTION_QUEST, IMPORT_PICTURE, CARD_SIZES_ACTION, CARD_STACK_ACTION, CARD_STACK_OFFSET_ACTION,
                BUGZ_REPORTER_ACTION, ErrorViewer.ALL_THREADS_ACTION, ABOUT_ACTION, EXIT_ACTION };
        JMenu menu = new JMenu(ForgeProps.getLocalized(MENU.TITLE));
        for (Action a : actions) {
            menu.add(a);
            if (a.equals(LOOK_AND_FEEL_ACTION) || a.equals(IMPORT_PICTURE) || a.equals(CARD_STACK_OFFSET_ACTION)
                    || a.equals(ErrorViewer.ALL_THREADS_ACTION)) {
                menu.addSeparator();
            }
        }

        // useLAFFonts.setSelected(false);

        // new stuff
        JMenu generatedDeck = new JMenu(ForgeProps.getLocalized(MENU_BAR.OPTIONS.GENERATE.TITLE));

        generatedDeck.add(removeSmallCreatures);
        removeSmallCreatures.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent arg0) {
                Singletons.getModel().getPreferences().deckGenRmvSmall = removeSmallCreatures.isSelected();
            }
        });

        generatedDeck.add(removeArtifacts);
        removeArtifacts.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent arg0) {
                Singletons.getModel().getPreferences().deckGenRmvArtifacts = removeArtifacts.isSelected();
            }
        });

        JMenu optionsMenu = new JMenu(ForgeProps.getLocalized(OPTIONS.TITLE));
        optionsMenu.add(generatedDeck);

        optionsMenu.add(useLAFFonts);
        optionsMenu.addSeparator();
        optionsMenu.add(cardOverlay);
        optionsMenu.add(cardScale);

        cardScale.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent arg0) {
                ImageCache.setScaleLargerThanOriginal(cardScale.isSelected());
            }
        });

        JMenu helpMenu = new JMenu(ForgeProps.getLocalized(MENU_BAR.HELP.TITLE));

        Action[] helpActions = {HOW_TO_PLAY_ACTION};
        for (Action a : helpActions) {
            helpMenu.add(a);
        }

        JMenuBar bar = new JMenuBar();
        bar.add(menu);
        bar.add(optionsMenu);
        bar.add(helpMenu);
        // bar.add(new MenuItem_HowToPlay());

        setJMenuBar(bar);
    }

    // returns, ArrayList of Deck objects
    /**
     * <p>
     * getDecks.
     * </p>
     * 
     * @return a {@link java.util.List} object.
     */
    private List<Deck> getDecks() {
        List<Deck> list = new ArrayList<Deck>(deckManager.getDecks());

        Collections.sort(list);
        return list;
    }

    /**
     * <p>
     * addListeners.
     * </p>
     */
    private void addListeners() {
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent ev) {
                dispose();
                System.exit(0);
            }
        });

        questButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                // close this windows
                // can't use this.dispose() because "this" object is an
                // ActionListener
                OldGuiNewGame.this.dispose();

                new QuestOptions();
            }
        });
    } // addListeners()

    /**
     * <p>
     * setupSealed.
     * </p>
     */
    private void setupSealed() {
        Deck deck = new Deck(GameType.Sealed);

        // ReadBoosterPack booster = new ReadBoosterPack();
        // CardList pack = booster.getBoosterPack5();

        ArrayList<String> sealedTypes = new ArrayList<String>();
        sealedTypes.add("Full Cardpool");
        sealedTypes.add("Block / Set");
        sealedTypes.add("Custom");

        String prompt = "Choose Sealed Deck Format:";
        Object o = GuiUtils.getChoice(prompt, sealedTypes.toArray());

        SealedDeck sd = null;

        if (o.toString().equals(sealedTypes.get(0))) {
            sd = new SealedDeck("Full");
        }

        else if (o.toString().equals(sealedTypes.get(1))) {
            sd = new SealedDeck("Block");
        }

        else if (o.toString().equals(sealedTypes.get(2))) {
            sd = new SealedDeck("Custom");
        }

        else {
            throw new IllegalStateException("choice <<" + safeToString(o)
                    + ">> does not equal any of the sealedTypes.");
        }

        ItemPool<CardPrinted> sDeck = sd.getCardpool();

        if (sDeck.countAll() > 1) {

            deck.addSideboard(sDeck);

            for (int i = 0; i < Constant.Color.BASIC_LANDS.length; i++) {
                for (int j = 0; j < 18; j++) {
                    deck.addSideboard(Constant.Color.BASIC_LANDS[i] + "|" + sd.getLandSetCode()[0]);
                }
            }

            String sDeckName = JOptionPane.showInputDialog(null,
                    ForgeProps.getLocalized(NEW_GAME_TEXT.SAVE_SEALED_MSG),
                    ForgeProps.getLocalized(NEW_GAME_TEXT.SAVE_SEALED_TTL), JOptionPane.QUESTION_MESSAGE);

            deck.setName(sDeckName);
            deck.setPlayerType(PlayerType.HUMAN);

            Constant.Runtime.HUMAN_DECK[0] = deck;
            Constant.Runtime.setGameType(GameType.Sealed);

            // Deck aiDeck = sd.buildAIDeck(sDeck.toForgeCardList());
            Deck aiDeck = sd.buildAIDeck(sd.getCardpool().toForgeCardList()); // AI
                                                                              // will
                                                                              // use
                                                                              // different
                                                                              // cardpool
            aiDeck.setName("AI_" + sDeckName);
            aiDeck.setPlayerType(PlayerType.COMPUTER);
            deckManager.addDeck(aiDeck);
            DeckManager.writeDeck(aiDeck, DeckManager.makeFileName(aiDeck));
            updateDeckComboBoxes();

            deckEditorButtonActionPerformed(GameType.Sealed, deck);

            Constant.Runtime.COMPUTER_DECK[0] = aiDeck;
        } else {
            new OldGuiNewGame();
        }
    }

    /**
     * <p>
     * setupDraft.
     * </p>
     */
    private void setupDraft() {
        DeckEditorDraft draft = new DeckEditorDraft();

        // determine what kind of booster draft to run
        ArrayList<String> draftTypes = new ArrayList<String>();
        draftTypes.add("Full Cardpool");
        draftTypes.add("Block / Set");
        draftTypes.add("Custom");

        String prompt = "Choose Draft Format:";
        Object o = GuiUtils.getChoice(prompt, draftTypes.toArray());

        if (o.toString().equals(draftTypes.get(0))) {
            draft.showGui(new BoosterDraft_1(CardPoolLimitation.Full));
        }

        else if (o.toString().equals(draftTypes.get(1))) {
            draft.showGui(new BoosterDraft_1(CardPoolLimitation.Block));
        }

        else if (o.toString().equals(draftTypes.get(2))) {
            draft.showGui(new BoosterDraft_1(CardPoolLimitation.Custom));
        }

    }

    /**
     * <p>
     * jbInit.
     * </p>
     * 
     * @throws java.lang.Exception
     *             if any.
     */
    private void jbInit() throws Exception {

        titleLabel.setText(ForgeProps.getLocalized(NEW_GAME_TEXT.NEW_GAME));
        titleLabel.setFont(new java.awt.Font("Dialog", 0, 26));

        /* CHOPPIC */
        titleLabel.setForeground(Color.WHITE);
        /* CHOPPIC */

        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);

        this.getContentPane().setLayout(new MigLayout("fill"));

        /*
         * Game Type Panel
         */

        /* jPanel2.setBorder(titledBorder1); */
        setCustomBorder(jPanel2, ForgeProps.getLocalized(NEW_GAME_TEXT.GAMETYPE));
        jPanel2.setLayout(new MigLayout("align center"));

        singleRadioButton.setText(ForgeProps.getLocalized(NEW_GAME_TEXT.CONSTRUCTED_TEXT));
        singleRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                singleRadioButtonActionPerformed(e);
            }
        });

        // sealedRadioButton.setToolTipText("");
        sealedRadioButton.setText(ForgeProps.getLocalized(NEW_GAME_TEXT.SEALED_TEXT));
        sealedRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                sealedRadioButtonActionPerformed(e);
            }
        });

        // draftRadioButton.setToolTipText("");
        draftRadioButton.setText(ForgeProps.getLocalized(NEW_GAME_TEXT.BOOSTER_TEXT));
        draftRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                draftRadioButtonActionPerformed(e);
            }
        });

        /*
         * Library Panel
         */

        /* jPanel1.setBorder(titledBorder2); */
        setCustomBorder(jPanel1, ForgeProps.getLocalized(NEW_GAME_TEXT.LIBRARY));
        jPanel1.setLayout(new MigLayout("align center"));

        jLabel2.setText(ForgeProps.getLocalized(NEW_GAME_TEXT.YOURDECK));
        jLabel3.setText(ForgeProps.getLocalized(NEW_GAME_TEXT.OPPONENT));

        /*
         * Settings Panel
         */

        /* jPanel3.setBorder(titledBorder3); */
        setCustomBorder(jPanel3, ForgeProps.getLocalized(NEW_GAME_TEXT.SETTINGS));
        jPanel3.setLayout(new MigLayout("align center"));

        // newGuiCheckBox.setText(ForgeProps.getLocalized(NEW_GAME_TEXT.NEW_GUI));
        smoothLandCheckBox.setText(ForgeProps.getLocalized(NEW_GAME_TEXT.AI_LAND));

        devModeCheckBox.setText(ForgeProps.getLocalized(NEW_GAME_TEXT.DEV_MODE));
        devModeCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                Constant.Runtime.DEV_MODE[0] = devModeCheckBox.isSelected();
                Singletons.getModel().getPreferences().developerMode = Constant.Runtime.DEV_MODE[0];
            }
        });

        upldDrftCheckBox.setText("Upload Draft Picks");

        upldDrftCheckBox.setToolTipText("Your picks and all other participants' picks will help the Forge AI"
                + " make better draft picks.");

        upldDrftCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                Constant.Runtime.UPLOAD_DRAFT[0] = upldDrftCheckBox.isSelected();
                Singletons.getModel().getPreferences().uploadDraftAI = Constant.Runtime.UPLOAD_DRAFT[0];
            }
        });

        foilRandomCheckBox.setText("Random Foiling");
        foilRandomCheckBox.setToolTipText("Approximately 1:20 cards will appear with foiling effects applied.");
        foilRandomCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                Constant.Runtime.RANDOM_FOIL[0] = foilRandomCheckBox.isSelected();
                Singletons.getModel().getPreferences().randCFoil = Constant.Runtime.RANDOM_FOIL[0];
            }
        });

        /*
         * Buttons
         */

        deckEditorButton.setFont(new java.awt.Font("Dialog", 0, 15));
        deckEditorButton.setText(ForgeProps.getLocalized(NEW_GAME_TEXT.DECK_EDITOR));
        deckEditorButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                deckEditorButtonActionPerformed(GameType.Constructed, null);
            }
        });

        startButton.setFont(new java.awt.Font("Dialog", 0, 18));
        startButton.setHorizontalTextPosition(SwingConstants.LEADING);
        startButton.setText(ForgeProps.getLocalized(NEW_GAME_TEXT.START_GAME));
        startButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                startButtonActionPerformed(e);
            }
        });

        questButton.setFont(new java.awt.Font("Dialog", 0, 18));
        questButton.setText(ForgeProps.getLocalized(NEW_GAME_TEXT.QUEST_MODE));

        this.getContentPane().add(titleLabel, "align center, span 3, grow, wrap");

        this.getContentPane().add(jPanel2, "span 3, grow, wrap");
        jPanel2.add(singleRadioButton, "span 3, wrap");
        jPanel2.add(sealedRadioButton, "span 3, wrap");
        jPanel2.add(draftRadioButton, "span 3, wrap");
        updatePanelDisplay(jPanel2);

        this.getContentPane().add(jPanel1, "span 2, grow");
        jPanel1.add(jLabel2);
        jPanel1.add(humanComboBox, "sg combobox, wrap");
        jPanel1.add(jLabel3);
        jPanel1.add(computerComboBox, "sg combobox");
        updatePanelDisplay(jPanel1);

        this.getContentPane().add(deckEditorButton, "sg buttons, align 50% 50%, wrap");

        this.getContentPane().add(jPanel3, "span 2, grow");

        // jPanel3.add(newGuiCheckBox, "wrap");
        jPanel3.add(smoothLandCheckBox, "wrap");
        jPanel3.add(devModeCheckBox, "wrap");
        jPanel3.add(upldDrftCheckBox, "wrap");
        jPanel3.add(foilRandomCheckBox, "wrap");
        updatePanelDisplay(jPanel3);

        this.getContentPane().add(startButton, "sg buttons, align 50% 50%, split 2, flowy");
        this.getContentPane().add(questButton, "sg buttons, align 50% 50%");

        buttonGroup1.add(singleRadioButton);
        buttonGroup1.add(sealedRadioButton);
        buttonGroup1.add(draftRadioButton);

        /* CHOPPIC */
        /* Add background image */
        ((JPanel) getContentPane()).setOpaque(false);
        ImageIcon bkgd = new ImageIcon("res/images/ui/newgame_background.jpg");
        JLabel myLabel = new JLabel(bkgd);

        // Do not pass Integer.MIN_VALUE directly here; it must be packaged in
        // an Integer
        // instance. Otherwise, GUI components will not draw unless moused over.
        getLayeredPane().add(myLabel, Integer.valueOf(Integer.MIN_VALUE));

        myLabel.setBounds(0, 0, bkgd.getIconWidth(), bkgd.getIconHeight());
        /* CHOPPIC */

    }

    /* CHOPPIC */
    /* Update Panel Display */
    /**
     * <p>
     * updatePanelDisplay.
     * </p>
     * 
     * @param panel
     *            a {@link javax.swing.JPanel} object.
     */
    final void updatePanelDisplay(final JPanel panel) {
        for (Component c : panel.getComponents()) {
            if (c instanceof JRadioButton) {
                ((JRadioButton) c).setOpaque(false);
            } else if (c instanceof JLabel) {
                ((JLabel) c).setOpaque(false);
            } else if (c instanceof JCheckBox) {
                ((JCheckBox) c).setOpaque(false);
            }
        }
        panel.setOpaque(false);
    }

    /**
     * <p>
     * setCustomBorder.
     * </p>
     * 
     * @param panel
     *            a {@link javax.swing.JPanel} object.
     * @param title
     *            a {@link java.lang.String} object.
     */
    final void setCustomBorder(final JPanel panel, final String title) {
        TitledBorder tb = BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), title);
        tb.setTitlePosition(TitledBorder.ABOVE_TOP);
        tb.setTitleFont(new java.awt.Font("Dialog", 0, 12));
        tb.setTitleColor(Color.BLUE);
        panel.setBorder(tb);
    }

    /* CHOPPIC */

    /**
     * <p>
     * deckEditorButton_actionPerformed.
     * </p>
     * 
     * @param gt
     *            the gt
     * @param deck
     *            the deck
     */
    final void deckEditorButtonActionPerformed(final GameType gt, final Deck deck) {

        DeckEditorCommon editor = new DeckEditorCommon(gt);

        Command exit = new Command() {
            private static final long serialVersionUID = -9133358399503226853L;

            public void execute() {

                updateDeckComboBoxes();
                OldGuiNewGame.this.setVisible(true);
            }
        };

        editor.show(exit);

        if (deck != null) {
            editor.getCustomMenu().showDeck(deck, gt);
        }

        this.setVisible(false);
        editor.setVisible(true);
    }

    /**
     * <p>
     * getRandomDeck.
     * </p>
     * 
     * @param d
     *            an array of {@link forge.deck.Deck} objects.
     * @return a {@link forge.deck.Deck} object.
     */
    final Deck getRandomDeck(final Deck[] d) {
        // get a random number between 0 and d.length
        // int i = (int) (Math.random() * d.length);
        Random r = MyRandom.getRandom();

        return d[r.nextInt(d.length)];
    }

    /**
     * <p>
     * startButton_actionPerformed.
     * </p>
     * 
     * @param e
     *            a {@link java.awt.event.ActionEvent} object.
     */
    final void startButtonActionPerformed(final ActionEvent e) {
        if (humanComboBox.getSelectedItem() == null || computerComboBox.getSelectedItem() == null) {
            return;
        }

        String human = humanComboBox.getSelectedItem().toString();

        String computer = null;
        if (computerComboBox.getSelectedItem() != null) {
            computer = computerComboBox.getSelectedItem().toString();
        }

        if (draftRadioButton.isSelected()) {
            if (human.equals("New Draft")) {
                dispose();
                setupDraft();
                return;

            } else {
                // load old draft
                Deck[] deck = deckManager.getDraftDeck(human);
                int index = Integer.parseInt(computer);

                Constant.Runtime.HUMAN_DECK[0] = deck[0];
                Constant.Runtime.COMPUTER_DECK[0] = deck[index];

                if (Constant.Runtime.COMPUTER_DECK[0] == null) {
                    throw new IllegalStateException("OldGuiNewGame : startButton() error - computer deck is null");
                }
            } // else - load old draft
        } // if
        else if (sealedRadioButton.isSelected()) {
            if (human.equals("New Sealed")) {
                dispose();

                setupSealed();

                return;
            } else {
                Constant.Runtime.HUMAN_DECK[0] = deckManager.getDeck(human);

            }

            if (!computer.equals("New Sealed")) {
                Constant.Runtime.COMPUTER_DECK[0] = deckManager.getDeck(computer);
            }
        } else {
            // non-draft decks
            GameType format = Constant.Runtime.getGameType();
            // boolean sealed = GameType.Sealed.equals(format);
            boolean constructed = GameType.Constructed.equals(format);

            boolean humanGenerate = human.equals("Generate Deck");
            boolean humanRandom = human.equals("Random");

            if (humanGenerate) {
                if (constructed) {
                    DeckGeneration.genDecks(PlayerType.HUMAN);
                }
                // else if(sealed)
                // Constant.Runtime.HumanDeck[0] = generateSealedDeck();
            } else if (humanRandom) {
                Constant.Runtime.HUMAN_DECK[0] = getRandomDeck(getDecks(format));

                JOptionPane.showMessageDialog(null,
                        String.format("You are using deck: %s", Constant.Runtime.HUMAN_DECK[0].getName()), "Deck Name",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                Constant.Runtime.HUMAN_DECK[0] = deckManager.getDeck(human);
            }

            assert computer != null;
            boolean computerGenerate = computer.equals("Generate Deck");
            boolean computerRandom = computer.equals("Random");

            if (computerGenerate) {
                if (constructed) {
                    DeckGeneration.genDecks(PlayerType.COMPUTER);
                } // Constant.Runtime.ComputerDeck[0] =
                  // generateConstructedDeck();
                  // else if(sealed)
                  // Constant.Runtime.ComputerDeck[0] = generateSealedDeck();
            } else if (computerRandom) {
                Constant.Runtime.COMPUTER_DECK[0] = getRandomDeck(getDecks(format));

                JOptionPane.showMessageDialog(null,
                        String.format("The computer is using deck: %s", Constant.Runtime.COMPUTER_DECK[0].getName()),
                        "Deck Name", JOptionPane.INFORMATION_MESSAGE);
            } else {
                Constant.Runtime.COMPUTER_DECK[0] = deckManager.getDeck(computer);
            }
        } // else

        // DO NOT CHANGE THIS ORDER, GuiDisplay needs to be created before cards
        // are added
        // Constant.Runtime.DevMode[0] = devModeCheckBox.isSelected();

        // if (newGuiCheckBox.isSelected())
        AllZone.setDisplay(new GuiDisplay4());
        // else AllZone.setDisplay(new GuiDisplay3());

        Constant.Runtime.SMOOTH[0] = smoothLandCheckBox.isSelected();

        AllZone.getGameAction().newGame(Constant.Runtime.HUMAN_DECK[0], Constant.Runtime.COMPUTER_DECK[0]);
        AllZone.getDisplay().setVisible(true);

        dispose();
    } // startButton_actionPerformed()

    /**
     * <p>
     * singleRadioButton_actionPerformed.
     * </p>
     * 
     * @param e
     *            a {@link java.awt.event.ActionEvent} object.
     */
    final void singleRadioButtonActionPerformed(final ActionEvent e) {
        Constant.Runtime.setGameType(GameType.Constructed);
        updateDeckComboBoxes();
    }

    /**
     * <p>
     * sealedRadioButton_actionPerformed.
     * </p>
     * 
     * @param e
     *            a {@link java.awt.event.ActionEvent} object.
     */
    final void sealedRadioButtonActionPerformed(final ActionEvent e) {
        Constant.Runtime.setGameType(GameType.Sealed);
        updateDeckComboBoxes();
    }

    /**
     * <p>
     * updateDeckComboBoxes.
     * </p>
     */
    private void updateDeckComboBoxes() {
        humanComboBox.removeAllItems();
        computerComboBox.removeAllItems();

        allDecks = getDecks();
        switch (Constant.Runtime.getGameType()) {
        case Sealed:
            humanComboBox.addItem("New Sealed");
            computerComboBox.addItem("New Sealed");

            for (Deck allDeck : allDecks) {
                if (allDeck.getDeckType().equals(GameType.Sealed)) {
                    JComboBox boxToAdd = allDeck.getPlayerType() == PlayerType.COMPUTER ? computerComboBox
                            : humanComboBox;
                    boxToAdd.addItem(allDeck.getName());
                }
            } // for
            break;
        case Constructed:
            humanComboBox.addItem("Generate Deck");
            computerComboBox.addItem("Generate Deck");

            humanComboBox.addItem("Random");
            computerComboBox.addItem("Random");

            for (Deck allDeck : allDecks) {
                if (allDeck.getDeckType().equals(GameType.Constructed)) {
                    humanComboBox.addItem(allDeck.getName());
                    computerComboBox.addItem(allDeck.getName());
                }
            } // for
            break;
        case Draft:
            humanComboBox.addItem("New Draft");
            Object[] key = deckManager.getDraftDecks().keySet().toArray();
            Arrays.sort(key);

            for (Object aKey : key) {
                humanComboBox.addItem(aKey);
            }

            for (int i = 0; i < 7; i++) {
                computerComboBox.addItem("" + (i + 1));
            }
            break;
        default:
            break;
        }
        // not sure if the code below is useful or not
        // this will select the deck that you previously used

        // if(Constant.Runtime.HumanDeck[0] != null)
        // humanComboBox.setSelectedItem(Constant.Runtime.HumanDeck[0].getName());

    } /* updateComboBoxes() */

    /**
     * <p>
     * getDecks.
     * </p>
     * 
     * @param gameType
     *            a {@link java.lang.String} object.
     * @return an array of {@link forge.deck.Deck} objects.
     */
    final Deck[] getDecks(final GameType gameType) {
        ArrayList<Deck> list = new ArrayList<Deck>();

        Deck d;
        for (Deck allDeck : deckManager.getDecks()) {
            d = allDeck;

            if (d.getDeckType().equals(gameType)) {
                list.add(d);
            }
        } // for

        // convert ArrayList to Deck[]
        Deck[] out = new Deck[list.size()];
        list.toArray(out);

        return out;
    } // getDecks()

    /**
     * Draft radio button action performed.
     * 
     * @param e
     *            the e
     */
    final void draftRadioButtonActionPerformed(final ActionEvent e) {
        Constant.Runtime.setGameType(GameType.Draft);
        updateDeckComboBoxes();
    }

    /**
     * The Class LookAndFeelAction.
     * 
     * @author dhudson
     */
    public static class LookAndFeelAction extends AbstractAction {

        private static final long serialVersionUID = -4447498333866711215L;
        private Component c;

        /**
         * Instantiates a new look and feel action.
         * 
         * @param component
         *            the component
         */
        public LookAndFeelAction(final Component component) {
            super(ForgeProps.getLocalized(MENU_BAR.MENU.LF));
            this.c = component;
        }

        /**
         * Action performed.
         * 
         * @param e
         *            the e
         */
        public final void actionPerformed(final ActionEvent e) {
            LookAndFeelInfo[] info = UIManager.getInstalledLookAndFeels();
            HashMap<String, String> LAFMap = new HashMap<String, String>();
            for (LookAndFeelInfo anInfo : info) {
                LAFMap.put(anInfo.getName(), anInfo.getClassName());
            }

            // add Substance LAFs:
            LAFMap.put("Autumn", "org.pushingpixels.substance.api.skin.SubstanceAutumnLookAndFeel");
            LAFMap.put("Business", "org.pushingpixels.substance.api.skin.SubstanceBusinessLookAndFeel");
            LAFMap.put("Business Black Steel",
                    "org.pushingpixels.substance.api.skin.SubstanceBusinessBlackSteelLookAndFeel");
            LAFMap.put("Business Blue Steel",
                    "org.pushingpixels.substance.api.skin.SubstanceBusinessBlueSteelLookAndFeel");
            LAFMap.put("Challenger Deep", "org.pushingpixels.substance.api.skin.SubstanceChallengerDeepLookAndFeel");
            LAFMap.put("Creme", "org.pushingpixels.substance.api.skin.SubstanceCremeLookAndFeel");
            LAFMap.put("Creme Coffee", "org.pushingpixels.substance.api.skin.SubstanceCremeCoffeeLookAndFeel");
            LAFMap.put("Dust", "org.pushingpixels.substance.api.skin.SubstanceDustLookAndFeel");
            LAFMap.put("Dust Coffee", "org.pushingpixels.substance.api.skin.SubstanceDustCoffeeLookAndFeel");
            LAFMap.put("Emerald Dusk", "org.pushingpixels.substance.api.skin.SubstanceEmeraldDuskLookAndFeel");
            LAFMap.put("Gemini", "org.pushingpixels.substance.api.skin.SubstanceGeminiLookAndFeel");
            LAFMap.put("Graphite", "org.pushingpixels.substance.api.skin.SubstanceGraphiteLookAndFeel");
            LAFMap.put("Graphite Aqua", "org.pushingpixels.substance.api.skin.SubstanceGraphiteAquaLookAndFeel");
            LAFMap.put("Graphite Glass", "org.pushingpixels.substance.api.skin.SubstanceGraphiteGlassLookAndFeel");
            LAFMap.put("Magma", "org.pushingpixels.substance.api.skin.SubstanceMagmaLookAndFeel");
            LAFMap.put("Magellan", "org.pushingpixels.substance.api.skin.SubstanceMagellanLookAndFeel");
            // LAFMap.put("Mariner",
            // "org.pushingpixels.substance.api.skin.SubstanceMarinerLookAndFeel");
            LAFMap.put("Mist Aqua", "org.pushingpixels.substance.api.skin.SubstanceMistAquaLookAndFeel");
            LAFMap.put("Mist Silver", "org.pushingpixels.substance.api.skin.SubstanceMistSilverLookAndFeel");
            LAFMap.put("Moderate", "org.pushingpixels.substance.api.skin.SubstanceModerateLookAndFeel");
            LAFMap.put("Nebula", "org.pushingpixels.substance.api.skin.SubstanceNebulaLookAndFeel");
            LAFMap.put("Nebula Brick Wall", "org.pushingpixels.substance.api.skin.SubstanceNebulaBrickWallLookAndFeel");
            // LAFMap.put("Office Black 2007",
            // "org.pushingpixels.substance.api.skin.SubstanceOfficeBlack2007LookAndFeel");
            LAFMap.put("Office Blue 2007", "org.pushingpixels.substance.api.skin.SubstanceOfficeBlue2007LookAndFeel");
            LAFMap.put("Office Silver 2007",
                    "org.pushingpixels.substance.api.skin.SubstanceOfficeSilver2007LookAndFeel");
            LAFMap.put("Raven", "org.pushingpixels.substance.api.skin.SubstanceRavenLookAndFeel");
            LAFMap.put("Raven Graphite", "org.pushingpixels.substance.api.skin.SubstanceRavenGraphiteLookAndFeel");
            // LAFMap.put("Raven Graphite Glass",
            // "org.pushingpixels.substance.api.skin.SubstanceRavenGraphiteGlassLookAndFeel");
            LAFMap.put("Sahara", "org.pushingpixels.substance.api.skin.SubstanceSaharaLookAndFeel");
            LAFMap.put("Twilight", "org.pushingpixels.substance.api.skin.SubstanceTwilightLookAndFeel");

            String[] keys = new String[LAFMap.size()];
            int count = 0;

            for (String s1 : LAFMap.keySet()) {
                keys[count++] = s1;
            }
            Arrays.sort(keys);

            ListChooser<String> ch = new ListChooser<String>("Choose one", 0, 1, keys);
            if (ch.show()) {
                try {
                    String name = ch.getSelectedValue();
                    int index = ch.getSelectedIndex();
                    if (index == -1) {
                        return;
                    }
                    // UIManager.setLookAndFeel(info[index].getClassName());
                    Singletons.getModel().getPreferences().laf = LAFMap.get(name);
                    UIManager.setLookAndFeel(LAFMap.get(name));

                    SwingUtilities.updateComponentTreeUI(c);
                } catch (Exception ex) {
                    ErrorViewer.showError(ex);
                }
            }
        }
    }

    /**
     * The Class DownloadPriceAction.
     * 
     * @author dhudson
     */
    public static class DownloadPriceAction extends AbstractAction {
        private static final long serialVersionUID = 929877827872974298L;

        /**
         * Instantiates a new download price action.
         */
        public DownloadPriceAction() {
            super(ForgeProps.getLocalized(MENU_BAR.MENU.DOWNLOADPRICE));
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent
         * )
         */
        /**
         * @param e ActionEvent
         */
        public final void actionPerformed(final ActionEvent e) {
            Gui_DownloadPrices gdp = new Gui_DownloadPrices();
            gdp.setVisible(true);
        }
    }

    /**
     * The Class BugzReporterAction.
     * 
     * @author dhudson
     */
    public static class BugzReporterAction extends AbstractAction {

        /**
         *
         */
        private static final long serialVersionUID = 6354047838575733085L;

        /**
         * Instantiates a new bugz reporter action.
         */
        public BugzReporterAction() {
            super("Report Bug");
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent
         * )
         */
        /**
         * @param e ActionEvent
         */
        public final void actionPerformed(final ActionEvent e) {
            BugzReporter br = new BugzReporter();
            br.setVisible(true);
        }

    }

    /*
     * public static class DownloadAction extends AbstractAction {
     * 
     * private static final long serialVersionUID = 6564425021778307101L;
     * 
     * public DownloadAction() {
     * super(ForgeProps.getLocalized(MENU_BAR.MENU.DOWNLOAD)); }
     * 
     * public void actionPerformed(ActionEvent e) {
     * 
     * Gui_DownloadPictures.startDownload(null); } }
     */
    /**
     * The Class DownloadActionLQ.
     * 
     * @author dhudson
     */
    public static class DownloadActionLQ extends AbstractAction {

        private static final long serialVersionUID = -6234380664413874813L;

        /**
         * Instantiates a new download action lq.
         */
        public DownloadActionLQ() {
            super(ForgeProps.getLocalized(MENU_BAR.MENU.DOWNLOADLQ));
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent
         * )
         */
        /**
         * @param e ActionEvent
         */
        public final void actionPerformed(final ActionEvent e) {
            new Gui_DownloadPictures_LQ(null);
        }
    }

    /**
     * The Class DownloadActionSetLQ.
     * 
     * @author dhudson
     */
    public static class DownloadActionSetLQ extends AbstractAction {
        private static final long serialVersionUID = 2947202546752930L;

        /**
         * Instantiates a new download action set lq.
         */
        public DownloadActionSetLQ() {
            super(ForgeProps.getLocalized(MENU_BAR.MENU.DOWNLOADSETLQ));
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent
         * )
         */
        /**
         * @param e ActionEvent
         */
        public final void actionPerformed(final ActionEvent e) {
            new Gui_DownloadSetPictures_LQ(null);
        }
    }

    /**
     * The Class DownloadActionQuest.
     * 
     * @author slapshot5
     */
    public static class DownloadActionQuest extends AbstractAction {
        private static final long serialVersionUID = -4439763134551377894L;

        /**
         * Instantiates a new download action quest.
         */
        public DownloadActionQuest() {
            super(ForgeProps.getLocalized(MENU_BAR.MENU.DOWNLOADQUESTIMG));
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent
         * )
         */
        /**
         * @param e ActionEvent
         */
        public final void actionPerformed(final ActionEvent e) {
            // GuiDownloadQuestImages.startDownload(null);
            new GuiDownloadQuestImages(null);
        }
    }

    /**
     * The Class ImportPictureAction.
     * 
     * @author dhudson
     */
    public static class ImportPictureAction extends AbstractAction {

        private static final long serialVersionUID = 6893292814498031508L;

        /**
         * Instantiates a new import picture action.
         */
        public ImportPictureAction() {
            super(ForgeProps.getLocalized(MENU_BAR.MENU.IMPORTPICTURE));
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent
         * )
         */
        /**
         * @param e ActionEvent
         */
        public final void actionPerformed(final ActionEvent e) {
            GUI_ImportPicture ip = new GUI_ImportPicture(null);
            ip.setVisible(true);
        }
    }

    /**
     * The Class CardSizesAction.
     * 
     * @author dhudson
     */
    public static class CardSizesAction extends AbstractAction {

        private static final long serialVersionUID = -2900235618450319571L;
        private static String[] keys = { "Tiny", "Smaller", "Small", "Medium", "Large", "Huge" };
        private static int[] widths = { 52, 80, 120, 200, 300, 400 };

        /**
         * Instantiates a new card sizes action.
         */
        public CardSizesAction() {
            super(ForgeProps.getLocalized(MENU_BAR.MENU.CARD_SIZES));
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent
         * )
         */
        /**
         * @param e ActionEvent
         */
        public final void actionPerformed(final ActionEvent e) {
            ListChooser<String> ch = new ListChooser<String>("Choose one", "Choose a new max card size", 0, 1, keys);
            if (ch.show()) {
                try {
                    int index = ch.getSelectedIndex();
                    if (index == -1) {
                        return;
                    }
                    set(index);
                } catch (Exception ex) {
                    ErrorViewer.showError(ex);
                }
            }
        }

        /**
         * Sets the.
         * 
         * @param index
         *            the index
         */
        public static void set(final int index) {
            Singletons.getModel().getPreferences().cardSize = CardSizeType.valueOf(keys[index].toLowerCase());
            Constant.Runtime.WIDTH[0] = widths[index];
            Constant.Runtime.HEIGHT[0] = (int) Math.round((widths[index] * (3.5 / 2.5)));
        }

        /**
         * Sets the.
         * 
         * @param s
         *            the s
         */
        public static void set(final CardSizeType s) {
            Singletons.getModel().getPreferences().cardSize = s;
            int index = 0;
            for (String str : keys) {
                if (str.toLowerCase().equals(s.toString())) {
                    break;
                }
                index++;
            }
            Constant.Runtime.WIDTH[0] = widths[index];
            Constant.Runtime.HEIGHT[0] = (int) Math.round((widths[index] * (3.5 / 2.5)));
        }
    }

    /**
     * The Class CardStackAction.
     * 
     * @author dhudson
     */
    public static class CardStackAction extends AbstractAction {

        private static final long serialVersionUID = -3770527681359311455L;
        private static String[] keys = {"3", "4", "5", "6", "7", "8", "9", "10", "11", "12"};
        private static int[] values = { 3, 4, 5, 6, 7, 8, 9, 10, 11, 12 };

        /**
         * Instantiates a new card stack action.
         */
        public CardStackAction() {
            super(ForgeProps.getLocalized(MENU_BAR.MENU.CARD_STACK));
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent
         * )
         */
        /**
         * @param e ActionEvent
         */
        public final void actionPerformed(final ActionEvent e) {

            ListChooser<String> ch
            = new ListChooser<String>("Choose one", "Choose the max size of a stack", 0, 1, keys);

            if (ch.show()) {
                try {
                    int index = ch.getSelectedIndex();
                    if (index == -1) {
                        return;
                    }
                    set(index);

                } catch (Exception ex) {
                    ErrorViewer.showError(ex);
                }
            }
        }

        /**
         * Sets the.
         * 
         * @param index
         *            the index
         */
        public static void set(final int index) {
            Singletons.getModel().getPreferences().maxStackSize = values[index];
            Constant.Runtime.STACK_SIZE[0] = values[index];
        }

        /**
         * Sets the val.
         * 
         * @param val
         *            the new val
         */
        public static void setVal(final int val) {
            Singletons.getModel().getPreferences().maxStackSize = val;
            Constant.Runtime.STACK_SIZE[0] = val;
        }
    }

    /**
     * The Class CardStackOffsetAction.
     * 
     * @author dhudson
     */
    public static class CardStackOffsetAction extends AbstractAction {

        private static final long serialVersionUID = 5021304777748833975L;
        private static String[] keys = {"Tiny", "Small", "Medium", "Large"};
        private static int[] offsets = { 5, 7, 10, 15 };

        /**
         * Instantiates a new card stack offset action.
         */
        public CardStackOffsetAction() {
            super(ForgeProps.getLocalized(MENU_BAR.MENU.CARD_STACK_OFFSET));
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent
         * )
         */
        /**
         * @param e ActionEvent
         */
        public final void actionPerformed(final ActionEvent e) {
            ListChooser<String> ch = new ListChooser<String>("Choose one", "Choose a stack offset value", 0, 1, keys);
            if (ch.show()) {
                try {
                    int index = ch.getSelectedIndex();
                    if (index == -1) {
                        return;
                    }
                    set(index);

                } catch (Exception ex) {
                    ErrorViewer.showError(ex);
                }
            }
        }

        /**
         * Sets the.
         * 
         * @param index
         *            the index
         */
        public static void set(final int index) {
            Singletons.getModel().getPreferences().stackOffset = StackOffsetType.valueOf(keys[index].toLowerCase());
            Constant.Runtime.STACK_OFFSET[0] = offsets[index];
        }

        /**
         * Sets the.
         * 
         * @param s
         *            the s
         */
        public static void set(final StackOffsetType s) {
            Singletons.getModel().getPreferences().stackOffset = s;
            int index = 0;
            for (String str : keys) {
                if (str.toLowerCase().equals(s.toString())) {
                    break;
                }
                index++;
            }
            Constant.Runtime.STACK_OFFSET[0] = offsets[index];
        }
    }

    /**
     * The Class HowToPlayAction.
     * 
     * @author dhudson
     */
    public static class HowToPlayAction extends AbstractAction {

        private static final long serialVersionUID = 5552000208438248428L;

        /**
         * Instantiates a new how to play action.
         */
        public HowToPlayAction() {
            super(ForgeProps.getLocalized(LANG.HowTo.TITLE));
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent
         * )
         */
        /**
         * @param e ActionEvent
         */
        public final void actionPerformed(final ActionEvent e) {
            String text = ForgeProps.getLocalized(LANG.HowTo.MESSAGE);

            JTextArea area = new JTextArea(text, 25, 40);
            area.setWrapStyleWord(true);
            area.setLineWrap(true);
            area.setEditable(false);
            area.setOpaque(false);

            JOptionPane.showMessageDialog(null, new JScrollPane(area), ForgeProps.getLocalized(LANG.HowTo.TITLE),
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * The Class AboutAction.
     * 
     * @author dhudson
     */
    public static class AboutAction extends AbstractAction {

        private static final long serialVersionUID = 5492173304463396871L;

        /**
         * Instantiates a new about action.
         */
        public AboutAction() {
            super(ForgeProps.getLocalized(MENU_BAR.MENU.ABOUT));
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent
         * )
         */
        /**
         * @param e ActionEvent
         */
        public final void actionPerformed(final ActionEvent e) {
            JTextArea area = new JTextArea(12, 25);

            if (useLAFFonts.isSelected()) {
                Font f = new Font(area.getFont().getName(), Font.PLAIN, 13);
                area.setFont(f);
            }

            area.setText("The various people who have contributed to this project apologize with deep remorse"
                    + " for any bugs that you may have noticed.\n\nThe development team.\n\nOriginal author: Forge\n\n"
                    + "(Quest icons used created by Teekatas, from his Legendora set:\n"
                    + " http://raindropmemory.deviantart.com)");

            area.setWrapStyleWord(true);
            area.setLineWrap(true);
            area.setEditable(false);

            JPanel p = new JPanel();
            area.setBackground(p.getBackground());

            JOptionPane.showMessageDialog(null, area, "About", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * The Class ExitAction.
     * 
     * @author slapshot5
     */
    public static class ExitAction extends AbstractAction {
        private static final long serialVersionUID = -319036939657136034L;

        /**
         * Instantiates a new exit action.
         */
        public ExitAction() {
            super(ForgeProps.getLocalized(MENU_BAR.MENU.EXIT));
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent
         * )
         */
        /**
         * @param e ActionEvent
         */
        public final void actionPerformed(final ActionEvent e) {
            System.exit(0);
        }
    }

    /**
     * <p>
     * exit.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean exit() {
        try {
            ForgePreferences preferences = Singletons.getModel().getPreferences();
            preferences.laf = UIManager.getLookAndFeel().getClass().getName();
            preferences.lafFonts = useLAFFonts.isSelected();
            // preferences.newGui = newGuiCheckBox.isSelected();
            preferences.stackAiLand = smoothLandCheckBox.isSelected();
            preferences.millingLossCondition = Constant.Runtime.MILL[0];
            preferences.developerMode = Constant.Runtime.DEV_MODE[0];
            preferences.cardOverlay = cardOverlay.isSelected();
            preferences.scaleLargerThanOriginal = ImageCache.isScaleLargerThanOriginal();
            preferences.uploadDraftAI = Constant.Runtime.UPLOAD_DRAFT[0];
            preferences.save();
        } catch (Exception ex) {
            int result = JOptionPane.showConfirmDialog(this,
                    "Preferences could not be saved. Continue to close without saving ?", "Confirm Exit",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
            if (result != JOptionPane.OK_OPTION) {
                return false;
            }
        }

        setVisible(false);
        dispose();
        return true;
    }

    /** {@inheritDoc} */
    protected final void processWindowEvent(final WindowEvent event) {
        if (event.getID() == WindowEvent.WINDOW_CLOSING) {
            if (!exit()) {
                return;
            }
        }
        super.processWindowEvent(event);
    }

    /* CHOPPIC */
    /* Panel with rounded border and semi-transparent background */
    private static class CustomPanel extends JPanel {
        private static final long serialVersionUID = 774205995101881824L;
        private final int radius;

        CustomPanel(final int neoRadius) {
            this.radius = neoRadius;
        }

        /**
         *
         */
        public void paintComponent(final Graphics g) {
            Color bg = getBackground();
            g.setColor(new Color(bg.getRed(), bg.getGreen(), bg.getBlue(), 180));
            g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            g.setColor(new Color(0, 0, 0, 70));
            g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
        }
    }

    /**
     * Load dynamic gamedata.
     */
    public static void loadDynamicGamedata() {
        if (!Constant.CardTypes.LOADED[0]) {
            ArrayList<String> typeListFile = FileUtil.readFile("res/gamedata/TypeLists.txt");

            ArrayList<String> tList = null;

            Constant.CardTypes.CARD_TYPES[0] = new Constant_StringArrayList();
            Constant.CardTypes.SUPER_TYPES[0] = new Constant_StringArrayList();
            Constant.CardTypes.BASIC_TYPES[0] = new Constant_StringArrayList();
            Constant.CardTypes.LAND_TYPES[0] = new Constant_StringArrayList();
            Constant.CardTypes.CREATURE_TYPES[0] = new Constant_StringArrayList();
            Constant.CardTypes.INSTANT_TYPES[0] = new Constant_StringArrayList();
            Constant.CardTypes.SORCERY_TYPES[0] = new Constant_StringArrayList();
            Constant.CardTypes.ENCHANTMENT_TYPES[0] = new Constant_StringArrayList();
            Constant.CardTypes.ARTIFACT_TYPES[0] = new Constant_StringArrayList();
            Constant.CardTypes.WALKER_TYPES[0] = new Constant_StringArrayList();

            if (typeListFile.size() > 0) {
                for (int i = 0; i < typeListFile.size(); i++) {
                    String s = typeListFile.get(i);

                    if (s.equals("[CardTypes]")) {
                        tList = Constant.CardTypes.CARD_TYPES[0].getList();
                    }

                    else if (s.equals("[SuperTypes]")) {
                        tList = Constant.CardTypes.SUPER_TYPES[0].getList();
                    }

                    else if (s.equals("[BasicTypes]")) {
                        tList = Constant.CardTypes.BASIC_TYPES[0].getList();
                    }

                    else if (s.equals("[LandTypes]")) {
                        tList = Constant.CardTypes.LAND_TYPES[0].getList();
                    }

                    else if (s.equals("[CreatureTypes]")) {
                        tList = Constant.CardTypes.CREATURE_TYPES[0].getList();
                    }

                    else if (s.equals("[InstantTypes]")) {
                        tList = Constant.CardTypes.INSTANT_TYPES[0].getList();
                    }

                    else if (s.equals("[SorceryTypes]")) {
                        tList = Constant.CardTypes.SORCERY_TYPES[0].getList();
                    }

                    else if (s.equals("[EnchantmentTypes]")) {
                        tList = Constant.CardTypes.ENCHANTMENT_TYPES[0].getList();
                    }

                    else if (s.equals("[ArtifactTypes]")) {
                        tList = Constant.CardTypes.ARTIFACT_TYPES[0].getList();
                    }

                    else if (s.equals("[WalkerTypes]")) {
                        tList = Constant.CardTypes.WALKER_TYPES[0].getList();
                    }

                    else if (s.length() > 1) {
                        tList.add(s);
                    }
                }
            }
            Constant.CardTypes.LOADED[0] = true;
            /*
             * if (Constant.Runtime.DevMode[0]) {
             * System.out.println(Constant.CardTypes.cardTypes[0].list);
             * System.out.println(Constant.CardTypes.superTypes[0].list);
             * System.out.println(Constant.CardTypes.basicTypes[0].list);
             * System.out.println(Constant.CardTypes.landTypes[0].list);
             * System.out.println(Constant.CardTypes.creatureTypes[0].list);
             * System.out.println(Constant.CardTypes.instantTypes[0].list);
             * System.out.println(Constant.CardTypes.sorceryTypes[0].list);
             * System.out.println(Constant.CardTypes.enchantmentTypes[0].list);
             * System.out.println(Constant.CardTypes.artifactTypes[0].list);
             * System.out.println(Constant.CardTypes.walkerTypes[0].list); }
             */
        }

        if (!Constant.Keywords.LOADED[0]) {
            ArrayList<String> nskwListFile = FileUtil.readFile("res/gamedata/NonStackingKWList.txt");

            Constant.Keywords.NON_STACKING_LIST[0] = new Constant_StringArrayList();

            if (nskwListFile.size() > 1) {
                for (int i = 0; i < nskwListFile.size(); i++) {
                    String s = nskwListFile.get(i);
                    if (s.length() > 1) {
                        Constant.Keywords.NON_STACKING_LIST[0].getList().add(s);
                    }
                }
            }
            Constant.Keywords.LOADED[0] = true;
            /*
             * if (Constant.Runtime.DevMode[0]) {
             * System.out.println(Constant.Keywords.NonStackingList[0].list); }
             */
        }

        /*
         * if (!Constant.Color.loaded[0]) { ArrayList<String> lcListFile =
         * FileUtil.readFile("res/gamedata/LandColorList");
         * 
         * if (lcListFile.size() > 1) { for (int i=0; i<lcListFile.size(); i++)
         * { String s = lcListFile.get(i); if (s.length() > 1)
         * Constant.Color.LandColor[0].map.add(s); } }
         * Constant.Keywords.loaded[0] = true; if (Constant.Runtime.DevMode[0])
         * { System.out.println(Constant.Keywords.NonStackingList[0].list); } }
         */
    }

}
