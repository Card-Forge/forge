/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
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
import forge.properties.NewConstants.Lang.GuiDisplay.ComputerHand;
import forge.properties.NewConstants.Lang.GuiDisplay.ComputerLibrary;
import forge.properties.NewConstants.Lang.GuiDisplay.HumanHand;
import forge.properties.NewConstants.Lang.GuiDisplay.HumanLibrary;

/**
 * <p>
 * GuiDisplay4 class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class GuiDisplay extends JFrame implements CardContainer, Display {
    /** Constant <code>serialVersionUID=4519302185194841060L</code>. */
    private static final long serialVersionUID = 4519302185194841060L;

    private final GuiInput inputControl;

    /** The stat font. */
    private final Font statFont = new Font("Dialog", Font.PLAIN, 12);

    /** The life font. */
    private final Font lifeFont = new Font("Dialog", Font.PLAIN, 40);
    // Font checkboxFont = new Font("Dialog", Font.PLAIN, 9);

    /** Constant <code>greenColor</code>. */
    private static Color greenColor = new Color(0, 164, 0);

    private Action humanGraveyardAction;
    private Action humanRemovedAction;
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
        this.setupActions();
        this.initComponents();

        this.addObservers();
        this.addListeners();
        this.addMenu();
        this.inputControl = new GuiInput();
    }

    /** {@inheritDoc} */
    @Override
    public final void setVisible(final boolean visible) {
        if (visible) {
            // causes an error if put in the constructor, causes some random
            // null pointer exception
            AllZone.getInputControl().updateObservers();

            // Use both so that when "un"maximizing, the frame isn't tiny
            this.setSize(1024, 740);
            this.setExtendedState(Frame.MAXIMIZED_BOTH);
        }
        super.setVisible(visible);
    }

    /** {@inheritDoc} */
    @Override
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
        this.humanGraveyardAction = new ZoneAction(AllZone.getHumanPlayer().getZone(Zone.Graveyard),
                NewConstants.Lang.GuiDisplay.HUMAN_GRAVEYARD);
        this.humanRemovedAction = new ZoneAction(AllZone.getHumanPlayer().getZone(Zone.Exile),
                NewConstants.Lang.GuiDisplay.HUMAN_REMOVED);
        this.humanFlashbackAction = new ZoneAction(AllZone.getHumanPlayer().getZone(Zone.Graveyard),
                NewConstants.Lang.GuiDisplay.HUMAN_FLASHBACK) {

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
        this.computerGraveyardAction = new ZoneAction(AllZone.getComputerPlayer().getZone(Zone.Graveyard),
                NewConstants.Lang.GuiDisplay.COMPUTER_GRAVEYARD);
        this.computerRemovedAction = new ZoneAction(AllZone.getComputerPlayer().getZone(Zone.Exile),
                NewConstants.Lang.GuiDisplay.COMPUTER_REMOVED);
        this.concedeAction = new ConcedeAction();

        this.humanDecklistAction = new DeckListAction(NewConstants.Lang.GuiDisplay.HUMAN_DECKLIST);
    }

    /**
     * <p>
     * addMenu.
     * </p>
     */
    private void addMenu() {
        // Trigger Context Menu creation
        this.triggerMenu = new TriggerReactionMenu();

        // Game Menu Creation
        final Object[] obj = { this.humanDecklistAction, this.humanGraveyardAction, this.humanRemovedAction,
                this.humanFlashbackAction, this.computerGraveyardAction, this.computerRemovedAction, new JSeparator(),
                GuiDisplay.playsoundCheckboxForMenu, new JSeparator(), ErrorViewer.ALL_THREADS_ACTION,
                this.concedeAction };

        final JMenu gameMenu = new JMenu(ForgeProps.getLocalized(NewConstants.Lang.GuiDisplay.MenuBar.Menu.TITLE));
        for (final Object o : obj) {
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
        final JMenu gamePhases = new JMenu(ForgeProps.getLocalized(NewConstants.Lang.GuiDisplay.MenuBar.PHASE.TITLE));

        final JMenuItem aiLabel = new JMenuItem("Computer");
        final JMenuItem humanLabel = new JMenuItem("Human");

        final Component[] objPhases = { aiLabel, GuiDisplay.cbAIUpkeep, GuiDisplay.cbAIDraw,
                GuiDisplay.cbAIBeginCombat, GuiDisplay.cbAIEndCombat, GuiDisplay.cbAIEndOfTurn, new JSeparator(),
                humanLabel, GuiDisplay.cbHumanUpkeep, GuiDisplay.cbHumanDraw, GuiDisplay.cbHumanBeginCombat,
                GuiDisplay.cbHumanEndCombat, GuiDisplay.cbHumanEndOfTurn };

        for (final Component cmp : objPhases) {
            gamePhases.add(cmp);
        }

        // Dev Mode Creation
        final JMenu devMenu = new JMenu(ForgeProps.getLocalized(NewConstants.Lang.GuiDisplay.MenuBar.DEV.TITLE));

        devMenu.setEnabled(Constant.Runtime.DEV_MODE[0]);

        if (Constant.Runtime.DEV_MODE[0]) {
            GuiDisplay.canLoseByDecking.setSelected(Constant.Runtime.MILL[0]);

            final Action viewAIHand = new ZoneAction(AllZone.getComputerPlayer().getZone(Zone.Hand), ComputerHand.BASE);
            final Action viewAILibrary = new ZoneAction(AllZone.getComputerPlayer().getZone(Zone.Library),
                    ComputerLibrary.BASE);
            final Action viewHumanLibrary = new ZoneAction(AllZone.getHumanPlayer().getZone(Zone.Library),
                    HumanLibrary.BASE);
            final ForgeAction generateMana = new ForgeAction(NewConstants.Lang.GuiDisplay.MANAGEN) {
                private static final long serialVersionUID = 7171104690016706405L;

                @Override
                public void actionPerformed(final ActionEvent arg0) {
                    GuiDisplayUtil.devModeGenerateMana();
                }
            };

            // + Battlefield setup +
            final ForgeAction setupBattleField = new ForgeAction(NewConstants.Lang.GuiDisplay.SETUPBATTLEFIELD) {
                private static final long serialVersionUID = -6660930759092583160L;

                @Override
                public void actionPerformed(final ActionEvent arg0) {
                    GuiDisplayUtil.devSetupGameState();
                }
            };
            // - Battlefield setup -

            // DevMode Tutor
            final ForgeAction tutor = new ForgeAction(NewConstants.Lang.GuiDisplay.TUTOR) {
                private static final long serialVersionUID = 2003222642609217705L;

                @Override
                public void actionPerformed(final ActionEvent arg0) {
                    GuiDisplayUtil.devModeTutor();
                }
            };
            // end DevMode Tutor

            // DevMode AddCounter
            final ForgeAction addCounter = new ForgeAction(NewConstants.Lang.GuiDisplay.ADDCOUNTER) {
                private static final long serialVersionUID = 3136264111882855268L;

                @Override
                public void actionPerformed(final ActionEvent arg0) {
                    GuiDisplayUtil.devModeAddCounter();
                }
            };
            // end DevMode AddCounter

            // DevMode Tap
            final ForgeAction tapPerm = new ForgeAction(NewConstants.Lang.GuiDisplay.TAPPERM) {
                private static final long serialVersionUID = -6092045653540313527L;

                @Override
                public void actionPerformed(final ActionEvent arg0) {
                    GuiDisplayUtil.devModeTapPerm();
                }
            };
            // end DevMode Tap

            // DevMode Untap
            final ForgeAction untapPerm = new ForgeAction(NewConstants.Lang.GuiDisplay.UNTAPPERM) {
                private static final long serialVersionUID = 5425291996157256656L;

                @Override
                public void actionPerformed(final ActionEvent arg0) {
                    GuiDisplayUtil.devModeUntapPerm();
                }
            };
            // end DevMode Untap

            // DevMode UnlimitedLand
            final ForgeAction unlimitedLand = new ForgeAction(NewConstants.Lang.GuiDisplay.NOLANDLIMIT) {
                private static final long serialVersionUID = 2184353891062202796L;

                @Override
                public void actionPerformed(final ActionEvent arg0) {
                    GuiDisplayUtil.devModeUnlimitedLand();
                }
            };
            // end DevMode UnlimitedLand

            // DevMode SetLife
            final ForgeAction setLife = new ForgeAction(NewConstants.Lang.GuiDisplay.SETLIFE) {
                private static final long serialVersionUID = -1750588303928974918L;

                @Override
                public void actionPerformed(final ActionEvent arg0) {
                    GuiDisplayUtil.devModeSetLife();
                }
            };
            // end DevMode SetLife

            final Object[] objDev = { GuiDisplay.canLoseByDecking, viewAIHand, viewAILibrary, viewHumanLibrary,
                    generateMana, setupBattleField, tutor, addCounter, tapPerm, untapPerm, unlimitedLand, setLife };
            for (final Object o : objDev) {
                if (o instanceof ForgeAction) {
                    devMenu.add(((ForgeAction) o).setupButton(new JMenuItem()));
                } else if (o instanceof Component) {
                    devMenu.add((Component) o);
                } else if (o instanceof Action) {
                    devMenu.add((Action) o);
                }
            }
        }

        final JMenuBar menuBar = new JMenuBar();
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
    @Override
    public final boolean canLoseByDecking() {
        return GuiDisplay.canLoseByDecking.isSelected();
    }

    /**
     * <p>
     * getButtonOK.
     * </p>
     * 
     * @return a {@link forge.MyButton} object.
     */
    @Override
    public final MyButton getButtonOK() {
        final MyButton ok = new MyButton() {
            @Override
            public void select() {
                GuiDisplay.this.inputControl.selectButtonOK();
            }

            @Override
            public boolean isSelectable() {
                return GuiDisplay.this.okButton.isEnabled();
            }

            @Override
            public void setSelectable(final boolean b) {
                GuiDisplay.this.okButton.setEnabled(b);
            }

            @Override
            public String getText() {
                return GuiDisplay.this.okButton.getText();
            }

            @Override
            public void setText(final String text) {
                GuiDisplay.this.okButton.setText(text);
            }

            @Override
            public void reset() {
                GuiDisplay.this.okButton.setText("OK");
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
    @Override
    public final MyButton getButtonCancel() {
        final MyButton cancel = new MyButton() {
            @Override
            public void select() {
                GuiDisplay.this.inputControl.selectButtonCancel();
            }

            @Override
            public boolean isSelectable() {
                return GuiDisplay.this.cancelButton.isEnabled();
            }

            @Override
            public void setSelectable(final boolean b) {
                GuiDisplay.this.cancelButton.setEnabled(b);
            }

            @Override
            public String getText() {
                return GuiDisplay.this.cancelButton.getText();
            }

            @Override
            public void setText(final String text) {
                GuiDisplay.this.cancelButton.setText(text);
            }

            @Override
            public void reset() {
                GuiDisplay.this.cancelButton.setText("Cancel");
            }
        };
        return cancel;
    } // getButtonCancel()

    /** {@inheritDoc} */
    @Override
    public final void showCombat(final String message) {
        this.combatArea.setText(message);
    }

    /** {@inheritDoc} */
    @Override
    public final void showMessage(final String s) {
        this.messageArea.setText(s);

        Border border = null;
        final int thickness = 3;

        if ((AllZone.getStack().size() > 0) && AllZone.getStack().peekInstance().getActivatingPlayer().isComputer()) {
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

        this.messageArea.setBorder(border);
    }

    /**
     * <p>
     * addListeners.
     * </p>
     */
    private void addListeners() {
        // mouse Card Detail
        this.playerHandPanel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(final MouseEvent me) {
                final Card c = GuiDisplay.this.playerHandPanel.getCardFromMouseOverPanel();
                if (c != null) {
                    GuiDisplay.this.setCard(c);
                }
            } // mouseMoved
        });

        this.playerPlayPanel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(final MouseEvent me) {
                final Card c = GuiDisplay.this.playerPlayPanel.getCardFromMouseOverPanel();
                if (c != null) {
                    GuiDisplay.this.setCard(c);
                }
            } // mouseMoved
        });

        this.oppPlayPanel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(final MouseEvent me) {
                final Card c = GuiDisplay.this.oppPlayPanel.getCardFromMouseOverPanel();
                if (c != null) {
                    GuiDisplay.this.setCard(c);
                }
            } // mouseMoved
        });

        // opponent life mouse listener
        this.oppLifeLabel.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(final MouseEvent e) {
                GuiDisplay.this.inputControl.selectPlayer(AllZone.getComputerPlayer());
            }
        });

        this.oppLifeLabel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(final MouseEvent me) {
                GuiDisplay.this.setCard(AllZone.getComputerPlayer().getManaPool());
            } // mouseMoved
        });

        // self life mouse listener
        this.playerLifeLabel.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(final MouseEvent e) {
                GuiDisplay.this.inputControl.selectPlayer(AllZone.getHumanPlayer());
            }
        });

        // self play (land) ---- Mouse
        this.playerPlayPanel.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(final MouseEvent e) {
                final Card c = GuiDisplay.this.playerPlayPanel.getCardFromMouseOverPanel();
                if (c != null) {

                    if (c.isTapped()
                            && ((GuiDisplay.this.inputControl.getInput() instanceof InputPayManaCost) || (GuiDisplay.this.inputControl
                                    .getInput() instanceof InputPayManaCostAbility))) {
                        final arcane.ui.CardPanel cardPanel = GuiDisplay.this.playerPlayPanel.getCardPanel(c
                                .getUniqueNumber());
                        for (final arcane.ui.CardPanel cp : cardPanel.getAttachedPanels()) {
                            if (cp.getCard().isUntapped()) {
                                break;
                            }
                        }
                    }

                    final CardList att = new CardList(AllZone.getCombat().getAttackers());
                    if ((c.isTapped() || c.hasSickness() || ((c.hasKeyword("Vigilance")) && att.contains(c)))
                            && (GuiDisplay.this.inputControl.getInput() instanceof InputAttack)) {
                        final arcane.ui.CardPanel cardPanel = GuiDisplay.this.playerPlayPanel.getCardPanel(c
                                .getUniqueNumber());
                        for (final arcane.ui.CardPanel cp : cardPanel.getAttachedPanels()) {
                            if (cp.getCard().isUntapped() && !cp.getCard().hasSickness()) {
                                break;
                            }
                        }
                    }

                    if (e.isMetaDown()) {
                        if (att.contains(c) && (GuiDisplay.this.inputControl.getInput() instanceof InputAttack)
                                && !c.hasKeyword("CARDNAME attacks each turn if able.")) {
                            c.untap();
                            AllZone.getCombat().removeFromCombat(c);
                        } else if (GuiDisplay.this.inputControl.getInput() instanceof InputBlock) {
                            if (c.getController().isHuman()) {
                                AllZone.getCombat().removeFromCombat(c);
                            }
                            ((InputBlock) GuiDisplay.this.inputControl.getInput()).removeFromAllBlocking(c);
                        }
                    } else {
                        GuiDisplay.this.inputControl.selectCard(c, AllZone.getHumanPlayer().getZone(Zone.Battlefield));
                    }
                }
            }
        });

        // self hand ---- Mouse
        this.playerHandPanel.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(final MouseEvent e) {
                if (e.getButton() != MouseEvent.BUTTON1) {
                    return;
                }
                final Card c = GuiDisplay.this.playerHandPanel.getCardFromMouseOverPanel();
                if (c != null) {
                    GuiDisplay.this.inputControl.selectCard(c, AllZone.getHumanPlayer().getZone(Zone.Hand));
                    GuiDisplay.this.okButton.requestFocusInWindow();
                }
            }
        });

        // *****************************************************************
        // computer

        // computer play (land) ---- Mouse
        this.oppPlayPanel.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(final MouseEvent e) {
                final Card c = GuiDisplay.this.oppPlayPanel.getCardFromMouseOverPanel();
                if (c != null) {
                    GuiDisplay.this.inputControl.selectCard(c, AllZone.getComputerPlayer().getZone(Zone.Battlefield));
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
    @Override
    public final Card getCard() {
        return this.detail.getCard();
    }

    /** {@inheritDoc} */
    @Override
    public final void setCard(final Card card) {
        this.detail.setCard(card);
        this.picture.setCard(card);
    }

    /**
     * <p>
     * addObservers.
     * </p>
     */
    private void addObservers() {
        // Remove all observers, placed by new UI.
        AllZone.getHumanPlayer().deleteObservers();
        AllZone.getHumanPlayer().getZone(Zone.Hand).deleteObservers();
        AllZone.getHumanPlayer().getZone(Zone.Graveyard).deleteObservers();
        AllZone.getHumanPlayer().getZone(Zone.Library).deleteObservers();
        AllZone.getHumanPlayer().getZone(Zone.Battlefield).deleteObservers();

        AllZone.getComputerPlayer().deleteObservers();
        AllZone.getComputerPlayer().getZone(Zone.Hand).deleteObservers();
        AllZone.getComputerPlayer().getZone(Zone.Graveyard).deleteObservers();
        AllZone.getComputerPlayer().getZone(Zone.Library).deleteObservers();
        AllZone.getComputerPlayer().getZone(Zone.Battlefield).deleteObservers();

        AllZone.getStack().deleteObservers();
        AllZone.getInputControl().deleteObservers();

        // long method
        final Observer o = new Observer() {
            @Override
            public void update(final Observable a, final Object b) {
                GuiDisplay.this.playerHandValue.setText("" + AllZone.getHumanPlayer().getZone(Zone.Hand).size());
                GuiDisplay.this.playerGraveValue.setText("" + AllZone.getHumanPlayer().getZone(Zone.Graveyard).size());
                GuiDisplay.this.playerLibraryValue.setText("" + AllZone.getHumanPlayer().getZone(Zone.Library).size());
                GuiDisplay.this.playerFBValue.setText(""
                        + CardFactoryUtil.getExternalZoneActivationCards(AllZone.getHumanPlayer()).size());
                GuiDisplay.this.playerRemovedValue.setText("" + AllZone.getHumanPlayer().getZone(Zone.Exile).size());

            }
        };
        AllZone.getHumanPlayer().getZone(Zone.Hand).addObserver(o);
        AllZone.getHumanPlayer().getZone(Zone.Graveyard).addObserver(o);
        AllZone.getHumanPlayer().getZone(Zone.Library).addObserver(o);
        // long method
        final Observer o1 = new Observer() {
            @Override
            public void update(final Observable a, final Object b) {
                GuiDisplay.this.oppHandValue.setText("" + AllZone.getComputerPlayer().getZone(Zone.Hand).size());
                GuiDisplay.this.oppGraveValue.setText("" + AllZone.getComputerPlayer().getZone(Zone.Graveyard).size());
                GuiDisplay.this.oppLibraryValue.setText("" + AllZone.getComputerPlayer().getZone(Zone.Library).size());
                GuiDisplay.this.oppRemovedValue.setText("" + AllZone.getComputerPlayer().getZone(Zone.Exile).size());
            }
        };
        AllZone.getComputerPlayer().getZone(Zone.Hand).addObserver(o1);
        AllZone.getComputerPlayer().getZone(Zone.Graveyard).addObserver(o1);
        AllZone.getComputerPlayer().getZone(Zone.Library).addObserver(o1);
        // opponent life
        this.oppLifeLabel.setText("" + AllZone.getComputerPlayer().getLife());
        AllZone.getComputerPlayer().addObserver(new Observer() {
            @Override
            public void update(final Observable a, final Object b) {
                final int life = AllZone.getComputerPlayer().getLife();
                GuiDisplay.this.oppLifeLabel.setText("" + life);
            }
        });
        AllZone.getComputerPlayer().updateObservers();

        if (AllZone.getQuestData() != null) {
            final File base = ForgeProps.getFile(NewConstants.IMAGE_ICON);
            String iconName = "";
            if (Constant.Quest.OPP_ICON_NAME[0] != null) {
                iconName = Constant.Quest.OPP_ICON_NAME[0];
                final File file = new File(base, iconName);
                final ImageIcon icon = new ImageIcon(file.toString());
                this.oppIconLabel.setIcon(icon);
                this.oppIconLabel.setAlignmentX(100);

            }
        }

        this.oppPCLabel.setText("Poison Counters: " + AllZone.getComputerPlayer().getPoisonCounters());
        AllZone.getComputerPlayer().addObserver(new Observer() {
            @Override
            public void update(final Observable a, final Object b) {
                final int pcs = AllZone.getComputerPlayer().getPoisonCounters();
                GuiDisplay.this.oppPCLabel.setText("Poison Counters: " + pcs);
            }
        });
        AllZone.getComputerPlayer().updateObservers();

        // player life
        this.playerLifeLabel.setText("" + AllZone.getHumanPlayer().getLife());
        AllZone.getHumanPlayer().addObserver(new Observer() {
            @Override
            public void update(final Observable a, final Object b) {
                final int life = AllZone.getHumanPlayer().getLife();
                GuiDisplay.this.playerLifeLabel.setText("" + life);
            }
        });
        AllZone.getHumanPlayer().updateObservers();

        this.playerPCLabel.setText("Poison Counters: " + AllZone.getHumanPlayer().getPoisonCounters());
        AllZone.getHumanPlayer().addObserver(new Observer() {
            @Override
            public void update(final Observable a, final Object b) {
                final int pcs = AllZone.getHumanPlayer().getPoisonCounters();
                GuiDisplay.this.playerPCLabel.setText("Poison Counters: " + pcs);
            }
        });
        AllZone.getHumanPlayer().updateObservers();

        // stack
        AllZone.getStack().addObserver(new Observer() {
            @Override
            public void update(final Observable a, final Object b) {
                GuiDisplay.this.stackPanel.removeAll();
                final MagicStack stack = AllZone.getStack();
                int count = 1;
                JLabel label;

                for (int i = stack.size() - 1; 0 <= i; i--) {
                    final int curI = i;
                    final String isOptional = stack.peekAbility(i).isOptionalTrigger()
                            && stack.peekAbility(i).getSourceCard().getController().isHuman() ? "(OPTIONAL) " : "";
                    label = new JLabel((count++) + ". " + isOptional + stack.peekInstance(i).getStackDescription());

                    // update card detail
                    final CardPanel cardPanel = new CardPanel(stack.peekInstance(i).getSpellAbility().getSourceCard());
                    cardPanel.setLayout(new BorderLayout());
                    cardPanel.add(label);
                    cardPanel.addMouseMotionListener(new MouseMotionAdapter() {

                        @Override
                        public void mouseMoved(final MouseEvent me) {
                            GuiDisplay.this.setCard(cardPanel.getCard());
                        } // mouseMoved
                    });

                    if (stack.peekInstance(curI).isOptionalTrigger()) {
                        cardPanel.addMouseListener(new MouseAdapter() {
                            @Override
                            public void mousePressed(final MouseEvent e) {
                                if (e.getButton() != MouseEvent.BUTTON3) {
                                    return;
                                }

                                GuiDisplay.this.triggerMenu.setTrigger(stack.peekAbility(curI).getSourceTrigger());
                                GuiDisplay.this.triggerMenu.show(e.getComponent(), e.getX(), e.getY());
                            }
                        });
                    }

                    GuiDisplay.this.stackPanel.add(cardPanel);
                }

                GuiDisplay.this.stackPanel.revalidate();
                GuiDisplay.this.stackPanel.repaint();

                GuiDisplay.this.okButton.requestFocusInWindow();

            }
        });
        AllZone.getStack().updateObservers();
        // END, stack

        // self hand
        AllZone.getHumanPlayer().getZone(Zone.Hand).addObserver(new Observer() {
            @Override
            public void update(final Observable a, final Object b) {
                final PlayerZone pZone = (PlayerZone) a;
                final HandArea p = GuiDisplay.this.playerHandPanel;

                final Card[] c = pZone.getCards();

                List<Card> tmp, diff;
                tmp = new ArrayList<Card>();
                for (final arcane.ui.CardPanel cpa : p.getCardPanels()) {
                    tmp.add(cpa.getGameCard());
                }
                diff = new ArrayList<Card>(tmp);
                diff.removeAll(Arrays.asList(c));
                if (diff.size() == p.getCardPanels().size()) {
                    p.clear();
                } else {
                    for (final Card card : diff) {
                        p.removeCardPanel(p.getCardPanel(card.getUniqueNumber()));
                    }
                }
                diff = new ArrayList<Card>(Arrays.asList(c));
                diff.removeAll(tmp);

                int fromZoneX = 0, fromZoneY = 0;
                final Rectangle pb = GuiDisplay.this.playerLibraryValue.getBounds();
                final Point zoneLocation = SwingUtilities.convertPoint(GuiDisplay.this.playerLibraryValue,
                        Math.round(pb.width / 2.0f), Math.round(pb.height / 2.0f), GuiDisplay.this.layeredPane);
                fromZoneX = zoneLocation.x;
                fromZoneY = zoneLocation.y;
                int startWidth, startX, startY;
                startWidth = 10;
                startX = fromZoneX - Math.round(startWidth / 2.0f);
                startY = fromZoneY - Math.round(Math.round(startWidth * arcane.ui.CardPanel.ASPECT_RATIO) / 2.0f);

                int endWidth, endX, endY;
                arcane.ui.CardPanel toPanel = null;

                for (final Card card : diff) {
                    toPanel = p.addCard(card);
                    endWidth = toPanel.getCardWidth();
                    final Point toPos = SwingUtilities.convertPoint(GuiDisplay.this.playerHandPanel,
                            toPanel.getCardLocation(), GuiDisplay.this.layeredPane);
                    endX = toPos.x;
                    endY = toPos.y;
                    final arcane.ui.CardPanel animationPanel = new arcane.ui.CardPanel(card);
                    if (GuiDisplay.this.isShowing()) {
                        Animation.moveCard(startX, startY, startWidth, endX, endY, endWidth, animationPanel, toPanel,
                                GuiDisplay.this.layeredPane, 500);
                    } else {
                        Animation.moveCard(toPanel);
                    }
                }
            }
        });
        //AllZone.getHumanPlayer().getZone(Zone.Hand).updateObservers();
        // END, self hand

        // self play
        AllZone.getHumanPlayer().getZone(Zone.Battlefield).addObserver(new Observer() {
            @Override
            public void update(final Observable a, final Object b) {
                final PlayerZone pZone = (PlayerZone) a;

                final Card[] c = pZone.getCards(false);

                GuiDisplayUtil.setupPlayZone(GuiDisplay.this.playerPlayPanel, c);
            }
        });
        AllZone.getHumanPlayer().getZone(Zone.Battlefield).updateObservers();
        // END - self play

        // computer play
        AllZone.getComputerPlayer().getZone(Zone.Battlefield).addObserver(new Observer() {
            @Override
            public void update(final Observable a, final Object b) {
                final PlayerZone pZone = (PlayerZone) a;

                final Card[] c = pZone.getCards(false);

                GuiDisplayUtil.setupPlayZone(GuiDisplay.this.oppPlayPanel, c);
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
        this.setTitle(ForgeProps.getLocalized(NewConstants.Lang.PROGRAM_NAME));
        this.setFont(new Font("Times New Roman", 0, 16));
        this.getContentPane().setLayout(new BorderLayout());

        // I tried using the JavaBeanConverter with this, but I got a
        // StackOverflowError due to an infinite loop. The default
        // XStream format seems just fine, anyway.
        final XStream xstream = new XStream();

        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent evt) {
                GuiDisplay.this.concede();
            }

            @Override
            public void windowClosed(final WindowEvent evt) {

                // Write the layout to the new file, usually
                // res/gui/display_new_layout.xml
                final File f = ForgeProps.getFile(NewConstants.Gui.GuiDisplay.LAYOUT_NEW);

                final Node layout = GuiDisplay.this.pane.getMultiSplitLayout().getModel();

                BufferedOutputStream out = null;
                try {
                    out = new BufferedOutputStream(new FileOutputStream(f));
                    xstream.toXML(layout, out);
                } catch (final IOException ex) {
                    assert System.err != null;

                    System.err.println("Ignoring exception:");
                    ex.printStackTrace(); // NOPMD by Braids on 8/21/11 9:20 PM
                    System.err.println("-------------------");
                } finally {
                    if (out != null) {
                        try {
                            out.close();
                        } catch (final Throwable ignored) { // NOPMD by Braids
                                                            // on
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
        final File file = ForgeProps.getFile(NewConstants.Gui.GuiDisplay.LAYOUT_NEW);

        try {
            model = GuiDisplay.loadModel(xstream, file);
        } catch (final XStreamException xse) {
            assert System.err != null;

            System.err.println("Error loading '" + file.getAbsolutePath() + "' using XStream: "
                    + xse.getLocalizedMessage());

            model = null;
        } catch (final FileNotFoundException e1) {
            model = null;
        }

        if (model == null) {
            assert System.err != null;

            System.err
                    .println("Unable to parse file '" + file.getAbsolutePath() + "' with XStream; trying XMLDecoder.");

            try {
                model = GuiDisplay.loadModelUsingXMLDecoder(file);
            } catch (final FileNotFoundException e1) {
                model = null;
            } catch (final Throwable exn) { // NOPMD by Braids on 8/21/11 9:20
                                            // PM
                System.err.println("Ignoring exception:");
                exn.printStackTrace(); // NOPMD by Braids on 8/21/11 9:20 PM
                System.err.println("-------------------");

                model = null;
            }
        }

        if (model == null) {
            System.err.println("XMLDecoder failed; using default layout.");
            final File defaultFile = ForgeProps.getFile(NewConstants.Gui.GuiDisplay.LAYOUT);

            try {
                model = GuiDisplay.loadModel(xstream, defaultFile);
            } catch (final Exception exn) {
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
            this.pane.getMultiSplitLayout().setModel(model);
        }

        this.pane.getMultiSplitLayout().setFloatingDividers(false);
        this.getContentPane().add(this.pane);

        // adding the individual parts

        this.initFonts(this.pane);

        this.initMsgYesNo(this.pane);
        this.initOpp(this.pane);
        this.initStackCombat(this.pane);
        this.initPlayer(this.pane);
        this.initZones(this.pane);
        this.initCardPicture(this.pane);
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
            } catch (final Throwable ignored) { // NOPMD by Braids on 8/21/11
                                                // 9:20 PM
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
            } catch (final Throwable ignored) { // NOPMD by Braids on 8/21/11
                                                // 9:20 PM
                // Ignore exceptions on close.
            }

            try {
                if (bufferedIn != null) {
                    bufferedIn.close();
                }
            } catch (final Throwable ignored) { // NOPMD by Braids on 8/21/11
                                                // 9:20 PM
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
        this.messageArea.setFont(this.getFont());

        this.oppLifeLabel.setFont(this.lifeFont);

        this.oppPCLabel.setFont(this.statFont);
        this.oppLibraryLabel.setFont(this.statFont);

        this.oppHandValue.setFont(this.statFont);
        this.oppLibraryValue.setFont(this.statFont);
        this.oppRemovedValue.setFont(this.statFont);
        this.oppGraveValue.setFont(this.statFont);

        this.playerLifeLabel.setFont(this.lifeFont);
        this.playerPCLabel.setFont(this.statFont);

        this.playerHandValue.setFont(this.statFont);
        this.playerLibraryValue.setFont(this.statFont);
        this.playerRemovedValue.setFont(this.statFont);
        this.playerGraveValue.setFont(this.statFont);
        this.playerFBValue.setFont(this.statFont);

        this.combatArea.setFont(this.getFont());
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
        this.messageArea.setEditable(false);
        this.messageArea.setLineWrap(true);
        this.messageArea.setWrapStyleWord(true);

        this.cancelButton.setText("Cancel");
        this.cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent evt) {
                GuiDisplay.this.cancelButtonActionPerformed(evt);
                GuiDisplay.this.okButton.requestFocusInWindow();
            }
        });
        this.okButton.setText("OK");
        this.okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent evt) {
                GuiDisplay.this.okButtonActionPerformed(evt);

                if (AllZone.getPhase().isNeedToNextPhase()) {
                    // moves to next turn
                    AllZone.getPhase().setNeedToNextPhase(false);
                    AllZone.getPhase().nextPhase();
                }
                GuiDisplay.this.okButton.requestFocusInWindow();
            }
        });
        this.okButton.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(final KeyEvent arg0) {
                // TODO make triggers on escape
                final int code = arg0.getKeyCode();
                if (code == KeyEvent.VK_ESCAPE) {
                    GuiDisplay.this.cancelButton.doClick();
                }
            }
        });

        this.okButton.requestFocusInWindow();

        // if(okButton.isEnabled())
        // okButton.doClick();
        final JPanel yesNoPanel = new JPanel(new FlowLayout());
        yesNoPanel.setBorder(new EtchedBorder());
        yesNoPanel.add(this.cancelButton);
        yesNoPanel.add(this.okButton);

        final JPanel panel = new JPanel(new BorderLayout());
        final JScrollPane scroll = new JScrollPane(this.messageArea);
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
        this.oppPCLabel.setForeground(GuiDisplay.greenColor);

        final JLabel oppHandLabel = new JLabel(ForgeProps.getLocalized(ComputerHand.BUTTON), SwingConstants.TRAILING);
        oppHandLabel.setFont(this.statFont);

        final JButton oppGraveButton = new JButton(this.computerGraveyardAction);
        oppGraveButton.setText((String) this.computerGraveyardAction.getValue("buttonText"));
        oppGraveButton.setMargin(new Insets(0, 0, 0, 0));
        oppGraveButton.setHorizontalAlignment(SwingConstants.TRAILING);
        oppGraveButton.setFont(this.statFont);

        final JPanel gravePanel = new JPanel(new BorderLayout());
        gravePanel.add(oppGraveButton, BorderLayout.EAST);

        final JButton oppRemovedButton = new JButton(this.computerRemovedAction);
        oppRemovedButton.setText((String) this.computerRemovedAction.getValue("buttonText"));
        oppRemovedButton.setMargin(new Insets(0, 0, 0, 0));
        // removedButton.setHorizontalAlignment(SwingConstants.TRAILING);
        oppRemovedButton.setFont(this.statFont);

        this.oppHandValue.setHorizontalAlignment(SwingConstants.LEADING);
        this.oppLibraryValue.setHorizontalAlignment(SwingConstants.LEADING);
        this.oppGraveValue.setHorizontalAlignment(SwingConstants.LEADING);
        this.oppRemovedValue.setHorizontalAlignment(SwingConstants.LEADING);

        final JPanel oppNumbersPanel = new JPanel(new GridLayout(0, 2, 3, 1));
        oppNumbersPanel.add(oppHandLabel);
        oppNumbersPanel.add(this.oppHandValue);
        oppNumbersPanel.add(oppRemovedButton);
        oppNumbersPanel.add(this.oppRemovedValue);
        oppNumbersPanel.add(this.oppLibraryLabel);
        oppNumbersPanel.add(this.oppLibraryValue);
        oppNumbersPanel.add(gravePanel);
        oppNumbersPanel.add(this.oppGraveValue);

        this.oppLifeLabel.setHorizontalAlignment(SwingConstants.CENTER);

        final JPanel oppIconLifePanel = new JPanel(new GridLayout(0, 1, 0, 0));
        oppIconLifePanel.add(this.oppIconLabel);
        oppIconLifePanel.add(this.oppLifeLabel);

        final JPanel oppPanel = new JPanel();
        oppPanel.setBorder(new TitledBorder(new EtchedBorder(), ForgeProps
                .getLocalized(NewConstants.Lang.GuiDisplay.COMPUTER_TITLE)));
        oppPanel.setLayout(new BorderLayout());
        oppPanel.add(oppNumbersPanel, BorderLayout.WEST);
        // oppPanel.add(oppIconLabel, BorderLayout.CENTER);
        // oppPanel.add(oppLifeLabel, BorderLayout.EAST);
        oppPanel.add(oppIconLifePanel, BorderLayout.EAST);
        oppPanel.add(this.oppPCLabel, BorderLayout.AFTER_LAST_LINE);
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
        this.stackPanel.setLayout(new GridLayout(0, 1, 10, 10));
        final JScrollPane stackPane = new JScrollPane(this.stackPanel);
        stackPane.setBorder(new EtchedBorder());
        pane.add(new ExternalPanel(stackPane), "stack");

        this.combatArea.setEditable(false);
        this.combatArea.setLineWrap(true);
        this.combatArea.setWrapStyleWord(true);

        final JScrollPane combatPane = new JScrollPane(this.combatArea);

        combatPane.setBorder(new TitledBorder(new EtchedBorder(), ForgeProps
                .getLocalized(NewConstants.Lang.GuiDisplay.COMBAT)));
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
        this.playerLifeLabel.setHorizontalAlignment(SwingConstants.CENTER);

        this.playerPCLabel.setForeground(GuiDisplay.greenColor);

        final JLabel playerLibraryLabel = new JLabel(ForgeProps.getLocalized(HumanLibrary.BUTTON),
                SwingConstants.TRAILING);
        playerLibraryLabel.setFont(this.statFont);

        final JLabel playerHandLabel = new JLabel(ForgeProps.getLocalized(HumanHand.TITLE), SwingConstants.TRAILING);
            playerHandLabel.setFont(this.statFont);

        // JLabel playerGraveLabel = new JLabel("Grave:",
        // SwingConstants.TRAILING);
        final JButton playerGraveButton = new JButton(this.humanGraveyardAction);
        playerGraveButton.setText((String) this.humanGraveyardAction.getValue("buttonText"));
        playerGraveButton.setMargin(new Insets(0, 0, 0, 0));
        playerGraveButton.setHorizontalAlignment(SwingConstants.TRAILING);
            playerGraveButton.setFont(this.statFont);

        final JButton playerFlashBackButton = new JButton(this.humanFlashbackAction);
        playerFlashBackButton.setText((String) this.humanFlashbackAction.getValue("buttonText"));
        playerFlashBackButton.setMargin(new Insets(0, 0, 0, 0));
        playerFlashBackButton.setHorizontalAlignment(SwingConstants.TRAILING);
            playerFlashBackButton.setFont(this.statFont);

        final JPanel gravePanel = new JPanel(new BorderLayout());
        gravePanel.add(playerGraveButton, BorderLayout.EAST);

        final JPanel playerFBPanel = new JPanel(new BorderLayout());
        playerFBPanel.add(playerFlashBackButton, BorderLayout.EAST);

        final JButton playerRemovedButton = new JButton(this.humanRemovedAction);
        playerRemovedButton.setText((String) this.humanRemovedAction.getValue("buttonText"));
        playerRemovedButton.setMargin(new Insets(0, 0, 0, 0));
        // removedButton.setHorizontalAlignment(SwingConstants.TRAILING);
        playerRemovedButton.setFont(this.statFont);

        this.playerHandValue.setHorizontalAlignment(SwingConstants.LEADING);
        this.playerLibraryValue.setHorizontalAlignment(SwingConstants.LEADING);
        this.playerGraveValue.setHorizontalAlignment(SwingConstants.LEADING);
        this.playerFBValue.setHorizontalAlignment(SwingConstants.LEADING);

        // playerRemovedValue.setFont(new Font("MS Sans Serif", 0, fontSize));
        this.playerRemovedValue.setHorizontalAlignment(SwingConstants.LEADING);

        final JPanel playerNumbersPanel = new JPanel(new GridLayout(0, 2, 5, 1));
        playerNumbersPanel.add(playerHandLabel);
        playerNumbersPanel.add(this.playerHandValue);
        playerNumbersPanel.add(playerRemovedButton);
        playerNumbersPanel.add(this.playerRemovedValue);
        playerNumbersPanel.add(playerLibraryLabel);
        playerNumbersPanel.add(this.playerLibraryValue);
        playerNumbersPanel.add(gravePanel);
        playerNumbersPanel.add(this.playerGraveValue);
        playerNumbersPanel.add(playerFBPanel);
        playerNumbersPanel.add(this.playerFBValue);

        final JPanel playerPanel = new JPanel();
        playerPanel.setBorder(new TitledBorder(new EtchedBorder(), ForgeProps
                .getLocalized(NewConstants.Lang.GuiDisplay.HUMAN_TITLE)));
        playerPanel.setLayout(new BorderLayout());
        playerPanel.add(playerNumbersPanel, BorderLayout.WEST);
        playerPanel.add(this.playerLifeLabel, BorderLayout.EAST);
        playerPanel.add(this.playerPCLabel, BorderLayout.AFTER_LAST_LINE);
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
        final JScrollPane oppScroll = new JScrollPane();
        this.oppPlayPanel = new PlayArea(oppScroll, true);
        oppScroll.setBorder(BorderFactory.createEtchedBorder());
        oppScroll.setViewportView(this.oppPlayPanel);
        pane.add(new ExternalPanel(oppScroll), "compyPlay");

        final JScrollPane playScroll = new JScrollPane();
        this.playerPlayPanel = new PlayArea(playScroll, false);
        playScroll.setBorder(BorderFactory.createEtchedBorder());
        playScroll.setViewportView(this.playerPlayPanel);
        pane.add(new ExternalPanel(playScroll), "humanPlay");

        final JScrollPane handScroll = new JScrollPane();
        this.playerHandPanel = new HandArea(handScroll, this);
        this.playerHandPanel.setBorder(BorderFactory.createEtchedBorder());
        handScroll.setViewportView(this.playerHandPanel);
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
        pane.add(new ExternalPanel(this.detail), "detail");
        pane.add(new ExternalPanel(this.picturePanel), "picture");
        this.picturePanel.setCardPanel(this.picture);
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
        this.inputControl.selectButtonCancel();
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
        this.inputControl.selectButtonOK();
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
    @Override
    public final boolean stopAtPhase(final Player turn, final String phase) {
        if (turn.isComputer()) {
            if (phase.equals(Constant.Phase.END_OF_TURN)) {
                return GuiDisplay.cbAIEndOfTurn.isSelected();
            } else if (phase.equals(Constant.Phase.UPKEEP)) {
                return GuiDisplay.cbAIUpkeep.isSelected();
            } else if (phase.equals(Constant.Phase.DRAW)) {
                return GuiDisplay.cbAIDraw.isSelected();
            } else if (phase.equals(Constant.Phase.COMBAT_BEGIN)) {
                return GuiDisplay.cbAIBeginCombat.isSelected();
            } else if (phase.equals(Constant.Phase.COMBAT_END)) {
                return GuiDisplay.cbAIEndCombat.isSelected();
            }
        } else {
            if (phase.equals(Constant.Phase.END_OF_TURN)) {
                return GuiDisplay.cbHumanEndOfTurn.isSelected();
            } else if (phase.equals(Constant.Phase.UPKEEP)) {
                return GuiDisplay.cbHumanUpkeep.isSelected();
            } else if (phase.equals(Constant.Phase.DRAW)) {
                return GuiDisplay.cbHumanDraw.isSelected();
            } else if (phase.equals(Constant.Phase.COMBAT_BEGIN)) {
                return GuiDisplay.cbHumanBeginCombat.isSelected();
            } else if (phase.equals(Constant.Phase.COMBAT_END)) {
                return GuiDisplay.cbHumanEndCombat.isSelected();
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
    @Override
    public final boolean loadPrefs() {
        final ForgePreferences fp = Singletons.getModel().getPreferences();

        GuiDisplay.cbAIUpkeep.setSelected(fp.isAIPhase("phase.ai.upkeep"));
        GuiDisplay.cbAIDraw.setSelected(fp.isAIPhase("phase.ai.draw"));
        GuiDisplay.cbAIEndOfTurn.setSelected(fp.isAIPhase("phase.ai.eot"));
        GuiDisplay.cbAIBeginCombat.setSelected(fp.isAIPhase("phase.ai.beginCombat"));
        GuiDisplay.cbAIEndCombat.setSelected(fp.isAIPhase("phase.ai.endCombat"));

        GuiDisplay.cbHumanUpkeep.setSelected(fp.isHumanPhase("phase.human.upkeep"));
        GuiDisplay.cbHumanDraw.setSelected(fp.isHumanPhase("phase.human.draw"));
        GuiDisplay.cbHumanEndOfTurn.setSelected(fp.isHumanPhase("phase.human.eot"));
        GuiDisplay.cbHumanBeginCombat.setSelected(fp.isHumanPhase("phase.human.beginCombat"));
        GuiDisplay.cbHumanEndCombat.setSelected(fp.isHumanPhase("phase.human.endCombat"));

        GuiDisplay.canLoseByDecking.setSelected(fp.isMillingLossCondition());

        return true;
    }

    /**
     * <p>
     * savePrefs.
     * </p>
     * 
     * @return a boolean.
     */
    @Override
    public final boolean savePrefs() {
        Constant.Runtime.MILL[0] = GuiDisplay.canLoseByDecking.isSelected();
        final ForgePreferences fp = Singletons.getModel().getPreferences();

        fp.setAIPhase("phase.ai.upkeep", GuiDisplay.cbAIUpkeep.isSelected());
        fp.setAIPhase("phase.ai.draw", GuiDisplay.cbAIDraw.isSelected());
        fp.setAIPhase("phase.ai.eot", GuiDisplay.cbAIEndOfTurn.isSelected());
        fp.setAIPhase("phase.ai.beginCombat", GuiDisplay.cbAIBeginCombat.isSelected());
        fp.setAIPhase("phase.ai.endCombat", GuiDisplay.cbAIEndCombat.isSelected());

        fp.setHumanPhase("phase.human.upkeep", GuiDisplay.cbHumanUpkeep.isSelected());
        fp.setHumanPhase("phase.human.draw", GuiDisplay.cbHumanDraw.isSelected());
        fp.setHumanPhase("phase.human.eot", GuiDisplay.cbHumanEndOfTurn.isSelected());
        fp.setHumanPhase("phase.human.beginCombat", GuiDisplay.cbHumanBeginCombat.isSelected());
        fp.setHumanPhase("phase.human.endCombat", GuiDisplay.cbHumanEndCombat.isSelected());

        fp.setMillingLossCondition(GuiDisplay.canLoseByDecking.isSelected());

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

    private final JXMultiSplitPane pane = new JXMultiSplitPane();
    private final JButton cancelButton = new JButton();
    private final JButton okButton = new JButton();
    private final JTextArea messageArea = new JTextArea(1, 10);
    private final JTextArea combatArea = new JTextArea();
    private final JPanel stackPanel = new JPanel();
    private PlayArea oppPlayPanel = null;
    private PlayArea playerPlayPanel = null;
    private HandArea playerHandPanel = null;
    // JPanel cdPanel = new JPanel();
    private final JLabel oppLifeLabel = new JLabel();
    private final JLabel oppIconLabel = new JLabel();
    private final JLabel playerLifeLabel = new JLabel();
    private final JLabel oppPCLabel = new JLabel();
    private final JLabel playerPCLabel = new JLabel();
    private final JLabel oppLibraryLabel = new JLabel(ForgeProps.getLocalized(ComputerLibrary.BUTTON),
            SwingConstants.TRAILING);
    private final JLabel oppHandValue = new JLabel();
    private final JLabel oppLibraryValue = new JLabel();
    private final JLabel oppGraveValue = new JLabel();
    private final JLabel oppRemovedValue = new JLabel();
    private final JLabel playerHandValue = new JLabel();
    private final JLabel playerLibraryValue = new JLabel();
    private final JLabel playerGraveValue = new JLabel();
    private final JLabel playerFBValue = new JLabel();
    private final JLabel playerRemovedValue = new JLabel();

    private final CardDetailPanel detail = new CardDetailPanel(null);
    private final ViewPanel picturePanel = new ViewPanel();
    private final arcane.ui.CardPanel picture = new arcane.ui.CardPanel(null);
    private final JLayeredPane layeredPane = SwingUtilities.getRootPane(this).getLayeredPane();

    private class ZoneAction extends ForgeAction {
        private static final long serialVersionUID = -5822976087772388839L;
        private final PlayerZone zone;
        private final String title;

        public ZoneAction(final PlayerZone zone, final String property) {
            super(property);
            this.title = ForgeProps.getLocalized(property + "/title");
            this.zone = zone;
        }

        @Override
        public void actionPerformed(final ActionEvent e) {
            Generator<Card> c = YieldUtils.toGenerator(this.getCardsAsIterable());

            if (AllZone.getNameChanger().shouldChangeCardName()) {
                c = AllZone.getNameChanger().changeCard(c);
            }

            final Iterable<Card> myIterable = YieldUtils.toIterable(c);
            final ArrayList<Card> choices = YieldUtils.toArrayList(myIterable);
            // System.out.println("immediately after: "+choices);
            // Iterator<Card> iter = myIterable.iterator();

            final ArrayList<Card> choices2 = new ArrayList<Card>();

            if (choices.isEmpty()) {
                GuiUtils.getChoiceOptional(this.title, new String[] { "no cards" });
            } else {
                for (int i = 0; i < choices.size(); i++) {
                    final Card crd = choices.get(i);
                    // System.out.println(crd+": "+crd.isFaceDown());
                    if (crd.isFaceDown()) {
                        final Card faceDown = new Card();
                        faceDown.setName("Face Down");
                        choices2.add(faceDown);
                        // System.out.println("Added: "+faceDown);
                    } else {
                        choices2.add(crd);
                    }
                }
                // System.out.println("Face down cards replaced: "+choices2);
                final Card choice = (Card) GuiUtils.getChoiceOptional(this.title, choices2.toArray());
                if (choice != null) {
                    this.doAction(choice);
                    /*
                     * Card choice = GuiUtils.getChoiceOptional(title, iter); if
                     * (choice != null) doAction(choice);
                     */
                }
            }
        }

        protected Iterable<Card> getCardsAsIterable() {
            return new ImmutableIterableFrom<Card>(Arrays.asList(this.zone.getCards()));
        }

        protected void doAction(final Card c) {
        }
    }

    private class ConcedeAction extends ForgeAction {

        private static final long serialVersionUID = -6976695235601916762L;

        public ConcedeAction() {
            super(NewConstants.Lang.GuiDisplay.CONCEDE);
        }

        @Override
        public void actionPerformed(final ActionEvent e) {
            GuiDisplay.this.concede();
        }
    }

    private class DeckListAction extends ForgeAction {
        public DeckListAction(final String property) {
            super(property);
        }

        private static final long serialVersionUID = 9874492387239847L;

        @Override
        public void actionPerformed(final ActionEvent e) {
            if (Constant.Runtime.HUMAN_DECK[0].countMain() > 1) {
                final HashMap<String, Integer> deckMap = new HashMap<String, Integer>();

                for (final Entry<CardPrinted, Integer> s : Constant.Runtime.HUMAN_DECK[0].getMain()) {
                    deckMap.put(s.getKey().getName(), s.getValue());
                }

                final String nl = System.getProperty("line.separator");
                final StringBuilder deckList = new StringBuilder();
                String dName = Constant.Runtime.HUMAN_DECK[0].getName();

                if (dName == null) {
                    dName = "";
                } else {
                    deckList.append(dName + nl);
                }

                final ArrayList<String> dmKeys = new ArrayList<String>();
                for (final String s : deckMap.keySet()) {
                    dmKeys.add(s);
                }

                Collections.sort(dmKeys);

                for (final String s : dmKeys) {
                    deckList.append(deckMap.get(s) + " x " + s + nl);
                }

                int rcMsg = -1138;
                String ttl = "Human's Decklist";
                if (!dName.equals("")) {
                    ttl += " - " + dName;
                }

                final StringBuilder msg = new StringBuilder();
                if (deckMap.keySet().size() <= 32) {
                    msg.append(deckList.toString() + nl);
                } else {
                    msg.append("Decklist too long for dialog." + nl + nl);
                }

                msg.append("Copy Decklist to Clipboard?");

                rcMsg = JOptionPane.showConfirmDialog(null, msg, ttl, JOptionPane.OK_CANCEL_OPTION);

                if (rcMsg == JOptionPane.OK_OPTION) {
                    final StringSelection ss = new StringSelection(deckList.toString());
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

            final ForgeAction actAccept = new ForgeAction(NewConstants.Lang.GuiDisplay.Trigger.ALWAYSACCEPT) {
                private static final long serialVersionUID = -3734674058185367612L;

                @Override
                public final void actionPerformed(final ActionEvent e) {
                    AllZone.getTriggerHandler().setAlwaysAcceptTrigger(TriggerReactionMenu.this.workTrigID);
                }
            };

            final ForgeAction actDecline = new ForgeAction(NewConstants.Lang.GuiDisplay.Trigger.ALWAYSDECLINE) {
                private static final long serialVersionUID = -1983295769159971502L;

                @Override
                public final void actionPerformed(final ActionEvent e) {
                    AllZone.getTriggerHandler().setAlwaysDeclineTrigger(TriggerReactionMenu.this.workTrigID);
                }
            };

            final ForgeAction actAsk = new ForgeAction(NewConstants.Lang.GuiDisplay.Trigger.ALWAYSASK) {
                private static final long serialVersionUID = 5045255351332940821L;

                @Override
                public final void actionPerformed(final ActionEvent e) {
                    AllZone.getTriggerHandler().setAlwaysAskTrigger(TriggerReactionMenu.this.workTrigID);
                }
            };

            final JCheckBoxMenuItem jcbmiAccept = new JCheckBoxMenuItem(actAccept);
            final JCheckBoxMenuItem jcbmiDecline = new JCheckBoxMenuItem(actDecline);
            final JCheckBoxMenuItem jcbmiAsk = new JCheckBoxMenuItem(actAsk);

            this.add(jcbmiAccept);
            this.add(jcbmiDecline);
            this.add(jcbmiAsk);
        }

        public void setTrigger(final int trigID) {
            this.workTrigID = trigID;

            if (AllZone.getTriggerHandler().isAlwaysAccepted(trigID)) {
                ((JCheckBoxMenuItem) this.getComponent(0)).setState(true);
                ((JCheckBoxMenuItem) this.getComponent(1)).setState(false);
                ((JCheckBoxMenuItem) this.getComponent(2)).setState(false);
            } else if (AllZone.getTriggerHandler().isAlwaysDeclined(trigID)) {
                ((JCheckBoxMenuItem) this.getComponent(0)).setState(false);
                ((JCheckBoxMenuItem) this.getComponent(1)).setState(true);
                ((JCheckBoxMenuItem) this.getComponent(2)).setState(false);
            } else {
                ((JCheckBoxMenuItem) this.getComponent(0)).setState(false);
                ((JCheckBoxMenuItem) this.getComponent(1)).setState(false);
                ((JCheckBoxMenuItem) this.getComponent(2)).setState(true);
            }
        }
    }
}
