package forge.gamemodes.net;

import forge.gamemodes.match.GameLobby;
import forge.gamemodes.match.LobbySlotType;
import forge.gamemodes.net.client.ClientGameLobby;
import forge.gamemodes.net.server.FServerManager;
import forge.gamemodes.net.server.ServerGameLobby;
import forge.gui.GuiBase;
import forge.interfaces.ILobbyListener;
import forge.interfaces.IUpdateable;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgeProfileProperties;
import forge.model.FModel;

/**
 * Launch the app on its standalone server mode.
 * TODO: Lot of WIP - This is more a POC
 * @author add-le
 */
public class StandaloneServer {

    private static int counter = 0;

    /**
     * Disable the default constructor.
     */
    public StandaloneServer() {}

    /**
     * Launch the server on its host mode.
     * @param args -p : set port
     */
    public static void start(String[] args) {

        System.out.println("Standalone server mode v0.0.1");

        final ForgePreferences prefs = FModel.getPreferences();
        boolean propertyConfig = prefs != null && prefs.getPrefBoolean(ForgePreferences.FPref.UI_NETPLAY_COMPAT);
        GuiBase.enablePropertyConfig(propertyConfig);

        final int port = ForgeProfileProperties.getServerPort();
        final FServerManager server = FServerManager.getInstance();
        final ServerGameLobby lobby = new ServerGameLobby();

        server.startServer(port);
        server.setLobby(lobby);

        lobby.setListener(new IUpdateable() {
            @Override
            public void update(final boolean fullUpdate) {

                System.out.println("\n");
                System.out.println("step: " + counter);
                System.out.println("\n");

                counter++;

                //view.update(fullUpdate);
                server.updateLobbyState();

                // TODO: Manual trigger for test very WIP.
                if(counter == 12) {

                    System.out.println("step on");

                    Runnable startGame = lobby.startGame();
                    startGame.run();
                }

            }
            @Override
            public  void update(final int slot, final LobbySlotType type) {return;}
        });

        server.setLobbyListener(new ILobbyListener() {
            @Override
            public final void update(final GameLobby.GameLobbyData state, final int slot) {
                // NO-OP, lobby connected directly
            }
            @Override
            public final void message(final String source, final String message) {
                //chatInterface.addMessage(new ChatMessage(source, message));
            }
            @Override
            public final void close() {
                // NO-OP, server can't receive close message
            }
            @Override
            public ClientGameLobby getLobby() {
                return null;
            }
        });

    }

}
