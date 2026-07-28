package forge.screens.online;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Gdx;

import forge.Forge;
import forge.gamemodes.net.ChatMessage;
import forge.gamemodes.net.IOnlineChatInterface;
import forge.gamemodes.net.IRemote;
import forge.gamemodes.net.event.MessageEvent;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.screens.FScreen;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FScrollPane;
import forge.toolbox.FTextField;
import forge.util.Utils;

public class OnlineChatScreen extends FScreen implements IOnlineChatInterface {
    private static final float PADDING = Utils.scale(5);

    private IRemote gameClient;
    private final ForgePreferences prefs = FModel.getPreferences();
    private final ChatLog lstLog = add(new ChatLog());
    private final FTextField txtSendMessage = add(new FTextField());

    private final List<IOnlineChatInterface> subscribers = new ArrayList<>();

    public OnlineChatScreen() {
        super(null, OnlineMenu.getMenu());

        txtSendMessage.setGhostText(Forge.getLocalizer().getMessage("lblEnterMessageToSend"));
        txtSendMessage.setChangedHandler(e -> sendMessage());
    }

    private void sendMessage() {
        String message = txtSendMessage.getText();
        if (message.isEmpty()) { return; }

        txtSendMessage.setText("");

        if (gameClient != null) {
            String source = prefs.getPref(FPref.PLAYER_NAME);
            gameClient.send(new MessageEvent(source, message));
        }
    }

    @Override
    protected void doLayout(float startY, float width, float height) {
        float x = PADDING;
        float y = startY + PADDING;
        float w = width - 2 * PADDING;
        float h = height - y - txtSendMessage.getHeight() - 2 * PADDING;
        lstLog.setBounds(x, y, w, h);
        y += h + PADDING;
        txtSendMessage.setBounds(x, y, w, txtSendMessage.getHeight());
    }

    @Override
    public void setGameClient(IRemote gameClient0) {
        gameClient = gameClient0;
        for (IOnlineChatInterface s : subscribers) {
            s.setGameClient(gameClient0);
        }
    }

    public IRemote getGameClient() {
        return gameClient;
    }

    @Override
    public void addMessage(ChatMessage message) {
        lstLog.addMessage(message);
        Gdx.graphics.requestRendering();
        for (IOnlineChatInterface s : subscribers) {
            s.addMessage(message);
        }
    }

    public void addSubscriber(IOnlineChatInterface subscriber) {
        if (subscriber == null || subscribers.contains(subscriber)) { return; }
        subscribers.add(subscriber);
        subscriber.setGameClient(gameClient);
        for (FDisplayObject obj : lstLog.getChildren()) {
            subscriber.addMessage(((ChatMessageBubble) obj).getMessage());
        }
    }

    public void removeSubscriber(IOnlineChatInterface subscriber) {
        subscribers.remove(subscriber);
    }

    private static class ChatLog extends FScrollPane {
        @Override
        protected ScrollBounds layoutAndGetScrollBounds(float visibleWidth, float visibleHeight) {
            float x = 0;
            float y = 0;
            float inset = 6 * PADDING;
            float bubbleWidth = visibleWidth - inset;

            for (FDisplayObject obj : getChildren()) {
                ChatMessageBubble bubble = (ChatMessageBubble)obj;
                if (bubble.isLocal) { //right-align local messages
                    bubble.setBounds(x + inset, y, bubbleWidth, bubble.getPreferredHeight(bubbleWidth));
                }
                else {
                    bubble.setBounds(x, y, bubbleWidth, bubble.getPreferredHeight(bubbleWidth));
                }
                y += bubble.getHeight() + PADDING;
            }
            return new ScrollBounds(visibleWidth, y);
        }

        private void addMessage(ChatMessage message) {
            add(new ChatMessageBubble(message));
            revalidate();
            scrollToBottom();
        }
    }
}
