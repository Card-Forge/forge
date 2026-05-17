package forge.screens.match.views;

import com.badlogic.gdx.Gdx;

import forge.Forge;
import forge.gamemodes.net.ChatMessage;
import forge.gamemodes.net.IOnlineChatInterface;
import forge.gamemodes.net.IRemote;
import forge.gamemodes.net.event.MessageEvent;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.menu.FDropDown;
import forge.menu.FMenuTab;
import forge.model.FModel;
import forge.screens.online.ChatMessageBubble;
import forge.screens.online.OnlineChatScreen;
import forge.screens.online.OnlineMenu;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FTextField;
import forge.util.Utils;

public class VChat extends FDropDown implements IOnlineChatInterface {
    private static final float PADDING = Utils.scale(5);

    private final FTextField txtSendMessage = add(new FTextField());

    private IRemote gameClient;

    public VChat() {
        txtSendMessage.setGhostText(Forge.getLocalizer().getMessage("lblEnterMessageToSend"));
        txtSendMessage.setChangedHandler(e -> sendMessage());

        OnlineChatScreen lobbyChat = lobbyChat();
        if (lobbyChat != null) {
            lobbyChat.addSubscriber(this); //replays history + sets gameClient
        }
    }

    public void unsubscribe() {
        OnlineChatScreen lobbyChat = lobbyChat();
        if (lobbyChat != null) {
            lobbyChat.removeSubscriber(this);
        }
    }

    private static OnlineChatScreen lobbyChat() {
        return (OnlineChatScreen) OnlineMenu.OnlineScreen.Chat.getScreen();
    }

    @Override
    public void setGameClient(IRemote gameClient0) {
        gameClient = gameClient0;
    }

    @Override
    public void addMessage(ChatMessage message) {
        add(new ChatMessageBubble(message));
        if (isVisible()) {
            updateSizeAndPosition();
        } else {
            FMenuTab tab = getMenuTab();
            if (tab != null) {
                tab.incrementUnread();
            }
        }
        Gdx.graphics.requestRendering();
    }

    @Override
    public void setVisible(boolean visible0) {
        boolean wasVisible = isVisible();
        super.setVisible(visible0);
        if (visible0 && !wasVisible) {
            FMenuTab tab = getMenuTab();
            if (tab != null) {
                tab.clearUnread();
            }
        }
    }

    private void sendMessage() {
        String message = txtSendMessage.getText();
        if (message.isEmpty()) { return; }

        txtSendMessage.setText("");

        if (gameClient != null) {
            String source = FModel.getPreferences().getPref(FPref.PLAYER_NAME);
            gameClient.send(new MessageEvent(source, message));
        }
    }

    @Override
    protected boolean autoHide() {
        return true;
    }

    @Override
    protected ScrollBounds updateAndGetPaneSize(float maxWidth, float maxVisibleHeight) {
        FDisplayObject owner = getDropDownOwner();
        float width = maxWidth - (owner != null ? owner.screenPos.x : 0);
        float minWidth = 6 * Utils.AVG_FINGER_WIDTH;
        if (width < minWidth) {
            width = minWidth;
        }
        float inset = 6 * PADDING;
        float bubbleWidth = width - inset;

        float y = 0;
        for (FDisplayObject obj : getChildren()) {
            if (obj == txtSendMessage) { continue; }
            ChatMessageBubble bubble = (ChatMessageBubble) obj;
            float h = bubble.getPreferredHeight(bubbleWidth);
            if (bubble.isLocal) { //right-align local messages
                bubble.setBounds(inset, y, bubbleWidth, h);
            } else {
                bubble.setBounds(0, y, bubbleWidth, h);
            }
            y += h + PADDING;
        }
        //pin send field at the bottom of scroll content; auto-scroll-to-bottom keeps it in view
        float fieldHeight = txtSendMessage.getHeight();
        txtSendMessage.setBounds(0, y, width, fieldHeight);
        y += fieldHeight + PADDING;

        return new ScrollBounds(width, y);
    }

    @Override
    protected void setScrollPositionsAfterLayout(float scrollLeft0, float scrollTop0) {
        super.setScrollPositionsAfterLayout(0, getMaxScrollTop()); //always scroll to bottom after layout
    }
}
