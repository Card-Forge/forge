package forge.quest.gui;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import forge.AllZone;
import forge.error.ErrorViewer;
import forge.gui.GuiUtils;
import forge.quest.data.QuestData;
import forge.quest.data.QuestDataIO;
import forge.quest.data.QuestPreferences;
import forge.view.swing.GuiHomeScreen;
import forge.view.swing.OldGuiNewGame;

/**
 * <p>
 * Gui_QuestOptions class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class QuestOptions extends JFrame {
    /** Constant <code>serialVersionUID=2018518804206822235L</code>. */
    private static final long serialVersionUID = 2018518804206822235L;

    private final QuestData questData = new QuestData();

    private final JLabel jLabel1 = new JLabel();
    private final JButton continueQuestButton = new JButton();
    private final JPanel jPanel1 = new JPanel();
    private final JPanel jPanel2 = new JPanel();
    private final GridLayout gridLayout1 = new GridLayout();

    private final JRadioButton easyRadio = new JRadioButton();
    private final JRadioButton hardRadio = new JRadioButton();
    private final JRadioButton mediumRadio = new JRadioButton();
    private final JRadioButton veryHardRadio = new JRadioButton();

    private final JRadioButton fantasyRadio = new JRadioButton();
    private final JRadioButton realisticRadio = new JRadioButton();

    private final JCheckBox cbStandardStart = new JCheckBox();

    private final JButton newQuestButton = new JButton();
    private final JTextArea jTextArea1 = new JTextArea();
    private final ButtonGroup buttonGroup1 = new ButtonGroup();
    private final ButtonGroup buttonGroup2 = new ButtonGroup();
    private final JPanel jPanel3 = new JPanel();

    /**
     * <p>
     * Constructor for Gui_QuestOptions.
     * </p>
     */
    public QuestOptions() {
        try {
            this.jbInit();
        } catch (final Exception ex) {
            ErrorViewer.showError(ex);
        }

        this.setup();
        this.setupRadioButtonText();

        this.setSize(540, 555);
        GuiUtils.centerFrame(this);
        this.setVisible(true);
    }

    /**
     * <p>
     * setup.
     * </p>
     */
    private void setup() {
        // make the text look correct on the screen
        this.jTextArea1.setBackground(this.getBackground());

        // if user closes this window, go back to "New Game" screen
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent ev) {
                QuestOptions.this.dispose();

                if (System.getenv("NG2") != null) {
                    if (System.getenv("NG2").equalsIgnoreCase("true")) {
                        final String[] argz = {};
                        GuiHomeScreen.main(argz);
                    } else {
                        new OldGuiNewGame();
                    }
                } else {
                    new OldGuiNewGame();
                }

            }
        });

        // is there any saved data?
        if (!this.questData.hasSaveFile()) {
            this.continueQuestButton.setEnabled(false);
        }
    } // setup()

    // show total number of games for each difficulty
    /**
     * <p>
     * setupRadioButtonText.
     * </p>
     */
    private void setupRadioButtonText() {
        final String[] diff = QuestPreferences.getDifficulty();
        final JRadioButton[] b = { this.easyRadio, this.mediumRadio, this.hardRadio, this.veryHardRadio };

        for (int i = 0; i < diff.length; i++) {
            // -2 because you start a level 1, and the last level is secret
            final int numberLevels = QuestData.RANK_TITLES.length - 2;
            final int numGames = numberLevels * QuestPreferences.getWinsForRankIncrease(i);

            b[i].setText(String.format("%s - %d", diff[i], numGames));
        }

    } // setupRadioButtonText()

    /**
     * <p>
     * jbInit.
     * </p>
     * 
     * @throws java.lang.Exception
     *             if any.
     */
    private void jbInit() throws Exception {
        final TitledBorder titledBorder1 = new TitledBorder(BorderFactory.createEtchedBorder(Color.white, new Color(
                148, 145, 140)), "Quest Length");
        final Border border2 = BorderFactory.createEtchedBorder(Color.white, new Color(148, 145, 140));
        final TitledBorder titledBorder2 = new TitledBorder(border2, "Continue");
        this.jLabel1.setFont(new java.awt.Font("Dialog", 0, 25));
        this.jLabel1.setHorizontalAlignment(SwingConstants.CENTER);
        this.jLabel1.setText("Quest Options");
        this.jLabel1.setBounds(new Rectangle(1, 0, 539, 63));
        this.setTitle("Quest Options");
        this.getContentPane().setLayout(null);
        this.continueQuestButton.setBounds(new Rectangle(69, 28, 179, 35));
        this.continueQuestButton.setFont(new java.awt.Font("Dialog", 0, 18));
        this.continueQuestButton.setText("Continue Quest");
        this.continueQuestButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                QuestOptions.this.continueQuestButtonActionPerformed(e);
            }
        });
        this.jPanel1.setBorder(titledBorder1);
        this.jPanel1.setBounds(new Rectangle(20, 63, 500, 353));
        this.jPanel1.setLayout(null);

        this.jPanel2.setBounds(new Rectangle(20, 27, 460, 101));
        this.jPanel2.setLayout(this.gridLayout1);

        this.gridLayout1.setColumns(2);
        this.gridLayout1.setRows(4);

        this.easyRadio.setText("Easy - 50 games");
        this.mediumRadio.setText("Medium - 100 games");
        this.hardRadio.setText("Hard - 200 games");
        this.veryHardRadio.setText("Very Hard - 300 games");
        this.realisticRadio.setText("Realistic");
        this.fantasyRadio.setText("Fantasy");

        this.easyRadio.setSelected(true);
        this.realisticRadio.setSelected(true);

        this.cbStandardStart.setText("Standard (Type 2) Starting Pool");

        this.newQuestButton.setBounds(new Rectangle(179, 292, 140, 38));
        this.newQuestButton.setFont(new java.awt.Font("Dialog", 0, 16));
        this.newQuestButton.setText("New Quest");
        this.newQuestButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                QuestOptions.this.newQuestButtonActionPerformed(e);
            }
        });

        final StringBuilder sb = new StringBuilder();
        sb.append("New Quest will delete your current player decks, credits and win loss record. ");
        sb.append("Continue Quest will allow you to continue a quest that you started at an earlier time.");
        sb.append("\r\n");
        sb.append("\r\n");
        sb.append("Realistic is the original quest mode with a new feature, the Card Shop. ");
        sb.append("Fantasy adds a Bazaar and the occasional fantasy themed opponent for you to battle.");

        this.jTextArea1.setBorder(BorderFactory.createEtchedBorder(Color.white, new Color(148, 145, 140)));
        this.jTextArea1.setEnabled(false);
        this.jTextArea1.setFont(new java.awt.Font("Dialog", 0, 12));
        this.jTextArea1.setDisabledTextColor(Color.black);
        this.jTextArea1.setEditable(false);
        // jTextArea1.setText("Note: Starting a new quest will delete your current quest data");
        this.jTextArea1.setText(sb.toString());
        this.jTextArea1.setLineWrap(true);
        this.jTextArea1.setWrapStyleWord(true);
        this.jTextArea1.setBounds(new Rectangle(86, 145, 327, 128));

        this.jPanel3.setBorder(titledBorder2);
        this.jPanel3.setBounds(new Rectangle(110, 427, 323, 86));
        this.jPanel3.setLayout(null);

        this.jPanel2.add(this.easyRadio, null);
        this.jPanel2.add(this.realisticRadio, null);
        this.jPanel2.add(this.mediumRadio, null);
        this.jPanel2.add(this.fantasyRadio, null);
        this.jPanel2.add(this.hardRadio, null);
        this.jPanel2.add(new JLabel("")); // for empty cell
        this.jPanel2.add(this.veryHardRadio, null);
        this.jPanel2.add(this.cbStandardStart, null);

        this.jPanel1.add(this.newQuestButton, null);
        this.jPanel1.add(this.jTextArea1, null);
        this.getContentPane().add(this.jPanel1, null);
        this.getContentPane().add(this.jPanel3, null);
        this.jPanel3.add(this.continueQuestButton, null);
        this.getContentPane().add(this.jLabel1, null);
        this.jPanel1.add(this.jPanel2, null);
        this.buttonGroup1.add(this.easyRadio);
        this.buttonGroup1.add(this.mediumRadio);
        this.buttonGroup1.add(this.hardRadio);
        this.buttonGroup1.add(this.veryHardRadio);

        this.buttonGroup2.add(this.realisticRadio);
        this.buttonGroup2.add(this.fantasyRadio);

    }

    /**
     * <p>
     * continueQuestButton_actionPerformed.
     * </p>
     * 
     * @param e
     *            a {@link java.awt.event.ActionEvent} object.
     */
    final void continueQuestButtonActionPerformed(final ActionEvent e) {
        // set global variable
        AllZone.setQuestData(QuestDataIO.loadData());
        AllZone.getQuestData().guessDifficultyIndex();
        this.dispose();

        new QuestFrame();

    }

    /**
     * <p>
     * newQuestButton_actionPerformed.
     * </p>
     * 
     * @param e
     *            a {@link java.awt.event.ActionEvent} object.
     */
    final void newQuestButtonActionPerformed(final ActionEvent e) {
        int difficulty = 0;

        final String mode = this.fantasyRadio.isSelected() ? forge.quest.data.QuestData.FANTASY
                : forge.quest.data.QuestData.REALISTIC;

        if (this.easyRadio.isSelected()) {
            difficulty = 0;
        } else if (this.mediumRadio.isSelected()) {
            difficulty = 1;
        } else if (this.hardRadio.isSelected()) {
            difficulty = 2;
        } else if (this.veryHardRadio.isSelected()) {
            difficulty = 3;
        } else {
            // user didn't select a difficulty{
            return;
        }

        if (this.questData.hasSaveFile()) {
            // this will overwrite your save file!
            final Object[] possibleValues = { "Yes", "No" };
            final Object choice = JOptionPane.showOptionDialog(null,
                    "Starting a new quest will overwrite your current quest. Continue?", "Start New Quest?",
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, possibleValues, possibleValues[1]);

            if (!choice.equals(0)) {
                return;
            }
        }

        // give the user a few cards to build a deck
        this.questData.newGame(difficulty, mode, this.cbStandardStart.isSelected());

        this.questData.saveData();

        // set global variable
        AllZone.setQuestData(this.questData);

        this.dispose();
        new QuestFrame();
    }

}
