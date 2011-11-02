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
import javax.swing.JPopupMenu;
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

import forge.Constant.Zone;
import forge.card.cardfactory.CardFactoryUtil;
import forge.error.ErrorViewer;
import forge.gui.ForgeAction;
import forge.gui.GuiUtils;
import forge.gui.game.CardDetailPanel;
import forge.gui.game.CardPanel;
import forge.gui.input.InputAttack;
import forge.gui.input.InputBlock;
import forge.gui.input.InputPayManaCost;
import forge.gui.input.InputPayManaCostAbility;
import forge.item.CardPrinted;
import forge.properties.ForgePreferences;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;

/**
 * <p>
 * GuiDisplay4 class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class GuiDisplay extends JFrame implements CardContainer, Display, NewConstants, NewConstants.Gui.GuiDisplay,
        NewConstants.Lang.GuiDisplay {
    /** Constant <code>serialVersionUID=4519302185194841060L</code>. */
    private static final long serialVersionUID = 4519302185194841060L;

    private GuiInput inputControl;

    /** The stat font. */
    private Font statFont = new Font("Dialog", Font.PLAIN, 12);

    /** The life font. */
    private Font lifeFont = new Font("Dialog", Font.PLAIN, 40);
    // Font checkboxFont = new Font("Dialog", Font.PLAIN, 9);

    /** Constant <code>greenColor</code>. */
    private static Color greenColor = new Color(0, 164, 0);

    private Action humanGraveyardAction;
    private Action humanRemovedACtion;
    private Action humanFlashbackAction;
    private Action computerGraveyardAction;
    private Action computerRemovedAction;
    private Action concedeAction;
    private Action humanDecklistAction;
    // public Card cCardHQ;

    // private CardList multiBlockers = new CardList();

    private TriggerReactionMenu triggerMenu;

    /**
     * <p>
     * Constructor for GuiDisplay4.
     * </p>
     */
    public GuiDisplay() {
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
    public final void setVisible(final boolean visible) {
        if (visible) {
            // causes an error if put in the constructor, causes some random
            // null pointer exception
            AllZone.getInputControl().updateObservers();

            // Use both so that when "un"maximizing, the frame isn't tiny
            setSize(1024, 740);
            setExtendedState(Frame.MAXIMIZED_BOTH);
        }
        super.setVisible(visible);
    }

    /** {@inheritDoc} */
    public final void assignDamage(final Card attacker, final CardList blockers, final int damage) {
        if (damage <= 0) {
            return;
        }
        new GuiMultipleBlockers(attacker, blockers, damage, this);
    }

    /**
     * <p>
     * setupActions.
     * </p>
     */
    private void setupActions() {
        humanGraveyardAction = new ZoneAction(AllZone.getHumanPlayer().getZone(Zone.Graveyard), HUMAN_GRAVEYARD);
        humanRemovedACtion = new ZoneAction(AllZone.getHumanPlayer().getZone(Zone.Exile), HUMAN_REMOVED);
        humanFlashbackAction = new ZoneAction(AllZone.getHumanPlayer().getZone(Zone.Graveyard), HUMAN_FLASHBACK) {

            private static final long serialVersionUID = 8120331222693706164L;

            @Override
            protected Iterable<Card> getCardsAsIterable() {
                return new ImmutableIterableFrom<Card>(CardFactoryUtil.getExternalZoneActivationCards(AllZone
                        .getHumanPlayer()));
            }

            @Override
            protected void doAction(final Card c) {
                AllZone.getGameAction().playCard(c);
            }
        };
        computerGraveyardAction = new ZoneAction(AllZone.getComputerPlayer().getZone(Zone.Graveyard),
                COMPUTER_GRAVEYARD);
        computerRemovedAction = new ZoneAction(AllZone.getComputerPlayer().getZone(Zone.Exile), COMPUTER_REMOVED);
        concedeAction = new ConcedeAction();

        humanDecklistAction = new DeckListAction(HUMAN_DECKLIST);
    }

    /**
     * <p>
     * addMenu.
     * </p>
     */
    private void addMenu() {
        // Trigger Context Menu creation
        triggerMenu = new TriggerReactionMenu();

        // Game Menu Creation
        Object[] obj = { humanDecklistAction, humanGraveyardAction, humanRemovedACtion, humanFlashbackAction,
                computerGraveyardAction, computerRemovedAction, new JSeparator(), playsoundCheckboxForMenu,
                new JSeparator(), ErrorViewer.ALL_THREADS_ACTION, concedeAction };

        JMenu gameMenu = new JMenu(ForgeProps.getLocalized(MenuBar.Menu.TITLE));
        for (Object o : obj) {
            if (o instanceof ForgeAction) {
                gameMenu.add(((ForgeAction) o).setupButton(new JMenuItem()));
            } else if (o instanceof Action) {
                gameMenu.add((Action) o);
            } else if (o instanceof Component) {
                gameMenu.add((Component) o);
            } else {
                throw new AssertionError();
            }
        }

        // Phase Menu Creation
        JMenu gamePhases = new JMenu(ForgeProps.getLocalized(MenuBar.PHASE.TITLE));

        JMenuItem aiLabel = new JMenuItem("Computer");
        JMenuItem humanLabel = new JMenuItem("Human");

        Component[] objPhases = { aiLabel, cbAIUpkeep, cbAIDraw, cbAIBeginCombat, cbAIEndCombat, cbAIEndOfTurn,
                new JSeparator(), humanLabel, cbHumanUpkeep, cbHumanDraw, cbHumanBeginCombat, cbHumanEndCombat,
                cbHumanEndOfTurn };

        for (Component cmp : objPhases) {
            gamePhases.add(cmp);
        }

        // Dev Mode Creation
        JMenu devMenu = new JMenu(ForgeProps.getLocalized(MenuBar.DEV.TITLE));

        devMenu.setEnabled(Constant.Runtime.DEV_MODE[0]);

        if (Constant.Runtime.DEV_MODE[0]) {
            canLoseByDecking.setSelected(Constant.Runtime.MILL[0]);

            Action viewAIHand = new ZoneAction(AllZone.getComputerPlayer().getZone(Zone.Hand), ComputerHand.BASE);
            Action viewAILibrary = new ZoneAction(AllZone.getComputerPlayer().getZone(Zone.Library),
                    ComputerLibrary.BASE);
            Action viewHumanLibrary
            = new ZoneAction(AllZone.getHumanPlayer().getZone(Zone.Library), HumanLibrary.BASE);
            ForgeAction generateMana = new ForgeAction(MANAGEN) {
                private static final long serialVersionUID = 7171104690016706405L;

                public void actionPerformed(final ActionEvent arg0) {
                    GuiDisplayUtil.devModeGenerateMana();
                }
            };

            // + Battlefield setup +
            ForgeAction setupBattleField = new ForgeAction(SETUPBATTLEFIELD) {
                private static final long serialVersionUID = -6660930759092583160L;

                public void actionPerformed(final ActionEvent arg0) {
                    GuiDisplayUtil.devSetupGameState();
                }
            };
            // - Battlefield setup -

            // DevMode Tutor
            ForgeAction tutor = new ForgeAction(TUTOR) {
                private static final long serialVersionUID = 2003222642609217705L;

                public void actionPerformed(final ActionEvent arg0) {
                    GuiDisplayUtil.devModeTutor();
                }
            };
            // end DevMode Tutor

            // DevMode AddCounter
            ForgeAction addCounter = new ForgeAction(ADDCOUNTER) {
                private static final long serialVersionUID = 3136264111882855268L;

                public void actionPerformed(final ActionEvent arg0) {
                    GuiDisplayUtil.devModeAddCounter();
                }
            };
            // end DevMode AddCounter

            // DevMode Tap
            ForgeAction tapPerm = new ForgeAction(TAPPERM) {
                private static final long serialVersionUID = -6092045653540313527L;

                public void actionPerformed(final ActionEvent arg0) {
                    GuiDisplayUtil.devModeTapPerm();
                }
            };
            // end DevMode Tap

            // DevMode Untap
            ForgeAction untapPerm = new ForgeAction(UNTAPPERM) {
                private static final long serialVersionUID = 5425291996157256656L;

                public void actionPerformed(final ActionEvent arg0) {
                    GuiDisplayUtil.devModeUntapPerm();
                }
            };
            // end DevMode Untap

            // DevMode UnlimitedLand
            ForgeAction unlimitedLand = new ForgeAction(NOLANDLIMIT) {
                private static final long serialVersionUID = 2184353891062202796L;

                public void actionPerformed(final ActionEvent arg0) {
                    GuiDisplayUtil.devModeUnlimitedLand();
                }
            };
            // end DevMode UnlimitedLand

            // DevMode SetLife
            ForgeAction setLife = new ForgeAction(SETLIFE) {
                private static final long serialVersionUID = -1750588303928974918L;

                public void actionPerformed(final ActionEvent arg0) {
                    GuiDisplayUtil.devModeSetLife();
                }
            };
            // end DevMode SetLife

            Object[] objDev = { GuiDisplay.canLoseByDecking, viewAIHand, viewAILibrary, viewHumanLibrary,
                    generateMana, setupBattleField, tutor, addCounter, tapPerm, untapPerm, unlimitedLand, setLife };
            for (Object o : objDev) {
                if (o instanceof ForgeAction) {
                    devMenu.add(((ForgeAction) o).setupButton(new JMenuItem()));
                } else if (o instanceof Component) {
                    devMenu.add((Component) o);
                } else if (o instanceof Action) {
                    devMenu.add((Action) o);
                }
            }
        }

        JMenuBar menuBar = new JMenuBar();
        menuBar.add(gameMenu);
        menuBar.add(gamePhases);
        menuBar.add(devMenu);
        menuBar.add(new MenuItemHowToPlay());
        this.setJMenuBar(menuBar);
    } // addMenu()

    /**
     * <p>
     * canLoseByDecking.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean canLoseByDecking() {
        return canLoseByDecking.isSelected();
    }

    /**
     * <p>
     * getButtonOK.
     * </p>
     * 
     * @return a {@link forge.MyButton} object.
     */
    public final MyButton getButtonOK() {
        MyButton ok = new MyButton() {
            public void select() {
                inputControl.selectButtonOK();
            }

            public boolean isSelectable() {
                return okButton.isEnabled();
            }

            public void setSelectable(final boolean b) {
                okButton.setEnabled(b);
            }

            public String getText() {
                return okButton.getText();
            }

            public void setText(final String text) {
                okButton.setText(text);
            }

            public void reset() {
                okButton.setText("OK");
            }
        };

        return ok;
    } // getButtonOK()

    /**
     * <p>
     * getButtonCancel.
     * </p>
     * 
     * @return a {@link forge.MyButton} object.
     */
    public final MyButton getButtonCancel() {
        MyButton cancel = new MyButton() {
            public void select() {
                inputControl.selectButtonCancel();
            }

            public boolean isSelectable() {
                return cancelButton.isEnabled();
            }

            public void setSelectable(final boolean b) {
                cancelButton.setEnabled(b);
            }

            public String getText() {
                return cancelButton.getText();
            }

            public void setText(final String text) {
                cancelButton.setText(text);
            }

            public void reset() {
                cancelButton.setText("Cancel");
            }
        };
        return cancel;
    } // getButtonCancel()

    /** {@inheritDoc} */
    public final void showCombat(final String message) {
        combatArea.setText(message);
    }

    /** {@inheritDoc} */
    public final void showMessage(final String s) {
        messageArea.setText(s);

        Border border = null;
        int thickness = 3;

        if (AllZone.getStack().size() > 0 && AllZone.getStack().peekInstance().getActivatingPlayer().isComputer()) {
            border = BorderFactory.createLineBorder(new Color(0, 255, 255), thickness);
        } else if (s.contains("Main")) {
            border = BorderFactory.createLineBorder(new Color(30, 0, 255), thickness);
        } else if (s.contains("To Block")) {
            border = BorderFactory.createLineBorder(new Color(13, 179, 0), thickness);
        } else if (s.contains("Play Instants and Abilities") || s.contains("Combat") || s.contains("Damage")) {
            border = BorderFactory.createLineBorder(new Color(255, 174, 0), thickness);
        } else if (s.contains("Declare Attackers")) {
            border = BorderFactory.createLineBorder(new Color(255, 0, 0), thickness);
        } else if (s.contains("Upkeep") || s.contains("Draw") || s.contains("End of Turn")) {
            border = BorderFactory.createLineBorder(new Color(200, 0, 170), thickness);
        } else {
            border = new EmptyBorder(1, 1, 1, 1);
        }

        messageArea.setBorder(border);
    }

    /**
     * <p>
     * addListeners.
     * </p>
     */
    private void addListeners() {
        // mouse Card Detail
        playerHandPanel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(final MouseEvent me) {
                Card c = playerHandPanel.getCardFromMouseOverPanel();
                if (c != null) {
                    setCard(c);
                }
            } // mouseMoved
        });

        playerPlayPanel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(final MouseEvent me) {
                Card c = playerPlayPanel.getCardFromMouseOverPanel();
                if (c != null) {
                    setCard(c);
                }
            } // mouseMoved
        });

        oppPlayPanel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(final MouseEvent me) {
                Card c = oppPlayPanel.getCardFromMouseOverPanel();
                if (c != null) {
                    setCard(c);
                }
            } // mouseMoved
        });

        // opponent life mouse listener
        oppLifeLabel.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(final MouseEvent e) {
                inputControl.selectPlayer(AllZone.getComputerPlayer());
            }
        });

        oppLifeLabel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(final MouseEvent me) {
                setCard(AllZone.getComputerPlayer().getManaPool());
            } // mouseMoved
        });

        // self life mouse listener
        playerLifeLabel.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(final MouseEvent e) {
                inputControl.selectPlayer(AllZone.getHumanPlayer());
            }
        });

        // self play (land) ---- Mouse
        playerPlayPanel.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(final MouseEvent e) {
                Card c = playerPlayPanel.getCardFromMouseOverPanel();
                if (c != null) {

                    if (c.isTapped()
                            && (inputControl.getInput() instanceof InputPayManaCost
                                    || inputControl.getInput() instanceof InputPayManaCostAbility)) {
                        arcane.ui.CardPanel cardPanel = playerPlayPanel.getCardPanel(c.getUniqueNumber());
                        for (arcane.ui.CardPanel cp : cardPanel.getAttachedPanels()) {
                            if (cp.getCard().isUntapped()) {
                                break;
                            }
                        }
                    }

                    CardList att = new CardList(AllZone.getCombat().getAttackers());
                    if ((c.isTapped() || c.hasSickness() || ((c.hasKeyword("Vigilance")) && att.contains(c)))
                            && (inputControl.getInput() instanceof InputAttack)) {
                        arcane.ui.CardPanel cardPanel = playerPlayPanel.getCardPanel(c.getUniqueNumber());
                        for (arcane.ui.CardPanel cp : cardPanel.getAttachedPanels()) {
                            if (cp.getCard().isUntapped() && !cp.getCard().hasSickness()) {
                                break;
                            }
                        }
                    }

                    if (e.isMetaDown()) {
                        if (att.contains(c) && (inputControl.getInput() instanceof InputAttack)
                                && !c.hasKeyword("CARDNAME attacks each turn if able.")) {
                            c.untap();
                            AllZone.getCombat().removeFromCombat(c);
                        } else if (inputControl.getInput() instanceof InputBlock) {
                            if (c.getController().isHuman()) {
                                AllZone.getCombat().removeFromCombat(c);
                            }
                            ((InputBlock) inputControl.getInput()).removeFromAllBlocking(c);
                        }
                    } else {
                        inputControl.selectCard(c, AllZone.getHumanPlayer().getZone(Zone.Battlefield));
                    }
                }
            }
        });

        // self hand ---- Mouse
        playerHandPanel.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(final MouseEvent e) {
                if (e.getButton() != MouseEvent.BUTTON1) {
                    return;
                }
                Card c = playerHandPanel.getCardFromMouseOverPanel();
                if (c != null) {
                    inputControl.selectCard(c, AllZone.getHumanPlayer().getZone(Zone.Hand));
                    okButton.requestFocusInWindow();
                }
            }
        });

        // *****************************************************************
        // computer

        // computer play (land) ---- Mouse
        oppPlayPanel.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(final MouseEvent e) {
                Card c = oppPlayPanel.getCardFromMouseOverPanel();
                if (c != null) {
                    inputControl.selectCard(c, AllZone.getComputerPlayer().getZone(Zone.Battlefield));
                }
            }
        });

    } // addListener()

    /**
     * <p>
     * getCard.
     * </p>
     * 
     * @return a {@link forge.Card} object.
     */
    public final Card getCard() {
        return detail.getCard();
    }

    /** {@inheritDoc} */
    public final void setCard(final Card card) {
        detail.setCard(card);
        picture.setCard(card);
    }

    /**
     * <p>
     * addObservers.
     * </p>
     */
    private void addObservers() {
        // long method
        Observer o = new Observer() {
            public void update(final Observable a, final Object b) {
                playerHandValue.setText("" + AllZone.getHumanPlayer().getZone(Zone.Hand).size());
                playerGraveValue.setText("" + AllZone.getHumanPlayer().getZone(Zone.Graveyard).size());
                playerLibraryValue.setText("" + AllZone.getHumanPlayer().getZone(Zone.Library).size());
                playerFBValue.setText(""
                        + CardFactoryUtil.getExternalZoneActivationCards(AllZone.getHumanPlayer()).size());
                playerRemovedValue.setText("" + AllZone.getHumanPlayer().getZone(Zone.Exile).size());

            }
        };
        AllZone.getHumanPlayer().getZone(Zone.Hand).addObserver(o);
        AllZone.getHumanPlayer().getZone(Zone.Graveyard).addObserver(o);
        AllZone.getHumanPlayer().getZone(Zone.Library).addObserver(o);
        // long method
        Observer o1 = new Observer() {
            public void update(final Observable a, final Object b) {
                oppHandValue.setText("" + AllZone.getComputerPlayer().getZone(Zone.Hand).size());
                oppGraveValue.setText("" + AllZone.getComputerPlayer().getZone(Zone.Graveyard).size());
                oppLibraryValue.setText("" + AllZone.getComputerPlayer().getZone(Zone.Library).size());
                oppRemovedValue.setText("" + AllZone.getComputerPlayer().getZone(Zone.Exile).size());
            }
        };
        AllZone.getComputerPlayer().getZone(Zone.Hand).addObserver(o1);
        AllZone.getComputerPlayer().getZone(Zone.Graveyard).addObserver(o1);
        AllZone.getComputerPlayer().getZone(Zone.Library).addObserver(o1);
        // opponent life
        oppLifeLabel.setText("" + AllZone.getComputerPlayer().getLife());
        AllZone.getComputerPlayer().addObserver(new Observer() {
            public void update(final Observable a, final Object b) {
                int life = AllZone.getComputerPlayer().getLife();
                oppLifeLabel.setText("" + life);
            }
        });
        AllZone.getComputerPlayer().updateObservers();

        if (AllZone.getQuestData() != null) {
            File base = ForgeProps.getFile(IMAGE_ICON);
            String iconName = "";
            if (Constant.Quest.OPP_ICON_NAME[0] != null) {
                iconName = Constant.Quest.OPP_ICON_NAME[0];
                File file = new File(base, iconName);
                ImageIcon icon = new ImageIcon(file.toString());
                oppIconLabel.setIcon(icon);
                oppIconLabel.setAlignmentX(100);

            }
        }

        oppPCLabel.setText("Poison Counters: " + AllZone.getComputerPlayer().getPoisonCounters());
        AllZone.getComputerPlayer().addObserver(new Observer() {
            public void update(final Observable a, final Object b) {
                int pcs = AllZone.getComputerPlayer().getPoisonCounters();
                oppPCLabel.setText("Poison Counters: " + pcs);
            }
        });
        AllZone.getComputerPlayer().updateObservers();

        // player life
        playerLifeLabel.setText("" + AllZone.getHumanPlayer().getLife());
        AllZone.getHumanPlayer().addObserver(new Observer() {
            public void update(final Observable a, final Object b) {
                int life = AllZone.getHumanPlayer().getLife();
                playerLifeLabel.setText("" + life);
            }
        });
        AllZone.getHumanPlayer().updateObservers();

        playerPCLabel.setText("Poison Counters: " + AllZone.getHumanPlayer().getPoisonCounters());
        AllZone.getHumanPlayer().addObserver(new Observer() {
            public void update(final Observable a, final Object b) {
                int pcs = AllZone.getHumanPlayer().getPoisonCounters();
                playerPCLabel.setText("Poison Counters: " + pcs);
            }
        });
        AllZone.getHumanPlayer().updateObservers();

        // stack
        AllZone.getStack().addObserver(new Observer() {
            public void update(final Observable a, final Object b) {
                stackPanel.removeAll();
                final MagicStack stack = AllZone.getStack();
                int count = 1;
                JLabel label;

                for (int i = stack.size() - 1; 0 <= i; i--) {
                    final int curI = i;
                    String isOptional = stack.peekAbility(i).isOptionalTrigger()
                            && stack.peekAbility(i).getSourceCard().getController().isHuman() ? "(OPTIONAL) " : "";
                    label = new JLabel((count++) + ". " + isOptional + stack.peekInstance(i).getStackDescription());

                    // update card detail
                    final CardPanel cardPanel = new CardPanel(stack.peekInstance(i).getSpellAbility().getSourceCard());
                    cardPanel.setLayout(new BorderLayout());
                    cardPanel.add(label);
                    cardPanel.addMouseMotionListener(new MouseMotionAdapter() {

                        @Override
                        public void mouseMoved(final MouseEvent me) {
                            setCard(cardPanel.getCard());
                        } // mouseMoved
                    });

                    if (stack.peekInstance(curI).isOptionalTrigger()) {
                        cardPanel.addMouseListener(new MouseAdapter() {
                            @Override
                            public void mousePressed(final MouseEvent e) {
                                if (e.getButton() != MouseEvent.BUTTON3) {
                                    return;
                                }

                                triggerMenu.setTrigger(stack.peekAbility(curI).getSourceTrigger());
                                triggerMenu.show(e.getComponent(), e.getX(), e.getY());
                            }
                        });
                    }

                    stackPanel.add(cardPanel);
                }

                stackPanel.revalidate();
                stackPanel.repaint();

                okButton.requestFocusInWindow();

            }
        });
        AllZone.getStack().updateObservers();
        // END, stack

        // self hand
        AllZone.getHumanPlayer().getZone(Zone.Hand).addObserver(new Observer() {
            public void update(final Observable a, final Object b) {
                PlayerZone pZone = (PlayerZone) a;
                HandArea p = playerHandPanel;

                Card[] c = pZone.getCards();

                List<Card> tmp, diff;
                tmp = new ArrayList<Card>();
                for (arcane.ui.CardPanel cpa : p.getCardPanels()) {
                    tmp.add(cpa.getGameCard());
                }
                diff = new ArrayList<Card>(tmp);
                diff.removeAll(Arrays.asList(c));
                if (diff.size() == p.getCardPanels().size()) {
                    p.clear();
                } else {
                    for (Card card : diff) {
                        p.removeCardPanel(p.getCardPanel(card.getUniqueNumber()));
                    }
                }
                diff = new ArrayList<Card>(Arrays.asList(c));
                diff.removeAll(tmp);

                int fromZoneX = 0, fromZoneY = 0;
                Rectangle pb = playerLibraryValue.getBounds();
                Point zoneLocation = SwingUtilities.convertPoint(playerLibraryValue, Math.round(pb.width / 2.0f),
                        Math.round(pb.height / 2.0f), layeredPane);
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
                    if (isShowing()) {
                        Animation.moveCard(startX, startY, startWidth, endX, endY, endWidth, animationPanel, toPanel,
                                layeredPane, 500);
                    } else {
                        Animation.moveCard(toPanel);
                    }
                }
            }
        });
        AllZone.getHumanPlayer().getZone(Zone.Hand).updateObservers();
        // END, self hand

        // self play
        AllZone.getHumanPlayer().getZone(Zone.Battlefield).addObserver(new Observer() {
            public void update(final Observable a, final Object b) {
                PlayerZone pZone = (PlayerZone) a;

                Card[] c = pZone.getCards(false);

                GuiDisplayUtil.setupPlayZone(playerPlayPanel, c);
            }
        });
        AllZone.getHumanPlayer().getZone(Zone.Battlefield).updateObservers();
        // END - self play

        // computer play
        AllZone.getComputerPlayer().getZone(Zone.Battlefield).addObserver(new Observer() {
            public void update(final Observable a, final Object b) {
                PlayerZone pZone = (PlayerZone) a;

                Card[] c = pZone.getCards(false);

                GuiDisplayUtil.setupPlayZone(oppPlayPanel, c);
            }
        });
        AllZone.getComputerPlayer().getZone(Zone.Battlefield).updateObservers();
        // END - computer play

    } // addObservers()

    /**
     * <p>
     * initComponents.
     * </p>
     */
    private void initComponents() {
        // Preparing the Frame
        setTitle(ForgeProps.getLocalized(Lang.PROGRAM_NAME));
        if (!Singletons.getModel().getPreferences().isLafFonts()) {
            setFont(new Font("Times New Roman", 0, 16));
        }
        getContentPane().setLayout(new BorderLayout());

        // I tried using the JavaBeanConverter with this, but I got a
        // StackOverflowError due to an infinite loop. The default
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
                } catch (IOException ex) {
                    assert System.err != null;

                    System.err.println("Ignoring exception:");
                    ex.printStackTrace(); // NOPMD by Braids on 8/21/11 9:20 PM
                    System.err.println("-------------------");
                } finally {
                    if (out != null) {
                        try {
                            out.close();
                        } catch (Throwable ignored) { // NOPMD by Braids on
                                                      // 8/21/11 9:20 PM
                            // Ignore failure to close.
                        }
                    }
                }
            }
        });

        // making the multi split pane
        Node model;

        // Try to load the latest saved layout, usually
        // res/gui/display_new_layout.xml
        final File file = ForgeProps.getFile(LAYOUT_NEW);

        try {
            model = loadModel(xstream, file);
        } catch (XStreamException xse) {
            assert System.err != null;

            System.err.println("Error loading '" + file.getAbsolutePath() + "' using XStream: "
                    + xse.getLocalizedMessage());

            model = null;
        } catch (FileNotFoundException e1) {
            model = null;
        }

        if (model == null) {
            assert System.err != null;

            System.err
                    .println("Unable to parse file '" + file.getAbsolutePath() + "' with XStream; trying XMLDecoder.");

            try {
                model = loadModelUsingXMLDecoder(file);
            } catch (FileNotFoundException e1) {
                model = null;
            } catch (Throwable exn) { // NOPMD by Braids on 8/21/11 9:20 PM
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
            } catch (Exception exn) {
                // Should never happen.
                throw new RuntimeException(exn); // NOPMD by Braids on 8/21/11
                                                 // 9:18 PM

                /*
                 * This code is useful for bootstrapping a
                 * display_new_layout.xml file.
                 * 
                 * System.err.println("Unable to parse file '" +
                 * defaultFile.getAbsolutePath() +
                 * "' with XStream; using hard coded defaults.");
                 * 
                 * model = parseModel(""// + "(ROW "// + "(COLUMN"// +
                 * " (LEAF weight=0.2 name=info)"// +
                 * " (LEAF weight=0.2 name=compy)"// +
                 * " (LEAF weight=0.2 name=stack)"// +
                 * " (LEAF weight=0.2 name=combat)"// +
                 * " (LEAF weight=0.2 name=human)) "// + "(COLUMN weight=1"// +
                 * " (LEAF weight=0.4 name=compyPlay)"// +
                 * " (LEAF weight=0.4 name=humanPlay)"// +
                 * " (LEAF weight=0.2 name=humanHand)) "// + "(COLUMN"// +
                 * " (LEAF weight=0.5 name=detail)"// +
                 * " (LEAF weight=0.5 name=picture)))");
                 * 
                 * pane.setModel(model);
                 */
            }
        }

        if (model != null) {
            pane.getMultiSplitLayout().setModel(model);
        }

        pane.getMultiSplitLayout().setFloatingDividers(false);
        getContentPane().add(pane);

        // adding the individual parts

        if (!Singletons.getModel().getPreferences().isLafFonts()) {
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
     * throws XStreamException if there was a parsing error
     * </p>
     * 
     * @param xstream
     *            the XStream parser to use; do not use JavaBeanConverter!
     * @param file
     *            the XML file containing the preferences
     * @return the preferences model as a Node instance
     * @throws FileNotFoundException
     *             if file does not exist
     */
    public static Node loadModel(final XStream xstream, final File file) throws FileNotFoundException {
        BufferedInputStream bufferedIn = null;
        Node model = null;
        try {
            bufferedIn = new BufferedInputStream(new FileInputStream(file));
            model = (Node) xstream.fromXML(bufferedIn);
        } finally {
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
     * XMLDecoder format. XStream is preferred.
     * 
     * @param file
     *            the XML file containing the preferences
     * @return the preferences model as a Node instance
     * @throws FileNotFoundException
     *             if file does not exist
     */
    public static Node loadModelUsingXMLDecoder(final File file) throws FileNotFoundException {
        BufferedInputStream bufferedIn = null;
        Node model = null;
        XMLDecoder decoder = null;
        try {
            bufferedIn = new BufferedInputStream(new FileInputStream(file));
            decoder = new XMLDecoder(bufferedIn);
            model = (Node) decoder.readObject();
        } finally {
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
     * <p>
     * initFonts.
     * </p>
     * 
     * @param pane
     *            a {@link javax.swing.JPanel} object.
     */
    private void initFonts(final JPanel pane) {
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
     * <p>
     * initMsgYesNo.
     * </p>
     * 
     * @param pane
     *            a {@link javax.swing.JPanel} object.
     */
    private void initMsgYesNo(final JPanel pane) {
        // messageArea.setBorder(BorderFactory.createEtchedBorder());
        messageArea.setEditable(false);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);

        cancelButton.setText("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent evt) {
                cancelButtonActionPerformed(evt);
                okButton.requestFocusInWindow();
            }
        });
        okButton.setText("OK");
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent evt) {
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
            public void keyPressed(final KeyEvent arg0) {
                // TODO make triggers on escape
                int code = arg0.getKeyCode();
                if (code == KeyEvent.VK_ESCAPE) {
                    cancelButton.doClick();
                }
            }
        });

        okButton.requestFocusInWindow();

        // if(okButton.isEnabled())
        // okButton.doClick();
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
     * <p>
     * initOpp.
     * </p>
     * 
     * @param pane
     *            a {@link javax.swing.JPanel} object.
     */
    private void initOpp(final JPanel pane) {
        // oppLifeLabel.setHorizontalAlignment(SwingConstants.CENTER);

        // oppPCLabel.setHorizontalAlignment(SwingConstants.TOP);
        oppPCLabel.setForeground(greenColor);

        JLabel oppHandLabel = new JLabel(ForgeProps.getLocalized(ComputerHand.BUTTON), SwingConstants.TRAILING);
        if (!Singletons.getModel().getPreferences().isLafFonts()) {
            oppHandLabel.setFont(statFont);
        }

        JButton oppGraveButton = new JButton(computerGraveyardAction);
        oppGraveButton.setText((String) computerGraveyardAction.getValue("buttonText"));
        oppGraveButton.setMargin(new Insets(0, 0, 0, 0));
        oppGraveButton.setHorizontalAlignment(SwingConstants.TRAILING);
        if (!Singletons.getModel().getPreferences().isLafFonts()) {
            oppGraveButton.setFont(statFont);
        }

        JPanel gravePanel = new JPanel(new BorderLayout());
        gravePanel.add(oppGraveButton, BorderLayout.EAST);

        JButton oppRemovedButton = new JButton(computerRemovedAction);
        oppRemovedButton.setText((String) computerRemovedAction.getValue("buttonText"));
        oppRemovedButton.setMargin(new Insets(0, 0, 0, 0));
        // removedButton.setHorizontalAlignment(SwingConstants.TRAILING);
        if (!Singletons.getModel().getPreferences().isLafFonts()) {
            oppRemovedButton.setFont(statFont);
        }

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
     * <p>
     * initStackCombat.
     * </p>
     * 
     * @param pane
     *            a {@link javax.swing.JPanel} object.
     */
    private void initStackCombat(final JPanel pane) {
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
     * <p>
     * initPlayer.
     * </p>
     * 
     * @param pane
     *            a {@link javax.swing.JPanel} object.
     */
    private void initPlayer(final JPanel pane) {
        // int fontSize = 12;
        playerLifeLabel.setHorizontalAlignment(SwingConstants.CENTER);

        playerPCLabel.setForeground(greenColor);

        JLabel playerLibraryLabel = new JLabel(ForgeProps.getLocalized(HumanLibrary.BUTTON), SwingConstants.TRAILING);
        if (!Singletons.getModel().getPreferences().isLafFonts()) {
            playerLibraryLabel.setFont(statFont);
        }

        JLabel playerHandLabel = new JLabel(ForgeProps.getLocalized(HumanHand.TITLE), SwingConstants.TRAILING);
        if (!Singletons.getModel().getPreferences().isLafFonts()) {
            playerHandLabel.setFont(statFont);
        }

        // JLabel playerGraveLabel = new JLabel("Grave:",
        // SwingConstants.TRAILING);
        JButton playerGraveButton = new JButton(humanGraveyardAction);
        playerGraveButton.setText((String) humanGraveyardAction.getValue("buttonText"));
        playerGraveButton.setMargin(new Insets(0, 0, 0, 0));
        playerGraveButton.setHorizontalAlignment(SwingConstants.TRAILING);
        if (!Singletons.getModel().getPreferences().isLafFonts()) {
            playerGraveButton.setFont(statFont);
        }

        JButton playerFlashBackButton = new JButton(humanFlashbackAction);
        playerFlashBackButton.setText((String) humanFlashbackAction.getValue("buttonText"));
        playerFlashBackButton.setMargin(new Insets(0, 0, 0, 0));
        playerFlashBackButton.setHorizontalAlignment(SwingConstants.TRAILING);
        if (!Singletons.getModel().getPreferences().isLafFonts()) {
            playerFlashBackButton.setFont(statFont);
        }

        JPanel gravePanel = new JPanel(new BorderLayout());
        gravePanel.add(playerGraveButton, BorderLayout.EAST);

        JPanel playerFBPanel = new JPanel(new BorderLayout());
        playerFBPanel.add(playerFlashBackButton, BorderLayout.EAST);

        JButton playerRemovedButton = new JButton(humanRemovedACtion);
        playerRemovedButton.setText((String) humanRemovedACtion.getValue("buttonText"));
        playerRemovedButton.setMargin(new Insets(0, 0, 0, 0));
        // removedButton.setHorizontalAlignment(SwingConstants.TRAILING);
        if (!Singletons.getModel().getPreferences().isLafFonts()) {
            playerRemovedButton.setFont(statFont);
        }

        playerHandValue.setHorizontalAlignment(SwingConstants.LEADING);
        playerLibraryValue.setHorizontalAlignment(SwingConstants.LEADING);
        playerGraveValue.setHorizontalAlignment(SwingConstants.LEADING);
        playerFBValue.setHorizontalAlignment(SwingConstants.LEADING);

        // playerRemovedValue.setFont(new Font("MS Sans Serif", 0, fontSize));
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
     * <p>
     * initZones.
     * </p>
     * 
     * @param pane
     *            a {@link javax.swing.JPanel} object.
     */
    private void initZones(final JPanel pane) {
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
     * <p>
     * initCardPicture.
     * </p>
     * 
     * @param pane
     *            a {@link javax.swing.JPanel} object.
     */
    private void initCardPicture(final JPanel pane) {
        pane.add(new ExternalPanel(detail), "detail");
        pane.add(new ExternalPanel(picturePanel), "picture");
        picturePanel.setCardPanel(picture);
    }

    /**
     * <p>
     * cancelButtonActionPerformed.
     * </p>
     * 
     * @param evt
     *            a {@link java.awt.event.ActionEvent} object.
     */
    private void cancelButtonActionPerformed(final ActionEvent evt) {
        inputControl.selectButtonCancel();
    }

    /**
     * <p>
     * okButtonActionPerformed.
     * </p>
     * 
     * @param evt
     *            a {@link java.awt.event.ActionEvent} object.
     */
    private void okButtonActionPerformed(final ActionEvent evt) {
        inputControl.selectButtonOK();
    }

    /**
     * Exit the Application.
     */
    private void concede() {
        AllZone.getHumanPlayer().concede();
        AllZone.getGameAction().checkStateEffects();
    }

    // ********** Phase stuff in Display ******************
    /** {@inheritDoc} */
    public final boolean stopAtPhase(final Player turn, final String phase) {
        if (turn.isComputer()) {
            if (phase.equals(Constant.Phase.END_OF_TURN)) {
                return cbAIEndOfTurn.isSelected();
            } else if (phase.equals(Constant.Phase.UPKEEP)) {
                return cbAIUpkeep.isSelected();
            } else if (phase.equals(Constant.Phase.DRAW)) {
                return cbAIDraw.isSelected();
            } else if (phase.equals(Constant.Phase.COMBAT_BEGIN)) {
                return cbAIBeginCombat.isSelected();
            } else if (phase.equals(Constant.Phase.COMBAT_END)) {
                return cbAIEndCombat.isSelected();
            }
        } else {
            if (phase.equals(Constant.Phase.END_OF_TURN)) {
                return cbHumanEndOfTurn.isSelected();
            } else if (phase.equals(Constant.Phase.UPKEEP)) {
                return cbHumanUpkeep.isSelected();
            } else if (phase.equals(Constant.Phase.DRAW)) {
                return cbHumanDraw.isSelected();
            } else if (phase.equals(Constant.Phase.COMBAT_BEGIN)) {
                return cbHumanBeginCombat.isSelected();
            } else if (phase.equals(Constant.Phase.COMBAT_END)) {
                return cbHumanEndCombat.isSelected();
            }
        }
        return true;
    }

    /**
     * <p>
     * loadPrefs.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean loadPrefs() {
        ForgePreferences fp = Singletons.getModel().getPreferences();

        cbAIUpkeep.setSelected(fp.isbAIUpkeep());
        cbAIDraw.setSelected(fp.isbAIDraw());
        cbAIEndOfTurn.setSelected(fp.isbAIEOT());
        cbAIBeginCombat.setSelected(fp.isbAIBeginCombat());
        cbAIEndCombat.setSelected(fp.isbAIEndCombat());

        cbHumanUpkeep.setSelected(fp.isbHumanUpkeep());
        cbHumanDraw.setSelected(fp.isbHumanDraw());
        cbHumanEndOfTurn.setSelected(fp.isbHumanEOT());
        cbHumanBeginCombat.setSelected(fp.isbHumanBeginCombat());
        cbHumanEndCombat.setSelected(fp.isbHumanEndCombat());

        canLoseByDecking.setSelected(fp.isMillingLossCondition());

        return true;
    }

    /**
     * <p>
     * savePrefs.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean savePrefs() {
        Constant.Runtime.MILL[0] = canLoseByDecking.isSelected();
        ForgePreferences fp = Singletons.getModel().getPreferences();

        fp.setbAIUpkeep(cbAIUpkeep.isSelected());
        fp.setbAIDraw(cbAIDraw.isSelected());
        fp.setbAIEOT(cbAIEndOfTurn.isSelected());
        fp.setbAIBeginCombat(cbAIBeginCombat.isSelected());
        fp.setbAIEndCombat(cbAIEndCombat.isSelected());

        fp.setbHumanUpkeep(cbHumanUpkeep.isSelected());
        fp.setbHumanDraw(cbHumanDraw.isSelected());
        fp.setbHumanEOT(cbHumanEndOfTurn.isSelected());
        fp.setbHumanBeginCombat(cbHumanBeginCombat.isSelected());
        fp.setbHumanEndCombat(cbHumanEndCombat.isSelected());

        fp.setMillingLossCondition(canLoseByDecking.isSelected());

        return true;
    }

    /** Constant <code>playsoundCheckboxForMenu</code>. */
    private static JCheckBoxMenuItem playsoundCheckboxForMenu = new JCheckBoxMenuItem("Play Sound", false);

    // Phases
    /** Constant <code>cbAIUpkeep</code>. */
    private static JCheckBoxMenuItem cbAIUpkeep = new JCheckBoxMenuItem("Upkeep", true);

    /** Constant <code>cbAIDraw</code>. */
    private static JCheckBoxMenuItem cbAIDraw = new JCheckBoxMenuItem("Draw", true);

    /** Constant <code>cbAIEndOfTurn</code>. */
    private static JCheckBoxMenuItem cbAIEndOfTurn = new JCheckBoxMenuItem("End of Turn", true);

    /** Constant <code>cbAIBeginCombat</code>. */
    private static JCheckBoxMenuItem cbAIBeginCombat = new JCheckBoxMenuItem("Begin Combat", true);

    /** Constant <code>cbAIEndCombat</code>. */
    private static JCheckBoxMenuItem cbAIEndCombat = new JCheckBoxMenuItem("End Combat", true);

    /** Constant <code>cbHumanUpkeep</code>. */
    private static JCheckBoxMenuItem cbHumanUpkeep = new JCheckBoxMenuItem("Upkeep", true);
    /** Constant <code>cbHumanDraw</code>. */
    private static JCheckBoxMenuItem cbHumanDraw = new JCheckBoxMenuItem("Draw", true);
    /** Constant <code>cbHumanEndOfTurn</code>. */
    private static JCheckBoxMenuItem cbHumanEndOfTurn = new JCheckBoxMenuItem("End of Turn", true);
    /** Constant <code>cbHumanBeginCombat</code>. */
    private static JCheckBoxMenuItem cbHumanBeginCombat = new JCheckBoxMenuItem("Begin Combat", true);

    /** Constant <code>cbHumanEndCombat</code>. */
    private static JCheckBoxMenuItem cbHumanEndCombat = new JCheckBoxMenuItem("End Combat", true);

    // ********** End of Phase stuff in Display ******************

    // ****** Developer Mode *******

    /** Constant <code>canLoseByDecking</code>. */
    private static JCheckBoxMenuItem canLoseByDecking = new JCheckBoxMenuItem("Lose by Decking", true);

    // *****************************

    private JXMultiSplitPane pane = new JXMultiSplitPane();
    private JButton cancelButton = new JButton();
    private JButton okButton = new JButton();
    private JTextArea messageArea = new JTextArea(1, 10);
    private JTextArea combatArea = new JTextArea();
    private JPanel stackPanel = new JPanel();
    private PlayArea oppPlayPanel = null;
    private PlayArea playerPlayPanel = null;
    private HandArea playerHandPanel = null;
    // JPanel cdPanel = new JPanel();
    private JLabel oppLifeLabel = new JLabel();
    private JLabel oppIconLabel = new JLabel();
    private JLabel playerLifeLabel = new JLabel();
    private JLabel oppPCLabel = new JLabel();
    private JLabel playerPCLabel = new JLabel();
    private JLabel oppLibraryLabel = new JLabel(ForgeProps.getLocalized(ComputerLibrary.BUTTON),
            SwingConstants.TRAILING);
    private JLabel oppHandValue = new JLabel();
    private JLabel oppLibraryValue = new JLabel();
    private JLabel oppGraveValue = new JLabel();
    private JLabel oppRemovedValue = new JLabel();
    private JLabel playerHandValue = new JLabel();
    private JLabel playerLibraryValue = new JLabel();
    private JLabel playerGraveValue = new JLabel();
    private JLabel playerFBValue = new JLabel();
    private JLabel playerRemovedValue = new JLabel();

    private CardDetailPanel detail = new CardDetailPanel(null);
    private ViewPanel picturePanel = new ViewPanel();
    private arcane.ui.CardPanel picture = new arcane.ui.CardPanel(null);
    private JLayeredPane layeredPane = SwingUtilities.getRootPane(this).getLayeredPane();

    private class ZoneAction extends ForgeAction {
        private static final long serialVersionUID = -5822976087772388839L;
        private PlayerZone zone;
        private String title;

        public ZoneAction(final PlayerZone zone, final String property) {
            super(property);
            title = ForgeProps.getLocalized(property + "/title");
            this.zone = zone;
        }

        public void actionPerformed(final ActionEvent e) {
            Generator<Card> c = YieldUtils.toGenerator(getCardsAsIterable());

            if (AllZone.getNameChanger().shouldChangeCardName()) {
                c = AllZone.getNameChanger().changeCard(c);
            }

            Iterable<Card> myIterable = YieldUtils.toIterable(c);
            ArrayList<Card> choices = YieldUtils.toArrayList(myIterable);
            // System.out.println("immediately after: "+choices);
            // Iterator<Card> iter = myIterable.iterator();

            ArrayList<Card> choices2 = new ArrayList<Card>();

            if (choices.isEmpty()) {
                GuiUtils.getChoiceOptional(title, new String[] { "no cards" });
            } else {
                for (int i = 0; i < choices.size(); i++) {
                    Card crd = choices.get(i);
                    // System.out.println(crd+": "+crd.isFaceDown());
                    if (crd.isFaceDown()) {
                        Card faceDown = new Card();
                        faceDown.setName("Face Down");
                        choices2.add(faceDown);
                        // System.out.println("Added: "+faceDown);
                    } else {
                        choices2.add(crd);
                    }
                }
                // System.out.println("Face down cards replaced: "+choices2);
                Card choice = (Card) GuiUtils.getChoiceOptional(title, choices2.toArray());
                if (choice != null) {
                    doAction(choice);
                    /*
                     * Card choice = GuiUtils.getChoiceOptional(title, iter); if
                     * (choice != null) doAction(choice);
                     */
                }
            }
        }

        protected Iterable<Card> getCardsAsIterable() {
            return new ImmutableIterableFrom<Card>(Arrays.asList(zone.getCards()));
        }

        protected void doAction(final Card c) {
        }
    }

    private class ConcedeAction extends ForgeAction {

        private static final long serialVersionUID = -6976695235601916762L;

        public ConcedeAction() {
            super(CONCEDE);
        }

        public void actionPerformed(final ActionEvent e) {
            concede();
        }
    }

    private class DeckListAction extends ForgeAction {
        public DeckListAction(final String property) {
            super(property);
        }

        private static final long serialVersionUID = 9874492387239847L;

        public void actionPerformed(final ActionEvent e) {
            if (Constant.Runtime.HUMAN_DECK[0].countMain() > 1) {
                HashMap<String, Integer> deckMap = new HashMap<String, Integer>();

                for (Entry<CardPrinted, Integer> s : Constant.Runtime.HUMAN_DECK[0].getMain()) {
                    deckMap.put(s.getKey().getName(), s.getValue());
                }

                String nl = System.getProperty("line.separator");
                StringBuilder deckList = new StringBuilder();
                String dName = Constant.Runtime.HUMAN_DECK[0].getName();

                if (dName == null) {
                    dName = "";
                } else {
                    deckList.append(dName + nl);
                }

                ArrayList<String> dmKeys = new ArrayList<String>();
                for (String s : deckMap.keySet()) {
                    dmKeys.add(s);
                }

                Collections.sort(dmKeys);

                for (String s : dmKeys) {
                    deckList.append(deckMap.get(s) + " x " + s + nl);
                }

                int rcMsg = -1138;
                String ttl = "Human's Decklist";
                if (!dName.equals("")) {
                    ttl += " - " + dName;
                }

                StringBuilder msg = new StringBuilder();
                if (deckMap.keySet().size() <= 32) {
                    msg.append(deckList.toString() + nl);
                } else {
                    msg.append("Decklist too long for dialog." + nl + nl);
                }

                msg.append("Copy Decklist to Clipboard?");

                rcMsg = JOptionPane.showConfirmDialog(null, msg, ttl, JOptionPane.OK_CANCEL_OPTION);

                if (rcMsg == JOptionPane.OK_OPTION) {
                    StringSelection ss = new StringSelection(deckList.toString());
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, null);
                }
            }
        }
    }

    private class TriggerReactionMenu extends JPopupMenu {
        private static final long serialVersionUID = 6665085414634139984L;
        private int workTrigID;

        public TriggerReactionMenu() {
            super();

            ForgeAction actAccept = new ForgeAction(Lang.GuiDisplay.Trigger.ALWAYSACCEPT) {
                private static final long serialVersionUID = -3734674058185367612L;

                @Override
                public final void actionPerformed(final ActionEvent e) {
                    AllZone.getTriggerHandler().setAlwaysAcceptTrigger(workTrigID);
                }
            };

            ForgeAction actDecline = new ForgeAction(Lang.GuiDisplay.Trigger.ALWAYSDECLINE) {
                private static final long serialVersionUID = -1983295769159971502L;

                @Override
                public final void actionPerformed(final ActionEvent e) {
                    AllZone.getTriggerHandler().setAlwaysDeclineTrigger(workTrigID);
                }
            };

            ForgeAction actAsk = new ForgeAction(Lang.GuiDisplay.Trigger.ALWAYSASK) {
                private static final long serialVersionUID = 5045255351332940821L;

                @Override
                public final void actionPerformed(final ActionEvent e) {
                    AllZone.getTriggerHandler().setAlwaysAskTrigger(workTrigID);
                }
            };

            JCheckBoxMenuItem jcbmiAccept = new JCheckBoxMenuItem(actAccept);
            JCheckBoxMenuItem jcbmiDecline = new JCheckBoxMenuItem(actDecline);
            JCheckBoxMenuItem jcbmiAsk = new JCheckBoxMenuItem(actAsk);

            add(jcbmiAccept);
            add(jcbmiDecline);
            add(jcbmiAsk);
        }

        public void setTrigger(final int trigID) {
            workTrigID = trigID;

            if (AllZone.getTriggerHandler().isAlwaysAccepted(trigID)) {
                ((JCheckBoxMenuItem) getComponent(0)).setState(true);
                ((JCheckBoxMenuItem) getComponent(1)).setState(false);
                ((JCheckBoxMenuItem) getComponent(2)).setState(false);
            } else if (AllZone.getTriggerHandler().isAlwaysDeclined(trigID)) {
                ((JCheckBoxMenuItem) getComponent(0)).setState(false);
                ((JCheckBoxMenuItem) getComponent(1)).setState(true);
                ((JCheckBoxMenuItem) getComponent(2)).setState(false);
            } else {
                ((JCheckBoxMenuItem) getComponent(0)).setState(false);
                ((JCheckBoxMenuItem) getComponent(1)).setState(false);
                ((JCheckBoxMenuItem) getComponent(2)).setState(true);
            }
        }
    }
}
