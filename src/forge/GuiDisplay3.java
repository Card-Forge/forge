
package forge;


import static org.jdesktop.swingx.MultiSplitLayout.*;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
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
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.jdesktop.swingx.JXMultiSplitPane;
import org.jdesktop.swingx.MultiSplitLayout.Node;

import forge.error.ErrorViewer;
import forge.gui.ForgeAction;
import forge.gui.ListChooser;
import forge.gui.game.CardDetailPanel;
import forge.gui.game.CardPanel;
import forge.gui.game.CardPicturePanel;
import forge.properties.ForgePreferences;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;


public class GuiDisplay3 extends JFrame implements CardContainer, Display, NewConstants, NewConstants.GUI.GuiDisplay, NewConstants.LANG.GuiDisplay {
    private static final long serialVersionUID = 4519302185194841060L;
    
    private GuiInput          inputControl;
    
    Font                      statFont         = new Font("Dialog", Font.PLAIN, 12);
    Font                      lifeFont         = new Font("Dialog", Font.PLAIN, 40);
    Font                      checkboxFont     = new Font("Dialog", Font.PLAIN, 9);

    
    public static Color       greenColor               = new Color(0, 164, 0);
    
    private Action            HUMAN_GRAVEYARD_ACTION;
    private Action            HUMAN_REMOVED_ACTION;
    private Action            HUMAN_FLASHBACK_ACTION;
    private Action            COMPUTER_GRAVEYARD_ACTION;
    private Action            COMPUTER_REMOVED_ACTION;
    private Action            CONCEDE_ACTION;
    public Card               cCardHQ;
    
    //private CardList multiBlockers = new CardList();
    
    public GuiDisplay3() {
        setupActions();
        initComponents();
        
        addObservers();
        addListeners();
        addMenu();
        inputControl = new GuiInput();
    }
    
    @Override
    public void setVisible(boolean visible) {
        if(visible) {
            //causes an error if put in the constructor, causes some random null pointer exception
            AllZone.InputControl.updateObservers();
            
            //Use both so that when "un"maximizing, the frame isn't tiny
            setSize(1024, 740);
            System.out.println(getExtendedState());
            setExtendedState(Frame.MAXIMIZED_BOTH);
            System.out.println(getExtendedState());
        }
        super.setVisible(visible);
    }
    
    public void assignDamage(Card attacker, CardList blockers, int damage) {
        new Gui_MultipleBlockers3(attacker, blockers, damage, this);
    }
    
    /*
    public void addAssignDamage(Card attacker, Card blocker, int damage)
    {
    	multiBlockers.add(blocker);
    }
    
    public void addAssignDamage(Card attacker, int damage) {
        //new Gui_MultipleBlockers3(attacker, blockers, damage, this);
    	new Gui_MultipleBlockers3(attacker, multiBlockers, damage, this);
    }
    */

    private void setupActions() {
        HUMAN_GRAVEYARD_ACTION = new ZoneAction(AllZone.Human_Graveyard, HUMAN_GRAVEYARD);
        HUMAN_REMOVED_ACTION = new ZoneAction(AllZone.Human_Exile, HUMAN_REMOVED);
        HUMAN_FLASHBACK_ACTION = new ZoneAction(AllZone.Human_Exile, HUMAN_FLASHBACK) {
            
            private static final long serialVersionUID = 8120331222693706164L;
            
            @Override
            protected Card[] getCards() {
                return CardFactoryUtil.getGraveyardActivationCards(AllZone.HumanPlayer).toArray();
            }
            
            @Override
            protected void doAction(Card c) {
            	if(!c.isLand())
            	{
	                SpellAbility[] sa = c.getSpellAbility();
	                sa[1].setActivatingPlayer(AllZone.HumanPlayer);
	                if(sa[1].canPlay() && !c.isUnCastable()) AllZone.GameAction.playSpellAbility(sa[1]);
            	}
            	else if (CardFactoryUtil.canHumanPlayLand())
            		GameAction.playLand(c, AllZone.Human_Graveyard);
            }
        };
        COMPUTER_GRAVEYARD_ACTION = new ZoneAction(AllZone.Computer_Graveyard, COMPUTER_GRAVEYARD);
        COMPUTER_REMOVED_ACTION = new ZoneAction(AllZone.Computer_Exile, COMPUTER_REMOVED);
        CONCEDE_ACTION = new ConcedeAction();
    }
    
    private void addMenu() {
        Object[] obj = {
                HUMAN_GRAVEYARD_ACTION, HUMAN_REMOVED_ACTION, HUMAN_FLASHBACK_ACTION, COMPUTER_GRAVEYARD_ACTION,
                COMPUTER_REMOVED_ACTION, new JSeparator(), 
                GuiDisplay3.playsoundCheckboxForMenu, new JSeparator(), ErrorViewer.ALL_THREADS_ACTION,
                CONCEDE_ACTION};
        
        JMenu gameMenu = new JMenu(ForgeProps.getLocalized(MENU_BAR.MENU.TITLE));
        for(Object o:obj) {
            if(o instanceof ForgeAction) gameMenu.add(((ForgeAction) o).setupButton(new JMenuItem()));
            else if(o instanceof Action) gameMenu.add((Action) o);
            else if(o instanceof Component) gameMenu.add((Component) o);
            else throw new AssertionError();
        }
        
        JMenu gamePhases = new JMenu(ForgeProps.getLocalized(MENU_BAR.PHASE.TITLE)); 
        
        JMenuItem aiLabel = new JMenuItem("Computer");
        JMenuItem humanLabel = new JMenuItem("Human");
        
        Component[] objPhases = { aiLabel, GuiDisplay4.cbAIUpkeep, GuiDisplay4.cbAIDraw, GuiDisplay4.cbAIBeginCombat, 
				GuiDisplay4.cbAIEndCombat, GuiDisplay4.cbAIEndOfTurn, new JSeparator(), 
				humanLabel, GuiDisplay4.cbHumanUpkeep, GuiDisplay4.cbHumanDraw, GuiDisplay4.cbHumanBeginCombat, 
				GuiDisplay4.cbHumanEndCombat, GuiDisplay4.cbHumanEndOfTurn };
        
        for(Component cmp:objPhases) {
            gamePhases.add(cmp);
        }

        
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(gameMenu);
        menuBar.add(gamePhases);
        menuBar.add(new MenuItem_HowToPlay());
        this.setJMenuBar(menuBar);
    }//addMenu()
    
    public MyButton getButtonOK() {
        MyButton ok = new MyButton() {
            public void select() {
                inputControl.selectButtonOK();
            }
            
            public boolean isSelectable() {
                return okButton.isEnabled();
            }
            
            public void setSelectable(boolean b) {
                okButton.setEnabled(b);
            }
            
            public String getText() {
                return okButton.getText();
            }
            
            public void setText(String text) {
                okButton.setText(text);
            }
            
            public void reset() {
                okButton.setText("OK");
            }
        };
        
        return ok;
    }//getButtonOK()
    
    public MyButton getButtonCancel() {
        MyButton cancel = new MyButton() {
            public void select() {
                inputControl.selectButtonCancel();
            }
            
            public boolean isSelectable() {
                return cancelButton.isEnabled();
            }
            
            public void setSelectable(boolean b) {
                cancelButton.setEnabled(b);
            }
            
            public String getText() {
                return cancelButton.getText();
            }
            
            public void setText(String text) {
                cancelButton.setText(text);
            }
            
            public void reset() {
                cancelButton.setText("Cancel");
            }
        };
        return cancel;
    }//getButtonCancel()
    
    public void showCombat(String message) {
        combatArea.setText(message);
    }
    
    public void showMessage(String s) {
    	messageArea.setText(s);
        
        messageArea.setText(s);
        Border border = new EmptyBorder(1, 1, 1, 1);
            
        messageArea.setBorder(border);
            
        int thickness = 3;
            
        if (s.contains("Main"))
        	border = BorderFactory.createLineBorder(new Color(30, 0, 255), thickness);	
        else if (s.contains("To Block"))
        	border = BorderFactory.createLineBorder(new Color(13, 179, 0), thickness);
        else if (s.contains("Play Instants and Abilities") || s.contains("Combat") || s.contains("Damage"))
           	border = BorderFactory.createLineBorder(new Color(255, 174, 0), thickness);
        else if (s.contains("Declare Attackers"))
           	border = BorderFactory.createLineBorder(new Color(255, 0, 0), thickness);
        else if (s.contains("Upkeep") || s.contains("Draw") || s.contains("End of Turn"))
        	border = BorderFactory.createLineBorder(new Color(200, 0, 170), thickness);
            
         messageArea.setBorder(border);
    }
    
    //returned Object could be null
    public <T> T getChoiceOptional(String message, T... choices) {
        if(choices == null || choices.length == 0) return null;
        List<T> choice = getChoices(message, 0, 1, choices);
        return choice.isEmpty()? null:choice.get(0);
    }//getChoiceOptional()
    
    // returned Object will never be null
    public <T> T getChoice(String message, T... choices) {
        List<T> choice = getChoices(message, 1, 1, choices);
        assert choice.size() == 1;
        return choice.get(0);
    }//getChoice()
    
    // returned Object will never be null
    public <T> List<T> getChoicesOptional(String message, T... choices) {
        return getChoices(message, 0, choices.length, choices);
    }//getChoice()
    
    // returned Object will never be null
    public <T> List<T> getChoices(String message, T... choices) {
        return getChoices(message, 1, choices.length, choices);
    }//getChoice()
    
    // returned Object will never be null
    public <T> List<T> getChoices(String message, int min, int max, T... choices) {
        ListChooser<T> c = new ListChooser<T>(message, min, max, choices);
        final JList list = c.getJList();
        list.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent ev) {
                if(list.getSelectedValue() instanceof Card) {
                    setCard((Card) list.getSelectedValue());
                }
            }
        });
        c.show();
        return c.getSelectedValues();
    }//getChoice()
    
    private void addListeners() {
        //mouse Card Detail
        playerHandPanel.addMouseMotionListener(GuiDisplayUtil.getCardDetailMouse(this));
        playerLandPanel.addMouseMotionListener(GuiDisplayUtil.getCardDetailMouse(this));
        playerCreaturePanel.addMouseMotionListener(GuiDisplayUtil.getCardDetailMouse(this));
        
        oppLandPanel.addMouseMotionListener(GuiDisplayUtil.getCardDetailMouse(this));
        oppCreaturePanel.addMouseMotionListener(GuiDisplayUtil.getCardDetailMouse(this));
        

        //opponent life mouse listener
        oppLifeLabel.addMouseListener(new MouseAdapter() {
            
            @Override
            public void mousePressed(MouseEvent e) {
                inputControl.selectPlayer(AllZone.ComputerPlayer);
            }
        });
        
        //self life mouse listener
        playerLifeLabel.addMouseListener(new MouseAdapter() {
            
            @Override
            public void mousePressed(MouseEvent e) {
                inputControl.selectPlayer(AllZone.HumanPlayer);
            }
        });
        
        //self play (land) ---- Mouse
        playerLandPanel.addMouseListener(new MouseAdapter() {
            
            @Override
            public void mousePressed(MouseEvent e) {
                Object o = playerLandPanel.getComponentAt(e.getPoint());
                if(o instanceof CardPanel) {
                    CardPanel cardPanel = (CardPanel) o;
                    
                    if(cardPanel.getCard().isUntapped()) {
                        MP3Player mp3 = new MP3Player("tap.mp3");
                        mp3.play();
                    }
                    
                    if(cardPanel.getCard().isTapped()
                            && (inputControl.input instanceof Input_PayManaCost || inputControl.input instanceof Input_PayManaCost_Ability)) {
                        
                        while(cardPanel.connectedCard != null) {
                            cardPanel = cardPanel.connectedCard;
                            
                            if(cardPanel.getCard().isUntapped()) {
                                
                                break;
                            }
                        }
                    }
                    
                    inputControl.selectCard(cardPanel.getCard(), AllZone.Human_Battlefield);
                    
                }
            }
        });
        //self play (no land) ---- Mouse
        playerCreaturePanel.addMouseListener(new MouseAdapter() {
            
            @Override
            public void mousePressed(MouseEvent e) {
                Object o = playerCreaturePanel.getComponentAt(e.getPoint());
                if(o instanceof CardPanel) {
                    CardPanel cardPanel = (CardPanel) o;
                    
                    CardList att = new CardList(AllZone.Combat.getAttackers());
                    //CardList block = AllZone.Combat.getAllBlockers();
                    
                    if((cardPanel.getCard().isTapped() || cardPanel.getCard().hasSickness() || ((cardPanel.getCard().getKeyword().contains("Vigilance")) && att.contains(cardPanel.getCard())))
                            && (inputControl.input instanceof Input_Attack)) {
                        while(cardPanel.connectedCard != null) {
                            cardPanel = cardPanel.connectedCard;
                            if(cardPanel.getCard().isUntapped() && !cardPanel.getCard().hasSickness()) {
                                break;
                            }
                        }
                    }
                    //right click:
                    if(e.isMetaDown()) {
                        if(att.contains(cardPanel.getCard()) && (inputControl.input instanceof Input_Attack))  {
                            cardPanel.getCard().untap();
                            AllZone.Combat.removeFromCombat(cardPanel.getCard());
                        }
                        
                        /*
                        // won't work yet:
                        else if (block.contains(cardPanel.getCard()) && inputControl.input instanceof Input_Block)
                        {
                        	Card crd = cardPanel.getCard();
                        	AllZone.Combat.removeFromCombat(crd);
                        }
                        */
                        
                    }

                    else inputControl.selectCard(cardPanel.getCard(), AllZone.Human_Battlefield);
                }
            }
        });
        //self hand ---- Mouse
        playerHandPanel.addMouseListener(new MouseAdapter() {
            
            @Override
            public void mousePressed(MouseEvent e) {
                Object o = playerHandPanel.getComponentAt(e.getPoint());
                if(o instanceof CardPanel) {
                    CardContainer cardPanel = (CardContainer) o;
                    inputControl.selectCard(cardPanel.getCard(), AllZone.Human_Hand);
                    okButton.requestFocusInWindow();
                }
            }
        });
        
        //*****************************************************************
        //computer
        
        //computer play (land) ---- Mouse
        oppLandPanel.addMouseListener(new MouseAdapter() {
            
            @Override
            public void mousePressed(MouseEvent e) {
                Object o = oppLandPanel.getComponentAt(e.getPoint());
                if(o instanceof CardPanel) {
                    CardContainer cardPanel = (CardContainer) o;
                    inputControl.selectCard(cardPanel.getCard(), AllZone.Computer_Battlefield);
                }
            }
        });
        
        //computer play (no land) ---- Mouse
        oppCreaturePanel.addMouseListener(new MouseAdapter() {
            
            @Override
            public void mousePressed(MouseEvent e) {
                Object o = oppCreaturePanel.getComponentAt(e.getPoint());
                if(o instanceof CardPanel) {
                    CardContainer cardPanel = (CardContainer) o;
                    inputControl.selectCard(cardPanel.getCard(), AllZone.Computer_Battlefield);
                }
            }
        });
        

    }//addListener()
    
    public Card getCard() {
        return detail.getCard();
    }
    
    public void setCard(Card card) {
        detail.setCard(card);
        picture.setCard(card);
    }
    
    private void addObservers() {
        //Human Hand, Graveyard, and Library totals
        {//make sure to not interfer with anything below, since this is a very long method
            Observer o = new Observer() {
                public void update(Observable a, Object b) {
                    playerHandValue.setText("" + AllZone.Human_Hand.getCards().length);
                    playerGraveValue.setText("" + AllZone.Human_Graveyard.getCards().length);
                    playerLibraryValue.setText("" + AllZone.Human_Library.getCards().length);
                    playerFBValue.setText("" + CardFactoryUtil.getGraveyardActivationCards(AllZone.HumanPlayer).size());
                    playerRemovedValue.setText("" + AllZone.Human_Exile.getCards().length);
                    
                }
            };
            AllZone.Human_Hand.addObserver(o);
            AllZone.Human_Graveyard.addObserver(o);
            AllZone.Human_Library.addObserver(o);
        }
        
        //opponent Hand, Graveyard, and Library totals
        {//make sure to not interfer with anything below, since this is a very long method
            Observer o = new Observer() {
                public void update(Observable a, Object b) {
                    oppHandValue.setText("" + AllZone.Computer_Hand.getCards().length);
                    oppGraveValue.setText("" + AllZone.Computer_Graveyard.getCards().length);
                    oppLibraryValue.setText("" + AllZone.Computer_Library.getCards().length);
                    oppRemovedValue.setText("" + AllZone.Computer_Exile.getCards().length);
                }
            };
            AllZone.Computer_Hand.addObserver(o);
            AllZone.Computer_Graveyard.addObserver(o);
            AllZone.Computer_Library.addObserver(o);
        }
        

        //opponent life
        oppLifeLabel.setText("" + AllZone.ComputerPlayer.getLife());
        AllZone.ComputerPlayer.addObserver(new Observer() {
            public void update(Observable a, Object b) {
                int life = AllZone.ComputerPlayer.getLife();
                oppLifeLabel.setText("" + life);
            }
        });
        AllZone.ComputerPlayer.updateObservers();
        
        if (AllZone.QuestData != null) {
        	File base = ForgeProps.getFile(IMAGE_ICON);
        	String iconName = "";
        	if (Constant.Quest.oppIconName[0] != null) {
        		iconName = Constant.Quest.oppIconName[0];
	        	File file = new File(base, iconName);
	        	ImageIcon icon = new ImageIcon(file.toString());
	        	oppIconLabel.setIcon(icon);
	        	oppIconLabel.setAlignmentX(100);
	        	
        	}
        }
        
        oppPCLabel.setText("Poison Counters: " + AllZone.ComputerPlayer.getPoisonCounters());
        AllZone.ComputerPlayer.addObserver(new Observer() {
            public void update(Observable a, Object b) {
                int pcs = AllZone.ComputerPlayer.getPoisonCounters();
                oppPCLabel.setText("Poison Counters: " + pcs);
            }
        });
        AllZone.ComputerPlayer.updateObservers();
        
        //player life
        playerLifeLabel.setText("" + AllZone.HumanPlayer.getLife());
        AllZone.HumanPlayer.addObserver(new Observer() {
            public void update(Observable a, Object b) {
                int life = AllZone.HumanPlayer.getLife();
                playerLifeLabel.setText("" + life);
            }
        });
        AllZone.HumanPlayer.updateObservers();
        
        playerPCLabel.setText("Poison Counters: " + AllZone.HumanPlayer.getPoisonCounters());
        AllZone.HumanPlayer.addObserver(new Observer() {
            public void update(Observable a, Object b) {
                int pcs = AllZone.HumanPlayer.getPoisonCounters();
                playerPCLabel.setText("Poison Counters: " + pcs);
            }
        });
        AllZone.HumanPlayer.updateObservers();
        
        //stack
        AllZone.Stack.addObserver(new Observer() {
            public void update(Observable a, Object b) {
                stackPanel.removeAll();
                MagicStack stack = AllZone.Stack;
                int count = 1;
                JLabel label;
                
                for(int i = stack.size() - 1; 0 <= i; i--) {
                    label = new JLabel("" + (count++) + ". " + stack.peek(i).getStackDescription());
                    

                    //update card detail
                    final CardPanel cardPanel = new CardPanel(stack.peek(i).getSourceCard());
                    cardPanel.setLayout(new BorderLayout());
                    cardPanel.add(label);
                    cardPanel.addMouseMotionListener(new MouseMotionAdapter() {
                        
                        @Override
                        public void mouseMoved(MouseEvent me) {
                            setCard(cardPanel.getCard());
                        }//mouseMoved
                    });
                    
                    stackPanel.add(cardPanel);
                }
                
                stackPanel.revalidate();
                stackPanel.repaint();
                
                okButton.requestFocusInWindow();
                
            }
        });
        AllZone.Stack.updateObservers();
        //END, stack
        

        //self hand
        AllZone.Human_Hand.addObserver(new Observer() {
            public void update(Observable a, Object b) {
                PlayerZone pZone = (PlayerZone) a;
                JPanel p = playerHandPanel;
                p.removeAll();
                
                Card c[] = pZone.getCards();
                JPanel panel;
                for(int i = 0; i < c.length; i++) {
                    panel = new CardPanel(c[i]);
                    p.add(panel);
                }
                
                p.revalidate();
                p.repaint();
            }
        });
        AllZone.Human_Hand.updateObservers();
        //END, self hand
        
        //self play (land)
        AllZone.Human_Battlefield.addObserver(new Observer() {
            public void update(Observable a, Object b) {
                //PlayerZone pZone = (PlayerZone) a; //unused
                JPanel p = playerLandPanel;
                p.removeAll();
                
                GuiDisplayUtil.setupLandPanel(p, AllZone.Human_Battlefield.getCards());
                p.revalidate();
                p.repaint();
            }
        });
        AllZone.Human_Battlefield.updateObservers();
        //END - self play (only land)
        

        //self play (no land)
        AllZone.Human_Battlefield.addObserver(new Observer() {
            public void update(Observable a, Object b) {
                //PlayerZone pZone = (PlayerZone) a; //unused
                JPanel p = playerCreaturePanel;
                p.removeAll();
                
                GuiDisplayUtil.setupNoLandPanel(p, AllZone.Human_Battlefield.getCards());
                p.revalidate();
                p.repaint();
            }
        });
        AllZone.Human_Battlefield.updateObservers();
        //END - self play (no land)
        

        //computer play (no land)
        AllZone.Computer_Battlefield.addObserver(new Observer() {
            public void update(Observable a, Object b) {
                //PlayerZone pZone = (PlayerZone) a; //unused
                JPanel p = oppCreaturePanel;
                p.removeAll();
                
                GuiDisplayUtil.setupNoLandPanel(p, AllZone.Computer_Battlefield.getCards());
                
                p.revalidate();
                p.repaint();
            }
        });
        AllZone.Computer_Battlefield.updateObservers();
        //END - computer play (no land)
        
        //computer play (land)
        AllZone.Computer_Battlefield.addObserver(new Observer() {
            public void update(Observable a, Object b) {
                //PlayerZone pZone = (PlayerZone) a; //unused
                JPanel p = oppLandPanel;
                p.removeAll();
                
                GuiDisplayUtil.setupLandPanel(p, AllZone.Computer_Battlefield.getCards());
                p.revalidate();
                p.repaint();
            }
        });
        AllZone.Computer_Battlefield.updateObservers();
        //END - computer play (only land)
        
    }//addObservers()
    
    private void initComponents() {
        //Preparing the Frame
        setTitle(ForgeProps.getLocalized(LANG.PROGRAM_NAME));
        if(!Gui_NewGame.useLAFFonts.isSelected()) setFont(new Font("Times New Roman", 0, 16));
        getContentPane().setLayout(new BorderLayout());
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent evt) {
                concede();
            }
            
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
        try {
            XMLDecoder decoder = new XMLDecoder(new BufferedInputStream(new FileInputStream(f)));
            model = (Node) decoder.readObject();
            decoder.close();
            pane.getMultiSplitLayout().setModel(model);
            //pane.getMultiSplitLayout().setFloatingDividers(false);
        } catch(Exception ex) {
            model = parseModel(""//
                    + "(ROW "//
                    + "(COLUMN"//
                    + " (LEAF weight=0.2 name=info)"//
                    + " (LEAF weight=0.2 name=compy)"//
                    + " (LEAF weight=0.2 name=stack)"//
                    + " (LEAF weight=0.2 name=combat)"//
                    + " (LEAF weight=0.2 name=human)) "//
                    + "(COLUMN weight=1"//
                    + " (LEAF weight=0.2 name=compyLand)"//
                    + " (LEAF weight=0.2 name=compyPlay)"//
                    + " (LEAF weight=0.2 name=humanPlay)"//
                    + " (LEAF weight=0.2 name=humanLand)"//
                    + " (LEAF weight=0.2 name=humanHand)) "//
                    + "(COLUMN"//
                    + " (LEAF weight=0.5 name=detail)"//
                    + " (LEAF weight=0.5 name=picture)))");
            pane.setModel(model);
        }
        pane.getMultiSplitLayout().setFloatingDividers(false);
        getContentPane().add(pane);
        
        //adding the individual parts
        
        if(!Gui_NewGame.useLAFFonts.isSelected()) initFonts(pane);
        
        initMsgYesNo(pane);
        initOpp(pane);
        initStackCombat(pane);
        initPlayer(pane);
        initZones(pane);
        initCardPicture(pane);
    }
    
    private void initFonts(JPanel pane) {
        messageArea.setFont(getFont());
        
        oppLifeLabel.setFont(lifeFont);
        
        oppPCLabel.setFont(statFont);
        oppLibraryLabel.setFont(statFont);
        
        oppHandValue.setFont(statFont);
        oppLibraryValue.setFont(statFont);
        oppRemovedValue.setFont(statFont);
        oppGraveValue.setFont(statFont);
        
        playerLifeLabel.setFont(lifeFont);
        playerPCLabel.setFont(statFont);
        
        playerHandValue.setFont(statFont);
        playerLibraryValue.setFont(statFont);
        playerRemovedValue.setFont(statFont);
        playerGraveValue.setFont(statFont);
        playerFBValue.setFont(statFont);
        
        combatArea.setFont(getFont());
    }
    
    private void initMsgYesNo(JPanel pane) {
//        messageArea.setBorder(BorderFactory.createEtchedBorder());
        messageArea.setEditable(false);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        
        cancelButton.setText("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                cancelButtonActionPerformed(evt);
                okButton.requestFocusInWindow();
            }
        });
        okButton.setText("OK");
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                okButtonActionPerformed(evt);
                
                if(AllZone.Phase.isNeedToNextPhase()) {
                    //for debugging: System.out.println("There better be no nextPhase in the stack.");
                    AllZone.Phase.setNeedToNextPhase(false);
                    AllZone.Phase.nextPhase();
                }
                okButton.requestFocusInWindow();
            }
        });
        okButton.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent arg0) {
                // TODO make triggers on escape
                int code = arg0.getKeyCode();
                if(code == KeyEvent.VK_ESCAPE) {
                    cancelButton.doClick();
                }
            }
        });
        
        okButton.requestFocusInWindow();
        
        //if(okButton.isEnabled())
        //okButton.doClick();
        JPanel yesNoPanel = new JPanel(new FlowLayout());
        yesNoPanel.setBorder(new EtchedBorder());
        yesNoPanel.add(cancelButton);
        yesNoPanel.add(okButton);
        
        JPanel panel = new JPanel(new BorderLayout());
        JScrollPane scroll = new JScrollPane(messageArea);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        panel.add(scroll);
        panel.add(yesNoPanel, BorderLayout.SOUTH);
        pane.add(new ExternalPanel(panel), "info");
    }
    
    private void initOpp(JPanel pane) {
        //oppLifeLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        //oppPCLabel.setHorizontalAlignment(SwingConstants.TOP);
        oppPCLabel.setForeground(greenColor);
        
        JLabel oppHandLabel = new JLabel(ForgeProps.getLocalized(COMPUTER_HAND.TITLE), SwingConstants.TRAILING);
        if(!Gui_NewGame.useLAFFonts.isSelected()) oppHandLabel.setFont(statFont);
        
        //JLabel oppGraveLabel = new JLabel("Grave:", SwingConstants.TRAILING);
        JButton oppGraveButton = new JButton(COMPUTER_GRAVEYARD_ACTION);
        oppGraveButton.setText((String) COMPUTER_GRAVEYARD_ACTION.getValue("buttonText"));
        oppGraveButton.setMargin(new Insets(0, 0, 0, 0));
        oppGraveButton.setHorizontalAlignment(SwingConstants.TRAILING);
        if(!Gui_NewGame.useLAFFonts.isSelected()) oppGraveButton.setFont(statFont);
        

        JPanel gravePanel = new JPanel(new BorderLayout());
        gravePanel.add(oppGraveButton, BorderLayout.EAST);
        
        JButton oppRemovedButton = new JButton(COMPUTER_REMOVED_ACTION);
        oppRemovedButton.setText((String) COMPUTER_REMOVED_ACTION.getValue("buttonText"));
        oppRemovedButton.setMargin(new Insets(0, 0, 0, 0));
        //removedButton.setHorizontalAlignment(SwingConstants.TRAILING);
        if(!Gui_NewGame.useLAFFonts.isSelected()) oppRemovedButton.setFont(statFont);
        

        oppHandValue.setHorizontalAlignment(SwingConstants.LEADING);
        oppLibraryValue.setHorizontalAlignment(SwingConstants.LEADING);
        oppGraveValue.setHorizontalAlignment(SwingConstants.LEADING);
        oppRemovedValue.setHorizontalAlignment(SwingConstants.LEADING);
        
        JPanel oppNumbersPanel = new JPanel(new GridLayout(0, 2, 3, 1));
        oppNumbersPanel.add(oppHandLabel);
        oppNumbersPanel.add(oppHandValue);
        oppNumbersPanel.add(oppRemovedButton);
        oppNumbersPanel.add(oppRemovedValue);
        oppNumbersPanel.add(oppLibraryLabel);
        oppNumbersPanel.add(oppLibraryValue);
        oppNumbersPanel.add(gravePanel);
        oppNumbersPanel.add(oppGraveValue);
        
        oppLifeLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        JPanel oppIconLifePanel = new JPanel(new GridLayout(0, 1, 0, 0));
        oppIconLifePanel.add(oppIconLabel);
        oppIconLifePanel.add(oppLifeLabel);
        
        JPanel oppPanel = new JPanel();
        oppPanel.setBorder(new TitledBorder(new EtchedBorder(), ForgeProps.getLocalized(COMPUTER_TITLE)));
        oppPanel.setLayout(new BorderLayout());
        oppPanel.add(oppNumbersPanel, BorderLayout.WEST);
        // oppPanel.add(oppIconLabel, BorderLayout.CENTER);
        // oppPanel.add(oppLifeLabel, BorderLayout.EAST);
        oppPanel.add(oppIconLifePanel, BorderLayout.EAST);
        oppPanel.add(oppPCLabel, BorderLayout.AFTER_LAST_LINE);
        pane.add(new ExternalPanel(oppPanel), "compy");
    }
    
    private void initStackCombat(JPanel pane) {
        stackPanel.setLayout(new GridLayout(0, 1, 10, 10));
        JScrollPane stackPane = new JScrollPane(stackPanel);
        stackPane.setBorder(new EtchedBorder());
        pane.add(new ExternalPanel(stackPane), "stack");
        
        combatArea.setEditable(false);
        combatArea.setLineWrap(true);
        combatArea.setWrapStyleWord(true);
        
        JScrollPane combatPane = new JScrollPane(combatArea);
        
        combatPane.setBorder(new TitledBorder(new EtchedBorder(), ForgeProps.getLocalized(COMBAT)));
        pane.add(new ExternalPanel(combatPane), "combat");
    }
    
    private void initPlayer(JPanel pane) {
        //int fontSize = 12;
        playerLifeLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        playerPCLabel.setForeground(greenColor);
        
        JLabel playerLibraryLabel = new JLabel(ForgeProps.getLocalized(HUMAN_LIBRARY.TITLE),
                SwingConstants.TRAILING);
        if(!Gui_NewGame.useLAFFonts.isSelected()) playerLibraryLabel.setFont(statFont);
        
        JLabel playerHandLabel = new JLabel(ForgeProps.getLocalized(HUMAN_HAND.TITLE), SwingConstants.TRAILING);
        if(!Gui_NewGame.useLAFFonts.isSelected()) playerHandLabel.setFont(statFont);
        
        //JLabel playerGraveLabel = new JLabel("Grave:", SwingConstants.TRAILING);
        JButton playerGraveButton = new JButton(HUMAN_GRAVEYARD_ACTION);
        playerGraveButton.setText((String) HUMAN_GRAVEYARD_ACTION.getValue("buttonText"));
        playerGraveButton.setMargin(new Insets(0, 0, 0, 0));
        playerGraveButton.setHorizontalAlignment(SwingConstants.TRAILING);
        if(!Gui_NewGame.useLAFFonts.isSelected()) playerGraveButton.setFont(statFont);
        

        JButton playerFlashBackButton = new JButton(HUMAN_FLASHBACK_ACTION);
        playerFlashBackButton.setText((String) HUMAN_FLASHBACK_ACTION.getValue("buttonText"));
        playerFlashBackButton.setMargin(new Insets(0, 0, 0, 0));
        playerFlashBackButton.setHorizontalAlignment(SwingConstants.TRAILING);
        if(!Gui_NewGame.useLAFFonts.isSelected()) playerFlashBackButton.setFont(statFont);
        

        JPanel gravePanel = new JPanel(new BorderLayout());
        gravePanel.add(playerGraveButton, BorderLayout.EAST);
        
        JPanel playerFBPanel = new JPanel(new BorderLayout());
        playerFBPanel.add(playerFlashBackButton, BorderLayout.EAST);
        
        JButton playerRemovedButton = new JButton(HUMAN_REMOVED_ACTION);
        playerRemovedButton.setText((String) HUMAN_REMOVED_ACTION.getValue("buttonText"));
        playerRemovedButton.setMargin(new Insets(0, 0, 0, 0));
        //removedButton.setHorizontalAlignment(SwingConstants.TRAILING);
        if(!Gui_NewGame.useLAFFonts.isSelected()) playerRemovedButton.setFont(statFont);
        
        playerHandValue.setHorizontalAlignment(SwingConstants.LEADING);
        playerLibraryValue.setHorizontalAlignment(SwingConstants.LEADING);
        playerGraveValue.setHorizontalAlignment(SwingConstants.LEADING);
        playerFBValue.setHorizontalAlignment(SwingConstants.LEADING);
        
        //playerRemovedValue.setFont(new Font("MS Sans Serif", 0, fontSize));
        playerRemovedValue.setHorizontalAlignment(SwingConstants.LEADING);
        
        JPanel playerNumbersPanel = new JPanel(new GridLayout(0, 2, 5, 1));
        playerNumbersPanel.add(playerHandLabel);
        playerNumbersPanel.add(playerHandValue);
        playerNumbersPanel.add(playerRemovedButton);
        playerNumbersPanel.add(playerRemovedValue);
        playerNumbersPanel.add(playerLibraryLabel);
        playerNumbersPanel.add(playerLibraryValue);
        playerNumbersPanel.add(gravePanel);
        playerNumbersPanel.add(playerGraveValue);
        playerNumbersPanel.add(playerFBPanel);
        playerNumbersPanel.add(playerFBValue);
        
        JPanel playerPanel = new JPanel();
        playerPanel.setBorder(new TitledBorder(new EtchedBorder(), ForgeProps.getLocalized(HUMAN_TITLE)));
        playerPanel.setLayout(new BorderLayout());
        playerPanel.add(playerNumbersPanel, BorderLayout.WEST);
        playerPanel.add(playerLifeLabel, BorderLayout.EAST);
        playerPanel.add(playerPCLabel, BorderLayout.AFTER_LAST_LINE);
        pane.add(new ExternalPanel(playerPanel), "human");
    }
    
    private void initZones(JPanel pane) {
        JPanel[] zones = {oppLandPanel, oppCreaturePanel, playerCreaturePanel, playerLandPanel, playerHandPanel};
        String[] names = {"compyLand", "compyPlay", "humanPlay", "humanLand", "humanHand"};
        for(int i = 0; i < names.length; i++) {
            zones[i].setLayout(null);
            zones[i].setBorder(BorderFactory.createEtchedBorder());
            Dimension d = zones[i].getPreferredSize();
            d.height = 100;
            zones[i].setPreferredSize(d);
            pane.add(new ExternalPanel(new JScrollPane(zones[i])), names[i]);
        }
        playerHandPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
    }
    
    private void initCardPicture(JPanel pane) {
        pane.add(new ExternalPanel(detail), "detail");
        pane.add(new ExternalPanel(picture), "picture");
    }
    
    private void cancelButtonActionPerformed(ActionEvent evt) {
        inputControl.selectButtonCancel();
    }
    
    private void okButtonActionPerformed(ActionEvent evt) {
        inputControl.selectButtonOK();
    }
    
    /**
     * Exit the Application
     */
    private void concede() {
    	savePhases();
        dispose();
        Constant.Runtime.WinLose.addLose();
        if (!Constant.Quest.fantasyQuest[0])
        	new Gui_WinLose();
        else {
        	//new Gui_WinLose(Constant.Quest.humanList[0], Constant.Quest.computerList[0],Constant.Quest.humanLife[0], Constant.Quest.computerLife[0]);
        	CardList humanList = QuestUtil.getHumanPlantAndPet(AllZone.QuestData, AllZone.QuestAssignment);
    		CardList computerList = QuestUtil.getComputerCreatures(AllZone.QuestData, AllZone.QuestAssignment);
    		
    		int humanLife = QuestUtil.getLife(AllZone.QuestData);
    		int computerLife = 20;
    		
    		if (AllZone.QuestAssignment!=null)
    			computerLife = AllZone.QuestAssignment.getComputerLife();
    		new Gui_WinLose(humanList, computerList, humanLife, computerLife);
        }
    }
    
    // ********** Phase stuff in Display ******************
    public boolean stopAtPhase(Player turn, String phase) {
    	if (turn.isComputer()){
    		if (phase.equals(Constant.Phase.End_Of_Turn))
    			return cbAIEndOfTurn.isSelected();
    		else if (phase.equals(Constant.Phase.Upkeep))
    			return cbAIUpkeep.isSelected();
    		else if (phase.equals(Constant.Phase.Draw))
    			return cbAIDraw.isSelected();
    		else if (phase.equals(Constant.Phase.Combat_Begin))
    			return cbAIBeginCombat.isSelected();
    		else if (phase.equals(Constant.Phase.Combat_End))
    			return cbAIEndCombat.isSelected();
    	}
    	else{
    		if (phase.equals(Constant.Phase.End_Of_Turn))
    			return cbHumanEndOfTurn.isSelected();
    		else if (phase.equals(Constant.Phase.Upkeep))
    			return cbHumanUpkeep.isSelected();
    		else if (phase.equals(Constant.Phase.Draw))
    			return cbHumanDraw.isSelected();
    		else if (phase.equals(Constant.Phase.Combat_Begin))
    			return cbHumanBeginCombat.isSelected();
    		else if (phase.equals(Constant.Phase.Combat_End))
    			return cbHumanEndCombat.isSelected();
    	}
    	return false;
    }
    
    public boolean loadPhases(){
    	ForgePreferences fp = Gui_NewGame.preferences;

    	cbAIUpkeep.setSelected(fp.bAIUpkeep);
    	cbAIDraw.setSelected(fp.bAIDraw);
    	cbAIEndOfTurn.setSelected(fp.bAIEOT);
		cbAIBeginCombat.setSelected(fp.bAIBeginCombat);
		cbAIEndCombat.setSelected(fp.bAIEndCombat);
    	
    	cbHumanUpkeep.setSelected(fp.bHumanUpkeep);
    	cbHumanDraw.setSelected(fp.bHumanDraw);
    	cbHumanEndOfTurn.setSelected(fp.bHumanEOT);
		cbHumanBeginCombat.setSelected(fp.bHumanBeginCombat);
		cbHumanEndCombat.setSelected(fp.bHumanEndCombat);

    	return true;
    }
    
    public boolean savePhases(){
    	ForgePreferences fp = Gui_NewGame.preferences;
    	
    	fp.bAIUpkeep = cbAIUpkeep.isSelected();
    	fp.bAIDraw = cbAIDraw.isSelected();
    	fp.bAIEOT = cbAIEndOfTurn.isSelected();
    	fp.bAIBeginCombat = cbAIBeginCombat.isSelected();
    	fp.bAIEndCombat = cbAIEndCombat.isSelected();
    	
    	fp.bHumanUpkeep = cbHumanUpkeep.isSelected();
    	fp.bHumanDraw = cbHumanDraw.isSelected();
    	fp.bHumanEOT = cbHumanEndOfTurn.isSelected();
    	fp.bHumanBeginCombat = cbHumanBeginCombat.isSelected();
    	fp.bHumanEndCombat = cbHumanEndCombat.isSelected();
    	
    	return true;
    }
   
    public static JCheckBoxMenuItem playsoundCheckboxForMenu = new JCheckBoxMenuItem("Play Sound", false);
    
    // Phases
    public static JCheckBoxMenuItem cbAIUpkeep		       = new JCheckBoxMenuItem("Upkeep", true);
    public static JCheckBoxMenuItem cbAIDraw		       = new JCheckBoxMenuItem("Draw", true);
    public static JCheckBoxMenuItem cbAIEndOfTurn	       = new JCheckBoxMenuItem("End of Turn", true);
    public static JCheckBoxMenuItem cbAIBeginCombat		   = new JCheckBoxMenuItem("Begin Combat", true);
    public static JCheckBoxMenuItem cbAIEndCombat	       = new JCheckBoxMenuItem("End Combat", true); 
    
    public static JCheckBoxMenuItem cbHumanUpkeep		   = new JCheckBoxMenuItem("Upkeep", true);
    public static JCheckBoxMenuItem cbHumanDraw			   = new JCheckBoxMenuItem("Draw", true);
    public static JCheckBoxMenuItem cbHumanEndOfTurn	   = new JCheckBoxMenuItem("End of Turn", true);
    public static JCheckBoxMenuItem cbHumanBeginCombat	   = new JCheckBoxMenuItem("Begin Combat", true);
    public static JCheckBoxMenuItem cbHumanEndCombat	   = new JCheckBoxMenuItem("End Combat", true); 
    
    // ********** End of Phase stuff in Display ******************
    
    JXMultiSplitPane                  pane                     = new JXMultiSplitPane();
    JButton                         cancelButton             = new JButton();
    JButton                         okButton                 = new JButton();
    JTextArea                       messageArea              = new JTextArea(1, 10);
    JTextArea                       combatArea               = new JTextArea();
    JPanel                          stackPanel               = new JPanel();
    JPanel                          oppLandPanel             = new JPanel();
    JPanel                          oppCreaturePanel         = new JPanel();
    JPanel                          playerCreaturePanel      = new JPanel();
    JPanel                          playerLandPanel          = new JPanel();
    JPanel                          playerHandPanel          = new JPanel();
    JPanel                          cdPanel                  = new JPanel();
    JLabel                          oppLifeLabel             = new JLabel();
    JLabel							oppIconLabel			 = new JLabel();
    JLabel                          playerLifeLabel          = new JLabel();
    JLabel                          oppPCLabel               = new JLabel();
    JLabel                          playerPCLabel            = new JLabel();
    JLabel                          oppLibraryLabel          = new JLabel(
                                                                     ForgeProps.getLocalized(COMPUTER_LIBRARY.TITLE),
                                                                     SwingConstants.TRAILING);
    JLabel                          oppHandValue             = new JLabel();
    JLabel                          oppLibraryValue          = new JLabel();
    JLabel                          oppGraveValue            = new JLabel();
    JLabel                          oppRemovedValue          = new JLabel();
    JLabel                          playerHandValue          = new JLabel();
    JLabel                          playerLibraryValue       = new JLabel();
    JLabel                          playerGraveValue         = new JLabel();
    JLabel                          playerFBValue            = new JLabel();
    JLabel                          playerRemovedValue       = new JLabel();
    
    CardDetailPanel                 detail                   = new CardDetailPanel(null);
    CardPicturePanel                picture                  = new CardPicturePanel(null);
    
    private class ZoneAction extends ForgeAction {
        private static final long serialVersionUID = -5822976087772388839L;
        private PlayerZone        zone;
        private String            title;
        
        public ZoneAction(PlayerZone zone, String property) {
            super(property);
            title = ForgeProps.getLocalized(property + "/title");
            this.zone = zone;
        }
        
        public void actionPerformed(ActionEvent e) {
            Card[] c = getCards();
            
            if(AllZone.NameChanger.shouldChangeCardName()) c = AllZone.NameChanger.changeCard(c);
            
            if(c.length == 0) AllZone.Display.getChoiceOptional(title, new String[] {"no cards"});
            else {
                Card choice = AllZone.Display.getChoiceOptional(title, c);
                if(choice != null) doAction(choice);
            }
        }
        
        /*
        protected PlayerZone getZone() {
            return zone;
        }
        */
        protected Card[] getCards() {
            return zone.getCards();
        }
        
        protected void doAction(Card c) {}
    }
    
    private class ConcedeAction extends ForgeAction {
        
        private static final long serialVersionUID = -6976695235601916762L;
        
        public ConcedeAction() {
            super(CONCEDE);
        }
        
        public void actionPerformed(ActionEvent e) {
            concede();
        }
    }
}

//very hacky


class Gui_MultipleBlockers3 extends JFrame {
    private static final long serialVersionUID = 7622818310877381045L;
    
    private int               assignDamage;
    private Card              att;
    private CardList	      blockers;
    private CardContainer     guiDisplay;
    
    private BorderLayout      borderLayout1    = new BorderLayout();
    private JPanel            mainPanel        = new JPanel();
    private JScrollPane       jScrollPane1     = new JScrollPane();
    private JLabel            numberLabel      = new JLabel();
    private JPanel            jPanel3          = new JPanel();
    private BorderLayout      borderLayout3    = new BorderLayout();
    private JPanel            creaturePanel    = new JPanel();
    
    
    public static void main(String[] args) {
        CardList list = new CardList();
        list.add(AllZone.CardFactory.getCard("Elvish Piper", null));
        list.add(AllZone.CardFactory.getCard("Lantern Kami", null));
        list.add(AllZone.CardFactory.getCard("Frostling", null));
        list.add(AllZone.CardFactory.getCard("Frostling", null));
        
        for(int i = 0; i < 2; i++)
            new Gui_MultipleBlockers3(null, list, i + 1, null);
    }
    
    Gui_MultipleBlockers3(Card attacker, CardList creatureList, int damage, CardContainer display) {
        this();
        assignDamage = damage;
        updateDamageLabel();//update user message about assigning damage
        guiDisplay = display;
        att = attacker;
        blockers = creatureList;
        
        for(int i = 0; i < creatureList.size(); i++)
            creaturePanel.add(new CardPanel(creatureList.get(i)));
        
        if (att.getKeyword().contains("Trample")) {
	        Card player = new Card();
	        player.setName("Player");
	        player.addIntrinsicKeyword("Shroud");
	        player.addIntrinsicKeyword("Indestructible");
	        creaturePanel.add(new CardPanel(player));
        }
        
        JDialog dialog = new JDialog(this, true);
        dialog.setTitle("Multiple Blockers");
        dialog.setContentPane(mainPanel);
        dialog.setSize(470, 260);
        dialog.setVisible(true);
    }
    
    public Gui_MultipleBlockers3() {
        try {
            jbInit();
        } catch(Exception ex) {
            ErrorViewer.showError(ex);
        }
//    setSize(470, 280);
//    show();
    }
    
    private void jbInit() throws Exception {
        this.getContentPane().setLayout(borderLayout1);
        this.setTitle("Multiple Blockers");
        mainPanel.setLayout(null);
        numberLabel.setHorizontalAlignment(SwingConstants.CENTER);
        numberLabel.setHorizontalTextPosition(SwingConstants.CENTER);
        numberLabel.setText("Assign");
        numberLabel.setBounds(new Rectangle(52, 30, 343, 24));
        jPanel3.setLayout(borderLayout3);
        jPanel3.setBounds(new Rectangle(26, 75, 399, 114));
        creaturePanel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                creaturePanel_mousePressed(e);
            }
        });
        creaturePanel.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                creaturePanel_mouseMoved(e);
            }
        });
        mainPanel.add(jPanel3, null);
        jPanel3.add(jScrollPane1, BorderLayout.CENTER);
        mainPanel.add(numberLabel, null);
        jScrollPane1.getViewport().add(creaturePanel, null);
        this.getContentPane().add(mainPanel, BorderLayout.CENTER);
    }
    
    void okButton_actionPerformed(ActionEvent e) {
        dispose();
    }
    
    void creaturePanel_mousePressed(MouseEvent e) {
        Object o = creaturePanel.getComponentAt(e.getPoint());
        if(o instanceof CardPanel) {
        	
        	boolean assignedDamage = true;
        	
            CardContainer cardPanel = (CardContainer) o;
            Card c = cardPanel.getCard();
            //c.setAssignedDamage(c.getAssignedDamage() + 1);
            CardList cl = new CardList();
            cl.add(att);
            
            boolean assignedLethalDamageToAllBlockers = true;
        	for (Card crd : blockers )
        	{
        		if (crd.getTotalAssignedDamage() < ( crd.getNetDefense() - crd.getDamage() ))
        			assignedLethalDamageToAllBlockers = false;
        	}
        	
            
            if (c.getName().equals("Player") && att.getKeyword().contains("Trample") && assignedLethalDamageToAllBlockers)
            {
            	//what happens with Double Strike???
            	if (att.getKeyword().contains("First Strike"))
            		AllZone.Combat.addDefendingFirstStrikeDamage(1, att);
            	else
            		AllZone.Combat.addDefendingDamage(1, att);
            	
            	c.addAssignedDamage(1, att);
            }
            else if (!c.getName().equals("Player")){
            	c.addAssignedDamage(1, att);
            }
            else
            	assignedDamage = false;
            
            if (assignedDamage)
            {
	            assignDamage--;
	            updateDamageLabel();
	            if(assignDamage == 0) dispose();
            }
            
            if(guiDisplay != null) {
                guiDisplay.setCard(c);
            }
        }
        //reduce damage, show new user message, exit if necessary
        
    }//creaturePanel_mousePressed()
    
    void updateDamageLabel() {
        numberLabel.setText("Assign " + assignDamage + " damage - click on card to assign damage");
    }
    
    void creaturePanel_mouseMoved(MouseEvent e) {
        Object o = creaturePanel.getComponentAt(e.getPoint());
        if(o instanceof CardPanel) {
            CardContainer cardPanel = (CardContainer) o;
            Card c = cardPanel.getCard();
            
            if(guiDisplay != null) {
                guiDisplay.setCard(c);
            }
        }
    }
}
