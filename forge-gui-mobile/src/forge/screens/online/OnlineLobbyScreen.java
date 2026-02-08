package forge.screens.online;

import com.google.common.collect.ImmutableList;

import forge.Forge;
import forge.gamemodes.match.GameLobby;
import forge.gamemodes.net.ChatMessage;
import forge.gamemodes.net.IOnlineChatInterface;
import forge.gamemodes.net.IOnlineLobby;
import forge.gamemodes.net.NetConnectUtil;
import forge.gamemodes.net.OfflineLobby;
import forge.gamemodes.net.client.FGameClient;
import forge.gamemodes.net.server.FServerManager;
import forge.gui.FThreads;
import forge.gui.GuiBase;
import forge.gui.interfaces.ILobbyView;
import forge.gui.util.SOptionPane;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.skin.FSkinProp;
import forge.screens.LoadingOverlay;
import forge.screens.constructed.LobbyScreen;
import forge.screens.online.OnlineMenu.OnlineScreen;

public class OnlineLobbyScreen extends LobbyScreen implements IOnlineLobby {
    public OnlineLobbyScreen() {
        super(null, OnlineMenu.getMenu(), new OfflineLobby());
    }

    private static GameLobby gameLobby;

    public static GameLobby getGameLobby() {
        return gameLobby;
    }

    public static void clearGameLobby() {
        gameLobby = null;
    }

    public static void setGameLobby(GameLobby gameLobby) {
        OnlineLobbyScreen.gameLobby = gameLobby;
    }

    private static FGameClient fGameClient;

    public static FGameClient getfGameClient() {
        return fGameClient;
    }

    public static void closeClient() {
        getfGameClient().close();
        fGameClient = null;
    }

    @Override
    public void closeConn(String msg) {
        clearGameLobby();
        Forge.back();
        if (msg.length() > 0) {
            FThreads.invokeInBackgroundThread(() -> {
                final boolean callBackAlwaysTrue = SOptionPane.showOptionDialog(msg, Forge.getLocalizer().getMessage("lblError"), FSkinProp.ICO_WARNING, ImmutableList.of(Forge.getLocalizer().getMessage("lblOK")), 1) == 0;
                if (callBackAlwaysTrue) { //to activate online menu popup when player press play online
                    GuiBase.setInterrupted(false);

                    if(FServerManager.getInstance() != null)
                        FServerManager.getInstance().stopServer();
                    if(getfGameClient() != null)
                        closeClient();
                }
            });
        }
    }

    @Override
    public ILobbyView setLobby(GameLobby lobby0) {
        initLobby(lobby0);
        return this;
    }

    @Override
    public void setClient(FGameClient client) {
        fGameClient = client;
    }

    @Override
    public void onActivate() {
        if (GuiBase.isInterrupted()) {
            GuiBase.setInterrupted(false);
            return;
        }
        if (getGameLobby() == null) {
            setGameLobby(getLobby());
            //prompt to connect to server when offline lobby activated
            FThreads.invokeInBackgroundThread(() -> {
                //Not in gui thread
                final String url = NetConnectUtil.getServerUrl();
                FThreads.invokeInEdtLater(() -> {
                    //In GUI thread
                    if (url == null) {
                        closeConn(""); //go back to previous screen if user cancels connection
                        return;
                    }
                    final boolean joinServer = url.length() > 0;
                    final String caption = joinServer ?  Forge.getLocalizer().getMessage("lblConnectingToServer") : Forge.getLocalizer().getMessage("lblStartingServer");
                    LoadingOverlay.show(caption, true, () -> {
                        //in GUI thread due to above LoadingOverlay.show call executing ThreadUtil.invokeInGameThread(() -> FThreads.invokeInEdtLater(() -> { runnable.run(); ...
                        final ChatMessage[] result = new ChatMessage[1];
                        final IOnlineChatInterface chatInterface = (IOnlineChatInterface)OnlineScreen.Chat.getScreen();
                        if (joinServer) {
                            result[0] = NetConnectUtil.join(url, OnlineLobbyScreen.this, chatInterface);
                            String message = result[0].getMessage();
                            if (ForgeConstants.CLOSE_CONN_COMMAND.equals(message)) { //this message is returned via netconnectutil on exception
                                closeConn(Forge.getLocalizer().getMessage("UnableConnectToServer", url));
                                return;
                            } else if (message != null && message.startsWith(ForgeConstants.CONN_ERROR_PREFIX)) {
                                // Show detailed connection error
                                String errorDetail = message.substring(ForgeConstants.CONN_ERROR_PREFIX.length());
                                closeConn(errorDetail);
                                return;
                            } else if (ForgeConstants.INVALID_HOST_COMMAND.equals(message)) {
                                closeConn(Forge.getLocalizer().getMessage("lblDetectedInvalidHostAddress", url));
                                return;
                            }
                            chatInterface.addMessage(result[0]);
                        }
                        else {
                            FThreads.invokeInBackgroundThread(
                                    () -> {
                                        result[0] = NetConnectUtil.host(OnlineLobbyScreen.this, chatInterface);
                                        chatInterface.addMessage(result[0]);
                                        NetConnectUtil.copyHostedServerUrl();
                                    }
                            );
                        }
                        //update menu buttons
                        OnlineScreen.Lobby.update();
                    });
                });
            });
        }
    }
}
