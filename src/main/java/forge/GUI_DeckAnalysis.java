package forge;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.math.BigDecimal;
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.ListModel;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;
import javax.swing.event.MouseInputAdapter;
import javax.swing.table.DefaultTableModel;

import net.miginfocom.swing.MigLayout;

/**
 * This code was edited or generated using CloudGarden's Jigloo SWT/Swing GUI
 * Builder, which is free for non-commercial use. If Jigloo is being used
 * commercially (ie, by a corporation, company or business for any purpose
 * whatever) then you should purchase a license for each developer using Jigloo.
 * Please visit www.cloudgarden.com for details. Use of Jigloo implies
 * acceptance of these licensing terms. A COMMERCIAL LICENSE HAS NOT BEEN
 * PURCHASED FOR THIS MACHINE, SO JIGLOO OR THIS CODE CANNOT BE USED LEGALLY FOR
 * ANY CORPORATE OR COMMERCIAL PURPOSE.
 * 
 * @author Forge
 * @version $Id$
 */
public class GUI_DeckAnalysis extends javax.swing.JDialog {

    /** Constant <code>serialVersionUID=-8475271235196182185L</code> */
    private static final long serialVersionUID = -8475271235196182185L;
    private JPanel jPanel1;
    private JLabel jLabelColorless;
    private JLabel jLabelMultiColor;
    private JLabel jLabelWhite;
    private JLabel jLabelSixMana;
    private JLabel jLabelFiveMana;
    private JLabel jLabelFourMana;
    private JLabel jLabelThreeMana;
    private JLabel jLabel1;
    private JScrollPane jScrollPane1;
    private JTable jTable1;
    private JPanel jPanel5;
    private JButton jButtonRegenerate;
    private JLabel jLabel4;
    private JSeparator jSeparator4;
    private JPanel jPanel4;
    private JList jListFirstHand;
    private JLabel jLabelTwoMana;
    private JLabel jLabelOneMana;
    private JLabel jLabelManaCost;
    private JSeparator jSeparator3;
    private JLabel jLabelZeroMana;
    private JPanel jPanel3;
    private JLabel jLabelSorcery;
    private JLabel jLabelPlaneswalker;
    private JLabel jLabelRed;
    private JLabel jLabelGreen;
    private JLabel jLabelBlue;
    private JLabel jLabelBlack;
    private JLabel jLabelEnchant;
    private JLabel jLabelLandType;
    private JLabel jLabelInstant;
    private JLabel jLabelCreature;
    private JLabel jLabel3;
    private JSeparator jSeparator2;
    private JLabel jLabelArtifact;
    private JPanel jPanel2;
    private JLabel jLabelTotal;
    private JLabel jLabelLand;
    private JSeparator jSeparator1;
    private JLabel jLabel2;
    private JButton jButtonOk;
    private JFrame jF;
    // private ButtonGroup buttonGroup1;

    public CardList filterCardList;
    public TableModel tModel;

    /**
     * <p>
     * Constructor for GUI_DeckAnalysis.
     * </p>
     * 
     * @param g
     *            a {@link javax.swing.JFrame} object.
     * @param tb
     *            a {@link forge.TableModel} object.
     */
    public GUI_DeckAnalysis(JFrame g, TableModel tb) {
        super(g);
        tModel = tb;

        jF = g;
        initGUI();
    }

    /**
     * <p>
     * initGUI.
     * </p>
     */
    private void initGUI() {
        try {

            getContentPane().setLayout(null);
            setVisible(true);
            int wWidth = 600;
            int wHeight = 600;
            this.setPreferredSize(new java.awt.Dimension(wWidth, wHeight));

            Dimension screen = getToolkit().getScreenSize();
            int x = (screen.width - wWidth) / 2;
            int y = (screen.height - wHeight) / 2;
            this.setBounds(x, y, wWidth, wHeight);
            this.setResizable(false);
            this.setTitle("Deck Analysis");
            pack();
            // this.setIconImage(null);

            this.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent arg0) {
                    jF.setEnabled(true);
                }

                @Override
                public void windowOpened(WindowEvent arg0) {

                    int cBlack, cBlue, cGreen, cRed, cWhite, cMulticolor, cColorless, cLand;
                    int cArtifact, cCreature, cEnchant, cInstant, cLandType, cPlaneswalker, cSorcery;
                    int mZero, mOne, mTwo, mThree, mFour, mFive, mSixMore;
                    float tManaCost;
                    Card c;
                    cBlack = 0;
                    cBlue = 0;
                    cGreen = 0;
                    cRed = 0;
                    cWhite = 0;
                    cMulticolor = 0;
                    cColorless = 0;
                    cLand = 0;
                    cArtifact = 0;
                    cCreature = 0;
                    cEnchant = 0;
                    cInstant = 0;
                    cLandType = 0;
                    cPlaneswalker = 0;
                    cSorcery = 0;
                    mZero = 0;
                    mOne = 0;
                    mTwo = 0;
                    mThree = 0;
                    mFour = 0;
                    mFive = 0;
                    mSixMore = 0;
                    tManaCost = 0;
                    CardList cList;
                    cList = tModel.getCards();
                    for (int i = 0; i < cList.size(); i++) {
                        c = cList.getCard(i);
                        if (CardUtil.getColors(c).size() > 1) {
                            cMulticolor = cMulticolor + 1;
                        } else {
                            if (CardUtil.getColors(c).contains(Constant.Color.Black)) {
                                cBlack = cBlack + 1;
                            }
                            if (CardUtil.getColors(c).contains(Constant.Color.Blue)) {
                                cBlue = cBlue + 1;
                            }
                            if (CardUtil.getColors(c).contains(Constant.Color.Green)) {
                                cGreen = cGreen + 1;
                            }
                            if (CardUtil.getColors(c).contains(Constant.Color.Red)) {
                                cRed = cRed + 1;
                            }
                            if (CardUtil.getColors(c).contains(Constant.Color.White)) {
                                cWhite = cWhite + 1;
                            }
                            if (CardUtil.getColors(c).contains(Constant.Color.Colorless)) {
                                if (c.isLand()) {
                                    cLand = cLand + 1;
                                } else {
                                    cColorless = cColorless + 1;
                                }
                            }
                        }

                    }

                    for (int i = 0; i < cList.size(); i++) {
                        c = cList.getCard(i);
                        if (c.isArtifact()) {
                            cArtifact = cArtifact + 1;
                        }
                        if (c.isCreature()) {
                            cCreature = cCreature + 1;
                        }
                        if (c.isEnchantment()) {
                            cEnchant = cEnchant + 1;
                        }
                        if (c.isInstant()) {
                            cInstant = cInstant + 1;
                        }
                        if (c.isLand()) {
                            cLandType = cLandType + 1;
                        }
                        if (c.isPlaneswalker()) {
                            cPlaneswalker = cPlaneswalker + 1;
                        }
                        if (c.isSorcery()) {
                            cSorcery = cSorcery + 1;
                        }
                    }

                    for (int i = 0; i < cList.size(); i++) {
                        c = cList.getCard(i);
                        if (CardUtil.getConvertedManaCost(c.getManaCost()) == 0) {
                            mZero = mZero + 1;
                        }
                        if (CardUtil.getConvertedManaCost(c.getManaCost()) == 1) {
                            mOne = mOne + 1;
                        }
                        if (CardUtil.getConvertedManaCost(c.getManaCost()) == 2) {
                            mTwo = mTwo + 1;
                        }
                        if (CardUtil.getConvertedManaCost(c.getManaCost()) == 3) {
                            mThree = mThree + 1;
                        }
                        if (CardUtil.getConvertedManaCost(c.getManaCost()) == 4) {
                            mFour = mFour + 1;
                        }
                        if (CardUtil.getConvertedManaCost(c.getManaCost()) == 5) {
                            mFive = mFive + 1;
                        }
                        if (CardUtil.getConvertedManaCost(c.getManaCost()) >= 6) {
                            mSixMore = mSixMore + 1;
                        }
                    }

                    for (int i = 0; i < cList.size(); i++) {
                        c = cList.getCard(i);
                        tManaCost = tManaCost + CardUtil.getConvertedManaCost(c.getManaCost());
                    }
                    BigDecimal aManaCost = new BigDecimal(tManaCost / cList.size());
                    aManaCost = aManaCost.setScale(2, BigDecimal.ROUND_HALF_UP);
                    jLabelTotal.setText("Information about deck (total cards: " + cList.size() + "):");
                    jLabelManaCost.setText("Mana cost (ACC:" + aManaCost + ")");
                    Color cr = new Color(100, 100, 100);
                    if (cBlack == 0) {
                        jLabelBlack.setForeground(cr);
                    }
                    jLabelBlack.setText("Black: " + cBlack + " (" + cBlack * 100 / cList.size() + "%)");
                    if (cBlue == 0) {
                        jLabelBlue.setForeground(cr);
                    }
                    jLabelBlue.setText("Blue: " + cBlue + " (" + cBlue * 100 / cList.size() + "%)");
                    if (cGreen == 0) {
                        jLabelGreen.setForeground(cr);
                    }
                    jLabelGreen.setText("Green: " + cGreen + " (" + cGreen * 100 / cList.size() + "%)");
                    if (cRed == 0) {
                        jLabelRed.setForeground(cr);
                    }
                    jLabelRed.setText("Red: " + cRed + " (" + cRed * 100 / cList.size() + "%)");
                    if (cWhite == 0) {
                        jLabelWhite.setForeground(cr);
                    }
                    jLabelWhite.setText("White: " + cWhite + " (" + cWhite * 100 / cList.size() + "%)");
                    if (cMulticolor == 0) {
                        jLabelMultiColor.setForeground(cr);
                    }
                    jLabelMultiColor.setText("Multicolor: " + cMulticolor + " (" + cMulticolor * 100 / cList.size()
                            + "%)");
                    if (cColorless == 0) {
                        jLabelColorless.setForeground(cr);
                    }
                    jLabelColorless.setText("Colorless: " + cColorless + " (" + cColorless * 100 / cList.size() + "%)");
                    if (cLand == 0) {
                        jLabelLand.setForeground(cr);
                    }
                    jLabelLand.setText("Land: " + cLand + " (" + cLand * 100 / cList.size() + "%)");
                    if (cArtifact == 0) {
                        jLabelArtifact.setForeground(cr);
                    }
                    jLabelArtifact.setText("Artifact: " + cArtifact + " (" + cArtifact * 100 / cList.size() + "%)");
                    if (cCreature == 0) {
                        jLabelCreature.setForeground(cr);
                    }
                    jLabelCreature.setText("Creature: " + cCreature + " (" + cCreature * 100 / cList.size() + "%)");
                    if (cEnchant == 0) {
                        jLabelEnchant.setForeground(cr);
                    }
                    jLabelEnchant.setText("Enchant: " + cEnchant + " (" + cEnchant * 100 / cList.size() + "%)");
                    if (cInstant == 0) {
                        jLabelInstant.setForeground(cr);
                    }
                    jLabelInstant.setText("Instant: " + cInstant + " (" + cInstant * 100 / cList.size() + "%)");
                    if (cLandType == 0) {
                        jLabelLandType.setForeground(cr);
                    }
                    jLabelLandType.setText("Land: " + cLandType + " (" + cLandType * 100 / cList.size() + "%)");
                    if (cPlaneswalker == 0) {
                        jLabelPlaneswalker.setForeground(cr);
                    }
                    jLabelPlaneswalker.setText("Planeswalker: " + cPlaneswalker + " (" + cPlaneswalker * 100
                            / cList.size() + "%)");
                    if (cSorcery == 0) {
                        jLabelSorcery.setForeground(cr);
                    }
                    jLabelSorcery.setText("Sorcery: " + cSorcery + " (" + cSorcery * 100 / cList.size() + "%)");
                    if (mZero == 0) {
                        jLabelZeroMana.setForeground(cr);
                    }
                    jLabelZeroMana.setText("Zero mana: " + mZero + " (" + mZero * 100 / cList.size() + "%)");
                    if (mOne == 0) {
                        jLabelOneMana.setForeground(cr);
                    }
                    jLabelOneMana.setText("One mana: " + mOne + " (" + mOne * 100 / cList.size() + "%)");
                    if (mTwo == 0) {
                        jLabelTwoMana.setForeground(cr);
                    }
                    jLabelTwoMana.setText("Two mana: " + mTwo + " (" + mTwo * 100 / cList.size() + "%)");
                    if (mThree == 0) {
                        jLabelThreeMana.setForeground(cr);
                    }
                    jLabelThreeMana.setText("Three mana :" + mThree + " (" + mThree * 100 / cList.size() + "%)");
                    if (mFour == 0) {
                        jLabelFourMana.setForeground(cr);
                    }
                    jLabelFourMana.setText("Four mana: " + mFour + " (" + mFour * 100 / cList.size() + "%)");
                    if (mFive == 0) {
                        jLabelFiveMana.setForeground(cr);
                    }
                    jLabelFiveMana.setText("Five mana: " + mFive + " (" + mFive * 100 / cList.size() + "%)");
                    if (mSixMore == 0) {
                        jLabelSixMana.setForeground(cr);
                    }
                    jLabelSixMana.setText("Six and more: " + mSixMore + " (" + mSixMore * 100 / cList.size() + "%)");
                }
            });

            getContentPane().add(getJButton1());
            getContentPane().add(getJLabel1xx());
            getContentPane().add(getJButtonOk());
            getContentPane().add(getJPanel1());
            getContentPane().add(getJPanel2());
            getContentPane().add(getJPanel3());
            getContentPane().add(getJPanel4());
            getContentPane().add(getJPanel5());
            getContentPane().add(getJLabel1xxxxx());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * <p>
     * Getter for the field <code>jPanel1</code>.
     * </p>
     * 
     * @return a {@link javax.swing.JPanel} object.
     */
    private JPanel getJPanel1() {
        if (jPanel1 == null) {
            jPanel1 = new JPanel();

            jPanel1.setLayout(null);
            jPanel1.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
            jPanel1.setBackground(new java.awt.Color(192, 192, 192));
            jPanel1.setBounds(5, 35, 137, 203);
            jPanel1.add(getJLabel1());
            jPanel1.add(getJSeparator1());
            jPanel1.add(getJLabel2());
            jPanel1.add(getJLabel3());
            jPanel1.add(getJLabel4());
            jPanel1.add(getJLabel5());
            jPanel1.add(getJLabel6());
            jPanel1.add(getJLabel7());
            jPanel1.add(getJLabel8());
            jPanel1.add(getJLabel1x());
        }
        return jPanel1;
    }

    /**
     * <p>
     * Getter for the field <code>jLabel2</code>.
     * </p>
     * 
     * @return a {@link javax.swing.JLabel} object.
     */
    private JLabel getJLabel2() {
        if (jLabel2 == null) {
            jLabel2 = new JLabel();
            jLabel2.setText("Color");
            jLabel2.setHorizontalAlignment(SwingConstants.CENTER);
            jLabel2.setFont(new java.awt.Font("Segoe UI", 0, 14));
            jLabel2.setPreferredSize(new java.awt.Dimension(152, 39));
            jLabel2.setLayout(null);
            jLabel2.setBounds(2, -3, 135, 26);
        }
        return jLabel2;
    }

    /**
     * <p>
     * Getter for the field <code>jSeparator1</code>.
     * </p>
     * 
     * @return a {@link javax.swing.JSeparator} object.
     */
    private JSeparator getJSeparator1() {
        if (jSeparator1 == null) {
            jSeparator1 = new JSeparator();
            jSeparator1.setPreferredSize(new java.awt.Dimension(117, 6));
            jSeparator1.setLayout(null);
            jSeparator1.setBounds(1, 20, 136, 5);
        }
        return jSeparator1;
    }

    /**
     * <p>
     * Getter for the field <code>jButtonOk</code>.
     * </p>
     * 
     * @return a {@link javax.swing.JButton} object.
     */
    private JButton getJButtonOk() {
        if (jButtonOk == null) {
            jButtonOk = new JButton();
            jButtonOk.setLayout(null);
            jButtonOk.setText("OK");
            jButtonOk.setBounds(206, 536, 168, 31);
            jButtonOk.addMouseListener(new MouseInputAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    jF.setEnabled(true);
                    dispose();
                }
            });
        }
        return jButtonOk;
    }

    /**
     * <p>
     * Getter for the field <code>jLabel1</code>.
     * </p>
     * 
     * @return a {@link javax.swing.JLabel} object.
     */
    private JLabel getJLabel1() {
        if (jLabelBlack == null) {
            jLabelBlack = new JLabel();
            jLabelBlack.setText("Black:");
            jLabelBlack.setPreferredSize(new java.awt.Dimension(105, 12));
            jLabelBlack.setLayout(null);
            jLabelBlack.setBounds(10, 28, 127, 13);
        }
        return jLabelBlack;
    }

    /**
     * <p>
     * Getter for the field <code>jLabel3</code>.
     * </p>
     * 
     * @return a {@link javax.swing.JLabel} object.
     */
    private JLabel getJLabel3() {
        if (jLabelBlue == null) {
            jLabelBlue = new JLabel();
            jLabelBlue.setText("Blue:");
            jLabelBlue.setLayout(null);
            jLabelBlue.setBounds(10, 50, 127, 13);
        }
        return jLabelBlue;
    }

    /**
     * <p>
     * Getter for the field <code>jLabel4</code>.
     * </p>
     * 
     * @return a {@link javax.swing.JLabel} object.
     */
    private JLabel getJLabel4() {
        if (jLabelGreen == null) {
            jLabelGreen = new JLabel();
            jLabelGreen.setText("Green:");
            jLabelGreen.setLayout(null);
            jLabelGreen.setBounds(10, 72, 127, 13);
        }
        return jLabelGreen;
    }

    /**
     * <p>
     * getJLabel5.
     * </p>
     * 
     * @return a {@link javax.swing.JLabel} object.
     */
    private JLabel getJLabel5() {
        if (jLabelRed == null) {
            jLabelRed = new JLabel();
            jLabelRed.setText("Red:");
            jLabelRed.setLayout(null);
            jLabelRed.setBounds(10, 94, 127, 14);
        }
        return jLabelRed;
    }

    /**
     * <p>
     * getJLabel6.
     * </p>
     * 
     * @return a {@link javax.swing.JLabel} object.
     */
    private JLabel getJLabel6() {
        if (jLabelWhite == null) {
            jLabelWhite = new JLabel();
            jLabelWhite.setText("White:");
            jLabelWhite.setLayout(null);
            jLabelWhite.setBounds(10, 116, 127, 13);
        }
        return jLabelWhite;
    }

    /**
     * <p>
     * getJLabel7.
     * </p>
     * 
     * @return a {@link javax.swing.JLabel} object.
     */
    private JLabel getJLabel7() {
        if (jLabelMultiColor == null) {
            jLabelMultiColor = new JLabel();
            jLabelMultiColor.setText("Multicolor:");
            jLabelMultiColor.setLayout(null);
            jLabelMultiColor.setBounds(10, 138, 127, 12);
        }
        return jLabelMultiColor;
    }

    /**
     * <p>
     * getJLabel8.
     * </p>
     * 
     * @return a {@link javax.swing.JLabel} object.
     */
    private JLabel getJLabel8() {
        if (jLabelColorless == null) {
            jLabelColorless = new JLabel();
            jLabelColorless.setText("Colorless:");
            jLabelColorless.setLayout(null);
            jLabelColorless.setBounds(10, 160, 128, 11);
        }
        return jLabelColorless;
    }

    /**
     * <p>
     * getJLabel1x.
     * </p>
     * 
     * @return a {@link javax.swing.JLabel} object.
     */
    private JLabel getJLabel1x() {
        if (jLabelLand == null) {
            jLabelLand = new JLabel();
            jLabelLand.setText("Land: ");
            jLabelLand.setLayout(null);
            jLabelLand.setBounds(10, 182, 129, 10);
        }
        return jLabelLand;
    }

    /**
     * <p>
     * getJLabel1xx.
     * </p>
     * 
     * @return a {@link javax.swing.JLabel} object.
     */
    private JLabel getJLabel1xx() {
        if (jLabelTotal == null) {
            jLabelTotal = new JLabel();
            jLabelTotal.setText("Information about deck:");
            jLabelTotal.setLayout(null);
            jLabelTotal.setBounds(5, 0, 454, 35);
        }
        return jLabelTotal;
    }

    /**
     * <p>
     * Getter for the field <code>jPanel2</code>.
     * </p>
     * 
     * @return a {@link javax.swing.JPanel} object.
     */
    private JPanel getJPanel2() {
        if (jPanel2 == null) {
            jPanel2 = new JPanel();

            jPanel2.setBackground(new java.awt.Color(192, 192, 192));
            jPanel2.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
            jPanel2.setLayout(null);
            jPanel2.setBounds(153, 35, 137, 203);
            jPanel2.add(getJLabel1xxx());
            jPanel2.add(getJSeparator2());
            jPanel2.add(getJLabel3x());
            jPanel2.add(getJLabel4x());
            jPanel2.add(getJLabel5x());
            jPanel2.add(getJLabel6x());
            jPanel2.add(getJLabel7x());
            jPanel2.add(getJLabel8x());
            jPanel2.add(getJLabel10());
        }
        return jPanel2;
    }

    /**
     * <p>
     * getJLabel1xxx.
     * </p>
     * 
     * @return a {@link javax.swing.JLabel} object.
     */
    private JLabel getJLabel1xxx() {
        if (jLabelArtifact == null) {
            jLabelArtifact = new JLabel();
            jLabelArtifact.setText("Artifact:");
            jLabelArtifact.setPreferredSize(new java.awt.Dimension(105, 12));
            jLabelArtifact.setLayout(null);
            jLabelArtifact.setBounds(10, 28, 127, 13);
        }
        return jLabelArtifact;
    }

    /**
     * <p>
     * Getter for the field <code>jSeparator2</code>.
     * </p>
     * 
     * @return a {@link javax.swing.JSeparator} object.
     */
    private JSeparator getJSeparator2() {
        if (jSeparator2 == null) {
            jSeparator2 = new JSeparator();
            jSeparator2.setPreferredSize(new java.awt.Dimension(117, 6));
            jSeparator2.setLayout(null);
            jSeparator2.setBounds(1, 20, 136, 5);
        }
        return jSeparator2;
    }

    /**
     * <p>
     * getJLabel3x.
     * </p>
     * 
     * @return a {@link javax.swing.JLabel} object.
     */
    private JLabel getJLabel3x() {
        if (jLabel3 == null) {
            jLabel3 = new JLabel();
            jLabel3.setText("Type");
            jLabel3.setHorizontalAlignment(SwingConstants.CENTER);
            jLabel3.setFont(new java.awt.Font("Segoe UI", 0, 14));
            jLabel3.setPreferredSize(new java.awt.Dimension(152, 39));
            jLabel3.setLayout(null);
            jLabel3.setBounds(2, -3, 135, 26);
        }
        return jLabel3;
    }

    /**
     * <p>
     * getJLabel4x.
     * </p>
     * 
     * @return a {@link javax.swing.JLabel} object.
     */
    private JLabel getJLabel4x() {
        if (jLabelCreature == null) {
            jLabelCreature = new JLabel();
            jLabelCreature.setText("Creature:");
            jLabelCreature.setLayout(null);
            jLabelCreature.setBounds(10, 53, 127, 13);
        }
        return jLabelCreature;
    }

    /**
     * <p>
     * getJLabel5x.
     * </p>
     * 
     * @return a {@link javax.swing.JLabel} object.
     */
    private JLabel getJLabel5x() {
        if (jLabelEnchant == null) {
            jLabelEnchant = new JLabel();
            jLabelEnchant.setText("Enchant:");
            jLabelEnchant.setLayout(null);
            jLabelEnchant.setBounds(10, 79, 127, 13);
        }
        return jLabelEnchant;
    }

    /**
     * <p>
     * getJLabel6x.
     * </p>
     * 
     * @return a {@link javax.swing.JLabel} object.
     */
    private JLabel getJLabel6x() {
        if (jLabelInstant == null) {
            jLabelInstant = new JLabel();
            jLabelInstant.setText("Instant:");
            jLabelInstant.setLayout(null);
            jLabelInstant.setBounds(10, 105, 127, 14);
        }
        return jLabelInstant;
    }

    /**
     * <p>
     * getJLabel7x.
     * </p>
     * 
     * @return a {@link javax.swing.JLabel} object.
     */
    private JLabel getJLabel7x() {
        if (jLabelLandType == null) {
            jLabelLandType = new JLabel();
            jLabelLandType.setText("Land:");
            jLabelLandType.setLayout(null);
            jLabelLandType.setBounds(10, 130, 127, 13);
        }
        return jLabelLandType;
    }

    /**
     * <p>
     * getJLabel8x.
     * </p>
     * 
     * @return a {@link javax.swing.JLabel} object.
     */
    private JLabel getJLabel8x() {
        if (jLabelPlaneswalker == null) {
            jLabelPlaneswalker = new JLabel();
            jLabelPlaneswalker.setText("Planeswalker:");
            jLabelPlaneswalker.setLayout(null);
            jLabelPlaneswalker.setBounds(10, 156, 127, 13);
        }
        return jLabelPlaneswalker;
    }

    /**
     * <p>
     * getJLabel10.
     * </p>
     * 
     * @return a {@link javax.swing.JLabel} object.
     */
    private JLabel getJLabel10() {
        if (jLabelSorcery == null) {
            jLabelSorcery = new JLabel();
            jLabelSorcery.setText("Sorcery:");
            jLabelSorcery.setLayout(null);
            jLabelSorcery.setBounds(10, 182, 127, 11);
        }
        return jLabelSorcery;
    }

    /**
     * <p>
     * Getter for the field <code>jPanel3</code>.
     * </p>
     * 
     * @return a {@link javax.swing.JPanel} object.
     */
    private JPanel getJPanel3() {
        if (jPanel3 == null) {
            jPanel3 = new JPanel();
            jPanel3.setBackground(new java.awt.Color(192, 192, 192));
            jPanel3.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
            jPanel3.setLayout(null);
            jPanel3.setBounds(302, 35, 137, 203);
            jPanel3.add(getJLabel1xxxx());
            jPanel3.add(getJSeparator3());
            jPanel3.add(getJLabel4xx());
            jPanel3.add(getJLabel5xx());
            jPanel3.add(getJLabel6xx());
            jPanel3.add(getJLabel7xx());
            jPanel3.add(getJLabel8xx());
            jPanel3.add(getJLabel9());
            jPanel3.add(getJLabel10x());
        }
        return jPanel3;
    }

    /**
     * <p>
     * getJLabel1xxxx.
     * </p>
     * 
     * @return a {@link javax.swing.JLabel} object.
     */
    private JLabel getJLabel1xxxx() {
        if (jLabelZeroMana == null) {
            jLabelZeroMana = new JLabel();
            jLabelZeroMana.setText("Zero mana:");
            jLabelZeroMana.setPreferredSize(new java.awt.Dimension(105, 12));
            jLabelZeroMana.setLayout(null);
            jLabelZeroMana.setBounds(10, 28, 127, 13);
        }
        return jLabelZeroMana;
    }

    /**
     * <p>
     * Getter for the field <code>jSeparator3</code>.
     * </p>
     * 
     * @return a {@link javax.swing.JSeparator} object.
     */
    private JSeparator getJSeparator3() {
        if (jSeparator3 == null) {
            jSeparator3 = new JSeparator();
            jSeparator3.setPreferredSize(new java.awt.Dimension(117, 6));
            jSeparator3.setLayout(null);
            jSeparator3.setBounds(1, 20, 136, 5);
        }
        return jSeparator3;
    }

    /**
     * <p>
     * getJLabel4xx.
     * </p>
     * 
     * @return a {@link javax.swing.JLabel} object.
     */
    private JLabel getJLabel4xx() {
        if (jLabelManaCost == null) {
            jLabelManaCost = new JLabel();
            jLabelManaCost.setText("Mana cost");
            jLabelManaCost.setHorizontalAlignment(SwingConstants.CENTER);
            jLabelManaCost.setFont(new java.awt.Font("Segoe UI", 0, 14));
            jLabelManaCost.setPreferredSize(new java.awt.Dimension(152, 39));
            jLabelManaCost.setLayout(null);
            jLabelManaCost.setBounds(2, -3, 135, 26);
        }
        return jLabelManaCost;
    }

    /**
     * <p>
     * getJLabel5xx.
     * </p>
     * 
     * @return a {@link javax.swing.JLabel} object.
     */
    private JLabel getJLabel5xx() {
        if (jLabelOneMana == null) {
            jLabelOneMana = new JLabel();
            jLabelOneMana.setText("One mana:");
            jLabelOneMana.setLayout(null);
            jLabelOneMana.setBounds(10, 53, 127, 13);
        }
        return jLabelOneMana;
    }

    /**
     * <p>
     * getJLabel6xx.
     * </p>
     * 
     * @return a {@link javax.swing.JLabel} object.
     */
    private JLabel getJLabel6xx() {
        if (jLabelTwoMana == null) {
            jLabelTwoMana = new JLabel();
            jLabelTwoMana.setText("Two mana:");
            jLabelTwoMana.setLayout(null);
            jLabelTwoMana.setBounds(10, 79, 127, 13);
        }
        return jLabelTwoMana;
    }

    /**
     * <p>
     * getJLabel7xx.
     * </p>
     * 
     * @return a {@link javax.swing.JLabel} object.
     */
    private JLabel getJLabel7xx() {
        if (jLabelThreeMana == null) {
            jLabelThreeMana = new JLabel();
            jLabelThreeMana.setText("Three mana:");
            jLabelThreeMana.setLayout(null);
            jLabelThreeMana.setBounds(10, 105, 127, 14);
        }
        return jLabelThreeMana;
    }

    /**
     * <p>
     * getJLabel8xx.
     * </p>
     * 
     * @return a {@link javax.swing.JLabel} object.
     */
    private JLabel getJLabel8xx() {
        if (jLabelFourMana == null) {
            jLabelFourMana = new JLabel();
            jLabelFourMana.setText("Four mana:");
            jLabelFourMana.setLayout(null);
            jLabelFourMana.setBounds(10, 130, 127, 13);
        }
        return jLabelFourMana;
    }

    /**
     * <p>
     * getJLabel9.
     * </p>
     * 
     * @return a {@link javax.swing.JLabel} object.
     */
    private JLabel getJLabel9() {
        if (jLabelFiveMana == null) {
            jLabelFiveMana = new JLabel();
            jLabelFiveMana.setText("Five mana:");
            jLabelFiveMana.setLayout(null);
            jLabelFiveMana.setBounds(10, 156, 127, 13);
        }
        return jLabelFiveMana;
    }

    /**
     * <p>
     * getJLabel10x.
     * </p>
     * 
     * @return a {@link javax.swing.JLabel} object.
     */
    private JLabel getJLabel10x() {
        if (jLabelSixMana == null) {
            jLabelSixMana = new JLabel();
            jLabelSixMana.setText("Six and more:");
            jLabelSixMana.setLayout(null);
            jLabelSixMana.setBounds(10, 182, 127, 11);
        }
        return jLabelSixMana;
    }

    /**
     * <p>
     * getJList1.
     * </p>
     * 
     * @return a {@link javax.swing.JList} object.
     */
    private JList getJList1() {
        CardList rList;
        rList = tModel.getCards();

        rList.shuffle();
        ListModel jList1Model;
        if (jListFirstHand == null) {
            if (rList.size() >= 40) {
                jList1Model = new DefaultComboBoxModel(new String[] { rList.getCard(0).getName(),
                        rList.getCard(1).getName(), rList.getCard(2).getName(), rList.getCard(3).getName(),
                        rList.getCard(4).getName(), rList.getCard(5).getName(), rList.getCard(6).getName() });
                jListFirstHand = new JList();
            } else {
                jList1Model = new DefaultComboBoxModel(new String[] { "Few cards." });
                jListFirstHand = new JList();
            }
        } else {
            if (rList.size() >= 40) {
                jList1Model = new DefaultComboBoxModel(new String[] { rList.getCard(0).getName(),
                        rList.getCard(1).getName(), rList.getCard(2).getName(), rList.getCard(3).getName(),
                        rList.getCard(4).getName(), rList.getCard(5).getName(), rList.getCard(6).getName() });

            } else {
                jList1Model = new DefaultComboBoxModel(new String[] { "Few cards." });

            }
        }

        jListFirstHand.setModel(jList1Model);
        jListFirstHand.setLayout(null);
        jListFirstHand.setBackground(new java.awt.Color(192, 192, 192));
        jListFirstHand.setSelectionBackground(new java.awt.Color(192, 192, 192));
        jListFirstHand.setSelectionForeground(new java.awt.Color(0, 0, 0));
        jListFirstHand.setFixedCellHeight(24);
        jListFirstHand.setBounds(2, 21, 133, 167);

        return jListFirstHand;
    }

    /**
     * <p>
     * Getter for the field <code>jPanel4</code>.
     * </p>
     * 
     * @return a {@link javax.swing.JPanel} object.
     */
    private JPanel getJPanel4() {
        if (jPanel4 == null) {
            jPanel4 = new JPanel();
            jPanel4.setBackground(new java.awt.Color(192, 192, 192));
            jPanel4.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
            jPanel4.setLayout(null);
            jPanel4.setBounds(451, 35, 137, 202);
            jPanel4.add(getJSeparator4());
            jPanel4.add(getJLabel4xxx());
            jPanel4.add(getJList1());
            jPanel4.add(getJButton1());
        } else {
            jPanel4.removeAll();
            MigLayout jPanel4Layout = new MigLayout();
            jPanel4.setBackground(new java.awt.Color(192, 192, 192));
            jPanel4.setPreferredSize(new java.awt.Dimension(139, 201));
            jPanel4.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
            jPanel4.setLayout(jPanel4Layout);
            jPanel4.add(getJSeparator4());
            jPanel4.add(getJLabel4xxx());
            jPanel4.add(getJList1());
            jPanel4.add(getJButton1());
        }
        return jPanel4;

    }

    /**
     * <p>
     * Getter for the field <code>jSeparator4</code>.
     * </p>
     * 
     * @return a {@link javax.swing.JSeparator} object.
     */
    private JSeparator getJSeparator4() {
        if (jSeparator4 == null) {
            jSeparator4 = new JSeparator();
            jSeparator4.setPreferredSize(new java.awt.Dimension(138, 8));
            jSeparator4.setLayout(null);
            jSeparator4.setBounds(0, 19, 137, 7);
        }
        return jSeparator4;
    }

    /**
     * <p>
     * getJLabel4xxx.
     * </p>
     * 
     * @return a {@link javax.swing.JLabel} object.
     */
    private JLabel getJLabel4xxx() {
        if (jLabel4 == null) {
            jLabel4 = new JLabel();
            jLabel4.setText("Random start hand");
            jLabel4.setHorizontalAlignment(SwingConstants.CENTER);
            jLabel4.setFont(new java.awt.Font("Segoe UI", 0, 14));
            jLabel4.setPreferredSize(new java.awt.Dimension(136, 24));
            jLabel4.setLayout(null);
            jLabel4.setBounds(2, 0, 135, 20);
        }
        return jLabel4;
    }

    /**
     * <p>
     * getJButton1.
     * </p>
     * 
     * @return a {@link javax.swing.JButton} object.
     */
    private JButton getJButton1() {
        CardList rList;
        rList = tModel.getCards();
        if (jButtonRegenerate == null) {
            if (rList.size() >= 40) {
                jButtonRegenerate = new JButton();
                jButtonRegenerate.setLayout(null);
                jButtonRegenerate.setText("Regenerate hand");
                jButtonRegenerate.setPreferredSize(new java.awt.Dimension(139, 21));
                jButtonRegenerate.setBounds(2, 189, 133, 13);
                jButtonRegenerate.addActionListener(new java.awt.event.ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        jButtonRegenerate_actionPerformed(e);
                    }
                });
            } else {
                jButtonRegenerate = new JButton();
                jButtonRegenerate.setBounds(2, 189, 133, 13);
                jButtonRegenerate.setVisible(false);
            }
        }
        return jButtonRegenerate;
    }

    /**
     * <p>
     * jButtonRegenerate_actionPerformed.
     * </p>
     * 
     * @param e
     *            a {@link java.awt.event.ActionEvent} object.
     */
    void jButtonRegenerate_actionPerformed(ActionEvent e) {
        getContentPane().removeAll();
        getContentPane().add(getJPanel5());
        getContentPane().add(getJLabel1xx());
        getContentPane().add(getJButtonOk());
        getContentPane().add(getJPanel1());
        getContentPane().add(getJPanel2());
        getContentPane().add(getJPanel3());
        getContentPane().add(getJPanel4());
        getContentPane().add(getJPanel5());
        getContentPane().add(getJLabel1xxxxx());
        getContentPane().repaint();

    }

    /**
     * <p>
     * Getter for the field <code>jPanel5</code>.
     * </p>
     * 
     * @return a {@link javax.swing.JPanel} object.
     */
    private JPanel getJPanel5() {
        if (jPanel5 == null) {
            jPanel5 = new JPanel();
            jPanel5.setLayout(null);
            jPanel5.setBounds(5, 262, 583, 270);
            jPanel5.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
            jPanel5.add(getJScrollPane1());
        }
        return jPanel5;
    }

    /**
     * <p>
     * Getter for the field <code>jTable1</code>.
     * </p>
     * 
     * @return a {@link javax.swing.JTable} object.
     */
    private JTable getJTable1() {
        if (jTable1 == null) {
            DefaultTableModel dm = new DefaultTableModel();
            dm.setDataVector(new Object[][] { {} }, new Object[] { "Card", "Qty", "1st", "2nd", "3rd", "4th", "5th",
                    "6th", "7th" });

            jTable1 = new JTable(dm);
            CardList rList;
            rList = tModel.getCards();
            String[] cardsName = new String[rList.size()];
            int cCount;
            float fCount;
            float firstTurnF, secondTurnF, thirdTurnF, fourthTurnF, fivethTurnF, sixthTurnF, seventhTurnF;

            for (int i = 0; i < rList.size(); i++) {
                cardsName[i] = rList.getCard(i).getName();
            }
            Arrays.sort(cardsName);
            jTable1.setValueAt("Few cards.", 0, 0);

            if (rList.size() >= 40) {
                jTable1.setValueAt(cardsName[0], 0, 0);
                cCount = 1;
                for (int i = 1; i < cardsName.length; i++) {
                    if (cardsName[i].equals(cardsName[i - 1])) {
                        cCount = cCount + 1;

                    } else {
                        dm.addRow(new Object[][] { {} });
                        jTable1.setValueAt(cardsName[i], dm.getRowCount() - 1, 0);
                        jTable1.setValueAt(cCount, dm.getRowCount() - 2, 1);
                        fCount = cCount;

                        firstTurnF = fCount / rList.size();
                        BigDecimal firstTurn = new BigDecimal(firstTurnF * 100);
                        firstTurn = firstTurn.setScale(1, BigDecimal.ROUND_HALF_UP);
                        jTable1.setValueAt(firstTurn.toString() + " %", dm.getRowCount() - 2, 2);

                        secondTurnF = (1 - firstTurnF) * fCount / (rList.size() - 1) + firstTurnF;
                        BigDecimal secondTurn = new BigDecimal(secondTurnF * 100);
                        secondTurn = secondTurn.setScale(1, BigDecimal.ROUND_HALF_UP);
                        jTable1.setValueAt(secondTurn.toString() + " %", dm.getRowCount() - 2, 3);

                        thirdTurnF = (1 - secondTurnF) * fCount / (rList.size() - 2) + secondTurnF;
                        BigDecimal thirdTurn = new BigDecimal(thirdTurnF * 100);
                        thirdTurn = thirdTurn.setScale(1, BigDecimal.ROUND_HALF_UP);
                        jTable1.setValueAt(thirdTurn.toString() + " %", dm.getRowCount() - 2, 4);

                        fourthTurnF = (1 - thirdTurnF) * fCount / (rList.size() - 3) + thirdTurnF;
                        BigDecimal fourthTurn = new BigDecimal(fourthTurnF * 100);
                        fourthTurn = fourthTurn.setScale(1, BigDecimal.ROUND_HALF_UP);
                        jTable1.setValueAt(fourthTurn.toString() + " %", dm.getRowCount() - 2, 5);

                        fivethTurnF = (1 - fourthTurnF) * fCount / (rList.size() - 4) + fourthTurnF;
                        BigDecimal fivethTurn = new BigDecimal(fivethTurnF * 100);
                        fivethTurn = fivethTurn.setScale(1, BigDecimal.ROUND_HALF_UP);
                        jTable1.setValueAt(fivethTurn.toString() + " %", dm.getRowCount() - 2, 6);

                        sixthTurnF = (1 - fivethTurnF) * fCount / (rList.size() - 5) + fivethTurnF;
                        BigDecimal sixthTurn = new BigDecimal(sixthTurnF * 100);
                        sixthTurn = sixthTurn.setScale(1, BigDecimal.ROUND_HALF_UP);
                        jTable1.setValueAt(sixthTurn.toString() + " %", dm.getRowCount() - 2, 7);

                        seventhTurnF = (1 - sixthTurnF) * fCount / (rList.size() - 6) + sixthTurnF;
                        BigDecimal seventhTurn = new BigDecimal(seventhTurnF * 100);
                        seventhTurn = seventhTurn.setScale(1, BigDecimal.ROUND_HALF_UP);
                        jTable1.setValueAt(seventhTurn.toString() + " %", dm.getRowCount() - 2, 8);

                        cCount = 1;
                    }
                    if (i == cardsName.length - 1) {
                        jTable1.setValueAt(cCount, dm.getRowCount() - 1, 1);
                        fCount = cCount;

                        firstTurnF = fCount / rList.size();
                        BigDecimal firstTurn = new BigDecimal(firstTurnF * 100);
                        firstTurn = firstTurn.setScale(1, BigDecimal.ROUND_HALF_UP);
                        jTable1.setValueAt(firstTurn.toString() + " %", dm.getRowCount() - 1, 2);

                        secondTurnF = (1 - firstTurnF) * fCount / (rList.size() - 1) + firstTurnF;
                        BigDecimal secondTurn = new BigDecimal(secondTurnF * 100);
                        secondTurn = secondTurn.setScale(1, BigDecimal.ROUND_HALF_UP);
                        jTable1.setValueAt(secondTurn.toString() + " %", dm.getRowCount() - 1, 3);

                        thirdTurnF = (1 - secondTurnF) * fCount / (rList.size() - 2) + secondTurnF;
                        BigDecimal thirdTurn = new BigDecimal(thirdTurnF * 100);
                        thirdTurn = thirdTurn.setScale(1, BigDecimal.ROUND_HALF_UP);
                        jTable1.setValueAt(thirdTurn.toString() + " %", dm.getRowCount() - 1, 4);

                        fourthTurnF = (1 - thirdTurnF) * fCount / (rList.size() - 3) + thirdTurnF;
                        BigDecimal fourthTurn = new BigDecimal(fourthTurnF * 100);
                        fourthTurn = fourthTurn.setScale(1, BigDecimal.ROUND_HALF_UP);
                        jTable1.setValueAt(fourthTurn.toString() + " %", dm.getRowCount() - 1, 5);

                        fivethTurnF = (1 - fourthTurnF) * fCount / (rList.size() - 4) + fourthTurnF;
                        BigDecimal fivethTurn = new BigDecimal(fivethTurnF * 100);
                        fivethTurn = fivethTurn.setScale(1, BigDecimal.ROUND_HALF_UP);
                        jTable1.setValueAt(fivethTurn.toString() + " %", dm.getRowCount() - 1, 6);

                        sixthTurnF = (1 - fivethTurnF) * fCount / (rList.size() - 5) + fivethTurnF;
                        BigDecimal sixthTurn = new BigDecimal(sixthTurnF * 100);
                        sixthTurn = sixthTurn.setScale(1, BigDecimal.ROUND_HALF_UP);
                        jTable1.setValueAt(sixthTurn.toString() + " %", dm.getRowCount() - 1, 7);

                        seventhTurnF = (1 - sixthTurnF) * fCount / (rList.size() - 6) + sixthTurnF;
                        BigDecimal seventhTurn = new BigDecimal(seventhTurnF * 100);
                        seventhTurn = seventhTurn.setScale(1, BigDecimal.ROUND_HALF_UP);
                        jTable1.setValueAt(seventhTurn.toString() + " %", dm.getRowCount() - 1, 8);

                    }

                }
            }

            jTable1.getColumn("Qty").setMaxWidth(50);
            jTable1.getColumn("1st").setMaxWidth(50);
            jTable1.getColumn("2nd").setMaxWidth(50);
            jTable1.getColumn("3rd").setMaxWidth(50);
            jTable1.getColumn("4th").setMaxWidth(50);
            jTable1.getColumn("5th").setMaxWidth(50);
            jTable1.getColumn("6th").setMaxWidth(50);
            jTable1.getColumn("7th").setMaxWidth(50);
            jTable1.setRowHeight(18);
            jTable1.setPreferredSize(new java.awt.Dimension(576, 18 * dm.getRowCount() + 3));
        }
        return jTable1;
    }

    /**
     * <p>
     * Getter for the field <code>jScrollPane1</code>.
     * </p>
     * 
     * @return a {@link javax.swing.JScrollPane} object.
     */
    private JScrollPane getJScrollPane1() {
        if (jScrollPane1 == null) {
            jScrollPane1 = new JScrollPane();
            jScrollPane1.setBounds(2, 2, 582, 268);
            jScrollPane1.setSize(580, 268);
            jScrollPane1.setViewportView(getJTable1());
        }
        return jScrollPane1;
    }

    /**
     * <p>
     * getJLabel1xxxxx.
     * </p>
     * 
     * @return a {@link javax.swing.JLabel} object.
     */
    private JLabel getJLabel1xxxxx() {
        if (jLabel1 == null) {
            jLabel1 = new JLabel();
            jLabel1.setText("Draw Probabilities:");
            jLabel1.setLayout(null);
            jLabel1.setBounds(7, 237, 447, 25);
        }
        return jLabel1;
    }

}
