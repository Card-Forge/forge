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
package forge.util;

import java.util.Observable;

import forge.Singletons;
import forge.game.phase.PhaseHandler;

/**
 * <p>
 * MyObservable class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class MyObservable extends Observable {
    /**
     * <p>
     * updateObservers.
     * </p>
     */
    public final void updateObservers() {
        this.setChanged();
        this.notifyObservers();

        PhaseHandler phases = Singletons.getModel().getGame().getPhaseHandler();

        if ((phases != null) && !phases.mayPlayerHavePriority()) {
            if (phases.isNeedToNextPhaseInit()) {
                // this is used.
                phases.setPlayerMayHavePriority(true);
                phases.nextPhase();
            }
        }
    }
}
