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
package forge.gui.match.nonsingleton;

import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import forge.Card;

import forge.Command;
import forge.Constant;
import forge.Constant.Preferences;
import forge.Singletons;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.spellability.SpellAbility;
import forge.control.input.Input;
import forge.control.input.InputAttack;
import forge.control.input.InputBlock;
import forge.control.input.InputPayMana;
import forge.control.input.InputPayManaCost;
import forge.control.input.InputPayManaCostAbility;
import forge.control.input.InputPaySacCost;
import forge.game.GameState;
import forge.game.phase.CombatUtil;
import forge.game.player.Player;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;
import forge.gui.ForgeAction;
import forge.gui.GuiChoose;
import forge.gui.GuiDisplayUtil;
import forge.gui.framework.ICDoc;
import forge.gui.match.CMatchUI;
import forge.gui.match.controllers.CMessage;
import forge.gui.toolbox.FLabel;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;
import forge.properties.NewConstants.Lang.GuiDisplay.ComputerHand;
import forge.properties.NewConstants.Lang.GuiDisplay.ComputerLibrary;
import forge.properties.NewConstants.Lang.GuiDisplay.HumanHand;
import forge.properties.NewConstants.Lang.GuiDisplay.HumanLibrary;

/**
 * Controls Swing components of a player's field instance.
 */
public class CField implements ICDoc {
    private final Player player;
    private final VField view;
    private boolean initializedAlready = false;

    private MouseMotionListener mmlCardOver = new MouseMotionAdapter() { @Override
        public void mouseMoved(final MouseEvent e) {
            cardoverAction(); } };

    private final MouseListener madHand = new MouseAdapter() { @Override
        public void mousePressed(final MouseEvent e) {
            handAction(); } };

    private final MouseListener madAvatar = new MouseAdapter() { @Override
        public void mousePressed(final MouseEvent e) {
            avatarAction(); } };

    private final MouseListener madExiled = new MouseAdapter() { @Override
        public void mousePressed(final MouseEvent e) {
            exiledAction(); } };

    private final MouseListener madLibrary = new MouseAdapter() { @Override
        public void mousePressed(final MouseEvent e) {
            libraryAction(); } };

    private final MouseListener madGraveyard = new MouseAdapter() { @Override
        public void mousePressed(final MouseEvent e) {
            graveyardAction(); } };

    private final MouseListener madFlashback = new MouseAdapter() { @Override
        public void mousePressed(final MouseEvent e) {
            flashbackAction(); } };

    private final MouseListener madCardClick = new MouseAdapter() { @Override
        public void mousePressed(final MouseEvent e) {
            cardclickAction(e); } };

    private final MouseListener madBlack = new MouseAdapter() { @Override
        public void mousePressed(final MouseEvent e) {
            manaAction(Constant.Color.BLACK); } };

    private final MouseListener madBlue = new MouseAdapter() { @Override
        public void mousePressed(final MouseEvent e) {
            manaAction(Constant.Color.BLUE); } };

    private final MouseListener madGreen = new MouseAdapter() { @Override
        public void mousePressed(final MouseEvent e) {
            manaAction(Constant.Color.GREEN); } };

    private final MouseListener madRed = new MouseAdapter() { @Override
        public void mousePressed(final MouseEvent e) {
            manaAction(Constant.Color.RED); } };

    private final MouseListener madWhite = new MouseAdapter() { @Override
        public void mousePressed(final MouseEvent e) {
            manaAction(Constant.Color.WHITE); } };

    private final MouseListener madColorless = new MouseAdapter() { @Override
        public void mousePressed(final MouseEvent e) {
            manaAction(Constant.Color.COLORLESS); } };

    // Hand, Graveyard, Library, Flashback, Exile zones, attached to hand.
    private final Observer observerZones = new Observer() {
        @Override
        public void update(final Observable a, final Object b) {
            CField.this.view.updateZones(CField.this.player);
        }
    };

    // Life total, poison total, and keywords, attached directly to Player.
    private final Observer observerDetails = new Observer() {
        @Override
        public void update(final Observable a, final Object b) {
            CField.this.view.updateDetails(CField.this.player);
        }
    };

    // Card play area, attached to battlefield zone.
    private final Observer observerPlay = new Observer() {
        @Override
        public void update(final Observable a, final Object b) {
            final PlayerZone pZone = (PlayerZone) a;
            GuiDisplayUtil.setupPlayZone(CField.this.view.getTabletop(), pZone.getCards(false));
        }
    };

    /**
     * Controls Swing components of a player's field instance.
     * 
     * @param p0 &emsp; {@link forge.game.player.Player}
     * @param v0 &emsp; {@link forge.gui.match.nonsingleton.VField}
     */
    public CField(final Player p0, final VField v0) {
        this.player = p0;
        this.view = v0;
    }

    @Override
    public void initialize() {
        if (initializedAlready) { return; }
        initializedAlready = true;

        // Observers
        CField.this.player.getZone(ZoneType.Hand).addObserver(observerZones);
        CField.this.player.getZone(ZoneType.Battlefield).addObserver(observerPlay);
        CField.this.player.addObserver(observerDetails);

        // Listeners
        // Battlefield card clicks
        this.view.getTabletop().addMouseListener(madCardClick);

        // Battlefield card mouseover
        this.view.getTabletop().addMouseMotionListener(mmlCardOver);

        // Player select
        this.view.getAvatarArea().addMouseListener(madAvatar);

        // Detail label listeners
        ((FLabel) this.view.getLblGraveyard()).setHoverable(true);
        this.view.getLblGraveyard().addMouseListener(madGraveyard);

        ((FLabel) this.view.getLblExile()).setHoverable(true);
        this.view.getLblExile().addMouseListener(madExiled);

        this.view.getLblLibrary().addMouseListener(madLibrary);
        this.view.getLblHand().addMouseListener(madHand);

        ((FLabel) this.view.getLblFlashback()).setHoverable(true);
        this.view.getLblFlashback().addMouseListener(madFlashback);

        ((FLabel) this.view.getLblBlack()).setHoverable(true);
        this.view.getLblBlack().addMouseListener(madBlack);

        ((FLabel) this.view.getLblBlue()).setHoverable(true);
        this.view.getLblBlue().addMouseListener(madBlue);

        ((FLabel) this.view.getLblGreen()).setHoverable(true);
        this.view.getLblGreen().addMouseListener(madGreen);

        ((FLabel) this.view.getLblRed()).setHoverable(true);
        this.view.getLblRed().removeMouseListener(madRed);
        this.view.getLblRed().addMouseListener(madRed);

        ((FLabel) this.view.getLblWhite()).setHoverable(true);
        this.view.getLblWhite().removeMouseListener(madWhite);
        this.view.getLblWhite().addMouseListener(madWhite);

        ((FLabel) this.view.getLblColorless()).setHoverable(true);
        this.view.getLblColorless().addMouseListener(madColorless);
    }

    @Override
    public void update() {
    }

    /** @return {@link forge.game.player.Player} */
    public Player getPlayer() {
        return this.player;
    }

    /** @return {@link forge.gui.nonsingleton.VField} */
    public VField getView() {
        return this.view;
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
            final List<Card> choices = this.getCardsAsIterable();

            final ArrayList<Card> choices2 = new ArrayList<Card>();

            if (choices.isEmpty()) {
                GuiChoose.oneOrNone(this.title, new String[] { "no cards" });
            } else {
                for (int i = 0; i < choices.size(); i++) {
                    final Card crd = choices.get(i);
                    if (crd.isFaceDown()) {
                        if ((crd.getController().isComputer() && !crd.hasKeyword("Your opponent may look at this card."))
                                || !crd.hasKeyword("You may look at this card.")) {
                            final Card faceDown = new Card();
                            faceDown.setName("Face Down");
                            choices2.add(faceDown);
                        } else {
                            final Card faceDown = Singletons.getModel().getCardFactory().copyCard(crd);
                            faceDown.turnFaceUp();
                            choices2.add(faceDown);
                        }
                    } else {
                        choices2.add(crd);
                    }
                }
                final Card choice = GuiChoose.oneOrNone(this.title, choices2);
                if (choice != null) {
                    this.doAction(choice);
                }
            }
        }

        protected List<Card> getCardsAsIterable() {
            return this.zone.getCards();
        }

        protected void doAction(final Card c) {
        }
    } // End ZoneAction

    /** */
    private void handAction() {
        if (!CField.this.player.isComputer()) {
            new ZoneAction(CField.this.player.getZone(ZoneType.Hand), HumanHand.BASE)
            .actionPerformed(null);
        } else if (Preferences.DEV_MODE
                || CField.this.player.hasKeyword("Play with your hand revealed.")) {
            new ZoneAction(CField.this.player.getZone(ZoneType.Hand), ComputerHand.BASE)
            .actionPerformed(null);
        }
    }

    /** */
    private void flashbackAction() {
        if (!CField.this.player.isHuman()) {
            new ZoneAction(player.getZone(ZoneType.Graveyard), NewConstants.Lang.GuiDisplay.COMPUTER_FLASHBACK) {

                private static final long serialVersionUID = 8120331222693706164L;

                @Override
                protected List<Card> getCardsAsIterable() {
                    return CardFactoryUtil.getExternalZoneActivationCards(player);
                }

                @Override
                protected void doAction(final Card c) {
                    // you cannot play computer's card from graveyard
                }
            } .actionPerformed(null);
        }
        else {
            new ZoneAction(CField.this.player.getZone(ZoneType.Graveyard), NewConstants.Lang.GuiDisplay.HUMAN_FLASHBACK) {

                private static final long serialVersionUID = 8120331222693706164L;

                @Override
                protected List<Card> getCardsAsIterable() {
                    return CardFactoryUtil.getExternalZoneActivationCards(player);
                }

                @Override
                protected void doAction(final Card c) {
                    GameState game = Singletons.getModel().getGame();
                    SpellAbility ab = player.getController().getAbilityToPlay(game.getAbilitesOfCard(c, player));
                    if ( null != ab) {
                        player.playSpellAbility(c, ab);
                        Singletons.getModel().getGame().getPhaseHandler().setPriority(player);
                    }
                }
            } .actionPerformed(null);
        }
    }

    /** */
    private void libraryAction() {
        if (!Preferences.DEV_MODE) { return; }

        if (!CField.this.player.isComputer()) {
            new ZoneAction(CField.this.player.getZone(ZoneType.Library), HumanLibrary.BASE)
            .actionPerformed(null);
        } else {
            new ZoneAction(CField.this.player.getZone(ZoneType.Library), ComputerLibrary.BASE)
            .actionPerformed(null);
        }
    }

    /** */
    private void exiledAction() {
        if (CField.this.player.isComputer()) {
            new ZoneAction(CField.this.player.getZone(ZoneType.Exile),
                    NewConstants.Lang.GuiDisplay.COMPUTER_EXILED).actionPerformed(null);
        } else {
            new ZoneAction(CField.this.player.getZone(ZoneType.Exile),
                    NewConstants.Lang.GuiDisplay.HUMAN_EXILED).actionPerformed(null);
        }
    }

    private void graveyardAction() {
        if (CField.this.player.isComputer()) {
            new ZoneAction(CField.this.player.getZone(ZoneType.Graveyard),
                    NewConstants.Lang.GuiDisplay.COMPUTER_GRAVEYARD).actionPerformed(null);
        } else {
            new ZoneAction(CField.this.player.getZone(ZoneType.Graveyard),
                    NewConstants.Lang.GuiDisplay.HUMAN_GRAVEYARD).actionPerformed(null);
        }
    }

    private void avatarAction() {
        CMessage.SINGLETON_INSTANCE.getInputControl().selectPlayer(player);
    }

    /** */
    private void cardoverAction() {
        final Card c = CField.this.view.getTabletop().getCardFromMouseOverPanel();
        if (c != null) {
            CMatchUI.SINGLETON_INSTANCE.setCard(c);
        }
    }

    /** */
    private void cardclickAction(final MouseEvent e) {
        // original version:
        // final Card c = t.getDetailController().getCurrentCard();
        // Roujin's bug fix version dated 2-12-2012
        final Card c = CField.this.view.getTabletop().getCardFromMouseOverPanel();

        final Input input = CMessage.SINGLETON_INSTANCE.getInputControl().getInput();

        if (c != null && c.isInZone(ZoneType.Battlefield)) {
            if (c.isTapped()
                    && ((input instanceof InputPayManaCost) || (input instanceof InputPayManaCostAbility))) {
                final forge.view.arcane.CardPanel cardPanel = CField.this.view.getTabletop().getCardPanel(
                        c.getUniqueNumber());
                for (final forge.view.arcane.CardPanel cp : cardPanel.getAttachedPanels()) {
                    if (cp.getCard().isUntapped()) {
                        break;
                    }
                }
            }

            final List<Card> att = Singletons.getModel().getGame().getCombat().getAttackerList();
            if ((c.isTapped() || c.hasSickness() || ((c.hasKeyword("Vigilance")) && att.contains(c)))
                    && (input instanceof InputAttack)) {
                final forge.view.arcane.CardPanel cardPanel = CField.this.view.getTabletop().getCardPanel(
                        c.getUniqueNumber());
                for (final forge.view.arcane.CardPanel cp : cardPanel.getAttachedPanels()) {
                    if (cp.getCard().isUntapped() && !cp.getCard().hasSickness()) {
                        break;
                    }
                }
            }

            if (e.isMetaDown()) {
                if (att.contains(c) && (input instanceof InputAttack)
                        && !c.hasKeyword("CARDNAME attacks each turn if able.")) {
                    c.untap();
                    Singletons.getModel().getGame().getCombat().removeFromCombat(c);
                    CombatUtil.showCombat();
                } else if (input instanceof InputBlock) {
                    if (c.getController().isHuman()) {
                        Singletons.getModel().getGame().getCombat().removeFromCombat(c);
                    }
                    ((InputBlock) input).removeFromAllBlocking(c);
                    CombatUtil.showCombat();
                }
                else if (input instanceof InputPaySacCost) {
                    ((InputPaySacCost) input).unselectCard(c, Singletons.getControl().getPlayer().getZone(ZoneType.Battlefield));
                }
            } else {
                //Yosei, the Morning Star required cards to be chosen on computer side
                //earlier it was enforced that cards must be in player zone
                //this can potentially break some other functionality
                //(tapping lands works ok but some custom cards may not...)


                //in weird case card has no controller revert to default behaviour
                input.selectCard(c);
            }
        }
    }

    /** */
    private void manaAction(String constantColor) {
        if (CField.this.player.isComputer()) {
            System.out.println("Stop trying to spend the AI's mana");
            // TODO: Mindslaver might need to add changes here
        } else {
            final Input in = Singletons.getModel().getMatch().getInput().getInput();
            if (in instanceof InputPayMana) {
                // Do something
                ((InputPayMana) in).selectManaPool(constantColor);
            }
        }
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#getCommandOnSelect()
     */
    @Override
    public Command getCommandOnSelect() {
        return null;
    }
} // End class CField
