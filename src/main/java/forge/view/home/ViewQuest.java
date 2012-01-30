package forge.view.home;

import java.awt.Color;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.border.MatteBorder;

import net.miginfocom.swing.MigLayout;
import forge.AllZone;
import forge.Singletons;
import forge.control.home.ControlQuest;
import forge.game.GameType;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;
import forge.quest.data.QuestChallenge;
import forge.quest.data.QuestDuel;
import forge.quest.data.QuestEvent;
import forge.view.toolbox.DeckLister;
import forge.view.toolbox.FCheckBox;
import forge.view.toolbox.FLabel;
import forge.view.toolbox.FProgressBar;
import forge.view.toolbox.FRadioButton;
import forge.view.toolbox.FRoundedPanel;
import forge.view.toolbox.FScrollPane;
import forge.view.toolbox.FSkin;
import forge.view.toolbox.FTextArea;

/** 
 * Populates Swing components of Quest mode in home screen.
 *
 */
@SuppressWarnings("serial")
public class ViewQuest extends JScrollPane {
    private final FSkin skin;
    private final HomeTopLevel parentView;
    private final ControlQuest control;
    private final String eventPanelConstraints;
    private final Color clrBorders;
    private final JPanel pnlViewport, pnlTabber, pnlStats,
        pnlDuels, pnlChallenges, pnlStart, pnlTitle, pnlNewQuest,
        pnlDecks, pnlLoadQuest, pnlPrefs,
        tabDuels, tabChallenges, tabDecks, tabQuests, tabPreferences;
    private final JLabel lblTitle, lblLife, lblCredits,
        lblWins, lblLosses, lblNextChallengeInWins, lblWinStreak;

    private final JButton btnBazaar, btnSpellShop, btnStart, btnEmbark, btnNewDeck, btnCurrentDeck;

    private final JCheckBox cbPlant, cbZep, cbStandardStart;
    private final JComboBox cbxPet;
    private final JRadioButton radEasy, radMedium, radHard, radExpert, radFantasy, radClassic;

    private SelectablePanel selectedOpponent;
    private DeckLister lstDecks;
    private QuestFileLister lstQuests;
    private final FProgressBar barProgress;

    /**
     * Populates Swing components of Quest mode in home screen.
     *
     * @param v0 &emsp; {@link forge.view.home.HomeTopLevel} parent view
     */
    public ViewQuest(final HomeTopLevel v0) {
        // Display
        super(VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_AS_NEEDED);
        this.setBorder(null);
        this.setOpaque(false);
        this.getViewport().setOpaque(false);
        this.getVerticalScrollBar().setUnitIncrement(16);

        // Non-final inits
        this.parentView = v0;
        this.skin = Singletons.getView().getSkin();
        this.clrBorders = skin.getColor(FSkin.Colors.CLR_THEME).darker().darker();
        this.eventPanelConstraints = "w 100%!, h 80px!, gap 0 0 5px 5px";

        // Final component inits
        tabDuels = new SubTab("Duels");
        tabChallenges = new SubTab("Challenges");
        tabDecks = new SubTab("Decks");
        tabQuests = new SubTab("Quests");
        tabPreferences = new SubTab("Preferences");

        pnlTabber = new JPanel();
        pnlTitle = new FRoundedPanel();
        pnlStats = new JPanel();
        pnlDuels = new JPanel();
        pnlChallenges = new JPanel();
        pnlStart = new JPanel();
        pnlDecks = new JPanel();
        pnlNewQuest = new JPanel();
        pnlLoadQuest = new JPanel();
        pnlPrefs = new JPanel();

        lblTitle = new FLabel("New Quest");
        lblLife = new FLabel();
        lblCredits = new FLabel();
        lblWins = new FLabel();
        lblLosses = new FLabel();
        lblNextChallengeInWins = new FLabel();
        lblWinStreak = new FLabel();

        radEasy = new FRadioButton("Easy");
        radMedium = new FRadioButton("Medium");
        radHard = new FRadioButton("Hard");
        radExpert = new FRadioButton("Expert");
        radFantasy = new FRadioButton("Fantasy");
        radClassic = new FRadioButton("Classic");

        btnCurrentDeck = new SubButton();
        btnBazaar = new SubButton("Bazaar");
        btnSpellShop = new SubButton("Spell Shop");
        btnStart = new StartButton(parentView);
        btnEmbark = new SubButton("Embark!");
        btnNewDeck = new SubButton("Build a New Deck");
        cbxPet = new JComboBox();
        cbStandardStart = new FCheckBox("Standard (Type 2) Starting Pool");
        cbPlant = new FCheckBox("Summon Plant");
        cbZep = new FCheckBox("Launch Zeppelin");
        barProgress = new FProgressBar();

        lstDecks = new DeckLister(GameType.Quest);
        lstQuests = new QuestFileLister();

        // Final layout of parent panel
        pnlViewport = new JPanel();
        pnlViewport.setOpaque(false);
        pnlViewport.setLayout(new MigLayout("insets 0, gap 0, wrap, alignx center, hidemode 3"));

        final String constraints = "w 90%!, gap 0 0 0 20px, alignx center";
        pnlViewport.add(pnlTabber, constraints + ", h 20px!");
        pnlViewport.add(pnlTitle, constraints + ", h 60px!");
        pnlViewport.add(pnlStats, constraints);
        pnlViewport.add(pnlDuels, constraints);
        pnlViewport.add(pnlChallenges, constraints);
        pnlViewport.add(pnlStart, constraints);
        pnlViewport.add(pnlLoadQuest, constraints);
        pnlViewport.add(pnlNewQuest, constraints);
        pnlViewport.add(pnlDecks, constraints);
        pnlViewport.add(pnlPrefs, constraints);

        // Drop into scroll pane, init values from controller.
        this.setViewportView(pnlViewport);

        // Lay out each child panel, starting with previous quests.
        populateLoadQuest();
        populateTabber();
        populateTitle();
        populateStats();
        populateDuels();
        populateChallenges();
        populateStart();
        populateDecks();
        populateNewQuest();
        populatePrefs();

        // Init controller, select quest and deck, then start in duels tab.
        this.control = new ControlQuest(this);
        control.refreshQuests();
        control.refreshDecks();
        this.showDuelsTab();
    }

    //========= POPULATION METHODS
    /** Layout and details for Swing components in title panel. */
    private void populateTabber() {
        tabDuels.setToolTipText("Available Duels");
        tabChallenges.setToolTipText("Available Challenges");
        tabDecks.setToolTipText("Edit or create decks");
        tabQuests.setToolTipText("Load a Quest, or start a new Quest");
        tabPreferences.setToolTipText("Change Preference Settings");

        final String constraints = "w 20%!, h 20px!";
        pnlTabber.setOpaque(false);
        pnlTabber.setLayout(new MigLayout("insets 0, gap 0, align center"));

        pnlTabber.add(tabDuels, constraints);
        pnlTabber.add(tabChallenges, constraints);
        pnlTabber.add(tabDecks, constraints);
        pnlTabber.add(tabQuests, constraints);
        pnlTabber.add(tabPreferences, constraints);
    }

    /** Layout and details for Swing components in title panel. */
    private void populateTitle() {
        pnlTitle.setLayout(new MigLayout("insets 0, gap 0, align center"));
        pnlTitle.setBackground(skin.getColor(FSkin.Colors.CLR_THEME).darker());
        ((FRoundedPanel) pnlTitle).setBorderColor(clrBorders);
        pnlTitle.add(lblTitle, "h 70%!, gap 0 0 0 10%!");
    }

    /** Layout permanent parts of stats panel. */
    private void populateStats() {
        pnlStats.setOpaque(false);
        pnlStats.setBorder(new MatteBorder(1, 0, 1, 0, clrBorders));

        lblLife.setIcon(skin.getIcon(FSkin.QuestIcons.ICO_LIFE));
        lblCredits.setIcon(skin.getIcon(FSkin.QuestIcons.ICO_COINSTACK));
        lblWins.setIcon(skin.getIcon(FSkin.QuestIcons.ICO_PLUS));
        lblLosses.setIcon(skin.getIcon(FSkin.QuestIcons.ICO_MINUS));
        lblNextChallengeInWins.setText("No challenges available.");
        btnBazaar.setToolTipText("Peruse the Bazaar");
        btnSpellShop.setToolTipText("Travel to the Spell Shop");
    }

    /** Layout permanent parts of duels panel. */
    private void populateDuels() {
        pnlDuels.setOpaque(false);
        pnlDuels.setLayout(new MigLayout("insets 0, wrap"));
    }

    /** Layout permanent parts of challenges panel. */
    private void populateChallenges() {
        pnlChallenges.setOpaque(false);
        pnlChallenges.setLayout(new MigLayout("insets 0, wrap"));
    }

    /** Layout permanent parts of start panel. */
    private void populateStart() {
        pnlStart.setOpaque(false);
        pnlStart.setLayout(new MigLayout("insets 0, wrap, align center, hidemode 3"));
        pnlStart.add(cbxPet, "gap 0 0 0 5px, align center");
        pnlStart.add(cbPlant, "gap 0 0 5px 5px, align center");
        pnlStart.add(cbZep, "gap 0 0 5px 5px, align center");
        pnlStart.add(btnStart, "");
    }

    /** Layout permanent parts of decks panel. */
    private void populateDecks() {
        final FScrollPane scr = new FScrollPane(lstDecks);
        scr.setBorder(null);
        scr.getViewport().setBorder(null);

        pnlDecks.setOpaque(false);
        pnlDecks.setLayout(new MigLayout("insets 0, wrap, alignx center, wrap"));

        pnlDecks.add(btnNewDeck, "w 40%!, h 35px!, gap 25%! 0 0 20px");
        pnlDecks.add(scr, "w 90%!, h 350px!");
    }

    /** Layout permanent parts of quest load panel. */
    private void populateLoadQuest() {
        // New quest notes
        final FRoundedPanel pnl = new FRoundedPanel();
        pnl.setLayout(new MigLayout("insets 0, align center"));
        pnl.setBorderColor(clrBorders);
        pnl.setBackground(skin.getColor(FSkin.Colors.CLR_THEME));
        pnl.add(new FLabel("Load a previous Quest"), "h 24px!, gap 2px 2px 2px 2px");

        final FLabel lbl = new FLabel("To use quest files "
                + "from previous versions, put them into "
                + "the res/quest/data directory, and restart Forge.", SwingConstants.CENTER);
        lbl.setFontScaleFactor(0.8);

        final FScrollPane scr = new FScrollPane(lstQuests);
        scr.setBorder(null);
        scr.getViewport().setBorder(null);

        pnlLoadQuest.setOpaque(false);
        pnlLoadQuest.setLayout(new MigLayout("insets 0, gap 0, alignx center, wrap"));
        pnlLoadQuest.add(pnl, "w 99%, gap 0 0 0 10px");
        pnlLoadQuest.add(lbl, "w 99%!, h 18px!, gap 2px 2px 0 4px");
        pnlLoadQuest.add(scr, "w 99%!, h 200px!, gap 0 0 0 30px");
    }

    /** Layout permanent parts of new quests panel. */
    private void populateNewQuest() {
        // New quest notes
        final FRoundedPanel pnl1 = new FRoundedPanel();
        pnl1.setLayout(new MigLayout("insets 0, align center"));
        pnl1.setBorderColor(clrBorders);
        pnl1.setBackground(skin.getColor(FSkin.Colors.CLR_THEME));
        pnl1.add(new FLabel("Start a new quest"), "h 24px!, gap 2px 2px 2px 2px");

        final ButtonGroup group1 = new ButtonGroup();
        group1.add(radEasy);
        group1.add(radMedium);
        group1.add(radHard);
        group1.add(radExpert);

        radEasy.setSelected(true);
        radClassic.setSelected(true);

        final ButtonGroup group2 = new ButtonGroup();
        group2.add(radFantasy);
        group2.add(radClassic);

        final JPanel pnl2 = new JPanel();
        pnl2.setOpaque(false);
        pnl2.setLayout(new MigLayout("insets 0, gap 0"));

        final String constraints = "w 30%!, h 40px!";
        pnl2.add(radEasy, constraints + ", gap 15% 5% 0 0");
        pnl2.add(radFantasy, constraints + ", wrap");
        pnl2.add(radMedium, constraints + ", gap 15% 5% 0 0");
        pnl2.add(radClassic, constraints + ", wrap");
        pnl2.add(radHard, constraints + ", gap 15% 5% 0 0");
        pnl2.add(cbStandardStart, constraints + ", wrap");
        pnl2.add(radExpert, constraints + ", gap 15% 5% 0 0, wrap");

        pnl2.add(btnEmbark, "w 40%!, h 30px!, gapleft 30%, gaptop 3%, span 3 1");

        pnlNewQuest.setLayout(new MigLayout("insets 0, gap 0, align center, wrap"));
        pnlNewQuest.setOpaque(false);
        pnlNewQuest.add(pnl1, "w 99%, gap 0 0 0 10px");
        pnlNewQuest.add(pnl2, "w 99%!");
    }

    /** Layout permanent parts of prefs panel. */
    private void populatePrefs() {
        pnlPrefs.setOpaque(false);
        pnlPrefs.setLayout(new MigLayout("insets 0, gap 0"));
        pnlPrefs.add(new QuestPreferencesHandler(), "w 100%!");
    }

    private void hideAllPanels() {
        pnlTitle.setVisible(false);
        pnlStats.setVisible(false);
        pnlDuels.setVisible(false);
        pnlChallenges.setVisible(false);
        pnlStart.setVisible(false);
        pnlDecks.setVisible(false);
        pnlNewQuest.setVisible(false);
        pnlLoadQuest.setVisible(false);
        pnlPrefs.setVisible(false);
    }

    //========= UPDATE METHODS
    /** Update transitory parts of duels panel. */
    public void updateDuels() {
        if (AllZone.getQuestData() == null) { return; }

        pnlDuels.removeAll();
        final List<QuestDuel> duels = control.getQEM().generateDuels();

        for (QuestDuel d : duels) {
            SelectablePanel temp = new SelectablePanel(d);
            pnlDuels.add(temp, this.eventPanelConstraints);
        }
    }

    /** Update transitory parts of challenges panel. */
    public void updateChallenges() {
        if (AllZone.getQuestData() == null) { return; }

        pnlChallenges.removeAll();
        final List<QuestChallenge> challenges = control.getQEM().generateChallenges();

        for (QuestChallenge c : challenges) {
            SelectablePanel temp = new SelectablePanel(c);
            pnlChallenges.add(temp, this.eventPanelConstraints);
        }
    }

    /** Update transitory parts of stats panel. */
    public void updateStats() {
        pnlStats.removeAll();

        if (AllZone.getQuestData().isFantasy()) {
            pnlStats.setLayout(new MigLayout("insets 0, gap 0"));

            pnlStats.add(btnBazaar,     "w 15%!, h 70px!, gap 0 4% 10px 10px, span 1 2");
            pnlStats.add(lblWins,       "w 30%!, h 25px!, gap 0 2% 12px 0");
            pnlStats.add(lblLosses,     "w 30%!, h 25px!, gap 0 4% 12px 0");
            pnlStats.add(btnSpellShop,   "w 14.5%!, h 70px!, gap 0 0 10px 10px, span 1 2, wrap");
            pnlStats.add(lblCredits,    "w 30%!, h 25px!, gap 0 2% 0 0");
            pnlStats.add(lblLife,       "w 30%!, h 25px!, gap 0 4% 0 0 0, wrap");
            pnlStats.add(lblWinStreak, "h 20px!, align center, span 4 1, wrap");
            pnlStats.add(lblNextChallengeInWins, "h 20px!, align center, span 4 1, wrap");
            pnlStats.add(btnCurrentDeck, "w 40%!, h 26px!, align center, span 4 1, gap 0 0 0 5px");
        }
        else {
            pnlStats.setLayout(new MigLayout("insets 0, gap 0, align center"));
            lblCredits.setHorizontalAlignment(SwingConstants.CENTER);

            pnlStats.add(lblWins,       "w 150px!, h 25px!, gap 0 50px 5px 5px, align center");
            pnlStats.add(lblCredits,    "w 150px!, h 25px!, gap 0 0 5px 5px, align center, wrap");
            pnlStats.add(lblLosses,     "w 150px!, h 25px!, gap 0 50px 0 5px, align center");
            pnlStats.add(btnSpellShop,  "w 150px!, h 25px!, gap 0 0 0 5px, align center, wrap");
            pnlStats.add(lblWinStreak, "h 20px!, align center, span 4 1, wrap");
            pnlStats.add(lblNextChallengeInWins, "h 20px!, align center, span 4 1, gap 0 0 10px 5px, wrap");
            pnlStats.add(btnCurrentDeck, "w 40%!, h 26px!, align center, span 4 1, gap 0 0 0 5px");
        }
    }

    //========= TAB SHOW METHODS
    /** Display handler for duel tab click. */
    public void showDuelsTab() {
        control.updateTabber(tabDuels);
        this.hideAllPanels();
        pnlTitle.setVisible(true);

        if (AllZone.getQuestData() == null) {
            lblTitle.setText("Start a new Quest in the \"Quests\" tab.");
            return;
        }

        setCurrentDeckStatus();
        updateDuels();
        updateStats();
        lblTitle.setText("Duels: " + control.getRankString());
        pnlStats.setVisible(true);
        pnlDuels.setVisible(true);

        if (control.getCurrentDeck() != null) {
            pnlStart.setVisible(true);

            // Select first event.
            selectedOpponent = (SelectablePanel) pnlDuels.getComponent(0);
            selectedOpponent.setBackground(skin.getColor(FSkin.Colors.CLR_ACTIVE));
        }
    }

    /** Display handler for duel tab click. */
    public void showChallengesTab() {
        control.updateTabber(tabChallenges);
        this.hideAllPanels();
        pnlTitle.setVisible(true);

        if (AllZone.getQuestData() == null) {
            lblTitle.setText("Start a new Quest in the \"Quests\" tab.");
            return;
        }

        setCurrentDeckStatus();
        updateChallenges();
        updateStats();
        lblTitle.setText("Challenges: " + control.getRankString());
        pnlStats.setVisible(true);
        pnlChallenges.setVisible(true);

        // Select first event.
        if (pnlChallenges.getComponentCount() > 0) {
            pnlStart.setVisible(true);
            selectedOpponent = (SelectablePanel) pnlChallenges.getComponent(0);
            selectedOpponent.setBackground(skin.getColor(FSkin.Colors.CLR_ACTIVE));
        }

        this.getViewport().setViewPosition(new Point(0, 0));
    }

    /** Display handler for decks tab click. */
    public void showDecksTab() {
        control.updateTabber(tabDecks);
        this.hideAllPanels();
        pnlTitle.setVisible(true);

        if (AllZone.getQuestData() == null) {
            lblTitle.setText("Start a new Quest in the \"Quests\" tab.");
            return;
        }
        else {
            lblTitle.setText("Quest Deck Manager");
            pnlDecks.setVisible(true);
        }
    }

    /** Display handler for quests tab click. */
    public void showQuestsTab() {
        control.updateTabber(tabQuests);
        this.hideAllPanels();

        pnlNewQuest.setVisible(true);
        pnlLoadQuest.setVisible(true);
    }

    /** Display handler for quests tab click. */
    public void showPrefsTab() {
        control.updateTabber(tabPreferences);

        this.hideAllPanels();

        lblTitle.setText("Quest Preferences");
        pnlTitle.setVisible(true);
        pnlPrefs.setVisible(true);
    }

    /** Toggles red, bold font if no current deck. */
    public void setCurrentDeckStatus() {
        if (control.getCurrentDeck() == null) {
            btnCurrentDeck.setBackground(Color.red.darker());
            btnCurrentDeck.setText("  Build, then select a deck in the \"Decks\" tab.  ");
        }
        else {
            btnCurrentDeck.setBackground(skin.getColor(FSkin.Colors.CLR_INACTIVE));
            btnCurrentDeck.setText("Current deck: " + control.getCurrentDeck().getName());
        }
    }

    /** Selectable panels for duels and challenges. */
    public class SelectablePanel extends FRoundedPanel {
        private QuestEvent event;
        private Color clrDefault, clrHover, clrSelected;

        /** @param e0 &emsp; QuestEvent */
        public SelectablePanel(QuestEvent e0) {
            super();
            this.clrSelected = skin.getColor(FSkin.Colors.CLR_ACTIVE);
            this.clrDefault = skin.getColor(FSkin.Colors.CLR_INACTIVE);
            this.clrHover = skin.getColor(FSkin.Colors.CLR_HOVER);
            this.event = e0;

            this.setBackground(clrDefault);
            this.setLayout(new MigLayout("insets 0, gap 0"));

            final File base = ForgeProps.getFile(NewConstants.IMAGE_ICON);
            File file = new File(base, event.getIconFilename());

            FLabel lblIcon = new FLabel();
            lblIcon.setIconScaleFactor(1);
            lblIcon.setForeground(skin.getColor(FSkin.Colors.CLR_TEXT));
            if (!file.exists()) {
                lblIcon.setIcon(skin.getIcon(FSkin.ForgeIcons.ICO_UNKNOWN));
            }
            else {
                lblIcon.setIcon(new ImageIcon(file.toString()));
            }
            this.add(lblIcon, "h 60px!, w 60px!, gap 10px 10px 10px 0, span 1 2");

            // Name
            final FLabel lblName = new FLabel(event.getTitle() + ": " + event.getDifficulty());
            lblName.setFontScaleFactor(1);
            this.add(lblName, "h 20px!, gap 0 0 10px 5px, wrap");

            // Description
            final FTextArea tarDesc = new FTextArea();
            tarDesc.setText(event.getDescription());
            tarDesc.setFont(skin.getItalicFont(12));
            this.add(tarDesc, "w 80%!, h 30px!");

            this.setToolTipText("<html>" + event.getTitle()
                    + ": " + event.getDifficulty()
                    + "<br>" + event.getDescription()
                    + "</html>");

            this.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent e) {
                    SelectablePanel src = (SelectablePanel) e.getSource();

                    if (selectedOpponent != null) {
                        selectedOpponent.setBackground(clrDefault);
                    }

                    selectedOpponent = src;
                    src.setBackground(clrSelected);
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    if (selectedOpponent != e.getSource()) {
                        setBackground(clrHover);
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    if (selectedOpponent != e.getSource()) {
                        setBackground(clrDefault);
                    }
                }
            });
       }

        /** @return QuestEvent */
        public QuestEvent getEvent() {
            return event;
        }
    }

    //========= RETRIEVAL FUNCTIONS
    /** @return {@link javax.swing.JRadioButton} */
    public JRadioButton getRadEasy() {
        return radEasy;
    }

    /** @return {@link javax.swing.JRadioButton} */
    public JRadioButton getRadMedium() {
        return radMedium;
    }

    /** @return {@link javax.swing.JRadioButton} */
    public JRadioButton getRadHard() {
        return radHard;
    }

    /** @return {@link javax.swing.JRadioButton} */
    public JRadioButton getRadExpert() {
        return radExpert;
    }

    /** @return {@link javax.swing.JRadioButton} */
    public JRadioButton getRadFantasy() {
        return radFantasy;
    }

    /** @return {@link javax.swing.JRadioButton} */
    public JRadioButton getRadClassic() {
        return radClassic;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getCbStandardStart() {
        return cbStandardStart;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getCbPlant() {
        return cbPlant;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getCbZep() {
        return cbZep;
    }

    /** @return {@link javax.swing.JComboBox} */
    public JComboBox getCbxPet() {
        return cbxPet;
    }

    /** @return {@link forge.view.home.ViewQuest.SelectablePanel} */
    public SelectablePanel getSelectedOpponent() {
        return selectedOpponent;
    }

    /** @return {@link forge.view.home.HomeTopLevel} */
    public HomeTopLevel getParentView() {
        return parentView;
    }

    /** @return {@link forge.control.home.ControlQuest} */
    public ControlQuest getController() {
        return control;
    }

    /** @return {@link javax.swing.JLabel} */
    public JLabel getLblTitle() {
        return lblTitle;
    }

    /** @return {@link javax.swing.JLabel} */
    public JLabel getLblLife() {
        return lblLife;
    }

    /** @return {@link javax.swing.JLabel} */
    public JLabel getLblCredits() {
        return lblCredits;
    }

    /** @return {@link javax.swing.JLabel} */
    public JLabel getLblWins() {
        return lblWins;
    }

    /** @return {@link javax.swing.JLabel} */
    public JLabel getLblLosses() {
        return lblLosses;
    }

    /** @return {@link javax.swing.JLabel} */
    public JLabel getLblNextChallengeInWins() {
        return lblNextChallengeInWins;
    }

    /** @return {@link javax.swing.JLabel} */
    public JLabel getLblWinStreak() {
        return lblWinStreak;
    }

    /** @return {@link javax.swing.JButton} */
    public JButton getBtnCurrentDeck() {
        return btnCurrentDeck;
    }

    /** @return {@link javax.swing.JButton} */
    public JButton getBtnStart() {
        return btnStart;
    }

    /** @return {@link javax.swing.JButton} */
    public JButton getBtnBazaar() {
        return btnBazaar;
    }

    /** @return {@link javax.swing.JButton} */
    public JButton getBtnSpellShop() {
        return btnSpellShop;
    }

    /** @return {@link javax.swing.JButton} */
    public JButton getBtnEmbark() {
        return btnEmbark;
    }

    /** @return {@link javax.swing.JButton} */
    public JButton getBtnNewDeck() {
        return btnNewDeck;
    }

    /** @return {@link forge.view.toolbox.FProgressBar} */
    public FProgressBar getBarProgress() {
        return barProgress;
    }

    //========== CONTAINER RETRIEVAL
    /** @return {@link javax.swing.JPanel} */
    public JPanel getPnlStats() {
        return this.pnlStats;
    }

    /** @return {@link javax.swing.JPanel} */
    public JPanel getPnlTitle() {
        return this.pnlTitle;
    }

    /** @return {@link javax.swing.JPanel} */
    public JPanel getPnlDuels() {
        return this.pnlDuels;
    }

    /** @return {@link javax.swing.JPanel} */
    public JPanel getPnlChallenges() {
        return this.pnlChallenges;
    }

    /** @return {@link javax.swing.JPanel} */
    public JPanel getPnlStart() {
        return this.pnlStart;
    }

    /** @return {@link javax.swing.JPanel} */
    public JPanel getPnlLoadQuest() {
        return pnlLoadQuest;
    }

    /** @return {@link javax.swing.JPanel} */
    public JPanel getpnlPrefs() {
        return pnlPrefs;
    }

    /** @return {@link forge.view.toolbox.DeckLister} */
    public DeckLister getLstDecks() {
        return this.lstDecks;
    }

    /** @return {@link forge.view.home.QuestFileLister} */
    public QuestFileLister getLstQuests() {
        return this.lstQuests;
    }

    //========== TAB RETRIEVAL
    /** @return {@link javax.swing.JPanel} */
    public JPanel getTabDuels() {
        return tabDuels;
    }

    /** @return {@link javax.swing.JPanel} */
    public JPanel getTabChallenges() {
        return tabChallenges;
    }

    /** @return {@link javax.swing.JPanel} */
    public JPanel getTabDecks() {
        return tabDecks;
    }

    /** @return {@link javax.swing.JPanel} */
    public JPanel getTabQuests() {
        return tabQuests;
    }

    /** @return {@link javax.swing.JPanel} */
    public JPanel getTabPreferences() {
        return tabPreferences;
    }
}
