package forge.screens.online;

import com.badlogic.gdx.utils.Align;
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
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.toolbox.FButton;
import forge.toolbox.FLabel;
import forge.util.Utils;

public class OnlineLobbyScreen extends LobbyScreen implements IOnlineLobby {

    private static final String GUIDE_URL = "https://github.com/Card-Forge/forge/wiki/network-play";

    // Landing page components
    private final FLabel lblTitle;
    private final FLabel lblWarning;
    private final FLabel lblGuideText;
    private final FLabel lblGuideLink;
    private final FButton btnHost;
    private final FButton btnJoin;
    private boolean showLanding = true;

    public OnlineLobbyScreen() {
        super(null, OnlineMenu.getMenu(), new OfflineLobby());

        lblTitle = new FLabel.Builder()
                .text("- = *  H E R E   B E   E L D R A Z I  * = -")
                .font(FSkinFont.get(18)).align(Align.center).build();
        add(lblTitle);

        lblWarning = new FLabel.Builder()
                .text(Forge.getLocalizer().getMessage("lblOnlineWarning"))
                .font(FSkinFont.get(14)).align(Align.center).build();
        add(lblWarning);

        lblGuideText = new FLabel.Builder()
                .text(Forge.getLocalizer().getMessage("lblOnlineGuideText"))
                .font(FSkinFont.get(12)).align(Align.center).build();
        add(lblGuideText);

        lblGuideLink = new FLabel.Builder()
                .text(Forge.getLocalizer().getMessage("lblNetworkPlayGuide"))
                .font(FSkinFont.get(14)).align(Align.center)
                .textColor(FSkinColor.get(FSkinColor.Colors.CLR_ACTIVE))
                .command(e -> com.badlogic.gdx.Gdx.net.openURI(GUIDE_URL)).build();
        add(lblGuideLink);

        btnHost = new FButton(Forge.getLocalizer().getMessage("lblHostGame"));
        btnHost.setCommand(e -> activateHost());
        add(btnHost);

        btnJoin = new FButton(Forge.getLocalizer().getMessage("lblJoinGame"));
        btnJoin.setCommand(e -> activateJoin());
        add(btnJoin);
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
            showLanding = true;
            revalidate();
        } else {
            showLanding = false;
            super.onActivate();
        }
    }

    @Override
    protected void doLayoutAboveBtnStart(float startY, float width, float height) {
        if (showLanding) {
            // Hide lobby controls and start button during landing page
            btnStart.setVisible(false);
            setLobbyControlsVisible(false);

            float padding = Utils.scale(10);
            float y = startY + height * 0.15f;

            // Title
            float labelHeight = lblTitle.getAutoSizeBounds().height + padding;
            lblTitle.setBounds(padding, y, width - 2 * padding, labelHeight);
            lblTitle.setVisible(true);
            y += labelHeight + padding * 2;

            // Warning
            labelHeight = lblWarning.getAutoSizeBounds().height + padding;
            lblWarning.setBounds(padding, y, width - 2 * padding, labelHeight);
            lblWarning.setVisible(true);
            y += labelHeight + padding * 2;

            // Guide text
            labelHeight = lblGuideText.getAutoSizeBounds().height + padding;
            lblGuideText.setBounds(padding, y, width - 2 * padding, labelHeight);
            lblGuideText.setVisible(true);
            y += labelHeight;

            // Guide link
            labelHeight = lblGuideLink.getAutoSizeBounds().height + padding;
            lblGuideLink.setBounds(padding, y, width - 2 * padding, labelHeight);
            lblGuideLink.setVisible(true);
            y += labelHeight + padding * 4;

            // Buttons side by side
            float buttonWidth = (width - 3 * padding) / 2;
            float buttonHeight = Utils.AVG_FINGER_HEIGHT;
            btnHost.setBounds(padding, y, buttonWidth, buttonHeight);
            btnHost.setVisible(true);
            btnJoin.setBounds(padding * 2 + buttonWidth, y, buttonWidth, buttonHeight);
            btnJoin.setVisible(true);
        } else {
            // Hide landing page components, show lobby controls
            lblTitle.setVisible(false);
            lblWarning.setVisible(false);
            lblGuideText.setVisible(false);
            lblGuideLink.setVisible(false);
            btnHost.setVisible(false);
            btnJoin.setVisible(false);
            setLobbyControlsVisible(true);
            btnStart.setVisible(true);

            super.doLayoutAboveBtnStart(startY, width, height);
        }
    }

    private void activateHost() {
        showLanding = false;
        setGameLobby(getLobby());
        revalidate();
        NetConnectUtil.ensurePlayerName();
        final String caption = Forge.getLocalizer().getMessage("lblStartingServer");
        LoadingOverlay.show(caption, true, () -> {
            final ChatMessage[] result = new ChatMessage[1];
            final IOnlineChatInterface chatInterface = (IOnlineChatInterface) OnlineScreen.Chat.getScreen();
            FThreads.invokeInBackgroundThread(() -> {
                result[0] = NetConnectUtil.host(OnlineLobbyScreen.this, chatInterface);
                chatInterface.addMessage(result[0]);
                NetConnectUtil.copyHostedServerUrl();
            });
            OnlineScreen.Lobby.update();
        });
    }

    private void activateJoin() {
        showLanding = false;
        setGameLobby(getLobby());
        revalidate();
        FThreads.invokeInBackgroundThread(() -> {
            final String url = NetConnectUtil.getJoinServerUrl();
            FThreads.invokeInEdtLater(() -> {
                if (url == null) {
                    closeConn("");
                    return;
                }
                final String caption = Forge.getLocalizer().getMessage("lblConnectingToServer");
                LoadingOverlay.show(caption, true, () -> {
                    final ChatMessage[] result = new ChatMessage[1];
                    final IOnlineChatInterface chatInterface = (IOnlineChatInterface) OnlineScreen.Chat.getScreen();
                    result[0] = NetConnectUtil.join(url, OnlineLobbyScreen.this, chatInterface);
                    String message = result[0].getMessage();
                    if (ForgeConstants.CLOSE_CONN_COMMAND.equals(message)) {
                        closeConn(Forge.getLocalizer().getMessage("UnableConnectToServer", url));
                        return;
                    } else if (message != null && message.startsWith(ForgeConstants.CONN_ERROR_PREFIX)) {
                        String errorDetail = message.substring(ForgeConstants.CONN_ERROR_PREFIX.length());
                        closeConn(errorDetail);
                        return;
                    } else if (ForgeConstants.INVALID_HOST_COMMAND.equals(message)) {
                        closeConn(Forge.getLocalizer().getMessage("lblDetectedInvalidHostAddress", url));
                        return;
                    }
                    chatInterface.addMessage(result[0]);
                    OnlineScreen.Lobby.update();
                });
            });
        });
    }
}
