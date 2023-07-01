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
import java.awt.Dialog;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JButton;

import forge.game.GameView;
import forge.game.card.CardView;
import forge.gui.FThreads;
import forge.gui.framework.ICDoc;
import forge.gui.framework.SDisplayUtil;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;
import forge.screens.match.CMatchUI;
import forge.screens.match.views.VPrompt;
import forge.toolbox.FSkin;

/**
 * Controls the prompt panel in the match UI.
 *
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 */
public class CPrompt implements ICDoc {
    private final CMatchUI matchUI;
    private final VPrompt view;
    public CPrompt(final CMatchUI matchUI) {
        this.matchUI = matchUI;
        this.view = new VPrompt(this);
    }

    public final CMatchUI getMatchUI() {
        return matchUI;
    }
    public final VPrompt getView() {
        return view;
    }

    private Component lastFocusedButton = null;

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

    private final WindowAdapter focusOKButtonOnDialogClose = new WindowAdapter() {
        @Override
        public void windowClosed(WindowEvent evt) {
            view.getBtnOK().requestFocusInWindow();
        }
    };

    private final PropertyChangeListener focusOnEnable = new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (lastFocusedButton == null || lastFocusedButton == view.getBtnOK()) {
                // Attempt to resolve sporadic button focus issues when dialogs are shown.
                Dialog activeDialog = getActiveDialog(true);
                if (activeDialog != null) {
                    // If this dialog already has our listener, remove it
                    activeDialog.removeWindowListener(focusOKButtonOnDialogClose);
                    activeDialog.addWindowListener(focusOKButtonOnDialogClose);
                }

                // Focus the OK button when it becomes enabled
                boolean isEnabled = (Boolean) evt.getNewValue();
                if (isEnabled) {
                    view.getBtnOK().requestFocusInWindow();
                }
            }
        }
    };

    private final FocusListener onFocus = new FocusAdapter() {
        @Override
        public void focusGained(final FocusEvent e) {
            if (null != view.getParentCell() && view == view.getParentCell().getSelected()) {
                // only record focus changes when we're showing -- otherwise it is due to a tab visibility change
                lastFocusedButton = e.getComponent();
            }
        }
    };

    private void _initButton(final JButton button, final ActionListener onClick) {
        // remove to ensure listeners don't accumulate over many initializations
        button.removeActionListener(onClick);
        button.addActionListener(onClick);
        button.removeFocusListener(onFocus);
        button.addFocusListener(onFocus);
        if (button == view.getBtnOK()) {
            button.removePropertyChangeListener("enabled", focusOnEnable);
            button.addPropertyChangeListener("enabled", focusOnEnable);
        }
    }

    @Override
    public void initialize() {
        _initButton(view.getBtnCancel(), actCancel);
        _initButton(view.getBtnOK(), actOK);
    }

    private static Dialog getActiveDialog(boolean modalOnly)
    {
        Window[] windows = Window.getWindows();
        if (windows != null) {
            for (Window w : windows) {
                if (w.isShowing() && w instanceof Dialog && (!modalOnly || ((Dialog)w).isModal())) {
                    return (Dialog)w;
                }
            }
        }
        return null;
    }

    private void selectButtonOk() {
        matchUI.getGameController().selectButtonOk();
    }

    private void selectButtonCancel() {
        matchUI.getGameController().selectButtonCancel();
    }

    public void setMessage(final String header) {
        view.getTarMessage().setText(FSkin.encodeSymbols(header, false));
        view.setCardView(null);
    }
    public void setMessage(final String s0, final CardView card) {
    	view.getTarMessage().setText(FSkin.encodeSymbols(s0, false));
    	view.setCardView(card);
    }

    /**
     * Invoke a flashing animation on the prompt.
     */
    public void remind() {
        SDisplayUtil.remind(view);
    }

    public void alert() {
        if (FModel.getPreferences().getPrefBoolean(ForgePreferences.FPref.UI_REMIND_ON_PRIORITY)) {
            SDisplayUtil.remind(view, 15, 30);
        }
    }

    @Override
    public void register() {
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
        final GameView game = matchUI.getGameView();
        if (game == null) {
            return;
        }
        final String text = String.format("T:%d G:%d/%d [%s]", game.getTurn(), game.getNumPlayedGamesInMatch() + 1, game.getNumGamesInMatch(), game.getGameType());
        view.getLblGames().setText(text);
        view.getLblGames().setToolTipText(String.format("%s: Game #%d of %d, turn %d", game.getGameType(), game.getNumPlayedGamesInMatch() + 1, game.getNumGamesInMatch(), game.getTurn()));
    }
}
