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
package forge.game.player;

import forge.game.GameState;
import forge.game.ai.AiController;

/**
 * <p>
 * AIPlayer class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class AIPlayer extends Player {

    private final PlayerControllerAi controller;
    private final LobbyPlayerAi lobbyPlayer;
    /**
     * <p>
     * Constructor for AIPlayer.
     * </p>
     * @param computerAIGeneral
     * 
     * @param myName
     *            a {@link java.lang.String} object.
     */
    public AIPlayer(final LobbyPlayerAi player, final GameState game) {
        super(player.getName(), game);
        lobbyPlayer = player;
        controller = new PlayerControllerAi(game, this);
        
    }

    public AiController getAi() { 
        return controller.getAi();
    }
    


    // /////////////////////////

    /* (non-Javadoc)
     * @see forge.game.player.Player#getType()
     */
    @Override
    public PlayerType getType() {
        return PlayerType.COMPUTER;
    }


    /* (non-Javadoc)
     * @see forge.game.player.Player#getController()
     */
    @Override
    public PlayerControllerAi getController() {
        return controller;
    }

    /* (non-Javadoc)
     * @see forge.game.player.Player#getLobbyPlayer()
     */
    @Override
    public LobbyPlayerAi getLobbyPlayer() {
        // TODO Auto-generated method stub
        return lobbyPlayer;
    }
} // end AIPlayer class
