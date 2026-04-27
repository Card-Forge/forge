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
    private static final FSkinColor LOCAL_COLOR = FSkinColor.get(Colors.CLR_ZEBRA);
    private static final FSkinColor REMOTE_COLOR = LOCAL_COLOR.getContrastColor(-20);
    private static final FSkinColor WARNING_COLOR = FSkinColor.getStandardColor(140, 95, 25);
    private static final FSkinColor MESSAGE_COLOR = FSkinColor.get(Colors.CLR_TEXT);
    private static final FSkinColor SOURCE_COLOR = MESSAGE_COLOR.alphaColor(0.75f);
    private static final FSkinColor TIMESTAMP_COLOR = MESSAGE_COLOR.alphaColor(0.5f);
    private static final FSkinColor BORDER_COLOR = FSkinColor.get(Colors.CLR_BORDERS).alphaColor(0.5f);
    private static final float BORDER_THICKNESS = Utils.scale(1.5f);
    private static final float TEXT_INSET = Utils.scale(5);
    private static final float TRIANGLE_WIDTH = Utils.scale(8);

    private final ChatMessage message;
    public final boolean isLocal;
    private final String header;
    private final TextRenderer textRenderer = new TextRenderer();

    public ChatMessageBubble(ChatMessage message0) {
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
        float height = FONT.getCapHeight() + 4 * TEXT_INSET + 2 * BORDER_THICKNESS;
        if (header != null) {
            height += FONT.getLineHeight();
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
        FSkinColor color;
        if (message.getType() == ChatMessage.MessageType.WARNING) {
            color = WARNING_COLOR;
        } else {
            color = isLocal ? LOCAL_COLOR : REMOTE_COLOR;
        }
        int horzAlignment = isLocal ? Align.right : Align.left;
        float timestampHeight = FONT.getCapHeight();

        //draw bubble fill
        g.fillRect(color, x, y, w, h);
        g.drawRect(BORDER_THICKNESS, BORDER_COLOR, x, y, w, h);

        //draw triangle to make this look like chat bubble
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
        float y1 = y + timestampHeight + TEXT_INSET;
        float y3 = y1 + TRIANGLE_WIDTH * 1.25f;
        float y2 = (y1 + y3) / 2;

        g.fillTriangle(color, x1, y1, x2, y2, x3, y3);
        g.drawLine(BORDER_THICKNESS, BORDER_COLOR, x1, y1, x2, y2);
        g.drawLine(BORDER_THICKNESS, BORDER_COLOR, x2, y2, x3, y3);

        //draw text
        x += TEXT_INSET;
        y += TEXT_INSET;
        w -= 2 * TEXT_INSET;

        if (!isLocal && message.getSource() != null) {
            float sourceHeight = FONT.getLineHeight();
            g.drawText(message.getSource() + ":", FONT, SOURCE_COLOR, x, y, w, sourceHeight, false, horzAlignment, true);
            y += sourceHeight;
        }

        g.drawText(message.getTimestamp(), FONT, TIMESTAMP_COLOR, x, h - timestampHeight, w, timestampHeight, false, horzAlignment, true);

        h -= y + timestampHeight + TEXT_INSET;
        textRenderer.drawText(g, message.getMessage(), FONT, MESSAGE_COLOR, x, y, w, h, y, h, true, horzAlignment, true);
    }
}
