package forge.gui.deckeditor;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

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
import forge.CardList;
import forge.MyRandom;
import forge.card.CardRules;
import forge.card.CardType;
import forge.item.CardPrinted;
import forge.item.ItemPoolView;

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
public class DeckAnalysis extends javax.swing.JDialog {

    /** Constant <code>serialVersionUID=-8475271235196182185L</code>. */
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
    private final JFrame jF;
    // private ButtonGroup buttonGroup1;

    /** The filter card list. */
    private CardList filterCardList;

    /** The deck. */
    private ItemPoolView<CardPrinted> deck;

    /**
     * <p>
     * Constructor for GUI_DeckAnalysis.
     * </p>
     * 
     * @param g
     *            a {@link javax.swing.JFrame} object.
     * @param deckView
     *            the deck view
     */
    public DeckAnalysis(final JFrame g, final ItemPoolView<CardPrinted> deckView) {
        super(g);
        this.deck = deckView;

        this.jF = g;
        this.initGUI();
    }

    /**
     * <p>
     * initGUI.
     * </p>
     */
    private void initGUI() {
        try {

            this.getContentPane().setLayout(null);
            this.setVisible(true);
            final int wWidth = 600;
            final int wHeight = 600;
            this.setPreferredSize(new java.awt.Dimension(wWidth, wHeight));

            final Dimension screen = this.getToolkit().getScreenSize();
            final int x = (screen.width - wWidth) / 2;
            final int y = (screen.height - wHeight) / 2;
            this.setBounds(x, y, wWidth, wHeight);
            this.setResizable(false);
            this.setTitle("Deck Analysis");
            this.pack();
            // this.setIconImage(null);

            this.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(final WindowEvent arg0) {
                    DeckAnalysis.this.jF.setEnabled(true);
                }

                @Override
                public void windowOpened(final WindowEvent arg0) {

                    int cBlack, cBlue, cGreen, cRed, cWhite, cMulticolor, cColorless, cLand;
                    int cArtifact, cCreature, cEnchant, cInstant, cLandType, cPlaneswalker, cSorcery;
                    int mZero, mOne, mTwo, mThree, mFour, mFive, mSixMore;
                    float tManaCost;
                    CardRules c;
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

                    for (final Entry<CardPrinted, Integer> e : DeckAnalysis.this.deck) {
                        c = e.getKey().getCard();
                        final int cnt = e.getValue();

                        if (c.getColor().isMulticolor()) {
                            cMulticolor = cMulticolor + cnt;
                        } else {
                            if (c.getColor().isBlack()) {
                                cBlack = cBlack + cnt;
                            }
                            if (c.getColor().isBlue()) {
                                cBlue = cBlue + cnt;
                            }
                            if (c.getColor().isGreen()) {
                                cGreen = cGreen + cnt;
                            }
                            if (c.getColor().isRed()) {
                                cRed = cRed + cnt;
                            }
                            if (c.getColor().isWhite()) {
                                cWhite = cWhite + cnt;
                            }
                            if (c.getColor().isColorless()) {
                                if (c.getType().isLand()) {
                                    cLand = cLand + cnt;
                                } else {
                                    cColorless = cColorless + cnt;
                                }
                            }
                        }

                        // count card types
                        final CardType cType = c.getType();
                        if (cType.isArtifact()) {
                            cArtifact = cArtifact + cnt;
                        }
                        if (cType.isCreature()) {
                            cCreature = cCreature + cnt;
                        }
                        if (cType.isEnchantment()) {
                            cEnchant = cEnchant + cnt;
                        }
                        if (cType.isInstant()) {
                            cInstant = cInstant + cnt;
                        }
                        if (cType.isLand()) {
                            cLandType = cLandType + cnt;
                        }
                        if (cType.isPlaneswalker()) {
                            cPlaneswalker = cPlaneswalker + cnt;
                        }
                        if (cType.isSorcery()) {
                            cSorcery = cSorcery + cnt;
                        }

                        final int cmc = c.getManaCost().getCMC();
                        if (cmc == 0) {
                            mZero = mZero + cnt;
                        } else if (cmc == 1) {
                            mOne = mOne + cnt;
                        } else if (cmc == 2) {
                            mTwo = mTwo + cnt;
                        } else if (cmc == 3) {
                            mThree = mThree + cnt;
                        } else if (cmc == 4) {
                            mFour = mFour + cnt;
                        } else if (cmc == 5) {
                            mFive = mFive + 1;
                        } else if (cmc >= 6) {
                            mSixMore = mSixMore + 1;
                        }

                        tManaCost = tManaCost + (cmc * cnt);
                    }
                    final int total = DeckAnalysis.this.deck.countAll();
                    BigDecimal aManaCost = new BigDecimal(tManaCost / total);
                    aManaCost = aManaCost.setScale(2, BigDecimal.ROUND_HALF_UP);

                    DeckAnalysis.this.jLabelTotal.setText("Information about deck (total cards: " + total + "):");
                    DeckAnalysis.this.jLabelManaCost.setText("Mana cost (ACC:" + aManaCost + ")");
                    final Color cr = new Color(100, 100, 100);

                    if (cBlack == 0) {
                        DeckAnalysis.this.jLabelBlack.setForeground(cr);
                    }
                    DeckAnalysis.this.jLabelBlack.setText(DeckAnalysis.this.formatStat("Black", cBlack, total));
                    if (cBlue == 0) {
                        DeckAnalysis.this.jLabelBlue.setForeground(cr);
                    }
                    DeckAnalysis.this.jLabelBlue.setText(DeckAnalysis.this.formatStat("Blue", cBlue, total));
                    if (cGreen == 0) {
                        DeckAnalysis.this.jLabelGreen.setForeground(cr);
                    }
                    DeckAnalysis.this.jLabelGreen.setText(DeckAnalysis.this.formatStat("Green", cGreen, total));
                    if (cRed == 0) {
                        DeckAnalysis.this.jLabelRed.setForeground(cr);
                    }
                    DeckAnalysis.this.jLabelRed.setText(DeckAnalysis.this.formatStat("Red", cRed, total));
                    if (cWhite == 0) {
                        DeckAnalysis.this.jLabelWhite.setForeground(cr);
                    }
                    DeckAnalysis.this.jLabelWhite.setText(DeckAnalysis.this.formatStat("White", cWhite, total));
                    if (cMulticolor == 0) {
                        DeckAnalysis.this.jLabelMultiColor.setForeground(cr);
                    }
                    DeckAnalysis.this.jLabelMultiColor.setText(DeckAnalysis.this.formatStat("Multicolor", cMulticolor,
                            total));
                    if (cColorless == 0) {
                        DeckAnalysis.this.jLabelColorless.setForeground(cr);
                    }
                    DeckAnalysis.this.jLabelColorless.setText(DeckAnalysis.this.formatStat("Colorless", cColorless,
                            total));

                    if (cLand == 0) {
                        DeckAnalysis.this.jLabelLand.setForeground(cr);
                    }
                    DeckAnalysis.this.jLabelLand.setText(DeckAnalysis.this.formatStat("Land", cLand, total));
                    if (cArtifact == 0) {
                        DeckAnalysis.this.jLabelArtifact.setForeground(cr);
                    }
                    DeckAnalysis.this.jLabelArtifact.setText(DeckAnalysis.this.formatStat("Artifact", cArtifact, total));
                    if (cCreature == 0) {
                        DeckAnalysis.this.jLabelCreature.setForeground(cr);
                    }
                    DeckAnalysis.this.jLabelCreature.setText(DeckAnalysis.this.formatStat("Creature", cCreature, total));
                    if (cEnchant == 0) {
                        DeckAnalysis.this.jLabelEnchant.setForeground(cr);
                    }
                    DeckAnalysis.this.jLabelEnchant.setText(DeckAnalysis.this.formatStat("Enchant", cEnchant, total));
                    if (cInstant == 0) {
                        DeckAnalysis.this.jLabelInstant.setForeground(cr);
                    }
                    DeckAnalysis.this.jLabelInstant.setText(DeckAnalysis.this.formatStat("Instant", cInstant, total));
                    if (cLandType == 0) {
                        DeckAnalysis.this.jLabelLandType.setForeground(cr);
                    }
                    DeckAnalysis.this.jLabelLandType.setText(DeckAnalysis.this.formatStat("Land", cLandType, total));
                    if (cPlaneswalker == 0) {
                        DeckAnalysis.this.jLabelPlaneswalker.setForeground(cr);
                    }
                    DeckAnalysis.this.jLabelPlaneswalker.setText(DeckAnalysis.this.formatStat("Planeswalker",
                            cPlaneswalker, total));

                    if (cSorcery == 0) {
                        DeckAnalysis.this.jLabelSorcery.setForeground(cr);
                    }
                    DeckAnalysis.this.jLabelSorcery.setText(DeckAnalysis.this.formatStat("Sorcery", cSorcery, total));
                    if (mZero == 0) {
                        DeckAnalysis.this.jLabelZeroMana.setForeground(cr);
                    }
                    DeckAnalysis.this.jLabelZeroMana.setText(DeckAnalysis.this.formatStat("Zero mana", mZero, total));
                    if (mOne == 0) {
                        DeckAnalysis.this.jLabelOneMana.setForeground(cr);
                    }
                    DeckAnalysis.this.jLabelOneMana.setText(DeckAnalysis.this.formatStat("One mana", mOne, total));
                    if (mTwo == 0) {
                        DeckAnalysis.this.jLabelTwoMana.setForeground(cr);
                    }
                    DeckAnalysis.this.jLabelTwoMana.setText(DeckAnalysis.this.formatStat("Two mana", mTwo, total));
                    if (mThree == 0) {
                        DeckAnalysis.this.jLabelThreeMana.setForeground(cr);
                    }
                    DeckAnalysis.this.jLabelThreeMana.setText(DeckAnalysis.this.formatStat("Three mana", mThree, total));
                    if (mFour == 0) {
                        DeckAnalysis.this.jLabelFourMana.setForeground(cr);
                    }
                    DeckAnalysis.this.jLabelFourMana.setText(DeckAnalysis.this.formatStat("Four mana", mFour, total));
                    if (mFive == 0) {
                        DeckAnalysis.this.jLabelFiveMana.setForeground(cr);
                    }
                    DeckAnalysis.this.jLabelFiveMana.setText(DeckAnalysis.this.formatStat("Five mana", mFive, total));
                    if (mSixMore == 0) {
                        DeckAnalysis.this.jLabelSixMana.setForeground(cr);
                    }
                    DeckAnalysis.this.jLabelSixMana.setText(DeckAnalysis.this.formatStat("Six and more", mSixMore,
                            total));
                }
            });

            this.getContentPane().add(this.getJButton1());
            this.getContentPane().add(this.getJLabel1xx());
            this.getContentPane().add(this.getJButtonOk());
            this.getContentPane().add(this.getJPanel1());
            this.getContentPane().add(this.getJPanel2());
            this.getContentPane().add(this.getJPanel3());
            this.getContentPane().add(this.getJPanel4());
            this.getContentPane().add(this.getJPanel5());
            this.getContentPane().add(this.getJLabel1xxxxx());

        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    private String formatStat(final String statName, final int value, final int deckSize) {
        return String.format("%s: %d (%f%%)", statName, value, (100f * value) / deckSize);
    }

    /**
     * <p>
     * Getter for the field <code>jPanel1</code>.
     * </p>
     * 
     * @return a {@link javax.swing.JPanel} object.
     */
    private JPanel getJPanel1() {
        if (this.jPanel1 == null) {
            this.jPanel1 = new JPanel();

            this.jPanel1.setLayout(null);
            this.jPanel1.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
            this.jPanel1.setBackground(new java.awt.Color(192, 192, 192));
            this.jPanel1.setBounds(5, 35, 137, 203);
            this.jPanel1.add(this.getJLabel1());
            this.jPanel1.add(this.getJSeparator1());
            this.jPanel1.add(this.getJLabel2());
            this.jPanel1.add(this.getJLabel3());
            this.jPanel1.add(this.getJLabel4());
            this.jPanel1.add(this.getJLabel5());
            this.jPanel1.add(this.getJLabel6());
            this.jPanel1.add(this.getJLabel7());
            this.jPanel1.add(this.getJLabel8());
            this.jPanel1.add(this.getJLabel1x());
        }
        return this.jPanel1;
    }

    /**
     * <p>
     * Getter for the field <code>jLabel2</code>.
     * </p>
     * 
     * @return a {@link javax.swing.JLabel} object.
     */
    private JLabel getJLabel2() {
        if (this.jLabel2 == null) {
            this.jLabel2 = new JLabel();
            this.jLabel2.setText("Color");
            this.jLabel2.setHorizontalAlignment(SwingConstants.CENTER);
            this.jLabel2.setFont(new java.awt.Font("Segoe UI", 0, 14));
            this.jLabel2.setPreferredSize(new java.awt.Dimension(152, 39));
            this.jLabel2.setLayout(null);
            this.jLabel2.setBounds(2, -3, 135, 26);
        }
        return this.jLabel2;
    }

    /**
     * <p>
     * Getter for the field <code>jSeparator1</code>.
     * </p>
     * 
     * @return a {@link javax.swing.JSeparator} object.
     */
    private JSeparator getJSeparator1() {
        if (this.jSeparator1 == null) {
            this.jSeparator1 = new JSeparator();
            this.jSeparator1.setPreferredSize(new java.awt.Dimension(117, 6));
            this.jSeparator1.setLayout(null);
            this.jSeparator1.setBounds(1, 20, 136, 5);
        }
        return this.jSeparator1;
    }

    /**
     * <p>
     * Getter for the field <code>jButtonOk</code>.
     * </p>
     * 
     * @return a {@link javax.swing.JButton} object.
     */
    private JButton getJButtonOk() {
        if (this.jButtonOk == null) {
            this.jButtonOk = new JButton();
            this.jButtonOk.setLayout(null);
            this.jButtonOk.setText("OK");
            this.jButtonOk.setBounds(206, 536, 168, 31);
            this.jButtonOk.addMouseListener(new MouseInputAdapter() {
                @Override
                public void mouseClicked(final MouseEvent e) {
                    DeckAnalysis.this.jF.setEnabled(true);
                    DeckAnalysis.this.dispose();
                }
            });
        }
        return this.jButtonOk;
    }

    /**
     * <p>
     * Getter for the field <code>jLabel1</code>.
     * </p>
     * 
     * @return a {@link javax.swing.JLabel} object.
     */
    private JLabel getJLabel1() {
        if (this.jLabelBlack == null) {
            this.jLabelBlack = new JLabel();
            this.jLabelBlack.setText("Black:");
            this.jLabelBlack.setPreferredSize(new java.awt.Dimension(105, 12));
            this.jLabelBlack.setLayout(null);
            this.jLabelBlack.setBounds(10, 28, 127, 13);
        }
        return this.jLabelBlack;
    }

    /**
     * <p>
     * Getter for the field <code>jLabel3</code>.
     * </p>
     * 
     * @return a {@link javax.swing.JLabel} object.
     */
    private JLabel getJLabel3() {
        if (this.jLabelBlue == null) {
            this.jLabelBlue = new JLabel();
            this.jLabelBlue.setText("Blue:");
            this.jLabelBlue.setLayout(null);
            this.jLabelBlue.setBounds(10, 50, 127, 13);
        }
        return this.jLabelBlue;
    }

    /**
     * <p>
     * Getter for the field <code>jLabel4</code>.
     * </p>
     * 
     * @return a {@link javax.swing.JLabel} object.
     */
    private JLabel getJLabel4() {
        if (this.jLabelGreen == null) {
            this.jLabelGreen = new JLabel();
            this.jLabelGreen.setText("Green:");
            this.jLabelGreen.setLayout(null);
            this.jLabelGreen.setBounds(10, 72, 127, 13);
        }
        return this.jLabelGreen;
    }

    /**
     * <p>
     * getJLabel5.
     * </p>
     * 
     * @return a {@link javax.swing.JLabel} object.
     */
    private JLabel getJLabel5() {
        if (this.jLabelRed == null) {
            this.jLabelRed = new JLabel();
            this.jLabelRed.setText("Red:");
            this.jLabelRed.setLayout(null);
            this.jLabelRed.setBounds(10, 94, 127, 14);
        }
        return this.jLabelRed;
    }

    /**
     * <p>
     * getJLabel6.
     * </p>
     * 
     * @return a {@link javax.swing.JLabel} object.
     */
    private JLabel getJLabel6() {
        if (this.jLabelWhite == null) {
            this.jLabelWhite = new JLabel();
            this.jLabelWhite.setText("White:");
            this.jLabelWhite.setLayout(null);
            this.jLabelWhite.setBounds(10, 116, 127, 13);
        }
        return this.jLabelWhite;
    }

    /**
     * <p>
     * getJLabel7.
     * </p>
     * 
     * @return a {@link javax.swing.JLabel} object.
     */
    private JLabel getJLabel7() {
        if (this.jLabelMultiColor == null) {
            this.jLabelMultiColor = new JLabel();
            this.jLabelMultiColor.setText("Multicolor:");
            this.jLabelMultiColor.setLayout(null);
            this.jLabelMultiColor.setBounds(10, 138, 127, 12);
        }
        return this.jLabelMultiColor;
    }

    /**
     * <p>
     * getJLabel8.
     * </p>
     * 
     * @return a {@link javax.swing.JLabel} object.
     */
    private JLabel getJLabel8() {
        if (this.jLabelColorless == null) {
            this.jLabelColorless = new JLabel();
            this.jLabelColorless.setText("Colorless:");
            this.jLabelColorless.setLayout(null);
            this.jLabelColorless.setBounds(10, 160, 128, 11);
        }
        return this.jLabelColorless;
    }

    /**
     * <p>
     * getJLabel1x.
     * </p>
     * 
     * @return a {@link javax.swing.JLabel} object.
     */
    private JLabel getJLabel1x() {
        if (this.jLabelLand == null) {
            this.jLabelLand = new JLabel();
            this.jLabelLand.setText("Land: ");
            this.jLabelLand.setLayout(null);
            this.jLabelLand.setBounds(10, 182, 129, 10);
        }
        return this.jLabelLand;
    }

    /**
     * <p>
     * getJLabel1xx.
     * </p>
     * 
     * @return a {@link javax.swing.JLabel} object.
     */
    private JLabel getJLabel1xx() {
        if (this.jLabelTotal == null) {
            this.jLabelTotal = new JLabel();
            this.jLabelTotal.setText("Information about deck:");
            this.jLabelTotal.setLayout(null);
            this.jLabelTotal.setBounds(5, 0, 454, 35);
        }
        return this.jLabelTotal;
    }

    /**
     * <p>
     * Getter for the field <code>jPanel2</code>.
     * </p>
     * 
     * @return a {@link javax.swing.JPanel} object.
     */
    private JPanel getJPanel2() {
        if (this.jPanel2 == null) {
            this.jPanel2 = new JPanel();

            this.jPanel2.setBackground(new java.awt.Color(192, 192, 192));
            this.jPanel2.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
            this.jPanel2.setLayout(null);
            this.jPanel2.setBounds(153, 35, 137, 203);
            this.jPanel2.add(this.getJLabel1xxx());
            this.jPanel2.add(this.getJSeparator2());
            this.jPanel2.add(this.getJLabel3x());
            this.jPanel2.add(this.getJLabel4x());
            this.jPanel2.add(this.getJLabel5x());
            this.jPanel2.add(this.getJLabel6x());
            this.jPanel2.add(this.getJLabel7x());
            this.jPanel2.add(this.getJLabel8x());
            this.jPanel2.add(this.getJLabel10());
        }
        return this.jPanel2;
    }

    /**
     * <p>
     * getJLabel1xxx.
     * </p>
     * 
     * @return a {@link javax.swing.JLabel} object.
     */
    private JLabel getJLabel1xxx() {
        if (this.jLabelArtifact == null) {
            this.jLabelArtifact = new JLabel();
            this.jLabelArtifact.setText("Artifact:");
            this.jLabelArtifact.setPreferredSize(new java.awt.Dimension(105, 12));
            this.jLabelArtifact.setLayout(null);
            this.jLabelArtifact.setBounds(10, 28, 127, 13);
        }
        return this.jLabelArtifact;
    }

    /**
     * <p>
     * Getter for the field <code>jSeparator2</code>.
     * </p>
     * 
     * @return a {@link javax.swing.JSeparator} object.
     */
    private JSeparator getJSeparator2() {
        if (this.jSeparator2 == null) {
            this.jSeparator2 = new JSeparator();
            this.jSeparator2.setPreferredSize(new java.awt.Dimension(117, 6));
            this.jSeparator2.setLayout(null);
            this.jSeparator2.setBounds(1, 20, 136, 5);
        }
        return this.jSeparator2;
    }

    /**
     * <p>
     * getJLabel3x.
     * </p>
     * 
     * @return a {@link javax.swing.JLabel} object.
     */
    private JLabel getJLabel3x() {
        if (this.jLabel3 == null) {
            this.jLabel3 = new JLabel();
            this.jLabel3.setText("Type");
            this.jLabel3.setHorizontalAlignment(SwingConstants.CENTER);
            this.jLabel3.setFont(new java.awt.Font("Segoe UI", 0, 14));
            this.jLabel3.setPreferredSize(new java.awt.Dimension(152, 39));
            this.jLabel3.setLayout(null);
            this.jLabel3.setBounds(2, -3, 135, 26);
        }
        return this.jLabel3;
    }

    /**
     * <p>
     * getJLabel4x.
     * </p>
     * 
     * @return a {@link javax.swing.JLabel} object.
     */
    private JLabel getJLabel4x() {
        if (this.jLabelCreature == null) {
            this.jLabelCreature = new JLabel();
            this.jLabelCreature.setText("Creature:");
            this.jLabelCreature.setLayout(null);
            this.jLabelCreature.setBounds(10, 53, 127, 13);
        }
        return this.jLabelCreature;
    }

    /**
     * <p>
     * getJLabel5x.
     * </p>
     * 
     * @return a {@link javax.swing.JLabel} object.
     */
    private JLabel getJLabel5x() {
        if (this.jLabelEnchant == null) {
            this.jLabelEnchant = new JLabel();
            this.jLabelEnchant.setText("Enchant:");
            this.jLabelEnchant.setLayout(null);
            this.jLabelEnchant.setBounds(10, 79, 127, 13);
        }
        return this.jLabelEnchant;
    }

    /**
     * <p>
     * getJLabel6x.
     * </p>
     * 
     * @return a {@link javax.swing.JLabel} object.
     */
    private JLabel getJLabel6x() {
        if (this.jLabelInstant == null) {
            this.jLabelInstant = new JLabel();
            this.jLabelInstant.setText("Instant:");
            this.jLabelInstant.setLayout(null);
            this.jLabelInstant.setBounds(10, 105, 127, 14);
        }
        return this.jLabelInstant;
    }

    /**
     * <p>
     * getJLabel7x.
     * </p>
     * 
     * @return a {@link javax.swing.JLabel} object.
     */
    private JLabel getJLabel7x() {
        if (this.jLabelLandType == null) {
            this.jLabelLandType = new JLabel();
            this.jLabelLandType.setText("Land:");
            this.jLabelLandType.setLayout(null);
            this.jLabelLandType.setBounds(10, 130, 127, 13);
        }
        return this.jLabelLandType;
    }

    /**
     * <p>
     * getJLabel8x.
     * </p>
     * 
     * @return a {@link javax.swing.JLabel} object.
     */
    private JLabel getJLabel8x() {
        if (this.jLabelPlaneswalker == null) {
            this.jLabelPlaneswalker = new JLabel();
            this.jLabelPlaneswalker.setText("Planeswalker:");
            this.jLabelPlaneswalker.setLayout(null);
            this.jLabelPlaneswalker.setBounds(10, 156, 127, 13);
        }
        return this.jLabelPlaneswalker;
    }

    /**
     * <p>
     * getJLabel10.
     * </p>
     * 
     * @return a {@link javax.swing.JLabel} object.
     */
    private JLabel getJLabel10() {
        if (this.jLabelSorcery == null) {
            this.jLabelSorcery = new JLabel();
            this.jLabelSorcery.setText("Sorcery:");
            this.jLabelSorcery.setLayout(null);
            this.jLabelSorcery.setBounds(10, 182, 127, 11);
        }
        return this.jLabelSorcery;
    }

    /**
     * <p>
     * Getter for the field <code>jPanel3</code>.
     * </p>
     * 
     * @return a {@link javax.swing.JPanel} object.
     */
    private JPanel getJPanel3() {
        if (this.jPanel3 == null) {
            this.jPanel3 = new JPanel();
            this.jPanel3.setBackground(new java.awt.Color(192, 192, 192));
            this.jPanel3.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
            this.jPanel3.setLayout(null);
            this.jPanel3.setBounds(302, 35, 137, 203);
            this.jPanel3.add(this.getJLabel1xxxx());
            this.jPanel3.add(this.getJSeparator3());
            this.jPanel3.add(this.getJLabel4xx());
            this.jPanel3.add(this.getJLabel5xx());
            this.jPanel3.add(this.getJLabel6xx());
            this.jPanel3.add(this.getJLabel7xx());
            this.jPanel3.add(this.getJLabel8xx());
            this.jPanel3.add(this.getJLabel9());
            this.jPanel3.add(this.getJLabel10x());
        }
        return this.jPanel3;
    }

    /**
     * <p>
     * getJLabel1xxxx.
     * </p>
     * 
     * @return a {@link javax.swing.JLabel} object.
     */
    private JLabel getJLabel1xxxx() {
        if (this.jLabelZeroMana == null) {
            this.jLabelZeroMana = new JLabel();
            this.jLabelZeroMana.setText("Zero mana:");
            this.jLabelZeroMana.setPreferredSize(new java.awt.Dimension(105, 12));
            this.jLabelZeroMana.setLayout(null);
            this.jLabelZeroMana.setBounds(10, 28, 127, 13);
        }
        return this.jLabelZeroMana;
    }

    /**
     * <p>
     * Getter for the field <code>jSeparator3</code>.
     * </p>
     * 
     * @return a {@link javax.swing.JSeparator} object.
     */
    private JSeparator getJSeparator3() {
        if (this.jSeparator3 == null) {
            this.jSeparator3 = new JSeparator();
            this.jSeparator3.setPreferredSize(new java.awt.Dimension(117, 6));
            this.jSeparator3.setLayout(null);
            this.jSeparator3.setBounds(1, 20, 136, 5);
        }
        return this.jSeparator3;
    }

    /**
     * <p>
     * getJLabel4xx.
     * </p>
     * 
     * @return a {@link javax.swing.JLabel} object.
     */
    private JLabel getJLabel4xx() {
        if (this.jLabelManaCost == null) {
            this.jLabelManaCost = new JLabel();
            this.jLabelManaCost.setText("Mana cost");
            this.jLabelManaCost.setHorizontalAlignment(SwingConstants.CENTER);
            this.jLabelManaCost.setFont(new java.awt.Font("Segoe UI", 0, 14));
            this.jLabelManaCost.setPreferredSize(new java.awt.Dimension(152, 39));
            this.jLabelManaCost.setLayout(null);
            this.jLabelManaCost.setBounds(2, -3, 135, 26);
        }
        return this.jLabelManaCost;
    }

    /**
     * <p>
     * getJLabel5xx.
     * </p>
     * 
     * @return a {@link javax.swing.JLabel} object.
     */
    private JLabel getJLabel5xx() {
        if (this.jLabelOneMana == null) {
            this.jLabelOneMana = new JLabel();
            this.jLabelOneMana.setText("One mana:");
            this.jLabelOneMana.setLayout(null);
            this.jLabelOneMana.setBounds(10, 53, 127, 13);
        }
        return this.jLabelOneMana;
    }

    /**
     * <p>
     * getJLabel6xx.
     * </p>
     * 
     * @return a {@link javax.swing.JLabel} object.
     */
    private JLabel getJLabel6xx() {
        if (this.jLabelTwoMana == null) {
            this.jLabelTwoMana = new JLabel();
            this.jLabelTwoMana.setText("Two mana:");
            this.jLabelTwoMana.setLayout(null);
            this.jLabelTwoMana.setBounds(10, 79, 127, 13);
        }
        return this.jLabelTwoMana;
    }

    /**
     * <p>
     * getJLabel7xx.
     * </p>
     * 
     * @return a {@link javax.swing.JLabel} object.
     */
    private JLabel getJLabel7xx() {
        if (this.jLabelThreeMana == null) {
            this.jLabelThreeMana = new JLabel();
            this.jLabelThreeMana.setText("Three mana:");
            this.jLabelThreeMana.setLayout(null);
            this.jLabelThreeMana.setBounds(10, 105, 127, 14);
        }
        return this.jLabelThreeMana;
    }

    /**
     * <p>
     * getJLabel8xx.
     * </p>
     * 
     * @return a {@link javax.swing.JLabel} object.
     */
    private JLabel getJLabel8xx() {
        if (this.jLabelFourMana == null) {
            this.jLabelFourMana = new JLabel();
            this.jLabelFourMana.setText("Four mana:");
            this.jLabelFourMana.setLayout(null);
            this.jLabelFourMana.setBounds(10, 130, 127, 13);
        }
        return this.jLabelFourMana;
    }

    /**
     * <p>
     * getJLabel9.
     * </p>
     * 
     * @return a {@link javax.swing.JLabel} object.
     */
    private JLabel getJLabel9() {
        if (this.jLabelFiveMana == null) {
            this.jLabelFiveMana = new JLabel();
            this.jLabelFiveMana.setText("Five mana:");
            this.jLabelFiveMana.setLayout(null);
            this.jLabelFiveMana.setBounds(10, 156, 127, 13);
        }
        return this.jLabelFiveMana;
    }

    /**
     * <p>
     * getJLabel10x.
     * </p>
     * 
     * @return a {@link javax.swing.JLabel} object.
     */
    private JLabel getJLabel10x() {
        if (this.jLabelSixMana == null) {
            this.jLabelSixMana = new JLabel();
            this.jLabelSixMana.setText("Six and more:");
            this.jLabelSixMana.setLayout(null);
            this.jLabelSixMana.setBounds(10, 182, 127, 11);
        }
        return this.jLabelSixMana;
    }

    /**
     * <p>
     * getJList1.
     * </p>
     * 
     * @return a {@link javax.swing.JList} object.
     */
    private JList getJList1() {
        final List<CardPrinted> rList = this.deck.toFlatList();

        Collections.shuffle(rList, MyRandom.getRandom());
        Collections.shuffle(rList, MyRandom.getRandom());

        ListModel jList1Model;
        if (this.jListFirstHand == null) {
            this.jListFirstHand = new JList();
        }

        if (rList.size() >= 40) {
            jList1Model = new DefaultComboBoxModel(new String[] { rList.get(0).getName(), rList.get(1).getName(),
                    rList.get(2).getName(), rList.get(3).getName(), rList.get(4).getName(), rList.get(5).getName(),
                    rList.get(6).getName() });

        } else {
            jList1Model = new DefaultComboBoxModel(new String[] { "Few cards." });
        }

        this.jListFirstHand.setModel(jList1Model);
        this.jListFirstHand.setLayout(null);
        this.jListFirstHand.setBackground(new java.awt.Color(192, 192, 192));
        this.jListFirstHand.setSelectionBackground(new java.awt.Color(192, 192, 192));
        this.jListFirstHand.setSelectionForeground(new java.awt.Color(0, 0, 0));
        this.jListFirstHand.setFixedCellHeight(24);
        this.jListFirstHand.setBounds(2, 21, 133, 167);

        return this.jListFirstHand;
    }

    /**
     * <p>
     * Getter for the field <code>jPanel4</code>.
     * </p>
     * 
     * @return a {@link javax.swing.JPanel} object.
     */
    private JPanel getJPanel4() {
        if (this.jPanel4 == null) {
            this.jPanel4 = new JPanel();
            this.jPanel4.setBackground(new java.awt.Color(192, 192, 192));
            this.jPanel4.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
            this.jPanel4.setLayout(null);
            this.jPanel4.setBounds(451, 35, 137, 202);
            this.jPanel4.add(this.getJSeparator4());
            this.jPanel4.add(this.getJLabel4xxx());
            this.jPanel4.add(this.getJList1());
            this.jPanel4.add(this.getJButton1());
        } else {
            this.jPanel4.removeAll();
            final MigLayout jPanel4Layout = new MigLayout();
            this.jPanel4.setBackground(new java.awt.Color(192, 192, 192));
            this.jPanel4.setPreferredSize(new java.awt.Dimension(139, 201));
            this.jPanel4.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
            this.jPanel4.setLayout(jPanel4Layout);
            this.jPanel4.add(this.getJSeparator4());
            this.jPanel4.add(this.getJLabel4xxx());
            this.jPanel4.add(this.getJList1());
            this.jPanel4.add(this.getJButton1());
        }
        return this.jPanel4;

    }

    /**
     * <p>
     * Getter for the field <code>jSeparator4</code>.
     * </p>
     * 
     * @return a {@link javax.swing.JSeparator} object.
     */
    private JSeparator getJSeparator4() {
        if (this.jSeparator4 == null) {
            this.jSeparator4 = new JSeparator();
            this.jSeparator4.setPreferredSize(new java.awt.Dimension(138, 8));
            this.jSeparator4.setLayout(null);
            this.jSeparator4.setBounds(0, 19, 137, 7);
        }
        return this.jSeparator4;
    }

    /**
     * <p>
     * getJLabel4xxx.
     * </p>
     * 
     * @return a {@link javax.swing.JLabel} object.
     */
    private JLabel getJLabel4xxx() {
        if (this.jLabel4 == null) {
            this.jLabel4 = new JLabel();
            this.jLabel4.setText("Random start hand");
            this.jLabel4.setHorizontalAlignment(SwingConstants.CENTER);
            this.jLabel4.setFont(new java.awt.Font("Segoe UI", 0, 14));
            this.jLabel4.setPreferredSize(new java.awt.Dimension(136, 24));
            this.jLabel4.setLayout(null);
            this.jLabel4.setBounds(2, 0, 135, 20);
        }
        return this.jLabel4;
    }

    /**
     * <p>
     * getJButton1.
     * </p>
     * 
     * @return a {@link javax.swing.JButton} object.
     */
    private JButton getJButton1() {

        if (this.jButtonRegenerate == null) {
            if (this.deck.countAll() >= 40) {
                this.jButtonRegenerate = new JButton();
                this.jButtonRegenerate.setLayout(null);
                this.jButtonRegenerate.setText("Regenerate hand");
                this.jButtonRegenerate.setPreferredSize(new java.awt.Dimension(139, 21));
                this.jButtonRegenerate.setBounds(2, 189, 133, 13);
                this.jButtonRegenerate.addActionListener(new java.awt.event.ActionListener() {
                    @Override
                    public void actionPerformed(final ActionEvent e) {
                        DeckAnalysis.this.jButtonRegenerateActionPerformed(e);
                    }
                });
            } else {
                this.jButtonRegenerate = new JButton();
                this.jButtonRegenerate.setBounds(2, 189, 133, 13);
                this.jButtonRegenerate.setVisible(false);
            }
        }
        return this.jButtonRegenerate;
    }

    /**
     * <p>
     * jButtonRegenerate_actionPerformed.
     * </p>
     * 
     * @param e
     *            a {@link java.awt.event.ActionEvent} object.
     */
    final void jButtonRegenerateActionPerformed(final ActionEvent e) {
        this.getContentPane().removeAll();
        this.getContentPane().add(this.getJPanel5());
        this.getContentPane().add(this.getJLabel1xx());
        this.getContentPane().add(this.getJButtonOk());
        this.getContentPane().add(this.getJPanel1());
        this.getContentPane().add(this.getJPanel2());
        this.getContentPane().add(this.getJPanel3());
        this.getContentPane().add(this.getJPanel4());
        this.getContentPane().add(this.getJPanel5());
        this.getContentPane().add(this.getJLabel1xxxxx());
        this.getContentPane().repaint();

    }

    /**
     * <p>
     * Getter for the field <code>jPanel5</code>.
     * </p>
     * 
     * @return a {@link javax.swing.JPanel} object.
     */
    private JPanel getJPanel5() {
        if (this.jPanel5 == null) {
            this.jPanel5 = new JPanel();
            this.jPanel5.setLayout(null);
            this.jPanel5.setBounds(5, 262, 583, 270);
            this.jPanel5.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
            this.jPanel5.add(this.getJScrollPane1());
        }
        return this.jPanel5;
    }

    /**
     * <p>
     * Getter for the field <code>jTable1</code>.
     * </p>
     * 
     * @return a {@link javax.swing.JTable} object.
     */
    private JTable getJTable1() {
        if (this.jTable1 == null) {
            final DefaultTableModel dm = new DefaultTableModel();
            dm.setDataVector(new Object[][] { {} }, new Object[] { "Card", "Qty", "1st", "2nd", "3rd", "4th", "5th",
                    "6th", "7th" });

            this.jTable1 = new JTable(dm);
            final List<CardPrinted> rList = this.deck.toFlatList();
            final String[] cardsName = new String[rList.size()];
            int cCount;
            float fCount;
            float firstTurnF, secondTurnF, thirdTurnF, fourthTurnF, fivethTurnF, sixthTurnF, seventhTurnF;

            for (int i = 0; i < rList.size(); i++) {
                cardsName[i] = rList.get(i).getName();
            }
            Arrays.sort(cardsName);
            this.jTable1.setValueAt("Few cards.", 0, 0);

            if (rList.size() >= 40) {
                this.jTable1.setValueAt(cardsName[0], 0, 0);
                cCount = 1;
                for (int i = 1; i < cardsName.length; i++) {
                    if (cardsName[i].equals(cardsName[i - 1])) {
                        cCount = cCount + 1;

                    } else {
                        dm.addRow(new Object[][] { {} });
                        this.jTable1.setValueAt(cardsName[i], dm.getRowCount() - 1, 0);
                        this.jTable1.setValueAt(cCount, dm.getRowCount() - 2, 1);
                        fCount = cCount;

                        firstTurnF = fCount / rList.size();
                        BigDecimal firstTurn = new BigDecimal(firstTurnF * 100);
                        firstTurn = firstTurn.setScale(1, BigDecimal.ROUND_HALF_UP);
                        this.jTable1.setValueAt(firstTurn.toString() + " %", dm.getRowCount() - 2, 2);

                        secondTurnF = (((1 - firstTurnF) * fCount) / (rList.size() - 1)) + firstTurnF;
                        BigDecimal secondTurn = new BigDecimal(secondTurnF * 100);
                        secondTurn = secondTurn.setScale(1, BigDecimal.ROUND_HALF_UP);
                        this.jTable1.setValueAt(secondTurn.toString() + " %", dm.getRowCount() - 2, 3);

                        thirdTurnF = (((1 - secondTurnF) * fCount) / (rList.size() - 2)) + secondTurnF;
                        BigDecimal thirdTurn = new BigDecimal(thirdTurnF * 100);
                        thirdTurn = thirdTurn.setScale(1, BigDecimal.ROUND_HALF_UP);
                        this.jTable1.setValueAt(thirdTurn.toString() + " %", dm.getRowCount() - 2, 4);

                        fourthTurnF = (((1 - thirdTurnF) * fCount) / (rList.size() - 3)) + thirdTurnF;
                        BigDecimal fourthTurn = new BigDecimal(fourthTurnF * 100);
                        fourthTurn = fourthTurn.setScale(1, BigDecimal.ROUND_HALF_UP);
                        this.jTable1.setValueAt(fourthTurn.toString() + " %", dm.getRowCount() - 2, 5);

                        fivethTurnF = (((1 - fourthTurnF) * fCount) / (rList.size() - 4)) + fourthTurnF;
                        BigDecimal fivethTurn = new BigDecimal(fivethTurnF * 100);
                        fivethTurn = fivethTurn.setScale(1, BigDecimal.ROUND_HALF_UP);
                        this.jTable1.setValueAt(fivethTurn.toString() + " %", dm.getRowCount() - 2, 6);

                        sixthTurnF = (((1 - fivethTurnF) * fCount) / (rList.size() - 5)) + fivethTurnF;
                        BigDecimal sixthTurn = new BigDecimal(sixthTurnF * 100);
                        sixthTurn = sixthTurn.setScale(1, BigDecimal.ROUND_HALF_UP);
                        this.jTable1.setValueAt(sixthTurn.toString() + " %", dm.getRowCount() - 2, 7);

                        seventhTurnF = (((1 - sixthTurnF) * fCount) / (rList.size() - 6)) + sixthTurnF;
                        BigDecimal seventhTurn = new BigDecimal(seventhTurnF * 100);
                        seventhTurn = seventhTurn.setScale(1, BigDecimal.ROUND_HALF_UP);
                        this.jTable1.setValueAt(seventhTurn.toString() + " %", dm.getRowCount() - 2, 8);

                        cCount = 1;
                    }
                    if (i == (cardsName.length - 1)) {
                        this.jTable1.setValueAt(cCount, dm.getRowCount() - 1, 1);
                        fCount = cCount;

                        firstTurnF = fCount / rList.size();
                        BigDecimal firstTurn = new BigDecimal(firstTurnF * 100);
                        firstTurn = firstTurn.setScale(1, BigDecimal.ROUND_HALF_UP);
                        this.jTable1.setValueAt(firstTurn.toString() + " %", dm.getRowCount() - 1, 2);

                        secondTurnF = (((1 - firstTurnF) * fCount) / (rList.size() - 1)) + firstTurnF;
                        BigDecimal secondTurn = new BigDecimal(secondTurnF * 100);
                        secondTurn = secondTurn.setScale(1, BigDecimal.ROUND_HALF_UP);
                        this.jTable1.setValueAt(secondTurn.toString() + " %", dm.getRowCount() - 1, 3);

                        thirdTurnF = (((1 - secondTurnF) * fCount) / (rList.size() - 2)) + secondTurnF;
                        BigDecimal thirdTurn = new BigDecimal(thirdTurnF * 100);
                        thirdTurn = thirdTurn.setScale(1, BigDecimal.ROUND_HALF_UP);
                        this.jTable1.setValueAt(thirdTurn.toString() + " %", dm.getRowCount() - 1, 4);

                        fourthTurnF = (((1 - thirdTurnF) * fCount) / (rList.size() - 3)) + thirdTurnF;
                        BigDecimal fourthTurn = new BigDecimal(fourthTurnF * 100);
                        fourthTurn = fourthTurn.setScale(1, BigDecimal.ROUND_HALF_UP);
                        this.jTable1.setValueAt(fourthTurn.toString() + " %", dm.getRowCount() - 1, 5);

                        fivethTurnF = (((1 - fourthTurnF) * fCount) / (rList.size() - 4)) + fourthTurnF;
                        BigDecimal fivethTurn = new BigDecimal(fivethTurnF * 100);
                        fivethTurn = fivethTurn.setScale(1, BigDecimal.ROUND_HALF_UP);
                        this.jTable1.setValueAt(fivethTurn.toString() + " %", dm.getRowCount() - 1, 6);

                        sixthTurnF = (((1 - fivethTurnF) * fCount) / (rList.size() - 5)) + fivethTurnF;
                        BigDecimal sixthTurn = new BigDecimal(sixthTurnF * 100);
                        sixthTurn = sixthTurn.setScale(1, BigDecimal.ROUND_HALF_UP);
                        this.jTable1.setValueAt(sixthTurn.toString() + " %", dm.getRowCount() - 1, 7);

                        seventhTurnF = (((1 - sixthTurnF) * fCount) / (rList.size() - 6)) + sixthTurnF;
                        BigDecimal seventhTurn = new BigDecimal(seventhTurnF * 100);
                        seventhTurn = seventhTurn.setScale(1, BigDecimal.ROUND_HALF_UP);
                        this.jTable1.setValueAt(seventhTurn.toString() + " %", dm.getRowCount() - 1, 8);

                    }

                }
            }

            this.jTable1.getColumn("Qty").setMaxWidth(50);
            this.jTable1.getColumn("1st").setMaxWidth(50);
            this.jTable1.getColumn("2nd").setMaxWidth(50);
            this.jTable1.getColumn("3rd").setMaxWidth(50);
            this.jTable1.getColumn("4th").setMaxWidth(50);
            this.jTable1.getColumn("5th").setMaxWidth(50);
            this.jTable1.getColumn("6th").setMaxWidth(50);
            this.jTable1.getColumn("7th").setMaxWidth(50);
            this.jTable1.setRowHeight(18);
            this.jTable1.setPreferredSize(new java.awt.Dimension(576, (18 * dm.getRowCount()) + 3));
        }
        return this.jTable1;
    }

    /**
     * <p>
     * Getter for the field <code>jScrollPane1</code>.
     * </p>
     * 
     * @return a {@link javax.swing.JScrollPane} object.
     */
    private JScrollPane getJScrollPane1() {
        if (this.jScrollPane1 == null) {
            this.jScrollPane1 = new JScrollPane();
            this.jScrollPane1.setBounds(2, 2, 582, 268);
            this.jScrollPane1.setSize(580, 268);
            this.jScrollPane1.setViewportView(this.getJTable1());
        }
        return this.jScrollPane1;
    }

    /**
     * <p>
     * getJLabel1xxxxx.
     * </p>
     * 
     * @return a {@link javax.swing.JLabel} object.
     */
    private JLabel getJLabel1xxxxx() {
        if (this.jLabel1 == null) {
            this.jLabel1 = new JLabel();
            this.jLabel1.setText("Draw Probabilities:");
            this.jLabel1.setLayout(null);
            this.jLabel1.setBounds(7, 237, 447, 25);
        }
        return this.jLabel1;
    }

}
