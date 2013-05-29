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

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import org.apache.commons.lang3.tuple.Pair;

import forge.Card;
import forge.Command;
import forge.FThreads;
import forge.Singletons;
import forge.Constant.Preferences;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.spellability.SpellAbility;
import forge.game.Game;
import forge.game.player.HumanPlay;
import forge.game.player.LobbyPlayer;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.ForgeAction.MatchConstants;
import forge.gui.framework.ICDoc;
import forge.gui.input.Input;
import forge.gui.input.InputPayMana;
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
    private final LobbyPlayer viewer;
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



    private final Runnable updateZonesRunnable = new Runnable() { @Override public void run() { CField.this.view.updateZones(CField.this.player); } };
    private final Runnable updateDetailsRunnable = new Runnable() { @Override public void run() { CField.this.view.updateDetails(CField.this.player); } };
    
    // Life total, poison total, and keywords, attached directly to Player.
    private final Observer observerDetails = new Observer() {
        @Override
        public void update(final Observable a, final Object b) {
            FThreads.invokeInEdtNowOrLater(updateDetailsRunnable);
        }
    };
    // Hand, Graveyard, Library, Flashback, Exile zones, attached to hand.
    private final Observer observerZones = new Observer() {
        @Override
        public void update(final Observable a, final Object b) {
            FThreads.invokeInEdtNowOrLater(updateZonesRunnable);
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
    public CField(final Player p0, final VField v0, LobbyPlayer playerViewer) {
        this.player = p0;
        this.viewer = playerViewer;
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
                // activate cards only via your own flashback button
                if (player.getLobbyPlayer() != CField.this.viewer) {
                    return;
                }
                final Game game = player.getGame();
                // TODO: "can play" check needed!

                // should I check for who owns these cards? Are there any abilities to be played from opponent's graveyard? 
                final SpellAbility ab = player.getController().getAbilityToPlay(game.getAbilitesOfCard(c, player));
                if ( null != ab) {
                    game.getAction().invoke(new Runnable(){ @Override public void run(){
                        HumanPlay.playSpellAbility(player, ab);
                    }});
                }
            }
        };
    }

    private void handClicked() {
        if ( player.getLobbyPlayer() == viewer || Preferences.DEV_MODE || player.hasKeyword("Play with your hand revealed.")) {
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


        if (c == null || !c.isInZone(ZoneType.Battlefield)) {
            return;
        }

        CMessage.SINGLETON_INSTANCE.getInputControl().selectCard(c, e.isMetaDown());
    }

    /** */
    private void manaAction(byte colorCode) {
        if (CField.this.player.getLobbyPlayer() == CField.this.viewer) {
            final Input in = Singletons.getControl().getInputQueue().getInput();
            if (in instanceof InputPayMana) {
                // Do something
                ((InputPayMana) in).selectManaPool(colorCode);
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
    
        for(final Pair<FLabel, Byte> labelPair : this.view.getManaLabels()) {
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

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#getCommandOnSelect()
     */
    @Override
    public Command getCommandOnSelect() {
        return null;
    }
} // End class CField
