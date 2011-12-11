package forge.view.home;

import java.awt.BorderLayout;
import java.awt.Font;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.border.MatteBorder;

import net.miginfocom.swing.MigLayout;
import forge.AllZone;
import forge.quest.data.QuestData;
import forge.quest.gui.main.QuestChallenge;
import forge.quest.gui.main.QuestDuel;
import forge.quest.gui.main.QuestEventManager;
import forge.quest.gui.main.QuestSelectablePanel;
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

        // Quest Events (and options)
        populateQuestEvents();

        // Start button
        StartButton btnStart = new StartButton(parentView);

        JPanel pnlButtonContainer = new JPanel();
        pnlButtonContainer.setOpaque(false);

        pnlButtonContainer.setLayout(new BorderLayout());
        pnlButtonContainer.add(btnStart, SwingConstants.CENTER);
        viewport.add(pnlButtonContainer, "w 100%!, gapbottom 2%, gaptop 2%");

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
            duelsContainer.add(new QuestSelectablePanel(d), "w 100%, h 70px!, gapbottom 2px");
        }

        for (QuestChallenge c : challenges) {
            challengesContainer.add(new QuestSelectablePanel(c), "w 100%, h 70px!, gapbottom 2px");
        }

        JLabel lblDuels = new JLabel("Available Duels");
        lblDuels.setFont(skin.getFont1().deriveFont(Font.ITALIC, 14));

        JLabel lblChallenges = new JLabel("Available Challenges");
        lblChallenges.setFont(skin.getFont1().deriveFont(Font.ITALIC, 14));

        viewport.add(lblDuels, "w 90%, gapleft 5%, gapbottom 1%, gaptop 1%");
        viewport.add(duelsContainer, " w 90%, gapleft 5%, gapbottom 2%");
        viewport.add(lblChallenges, "w 90%, gapleft 5%, gapbottom 1%");
        viewport.add(challengesContainer, " w 90%, gapleft 5%, gapbottom 2%");

        JPanel optionsContainer = new JPanel();
        optionsContainer.setOpaque(false);
        optionsContainer.setLayout(new MigLayout("insets 0, gap 0"));

        SubButton btnEditor = new SubButton("Deck Editor");
        JList lstDeckChooser = new JList(new String[] {"Cosmo", "Elaine", "George"});

        optionsContainer.add(btnEditor, "w 30%, h 30px!, gapleft 15%, gapbottom 3px");
        optionsContainer.add(new OptionsCheckBox("Summon Pet"), "w 30%, h 33px!, gapleft 5%, wrap");
        optionsContainer.add(lstDeckChooser, "w 30%, h 60px!, gapleft 15%, span 1 2");
        optionsContainer.add(new OptionsCheckBox("Summon Wall"), "w 30%, h 30px!, gapleft 5%, wrap");
        optionsContainer.add(new OptionsCheckBox("Launch Zeppelin"), "w 30%, h 30px!, gapleft 5%, wrap");

        viewport.add(optionsContainer, "w 90%, gap 5% 0 1% 1%");
    }

    private void populateNewQuest() {
        JLabel lblNew = new JLabel("Embark on a new Quest");
        lblNew.setBorder(new MatteBorder(1, 0, 1, 0, skin.getColor("borders")));
        lblNew.setFont(skin.getFont1().deriveFont(Font.BOLD, 16));
        viewport.add(lblNew, "w 90%!, h 50px!, gap 5% 5% 2%");

        JLabel lblNotes = new JLabel("<html>"
                + "Start a new Quest will delete your current player decks, credits and win loss record."
                + "<br>Fantasy adds a Bazaar and the occasional fantasy themed opponent for you to battle."
                + "</html>");
        lblNotes.setFont(skin.getFont1().deriveFont(Font.PLAIN, 14));
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
}
