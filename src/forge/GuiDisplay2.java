package forge;



import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import forge.gui.ListChooser;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;


public class GuiDisplay2 extends javax.swing.JFrame implements Display, NewConstants {
    private static final long       serialVersionUID   = 8974795337536720207L;
    
    private GuiInput                inputControl;
    public static JCheckBoxMenuItem eotCheckboxForMenu = new JCheckBoxMenuItem("Stop at End of Turn", false);
    
    public boolean stopEOT() {
        return eotCheckboxForMenu.isSelected();
    }
    
    
    public GuiDisplay2() {
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
            setExtendedState(Frame.MAXIMIZED_BOTH);
        }
        super.setVisible(visible);
    }
    
    public void assignDamage(Card attacker, CardList blockers, int damage) {
        new Gui_MultipleBlockers(attacker, blockers, damage, this);
    }
    
    private void addMenu() {
        JMenuItem humanGraveyard = new JMenuItem("View Graveyard");
        humanGraveyard.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                Card c[] = AllZone.Human_Graveyard.getCards();
                
                if(AllZone.NameChanger.shouldChangeCardName()) c = AllZone.NameChanger.changeCard(c);
                
                if(c.length == 0) AllZone.Display.getChoiceOptional("Player's Grave", new String[] {"no cards"});
                else AllZone.Display.getChoiceOptional("Player's Grave", c);
            }
        });
        
        JMenuItem computerGraveyard = new JMenuItem("Computer - View Graveyard");
        computerGraveyard.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                Card c[] = AllZone.Computer_Graveyard.getCards();
                
                if(AllZone.NameChanger.shouldChangeCardName()) c = AllZone.NameChanger.changeCard(c);
                
                if(c.length == 0) AllZone.Display.getChoiceOptional("Computer's Grave", new String[] {"no cards"});
                else AllZone.Display.getChoiceOptional("Computer's Grave", c);
            }
        });
        

        JMenuItem concedeGame = new JMenuItem("Concede Game");
        concedeGame.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                dispose();
                Constant.Runtime.WinLose.addLose();
                new Gui_WinLose();
            }
        });
        

        JMenuItem gameMenu = new JMenu("Menu");
        gameMenu.add(humanGraveyard);
        gameMenu.add(computerGraveyard);
        // gameMenu.add(this.eotCheckboxForMenu);  // The static field GuiDisplay2.eotCheckboxForMenu should be accessed in a static way
        gameMenu.add(GuiDisplay2.eotCheckboxForMenu);
        gameMenu.add(new JSeparator());
        gameMenu.add(concedeGame);
        
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
        if(c == null) return;
        
        cdLabel1.setText("");
        cdLabel2.setText("");
        cdLabel3.setText("");
        cdLabel4.setText("");
        cdLabel5.setText("");
        cdArea.setText("");
        
        //do not change the name of tokens
        if(!(c.isToken() || c.getName().equals(""))) {
            //change name, probably need to fix this
            //cards that change creatures like Angelic Blessing don't show
            //that the creature has flying, or Coastal Hornclaw's flying ability
            //c = AllZone.CardFactory.copyCard(c);
            if(AllZone.NameChanger.shouldChangeCardName()) c = AllZone.NameChanger.changeCard(c);
        }
        
        if(c.isLand()) cdLabel1.setText(c.getName());
        else cdLabel1.setText(c.getName() + "  - " + c.getManaCost());
        
        cdLabel2.setText(GuiDisplayUtil.formatCardType(c));
        
        if(c.isCreature()) {
            String stats = "" + c.getNetAttack() + " / " + c.getNetDefense();
            cdLabel3.setText(stats);
        }
        
        if(c.isCreature()) cdLabel4.setText("Damage: " + c.getDamage() + " Assigned Damage: "
                + c.getAssignedDamage());
        
        if(c.isPlaneswalker()) cdLabel4.setText("Assigned Damage: " + c.getAssignedDamage());
        
        String uniqueID = c.getUniqueNumber() + " ";
        cdLabel5.setText("Card ID  " + uniqueID);
        
        this.cdArea.setText(c.getText());
        
        cdPanel.setBorder(GuiDisplayUtil.getBorder(c));
        
        //picture
        picturePanel.removeAll();
        picturePanel.add(GuiDisplayUtil.getPicture(c));
        picturePanel.revalidate();
    }//updateCardDetail()
    
    private void addObservers() {
        //Human Hand, Graveyard, and Library totals
        {//make sure to not interfer with anything below, since this is a very long method
            Observer o = new Observer() {
                public void update(Observable a, Object b) {
                    playerHandLabel.setText("" + AllZone.Human_Hand.getCards().length);
                    playerGraveLabel.setText("" + AllZone.Human_Graveyard.getCards().length);
                    playerLibraryLabel.setText("" + AllZone.Human_Library.getCards().length);
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
                    oppHandLabel.setText("" + AllZone.Computer_Hand.getCards().length);
                    oppGraveLabel.setText("" + AllZone.Computer_Graveyard.getCards().length);
                    oppLibraryLabel.setText("" + AllZone.Computer_Library.getCards().length);
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
                    String text = stack.peek(i).getStackDescription();
                    
                    //change card name
                    if(AllZone.NameChanger.shouldChangeCardName()) {
                        Card c = stack.peek(i).getSourceCard();
                        text = AllZone.NameChanger.changeString(c, text);
                    }
                    
                    label = new JLabel("" + (count++) + ". " + text);
                    
                    //update card detail
                    final CardPanel cardPanel = new CardPanel(stack.peek(i).getSourceCard());
                    cardPanel.setLayout(new BorderLayout());
                    cardPanel.add(label);
                    cardPanel.addMouseMotionListener(new MouseMotionAdapter() {
                        @Override
                        public void mouseMoved(MouseEvent me) {
                            GuiDisplay2.this.updateCardDetail(cardPanel.getCard());
                        }//mouseMoved
                    });
                    
                    stackPanel.add(cardPanel);
                }
                
                stackPanel.revalidate();
                stackPanel.repaint();
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
                
                //change card names
                if(AllZone.NameChanger.shouldChangeCardName()) {
                    AllZone.Human_Hand.setUpdate(false);
                    c = AllZone.NameChanger.changeCard(c);
                    AllZone.Human_Hand.setUpdate(true);
                }
                
                JPanel panel;
                for(int i = 0; i < c.length; i++) {
                    panel = GuiDisplayUtil.getCardPanel(c[i]);
                    p.add(panel);
                }
                p.revalidate();
                p.repaint();
            }
        });
        AllZone.Human_Hand.updateObservers();
        //END, self hand
        
        //self play (land)
        AllZone.Human_Play.addObserver(new Observer() {
            public void update(Observable a, Object b) {
                //PlayerZone pZone = (PlayerZone)a; //unused
                JPanel p = playerLandPanel;
                p.removeAll();
                
                Card[] c = AllZone.Human_Play.getCards();
                
                //change card names
                if(AllZone.NameChanger.shouldChangeCardName()) {
                    AllZone.Human_Play.setUpdate(false);
                    c = AllZone.NameChanger.changeCard(c);
                    AllZone.Human_Play.setUpdate(true);
                }
                
                GuiDisplayUtil.setupLandPanel(p, c);
                
                p.revalidate();
                p.repaint();
            }
        });
        AllZone.Human_Play.updateObservers();
        //END - self play (only land)
        

        //self play (no land)
        AllZone.Human_Play.addObserver(new Observer() {
            public void update(Observable a, Object b) {
                //PlayerZone pZone = (PlayerZone)a; //unused
                JPanel p = playerCreaturePanel;
                p.removeAll();
                
                Card[] c = AllZone.Human_Play.getCards();
                
                //change card names
                if(AllZone.NameChanger.shouldChangeCardName()) {
                    AllZone.Human_Play.setUpdate(false);
                    c = AllZone.NameChanger.changeCard(c);
                    AllZone.Human_Play.setUpdate(true);
                }
                
                GuiDisplayUtil.setupNoLandPanel(p, c);
                
                p.revalidate();
                p.repaint();
            }
        });
        AllZone.Human_Play.updateObservers();
        //END - self play (no land)
        

        //computer play (no land)
        AllZone.Computer_Play.addObserver(new Observer() {
            public void update(Observable a, Object b) {
                //PlayerZone pZone = (PlayerZone)a; //unused
                JPanel p = oppCreaturePanel;
                p.removeAll();
                
                Card[] c = AllZone.Computer_Play.getCards();
                
                //change card names
                if(AllZone.NameChanger.shouldChangeCardName()) {
                    AllZone.Computer_Play.setUpdate(false);
                    c = AllZone.NameChanger.changeCard(c);
                    AllZone.Computer_Play.setUpdate(true);
                }
                
                GuiDisplayUtil.setupNoLandPanel(p, c);
                
                p.revalidate();
                p.repaint();
            }
        });
        AllZone.Computer_Play.updateObservers();
        //END - computer play (no land)
        
        //computer play (land)
        AllZone.Computer_Play.addObserver(new Observer() {
            public void update(Observable a, Object b) {
                //PlayerZone pZone = (PlayerZone)a; //unused
                JPanel p = oppLandPanel;
                p.removeAll();
                
                Card[] c = AllZone.Computer_Play.getCards();
                
                //change card names
                if(AllZone.NameChanger.shouldChangeCardName()) {
                    AllZone.Computer_Play.setUpdate(false);
                    c = AllZone.NameChanger.changeCard(c);
                    AllZone.Computer_Play.setUpdate(true);
                }
                
                GuiDisplayUtil.setupLandPanel(p, c);
                
                p.revalidate();
                p.repaint();
            }
        });
        AllZone.Computer_Play.updateObservers();
        //END - computer play (only land)
        
    }//addObservers()
    
    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code.
     * The content of this method is always regenerated by the Form Editor.
     */
    private void initComponents()//GEN-BEGIN:initComponents
    {
        jMenuBar2 = new javax.swing.JMenuBar();
        jScrollPane1 = new javax.swing.JScrollPane();
        messageArea = new javax.swing.JTextArea();
        jPanel1 = new javax.swing.JPanel();
        cancelButton = new javax.swing.JButton();
        okButton = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        stackPanel = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        combatArea = new javax.swing.JTextArea();
        jPanel5 = new javax.swing.JPanel();
        jScrollPane4 = new javax.swing.JScrollPane();
        jPanel6 = new javax.swing.JPanel();
        oppLandPanel = new javax.swing.JPanel();
        oppCreaturePanel = new javax.swing.JPanel();
        playerCreaturePanel = new javax.swing.JPanel();
        playerLandPanel = new javax.swing.JPanel();
        playerHandPanel = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        oppLifeLabel = new javax.swing.JLabel();
        jPanel7 = new javax.swing.JPanel();
        playerLifeLabel = new javax.swing.JLabel();
        cdPanel = new javax.swing.JPanel();
        jPanel9 = new javax.swing.JPanel();
        cdLabel1 = new javax.swing.JLabel();
        cdLabel2 = new javax.swing.JLabel();
        cdLabel3 = new javax.swing.JLabel();
        cdLabel4 = new javax.swing.JLabel();
        cdLabel5 = new javax.swing.JLabel();
        jPanel10 = new javax.swing.JPanel();
        jScrollPane5 = new javax.swing.JScrollPane();
        cdArea = new javax.swing.JTextArea();
        picturePanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        oppHandLabel = new javax.swing.JLabel();
        oppLibraryLabel = new javax.swing.JLabel();
        oppGraveLabel = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        playerHandLabel = new javax.swing.JLabel();
        playerLibraryLabel = new javax.swing.JLabel();
        playerGraveLabel = new javax.swing.JLabel();
        jMenuBar1 = new javax.swing.JMenuBar();
        menuBar = new javax.swing.JMenu();
        
        getContentPane().setLayout(null);
        
        setTitle(ForgeProps.getLocalized(LANG.PROGRAM_NAME));
        setFont(new java.awt.Font("Times New Roman", 0, 16));
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent evt) {
                exitForm(evt);
            }
        });
        
        messageArea.setEditable(false);
        messageArea.setFont(getFont());
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        jScrollPane1.setViewportView(messageArea);
        
        getContentPane().add(jScrollPane1);
        jScrollPane1.setBounds(10, 20, 290, 100);
        
        jPanel1.setBorder(new javax.swing.border.EtchedBorder());
        cancelButton.setText("Cancel");
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });
        
        jPanel1.add(cancelButton);
        
        okButton.setText("OK");
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
                
                if(AllZone.Phase.isNeedToNextPhase() == true) {
                    //for debugging: System.out.println("There better be no nextPhase in the stack.");
                    AllZone.Phase.setNeedToNextPhase(false);
                    AllZone.Phase.nextPhase();
                }
            }
        });
        
        jPanel1.add(okButton);
        
        getContentPane().add(jPanel1);
        jPanel1.setBounds(10, 130, 290, 40);
        
        jScrollPane2.setBorder(new javax.swing.border.EtchedBorder());
        stackPanel.setLayout(new java.awt.GridLayout(0, 1, 10, 10));
        
        jScrollPane2.setViewportView(stackPanel);
        
        getContentPane().add(jScrollPane2);
        jScrollPane2.setBounds(10, 260, 290, 210);
        
        jPanel2.setLayout(new java.awt.BorderLayout());
        
        jPanel2.setBorder(new javax.swing.border.TitledBorder(new javax.swing.border.EtchedBorder(), "Combat"));
        combatArea.setEditable(false);
        combatArea.setFont(getFont());
        combatArea.setLineWrap(true);
        combatArea.setWrapStyleWord(true);
        jScrollPane3.setViewportView(combatArea);
        
        jPanel2.add(jScrollPane3, java.awt.BorderLayout.CENTER);
        
        getContentPane().add(jPanel2);
        jPanel2.setBounds(10, 480, 290, 120);
        
        jPanel5.setLayout(new java.awt.BorderLayout());
        
        jPanel5.setBorder(new javax.swing.border.EmptyBorder(new java.awt.Insets(1, 1, 1, 1)));
        jPanel6.setLayout(new java.awt.GridLayout(5, 0, 10, 10));
        
        oppLandPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        
        oppLandPanel.setBorder(new javax.swing.border.EtchedBorder());
        jPanel6.add(oppLandPanel);
        
        oppCreaturePanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        
        oppCreaturePanel.setBorder(new javax.swing.border.EtchedBorder());
        jPanel6.add(oppCreaturePanel);
        
        playerCreaturePanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        
        playerCreaturePanel.setBorder(new javax.swing.border.EtchedBorder());
        jPanel6.add(playerCreaturePanel);
        
        playerLandPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        
        playerLandPanel.setBorder(new javax.swing.border.EtchedBorder());
        jPanel6.add(playerLandPanel);
        
        playerHandPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        
        playerHandPanel.setBorder(new javax.swing.border.EtchedBorder());
        jPanel6.add(playerHandPanel);
        
        jScrollPane4.setViewportView(jPanel6);
        
        jPanel5.add(jScrollPane4, java.awt.BorderLayout.CENTER);
        
        getContentPane().add(jPanel5);
        jPanel5.setBounds(320, 20, 460, 670);
        
        jPanel3.setLayout(new java.awt.BorderLayout());
        
        jPanel3.setBorder(new javax.swing.border.TitledBorder(new javax.swing.border.EtchedBorder(), "Computer"));
        oppLifeLabel.setFont(new java.awt.Font("MS Sans Serif", 0, 40));
        oppLifeLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        oppLifeLabel.setText("19");
        oppLifeLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jPanel3.add(oppLifeLabel, java.awt.BorderLayout.CENTER);
        
        getContentPane().add(jPanel3);
        jPanel3.setBounds(210, 170, 90, 90);
        
        jPanel7.setLayout(new java.awt.BorderLayout());
        
        jPanel7.setBorder(new javax.swing.border.TitledBorder(new javax.swing.border.EtchedBorder(), "Player"));
        playerLifeLabel.setFont(new java.awt.Font("MS Sans Serif", 0, 40));
        playerLifeLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        playerLifeLabel.setText("19");
        playerLifeLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jPanel7.add(playerLifeLabel, java.awt.BorderLayout.CENTER);
        
        getContentPane().add(jPanel7);
        jPanel7.setBounds(210, 600, 90, 90);
        
        cdPanel.setLayout(new java.awt.GridLayout(2, 1, 0, 5));
        
        cdPanel.setBorder(new javax.swing.border.EtchedBorder());
        jPanel9.setLayout(new java.awt.GridLayout(5, 0, 0, 5));
        
        cdLabel1.setFont(getFont());
        cdLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        cdLabel1.setText("jLabel3");
        jPanel9.add(cdLabel1);
        
        cdLabel2.setFont(getFont());
        cdLabel2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        cdLabel2.setText("jLabel4");
        jPanel9.add(cdLabel2);
        
        cdLabel3.setFont(getFont());
        cdLabel3.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        cdLabel3.setText("jLabel5");
        jPanel9.add(cdLabel3);
        
        cdLabel4.setFont(getFont());
        cdLabel4.setText("jLabel6");
        jPanel9.add(cdLabel4);
        
        cdLabel5.setFont(getFont());
        cdLabel5.setText("jLabel7");
        jPanel9.add(cdLabel5);
        
        cdPanel.add(jPanel9);
        
        jPanel10.setLayout(new java.awt.BorderLayout());
        
        cdArea.setFont(getFont());
        cdArea.setLineWrap(true);
        cdArea.setWrapStyleWord(true);
        jScrollPane5.setViewportView(cdArea);
        
        jPanel10.add(jScrollPane5, java.awt.BorderLayout.CENTER);
        
        cdPanel.add(jPanel10);
        
        getContentPane().add(cdPanel);
        cdPanel.setBounds(790, 20, 230, 300);
        
        picturePanel.setLayout(new java.awt.BorderLayout());
        
        picturePanel.setBorder(new javax.swing.border.EtchedBorder());
        getContentPane().add(picturePanel);
        picturePanel.setBounds(790, 350, 230, 300);
        
        jLabel1.setFont(new java.awt.Font("MS Sans Serif", 0, 18));
        jLabel1.setText("Library:");
        getContentPane().add(jLabel1);
        jLabel1.setBounds(60, 200, 70, 20);
        
        jLabel2.setFont(new java.awt.Font("MS Sans Serif", 0, 18));
        jLabel2.setText("Hand:");
        getContentPane().add(jLabel2);
        jLabel2.setBounds(60, 170, 60, 20);
        
        jLabel3.setFont(new java.awt.Font("MS Sans Serif", 0, 18));
        jLabel3.setText("Grave:");
        getContentPane().add(jLabel3);
        jLabel3.setBounds(60, 230, 70, 20);
        
        oppHandLabel.setFont(new java.awt.Font("MS Sans Serif", 0, 18));
        oppHandLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        oppHandLabel.setText("7");
        oppHandLabel.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        getContentPane().add(oppHandLabel);
        oppHandLabel.setBounds(90, 170, 60, 20);
        
        oppLibraryLabel.setFont(new java.awt.Font("MS Sans Serif", 0, 18));
        oppLibraryLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        oppLibraryLabel.setText("60");
        oppLibraryLabel.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        getContentPane().add(oppLibraryLabel);
        oppLibraryLabel.setBounds(90, 200, 60, 20);
        
        oppGraveLabel.setFont(new java.awt.Font("MS Sans Serif", 0, 18));
        oppGraveLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        oppGraveLabel.setText("200");
        oppGraveLabel.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        getContentPane().add(oppGraveLabel);
        oppGraveLabel.setBounds(90, 230, 60, 20);
        
        jLabel7.setFont(new java.awt.Font("MS Sans Serif", 0, 18));
        jLabel7.setText("Hand:");
        getContentPane().add(jLabel7);
        jLabel7.setBounds(60, 610, 60, 20);
        
        jLabel8.setFont(new java.awt.Font("MS Sans Serif", 0, 18));
        jLabel8.setText("Library:");
        getContentPane().add(jLabel8);
        jLabel8.setBounds(60, 640, 60, 20);
        
        jLabel9.setFont(new java.awt.Font("MS Sans Serif", 0, 18));
        jLabel9.setText("Grave:");
        getContentPane().add(jLabel9);
        jLabel9.setBounds(60, 670, 60, 20);
        
        playerHandLabel.setFont(new java.awt.Font("MS Sans Serif", 0, 18));
        playerHandLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        playerHandLabel.setText("6");
        playerHandLabel.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        getContentPane().add(playerHandLabel);
        playerHandLabel.setBounds(90, 610, 60, 20);
        
        playerLibraryLabel.setFont(new java.awt.Font("MS Sans Serif", 0, 18));
        playerLibraryLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        playerLibraryLabel.setText("54");
        playerLibraryLabel.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        getContentPane().add(playerLibraryLabel);
        playerLibraryLabel.setBounds(90, 640, 60, 20);
        
        playerGraveLabel.setFont(new java.awt.Font("MS Sans Serif", 0, 18));
        playerGraveLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        playerGraveLabel.setText("0");
        playerGraveLabel.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        getContentPane().add(playerGraveLabel);
        playerGraveLabel.setBounds(90, 670, 60, 20);
        
        menuBar.setText("Menu");
        jMenuBar1.add(menuBar);
        
        setJMenuBar(jMenuBar1);
        
        pack();
    }//GEN-END:initComponents
    
    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cancelButtonActionPerformed
    {//GEN-HEADEREND:event_cancelButtonActionPerformed
        inputControl.selectButtonCancel();
    }//GEN-LAST:event_cancelButtonActionPerformed
    
    private void okButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_okButtonActionPerformed
    {//GEN-HEADEREND:event_okButtonActionPerformed
        inputControl.selectButtonOK();
    }//GEN-LAST:event_okButtonActionPerformed
    
    /** Exit the Application */
    private void exitForm(java.awt.event.WindowEvent evt)//GEN-FIRST:event_exitForm
    {
        dispose();
        Constant.Runtime.WinLose.addLose();
        new Gui_WinLose();
    }//GEN-LAST:event_exitForm
    

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton     cancelButton;
    private javax.swing.JTextArea   cdArea;
    private javax.swing.JLabel      cdLabel1;
    private javax.swing.JLabel      cdLabel2;
    private javax.swing.JLabel      cdLabel3;
    private javax.swing.JLabel      cdLabel4;
    private javax.swing.JLabel      cdLabel5;
    public javax.swing.JPanel       cdPanel;
    private javax.swing.JTextArea   combatArea;
    private javax.swing.JLabel      jLabel1;
    private javax.swing.JLabel      jLabel2;
    private javax.swing.JLabel      jLabel3;
    private javax.swing.JLabel      jLabel7;
    private javax.swing.JLabel      jLabel8;
    private javax.swing.JLabel      jLabel9;
    private javax.swing.JMenuBar    jMenuBar1;
    @SuppressWarnings("unused")
    // jMenuBar2
    private javax.swing.JMenuBar    jMenuBar2;
    private javax.swing.JPanel      jPanel1;
    private javax.swing.JPanel      jPanel10;
    private javax.swing.JPanel      jPanel2;
    private javax.swing.JPanel      jPanel3;
    private javax.swing.JPanel      jPanel5;
    private javax.swing.JPanel      jPanel6;
    private javax.swing.JPanel      jPanel7;
    private javax.swing.JPanel      jPanel9;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JMenu       menuBar;
    private javax.swing.JTextArea   messageArea;
    private javax.swing.JButton     okButton;
    private javax.swing.JPanel      oppCreaturePanel;
    private javax.swing.JLabel      oppGraveLabel;
    private javax.swing.JLabel      oppHandLabel;
    private javax.swing.JPanel      oppLandPanel;
    private javax.swing.JLabel      oppLibraryLabel;
    private javax.swing.JLabel      oppLifeLabel;
    private javax.swing.JPanel      picturePanel;
    private javax.swing.JPanel      playerCreaturePanel;
    private javax.swing.JLabel      playerGraveLabel;
    private javax.swing.JLabel      playerHandLabel;
    private javax.swing.JPanel      playerHandPanel;
    private javax.swing.JPanel      playerLandPanel;
    private javax.swing.JLabel      playerLibraryLabel;
    private javax.swing.JLabel      playerLifeLabel;
    private javax.swing.JPanel      stackPanel;
    // End of variables declaration//GEN-END:variables
    
}
