package forge.screens.online;

import forge.FThreads;
import forge.Forge;
import forge.interfaces.ILobbyView;
import forge.match.GameLobby;
import forge.net.ChatMessage;
import forge.net.IOnlineChatInterface;
import forge.net.IOnlineLobby;
import forge.net.NetConnectUtil;
import forge.net.OfflineLobby;
import forge.net.client.FGameClient;
import forge.screens.LoadingOverlay;
import forge.screens.constructed.LobbyScreen;
import forge.screens.online.OnlineMenu.OnlineScreen;

public class OnlineLobbyScreen extends LobbyScreen implements IOnlineLobby {
    public OnlineLobbyScreen() {
        super(null, OnlineMenu.getMenu(), new OfflineLobby());
    }

    @Override
    public ILobbyView setLobby(GameLobby lobby0) {
        initLobby(lobby0);
        return this;
    }

    @Override
    public void setClient(FGameClient client) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onActivate() {
        if (getLobby() instanceof OfflineLobby) {
            //prompt to connect to server when offline lobby activated
            FThreads.invokeInBackgroundThread(new Runnable() {
                @Override
                public void run() {
                    final String url = NetConnectUtil.getServerUrl();
                    FThreads.invokeInEdtLater(new Runnable() {
                        @Override
                        public void run() {
                            if (url == null) {
                                Forge.back(); //go back to previous screen if user cancels connection
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
