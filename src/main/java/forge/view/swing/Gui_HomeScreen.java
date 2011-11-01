package forge.view.swing;

import static net.slightlymagic.braids.util.UtilFunctions.safeToString;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.border.BevelBorder;

import org.eclipse.wb.swing.FocusTraversalOnArray;

import forge.AllZone;
import forge.CardList;
import forge.Command;
import forge.Constant;
import forge.GUI_ImportPicture;
import forge.GuiDisplay4;
import forge.Gui_DownloadPrices;
import forge.Gui_DownloadSetPictures_LQ;
import forge.MyRandom;
import forge.PlayerType;
import forge.Singletons;
import forge.deck.Deck;
import forge.deck.DeckGeneration;
import forge.deck.DeckManager;
import forge.deck.generate.Generate2ColorDeck;
import forge.deck.generate.Generate3ColorDeck;
import forge.deck.generate.GenerateConstructedMultiColorDeck;
import forge.deck.generate.GenerateThemeDeck;
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
import forge.properties.ForgePreferences.CardSizeType;
import forge.properties.ForgePreferences.StackOffsetType;
import forge.properties.ForgeProps;
import forge.properties.NewConstants.LANG;
import forge.properties.NewConstants.LANG.OldGuiNewGame.NEW_GAME_TEXT;
import forge.quest.gui.QuestOptions;

/**
 * The Class Gui_HomeScreen.
 */
public class Gui_HomeScreen {
    // Hack... WindowBuilder can't deal with path relative to the project folder
    // like "res/"
    // So... use a full path when debugging or designing with WindowBuilder
    // private String HomeScreenPath =
    // "/home/rob/ForgeSVN/ForgeSVN/res/images/ui/HomeScreen/";
    // And switch to relative path for distribution
    private String HomeScreenPath = "res/images/ui/HomeScreen/";

    private JFrame gHS;

    private JLabel lblBackground = new JLabel();
    private ImageIcon imgBackground = new ImageIcon(HomeScreenPath + "default_600/Main.jpg");

    // Interactive Elements
    private JLabel lblGameMode = new JLabel();
    private ImageIcon imgMode = new ImageIcon(HomeScreenPath + "default_600/btnMode_title.png");

    private JButton cmdConstructed = new JButton();
    private ImageIcon imgConstructedUp = new ImageIcon(HomeScreenPath + "default_600/btnMode_constrUp.png");
    private ImageIcon imgConstructedOver = new ImageIcon(HomeScreenPath + "default_600/btnMode_constrOver.png");
    private ImageIcon imgConstructedDown = new ImageIcon(HomeScreenPath + "default_600/btnMode_constrDown.png");
    private ImageIcon imgConstructedSel = new ImageIcon(HomeScreenPath + "default_600/btnMode_constrToggle2.png");

    private JButton cmdSealed = new JButton();
    private ImageIcon imgSealedUp = new ImageIcon(HomeScreenPath + "default_600/btnMode_sealedUp.png");
    private ImageIcon imgSealedOver = new ImageIcon(HomeScreenPath + "default_600/btnMode_sealedOver.png");
    private ImageIcon imgSealedDown = new ImageIcon(HomeScreenPath + "default_600/btnMode_sealedDown.png");
    private ImageIcon imgSealedSel = new ImageIcon(HomeScreenPath + "default_600/btnMode_sealedToggle2.png");

    private JButton cmdDraft = new JButton();
    private ImageIcon imgDraftUp = new ImageIcon(HomeScreenPath + "default_600/btnMode_draftUp.png");
    private ImageIcon imgDraftOver = new ImageIcon(HomeScreenPath + "default_600/btnMode_draftOver.png");
    private ImageIcon imgDraftDown = new ImageIcon(HomeScreenPath + "default_600/btnMode_draftDown.png");
    private ImageIcon imgDraftSel = new ImageIcon(HomeScreenPath + "default_600/btnMode_draftToggle2.png");

    private JButton cmdQuest = new JButton();
    private ImageIcon imgQuestUp = new ImageIcon(HomeScreenPath + "default_600/btnMode_questUp.png");
    private ImageIcon imgQuestOver = new ImageIcon(HomeScreenPath + "default_600/btnMode_questOver.png");
    private ImageIcon imgQuestDown = new ImageIcon(HomeScreenPath + "default_600/btnMode_questDown.png");
    private ImageIcon imgQuestSel = new ImageIcon(HomeScreenPath + "default_600/btnMode_questToggle2.png");

    private JLabel lblLibrary = new JLabel();
    private ImageIcon imgLibrary = new ImageIcon(HomeScreenPath + "default_600/btnLibr_title.png");

    private JButton cmdHumanDeck;
    private ImageIcon imgHumanUp = new ImageIcon(HomeScreenPath + "default_600/btnLibr_humanUp.png");
    private ImageIcon imgHumanOver = new ImageIcon(HomeScreenPath + "default_600/btnLibr_humanOver.png");
    private ImageIcon imgHumanDown = new ImageIcon(HomeScreenPath + "default_600/btnLibr_humanDown.png");
    private ImageIcon imgHumanSel = new ImageIcon(HomeScreenPath + "default_600/btnLibr_humanToggle2.png");

    private JButton cmdAIDeck;
    private ImageIcon imgAIUp = new ImageIcon(HomeScreenPath + "default_600/btnLibr_aiUp.png");
    private ImageIcon imgAIOver = new ImageIcon(HomeScreenPath + "default_600/btnLibr_aiOver.png");
    private ImageIcon imgAIDown = new ImageIcon(HomeScreenPath + "default_600/btnLibr_aiDown.png");
    private ImageIcon imgAISel = new ImageIcon(HomeScreenPath + "default_600/btnLibr_aiToggle2.png");

    private final JButton cmdDeckEditor = new JButton();
    private ImageIcon imgEditorUp = new ImageIcon(HomeScreenPath + "default_600/btnDeck_editorUp.png");
    private ImageIcon imgEditorOver = new ImageIcon(HomeScreenPath + "default_600/btnDeck_editorOver.png");
    private ImageIcon imgEditorDown = new ImageIcon(HomeScreenPath + "default_600/btnDeck_editorDown.png");

    private JButton cmdStart = new JButton();
    private ImageIcon imgStartUp = new ImageIcon(HomeScreenPath + "default_600/btnStart_Up.png");
    private ImageIcon imgStartOver = new ImageIcon(HomeScreenPath + "default_600/btnStart_Over.png");
    private ImageIcon imgStartDown = new ImageIcon(HomeScreenPath + "default_600/btnStart_Down.png");

    private final JButton cmdSettings = new JButton();
    private ImageIcon imgSettingsUp = new ImageIcon(HomeScreenPath + "default_600/btnSettings_unselected.png");
    private ImageIcon imgSettingsOver = new ImageIcon(HomeScreenPath + "default_600/btnSettings_hover.png");
    private ImageIcon imgSettingsDown = new ImageIcon(HomeScreenPath + "default_600/btnSettings_selected.png");

    private final JButton cmdUtilities = new JButton("");
    private ImageIcon imgUtilitiesUp = new ImageIcon(HomeScreenPath + "default_600/btnUtils_unselected.png");
    private ImageIcon imgUtilitiesOver = new ImageIcon(HomeScreenPath + "default_600/btnUtils_hover.png");
    private ImageIcon imgUtilitiesDown = new ImageIcon(HomeScreenPath + "default_600/btnUtils_selected.png");

    // Intro Panel
    private final JPanel pnlIntro = new JPanel();
    private JLabel lblIntro = new JLabel();

    // Deck Panel
    private final JPanel pnlDecks = new JPanel();
    private JLabel lblDecksHeader = new JLabel();
    private JList lstDecks = new JList();
    private final JScrollPane scrDecks = new JScrollPane();
    private final JButton cmdDeckSelect = new JButton("Select Deck");

    // Settings Panel
    private final JPanel pnlSettings = new JPanel();
    private final JScrollPane scrSettings = new JScrollPane();
    private final JPanel pnlSettingsA = new JPanel();
    private final JCheckBox chkStackAiLand = new JCheckBox("Stack AI Land");
    private final JCheckBox chkUploadDraftData = new JCheckBox("Upload Draft Data");
    private final JCheckBox chkDeveloperMode = new JCheckBox("Developer Mode");
    private final JCheckBox chkFoil = new JCheckBox("Random Foiling");
    private final JCheckBox chkMana = new JCheckBox("Use Text and Mana Overlay");
    private final JButton cmdLAF = new JButton("Choose Look and Feel");
    private final JCheckBox chkLAF = new JCheckBox("Use Look and Feel Fonts");
    private final JButton cmdSize = new JButton("Choose Card Size");
    private final JCheckBox chkScale = new JCheckBox("Scale Card Image Larger");
    private final JButton cmdStack = new JButton("Choose Stack Offset");
    private final JCheckBox chkRemoveArtifacts = new JCheckBox("Remove Artifacts");
    private final JCheckBox chkRemoveSmall = new JCheckBox("Remove Small Creatures");

    // Utilities Panel
    private final JPanel pnlUtilities = new JPanel();
    private final JButton cmdDownloadLQSetPics = new JButton("Download LQ Set Pics");
    private final JButton cmdDownloadPrices = new JButton("Download Prices");
    private final JButton cmdImportPics = new JButton("Import Pictures");
    private final JButton cmdReportBug = new JButton("Report Bug");
    private final JButton cmdHowToPlay = new JButton("How To Play");

    // Local objects
    private Color clrScrollBackground = new Color(222, 184, 135);

    private final DeckManager deckManager = AllZone.getDeckManager();
    private List<Deck> allDecks;
    private static DeckEditorCommon editor;

    private String PlayerSelected = "";
    private GameType GameTypeSelected = GameType.Constructed;
    private String HumanDeckSelected = "";
    private String AIDeckSelected = "";

    /**
     * Launch the application.
     * 
     * @param args
     *            the arguments
     */
    public static void main(final String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    Gui_HomeScreen window = new Gui_HomeScreen();
                    window.gHS.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Create the application.
     */
    public Gui_HomeScreen() {
        initialize();
    }

    /**
     * Initialize the contents of the frame.
     */
    private void initialize() {
        gHS = new JFrame();
        gHS.setIconImage(Toolkit.getDefaultToolkit().getImage(HomeScreenPath + "../favicon.png"));
        gHS.setTitle("Forge");
        gHS.setResizable(false);
        gHS.setBounds(100, 100, 605, 627);
        gHS.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        gHS.getContentPane().setLayout(null);
        lblGameMode.setFocusable(false);
        lblGameMode.setOpaque(false);
        lblGameMode.setBorder(null);
        lblGameMode.setIcon(imgMode);
        lblGameMode.setBounds(10, 187, 205, 30);
        gHS.getContentPane().add(lblGameMode);
        cmdConstructed.setSelectedIcon(imgConstructedSel);
        cmdConstructed.setBorderPainted(false);
        cmdConstructed.setBorder(null);
        cmdConstructed.setPressedIcon(imgConstructedDown);
        cmdConstructed.setRolloverEnabled(true);
        cmdConstructed.setRolloverIcon(imgConstructedOver);
        cmdConstructed.setOpaque(false);
        cmdConstructed.setIcon(imgConstructedUp);
        cmdConstructed.setAlignmentX(Component.CENTER_ALIGNMENT);
        cmdConstructed.setContentAreaFilled(false);
        cmdConstructed.setBounds(9, 217, 205, 26);
        cmdConstructed.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent arg0) {
                GameTypeSelected = GameType.Constructed;
                showDecks();
                doGameModeSelect();
            }
        });
        cmdConstructed.addFocusListener(new FocusListener() {
            public void focusGained(final FocusEvent arg0) {
                cmdConstructed.setIcon(imgConstructedOver);
            }

            public void focusLost(final FocusEvent arg0) {
                cmdConstructed.setIcon(imgConstructedUp);
            }
        });
        gHS.getContentPane().add(cmdConstructed);
        cmdSealed.setRolloverIcon(imgSealedOver);
        cmdSealed.setPressedIcon(imgSealedDown);
        cmdSealed.setRolloverEnabled(true);
        cmdSealed.setSelectedIcon(imgSealedSel);
        cmdSealed.setOpaque(false);
        cmdSealed.setBorder(null);
        cmdSealed.setBorderPainted(false);
        cmdSealed.setIcon(imgSealedUp);
        cmdSealed.setFont(new Font("Dialog", Font.BOLD, 10));
        cmdSealed.setAlignmentX(Component.CENTER_ALIGNMENT);
        cmdSealed.setContentAreaFilled(false);
        cmdSealed.setBounds(9, 243, 205, 26);
        cmdSealed.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent arg0) {
                GameTypeSelected = GameType.Sealed;
                showDecks();
                doGameModeSelect();
            }
        });
        cmdSealed.addFocusListener(new FocusListener() {
            public void focusGained(final FocusEvent arg0) {
                cmdSealed.setIcon(imgSealedOver);
            }

            public void focusLost(final FocusEvent arg0) {
                cmdSealed.setIcon(imgSealedUp);
            }
        });
        gHS.getContentPane().add(cmdSealed);
        cmdDraft.setSelectedIcon(imgDraftSel);
        cmdDraft.setRolloverIcon(imgDraftOver);
        cmdDraft.setRolloverEnabled(true);
        cmdDraft.setPressedIcon(imgDraftDown);
        cmdDraft.setOpaque(false);
        cmdDraft.setBorder(null);
        cmdDraft.setBorderPainted(false);
        cmdDraft.setIcon(imgDraftUp);
        cmdDraft.setFont(new Font("Dialog", Font.BOLD, 10));
        cmdDraft.setAlignmentX(Component.CENTER_ALIGNMENT);
        cmdDraft.setContentAreaFilled(false);
        cmdDraft.setBounds(9, 269, 205, 26);
        cmdDraft.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent arg0) {
                GameTypeSelected = GameType.Draft;
                showDecks();
                doGameModeSelect();
            }
        });
        cmdDraft.addFocusListener(new FocusListener() {
            public void focusGained(final FocusEvent arg0) {
                cmdDraft.setIcon(imgDraftOver);
            }

            public void focusLost(final FocusEvent arg0) {
                cmdDraft.setIcon(imgDraftUp);
            }
        });
        gHS.getContentPane().add(cmdDraft);
        cmdQuest.setRolloverIcon(imgQuestOver);
        cmdQuest.setRolloverEnabled(true);
        cmdQuest.setSelectedIcon(imgQuestSel);
        cmdQuest.setPressedIcon(imgQuestDown);
        cmdQuest.setOpaque(false);
        cmdQuest.setBorder(null);
        cmdQuest.setBorderPainted(false);
        cmdQuest.setIcon(imgQuestUp);
        cmdQuest.setFont(new Font("Dialog", Font.BOLD, 10));
        cmdQuest.setAlignmentX(Component.CENTER_ALIGNMENT);
        cmdQuest.setContentAreaFilled(false);
        cmdQuest.setBounds(9, 295, 205, 26);
        cmdQuest.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent arg0) {
                GameTypeSelected = GameType.Quest;
                showDecks();
                doGameModeSelect();
            }
        });
        cmdQuest.addFocusListener(new FocusListener() {
            public void focusGained(final FocusEvent arg0) {
                cmdQuest.setIcon(imgQuestOver);
            }

            public void focusLost(final FocusEvent arg0) {
                cmdQuest.setIcon(imgQuestUp);
            }
        });
        gHS.getContentPane().add(cmdQuest);
        lblLibrary.setFocusable(false);
        lblLibrary.setIcon(imgLibrary);
        lblLibrary.setOpaque(false);
        lblLibrary.setBounds(10, 338, 205, 30);
        gHS.getContentPane().add(lblLibrary);
        cmdHumanDeck = new JButton("");
        cmdHumanDeck.setSelectedIcon(imgHumanSel);
        cmdHumanDeck.setRolloverIcon(imgHumanOver);
        cmdHumanDeck.setPressedIcon(imgHumanDown);
        cmdHumanDeck.setRolloverEnabled(true);
        cmdHumanDeck.setIcon(imgHumanUp);
        cmdHumanDeck.setOpaque(false);
        cmdHumanDeck.setContentAreaFilled(false);
        cmdHumanDeck.setBorder(null);
        cmdHumanDeck.setBorderPainted(false);
        cmdHumanDeck.setBounds(8, 368, 205, 26);
        cmdHumanDeck.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent arg0) {
                PlayerSelected = "Human";
                showDecks();
            }
        });
        cmdHumanDeck.addFocusListener(new FocusListener() {
            public void focusGained(final FocusEvent arg0) {
                cmdHumanDeck.setIcon(imgHumanOver);
            }

            public void focusLost(final FocusEvent arg0) {
                cmdHumanDeck.setIcon(imgHumanUp);
            }
        });
        gHS.getContentPane().add(cmdHumanDeck);
        cmdAIDeck = new JButton("");
        cmdAIDeck.setSelectedIcon(imgAISel);
        cmdAIDeck.setPressedIcon(imgAIDown);
        cmdAIDeck.setRolloverIcon(imgAIOver);
        cmdAIDeck.setRolloverEnabled(true);
        cmdAIDeck.setIcon(imgAIUp);
        cmdAIDeck.setOpaque(false);
        cmdAIDeck.setContentAreaFilled(false);
        cmdAIDeck.setBorder(null);
        cmdAIDeck.setBorderPainted(false);
        cmdAIDeck.setBounds(8, 394, 205, 26);
        cmdAIDeck.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                PlayerSelected = "AI";
                showDecks();

            }
        });
        cmdAIDeck.addFocusListener(new FocusListener() {
            public void focusGained(final FocusEvent arg0) {
                cmdAIDeck.setIcon(imgAIOver);
            }

            public void focusLost(final FocusEvent arg0) {
                cmdAIDeck.setIcon(imgAIUp);
            }
        });
        gHS.getContentPane().add(cmdAIDeck);
        cmdDeckEditor.setFocusPainted(false);
        cmdDeckEditor.setPressedIcon(imgEditorDown);
        cmdDeckEditor.setRolloverIcon(imgEditorOver);
        cmdDeckEditor.setRolloverEnabled(true);
        cmdDeckEditor.setContentAreaFilled(false);
        cmdDeckEditor.setBorderPainted(false);
        cmdDeckEditor.setBorder(null);
        cmdDeckEditor.setOpaque(false);
        cmdDeckEditor.setIcon(imgEditorUp);
        cmdDeckEditor.setBounds(10, 436, 205, 30);
        cmdDeckEditor.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent arg0) {
                doShowEditor();
            }
        });
        cmdDeckEditor.addFocusListener(new FocusListener() {
            public void focusGained(final FocusEvent arg0) {
                cmdDeckEditor.setIcon(imgEditorOver);
            }

            public void focusLost(final FocusEvent arg0) {
                cmdDeckEditor.setIcon(imgEditorUp);
            }
        });
        gHS.getContentPane().add(cmdDeckEditor);
        cmdStart.setPressedIcon(imgStartDown);
        cmdStart.setRolloverIcon(imgStartOver);
        cmdStart.setRolloverEnabled(true);
        cmdStart.setIcon(imgStartUp);
        cmdStart.setOpaque(false);
        cmdStart.setContentAreaFilled(false);
        cmdStart.setBorder(null);
        cmdStart.setBorderPainted(false);
        cmdStart.setBounds(10, 476, 205, 84);
        cmdStart.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent arg0) {
                doStartGame();
            }
        });
        cmdStart.addFocusListener(new FocusListener() {
            public void focusGained(final FocusEvent arg0) {
                cmdStart.setIcon(imgStartDown);
            }

            public void focusLost(final FocusEvent arg0) {
                cmdStart.setIcon(imgStartUp);
            }
        });
        gHS.getContentPane().add(cmdStart);
        cmdSettings.setPressedIcon(imgSettingsDown);
        cmdSettings.setRolloverIcon(imgSettingsOver);
        cmdSettings.setRolloverEnabled(true);
        cmdSettings.setIcon(imgSettingsUp);
        cmdSettings.setOpaque(false);
        cmdSettings.setContentAreaFilled(false);
        cmdSettings.setBorder(null);
        cmdSettings.setBorderPainted(false);
        cmdSettings.setBounds(212, 10, 205, 50);
        cmdSettings.addFocusListener(new FocusListener() {
            public void focusGained(final FocusEvent arg0) {
                cmdSettings.setIcon(imgSettingsOver);
            }

            public void focusLost(final FocusEvent arg0) {
                cmdSettings.setIcon(imgSettingsUp);
            }
        });
        cmdSettings.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent arg0) {
                pnlIntro.setVisible(false);
                pnlDecks.setVisible(false);
                pnlUtilities.setVisible(false);

                pnlSettings.setVisible(true);
            }
        });
        gHS.getContentPane().add(cmdSettings);
        cmdUtilities.setIcon(imgUtilitiesUp);
        cmdUtilities.setRolloverEnabled(true);
        cmdUtilities.setRolloverIcon(imgUtilitiesOver);
        cmdUtilities.setPressedIcon(imgUtilitiesDown);
        cmdUtilities.setOpaque(false);
        cmdUtilities.setContentAreaFilled(false);
        cmdUtilities.setBorder(null);
        cmdUtilities.setBorderPainted(false);
        cmdUtilities.setBounds(395, 10, 205, 50);
        cmdUtilities.addFocusListener(new FocusListener() {
            public void focusGained(final FocusEvent arg0) {
                cmdUtilities.setIcon(imgUtilitiesOver);
            }

            public void focusLost(final FocusEvent arg0) {
                cmdUtilities.setIcon(imgUtilitiesUp);
            }
        });
        cmdUtilities.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent arg0) {
                pnlIntro.setVisible(false);
                pnlDecks.setVisible(false);
                pnlSettings.setVisible(false);

                pnlUtilities.setVisible(true);
            }
        });
        gHS.getContentPane().add(cmdUtilities);
        pnlIntro.setVisible(true);
        pnlIntro.setOpaque(false);
        pnlIntro.setBounds(245, 135, 325, 345);
        gHS.getContentPane().add(pnlIntro);
        pnlIntro.setLayout(null);
        lblIntro.setBounds(10, 10, 305, 300);
        lblIntro.setFont(new Font("", Font.BOLD, 12));
        lblIntro.setHorizontalAlignment(SwingConstants.LEFT);
        lblIntro.setOpaque(false);
        lblIntro.setFocusable(false);
        lblIntro.setText("<html>Forge is an open source implementation of Magic: the Gathering written in the Java programming language.<br><br>"
                + "<list><li>Select a Game Mode on the left</li><li>Select a Player</li><li>Choose a deck from the list</li><li>Click Select Deck</li><li>Press Start to begin the game</li></list></html>");
        pnlIntro.add(lblIntro);
        pnlDecks.setVisible(false);
        pnlDecks.setOpaque(false);
        pnlDecks.setBounds(245, 135, 325, 345);
        gHS.getContentPane().add(pnlDecks);
        pnlDecks.setLayout(null);
        lblDecksHeader.setBounds(10, 10, 305, 34);
        pnlDecks.add(lblDecksHeader);
        lblDecksHeader.setFont(new Font("", Font.BOLD | Font.ITALIC, 14));
        lblDecksHeader.setHorizontalAlignment(SwingConstants.CENTER);
        lblDecksHeader.setOpaque(false);
        lblDecksHeader.setFocusable(false);
        scrDecks.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrDecks.setOpaque(false);
        scrDecks.setBackground(clrScrollBackground);
        scrDecks.setBounds(10, 45, 305, 260);
        pnlDecks.add(scrDecks);
        lstDecks.setVisible(true);
        scrDecks.setViewportView(lstDecks);
        lstDecks.setBackground(clrScrollBackground);
        lstDecks.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
        cmdDeckSelect.setBounds(112, 310, 100, 23);
        pnlDecks.add(cmdDeckSelect);
        cmdDeckSelect.setBorder(new BevelBorder(BevelBorder.RAISED, null, null, null, null));
        cmdDeckSelect.setBackground(new Color(255, 222, 173));
        cmdDeckSelect.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                doDeckSelect();
            }
        });
        pnlSettings.setVisible(false);
        pnlSettings.setOpaque(false);
        pnlSettings.setBounds(245, 135, 325, 345);
        pnlSettings.setLayout(null);
        gHS.getContentPane().add(pnlSettings);
        scrSettings.setPreferredSize(new Dimension(1, 3));
        scrSettings.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrSettings.setBackground(clrScrollBackground);
        scrSettings.setOpaque(false);
        scrSettings.setBounds(10, 12, 305, 320);
        pnlSettings.add(scrSettings);
        pnlSettingsA.setBackground(clrScrollBackground);
        pnlSettingsA.setLayout(new GridLayout(15, 1, 0, 0));
        scrSettings.setViewportView(pnlSettingsA);
        JLabel lblBasic = new JLabel("<html><u>Basic Settings</u></html>");
        lblBasic.setHorizontalAlignment(SwingConstants.CENTER);
        pnlSettingsA.add(lblBasic);
        chkDeveloperMode.setOpaque(false);
        chkDeveloperMode.setBackground(clrScrollBackground);
        chkDeveloperMode.setSelected(Singletons.getModel().getPreferences().developerMode);
        chkDeveloperMode.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent arg0) {
                Singletons.getModel().getPreferences().developerMode = chkDeveloperMode.isSelected();
            }
        });
        pnlSettingsA.add(chkDeveloperMode);
        chkStackAiLand.setOpaque(false);
        chkStackAiLand.setBackground(clrScrollBackground);
        chkStackAiLand.setSelected(Singletons.getModel().getPreferences().stackAiLand);
        chkStackAiLand.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent arg0) {
                Singletons.getModel().getPreferences().stackAiLand = chkStackAiLand.isSelected();
            }
        });
        pnlSettingsA.add(chkStackAiLand);
        chkUploadDraftData.setBackground(clrScrollBackground);
        chkUploadDraftData.setOpaque(false);
        chkUploadDraftData.setSelected(Singletons.getModel().getPreferences().uploadDraftAI);
        chkUploadDraftData.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent arg0) {
                Singletons.getModel().getPreferences().uploadDraftAI = chkUploadDraftData.isSelected();
            }
        });
        pnlSettingsA.add(chkUploadDraftData);
        JLabel lblGraphs = new JLabel("<html><u>Graphical Settings</u></html>");
        lblGraphs.setHorizontalAlignment(SwingConstants.CENTER);
        pnlSettingsA.add(lblGraphs);
        chkMana.setOpaque(false);
        chkMana.setBackground(clrScrollBackground);
        chkMana.setSelected(Singletons.getModel().getPreferences().cardOverlay);
        chkMana.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent arg0) {
                Singletons.getModel().getPreferences().cardOverlay = chkMana.isSelected();
            }
        });
        pnlSettingsA.add(chkMana);
        chkFoil.setOpaque(false);
        chkFoil.setBackground(clrScrollBackground);
        chkFoil.setSelected(Singletons.getModel().getPreferences().randCFoil);
        chkFoil.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent arg0) {
                Singletons.getModel().getPreferences().randCFoil = chkFoil.isSelected();
            }
        });
        pnlSettingsA.add(chkFoil);
        // cmdLAF.setBorderPainted(false);
        // cmdLAF.setBorder(new BevelBorder(BevelBorder.RAISED, null,
        // null, null, null));
        cmdLAF.setOpaque(false);
        cmdLAF.setBackground(clrScrollBackground);
        cmdLAF.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent arg0) {
                doLAF();
            }
        });
        pnlSettingsA.add(cmdLAF);
        chkLAF.setOpaque(false);
        chkLAF.setBackground(clrScrollBackground);
        chkLAF.setSelected(Singletons.getModel().getPreferences().lafFonts);
        chkLAF.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent arg0) {
                Singletons.getModel().getPreferences().lafFonts = chkLAF.isSelected();
            }
        });
        pnlSettingsA.add(chkLAF);
        cmdSize.setOpaque(false);
        cmdSize.setBackground(clrScrollBackground);
        cmdSize.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent arg0) {
                doCardSize();
            }
        });
        pnlSettingsA.add(cmdSize);
        chkScale.setOpaque(false);
        chkScale.setBackground(clrScrollBackground);
        chkScale.setSelected(Singletons.getModel().getPreferences().scaleLargerThanOriginal);
        chkScale.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent arg0) {
                Singletons.getModel().getPreferences().scaleLargerThanOriginal = chkScale.isSelected();
            }
        });
        pnlSettingsA.add(chkScale);
        cmdStack.setOpaque(false);
        cmdStack.setBackground(clrScrollBackground);
        cmdStack.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent arg0) {
                doStackOffset();
            }
        });
        pnlSettingsA.add(cmdStack);
        JLabel lblGenGraphs = new JLabel("<html><u>Deck Generation Settings</u></html>");
        lblGenGraphs.setHorizontalAlignment(SwingConstants.CENTER);
        pnlSettingsA.add(lblGenGraphs);
        chkRemoveArtifacts.setOpaque(false);
        chkRemoveArtifacts.setBackground(clrScrollBackground);
        chkRemoveArtifacts.setSelected(Singletons.getModel().getPreferences().deckGenRmvArtifacts);
        chkRemoveArtifacts.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent arg0) {
                Singletons.getModel().getPreferences().deckGenRmvArtifacts = chkRemoveArtifacts.isSelected();
            }
        });
        pnlSettingsA.add(chkRemoveArtifacts);
        chkRemoveSmall.setOpaque(false);
        chkRemoveSmall.setBackground(clrScrollBackground);
        chkRemoveSmall.setSelected(Singletons.getModel().getPreferences().deckGenRmvSmall);
        chkRemoveSmall.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent arg0) {
                Singletons.getModel().getPreferences().deckGenRmvSmall = chkRemoveSmall.isSelected();
            }
        });
        pnlSettingsA.add(chkRemoveSmall);
        pnlUtilities.setOpaque(false);
        pnlUtilities.setVisible(false);
        pnlUtilities.setBounds(245, 135, 325, 345);
        pnlUtilities.setLayout(new GridLayout(5, 1, 0, 0));
        gHS.getContentPane().add(pnlUtilities);
        cmdDownloadLQSetPics.setOpaque(false);
        cmdDownloadLQSetPics.setBackground(clrScrollBackground);
        cmdDownloadLQSetPics.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent arg0) {
                new Gui_DownloadSetPictures_LQ(null);
            }
        });
        pnlUtilities.add(cmdDownloadLQSetPics);
        cmdDownloadPrices.setOpaque(false);
        cmdDownloadPrices.setBackground(clrScrollBackground);
        cmdDownloadPrices.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent arg0) {
                Gui_DownloadPrices gdp = new Gui_DownloadPrices();
                gdp.setVisible(true);
            }
        });
        pnlUtilities.add(cmdDownloadPrices);
        cmdImportPics.setOpaque(false);
        cmdImportPics.setBackground(clrScrollBackground);
        cmdImportPics.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent arg0) {
                GUI_ImportPicture ip = new GUI_ImportPicture(null);
                ip.setVisible(true);
            }
        });
        pnlUtilities.add(cmdImportPics);
        cmdReportBug.setOpaque(false);
        cmdReportBug.setBackground(clrScrollBackground);
        cmdReportBug.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent arg0) {
                BugzReporter br = new BugzReporter();
                br.setVisible(true);
            }
        });
        pnlUtilities.add(cmdReportBug);
        cmdHowToPlay.setOpaque(false);
        cmdHowToPlay.setBackground(clrScrollBackground);
        cmdHowToPlay.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent arg0) {
                String text = ForgeProps.getLocalized(LANG.HowTo.MESSAGE);

                JTextArea area = new JTextArea(text, 25, 40);
                area.setWrapStyleWord(true);
                area.setLineWrap(true);
                area.setEditable(false);
                area.setOpaque(false);

                JOptionPane.showMessageDialog(null, new JScrollPane(area),
                        ForgeProps.getLocalized(LANG.HowTo.TITLE), JOptionPane.INFORMATION_MESSAGE);
            }
        });
        pnlUtilities.add(cmdHowToPlay);
        lblBackground.setIcon(imgBackground);
        lblBackground.setBounds(0, 0, 600, 600);
        gHS.getContentPane().add(lblBackground);
        gHS.setFocusTraversalPolicy(new FocusTraversalOnArray(new Component[] {lblBackground, cmdConstructed,
                cmdSealed, cmdDraft, cmdQuest, cmdHumanDeck, cmdAIDeck, cmdDeckEditor, cmdSettings, cmdStart, lstDecks,
                cmdDeckSelect}));

        GuiUtils.centerFrame(gHS);
        
        // non gui init stuff
        allDecks = new ArrayList<Deck>(deckManager.getDecks());
    }

    private void doGameModeSelect() {
        // simulate a radio button group, because JRadioButton wasn't
        // transparent on Roll-over
        cmdConstructed.setSelected(GameTypeSelected.equals(GameType.Constructed));
        cmdSealed.setSelected(GameTypeSelected.equals(GameType.Sealed));
        cmdDraft.setSelected(GameTypeSelected.equals(GameType.Draft));
        cmdQuest.setSelected(GameTypeSelected.equals(GameType.Quest));
    }

    private void doDeckSelect() {
        if (lstDecks.getSelectedIndex() != -1) {
            if (PlayerSelected.equals("Human")) {
                HumanDeckSelected = lstDecks.getSelectedValue().toString();
                cmdHumanDeck.setSelected(true);
                cmdHumanDeck.setToolTipText(HumanDeckSelected);
            } else if (PlayerSelected.equals("AI")) {
                AIDeckSelected = lstDecks.getSelectedValue().toString();
                cmdAIDeck.setSelected(true);
                cmdAIDeck.setToolTipText(AIDeckSelected);
            }

        }
    }

    private boolean doDeckLogic() {
        if (GameTypeSelected.equals(GameType.Constructed)) {
            if (HumanDeckSelected.equals("Generate Deck")) {
                DeckGeneration.genDecks(PlayerType.HUMAN);

            } else if (HumanDeckSelected.equals("Random Deck")) {
                Deck rDeck = chooseRandomDeck();

                if (rDeck != null) {
                    String msg = String.format("You are using deck: %s.", Constant.Runtime.HUMAN_DECK[0].getName());
                    JOptionPane.showMessageDialog(null, msg, "Random Deck Name", JOptionPane.INFORMATION_MESSAGE);

                    Constant.Runtime.HUMAN_DECK[0] = rDeck;
                } else {
                    JOptionPane.showMessageDialog(null, "No decks available.", "Random Deck Name",
                            JOptionPane.INFORMATION_MESSAGE);
                    return false;
                }

            } else {
                Constant.Runtime.HUMAN_DECK[0] = deckManager.getDeck(HumanDeckSelected);
            }

            if (AIDeckSelected.equals("Generate Deck")) {
                DeckGeneration.genDecks(PlayerType.COMPUTER);

            } else if (AIDeckSelected.equals("Random Deck")) {
                Deck rDeck = chooseRandomDeck();

                if (rDeck != null) {
                    String msg = String.format("The computer is using deck: %s.",
                            Constant.Runtime.COMPUTER_DECK[0].getName());
                    JOptionPane.showMessageDialog(null, msg, "Random Deck Name", JOptionPane.INFORMATION_MESSAGE);

                    Constant.Runtime.COMPUTER_DECK[0] = rDeck;
                } else {
                    JOptionPane.showMessageDialog(null, "No decks available.", "Random Deck Name",
                            JOptionPane.INFORMATION_MESSAGE);
                    return false;
                }

            } else {
                Constant.Runtime.COMPUTER_DECK[0] = deckManager.getDeck(AIDeckSelected);
            }

        } else if (GameTypeSelected.equals(GameType.Sealed)) {
            if (HumanDeckSelected.equals("New Sealed")) {
                // NG2.dispose();

                launchSealed();

                return false;

            } else {
                if (!HumanDeckSelected.equals("") && !AIDeckSelected.equals("")) {
                    Constant.Runtime.HUMAN_DECK[0] = deckManager.getDeck(HumanDeckSelected);
                    Constant.Runtime.COMPUTER_DECK[0] = deckManager.getDeck(AIDeckSelected);
                }
            }
        } else if (GameTypeSelected.equals(GameType.Draft)) {
            if (HumanDeckSelected.equals("NewDraft")) {
                // NG2.dispose();

                launchDraft();

                return false;
            } else {
                if (!HumanDeckSelected.equals("") && !AIDeckSelected.equals("")) {
                    Constant.Runtime.HUMAN_DECK[0] = deckManager.getDraftDeck(HumanDeckSelected)[0];

                    String[] aiDeck = AIDeckSelected.split(" - ");
                    int AIDeckNum = Integer.parseInt(aiDeck[1]);
                    String AIDeckName = aiDeck[0];

                    Constant.Runtime.COMPUTER_DECK[0] = deckManager.getDraftDeck(AIDeckName)[AIDeckNum];
                }
            }
        }

        return true;
    }

    private void launchDraft() {
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

    private void launchSealed() {
        String[] sealedTypes = {"Full Cardpool", "Block / Set", "Custom"};

        String prompt = "Choose Sealed Deck Format:";
        Object o = GuiUtils.getChoice(prompt, sealedTypes);

        SealedDeck sd = null;

        if (o.toString().equals(sealedTypes[0])) {
            sd = new SealedDeck("Full");
        }

        else if (o.toString().equals(sealedTypes[1])) {
            sd = new SealedDeck("Block");
        }

        else if (o.toString().equals(sealedTypes[2])) {
            sd = new SealedDeck("Custom");
        }

        else {
            throw new IllegalStateException("choice <<" + safeToString(o)
                    + ">> does not equal any of the sealedTypes.");
        }

        ItemPool<CardPrinted> sDeck = sd.getCardpool();

        if (sDeck.countAll() > 1) {
            Deck deck = new Deck(GameType.Sealed);

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

            HumanDeckSelected = sDeckName;
            Constant.Runtime.HUMAN_DECK[0] = deck;
            AIDeckSelected = "AI_" + sDeckName;

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
            Constant.Runtime.COMPUTER_DECK[0] = aiDeck;

            showDecks();

            // cmdDeckEditor.doClick();
            // editor.customMenu.setCurrentGameType(Constant.GameType.Sealed);
            // editor.customMenu.showSealedDeck(deck);
        }
    }

    private Deck chooseGeneratedDeck(final String p) {
        Deck ret = null;

        ArrayList<String> decks = new ArrayList<String>();
        decks.add("5-Color Deck (original)");
        decks.add("Semi-Random Theme Deck");
        decks.add("2-Color Deck (new)");
        decks.add("3-Color Deck (new)");

        StringBuilder prompt = new StringBuilder("Generate ");
        if (p.equals("H")) {
            prompt.append("Human ");
        } else {
            prompt.append("Computer ");
        }
        prompt.append("Deck");

        Object o = GuiUtils.getChoice(prompt.toString(), decks.toArray());

        if (o.toString().equals(decks.get(0))) {
            GenerateConstructedMultiColorDeck gen = new GenerateConstructedMultiColorDeck();
            CardList FiveClrDeck = gen.generate5ColorDeck();
            ret = new Deck(GameType.Constructed);

            for (int i = 0; i < 60; i++) {
                ret.addMain(FiveClrDeck.get(i).getName());
            }

        } else if (o.toString().equals(decks.get(1))) {
            GenerateThemeDeck gen = new GenerateThemeDeck();
            ArrayList<String> tNames = gen.getThemeNames();
            tNames.add(0, "Random");
            Object t = GuiUtils.getChoice("Select a theme.", tNames.toArray());

            String stDeck;
            if (t.toString().equals("Random")) {
                Random r = MyRandom.getRandom();
                stDeck = tNames.get(r.nextInt(tNames.size() - 1) + 1);
            } else {
                stDeck = t.toString();
            }

            CardList td = gen.getThemeDeck(stDeck, 60);
            ret = new Deck(GameType.Constructed);

            for (int i = 0; i < td.size(); i++) {
                ret.addMain(td.get(i).getName());
            }

        } else if (o.toString().equals(decks.get(2))) {
            ArrayList<String> colors = new ArrayList<String>();
            colors.add("Random");
            for (String c : Constant.Color.ONLY_COLORS) {
                colors.add(c);
            }

            String c1;
            String c2;
            PlayerType pt = null;
            if (p.equals("H")) {
                pt = PlayerType.HUMAN;
                c1 = GuiUtils.getChoice("Select first color.", colors.toArray()).toString();

                if (c1.equals("Random")) {
                    c1 = colors.get(MyRandom.getRandom().nextInt(colors.size() - 1) + 1);
                }

                colors.remove(c1);

                c2 = GuiUtils.getChoice("Select second color.", colors.toArray()).toString();

                if (c2.equals("Random")) {
                    c2 = colors.get(MyRandom.getRandom().nextInt(colors.size() - 1) + 1);
                }
            } else {
                // if (p.equals("C"))
                pt = PlayerType.COMPUTER;
                c1 = colors.get(MyRandom.getRandom().nextInt(colors.size() - 1) + 1);
                colors.remove(c1);
                c2 = colors.get(MyRandom.getRandom().nextInt(colors.size() - 1) + 1);
            }
            Generate2ColorDeck gen = new Generate2ColorDeck(c1, c2);
            CardList d = gen.get2ColorDeck(60, pt);

            ret = new Deck(GameType.Constructed);

            for (int i = 0; i < d.size(); i++) {
                ret.addMain(d.get(i).getName());
            }

        } else if (o.toString().equals(decks.get(3))) {
            ArrayList<String> colors = new ArrayList<String>();
            colors.add("Random");
            for (String c : Constant.Color.ONLY_COLORS) {
                colors.add(c);
            }

            String c1;
            String c2;
            String c3;
            PlayerType pt = null;
            if (p.equals("H")) {
                pt = PlayerType.HUMAN;

                c1 = GuiUtils.getChoice("Select first color.", colors.toArray()).toString();

                if (c1.equals("Random")) {
                    c1 = colors.get(MyRandom.getRandom().nextInt(colors.size() - 1) + 1);
                }

                colors.remove(c1);

                c2 = GuiUtils.getChoice("Select second color.", colors.toArray()).toString();

                if (c2.equals("Random")) {
                    c2 = colors.get(MyRandom.getRandom().nextInt(colors.size() - 1) + 1);
                }

                colors.remove(c2);

                c3 = GuiUtils.getChoice("Select third color.", colors.toArray()).toString();
                if (c3.equals("Random")) {
                    c3 = colors.get(MyRandom.getRandom().nextInt(colors.size() - 1) + 1);
                }

            } else {
                // if (p.equals("C"))
                pt = PlayerType.COMPUTER;

                c1 = colors.get(MyRandom.getRandom().nextInt(colors.size() - 1) + 1);
                colors.remove(c1);
                c2 = colors.get(MyRandom.getRandom().nextInt(colors.size() - 1) + 1);
                colors.remove(c2);
                c3 = colors.get(MyRandom.getRandom().nextInt(colors.size() - 1) + 1);
            }
            Generate3ColorDeck gen = new Generate3ColorDeck(c1, c2, c3);
            CardList d = gen.get3ColorDeck(60, pt);

            ret = new Deck(GameType.Constructed);

            for (int i = 0; i < d.size(); i++) {
                ret.addMain(d.get(i).getName());
            }

        }

        return ret;
    }

    private Deck chooseRandomDeck() {
        Deck ret = null;

        ArrayList<Deck> subDecks = new ArrayList<Deck>();
        for (Deck d : allDecks) {
            if (d.getDeckType().equals(GameType.Constructed) && !d.isCustomPool()) {
                subDecks.add(d);
            }
        }

        if (subDecks.size() > 0) {
            int n = MyRandom.getRandom().nextInt(subDecks.size());
            ret = subDecks.get(n);

        } else {
            JOptionPane.showMessageDialog(null, "Not enough decks to choose from.", "Random Deck Name",
                    JOptionPane.INFORMATION_MESSAGE);
        }

        return ret;
    }

    private void showDecks() {
        deckManager.readAllDecks();

        lblDecksHeader.setText("");
        pnlIntro.setVisible(false);
        pnlDecks.setVisible(false);
        pnlSettings.setVisible(false);
        pnlUtilities.setVisible(false);

        DefaultListModel DeckList = new DefaultListModel();
        lstDecks.setModel(DeckList);

        if (GameTypeSelected.equals(GameType.Constructed)) {
            if (PlayerSelected.equals("Human")) {
                lblDecksHeader.setText("Your Constructed Decks");
            } else if (PlayerSelected.equals("AI")) {
                lblDecksHeader.setText("AI Constructed Decks");
            }

            if (!PlayerSelected.equals("")) {
                DeckList.addElement("Generate Deck");
                DeckList.addElement("Random Deck");

                for (Deck aDeck : allDecks) {
                    if (aDeck.getDeckType().equals(GameType.Constructed) && !aDeck.isCustomPool()) {
                        DeckList.addElement(aDeck.getName());
                    }
                }

            }

        } else if (GameTypeSelected.equals(GameType.Sealed)) {
            if (PlayerSelected.equals("Human")) {
                lblDecksHeader.setText("Your Sealed Decks");

                DeckList.addElement("New Sealed");

                for (Deck aDeck : allDecks) {
                    if (aDeck.getDeckType().equals(GameType.Sealed) && aDeck.getPlayerType() == PlayerType.HUMAN) {
                        DeckList.addElement(aDeck.getName());
                    }
                }
            } else if (PlayerSelected.equals("AI")) {
                lblDecksHeader.setText("AI Sealed Decks");

                for (Deck aDeck : allDecks) {
                    if (aDeck.getDeckType().equals(GameType.Sealed)
                            && aDeck.getPlayerType().equals(PlayerType.COMPUTER)) {
                        DeckList.addElement(aDeck.getName());
                    }
                }
            }

        } else if (GameTypeSelected.equals(GameType.Draft)) {
            if (PlayerSelected.equals("Human")) {
                lblDecksHeader.setText("Your Draft Decks");

                DeckList.addElement("New Draft");

                for (String sKey : deckManager.getDraftDecks().keySet()) {
                    DeckList.addElement(sKey);
                }

            } else if (PlayerSelected.equals("AI")) {
                lblDecksHeader.setText("AI Draft Decks");

                for (String sKey : deckManager.getDraftDecks().keySet()) {
                    for (int i = 1; i <= 7; i++) {
                        DeckList.addElement(sKey + " - " + i);
                    }
                }
            }

        } else if (cmdQuest.isSelected()) {
            lblDecksHeader.setText("");
            // lstDecks.setVisible(false);
            // cmdDeckSelect.setVisible(false);
        }

        if (!PlayerSelected.equals("") && !GameTypeSelected.equals(GameType.Quest)) {
            lstDecks.setModel(DeckList);
            // lstDecks.setVisible(true);
            // scrDecks.setVisible(true);
            pnlDecks.setVisible(true);
            // cmdDeckSelect.setVisible(true);
        }

    }

    private void doShowEditor() {
        if (editor == null) {

            editor = new DeckEditorCommon(GameType.Constructed);

            Command exit = new Command() {
                private static final long serialVersionUID = -9133358399503226853L;

                public void execute() {
                    String[] ng = {""};
                    Gui_HomeScreen.main(ng);
                }
            };
            editor.show(exit);
            editor.setVisible(true);
        } // if

        // refresh decks:
        allDecks = new ArrayList<Deck>(deckManager.getDecks());

        // TO-DO (TO have DOne) - this seems hacky. If someone knows how to do
        // this for real, feel free.
        // This make it so the second time you open the Deck Editor, typing a
        // card name and pressing enter will filter
        // editor.getRootPane().setDefaultButton(editor.filterButton);

        editor.setVisible(true);

        gHS.dispose();
    }

    private void doStartGame() {
        if (GameTypeSelected.equals(GameType.Quest)) {
            new QuestOptions();
        } else {
            if (HumanDeckSelected.equals("") && AIDeckSelected.equals("")) {
                return;
            }

            if (!doDeckLogic()) {
                return;
            }

            AllZone.setDisplay(new GuiDisplay4());
            AllZone.getGameAction().newGame(Constant.Runtime.HUMAN_DECK[0], Constant.Runtime.COMPUTER_DECK[0]);
            AllZone.getDisplay().setVisible(true);
        }

        Constant.Runtime.setGameType(GameTypeSelected);

        gHS.dispose();
    }

    private void doLAF() {
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
        LAFMap.put("Business Blue Steel", "org.pushingpixels.substance.api.skin.SubstanceBusinessBlueSteelLookAndFeel");
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
        LAFMap.put("Mist Aqua", "org.pushingpixels.substance.api.skin.SubstanceMistAquaLookAndFeel");
        LAFMap.put("Mist Silver", "org.pushingpixels.substance.api.skin.SubstanceMistSilverLookAndFeel");
        LAFMap.put("Moderate", "org.pushingpixels.substance.api.skin.SubstanceModerateLookAndFeel");
        LAFMap.put("Nebula", "org.pushingpixels.substance.api.skin.SubstanceNebulaLookAndFeel");
        LAFMap.put("Nebula Brick Wall", "org.pushingpixels.substance.api.skin.SubstanceNebulaBrickWallLookAndFeel");
        LAFMap.put("Office Blue 2007", "org.pushingpixels.substance.api.skin.SubstanceOfficeBlue2007LookAndFeel");
        LAFMap.put("Office Silver 2007", "org.pushingpixels.substance.api.skin.SubstanceOfficeSilver2007LookAndFeel");
        LAFMap.put("Raven", "org.pushingpixels.substance.api.skin.SubstanceRavenLookAndFeel");
        LAFMap.put("Raven Graphite", "org.pushingpixels.substance.api.skin.SubstanceRavenGraphiteLookAndFeel");
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

                // SwingUtilities.updateComponentTreeUI(NG2);
            } catch (Exception ex) {
                ErrorViewer.showError(ex);
            }
        }
    }

    private void doCardSize() {
        String[] keys = {"Tiny", "Smaller", "Small", "Medium", "Large(default)", "Huge"};
        int[] widths = { 52, 80, 120, 200, 300, 400 };
        int[] heights = { 50, 59, 88, 98, 130, 168 };

        ListChooser<String> ch = new ListChooser<String>("Choose one", "Choose a new max card size", 0, 1, keys);
        if (ch.show()) {
            try {
                int index = ch.getSelectedIndex();
                if (index == -1) {
                    return;
                }

                Singletons.getModel().getPreferences().cardSize = CardSizeType.valueOf(keys[index].toLowerCase());
                Constant.Runtime.WIDTH[0] = widths[index];
                Constant.Runtime.HEIGHT[0] = heights[index];

            } catch (Exception ex) {
                ErrorViewer.showError(ex);
            }
        }
    }

    private void doStackOffset() {
        String[] keys = {"Tiny", "Small", "Medium", "Large"};
        int[] offsets = { 5, 7, 10, 15 };

        ListChooser<String> ch = new ListChooser<String>("Choose one", "Choose a stack offset value", 0, 1, keys);
        if (ch.show()) {
            try {
                int index = ch.getSelectedIndex();
                if (index == -1) {
                    return;
                }
                Singletons.getModel().getPreferences().stackOffset = StackOffsetType.valueOf(keys[index].toLowerCase());
                Constant.Runtime.STACK_OFFSET[0] = offsets[index];

            } catch (Exception ex) {
                ErrorViewer.showError(ex);
            }
        }
    }
}
