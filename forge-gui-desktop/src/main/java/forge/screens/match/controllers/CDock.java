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

import java.util.Map;

import com.google.common.primitives.Ints;

import forge.Singletons;
import forge.gamemodes.match.YieldController;
import forge.gui.UiCommand;
import forge.gui.framework.ICDoc;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.localinstance.skin.FSkinProp;
import forge.model.FModel;
import forge.screens.match.CMatchUI;
import forge.screens.match.VAutoYieldsAndTriggers;
import forge.screens.match.VYieldSettings;
import forge.screens.match.views.VDock;
import forge.screens.match.views.VDock.DockButtonId;
import forge.toolbox.FSkin;
import forge.util.Localizer;

/**
 * Controls the dock panel in the match UI.
 *
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 */
public class CDock implements ICDoc {

    private final CMatchUI matchUI;
    private final VDock view;
    public CDock(final CMatchUI matchUI) {
        this.matchUI = matchUI;
        this.view = new VDock(this);
    }

    public enum ArcState {
        OFF, MOUSEOVER, ON;
        public ArcState next() {
            final ArcState[] vs = values();
            return vs[(ordinal() + 1) % vs.length];
        }
    }

    public VDock getView() {
        return view;
    }

    /** Reads UI_TARGETING_OVERLAY on demand. Falls back to OFF for an
     *  unparseable / unset pref. */
    public ArcState getArcState() {
        final Integer ord = Ints.tryParse(FModel.getPreferences().getPref(FPref.UI_TARGETING_OVERLAY));
        return ArcState.values()[ord == null ? 0 : ord];
    }

    public void toggleTargeting() {
        final ArcState next = getArcState().next();
        FModel.getPreferences().setPref(FPref.UI_TARGETING_OVERLAY, String.valueOf(next.ordinal()));
        FModel.getPreferences().save();
        update();
        Singletons.getView().getFrame().repaint();
    }

    @Override
    public void register() {
    }

    @Override
    public void initialize() {
        final Map<DockButtonId, UiCommand> commands = Map.of(
                DockButtonId.CONCEDE,        matchUI::concede,
                DockButtonId.YIELD_SETTINGS, () -> new VYieldSettings(matchUI).showDialog(),
                DockButtonId.END_TURN,       () -> YieldController.endTurn(matchUI.getGameController(), matchUI.getCurrentPlayer()),
                DockButtonId.AUTO_PASS,      this::toggleAutoPass,
                DockButtonId.VIEW_DECK_LIST, matchUI::viewDeckList,
                DockButtonId.ALPHA_STRIKE,   () -> matchUI.getGameController().alphaStrike(),
                DockButtonId.TARGETING,      this::toggleTargeting,
                DockButtonId.AUTO_YIELDS,    () -> new VAutoYieldsAndTriggers(matchUI).showDialog());
        commands.forEach((id, cmd) -> view.getButton(id).setCommand(cmd));

        update();
    }

    private void toggleAutoPass() {
        YieldController.toggleAutoPassNoActions(matchUI.getGameController());
        update();
    }

    @Override
    public void update() {
        final ArcState arcs = getArcState();
        view.getButton(DockButtonId.AUTO_PASS).setActive(FModel.getPreferences().getPrefBoolean(FPref.YIELD_AUTO_PASS_NO_ACTIONS));
        final VDock.DockButton targeting = view.getButton(DockButtonId.TARGETING);
        targeting.setActive(arcs != ArcState.OFF);
        final FSkinProp arcIcon = switch (arcs) {
            case ON -> FSkinProp.ICO_ARCSON;
            case MOUSEOVER -> FSkinProp.ICO_ARCSHOVER;
            case OFF -> FSkinProp.ICO_ARCSOFF;
        };
        targeting.setImage(FSkin.getIcon(arcIcon));
        final Localizer loc = Localizer.getInstance();
        final String stateKey = switch (arcs) {
            case OFF -> "lblOff";
            case MOUSEOVER -> "lblCardMouseOver";
            case ON -> "lblAlwaysOn";
        };
        targeting.setToolTipText(loc.getMessage("lblTargetingArcs") + ": " + loc.getMessage(stateKey));
    }

}
