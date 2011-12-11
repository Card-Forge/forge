package forge.view.home;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;

import net.miginfocom.swing.MigLayout;
import forge.AllZone;
import forge.Command;
import forge.Constant;
import forge.gui.GuiUtils;
import forge.gui.MultiLineLabel;
import forge.gui.MultiLineLabelUI;
import forge.gui.deckeditor.DeckEditorQuest;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;
import forge.quest.data.QuestData;
import forge.quest.data.QuestUtil;
import forge.quest.gui.QuestFrame;
import forge.quest.gui.main.QuestChallenge;
import forge.quest.gui.main.QuestDuel;
import forge.quest.gui.main.QuestEvent;
import forge.quest.gui.main.QuestEventManager;
import forge.view.GuiTopLevel;
import forge.view.toolbox.FButton;
import forge.view.toolbox.FSkin;

/** 
 * TODO: Write javadoc for this type.
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

    /**
     * TODO: Write javadoc for Constructor.
     * @param v0 &emsp; HomeTopLevel parent view
     */
    public ViewQuest(HomeTopLevel v0) {
        super(VERTICAL_SCROLLBAR_ALWAYS, HORIZONTAL_SCROLLBAR_AS_NEEDED);
        this.setOpaque(false);
        this.setBorder(null);
        parentView = v0;
        skin = AllZone.getSkin();
        questData = AllZone.getQuestData();

        viewport = new JPanel();
        viewport.setOpaque(false);
        viewport.setLayout(new MigLayout("insets 0, gap 0, wrap"));
        this.getViewport().setOpaque(false);

        JLabel lblContinue = new JLabel(questData.getRank());
        lblContinue.setBorder(new MatteBorder(0, 0, 1, 0, skin.getColor("borders")));
        lblContinue.setForeground(skin.getColor("text"));
        lblContinue.setFont(skin.getFont1().deriveFont(Font.BOLD, 20));
        viewport.add(lblContinue, "w 90%!, gap 5% 0 2% 0");

        // Quest events and options
        populateQuestEvents();
        populateQuestOptions();

        // Start button
        StartButton btnStart = new StartButton(parentView);

        JPanel pnlButtonContainer = new JPanel();
        pnlButtonContainer.setOpaque(false);

        pnlButtonContainer.setLayout(new BorderLayout());
        pnlButtonContainer.add(btnStart, SwingConstants.CENTER);
        viewport.add(pnlButtonContainer, "w 100%!, gapbottom 2%, gaptop 2%");

        btnStart.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) { start(); }
        });

        // New Quest
        populateNewQuest();

        this.setViewportView(viewport);
    }

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

        JLabel lblDuels = new JLabel("Available Duels");
        lblDuels.setForeground(skin.getColor("text"));
        lblDuels.setFont(skin.getFont1().deriveFont(Font.ITALIC, 14));

        JLabel lblChallenges = new JLabel("Available Challenges");
        lblChallenges.setForeground(skin.getColor("text"));
        lblChallenges.setFont(skin.getFont1().deriveFont(Font.ITALIC, 14));

        viewport.add(lblDuels, "w 90%, gapleft 5%, gapbottom 1%, gaptop 1%");
        viewport.add(duelsContainer, " w 90%, gapleft 5%, gapbottom 2%");
        viewport.add(lblChallenges, "w 90%, gapleft 5%, gapbottom 1%");
        viewport.add(challengesContainer, " w 90%, gapleft 5%, gapbottom 2%");

        // Select first event.
        selectedOpponent = (SelectablePanel) duelsContainer.getComponent(0);
        selectedOpponent.setBackground(skin.getColor("active"));
    }

    /** */
    private void populateQuestOptions() {
        JPanel optionsContainer = new JPanel();
        optionsContainer.setOpaque(false);
        optionsContainer.setLayout(new MigLayout("insets 0, gap 0"));

        SubButton btnEditor = new SubButton("");
        btnEditor.setAction(new AbstractAction() {
           @Override
           public void actionPerformed(ActionEvent e) {
               showDeckEditor();
           }
        });
        btnEditor.setText("Deck Editor");

        String[] decks = objectArrayToStringArray(questData.getDeckNames().toArray());
        lstDeckChooser = new JList(decks);
        lstDeckChooser.setSelectedIndex(0);

        optionsContainer.add(btnEditor, "w 30%, h 30px!, gapleft 15%, gapbottom 3px");
        optionsContainer.add(new OptionsCheckBox("Summon Pet"), "w 30%, h 33px!, gapleft 5%, wrap");
        optionsContainer.add(lstDeckChooser, "w 30%, h 60px!, gapleft 15%, span 1 2");
        optionsContainer.add(new OptionsCheckBox("Summon Wall"), "w 30%, h 30px!, gapleft 5%, wrap");
        optionsContainer.add(new OptionsCheckBox("Launch Zeppelin"), "w 30%, h 30px!, gapleft 5%, wrap");

        viewport.add(optionsContainer, "w 90%, gap 5% 0 1% 1%");
    }

    private void populateNewQuest() {
        JLabel lblNew = new JLabel("Embark on a new Quest");
        lblNew.setForeground(skin.getColor("text"));
        lblNew.setBorder(new MatteBorder(1, 0, 1, 0, skin.getColor("borders")));
        lblNew.setFont(skin.getFont1().deriveFont(Font.BOLD, 16));
        viewport.add(lblNew, "w 90%!, h 50px!, gap 5% 5% 2%");

        JLabel lblNotes = new JLabel("<html>"
                + "Start a new Quest will delete your current player decks, credits and win loss record."
                + "<br>Fantasy adds a Bazaar and the occasional fantasy themed opponent for you to battle."
                + "</html>");
        lblNotes.setFont(skin.getFont1().deriveFont(Font.PLAIN, 14));
        lblNotes.setForeground(skin.getColor("text"));
        viewport.add(lblNotes, "w 90%, gapleft 5%");

        JRadioButton radEasy = new OptionsRadio("Easy - 50 games");
        JRadioButton radMedium = new OptionsRadio("Medium - 100 games");
        JRadioButton radHard = new OptionsRadio("Hard - 150 games");
        JRadioButton radExpert = new OptionsRadio("Expert - 200 games");

        JRadioButton radFantasy = new OptionsRadio("Fantasy");
        JRadioButton radClassic = new OptionsRadio("Classic");
        JCheckBox cbStandardStart = new OptionsCheckBox("Standard (Type 2) Starting Pool");

        FButton btnEmbark = new FButton("Embark!");

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

        viewport.add(optionsContainer, "w 100%!, gaptop 2%");
    }

    /** @return HomeTopLevel */
    public HomeTopLevel getParentView() {
        return parentView;
    }

    /** Consolidates radio button styling in one place. */
    private class OptionsRadio extends JRadioButton {
        public OptionsRadio(String txt0) {
            super();
            setText(txt0);
            setOpaque(false);
        }
    }

    /** Consolidates checkbox styling in one place. */
    private class OptionsCheckBox extends JCheckBox {
        public OptionsCheckBox(String txt0) {
            super();
            setText(txt0);
            setOpaque(false);
        }
    }

    private void start() {
        Constant.Runtime.HUMAN_DECK[0] = this.questData.getDeck((String) lstDeckChooser.getSelectedValue());
        Constant.Runtime.COMPUTER_DECK[0] = this.selectedOpponent.getEvent().getEventDeck();

        AllZone.setQuestEvent(this.selectedOpponent.getEvent());

        GuiTopLevel g = (GuiTopLevel) AllZone.getDisplay();
        g.getController().changeState(1);
        g.getController().getMatchController().initMatch();

        AllZone.getGameAction().newGame(
                Constant.Runtime.HUMAN_DECK[0], Constant.Runtime.COMPUTER_DECK[0],
                QuestUtil.getHumanStartingCards(this.questData),
                QuestUtil.getComputerStartingCards(this.questData),
                this.questData.getLife(), 20, null);
    }

    private class SelectablePanel extends JPanel {
        private QuestEvent event;

        public SelectablePanel(QuestEvent e0) {
            super();
            setBorder(new LineBorder(skin.getColor("borders"), 1));
            setBackground(skin.getColor("inactive"));
            setLayout(new MigLayout("insets 0, gap 0"));

            this.event = e0;
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

            final File base = ForgeProps.getFile(NewConstants.IMAGE_ICON);
            File file = new File(base, event.getIcon());

            if (!file.exists()) {
                file = new File(base, "Unknown.jpg");
            }

            JLabel lblIcon = new JLabel(GuiUtils.getResizedIcon(new ImageIcon(file.toString()), 60, 60));
            lblIcon.setForeground(skin.getColor("text"));
            this.add(lblIcon, "h 60px!, w 60px!, gap 5px 5px 5px 5px, span 1 2");

            JLabel lblName = new JLabel(event.getTitle() + ": " + event.getDifficulty());
            lblName.setFont(skin.getFont1().deriveFont(Font.BOLD, 17));
            lblName.setForeground(skin.getColor("text"));
            this.add(lblName, "h 20px!, gap 1% 1% 5px 5px, wrap");

            MultiLineLabel lblDesc = new MultiLineLabel(event.getDescription());
            lblDesc.setFont(skin.getFont1().deriveFont(Font.PLAIN, 12));
            lblDesc.setForeground(skin.getColor("text"));
            lblDesc.setUI(MultiLineLabelUI.getLabelUI());
            this.add(lblDesc, " h 35px!, w 80%!, gap 1% 0 0 5px");
        }

        public QuestEvent getEvent() {
            return event;
        }
    }

    private String[] objectArrayToStringArray(Object[] o0) {
        String[] output = new String[o0.length];

        for (int i = 0; i < o0.length; i++) {
            output[i] = o0[i].toString();
        }

        return output;
    }

    /** */
    final void showDeckEditor() {
        final Command exit = new Command() {
            private static final long serialVersionUID = -5110231879431074581L;

            @Override
            public void execute() {
                // saves all deck data
                AllZone.getQuestData().saveData();

                new QuestFrame();
            }
        };

        final DeckEditorQuest g = new DeckEditorQuest(AllZone.getQuestData());

        g.show(exit);
        g.setVisible(true);
    } // deck editor button
}
