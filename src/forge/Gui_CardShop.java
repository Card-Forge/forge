
package forge;


import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Map;
import java.util.Random;

import javax.swing.BorderFactory;
import javax.swing.JButton;
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
import forge.error.ErrorViewer;
import forge.gui.game.CardDetailPanel;
import forge.gui.game.CardPicturePanel;
import forge.properties.NewConstants;

public class Gui_CardShop extends JFrame implements CardContainer, DeckDisplay, NewConstants {

	private static final long serialVersionUID = 3988857075791576483L;

	Gui_DeckEditor_Menu       customMenu;
    
    //private ImageIcon         upIcon               = Constant.IO.upIcon;
    //private ImageIcon         downIcon             = Constant.IO.downIcon;
    
    private CardShopTableModel        topModel;
    private CardShopTableModel        bottomModel;
    
    private JScrollPane       jScrollPane1         = new JScrollPane();
    private JScrollPane       jScrollPane2         = new JScrollPane();
    private JButton           sellButton           = new JButton();
    @SuppressWarnings("unused")
    // border1
    private Border            border1;
    private TitledBorder      titledBorder1;
    private Border            border2;
    private TitledBorder      titledBorder2;
    private JButton           buyButton            = new JButton();

    private JTable            topTable             = new JTable();
    private JTable            bottomTable          = new JTable();
    private JScrollPane       jScrollPane3         = new JScrollPane();
    private JPanel            jPanel3              = new JPanel();
    private GridLayout        gridLayout1          = new GridLayout();
    private JLabel            creditsLabel         = new JLabel();
    private JLabel            jLabel1              = new JLabel();
    private JLabel			  sellPercentageLabel  = new JLabel();
    
    private double			  multi;
    
    private CardList          top;
    private CardList          bottom;
    public Card               cCardHQ;
    
    private CardDetailPanel   detail               = new CardDetailPanel(null);
    private CardPicturePanel  picture              = new CardPicturePanel(null);
    private JPanel            glassPane;
    
    private QuestData 		  questData;
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
            
            String PC = c.getSVar("PicCount");
            Random r = new Random();
            int n = 0;
            if (PC.matches("[0-9][0-9]?"))
            	n = Integer.parseInt(PC);
            if (n > 1)
                c.setRandomPicture(r.nextInt(n));
            
        	if (c.getCurSetCode().equals(""))
        	{
        		c.setCurSetCode(c.getMostRecentSet());
        		c.setImageFilename(CardUtil.buildFilename(c));
        	}
            

            topModel.addCard(c);
            
        }// for
        
        // update bottom
        for(int i = 0; i < bottom.size(); i++) {
            c = bottom.get(i);
            
            // add rarity to card if this is a sealed card pool
            if(!customMenu.getGameType().equals(Constant.GameType.Constructed)) c.setRarity(pack.getRarity(c.getName()));

            String PC = c.getSVar("PicCount");
            Random r = new Random();
            int n = 0;
            if (PC.matches("[0-9][0-9]?"))
            	n = Integer.parseInt(PC);
            if (n > 1)
                c.setRandomPicture(r.nextInt(n));
            
            if (c.getCurSetCode().equals(""))
        	{
        		c.setCurSetCode(c.getMostRecentSet());
        		c.setImageFilename(CardUtil.buildFilename(c));
        	}

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
            
            topModel.addCard(c);
        }// for
        
        topModel.resort();
    }
    
    
    public CardShopTableModel getTopTableModel() {
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
                Gui_CardShop.this.dispose();
                exitCommand.execute();
            }
        };
        
        customMenu = new Gui_DeckEditor_Menu(this, exit);
        customMenu.setTitle("Card Shop");
        //this.setJMenuBar(customMenu);
        

        //do not change this!!!!
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent ev) {
                customMenu.close();
            }
        });
        
        setup();
        
        //show cards, makes this user friendly
        //customMenu.newConstructed();
        
        //get pricelist:
        ReadPriceList r = new ReadPriceList();
        Map<String,Long> map = r.getPriceList();
        
        ReadBoosterPack pack = new ReadBoosterPack();
        CardList shop;
        
        if (questData.getShopList() == null || questData.getShopList().size() == 0)
        {
	        shop = pack.getShopCards(questData.getWin());
	        ArrayList<String> shopListToBeSaved = new ArrayList<String>();
	        
	        for (int i = 0; i <shop.size();i++)
	        {
	        	Card crd = shop.get(i);
	        	if (map.containsKey(crd.getName())) 
	        		crd.setValue(map.get(crd.getName()));
	        	else
	        	{
	        		System.out.println("Card " + crd.getName() + " is not in the price list.");
	        		crd.setValue(10);
	        		if (crd.getRarity().equals("Common"))
	            		crd.setValue(10);
	            	else if (crd.getRarity().equals("Uncommon"))
	            		crd.setValue(50);
	            	else if (crd.getRarity().equals("Rare"))
	            		crd.setValue(200);
	        	}
	        	shopListToBeSaved.add(crd.getName());
	        }
	        questData.setShopList(shopListToBeSaved);
        }
        else //grab existing shopList
        {
        	ArrayList<String> shopList = questData.getShopList();
        	shop = new CardList();
        	
        	 for(int i = 0; i < shopList.size(); i++) {
                 Card c = AllZone.CardFactory.getCard(shopList.get(i).toString(), null);
                 c.setRarity(pack.getRarity(c.getName()));
                 if (map.containsKey(c.getName()))
                 	c.setValue(map.get(c.getName()));
                 else //card is not on pricelist
                 {
                 	System.out.println("Card " + c.getName() + " is not in the price list.");
                 	if (c.getRarity().equals("Common"))
                 		c.setValue(10);
                 	else if (c.getRarity().equals("Uncommon"))
                 		c.setValue(50);
                 	else if (c.getRarity().equals("Rare"))
                 		c.setValue(200);
                 }
                 
             	 shop.add(c);
             }
        }
        
        ArrayList<String> list = questData.getCardpool();
        CardList owned = new CardList();
        
        for(int i = 0; i < list.size(); i++) {
            Card c = AllZone.CardFactory.getCard(list.get(i).toString(), null);
            
            c.setRarity(pack.getRarity(c.getName()));
            if (map.containsKey(c.getName()))
            	c.setValue(map.get(c.getName()));
            else //card is not on pricelist
            {
            	System.out.println("Card " + c.getName() + " is not in the price list.");
            	if (c.getRarity().equals("Common"))
            		c.setValue(10);
            	else if (c.getRarity().equals("Uncommon"))
            		c.setValue(50);
            	else if (c.getRarity().equals("Rare"))
            		c.setValue(200);
            }
        	owned.add(c);
        }
       
        customMenu.populateShop(shop, owned);
        
        double multiPercent = multi*100;
        NumberFormat formatter = new DecimalFormat("#0.00");
        String maxSellingPrice = "";
        if (questData.getWin() <= 50)
        	maxSellingPrice = "     Max selling price: 500";
        sellPercentageLabel.setText("(Sell percentage: " + formatter.format(multiPercent) +"% of value)" +maxSellingPrice);
        
        topModel.sort(1, true);
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
                if(i.getWidth(null) < 300) return;
                glassPane.setVisible(true);
            }
        });
    }//addListeners()
    
    private void setup() {
    	multi = 0.20 + (0.001 *questData.getWin());
        if (multi > 0.6)
        	multi = 0.6;
        
        if (questData.getMode().equals("Fantasy"))
        {
        	if (questData.getEstatesLevel()==1)
        		multi+=0.01;
        	else if (questData.getEstatesLevel()==2)
        		multi+=0.0175;
        	else if (questData.getEstatesLevel()>=3)
        		multi+=0.025;
        }
    	
    	addListeners();
        
        //construct topTable, get all cards
        topModel = new CardShopTableModel(new CardList(), this);
        topModel.addListeners(topTable);
        
        topTable.setModel(topModel);
        topModel.resizeCols(topTable);
        
        //construct bottomModel
        bottomModel = new CardShopTableModel(this);
        bottomModel.addListeners(bottomTable);
        
        bottomTable.setModel(bottomModel);
        topModel.resizeCols(bottomTable);
        
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
    
    public Gui_CardShop(QuestData qd) {
        questData = qd;
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
        border1 = new EtchedBorder(EtchedBorder.RAISED, Color.white, new Color(148, 145, 140));
        titledBorder1 = new TitledBorder(BorderFactory.createEtchedBorder(Color.white, new Color(148, 145, 140)),
                "All Cards");
        border2 = BorderFactory.createEtchedBorder(Color.white, new Color(148, 145, 140));
        titledBorder2 = new TitledBorder(border2, "Owned Cards");
        this.getContentPane().setLayout(null);
        jScrollPane1.setBorder(titledBorder1);
        jScrollPane1.setBounds(new Rectangle(19, 20, 726, 346));
        jScrollPane2.setBorder(titledBorder2);
        jScrollPane2.setBounds(new Rectangle(19, 458, 726, 218));
        sellButton.setBounds(new Rectangle(180, 403, 146, 49));
        //removeButton.setIcon(upIcon);
        if(!Gui_NewGame.useLAFFonts.isSelected()) sellButton.setFont(new java.awt.Font("Dialog", 0, 13));
        sellButton.setText("Sell Card");
        sellButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sellButton_actionPerformed(e);
            }
        });
        buyButton.setText("Buy Card");
        buyButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                buyButton_actionPerformed(e);
            }
        });

        if(!Gui_NewGame.useLAFFonts.isSelected()) buyButton.setFont(new java.awt.Font("Dialog", 0, 13));
        buyButton.setBounds(new Rectangle(23, 403, 146, 49));
        
        detail.setBounds(new Rectangle(765, 23, 239, 323));
        picture.setBounds(new Rectangle(765, 372, 239, 338));
        picture.addMouseListener(new CustomListener());
        //Do not lower statsLabel any lower, we want this to be visible at 1024 x 768 screen size
        this.setTitle("Card Shop");
        jScrollPane3.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        jScrollPane3.setBounds(new Rectangle(6, 168, 225, 143));
        jPanel3.setBounds(new Rectangle(7, 21, 224, 141));
        jPanel3.setLayout(gridLayout1);
        gridLayout1.setColumns(1);
        gridLayout1.setRows(0);
        creditsLabel.setBounds(new Rectangle(19, 365, 720, 31));
        creditsLabel.setText("Total credits: " + questData.getCredits());
        if(!Gui_NewGame.useLAFFonts.isSelected()) creditsLabel.setFont(new java.awt.Font("Dialog", 0, 14));
        sellPercentageLabel.setBounds(new Rectangle(350, 403, 450, 31));
        sellPercentageLabel.setText("(Sell percentage: " + multi+")");
        if(!Gui_NewGame.useLAFFonts.isSelected()) sellPercentageLabel.setFont(new java.awt.Font("Dialog", 0, 14));
        jLabel1.setText("Click on the column name (like name or color) to sort the cards");
        jLabel1.setBounds(new Rectangle(20, 1, 400, 19));
        this.getContentPane().add(detail, null);
        this.getContentPane().add(picture, null);
        this.getContentPane().add(jScrollPane1, null);
        this.getContentPane().add(jScrollPane2, null);
        this.getContentPane().add(creditsLabel, null);
        this.getContentPane().add(buyButton, null);
        this.getContentPane().add(sellButton, null);
        this.getContentPane().add(sellPercentageLabel, null);
        this.getContentPane().add(jLabel1, null);
        jScrollPane2.getViewport().add(bottomTable, null);
        jScrollPane1.getViewport().add(topTable, null);
        
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
    
    void buyButton_actionPerformed(ActionEvent e) {
        int n = topTable.getSelectedRow();
        if(n != -1) {
            Card c = topModel.rowToCard(n);
            
            if(c.getValue() <= questData.getCredits())
            {
	            bottomModel.addCard(c);
	            bottomModel.resort();
	            
	            topModel.removeCard(c);
	            
	            questData.subtractCredits(c.getValue());
	            questData.addCard(c);
	            
	            questData.removeCardFromShopList(c);
	            
	            creditsLabel.setText("Total credits: " + questData.getCredits());
	            
	            //3 conditions" 0 cards left, select the same row, select next row
	            int size = topModel.getRowCount();
	            if(size != 0) {
	                if(size == n) n--;
	                topTable.addRowSelectionInterval(n, n);
	            }
            }
            else
            {
            	 JOptionPane.showMessageDialog(null, "Not enough credits!");
            }
        }//if(valid row)
    }//buyButton_actionPerformed
    
    
    void sellButton_actionPerformed(ActionEvent e) {
        
        int n = bottomTable.getSelectedRow();
        if(n != -1) {
            Card c = bottomModel.rowToCard(n);
            bottomModel.removeCard(c);
            
            topModel.addCard(c);
            topModel.resort();

            //bottomModel.removeCard(c);
            questData.addCardToShopList(c);
            
            long price = (long) (multi * c.getValue());
            if (questData.getWin() <= 50 && price > 500)
            	price = 500;

            questData.addCredits(price);
            questData.removeCard(c);
            
            creditsLabel.setText("Total credits: " + questData.getCredits());
            
            //remove sold cards from all decks:
            ArrayList<String> deckNames = questData.getDeckNames();
            for (String deckName:deckNames)
            {
            	Deck deck = questData.getDeck(deckName);
            	if (deck.getMain().contains(c.getName()))
            	{
            		//count occurences:
            		int cardPoolCount = 0;
            		ArrayList<String> cpList = questData.getCards();
            		while(cpList.contains(c.getName()))
            		{
            			cpList.remove(cpList.indexOf(c.getName()));
            			cardPoolCount++;
            		}
            		if (cardPoolCount < 4)
            			deck.removeMain(c);
            	}
            }
            
            //3 conditions" 0 cards left, select the same row, select next row
            int size = bottomModel.getRowCount();
            if(size != 0) {
                if(size == n) n--;
                bottomTable.addRowSelectionInterval(n, n);
            }
        }//if(valid row)
    }//sellButton_actionPerformed
    

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
    
    public class CustomListener extends MouseAdapter {

    }
    
}
