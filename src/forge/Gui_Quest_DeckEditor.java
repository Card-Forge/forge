
package forge;


import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.MouseInputListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileFilter;

import forge.error.ErrorViewer;
import forge.gui.game.CardDetailPanel;
import forge.gui.game.CardPicturePanel;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;


public class Gui_Quest_DeckEditor extends JFrame implements CardContainer, DeckDisplay, NewConstants {
    private static final long serialVersionUID     = 152061168634545L;
    
    Gui_Quest_DeckEditor_Menu customMenu;
    
    //private ImageIcon         upIcon               = Constant.IO.upIcon;
    //private ImageIcon         downIcon             = Constant.IO.downIcon;
    
    public TableModel         topModel;
    public TableModel         bottomModel;
    
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
    private JButton           analysisButton       = new JButton();
    private JButton           changePictureButton  = new JButton();
    private JButton           removePictureButton  = new JButton();
    private JLabel            statsLabel           = new JLabel();
    private JTable            topTable             = new JTable();
    private JTable            bottomTable          = new JTable();
    private GridLayout        gridLayout1          = new GridLayout();
    private JLabel            statsLabel2          = new JLabel();
    private JLabel            jLabel1              = new JLabel();
    
    public JCheckBox          whiteCheckBox        = new JCheckBox("W", true);
    public JCheckBox          blueCheckBox         = new JCheckBox("U", true);
    public JCheckBox          blackCheckBox        = new JCheckBox("B", true);
    public JCheckBox          redCheckBox          = new JCheckBox("R", true);
    public JCheckBox          greenCheckBox        = new JCheckBox("G", true);
    public JCheckBox          colorlessCheckBox    = new JCheckBox("C", true);
    
    public JCheckBox          landCheckBox         = new JCheckBox("Land", true);
    public JCheckBox          creatureCheckBox     = new JCheckBox("Creature", true);
    public JCheckBox          sorceryCheckBox      = new JCheckBox("Sorcery", true);
    public JCheckBox          instantCheckBox      = new JCheckBox("Instant", true);
    public JCheckBox          planeswalkerCheckBox = new JCheckBox("Planeswalker", true);
    public JCheckBox          artifactCheckBox     = new JCheckBox("Artifact", true);
    public JCheckBox          enchantmentCheckBox  = new JCheckBox("Enchant", true);
    public CardList           stCardList;
    public boolean            filterUsed;
    private CardList          top;
    private CardList          bottom;
    public Card               cCardHQ;
    private static File       previousDirectory    = null;
    
    private CardDetailPanel   detail               = new CardDetailPanel(null);
    private CardPicturePanel  picture              = new CardPicturePanel(null);
    private JPanel            glassPane;
    
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
        QuestData_BoosterPack pack = new QuestData_BoosterPack();
        
        ArrayList<String> addedList = AllZone.QuestData.getAddedCards();
        

        //update top
        for(int i = 0; i < top.size(); i++) {
            c = top.get(i);
            
            cardName = c.getName();
            c.setRarity(pack.getRarity(cardName));
            
            if(addedList.contains(cardName)) c.setRarity("new");
            
        	c.setCurSetCode(c.getMostRecentSet());
            c.setImageFilename(CardUtil.buildFilename(c));

            topModel.addCard(c);
        }//for
        
        //update bottom
        for(int i = 0; i < bottom.size(); i++) {
            c = bottom.get(i);
            

            c.setRarity(pack.getRarity(c.getName()));;
            
            bottomModel.addCard(c);
        }//for
        
        topModel.resort();
        bottomModel.resort();
    }//updateDisplay
    
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
    
    public TableModel getTopTableModel() {
        return topModel;
    }
    
    
    //top shows available card pool
    public CardList getTop() {
        return topModel.getCards();
    }
    
    //bottom shows cards that the user has chosen for his library
    public CardList getBottom() {
        return bottomModel.getCards();
    }
    
    
    public void show(final Command exitCommand) {
        final Command exit = new Command() {
            private static final long serialVersionUID = -7428793574300520612L;
            
            public void execute() {
                Gui_Quest_DeckEditor.this.dispose();
                exitCommand.execute();
            }
        };
        
        //do not change this!!!!
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent ev) {
                customMenu.close();
            }
        });
        

        setup();
        
        customMenu = new Gui_Quest_DeckEditor_Menu(this, exit);
        this.setJMenuBar(customMenu);
        

        QuestData questData = AllZone.QuestData;
        Deck deck = null;
        
        //open deck that the player used if QuestData has it
        if(Constant.Runtime.HumanDeck[0] != null
                && questData.getDeckNames().contains(Constant.Runtime.HumanDeck[0].getName())) {
            deck = questData.getDeck(Constant.Runtime.HumanDeck[0].getName());
        } else {
            deck = new Deck(Constant.GameType.Sealed);
            deck.setName("");
        }
        
        //tell Gui_Quest_DeckEditor the name of the deck
        customMenu.setHumanPlayer(deck.getName());
        

        //convert Deck main into CardList to show on the screen
        //convert Deck main into CardList to show on the screen
        CardList bottom = new CardList();
        for(int i = 0; i < deck.countMain(); i++) {
            bottom.add(AllZone.CardFactory.getCard(deck.getMain(i), null));
        }
        

        ArrayList<String> list = AllZone.QuestData.getCardpool();
        CardList cardpool = Gui_Quest_DeckEditor_Menu.covertToCardList(list);
        
        //remove bottom cards that are in the deck from the card pool
        for(int i = 0; i < bottom.size(); i++) {
            if(cardpool.containsName(bottom.get(i).getName())) cardpool.remove(bottom.get(i).getName());
        }
        
        //show cards, makes this user friendly, lol, well may, ha
        updateDisplay(cardpool, bottom);
        

        //this affects the card pool
        topModel.sort(4, true);//sort by type
        topModel.sort(3, true);//then sort by color
        
        bottomModel.sort(1, true);
    }//show(Command)
    
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
                if(component == null || !SwingUtilities.isDescendingFrom(component, picture)) {
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
                if(c == null) return;
                Image i = ImageCache.getOriginalImage(c);
                if(i == null) return;
                if(i.getWidth(null) < 200) return;
                glassPane.setVisible(true);
            }
        });
    }//addListeners()
    
    public void setup() {
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
        bottomModel.resizeCols(bottomTable);
        
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
        this.setResizable(false);
        Dimension screen = getToolkit().getScreenSize();
        Rectangle bounds = getBounds();
        bounds.width = 1024;
        bounds.height = 768;
        bounds.x = (screen.width - bounds.width) / 2;
        bounds.y = (screen.height - bounds.height) / 2;
        setBounds(bounds);
        

        //TODO use this as soon the deck editor has resizable GUI
//        //Use both so that when "un"maximizing, the frame isn't tiny
//        setSize(1024, 740);
//        setExtendedState(Frame.MAXIMIZED_BOTH);
    }//setupAndDisplay()
    
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
    
    public Gui_Quest_DeckEditor() {
        try {
            filterUsed = false;
            jbInit();
        } catch(Exception ex) {
            ErrorViewer.showError(ex);
        }
    }
    
    public Card getCard() {
        return detail.getCard();
    }
    
    public void setCard(Card card) {
        detail.setCard(card);
        picture.setCard(card);
    }
    
    private void jbInit() throws Exception {
        
        border1 = new EtchedBorder(EtchedBorder.RAISED, Color.white, new Color(148, 145, 140));
        titledBorder1 = new TitledBorder(BorderFactory.createEtchedBorder(Color.white, new Color(148, 145, 140)),
                "All Cards");
        border2 = BorderFactory.createEtchedBorder(Color.white, new Color(148, 145, 140));
        titledBorder2 = new TitledBorder(border2, "Deck");
        this.getContentPane().setLayout(null);
        jScrollPane1.setBorder(titledBorder1);
        jScrollPane1.setBounds(new Rectangle(19, 20, 726, 346));
        jScrollPane2.setBorder(titledBorder2);
        jScrollPane2.setBounds(new Rectangle(19, 458, 726, 218));
        removeButton.setBounds(new Rectangle(180, 403, 146, 49));
        //removeButton.setIcon(upIcon);
        if(!Gui_NewGame.useLAFFonts.isSelected()) removeButton.setFont(new java.awt.Font("Dialog", 0, 13));
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
        //addButton.setIcon(downIcon);
        if(!Gui_NewGame.useLAFFonts.isSelected()) addButton.setFont(new java.awt.Font("Dialog", 0, 13));
        addButton.setBounds(new Rectangle(23, 403, 146, 49));
        
        analysisButton.setText("Deck Analysis");
        analysisButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                analysisButton_actionPerformed(e);
            }
        });
        if(!Gui_NewGame.useLAFFonts.isSelected()) analysisButton.setFont(new java.awt.Font("Dialog", 0, 13));
        analysisButton.setBounds(new Rectangle(578, 426, 166, 25));
        
        changePictureButton.setText("Change picture...");
        changePictureButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                changePictureButton_actionPerformed(e);
            }
        });
        if(!Gui_NewGame.useLAFFonts.isSelected()) changePictureButton.setFont(new java.awt.Font("Dialog", 0, 10));
        changePictureButton.setBounds(new Rectangle(765, 349, 118, 20));
        
        removePictureButton.setText("Remove picture...");
        removePictureButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                removePictureButton_actionPerformed(e);
            }
        });
        if(!Gui_NewGame.useLAFFonts.isSelected()) removePictureButton.setFont(new java.awt.Font("Dialog", 0, 10));
        removePictureButton.setBounds(new Rectangle(885, 349, 118, 20));
        
        /**
         * Type filtering
         */
        Font f = new Font("Tahoma", Font.PLAIN, 10);
        landCheckBox.setBounds(340, 400, 48, 20);
        if(!Gui_NewGame.useLAFFonts.isSelected()) landCheckBox.setFont(f);
        landCheckBox.setOpaque(false);
        landCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                updateDisplay();
            }
        });
        creatureCheckBox.setBounds(385, 400, 65, 20);
        if(!Gui_NewGame.useLAFFonts.isSelected()) creatureCheckBox.setFont(f);
        creatureCheckBox.setOpaque(false);
        creatureCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                updateDisplay();
            }
        });
        sorceryCheckBox.setBounds(447, 400, 62, 20);
        if(!Gui_NewGame.useLAFFonts.isSelected()) sorceryCheckBox.setFont(f);
        sorceryCheckBox.setOpaque(false);
        sorceryCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                updateDisplay();
            }
        });
        instantCheckBox.setBounds(505, 400, 60, 20);
        if(!Gui_NewGame.useLAFFonts.isSelected()) instantCheckBox.setFont(f);
        instantCheckBox.setOpaque(false);
        instantCheckBox.addItemListener(new ItemListener() {
            
            public void itemStateChanged(ItemEvent e) {
                updateDisplay();
            }
        });
        planeswalkerCheckBox.setBounds(558, 400, 85, 20);
        if(!Gui_NewGame.useLAFFonts.isSelected()) planeswalkerCheckBox.setFont(f);
        planeswalkerCheckBox.setOpaque(false);
        planeswalkerCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                updateDisplay();
            }
        });
        artifactCheckBox.setBounds(638, 400, 58, 20);
        if(!Gui_NewGame.useLAFFonts.isSelected()) artifactCheckBox.setFont(f);
        artifactCheckBox.setOpaque(false);
        artifactCheckBox.addItemListener(new ItemListener() {
            
            public void itemStateChanged(ItemEvent e) {
                updateDisplay();
            }
        });
        enchantmentCheckBox.setBounds(692, 400, 80, 20);
        if(!Gui_NewGame.useLAFFonts.isSelected()) enchantmentCheckBox.setFont(f);
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
        
        detail.setBounds(new Rectangle(765, 23, 239, 323));
        picture.setBounds(new Rectangle(765, 372, 239, 338));
        picture.addMouseListener(new CustomListener());
        if(!Gui_NewGame.useLAFFonts.isSelected()) statsLabel.setFont(new java.awt.Font("Dialog", 0, 14));
        statsLabel.setText("Total - 0, Creatures - 0 Land - 0");
        statsLabel.setBounds(new Rectangle(19, 672, 720, 31));
        //Do not lower statsLabel any lower, we want this to be visible at 1024 x 768 screen size
        this.setTitle("Deck Editor");
        gridLayout1.setColumns(1);
        gridLayout1.setRows(0);
        statsLabel2.setBounds(new Rectangle(19, 365, 720, 31));
        statsLabel2.setText("Total - 0, Creatures - 0 Land - 0");
        if(!Gui_NewGame.useLAFFonts.isSelected()) statsLabel2.setFont(new java.awt.Font("Dialog", 0, 14));
        jLabel1.setText("Click on the column name (like name or color) to sort the cards");
        jLabel1.setBounds(new Rectangle(20, 1, 400, 19));
        this.getContentPane().add(detail, null);
        this.getContentPane().add(picture, null);
        this.getContentPane().add(jScrollPane1, null);
        this.getContentPane().add(jScrollPane2, null);
        this.getContentPane().add(addButton, null);
        this.getContentPane().add(removeButton, null);
        this.getContentPane().add(analysisButton, null);
        this.getContentPane().add(changePictureButton, null);
        this.getContentPane().add(removePictureButton, null);
        this.getContentPane().add(statsLabel2, null);
        this.getContentPane().add(statsLabel, null);
        this.getContentPane().add(jLabel1, null);
        jScrollPane2.getViewport().add(bottomTable, null);
        jScrollPane1.getViewport().add(topTable, null);
        
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
        
        glassPane = new JPanel() {
            private static final long serialVersionUID = 7394924497724994317L;
            
            @Override
            protected void paintComponent(java.awt.Graphics g) {
                Image image = ImageCache.getOriginalImage(picture.getCard());
                g.drawImage(image, glassPane.getWidth() - image.getWidth(null), glassPane.getHeight()
                        - image.getHeight(null), null);
            }
        };
        setGlassPane(glassPane);
    }
    
    void addButton_actionPerformed(ActionEvent e) {
        setTitle("Deck Editor : " + customMenu.getDeckName() + " : unsaved");
        
        int n = topTable.getSelectedRow();
        if(n != -1) {
            Card c = topModel.rowToCard(n);
            bottomModel.addCard(c);
            bottomModel.resort();
            if(filterUsed == true) {
                stCardList.remove(c.getName());
                stCardList.shuffle();
            }
            

            if(!Constant.GameType.Constructed.equals(customMenu.getGameType())) {
                topModel.removeCard(c);
                if(filterUsed == false) {
                    stCardList = this.getTop();
                }
                
            }
            
            //3 conditions" 0 cards left, select the same row, select next row
            int size = topModel.getRowCount();
            if(size != 0) {
                if(size == n) n--;
                topTable.addRowSelectionInterval(n, n);
            }
        }//if(valid row)
        

    }//addButton_actionPerformed
    
    void analysisButton_actionPerformed(ActionEvent e) {
        
        if(bottomModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(null, "Cards in deck not found.", "Analysis Deck",
                    JOptionPane.INFORMATION_MESSAGE);
        } else {
            Gui_Quest_DeckEditor g = Gui_Quest_DeckEditor.this;
            GUI_DeckAnalysis dAnalysis = new GUI_DeckAnalysis(g, bottomModel);
            dAnalysis.setVisible(true);
            g.setEnabled(false);
        }
    }
    
    void changePictureButton_actionPerformed(ActionEvent e) {
        if(cCardHQ != null) {
            File file = getImportFilename();
            if(file != null) {
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
                    while(fis.available() > 0) {
                        length = fis.read(buff);
                        if(length > 0) fos.write(buff, 0, length);
                    }
                    fos.flush();
                    fis.close();
                    fos.close();
                    setCard(cCardHQ);
                    
                } catch(IOException e1) {
                    e1.printStackTrace();
                }
                
            }
        }
    }
    
    private File getImportFilename() {
        JFileChooser chooser = new JFileChooser(previousDirectory);
        ImagePreviewPanel preview = new ImagePreviewPanel();
        chooser.setAccessory(preview);
        chooser.addPropertyChangeListener(preview);
        chooser.addChoosableFileFilter(dckFilter);
        int returnVal = chooser.showOpenDialog(null);
        
        if(returnVal == JFileChooser.APPROVE_OPTION) {
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
    
    
    void removePictureButton_actionPerformed(ActionEvent e) {
        if(cCardHQ != null) {
            String options[] = {"Yes", "No"};
            int value = JOptionPane.showOptionDialog(null,
                    "Do you want delete " + cCardHQ.getName() + " picture?", "Delete picture",
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
            if(value == 0) {
                String fileName = GuiDisplayUtil.cleanString(cCardHQ.getName()) + ".jpg";
                File base = ForgeProps.getFile(IMAGE_BASE);
                File f = new File(base, fileName);
                f.delete();
                JOptionPane.showMessageDialog(null, "Picture " + cCardHQ.getName() + " deleted.",
                        "Delete picture", JOptionPane.INFORMATION_MESSAGE);
                setCard(cCardHQ);
            }
        }
        
    }
    
    
    void removeButton_actionPerformed(ActionEvent e) {
        setTitle("Deck Editor : " + customMenu.getDeckName() + " : unsaved");
        
        int n = bottomTable.getSelectedRow();
        if(n != -1) {
            Card c = bottomModel.rowToCard(n);
            bottomModel.removeCard(c);
            if(filterUsed == true) {
                stCardList.add(c);
            }
            
            if(!Constant.GameType.Constructed.equals(customMenu.getGameType())) {
                topModel.addCard(c);
                topModel.resort();
                if(filterUsed == false) {
                    stCardList = this.getTop();
                }
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
            c = AllZone.CardFactory.getCard(deck.getMain(i), AllZone.HumanPlayer);
            
            c.setRarity(pack.getRarity(c.getName()));
            

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
    
    public class CustomListener extends MouseAdapter {
//        TODO reenable
//        public void mouseEntered(MouseEvent e) {
//            
//            if(picturePanel.getComponentCount() != 0) {
//                
//
//                if(GuiDisplayUtil.IsPictureHQExists(cCardHQ)) {
//                    int cWidth = 0;
//                    try {
//                        cWidth = GuiDisplayUtil.getPictureHQwidth(cCardHQ);
//                    } catch(IOException e2) {
//                        // TODO Auto-generated catch block
//                        e2.printStackTrace();
//                    }
//                    int cHeight = 0;
//                    try {
//                        cHeight = GuiDisplayUtil.getPictureHQheight(cCardHQ);
//                    } catch(IOException e2) {
//                        // TODO Auto-generated catch block
//                        e2.printStackTrace();
//                    }
//                    
//                    if(cWidth >= 312 && cHeight >= 445) {
//                        
//                        GUI_PictureHQ hq = new GUI_PictureHQ(Gui_Quest_DeckEditor.this, cCardHQ);
//                        try {
//                            hq.letsGo(Gui_Quest_DeckEditor.this, cCardHQ);
//                        } catch(IOException e1) {
//                            e1.printStackTrace();
//                        }
//                    }
//                    
//                }
//            }
//            
//        }
    }
    
}
