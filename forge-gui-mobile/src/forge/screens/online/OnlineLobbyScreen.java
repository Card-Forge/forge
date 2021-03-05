package forge.screens.online;

import com.google.common.collect.ImmutableList;
import forge.FThreads;
import forge.Forge;
import forge.GuiBase;
import forge.gamemodes.match.GameLobby;
import forge.gamemodes.net.ChatMessage;
import forge.gamemodes.net.IOnlineChatInterface;
import forge.gamemodes.net.IOnlineLobby;
import forge.gamemodes.net.NetConnectUtil;
import forge.gamemodes.net.OfflineLobby;
import forge.gamemodes.net.client.FGameClient;
import forge.gamemodes.net.server.FServerManager;
import forge.interfaces.ILobbyView;
import forge.localinstance.assets.FSkinProp;
import forge.localinstance.properties.ForgeConstants;
import forge.screens.LoadingOverlay;
import forge.screens.constructed.LobbyScreen;
import forge.screens.online.OnlineMenu.OnlineScreen;
import forge.util.gui.SOptionPane;
import forge.util.Localizer;

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
            FThreads.invokeInBackgroundThread(new Runnable() {
                @Override
                public void run() {
                    final boolean callBackAlwaysTrue = SOptionPane.showOptionDialog(msg, Localizer.getInstance().getMessage("lblError"), FSkinProp.ICO_WARNING, ImmutableList.of(Localizer.getInstance().getMessage("lblOk")), 1) == 0;
                    if (callBackAlwaysTrue) { //to activate online menu popup when player press play online
                        GuiBase.setInterrupted(false);

                        if(FServerManager.getInstance() != null)
                            FServerManager.getInstance().stopServer();
                        if(getfGameClient() != null)
                            closeClient();
                    }
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
            FThreads.invokeInBackgroundThread(new Runnable() {
                @Override
                public void run() {
                    final String url = NetConnectUtil.getServerUrl();
                    FThreads.invokeInEdtLater(new Runnable() {
                        @Override
                        public void run() {
                            if (url == null) {
                                closeConn(""); //go back to previous screen if user cancels connection
                                return;
                            }

                            final boolean joinServer = url.length() > 0;
                            final String caption = joinServer ?  Localizer.getInstance().getMessage("lblConnectingToServer") : Localizer.getInstance().getMessage("lblStartingServer");
                            LoadingOverlay.show(caption, new Runnable() {
                                @Override
                                public void run() {
                                    final ChatMessage result;
                                    final IOnlineChatInterface chatInterface = (IOnlineChatInterface)OnlineScreen.Chat.getScreen();
                                    if (joinServer) {
                                        result = NetConnectUtil.join(url, OnlineLobbyScreen.this, chatInterface);
                                        if (result.getMessage() == ForgeConstants.CLOSE_CONN_COMMAND) { //this message is returned via netconnectutil on exception
                                            closeConn(Localizer.getInstance().getMessage("lblDetectedInvalidHostAddress", url));
                                            return;
                                        }
                                    }
                                    else {
                                        result = NetConnectUtil.host(OnlineLobbyScreen.this, chatInterface);
                                    }
                                    chatInterface.addMessage(result);
                                    if (!joinServer) {
                                        FThreads.invokeInBackgroundThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                NetConnectUtil.copyHostedServerUrl();
                                            }
                                        });
                                    }
                                    //update menu buttons
                                    OnlineScreen.Lobby.update();
                                }
                            });
                        }
                    });
                }
            });
        }
    }
}
