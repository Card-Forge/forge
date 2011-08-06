
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
import java.util.Observable;
import java.util.Observer;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
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
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.jdesktop.swingx.MultiSplitPane;
import org.jdesktop.swingx.MultiSplitLayout.Node;

import forge.error.ErrorViewer;
import forge.gui.ForgeAction;
import forge.gui.ListChooser;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;


public class GuiDisplay3 extends JFrame implements Display, NewConstants, NewConstants.GUI.GuiDisplay, NewConstants.LANG.GuiDisplay {
    private static final long serialVersionUID = 4519302185194841060L;
    
    private GuiInput          inputControl;
    
    Font                      statFont         = new Font("MS Sans Serif", Font.PLAIN, 12);
    Font                      lifeFont         = new Font("MS Sans Serif", Font.PLAIN, 40);
    Font                      checkboxFont     = new Font("MS Sans Serif", Font.PLAIN, 9);
    
    /*
    public Color c1 = new Color(112,112,112);
    public Color c2 = new Color(50,50,50);
    public Color c3 = new Color(204,204,204);
    */

    public static Color       c1               = new Color(204, 204, 204);
    public static Color       c2               = new Color(204, 204, 204);
    
    private Action            HUMAN_GRAVEYARD_ACTION;
    private Action            HUMAN_REMOVED_ACTION;
    private Action            HUMAN_FLASHBACK_ACTION;
    private Action            COMPUTER_GRAVEYARD_ACTION;
    private Action            COMPUTER_REMOVED_ACTION;
    private Action            CONCEDE_ACTION;
    
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
    
    private void setupActions() {
        HUMAN_GRAVEYARD_ACTION = new ZoneAction(AllZone.Human_Graveyard, HUMAN_GRAVEYARD);
        HUMAN_REMOVED_ACTION = new ZoneAction(AllZone.Human_Removed, HUMAN_REMOVED);
        HUMAN_FLASHBACK_ACTION = new ZoneAction(AllZone.Human_Removed, HUMAN_FLASHBACK) {
            
            private static final long serialVersionUID = 8120331222693706164L;
            
            @Override
            protected Card[] getCards() {
                return CardFactoryUtil.getFlashbackCards(Constant.Player.Human).toArray();
            }
            
            @Override
            protected void doAction(Card c) {
                SpellAbility[] sa = c.getSpellAbility();
                if(sa[1].canPlay()) AllZone.GameAction.playSpellAbility(sa[1]);
            }
        };
        COMPUTER_GRAVEYARD_ACTION = new ZoneAction(AllZone.Computer_Graveyard, COMPUTER_GRAVEYARD);
        COMPUTER_REMOVED_ACTION = new ZoneAction(AllZone.Computer_Removed, COMPUTER_REMOVED);
        CONCEDE_ACTION = new ConcedeAction();
    }
    
    private void addMenu() {
        Object[] obj = {
                HUMAN_GRAVEYARD_ACTION, HUMAN_REMOVED_ACTION, HUMAN_FLASHBACK_ACTION, COMPUTER_GRAVEYARD_ACTION,
                COMPUTER_REMOVED_ACTION, GuiDisplay3.eotCheckboxForMenu, new JSeparator(),
                ErrorViewer.ALL_THREADS_ACTION, new JSeparator(), CONCEDE_ACTION};
        
        JMenu gameMenu = new JMenu(ForgeProps.getLocalized(MENU_BAR.MENU.TITLE));
        for(Object o:obj) {
            if(o instanceof ForgeAction) gameMenu.add(((ForgeAction) o).setupButton(new JMenuItem()));
            else if(o instanceof Action) gameMenu.add((Action) o);
            else if(o instanceof Component) gameMenu.add((Component) o);
            else throw new AssertionError();
        }
        
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(gameMenu);
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
    }
    
    //returned Object could be null
    public <T> T getChoiceOptional(String message, T[] choices) {
        ListChooser<T> c = new ListChooser<T>(message, 0, 1, choices);
        final JList list = c.getJList();
        if(choices[0] instanceof Card) {
            list.addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent ev) {
                    if(list.getSelectedValue() instanceof Card) updateCardDetail((Card) list.getSelectedValue());
                }
            });
        }
        if(!c.show()) return null;
        
        return c.getSelectedValue();
    }//getChoiceOptional()
    
    // returned Object will never be null
    public <T> T getChoice(String message, T[] choices) {
        ListChooser<T> c = new ListChooser<T>(message, 1, choices);
        final JList list = c.getJList();
        if(choices[0] instanceof Card) {
            list.addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent ev) {
                    if(list.getSelectedValue() instanceof Card) updateCardDetail((Card) list.getSelectedValue());
                }
            });
        }
        c.show();
        return c.getSelectedValue();
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
                inputControl.selectPlayer(Constant.Player.Computer);
            }
        });
        
        //self life mouse listener
        playerLifeLabel.addMouseListener(new MouseAdapter() {
            
            @Override
            public void mousePressed(MouseEvent e) {
                inputControl.selectPlayer(Constant.Player.Human);
            }
        });
        
        //self play (land) ---- Mouse
        playerLandPanel.addMouseListener(new MouseAdapter() {
            
            @Override
            public void mousePressed(MouseEvent e) {
                Object o = playerLandPanel.getComponentAt(e.getPoint());
                if(o instanceof CardPanel) {
                    CardPanel cardPanel = (CardPanel) o;
                    
                    if(cardPanel.getCard().isTapped()
                            && (inputControl.input instanceof Input_PayManaCost || inputControl.input instanceof Input_PayManaCost_Ability)) {
                        while(cardPanel.connectedCard != null) {
                            cardPanel = cardPanel.connectedCard;
                            if(cardPanel.getCard().isUntapped()) {
                                break;
                            }
                        }
                    }
                    
                    inputControl.selectCard(cardPanel.getCard(), AllZone.Human_Play);
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
                    
                    if((cardPanel.getCard().isTapped() || cardPanel.getCard().hasSickness() || ((cardPanel.getCard().getKeyword().contains("Vigilance")) && att.contains(cardPanel.getCard())))
                            && (inputControl.input instanceof Input_Attack)) {
                        while(cardPanel.connectedCard != null) {
                            cardPanel = cardPanel.connectedCard;
                            if(cardPanel.getCard().isUntapped() && !cardPanel.getCard().hasSickness()) {
                                break;
                            }
                        }
                    }
                    
                    inputControl.selectCard(cardPanel.getCard(), AllZone.Human_Play);
                }
            }
        });
        //self hand ---- Mouse
        playerHandPanel.addMouseListener(new MouseAdapter() {
            
            @Override
            public void mousePressed(MouseEvent e) {
                Object o = playerHandPanel.getComponentAt(e.getPoint());
                if(o instanceof CardPanel) {
                    CardPanel cardPanel = (CardPanel) o;
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
                    CardPanel cardPanel = (CardPanel) o;
                    inputControl.selectCard(cardPanel.getCard(), AllZone.Computer_Play);
                }
            }
        });
        
        //computer play (no land) ---- Mouse
        oppCreaturePanel.addMouseListener(new MouseAdapter() {
            
            @Override
            public void mousePressed(MouseEvent e) {
                Object o = oppCreaturePanel.getComponentAt(e.getPoint());
                if(o instanceof CardPanel) {
                    CardPanel cardPanel = (CardPanel) o;
                    inputControl.selectCard(cardPanel.getCard(), AllZone.Computer_Play);
                }
            }
        });
        

    }//addListener()
    
    public void updateCardDetail(Card c) {
//      if(! c.isToken())
//        System.out.println(c +" " +c.getSpellAbility()[0].canPlay() +" " +c.getSpellAbility()[0].getManaCost());
        
        if(c == null) return;
        
        cdLabel1.setText("");
        cdLabel2.setText("");
        cdLabel3.setText("");
        cdLabel4.setText("");
        cdLabel5.setText("");
        cdLabel6.setText("");
        cdArea.setText("");
        
        if(!c.isFaceDown()) {
            if(c.isLand()) cdLabel1.setText(c.getName());
            else cdLabel1.setText(c.getName() + "  - " + c.getManaCost());
        } else cdLabel1.setText("Morph");
        
        cdLabel2.setText(GuiDisplayUtil.formatCardType(c));
        
        if(c.isFaceDown()) cdLabel2.setText("Creature");
        
        if(c.isCreature()) {
            String stats = "" + c.getNetAttack() + " / " + c.getNetDefense();
            cdLabel3.setText(stats);
        }
        
        if(c.isCreature()) cdLabel4.setText("Damage: " + c.getDamage() + " Assigned Damage: "
                + c.getAssignedDamage());
        
        if(c.isPlaneswalker()) cdLabel4.setText("Assigned Damage: " + c.getAssignedDamage());
        
        String uniqueID = c.getUniqueNumber() + " ";
        cdLabel5.setText("Card ID  " + uniqueID);
        
        //if (c.getCounters(Counters.SPORE) != 0)
        //	cdLabel6.setText("Spore counters: " + c.getCounters(Counters.SPORE));
        
        String tokenText = "";
        if(c.isToken()) tokenText = tokenText + "Token\r\n";
        
        String counterText = "\r\n";
        
        if(c.getCounters(Counters.AGE) != 0) counterText = counterText + "Age counters: "
                + c.getCounters(Counters.AGE) + "\r\n";
        if(c.getCounters(Counters.BLAZE) != 0) counterText = counterText + "Blaze counters: "
                + c.getCounters(Counters.BLAZE) + "\r\n";
        if(c.getCounters(Counters.CHARGE) != 0) counterText = counterText + "Charge counters: "
                + c.getCounters(Counters.CHARGE) + "\r\n";
        if(c.getCounters(Counters.DIVINITY) != 0) counterText = counterText + "Divinity counters: "
                + c.getCounters(Counters.DIVINITY) + "\r\n";
        if(c.getCounters(Counters.FADE) != 0) counterText = counterText + "Fade counters: "
                + c.getCounters(Counters.FADE) + "\r\n";
        if(c.getCounters(Counters.HOOFPRINT) != 0) counterText = counterText + "Hoofprint counters: "
                + c.getCounters(Counters.HOOFPRINT) + "\r\n";
        if(c.getCounters(Counters.ICE) != 0) counterText = counterText + "Ice counters: "
                + c.getCounters(Counters.ICE) + "\r\n";
        if(c.getCounters(Counters.LOYALTY) != 0) counterText = counterText + "Loyalty counters: "
                + c.getCounters(Counters.LOYALTY) + "\r\n";
        if(c.getCounters(Counters.MANA) != 0) counterText = counterText + "Mana counters: "
                + c.getCounters(Counters.MANA) + "\r\n";
        if(c.getCounters(Counters.P0M1) != 0) counterText = counterText + "0/-1 counters: "
                + c.getCounters(Counters.P0M1) + "\r\n";
        if(c.getNetPTCounters() != 0) { //+1/+1 and -1/-1 counters should cancel each other out:
            if(c.getNetPTCounters() > 0) counterText = counterText + "+1/+1 counters: " + c.getNetPTCounters()
                    + "\r\n";
            else {
                int m1m1Counters = -1 * c.getNetPTCounters();
                counterText = counterText + "-1/-1 counters: " + m1m1Counters + "\r\n";
            }
        }
        /*if (c.getCounters(Counters.P1P1) != 0)
         	counterText = counterText + "+1/+1 counters: " + c.getCounters(Counters.P1P1) + "\r\n";
        if (c.getCounters(Counters.M1M1) != 0)
         	counterText = counterText + "-1/-1 counters: " + c.getCounters(Counters.M1M1) + "\r\n";
        */
        if(c.getCounters(Counters.QUEST) != 0) counterText = counterText + "Quest counters: "
                + c.getCounters(Counters.QUEST) + "\r\n";
        if(c.getCounters(Counters.SPORE) != 0) counterText = counterText + "Spore counters: "
                + c.getCounters(Counters.SPORE) + "\r\n";
        
        String chosenTypeText = "";
        if(c.getChosenType() != "") chosenTypeText = "(chosen type: " + c.getChosenType() + ")";
        
        String equippingText = "";
        if(c.getEquipping().size() > 0) equippingText = "=Equipping " + c.getEquipping().get(0) + "=";
        

        String equippedByText = "";
        if(c.getEquippedBy().size() > 0) {
            equippedByText = "=Equipped by " + c.getEquippedBy().get(0);
            for(int i = 1; i < c.getEquippedBy().size(); i++) {
                equippedByText += ", " + c.getEquippedBy().get(i);
            }
            equippedByText += "=";
        }
        
        String enchantingText = "";
        if(c.getEnchanting().size() > 0) {
            enchantingText = "*Enchanting " + c.getEnchanting().get(0) + "*";
        }
        
        String enchantedByText = "";
        if(c.getEnchantedBy().size() > 0) {
            enchantedByText = "*Enchanted by " + c.getEnchantedBy().get(0);
            for(int i = 1; i < c.getEnchantedBy().size(); i++) {
                enchantedByText += ", " + c.getEnchantedBy().get(i);
            }
            enchantedByText += "*";
        }
        
        if(!c.isFaceDown()) this.cdArea.setText(tokenText + c.getText() + counterText + chosenTypeText
                + equippingText + equippedByText + enchantingText + enchantedByText);
        else this.cdArea.setText(tokenText + counterText);
        
        cdPanel.setBorder(GuiDisplayUtil.getBorder(c));
        
        //picture
        picturePanel.removeAll();
        JPanel pic = GuiDisplayUtil.getPicture(c);
        pic.setSize(300, 300);
        picturePanel.add(pic);
        picturePanel.revalidate();
    }//updateCardDetail()
    
    private void addObservers() {
        //Human Hand, Graveyard, and Library totals
        {//make sure to not interfer with anything below, since this is a very long method
            Observer o = new Observer() {
                public void update(Observable a, Object b) {
                    playerHandValue.setText("" + AllZone.Human_Hand.getCards().length);
                    playerGraveValue.setText("" + AllZone.Human_Graveyard.getCards().length);
                    playerLibraryValue.setText("" + AllZone.Human_Library.getCards().length);
                    playerFBValue.setText("" + CardFactoryUtil.getFlashbackCards(Constant.Player.Human).size());
                    playerRemovedValue.setText("" + AllZone.Human_Removed.getCards().length);
                    
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
                    oppRemovedValue.setText("" + AllZone.Computer_Removed.getCards().length);
                }
            };
            AllZone.Computer_Hand.addObserver(o);
            AllZone.Computer_Graveyard.addObserver(o);
            AllZone.Computer_Library.addObserver(o);
        }
        

        //opponent life
        oppLifeLabel.setText("" + AllZone.Computer_Life.getLife());
        AllZone.Computer_Life.addObserver(new Observer() {
            public void update(Observable a, Object b) {
                int life = AllZone.Computer_Life.getLife();
                oppLifeLabel.setText("" + life);
            }
        });
        AllZone.Computer_Life.updateObservers();
        
        //player life
        playerLifeLabel.setText("" + AllZone.Human_Life.getLife());
        AllZone.Human_Life.addObserver(new Observer() {
            public void update(Observable a, Object b) {
                int life = AllZone.Human_Life.getLife();
                playerLifeLabel.setText("" + life);
            }
        });
        AllZone.Human_Life.updateObservers();
        
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
                            GuiDisplay3.this.updateCardDetail(cardPanel.getCard());
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
                    panel = GuiDisplayUtil.getCardPanel(c[i]);
                    p.add(panel);
                }
                
                p.setBackground(c2);
                p.revalidate();
                p.repaint();
            }
        });
        AllZone.Human_Hand.updateObservers();
        //END, self hand
        
        //self play (land)
        AllZone.Human_Play.addObserver(new Observer() {
            public void update(Observable a, Object b) {
                //PlayerZone pZone = (PlayerZone) a; //unused
                JPanel p = playerLandPanel;
                p.removeAll();
                
                GuiDisplayUtil.setupLandPanel(p, AllZone.Human_Play.getCards());
                p.setBackground(c2);
                p.revalidate();
                p.repaint();
            }
        });
        AllZone.Human_Play.updateObservers();
        //END - self play (only land)
        

        //self play (no land)
        AllZone.Human_Play.addObserver(new Observer() {
            public void update(Observable a, Object b) {
                //PlayerZone pZone = (PlayerZone) a; //unused
                JPanel p = playerCreaturePanel;
                p.removeAll();
                
                GuiDisplayUtil.setupNoLandPanel(p, AllZone.Human_Play.getCards());
                p.setBackground(c2);
                p.revalidate();
                p.repaint();
            }
        });
        AllZone.Human_Play.updateObservers();
        //END - self play (no land)
        

        //computer play (no land)
        AllZone.Computer_Play.addObserver(new Observer() {
            public void update(Observable a, Object b) {
                //PlayerZone pZone = (PlayerZone) a; //unused
                JPanel p = oppCreaturePanel;
                p.removeAll();
                
                GuiDisplayUtil.setupNoLandPanel(p, AllZone.Computer_Play.getCards());
                
                p.setBackground(c2);
                p.revalidate();
                p.repaint();
            }
        });
        AllZone.Computer_Play.updateObservers();
        //END - computer play (no land)
        
        //computer play (land)
        AllZone.Computer_Play.addObserver(new Observer() {
            public void update(Observable a, Object b) {
                //PlayerZone pZone = (PlayerZone) a; //unused
                JPanel p = oppLandPanel;
                p.removeAll();
                
                GuiDisplayUtil.setupLandPanel(p, AllZone.Computer_Play.getCards());
                p.setBackground(c2);
                p.revalidate();
                p.repaint();
            }
        });
        AllZone.Computer_Play.updateObservers();
        //END - computer play (only land)
        
    }//addObservers()
    
    private void initComponents() {
        //Preparing the Frame
        setTitle(ForgeProps.getLocalized(LANG.PROGRAM_NAME));
        setFont(new Font("Times New Roman", 0, 16));
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
        initMsgYesNo(pane);
        initOpp(pane);
        initStackCombat(pane);
        initPlayer(pane);
        initZones(pane);
        initCardPicture(pane);
    }
    
    private void initMsgYesNo(JPanel pane) {
//        messageArea.setBorder(BorderFactory.createEtchedBorder());
        messageArea.setEditable(false);
        messageArea.setFont(getFont());
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
                
                if(AllZone.Phase.isNeedToNextPhase() == true) {
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
                } else if(code == KeyEvent.VK_ENTER) {
                    //same as space
                    okButton.doClick();
                }
            }
        });
        
        okButton.requestFocusInWindow();
        
        //if(okButton.isEnabled())
        //okButton.doClick();
        JPanel yesNoPanel = new JPanel(new FlowLayout());
        yesNoPanel.setBackground(c1);
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
        oppLifeLabel.setFont(lifeFont);
        oppLifeLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        JLabel oppLibraryLabel = new JLabel(ForgeProps.getLocalized(COMPUTER_LIBRARY.TITLE),
                SwingConstants.TRAILING);
        oppLibraryLabel.setFont(statFont);
        
        JLabel oppHandLabel = new JLabel(ForgeProps.getLocalized(COMPUTER_HAND.TITLE), SwingConstants.TRAILING);
        oppHandLabel.setFont(statFont);
        
        //JLabel oppGraveLabel = new JLabel("Grave:", SwingConstants.TRAILING);
        JButton oppGraveButton = new JButton(COMPUTER_GRAVEYARD_ACTION);
        oppGraveButton.setText((String) COMPUTER_GRAVEYARD_ACTION.getValue("buttonText"));
        oppGraveButton.setMargin(new Insets(0, 0, 0, 0));
        oppGraveButton.setHorizontalAlignment(SwingConstants.TRAILING);
        oppGraveButton.setFont(statFont);
        

        JPanel gravePanel = new JPanel(new BorderLayout());
        gravePanel.add(oppGraveButton, BorderLayout.EAST);
        
        JButton oppRemovedButton = new JButton(COMPUTER_REMOVED_ACTION);
        oppRemovedButton.setText((String) COMPUTER_REMOVED_ACTION.getValue("buttonText"));
        oppRemovedButton.setMargin(new Insets(0, 0, 0, 0));
        //removedButton.setHorizontalAlignment(SwingConstants.TRAILING);
        oppRemovedButton.setFont(statFont);
        

        oppHandValue.setFont(statFont);
        oppHandValue.setHorizontalAlignment(SwingConstants.LEADING);
        
        oppLibraryValue.setFont(statFont);
        oppLibraryValue.setHorizontalAlignment(SwingConstants.LEADING);
        
        oppGraveValue.setFont(statFont);
        oppGraveValue.setHorizontalAlignment(SwingConstants.LEADING);
        
        oppRemovedValue.setFont(statFont);
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
        oppNumbersPanel.setBackground(c1);
        
        JPanel oppPanel = new JPanel();
        oppPanel.setBackground(c1);
        oppPanel.setBorder(new TitledBorder(new EtchedBorder(), ForgeProps.getLocalized(COMPUTER_TITLE)));
        oppPanel.setLayout(new BorderLayout());
        oppPanel.add(oppNumbersPanel, BorderLayout.WEST);
        oppPanel.add(oppLifeLabel, BorderLayout.EAST);
        pane.add(new ExternalPanel(oppPanel), "compy");
    }
    
    private void initStackCombat(JPanel pane) {
        stackPanel.setLayout(new GridLayout(0, 1, 10, 10));
        JScrollPane stackPane = new JScrollPane(stackPanel);
        stackPane.setBorder(new EtchedBorder());
        pane.add(new ExternalPanel(stackPane), "stack");
        
        combatArea.setEditable(false);
        combatArea.setFont(getFont());
        combatArea.setLineWrap(true);
        combatArea.setWrapStyleWord(true);
        combatArea.setBackground(c1);
        
        JScrollPane combatPane = new JScrollPane(combatArea);
        combatPane.setBackground(c1);
        
        combatPane.setBorder(new TitledBorder(new EtchedBorder(), ForgeProps.getLocalized(COMBAT)));
        pane.add(new ExternalPanel(combatPane), "combat");
    }
    
    private void initPlayer(JPanel pane) {
        int fontSize = 12;
        playerLifeLabel.setFont(lifeFont);
        playerLifeLabel.setHorizontalAlignment(SwingConstants.CENTER);
        

        JLabel playerLibraryLabel = new JLabel(ForgeProps.getLocalized(HUMAN_LIBRARY.TITLE),
                SwingConstants.TRAILING);
        playerLibraryLabel.setFont(statFont);
        
        JLabel playerHandLabel = new JLabel(ForgeProps.getLocalized(HUMAN_HAND.TITLE), SwingConstants.TRAILING);
        playerHandLabel.setFont(statFont);
        
        //JLabel playerGraveLabel = new JLabel("Grave:", SwingConstants.TRAILING);
        JButton playerGraveButton = new JButton(HUMAN_GRAVEYARD_ACTION);
        playerGraveButton.setText((String) HUMAN_GRAVEYARD_ACTION.getValue("buttonText"));
        playerGraveButton.setMargin(new Insets(0, 0, 0, 0));
        playerGraveButton.setHorizontalAlignment(SwingConstants.TRAILING);
        playerGraveButton.setFont(statFont);
        

        JButton playerFlashBackButton = new JButton(HUMAN_FLASHBACK_ACTION);
        playerFlashBackButton.setText((String) HUMAN_FLASHBACK_ACTION.getValue("buttonText"));
        playerFlashBackButton.setMargin(new Insets(0, 0, 0, 0));
        playerFlashBackButton.setHorizontalAlignment(SwingConstants.TRAILING);
        playerFlashBackButton.setFont(statFont);
        

        JPanel gravePanel = new JPanel(new BorderLayout());
        gravePanel.add(playerGraveButton, BorderLayout.EAST);
        
        JPanel playerFBPanel = new JPanel(new BorderLayout());
        playerFBPanel.add(playerFlashBackButton, BorderLayout.EAST);
        
        JButton playerRemovedButton = new JButton(HUMAN_REMOVED_ACTION);
        playerRemovedButton.setText((String) HUMAN_REMOVED_ACTION.getValue("buttonText"));
        playerRemovedButton.setMargin(new Insets(0, 0, 0, 0));
        //removedButton.setHorizontalAlignment(SwingConstants.TRAILING);
        playerRemovedButton.setFont(statFont);
        

        playerHandValue.setFont(statFont);
        playerHandValue.setHorizontalAlignment(SwingConstants.LEADING);
        
        playerLibraryValue.setFont(statFont);
        playerLibraryValue.setHorizontalAlignment(SwingConstants.LEADING);
        
        playerGraveValue.setFont(statFont);
        playerGraveValue.setHorizontalAlignment(SwingConstants.LEADING);
        
        playerFBValue.setFont(statFont);
        playerGraveValue.setHorizontalAlignment(SwingConstants.LEADING);
        
        playerRemovedValue.setFont(new Font("MS Sans Serif", 0, fontSize));
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
        playerNumbersPanel.setBackground(c1);
        
        JPanel playerPanel = new JPanel();
        playerPanel.setBackground(c1);
        playerPanel.setBorder(new TitledBorder(new EtchedBorder(), ForgeProps.getLocalized(HUMAN_TITLE)));
        playerPanel.setLayout(new BorderLayout());
        playerPanel.add(playerNumbersPanel, BorderLayout.WEST);
        playerPanel.add(playerLifeLabel, BorderLayout.EAST);
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
        cdLabel1.setFont(getFont());
        cdLabel1.setHorizontalAlignment(SwingConstants.CENTER);
        cdLabel1.setText("jLabel3");
        
        cdLabel2.setFont(getFont());
        cdLabel2.setHorizontalAlignment(SwingConstants.CENTER);
        cdLabel2.setText("jLabel4");
        
        cdLabel3.setFont(getFont());
        cdLabel3.setHorizontalAlignment(SwingConstants.CENTER);
//~        cdLabel3.setText("jLabel5");
        
        cdLabel4.setFont(getFont());
//~        cdLabel4.setText("jLabel6");
        
        cdLabel5.setFont(getFont());
//~        cdLabel5.setText("jLabel7");
        
        cdLabel6.setFont(getFont());
//~        cdLabel6.setText("jLabel8");
        
        JPanel cdLabels = new JPanel(new GridLayout(6, 0, 0, 5));
        cdLabels.add(cdLabel1);
        cdLabels.add(cdLabel2);
        cdLabels.add(cdLabel3);
        cdLabels.add(cdLabel4);
        cdLabels.add(cdLabel6);
        cdLabels.add(cdLabel5);
        //cdLabels.setBackground(c1);
        
        //StyledEditorKit se = new StyledEditorKit();
        
        //cdArea.setEditorKit(new StyledEditorKit());
        cdArea.setFont(getFont());
        cdArea.setLineWrap(true);
        cdArea.setWrapStyleWord(true);
        
        JScrollPane cdPane = new JScrollPane(cdArea);
        
        cdPanel.setLayout(new GridLayout(2, 1, 0, 5));
        cdPanel.setBorder(new EtchedBorder());
        cdPanel.add(cdLabels);
        cdPanel.add(cdPane);
        pane.add(new ExternalPanel(cdPanel), "detail");
        
        //~ picturePanel.setBorder(new EtchedBorder());
        
        picturePanel.setLayout(new BoxLayout(picturePanel, BoxLayout.Y_AXIS));
        picturePanel.setBackground(c1);
        pane.add(new ExternalPanel(picturePanel), "picture");
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
        dispose();
        Constant.Runtime.WinLose.addLose();
        new Gui_WinLose();
    }
    
    public boolean stopEOT() {
        return eotCheckboxForMenu.isSelected();
    }
    
    public static JCheckBoxMenuItem eotCheckboxForMenu  = new JCheckBoxMenuItem("Stop at End of Turn", false);
    
    MultiSplitPane                  pane                = new MultiSplitPane();
    JButton                         cancelButton        = new JButton();
    JButton                         okButton            = new JButton();
    JTextArea                       messageArea         = new JTextArea(1, 10);
    JTextArea                       cdArea              = new JTextArea(4, 12);
    //JEditorPane cdArea 			  = new JEditorPane();
    JTextArea                       combatArea          = new JTextArea();
    JPanel                          stackPanel          = new JPanel();
    JPanel                          oppLandPanel        = new JPanel();
    JPanel                          oppCreaturePanel    = new JPanel();
    JPanel                          playerCreaturePanel = new JPanel();
    JPanel                          playerLandPanel     = new JPanel();
    //JPanel    playerLandPanel 	  = new ImageJPanel("forest.jpg");
    //JPanel playerLandPanel = new BackgroundPanel("bg1.jpg");
    JPanel                          playerHandPanel     = new JPanel();
    //JPanel playerHandPanel = new BackgroundPanel("bg2.jpg");
    JPanel                          cdPanel             = new JPanel();
    //JPanel    picturePanel        = new JPanel();
    JPanel                          picturePanel        = new JPanel();
    JLabel                          oppLifeLabel        = new JLabel();
    JLabel                          playerLifeLabel     = new JLabel();
    JLabel                          cdLabel1            = new JLabel();
    JLabel                          cdLabel2            = new JLabel();
    JLabel                          cdLabel3            = new JLabel();
    JLabel                          cdLabel4            = new JLabel();
    JLabel                          cdLabel5            = new JLabel();
    JLabel                          cdLabel6            = new JLabel();
    JLabel                          oppHandValue        = new JLabel();
    JLabel                          oppLibraryValue     = new JLabel();
    JLabel                          oppGraveValue       = new JLabel();
    JLabel                          oppRemovedValue     = new JLabel();
    JLabel                          playerHandValue     = new JLabel();
    JLabel                          playerLibraryValue  = new JLabel();
    JLabel                          playerGraveValue    = new JLabel();
    JLabel                          playerFBValue       = new JLabel();
    JLabel                          playerRemovedValue  = new JLabel();
    
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
    private GuiDisplay3       guiDisplay;
    
    private BorderLayout      borderLayout1    = new BorderLayout();
    private JPanel            mainPanel        = new JPanel();
    private JScrollPane       jScrollPane1     = new JScrollPane();
    private JLabel            numberLabel      = new JLabel();
    private JPanel            jPanel3          = new JPanel();
    private BorderLayout      borderLayout3    = new BorderLayout();
    private JPanel            creaturePanel    = new JPanel();
    
    
    public static void main(String[] args) {
        CardList list = new CardList();
        list.add(AllZone.CardFactory.getCard("Elvish Piper", ""));
        list.add(AllZone.CardFactory.getCard("Lantern Kami", ""));
        list.add(AllZone.CardFactory.getCard("Frostling", ""));
        list.add(AllZone.CardFactory.getCard("Frostling", ""));
        
        for(int i = 0; i < 2; i++)
            new Gui_MultipleBlockers3(null, list, i + 1, null);
    }
    
    Gui_MultipleBlockers3(Card attacker, CardList creatureList, int damage, GuiDisplay3 display) {
        this();
        assignDamage = damage;
        updateDamageLabel();//update user message about assigning damage
        guiDisplay = display;
        att = attacker;
        
        for(int i = 0; i < creatureList.size(); i++)
            creaturePanel.add(GuiDisplayUtil.getCardPanel(creatureList.get(i)));
        

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
            CardPanel cardPanel = (CardPanel) o;
            Card c = cardPanel.getCard();
            //c.setAssignedDamage(c.getAssignedDamage() + 1);
            CardList cl = new CardList();
            cl.add(att);
            AllZone.GameAction.setAssignedDamage(c, cl, c.getAssignedDamage() + 1);
            
            if(guiDisplay != null) guiDisplay.updateCardDetail(c);
        }
        //reduce damage, show new user message, exit if necessary
        assignDamage--;
        updateDamageLabel();
        if(assignDamage == 0) dispose();
    }//creaturePanel_mousePressed()
    
    void updateDamageLabel() {
        numberLabel.setText("Assign " + assignDamage + " damage - click on card to assign damage");
    }
    
    void creaturePanel_mouseMoved(MouseEvent e) {
        Object o = creaturePanel.getComponentAt(e.getPoint());
        if(o instanceof CardPanel) {
            CardPanel cardPanel = (CardPanel) o;
            Card c = cardPanel.getCard();
            
            if(guiDisplay != null) guiDisplay.updateCardDetail(c);
        }
    }
}
