package forge.screens.rogue;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Align;
import forge.Graphics;
import forge.assets.FSkinImage;
import forge.gui.GuiBase;
import forge.toolbox.FDisplayObject;

/** Adventure's wooden button. The main action is gold, a page's chief action has adventure's gold frame, and a selected tab is pressed in. */
public class RogueButton extends FDisplayObject {
    public enum State { NORMAL, MAIN, GOLD, DISABLED, SELECTED }

    private static final Color FACE = new Color(0x262335ff);
    private static final Color FACE_DISABLED = new Color(0x474747ff);
    private static final Color TEXT_DISABLED = new Color(0.62f, 0.62f, 0.6f, 1);

    private String text;
    private final String icon;
    private FSkinImage lead;
    private FSkinImage after;
    private String hint;
    private State state;
    private final Runnable command;
    private boolean pressed;
    private float weight = 1;
    private float uprightWeight = 1;

    /** icon is an adventure item sprite name, or null. */
    public RogueButton(String text, String icon, State state, Runnable command) {
        this.text = text;
        this.icon = icon;
        this.state = state;
        this.command = command;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    /** A skin image before the text, such as a lock, in place of the item icon; null for none. */
    public RogueButton lead(FSkinImage lead) {
        this.lead = lead;
        return this;
    }

    /** A skin image after the text, such as the coin after a price; null for none. */
    public RogueButton after(FSkinImage after) {
        this.after = after;
        return this;
    }

    /**
     * Explains the button, such as how to unlock it: on desktop while the pointer rests on it, on a phone when it is
     * tapped while disabled. Null for none.
     */
    public void setHint(String hint) {
        this.hint = hint;
    }

    /** Its share of the width in the sideways row of buttons. */
    public RogueButton weight(float weight) {
        this.weight = weight;
        return this;
    }

    public float getWeight() {
        return weight;
    }

    /** Its share of the width in an upright row of buttons. */
    public RogueButton uprightWeight(float uprightWeight) {
        this.uprightWeight = uprightWeight;
        return this;
    }

    public float getUprightWeight() {
        return uprightWeight;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    /** Disabled while the flow works, so a tap is not silently ignored; a selected tab keeps its look. */
    private State shown() {
        return state != State.SELECTED && RogueController.get().working() ? State.DISABLED : state;
    }

    @Override
    public boolean press(float x, float y) {
        pressed = shown() != State.DISABLED;
        return true;
    }

    @Override
    public boolean release(float x, float y) {
        pressed = false;
        return true;
    }

    @Override
    public void setHovered(boolean hovered) {
        super.setHovered(hovered);
        if (hovered && hint != null) {
            RogueScreen.hoverNote(this, null, hint);
        } else {
            RogueScreen.hover(this, false, null);
        }
    }

    @Override
    public boolean tap(float x, float y, int count) {
        if (shown() == State.DISABLED && hint != null && GuiBase.isMobile()) {
            RogueTermPopup.show(null, hint, localToScreenX(x), localToScreenY(y));
            return true;
        }
        if (shown() == State.DISABLED || command == null) {
            return false;
        }
        command.run();
        return true;
    }

    @Override
    public void draw(Graphics g) {
        float u = RogueLook.unit();
        float w = getWidth();
        float h = getHeight();
        State state = shown();
        g.fillRect(state == State.DISABLED ? FACE_DISABLED : FACE, 3 * u, 3 * u, w - 6 * u, h - 8 * u);
        String patch = switch (state) {
            case MAIN -> "unpressedgold10patch";
            case GOLD -> "TenPatchGold";
            case DISABLED -> "unpressed-disable10Patch";
            case SELECTED -> "pressed10Patch";
            case NORMAL -> pressed ? "pressed10Patch" : "unpressed10patch";
        };
        RogueLook.patch(g, patch, 0, 0, w, h);

        boolean hasText = text != null && !text.isEmpty();
        float iconSize = 13 * u;
        float iconWidth = icon == null && lead == null ? 0 : iconSize + (hasText ? 3 * u : 0);
        float afterWidth = after == null ? 0 : iconSize + 3 * u;
        float textWidth = hasText ? RogueLook.textWidth(text, 1) : 0;
        float k = 1;
        while (textWidth * k + iconWidth + afterWidth > w - 8 * u && k > 0.7f) {
            k -= 0.04f;
        }
        float x = (w - textWidth * k - iconWidth - afterWidth) / 2;
        float midY = h / 2 - u;
        if (lead != null) {
            RogueLook.skin(g, lead, x, midY - iconSize / 2, iconSize);
        } else if (icon != null) {
            RogueLook.item(g, icon, x, midY - iconSize / 2, iconSize);
        }
        if (hasText) {
            Color color = state == State.DISABLED ? TEXT_DISABLED
                : state == State.MAIN || state == State.GOLD || state == State.SELECTED ? RogueLook.GOLD : Color.WHITE;
            RogueLook.text(g, text, x + iconWidth, midY - 6.5f * k * u, k, color, 0, Align.left);
        }
        if (after != null) {
            RogueLook.skin(g, after, x + iconWidth + textWidth * k + 3 * u, midY - iconSize / 2, iconSize);
        }
    }
}
