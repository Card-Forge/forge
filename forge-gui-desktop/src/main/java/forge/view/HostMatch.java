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

import javax.swing.SwingUtilities;

import forge.gamemodes.net.ChatMessage;
import forge.gamemodes.net.NetConnectUtil;
import forge.gui.FNetOverlay;
import forge.gui.SOverlayUtils;
import forge.gui.framework.EDocID;
import forge.screens.home.CHomeUI;
import forge.screens.home.online.VSubmenuOnlineLobby;

/**
 * Command-line "host" mode launcher.
 * <p>
 * Starts a local multiplayer server on the given port (e.g. {@code 36743}) for
 * others to join. Port forwarding is assumed to already be in place, so the
 * UPnP / port-forwarding prompt normally shown when hosting from the GUI is
 * skipped entirely.
 */
public final class HostMatch {
    private HostMatch() {
    }

    /**
     * Hosts a game on the given local {@code port}.
     * <p>
     * This should be called after the Forge singletons and controller have
     * been initialized (see {@link Main}). The hosting work is deferred to the
     * EDT so it runs after the main window is shown, mirroring the normal GUI
     * "Host A Game" flow but without the port-forwarding prompt.
     *
     * @param port the local port to bind the server to
     */
    public static void host(final int port) {
        NetConnectUtil.ensurePlayerName();
        SwingUtilities.invokeLater(() -> {
            try {
                System.out.println("[HostMatch] moving to network lobby screen...");
                CHomeUI.SINGLETON_INSTANCE.itemClick(EDocID.HOME_NETWORK);
                System.out.println("[HostMatch] hosting game on port " + port + " (port forwarding assumed configured)...");

                SOverlayUtils.startGameOverlay("Starting Server...");
                SOverlayUtils.showOverlay();

                final ChatMessage result = NetConnectUtil.host(
                        VSubmenuOnlineLobby.SINGLETON_INSTANCE,
                        FNetOverlay.SINGLETON_INSTANCE,
                        port);

                SOverlayUtils.hideOverlay();
                FNetOverlay.SINGLETON_INSTANCE.show(result);
                if (CHomeUI.SINGLETON_INSTANCE.getCurrentDocID() == EDocID.HOME_NETWORK) {
                    VSubmenuOnlineLobby.SINGLETON_INSTANCE.populate();
                }
                System.out.println("[HostMatch] hosting started.");
            } catch (final Throwable t) {
                // Print directly so it always shows on the console (even though
                // ExceptionHandler also duplicates it to forge.log).
                t.printStackTrace();
            }
        });
    }
}
