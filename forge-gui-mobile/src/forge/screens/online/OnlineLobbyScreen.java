package forge.screens.online;

import forge.FThreads;
import forge.Forge;
import forge.assets.FSkinProp;
import forge.interfaces.ILobbyView;
import forge.match.GameLobby;
import forge.net.ChatMessage;
import forge.net.IOnlineChatInterface;
import forge.net.IOnlineLobby;
import forge.net.NetConnectUtil;
import forge.net.OfflineLobby;
import forge.net.client.FGameClient;
import forge.properties.ForgeConstants;
import forge.screens.LoadingOverlay;
import forge.screens.constructed.LobbyScreen;
import forge.screens.online.OnlineMenu.OnlineScreen;
import forge.util.gui.SOptionPane;

public class OnlineLobbyScreen extends LobbyScreen implements IOnlineLobby {
    public OnlineLobbyScreen() {
        super(null, OnlineMenu.getMenu(), new OfflineLobby());
    }

    private static GameLobby gameLobby;

    public GameLobby getGameLobby() {
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

    public void closeConn(String msg) {
        clearGameLobby();
        Forge.back();
        if (msg.length() > 0) {
            FThreads.invokeInBackgroundThread(new Runnable() {
                @Override
                public void run() {
                    SOptionPane.showMessageDialog(msg, "Error", FSkinProp.ICO_WARNING);
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
                            final String caption = joinServer ? "Connecting to server..." : "Starting server...";
                            LoadingOverlay.show(caption, new Runnable() {
                                @Override
                                public void run() {
                                    final ChatMessage result;
                                    final IOnlineChatInterface chatInterface = (IOnlineChatInterface)OnlineScreen.Chat.getScreen();
                                    if (joinServer) {
                                        result = NetConnectUtil.join(url, OnlineLobbyScreen.this, chatInterface);
                                        if (result.getMessage() == ForgeConstants.CLOSE_CONN_COMMAND) { //this message is returned via netconnectutil on exception
                                            closeConn("Invalid host address (" + url + ") was detected.");
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
                                }
                            });
                        }
                    });
                }
            });
        }
    }
}
