package forge.view.swing;

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
import net.slightlymagic.braids.util.UtilFunctions;

import com.esotericsoftware.minlog.Log;

import forge.AllZone;
import forge.Command;
import forge.Constant;
import forge.ConstantStringArrayList;
import forge.FileUtil;
import forge.GuiImportPicture;
import forge.GuiDisplay;
import forge.GuiDownloadQuestImages;
import forge.GuiDownloadPicturesLQ;
import forge.GuiDownloadPrices;
import forge.GuiDownloadSetPicturesLQ;
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
import forge.game.limited.BoosterDraft;
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
import forge.properties.NewConstants.Lang.OldGuiNewGame.MenuBar.Menu;
import forge.properties.NewConstants.Lang.OldGuiNewGame.MenuBar.Options;
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
public class OldGuiNewGame extends JFrame {
    /** Constant <code>serialVersionUID=-2437047615019135648L</code>. */
    private static final long serialVersionUID = -2437047615019135648L;

    // private final DeckManager deckManager = new
    // DeckManager(ForgeProps.getFile(NEW_DECKS));
    private final DeckManager deckManager = AllZone.getDeckManager();
    // with the new IO, there's no reason to use different instances
    private List<Deck> allDecks;

    private final JLabel titleLabel = new JLabel();
    private final JLabel jLabel2 = new JLabel();
    private final JLabel jLabel3 = new JLabel();
    private final JComboBox humanComboBox = new JComboBox();
    private final JComboBox computerComboBox = new JComboBox();
    private final JButton deckEditorButton = new JButton();
    private final JButton startButton = new JButton();
    private final ButtonGroup buttonGroup1 = new ButtonGroup();
    private final JRadioButton sealedRadioButton = new JRadioButton();
    private final JRadioButton singleRadioButton = new JRadioButton();

    private final JRadioButton draftRadioButton = new JRadioButton();

    /* CHOPPIC */
    private final CustomPanel jPanel1 = new CustomPanel(10);
    private final CustomPanel jPanel2 = new CustomPanel(10);
    private final CustomPanel jPanel3 = new CustomPanel(10);
    /* CHOPPIC */

    // @SuppressWarnings("unused")
    // titledBorder2
    /** Constant <code>newGuiCheckBox</code>. */
    // private static JCheckBox newGuiCheckBox = new JCheckBox("", true);
    /** Constant <code>smoothLandCheckBox</code>. */
    private static JCheckBox smoothLandCheckBox = new JCheckBox("", false);
    /** Constant <code>devModeCheckBox</code>. */
    private static JCheckBox devModeCheckBox = new JCheckBox("", true);

    /** The upld drft check box. */
    private static JCheckBox upldDrftCheckBox = new JCheckBox("", true);

    /** The foil random check box. */
    private static JCheckBox foilRandomCheckBox = new JCheckBox("", true);

    private static JCheckBoxMenuItem singletons = new JCheckBoxMenuItem(
            ForgeProps.getLocalized(NewConstants.Lang.OldGuiNewGame.MenuBar.Options.Generate.SINGLETONS));
    
    /** Constant <code>removeSmallCreatures</code>. */
    private static JCheckBoxMenuItem removeSmallCreatures = new JCheckBoxMenuItem(
            ForgeProps.getLocalized(NewConstants.Lang.OldGuiNewGame.MenuBar.Options.Generate.REMOVE_SMALL));

    /** Constant <code>removeArtifacts</code>. */
    private static JCheckBoxMenuItem removeArtifacts = new JCheckBoxMenuItem(
            ForgeProps.getLocalized(NewConstants.Lang.OldGuiNewGame.MenuBar.Options.Generate.REMOVE_ARTIFACTS));
    /** Constant <code>useLAFFonts</code>. */
    private static JCheckBoxMenuItem useLAFFonts = new JCheckBoxMenuItem(ForgeProps.getLocalized(NewConstants.Lang.OldGuiNewGame.MenuBar.Options.FONT));
    /** Constant <code>cardOverlay</code>. */
    private static JCheckBoxMenuItem cardOverlay = new JCheckBoxMenuItem(
            ForgeProps.getLocalized(NewConstants.Lang.OldGuiNewGame.MenuBar.Options.CARD_OVERLAY));
    /** Constant <code>cardScale</code>. */
    private static JCheckBoxMenuItem cardScale = new JCheckBoxMenuItem(
            ForgeProps.getLocalized(NewConstants.Lang.OldGuiNewGame.MenuBar.Options.CARD_SCALE));
    private final JButton questButton = new JButton();

    private final Action lookAndFeelAction = new LookAndFeelAction(this);
    // private Action DOWNLOAD_ACTION = new DownloadAction();
    private final Action downloadActionLQ = new DownloadActionLQ();
    private final Action downloadActionSetLQ = new DownloadActionSetLQ();
    private final Action downloadActionQuest = new DownloadActionQuest();
    private final Action importPicture = new ImportPictureAction();
    private final Action cardSizesAction = new CardSizesAction();
    private final Action cardStackAction = new CardStackAction();
    private final Action cardStackOffsetAction = new CardStackOffsetAction();
    private final Action aboutAction = new AboutAction();
    private final Action howToPlayAction = new HowToPlayAction();
    private final Action dnldPricesAction = new DownloadPriceAction();
    private final Action bugzReporterAction = new BugzReporterAction();
    private final Action exitAction = new ExitAction();

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
            this.jbInit();
        } catch (final Exception ex) {
            ErrorViewer.showError(ex);
        }

        if (Constant.Runtime.getGameType().equals(GameType.Constructed)) {
            this.singleRadioButton.setSelected(true);
            this.updateDeckComboBoxes();
        }
        if (Constant.Runtime.getGameType().equals(GameType.Sealed)) {
            this.sealedRadioButton.setSelected(true);
            this.updateDeckComboBoxes();
        }
        if (Constant.Runtime.getGameType().equals(GameType.Draft)) {
            this.draftRadioButton.setSelected(true);
            this.draftRadioButtonActionPerformed(null);
        }

        this.addListeners();

        this.setSize(550, 565);
        GuiUtils.centerFrame(this);

        this.setTitle(ForgeProps.getLocalized(NewConstants.Lang.PROGRAM_NAME));
        this.setupMenu();
        this.setVisible(true);

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
        final Action[] actions = {
                this.// Remove the option to download HQ pics since the HQ pics
                // server appears to be offline.
                // LOOK_AND_FEEL_ACTION, DNLD_PRICES_ACTION, DOWNLOAD_ACTION,
                // DOWNLOAD_ACTION_LQ, DOWNLOAD_ACTION_SETLQ, IMPORT_PICTURE,
                // CARD_SIZES_ACTION,
                lookAndFeelAction, this.dnldPricesAction, this.downloadActionLQ, this.downloadActionSetLQ,
                this.downloadActionQuest, this.importPicture, this.cardSizesAction, this.cardStackAction,
                this.cardStackOffsetAction, this.bugzReporterAction, ErrorViewer.ALL_THREADS_ACTION,
                this.aboutAction, this.exitAction };
        final JMenu menu = new JMenu(ForgeProps.getLocalized(Menu.TITLE));
        for (final Action a : actions) {
            menu.add(a);
            if (a.equals(this.lookAndFeelAction) || a.equals(this.importPicture)
                    || a.equals(this.cardStackOffsetAction) || a.equals(ErrorViewer.ALL_THREADS_ACTION)) {
                menu.addSeparator();
            }
        }

        // useLAFFonts.setSelected(false);

        // new stuff
        final JMenu generatedDeck = new JMenu(ForgeProps.getLocalized(NewConstants.Lang.OldGuiNewGame.MenuBar.Options.Generate.TITLE));
        
        generatedDeck.add(OldGuiNewGame.singletons);
        OldGuiNewGame.singletons.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                Singletons.getModel().getPreferences()
                        .setDeckGenSingletons(OldGuiNewGame.singletons.isSelected());
            }
        });

        generatedDeck.add(OldGuiNewGame.removeSmallCreatures);
        OldGuiNewGame.removeSmallCreatures.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                Singletons.getModel().getPreferences()
                        .setDeckGenRmvSmall(OldGuiNewGame.removeSmallCreatures.isSelected());
            }
        });
        
        generatedDeck.add(OldGuiNewGame.removeSmallCreatures);
        OldGuiNewGame.removeSmallCreatures.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                Singletons.getModel().getPreferences()
                        .setDeckGenRmvSmall(OldGuiNewGame.removeSmallCreatures.isSelected());
            }
        });

        generatedDeck.add(OldGuiNewGame.removeArtifacts);
        OldGuiNewGame.removeArtifacts.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                Singletons.getModel().getPreferences()
                        .setDeckGenRmvArtifacts(OldGuiNewGame.removeArtifacts.isSelected());
            }
        });

        final JMenu optionsMenu = new JMenu(ForgeProps.getLocalized(Options.TITLE));
        optionsMenu.add(generatedDeck);

        optionsMenu.add(OldGuiNewGame.getUseLAFFonts());
        optionsMenu.addSeparator();
        optionsMenu.add(OldGuiNewGame.getCardOverlay());
        optionsMenu.add(OldGuiNewGame.getCardScale());

        OldGuiNewGame.getCardScale().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                ImageCache.setScaleLargerThanOriginal(OldGuiNewGame.getCardScale().isSelected());
            }
        });

        final JMenu helpMenu = new JMenu(ForgeProps.getLocalized(NewConstants.Lang.OldGuiNewGame.MenuBar.Help.TITLE));

        final Action[] helpActions = { this.howToPlayAction };
        for (final Action a : helpActions) {
            helpMenu.add(a);
        }

        final JMenuBar bar = new JMenuBar();
        bar.add(menu);
        bar.add(optionsMenu);
        bar.add(helpMenu);
        // bar.add(new MenuItem_HowToPlay());

        this.setJMenuBar(bar);
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
        final List<Deck> list = new ArrayList<Deck>(this.deckManager.getDecks());

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
                OldGuiNewGame.this.dispose();
                System.exit(0);
            }
        });

        this.questButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
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
        final Deck deck = new Deck(GameType.Sealed);

        // ReadBoosterPack booster = new ReadBoosterPack();
        // CardList pack = booster.getBoosterPack5();

        final ArrayList<String> sealedTypes = new ArrayList<String>();
        sealedTypes.add("Full Cardpool");
        sealedTypes.add("Block / Set");
        sealedTypes.add("Custom");

        final String prompt = "Choose Sealed Deck Format:";
        final Object o = GuiUtils.getChoice(prompt, sealedTypes.toArray());

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
            throw new IllegalStateException("choice <<" + UtilFunctions.safeToString(o)
                    + ">> does not equal any of the sealedTypes.");
        }

        final ItemPool<CardPrinted> sDeck = sd.getCardpool();

        if (sDeck.countAll() > 1) {

            deck.addSideboard(sDeck);

            for (final String element : Constant.Color.BASIC_LANDS) {
                for (int j = 0; j < 18; j++) {
                    deck.addSideboard(element + "|" + sd.getLandSetCode()[0]);
                }
            }

            final String sDeckName = JOptionPane.showInputDialog(null,
                    ForgeProps.getLocalized(NewConstants.Lang.OldGuiNewGame.NewGameText.SAVE_SEALED_MSG),
                    ForgeProps.getLocalized(NewConstants.Lang.OldGuiNewGame.NewGameText.SAVE_SEALED_TTL), JOptionPane.QUESTION_MESSAGE);

            deck.setName(sDeckName);
            deck.setPlayerType(PlayerType.HUMAN);

            Constant.Runtime.HUMAN_DECK[0] = deck;
            Constant.Runtime.setGameType(GameType.Sealed);

            // Deck aiDeck = sd.buildAIDeck(sDeck.toForgeCardList());
            final Deck aiDeck = sd.buildAIDeck(sd.getCardpool().toForgeCardList()); // AI
            // will
            // use
            // different
            // cardpool
            aiDeck.setName("AI_" + sDeckName);
            aiDeck.setPlayerType(PlayerType.COMPUTER);
            this.deckManager.addDeck(aiDeck);
            DeckManager.writeDeck(aiDeck, DeckManager.makeFileName(aiDeck));
            this.updateDeckComboBoxes();

            this.deckEditorButtonActionPerformed(GameType.Sealed, deck);

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
        final DeckEditorDraft draft = new DeckEditorDraft();

        // determine what kind of booster draft to run
        final ArrayList<String> draftTypes = new ArrayList<String>();
        draftTypes.add("Full Cardpool");
        draftTypes.add("Block / Set");
        draftTypes.add("Custom");

        final String prompt = "Choose Draft Format:";
        final Object o = GuiUtils.getChoice(prompt, draftTypes.toArray());

        if (o.toString().equals(draftTypes.get(0))) {
            draft.showGui(new BoosterDraft(CardPoolLimitation.Full));
        }

        else if (o.toString().equals(draftTypes.get(1))) {
            draft.showGui(new BoosterDraft(CardPoolLimitation.Block));
        }

        else if (o.toString().equals(draftTypes.get(2))) {
            draft.showGui(new BoosterDraft(CardPoolLimitation.Custom));
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

        this.titleLabel.setText(ForgeProps.getLocalized(NewConstants.Lang.OldGuiNewGame.NewGameText.NEW_GAME));
        this.titleLabel.setFont(new java.awt.Font("Dialog", 0, 26));

        /* CHOPPIC */
        this.titleLabel.setForeground(Color.WHITE);
        /* CHOPPIC */

        this.titleLabel.setHorizontalAlignment(SwingConstants.CENTER);

        this.getContentPane().setLayout(new MigLayout("fill"));

        /*
         * Game Type Panel
         */

        /* jPanel2.setBorder(titledBorder1); */
        this.setCustomBorder(this.jPanel2, ForgeProps.getLocalized(NewConstants.Lang.OldGuiNewGame.NewGameText.GAMETYPE));
        this.jPanel2.setLayout(new MigLayout("align center"));

        this.singleRadioButton.setText(ForgeProps.getLocalized(NewConstants.Lang.OldGuiNewGame.NewGameText.CONSTRUCTED_TEXT));
        this.singleRadioButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                OldGuiNewGame.this.singleRadioButtonActionPerformed(e);
            }
        });

        // sealedRadioButton.setToolTipText("");
        this.sealedRadioButton.setText(ForgeProps.getLocalized(NewConstants.Lang.OldGuiNewGame.NewGameText.SEALED_TEXT));
        this.sealedRadioButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                OldGuiNewGame.this.sealedRadioButtonActionPerformed(e);
            }
        });

        // draftRadioButton.setToolTipText("");
        this.draftRadioButton.setText(ForgeProps.getLocalized(NewConstants.Lang.OldGuiNewGame.NewGameText.BOOSTER_TEXT));
        this.draftRadioButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                OldGuiNewGame.this.draftRadioButtonActionPerformed(e);
            }
        });

        /*
         * Library Panel
         */

        /* jPanel1.setBorder(titledBorder2); */
        this.setCustomBorder(this.jPanel1, ForgeProps.getLocalized(NewConstants.Lang.OldGuiNewGame.NewGameText.LIBRARY));
        this.jPanel1.setLayout(new MigLayout("align center"));

        this.jLabel2.setText(ForgeProps.getLocalized(NewConstants.Lang.OldGuiNewGame.NewGameText.YOURDECK));
        this.jLabel3.setText(ForgeProps.getLocalized(NewConstants.Lang.OldGuiNewGame.NewGameText.OPPONENT));

        /*
         * Settings Panel
         */

        /* jPanel3.setBorder(titledBorder3); */
        this.setCustomBorder(this.jPanel3, ForgeProps.getLocalized(NewConstants.Lang.OldGuiNewGame.NewGameText.SETTINGS));
        this.jPanel3.setLayout(new MigLayout("align center"));

        // newGuiCheckBox.setText(ForgeProps.getLocalized(NEW_GAME_TEXT.NEW_GUI));
        OldGuiNewGame.getSmoothLandCheckBox().setText(ForgeProps.getLocalized(NewConstants.Lang.OldGuiNewGame.NewGameText.AI_LAND));

        OldGuiNewGame.getDevModeCheckBox().setText(ForgeProps.getLocalized(NewConstants.Lang.OldGuiNewGame.NewGameText.DEV_MODE));
        OldGuiNewGame.getDevModeCheckBox().addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                Constant.Runtime.DEV_MODE[0] = OldGuiNewGame.getDevModeCheckBox().isSelected();
                Singletons.getModel().getPreferences().setDeveloperMode(Constant.Runtime.DEV_MODE[0]);
            }
        });

        OldGuiNewGame.getUpldDrftCheckBox().setText("Upload Draft Picks");

        OldGuiNewGame.getUpldDrftCheckBox()
                .setToolTipText("Your picks and all other participants' picks will help the Forge AI"
                        + " make better draft picks.");

        OldGuiNewGame.getUpldDrftCheckBox().addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                Constant.Runtime.UPLOAD_DRAFT[0] = OldGuiNewGame.getUpldDrftCheckBox().isSelected();
                Singletons.getModel().getPreferences().setUploadDraftAI(Constant.Runtime.UPLOAD_DRAFT[0]);
            }
        });

        OldGuiNewGame.getFoilRandomCheckBox().setText("Random Foiling");
        OldGuiNewGame.getFoilRandomCheckBox()
                .setToolTipText("Approximately 1:20 cards will appear with foiling effects applied.");
        OldGuiNewGame.getFoilRandomCheckBox().addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                Constant.Runtime.RANDOM_FOIL[0] = OldGuiNewGame.getFoilRandomCheckBox().isSelected();
                Singletons.getModel().getPreferences().setRandCFoil(Constant.Runtime.RANDOM_FOIL[0]);
            }
        });

        /*
         * Buttons
         */

        this.deckEditorButton.setFont(new java.awt.Font("Dialog", 0, 15));
        this.deckEditorButton.setText(ForgeProps.getLocalized(NewConstants.Lang.OldGuiNewGame.NewGameText.DECK_EDITOR));
        this.deckEditorButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                OldGuiNewGame.this.deckEditorButtonActionPerformed(GameType.Constructed, null);
            }
        });

        this.startButton.setFont(new java.awt.Font("Dialog", 0, 18));
        this.startButton.setHorizontalTextPosition(SwingConstants.LEADING);
        this.startButton.setText(ForgeProps.getLocalized(NewConstants.Lang.OldGuiNewGame.NewGameText.START_GAME));
        this.startButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                OldGuiNewGame.this.startButtonActionPerformed(e);
            }
        });

        this.questButton.setFont(new java.awt.Font("Dialog", 0, 18));
        this.questButton.setText(ForgeProps.getLocalized(NewConstants.Lang.OldGuiNewGame.NewGameText.QUEST_MODE));

        this.getContentPane().add(this.titleLabel, "align center, span 3, grow, wrap");

        this.getContentPane().add(this.jPanel2, "span 3, grow, wrap");
        this.jPanel2.add(this.singleRadioButton, "span 3, wrap");
        this.jPanel2.add(this.sealedRadioButton, "span 3, wrap");
        this.jPanel2.add(this.draftRadioButton, "span 3, wrap");
        this.updatePanelDisplay(this.jPanel2);

        this.getContentPane().add(this.jPanel1, "span 2, grow");
        this.jPanel1.add(this.jLabel2);
        this.jPanel1.add(this.humanComboBox, "sg combobox, wrap");
        this.jPanel1.add(this.jLabel3);
        this.jPanel1.add(this.computerComboBox, "sg combobox");
        this.updatePanelDisplay(this.jPanel1);

        this.getContentPane().add(this.deckEditorButton, "sg buttons, align 50% 50%, wrap");

        this.getContentPane().add(this.jPanel3, "span 2, grow");

        // jPanel3.add(newGuiCheckBox, "wrap");
        this.jPanel3.add(OldGuiNewGame.getSmoothLandCheckBox(), "wrap");
        this.jPanel3.add(OldGuiNewGame.getDevModeCheckBox(), "wrap");
        this.jPanel3.add(OldGuiNewGame.getUpldDrftCheckBox(), "wrap");
        this.jPanel3.add(OldGuiNewGame.getFoilRandomCheckBox(), "wrap");
        this.updatePanelDisplay(this.jPanel3);

        this.getContentPane().add(this.startButton, "sg buttons, align 50% 50%, split 2, flowy");
        this.getContentPane().add(this.questButton, "sg buttons, align 50% 50%");

        this.buttonGroup1.add(this.singleRadioButton);
        this.buttonGroup1.add(this.sealedRadioButton);
        this.buttonGroup1.add(this.draftRadioButton);

        /* CHOPPIC */
        /* Add background image */
        ((JPanel) this.getContentPane()).setOpaque(false);
        final ImageIcon bkgd = new ImageIcon("res/images/ui/newgame_background.jpg");
        final JLabel myLabel = new JLabel(bkgd);

        // Do not pass Integer.MIN_VALUE directly here; it must be packaged in
        // an Integer
        // instance. Otherwise, GUI components will not draw unless moused over.
        this.getLayeredPane().add(myLabel, Integer.valueOf(Integer.MIN_VALUE));

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
        for (final Component c : panel.getComponents()) {
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
        final TitledBorder tb = BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), title);
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

        final DeckEditorCommon editor = new DeckEditorCommon(gt);

        final Command exit = new Command() {
            private static final long serialVersionUID = -9133358399503226853L;

            @Override
            public void execute() {

                OldGuiNewGame.this.updateDeckComboBoxes();
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
        final Random r = MyRandom.getRandom();

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
        if ((this.humanComboBox.getSelectedItem() == null) || (this.computerComboBox.getSelectedItem() == null)) {
            return;
        }

        final String human = this.humanComboBox.getSelectedItem().toString();

        String computer = null;
        if (this.computerComboBox.getSelectedItem() != null) {
            computer = this.computerComboBox.getSelectedItem().toString();
        }

        if (this.draftRadioButton.isSelected()) {
            if (human.equals("New Draft")) {
                this.dispose();
                this.setupDraft();
                return;

            } else {
                // load old draft
                final Deck[] deck = this.deckManager.getDraftDeck(human);
                final int index = Integer.parseInt(computer);

                Constant.Runtime.HUMAN_DECK[0] = deck[0];
                Constant.Runtime.COMPUTER_DECK[0] = deck[index];

                if (Constant.Runtime.COMPUTER_DECK[0] == null) {
                    throw new IllegalStateException("OldGuiNewGame : startButton() error - computer deck is null");
                }
            } // else - load old draft
        } // if
        else if (this.sealedRadioButton.isSelected()) {
            if (human.equals("New Sealed")) {
                this.dispose();

                this.setupSealed();

                return;
            } else {
                Constant.Runtime.HUMAN_DECK[0] = this.deckManager.getDeck(human);

            }

            if (!computer.equals("New Sealed")) {
                Constant.Runtime.COMPUTER_DECK[0] = this.deckManager.getDeck(computer);
            }
        } else {
            // non-draft decks
            final GameType format = Constant.Runtime.getGameType();
            // boolean sealed = GameType.Sealed.equals(format);
            final boolean constructed = GameType.Constructed.equals(format);

            final boolean humanGenerate = human.equals("Generate Deck");
            final boolean humanRandom = human.equals("Random");

            if (humanGenerate) {
                if (constructed) {
                    DeckGeneration.genDecks(PlayerType.HUMAN);
                }
                // else if(sealed)
                // Constant.Runtime.HumanDeck[0] = generateSealedDeck();
            } else if (humanRandom) {
                Constant.Runtime.HUMAN_DECK[0] = this.getRandomDeck(this.getDecks(format));

                JOptionPane.showMessageDialog(null,
                        String.format("You are using deck: %s", Constant.Runtime.HUMAN_DECK[0].getName()), "Deck Name",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                Constant.Runtime.HUMAN_DECK[0] = this.deckManager.getDeck(human);
            }

            assert computer != null;
            final boolean computerGenerate = computer.equals("Generate Deck");
            final boolean computerRandom = computer.equals("Random");

            if (computerGenerate) {
                if (constructed) {
                    DeckGeneration.genDecks(PlayerType.COMPUTER);
                } // Constant.Runtime.ComputerDeck[0] =
                  // generateConstructedDeck();
                  // else if(sealed)
                  // Constant.Runtime.ComputerDeck[0] = generateSealedDeck();
            } else if (computerRandom) {
                Constant.Runtime.COMPUTER_DECK[0] = this.getRandomDeck(this.getDecks(format));

                JOptionPane.showMessageDialog(null,
                        String.format("The computer is using deck: %s", Constant.Runtime.COMPUTER_DECK[0].getName()),
                        "Deck Name", JOptionPane.INFORMATION_MESSAGE);
            } else {
                Constant.Runtime.COMPUTER_DECK[0] = this.deckManager.getDeck(computer);
            }
        } // else

        // DO NOT CHANGE THIS ORDER, GuiDisplay needs to be created before cards
        // are added
        // Constant.Runtime.DevMode[0] = devModeCheckBox.isSelected();

        // if (newGuiCheckBox.isSelected())
        AllZone.setDisplay(new GuiDisplay());
        // else AllZone.setDisplay(new GuiDisplay3());

        Constant.Runtime.SMOOTH[0] = OldGuiNewGame.getSmoothLandCheckBox().isSelected();

        AllZone.getGameAction().newGame(Constant.Runtime.HUMAN_DECK[0], Constant.Runtime.COMPUTER_DECK[0]);
        AllZone.getDisplay().setVisible(true);

        this.dispose();
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
        this.updateDeckComboBoxes();
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
        this.updateDeckComboBoxes();
    }

    /**
     * <p>
     * updateDeckComboBoxes.
     * </p>
     */
    private void updateDeckComboBoxes() {
        this.humanComboBox.removeAllItems();
        this.computerComboBox.removeAllItems();

        this.allDecks = this.getDecks();
        switch (Constant.Runtime.getGameType()) {
        case Sealed:
            this.humanComboBox.addItem("New Sealed");
            this.computerComboBox.addItem("New Sealed");

            for (final Deck allDeck : this.allDecks) {
                if (allDeck.getDeckType().equals(GameType.Sealed)) {
                    final JComboBox boxToAdd = allDeck.getPlayerType() == PlayerType.COMPUTER ? this.computerComboBox
                            : this.humanComboBox;
                    boxToAdd.addItem(allDeck.getName());
                }
            } // for
            break;
        case Constructed:
            this.humanComboBox.addItem("Generate Deck");
            this.computerComboBox.addItem("Generate Deck");

            this.humanComboBox.addItem("Random");
            this.computerComboBox.addItem("Random");

            for (final Deck allDeck : this.allDecks) {
                if (allDeck.getDeckType().equals(GameType.Constructed)) {
                    this.humanComboBox.addItem(allDeck.getName());
                    this.computerComboBox.addItem(allDeck.getName());
                }
            } // for
            break;
        case Draft:
            this.humanComboBox.addItem("New Draft");
            final Object[] key = this.deckManager.getDraftDecks().keySet().toArray();
            Arrays.sort(key);

            for (final Object aKey : key) {
                this.humanComboBox.addItem(aKey);
            }

            for (int i = 0; i < 7; i++) {
                this.computerComboBox.addItem("" + (i + 1));
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
        final ArrayList<Deck> list = new ArrayList<Deck>();

        Deck d;
        for (final Deck allDeck : this.deckManager.getDecks()) {
            d = allDeck;

            if (d.getDeckType().equals(gameType)) {
                list.add(d);
            }
        } // for

        // convert ArrayList to Deck[]
        final Deck[] out = new Deck[list.size()];
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
        this.updateDeckComboBoxes();
    }

    /**
     * The Class LookAndFeelAction.
     * 
     * @author dhudson
     */
    public static class LookAndFeelAction extends AbstractAction {

        private static final long serialVersionUID = -4447498333866711215L;
        private final Component c;

        /**
         * Instantiates a new look and feel action.
         * 
         * @param component
         *            the component
         */
        public LookAndFeelAction(final Component component) {
            super(ForgeProps.getLocalized(NewConstants.Lang.OldGuiNewGame.MenuBar.Menu.LF));
            this.c = component;
        }

        /**
         * Action performed.
         * 
         * @param e
         *            the e
         */
        @Override
        public final void actionPerformed(final ActionEvent e) {
            final LookAndFeelInfo[] info = UIManager.getInstalledLookAndFeels();
            final HashMap<String, String> lafMap = new HashMap<String, String>();
            for (final LookAndFeelInfo anInfo : info) {
                lafMap.put(anInfo.getName(), anInfo.getClassName());
            }

            // add Substance LAFs:
            lafMap.put("Autumn", "org.pushingpixels.substance.api.skin.SubstanceAutumnLookAndFeel");
            lafMap.put("Business", "org.pushingpixels.substance.api.skin.SubstanceBusinessLookAndFeel");
            lafMap.put("Business Black Steel",
                    "org.pushingpixels.substance.api.skin.SubstanceBusinessBlackSteelLookAndFeel");
            lafMap.put("Business Blue Steel",
                    "org.pushingpixels.substance.api.skin.SubstanceBusinessBlueSteelLookAndFeel");
            lafMap.put("Challenger Deep", "org.pushingpixels.substance.api.skin.SubstanceChallengerDeepLookAndFeel");
            lafMap.put("Creme", "org.pushingpixels.substance.api.skin.SubstanceCremeLookAndFeel");
            lafMap.put("Creme Coffee", "org.pushingpixels.substance.api.skin.SubstanceCremeCoffeeLookAndFeel");
            lafMap.put("Dust", "org.pushingpixels.substance.api.skin.SubstanceDustLookAndFeel");
            lafMap.put("Dust Coffee", "org.pushingpixels.substance.api.skin.SubstanceDustCoffeeLookAndFeel");
            lafMap.put("Emerald Dusk", "org.pushingpixels.substance.api.skin.SubstanceEmeraldDuskLookAndFeel");
            lafMap.put("Gemini", "org.pushingpixels.substance.api.skin.SubstanceGeminiLookAndFeel");
            lafMap.put("Graphite", "org.pushingpixels.substance.api.skin.SubstanceGraphiteLookAndFeel");
            lafMap.put("Graphite Aqua", "org.pushingpixels.substance.api.skin.SubstanceGraphiteAquaLookAndFeel");
            lafMap.put("Graphite Glass", "org.pushingpixels.substance.api.skin.SubstanceGraphiteGlassLookAndFeel");
            lafMap.put("Magma", "org.pushingpixels.substance.api.skin.SubstanceMagmaLookAndFeel");
            lafMap.put("Magellan", "org.pushingpixels.substance.api.skin.SubstanceMagellanLookAndFeel");
            // LAFMap.put("Mariner",
            // "org.pushingpixels.substance.api.skin.SubstanceMarinerLookAndFeel");
            lafMap.put("Mist Aqua", "org.pushingpixels.substance.api.skin.SubstanceMistAquaLookAndFeel");
            lafMap.put("Mist Silver", "org.pushingpixels.substance.api.skin.SubstanceMistSilverLookAndFeel");
            lafMap.put("Moderate", "org.pushingpixels.substance.api.skin.SubstanceModerateLookAndFeel");
            lafMap.put("Nebula", "org.pushingpixels.substance.api.skin.SubstanceNebulaLookAndFeel");
            lafMap.put("Nebula Brick Wall", "org.pushingpixels.substance.api.skin.SubstanceNebulaBrickWallLookAndFeel");
            // LAFMap.put("Office Black 2007",
            // "org.pushingpixels.substance.api.skin.SubstanceOfficeBlack2007LookAndFeel");
            lafMap.put("Office Blue 2007", "org.pushingpixels.substance.api.skin.SubstanceOfficeBlue2007LookAndFeel");
            lafMap.put("Office Silver 2007",
                    "org.pushingpixels.substance.api.skin.SubstanceOfficeSilver2007LookAndFeel");
            lafMap.put("Raven", "org.pushingpixels.substance.api.skin.SubstanceRavenLookAndFeel");
            lafMap.put("Raven Graphite", "org.pushingpixels.substance.api.skin.SubstanceRavenGraphiteLookAndFeel");
            // LAFMap.put("Raven Graphite Glass",
            // "org.pushingpixels.substance.api.skin.SubstanceRavenGraphiteGlassLookAndFeel");
            lafMap.put("Sahara", "org.pushingpixels.substance.api.skin.SubstanceSaharaLookAndFeel");
            lafMap.put("Twilight", "org.pushingpixels.substance.api.skin.SubstanceTwilightLookAndFeel");

            final String[] keys = new String[lafMap.size()];
            int count = 0;

            for (final String s1 : lafMap.keySet()) {
                keys[count++] = s1;
            }
            Arrays.sort(keys);

            final ListChooser<String> ch = new ListChooser<String>("Choose one", 0, 1, keys);
            if (ch.show()) {
                try {
                    final String name = ch.getSelectedValue();
                    final int index = ch.getSelectedIndex();
                    if (index == -1) {
                        return;
                    }
                    // UIManager.setLookAndFeel(info[index].getClassName());
                    Singletons.getModel().getPreferences().setLaf(lafMap.get(name));
                    UIManager.setLookAndFeel(lafMap.get(name));

                    SwingUtilities.updateComponentTreeUI(this.c);
                } catch (final Exception ex) {
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
            super(ForgeProps.getLocalized(NewConstants.Lang.OldGuiNewGame.MenuBar.Menu.DOWNLOADPRICE));
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent
         * )
         */
        /**
         * Action performed.
         * 
         * @param e
         *            ActionEvent
         */
        @Override
        public final void actionPerformed(final ActionEvent e) {
            final GuiDownloadPrices gdp = new GuiDownloadPrices();
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
         * Action performed.
         * 
         * @param e
         *            ActionEvent
         */
        @Override
        public final void actionPerformed(final ActionEvent e) {
            final BugzReporter br = new BugzReporter();
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
            super(ForgeProps.getLocalized(NewConstants.Lang.OldGuiNewGame.MenuBar.Menu.DOWNLOADLQ));
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent
         * )
         */
        /**
         * Action performed.
         * 
         * @param e
         *            ActionEvent
         */
        @Override
        public final void actionPerformed(final ActionEvent e) {
            new GuiDownloadPicturesLQ(null);
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
            super(ForgeProps.getLocalized(NewConstants.Lang.OldGuiNewGame.MenuBar.Menu.DOWNLOADSETLQ));
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent
         * )
         */
        /**
         * Action performed.
         * 
         * @param e
         *            ActionEvent
         */
        @Override
        public final void actionPerformed(final ActionEvent e) {
            new GuiDownloadSetPicturesLQ(null);
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
            super(ForgeProps.getLocalized(NewConstants.Lang.OldGuiNewGame.MenuBar.Menu.DOWNLOADQUESTIMG));
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent
         * )
         */
        /**
         * Action performed.
         * 
         * @param e
         *            ActionEvent
         */
        @Override
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
            super(ForgeProps.getLocalized(NewConstants.Lang.OldGuiNewGame.MenuBar.Menu.IMPORTPICTURE));
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent
         * )
         */
        /**
         * Action performed.
         * 
         * @param e
         *            ActionEvent
         */
        @Override
        public final void actionPerformed(final ActionEvent e) {
            final GuiImportPicture ip = new GuiImportPicture(null);
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
            super(ForgeProps.getLocalized(NewConstants.Lang.OldGuiNewGame.MenuBar.Menu.CARD_SIZES));
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent
         * )
         */
        /**
         * Action performed.
         * 
         * @param e
         *            ActionEvent
         */
        @Override
        public final void actionPerformed(final ActionEvent e) {
            final ListChooser<String> ch = new ListChooser<String>("Choose one", "Choose a new max card size", 0, 1,
                    CardSizesAction.keys);
            if (ch.show()) {
                try {
                    final int index = ch.getSelectedIndex();
                    if (index == -1) {
                        return;
                    }
                    CardSizesAction.set(index);
                } catch (final Exception ex) {
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
            Singletons.getModel().getPreferences()
                    .setCardSize(CardSizeType.valueOf(CardSizesAction.keys[index].toLowerCase()));
            Constant.Runtime.WIDTH[0] = CardSizesAction.widths[index];
            Constant.Runtime.HEIGHT[0] = (int) Math.round((CardSizesAction.widths[index] * (3.5 / 2.5)));
        }

        /**
         * Sets the.
         * 
         * @param s
         *            the s
         */
        public static void set(final CardSizeType s) {
            Singletons.getModel().getPreferences().setCardSize(s);
            int index = 0;
            for (final String str : CardSizesAction.keys) {
                if (str.toLowerCase().equals(s.toString())) {
                    break;
                }
                index++;
            }
            Constant.Runtime.WIDTH[0] = CardSizesAction.widths[index];
            Constant.Runtime.HEIGHT[0] = (int) Math.round((CardSizesAction.widths[index] * (3.5 / 2.5)));
        }
    }

    /**
     * The Class CardStackAction.
     * 
     * @author dhudson
     */
    public static class CardStackAction extends AbstractAction {

        private static final long serialVersionUID = -3770527681359311455L;
        private static String[] keys = { "3", "4", "5", "6", "7", "8", "9", "10", "11", "12" };
        private static int[] values = { 3, 4, 5, 6, 7, 8, 9, 10, 11, 12 };

        /**
         * Instantiates a new card stack action.
         */
        public CardStackAction() {
            super(ForgeProps.getLocalized(NewConstants.Lang.OldGuiNewGame.MenuBar.Menu.CARD_STACK));
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent
         * )
         */
        /**
         * Action performed.
         * 
         * @param e
         *            ActionEvent
         */
        @Override
        public final void actionPerformed(final ActionEvent e) {

            final ListChooser<String> ch = new ListChooser<String>("Choose one", "Choose the max size of a stack", 0,
                    1, CardStackAction.keys);

            if (ch.show()) {
                try {
                    final int index = ch.getSelectedIndex();
                    if (index == -1) {
                        return;
                    }
                    CardStackAction.set(index);

                } catch (final Exception ex) {
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
            Singletons.getModel().getPreferences().setMaxStackSize(CardStackAction.values[index]);
            Constant.Runtime.STACK_SIZE[0] = CardStackAction.values[index];
        }

        /**
         * Sets the val.
         * 
         * @param val
         *            the new val
         */
        public static void setVal(final int val) {
            Singletons.getModel().getPreferences().setMaxStackSize(val);
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
        private static String[] keys = { "Tiny", "Small", "Medium", "Large" };
        private static int[] offsets = { 5, 7, 10, 15 };

        /**
         * Instantiates a new card stack offset action.
         */
        public CardStackOffsetAction() {
            super(ForgeProps.getLocalized(NewConstants.Lang.OldGuiNewGame.MenuBar.Menu.CARD_STACK_OFFSET));
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent
         * )
         */
        /**
         * Action performed.
         * 
         * @param e
         *            ActionEvent
         */
        @Override
        public final void actionPerformed(final ActionEvent e) {
            final ListChooser<String> ch = new ListChooser<String>("Choose one", "Choose a stack offset value", 0, 1,
                    CardStackOffsetAction.keys);
            if (ch.show()) {
                try {
                    final int index = ch.getSelectedIndex();
                    if (index == -1) {
                        return;
                    }
                    CardStackOffsetAction.set(index);

                } catch (final Exception ex) {
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
            Singletons.getModel().getPreferences()
                    .setStackOffset(StackOffsetType.valueOf(CardStackOffsetAction.keys[index].toLowerCase()));
            Constant.Runtime.STACK_OFFSET[0] = CardStackOffsetAction.offsets[index];
        }

        /**
         * Sets the.
         * 
         * @param s
         *            the s
         */
        public static void set(final StackOffsetType s) {
            Singletons.getModel().getPreferences().setStackOffset(s);
            int index = 0;
            for (final String str : CardStackOffsetAction.keys) {
                if (str.toLowerCase().equals(s.toString())) {
                    break;
                }
                index++;
            }
            Constant.Runtime.STACK_OFFSET[0] = CardStackOffsetAction.offsets[index];
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
            super(ForgeProps.getLocalized(NewConstants.Lang.HowTo.TITLE));
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent
         * )
         */
        /**
         * Action performed.
         * 
         * @param e
         *            ActionEvent
         */
        @Override
        public final void actionPerformed(final ActionEvent e) {
            final String text = ForgeProps.getLocalized(NewConstants.Lang.HowTo.MESSAGE);

            final JTextArea area = new JTextArea(text, 25, 40);
            area.setWrapStyleWord(true);
            area.setLineWrap(true);
            area.setEditable(false);
            area.setOpaque(false);

            JOptionPane.showMessageDialog(null, new JScrollPane(area), ForgeProps.getLocalized(NewConstants.Lang.HowTo.TITLE),
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
            super(ForgeProps.getLocalized(NewConstants.Lang.OldGuiNewGame.MenuBar.Menu.ABOUT));
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent
         * )
         */
        /**
         * Action performed.
         * 
         * @param e
         *            ActionEvent
         */
        @Override
        public final void actionPerformed(final ActionEvent e) {
            final JTextArea area = new JTextArea(12, 25);

            if (OldGuiNewGame.getUseLAFFonts().isSelected()) {
                final Font f = new Font(area.getFont().getName(), Font.PLAIN, 13);
                area.setFont(f);
            }

            area.setText("The various people who have contributed to this project apologize with deep remorse"
                    + " for any bugs that you may have noticed.\n\nThe development team.\n\nOriginal author: Forge\n\n"
                    + "(Quest icons used created by Teekatas, from his Legendora set:\n"
                    + " http://raindropmemory.deviantart.com)");

            area.setWrapStyleWord(true);
            area.setLineWrap(true);
            area.setEditable(false);

            final JPanel p = new JPanel();
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
            super(ForgeProps.getLocalized(NewConstants.Lang.OldGuiNewGame.MenuBar.Menu.EXIT));
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent
         * )
         */
        /**
         * Action performed.
         * 
         * @param e
         *            ActionEvent
         */
        @Override
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
            final ForgePreferences preferences = Singletons.getModel().getPreferences();
            preferences.setLaf(UIManager.getLookAndFeel().getClass().getName());
            preferences.setLafFonts(OldGuiNewGame.getUseLAFFonts().isSelected());
            // preferences.newGui = newGuiCheckBox.isSelected();
            preferences.setStackAiLand(OldGuiNewGame.getSmoothLandCheckBox().isSelected());
            preferences.setMillingLossCondition(Constant.Runtime.MILL[0]);
            preferences.setDeveloperMode(Constant.Runtime.DEV_MODE[0]);
            preferences.setCardOverlay(OldGuiNewGame.getCardOverlay().isSelected());
            preferences.setScaleLargerThanOriginal(ImageCache.isScaleLargerThanOriginal());
            preferences.setUploadDraftAI(Constant.Runtime.UPLOAD_DRAFT[0]);
            preferences.save();
        } catch (final Exception ex) {
            final int result = JOptionPane.showConfirmDialog(this,
                    "Preferences could not be saved. Continue to close without saving ?", "Confirm Exit",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
            if (result != JOptionPane.OK_OPTION) {
                return false;
            }
        }

        this.setVisible(false);
        this.dispose();
        return true;
    }

    /** {@inheritDoc} */
    @Override
    protected final void processWindowEvent(final WindowEvent event) {
        if (event.getID() == WindowEvent.WINDOW_CLOSING) {
            if (!this.exit()) {
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
        @Override
        public void paintComponent(final Graphics g) {
            final Color bg = this.getBackground();
            g.setColor(new Color(bg.getRed(), bg.getGreen(), bg.getBlue(), 180));
            g.fillRoundRect(0, 0, this.getWidth() - 1, this.getHeight() - 1, this.radius, this.radius);
            g.setColor(new Color(0, 0, 0, 70));
            g.drawRoundRect(0, 0, this.getWidth() - 1, this.getHeight() - 1, this.radius, this.radius);
        }
    }

    /**
     * Load dynamic gamedata.
     */
    public static void loadDynamicGamedata() {
        if (!Constant.CardTypes.LOADED[0]) {
            final ArrayList<String> typeListFile = FileUtil.readFile("res/gamedata/TypeLists.txt");

            ArrayList<String> tList = null;

            Constant.CardTypes.CARD_TYPES[0] = new ConstantStringArrayList();
            Constant.CardTypes.SUPER_TYPES[0] = new ConstantStringArrayList();
            Constant.CardTypes.BASIC_TYPES[0] = new ConstantStringArrayList();
            Constant.CardTypes.LAND_TYPES[0] = new ConstantStringArrayList();
            Constant.CardTypes.CREATURE_TYPES[0] = new ConstantStringArrayList();
            Constant.CardTypes.INSTANT_TYPES[0] = new ConstantStringArrayList();
            Constant.CardTypes.SORCERY_TYPES[0] = new ConstantStringArrayList();
            Constant.CardTypes.ENCHANTMENT_TYPES[0] = new ConstantStringArrayList();
            Constant.CardTypes.ARTIFACT_TYPES[0] = new ConstantStringArrayList();
            Constant.CardTypes.WALKER_TYPES[0] = new ConstantStringArrayList();

            if (typeListFile.size() > 0) {
                for (int i = 0; i < typeListFile.size(); i++) {
                    final String s = typeListFile.get(i);

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
            final ArrayList<String> nskwListFile = FileUtil.readFile("res/gamedata/NonStackingKWList.txt");

            Constant.Keywords.NON_STACKING_LIST[0] = new ConstantStringArrayList();

            if (nskwListFile.size() > 1) {
                for (int i = 0; i < nskwListFile.size(); i++) {
                    final String s = nskwListFile.get(i);
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

    /**
     * @return the cardOverlay
     */
    public static JCheckBoxMenuItem getCardOverlay() {
        return cardOverlay;
    }

    /**
     * @param cardOverlay the cardOverlay to set
     */
    public static void setCardOverlay(JCheckBoxMenuItem cardOverlay) {
        OldGuiNewGame.cardOverlay = cardOverlay; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the cardScale
     */
    public static JCheckBoxMenuItem getCardScale() {
        return cardScale;
    }

    /**
     * @param cardScale the cardScale to set
     */
    public static void setCardScale(JCheckBoxMenuItem cardScale) {
        OldGuiNewGame.cardScale = cardScale; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the useLAFFonts
     */
    public static JCheckBoxMenuItem getUseLAFFonts() {
        return useLAFFonts;
    }

    /**
     * @param useLAFFonts the useLAFFonts to set
     */
    public static void setUseLAFFonts(JCheckBoxMenuItem useLAFFonts) {
        OldGuiNewGame.useLAFFonts = useLAFFonts; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the smoothLandCheckBox
     */
    static JCheckBox getSmoothLandCheckBox() {
        return smoothLandCheckBox;
    }

    /**
     * @param smoothLandCheckBox the smoothLandCheckBox to set
     */
    static void setSmoothLandCheckBox(JCheckBox smoothLandCheckBox) {
        OldGuiNewGame.smoothLandCheckBox = smoothLandCheckBox; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the devModeCheckBox
     */
    public static JCheckBox getDevModeCheckBox() {
        return devModeCheckBox;
    }

    /**
     * @param devModeCheckBox the devModeCheckBox to set
     */
    public static void setDevModeCheckBox(JCheckBox devModeCheckBox) {
        OldGuiNewGame.devModeCheckBox = devModeCheckBox; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the upldDrftCheckBox
     */
    public static JCheckBox getUpldDrftCheckBox() {
        return upldDrftCheckBox;
    }

    /**
     * @param upldDrftCheckBox the upldDrftCheckBox to set
     */
    public static void setUpldDrftCheckBox(JCheckBox upldDrftCheckBox) {
        OldGuiNewGame.upldDrftCheckBox = upldDrftCheckBox; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the foilRandomCheckBox
     */
    public static JCheckBox getFoilRandomCheckBox() {
        return foilRandomCheckBox;
    }

    /**
     * @param foilRandomCheckBox the foilRandomCheckBox to set
     */
    public static void setFoilRandomCheckBox(JCheckBox foilRandomCheckBox) {
        OldGuiNewGame.foilRandomCheckBox = foilRandomCheckBox; // TODO: Add 0 to parameter's name.
    }

}
