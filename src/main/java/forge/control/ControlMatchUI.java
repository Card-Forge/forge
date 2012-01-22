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
package forge.control;

import java.util.List;

import forge.AllZone;
import forge.Constant.Zone;
import forge.ImageCache;
import forge.Player;
import forge.control.match.ControlField;
import forge.view.match.MatchTopLevel;

/**
 * <p>
 * ControlMatchUI
 * </p>
 * Top-level controller for matches. Implements Display.
 * 
 */
public class ControlMatchUI {
    private final MatchTopLevel view;

    /**
     * <p>
     * ControlMatchUI
     * </p>
     * Constructs instance of match UI controller, used as a single point of
     * top-level control for child UIs - in other words, this class controls the
     * controllers. Tasks targeting the view of individual components are found
     * in a separate controller for that component and should not be included
     * here.
     * 
     * This constructor is called after child components have been instantiated.
     * When children are instantiated, they also instantiate their controller.
     * So, this class must be called after everything is already in place.
     * 
     * @param v
     *            &emsp; A MatchTopLevel object
     */
    public ControlMatchUI(final MatchTopLevel v) {
        this.view = v;
    }

    /**
     * Fires up controllers for each component of UI.
     * 
     */
    public void initMatch() {
        // All child components have been assembled; observers and listeners can
        // be added safely.
        this.view.getTabberController().addObservers();
        this.view.getTabberController().addListeners();

        this.view.getInputController().addListeners();

        this.view.getHandController().addObservers();
        this.view.getHandController().addListeners();

        // Update all observers with values for start of match.
        final List<ControlField> fieldControllers = this.view.getFieldControllers();
        for (final ControlField f : fieldControllers) {
            f.addObservers();
            f.addListeners();
            f.getPlayer().updateObservers();
        }

        AllZone.getHumanPlayer().getZone(Zone.Hand).updateObservers();
        AllZone.getComputerPlayer().getZone(Zone.Hand).updateObservers();
        AllZone.getStack().updateObservers();
        AllZone.getHumanPlayer().getZone(Zone.Battlefield).updateObservers();
        AllZone.getInputControl().updateObservers();
        this.view.getTabberController().updateObservers();
    }

    /**
     * Deletes all observers for Match UI elements and clears the ImageCache.
     */
    public void deinitMatch() {

        ImageCache.clear();

        // Delete player observers
        for (Player p : AllZone.getPlayersInGame()) {
            p.deleteObservers();
            p.getZone(Zone.Battlefield).deleteObservers();
            p.getZone(Zone.Hand).deleteObservers();
        }

        AllZone.getStack().deleteObservers();
        AllZone.getGameLog().deleteObservers();
        AllZone.getInputControl().deleteObservers();
        AllZone.getPhaseHandler().deleteObservers();
    }

    /**
     * Resets all phase buttons in all fields to "inactive", so highlight won't
     * be drawn on them. "Enabled" state remains the same.
     */
    // This method is in the top-level controller because it affects ALL fields
    // (not just one).
    public void resetAllPhaseButtons() {
        final List<ControlField> fieldControllers = this.view.getFieldControllers();

        for (final ControlField c : fieldControllers) {
            c.resetPhaseButtons();
        }
    }

    /** @return MatchTopLevel */
    public MatchTopLevel getView() {
        return view;
    }
}
