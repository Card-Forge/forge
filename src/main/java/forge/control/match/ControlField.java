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
import java.awt.event.MouseMotionAdapter;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Observable;
import java.util.Observer;

import javax.swing.ImageIcon;

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
import forge.card.cardfactory.CardFactoryUtil;
import forge.gui.ForgeAction;
import forge.gui.GuiUtils;
import forge.gui.input.Input;
import forge.gui.input.InputMana;
import forge.gui.input.InputAttack;
import forge.gui.input.InputBlock;
import forge.gui.input.InputPayManaCost;
import forge.gui.input.InputPayManaCostAbility;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;
import forge.properties.NewConstants.Lang.GuiDisplay.ComputerHand;
import forge.properties.NewConstants.Lang.GuiDisplay.ComputerLibrary;
import forge.properties.NewConstants.Lang.GuiDisplay.HumanHand;
import forge.properties.NewConstants.Lang.GuiDisplay.HumanLibrary;
import forge.view.GuiTopLevel;
import forge.view.match.ViewField;
import forge.view.match.ViewField.DetailLabel;
import forge.view.match.ViewTopLevel;

/**
 * Child controller, applied to single field in battlefield. Manages player view
 * functions such as card observers, life total changes, graveyard button click,
 * etc.
 * 
 */
public class ControlField {
    private final Player player;
    private final ViewField view;

    private MouseMotionAdapter maCardOver;
    private MouseAdapter maAvatar, maLibrary, maHand, maExiled, maGraveyard, maFlashback, maCardClick;

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
        // Hand, Graveyard, Library, Flashback, Exile zones, attached to hand.
        final Observer o1 = new Observer() {
            @Override
            public void update(final Observable a, final Object b) {
                ControlField.this.view.updateZones(ControlField.this.player);
            }
        };

        this.player.getZone(Zone.Hand).addObserver(o1);

        // Life total, poison total, and keywords, attached directly to Player.
        final Observer o2 = new Observer() {
            @Override
            public void update(final Observable a, final Object b) {
                ControlField.this.view.updateDetails(ControlField.this.player);
            }
        };
        this.player.addObserver(o2);

        if (AllZone.getQuestData() != null && this.player.isComputer()) {
            final File base = ForgeProps.getFile(NewConstants.IMAGE_ICON);
            String iconName = "";
            if (Constant.Quest.OPP_ICON_NAME[0] != null) {
                iconName = Constant.Quest.OPP_ICON_NAME[0];
                final File file = new File(base, iconName);
                this.getView().setImage(new ImageIcon(file.toString()).getImage());

            }
        }

        // Card play area, attached to battlefield zone.
        final Observer o3 = new Observer() {
            @Override
            public void update(final Observable a, final Object b) {
                final PlayerZone pZone = (PlayerZone) a;
                final Card[] c = pZone.getCards(false);
                GuiDisplayUtil.setupPlayZone(ControlField.this.view.getTabletop(), c);
            }
        };

        this.player.getZone(Zone.Battlefield).addObserver(o3);
    }

    /**
     * Listeners for user actions on the battlefield.
     * 
     */
    public void addListeners() {
        // When/if zone action properties become less specific, the conditional
        // tests for computer/human players can be removed. If that's not ever
        // going to happen, this comment can be removed. :) Doublestrike
        // 29-10-11.

        this.addZoneListeners();
        this.addPoolListeners();

        // Battlefield card clicks
        this.view.getTabletop().removeMouseListener(maCardClick);
        this.view.getTabletop().addMouseListener(maCardClick);

        // Battlefield card mouseover
        this.view.getTabletop().removeMouseMotionListener(maCardOver);
        this.view.getTabletop().addMouseMotionListener(maCardOver);

        // Player select
        this.view.getAvatarArea().removeMouseListener(maAvatar);
        this.view.getAvatarArea().addMouseListener(maAvatar);
    } // End addListeners()

    /**
     * Adds listeners to "zone" labels: flashback, graveyard, etc. This method
     * only exists to avoid the 150-line limit in the checkstyle rules.
     */
    private void addZoneListeners() {
        // Graveyard card list button
        this.view.getLblGraveyard().enableHover();
        this.view.getLblGraveyard().removeMouseListener(maGraveyard);
        this.view.getLblGraveyard().addMouseListener(maGraveyard);
        // Exile card list button
        this.view.getLblExile().enableHover();
        this.view.getLblExile().removeMouseListener(maExiled);
        this.view.getLblExile().addMouseListener(maExiled);

        // Library card list button
        if (Constant.Runtime.DEV_MODE[0]) {
            this.view.getLblLibrary().enableHover();
            this.view.getLblLibrary().removeMouseListener(maLibrary);
            this.view.getLblLibrary().addMouseListener(maLibrary);
        }

        // Flashback card list button
        this.view.getLblFlashback().enableHover();
        this.view.getLblFlashback().removeMouseListener(maFlashback);
        this.view.getLblFlashback().addMouseListener(maFlashback);

        // Hand button
        if (Constant.Runtime.DEV_MODE[0]) {
            this.view.getLblHand().enableHover();
            this.view.getLblHand().removeMouseListener(maHand);
            this.view.getLblHand().addMouseListener(maHand);
        }
    }

    /**
     * Adds listeners to mana "pool" labels, for paying mana.
     */
    private void addPoolListeners() {
        this.addPoolListener(this.view.getLblBlack(), Constant.Color.BLACK);
        this.addPoolListener(this.view.getLblBlue(), Constant.Color.BLUE);
        this.addPoolListener(this.view.getLblGreen(), Constant.Color.GREEN);
        this.addPoolListener(this.view.getLblRed(), Constant.Color.RED);
        this.addPoolListener(this.view.getLblWhite(), Constant.Color.WHITE);
        this.addPoolListener(this.view.getLblColorless(), Constant.Color.COLORLESS);
    }

    private void addPoolListener(DetailLabel label, final String color) {
        label.enableHover();
        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                if (ControlField.this.player.isComputer()) {
                    System.out.print("Stop trying to spend the AI's mana");
                    // TODO: Mindslaver might need to add changes here
                } else {
                    Input in = AllZone.getInputControl().getInput();
                    if (in instanceof InputMana) {
                        // Do something
                        ((InputMana) in).selectManaPool(color);
                    }
                }
            }
        });
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
    } // End ZoneAction

    /** Simple method that inits the mouse adapters for listeners,
     * here to simplify life in the constructor.
     */
    private void initMouseAdapters() {
        // Hand listener
        maHand = new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                if (!ControlField.this.player.isComputer()) {
                    new ZoneAction(ControlField.this.player.getZone(Zone.Hand), HumanHand.BASE)
                    .actionPerformed(null);
                } else {
                    new ZoneAction(ControlField.this.player.getZone(Zone.Hand), ComputerHand.BASE)
                    .actionPerformed(null);
                }
            }
        };

        // Flashback listener
        maFlashback = new MouseAdapter() {
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
                            AllZone.getGameAction().playCard(c);
                        }
                    } .actionPerformed(null);
                } else {
                    new ZoneAction(ControlField.this.player.getZone(Zone.Graveyard),
                            NewConstants.Lang.GuiDisplay.COMPUTER_FLASHBACK).actionPerformed(null);
                }
            }
        };

        // Library listener
        maLibrary = new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
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
        maExiled = new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                if (ControlField.this.player.isComputer()) {
                    new ZoneAction(ControlField.this.player.getZone(Zone.Exile),
                            NewConstants.Lang.GuiDisplay.COMPUTER_REMOVED).actionPerformed(null);
                } else {
                    new ZoneAction(ControlField.this.player.getZone(Zone.Exile),
                            NewConstants.Lang.GuiDisplay.HUMAN_REMOVED).actionPerformed(null);
                }
            }
        };

        // Graveyard adapter
        maGraveyard = new MouseAdapter() {
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
        maAvatar = new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                final ViewTopLevel t = ((GuiTopLevel) AllZone.getDisplay()).getController().getMatchController().getView();
                if (ControlField.this.player.isComputer()) {
                    t.getInputController().getInputControl().selectPlayer(AllZone.getComputerPlayer());
                } else {
                    t.getInputController().getInputControl().selectPlayer(AllZone.getHumanPlayer());
                }
            }
        };

        // Battlefield card mouse over
        maCardOver = new MouseMotionAdapter() {
            @Override
            public void mouseMoved(final MouseEvent me) {
                final ViewTopLevel t = ((GuiTopLevel) AllZone.getDisplay()).getController().getMatchController().getView();
                final Card c = ControlField.this.view.getTabletop().getCardFromMouseOverPanel();
                if (c != null) {
                    t.getDetailController().showCard(c);
                    t.getPictureController().showCard(c);
                }
            }
        };

        // Battlefield card
        maCardClick = new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                final ViewTopLevel t = ((GuiTopLevel) AllZone.getDisplay()).getController().getMatchController().getView();
                final Card c = t.getDetailController().getCurrentCard();
                final Input input = t.getInputController().getInputControl().getInput();

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

                    final CardList att = new CardList(AllZone.getCombat().getAttackers());
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
                        t.getInputController().getInputControl()
                                .selectCard(c, AllZone.getHumanPlayer().getZone(Zone.Battlefield));
                    }
                }
            }
        };
    } // End initMouseAdapters()
} // End class ControlField
