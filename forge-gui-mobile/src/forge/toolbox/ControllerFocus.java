package forge.toolbox;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;

import forge.Forge;
import forge.Graphics;
import forge.itemmanager.ItemManager;
import forge.menu.FDropDown;
import forge.menu.FMenuItem;
import forge.menu.FMenuTab;
import forge.screens.match.MatchScreen;
import forge.util.Utils;

/**
 * Generic gamepad focus for Classic-mode screens and dialogs, which otherwise only support touch.
 * <p>
 * Keeps a single focused widget per root container (the top overlay, an open dropdown, or the current screen):
 * <ul>
 *     <li>D-pad / stick: move focus spatially</li>
 *     <li>A: activate the focused widget (synthetic tap)</li>
 *     <li>B: close an open dropdown, otherwise falls through to Back</li>
 * </ul>
 * The match screen keeps its own card navigation; this is only used there for overlays and dropdowns.
 */
public final class ControllerFocus {
    private static final Color FOCUS_COLOR = new Color(1f, 0.82f, 0.2f, 1f);
    private static final int MAX_LIST_ITEMS = 400;

    private static FDisplayObject focusRoot;
    private static FDisplayObject focused;
    private static int focusedItem = -1;

    private ControllerFocus() { }

    /** Marker for custom widgets that act on tap and should be reachable with the controller (A taps them). */
    public interface Focusable { }

    private static final class Target {
        final FDisplayObject obj;
        final int item; //list item index, or -1
        final FScrollPane scrollParent;
        final Rectangle rect;

        Target(FDisplayObject obj, int item, FScrollPane scrollParent, Rectangle rect) {
            this.obj = obj;
            this.item = item;
            this.scrollParent = scrollParent;
            this.rect = rect;
        }
        boolean is(FDisplayObject o, int i) {
            return obj == o && item == i;
        }
        float cx() { return rect.x + rect.width / 2f; }
        float cy() { return rect.y + rect.height / 2f; }
    }

    /** Clears focus, e.g. after touch input. */
    public static void reset() {
        focusRoot = null;
        focused = null;
        focusedItem = -1;
    }

    /**
     * Handles a controller key for the given container.
     * @return true if consumed; false to let the container's own keyDown handle it
     */
    public static boolean handleKey(FContainer container, int key) {
        FContainer root = findOpenDropDown(container);
        boolean dropDownOpen = root != null;
        if (root == null) {
            if (container instanceof MatchScreen) {
                return false; //match screen has its own card/row navigation
            }
            root = container;
        }
        if (root != focusRoot) {
            focusRoot = root;
            focused = null;
            focusedItem = -1;
        }

        List<Target> targets = new ArrayList<>();
        collect(root, null, targets);
        Target current = null;
        for (Target t : targets) {
            if (t.is(focused, focusedItem)) {
                current = t;
                break;
            }
        }

        boolean isDpad = key == Keys.DPAD_UP || key == Keys.DPAD_DOWN || key == Keys.DPAD_LEFT || key == Keys.DPAD_RIGHT;
        if (current == null && !targets.isEmpty() && (isDpad || key == Keys.BUTTON_A)) {
            //nothing focused yet: start on the first widget (the first d-pad press just shows it, A activates it)
            current = targets.get(0);
            setFocus(current);
            if (isDpad) {
                return true;
            }
        }

        //an item manager (deck/card lists) handles its own d-pad, A, Y and L1;
        //the d-pad leaves it when its selection can't move any further
        if (current != null && current.obj instanceof ItemManager && key != Keys.BUTTON_B) {
            ItemManager<?> im = (ItemManager<?>) current.obj;
            int before = im.getSelectedIndex();
            boolean handled = im.keyDown(key);
            if (handled && !(isDpad && im.getSelectedIndex() == before)) {
                return true;
            }
        }

        switch (key) {
            case Keys.DPAD_UP:
            case Keys.DPAD_DOWN:
            case Keys.DPAD_LEFT:
            case Keys.DPAD_RIGHT:
                if (targets.isEmpty()) {
                    return false;
                }
                setFocus(findInDirection(current, targets, key));
                return true;
            case Keys.BUTTON_A:
                if (current == null) {
                    return false;
                }
                activate(current);
                return true;
            case Keys.BUTTON_B:
                if (dropDownOpen) {
                    ((FDropDown) root).hide();
                    return true;
                }
                return false;
            default:
                return false;
        }
    }

    /** Debug: lists the focusable widgets of a container and the focused one (for test tools). */
    public static String describe(FContainer container) {
        StringBuilder sb = new StringBuilder();
        FContainer root = findOpenDropDown(container);
        if (root == null) {
            root = container;
        }
        sb.append("root=").append(root.getClass().getName()).append('\n');
        describeTree(root, 0, sb);
        List<Target> targets = new ArrayList<>();
        collect(root, null, targets);
        for (Target t : targets) {
            sb.append(t.is(focused, focusedItem) ? "* " : "  ").append(t.obj.getClass().getSimpleName());
            if (t.item >= 0) {
                sb.append('[').append(t.item).append(']');
            }
            sb.append(' ').append(t.rect).append('\n');
        }
        return sb.toString();
    }

    private static void describeTree(FDisplayObject obj, int depth, StringBuilder sb) {
        if (depth > 6) {
            return;
        }
        sb.append("  ".repeat(depth)).append("- ").append(obj.getClass().getName())
                .append(obj.isVisible() ? "" : " (hidden)").append(obj.isEnabled() ? "" : " (disabled)")
                .append(' ').append(obj.screenPos).append('\n');
        if (obj instanceof FContainer && !(obj instanceof ItemManager)) {
            for (FDisplayObject child : ((FContainer) obj).getChildren()) {
                describeTree(child, depth + 1, sb);
            }
        }
    }

    /**
     * Draws the focus ring; call after the screen and overlays are drawn.
     * @param keyTarget the container controller keys currently go to; the ring is hidden once focus belongs to another
     *                  (e.g. after the screen changed), until the next d-pad press focuses a widget there
     */
    public static void draw(Graphics g, FContainer keyTarget) {
        if (focused == null || keyTarget == null || !Forge.hasGamepad() || !Forge.lastInputWasController()) {
            return;
        }
        FContainer activeRoot = findOpenDropDown(keyTarget);
        if (activeRoot == null) {
            activeRoot = keyTarget;
        }
        if (activeRoot != focusRoot) {
            return;
        }
        if (!focused.isVisible()) {
            return;
        }
        Rectangle r = rectOf(focused, focusedItem);
        if (r == null || r.width <= 0 || r.height <= 0) {
            return;
        }
        drawRing(g, r.x, r.y, r.width, r.height);
    }

    /** Draws the controller focus ring around the given rectangle (local coordinates of the object being drawn). */
    public static void drawRing(Graphics g, float x, float y, float w, float h) {
        g.drawRect(Utils.scale(2), FOCUS_COLOR, x, y, w, h);
    }

    private static void setFocus(Target t) {
        if (t == null) {
            return;
        }
        focused = t.obj;
        focusedItem = t.item;
        if (t.item >= 0 && t.obj instanceof FList) {
            ((FList<?>) t.obj).scrollIntoView(t.item);
        } else if (t.scrollParent != null) {
            t.scrollParent.scrollIntoView(t.obj);
        }
    }

    private static void activate(Target t) {
        FDisplayObject o = t.obj;
        if (o instanceof FButton) {
            ((FButton) o).trigger();
            return;
        }
        float x = o.getWidth() / 2f;
        float y = o.getHeight() / 2f;
        if (t.item >= 0 && o instanceof FList) {
            FList<?> list = (FList<?>) o;
            list.scrollIntoView(t.item);
            float itemSize = list.getListItemRenderer().getItemHeight();
            float pos = list.getItemStartPosition(t.item) + itemSize / 2f;
            if (list.getListItemRenderer().layoutHorizontal()) {
                x = pos;
            } else {
                y = pos;
            }
        }
        o.press(x, y);
        boolean handled = o.tap(x, y, 1);
        o.release(x, y);
        //a real tap bubbles up to the parents when not handled; menu items rely on that so their dropdown hides
        if (!handled && focusRoot instanceof FDropDown && focusRoot != o && focusRoot.isVisible()) {
            focusRoot.tap(o.screenPos.x - focusRoot.screenPos.x + x, o.screenPos.y - focusRoot.screenPos.y + y, 1);
        }
    }

    private static Target findInDirection(Target from, List<Target> targets, int key) {
        Target best = null;
        float bestScore = Float.MAX_VALUE;
        float fx = from.cx(), fy = from.cy();
        for (Target t : targets) {
            if (t == from) {
                continue;
            }
            float dx = t.cx() - fx;
            float dy = t.cy() - fy;
            boolean vertical = key == Keys.DPAD_UP || key == Keys.DPAD_DOWN;
            float primary;
            switch (key) {
                case Keys.DPAD_UP:    primary = -dy; break;
                case Keys.DPAD_DOWN:  primary = dy;  break;
                case Keys.DPAD_LEFT:  primary = -dx; break;
                default:              primary = dx;  break;
            }
            if (primary <= 1f) {
                continue;
            }
            //sideways distance between the edges (0 when they overlap), so a narrow widget right below a wide one
            //wins over a wide one further away; ties go to the one lined up with the start of the current widget
            Rectangle a = from.rect, b = t.rect;
            float gap = vertical
                    ? Math.max(0, Math.max(a.x, b.x) - Math.min(a.x + a.width, b.x + b.width))
                    : Math.max(0, Math.max(a.y, b.y) - Math.min(a.y + a.height, b.y + b.height));
            float align = vertical ? Math.abs(b.x - a.x) : Math.abs(b.y - a.y);
            float score = primary + 2f * gap + 0.1f * align;
            if (score < bestScore) {
                bestScore = score;
                best = t;
            }
        }
        return best == null ? from : best;
    }

    private static FContainer findOpenDropDown(FContainer container) {
        for (FDisplayObject child : container.getChildren()) {
            //only pop-up menus take over focus; docked ones are part of the screen
            if (child instanceof FDropDown && child.isVisible() && ((FDropDown) child).isAutoHide()) {
                return (FContainer) child;
            }
        }
        return null;
    }

    private static void collect(FDisplayObject obj, FScrollPane scrollParent, List<Target> out) {
        if (!obj.isVisible() || !obj.isEnabled()) {
            return;
        }
        if (obj instanceof ItemManager) {
            addTarget(obj, -1, scrollParent, out);
            return;
        }
        if (obj instanceof FList && !(obj instanceof FChoiceList && ((FChoiceList<?>) obj).getCount() == 0)) {
            FList<?> list = (FList<?>) obj;
            int count = Math.min(list.getCount(), MAX_LIST_ITEMS);
            for (int i = 0; i < count; i++) {
                addTarget(list, i, scrollParent, out);
            }
            if (count > 0) {
                return;
            }
        }
        if (isFocusable(obj)) {
            addTarget(obj, -1, scrollParent, out);
            return;
        }
        if (obj instanceof FContainer) {
            FScrollPane sp = obj instanceof FScrollPane ? (FScrollPane) obj : scrollParent;
            for (FDisplayObject child : ((FContainer) obj).getChildren()) {
                collect(child, sp, out);
            }
        }
    }

    private static boolean isFocusable(FDisplayObject obj) {
        if (obj instanceof Focusable || obj instanceof FButton || obj instanceof FTextField || obj instanceof FToggleSwitch
                || obj instanceof FMenuItem || obj instanceof FMenuTab) {
            return true;
        }
        return obj instanceof FLabel && ((FLabel) obj).isActionable();
    }

    private static void addTarget(FDisplayObject obj, int item, FScrollPane scrollParent, List<Target> out) {
        Rectangle r = rectOf(obj, item);
        if (r == null || r.width <= 0 || r.height <= 0) {
            return;
        }
        out.add(new Target(obj, item, scrollParent, r));
    }

    private static Rectangle rectOf(FDisplayObject obj, int item) {
        Rectangle sp = obj.screenPos;
        if (item < 0 || !(obj instanceof FList)) {
            return new Rectangle(sp);
        }
        FList<?> list = (FList<?>) obj;
        float size = list.getListItemRenderer().getItemHeight();
        float start = list.getItemStartPosition(item);
        if (list.getListItemRenderer().layoutHorizontal()) {
            return new Rectangle(sp.x + start, sp.y, size, sp.height);
        }
        return new Rectangle(sp.x, sp.y + start, sp.width, size);
    }
}
