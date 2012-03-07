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
package forge.control.match;

import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

import net.slightlymagic.braids.util.ImmutableIterableFrom;

import com.google.code.jyield.Generator;
import com.google.code.jyield.YieldUtils;

import forge.AllZone;
import forge.Card;
import forge.CardList;
import forge.Constant;
import forge.Constant.Zone;
import forge.GuiDisplayUtil;
import forge.Player;
import forge.PlayerZone;
import forge.Singletons;
import forge.card.cardfactory.CardFactoryUtil;
import forge.control.input.Input;
import forge.control.input.InputAttack;
import forge.control.input.InputBlock;
import forge.control.input.InputMana;
import forge.control.input.InputPayManaCost;
import forge.control.input.InputPayManaCostAbility;
import forge.gui.ForgeAction;
import forge.gui.GuiUtils;
import forge.gui.toolbox.FLabel;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;
import forge.properties.NewConstants.Lang.GuiDisplay.ComputerHand;
import forge.properties.NewConstants.Lang.GuiDisplay.ComputerLibrary;
import forge.properties.NewConstants.Lang.GuiDisplay.HumanHand;
import forge.properties.NewConstants.Lang.GuiDisplay.HumanLibrary;
import forge.view.match.ViewField;

/**
 * Child controller, applied to single field in battlefield. Manages player view
 * functions such as card observers, life total changes, graveyard button click,
 * etc.
 * 
 */
public class ControlField {
    private final Player player;
    private final ViewField view;

    private MouseMotionListener mmlCardOver;
    private MouseListener madAvatar, madLibrary, madHand, madExiled,
        madGraveyard, madFlashback, madCardClick, madBlack,
        madBlue, madGreen, madRed, madWhite, madColorless;

    private Observer observerZones, observerDetails, observerPlay;

    /**
     * Child controller, applied to single field in battlefield. Manages player
     * view functions such as card observers, life total changes, graveyard
     * button click, etc.
     * 
     * @param p
     *            &emsp; The Player this field applies to
     * @param v
     *            &emsp; The Swing component for this field
     */
    public ControlField(final Player p, final ViewField v) {
        this.player = p;
        this.view = v;

        initMouseAdapters();
        initObservers();

        addObservers();
        addListeners();
    }

    /**
     * Gets the player.
     * 
     * @return Player
     */
    public Player getPlayer() {
        return this.player;
    }

    /**
     * Gets the view.
     * 
     * @return ViewField
     */
    public ViewField getView() {
        return this.view;
    }

    /**
     * Adds observers to field components where required: card stats, player
     * stats, etc.
     */
    public void addObservers() {
        this.player.getZone(Zone.Hand).addObserver(observerZones);
        this.player.addObserver(observerDetails);
        this.player.getZone(Zone.Battlefield).addObserver(observerPlay);
    }

    /**
     * Listeners for user actions on the battlefield.
     * 
     */
    public void addListeners() {
        // Battlefield card clicks
        this.view.getTabletop().removeMouseListener(madCardClick);
        this.view.getTabletop().addMouseListener(madCardClick);

        // Battlefield card mouseover
        this.view.getTabletop().removeMouseMotionListener(mmlCardOver);
        this.view.getTabletop().addMouseMotionListener(mmlCardOver);

        // Player select
        this.view.getAvatarArea().removeMouseListener(madAvatar);
        this.view.getAvatarArea().addMouseListener(madAvatar);

        // Graveyard card list button
        ((FLabel) this.view.getLblGraveyard()).setHoverable(true);
        this.view.getLblGraveyard().removeMouseListener(madGraveyard);
        this.view.getLblGraveyard().addMouseListener(madGraveyard);
        // Exile card list button
        ((FLabel) this.view.getLblExile()).setHoverable(true);
        this.view.getLblExile().removeMouseListener(madExiled);
        this.view.getLblExile().addMouseListener(madExiled);

        // Library card list button
        this.view.getLblLibrary().removeMouseListener(madLibrary);
        this.view.getLblLibrary().addMouseListener(madLibrary);

        this.view.getLblHand().removeMouseListener(madHand);
        this.view.getLblHand().addMouseListener(madHand);

        // Flashback card list button
        ((FLabel) this.view.getLblFlashback()).setHoverable(true);
        this.view.getLblFlashback().removeMouseListener(madFlashback);
        this.view.getLblFlashback().addMouseListener(madFlashback);

        ((FLabel) this.view.getLblBlack()).setHoverable(true);
        this.view.getLblBlack().removeMouseListener(madBlack);
        this.view.getLblBlack().addMouseListener(madBlack);

        ((FLabel) this.view.getLblBlue()).setHoverable(true);
        this.view.getLblBlue().removeMouseListener(madBlue);
        this.view.getLblBlue().addMouseListener(madBlue);

        ((FLabel) this.view.getLblGreen()).setHoverable(true);
        this.view.getLblGreen().removeMouseListener(madGreen);
        this.view.getLblGreen().addMouseListener(madGreen);

        ((FLabel) this.view.getLblRed()).setHoverable(true);
        this.view.getLblRed().removeMouseListener(madRed);
        this.view.getLblRed().addMouseListener(madRed);

        ((FLabel) this.view.getLblWhite()).setHoverable(true);
        this.view.getLblWhite().removeMouseListener(madWhite);
        this.view.getLblWhite().addMouseListener(madWhite);

        ((FLabel) this.view.getLblColorless()).setHoverable(true);
        this.view.getLblColorless().removeMouseListener(madColorless);
        this.view.getLblColorless().addMouseListener(madColorless);
    }

    /**
     * Resets all phase buttons to "inactive", so highlight won't be drawn on
     * them. "Enabled" state remains the same.
     */
    public void resetPhaseButtons() {
        this.view.getLblUpkeep().setActive(false);
        this.view.getLblDraw().setActive(false);
        this.view.getLblMain1().setActive(false);
        this.view.getLblBeginCombat().setActive(false);
        this.view.getLblDeclareAttackers().setActive(false);
        this.view.getLblDeclareBlockers().setActive(false);
        this.view.getLblFirstStrike().setActive(false);
        this.view.getLblCombatDamage().setActive(false);
        this.view.getLblEndCombat().setActive(false);
        this.view.getLblMain2().setActive(false);
        this.view.getLblEndTurn().setActive(false);
        this.view.getLblCleanup().setActive(false);
    }

    /**
     * Receives click and programmatic requests for viewing data stacks in the
     * "zones" of a player field: hand, library, etc.
     * 
     */
    private class ZoneAction extends ForgeAction {
        private static final long serialVersionUID = -5822976087772388839L;
        private final PlayerZone zone;
        private final String title;

        /**
         * Receives click and programmatic requests for viewing data stacks in
         * the "zones" of a player field: hand, graveyard, etc. The library
         * "zone" is an exception to the rule; it's handled in DeckListAction.
         * 
         * @param zone
         *            &emsp; PlayerZone obj
         * @param property
         *            &emsp; String obj
         */
        public ZoneAction(final PlayerZone zone, final String property) {
            super(property);
            this.title = ForgeProps.getLocalized(property + "/title");
            this.zone = zone;
        }

        /**
         * @param e
         *            &emsp; ActionEvent obj
         */
        @Override
        public void actionPerformed(final ActionEvent e) {
            Generator<Card> c = YieldUtils.toGenerator(this.getCardsAsIterable());

            if (AllZone.getNameChanger().shouldChangeCardName()) {
                c = AllZone.getNameChanger().changeCard(c);
            }

            final Iterable<Card> myIterable = YieldUtils.toIterable(c);
            final ArrayList<Card> choices = YieldUtils.toArrayList(myIterable);

            final ArrayList<Card> choices2 = new ArrayList<Card>();

            if (choices.isEmpty()) {
                GuiUtils.chooseOneOrNone(this.title, new String[] { "no cards" });
            } else {
                for (int i = 0; i < choices.size(); i++) {
                    final Card crd = choices.get(i);
                    if (crd.isFaceDown()) {
                        if (crd.getController().isComputer() || !crd.hasKeyword("You may look at this card.")) {
                            final Card faceDown = new Card();
                            faceDown.setName("Face Down");
                            choices2.add(faceDown);
                        } else {
                            final Card faceDown = AllZone.getCardFactory().copyCard(crd);
                            faceDown.turnFaceUp();
                            choices2.add(faceDown);
                        }
                    } else {
                        choices2.add(crd);
                    }
                }
                final Card choice = (Card) GuiUtils.chooseOneOrNone(this.title, choices2.toArray());
                if (choice != null) {
                    this.doAction(choice);
                }
            }
        }

        protected Iterable<Card> getCardsAsIterable() {
            return new ImmutableIterableFrom<Card>(this.zone.getCards());
        }

        protected void doAction(final Card c) {
        }
    } // End ZoneAction

    private void initObservers() {
        // Hand, Graveyard, Library, Flashback, Exile zones, attached to hand.
        observerZones = new Observer() {
            @Override
            public void update(final Observable a, final Object b) {
                ControlField.this.view.updateZones(ControlField.this.player);
            }
        };

        // Life total, poison total, and keywords, attached directly to Player.
        observerDetails = new Observer() {
            @Override
            public void update(final Observable a, final Object b) {
                ControlField.this.view.updateDetails(ControlField.this.player);
            }
        };

        // Card play area, attached to battlefield zone.
        observerPlay = new Observer() {
            @Override
            public void update(final Observable a, final Object b) {
                final PlayerZone pZone = (PlayerZone) a;
                GuiDisplayUtil.setupPlayZone(ControlField.this.view.getTabletop(), pZone.getCards(false));
            }
        };
    }

    /** Simple method that inits the mouse adapters for listeners,
     * here to simplify life in the constructor.
     */
    private void initMouseAdapters() {
        // Hand listener
        madHand = new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                if (!ControlField.this.player.isComputer()) {
                    new ZoneAction(ControlField.this.player.getZone(Zone.Hand), HumanHand.BASE)
                    .actionPerformed(null);
                } else if (Constant.Runtime.DEV_MODE[0]
                        || ControlField.this.player.hasKeyword("Play with your hand revealed.")) {
                    new ZoneAction(ControlField.this.player.getZone(Zone.Hand), ComputerHand.BASE)
                    .actionPerformed(null);
                }
            }
        };

        // Flashback listener
        madFlashback = new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                if (!ControlField.this.player.isComputer()) {
                    new ZoneAction(AllZone.getHumanPlayer().getZone(Zone.Graveyard),
                            NewConstants.Lang.GuiDisplay.HUMAN_FLASHBACK) {

                        private static final long serialVersionUID = 8120331222693706164L;

                        @Override
                        protected Iterable<Card> getCardsAsIterable() {
                            return new ImmutableIterableFrom<Card>(CardFactoryUtil.getExternalZoneActivationCards(AllZone
                                    .getHumanPlayer()));
                        }

                        @Override
                        protected void doAction(final Card c) {
                            Singletons.getModel().getGameAction().playCard(c);
                        }
                    } .actionPerformed(null);
                } else {
                    new ZoneAction(ControlField.this.player.getZone(Zone.Graveyard),
                            NewConstants.Lang.GuiDisplay.COMPUTER_FLASHBACK) {

                        private static final long serialVersionUID = 8120331222693706164L;

                        @Override
                        protected Iterable<Card> getCardsAsIterable() {
                            return new ImmutableIterableFrom<Card>(CardFactoryUtil.getExternalZoneActivationCards(AllZone
                                    .getComputerPlayer()));
                        }

                        @Override
                        protected void doAction(final Card c) {
                            Singletons.getModel().getGameAction().playCard(c);
                        }
                    } .actionPerformed(null);
                }
            }
        };

        // Library listener
        madLibrary = new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                if (!Constant.Runtime.DEV_MODE[0]) { return; }

                if (!ControlField.this.player.isComputer()) {
                    new ZoneAction(ControlField.this.player.getZone(Zone.Library), HumanLibrary.BASE)
                    .actionPerformed(null);
                } else {
                    new ZoneAction(ControlField.this.player.getZone(Zone.Library), ComputerLibrary.BASE)
                    .actionPerformed(null);
                }
            }
        };

        // Exiled adapter
        madExiled = new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                if (ControlField.this.player.isComputer()) {
                    new ZoneAction(ControlField.this.player.getZone(Zone.Exile),
                            NewConstants.Lang.GuiDisplay.COMPUTER_EXILED).actionPerformed(null);
                } else {
                    new ZoneAction(ControlField.this.player.getZone(Zone.Exile),
                            NewConstants.Lang.GuiDisplay.HUMAN_EXILED).actionPerformed(null);
                }
            }
        };

        // Graveyard adapter
        madGraveyard = new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                if (ControlField.this.player.isComputer()) {
                    new ZoneAction(ControlField.this.player.getZone(Zone.Graveyard),
                            NewConstants.Lang.GuiDisplay.COMPUTER_GRAVEYARD).actionPerformed(null);
                } else {
                    new ZoneAction(ControlField.this.player.getZone(Zone.Graveyard),
                            NewConstants.Lang.GuiDisplay.HUMAN_GRAVEYARD).actionPerformed(null);
                }
            }
        };

        // Avatar
        madAvatar = new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                if (ControlField.this.player.isComputer()) {
                    Singletons.getControl().getControlMatch().getMessageControl()
                        .getInputControl().selectPlayer(AllZone.getComputerPlayer());
                } else {
                    Singletons.getControl().getControlMatch().getMessageControl()
                        .getInputControl().selectPlayer(AllZone.getHumanPlayer());
                }
            }
        };

        // Battlefield card mouse over
        mmlCardOver = new MouseMotionAdapter() {
            @Override
            public void mouseMoved(final MouseEvent me) {
                final Card c = ControlField.this.view.getTabletop().getCardFromMouseOverPanel();
                if (c != null) {
                    Singletons.getControl().getControlMatch().setCard(c);
                }
            }
        };

        // Battlefield card
        madCardClick = new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {

                // original version:
                // final Card c = t.getDetailController().getCurrentCard();
                // Roujin's bug fix version dated 2-12-2012
                final Card c = ControlField.this.view.getTabletop().getCardFromMouseOverPanel();

                final Input input = Singletons.getControl().getControlMatch().getMessageControl().getInputControl().getInput();

                if (c != null && c.isInZone(Zone.Battlefield)) {
                    if (c.isTapped()
                            && ((input instanceof InputPayManaCost) || (input instanceof InputPayManaCostAbility))) {
                        final arcane.ui.CardPanel cardPanel = ControlField.this.view.getTabletop().getCardPanel(
                                c.getUniqueNumber());
                        for (final arcane.ui.CardPanel cp : cardPanel.getAttachedPanels()) {
                            if (cp.getCard().isUntapped()) {
                                break;
                            }
                        }
                    }

                    final CardList att = AllZone.getCombat().getAttackerList();
                    if ((c.isTapped() || c.hasSickness() || ((c.hasKeyword("Vigilance")) && att.contains(c)))
                            && (input instanceof InputAttack)) {
                        final arcane.ui.CardPanel cardPanel = ControlField.this.view.getTabletop().getCardPanel(
                                c.getUniqueNumber());
                        for (final arcane.ui.CardPanel cp : cardPanel.getAttachedPanels()) {
                            if (cp.getCard().isUntapped() && !cp.getCard().hasSickness()) {
                                break;
                            }
                        }
                    }

                    if (e.isMetaDown()) {
                        if (att.contains(c) && (input instanceof InputAttack)
                                && !c.hasKeyword("CARDNAME attacks each turn if able.")) {
                            c.untap();
                            AllZone.getCombat().removeFromCombat(c);
                        } else if (input instanceof InputBlock) {
                            if (c.getController().isHuman()) {
                                AllZone.getCombat().removeFromCombat(c);
                            }
                            ((InputBlock) input).removeFromAllBlocking(c);
                        }
                    } else {
                        Singletons.getControl().getControlMatch().getMessageControl()
                            .getInputControl().selectCard(c,
                                    AllZone.getHumanPlayer().getZone(Zone.Battlefield));
                    }
                }
            }
        };

        madBlack = new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                if (ControlField.this.player.isComputer()) {
                    System.out.println("Stop trying to spend the AI's mana");
                    // TODO: Mindslaver might need to add changes here
                } else {
                    Input in = AllZone.getInputControl().getInput();
                    if (in instanceof InputMana) {
                        // Do something
                        ((InputMana) in).selectManaPool(Constant.Color.BLACK);
                    }
                }
            }
        };

        madBlue = new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                if (ControlField.this.player.isComputer()) {
                    System.out.println("Stop trying to spend the AI's mana");
                    // TODO: Mindslaver might need to add changes here
                } else {
                    Input in = AllZone.getInputControl().getInput();
                    if (in instanceof InputMana) {
                        // Do something
                        ((InputMana) in).selectManaPool(Constant.Color.BLUE);
                    }
                }
            }
        };

        madGreen = new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                if (ControlField.this.player.isComputer()) {
                    System.out.println("Stop trying to spend the AI's mana");
                    // TODO: Mindslaver might need to add changes here
                } else {
                    Input in = AllZone.getInputControl().getInput();
                    if (in instanceof InputMana) {
                        // Do something
                        ((InputMana) in).selectManaPool(Constant.Color.GREEN);
                    }
                }
            }
        };

        madRed = new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                if (ControlField.this.player.isComputer()) {
                    System.out.println("Stop trying to spend the AI's mana");
                    // TODO: Mindslaver might need to add changes here
                } else {
                    Input in = AllZone.getInputControl().getInput();
                    if (in instanceof InputMana) {
                        // Do something
                        ((InputMana) in).selectManaPool(Constant.Color.RED);
                    }
                }
            }
        };

        madWhite = new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                if (ControlField.this.player.isComputer()) {
                    System.out.println("Stop trying to spend the AI's mana");
                    // TODO: Mindslaver might need to add changes here
                } else {
                    Input in = AllZone.getInputControl().getInput();
                    if (in instanceof InputMana) {
                        // Do something
                        ((InputMana) in).selectManaPool(Constant.Color.WHITE);
                    }
                }
            }
        };

        madColorless = new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                if (ControlField.this.player.isComputer()) {
                    System.out.println("Stop trying to spend the AI's mana");
                    // TODO: Mindslaver might need to add changes here
                } else {
                    Input in = AllZone.getInputControl().getInput();
                    if (in instanceof InputMana) {
                        // Do something
                        ((InputMana) in).selectManaPool(Constant.Color.COLORLESS);
                    }
                }
            }
        };
    } // End initMouseAdapters()
} // End class ControlField
