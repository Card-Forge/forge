package forge.screens.online;

import com.badlogic.gdx.utils.Align;

import forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinColor.Colors;
import forge.assets.FSkinFont;
import forge.assets.TextRenderer;
import forge.gamemodes.net.ChatMessage;
import forge.toolbox.FDisplayObject;
import forge.util.Utils;

public class ChatMessageBubble extends FDisplayObject {
    private static final FSkinFont FONT = FSkinFont.get(12);
    private static final FSkinFont META_FONT = FSkinFont.get(9);
    private static final FSkinColor LOCAL_COLOR = FSkinColor.get(Colors.CLR_ZEBRA);
    private static final FSkinColor REMOTE_COLOR = LOCAL_COLOR.getContrastColor(-20);
    private static final FSkinColor WARNING_COLOR = FSkinColor.getStandardColor(140, 95, 25);
    private static final FSkinColor SYSTEM_BUBBLE_COLOR = FSkinColor.getStandardColor(45, 95, 160);
    private static final FSkinColor SYSTEM_TEXT_COLOR = FSkinColor.getStandardColor(255, 255, 255);
    private static final FSkinColor MESSAGE_COLOR = FSkinColor.get(Colors.CLR_TEXT);
    private static final FSkinColor META_COLOR = MESSAGE_COLOR.alphaColor(0.55f);
    private static final FSkinColor SYSTEM_META_COLOR = SYSTEM_TEXT_COLOR.alphaColor(0.7f);
    private static final FSkinColor BORDER_COLOR = FSkinColor.get(Colors.CLR_BORDERS).alphaColor(0.5f);
    private static final float BORDER_THICKNESS = Utils.scale(1.5f);
    private static final float TEXT_INSET = Utils.scale(5);
    private static final float TRIANGLE_WIDTH = Utils.scale(8);

    private final ChatMessage message;
    public final boolean isLocal;
    private final boolean isSystem;
    private final TextRenderer textRenderer = new TextRenderer();

    public ChatMessage getMessage() {
        return message;
    }

    public ChatMessageBubble(ChatMessage message0) {
        message = message0;
        isLocal = message.isLocal();
        isSystem = message.getType() == ChatMessage.MessageType.SYSTEM;
    }

    public float getPreferredHeight(float width) {
        return META_FONT.getLineHeight() + 4 * TEXT_INSET + 2 * BORDER_THICKNESS
                + textRenderer.getWrappedBounds(message.getMessage(), FONT, width - 2 * TEXT_INSET).height;
    }

    @Override
    public void draw(Graphics g) {
        float x = isLocal ? 0 : TRIANGLE_WIDTH;
        float y = TEXT_INSET;
        float w = getWidth() - TRIANGLE_WIDTH;
        float h = getHeight() - TEXT_INSET;
        FSkinColor bubbleColor;
        FSkinColor textColor;
        FSkinColor metaColor;
        switch (message.getType()) {
            case WARNING:
                bubbleColor = WARNING_COLOR;
                textColor = MESSAGE_COLOR;
                metaColor = META_COLOR;
                break;
            case SYSTEM:
                bubbleColor = SYSTEM_BUBBLE_COLOR;
                textColor = SYSTEM_TEXT_COLOR;
                metaColor = SYSTEM_META_COLOR;
                break;
            default:
                bubbleColor = isLocal ? LOCAL_COLOR : REMOTE_COLOR;
                textColor = MESSAGE_COLOR;
                metaColor = META_COLOR;
                break;
        }
        int horzAlignment = isLocal ? Align.right : Align.left;

        //draw bubble fill
        g.fillRect(bubbleColor, x, y, w, h);
        g.drawRect(BORDER_THICKNESS, BORDER_COLOR, x, y, w, h);

        //draw triangle to make this look like chat bubble — anchored near top of message area
        float metaHeight = META_FONT.getLineHeight();
        float x1, x2;
        if (isLocal) {
            x1 = w - 1;
            x2 = w + TRIANGLE_WIDTH;
        }
        else {
            x1 = TRIANGLE_WIDTH + 1;
            x2 = 0;
        }
        float x3 = x1;
        float y1 = y + TEXT_INSET;
        float y3 = y1 + TRIANGLE_WIDTH * 1.25f;
        float y2 = (y1 + y3) / 2;

        g.fillTriangle(bubbleColor, x1, y1, x2, y2, x3, y3);
        g.drawLine(BORDER_THICKNESS, BORDER_COLOR, x1, y1, x2, y2);
        g.drawLine(BORDER_THICKNESS, BORDER_COLOR, x2, y2, x3, y3);

        //draw text
        x += TEXT_INSET;
        y += TEXT_INSET;
        w -= 2 * TEXT_INSET;

        //message text first, meta line below
        float messageHeight = getHeight() - y - metaHeight - 2 * TEXT_INSET;
        textRenderer.drawText(g, message.getMessage(), FONT, textColor, x, y, w, messageHeight, y, messageHeight, true, horzAlignment, true);
        y += messageHeight + TEXT_INSET;

        //meta line: "Sender · HH:MM:SS" — Server identifies itself for system messages
        String sourceLabel;
        if (message.getType() == ChatMessage.MessageType.SYSTEM) {
            sourceLabel = "Server";
        } else {
            sourceLabel = message.getSource();
        }
        String meta = sourceLabel != null
                ? sourceLabel + " · " + message.getTimestamp()
                : message.getTimestamp();
        g.drawText(meta, META_FONT, metaColor, x, y, w, metaHeight, false, horzAlignment, true);
    }

    public boolean isSystem() {
        return isSystem;
    }
}
