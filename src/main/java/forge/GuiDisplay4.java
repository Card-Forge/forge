package forge;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
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
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Observable;
import java.util.Observer;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import net.slightlymagic.braids.util.ImmutableIterableFrom;

import org.jdesktop.swingx.JXMultiSplitPane;
import org.jdesktop.swingx.MultiSplitLayout.Node;

import arcane.ui.HandArea;
import arcane.ui.PlayArea;
import arcane.ui.ViewPanel;
import arcane.ui.util.Animation;

import com.google.code.jyield.Generator;
import com.google.code.jyield.YieldUtils;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;

import forge.card.CardPrinted;
import forge.card.cardFactory.CardFactoryUtil;
import forge.error.ErrorViewer;
import forge.gui.ForgeAction;
import forge.gui.GuiUtils;
import forge.gui.game.CardDetailPanel;
import forge.gui.game.CardPanel;
import forge.gui.input.Input_Attack;
import forge.gui.input.Input_Block;
import forge.gui.input.Input_PayManaCost;
import forge.gui.input.Input_PayManaCost_Ability;
import forge.properties.ForgePreferences;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;
import forge.view.swing.OldGuiNewGame;


/**
 * <p>GuiDisplay4 class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class GuiDisplay4 extends JFrame implements CardContainer, Display, NewConstants, NewConstants.GUI.GuiDisplay, NewConstants.LANG.GuiDisplay {
    /** Constant <code>serialVersionUID=4519302185194841060L</code> */
    private static final long serialVersionUID = 4519302185194841060L;

    private GuiInput inputControl;

    Font statFont = new Font("Dialog", Font.PLAIN, 12);
    Font lifeFont = new Font("Dialog", Font.PLAIN, 40);
   // Font checkboxFont = new Font("Dialog", Font.PLAIN, 9);

    /** Constant <code>greenColor</code> */
    public static Color greenColor = new Color(0, 164, 0);

    private Action HUMAN_GRAVEYARD_ACTION;
    private Action HUMAN_REMOVED_ACTION;
    private Action HUMAN_FLASHBACK_ACTION;
    private Action COMPUTER_GRAVEYARD_ACTION;
    private Action COMPUTER_REMOVED_ACTION;
    private Action CONCEDE_ACTION;
    private Action HUMAN_DECKLIST_ACTION;
    //public Card cCardHQ;

    //private CardList multiBlockers = new CardList();

    /**
     * <p>Constructor for GuiDisplay4.</p>
     */
    public GuiDisplay4() {
        AllZone.setDisplay(this);
        setupActions();
        initComponents();

        addObservers();
        addListeners();
        addMenu();
        inputControl = new GuiInput();
    }

    /** {@inheritDoc} */
    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            //causes an error if put in the constructor, causes some random null pointer exception
            AllZone.getInputControl().updateObservers();

            //Use both so that when "un"maximizing, the frame isn't tiny
            setSize(1024, 740);
            setExtendedState(Frame.MAXIMIZED_BOTH);
        }
        super.setVisible(visible);
    }

    /** {@inheritDoc} */
    public void assignDamage(Card attacker, CardList blockers, int damage) {
        if (damage <= 0)
            return;
        new Gui_MultipleBlockers4(attacker, blockers, damage, this);
    }

    /**
     * <p>setupActions.</p>
     */
    private void setupActions() {
        HUMAN_GRAVEYARD_ACTION = new ZoneAction(AllZone.getHumanGraveyard(), HUMAN_GRAVEYARD);
        HUMAN_REMOVED_ACTION = new ZoneAction(AllZone.getHumanExile(), HUMAN_REMOVED);
        HUMAN_FLASHBACK_ACTION = new ZoneAction(AllZone.getHumanGraveyard(), HUMAN_FLASHBACK) {

            private static final long serialVersionUID = 8120331222693706164L;

            @Override
            protected Iterable<Card> getCardsAsIterable() {
            	return new ImmutableIterableFrom<Card>(CardFactoryUtil.getExternalZoneActivationCards(AllZone.getHumanPlayer()));
            }
            
            @Override
            protected void doAction(Card c) {
            	AllZone.getGameAction().playCard(c);
            }
        };
        COMPUTER_GRAVEYARD_ACTION = new ZoneAction(AllZone.getComputerGraveyard(), COMPUTER_GRAVEYARD);
        COMPUTER_REMOVED_ACTION = new ZoneAction(AllZone.getComputerExile(), COMPUTER_REMOVED);
        CONCEDE_ACTION = new ConcedeAction();
        
        HUMAN_DECKLIST_ACTION = new DeckListAction(HUMAN_DECKLIST);
    }

    /**
     * <p>addMenu.</p>
     */
    private void addMenu() {
        // Game Menu Creation
        Object[] obj = {
                HUMAN_DECKLIST_ACTION, HUMAN_GRAVEYARD_ACTION, HUMAN_REMOVED_ACTION, HUMAN_FLASHBACK_ACTION, COMPUTER_GRAVEYARD_ACTION,
                COMPUTER_REMOVED_ACTION, new JSeparator(),
                playsoundCheckboxForMenu, new JSeparator(), ErrorViewer.ALL_THREADS_ACTION,
                CONCEDE_ACTION};

        JMenu gameMenu = new JMenu(ForgeProps.getLocalized(MENU_BAR.MENU.TITLE));
        for (Object o : obj) {
            if (o instanceof ForgeAction) gameMenu.add(((ForgeAction) o).setupButton(new JMenuItem()));
            else if (o instanceof Action) gameMenu.add((Action) o);
            else if (o instanceof Component) gameMenu.add((Component) o);
            else throw new AssertionError();
        }

        // Phase Menu Creation
        JMenu gamePhases = new JMenu(ForgeProps.getLocalized(MENU_BAR.PHASE.TITLE));

        JMenuItem aiLabel = new JMenuItem("Computer");
        JMenuItem humanLabel = new JMenuItem("Human");

        Component[] objPhases = {aiLabel, cbAIUpkeep, cbAIDraw, cbAIBeginCombat,
                cbAIEndCombat, cbAIEndOfTurn, new JSeparator(),
                humanLabel, cbHumanUpkeep, cbHumanDraw, cbHumanBeginCombat,
                cbHumanEndCombat, cbHumanEndOfTurn};

        for (Component cmp : objPhases) {
            gamePhases.add(cmp);
        }

        // Dev Mode Creation
        JMenu devMenu = new JMenu(ForgeProps.getLocalized(MENU_BAR.DEV.TITLE));

        devMenu.setEnabled(Constant.Runtime.DevMode[0]);

        if (Constant.Runtime.DevMode[0]) {
            canLoseByDecking.setSelected(Constant.Runtime.Mill[0]);

            Action viewAIHand = new ZoneAction(AllZone.getComputerHand(), COMPUTER_HAND.BASE);
            Action viewAILibrary = new ZoneAction(AllZone.getComputerLibrary(), COMPUTER_LIBRARY.BASE);
            Action viewHumanLibrary = new ZoneAction(AllZone.getHumanLibrary(), HUMAN_LIBRARY.BASE);
            ForgeAction generateMana = new ForgeAction(MANAGEN) {
                private static final long serialVersionUID = 7171104690016706405L;

                public void actionPerformed(ActionEvent arg0) {
                    GuiDisplayUtil.devModeGenerateMana();
                }
            };

            // + Battlefield setup +
            ForgeAction setupBattleField = new ForgeAction(SETUPBATTLEFIELD) {
                private static final long serialVersionUID = -6660930759092583160L;

                public void actionPerformed(ActionEvent arg0) {
                    GuiDisplayUtil.devSetupGameState();
                }
            };
            // - Battlefield setup -

            //DevMode Tutor
            ForgeAction tutor = new ForgeAction(TUTOR) {
                private static final long serialVersionUID = 2003222642609217705L;

                public void actionPerformed(ActionEvent arg0) {
                    GuiDisplayUtil.devModeTutor();
                }
            };
            //end DevMode Tutor
            
            //DevMode AddCounter
            ForgeAction addCounter = new ForgeAction(ADDCOUNTER) {
				private static final long serialVersionUID = 3136264111882855268L;

				public void actionPerformed(ActionEvent arg0) {
            		GuiDisplayUtil.devModeAddCounter();
            	}
            };
            //end DevMode AddCounter

            //DevMode Tap
            ForgeAction tapPerm = new ForgeAction(TAPPERM) {
				private static final long serialVersionUID = -6092045653540313527L;

				public void actionPerformed(ActionEvent arg0) {
            		GuiDisplayUtil.devModeTapPerm();
            	}
            };
            //end DevMode Tap

            //DevMode Untap
            ForgeAction untapPerm = new ForgeAction(UNTAPPERM) {
				private static final long serialVersionUID = 5425291996157256656L;

				public void actionPerformed(ActionEvent arg0) {
            		GuiDisplayUtil.devModeUntapPerm();
            	}
            };
            //end DevMode Untap
            
            //DevMode UnlimitedLand
            ForgeAction unlimitedLand = new ForgeAction(NOLANDLIMIT) {
				private static final long serialVersionUID = 2184353891062202796L;

				public void actionPerformed(ActionEvent arg0) {
            		GuiDisplayUtil.devModeUnlimitedLand();
            	}
            };
            //end DevMode UnlimitedLand
            
            //DevMode SetLife
            ForgeAction setLife = new ForgeAction(SETLIFE) {
                private static final long serialVersionUID = -1750588303928974918L;

                public void actionPerformed(ActionEvent arg0) {
                    GuiDisplayUtil.devModeSetLife();
                }
            };
            //end DevMode SetLife

            Object[] objDev = {
            		GuiDisplay4.canLoseByDecking,
            		viewAIHand,
            		viewAILibrary,
            		viewHumanLibrary,
            		generateMana,
            		setupBattleField,
            		tutor,
            		addCounter,
            		tapPerm,
            		untapPerm,
            		unlimitedLand,
            		setLife
            };
            for (Object o : objDev) {
                if (o instanceof ForgeAction)
                    devMenu.add(((ForgeAction) o).setupButton(new JMenuItem()));
                else if (o instanceof Component)
                    devMenu.add((Component) o);
                else if (o instanceof Action)
                    devMenu.add((Action) o);
            }
        }

        JMenuBar menuBar = new JMenuBar();
        menuBar.add(gameMenu);
        menuBar.add(gamePhases);
        menuBar.add(devMenu);
        menuBar.add(new MenuItem_HowToPlay());
        this.setJMenuBar(menuBar);
    }//addMenu()

    /**
     * <p>canLoseByDecking.</p>
     *
     * @return a boolean.
     */
    public boolean canLoseByDecking() {
        return canLoseByDecking.isSelected();
    }

    /**
     * <p>getButtonOK.</p>
     *
     * @return a {@link forge.MyButton} object.
     */
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

    /**
     * <p>getButtonCancel.</p>
     *
     * @return a {@link forge.MyButton} object.
     */
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

    /** {@inheritDoc} */
    public void showCombat(String message) {
        combatArea.setText(message);
    }

    /** {@inheritDoc} */
    public void showMessage(String s) {
        messageArea.setText(s);

        Border border = null;
        int thickness = 3;

        if (AllZone.getStack().size() > 0 && AllZone.getStack().peekInstance().getActivatingPlayer().isComputer())
            border = BorderFactory.createLineBorder(new Color(0, 255, 255), thickness);
        else if (s.contains("Main"))
            border = BorderFactory.createLineBorder(new Color(30, 0, 255), thickness);
        else if (s.contains("To Block"))
            border = BorderFactory.createLineBorder(new Color(13, 179, 0), thickness);
        else if (s.contains("Play Instants and Abilities") || s.contains("Combat") || s.contains("Damage"))
            border = BorderFactory.createLineBorder(new Color(255, 174, 0), thickness);
        else if (s.contains("Declare Attackers"))
            border = BorderFactory.createLineBorder(new Color(255, 0, 0), thickness);
        else if (s.contains("Upkeep") || s.contains("Draw") || s.contains("End of Turn"))
            border = BorderFactory.createLineBorder(new Color(200, 0, 170), thickness);
        else
            border = new EmptyBorder(1, 1, 1, 1);

        messageArea.setBorder(border);
    }

    /**
     * <p>addListeners.</p>
     */
    private void addListeners() {
        //mouse Card Detail
        playerHandPanel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent me) {
                Card c = playerHandPanel.getCardFromMouseOverPanel();
                if (c != null) {
                    setCard(c);
                }
            }//mouseMoved
        });

        playerPlayPanel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent me) {
                Card c = playerPlayPanel.getCardFromMouseOverPanel();
                if (c != null) {
                    setCard(c);
                }
            }//mouseMoved
        });

        oppPlayPanel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent me) {
                Card c = oppPlayPanel.getCardFromMouseOverPanel();
                if (c != null) {
                    setCard(c);
                }
            }//mouseMoved
        });


        //opponent life mouse listener
        oppLifeLabel.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                inputControl.selectPlayer(AllZone.getComputerPlayer());
            }
        });

        oppLifeLabel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent me) {
                setCard(AllZone.getComputerManaPool());
            }//mouseMoved
        });

        //self life mouse listener
        playerLifeLabel.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                inputControl.selectPlayer(AllZone.getHumanPlayer());
            }
        });

        //self play (land) ---- Mouse
        playerPlayPanel.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                Card c = playerPlayPanel.getCardFromMouseOverPanel();
                if (c != null) {

                    if (c.isTapped()
                            && (inputControl.input instanceof Input_PayManaCost || inputControl.input instanceof Input_PayManaCost_Ability)) {
                        arcane.ui.CardPanel cardPanel = playerPlayPanel.getCardPanel(c.getUniqueNumber());
                        for (arcane.ui.CardPanel cp : cardPanel.attachedPanels) {
                            if (cp.getCard().isUntapped()) {
                                break;
                            }
                        }
                    }

                    CardList att = new CardList(AllZone.getCombat().getAttackers());
                    if ((c.isTapped() || c.hasSickness() || ((c.hasKeyword("Vigilance")) && att.contains(c)))
                            && (inputControl.input instanceof Input_Attack)) {
                        arcane.ui.CardPanel cardPanel = playerPlayPanel.getCardPanel(c.getUniqueNumber());
                        for (arcane.ui.CardPanel cp : cardPanel.attachedPanels) {
                            if (cp.getCard().isUntapped() && !cp.getCard().hasSickness()) {
                                break;
                            }
                        }
                    }

                    if (e.isMetaDown()) {
                        if (att.contains(c) && (inputControl.input instanceof Input_Attack)
                                && !c.hasKeyword("CARDNAME attacks each turn if able.")) {
                            c.untap();
                            AllZone.getCombat().removeFromCombat(c);
                        } else if (inputControl.input instanceof Input_Block) {
                            if (c.getController().isHuman())
                                AllZone.getCombat().removeFromCombat(c);
                            ((Input_Block) inputControl.input).removeFromAllBlocking(c);
                        }
                    } else inputControl.selectCard(c, AllZone.getHumanBattlefield());
                }
            }
        });

        //self hand ---- Mouse
        playerHandPanel.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() != MouseEvent.BUTTON1) return;
                Card c = playerHandPanel.getCardFromMouseOverPanel();
                if (c != null) {
                    inputControl.selectCard(c, AllZone.getHumanHand());
                    okButton.requestFocusInWindow();
                }
            }
        });

        //*****************************************************************
        //computer

        //computer play (land) ---- Mouse
        oppPlayPanel.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                Card c = oppPlayPanel.getCardFromMouseOverPanel();
                if (c != null) {
                    inputControl.selectCard(c, AllZone.getComputerBattlefield());
                }
            }
        });


    }//addListener()

    /**
     * <p>getCard.</p>
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
     * <p>addObservers.</p>
     */
    private void addObservers() {
        //Human Hand, Graveyard, and Library totals
        {//make sure to not interfer with anything below, since this is a very long method
            Observer o = new Observer() {
                public void update(Observable a, Object b) {
                    playerHandValue.setText("" + AllZone.getHumanHand().size());
                    playerGraveValue.setText("" + AllZone.getHumanGraveyard().size());
                    playerLibraryValue.setText("" + AllZone.getHumanLibrary().size());
                    playerFBValue.setText("" + CardFactoryUtil.getExternalZoneActivationCards(AllZone.getHumanPlayer()).size());
                    playerRemovedValue.setText("" + AllZone.getHumanExile().size());

                }
            };
            AllZone.getHumanHand().addObserver(o);
            AllZone.getHumanGraveyard().addObserver(o);
            AllZone.getHumanLibrary().addObserver(o);
        }

        //opponent Hand, Graveyard, and Library totals
        {//make sure to not interfer with anything below, since this is a very long method
            Observer o = new Observer() {
                public void update(Observable a, Object b) {
                    oppHandValue.setText("" + AllZone.getComputerHand().size());
                    oppGraveValue.setText("" + AllZone.getComputerGraveyard().size());
                    oppLibraryValue.setText("" + AllZone.getComputerLibrary().size());
                    oppRemovedValue.setText("" + AllZone.getComputerExile().size());
                }
            };
            AllZone.getComputerHand().addObserver(o);
            AllZone.getComputerGraveyard().addObserver(o);
            AllZone.getComputerLibrary().addObserver(o);
        }


        //opponent life
        oppLifeLabel.setText("" + AllZone.getComputerPlayer().getLife());
        AllZone.getComputerPlayer().addObserver(new Observer() {
            public void update(Observable a, Object b) {
                int life = AllZone.getComputerPlayer().getLife();
                oppLifeLabel.setText("" + life);
            }
        });
        AllZone.getComputerPlayer().updateObservers();

        if (AllZone.getQuestData() != null) {
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

        oppPCLabel.setText("Poison Counters: " + AllZone.getComputerPlayer().getPoisonCounters());
        AllZone.getComputerPlayer().addObserver(new Observer() {
            public void update(Observable a, Object b) {
                int pcs = AllZone.getComputerPlayer().getPoisonCounters();
                oppPCLabel.setText("Poison Counters: " + pcs);
            }
        });
        AllZone.getComputerPlayer().updateObservers();

        //player life
        playerLifeLabel.setText("" + AllZone.getHumanPlayer().getLife());
        AllZone.getHumanPlayer().addObserver(new Observer() {
            public void update(Observable a, Object b) {
                int life = AllZone.getHumanPlayer().getLife();
                playerLifeLabel.setText("" + life);
            }
        });
        AllZone.getHumanPlayer().updateObservers();

        playerPCLabel.setText("Poison Counters: " + AllZone.getHumanPlayer().getPoisonCounters());
        AllZone.getHumanPlayer().addObserver(new Observer() {
            public void update(Observable a, Object b) {
                int pcs = AllZone.getHumanPlayer().getPoisonCounters();
                playerPCLabel.setText("Poison Counters: " + pcs);
            }
        });
        AllZone.getHumanPlayer().updateObservers();

        //stack
        AllZone.getStack().addObserver(new Observer() {
            public void update(Observable a, Object b) {
                stackPanel.removeAll();
                MagicStack stack = AllZone.getStack();
                int count = 1;
                JLabel label;

                for (int i = stack.size() - 1; 0 <= i; i--) {
                    label = new JLabel("" + (count++) + ". " + stack.peekInstance(i).getStackDescription());

                    //update card detail
                    final CardPanel cardPanel = new CardPanel(stack.peekInstance(i).getSourceCard());
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
        AllZone.getStack().updateObservers();
        //END, stack


        //self hand
        AllZone.getHumanHand().addObserver(new Observer() {
            public void update(Observable a, Object b) {
                PlayerZone pZone = (PlayerZone) a;
                HandArea p = playerHandPanel;
                ;

                Card c[] = AllZoneUtil.getCardsInZone(pZone).toArray();

                List<Card> tmp, diff;
                tmp = new ArrayList<Card>();
                for (arcane.ui.CardPanel cpa : p.cardPanels)
                    tmp.add(cpa.gameCard);
                diff = new ArrayList<Card>(tmp);
                diff.removeAll(Arrays.asList(c));
                if (diff.size() == p.cardPanels.size())
                    p.clear();
                else {
                    for (Card card : diff) {
                        p.removeCardPanel(p.getCardPanel(card.getUniqueNumber()));
                    }
                }
                diff = new ArrayList<Card>(Arrays.asList(c));
                diff.removeAll(tmp);

                int fromZoneX = 0, fromZoneY = 0;
                Rectangle pb = playerLibraryValue.getBounds();
                Point zoneLocation = SwingUtilities.convertPoint(playerLibraryValue, Math.round(pb.width / 2.0f), Math.round(pb.height / 2.0f), layeredPane);
                fromZoneX = zoneLocation.x;
                fromZoneY = zoneLocation.y;
                int startWidth, startX, startY;
                startWidth = 10;
                startX = fromZoneX - Math.round(startWidth / 2.0f);
                startY = fromZoneY - Math.round(Math.round(startWidth * arcane.ui.CardPanel.ASPECT_RATIO) / 2.0f);

                int endWidth, endX, endY;
                arcane.ui.CardPanel toPanel = null;

                for (Card card : diff) {
                    toPanel = p.addCard(card);
                    endWidth = toPanel.getCardWidth();
                    Point toPos = SwingUtilities.convertPoint(playerHandPanel, toPanel.getCardLocation(), layeredPane);
                    endX = toPos.x;
                    endY = toPos.y;
                    arcane.ui.CardPanel animationPanel = new arcane.ui.CardPanel(card);
                    if (isShowing())
                        Animation.moveCard(startX, startY, startWidth, endX, endY, endWidth, animationPanel, toPanel, layeredPane, 500);
                    else
                        Animation.moveCard(toPanel);
                }
            }
        });
        AllZone.getHumanHand().updateObservers();
        //END, self hand

        //self play
        AllZone.getHumanBattlefield().addObserver(new Observer() {
            public void update(Observable a, Object b) {
                PlayerZone pZone = (PlayerZone) a;

                Card c[] = AllZoneUtil.getCardsInZone(pZone).toArray();

                GuiDisplayUtil.setupPlayZone(playerPlayPanel, c);
            }
        });
        AllZone.getHumanBattlefield().updateObservers();
        //END - self play


        //computer play
        AllZone.getComputerBattlefield().addObserver(new Observer() {
            public void update(Observable a, Object b) {
                PlayerZone pZone = (PlayerZone) a;

                Card c[] = AllZoneUtil.getCardsInZone(pZone).toArray();

                GuiDisplayUtil.setupPlayZone(oppPlayPanel, c);
            }
        });
        AllZone.getComputerBattlefield().updateObservers();
        //END - computer play

    }//addObservers()

    /**
     * <p>initComponents.</p>
     */
    private void initComponents() {
        //Preparing the Frame
        setTitle(ForgeProps.getLocalized(LANG.PROGRAM_NAME));
        if (!OldGuiNewGame.useLAFFonts.isSelected()) {
            setFont(new Font("Times New Roman", 0, 16));
        }
        getContentPane().setLayout(new BorderLayout());

        // I tried using the JavaBeanConverter with this, but I got a
        // StackOverflowError due to an infinite loop.  The default
        // XStream format seems just fine, anyway.
        final XStream xstream = new XStream();


        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent evt) {
                concede();
            }

            @Override
            public void windowClosed(final WindowEvent evt) {

                // Write the layout to the new file, usually
                // res/gui/display_new_layout.xml
                File f = ForgeProps.getFile(LAYOUT_NEW);

                Node layout = pane.getMultiSplitLayout().getModel();

                BufferedOutputStream out = null;
                try {
                    out = new BufferedOutputStream(new FileOutputStream(f));
                    xstream.toXML(layout, out);
                }
                catch (IOException ex) {
                    assert System.err != null;

                    System.err.println("Ignoring exception:");
                    ex.printStackTrace(); // NOPMD by Braids on 8/21/11 9:20 PM
                    System.err.println("-------------------");
                }
                finally {
                    if (out != null) {
                        try {
                            out.close();
                        } catch (Throwable ignored) { // NOPMD by Braids on 8/21/11 9:20 PM
                            // Ignore failure to close.
                        }
                    }
                }
            }
        });

        //making the multi split pane
        Node model;

        // Try to load the latest saved layout, usually
        // res/gui/display_new_layout.xml
        final File file = ForgeProps.getFile(LAYOUT_NEW);

        try {
            model = loadModel(xstream, file);
        }
        catch (XStreamException xse) {
            assert System.err != null;

            System.err.println("Error loading '" + file.getAbsolutePath() + "' using XStream: "
                    + xse.getLocalizedMessage());

            model = null;
        }
        catch (FileNotFoundException e1) {
            model = null;
        }

        if (model == null) {
            assert System.err != null;

            System.err.println("Unable to parse file '" + file.getAbsolutePath()
                    + "' with XStream; trying XMLDecoder.");

            try {
                model = loadModelUsingXMLDecoder(file);
            }
            catch (FileNotFoundException e1) {
                model = null;
            }
            catch (Throwable exn) { // NOPMD by Braids on 8/21/11 9:20 PM
                System.err.println("Ignoring exception:");
                exn.printStackTrace(); // NOPMD by Braids on 8/21/11 9:20 PM
                System.err.println("-------------------");

                model = null;
            }
        }

        if (model == null) {
            System.err.println("XMLDecoder failed; using default layout.");
            final File defaultFile = ForgeProps.getFile(LAYOUT);

            try {
                model = loadModel(xstream, defaultFile);
            }
            catch (Exception exn) {
                // Should never happen.
                throw new RuntimeException(exn); // NOPMD by Braids on 8/21/11 9:18 PM

                /*
                 * This code is useful for bootstrapping a display_new_layout.xml file.
                 * 
                System.err.println("Unable to parse file '" + defaultFile.getAbsolutePath()
                        + "' with XStream; using hard coded defaults.");

                model = parseModel(""//
                        + "(ROW "//
                        + "(COLUMN"//
                        + " (LEAF weight=0.2 name=info)"//
                        + " (LEAF weight=0.2 name=compy)"//
                        + " (LEAF weight=0.2 name=stack)"//
                        + " (LEAF weight=0.2 name=combat)"//
                        + " (LEAF weight=0.2 name=human)) "//
                        + "(COLUMN weight=1"//
                        + " (LEAF weight=0.4 name=compyPlay)"//
                        + " (LEAF weight=0.4 name=humanPlay)"//
                        + " (LEAF weight=0.2 name=humanHand)) "//
                        + "(COLUMN"//
                        + " (LEAF weight=0.5 name=detail)"//
                        + " (LEAF weight=0.5 name=picture)))");

                pane.setModel(model);
                 *
                 */
            }
        }

        if (model != null) {
            pane.getMultiSplitLayout().setModel(model);
        }

        pane.getMultiSplitLayout().setFloatingDividers(false);
        getContentPane().add(pane);

        //adding the individual parts

        if (!OldGuiNewGame.useLAFFonts.isSelected()) {
            initFonts(pane);
        }

        initMsgYesNo(pane);
        initOpp(pane);
        initStackCombat(pane);
        initPlayer(pane);
        initZones(pane);
        initCardPicture(pane);
    }

    /**
     * Load the panel size preferences from a the given file using XStream.
     * 
     * <p>
     * throws XStreamException  if there was a parsing error
     * </p>
     * 
     * @param xstream  the XStream parser to use; do not use JavaBeanConverter!
     * @param file  the XML file containing the preferences
     * @return  the preferences model as a Node instance
     * @throws FileNotFoundException  if file does not exist
     */
    public static Node loadModel(final XStream xstream, final File file) throws FileNotFoundException
    {
        BufferedInputStream bufferedIn = null;
        Node model = null;
        try {
            bufferedIn = new BufferedInputStream(new FileInputStream(file));
            model = (Node) xstream.fromXML(bufferedIn);
        }
        finally {
            try {
                if (bufferedIn != null) {
                    bufferedIn.close();
                }
            } catch (Throwable ignored) { // NOPMD by Braids on 8/21/11 9:20 PM
                // Ignore exceptions on close.
            }
        }
        return model;
    }

    /**
     * Load the panel size preferences from a the given file using the old
     * XMLDecoder format.  XStream is preferred.
     * 
     * @param file  the XML file containing the preferences
     * @return  the preferences model as a Node instance
     * @throws FileNotFoundException  if file does not exist
     */
    public static Node loadModelUsingXMLDecoder(final File file) throws FileNotFoundException {
        BufferedInputStream bufferedIn = null;
        Node model = null;
        XMLDecoder decoder = null;
        try {
            bufferedIn = new BufferedInputStream(new FileInputStream(file));
            decoder = new XMLDecoder(bufferedIn);
            model = (Node) decoder.readObject();
        }
        finally {
            try {
                if (decoder != null) {
                    decoder.close();
                }
            } catch (Throwable ignored) { // NOPMD by Braids on 8/21/11 9:20 PM
                // Ignore exceptions on close.
            }

            try {
                if (bufferedIn != null) {
                    bufferedIn.close();
                }
            } catch (Throwable ignored) { // NOPMD by Braids on 8/21/11 9:20 PM
                // Ignore exceptions on close.
            }
        }
        return model;
    }

    /**
     * <p>initFonts.</p>
     *
     * @param pane a {@link javax.swing.JPanel} object.
     */
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

    /**
     * <p>initMsgYesNo.</p>
     *
     * @param pane a {@link javax.swing.JPanel} object.
     */
    private void initMsgYesNo(JPanel pane) {
        //messageArea.setBorder(BorderFactory.createEtchedBorder());
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

                if (AllZone.getPhase().isNeedToNextPhase()) {
                    // moves to next turn
                    AllZone.getPhase().setNeedToNextPhase(false);
                    AllZone.getPhase().nextPhase();
                }
                okButton.requestFocusInWindow();
            }
        });
        okButton.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent arg0) {
                // TODO make triggers on escape
                int code = arg0.getKeyCode();
                if (code == KeyEvent.VK_ESCAPE) {
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

    /**
     * <p>initOpp.</p>
     *
     * @param pane a {@link javax.swing.JPanel} object.
     */
    private void initOpp(JPanel pane) {
        //oppLifeLabel.setHorizontalAlignment(SwingConstants.CENTER);

        //oppPCLabel.setHorizontalAlignment(SwingConstants.TOP);
        oppPCLabel.setForeground(greenColor);

        JLabel oppHandLabel = new JLabel(ForgeProps.getLocalized(COMPUTER_HAND.BUTTON), SwingConstants.TRAILING);
        if (!OldGuiNewGame.useLAFFonts.isSelected()) oppHandLabel.setFont(statFont);

        JButton oppGraveButton = new JButton(COMPUTER_GRAVEYARD_ACTION);
        oppGraveButton.setText((String) COMPUTER_GRAVEYARD_ACTION.getValue("buttonText"));
        oppGraveButton.setMargin(new Insets(0, 0, 0, 0));
        oppGraveButton.setHorizontalAlignment(SwingConstants.TRAILING);
        if (!OldGuiNewGame.useLAFFonts.isSelected()) oppGraveButton.setFont(statFont);


        JPanel gravePanel = new JPanel(new BorderLayout());
        gravePanel.add(oppGraveButton, BorderLayout.EAST);

        JButton oppRemovedButton = new JButton(COMPUTER_REMOVED_ACTION);
        oppRemovedButton.setText((String) COMPUTER_REMOVED_ACTION.getValue("buttonText"));
        oppRemovedButton.setMargin(new Insets(0, 0, 0, 0));
        //removedButton.setHorizontalAlignment(SwingConstants.TRAILING);
        if (!OldGuiNewGame.useLAFFonts.isSelected()) oppRemovedButton.setFont(statFont);


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

    /**
     * <p>initStackCombat.</p>
     *
     * @param pane a {@link javax.swing.JPanel} object.
     */
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

    /**
     * <p>initPlayer.</p>
     *
     * @param pane a {@link javax.swing.JPanel} object.
     */
    private void initPlayer(JPanel pane) {
        //int fontSize = 12;
        playerLifeLabel.setHorizontalAlignment(SwingConstants.CENTER);

        playerPCLabel.setForeground(greenColor);

        JLabel playerLibraryLabel = new JLabel(ForgeProps.getLocalized(HUMAN_LIBRARY.BUTTON),
                SwingConstants.TRAILING);
        if (!OldGuiNewGame.useLAFFonts.isSelected()) playerLibraryLabel.setFont(statFont);

        JLabel playerHandLabel = new JLabel(ForgeProps.getLocalized(HUMAN_HAND.TITLE), SwingConstants.TRAILING);
        if (!OldGuiNewGame.useLAFFonts.isSelected()) playerHandLabel.setFont(statFont);

        //JLabel playerGraveLabel = new JLabel("Grave:", SwingConstants.TRAILING);
        JButton playerGraveButton = new JButton(HUMAN_GRAVEYARD_ACTION);
        playerGraveButton.setText((String) HUMAN_GRAVEYARD_ACTION.getValue("buttonText"));
        playerGraveButton.setMargin(new Insets(0, 0, 0, 0));
        playerGraveButton.setHorizontalAlignment(SwingConstants.TRAILING);
        if (!OldGuiNewGame.useLAFFonts.isSelected()) playerGraveButton.setFont(statFont);


        JButton playerFlashBackButton = new JButton(HUMAN_FLASHBACK_ACTION);
        playerFlashBackButton.setText((String) HUMAN_FLASHBACK_ACTION.getValue("buttonText"));
        playerFlashBackButton.setMargin(new Insets(0, 0, 0, 0));
        playerFlashBackButton.setHorizontalAlignment(SwingConstants.TRAILING);
        if (!OldGuiNewGame.useLAFFonts.isSelected()) playerFlashBackButton.setFont(statFont);


        JPanel gravePanel = new JPanel(new BorderLayout());
        gravePanel.add(playerGraveButton, BorderLayout.EAST);

        JPanel playerFBPanel = new JPanel(new BorderLayout());
        playerFBPanel.add(playerFlashBackButton, BorderLayout.EAST);

        JButton playerRemovedButton = new JButton(HUMAN_REMOVED_ACTION);
        playerRemovedButton.setText((String) HUMAN_REMOVED_ACTION.getValue("buttonText"));
        playerRemovedButton.setMargin(new Insets(0, 0, 0, 0));
        //removedButton.setHorizontalAlignment(SwingConstants.TRAILING);
        if (!OldGuiNewGame.useLAFFonts.isSelected()) playerRemovedButton.setFont(statFont);

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

    /**
     * <p>initZones.</p>
     *
     * @param pane a {@link javax.swing.JPanel} object.
     */
    private void initZones(JPanel pane) {
        JScrollPane oppScroll = new JScrollPane();
        oppPlayPanel = new PlayArea(oppScroll, true);
        oppScroll.setBorder(BorderFactory.createEtchedBorder());
        oppScroll.setViewportView(oppPlayPanel);
        pane.add(new ExternalPanel(oppScroll), "compyPlay");

        JScrollPane playScroll = new JScrollPane();
        playerPlayPanel = new PlayArea(playScroll, false);
        playScroll.setBorder(BorderFactory.createEtchedBorder());
        playScroll.setViewportView(playerPlayPanel);
        pane.add(new ExternalPanel(playScroll), "humanPlay");

        JScrollPane handScroll = new JScrollPane();
        playerHandPanel = new HandArea(handScroll, this);
        playerHandPanel.setBorder(BorderFactory.createEtchedBorder());
        handScroll.setViewportView(playerHandPanel);
        pane.add(new ExternalPanel(handScroll), "humanHand");
    }

    /**
     * <p>initCardPicture.</p>
     *
     * @param pane a {@link javax.swing.JPanel} object.
     */
    private void initCardPicture(JPanel pane) {
        pane.add(new ExternalPanel(detail), "detail");
        pane.add(new ExternalPanel(picturePanel), "picture");
        picturePanel.setCardPanel(picture);
    }

    /**
     * <p>cancelButtonActionPerformed.</p>
     *
     * @param evt a {@link java.awt.event.ActionEvent} object.
     */
    private void cancelButtonActionPerformed(ActionEvent evt) {
        inputControl.selectButtonCancel();
    }

    /**
     * <p>okButtonActionPerformed.</p>
     *
     * @param evt a {@link java.awt.event.ActionEvent} object.
     */
    private void okButtonActionPerformed(ActionEvent evt) {
        inputControl.selectButtonOK();
    }

    /**
     * Exit the Application
     */
    private void concede() {
        AllZone.getHumanPlayer().concede();
        AllZone.getGameAction().checkStateEffects();
    }

    // ********** Phase stuff in Display ******************
    /** {@inheritDoc} */
    public boolean stopAtPhase(Player turn, String phase) {
        if (turn.isComputer()) {
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
        } else {
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
        return true;
    }

    /**
     * <p>loadPrefs.</p>
     *
     * @return a boolean.
     */
    public boolean loadPrefs() {
        ForgePreferences fp = Singletons.getModel().getPreferences();

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

        canLoseByDecking.setSelected(fp.millingLossCondition);

        return true;
    }

    /**
     * <p>savePrefs.</p>
     *
     * @return a boolean.
     */
    public boolean savePrefs() {
        Constant.Runtime.Mill[0] = canLoseByDecking.isSelected();
        ForgePreferences fp = Singletons.getModel().getPreferences();

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

        fp.millingLossCondition = canLoseByDecking.isSelected();

        return true;
    }

    /** Constant <code>playsoundCheckboxForMenu</code> */
    public static JCheckBoxMenuItem playsoundCheckboxForMenu = new JCheckBoxMenuItem("Play Sound", false);

    // Phases
    /** Constant <code>cbAIUpkeep</code> */
    public static JCheckBoxMenuItem cbAIUpkeep = new JCheckBoxMenuItem("Upkeep", true);
    /** Constant <code>cbAIDraw</code> */
    public static JCheckBoxMenuItem cbAIDraw = new JCheckBoxMenuItem("Draw", true);
    /** Constant <code>cbAIEndOfTurn</code> */
    public static JCheckBoxMenuItem cbAIEndOfTurn = new JCheckBoxMenuItem("End of Turn", true);
    /** Constant <code>cbAIBeginCombat</code> */
    public static JCheckBoxMenuItem cbAIBeginCombat = new JCheckBoxMenuItem("Begin Combat", true);
    /** Constant <code>cbAIEndCombat</code> */
    public static JCheckBoxMenuItem cbAIEndCombat = new JCheckBoxMenuItem("End Combat", true);

    /** Constant <code>cbHumanUpkeep</code> */
    public static JCheckBoxMenuItem cbHumanUpkeep = new JCheckBoxMenuItem("Upkeep", true);
    /** Constant <code>cbHumanDraw</code> */
    public static JCheckBoxMenuItem cbHumanDraw = new JCheckBoxMenuItem("Draw", true);
    /** Constant <code>cbHumanEndOfTurn</code> */
    public static JCheckBoxMenuItem cbHumanEndOfTurn = new JCheckBoxMenuItem("End of Turn", true);
    /** Constant <code>cbHumanBeginCombat</code> */
    public static JCheckBoxMenuItem cbHumanBeginCombat = new JCheckBoxMenuItem("Begin Combat", true);
    /** Constant <code>cbHumanEndCombat</code> */
    public static JCheckBoxMenuItem cbHumanEndCombat = new JCheckBoxMenuItem("End Combat", true);

    // ********** End of Phase stuff in Display ******************

    // ****** Developer Mode ******* 

    /** Constant <code>canLoseByDecking</code> */
    public static JCheckBoxMenuItem canLoseByDecking = new JCheckBoxMenuItem("Lose by Decking", true);

    // *****************************


    JXMultiSplitPane pane = new JXMultiSplitPane();
    JButton cancelButton = new JButton();
    JButton okButton = new JButton();
    JTextArea messageArea = new JTextArea(1, 10);
    JTextArea combatArea = new JTextArea();
    JPanel stackPanel = new JPanel();
    PlayArea oppPlayPanel = null;
    PlayArea playerPlayPanel = null;
    HandArea playerHandPanel = null;
    //JPanel cdPanel = new JPanel();
    JLabel oppLifeLabel = new JLabel();
    JLabel oppIconLabel = new JLabel();
    JLabel playerLifeLabel = new JLabel();
    JLabel oppPCLabel = new JLabel();
    JLabel playerPCLabel = new JLabel();
    JLabel oppLibraryLabel = new JLabel(
            ForgeProps.getLocalized(COMPUTER_LIBRARY.BUTTON),
            SwingConstants.TRAILING);
    JLabel oppHandValue = new JLabel();
    JLabel oppLibraryValue = new JLabel();
    JLabel oppGraveValue = new JLabel();
    JLabel oppRemovedValue = new JLabel();
    JLabel playerHandValue = new JLabel();
    JLabel playerLibraryValue = new JLabel();
    JLabel playerGraveValue = new JLabel();
    JLabel playerFBValue = new JLabel();
    JLabel playerRemovedValue = new JLabel();

    CardDetailPanel detail = new CardDetailPanel(null);
    ViewPanel picturePanel = new ViewPanel();
    arcane.ui.CardPanel picture = new arcane.ui.CardPanel(null);
    JLayeredPane layeredPane = SwingUtilities.getRootPane(this).getLayeredPane();

    private class ZoneAction extends ForgeAction {
        private static final long serialVersionUID = -5822976087772388839L;
        private PlayerZone zone;
        private String title;

        public ZoneAction(PlayerZone zone, String property) {
            super(property);
            title = ForgeProps.getLocalized(property + "/title");
            this.zone = zone;
        }

        public void actionPerformed(ActionEvent e) {
            Generator<Card> c = YieldUtils.toGenerator(getCardsAsIterable());

            if (AllZone.getNameChanger().shouldChangeCardName()) {
            	c = AllZone.getNameChanger().changeCard(c);
            }
            
            Iterable<Card> myIterable = YieldUtils.toIterable(c);
            ArrayList<Card> choices = YieldUtils.toArrayList(myIterable);
            //System.out.println("immediately after: "+choices);
            //Iterator<Card> iter = myIterable.iterator();
            
            ArrayList<Card> choices2 = new ArrayList<Card>();
            
            if (choices.isEmpty()) {
            	GuiUtils.getChoiceOptional(title, new String[]{"no cards"});
            }
            else {
                for(int i = 0; i < choices.size(); i++) {
                    Card crd = choices.get(i);
                    //System.out.println(crd+": "+crd.isFaceDown());
                    if(crd.isFaceDown()) {
                        Card faceDown = new Card();
                        faceDown.setName("Face Down");
                        choices2.add(faceDown);
                        //System.out.println("Added: "+faceDown);
                    }
                    else {
                        choices2.add(crd);
                    }
                }
                //System.out.println("Face down cards replaced: "+choices2);
                Card choice = (Card) GuiUtils.getChoiceOptional(title, choices2.toArray());
                if (choice != null) doAction(choice);
                /*
                    Card choice = GuiUtils.getChoiceOptional(title, iter);
                    if (choice != null) doAction(choice);
                 */
            }
        }

        /**
         * @deprecated
         * @see #getCardsAsIterable()
         */
        @SuppressWarnings("unused")
		protected Card[] getCards() {
            return AllZoneUtil.getCardsInZone(zone).toArray();
        }
        
        protected Iterable<Card> getCardsAsIterable() {
        	return new ImmutableIterableFrom<Card>(AllZoneUtil.getCardsInZone(zone));
        }

        protected void doAction(Card c) {
        }
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
    
    private class DeckListAction extends ForgeAction {
        public DeckListAction(String property) {
            super(property);
        }

        private static final long serialVersionUID = 9874492387239847L;
                
        public void actionPerformed(ActionEvent e) {
            if (Constant.Runtime.HumanDeck[0].countMain() > 1) {
                HashMap<String, Integer> deckMap = new HashMap<String, Integer>();
                
                for (Entry<CardPrinted, Integer> s : Constant.Runtime.HumanDeck[0].getMain()){
                    deckMap.put(s.getKey().getName(), s.getValue());
                }
                
                String nl = System.getProperty("line.separator");
                StringBuilder DeckList = new StringBuilder();
                String dName = Constant.Runtime.HumanDeck[0].getName();
                
                if (dName == null)
                    dName = "";
                else
                    DeckList.append(dName + nl);
                
                ArrayList<String> dmKeys = new ArrayList<String>();
                for (String s : deckMap.keySet()) 
                    dmKeys.add(s);
                
                Collections.sort(dmKeys);
                        
                for (String s : dmKeys) {
                    DeckList.append(deckMap.get(s) + " x " + s + nl);
                }
                
                int rcMsg = -1138;
                String ttl = "Human's Decklist";
                if (!dName.equals(""))
                        ttl += " - " + dName;
                
                StringBuilder msg = new StringBuilder();
                if (deckMap.keySet().size() <= 32) 
                    msg.append(DeckList.toString() + nl);
                else    
                    msg.append("Decklist too long for dialog." + nl + nl);
                
                msg.append("Copy Decklist to Clipboard?");
                
                rcMsg = JOptionPane.showConfirmDialog(null, msg, ttl, JOptionPane.OK_CANCEL_OPTION);
                
                if (rcMsg == JOptionPane.OK_OPTION) {
                    StringSelection ss = new StringSelection(DeckList.toString());
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, null);
                }
            }
        }
    }
}

