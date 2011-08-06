
package forge;


import static javax.swing.BorderFactory.*;
import static org.jdesktop.swingx.MultiSplitLayout.*;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.jdesktop.swingx.JXMultiSplitPane;
import org.jdesktop.swingx.MultiSplitLayout.Node;

import forge.error.ErrorViewer;
import forge.gui.game.CardDetailPanel;
import forge.gui.game.CardPicturePanel;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;
import forge.properties.NewConstants.LANG;


/**
 * This class shows a deck editor. It has two parts, one contains available cards, one used cards.
 * 
 * @version V0.0 24.10.2009
 * @author Clemens Koza
 */
@SuppressWarnings("unused")
public class Gui_DeckEditorNew extends JFrame implements CardContainer, NewConstants.GUI.GuiDeckEditor {
    private static final long serialVersionUID = 680850452718332565L;
    
    public static void main(String[] args) {
//        JFrame jf = new JFrame();
        Gui_DeckEditorNew jf = new Gui_DeckEditorNew();
        jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
//        MultiSplitPane p = new MultiSplitPane();
//        p.setModel(MultiSplitLayout.parseModel("(ROW (LEAF name=pool) (LEAF name=deck))"));
//        p.add(new JTextField("pool"), "pool");
//        p.add(new JTextField("deck"), "deck");
//        jf.add(p);
        
//        jf.pack();
        jf.setVisible(true);
    }
    
    Gui_DeckEditor_Menu      customMenu;
    
    private ImageIcon        upIcon               = Constant.IO.upIcon;
    private ImageIcon        downIcon             = Constant.IO.downIcon;
    
    private JScrollPane      jScrollPane1         = new JScrollPane();
    private JScrollPane      jScrollPane2         = new JScrollPane();
    private JButton          removeButton         = new JButton();
    
    // border1
    private Border           border1;
    private TitledBorder     titledBorder1;
    private Border           border2;
    private TitledBorder     titledBorder2;
    private JButton          addButton            = new JButton();
    private Border           border3;
    private TitledBorder     titledBorder3;
    private JLabel           statsLabel           = new JLabel();
    private JScrollPane      jScrollPane3         = new JScrollPane();
    private JPanel           jPanel3              = new JPanel();
    private GridLayout       gridLayout1          = new GridLayout();
    private BorderLayout     borderLayout1        = new BorderLayout();
    private JLabel           statsLabel2          = new JLabel();
    private JLabel           jLabel1              = new JLabel();
    
    private JCheckBox        whiteCheckBox        = new JCheckBox("W", true);
    private JCheckBox        blueCheckBox         = new JCheckBox("U", true);
    private JCheckBox        blackCheckBox        = new JCheckBox("B", true);
    private JCheckBox        redCheckBox          = new JCheckBox("R", true);
    private JCheckBox        greenCheckBox        = new JCheckBox("G", true);
    private JCheckBox        colorlessCheckBox    = new JCheckBox("C", true);
    
    private JCheckBox        landCheckBox         = new JCheckBox("Land", true);
    private JCheckBox        creatureCheckBox     = new JCheckBox("Creature", true);
    private JCheckBox        sorceryCheckBox      = new JCheckBox("Sorcery", true);
    private JCheckBox        instantCheckBox      = new JCheckBox("Instant", true);
    private JCheckBox        planeswalkerCheckBox = new JCheckBox("Planeswalker", true);
    private JCheckBox        artifactCheckBox     = new JCheckBox("Artifact", true);
    private JCheckBox        enchantmentCheckBox  = new JCheckBox("Enchant", true);
    
    private JXMultiSplitPane   pane;
    
    private CardPoolModel    topModel;
    private CardPoolModel    bottomModel;
    
    private JTable           topTable;
    private JTable           bottomTable;
    
    private CardDetailPanel  detail;
    private CardPicturePanel picture;
    
    public Gui_DeckEditorNew() {
        this(null, null);
    }
    
    public Gui_DeckEditorNew(CardPoolModel top, CardPoolModel bottom) {
        try {
            topModel = top != null? top:new EmptyCardPoolModel();
            bottomModel = bottom != null? bottom:new EmptyCardPoolModel();
            setupFrame();
            setupGUI();
            jbInit();
        } catch(Exception ex) {
            ErrorViewer.showError(ex);
        }
    }
    
    private void setupFrame() {
        //Preparing the Frame
        setTitle(ForgeProps.getProperty(LANG.PROGRAM_NAME));
        setFont(new Font("Times New Roman", 0, 16));
        getContentPane().setLayout(new BorderLayout());
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                File f = ForgeProps.getFile(LAYOUT);
                Node layout = pane.getMultiSplitLayout().getModel();
                try {
                    XMLEncoder encoder = new XMLEncoder(new BufferedOutputStream(new FileOutputStream(f)));
                    encoder.writeObject(layout);
                    encoder.close();
                } catch(IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
        
        //making the multi split pane
        Node model;
        File f = ForgeProps.getFile(LAYOUT);
        pane = new JXMultiSplitPane();
        try {
            XMLDecoder decoder = new XMLDecoder(new BufferedInputStream(new FileInputStream(f)));
            model = (Node) decoder.readObject();
            decoder.close();
            pane.getMultiSplitLayout().setModel(model);
            //pane.getMultiSplitLayout().setFloatingDividers(false);
        } catch(Exception ex) {
//            model = parseModel(""//
//                    + "(ROW "//
//                    + " (COLUMN pool deck) "//
////                    + "  (LEAF weight=0.5 name=pool) "//
////                    + "  (LEAF weight=0.5 name=deck) "//
////                    + " ) "//
//                    + " (COLUMN detail picture) "//
////                    + "  (LEAF weight=0.5 name=detail) "//
////                    + "  (LEAF weight=0.5 name=picture) "//
////                    + " ) "//
//                    + ") ");
            model = parseModel(""//
                    + "(ROW"//
                    + " (COLUMN weight=0.5"// 
                    + "  (LEAF weight=0.5 name=pool)"//
                    + "  (LEAF weight=0.5 name=deck)"//
                    + " )" //
                    + " (COLUMN weight=0.5"//
                    + "  (LEAF weight=0.5 name=detail)"//
                    + "  (LEAF weight=0.5 name=picture)"//
                    + " )"//
                    + ")");
            pane.setModel(model);
        }
        getContentPane().add(pane);
    }
    
    private void jbInit() throws Exception {
        { //Card pool
            JPanel pool = new JPanel(new BorderLayout());
            pool.add(new JLabel("Click on the column name (like name or color) to sort the cards"),
                    BorderLayout.NORTH);
            JScrollPane sp = new JScrollPane(topTable);
            sp.setBorder(createTitledBorder("All Cards"));
            pool.add(sp);
            JPanel south = new JPanel(new BorderLayout());
            south.add(statsLabel, BorderLayout.NORTH);
            { //add, remove, filters
                JPanel buttons = new JPanel(new GridLayout(1, 0));
                buttons.add(addButton);
                buttons.add(removeButton);
                south.add(buttons, BorderLayout.WEST);
                
                JPanel filters = new JPanel(new FlowLayout());
                //TODO add checkboxes
                south.add(filters);
            }
            pool.add(south, BorderLayout.SOUTH);
            
            pane.add(pool, "pool");
        }
        
        { //Deck
            JPanel deck = new JPanel(new BorderLayout());
            JScrollPane sp = new JScrollPane(bottomTable);
            sp.setBorder(createTitledBorder("Deck"));
            deck.add(sp);
            deck.add(statsLabel, BorderLayout.SOUTH);
            
            pane.add(deck, "deck");
        }
        
        /*
        border1 = new EtchedBorder(EtchedBorder.RAISED, Color.white, new Color(148, 145, 140));
        titledBorder1 = new TitledBorder(createEtchedBorder(Color.white, new Color(148, 145, 140)), "All Cards");
        border2 = BorderFactory.createEtchedBorder(Color.white, new Color(148, 145, 140));
        titledBorder2 = new TitledBorder(border2, "Deck");
        border3 = BorderFactory.createEtchedBorder(Color.white, new Color(148, 145, 140));
        titledBorder3 = new TitledBorder(border3, "Card Detail");
        this.getContentPane().setLayout(null);
        jScrollPane1.setBorder(titledBorder1);
        jScrollPane1.setBounds(new Rectangle(19, 28, 726, 346));
        jScrollPane2.setBorder(titledBorder2);
        jScrollPane2.setBounds(new Rectangle(19, 458, 726, 218));
        removeButton.setBounds(new Rectangle(180, 403, 146, 49));
        removeButton.setIcon(upIcon);
        removeButton.setFont(new java.awt.Font("Dialog", 0, 13));
        removeButton.setText("Remove Card");
        removeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                removeButton_actionPerformed(e);
            }
        });
        addButton.setText("Add Card");
        addButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                addButton_actionPerformed(e);
            }
        });
        addButton.setIcon(downIcon);
        addButton.setFont(new java.awt.Font("Dialog", 0, 13));
        addButton.setBounds(new Rectangle(23, 403, 146, 49));
        
        // Type filtering
        Font f = new Font("Tahoma", Font.PLAIN, 10);
        landCheckBox.setBounds(340, 400, 48, 20);
        landCheckBox.setFont(f);
        landCheckBox.setOpaque(false);
        landCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                updateDisplay();
            }
        });
        creatureCheckBox.setBounds(385, 400, 65, 20);
        creatureCheckBox.setFont(f);
        creatureCheckBox.setOpaque(false);
        creatureCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                updateDisplay();
            }
        });
        sorceryCheckBox.setBounds(447, 400, 62, 20);
        sorceryCheckBox.setFont(f);
        sorceryCheckBox.setOpaque(false);
        sorceryCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                updateDisplay();
            }
        });
        instantCheckBox.setBounds(505, 400, 60, 20);
        instantCheckBox.setFont(f);
        instantCheckBox.setOpaque(false);
        instantCheckBox.addItemListener(new ItemListener() {
            
            public void itemStateChanged(ItemEvent e) {
                updateDisplay();
            }
        });
        planeswalkerCheckBox.setBounds(558, 400, 85, 20);
        planeswalkerCheckBox.setFont(f);
        planeswalkerCheckBox.setOpaque(false);
        planeswalkerCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                updateDisplay();
            }
        });
        artifactCheckBox.setBounds(638, 400, 58, 20);
        artifactCheckBox.setFont(f);
        artifactCheckBox.setOpaque(false);
        artifactCheckBox.addItemListener(new ItemListener() {
            
            public void itemStateChanged(ItemEvent e) {
                updateDisplay();
            }
        });
        enchantmentCheckBox.setBounds(692, 400, 80, 20);
        enchantmentCheckBox.setFont(f);
        enchantmentCheckBox.setOpaque(false);
        enchantmentCheckBox.addItemListener(new ItemListener() {
            
            public void itemStateChanged(ItemEvent e) {
                updateDisplay();
            }
        });
        
        // Color filtering
        whiteCheckBox.setBounds(340, 430, 40, 20);
        whiteCheckBox.setOpaque(false);
        whiteCheckBox.addItemListener(new ItemListener() {
            
            public void itemStateChanged(ItemEvent e) {
                updateDisplay();
            }
        });
        blueCheckBox.setBounds(380, 430, 40, 20);
        blueCheckBox.setOpaque(false);
        blueCheckBox.addItemListener(new ItemListener() {
            
            public void itemStateChanged(ItemEvent e) {
                updateDisplay();
            }
        });
        blackCheckBox.setBounds(420, 430, 40, 20);
        blackCheckBox.setOpaque(false);
        blackCheckBox.addItemListener(new ItemListener() {
            
            public void itemStateChanged(ItemEvent e) {
                updateDisplay();
            }
        });
        redCheckBox.setBounds(460, 430, 40, 20);
        redCheckBox.setOpaque(false);
        redCheckBox.addItemListener(new ItemListener() {
            
            public void itemStateChanged(ItemEvent e) {
                updateDisplay();
            }
        });
        greenCheckBox.setBounds(500, 430, 40, 20);
        greenCheckBox.setOpaque(false);
        greenCheckBox.addItemListener(new ItemListener() {
            
            public void itemStateChanged(ItemEvent e) {
                updateDisplay();
            }
        });
        colorlessCheckBox.setBounds(540, 430, 40, 20);
        colorlessCheckBox.setOpaque(false);
        colorlessCheckBox.addItemListener(new ItemListener() {
            
            public void itemStateChanged(ItemEvent e) {
                updateDisplay();
            }
        });
        
        // Other
        cardDetailPanel.setBorder(titledBorder3);
        cardDetailPanel.setBounds(new Rectangle(765, 23, 239, 323));
        cardDetailPanel.setLayout(null);
        picturePanel.setBorder(BorderFactory.createEtchedBorder());
        picturePanel.setBounds(new Rectangle(772, 362, 226, 301));
        picturePanel.setLayout(borderLayout1);
        statsLabel.setFont(new java.awt.Font("Dialog", 0, 14));
        statsLabel.setText("Total - 0, Creatures - 0 Land - 0");
        statsLabel.setBounds(new Rectangle(19, 672, 720, 31));
        //Do not lower statsLabel any lower, we want this to be visible at 1024 x 768 screen size
        this.setTitle("Deck Editor");
        jScrollPane3.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        jScrollPane3.setBounds(new Rectangle(6, 168, 225, 143));
        jPanel3.setBounds(new Rectangle(7, 21, 224, 141));
        jPanel3.setLayout(gridLayout1);
        cdLabel4.setFont(new java.awt.Font("Dialog", 0, 14));
        cdLabel4.setHorizontalAlignment(SwingConstants.LEFT);
        cdLabel1.setFont(new java.awt.Font("Dialog", 0, 14));
        cdLabel1.setHorizontalAlignment(SwingConstants.CENTER);
        cdLabel2.setFont(new java.awt.Font("Dialog", 0, 14));
        cdLabel2.setHorizontalAlignment(SwingConstants.CENTER);
        cdLabel3.setFont(new java.awt.Font("Dialog", 0, 14));
        cdLabel3.setHorizontalAlignment(SwingConstants.CENTER);
        gridLayout1.setColumns(1);
        gridLayout1.setRows(0);
        cdLabel5.setFont(new java.awt.Font("Dialog", 0, 14));
        cdLabel5.setHorizontalAlignment(SwingConstants.LEFT);
        cdTextArea.setFont(new java.awt.Font("Dialog", 0, 12));
        cdTextArea.setLineWrap(true);
        cdTextArea.setWrapStyleWord(true);
        statsLabel2.setBounds(new Rectangle(19, 371, 720, 31));
        statsLabel2.setText("Total - 0, Creatures - 0 Land - 0");
        statsLabel2.setFont(new java.awt.Font("Dialog", 0, 14));
        jLabel1.setText("Click on the column name (like name or color) to sort the cards");
        jLabel1.setBounds(new Rectangle(20, 9, 400, 19));
        this.getContentPane().add(cardDetailPanel, null);
        cardDetailPanel.add(jScrollPane3, null);
        jScrollPane3.getViewport().add(cdTextArea, null);
        cardDetailPanel.add(jPanel3, null);
        jPanel3.add(cdLabel1, null);
        jPanel3.add(cdLabel2, null);
        jPanel3.add(cdLabel3, null);
        jPanel3.add(cdLabel4, null);
        this.getContentPane().add(picturePanel, null);
        this.getContentPane().add(jScrollPane1, null);
        this.getContentPane().add(jScrollPane2, null);
        this.getContentPane().add(addButton, null);
        this.getContentPane().add(removeButton, null);
        this.getContentPane().add(statsLabel2, null);
        this.getContentPane().add(statsLabel, null);
        this.getContentPane().add(jLabel1, null);
        jScrollPane2.getViewport().add(bottomTable, null);
        jScrollPane1.getViewport().add(topTable, null);
        jPanel3.add(cdLabel5, null);
        
        this.getContentPane().add(landCheckBox, null);
        this.getContentPane().add(creatureCheckBox, null);
        this.getContentPane().add(sorceryCheckBox, null);
        this.getContentPane().add(instantCheckBox, null);
        this.getContentPane().add(planeswalkerCheckBox, null);
        this.getContentPane().add(artifactCheckBox, null);
        this.getContentPane().add(enchantmentCheckBox, null);
        
        this.getContentPane().add(whiteCheckBox, null);
        this.getContentPane().add(blueCheckBox, null);
        this.getContentPane().add(blackCheckBox, null);
        this.getContentPane().add(redCheckBox, null);
        this.getContentPane().add(greenCheckBox, null);
        this.getContentPane().add(colorlessCheckBox, null);
        */
    }
    
    private void setupGUI() {
        addListeners();
        
        topTable = new JTable(topModel);
        CardPoolModel.setColumnWidths(topTable);
        
        bottomTable = new JTable(bottomModel);
        CardPoolModel.setColumnWidths(bottomTable);
        
//        //get stats from deck
//        bottomModel.addTableModelListener(new TableModelListener() {
//            public void tableChanged(TableModelEvent ev) {
//                CardList deck = bottomModel.getCards();
//                statsLabel.setText(getStats(deck));
//            }
//        });
//        
//
//        //get stats from all cards
//        topModel.addTableModelListener(new TableModelListener() {
//            public void tableChanged(TableModelEvent ev) {
//                CardList deck = topModel.getCards();
//                statsLabel2.setText(getStats(deck));
//            }
//        });
        
        setSize(1024, 768);
        
        //TODO use this as soon the deck editor has resizable GUI
//        //Use both so that when "un"maximizing, the frame isn't tiny
//        setSize(1024, 740);
//        setExtendedState(Frame.MAXIMIZED_BOTH);
    }//setupAndDisplay()
    
    private void addListeners() {

    }//addListeners()
    
    @Override
    public void setTitle(String message) {
        super.setTitle(message);
    }
    
    /*
    public void updateDisplay(CardList top, CardList bottom) {
        
        this.top = top;
        this.bottom = bottom;
        
        topModel.clear();
        bottomModel.clear();
        
        if(AllZone.NameChanger.shouldChangeCardName()) {
            top = new CardList(AllZone.NameChanger.changeCard(top.toArray()));
            bottom = new CardList(AllZone.NameChanger.changeCard(bottom.toArray()));
        }
        
        Card c;
        String cardName;
        ReadBoosterPack pack = new ReadBoosterPack();
        
        // update top
        for(int i = 0; i < top.size(); i++) {
            c = top.get(i);
            
            // add rarity to card if this is a sealed card pool
            
            cardName = AllZone.NameChanger.getOriginalName(c.getName());
            if(!pack.getRarity(cardName).equals("error")) {
                c.setRarity(pack.getRarity(cardName));
            }
            
            boolean filteredOut = filterByColor(c);
            
            if(!filteredOut) {
                filteredOut = filterByType(c);
            }
            
            if(!filteredOut) {
                topModel.addCard(c);
            }
        }// for
        
        // update bottom
        for(int i = 0; i < bottom.size(); i++) {
            c = bottom.get(i);
            
            // add rarity to card if this is a sealed card pool
            if(!customMenu.getGameType().equals(Constant.GameType.Constructed)) c.setRarity(pack.getRarity(c.getName()));
            
            bottomModel.addCard(c);
        }// for
        
        topModel.resort();
        bottomModel.resort();
    }// updateDisplay
    */

    /*
    public void updateDisplay() {
        //updateDisplay(this.top, this.bottom);
        
        topModel.clear();
        
        if(AllZone.NameChanger.shouldChangeCardName()) {
            top = new CardList(AllZone.NameChanger.changeCard(top.toArray()));
            bottom = new CardList(AllZone.NameChanger.changeCard(bottom.toArray()));
        }
        
        Card c;
        String cardName;
        ReadBoosterPack pack = new ReadBoosterPack();
        
        // update top
        for(int i = 0; i < top.size(); i++) {
            c = top.get(i);
            
            // add rarity to card if this is a sealed card pool
            
            cardName = AllZone.NameChanger.getOriginalName(c.getName());
            if(!pack.getRarity(cardName).equals("error")) {
                c.setRarity(pack.getRarity(cardName));
            }
            
            boolean filteredOut = filterByColor(c);
            
            if(!filteredOut) {
                filteredOut = filterByType(c);
            }
            
            if(!filteredOut) {
                topModel.addCard(c);
            }
        }// for
        
        topModel.resort();
    }
    */

    private boolean filterByColor(Card c) {
        boolean filterOut = false;
        
        if(!whiteCheckBox.isSelected()) {
            if(CardUtil.getColors(c).contains(Constant.Color.White)) {
                filterOut = true;
            }
        }
        
        if(!blueCheckBox.isSelected()) {
            if(CardUtil.getColors(c).contains(Constant.Color.Blue)) {
                filterOut = true;
            }
        }
        
        if(!blackCheckBox.isSelected()) {
            if(CardUtil.getColors(c).contains(Constant.Color.Black)) {
                filterOut = true;
            }
        }
        
        if(!redCheckBox.isSelected()) {
            if(CardUtil.getColors(c).contains(Constant.Color.Red)) {
                filterOut = true;
            }
        }
        
        if(!greenCheckBox.isSelected()) {
            if(CardUtil.getColors(c).contains(Constant.Color.Green)) {
                filterOut = true;
            }
        }
        
        if(!colorlessCheckBox.isSelected()) {
            if(CardUtil.getColors(c).contains(Constant.Color.Colorless)) {
                filterOut = true;
            }
        }
        
        return filterOut;
    }
    
    private boolean filterByType(Card c) {
        boolean filterOut = false;
        
        if(!landCheckBox.isSelected() && c.isLand()) {
            filterOut = true;
        }
        
        if(!creatureCheckBox.isSelected() && c.isCreature()) {
            filterOut = true;
        }
        
        if(!sorceryCheckBox.isSelected() && c.isSorcery()) {
            filterOut = true;
        }
        
        if(!instantCheckBox.isSelected() && c.isInstant()) {
            filterOut = true;
        }
        
        if(!planeswalkerCheckBox.isSelected() && c.isPlaneswalker()) {
            filterOut = true;
        }
        
        if(!artifactCheckBox.isSelected() && c.isArtifact()) {
            filterOut = true;
        }
        
        if(!enchantmentCheckBox.isSelected() && c.isEnchantment()) {
            filterOut = true;
        }
        
        return filterOut;
    }
    
    /*
    public void show(final Command exitCommand) {
        final Command exit = new Command() {
            private static final long serialVersionUID = 5210924838133689758L;
            
            public void execute() {
                Gui_DeckEditorNew.this.dispose();
                exitCommand.execute();
            }
        };
        
        customMenu = new Gui_DeckEditor_Menu(this, exit);
        this.setJMenuBar(customMenu);
        

        //do not change this!!!!
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent ev) {
                customMenu.close();
            }
        });
        

        setupGUI();
        
        //show cards, makes this user friendly
        customMenu.newConstructed();
        
        topModel.sort(1, true);
        bottomModel.sort(1, true);
    }//show(Command)
    */

    private String getStats(CardList deck) {
        int total = deck.size();
        int creature = deck.getType("Creature").size();
        int land = deck.getType("Land").size();
        
        StringBuffer show = new StringBuffer();
        show.append("Total - ").append(total).append(", Creatures - ").append(creature).append(", Land - ").append(land);
        String[] color = Constant.Color.Colors;
        for(int i = 0; i < 5; i++)
        	show.append(", ").append(color[i]).append(" - ").append(CardListUtil.getColor(deck, color[i]).size());
        
        return show.toString();
    }//getStats()
    
    public Card getCard() {
        return detail.getCard();
    }
    
    public void setCard(Card card) {
        detail.setCard(card);
        picture.setCard(card);
    }
    
    /*
    void addButton_actionPerformed(ActionEvent e) {
        setTitle("Deck Editor : " + customMenu.getDeckName() + " : unsaved");
        
        int n = topTable.getSelectedRow();
        if(n != -1) {
            Card c = topModel.rowToCard(n);
            bottomModel.addCard(c);
            bottomModel.resort();
            
            if(!Constant.GameType.Constructed.equals(customMenu.getGameType())) {
                topModel.removeCard(c);
            }
            
            //3 conditions" 0 cards left, select the same row, select next row
            int size = topModel.getRowCount();
            if(size != 0) {
                if(size == n) n--;
                topTable.addRowSelectionInterval(n, n);
            }
        }//if(valid row)
    }//addButton_actionPerformed
    */

    /*
    void removeButton_actionPerformed(ActionEvent e) {
        setTitle("Deck Editor : " + customMenu.getDeckName() + " : unsaved");
        
        int n = bottomTable.getSelectedRow();
        if(n != -1) {
            Card c = bottomModel.rowToCard(n);
            bottomModel.removeCard(c);
            
            if(!Constant.GameType.Constructed.equals(customMenu.getGameType())) {
                topModel.addCard(c);
                topModel.resort();
            }
            
            //3 conditions" 0 cards left, select the same row, select next row
            int size = bottomModel.getRowCount();
            if(size != 0) {
                if(size == n) n--;
                bottomTable.addRowSelectionInterval(n, n);
            }
        }//if(valid row)
    }//removeButton_actionPerformed
    */

    private void stats_actionPerformed(CardList list) {

    }
    
    //refresh Gui from deck, Gui shows the cards in the deck
    /*
    @SuppressWarnings("unused")
    private void refreshGui() {
        Deck deck = Constant.Runtime.HumanDeck[0];
        if(deck == null) //this is just a patch, i know
        deck = new Deck(Constant.Runtime.GameType[0]);
        
        topModel.clear();
        bottomModel.clear();
        
        Card c;
        ReadBoosterPack pack = new ReadBoosterPack();
        for(int i = 0; i < deck.countMain(); i++) {
            c = AllZone.CardFactory.getCard(deck.getMain(i), AllZone.HumanPlayer);
            
            //add rarity to card if this is a sealed card pool
            if(Constant.Runtime.GameType[0].equals(Constant.GameType.Sealed)) c.setRarity(pack.getRarity(c.getName()));
            
            bottomModel.addCard(c);
        }//for
        
        if(deck.isSealed() || deck.isDraft()) {
            //add sideboard to GUI
            for(int i = 0; i < deck.countSideboard(); i++) {
                c = AllZone.CardFactory.getCard(deck.getSideboard(i), AllZone.HumanPlayer);
                c.setRarity(pack.getRarity(c.getName()));
                topModel.addCard(c);
            }
        } else {
            CardList all = AllZone.CardFactory.getAllCards();
            for(int i = 0; i < all.size(); i++)
                topModel.addCard(all.get(i));
        }
        
        topModel.resort();
        bottomModel.resort();
    }////refreshGui()
    */

    public abstract static class CardPoolModel extends AbstractTableModel {
        private static final long       serialVersionUID = 7773113247062724912L;
        
        private static final String[]   labels           = {"Qty", "Name", "Cost", "Color", "Type", "Stats", "R"};
        private static final Class<?>[] classes          = {
                                                                 Integer.class, String.class, String.class,
                                                                 String.class};
        //values taken from TableModel
        private static final int[]      minWidth         = {-1, 190, 85, -1, -1, -1, -1};
        private static final int[]      prefWidth        = {25, 190, 85, 58, 130, 32, 20};
        private static final int[]      maxWidth         = {25, 190, 126, 58, -1, 42, 20};
        
        /**
         * Sets the column widths of the table. The table must use a CardPoolModel as its table model.
         */
        public static void setColumnWidths(JTable t) {
            if(t.getModel() != null && !(t.getModel() instanceof CardPoolModel)) throw new IllegalArgumentException(
                    "Model is not a CardPoolModel");
            TableColumnModel m = t.getColumnModel();
            for(int i = 0; i < m.getColumnCount(); i++) {
                TableColumn c = m.getColumn(i);
                
                if(minWidth[i] >= 0) c.setMinWidth(minWidth[i]);
                if(prefWidth[i] >= 0) c.setPreferredWidth(prefWidth[i]);
                if(maxWidth[i] >= 0) c.setMaxWidth(maxWidth[i]);
            }
        }
        
        public int getColumnCount() {
            return labels.length;
        }
        
        @Override
        public String getColumnName(int column) {
            return labels[column];
        }
        
        @Override
        public Class<?> getColumnClass(int column) {
            return classes[column];
        }
        
        public Object getValueAt(int rowIndex, int column) {
            CardQty cq = getRow(rowIndex);
            Card c = cq.getCard();
            switch(column) {
                case 0: //Qty
                    return cq.getQty();
                case 1: //Name
                    return cq.getCard().getName();
                case 2: //Cost
                    return cq.getCard().getManaCost();
                case 3: //Color
                    return TableSorter.getColor(c);
                case 4: //Type
                    return GuiDisplayUtil.formatCardType(c);
                case 5: //Stats
                    return c.isCreature()? c.getBaseAttackString() + "/" + c.getBaseDefenseString():"";
                case 6: //R
                    String rarity = c.getRarity();
                    if(rarity.length() > 0) rarity = rarity.substring(0, 1);
                    return rarity;
                default:
                    throw new AssertionError();
            }
        }
        
        /**
         * Returns the non-null {@link CardQty} object for the given row index, unsorted. Throws an
         * {@link IllegalArgumentException} if {@code index < 0 || index >= }{@link #getRowCount()}.
         */
        public abstract CardQty getRow(int row);
        
        public abstract int getRowCount();
        
        /**
         * Adds the card to the pool. If a CardQty for the specified card exists, its qty should be increased by 1.
         * Otherwise, a new {@link CardQty} object should be added to the model, informing all listeners.
         */
        public abstract void add(Card c);
        
        /**
         * Adds the card to the pool. If a CardQty for the specified card exists and has more than 1 instances, its
         * qty should be decreased by 1. Otherwise, the {@link CardQty} should be removed from the model, informing
         * all listeners.
         */
        public abstract void remove(Card c);
        
        public static interface CardQty {
            public Card getCard();
            
            public int getQty();
        }
    }
    
    /**
     * A stub model that cannot contain cards.
     */
    private static class EmptyCardPoolModel extends CardPoolModel {
        private static final long serialVersionUID = -5832487673696527783L;
        
        @Override
        public void add(Card c) {}
        
        @Override
        public void remove(Card c) {}
        
        @Override
        public int getRowCount() {
            return 0;
        }
        
        @Override
        public CardQty getRow(int row) {
            throw new IllegalArgumentException();
        }
    }
}
