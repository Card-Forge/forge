package forge.screens.match.arena;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.math.Rectangle;

import forge.Forge;
import forge.Graphics;
import forge.card.CardZoom;
import forge.game.card.CardView;
import forge.screens.FScreen;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FOverlay;

/**
 * The frame every Arena dialog is drawn in (what the Swing {@code ArenaOptionsOverlay} and {@code ArenaCardBrowser}
 * share): it covers the whole match, darkens the board behind it, comes in with the Arena entrance (the backdrop
 * fades over the board, then the contents rise into place), steps aside with "View Battlefield" (shrinking to that
 * one button until pressed again), and goes with the board fading back in its place.
 *
 * <p>Subclasses draw the scene (under everything) and the contents (risen and faded in with the entrance) and get the
 * pointer, taps, drags, the wheel and keys in their own coordinates. Their buttons are {@link ArenaDialogKit.Pill}s:
 * register them with {@link #addPill} and the frame draws, lights, presses and fires them. Enter submits, Escape
 * cancels where the dialog allows it, and the arrow keys and the wheel go to the dialog.</p>
 */
public abstract class ArenaDialogOverlay extends FOverlay {
    protected final ArenaDialogKit.Entrance entrance = new ArenaDialogKit.Entrance();
    protected final ArenaDialogKit.Heading heading = new ArenaDialogKit.Heading();
    protected final ArenaDialogKit.Pill btnView = new ArenaDialogKit.Pill();
    protected boolean viewingBattlefield;
    /** Where the pointer is, in the dialog's own coordinates (-1 when unknown). */
    protected float pointerX = -1, pointerY = -1;
    private final List<ArenaDialogKit.Pill> pills = new ArrayList<>();
    private final Rectangle viewRect = new Rectangle();
    private final ContentLayer content = new ContentLayer();
    private ArenaDialogKit.Pill pressedPill;
    private boolean pointerDown;
    private boolean closed;
    private int openMs = 320;

    protected ArenaDialogOverlay() {
        super(null); // the dialog draws its own backdrop
        btnView.action = this::toggleBattlefield;
        updateViewText();
        pills.add(btnView);
    }

    /** Registers a button: drawn with the contents, lit under the pointer, fired by a tap. */
    protected final ArenaDialogKit.Pill addPill(final ArenaDialogKit.Pill pill) {
        pills.add(pill);
        return pill;
    }

    /** How long the entrance takes (Swing: 320 ms for the options, 340 for the browser). */
    protected final void setOpenMillis(final int ms) {
        openMs = ms;
    }

    public final boolean isClosed() {
        return closed;
    }

    /** Opens the dialog over the match. Render thread. */
    public void open() {
        closed = false;
        pointerX = Forge.mouseMovedX;
        pointerY = Forge.mouseMovedY;
        show();
        entrance.begin(openMs);
        onOpened();
        ArenaMotion.requestFrame();
    }

    protected void onOpened() {
    }

    /** Takes the dialog down; what it showed fades off the board rather than vanishing. Runs {@link #onClosed()}. */
    protected final void close() {
        if (closed) {
            return;
        }
        closed = true;
        if (!viewingBattlefield) {
            ArenaCurtain.fadeOver(ArenaMotion.CLOSE_MS);
        }
        hide();
        onClosed();
    }

    /** Called once when the dialog has gone: hand the answer back here. */
    protected abstract void onClosed();

    // ---- view battlefield -------------------------------------------------------------------------------------------

    protected void toggleBattlefield() {
        if (!viewingBattlefield) {
            ArenaCurtain.fadeOver(ArenaMotion.DIALOG_GONE_MS); // the dialog fades off the board it gives way to
        }
        viewingBattlefield = !viewingBattlefield;
        updateViewText();
        if (!viewingBattlefield) {
            entrance.begin(280); // and comes back over it the way it came in
        }
        onViewToggled();
        ArenaMotion.requestFrame();
    }

    protected void onViewToggled() {
    }

    private void updateViewText() {
        btnView.text = Forge.getLocalizer().getMessage(viewingBattlefield ? "lblArenaReturn" : "lblArenaViewBattlefield");
    }

    @Override
    public boolean preventInputBehindOverlay() {
        return !viewingBattlefield; // while the board is being looked at, only the button is the dialog's
    }

    // ---- layout and drawing -----------------------------------------------------------------------------------------

    @Override
    protected final void doLayout(final float width, final float height) {
        ArenaDialogKit.viewButtonBounds(width, height, viewRect);
        btnView.bounds.set(viewRect);
        layoutDialog(width, height);
    }

    /** Places the buttons and contents for a window this size. */
    protected abstract void layoutDialog(float w, float h);

    /** The backdrop's own layer: the heading, the prompt's card, guides. {@code alpha} is how far it is in. */
    protected abstract void drawScene(Graphics g, float w, float h, float alpha);

    /** What the dialog holds (cards, options, buttons are drawn after it): {@code alpha} is how far it is in. */
    protected abstract void drawContent(Graphics g, float w, float h, float alpha);

    @Override
    public void draw(final Graphics g) {
        final float w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        if (viewingBattlefield) {
            btnView.draw(g, 1f);
            return;
        }
        final float scene = entrance.scene();
        ArenaDialogKit.backdrop(g, w, h, scene);
        drawScene(g, w, h, scene);
        content.setBounds(0, entrance.contentDrop(h), w, h);
        g.draw(content);
    }

    /** The contents, drawn one level down so that they can rise into place with the entrance. */
    private final class ContentLayer extends FDisplayObject {
        @Override
        public void draw(final Graphics g) {
            final float w = getWidth(), h = getHeight(), a = entrance.content();
            if (a <= 0f) {
                return;
            }
            drawContent(g, w, h, a);
            for (final ArenaDialogKit.Pill pill : pills) {
                pill.draw(g, a);
            }
        }
    }

    // ---- input ------------------------------------------------------------------------------------------------------

    @Override
    public void buildTouchListeners(final float screenX, final float screenY, final List<FDisplayObject> listeners) {
        if (!isVisible()) {
            return;
        }
        final float x = screenToLocalX(screenX), y = screenToLocalY(screenY);
        if (viewingBattlefield) {
            final boolean over = btnView.contains(x, y);
            btnView.setHover(over);
            if (over) {
                listeners.add(this);
            }
            return;
        }
        if (x != pointerX || y != pointerY) {
            pointerX = x;
            pointerY = y;
            boolean onPill = false;
            for (final ArenaDialogKit.Pill pill : pills) {
                final boolean over = !onPill && pill.contains(x, y);
                onPill |= over;
                pill.setHover(over);
            }
            onPointer(x, y, onPill);
        }
        super.buildTouchListeners(screenX, screenY, listeners);
    }

    /** The pointer moved to x, y ({@code onButton} when it is over one of the buttons). */
    protected void onPointer(final float x, final float y, final boolean onButton) {
    }

    private ArenaDialogKit.Pill pillAt(final float x, final float y) {
        if (viewingBattlefield) {
            return btnView.contains(x, y) ? btnView : null;
        }
        for (final ArenaDialogKit.Pill pill : pills) {
            if (pill.contains(x, y)) {
                return pill;
            }
        }
        return null;
    }

    @Override
    public boolean press(final float x, final float y) {
        pointerDown = true;
        pressedPill = pillAt(x, y);
        if (pressedPill != null) {
            pressedPill.pressed = true;
            return true;
        }
        if (!viewingBattlefield) {
            onPress(x, y);
        }
        return true;
    }

    @Override
    public boolean release(final float x, final float y) {
        pointerDown = false;
        if (pressedPill != null) {
            pressedPill.pressed = false;
            pressedPill = null;
            return true;
        }
        if (!viewingBattlefield) {
            onRelease(x, y);
        }
        return true;
    }

    @Override
    public boolean tap(final float x, final float y, final int count) {
        final ArenaDialogKit.Pill pill = pillAt(x, y);
        if (pill != null) {
            pill.fire();
            return true;
        }
        if (!viewingBattlefield) {
            onTap(x, y, count);
        }
        return true;
    }

    @Override
    public boolean longPress(final float x, final float y) {
        if (!viewingBattlefield) {
            final CardView card = cardAt(x, y);
            if (card != null) {
                CardZoom.show(card);
            }
        }
        return true;
    }

    @Override
    public boolean pan(final float x, final float y, final float deltaX, final float deltaY, final boolean moreVertical) {
        if (viewingBattlefield) {
            return false;
        }
        if (pointerDown) {
            onDrag(x, y, deltaX, deltaY);
        } else { // the mouse wheel
            onScroll(x, y, deltaX, deltaY);
        }
        return true;
    }

    @Override
    public boolean panStop(final float x, final float y) {
        if (!viewingBattlefield && pointerDown) {
            onDragEnd(x, y);
        }
        return !viewingBattlefield;
    }

    @Override
    public boolean fling(final float velocityX, final float velocityY) {
        return false; // the arrow keys come as flings first; refusing them lets them through to keyDown
    }

    @Override
    public boolean zoom(final float x, final float y, final float amount) {
        return !viewingBattlefield;
    }

    @Override
    public boolean keyDown(final int keyCode) {
        switch (keyCode) {
            case Keys.ENTER:
            case Keys.NUMPAD_ENTER:
            case Keys.SPACE:
            case Keys.BUTTON_A:
                if (viewingBattlefield) {
                    toggleBattlefield();
                } else {
                    onSubmitKey();
                }
                return true;
            case Keys.ESCAPE:
            case Keys.BACK:
            case Keys.BUTTON_B:
                if (Forge.endKeyInput()) {
                    return true;
                }
                if (viewingBattlefield) {
                    toggleBattlefield();
                } else {
                    onEscapeKey();
                }
                return true;
            case Keys.LEFT: // also the gamepad's DPAD_LEFT (same key code)
                if (!viewingBattlefield) {
                    onArrow(-1, 0);
                }
                return true;
            case Keys.RIGHT: // also the gamepad's DPAD_RIGHT (same key code)
                if (!viewingBattlefield) {
                    onArrow(1, 0);
                }
                return true;
            case Keys.UP: // also the gamepad's DPAD_UP (same key code)
                if (!viewingBattlefield) {
                    onArrow(0, -1);
                }
                return true;
            case Keys.DOWN: // also the gamepad's DPAD_DOWN (same key code)
                if (!viewingBattlefield) {
                    onArrow(0, 1);
                }
                return true;
            case Keys.V:
                toggleBattlefield();
                return true;
            default:
                if (passesThrough(keyCode)) {
                    final FScreen screen = Forge.getCurrentScreen();
                    return screen != null && screen.keyDown(keyCode);
                }
                return true; // the board's shortcuts wait while a question is open
        }
    }

    /**
     * Whether a key the dialog does not use still goes to the match screen while the dialog is open: conceding
     * (Ctrl+Q) and the shortcut list (H), which both open their own dialog on top of this one. Everything else the
     * board does waits. Forge's own keys (the Android menu and back keys, the arrow keys' swipes) are handled by
     * {@code Forge}'s input processor before any overlay sees them, so they are not affected.
     */
    protected boolean passesThrough(final int keyCode) {
        final ArenaShortcuts.Action a = ArenaShortcuts.match(keyCode);
        return a == ArenaShortcuts.Action.CONCEDE || a == ArenaShortcuts.Action.SHOW_SHORTCUTS;
    }

    protected void onPress(final float x, final float y) {
    }

    protected void onRelease(final float x, final float y) {
    }

    protected void onTap(final float x, final float y, final int count) {
    }

    protected void onDrag(final float x, final float y, final float dx, final float dy) {
    }

    protected void onDragEnd(final float x, final float y) {
    }

    /** The wheel (or a two-finger scroll): dy is the board's pan distance, negative to scroll back. */
    protected void onScroll(final float x, final float y, final float dx, final float dy) {
    }

    protected void onArrow(final int dx, final int dy) {
    }

    protected abstract void onSubmitKey();

    protected abstract void onEscapeKey();

    /** The card under x, y if any, for a long press (or right click) to zoom. */
    protected CardView cardAt(final float x, final float y) {
        return null;
    }
}
