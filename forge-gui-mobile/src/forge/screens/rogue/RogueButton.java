package forge.screens.rogue;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Align;
import forge.Graphics;
import forge.toolbox.FDisplayObject;

/** Adventure's wooden button. The main action is gold, a page's chief action has adventure's gold frame, and a selected tab is pressed in. */
public class RogueButton extends FDisplayObject {
    public enum State { NORMAL, MAIN, GOLD, DISABLED, SELECTED }

    private static final Color FACE = new Color(0x262335ff);
    private static final Color FACE_DISABLED = new Color(0x474747ff);
    private static final Color TEXT_DISABLED = new Color(0.62f, 0.62f, 0.6f, 1);

    private String text;
    private final String icon;
    private State state;
    private final Runnable command;
    private boolean pressed;
    private float weight = 1;

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

    /** Its share of the width in the sideways row of buttons. */
    public RogueButton weight(float weight) {
        this.weight = weight;
        return this;
    }

    public float getWeight() {
        return weight;
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
    public boolean tap(float x, float y, int count) {
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
        float iconWidth = icon == null ? 0 : iconSize + (hasText ? 3 * u : 0);
        float textWidth = hasText ? RogueLook.textWidth(text, 1) : 0;
        float k = 1;
        while (textWidth * k + iconWidth > w - 8 * u && k > 0.7f) {
            k -= 0.04f;
        }
        float x = (w - textWidth * k - iconWidth) / 2;
        float midY = h / 2 - u;
        if (icon != null) {
            RogueLook.item(g, icon, x, midY - iconSize / 2, iconSize);
        }
        if (hasText) {
            Color color = state == State.DISABLED ? TEXT_DISABLED
                : state == State.MAIN || state == State.GOLD || state == State.SELECTED ? RogueLook.GOLD : Color.WHITE;
            RogueLook.text(g, text, x + iconWidth, midY - 6.5f * k * u, k, color, 0, Align.left);
        }
    }
}
