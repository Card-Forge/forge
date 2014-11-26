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
package forge.screens.match.controllers;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.List;

import javax.swing.JButton;

import forge.FThreads;
import forge.UiCommand;
import forge.game.GameView;
import forge.game.card.CardView;
import forge.game.player.PlayerView;
import forge.game.spellability.SpellAbility;
import forge.gui.framework.ICDoc;
import forge.gui.framework.SDisplayUtil;
import forge.match.MatchUtil;
import forge.screens.match.views.VPrompt;
import forge.toolbox.FSkin;
import forge.util.ITriggerEvent;

/**
 * Controls the prompt panel in the match UI.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 */
public enum CPrompt implements ICDoc {
    SINGLETON_INSTANCE;

    private Component lastFocusedButton = null;
    private final VPrompt view = VPrompt.SINGLETON_INSTANCE;

    private final ActionListener actCancel = new ActionListener() {
        @Override
        public void actionPerformed(final ActionEvent evt) {
            selectButtonCancel();
        }
    };
    private final ActionListener actOK = new ActionListener() {
        @Override
        public void actionPerformed(final ActionEvent evt) {
            selectButtonOk();
        }
    };

    private final FocusListener onFocus = new FocusAdapter() {
        @Override
        public void focusGained(FocusEvent e) {
            if (null != view.getParentCell() && view == view.getParentCell().getSelected()) {
                // only record focus changes when we're showing -- otherwise it is due to a tab visibility change
                lastFocusedButton = e.getComponent();
            }
        }
    };

    private void _initButton(JButton button, ActionListener onClick) {
        // remove to ensure listeners don't accumulate over many initializations
        button.removeActionListener(onClick);
        button.addActionListener(onClick);
        button.removeFocusListener(onFocus);
        button.addFocusListener(onFocus);
    }

    @Override
    public void initialize() {
        _initButton(view.getBtnCancel(), actCancel);
        _initButton(view.getBtnOK(), actOK);
    }

    public void selectButtonOk() {
        MatchUtil.getHumanController().selectButtonOk();
    }

    public void selectButtonCancel() {
        MatchUtil.getHumanController().selectButtonCancel();
    }

    public boolean passPriority() {
        return MatchUtil.getHumanController().passPriority();
    }

    public boolean passPriorityUntilEndOfTurn() {
        return MatchUtil.getHumanController().passPriorityUntilEndOfTurn();
    }

    public void selectPlayer(final PlayerView playerView, final ITriggerEvent triggerEvent) {
        MatchUtil.getHumanController().selectPlayer(playerView, triggerEvent);
    }

    public void selectCard(final CardView cardView, final List<CardView> otherCardViewsToSelect, final ITriggerEvent triggerEvent) {
        MatchUtil.getHumanController().selectCard(cardView, otherCardViewsToSelect, triggerEvent);
    }

    public void selectAbility(final SpellAbility sa) {
        MatchUtil.getHumanController().selectAbility(sa);
    }

    public void setMessage(String s0) {
        view.getTarMessage().setText(FSkin.encodeSymbols(s0, false));
    }

    /** Flashes animation on input panel if play is currently waiting on input. */
    public void remind() {
        SDisplayUtil.remind(view);
    }

    @Override
    public UiCommand getCommandOnSelect() {
        return null;
    }

    @Override
    public void update() {
        // set focus back to button that last had it
        if (null != lastFocusedButton) {
            lastFocusedButton.requestFocusInWindow();
        }
    }

    public void updateText() {
        FThreads.assertExecutedByEdt(true);
        final GameView game = MatchUtil.getGameView();
        final String text = String.format("T:%d G:%d/%d [%s]", game.getTurn(), game.getNumPlayedGamesInMatch() + 1, game.getNumGamesInMatch(), game.getGameType());
        view.getLblGames().setText(text);
        view.getLblGames().setToolTipText(String.format("%s: Game #%d of %d, turn %d", game.getGameType(), game.getNumPlayedGamesInMatch() + 1, game.getNumGamesInMatch(), game.getTurn()));
    }
}
