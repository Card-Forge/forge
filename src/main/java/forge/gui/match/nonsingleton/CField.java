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

import java.awt.Event;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import org.apache.commons.lang3.tuple.Pair;

import forge.Card;
import forge.Command;
import forge.Constant;
import forge.FThreads;
import forge.Constant.Preferences;
import forge.Singletons;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.spellability.SpellAbility;
import forge.control.FControl;
import forge.control.Lobby;
import forge.control.input.Input;
import forge.control.input.InputPayManaBase;
import forge.game.GameState;
import forge.game.player.HumanPlayer;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.ForgeAction.MatchConstants;
import forge.gui.framework.ICDoc;
import forge.gui.match.CMatchUI;
import forge.gui.match.controllers.CMessage;
import forge.gui.toolbox.FLabel;

/**
 * Controls Swing components of a player's field instance.
 */
public class CField implements ICDoc {
    // The one who owns cards on this side of table
    private final Player player;
    // Tho one who looks at screen and 'performs actions'
    private final HumanPlayer playerViewer;
    private final VField view;
    private boolean initializedAlready = false;

    private MouseMotionListener mmlCardOver = new MouseMotionAdapter() { @Override
        public void mouseMoved(final MouseEvent e) {
            cardoverAction(e); } };

    private final MouseListener madHand = new MouseAdapter() { @Override
        public void mousePressed(final MouseEvent e) {
            handClicked(); } };

    private final MouseListener madAvatar = new MouseAdapter() { @Override
        public void mousePressed(final MouseEvent e) {
            CMessage.SINGLETON_INSTANCE.getInputControl().selectPlayer(player); } };

    private final MouseListener madExiled = new MouseAdapter() { @Override
        public void mousePressed(final MouseEvent e) { exileAction.actionPerformed(null); } };

    private final MouseListener madLibrary = new MouseAdapter() { @Override
        public void mousePressed(final MouseEvent e) { if (Preferences.DEV_MODE) libraryAction.actionPerformed(null); } };

    private final MouseListener madGraveyard = new MouseAdapter() { @Override
        public void mousePressed(final MouseEvent e) { graveAction.actionPerformed(null); } };

    private final MouseListener madFlashback = new MouseAdapter() { @Override
        public void mousePressed(final MouseEvent e) { flashBackAction.actionPerformed(null); } };

    private final MouseListener madCardClick = new MouseAdapter() { @Override
        public void mousePressed(final MouseEvent e) { cardclickAction(e); } };


    // Hand, Graveyard, Library, Flashback, Exile zones, attached to hand.
    private final Observer observerZones = new Observer() {
        @Override
        public void update(final Observable a, final Object b) {
            FThreads.invokeInEdtNowOrLater(updateZonesRunnable);
        }
    };
    private final Runnable updateZonesRunnable = new Runnable() { @Override public void run() { CField.this.view.updateZones(CField.this.player); } };
    private final Runnable updateDetailsRunnable = new Runnable() { @Override public void run() { CField.this.view.updateDetails(CField.this.player); } };
    
    // Life total, poison total, and keywords, attached directly to Player.
    private final Observer observerDetails = new Observer() {
        @Override
        public void update(final Observable a, final Object b) {
            FThreads.invokeInEdtNowOrLater(updateDetailsRunnable);
        }
    };

    // Card play area, attached to battlefield zone.
    private final Observer observerPlay = new Observer() {
        @Override
        public void update(final Observable a, final Object b) {
            //FThreads.checkEDT("observerPlay.update", true);
            CField.this.view.getTabletop().setupPlayZone();
        }
    };

    private final ZoneAction handAction;
    private final ZoneAction libraryAction;
    private final ZoneAction exileAction;
    private final ZoneAction graveAction;
    private final ZoneAction flashBackAction;

    /**
     * Controls Swing components of a player's field instance.
     * 
     * @param p0 &emsp; {@link forge.game.player.Player}
     * @param v0 &emsp; {@link forge.gui.match.nonsingleton.VField}
     * @param playerViewer 
     */
    @SuppressWarnings("serial")
    public CField(final Player p0, final VField v0, HumanPlayer playerViewer) {
        this.player = p0;
        this.playerViewer = playerViewer;
        this.view = v0;

        handAction = new ZoneAction(player.getZone(ZoneType.Hand), MatchConstants.HUMANHAND);
        libraryAction = new ZoneAction(player.getZone(ZoneType.Library), MatchConstants.HUMANLIBRARY);
        exileAction = new ZoneAction(player.getZone(ZoneType.Exile), MatchConstants.HUMANEXILED);
        graveAction = new ZoneAction(player.getZone(ZoneType.Graveyard), MatchConstants.HUMANGRAVEYARD);
        flashBackAction = new ZoneAction(player.getZone(ZoneType.Graveyard), MatchConstants.HUMANFLASHBACK) {
            @Override
            protected List<Card> getCardsAsIterable() {
                return CardFactoryUtil.getExternalZoneActivationCards(player);
            }

            @Override
            protected void doAction(final Card c) {
                final GameState game = player.getGame();
                // should I check for who owns these cards? Are there any abilities to be played from opponent's graveyard? 
                final SpellAbility ab = CField.this.playerViewer.getController().getAbilityToPlay(game.getAbilitesOfCard(c, CField.this.playerViewer));
                if ( null != ab) {
                    FThreads.invokeInNewThread(new Runnable(){ @Override public void run(){
                        CField.this.playerViewer.playSpellAbility(c, ab);
                    }});
                }
            }
        };
    }

    private void handClicked() {
        if ( player == playerViewer || Preferences.DEV_MODE || player.hasKeyword("Play with your hand revealed.")) {
            handAction.actionPerformed(null);
        }
    }

    /** */
    private void cardoverAction(MouseEvent e) {
        final Card c = CField.this.view.getTabletop().getHoveredCard(e);
        if (c != null) {
            CMatchUI.SINGLETON_INSTANCE.setCard(c, e.isShiftDown());
        }
    }

    /** */
    private void cardclickAction(final MouseEvent e) {
        // original version:
        // final Card c = t.getDetailController().getCurrentCard();
        // Roujin's bug fix version dated 2-12-2012
        final Card c = CField.this.view.getTabletop().getHoveredCard(e);

        final Input input = CMessage.SINGLETON_INSTANCE.getInputControl().getInput();

        if (c == null || !c.isInZone(ZoneType.Battlefield)) {
            return;
        }

        //Yosei, the Morning Star required cards to be chosen on computer side
        //earlier it was enforced that cards must be in player zone
        //this can potentially break some other functionality
        //(tapping lands works ok but some custom cards may not...)
        if ( input != null ){
            input.selectCard(c, e.isMetaDown());
        }

    }

    /** */
    private void manaAction(String constantColor) {
        if (CField.this.player == CField.this.playerViewer) {
            final Input in = Singletons.getModel().getMatch().getInput().getInput();
            if (in instanceof InputPayManaBase) {
                // Do something
                ((InputPayManaBase) in).selectManaPool(constantColor);
            }
        }
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
    
        if (Preferences.DEV_MODE) {
            ((FLabel) this.view.getLblLibrary()).setHoverable(true);
        }
        this.view.getLblLibrary().addMouseListener(madLibrary);
    
        ((FLabel) this.view.getLblHand()).setHoverable(true);
        this.view.getLblHand().addMouseListener(madHand);
    
        ((FLabel) this.view.getLblFlashback()).setHoverable(true);
        this.view.getLblFlashback().addMouseListener(madFlashback);
    
        for(final Pair<FLabel, String> labelPair : this.view.getManaLabels()) {
            labelPair.getLeft().setHoverable(true);
            labelPair.getLeft().addMouseListener(new MouseAdapter() { @Override
                public void mousePressed(final MouseEvent e) {
                manaAction(labelPair.getRight()); } }
            );
        }
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

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#getCommandOnSelect()
     */
    @Override
    public Command getCommandOnSelect() {
        return null;
    }
} // End class CField
