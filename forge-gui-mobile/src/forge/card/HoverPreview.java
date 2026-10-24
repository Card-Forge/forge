package forge.card;

import com.badlogic.gdx.math.Rectangle;
import forge.Forge;
import forge.Graphics;
import forge.gui.GuiBase;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;
import forge.toolbox.FCardPanel;

/**
 * Mouse-hover card preview for card lists (deck editor, collection, shops) on desktop.
 * Views report the card under the mouse while drawing; the preview is drawn on top of
 * everything at the end of the frame, on the side of the screen away from the mouse.
 * Controlled by the "Card List Hover Preview" setting.
 */
public final class HoverPreview {
    private static PaperCard hoveredCard;

    private HoverPreview() {
    }

    /** Call while drawing an item; records the card if the mouse is over both the item and its visible area. */
    public static void report(PaperCard card, float screenX, float screenY, float w, float h, Rectangle visibleArea) {
        if (card == null || !isEnabled())
            return;
        float mx = Forge.mouseMovedX, my = Forge.mouseMovedY;
        if (mx < screenX || mx >= screenX + w || my < screenY || my >= screenY + h)
            return;
        if (visibleArea != null && !visibleArea.contains(mx, my))
            return;
        hoveredCard = card;
    }

    /** Draws the preview for this frame, if any, and resets for the next frame. */
    public static void drawAndClear(Graphics g) {
        PaperCard card = hoveredCard;
        hoveredCard = null;
        if (card == null || !isEnabled())
            return;
        float screenW = Forge.getScreenWidth(), screenH = Forge.getScreenHeight();
        float margin = screenH * 0.03f;
        float cardH = screenH * 0.7f;
        float cardW = cardH / FCardPanel.ASPECT_RATIO;
        float x = Forge.mouseMovedX < screenW / 2 ? screenW - cardW - margin : margin;
        float y = (screenH - cardH) / 2;
        CardRenderer.drawCard(g, card, x, y, cardW, cardH, CardRenderer.CardStackPosition.Top);
    }

    private static boolean isEnabled() {
        return !GuiBase.isMobile()
                && FModel.getPreferences().getPrefBoolean(ForgePreferences.FPref.UI_ENABLE_HOVER_PREVIEW);
    }
}
