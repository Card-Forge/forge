package forge;

import forge.error.ErrorViewer;
import forge.gui.GuiUtils;
import forge.quest.data.QuestDataIO;
import forge.quest.gui.QuestFrame;
import forge.view.swing.OldGuiNewGame;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.awt.Color;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * <p>Gui_QuestOptions class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class Gui_QuestOptions extends JFrame {
    /** Constant <code>serialVersionUID=2018518804206822235L</code> */
    private static final long serialVersionUID = 2018518804206822235L;

    private forge.quest.data.QuestData questData = new forge.quest.data.QuestData();

    private JLabel jLabel1 = new JLabel();
    private JButton continueQuestButton = new JButton();
    private JPanel jPanel1 = new JPanel();
    private JPanel jPanel2 = new JPanel();
    private GridLayout gridLayout1 = new GridLayout();

    private JRadioButton easyRadio = new JRadioButton();
    private JRadioButton hardRadio = new JRadioButton();
    private JRadioButton mediumRadio = new JRadioButton();
    private JRadioButton veryHardRadio = new JRadioButton();

    private JRadioButton fantasyRadio = new JRadioButton();
    private JRadioButton realisticRadio = new JRadioButton();

    private JCheckBox cbStandardStart = new JCheckBox();

    private JButton newQuestButton = new JButton();
    private JTextArea jTextArea1 = new JTextArea();
    private ButtonGroup buttonGroup1 = new ButtonGroup();
    private ButtonGroup buttonGroup2 = new ButtonGroup();
    private JPanel jPanel3 = new JPanel();

    /**
     * <p>Constructor for Gui_QuestOptions.</p>
     */
    public Gui_QuestOptions() {
        try {
            jbInit();
        } catch (Exception ex) {
            ErrorViewer.showError(ex);
        }

        setup();
        setupRadioButtonText();

        this.setSize(540, 555);
        GuiUtils.centerFrame(this);
        setVisible(true);
    }

    /**
     * <p>setup.</p>
     */
    private void setup() {
        //make the text look correct on the screen
        jTextArea1.setBackground(getBackground());


        //if user closes this window, go back to "New Game" screen
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent ev) {
                Gui_QuestOptions.this.dispose();
                new OldGuiNewGame();
            }
        });

        //is there any saved data?
        if (!questData.hasSaveFile()) continueQuestButton.setEnabled(false);
    }//setup()

    //show total number of games for each difficulty
    /**
     * <p>setupRadioButtonText.</p>
     */
    private void setupRadioButtonText() {
        String[] diff = questData.getDifficultyChoices();
        JRadioButton[] b = {easyRadio, mediumRadio, hardRadio, veryHardRadio};

        for (int i = 0; i < diff.length; i++) {
            b[i].setText(diff[i] + " - " + questData.getTotalNumberOfGames(i));
        }

    }//setupRadioButtonText()

    /**
     * <p>jbInit.</p>
     *
     * @throws java.lang.Exception if any.
     */
    private void jbInit() throws Exception {
        TitledBorder titledBorder1 = new TitledBorder(BorderFactory.createEtchedBorder(Color.white,
                new Color(148, 145, 140)),
                "Quest Length");
        Border border2 = BorderFactory.createEtchedBorder(Color.white, new Color(148, 145, 140));
        TitledBorder titledBorder2 = new TitledBorder(border2, "Continue");
        jLabel1.setFont(new java.awt.Font("Dialog", 0, 25));
        jLabel1.setHorizontalAlignment(SwingConstants.CENTER);
        jLabel1.setText("Quest Options");
        jLabel1.setBounds(new Rectangle(1, 0, 539, 63));
        this.setTitle("Quest Options");
        this.getContentPane().setLayout(null);
        continueQuestButton.setBounds(new Rectangle(69, 28, 179, 35));
        continueQuestButton.setFont(new java.awt.Font("Dialog", 0, 18));
        continueQuestButton.setText("Continue Quest");
        continueQuestButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                continueQuestButton_actionPerformed(e);
            }
        });
        jPanel1.setBorder(titledBorder1);
        jPanel1.setBounds(new Rectangle(20, 63, 500, 353));
        jPanel1.setLayout(null);

        jPanel2.setBounds(new Rectangle(20, 27, 460, 101));
        jPanel2.setLayout(gridLayout1);

        gridLayout1.setColumns(2);
        gridLayout1.setRows(4);

        easyRadio.setText("Easy - 50 games");
        mediumRadio.setText("Medium - 100 games");
        hardRadio.setText("Hard - 200 games");
        veryHardRadio.setText("Very Hard - 300 games");
        realisticRadio.setText("Realistic");
        fantasyRadio.setText("Fantasy");

        easyRadio.setSelected(true);
        realisticRadio.setSelected(true);

        cbStandardStart.setText("Standard (Type 2) Starting Pool");

        newQuestButton.setBounds(new Rectangle(179, 292, 140, 38));
        newQuestButton.setFont(new java.awt.Font("Dialog", 0, 16));
        newQuestButton.setText("New Quest");
        newQuestButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                newQuestButton_actionPerformed(e);
            }
        });

        StringBuilder sb = new StringBuilder();
        sb.append("New Quest will delete your current player decks, credits and win loss record. ");
        sb.append("Continue Quest will allow you to continue a quest that you started at an earlier time.");
        sb.append("\r\n");
        sb.append("\r\n");
        sb.append("Realistic is the original quest mode with a new feature, the Card Shop. ");
        sb.append("Fantasy adds a Bazaar and the occasional fantasy themed opponent for you to battle.");

        jTextArea1.setBorder(BorderFactory.createEtchedBorder(Color.white, new Color(148, 145, 140)));
        jTextArea1.setEnabled(false);
        jTextArea1.setFont(new java.awt.Font("Dialog", 0, 12));
        jTextArea1.setDisabledTextColor(Color.black);
        jTextArea1.setEditable(false);
//      jTextArea1.setText("Note: Starting a new quest will delete your current quest data");
        jTextArea1.setText(sb.toString());
        jTextArea1.setLineWrap(true);
        jTextArea1.setWrapStyleWord(true);
        jTextArea1.setBounds(new Rectangle(86, 145, 327, 128));

        jPanel3.setBorder(titledBorder2);
        jPanel3.setBounds(new Rectangle(110, 427, 323, 86));
        jPanel3.setLayout(null);

        jPanel2.add(easyRadio, null);
        jPanel2.add(realisticRadio, null);
        jPanel2.add(mediumRadio, null);
        jPanel2.add(fantasyRadio, null);
        jPanel2.add(hardRadio, null);
        jPanel2.add(new JLabel(""));  // for empty cell
        jPanel2.add(veryHardRadio, null);
        jPanel2.add(cbStandardStart, null);

        jPanel1.add(newQuestButton, null);
        jPanel1.add(jTextArea1, null);
        this.getContentPane().add(jPanel1, null);
        this.getContentPane().add(jPanel3, null);
        jPanel3.add(continueQuestButton, null);
        this.getContentPane().add(jLabel1, null);
        jPanel1.add(jPanel2, null);
        buttonGroup1.add(easyRadio);
        buttonGroup1.add(mediumRadio);
        buttonGroup1.add(hardRadio);
        buttonGroup1.add(veryHardRadio);

        buttonGroup2.add(realisticRadio);
        buttonGroup2.add(fantasyRadio);

    }

    /**
     * <p>continueQuestButton_actionPerformed.</p>
     *
     * @param e a {@link java.awt.event.ActionEvent} object.
     */
    void continueQuestButton_actionPerformed(ActionEvent e) {
        //set global variable
        AllZone.setQuestData(QuestDataIO.loadData());
        AllZone.getQuestData().setDifficultyIndex();
        dispose();

        new QuestFrame();

    }

    /**
     * <p>newQuestButton_actionPerformed.</p>
     *
     * @param e a {@link java.awt.event.ActionEvent} object.
     */
    void newQuestButton_actionPerformed(ActionEvent e) {
        int difficulty = 0;

        String mode = fantasyRadio.isSelected() ? forge.quest.data.QuestData.FANTASY : forge.quest.data.QuestData.REALISTIC;

        if (easyRadio.isSelected()) difficulty = 0;

        else if (mediumRadio.isSelected()) difficulty = 1;

        else if (hardRadio.isSelected()) difficulty = 2;

        else if (veryHardRadio.isSelected()) difficulty = 3;

        else //user didn't select a difficulty{
            return;

        if (questData.hasSaveFile()) {
            // this will overwrite your save file!
            Object[] possibleValues = {"Yes", "No"};
            Object choice = JOptionPane.showOptionDialog(null, "Starting a new quest will overwrite your current quest. Continue?",
                    "Start New Quest?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                    null, possibleValues, possibleValues[1]);

            if (!choice.equals(0))
                return;
        }

        //give the user a few cards to build a deck
        questData.newGame(difficulty, mode, cbStandardStart.isSelected());

        questData.saveData();


        //set global variable
        AllZone.setQuestData(questData);

        dispose();
        new QuestFrame();
    }


}
