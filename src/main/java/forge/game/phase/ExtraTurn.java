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
package forge.game.phase;

import forge.game.player.Player;

/**
 * <p>
 * ExtraTurn class.
 * Stores informations about extra turns
 * </p>
 * 
 * @author Forge
 * @version $Id: ExtraTurn 12482 2011-12-06 11:14:11Z Sloth $
 */
public class ExtraTurn {

    private Player player = null;
    private boolean loseAtEndStep = false;
    private boolean skipUntap = false;
    /**
     * TODO: Write javadoc for Constructor.
     * @param player the player
     */
    public ExtraTurn(Player player) {
        this.player = player;
    }

    /**
     * @return the player
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * @param player the player to set
     */
    public void setPlayer(Player player) {
        this.player = player;
    }

    /**
     * @return the loseAtEndStep
     */
    public boolean isLoseAtEndStep() {
        return loseAtEndStep;
    }

    /**
     * @param loseAtEndStep the loseAtEndStep to set
     */
    public void setLoseAtEndStep(boolean loseAtEndStep) {
        this.loseAtEndStep = loseAtEndStep;
    }

    /**
     * @return the skipUntap
     */
    public boolean isSkipUntap() {
        return skipUntap;
    }

    /**
     * @param skipUntap the skipUntap to set
     */
    public void setSkipUntap(boolean skipUntap) {
        this.skipUntap = skipUntap;
    }

} //end class Untap
