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
package forge.gui.match.controllers;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import forge.AllZone;
import forge.Card;
import forge.CardPredicates.Presets;
import forge.Command;
import forge.Constant;
import forge.Singletons;
import forge.deck.Deck;
import forge.game.phase.CombatUtil;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.ForgeAction;
import forge.gui.SOverlayUtils;
import forge.gui.framework.ICDoc;
import forge.gui.framework.SLayoutIO;
import forge.gui.match.views.VDock;
import forge.gui.toolbox.FOverlay;
import forge.gui.toolbox.SaveOpenDialog;
import forge.gui.toolbox.SaveOpenDialog.Filetypes;
import forge.item.CardPrinted;
import forge.properties.NewConstants;
import forge.view.FView;

/**
 * Controls the dock panel in the match UI.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 */
public enum CDock implements ICDoc {
    /** */
    SINGLETON_INSTANCE;

    /** Concede game, bring up WinLose UI. */
    public void concede() {
        if (FOverlay.SINGLETON_INSTANCE.getPanel().isShowing()) {
            return;
        }

        AllZone.getHumanPlayer().concede();
        Singletons.getModel().getGameAction().checkStateEffects();
    }

    /**
     * End turn.
     */
    public void endTurn() {
        Singletons.getModel().getGameState().getPhaseHandler().autoPassToCleanup();
    }

    private void revertLayout() {
        SOverlayUtils.genericOverlay();
        FView.SINGLETON_INSTANCE.getPnlContent().removeAll();

        final SwingWorker<Void, Void> w = new SwingWorker<Void, Void>() {
            @Override
            public Void doInBackground() {
                SLayoutIO.loadLayout(null);
                SOverlayUtils.hideOverlay();
                return null;
            }
        };
        w.execute();
    }

    private void saveLayout() {
        final SwingWorker<Void, Void> w = new SwingWorker<Void, Void>() {
            @Override
            public Void doInBackground() {
                final SaveOpenDialog dlgSave = new SaveOpenDialog();
                final File defFile = new File(SLayoutIO.getFilePreferred());
                final File saveFile = dlgSave.SaveDialog(defFile, Filetypes.LAYOUT);
                if (saveFile != null) {
                    SLayoutIO.saveLayout(saveFile);
                }
                return null;
            }
        };
        w.execute();
    }

    private void openLayout() {
        SOverlayUtils.genericOverlay();
        FView.SINGLETON_INSTANCE.getPnlContent().removeAll();

        final SwingWorker<Void, Void> w = new SwingWorker<Void, Void>() {
            @Override
            public Void doInBackground() {
                final SaveOpenDialog dlgOpen = new SaveOpenDialog();
                final File defFile = new File(SLayoutIO.getFilePreferred());
                final File loadFile = dlgOpen.OpenDialog(defFile, Filetypes.LAYOUT);

                if (loadFile != null) {
                    SLayoutIO.loadLayout(loadFile);
                    SLayoutIO.saveLayout(null);
                }

                SOverlayUtils.hideOverlay();
                return null;
            }
        };
        w.execute();
    }

    /**
     * View deck list.
     */
    private void viewDeckList() {
        new DeckListAction(NewConstants.Lang.GuiDisplay.HUMAN_DECKLIST).actionPerformed(null);
    }
    
    /** Attack with everyone */
    public void alphaStrike() {
        PhaseHandler ph = Singletons.getModel().getGameState().getPhaseHandler();
        
        Player human = AllZone.getHumanPlayer();
        
        if (ph.is(PhaseType.COMBAT_DECLARE_ATTACKERS, human)) {
            for(Card c : human.getCardsIn(ZoneType.Battlefield).filter(Presets.CREATURES)) {
                if (!c.isAttacking() && CombatUtil.canAttack(c, AllZone.getCombat())) {
                    AllZone.getCombat().addAttacker(c);
                }
            }
            //human.updateObservers();
            // TODO Is this redrawing immediately?
            FView.SINGLETON_INSTANCE.getFrame().repaint();
        }
    }

    /**
     * Receives click and programmatic requests for viewing a player's library
     * (typically used in dev mode). Allows copy of the cardlist to clipboard.
     * 
     */
    private class DeckListAction extends ForgeAction {
        public DeckListAction(final String property) {
            super(property);
        }

        private static final long serialVersionUID = 9874492387239847L;

        @Override
        public void actionPerformed(final ActionEvent e) {
            Deck targetDeck;

            if (!Constant.Runtime.HUMAN_DECK[0].getMain().isEmpty()) {
                targetDeck = Constant.Runtime.HUMAN_DECK[0];
            } else if (!Constant.Runtime.COMPUTER_DECK[0].getMain().isEmpty()) {
                targetDeck = Constant.Runtime.COMPUTER_DECK[0];
            } else {
                return;
            }

            final HashMap<String, Integer> deckMap = new HashMap<String, Integer>();

            for (final Entry<CardPrinted, Integer> s : targetDeck.getMain()) {
                deckMap.put(s.getKey().getName(), s.getValue());
            }

            final String nl = System.getProperty("line.separator");
            final StringBuilder deckList = new StringBuilder();
            String dName = targetDeck.getName();

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
    } // End DeckListAction

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#getCommandOnSelect()
     */
    @Override
    public Command getCommandOnSelect() {
        return null;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#initialize()
     */
    @Override
    public void initialize() {
        VDock.SINGLETON_INSTANCE.getBtnConcede()
            .addMouseListener(new MouseAdapter() { @Override
                public void mousePressed(final MouseEvent e) {
                    concede(); } });

        VDock.SINGLETON_INSTANCE.getBtnSettings()
            .addMouseListener(new MouseAdapter() { @Override
                public void mousePressed(final MouseEvent e) {
                    SOverlayUtils.showOverlay(); } });

        VDock.SINGLETON_INSTANCE.getBtnEndTurn()
            .addMouseListener(new MouseAdapter() { @Override
                public void mousePressed(final MouseEvent e) {
                    endTurn(); } });

        VDock.SINGLETON_INSTANCE.getBtnViewDeckList()
            .addMouseListener(new MouseAdapter() { @Override
                public void mousePressed(final MouseEvent e) {
                    viewDeckList(); } });

        VDock.SINGLETON_INSTANCE.getBtnRevertLayout()
        .addMouseListener(new MouseAdapter() { @Override
            public void mousePressed(final MouseEvent e) {
                revertLayout(); } });

        VDock.SINGLETON_INSTANCE.getBtnOpenLayout()
        .addMouseListener(new MouseAdapter() { @Override
            public void mousePressed(final MouseEvent e) {
                openLayout(); } });

        VDock.SINGLETON_INSTANCE.getBtnSaveLayout()
        .addMouseListener(new MouseAdapter() { @Override
            public void mousePressed(final MouseEvent e) {
                saveLayout(); } });
        
        VDock.SINGLETON_INSTANCE.getBtnAlphaStrike()
        .addMouseListener(new MouseAdapter() { @Override
            public void mousePressed(final MouseEvent e) {
                alphaStrike(); } });
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#update()
     */
    @Override
    public void update() {
    }
}
