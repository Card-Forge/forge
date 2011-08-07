package forge;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.MouseInputListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileFilter;

import net.miginfocom.swing.MigLayout;
import arcane.ui.CardPanel;
import arcane.ui.ViewPanel;
import forge.deck.Deck;
import forge.error.ErrorViewer;
import forge.gui.game.CardDetailPanel;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;

/**
 * <p>
 * Gui_DeckEditor class.
 * </p>
 * 
 * @author Forge
 * @version $Id: $
 */
public class Gui_DeckEditor extends JFrame implements CardContainer, DeckDisplay, NewConstants {
    /** Constant <code>serialVersionUID=130339644136746796L</code> */
    private static final long serialVersionUID = 130339644136746796L;

    Gui_DeckEditor_Menu customMenu;
    public Gui_ProgressBarWindow gPBW = new Gui_ProgressBarWindow();

    // private ImageIcon upIcon = Constant.IO.upIcon;
    // private ImageIcon downIcon = Constant.IO.downIcon;

    private TableModel topModel;
    private TableModel bottomModel;

    private JScrollPane jScrollPane1 = new JScrollPane();
    private JScrollPane jScrollPane2 = new JScrollPane();
    private JButton removeButton = new JButton();
    @SuppressWarnings("unused")
    // border1
    private Border border1;
    private TitledBorder titledBorder1;
    private Border border2;
    private TitledBorder titledBorder2;
    private JButton addButton = new JButton();
    private JButton analysisButton = new JButton();
    private JButton changePictureButton = new JButton();
    private JButton removePictureButton = new JButton();
    private JLabel statsLabel = new JLabel();
    private JTable topTable = new JTable();
    private JTable bottomTable = new JTable();
    private JScrollPane jScrollPane3 = new JScrollPane();
    private JPanel jPanel3 = new JPanel();
    private GridLayout gridLayout1 = new GridLayout();
    private JLabel statsLabel2 = new JLabel();
    private JLabel jLabel1 = new JLabel();

    private JLabel jLabel2 = new JLabel();
    private JLabel jLabel3 = new JLabel();

    private JLabel jLabel4 = new JLabel();

    /*
     * public JCheckBox whiteCheckBox = new JCheckBox("W", true); public
     * JCheckBox blueCheckBox = new JCheckBox("U", true); public JCheckBox
     * blackCheckBox = new JCheckBox("B", true); public JCheckBox redCheckBox =
     * new JCheckBox("R", true); public JCheckBox greenCheckBox = new
     * JCheckBox("G", true); public JCheckBox colorlessCheckBox = new
     * JCheckBox("C", true);
     * 
     * public JCheckBox landCheckBox = new JCheckBox("Land", true); public
     * JCheckBox creatureCheckBox = new JCheckBox("Creature", true); public
     * JCheckBox sorceryCheckBox = new JCheckBox("Sorcery", true); public
     * JCheckBox instantCheckBox = new JCheckBox("Instant", true); public
     * JCheckBox planeswalkerCheckBox = new JCheckBox("Planeswalker", true);
     * public JCheckBox artifactCheckBox = new JCheckBox("Artifact", true);
     * public JCheckBox enchantmentCheckBox = new JCheckBox("Enchantment",
     * true);
     */

    public JCheckBox whiteCheckBox = new GuiFilterCheckBox("white", "White");
    public JCheckBox blueCheckBox = new GuiFilterCheckBox("blue", "Blue");
    public JCheckBox blackCheckBox = new GuiFilterCheckBox("black", "Black");
    public JCheckBox redCheckBox = new GuiFilterCheckBox("red", "Red");
    public JCheckBox greenCheckBox = new GuiFilterCheckBox("green", "Green");
    public JCheckBox colorlessCheckBox = new GuiFilterCheckBox("colorless", "Colorless");

    public JCheckBox landCheckBox = new GuiFilterCheckBox("land", "Land");
    public JCheckBox creatureCheckBox = new GuiFilterCheckBox("creature", "Creature");
    public JCheckBox sorceryCheckBox = new GuiFilterCheckBox("sorcery", "Sorcery");
    public JCheckBox instantCheckBox = new GuiFilterCheckBox("instant", "Instant");
    public JCheckBox planeswalkerCheckBox = new GuiFilterCheckBox("planeswalker", "Planeswalker");
    public JCheckBox artifactCheckBox = new GuiFilterCheckBox("artifact", "Artifact");
    public JCheckBox enchantmentCheckBox = new GuiFilterCheckBox("enchant", "Enchantment");

    /* CHOPPIC */
    public JButton filterButton = new JButton();
    private JTextField searchTextField = new JTextField();
    /* CHOPPIC */

    private JTextField searchTextField2 = new JTextField();
    private JTextField searchTextField3 = new JTextField();
    private JComboBox searchSetCombo = new JComboBox();
    private JButton clearFilterButton = new JButton();

    private CardList top;
    private CardList bottom;
    public Card cCardHQ;
    /** Constant <code>previousDirectory</code> */
    private static File previousDirectory = null;

    private CardDetailPanel detail = new CardDetailPanel(null);
    private CardPanel picture = new CardPanel(null);
    private ViewPanel pictureViewPanel = new ViewPanel();
    private JPanel glassPane;

    /** {@inheritDoc} */
    @Override
    public void setTitle(String message) {
        super.setTitle(message);
    }

    /** {@inheritDoc} */
    public void updateDisplay(CardList top, CardList bottom) {
        this.top = top;
        this.bottom = bottom;

        topModel.clear();
        bottomModel.clear();

        top = AllZone.getNameChanger().changeCardsIfNeeded(top);
        bottom = AllZone.getNameChanger().changeCardsIfNeeded(bottom);

        Card c;
        String cardName;
        ReadBoosterPack pack = new ReadBoosterPack();

        if (gPBW.isVisible())
            gPBW.setProgressRange(0, top.size() + bottom.size());

        // update top
        for (int i = 0; i < top.size(); i++) {
            if (gPBW.isVisible())
                gPBW.increment();

            c = top.get(i);

            // add rarity to card if this is a sealed card pool

            cardName = AllZone.getNameChanger().getOriginalName(c.getName());
            if (!pack.getRarity(cardName).equals("error")) {
                c.setRarity(pack.getRarity(cardName));
            }

            boolean filteredOut = filterByColor(c);

            if (!filteredOut) {
                filteredOut = filterByType(c);
            }

            // String PC = c.getSVar("PicCount");
            Random r = MyRandom.random;
            // int n = 0;
            // if (!PC.equals("")) {
            // if (PC.matches("[0-9][0-9]?"))
            // n = Integer.parseInt(PC);
            // if (n > 1)
            // c.setRandomPicture(r.nextInt(n));
            // }

            if (c.getCurSetCode().equals(""))
                c.setCurSetCode(c.getMostRecentSet());

            if (!c.getCurSetCode().equals("")) {
                int n = SetInfoUtil.getSetInfo_Code(c.getSets(), c.getCurSetCode()).PicCount;
                if (n > 1)
                    c.setRandomPicture(r.nextInt(n - 1) + 1);

                c.setImageFilename(CardUtil.buildFilename(c));
            }

            if (!filteredOut) {
                topModel.addCard(c);
            }
        }// for

        // update bottom
        for (int i = 0; i < bottom.size(); i++) {
            if (gPBW.isVisible())
                gPBW.increment();

            c = bottom.get(i);

            // add rarity to card if this is a sealed card pool
            if (!customMenu.getGameType().equals(Constant.GameType.Constructed))
                c.setRarity(pack.getRarity(c.getName()));

            // String PC = c.getSVar("PicCount");
            Random r = MyRandom.random;
            // int n = 0;
            // if (!PC.equals("")) {
            // if (PC.matches("[0-9][0-9]?"))
            // n = Integer.parseInt(PC);
            // if (n > 1)
            // c.setRandomPicture(r.nextInt(n));
            // }

            if (c.getCurSetCode().equals(""))
                c.setCurSetCode(c.getMostRecentSet());

            if (!c.getCurSetCode().equals("")) {
                int n = SetInfoUtil.getSetInfo_Code(c.getSets(), c.getCurSetCode()).PicCount;
                if (n > 1)
                    c.setRandomPicture(r.nextInt(n - 1) + 1);

                c.setImageFilename(CardUtil.buildFilename(c));
            }

            bottomModel.addCard(c);
        }// for

        if (gPBW.isVisible())
            gPBW.setTitle("Sorting Deck Editor");
        topModel.resort();
        topTable.repaint();
        bottomModel.resort();
        bottomTable.repaint();
    }// updateDisplay

    /**
     * <p>
     * updateDisplay.
     * </p>
     */
    public void updateDisplay() {
        // updateDisplay(this.top, this.bottom);

        topModel.clear();

        top = AllZone.getNameChanger().changeCardsIfNeeded(top);
        bottom = AllZone.getNameChanger().changeCardsIfNeeded(bottom);

        Card c;
        String cardName;
        ReadBoosterPack pack = new ReadBoosterPack();

        // update top
        for (int i = 0; i < top.size(); i++) {
            c = top.get(i);

            // add rarity to card if this is a sealed card pool

            cardName = AllZone.getNameChanger().getOriginalName(c.getName());
            if (!pack.getRarity(cardName).equals("error")) {
                c.setRarity(pack.getRarity(cardName));
            }

            boolean filteredOut = filterByColor(c);

            if (!filteredOut) {
                filteredOut = filterByType(c);
            }

            if (!filteredOut) {
                filteredOut = filterByName(c);
            }

            if (!filteredOut) {
                filteredOut = filterByCardType(c);
            }

            if (!filteredOut) {
                filteredOut = filterByCardDescription(c);
            }

            if (!filteredOut) {
                filteredOut = filterByCardSetCode(c);
            }

            if (!filteredOut) {
                topModel.addCard(c);
            }
        }// for

        topModel.resort();
    }

    /* CHOPPIC */
    /**
     * <p>
     * filterByName.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    private boolean filterByName(Card c) {
        boolean filterOut = false;
        filterOut = !(c.getName().toLowerCase().contains(searchTextField.getText().toLowerCase()));
        return filterOut;
    }

    /* CHOPPIC */

    /**
     * <p>
     * filterByCardType.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    private boolean filterByCardType(Card c) {
        boolean filterOut = false;
        if (!(searchTextField2.getText() == "")) {
            filterOut = !(c.getType().toString().toLowerCase().contains(searchTextField2.getText().toLowerCase()));
        }
        return filterOut;
    }

    /**
     * <p>
     * filterByCardDescription.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    private boolean filterByCardDescription(Card c) {
        boolean filterOut = false;
        if (!(searchTextField3.getText() == "")) {
            filterOut = !(c.getText().toString().toLowerCase().contains(searchTextField3.getText().toLowerCase()));
        }
        return filterOut;
    }

    /**
     * <p>
     * filterByCardSetCode.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    private boolean filterByCardSetCode(Card c) {
        boolean filterOut = false;
        String SC = "";

        if (!(searchSetCombo.getSelectedItem().toString().equals(""))) {
            SC = SetInfoUtil.getSetCode3_SetName(searchSetCombo.getSelectedItem().toString());

            boolean result = false;

            if (SetInfoUtil.getSetInfo_Code(c.getSets(), SC) != null) {
                c.setCurSetCode(SC);

                Random r = MyRandom.random;
                int n = SetInfoUtil.getSetInfo_Code(c.getSets(), SC).PicCount;
                if (n > 1)
                    c.setRandomPicture(r.nextInt(n - 1) + 1);

                result = true;
            }
            filterOut = !(result);
        } else {
            SC = c.getMostRecentSet();
            if (!SC.equals(""))
                c.setCurSetCode(c.getMostRecentSet());
        }

        c.setImageFilename(CardUtil.buildFilename(c));

        return filterOut;
    }

    /**
     * <p>
     * filterByColor.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    private boolean filterByColor(Card c) {
        boolean filterOut = false;

        if (!whiteCheckBox.isSelected()) {
            if (CardUtil.getColors(c).contains(Constant.Color.White)) {
                filterOut = true;
            }
        }

        if (!blueCheckBox.isSelected()) {
            if (CardUtil.getColors(c).contains(Constant.Color.Blue)) {
                filterOut = true;
            }
        }

        if (!blackCheckBox.isSelected()) {
            if (CardUtil.getColors(c).contains(Constant.Color.Black)) {
                filterOut = true;
            }
        }

        if (!redCheckBox.isSelected()) {
            if (CardUtil.getColors(c).contains(Constant.Color.Red)) {
                filterOut = true;
            }
        }

        if (!greenCheckBox.isSelected()) {
            if (CardUtil.getColors(c).contains(Constant.Color.Green)) {
                filterOut = true;
            }
        }

        if (!colorlessCheckBox.isSelected()) {
            if (CardUtil.getColors(c).contains(Constant.Color.Colorless)) {
                filterOut = true;
            }
        }

        return filterOut;
    }

    /**
     * <p>
     * filterByType.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    private boolean filterByType(Card c) {
        boolean filterOut = false;

        if (!landCheckBox.isSelected() && c.isLand()) {
            filterOut = true;
        }

        if (!creatureCheckBox.isSelected() && c.isCreature()) {
            filterOut = true;
        }

        if (!sorceryCheckBox.isSelected() && c.isSorcery()) {
            filterOut = true;
        }

        if (!instantCheckBox.isSelected() && c.isInstant()) {
            filterOut = true;
        }

        if (!planeswalkerCheckBox.isSelected() && c.isPlaneswalker()) {
            filterOut = true;
        }

        if (!artifactCheckBox.isSelected() && c.isArtifact()) {
            filterOut = true;
        }

        if (!enchantmentCheckBox.isSelected() && c.isEnchantment()) {
            filterOut = true;
        }

        return filterOut;
    }

    // top shows available card pool
    // if constructed, top shows all cards
    // if sealed, top shows 5 booster packs
    // if draft, top shows cards that were chosen

    /**
     * <p>
     * getTopTableModel.
     * </p>
     * 
     * @return a {@link forge.TableModel} object.
     */
    public TableModel getTopTableModel() {
        return topModel;
    }

    /**
     * <p>
     * Getter for the field <code>top</code>.
     * </p>
     * 
     * @return a {@link forge.CardList} object.
     */
    public CardList getTop() {
        return topModel.getCards();
    }

    // bottom shows cards that the user has chosen for his library
    /**
     * <p>
     * Getter for the field <code>bottom</code>.
     * </p>
     * 
     * @return a {@link forge.CardList} object.
     */
    public CardList getBottom() {
        return bottomModel.getCards();
    }

    /**
     * <p>
     * show.
     * </p>
     * 
     * @param exitCommand
     *            a {@link forge.Command} object.
     */
    public void show(final Command exitCommand) {
        final Command exit = new Command() {
            private static final long serialVersionUID = 5210924838133689758L;

            public void execute() {
                Gui_DeckEditor.this.dispose();
                exitCommand.execute();
            }
        };

        // pm = new ProgressMonitor(this, "Loading Deck Editor", "", 0, 20000);
        gPBW.setTitle("Loading Deck Editor");
        gPBW.setVisible(true);

        customMenu = new Gui_DeckEditor_Menu(this, exit);
        this.setJMenuBar(customMenu);

        // do not change this!!!!
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent ev) {
                customMenu.close();
            }
        });

        setup();

        // show cards, makes this user friendly
        if (Constant.Runtime.GameType[0].equals(Constant.GameType.Constructed))
            customMenu.newConstructed();

        topModel.sort(1, true);
        bottomModel.sort(1, true);

        gPBW.dispose();
    }// show(Command)

    /**
     * <p>
     * addListeners.
     * </p>
     */
    private void addListeners() {
        MouseInputListener l = new MouseInputListener() {
            public void mouseReleased(MouseEvent e) {
                redispatchMouseEvent(e);
            }

            public void mousePressed(MouseEvent e) {
                redispatchMouseEvent(e);
            }

            public void mouseExited(MouseEvent e) {
                redispatchMouseEvent(e);
            }

            public void mouseEntered(MouseEvent e) {
                redispatchMouseEvent(e);
            }

            public void mouseClicked(MouseEvent e) {
                redispatchMouseEvent(e);
            }

            public void mouseMoved(MouseEvent e) {
                redispatchMouseEvent(e);
            }

            public void mouseDragged(MouseEvent e) {
                redispatchMouseEvent(e);
            }

            private void redispatchMouseEvent(MouseEvent e) {
                Container content = getContentPane();
                Point glassPoint = e.getPoint();
                Point contentPoint = SwingUtilities.convertPoint(glassPane, glassPoint, content);

                Component component = SwingUtilities.getDeepestComponentAt(content, contentPoint.x, contentPoint.y);
                if (component == null || !SwingUtilities.isDescendingFrom(component, picture)) {
                    glassPane.setVisible(false);
                }
            }
        };

        glassPane.addMouseMotionListener(l);
        glassPane.addMouseListener(l);

        picture.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                Card c = picture.getCard();
                if (c == null)
                    return;
                Image i = ImageCache.getOriginalImage(c);
                if (i == null)
                    return;
                if (i.getWidth(null) < 300)
                    return;
                glassPane.setVisible(true);
            }
        });
    }// addListeners()

    /**
     * <p>
     * setup.
     * </p>
     */
    private void setup() {
        addListeners();

        // construct topTable, get all cards
        topModel = new TableModel(new CardList(), this);
        topModel.addListeners(topTable);

        topTable.setModel(topModel);
        topModel.resizeCols(topTable);

        // construct bottomModel
        bottomModel = new TableModel(this);
        bottomModel.addListeners(bottomTable);

        bottomTable.setModel(bottomModel);
        topModel.resizeCols(bottomTable);

        // get stats from deck
        bottomModel.addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent ev) {
                CardList deck = bottomModel.getCards();
                statsLabel.setText(getStats(deck));
            }
        });

        // get stats from all cards
        topModel.addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent ev) {
                CardList deck = topModel.getCards();
                statsLabel2.setText(getStats(deck));
            }
        });

        // TODO use this as soon the deck editor has resizable GUI
        // Use both so that when "un"maximizing, the frame isn't tiny
        setSize(1024, 740);
        setExtendedState(Frame.MAXIMIZED_BOTH);

        // This was an attempt to limit the width of the deck editor to 1400
        // pixels.
        /*
         * setSize(1024, 740); Rectangle bounds = getBounds(); Dimension screen
         * = getToolkit().getScreenSize(); int maxWidth;
         * 
         * if (screen.width >= 1400) { maxWidth = 1400; } else { maxWidth =
         * screen.width; } bounds.width = maxWidth; bounds.height =
         * screen.height;
         * 
         * setMaximizedBounds(bounds);
         */
    }// setupAndDisplay()

    /**
     * <p>
     * getStats.
     * </p>
     * 
     * @param deck
     *            a {@link forge.CardList} object.
     * @return a {@link java.lang.String} object.
     */
    private String getStats(CardList deck) {
        int total = deck.size();
        int creature = deck.getType("Creature").size();
        int land = deck.getType("Land").size();

        StringBuffer show = new StringBuffer();
        show.append("Total: ").append(total).append(",  Creatures: ").append(creature).append(",  Land: ").append(land);
        String[] color = Constant.Color.Colors;
        for (int i = 0; i < 5; i++)
            show.append(",  ").append(color[i]).append(": ").append(CardListUtil.getColor(deck, color[i]).size());

        return show.toString();
    }// getStats()

    /**
     * <p>
     * Constructor for Gui_DeckEditor.
     * </p>
     */
    public Gui_DeckEditor() {
        try {
            jbInit();
        } catch (Exception ex) {
            ErrorViewer.showError(ex);
        }
    }

    /**
     * <p>
     * getCard.
     * </p>
     * 
     * @return a {@link forge.Card} object.
     */
    public Card getCard() {
        return detail.getCard();
    }

    /** {@inheritDoc} */
    public void setCard(Card card) {
        detail.setCard(card);
        picture.setCard(card);
    }

    /**
     * <p>
     * jbInit.
     * </p>
     * 
     * @throws java.lang.Exception
     *             if any.
     */
    private void jbInit() throws Exception {
        border1 = new EtchedBorder(EtchedBorder.RAISED, Color.white, new Color(148, 145, 140));
        titledBorder1 = new TitledBorder(BorderFactory.createEtchedBorder(Color.white, new Color(148, 145, 140)),
                "All Cards");
        border2 = BorderFactory.createEtchedBorder(Color.white, new Color(148, 145, 140));
        titledBorder2 = new TitledBorder(border2, "Deck");
        this.getContentPane().setLayout(null);
        String tableToolTip = "Click on the column name (like name or color) to sort the cards";
        jScrollPane1.setBorder(titledBorder1);
        jScrollPane1.setToolTipText(tableToolTip);
        jScrollPane2.setBorder(titledBorder2);
        jScrollPane2.setToolTipText(tableToolTip);
        // removeButton.setIcon(upIcon);
        if (!Gui_NewGame.useLAFFonts.isSelected())
            removeButton.setFont(new java.awt.Font("Dialog", 0, 13));
        removeButton.setText("Remove from Deck");
        removeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                removeButton_actionPerformed(e);
            }
        });
        addButton.setText("Add to Deck");
        addButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                addButton_actionPerformed(e);
            }
        });
        // addButton.setIcon(downIcon);
        if (!Gui_NewGame.useLAFFonts.isSelected())
            addButton.setFont(new java.awt.Font("Dialog", 0, 13));

        /* CHOPPIC */
        filterButton.setText("Apply Filter");
        filterButton.setToolTipText("Pressing the \"return\" key will activate this button");
        filterButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                filterButton_actionPerformed(e);
            }
        });
        if (!Gui_NewGame.useLAFFonts.isSelected())
            filterButton.setFont(new java.awt.Font("Dialog", 0, 13));
        /* CHOPPIC */

        clearFilterButton.setText("Clear Filter");
        clearFilterButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                clearFilterButton_actionPerformed(e);
            }
        });
        if (!Gui_NewGame.useLAFFonts.isSelected())
            clearFilterButton.setFont(new java.awt.Font("Dialog", 0, 13));

        analysisButton.setText("Deck Analysis");
        analysisButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                analysisButton_actionPerformed(e);
            }
        });
        if (!Gui_NewGame.useLAFFonts.isSelected())
            analysisButton.setFont(new java.awt.Font("Dialog", 0, 13));

        changePictureButton.setText("Change picture...");
        changePictureButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                changePictureButton_actionPerformed(e);
            }
        });
        if (!Gui_NewGame.useLAFFonts.isSelected())
            changePictureButton.setFont(new java.awt.Font("Dialog", 0, 10));

        removePictureButton.setText("Remove picture...");
        removePictureButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                removePictureButton_actionPerformed(e);
            }
        });
        if (!Gui_NewGame.useLAFFonts.isSelected())
            removePictureButton.setFont(new java.awt.Font("Dialog", 0, 10));

        /**
         * Type filtering
         */
        Font f = new Font("Tahoma", Font.PLAIN, 10);
        if (!Gui_NewGame.useLAFFonts.isSelected())
            landCheckBox.setFont(f);
        landCheckBox.setOpaque(false);
        if (!Gui_NewGame.useLAFFonts.isSelected())
            creatureCheckBox.setFont(f);
        creatureCheckBox.setOpaque(false);
        if (!Gui_NewGame.useLAFFonts.isSelected())
            sorceryCheckBox.setFont(f);
        sorceryCheckBox.setOpaque(false);
        if (!Gui_NewGame.useLAFFonts.isSelected())
            instantCheckBox.setFont(f);
        instantCheckBox.setOpaque(false);
        if (!Gui_NewGame.useLAFFonts.isSelected())
            planeswalkerCheckBox.setFont(f);
        planeswalkerCheckBox.setOpaque(false);
        if (!Gui_NewGame.useLAFFonts.isSelected())
            artifactCheckBox.setFont(f);
        artifactCheckBox.setOpaque(false);
        if (!Gui_NewGame.useLAFFonts.isSelected())
            enchantmentCheckBox.setFont(f);
        enchantmentCheckBox.setOpaque(false);

        /**
         * Color filtering
         */
        whiteCheckBox.setOpaque(false);
        blueCheckBox.setOpaque(false);
        blackCheckBox.setOpaque(false);
        redCheckBox.setOpaque(false);
        greenCheckBox.setOpaque(false);
        colorlessCheckBox.setOpaque(false);

        // picture.addMouseListener(new CustomListener());
        if (!Gui_NewGame.useLAFFonts.isSelected())
            statsLabel.setFont(new java.awt.Font("Dialog", 0, 13));
        statsLabel.setText("Total: 0, Creatures: 0, Land: 0");
        // Do not lower statsLabel any lower, we want this to be visible at 1024
        // x 768 screen size
        this.setTitle("Deck Editor");
        jScrollPane3.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        jPanel3.setLayout(gridLayout1);
        gridLayout1.setColumns(1);
        gridLayout1.setRows(0);
        statsLabel2.setText("Total: 0, Creatures: 0, Land: 0");
        if (!Gui_NewGame.useLAFFonts.isSelected())
            statsLabel2.setFont(new java.awt.Font("Dialog", 0, 13));
        /*
         * jLabel1.setText(
         * "Click on the column name (like name or color) to sort the cards");
         */

        pictureViewPanel.setCardPanel(picture);

        this.getContentPane().setLayout(new MigLayout("fill"));

        // this.getContentPane().add(landCheckBox,
        // "cell 0 0, egx checkbox, split 16");
        this.getContentPane().add(landCheckBox, "cell 0 0, egx checkbox, grow, split 15");
        this.getContentPane().add(creatureCheckBox, "grow");
        this.getContentPane().add(sorceryCheckBox, "grow");
        this.getContentPane().add(instantCheckBox, "grow");
        this.getContentPane().add(planeswalkerCheckBox, "grow");
        this.getContentPane().add(artifactCheckBox, "grow");
        this.getContentPane().add(enchantmentCheckBox, "grow");

        this.getContentPane().add(whiteCheckBox, "grow");
        this.getContentPane().add(blueCheckBox, "grow");
        this.getContentPane().add(blackCheckBox, "grow");
        this.getContentPane().add(redCheckBox, "grow");
        this.getContentPane().add(greenCheckBox, "grow");
        this.getContentPane().add(colorlessCheckBox, "grow");

        this.getContentPane().add(filterButton, "wmin 100, hmin 25, wmax 140, hmax 25, grow");
        this.getContentPane().add(clearFilterButton, "wmin 100, hmin 25, wmax 140, hmax 25, grow");

        this.getContentPane().add(jScrollPane1, "cell 0 2 1 2, pushy, grow");
        // this.getContentPane().add(detail, "w 239, h 323, grow, flowy, wrap");
        this.getContentPane().add(detail, "w 239, h 323, cell 1 0 1 3, grow, flowy, wrap");
        // this.getContentPane().add(detail,
        // "align 50% 50%, wmin 239, hmin 323, cell 1 0 1 2, flowy");
        this.getContentPane().add(changePictureButton, "align 50% 0%, cell 1 3, split 2, flowx");
        this.getContentPane().add(removePictureButton, "align 50% 0%, wrap");

        jLabel1.setText("Name:");
        jLabel1.setToolTipText("Card names must include the text in this field");
        this.getContentPane().add(jLabel1, "cell 0 1, split 7");
        this.getContentPane().add(searchTextField, "wmin 100, grow");

        jLabel2.setText("Type:");
        jLabel2.setToolTipText("Card types must include the text in this field");
        this.getContentPane().add(jLabel2, "");
        this.getContentPane().add(searchTextField2, "wmin 100, grow");
        jLabel3.setText("Text:");
        jLabel3.setToolTipText("Card descriptions must include the text in this field");
        this.getContentPane().add(jLabel3, "");
        this.getContentPane().add(searchTextField3, "wmin 200, grow");

        searchSetCombo.removeAllItems();
        searchSetCombo.addItem("");
        for (int i = 0; i < SetInfoUtil.getSetNameList().size(); i++)
            searchSetCombo.addItem(SetInfoUtil.getSetNameList().get(i));
        this.getContentPane().add(searchSetCombo, "wmin 150, grow");

        this.getContentPane().add(statsLabel2, "cell 0 4");
        this.getContentPane().add(pictureViewPanel, "wmin 239, hmin 323, grow, cell 1 4 1 4");

        this.getContentPane().add(addButton, "w 100, h 49, sg button, cell 0 5, split 4");
        this.getContentPane().add(removeButton, "w 100, h 49, sg button");

        // jLabel4 is used to push the analysis button to the right
        // This will separate this button from the add and remove card buttons
        jLabel4.setText("");
        this.getContentPane().add(jLabel4, "wmin 100, grow");

        this.getContentPane().add(analysisButton, "w 100, h 49, wrap");

        this.getContentPane().add(jScrollPane2, "cell 0 6, grow");
        this.getContentPane().add(statsLabel, "cell 0 7");

        jScrollPane2.getViewport().add(bottomTable, null);
        jScrollPane1.getViewport().add(topTable, null);

        glassPane = new JPanel() {
            private static final long serialVersionUID = 7394924497724994317L;

            @Override
            protected void paintComponent(java.awt.Graphics g) {
                Image image = ImageCache.getOriginalImage(picture.getCard());
                g.drawImage(image, glassPane.getWidth() - image.getWidth(null),
                        glassPane.getHeight() - image.getHeight(null), null);
            }
        };
        setGlassPane(glassPane);

        javax.swing.JRootPane rootPane = this.getRootPane();
        rootPane.setDefaultButton(filterButton);
    }

    /**
     * <p>
     * addButton_actionPerformed.
     * </p>
     * 
     * @param e
     *            a {@link java.awt.event.ActionEvent} object.
     */
    void addButton_actionPerformed(ActionEvent e) {
        setTitle("Deck Editor : " + customMenu.getDeckName() + " : unsaved");

        int n = topTable.getSelectedRow();
        if (n != -1) {
            Card c = topModel.rowToCard(n);

            if (customMenu.getGameType().equals(Constant.GameType.Constructed)) {
                Card newC = new Card();
                newC.setName(c.getName());
                newC.setColor(c.getColor());
                newC.setType(c.getType());
                newC.setManaCost(c.getManaCost());
                newC.setBaseAttack(c.getBaseAttack());
                newC.setBaseDefense(c.getBaseDefense());
                newC.setBaseLoyalty(c.getBaseLoyalty());
                newC.setRarity(c.getRarity());
                newC.setCurSetCode(c.getCurSetCode());
                newC.setImageFilename(c.getImageFilename());
                newC.setSets(c.getSets());
                newC.setText(c.getText());

                bottomModel.addCard(newC);
                bottomModel.resort();
            } else {
                // if(!Constant.GameType.Constructed.equals(customMenu.getGameType()))
                // {
                bottomModel.addCard(c);
                bottomModel.resort();

                top.remove(c);
                topModel.removeCard(c);
            }

            // 3 conditions" 0 cards left, select the same row, select next row
            int size = topModel.getRowCount();
            if (size != 0) {
                if (size == n)
                    n--;
                topTable.addRowSelectionInterval(n, n);
            }
        }// if(valid row)
    }// addButton_actionPerformed

    /* CHOPPIC */
    /**
     * <p>
     * filterButton_actionPerformed.
     * </p>
     * 
     * @param e
     *            a {@link java.awt.event.ActionEvent} object.
     */
    void filterButton_actionPerformed(ActionEvent e) {
        updateDisplay();
    }

    /* CHOPPIC */

    /**
     * <p>
     * clearFilterButton_actionPerformed.
     * </p>
     * 
     * @param e
     *            a {@link java.awt.event.ActionEvent} object.
     */
    void clearFilterButton_actionPerformed(ActionEvent e) {

        if (!landCheckBox.isSelected())
            landCheckBox.doClick();
        if (!creatureCheckBox.isSelected())
            creatureCheckBox.doClick();
        if (!sorceryCheckBox.isSelected())
            sorceryCheckBox.doClick();
        if (!instantCheckBox.isSelected())
            instantCheckBox.doClick();
        if (!planeswalkerCheckBox.isSelected())
            planeswalkerCheckBox.doClick();
        if (!artifactCheckBox.isSelected())
            artifactCheckBox.doClick();
        if (!enchantmentCheckBox.isSelected())
            enchantmentCheckBox.doClick();

        if (!whiteCheckBox.isSelected())
            whiteCheckBox.doClick();
        if (!blueCheckBox.isSelected())
            blueCheckBox.doClick();
        if (!blackCheckBox.isSelected())
            blackCheckBox.doClick();
        if (!redCheckBox.isSelected())
            redCheckBox.doClick();
        if (!greenCheckBox.isSelected())
            greenCheckBox.doClick();
        if (!colorlessCheckBox.isSelected())
            colorlessCheckBox.doClick();

        searchTextField.setText("");
        searchTextField2.setText("");
        searchTextField3.setText("");
        searchSetCombo.setSelectedIndex(0);

        updateDisplay();
    }// clearFilterButton_actionPerformed

    /**
     * <p>
     * analysisButton_actionPerformed.
     * </p>
     * 
     * @param e
     *            a {@link java.awt.event.ActionEvent} object.
     */
    void analysisButton_actionPerformed(ActionEvent e) {

        if (bottomModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(null, "Cards in deck not found.", "Analysis Deck",
                    JOptionPane.INFORMATION_MESSAGE);
        } else {
            Gui_DeckEditor g = Gui_DeckEditor.this;
            GUI_DeckAnalysis dAnalysis = new GUI_DeckAnalysis(g, bottomModel);
            dAnalysis.setVisible(true);
            g.setEnabled(false);
        }
    }

    /**
     * <p>
     * changePictureButton_actionPerformed.
     * </p>
     * 
     * @param e
     *            a {@link java.awt.event.ActionEvent} object.
     */
    void changePictureButton_actionPerformed(ActionEvent e) {
        if (cCardHQ != null) {
            File file = getImportFilename();
            if (file != null) {
                String fileName = GuiDisplayUtil.cleanString(cCardHQ.getName()) + ".jpg";
                File base = ForgeProps.getFile(IMAGE_BASE);
                File f = new File(base, fileName);
                f.delete();

                try {

                    f.createNewFile();
                    FileOutputStream fos = new FileOutputStream(f);
                    FileInputStream fis = new FileInputStream(file);
                    byte[] buff = new byte[32 * 1024];
                    int length;
                    while (fis.available() > 0) {
                        length = fis.read(buff);
                        if (length > 0)
                            fos.write(buff, 0, length);
                    }
                    fos.flush();
                    fis.close();
                    fos.close();
                    setCard(cCardHQ);

                } catch (IOException e1) {
                    e1.printStackTrace();
                }

            }
        }
    }

    /**
     * <p>
     * getImportFilename.
     * </p>
     * 
     * @return a {@link java.io.File} object.
     */
    private File getImportFilename() {
        JFileChooser chooser = new JFileChooser(previousDirectory);
        ImagePreviewPanel preview = new ImagePreviewPanel();
        chooser.setAccessory(preview);
        chooser.addPropertyChangeListener(preview);
        chooser.addChoosableFileFilter(dckFilter);
        int returnVal = chooser.showOpenDialog(null);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            previousDirectory = file.getParentFile();
            return file;
        }

        return null;

    }

    private FileFilter dckFilter = new FileFilter() {

        @Override
        public boolean accept(File f) {
            return f.getName().endsWith(".jpg") || f.isDirectory();
        }

        @Override
        public String getDescription() {
            return "*.jpg";
        }

    };

    /**
     * <p>
     * removePictureButton_actionPerformed.
     * </p>
     * 
     * @param e
     *            a {@link java.awt.event.ActionEvent} object.
     */
    void removePictureButton_actionPerformed(ActionEvent e) {
        if (cCardHQ != null) {
            String options[] = { "Yes", "No" };
            int value = JOptionPane.showOptionDialog(null, "Do you want delete " + cCardHQ.getName() + " picture?",
                    "Delete picture", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options,
                    options[1]);
            if (value == 0) {
                String fileName = GuiDisplayUtil.cleanString(cCardHQ.getName()) + ".jpg";
                File base = ForgeProps.getFile(IMAGE_BASE);
                File f = new File(base, fileName);
                f.delete();
                JOptionPane.showMessageDialog(null, "Picture " + cCardHQ.getName() + " deleted.", "Delete picture",
                        JOptionPane.INFORMATION_MESSAGE);
                setCard(cCardHQ);
            }
        }

    }

    /**
     * <p>
     * removeButton_actionPerformed.
     * </p>
     * 
     * @param e
     *            a {@link java.awt.event.ActionEvent} object.
     */
    void removeButton_actionPerformed(ActionEvent e) {
        setTitle("Deck Editor : " + customMenu.getDeckName() + " : unsaved");

        int n = bottomTable.getSelectedRow();
        if (n != -1) {
            Card c = bottomModel.rowToCard(n);
            bottomModel.removeCard(c);

            if (!Constant.GameType.Constructed.equals(customMenu.getGameType())) {
                topModel.addCard(c);
                topModel.resort();
            }

            // 3 conditions" 0 cards left, select the same row, select next row
            int size = bottomModel.getRowCount();
            if (size != 0) {
                if (size == n)
                    n--;
                bottomTable.addRowSelectionInterval(n, n);
            }
        }// if(valid row)
    }//

    /**
     * <p>
     * stats_actionPerformed.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     */
    @SuppressWarnings("unused")
    // stats_actionPerformed
    private void stats_actionPerformed(CardList list) {

    }

    // refresh Gui from deck, Gui shows the cards in the deck
    /**
     * <p>
     * refreshGui.
     * </p>
     */
    @SuppressWarnings("unused")
    // refreshGui
    private void refreshGui() {
        Deck deck = Constant.Runtime.HumanDeck[0];
        if (deck == null) // this is just a patch, i know
            deck = new Deck(Constant.Runtime.GameType[0]);

        topModel.clear();
        bottomModel.clear();

        Card c;
        // ReadBoosterPack pack = new ReadBoosterPack();
        for (int i = 0; i < deck.countMain(); i++) {
            c = AllZone.getCardFactory().getCard(deck.getMain(i), AllZone.getHumanPlayer());

            // add rarity to card if this is a sealed card pool
            // if(Constant.Runtime.GameType[0].equals(Constant.GameType.Sealed))
            // c.setRarity(pack.getRarity(c.getName()));

            bottomModel.addCard(c);
        }// for

        if (deck.isSealed() || deck.isDraft()) {
            // add sideboard to GUI
            for (int i = 0; i < deck.countSideboard(); i++) {
                c = AllZone.getCardFactory().getCard(deck.getSideboard(i), AllZone.getHumanPlayer());
                // c.setRarity(pack.getRarity(c.getName()));
                topModel.addCard(c);
            }
        } else {
            for (Card loopCard : AllZone.getCardFactory()) {
                topModel.addCard(loopCard);
                c = loopCard; // this might not be necessary
            }
        }

        topModel.resort();
        bottomModel.resort();
    } // //refreshGui()

    /* CHOPPIC */

    // public class CustomListener extends MouseAdapter {
    // reenable
    // public void mouseEntered(MouseEvent e) {
    //
    // if(picturePanel.getComponentCount() != 0) {
    //
    // if(GuiDisplayUtil.IsPictureHQExists(cCardHQ)) {
    // int cWidth = 0;
    // try {
    // cWidth = GuiDisplayUtil.getPictureHQwidth(cCardHQ);
    // } catch(IOException e2) {
    // // TODO Auto-generated catch block
    // e2.printStackTrace();
    // }
    // int cHeight = 0;
    // try {
    // cHeight = GuiDisplayUtil.getPictureHQheight(cCardHQ);
    // } catch(IOException e2) {
    // // Auto-generated catch block
    // e2.printStackTrace();
    // }
    //
    // if(cWidth >= 312 && cHeight >= 445) {
    // if(hq == null) {
    // hq = new GUI_PictureHQ(Gui_DeckEditor.this, cCardHQ);
    // }
    // try {
    // hq.letsGo(Gui_DeckEditor.this, cCardHQ);
    // } catch(IOException e1) {
    // e1.printStackTrace();
    // }
    // }
    //
    // }
    // }
    //
    // }
    // }

}
