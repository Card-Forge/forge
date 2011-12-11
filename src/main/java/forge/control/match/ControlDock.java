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

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JOptionPane;

import org.apache.commons.lang3.StringUtils;

import forge.AllZone;
import forge.Constant;
import forge.Singletons;
import forge.deck.Deck;
import forge.gui.ForgeAction;
import forge.item.CardPrinted;
import forge.properties.ForgePreferences;
import forge.properties.NewConstants;
import forge.view.match.ViewDock;
import forge.view.match.ViewDock.KeyboardShortcutField;

/**
 * Child controller, handles dock button operations.
 * 
 */
public class ControlDock {
    private final ViewDock view;

    /**
     * Child controller, handles dock button operations.
     * 
     * @param v
     *            &emsp; ViewDock obj
     */
    public ControlDock(final ViewDock v) {
        this.view = v;
    }

    /** Concede game, bring up WinLose UI. */
    public void concede() {
        AllZone.getHumanPlayer().concede();
        AllZone.getGameAction().checkStateEffects();
    }

    /**
     * Gets the view.
     * 
     * @return ViewDock
     */
    public ViewDock getView() {
        return this.view;
    }

    /** Updates and saves ForgePreferences with current shortcuts. */
    public void saveKeyboardShortcuts() {
        final ForgePreferences fp = Singletons.getModel().getPreferences();
        final Map<String, KeyboardShortcutField> shortcuts = this.view.getKeyboardShortcutFields();

        fp.setShowStackShortcut(shortcuts.get("showstack").getCodeString());
        fp.setShowCombatShortcut(shortcuts.get("showcombat").getCodeString());
        fp.setShowPlayersShortcut(shortcuts.get("showplayers").getCodeString());
        fp.setShowConsoleShortcut(shortcuts.get("showconsole").getCodeString());
        fp.setShowDevShortcut(shortcuts.get("showdev").getCodeString());
        fp.setConcedeShortcut(shortcuts.get("concede").getCodeString());
        fp.setShowPictureShortcut(shortcuts.get("showpicture").getCodeString());
        fp.setShowDetailShortcut(shortcuts.get("showdetail").getCodeString());

        try {
            fp.save();
        } catch (final Exception e) {
            e.printStackTrace();
        }

        AllZone.getOverlay().hideOverlay();
    }

    /**
     * - Adds keycode to list stored in name of a text field. - Code is not
     * added if already in list. - Backspace removes last code in list. - Sets
     * text of text field with character equivalent of keycodes.
     * 
     * @param e
     *            &emsp; KeyEvent
     */
    public void addKeyCode(final KeyEvent e) {
        final KeyboardShortcutField ksf = (KeyboardShortcutField) e.getSource();
        final String newCode = Integer.toString(e.getKeyCode());
        final String codestring = ksf.getCodeString();
        List<String> existingCodes;

        if (codestring != null) {
            existingCodes = new ArrayList<String>(Arrays.asList(codestring.split(" ")));
        } else {
            existingCodes = new ArrayList<String>();
        }

        // Backspace (8) will remove last code from list.
        if (e.getKeyCode() == 8) {
            existingCodes.remove(existingCodes.size() - 1);
        } else if (!existingCodes.contains(newCode)) {
            existingCodes.add(newCode);
        }

        ksf.setCodeString(StringUtils.join(existingCodes, ' '));
    }

    /**
     * End turn.
     */
    public void endTurn() {
        // Big thanks to you, Gameplay Guru, since I was too lazy to figure this
        // out before release. Doublestrike 24-11-11
        System.err.println("forge.control.match > ControlDock > endTurn()");
        System.out.println("Should skip to the end of turn, or entire turn.");
        System.err.println("If some gameplay guru could implement this, that would be great...");
    }

    /**
     * View deck list.
     */
    public void viewDeckList() {
        new DeckListAction(NewConstants.Lang.GuiDisplay.HUMAN_DECKLIST).actionPerformed(null);
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

            if (Constant.Runtime.HUMAN_DECK[0].countMain() > 1) {
                targetDeck = Constant.Runtime.HUMAN_DECK[0];
            } else if (Constant.Runtime.COMPUTER_DECK[0].countMain() > 1) {
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
}
