package forge.screens.rogue;

import com.badlogic.gdx.graphics.Color;
import forge.Forge;
import forge.Graphics;
import forge.card.CardZoom;
import forge.gamemodes.rogue.KeywordHint;
import forge.gamemodes.rogue.PreviewReferenceType;
import forge.gamemodes.rogue.TextHelper;
import forge.gui.GuiBase;
import forge.item.PaperCard;
import forge.toolbox.FDisplayObject;
import java.util.ArrayList;
import java.util.List;

/**
 * A Rogue Commander text with its cards underlined solid and its defined terms underlined in dots, each across the
 * whole name. Tapping a card zooms it. On desktop, resting the pointer on a card or term shows it; on a phone, tapping a
 * term explains it.
 */
public class RogueText extends FDisplayObject {
    private record Hit(float x, float y, float w, float h, TextHelper.Part part) { }

    private final List<TextHelper.Part> parts;
    private final float scale;
    private final Color color;
    private final List<Hit> hits = new ArrayList<>();

    /** effectCardReference, which may be null, names the effect card whose name in the text is underlined as that card. */
    public RogueText(String raw, String effectCardReference, float scale, Color color) {
        this.parts = TextHelper.parts(raw, effectCardReference);
        this.scale = scale;
        this.color = color;
    }

    public float getPreferredHeight(float width) {
        return lay(null, width);
    }

    @Override
    public void draw(Graphics g) {
        lay(g, getWidth());
    }

    @Override
    public boolean tap(float x, float y, int count) {
        TextHelper.Part part = partAt(x, y);
        if (part == null) {
            return false;
        }
        if (part.type() == PreviewReferenceType.CARD) {
            PaperCard card = TextHelper.resolveCard(part.token());
            if (card != null) {
                CardZoom.show(card);
            }
        } else if (GuiBase.isMobile()) {
            // Desktop explains a term while the pointer rests on it
            KeywordHint hint = KeywordHint.fromToken(part.token());
            if (hint != null) {
                RogueTermPopup.show(hint.getTitle(), hint.getHintText(), localToScreenX(x), localToScreenY(y));
            }
        }
        return true;
    }

    @Override
    public void setHovered(boolean hovered) {
        super.setHovered(hovered);
        TextHelper.Part part = hovered ? partAt(screenToLocalX(Forge.mouseMovedX), screenToLocalY(Forge.mouseMovedY)) : null;
        if (part != null && part.type() == PreviewReferenceType.KEYWORD) {
            KeywordHint hint = KeywordHint.fromToken(part.token());
            if (hint != null) {
                RogueScreen.hoverNote(this, hint.getTitle(), hint.getHintText());
                return;
            }
        }
        boolean card = part != null && part.type() == PreviewReferenceType.CARD;
        RogueScreen.hover(this, card, card ? TextHelper.resolveCard(part.token()) : null);
    }

    private TextHelper.Part partAt(float x, float y) {
        for (Hit hit : hits) {
            if (x >= hit.x() && x <= hit.x() + hit.w() && y >= hit.y() && y <= hit.y() + hit.h()) {
                return hit.part();
            }
        }
        return null;
    }

    /** Draws when g is set; either way returns the height. */
    private float lay(Graphics g, float width) {
        float lineHeight = RogueLook.lineHeight(scale);
        float space = RogueLook.textWidth("a a", scale) - 2 * RogueLook.textWidth("a", scale);
        float x = 0;
        float y = 0;
        if (g != null) {
            hits.clear();
        }
        for (TextHelper.Part part : parts) {
            if (part.text().isEmpty()) {
                continue;
            }
            boolean card = part.type() == PreviewReferenceType.CARD;
            boolean term = part.type() == PreviewReferenceType.KEYWORD && KeywordHint.fromToken(part.token()) != null;
            boolean marked = card || term;
            Color partColor = card ? RogueLook.TEAL : color;
            float start = -1;
            float end = 0;
            for (String word : part.text().split("(?<= )|(?=\n)")) {
                boolean newLine = word.startsWith("\n");
                if (newLine) {
                    word = word.substring(1);
                }
                String bare = word.replaceAll("\\s+$", "");
                float wordWidth = RogueLook.textWidth(bare, scale);
                boolean punctuation = bare.length() == 1 && ".,;:!?)".contains(bare);
                if (newLine || (x + wordWidth > width && x > 0 && !punctuation)) {
                    if (g != null && marked && start >= 0) {
                        underline(g, start, end, y, card, part);
                    }
                    start = -1;
                    x = 0;
                    y += lineHeight;
                }
                if (g != null) {
                    RogueLook.text(g, bare, x, y, scale, partColor);
                }
                if (start < 0) {
                    start = x;
                }
                end = x + wordWidth;
                x += wordWidth + (word.endsWith(" ") ? space : 0);
            }
            if (g != null && marked && start >= 0) {
                underline(g, start, end, y, card, part);
            }
        }
        return y + lineHeight;
    }

    private void underline(Graphics g, float x1, float x2, float lineTop, boolean card, TextHelper.Part part) {
        float u = RogueLook.unit();
        float y = lineTop + 12.2f * scale * u;
        if (card) {
            RogueLook.line(g, x1, y, x2, y, 0.7f, RogueLook.TEAL);
        } else {
            for (float d = x1; d < x2; d += 2.4f * u) {
                g.fillRect(color, d, y, Math.min(1.2f * u, x2 - d), 0.9f * u);
            }
        }
        hits.add(new Hit(x1, lineTop, x2 - x1, RogueLook.lineHeight(scale), part));
    }
}
