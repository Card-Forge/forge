package forge.screens.online;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.TextRenderer;
import forge.assets.FSkinColor.Colors;
import forge.model.FModel;
import forge.net.ChatMessage;
import forge.net.IOnlineChatInterface;
import forge.net.IRemote;
import forge.net.event.MessageEvent;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;
import forge.screens.FScreen;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FScrollPane;
import forge.toolbox.FTextField;
import forge.util.Utils;

public class OnlineChatScreen extends FScreen implements IOnlineChatInterface {
    private static final float PADDING = Utils.scale(5);

    private IRemote gameClient;
    private final ForgePreferences prefs = FModel.getPreferences();
    private final ChatLog lstLog = add(new ChatLog());
    private final FTextField txtSendMessage = add(new FTextField());

    public OnlineChatScreen() {
        super(null, OnlineMenu.getMenu());

        txtSendMessage.setGhostText("Enter message to send");
        txtSendMessage.setChangedHandler(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                sendMessage();
            }
        });
    }
    
    private void sendMessage() {
        String message = txtSendMessage.getText();
        if (message.isEmpty()) { return; }

        txtSendMessage.setText("");

        if (gameClient != null) {
            String source = prefs.getPref(FPref.PLAYER_NAME);
            if (lstLog.getChildCount() % 2 == 1) {
                source = "RemoteGuy"; //TODO: Remove this
            }
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
    }

    @Override
    public void addMessage(ChatMessage message) {
        lstLog.addMessage(message);
        Gdx.graphics.requestRendering();
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
            return new ScrollBounds(visibleWidth, y - PADDING);
        }

        private void addMessage(ChatMessage message) {
            add(new ChatMessageBubble(message));
            revalidate();
            scrollToBottom();
        }
    }

    private static class ChatMessageBubble extends FDisplayObject {
        private static final FSkinFont FONT = FSkinFont.get(12);
        private static final FSkinColor LOCAL_COLOR = FSkinColor.get(Colors.CLR_ZEBRA);
        private static final FSkinColor REMOTE_COLOR = LOCAL_COLOR.getContrastColor(-20);
        private static final FSkinColor MESSAGE_COLOR = FSkinColor.get(Colors.CLR_TEXT);
        private static final FSkinColor SOURCE_COLOR = MESSAGE_COLOR.alphaColor(0.75f);
        private static final FSkinColor TIMESTAMP_COLOR = MESSAGE_COLOR.alphaColor(0.5f);
        private static final FSkinColor BORDER_COLOR = FSkinColor.get(Colors.CLR_BORDERS);
        private static final float BORDER_THICKNESS = Utils.scale(1);
        private static final float TEXT_INSET = Utils.scale(5);
        private static final float TRIANGLE_WIDTH = Utils.scale(5);

        private final ChatMessage message;
        private final boolean isLocal;
        private final String header;
        private final TextRenderer textRenderer = new TextRenderer();

        private ChatMessageBubble(ChatMessage message0) {
            message = message0;
            isLocal = message.isLocal();
            if (isLocal || message.getSource() == null) {
                header = null;
            }
            else {
                header = message.getSource() + ":";
            }
        }

        public float getPreferredHeight(float width) {
            float height = FONT.getCapHeight() + 4 * TEXT_INSET + TRIANGLE_WIDTH;
            if (header != null) {
                height += FONT.getLineHeight() + TEXT_INSET;
            }
            height += textRenderer.getWrappedBounds(message.getMessage(), FONT, width - 2 * TEXT_INSET).height;
            return height;
        }

        @Override
        public void draw(Graphics g) {
            float x = isLocal ? 0 : TRIANGLE_WIDTH;
            float y = TEXT_INSET;
            float w = getWidth() - TRIANGLE_WIDTH;
            float h = getHeight() - TEXT_INSET;
            FSkinColor color = isLocal ? LOCAL_COLOR : REMOTE_COLOR;
            HAlignment horzAlignment = isLocal ? HAlignment.RIGHT : HAlignment.LEFT;

            g.fillRect(color, x, y, w, h);
            g.drawRect(BORDER_THICKNESS, BORDER_COLOR, x, y, w, h);

            x += TEXT_INSET;
            y += TEXT_INSET;
            w -= 2 * TEXT_INSET;

            if (!isLocal && message.getSource() != null) {
                float sourceHeight = FONT.getLineHeight();
                g.drawText(message.getSource() + ":", FONT, SOURCE_COLOR, x, y, w, sourceHeight, false, horzAlignment, true);
                y += sourceHeight + TEXT_INSET;
            }

            float timestampHeight = FONT.getCapHeight();
            g.drawText(message.getTimestamp(), FONT, TIMESTAMP_COLOR, x, h - timestampHeight, w, timestampHeight, false, horzAlignment, true);

            h -= y + timestampHeight + TEXT_INSET;
            textRenderer.drawText(g, message.getMessage(), FONT, MESSAGE_COLOR, x, y, w, h, y, h, true, horzAlignment, true);
        }
    }
}
