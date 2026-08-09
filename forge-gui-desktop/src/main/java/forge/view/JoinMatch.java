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
package forge.view;

import forge.gui.FThreads;
import forge.gui.framework.EDocID;
import forge.screens.home.CHomeUI;
import forge.screens.home.online.CSubmenuOnlineLobby;

/**
 * Command-line "join" mode launcher.
 * <p>
 * Brings the client up to the network lobby and connects it to a game hosted
 * at the supplied server URL (e.g. {@code ipaddress:port}) using the same
 * code path as pressing "Join A Game" in the GUI.
 */
public final class JoinMatch {
    private JoinMatch() {
    }

    /**
     * Launches the GUI client into the lobby of the game hosted at
     * {@code serverUrl} (e.g. {@code "127.0.0.1:26782"}).
     * <p>
     * This should be called after the Forge singletons and controller have
     * been initialized (see {@link Main}). It schedules the join on the EDT.
     *
     * @param serverUrl the {@code host:port} (or full URL) of the game server to join
     */
    public static void join(final String serverUrl) {
        if (serverUrl == null || serverUrl.isEmpty()) {
            System.out.println("No server URL supplied. Usage: forge join ipaddress:port");
            return;
        }

        // Navigate to and prepare the online lobby screen, then join the server.
        // Done on the EDT since it manipulates Swing state and mirrors the
        // normal "Join A Game" button flow.
        FThreads.invokeInEdtLater(() -> {
            CHomeUI.SINGLETON_INSTANCE.itemClick(EDocID.HOME_NETWORK);
            CSubmenuOnlineLobby.SINGLETON_INSTANCE.joinServer(serverUrl);
        });
    }
}
