package forge.view.home;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;

import net.miginfocom.swing.MigLayout;
import forge.AllZone;
import forge.control.home.ControlQuest;
import forge.gui.GuiUtils;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;
import forge.quest.data.QuestData;
import forge.quest.data.QuestDataIO;
import forge.quest.data.item.QuestItemZeppelin;
import forge.quest.data.pet.QuestPetAbstract;
import forge.quest.gui.main.QuestChallenge;
import forge.quest.gui.main.QuestDuel;
import forge.quest.gui.main.QuestEvent;
import forge.quest.gui.main.QuestEventManager;
import forge.view.toolbox.FSkin;

/** 
 * Populates Swing components of Quest mode in home screen.
 *
 */
@SuppressWarnings("serial")
public class ViewQuest extends JScrollPane {
    private FSkin skin;
    private HomeTopLevel parentView;
    private QuestEventManager qem;
    private QuestData questData;
    private JPanel viewport;
    private SelectablePanel selectedOpponent;
    private JList lstDeckChooser;
    private ControlQuest control;
    private JRadioButton radEasy, radMedium, radHard, radExpert, radFantasy, radClassic;
    private JCheckBox cbStandardStart, cbPlant, cbZep;
    private JComboBox cbxPet;
    private JLabel lblPlant, lblPet, lblZep, lblLife, lblCredits;
    private boolean previousQuestExists = false;

    /**
     * Populates Swing components of Quest mode in home screen.
     *
     * @param v0 &emsp; HomeTopLevel parent view
     */
    public ViewQuest(HomeTopLevel v0) {
        // Basic init stuff
        super(VERTICAL_SCROLLBAR_ALWAYS, HORIZONTAL_SCROLLBAR_AS_NEEDED);
        this.setOpaque(false);
        this.setBorder(null);
        this.getVerticalScrollBar().setUnitIncrement(16);
        parentView = v0;
        skin = AllZone.getSkin();

        // Title and viewport.  Panel is put into scroll pane for resize safety.
        viewport = new JPanel();
        viewport.setOpaque(false);
        viewport.setLayout(new MigLayout("insets 0, gap 0, wrap 2"));
        this.getViewport().setOpaque(false);

        JLabel lblTitle = new JLabel();
        lblTitle.setOpaque(true);
        lblTitle.setBorder(new MatteBorder(0, 0, 1, 0, skin.getColor("borders")));
        lblTitle.setForeground(skin.getColor("text"));
        lblTitle.setBackground(skin.getColor("theme").darker());
        lblTitle.setFont(skin.getFont1().deriveFont(Font.BOLD, 20));
        viewport.add(lblTitle, "w 90%!, h 50px!, gap 5% 0 2% 0, span 2");

        File f = new File("res/quest/questData.dat");
        if (f.exists()) {
            AllZone.setQuestData(QuestDataIO.loadData());
            questData = AllZone.getQuestData();
            previousQuestExists = true;

            lblTitle.setText("   " + questData.getRank());

            JLabel lblStats = new JLabel("Wins: " + questData.getWin()
                    + " / Losses: " + questData.getLost());
            lblStats.setForeground(skin.getColor("text"));
            lblStats.setFont(skin.getFont1().deriveFont(Font.BOLD, 17));
            lblStats.setHorizontalAlignment(SwingConstants.CENTER);
            viewport.add(lblStats, "h 35px!, ax center, span 2");

            // Quest events
            populateQuestEvents();

            // Quest options
            populateQuestOptions();

            // Start button
            populateStartArea();
        }
        else {
            lblTitle.setText("    New Quest");
        }

        // New Quest
        populateNewQuest();

        // Drop into scroll pane, init controller.
        this.setViewportView(viewport);
        control = new ControlQuest(this);
    }

    //========= POPULATION METHODS
    //...mainly here to avoid one big lump of a constructor.

    private void populateQuestEvents() {
        // Retrieve quest events, or generate (on first run)
        this.qem = AllZone.getQuestEventManager();

        if (this.qem == null) {
            this.qem = new QuestEventManager();
            this.qem.assembleAllEvents();
            AllZone.setQuestEventManager(this.qem);
        }

        JPanel duelsContainer = new JPanel();
        duelsContainer.setOpaque(false);
        duelsContainer.setLayout(new MigLayout("insets 0, gap 0, wrap"));

        JPanel challengesContainer = new JPanel();
        challengesContainer.setOpaque(false);
        challengesContainer.setLayout(new MigLayout("insets 0, gap 0, wrap"));

        List<QuestDuel> duels = qem.generateDuels();
        List<QuestChallenge> challenges = qem.generateChallenges();

        for (QuestDuel d : duels) {
            SelectablePanel temp = new SelectablePanel(d);
            duelsContainer.add(temp, "w 100%, h 70px:70px, gapbottom 5px");
        }

        for (QuestChallenge c : challenges) {
            SelectablePanel temp = new SelectablePanel(c);
            challengesContainer.add(temp, "w 100%, h 70px:70px, gapbottom 5px");
        }

        if (challenges.size() == 0) {
            JLabel lblTeaser = new JLabel("(Next challenge available in "
                    + nextChallengeInWins() + " wins.)");
            lblTeaser.setHorizontalAlignment(SwingConstants.CENTER);
            lblTeaser.setForeground(skin.getColor("text"));
            lblTeaser.setFont(skin.getFont1().deriveFont(Font.BOLD, 16));
            challengesContainer.add(lblTeaser, "w 100%!, ax center, ay top");
        }

        JLabel lblDuels = new JLabel("Available Duels");
        lblDuels.setForeground(skin.getColor("text"));
        lblDuels.setHorizontalAlignment(SwingConstants.CENTER);
        lblDuels.setFont(skin.getFont1().deriveFont(Font.ITALIC, 14));

        JLabel lblChallenges = new JLabel("Available Challenges");
        lblChallenges.setForeground(skin.getColor("text"));
        lblChallenges.setHorizontalAlignment(SwingConstants.CENTER);
        lblChallenges.setFont(skin.getFont1().deriveFont(Font.ITALIC, 14));

        viewport.add(lblDuels, "w 48%, gap 1% 1% 2% 1%");
        viewport.add(lblChallenges, "w 48%, gap 0 0 2% 1%, wrap");
        viewport.add(duelsContainer, " w 48%, gap 1% 1% 1% 2%, ay top");
        viewport.add(challengesContainer, " w 48%, gap 0 0 1% 2%, wrap");

        // Select first event.
        selectedOpponent = (SelectablePanel) duelsContainer.getComponent(0);
        selectedOpponent.setBackground(skin.getColor("active"));
    }

    /** */
    private void populateQuestOptions() {
        JPanel optionsContainer = new JPanel();
        optionsContainer.setOpaque(false);
        optionsContainer.setLayout(new MigLayout("insets 0, gap 0"));
        optionsContainer.setBorder(new MatteBorder(0, 0, 1, 0, skin.getColor("borders")));

        lblCredits = new JLabel("Credits: " + Long.toString(questData.getCredits()));
        lblCredits.setIcon(GuiUtils.getResizedIcon(new ImageIcon("res/images/icons/CoinStack.png"), 26, 26));
        lblCredits.setForeground(skin.getColor("text"));
        lblCredits.setIconTextGap(5);
        lblCredits.setHorizontalAlignment(SwingConstants.CENTER);
        lblCredits.setFont(skin.getFont1().deriveFont(Font.BOLD, 14));

        lblLife = new JLabel("Life: " + Long.toString(questData.getLife()));
        lblLife.setIcon(GuiUtils.getResizedIcon(new ImageIcon("res/images/icons/Life.png"), 26, 26));
        lblLife.setForeground(skin.getColor("text"));
        lblLife.setIconTextGap(5);
        lblLife.setHorizontalAlignment(SwingConstants.CENTER);
        lblLife.setFont(skin.getFont1().deriveFont(Font.BOLD, 14));

        SubButton btnEditor = new SubButton("");
        btnEditor.setAction(new AbstractAction() {
           @Override
           public void actionPerformed(ActionEvent e) {
               control.showDeckEditor();
           }
        });
        btnEditor.setText("Deck Editor");

        SubButton btnCardShop = new SubButton("");
        btnCardShop.setAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                control.showCardShop();
            }
         });
        btnCardShop.setText("Card Shop");

        SubButton btnBazaar = new SubButton("");
        btnBazaar.setAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                control.showBazaar();
            }
         });
        btnBazaar.setText("Bazaar");

        lstDeckChooser = new JList();

        optionsContainer.add(btnEditor, "w 35%, h 30px!, gap 10% 5% 10px 10px");
        optionsContainer.add(lblCredits, "w 35%!, h 30px!, wrap");

        optionsContainer.add(new JScrollPane(lstDeckChooser), "w 35%, h 110px!, gap 10% 5% 0 10px, span 1 3");
        optionsContainer.add(lblLife, "w 35%, h 30px!, gap 0 0 0 10px, wrap");

        optionsContainer.add(btnCardShop, "w 35%, h 30px!, gap 0 0 0 10px, wrap");
        optionsContainer.add(btnBazaar, "w 35%, h 30px!, gap 0 0 0 10px, wrap");

        if (!questData.isFantasy()) {
            lblLife.setVisible(false);
            btnBazaar.setVisible(false);
        }

        viewport.add(optionsContainer, "w 90%, gap 5% 0 1% 1%, span 2 1");
    }

    private void populateStartArea() {
        JPanel pnlButtonContainer = new JPanel();
        pnlButtonContainer.setOpaque(false);
        pnlButtonContainer.setLayout(new MigLayout("insets 0, gap 0, wrap 2, ax center, hidemode 3"));

        cbxPet = new JComboBox();
        cbxPet.setFont(skin.getFont1().deriveFont(Font.PLAIN, 14));

        cbPlant = new OptionsCheckBox("Summon Plant");
        cbPlant.setFont(skin.getFont1().deriveFont(Font.PLAIN, 14));
        cbZep = new OptionsCheckBox("Launch Zeppelin");
        cbZep.setFont(skin.getFont1().deriveFont(Font.PLAIN, 14));

        lblPet = new JLabel(GuiUtils.getResizedIcon(
                new ImageIcon("res/images/icons/PetIcon.png"), 30, 30));
        lblPlant = new JLabel(GuiUtils.getResizedIcon(
                new ImageIcon("res/images/icons/PlantIcon.png"), 30, 30));
        lblZep = new JLabel(GuiUtils.getResizedIcon(
                new ImageIcon("res/images/icons/ZeppelinIcon.png"), 30, 30));

        StartButton btnStart = new StartButton(parentView);
        btnStart.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) { control.start(); }
        });

        pnlButtonContainer.add(lblPet, "w 30px!, h 30px!, gapright 10px");
        pnlButtonContainer.add(cbxPet, "w 30%!, h 30px!, gapbottom 10px, wrap");

        pnlButtonContainer.add(lblPlant, "w 30px!, h 30px!, gapright 10px");
        pnlButtonContainer.add(cbPlant, "w 30%!, h 30px!, gapbottom 10px, wrap");

        pnlButtonContainer.add(lblZep, "w 30px!, h 30px!, gapright 10px");
        pnlButtonContainer.add(cbZep, "w 30%!, h 30px!, gapbottom 10px, wrap");

        pnlButtonContainer.add(btnStart, "span 2 1");

        viewport.add(pnlButtonContainer, "w 100%!, gapbottom 2%, gaptop 2%, span 2");

        if (this.questData.getMode().equals(QuestData.FANTASY)) {
            final Set<String> petList = this.questData.getPetManager().getAvailablePetNames();
            final QuestPetAbstract pet = this.questData.getPetManager().getSelectedPet();

            // Pet list visibility
            if (petList.size() > 0) {
                cbxPet.setEnabled(true);
                cbxPet.addItem("Don't summon a pet");
                for (final String aPetList : petList) {
                    cbxPet.addItem(aPetList);
                }

                if (pet != null) { cbxPet.setSelectedItem(pet.getName()); }
            } else {
                cbxPet.setVisible(false);
                lblPet.setVisible(false);
            }

            // Plant visiblity
            if (this.questData.getPetManager().getPlant().getLevel() == 0) {
                cbPlant.setVisible(false);
                lblPlant.setVisible(false);
            }
            else {
                cbPlant.setSelected(this.questData.getPetManager().shouldPlantBeUsed());
            }

            // Zeppelin visibility
            final QuestItemZeppelin zeppelin = (QuestItemZeppelin) this.questData.getInventory().getItem("Zeppelin");
            cbZep.setVisible(zeppelin.hasBeenUsed());
            lblZep.setVisible(zeppelin.hasBeenUsed());
        }
        else {
            cbxPet.setVisible(false);
            lblPet.setVisible(false);
            cbPlant.setVisible(false);
            lblPlant.setVisible(false);
            cbZep.setVisible(false);
            lblZep.setVisible(false);
        }
    }

    private void populateNewQuest() {
        if (previousQuestExists) {
            JLabel lblNew = new JLabel("  Embark on a new Quest");
            lblNew.setForeground(skin.getColor("text"));
            lblNew.setBackground(skin.getColor("theme").darker());
            lblNew.setOpaque(true);
            lblNew.setBorder(new MatteBorder(1, 0, 1, 0, skin.getColor("borders")));
            lblNew.setFont(skin.getFont1().deriveFont(Font.BOLD, 16));
            viewport.add(lblNew, "w 90%!, h 50px!, gap 5% 5% 2%, span 2");

            JLabel lblNotes = new JLabel("<html>"
                    + "Start a new Quest will delete your current player decks, credits and win loss record."
                    + "<br>Fantasy adds a Bazaar and the occasional fantasy themed opponent for you to battle."
                    + "</html>");
            lblNotes.setFont(skin.getFont1().deriveFont(Font.PLAIN, 14));
            lblNotes.setForeground(skin.getColor("text"));
            viewport.add(lblNotes, "w 90%, gapleft 5%, span 2");
        }

        radEasy = new OptionsRadio("Easy - 50 games");
        radMedium = new OptionsRadio("Medium - 100 games");
        radHard = new OptionsRadio("Hard - 150 games");
        radExpert = new OptionsRadio("Expert - 200 games");

        ButtonGroup group1 = new ButtonGroup();
        group1.add(radEasy);
        group1.add(radMedium);
        group1.add(radHard);
        group1.add(radExpert);

        radFantasy = new OptionsRadio("Fantasy");
        radClassic = new OptionsRadio("Classic");

        radEasy.setSelected(true);
        radClassic.setSelected(true);

        ButtonGroup group2 = new ButtonGroup();
        group2.add(radFantasy);
        group2.add(radClassic);

        cbStandardStart = new OptionsCheckBox("Standard (Type 2) Starting Pool");

        SubButton btnEmbark = new SubButton("");
        btnEmbark.setAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                control.newQuest();
            }
         });
        btnEmbark.setText("Embark!");

        JPanel optionsContainer = new JPanel();
        optionsContainer.setOpaque(false);
        optionsContainer.setLayout(new MigLayout("insets 0, gap 0"));

        String constraints = "w 30%!, h 40px!";
        optionsContainer.add(radEasy, constraints + ", gap 15% 5% 0 0");
        optionsContainer.add(radFantasy, constraints + ", wrap");
        optionsContainer.add(radMedium, constraints + ", gap 15% 5% 0 0");
        optionsContainer.add(radClassic, constraints + ", wrap");
        optionsContainer.add(radHard, constraints + ", gap 15% 5% 0 0");
        optionsContainer.add(cbStandardStart, constraints + ", wrap");
        optionsContainer.add(radExpert, constraints + ", gap 15% 5% 0 0, wrap");

        optionsContainer.add(btnEmbark, "w 40%!, h 30px!, gapleft 30%, gaptop 3%, span 3 1");

        viewport.add(optionsContainer, "w 100%!, gaptop 2%, span 2");
    }

    //========= CUSTOM CLASSES

    /** Consolidates radio button styling in one place. */
    private class OptionsRadio extends JRadioButton {
        public OptionsRadio(String txt0) {
            super();
            setText(txt0);
            setForeground(skin.getColor("text"));
            setBackground(skin.getColor("hover"));
            setOpaque(false);

            this.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    setOpaque(true);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    setOpaque(false);
                }
            });
        }
    }

    /** Consolidates checkbox styling in one place. */
    private class OptionsCheckBox extends JCheckBox {
        public OptionsCheckBox(String txt0) {
            super();
            setText(txt0);
            setForeground(skin.getColor("text"));
            setBackground(skin.getColor("hover"));
            setOpaque(false);

            this.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    setOpaque(true);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    setOpaque(false);
                }
            });
        }
    }

    /** Selectable panels for duels and challenges. */
    public class SelectablePanel extends JPanel {
        private QuestEvent event;

        /** @param e0 &emsp; QuestEvent */
        public SelectablePanel(QuestEvent e0) {
            super();
            setBorder(new LineBorder(skin.getColor("borders"), 1));
            setBackground(skin.getColor("inactive"));
            setLayout(new MigLayout("insets 0, gap 0"));
            this.event = e0;

            final File base = ForgeProps.getFile(NewConstants.IMAGE_ICON);
            File file = new File(base, event.getIcon());

            if (!file.exists()) {
                file = new File(base, "Unknown.jpg");
            }

            JLabel lblIcon = new JLabel(GuiUtils.getResizedIcon(new ImageIcon(file.toString()), 60, 60));
            lblIcon.setForeground(skin.getColor("text"));
            this.add(lblIcon, "h 60px!, w 60px!, gap 5px 5px 5px 5px, span 1 2");

            // Name
            JLabel lblName = new JLabel(event.getTitle() + ": " + event.getDifficulty());
            lblName.setFont(skin.getFont1().deriveFont(Font.BOLD, 17));
            lblName.setForeground(skin.getColor("text"));
            this.add(lblName, "h 20px!, gap 1% 1% 5px 5px, wrap");

            // Description
            JTextArea tarDesc = new JTextArea();
            tarDesc.setText(event.getDescription());
            tarDesc.setFont(skin.getFont1().deriveFont(Font.ITALIC, 12));
            tarDesc.setForeground(skin.getColor("text"));
            tarDesc.setOpaque(false);
            tarDesc.setWrapStyleWord(true);
            tarDesc.setLineWrap(true);
            tarDesc.setFocusable(false);
            tarDesc.setEditable(false);
            this.add(tarDesc, " h 35px!, w 75%!, gap 1% 0 0 5px");

            this.setToolTipText("<html>" + event.getTitle()
                    + ": " + event.getDifficulty()
                    + "<br>" + event.getDescription()
                    + "</html>");

            this.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent e) {
                    SelectablePanel src = (SelectablePanel) e.getSource();

                    if (selectedOpponent != null) {
                        selectedOpponent.setBackground(skin.getColor("inactive"));
                    }

                    selectedOpponent = src;
                    src.setBackground(skin.getColor("active"));
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    if (selectedOpponent != e.getSource()) {
                        setBackground(skin.getColor("hover"));
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    if (selectedOpponent != e.getSource()) {
                        setBackground(skin.getColor("inactive"));
                    }
                }
            });
       }

        /** @return QuestEvent */
        public QuestEvent getEvent() {
            return event;
        }
    }

    /**
     * <p>
     * nextChallengeInWins.
     * </p>
     * 
     * @return a int.
     */
    private int nextChallengeInWins() {
        final QuestData questData = AllZone.getQuestData();

        // Number of wins was 25, lowereing the number to 20 to help short term
        // questers.
        if (questData.getWin() < 20) {
            return 20 - questData.getWin();
        }

        // The int mul has been lowered by one, should face special opps more
        // frequently.
        final int challengesPlayed = questData.getChallengesPlayed();
        int mul = 5;

        if (questData.getInventory().hasItem("Zeppelin")) {
            mul = 3;
        } else if (questData.getInventory().hasItem("Map")) {
            mul = 4;
        }

        final int delta = (challengesPlayed * mul) - questData.getWin();

        return (delta > 0) ? delta : 0;
    }

    //========= RETRIEVAL FUNCTIONS

    /** @return JList */
    public JList getLstDeckChooser() {
        return lstDeckChooser;
    }

    /** @return JRadioButton */
    public JRadioButton getRadEasy() {
        return radEasy;
    }

    /** @return JRadioButton */
    public JRadioButton getRadMedium() {
        return radMedium;
    }

    /** @return JRadioButton */
    public JRadioButton getRadHard() {
        return radHard;
    }

    /** @return JRadioButton */
    public JRadioButton getRadExpert() {
        return radExpert;
    }

    /** @return JRadioButton */
    public JRadioButton getRadFantasy() {
        return radFantasy;
    }

    /** @return JRadioButton */
    public JRadioButton getRadClassic() {
        return radClassic;
    }

    /** @return JCheckBox */
    public JCheckBox getCbStandardStart() {
        return cbStandardStart;
    }

    /** @return SelectablePanel */
    public SelectablePanel getSelectedOpponent() {
        return selectedOpponent;
    }

    /** @return HomeTopLevel */
    public HomeTopLevel getParentView() {
        return parentView;
    }

    /** @return ControlQuest */
    public ControlQuest getController() {
        return control;
    }

    /** @return JComboBox */
    public JComboBox getPetComboBox() {
        return cbxPet;
    }

    /** @return JCheckBox */
    public JCheckBox getPlantCheckBox() {
        return cbPlant;
    }

    /** @return QuestData instance currently in use in this view */
    public QuestData getQuestData() {
        return questData;
    }

    /** @return boolean */
    public boolean hasPreviousQuest() {
        return previousQuestExists;
    }

    /** @return JLabel */
    public JLabel getLblLife() {
        return lblLife;
    }

    /** @return JLabel */
    public JLabel getLblCredits() {
        return lblCredits;
    }
}
