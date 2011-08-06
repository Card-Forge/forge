
package forge;


import java.awt.Color;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import forge.error.ErrorViewer;
import forge.gui.game.CardDetailPanel;
import forge.gui.game.CardPicturePanel;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;


public class Gui_BoosterDraft extends JFrame implements CardContainer, NewConstants, NewConstants.LANG.Gui_BoosterDraft {
    private static final long    serialVersionUID  = -6055633915602448260L;
    
    private BoosterDraft         boosterDraft;
    
    private static final boolean limitedDeckEditor = true;
    
    private TableModel           allCardModel;
    private TableModel           deckModel;
    
    private JScrollPane          jScrollPane1      = new JScrollPane();
    private JScrollPane          jScrollPane2      = new JScrollPane();
    private TitledBorder         titledBorder1;
    private TitledBorder         titledBorder2;
    private Border               border3;
    private TitledBorder         titledBorder3;
    private JLabel               statsLabel        = new JLabel();
    private JTable               allCardTable      = new JTable();
    private JTable               deckTable         = new JTable();
    private JScrollPane          jScrollPane3      = new JScrollPane();
    private JPanel               jPanel3           = new JPanel();
    private GridLayout           gridLayout1       = new GridLayout();
    private JLabel               statsLabel2       = new JLabel();
    private JButton              jButton1          = new JButton();
    private CardDetailPanel      detail            = new CardDetailPanel(null);
    private CardPicturePanel     picture           = new CardPicturePanel(null);
    
    public static void main(String[] args) {
        Constant.Runtime.GameType[0] = Constant.GameType.Draft;
        Constant.Runtime.HumanDeck[0] = new Deck(Constant.GameType.Sealed);
        
        Gui_BoosterDraft g = new Gui_BoosterDraft();
        g.showGui(new BoosterDraftTest());
    }
    
    
    public void showGui(BoosterDraft in_boosterDraft) {
        boosterDraft = in_boosterDraft;
        
        setup();
        showChoices(boosterDraft.nextChoice());
        
        allCardModel.sort(1, true);
        deckModel.sort(1, true);
        
        setVisible(true);
    }
    
    private void addListeners() {
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent ev) {
                int n = JOptionPane.showConfirmDialog(null, ForgeProps.getLocalized(CLOSE_MESSAGE), "",
                        JOptionPane.YES_NO_OPTION);
                if(n == JOptionPane.YES_OPTION) {
                    dispose();
                    new Gui_NewGame();
                }
            }//windowClosing()
        });
    }//addListeners()
    
    private void setup() {
        addListeners();
//    setupMenu();
        
        //construct allCardTable, get all cards
        allCardModel = new TableModel(new CardList(), this);
        allCardModel.addListeners(allCardTable);
        allCardTable.setModel(allCardModel);
        
        allCardModel.resizeCols(allCardTable);
        
        //construct deckModel
        deckModel = new TableModel(this);
        deckModel.addListeners(deckTable);
        deckTable.setModel(deckModel);
        
        deckModel.resizeCols(deckTable);
        
        //add cards to GUI from deck
//    refreshGui();
        
        allCardTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if((e.getModifiers() & InputEvent.BUTTON3_MASK) != 0) jButton1_actionPerformed(null);
            }
        });//MouseListener
        

        //get stats from deck
        deckModel.addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent ev) {
                CardList deck = deckModel.getCards();
                statsLabel.setText(getStats(deck));
            }
        });
        

        //get stats from all cards
        allCardModel.addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent ev) {
                CardList deck = allCardModel.getCards();
                statsLabel2.setText(getStats(deck));
            }
        });
        
        //Use both so that when "un"maximizing, the frame isn't tiny
        setSize(1024, 740);
        setExtendedState(Frame.MAXIMIZED_BOTH);
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
    
    public Gui_BoosterDraft() {
        try {
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
        titledBorder1 = new TitledBorder(BorderFactory.createEtchedBorder(Color.white, new Color(148, 145, 140)),
                "Previously Picked Cards");
        titledBorder2 = new TitledBorder(BorderFactory.createEtchedBorder(Color.white, new Color(148, 145, 140)),
                "Choose one card");
        border3 = BorderFactory.createEtchedBorder(Color.white, new Color(148, 145, 140));
        titledBorder3 = new TitledBorder(border3, "Card Detail");
        this.getContentPane().setLayout(null);
        jScrollPane1.setBorder(titledBorder2);
        jScrollPane1.setBounds(new Rectangle(19, 28, 661, 344));
        jScrollPane2.setBorder(titledBorder1);
        jScrollPane2.setBounds(new Rectangle(19, 478, 661, 184));
        detail.setBorder(titledBorder3);
        detail.setBounds(new Rectangle(693, 23, 239, 323));
        picture.setBounds(new Rectangle(693, 348, 240, 340));
        statsLabel.setFont(new java.awt.Font("Dialog", 0, 16));
        statsLabel.setText("Total - 0, Creatures - 0 Land - 0");
        statsLabel.setBounds(new Rectangle(19, 665, 665, 31));
        this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        this.setTitle("Booster Draft");
        jScrollPane3.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        jScrollPane3.setBounds(new Rectangle(6, 168, 225, 143));
        jPanel3.setBounds(new Rectangle(7, 21, 224, 141));
        jPanel3.setLayout(gridLayout1);
        gridLayout1.setColumns(1);
        gridLayout1.setRows(0);
        statsLabel2.setBounds(new Rectangle(19, 378, 665, 31));
        statsLabel2.setText("Total - 0, Creatures - 0 Land - 0");
        statsLabel2.setFont(new java.awt.Font("Dialog", 0, 16));
        jButton1.setBounds(new Rectangle(238, 418, 147, 44));
        jButton1.setFont(new java.awt.Font("Dialog", 0, 16));
        jButton1.setText("Choose Card");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jButton1_actionPerformed(e);
            }
        });
        this.getContentPane().add(detail, null);
        this.getContentPane().add(picture, null);
        this.getContentPane().add(jScrollPane1, null);
        this.getContentPane().add(statsLabel2, null);
        this.getContentPane().add(statsLabel, null);
        this.getContentPane().add(jScrollPane2, null);
        this.getContentPane().add(jButton1, null);
        jScrollPane2.getViewport().add(deckTable, null);
        jScrollPane1.getViewport().add(allCardTable, null);
    }
    
    void addButton_actionPerformed(ActionEvent e) {
        int n = allCardTable.getSelectedRow();
        if(n != -1) {
            setTitle("Deck Editor - " + Constant.Runtime.HumanDeck[0].getName() + " - changed");
            
            Card c = allCardModel.rowToCard(n);
            deckModel.addCard(c);
            deckModel.resort();
            
            if(limitedDeckEditor) {
                allCardModel.removeCard(c);
            }
            
            //3 conditions" 0 cards left, select the same row, select next row
            int size = allCardModel.getRowCount();
            if(size != 0) {
                if(size == n) n--;
                allCardTable.addRowSelectionInterval(n, n);
            }
        }//if(valid row)
    }//addButton_actionPerformed
    
    void removeButton_actionPerformed(ActionEvent e) {
        int n = deckTable.getSelectedRow();
        if(n != -1) {
            setTitle("Deck Editor - " + Constant.Runtime.HumanDeck[0].getName() + " - changed");
            
            Card c = deckModel.rowToCard(n);
            deckModel.removeCard(c);
            
            if(limitedDeckEditor) {
                allCardModel.addCard(c);
                allCardModel.resort();
            }
            
            //3 conditions" 0 cards left, select the same row, select next row
            int size = deckModel.getRowCount();
            if(size != 0) {
                if(size == n) n--;
                deckTable.addRowSelectionInterval(n, n);
            }
        }//if(valid row)
    }//removeButton_actionPerformed
    
    //if true, don't do anything else
    private boolean checkSaveDeck() {
        //a crappy way of checking if the deck has been saved
        if(getTitle().endsWith("changed")) {
            
            int n = JOptionPane.showConfirmDialog(null, ForgeProps.getLocalized(SAVE_MESSAGE),
                    ForgeProps.getLocalized(SAVE_TITLE), JOptionPane.YES_NO_CANCEL_OPTION);
            if(n == JOptionPane.CANCEL_OPTION) return true;
            else if(n == JOptionPane.YES_OPTION) saveItem_actionPerformed();
        }
        return false;
    }//checkSaveDeck()
    
    private void newItem_actionPerformed() {
        if(checkSaveDeck()) return;
        
        setTitle("Deck Editor");
        
        Deck deck = Constant.Runtime.HumanDeck[0];
        while(deck.countMain() != 0)
            deck.addSideboard(deck.removeMain(0));
        
        refreshGui();
    }//newItem_actionPerformed
    
    private void closeItem_actionPerformed() {
        //check if saved, show dialog "yes, "no"
        checkSaveDeck();
        dispose();
    }
    
    private void stats_actionPerformed(CardList list) {

    }
    
    private void saveAsItem_actionPerformed() {}//saveItem_actionPerformed()
    
    private void saveItem_actionPerformed() {}
    
    private void openItem_actionPerformed() {}//openItem_actionPerformed()
    
    public void deleteItem_actionPerformed() {}
    
    public void renameItem_actionPerformed() {
        String newName = "";
        while(newName.equals("")) {
            newName = JOptionPane.showInputDialog(null, ForgeProps.getLocalized(RENAME_MESSAGE),
                    ForgeProps.getLocalized(RENAME_TITLE), JOptionPane.QUESTION_MESSAGE);
            if(newName == null) break;
        }
        
        //when the user selects "Cancel"
        if(newName != null) {
            //String oldName = Constant.Runtime.HumanDeck[0].getName(); //unused
            
            Constant.Runtime.HumanDeck[0].setName(newName);
            setTitle("Deck Editor - " + newName + " - changed");
        }
    }
    
    @SuppressWarnings("unused")
    // setupMenu
    private void setupMenu() {
        //final boolean[] isSaved = new boolean[1]; // unused
        
        JMenuItem newItem = new JMenuItem("New");
        JMenuItem openItem = new JMenuItem("Open");
        JMenuItem saveItem = new JMenuItem("Save");
        JMenuItem saveAsItem = new JMenuItem("Save As");
        JMenuItem renameItem = new JMenuItem("Rename");
        JMenuItem deleteItem = new JMenuItem("Delete");
        JMenuItem statsPoolItem = new JMenuItem("Statistics - Card Pool");
        JMenuItem statsDeckItem = new JMenuItem("Statistics - Deck");
        JMenuItem closeItem = new JMenuItem("Close");
        
        newItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                newItem_actionPerformed();
            }
        });
        openItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                openItem_actionPerformed();
            }
        });
        saveItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                saveItem_actionPerformed();
            }
        });
        saveAsItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                saveAsItem_actionPerformed();
            }
        });
        renameItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                renameItem_actionPerformed();
            }
        });
        deleteItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                deleteItem_actionPerformed();
            }
        });
        statsPoolItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                stats_actionPerformed(allCardModel.getCards());
            }
        });
        statsDeckItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                stats_actionPerformed(deckModel.getCards());
            }
        });
        closeItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                closeItem_actionPerformed();
            }
        });
        
        JMenu fileMenu = new JMenu("Deck Actions");
        fileMenu.add(newItem);
        fileMenu.add(openItem);
        fileMenu.add(saveItem);
        fileMenu.add(saveAsItem);
        
        fileMenu.addSeparator();
        fileMenu.add(renameItem);
        fileMenu.add(deleteItem);
//    fileMenu.add(statsPoolItem);
//    fileMenu.add(statsDeckItem);
        fileMenu.addSeparator();
        fileMenu.add(closeItem);
        
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(fileMenu);
        
        this.setJMenuBar(menuBar);
    }/*setupMenu();  */
    
    //refresh Gui from deck, Gui shows the cards in the deck
    private void refreshGui() {
        Deck deck = Constant.Runtime.HumanDeck[0];
        if(deck == null) //this is just a patch, i know
        deck = new Deck(Constant.Runtime.GameType[0]);
        
        allCardModel.clear();
        deckModel.clear();
        
        Card c;
        ReadDraftBoosterPack pack = new ReadDraftBoosterPack();
        for(int i = 0; i < deck.countMain(); i++) {
            c = AllZone.CardFactory.getCard(deck.getMain(i), AllZone.HumanPlayer);
            
            //add rarity to card if this is a sealed card pool
            if(!Constant.Runtime.GameType[0].equals(Constant.GameType.Constructed)) c.setRarity(pack.getRarity(c.getName()));;
            
            
            deckModel.addCard(c);
        }//for
        
        if(deck.isSealed() || deck.isRegular()) {
            //add sideboard to GUI
            for(int i = 0; i < deck.countSideboard(); i++) {
                c = AllZone.CardFactory.getCard(deck.getSideboard(i), AllZone.HumanPlayer);
                c.setRarity(pack.getRarity(c.getName()));
                allCardModel.addCard(c);
            }
        } else {
            CardList all = AllZone.CardFactory.getAllCards();
            for(int i = 0; i < all.size(); i++)
                allCardModel.addCard(all.get(i));
        }
        
        allCardModel.resort();
        deckModel.resort();
    }//refreshGui()
    
    //updates Constant.Runtime.HumanDeck[0] from the cards shown in the GUI
    @SuppressWarnings("unused")
    // refreshDeck
    private void refreshDeck() {
        //make new Deck
        Deck deck = new Deck(Constant.Runtime.GameType[0]);
        deck.setName(Constant.Runtime.HumanDeck[0].getName());
        Constant.Runtime.HumanDeck[0] = deck;
        
        //update Deck with cards shown in GUI
        CardList list = deckModel.getCards();
        for(int i = 0; i < list.size(); i++)
            deck.addMain(list.get(i).getName());
        
        if(deck.isSealed()) {
            //add sideboard to deck
            list = allCardModel.getCards();
            for(int i = 0; i < list.size(); i++)
                deck.addSideboard(list.get(i).getName());
        }
    }/* refreshDeck() */
    
    void jButton1_actionPerformed(ActionEvent e) {
        //pick card
        int n = allCardTable.getSelectedRow();
        if(n == -1) //is valid selection?
        return;
        
        Card c = allCardModel.rowToCard(n);
        
        deckModel.addCard(c);
        deckModel.resort();
        
        //get next booster pack
        boosterDraft.setChoice(c);
        if(boosterDraft.hasNextChoice()) {
            showChoices(boosterDraft.nextChoice());
        } else {
            //quit
            saveDraft();
            dispose();
        }
    }/*OK Button*/
    
    private void showChoices(CardList list) {
        allCardModel.clear();
        
        ReadDraftBoosterPack pack = new ReadDraftBoosterPack();
        Card c;
        for(int i = 0; i < list.size(); i++) {
            c = list.get(i);
            c.setRarity(pack.getRarity(c.getName()));
            
            if (c.getCurSetCode().equals(""))
            {
            	c.setCurSetCode(c.getMostRecentSet());
            	c.setImageFilename(CardUtil.buildFilename(c));
            }
            
            allCardModel.addCard(c);
        }
        allCardModel.resort();
        allCardTable.setRowSelectionInterval(0, 0);
        
    }//showChoices()
    
    private Deck getPlayersDeck() {
        Deck deck = new Deck(Constant.GameType.Draft);
        Constant.Runtime.HumanDeck[0] = deck;
        
        //add sideboard to deck
        CardList list = deckModel.getCards();
        for(int i = 0; i < list.size(); i++)
            deck.addSideboard(list.get(i).getName());
        

        for(int i = 0; i < 40; i++) {
            deck.addSideboard("Forest");
            deck.addSideboard("Mountain");
            deck.addSideboard("Swamp");
            deck.addSideboard("Island");
            deck.addSideboard("Plains");
        }
        
        return deck;
    }//getPlayersDeck()
    
    private void saveDraft() {
        String s = "";
        while(s == null || s.length() == 0) {
            s = JOptionPane.showInputDialog(null, ForgeProps.getLocalized(SAVE_DRAFT_MESSAGE),
                    ForgeProps.getLocalized(SAVE_DRAFT_TITLE), JOptionPane.QUESTION_MESSAGE);
        }
        //TODO: check if overwriting the same name, and let the user delete old drafts
        
        //construct computer's decks
        //save draft
        Deck[] computer = boosterDraft.getDecks();
        
        Deck human = getPlayersDeck();
        human.setName(s);
        
        Deck[] all = {
                human, computer[0], computer[1], computer[2], computer[3], computer[4], computer[5], computer[6]};
        
//        DeckIO deckIO = new OldDeckIO(ForgeProps.getFile(BOOSTER_DECKS));
        DeckIO deckIO = new NewDeckIO(ForgeProps.getFile(NEW_DECKS));
        deckIO.writeBoosterDeck(all);
        
        //write file
        deckIO.close();
        
        //close and open next screen
        dispose();
        new Gui_NewGame();
    }/*saveDraft()*/
}
