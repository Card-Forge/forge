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

import java.util.Iterator;

import com.google.common.collect.Iterators;
import com.google.common.primitives.Ints;

import forge.Singletons;
import forge.gui.SOverlayUtils;
import forge.gui.UiCommand;
import forge.gui.framework.ICDoc;
import forge.gui.framework.SLayoutIO;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.localinstance.skin.FSkinProp;
import forge.model.FModel;
import forge.screens.match.CMatchUI;
import forge.screens.match.views.VDock;
import forge.toolbox.FSkin;
import forge.util.Localizer;

/**
 * Controls the dock panel in the match UI.
 *
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 */
public class CDock implements ICDoc {

    private ArcState arcState;
    private final Iterator<ArcState> arcStateIterator = Iterators.cycle(ArcState.values());

    private final CMatchUI matchUI;
    private final VDock view;
    public CDock(final CMatchUI matchUI) {
        this.matchUI = matchUI;
        this.view = new VDock(this);
    }

    public enum ArcState { OFF, MOUSEOVER, ON}

    public VDock getView() {
        return view;
    }

    /**
     * End turn.
     */
    public void endTurn() {
        matchUI.getGameController().passPriorityUntilEndOfTurn();
    }

    /**
     * @return int State of targeting arc preference:<br>
     * 0 = don't draw<br>
     * 1 = draw on card mouseover<br>
     * 2 = always draw
     */
    public ArcState getArcState() {
        return arcState;
    }

    /** @param state0 int */
    private void refreshArcStateDisplay() {
        switch (arcState) {
        case OFF:
            view.getBtnTargeting().setToolTipText(Localizer.getInstance().getMessage("lblTargetingArcsOff"));
            view.getBtnTargeting().setIcon(FSkin.getIcon(FSkinProp.ICO_ARCSOFF));
            view.getBtnTargeting().repaintSelf();
            break;
        case MOUSEOVER:
            view.getBtnTargeting().setToolTipText(Localizer.getInstance().getMessage("lblTargetingArcsCardMouseover"));
            view.getBtnTargeting().setIcon(FSkin.getIcon(FSkinProp.ICO_ARCSHOVER));
            view.getBtnTargeting().repaintSelf();
            break;
        case ON:
            view.getBtnTargeting().setToolTipText(Localizer.getInstance().getMessage("lblTargetingArcsAlwaysOn"));
            view.getBtnTargeting().setIcon(FSkin.getIcon(FSkinProp.ICO_ARCSON));
            view.getBtnTargeting().repaintSelf();
            break;
        }

        FModel.getPreferences().setPref(FPref.UI_TARGETING_OVERLAY, String.valueOf(arcState.ordinal()));
        FModel.getPreferences().save();
    }

    /** Toggle targeting overlay painting. */
    public void toggleTargeting() {
        arcState = arcStateIterator.next();

        refreshArcStateDisplay();
        Singletons.getView().getFrame().repaint(); // repaint the match UI
    }

    public void setArcState(final ArcState state) {
        arcState = state;
        while (arcStateIterator.next() != arcState) { /* Put the iterator to the correct value */ }
    }

    @Override
    public void register() {
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#initialize()
     */
    @SuppressWarnings("serial")
    @Override
    public void initialize() {
        final String temp = FModel.getPreferences()
                .getPref(FPref.UI_TARGETING_OVERLAY);
        final Integer arcState = Ints.tryParse(temp);
        setArcState(ArcState.values()[arcState == null ? 0 : arcState]);
        refreshArcStateDisplay();

        view.getBtnConcede().setCommand((UiCommand) matchUI::concede);
        view.getBtnSettings().setCommand((UiCommand) SOverlayUtils::showOverlay);
        view.getBtnEndTurn().setCommand((UiCommand) this::endTurn);
        view.getBtnViewDeckList().setCommand((UiCommand) matchUI::viewDeckList);
        view.getBtnRevertLayout().setCommand((UiCommand) SLayoutIO::revertLayout);
        view.getBtnOpenLayout().setCommand((UiCommand) SLayoutIO::openLayout);
        view.getBtnSaveLayout().setCommand((UiCommand) SLayoutIO::saveLayout);
        view.getBtnAlphaStrike().setCommand((UiCommand) () -> matchUI.getGameController().alphaStrike());
        view.getBtnTargeting().setCommand((UiCommand) this::toggleTargeting);
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#update()
     */
    @Override
    public void update() {
    }

}
