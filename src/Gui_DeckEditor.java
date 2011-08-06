import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import forge.error.ErrorViewer;


public class Gui_DeckEditor extends JFrame implements CardDetail, DeckDisplay {
    private static final long serialVersionUID     = 130339644136746796L;
    
    Gui_DeckEditor_Menu       customMenu;
    
    private ImageIcon         upIcon               = Constant.IO.upIcon;
    private ImageIcon         downIcon             = Constant.IO.downIcon;
    
    private TableModel        topModel;
    private TableModel        bottomModel;
    
    private JScrollPane       jScrollPane1         = new JScrollPane();
    private JScrollPane       jScrollPane2         = new JScrollPane();
    private JButton           removeButton         = new JButton();
    @SuppressWarnings("unused")
    // border1
    private Border            border1;
    private TitledBorder      titledBorder1;
    private Border            border2;
    private TitledBorder      titledBorder2;
    private JButton           addButton            = new JButton();
    private JPanel            cardDetailPanel      = new JPanel();
    private Border            border3;
    private TitledBorder      titledBorder3;
    private JPanel            picturePanel         = new JPanel();
    private JLabel            statsLabel           = new JLabel();
    private JTable            topTable             = new JTable();
    private JTable            bottomTable          = new JTable();
    private JScrollPane       jScrollPane3         = new JScrollPane();
    private JPanel            jPanel3              = new JPanel();
    private JLabel            cdLabel4             = new JLabel();
    private JLabel            cdLabel1             = new JLabel();
    private JLabel            cdLabel2             = new JLabel();
    private JLabel            cdLabel3             = new JLabel();
    private GridLayout        gridLayout1          = new GridLayout();
    private JLabel            cdLabel5             = new JLabel();
    private JTextArea         cdTextArea           = new JTextArea();
    private BorderLayout      borderLayout1        = new BorderLayout();
    private JLabel            statsLabel2          = new JLabel();
    private JLabel            jLabel1              = new JLabel();
    
    private JCheckBox         whiteCheckBox        = new JCheckBox("W", true);
    private JCheckBox         blueCheckBox         = new JCheckBox("U", true);
    private JCheckBox         blackCheckBox        = new JCheckBox("B", true);
    private JCheckBox         redCheckBox          = new JCheckBox("R", true);
    private JCheckBox         greenCheckBox        = new JCheckBox("G", true);
    private JCheckBox         colorlessCheckBox    = new JCheckBox("C", true);
    
    private JCheckBox         landCheckBox         = new JCheckBox("Land", true);
    private JCheckBox         creatureCheckBox     = new JCheckBox("Creature", true);
    private JCheckBox         sorceryCheckBox      = new JCheckBox("Sorcery", true);
    private JCheckBox         instantCheckBox      = new JCheckBox("Instant", true);
    private JCheckBox         planeswalkerCheckBox = new JCheckBox("Planeswalker", true);
    private JCheckBox         artifactCheckBox     = new JCheckBox("Artifact", true);
    private JCheckBox         enchantmentCheckBox  = new JCheckBox("Enchant", true);
    
    private CardList          top;
    private CardList          bottom;
    
    public static void main(String[] args) {

    }
    
    @Override
    public void setTitle(String message) {
        super.setTitle(message);
    }
    
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
    
    //top shows available card pool
    //if constructed, top shows all cards
    //if sealed, top shows 5 booster packs
    //if draft, top shows cards that were chosen
    
    public TableModel getTopTableModel() {
        return topModel;
    }
    
    public CardList getTop() {
        return topModel.getCards();
    }
    
    //bottom shows cards that the user has chosen for his library
    public CardList getBottom() {
        return bottomModel.getCards();
    }
    
    public void show(final Command exitCommand) {
        final Command exit = new Command() {
            private static final long serialVersionUID = 5210924838133689758L;
            
            public void execute() {
                Gui_DeckEditor.this.dispose();
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
        

        setup();
        
        //show cards, makes this user friendly
        customMenu.newConstructed();
        
        topModel.sort(1, true);
        bottomModel.sort(1, true);
    }//show(Command)
    
    private void addListeners() {

    }//addListeners()
    
    private void setup() {
        addListeners();
        
        //construct topTable, get all cards
        topModel = new TableModel(new CardList(), this);
        topModel.addListeners(topTable);
        
        topTable.setModel(topModel);
        topModel.resizeCols(topTable);
        
        //construct bottomModel
        bottomModel = new TableModel(this);
        bottomModel.addListeners(bottomTable);
        
        bottomTable.setModel(bottomModel);
        topModel.resizeCols(bottomTable);
        
        //get stats from deck
        bottomModel.addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent ev) {
                CardList deck = bottomModel.getCards();
                statsLabel.setText(getStats(deck));
            }
        });
        

        //get stats from all cards
        topModel.addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent ev) {
                CardList deck = topModel.getCards();
                statsLabel2.setText(getStats(deck));
            }
        });
        
        setSize(1024, 768);
        
        //TODO use this as soon the deck editor has resizable GUI
//        //Use both so that when "un"maximizing, the frame isn't tiny
//        setSize(1024, 740);
//        setExtendedState(Frame.MAXIMIZED_BOTH);
    }//setupAndDisplay()
    
    private String getStats(CardList deck) {
        int total = deck.size();
        int creature = deck.getType("Creature").size();
        int land = deck.getType("Land").size();
        
        String show = "Total - " + total + ", Creatures - " + creature + ", Land - " + land;
        String[] color = Constant.Color.Colors;
        for(int i = 0; i < 5; i++)
            show += ", " + color[i] + " - " + CardListUtil.getColor(deck, color[i]).size();
        
        return show;
    }//getStats()
    
    public Gui_DeckEditor() {
        try {
            jbInit();
        } catch(Exception ex) {
            ErrorViewer.showError(ex);
        }
    }
    
    public void updateCardDetail(Card c) {
        //change card name if needed
        c = AllZone.CardFactory.copyCard(c);
        if(AllZone.NameChanger.shouldChangeCardName()) c = AllZone.NameChanger.changeCard(c);
        
        CardDetailUtil.updateCardDetail(c, cdTextArea, cardDetailPanel, picturePanel, new JLabel[] {
                cdLabel1, cdLabel2, cdLabel3, cdLabel4, cdLabel5});
    }
    
    private void jbInit() throws Exception {
        border1 = new EtchedBorder(EtchedBorder.RAISED, Color.white, new Color(148, 145, 140));
        titledBorder1 = new TitledBorder(BorderFactory.createEtchedBorder(Color.white, new Color(148, 145, 140)),
                "All Cards");
        border2 = BorderFactory.createEtchedBorder(Color.white, new Color(148, 145, 140));
        titledBorder2 = new TitledBorder(border2, "Deck");
        border3 = BorderFactory.createEtchedBorder(Color.white, new Color(148, 145, 140));
        titledBorder3 = new TitledBorder(border3, "Card Detail");
        this.getContentPane().setLayout(null);
        jScrollPane1.setBorder(titledBorder1);
        jScrollPane1.setBounds(new Rectangle(19, 28, 726, 346));
        jScrollPane2.getViewport().setBackground(new Color(204, 204, 204));
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
        
        /**
         * Type filtering
         */
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
        
        /**
         * Color filtering
         */
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
        
        /**
         * Other
         */
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
    }
    
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
    
    @SuppressWarnings("unused")
    // stats_actionPerformed
    private void stats_actionPerformed(CardList list) {

    }
    
    //refresh Gui from deck, Gui shows the cards in the deck
    @SuppressWarnings("unused")
    // refreshGui
    private void refreshGui() {
        Deck deck = Constant.Runtime.HumanDeck[0];
        if(deck == null) //this is just a patch, i know
        deck = new Deck(Constant.Runtime.GameType[0]);
        
        topModel.clear();
        bottomModel.clear();
        
        Card c;
        ReadBoosterPack pack = new ReadBoosterPack();
        for(int i = 0; i < deck.countMain(); i++) {
            c = AllZone.CardFactory.getCard(deck.getMain(i), Constant.Player.Human);
            
            //add rarity to card if this is a sealed card pool
            if(Constant.Runtime.GameType[0].equals(Constant.GameType.Sealed)) c.setRarity(pack.getRarity(c.getName()));
            
            bottomModel.addCard(c);
        }//for
        
        if(deck.isSealed() || deck.isDraft()) {
            //add sideboard to GUI
            for(int i = 0; i < deck.countSideboard(); i++) {
                c = AllZone.CardFactory.getCard(deck.getSideboard(i), Constant.Player.Human);
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
}
