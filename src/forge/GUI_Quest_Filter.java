package forge;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.event.WindowAdapter;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.BevelBorder;
import javax.swing.event.MouseInputAdapter;

import net.miginfocom.swing.MigLayout;

/**
 * @author Forge
 * @version $Id: $
 */
public class GUI_Quest_Filter extends JDialog {

    /**
     * Constant <code>serialVersionUID=-8475271235196182185L</code>
     */
    private static final long serialVersionUID = -8475271235196182185L;
    private JLabel nameLabel;
    private JTextField nameTextField;
    private JLabel cardTextLabel;
    private JTextField cardTextField;
    private JPanel colorPanel;
    private JPanel bottomPanel;
    private JCheckBox jCheckBoxColorless;
    private JCheckBox jCheckBoxWhite;
    private JCheckBox jCheckBoxRed;
    private JCheckBox jCheckBoxGreen;
    private JCheckBox jCheckBoxBlue;
    private JLabel colorLabel;
    private JLabel typeLabel;
    private JCheckBox jCheckBoxPlaneswalker;
    private JCheckBox jCheckBoxArtifact;
    private JCheckBox jCheckBoxCreature;
    private JCheckBox jCheckBoxEnchant;
    private JCheckBox jCheckBoxInstant;
    private JCheckBox jCheckBoxLand;
    private JCheckBox jCheckBoxSorcery;
    private JPanel typePanel;
    private JCheckBox jCheckBoxBlack;
    private JButton jButtonOk;
    private JPanel topPanel;
    private DeckDisplay deckDisplay;

    /**
     * <p>
     * Constructor for GUI_Quest_Filter.
     * </p>
     * 
     * @param g
     *            a {@link javax.swing.JFrame} object.
     * @param display
     *            a {@link forge.DeckDisplay} object.
     */
    public GUI_Quest_Filter(JFrame g, DeckDisplay display) {
        super(g);
        deckDisplay = display;
        initGUI();
    }

    /**
     * <p>
     * initGUI.
     * </p>
     */
    private void initGUI() {
        try {
            this.isResizable();
            getContentPane().setLayout(new MigLayout("fill"));
            getContentPane().add(getTopPanel(), "span 3, wrap");
            getContentPane().add(getColorPanel(), "aligny top, growy");
            getContentPane().add(getTypePanel(), "aligny top, wrap");
            getContentPane().add(getBottomPanel(), "align center, span 3");
            setVisible(true);
            Dimension screen = getToolkit().getScreenSize();
            int x = (screen.width - 340) / 2;
            int y = (screen.height - 500) / 2;
            this.setBounds(x, y, 340, 500);
            this.setResizable(true);
            this.setTitle("Filter");

            setIconImage(null);

            this.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent arg0) {
                    Gui_DeckEditor g = (Gui_DeckEditor) deckDisplay;
                    g.setEnabled(true);
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private JTextField getNameTextField() {
        if (nameTextField == null) {
            nameTextField = new JTextField(30);
            nameTextField.addKeyListener(new java.awt.event.KeyAdapter() {
                @Override
                public void keyPressed(java.awt.event.KeyEvent e) {

                    if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                        FilterCardTable();
                    }
                }
            });
        }
        return nameTextField;
    }

    private JLabel getNameLabel() {
        if (nameLabel == null) {
            nameLabel = new JLabel();
            nameLabel.setText("Name:");
            nameLabel.setFont(new Font("Segoe UI", 0, 16));
        }
        return nameLabel;
    }

    private JPanel getTopPanel() {
        if (topPanel == null) {
            topPanel = new JPanel();
            topPanel.setLayout(new MigLayout());
            topPanel.add(getNameLabel(), "gap");
            topPanel.add(getNameTextField(), "span 3, wrap");
            topPanel.add(getCardTextLabel(), "gap");
            topPanel.add(getCardTextField(), "span 3, wrap");
        }

        return topPanel;
    }

    /**
     * <p>
     * Getter for the field <code>colorPanel</code>.
     * </p>
     * 
     * @return a {@link javax.swing.JPanel} object.
     */
    private JPanel getColorPanel() {
        if (colorPanel == null) {
            colorPanel = new JPanel();
            colorPanel.setLayout(new MigLayout());
            colorPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
            colorPanel.setBackground(new java.awt.Color(192, 192, 192));
            colorPanel.add(getColorLabel(), "align, wrap");
            colorPanel.add(getJCheckBoxBlack(), "wrap");
            colorPanel.add(getJCheckBoxColorless(), "wrap");
            colorPanel.add(getJCheckBoxWhite(), "wrap");
            colorPanel.add(getJCheckBoxRed(), "wrap");
            colorPanel.add(getJCheckBoxGreen(), "wrap");
            colorPanel.add(getJCheckBoxBlue(), "wrap");
        }
        return colorPanel;
    }

    private JPanel getBottomPanel() {
        if (bottomPanel == null) {
            bottomPanel = new JPanel();
            bottomPanel.setLayout(new MigLayout());
            bottomPanel.add(getJButtonOk(), "align, span 3, grow");
        }
        return bottomPanel;
    }

    /**
     * <p>
     * Getter for the field <code>jCheckBoxBlue</code>.
     * </p>
     * 
     * @return a {@link javax.swing.JCheckBox} object.
     */
    private JCheckBox getJCheckBoxBlue() {
        if (jCheckBoxBlue == null) {
            jCheckBoxBlue = new JCheckBox();
            jCheckBoxBlue.setText("Blue");
            jCheckBoxBlue.setSelected(true);
            jCheckBoxBlue.setBackground(new java.awt.Color(192, 192, 192));
            jCheckBoxBlue.addKeyListener(new java.awt.event.KeyAdapter() {
                @Override
                public void keyPressed(java.awt.event.KeyEvent e) {

                    if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                        FilterCardTable();
                    }
                }
            });
        }
        return jCheckBoxBlue;
    }

    /**
     * <p>
     * Getter for the field <code>jCheckBoxGreen</code>.
     * </p>
     * 
     * @return a {@link javax.swing.JCheckBox} object.
     */
    private JCheckBox getJCheckBoxGreen() {
        if (jCheckBoxGreen == null) {
            jCheckBoxGreen = new JCheckBox();
            jCheckBoxGreen.setText("Green");
            jCheckBoxGreen.setSelected(true);
            jCheckBoxGreen.setBackground(new java.awt.Color(192, 192, 192));
            jCheckBoxGreen.addKeyListener(new java.awt.event.KeyAdapter() {
                @Override
                public void keyPressed(java.awt.event.KeyEvent e) {

                    if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                        FilterCardTable();
                    }
                }
            });
        }
        return jCheckBoxGreen;
    }

    /**
     * <p>
     * Getter for the field <code>jCheckBoxRed</code>.
     * </p>
     * 
     * @return a {@link javax.swing.JCheckBox} object.
     */
    private JCheckBox getJCheckBoxRed() {
        if (jCheckBoxRed == null) {
            jCheckBoxRed = new JCheckBox();
            jCheckBoxRed.setText("Red");
            jCheckBoxRed.setSelected(true);
            jCheckBoxRed.setBackground(new java.awt.Color(192, 192, 192));
            jCheckBoxRed.addKeyListener(new java.awt.event.KeyAdapter() {
                @Override
                public void keyPressed(java.awt.event.KeyEvent e) {

                    if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                        FilterCardTable();
                    }
                }
            });
        }
        return jCheckBoxRed;
    }

    /**
     * <p>
     * Getter for the field <code>jCheckBoxWhite</code>.
     * </p>
     * 
     * @return a {@link javax.swing.JCheckBox} object.
     */
    private JCheckBox getJCheckBoxWhite() {
        if (jCheckBoxWhite == null) {
            jCheckBoxWhite = new JCheckBox();
            jCheckBoxWhite.setText("White");
            jCheckBoxWhite.setSelected(true);
            jCheckBoxWhite.setBackground(new java.awt.Color(192, 192, 192));
            jCheckBoxWhite.addKeyListener(new java.awt.event.KeyAdapter() {
                @Override
                public void keyPressed(java.awt.event.KeyEvent e) {

                    if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                        FilterCardTable();
                    }
                }
            });
        }
        return jCheckBoxWhite;
    }

    /**
     * <p>
     * Getter for the field <code>jCheckBoxColorless</code>.
     * </p>
     * 
     * @return a {@link javax.swing.JCheckBox} object.
     */
    private JCheckBox getJCheckBoxColorless() {
        if (jCheckBoxColorless == null) {
            jCheckBoxColorless = new JCheckBox();
            jCheckBoxColorless.setText("Colorless");
            jCheckBoxColorless.setSelected(true);
            jCheckBoxColorless.setBackground(new java.awt.Color(192, 192, 192));
            jCheckBoxColorless.addKeyListener(new java.awt.event.KeyAdapter() {
                @Override
                public void keyPressed(java.awt.event.KeyEvent e) {

                    if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                        FilterCardTable();
                    }
                }
            });
        }
        return jCheckBoxColorless;
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
            jButtonOk.setText("OK");
            jButtonOk.addMouseListener(new MouseInputAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    FilterCardTable();
                }
            });
        }
        return jButtonOk;
    }

    /**
     * <p>
     * Getter for the field <code>jCheckBoxBlack</code>.
     * </p>
     * 
     * @return a {@link javax.swing.JCheckBox} object.
     */
    private JCheckBox getJCheckBoxBlack() {
        if (jCheckBoxBlack == null) {
            jCheckBoxBlack = new JCheckBox();
            jCheckBoxBlack.setText("Black");
            jCheckBoxBlack.setBackground(new java.awt.Color(192, 192, 192));
            jCheckBoxBlack.setSelected(true);
            jCheckBoxBlack.addKeyListener(new java.awt.event.KeyAdapter() {
                @Override
                public void keyPressed(java.awt.event.KeyEvent e) {

                    if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                        FilterCardTable();
                    }
                }
            });
        }
        return jCheckBoxBlack;
    }

    /**
     * <p>
     * Getter for the field <code>typePanel</code>.
     * </p>
     * 
     * @return a {@link javax.swing.JPanel} object.
     */
    private JPanel getTypePanel() {
        if (typePanel == null) {
            typePanel = new JPanel();
            typePanel.setLayout(new MigLayout());
            typePanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
            typePanel.setBackground(new java.awt.Color(192, 192, 192));
            typePanel.add(getTypeLabel(), "align, wrap");
            typePanel.add(getJCheckBoxSorcery(), "wrap");
            typePanel.add(getJCheckBoxPlaneswalker(), "wrap");
            typePanel.add(getJCheckBoxLand(), "wrap");
            typePanel.add(getJCheckBoxInstant(), "wrap");
            typePanel.add(getJCheckBoxEnchant(), "wrap");
            typePanel.add(getJCheckBoxCreature(), "wrap");
            typePanel.add(getJCheckBoxArtifact(), "wrap");
        }
        return typePanel;
    }

    /**
     * <p>
     * Getter for the field <code>colorLabel</code>.
     * </p>
     * 
     * @return a {@link javax.swing.JLabel} object.
     */
    private JLabel getColorLabel() {
        if (colorLabel == null) {
            colorLabel = new JLabel();
            colorLabel.setText("Color");
            colorLabel.setFont(new java.awt.Font("Segoe UI", 0, 14));
        }
        return colorLabel;
    }

    /**
     * <p>
     * Getter for the field <code>typeLabel</code>.
     * </p>
     * 
     * @return a {@link javax.swing.JLabel} object.
     */
    private JLabel getTypeLabel() {
        if (typeLabel == null) {
            typeLabel = new JLabel();
            typeLabel.setText("Type");
            typeLabel.setFont(new java.awt.Font("Segoe UI", 0, 14));
        }
        return typeLabel;
    }

    /**
     * <p>
     * getJCheckBoxSorcery.
     * </p>
     * 
     * @return a {@link javax.swing.JCheckBox} object.
     */
    private JCheckBox getJCheckBoxSorcery() {
        if (jCheckBoxSorcery == null) {
            jCheckBoxSorcery = new JCheckBox();
            jCheckBoxSorcery.setText("Sorcery");
            jCheckBoxSorcery.setSelected(true);
            jCheckBoxSorcery.setBackground(new java.awt.Color(192, 192, 192));
            jCheckBoxSorcery.addKeyListener(new java.awt.event.KeyAdapter() {
                @Override
                public void keyPressed(java.awt.event.KeyEvent e) {

                    if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                        FilterCardTable();
                    }
                }
            });
        }
        return jCheckBoxSorcery;
    }

    /**
     * <p>
     * getJCheckBoxPlaneswalker.
     * </p>
     * 
     * @return a {@link javax.swing.JCheckBox} object.
     */
    private JCheckBox getJCheckBoxPlaneswalker() {
        if (jCheckBoxPlaneswalker == null) {
            jCheckBoxPlaneswalker = new JCheckBox();
            jCheckBoxPlaneswalker.setText("Planeswalker");
            jCheckBoxPlaneswalker.setSelected(true);
            jCheckBoxPlaneswalker.setBackground(new java.awt.Color(192, 192, 192));
            jCheckBoxPlaneswalker.addKeyListener(new java.awt.event.KeyAdapter() {
                @Override
                public void keyPressed(java.awt.event.KeyEvent e) {

                    if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                        FilterCardTable();
                    }
                }
            });
        }
        return jCheckBoxPlaneswalker;
    }

    /**
     * <p>
     * getJCheckBoxLand.
     * </p>
     * 
     * @return a {@link javax.swing.JCheckBox} object.
     */
    private JCheckBox getJCheckBoxLand() {
        if (jCheckBoxLand == null) {
            jCheckBoxLand = new JCheckBox();
            jCheckBoxLand.setText("Land");
            jCheckBoxLand.setSelected(true);
            jCheckBoxLand.setBackground(new java.awt.Color(192, 192, 192));
            jCheckBoxLand.addKeyListener(new java.awt.event.KeyAdapter() {
                @Override
                public void keyPressed(java.awt.event.KeyEvent e) {

                    if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                        FilterCardTable();
                    }
                }
            });
        }
        return jCheckBoxLand;
    }

    /**
     * <p>
     * getJCheckBoxInstant.
     * </p>
     * 
     * @return a {@link javax.swing.JCheckBox} object.
     */
    private JCheckBox getJCheckBoxInstant() {
        if (jCheckBoxInstant == null) {
            jCheckBoxInstant = new JCheckBox();
            jCheckBoxInstant.setText("Instant");
            jCheckBoxInstant.setSelected(true);
            jCheckBoxInstant.setBackground(new java.awt.Color(192, 192, 192));
            jCheckBoxInstant.addKeyListener(new java.awt.event.KeyAdapter() {
                @Override
                public void keyPressed(java.awt.event.KeyEvent e) {

                    if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                        FilterCardTable();
                    }
                }
            });
        }
        return jCheckBoxInstant;
    }

    /**
     * <p>
     * getJCheckBoxEnchant.
     * </p>
     * 
     * @return a {@link javax.swing.JCheckBox} object.
     */
    private JCheckBox getJCheckBoxEnchant() {
        if (jCheckBoxEnchant == null) {
            jCheckBoxEnchant = new JCheckBox();
            jCheckBoxEnchant.setText("Enchant");
            jCheckBoxEnchant.setSelected(true);
            jCheckBoxEnchant.setBackground(new java.awt.Color(192, 192, 192));
            jCheckBoxEnchant.addKeyListener(new java.awt.event.KeyAdapter() {
                @Override
                public void keyPressed(java.awt.event.KeyEvent e) {

                    if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                        FilterCardTable();
                    }
                }
            });
        }
        return jCheckBoxEnchant;
    }

    /**
     * <p>
     * getJCheckBoxCreature.
     * </p>
     * 
     * @return a {@link javax.swing.JCheckBox} object.
     */
    private JCheckBox getJCheckBoxCreature() {
        if (jCheckBoxCreature == null) {
            jCheckBoxCreature = new JCheckBox();
            jCheckBoxCreature.setText("Creature");
            jCheckBoxCreature.setSelected(true);
            jCheckBoxCreature.setBackground(new java.awt.Color(192, 192, 192));
            jCheckBoxCreature.addKeyListener(new java.awt.event.KeyAdapter() {
                @Override
                public void keyPressed(java.awt.event.KeyEvent e) {

                    if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                        FilterCardTable();
                    }
                }
            });
        }
        return jCheckBoxCreature;
    }

    /**
     * <p>
     * getJCheckBoxArtifact.
     * </p>
     * 
     * @return a {@link javax.swing.JCheckBox} object.
     */
    private JCheckBox getJCheckBoxArtifact() {
        if (jCheckBoxArtifact == null) {
            jCheckBoxArtifact = new JCheckBox();
            jCheckBoxArtifact.setText("Artifact");
            jCheckBoxArtifact.setSelected(true);
            jCheckBoxArtifact.setBackground(new java.awt.Color(192, 192, 192));
            jCheckBoxArtifact.addKeyListener(new java.awt.event.KeyAdapter() {
                @Override
                public void keyPressed(java.awt.event.KeyEvent e) {

                    if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                        FilterCardTable();
                    }
                }
            });
        }
        return jCheckBoxArtifact;
    }

    /**
     * <p>
     * getCardTextField.
     * </p>
     * 
     * @return a {@link javax.swing.JTextField} object.
     */
    private JTextField getCardTextField() {
        if (cardTextField == null) {
            cardTextField = new JTextField(30);
            cardTextField.addKeyListener(new java.awt.event.KeyAdapter() {
                @Override
                public void keyPressed(java.awt.event.KeyEvent e) {

                    if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                        FilterCardTable();
                    }
                }
            });
        }
        return cardTextField;
    }

    /**
     * <p>
     * Getter for the field <code>cardTextLabel</code>.
     * </p>
     * 
     * @return a {@link javax.swing.JLabel} object.
     */
    private JLabel getCardTextLabel() {
        if (cardTextLabel == null) {
            cardTextLabel = new JLabel();
            cardTextLabel.setText("Card Text:");
            cardTextLabel.setFont(new java.awt.Font("Segoe UI", 0, 16));
        }
        return cardTextLabel;
    }

    /**
     * <p>
     * FilterCardTable.
     * </p>
     */
    private void FilterCardTable() {
        String name = nameTextField.getText();
        String cText = cardTextField.getText();
        Gui_Quest_DeckEditor g = (Gui_Quest_DeckEditor) deckDisplay;
        CardFilter filter = new CardFilter();
        g.setEnabled(true);
        CardList filterCardList = g.stCardList;
        if (name != "") {
            if (cText != "") {
                filterCardList = filter.cardListNameFilter(filterCardList, name);
                if (filterCardList.size() == 0) {
                    JOptionPane.showMessageDialog(null, "Sorry, cards with name: " + name + " not found.", "Filter",
                            JOptionPane.INFORMATION_MESSAGE);
                    g.filterUsed = false;
                    deckDisplay.updateDisplay(g.stCardList, deckDisplay.getBottom());
                } else {
                    filterCardList = filter.CardListTextFilter(filterCardList, cText);
                    if (filterCardList.size() == 0) {
                        JOptionPane.showMessageDialog(null, "Sorry, cards with text: " + cText + " not found.",
                                "Filter", JOptionPane.INFORMATION_MESSAGE);
                        g.filterUsed = false;
                        deckDisplay.updateDisplay(g.stCardList, deckDisplay.getBottom());
                    } else {
                        if (jCheckBoxBlack.isSelected() == false) {
                            filterCardList = filter.CardListColorFilter(filterCardList, "black");
                            g.blackCheckBox.setSelected(false);
                            g.blackCheckBox.setEnabled(false);
                        }
                        if (jCheckBoxBlue.isSelected() == false) {
                            filterCardList = filter.CardListColorFilter(filterCardList, "blue");
                            g.blueCheckBox.setSelected(false);
                            g.blueCheckBox.setEnabled(false);
                        }
                        if (jCheckBoxGreen.isSelected() == false) {
                            filterCardList = filter.CardListColorFilter(filterCardList, "green");
                            g.greenCheckBox.setSelected(false);
                            g.greenCheckBox.setEnabled(false);
                        }
                        if (jCheckBoxRed.isSelected() == false) {
                            filterCardList = filter.CardListColorFilter(filterCardList, "red");
                            g.redCheckBox.setSelected(false);
                            g.redCheckBox.setEnabled(false);
                        }
                        if (jCheckBoxWhite.isSelected() == false) {
                            filterCardList = filter.CardListColorFilter(filterCardList, "white");
                            g.whiteCheckBox.setSelected(false);
                            g.whiteCheckBox.setEnabled(false);
                        }
                        if (jCheckBoxColorless.isSelected() == false) {
                            filterCardList = filter.CardListColorFilter(filterCardList, "colorless");
                            g.colorlessCheckBox.setSelected(false);
                            g.colorlessCheckBox.setEnabled(false);
                        }
                        if (jCheckBoxArtifact.isSelected() == false) {
                            filterCardList = filter.CardListTypeFilter(filterCardList, "artifact");
                            g.artifactCheckBox.setSelected(false);
                            g.artifactCheckBox.setEnabled(false);
                        }
                        if (jCheckBoxCreature.isSelected() == false) {
                            filterCardList = filter.CardListTypeFilter(filterCardList, "creature");
                            g.creatureCheckBox.setSelected(false);
                            g.creatureCheckBox.setEnabled(false);
                        }
                        if (jCheckBoxEnchant.isSelected() == false) {
                            filterCardList = filter.CardListTypeFilter(filterCardList, "enchantment");
                            g.enchantmentCheckBox.setSelected(false);
                            g.enchantmentCheckBox.setEnabled(false);
                        }
                        if (jCheckBoxInstant.isSelected() == false) {
                            filterCardList = filter.CardListTypeFilter(filterCardList, "instant");
                            g.instantCheckBox.setSelected(false);
                            g.instantCheckBox.setEnabled(false);
                        }
                        if (jCheckBoxLand.isSelected() == false) {
                            filterCardList = filter.CardListTypeFilter(filterCardList, "land");
                            g.landCheckBox.setSelected(false);
                            g.landCheckBox.setEnabled(false);
                        }
                        if (jCheckBoxPlaneswalker.isSelected() == false) {
                            filterCardList = filter.CardListTypeFilter(filterCardList, "planeswalker");
                            g.planeswalkerCheckBox.setSelected(false);
                            g.planeswalkerCheckBox.setEnabled(false);
                        }
                        if (jCheckBoxSorcery.isSelected() == false) {
                            filterCardList = filter.CardListTypeFilter(filterCardList, "sorcery");
                            g.sorceryCheckBox.setSelected(false);
                            g.sorceryCheckBox.setEnabled(false);
                        }

                        deckDisplay.updateDisplay(filterCardList, deckDisplay.getBottom());
                    }
                }
            }

        }
        dispose();
    }
}
