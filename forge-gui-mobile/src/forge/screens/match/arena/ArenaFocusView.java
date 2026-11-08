package forge.screens.match.arena;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Align;
import com.google.common.collect.Multiset;

import forge.Forge;
import forge.Graphics;
import forge.assets.FSkinFont;
import forge.assets.TextRenderer;
import forge.card.CardImageRenderer;
import forge.card.CardRenderer;
import forge.card.CardRenderer.CardStackPosition;
import forge.card.CardZoom;
import forge.card.mana.ManaCost;
import forge.game.GameView;
import forge.game.card.CardView;
import forge.game.card.CardView.CardStateView;
import forge.game.card.CounterType;
import forge.gui.card.CardDetailUtil;
import forge.item.IPaperCard;
import forge.screens.match.MatchController;
import forge.toolbox.FCardPanel;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FOverlay;

/**
 * The Arena look of the card zoom on the match screen: what {@link CardZoom} shows (a card, or a list of cards with
 * one of them current) laid over the dimmed board. The card stands large in the middle, lit in its own colour, with
 * its details on a glass panel beside it (name and cost, type, the rules text with everything granted to it, power
 * and toughness or loyalty, and what is true of it now: counters, tapped, sick, in combat, damage). A list has a strip
 * of its cards along the bottom and arrows at the sides: the wheel, the arrow keys, the arrows and the strip step
 * through it. The action the zoom offers for the card ("Cast", "Activate"...) and its other face are pills on the
 * panel. Esc, right click, Z or a click away from the card closes it; what it showed fades off the board.
 *
 * <p>{@link CardZoom} hands its shows over to this while {@link ArenaTheme#isMatchBoard()} is true; everything else
 * (the menus, the deck editors, adventure) keeps the default zoom.</p>
 */
public final class ArenaFocusView extends FOverlay {
    private static final ArenaFocusView instance = new ArenaFocusView();

    // layout, as shares of the window (or Swing pixels through ArenaTheme.px)
    private static final float TOP = 0.06f, CARD_MOST = 0.74f, STRIP_H = 0.12f, STRIP_GAP = 0.035f;
    private static final float PANEL_MOST = 0.26f, GAP = 0.016f, ARROW = 0.06f;
    private static final float PAD = 26f;
    private static final int OPEN_MS = 220, STEP_MS = 200, FLIP_MS = 300;

    private static final Color BACKDROP = ArenaTheme.rgb(0x040507);
    private static final Color THUMB_SHADE = ArenaTheme.rgba(0, 0, 0, 120);
    private static final Color CHIP_FILL = ArenaTheme.rgb(0x282C36);
    private static final Color PLATE_TOP = ArenaTheme.rgb(0xFBF8EF), PLATE_BOTTOM = ArenaTheme.rgb(0xE0DACC), PLATE_EDGE = ArenaTheme.rgb(0x040406);
    private static final Color STATS_TEXT = ArenaTheme.rgb(0x141210);
    private static final Color ARROW_TOP = ArenaTheme.rgb(0x1F1C18), ARROW_BOTTOM = ArenaTheme.rgb(0x0A0908);
    private static final Color S1 = new Color(), S2 = new Color(), S3 = new Color();

    private final List<CardView> cards = new ArrayList<>();
    private CardZoom.ActivateHandler handler;
    private int index, initialIndex;
    private boolean altState;
    private boolean closed = true;

    private final ArenaMotion.Fade entrance = new ArenaMotion.Fade(0f);
    /** Where the strip is scrolled to, in cards (eases after the current card). */
    private final ArenaMotion.Spring stripPos = new ArenaMotion.Spring(0f, 70, 0.002f);
    /** The current card arriving from one side when it changes: -1 or 1 down to 0. */
    private final ArenaMotion.Fade slide = new ArenaMotion.Fade(0f);
    /** Turning over to the other face: 0 to 1, the face changing half way. */
    private final ArenaMotion.Fade flip = new ArenaMotion.Fade(0f);
    private boolean flipSwapped;

    // layout of the window as last laid out
    private float lw, lh;
    private final Rectangle cardRect = new Rectangle(), panelRect = new Rectangle();
    private final Rectangle prevArrow = new Rectangle(), nextArrow = new Rectangle();
    private float thumbW, thumbH, thumbStep, stripY;
    private final Pill btnActivate = new Pill(true), btnFlip = new Pill(false), btnClose = new Pill(false);
    private final Pill[] pills = {btnActivate, btnFlip, btnClose};
    private Pill pressed;
    private boolean pointerDown;
    private float pointerX = -1, pointerY = -1, dragX;

    // details of the current card, worked out when it changes (not while drawing)
    private final Details details = new Details();

    private ArenaFocusView() {
        super(null); // draws its own backdrop
        btnActivate.action = this::activate;
        btnFlip.action = this::turnOver;
        btnClose.action = this::close;
        btnClose.text = Forge.getLocalizer().getMessageorUseDefault("lblClose", "Close");
    }

    // ---- opening and closing ------------------------------------------------------------------------------------------

    /**
     * Shows these items (cards, paper cards or map entries keyed by a card) with {@code index} current; {@code handler}
     * as CardZoom's activate handler (may be null), {@code otherFace} to start on the card's other face. Render
     * thread. Returns false (and shows nothing) when the items are not all cards, so the default zoom shows them.
     */
    public static boolean show(final List<?> items, final int index, final CardZoom.ActivateHandler handler, final boolean otherFace) {
        if (items == null || index < 0 || index >= items.size()) {
            return false;
        }
        final List<CardView> list = new ArrayList<>(items.size());
        for (final Object item : items) {
            final CardView cv = toCard(item);
            if (cv == null) {
                return false;
            }
            list.add(cv);
        }
        instance.open(list, index, handler, otherFace);
        return true;
    }

    /** Whether the focus view is up. */
    public static boolean isOpen() {
        return instance.isVisible() && !instance.closed;
    }

    /** Closes it if it is up (with the same fade as Esc). */
    public static void hideFocus() {
        if (isOpen()) {
            instance.close();
        }
    }

    private static CardView toCard(Object item) {
        if (item instanceof Map.Entry) {
            item = ((Map.Entry<?, ?>) item).getKey();
        }
        if (item instanceof CardView) {
            return (CardView) item;
        }
        if (item instanceof IPaperCard) {
            return CardView.getCardForUi((IPaperCard) item);
        }
        return null;
    }

    private void open(final List<CardView> list, final int index0, final CardZoom.ActivateHandler handler0, final boolean otherFace) {
        cards.clear();
        cards.addAll(list);
        handler = handler0;
        index = index0;
        initialIndex = index0;
        altState = otherFace && cards.get(index).getAlternateState() != null;
        flip.set(0f);
        slide.set(0f);
        stripPos.set(index);
        pressed = null;
        pointerDown = false;
        final boolean wasOpen = isVisible() && !closed;
        closed = false;
        lw = -1; // lay out again for the new cards
        onCardChanged();
        if (!wasOpen) {
            entrance.set(0f);
            entrance.to(1f, OPEN_MS, 0, ArenaMotion.OUT, null);
            show();
        }
        ArenaMotion.requestFrame();
    }

    /** Takes it down; what it showed fades off the board. Tells the list which card was last current, as CardZoom does. */
    private void close() {
        if (closed) {
            return;
        }
        closed = true;
        ArenaCurtain.fadeOver(ArenaMotion.CLOSE_MS);
        hide();
        if (handler != null && index != initialIndex) {
            handler.setSelectedIndex(index);
        }
        handler = null;
        cards.clear();
    }

    @Override
    public void setVisible(final boolean visible0) {
        super.setVisible(visible0);
        if (!visible0) {
            closed = true;
        }
    }

    private void activate() {
        if (handler == null || details.action == null) {
            return;
        }
        final CardZoom.ActivateHandler h = handler;
        final int i = index;
        close();
        h.activate(i);
    }

    private void turnOver() {
        if (flip.isMoving() || !canTurnOver()) {
            return;
        }
        final CardView cv = cards.get(index);
        if (cv.isFaceDown() && cv.getBackup() != null && !altState) {
            // a face-down card shows what it really is when it may be seen, as CardZoom does
            final CardView backup = cv.getBackup();
            if (backup.isDoubleFacedCard() || backup.isFlipCard() || backup.hasSecondaryState()) {
                cards.set(index, backup);
                onCardChanged();
                return;
            }
        }
        flipSwapped = false;
        flip.set(0f);
        flip.to(1f, FLIP_MS, 0, ArenaMotion.IN_OUT, () -> flip.set(0f));
    }

    private boolean canTurnOver() {
        final CardView cv = cards.get(index);
        return MatchController.instance.mayFlip(cv) || cv.isFaceDown() && cv.getBackup() != null;
    }

    private void step(final int dir) {
        final int next = Math.max(0, Math.min(cards.size() - 1, index + dir));
        if (next == index) {
            return;
        }
        goTo(next);
    }

    private void goTo(final int next) {
        if (next == index || next < 0 || next >= cards.size()) {
            return;
        }
        slide.set(next > index ? 1f : -1f);
        slide.to(0f, STEP_MS, 0, ArenaMotion.OUT, null);
        index = next;
        altState = false;
        flip.set(0f);
        stripPos.to(index);
        onCardChanged();
    }

    private void onCardChanged() {
        details.forCard(cards.get(index), altState, handler == null ? null : handler.getActivateAction(index));
        btnActivate.visible = details.action != null;
        btnActivate.text = details.action == null ? "" : details.action;
        btnFlip.visible = canTurnOver();
        btnFlip.text = Forge.getLocalizer().getMessageorUseDefault("lblFlip", "Flip");
        lw = -1; // the panel's rows depend on the card
        ArenaMotion.requestFrame();
    }

    // ---- layout -------------------------------------------------------------------------------------------------------

    @Override
    protected void doLayout(final float width, final float height) {
        lw = -1;
    }

    private void layout(final float w, final float h) {
        if (lw == w && lh == h) {
            return;
        }
        lw = w;
        lh = h;
        final boolean strip = cards.size() > 1;
        thumbH = strip ? Math.round(h * STRIP_H) : 0f;
        thumbW = Math.round(thumbH / FCardPanel.ASPECT_RATIO);
        thumbStep = thumbW + Math.max(4f, ArenaTheme.px(12));
        stripY = strip ? h - thumbH - Math.round(h * STRIP_GAP) : h;
        final float top = Math.round(h * TOP);
        final float bottom = strip ? stripY - Math.round(h * STRIP_GAP) : h - top;
        final float cardH = Math.min(Math.round(h * CARD_MOST), bottom - top);
        final float cardW = Math.round(cardH / FCardPanel.ASPECT_RATIO);
        final float gap = Math.round(w * GAP);
        final float panelW = Math.round(Math.min(w * PANEL_MOST, cardW));
        final float block = cardW + gap + panelW;
        final float x = Math.round((w - block) / 2f);
        final float y = Math.round(top + (bottom - top - cardH) / 2f);
        cardRect.set(x, y, cardW, cardH);
        panelRect.set(x + cardW + gap, y, panelW, cardH);
        final float d = Math.round(h * ARROW);
        prevArrow.set(x - gap - d, y + cardH / 2f - d / 2f, d, d);
        nextArrow.set(panelRect.x + panelW + gap, y + cardH / 2f - d / 2f, d, d);
        // the pills along the foot of the panel, all one size
        final float pad = ArenaTheme.px(PAD), bh = Math.round(Math.max(ArenaTheme.px(40), h * 0.05f));
        details.layout(panelRect, h, bh);
        // the panel is as tall as what it says (at least 40% of the card, at most all of it), from the card's top
        final float panelH = Math.round(Math.max(cardH * 0.4f, Math.min(cardH, details.contentHeight)));
        panelRect.height = panelH;
        int shown = 0;
        for (final Pill p : pills) {
            if (p.visible) {
                shown++;
            }
        }
        final float room = panelW - 2 * pad, bgap = ArenaTheme.px(10);
        final float bw = shown == 0 ? 0 : (room - (shown - 1) * bgap) / shown;
        float bx = panelRect.x + pad;
        for (final Pill p : pills) {
            if (p.visible) {
                p.bounds.set(Math.round(bx), panelRect.y + panelH - pad - bh, Math.round(bw), bh);
                bx += bw + bgap;
            } else {
                p.bounds.set(0, 0, 0, 0);
            }
        }
        details.bottom = panelRect.y + panelH - pad - bh - ArenaTheme.px(14);
    }

    // ---- drawing ------------------------------------------------------------------------------------------------------

    @Override
    public void draw(final Graphics g) {
        final float w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0 || cards.isEmpty()) {
            return;
        }
        layout(w, h);
        // the face changes half way through turning over
        if (flip.isMoving() && !flipSwapped && flip.get() >= 0.5f) {
            flipSwapped = true;
            altState = !altState;
            details.forCard(cards.get(index), altState, handler == null ? null : handler.getActivateAction(index));
            lw = -1;
            layout(w, h);
        }
        final float e = entrance.get();
        g.fillRect(ArenaTheme.alpha(BACKDROP, 0.72f * e, S1), 0, 0, w, h);
        ArenaArt.vignette(g, 0, 0, w, h, Math.min(w, h) * 0.34f, 0.45f * e);
        final float rise = Math.round((1f - e) * h * 0.02f);
        final float old = g.getfloatAlphaComposite();
        if (e < 1f) {
            g.setAlphaComposite(old * e);
        }
        try {
            drawCard(g, w, h, rise);
            drawPanel(g, rise);
            drawArrows(g, rise);
            drawStrip(g, w, rise);
        } finally {
            if (e < 1f) {
                g.setAlphaComposite(old);
            }
        }
    }

    private void drawCard(final Graphics g, final float w, final float h, final float rise) {
        final CardView cv = cards.get(index);
        final float s = slide.get();
        final float x = cardRect.x + s * cardRect.width * 0.25f, y = cardRect.y + rise;
        final float cw = cardRect.width, ch = cardRect.height, r = cw * 0.035f;
        final float turn = flip.isMoving() ? Math.max(0.02f, Math.abs((float) Math.cos(flip.get() * Math.PI))) : 1f;
        final float alpha = 1f - Math.abs(s) * 0.6f;
        final float old = g.getfloatAlphaComposite();
        g.setAlphaComposite(old * alpha);
        // a cast shadow, and the light of its own colour round it
        g.drawSoftShadow(ArenaTheme.alpha(ArenaTheme.BLACK, 0.85f, S1), x - ch * 0.016f, y + ch * 0.022f, cw, ch, r, ch * 0.05f);
        final CardStateView state = cv.getState(altState);
        final Color tint = state == null || !MatchController.instance.mayView(cv) ? ArenaTheme.GOLD : ArenaTheme.frameLine(ArenaTheme.tileTint(state));
        g.drawGlow(ArenaTheme.alpha(tint, 0.55f, S1), x, y, cw, ch, r, ch * 0.045f);
        final float fw = cw * turn, fx = x + (cw - fw) / 2f;
        final GameView gameView = MatchController.instance.getGameView();
        CardImageRenderer.drawZoom(g, cv, gameView, altState, fx, y, fw, ch, w, h, false);
        g.setAlphaComposite(old);
    }

    private void drawPanel(final Graphics g, final float rise) {
        final float x = panelRect.x, y = panelRect.y + rise, w = panelRect.width, h = panelRect.height;
        ArenaArt.shadow(g, x, y, w, h, ArenaTheme.px(14), ArenaTheme.px(18), 0.55f, ArenaTheme.px(6));
        ArenaArt.glassPanel(g, x, y, w, h, ArenaTheme.px(14));
        details.draw(g, x, y, rise);
        for (final Pill p : pills) {
            p.draw(g, rise);
        }
    }

    private void drawArrows(final Graphics g, final float rise) {
        if (cards.size() < 2) {
            return;
        }
        if (index > 0) {
            arrow(g, prevArrow, -1, rise);
        }
        if (index < cards.size() - 1) {
            arrow(g, nextArrow, 1, rise);
        }
    }

    /** A round dark button in a gold rim with a white chevron (the Arena round button). */
    private void arrow(final Graphics g, final Rectangle r, final int dir, final float rise) {
        final boolean lit = r.contains(pointerX, pointerY);
        final float d = r.width, cx = r.x + d / 2f, cy = r.y + d / 2f + rise, rad = d / 2f;
        g.drawSoftShadow(ArenaTheme.alpha(ArenaTheme.BLACK, 0.5f, S1), cx - rad, cy - rad + ArenaTheme.px(2), d, d, rad, ArenaTheme.px(6));
        g.fillSmoothRoundRectGradient(lit ? ArenaTheme.SLATE : ARROW_TOP, lit ? ArenaTheme.NAVY : ARROW_BOTTOM, true, cx - rad, cy - rad, d, d, rad);
        g.drawArenaShape(Graphics.SHAPE_RING, cx - rad, cy - rad, d, d, rad, Math.max(2f, d * 0.05f),
                lit ? ArenaTheme.WHITE : ArenaTheme.GOLD_BRIGHT, ArenaTheme.GOLD, true);
        final float leg = d * 0.2f, t = Math.max(3f, d * 0.1f);
        chevron(g, cx + dir * leg * 0.15f, cy, leg, t, ArenaTheme.WHITE, dir);
    }

    private static void chevron(final Graphics g, final float cx, final float cy, final float leg, final float t, final Color c, final int dir) {
        stroke(g, cx - dir * leg * 0.5f, cy - leg, cx + dir * leg * 0.5f, cy, t, c);
        stroke(g, cx + dir * leg * 0.5f, cy, cx - dir * leg * 0.5f, cy + leg, t, c);
    }

    /** A round-capped bar from (x1, y1) to (x2, y2). */
    private static void stroke(final Graphics g, final float x1, final float y1, final float x2, final float y2, final float t, final Color c) {
        final float dx = x2 - x1, dy = y2 - y1, len = (float) Math.sqrt(dx * dx + dy * dy);
        final float mx = (x1 + x2) / 2f, my = (y1 + y2) / 2f;
        g.startRotateTransform(mx, my, -(float) Math.toDegrees(Math.atan2(dy, dx)));
        g.fillSmoothRoundRect(c, mx - len / 2f - t / 2f, my - t / 2f, len + t, t, t / 2f);
        g.endTransform();
    }

    private void drawStrip(final Graphics g, final float w, final float rise) {
        if (cards.size() < 2 || thumbH <= 0) {
            return;
        }
        final float pos = stripPos.get();
        final int first = Math.max(0, (int) Math.floor(pos - w / thumbStep / 2f) - 1);
        final int last = Math.min(cards.size() - 1, (int) Math.ceil(pos + w / thumbStep / 2f) + 1);
        final float y = stripY + rise, r = thumbW * 0.045f;
        for (int i = first; i <= last; i++) {
            final float x = thumbX(i, w, pos);
            if (x + thumbW < 0 || x > w) {
                continue;
            }
            final boolean current = i == index;
            final boolean over = !current && pointerY >= stripY && pointerY <= stripY + thumbH && pointerX >= x && pointerX <= x + thumbW;
            final float lift = current ? Math.round(thumbH * 0.08f) : over ? Math.round(thumbH * 0.04f) : 0f;
            g.drawSoftShadow(ArenaTheme.alpha(ArenaTheme.BLACK, 0.7f, S1), x, y - lift + ArenaTheme.px(3), thumbW, thumbH, r, ArenaTheme.px(8));
            if (current) {
                g.drawGlow(ArenaTheme.alpha(ArenaTheme.GOLD_BRIGHT, 0.85f, S1), x, y - lift, thumbW, thumbH, r, ArenaTheme.px(10));
            }
            CardRenderer.drawCard(g, cards.get(i), x, y - lift, thumbW, thumbH, CardStackPosition.Top);
            if (current) {
                g.drawSmoothRoundRect(Math.max(2f, ArenaTheme.px(2.5f)), ArenaTheme.GOLD_BRIGHT, x, y - lift, thumbW, thumbH, r);
            } else if (!over) {
                g.fillSmoothRoundRect(THUMB_SHADE, x, y - lift, thumbW, thumbH, r);
            }
        }
    }

    private float thumbX(final int i, final float w, final float pos) {
        return Math.round(w / 2f + (i - pos) * thumbStep - thumbW / 2f);
    }

    /** The strip's card at x, y, or -1. */
    private int thumbAt(final float x, final float y) {
        if (cards.size() < 2 || y < stripY - thumbH * 0.1f || y > stripY + thumbH) {
            return -1;
        }
        final float pos = stripPos.get();
        final int i = Math.round((x - lw / 2f) / thumbStep + pos);
        if (i < 0 || i >= cards.size()) {
            return -1;
        }
        final float tx = thumbX(i, lw, pos);
        return x >= tx && x <= tx + thumbW ? i : -1;
    }

    // ---- input --------------------------------------------------------------------------------------------------------

    @Override
    public void buildTouchListeners(final float screenX, final float screenY, final List<FDisplayObject> listeners) {
        if (!isVisible()) {
            return;
        }
        final float x = screenToLocalX(screenX), y = screenToLocalY(screenY);
        if (x != pointerX || y != pointerY) {
            pointerX = x;
            pointerY = y;
            for (final Pill p : pills) {
                p.setHover(p.contains(x, y));
            }
            ArenaMotion.requestFrame();
        }
        super.buildTouchListeners(screenX, screenY, listeners);
    }

    private Pill pillAt(final float x, final float y) {
        for (final Pill p : pills) {
            if (p.contains(x, y)) {
                return p;
            }
        }
        return null;
    }

    @Override
    public boolean press(final float x, final float y) {
        pointerDown = true;
        dragX = 0f;
        pressed = pillAt(x, y);
        if (pressed != null) {
            pressed.pressed = true;
        }
        return true;
    }

    @Override
    public boolean release(final float x, final float y) {
        pointerDown = false;
        if (pressed != null) {
            pressed.pressed = false;
            pressed = null;
        }
        return true;
    }

    @Override
    public boolean tap(final float x, final float y, final int count) {
        if (closed) {
            return true;
        }
        if (Forge.mouseButtonID == Input.Buttons.RIGHT) {
            close();
            return true;
        }
        final Pill p = pillAt(x, y);
        if (p != null) {
            p.fire();
            return true;
        }
        if (cards.size() > 1) {
            if (index > 0 && prevArrow.contains(x, y)) {
                step(-1);
                return true;
            }
            if (index < cards.size() - 1 && nextArrow.contains(x, y)) {
                step(1);
                return true;
            }
            final int t = thumbAt(x, y);
            if (t >= 0) {
                goTo(t);
                return true;
            }
        }
        if (panelRect.contains(x, y)) {
            return true; // reading the panel is not a reason to close
        }
        close(); // the card itself, or anywhere away from it
        return true;
    }

    @Override
    public boolean longPress(final float x, final float y) {
        return true;
    }

    @Override
    public boolean pan(final float x, final float y, final float deltaX, final float deltaY, final boolean moreVertical) {
        if (closed || cards.size() < 2) {
            return true;
        }
        if (!pointerDown) {
            // the mouse wheel: down steps on, up steps back
            if (deltaY != 0f) {
                step(deltaY < 0f ? 1 : -1);
            } else if (deltaX != 0f) {
                step(deltaX < 0f ? 1 : -1);
            }
            return true;
        }
        // a drag sideways steps once it has gone far enough
        dragX += deltaX;
        final float reach = Math.max(ArenaTheme.px(60), cardRect.width * 0.18f);
        if (Math.abs(dragX) > reach) {
            step(dragX < 0f ? 1 : -1);
            dragX = 0f;
        }
        return true;
    }

    @Override
    public boolean panStop(final float x, final float y) {
        dragX = 0f;
        return true;
    }

    @Override
    public boolean fling(final float velocityX, final float velocityY) {
        if (closed) {
            return true;
        }
        // the arrow keys come as flings: left and right step through the list, up and down do nothing here
        if (Math.abs(velocityX) > Math.abs(velocityY)) {
            if (!pointerDown || Math.abs(dragX) < 1f) {
                step(velocityX > 0 ? -1 : 1);
            }
            return true;
        }
        return true;
    }

    @Override
    public boolean zoom(final float x, final float y, final float amount) {
        return true;
    }

    @Override
    public boolean keyDown(final int keyCode) {
        if (closed) {
            return false;
        }
        switch (keyCode) {
            case Keys.ESCAPE:
            case Keys.BACK:
            case Keys.BUTTON_B:
            case Keys.Z:
                if (keyCode == Keys.ESCAPE && Forge.endKeyInput()) {
                    return true;
                }
                close();
                return true;
            case Keys.ENTER:
            case Keys.NUMPAD_ENTER:
            case Keys.SPACE:
            case Keys.BUTTON_A:
                if (btnActivate.visible) {
                    activate();
                } else {
                    close();
                }
                return true;
            case Keys.F:
            case Keys.BUTTON_X:
                turnOver();
                return true;
            case Keys.LEFT:
            case Keys.PAGE_UP:
                step(-1);
                return true;
            case Keys.RIGHT:
            case Keys.PAGE_DOWN:
                step(1);
                return true;
            case Keys.HOME:
                goTo(0);
                return true;
            case Keys.END:
                goTo(cards.size() - 1);
                return true;
            default:
                return true; // the board waits while a card is being looked at
        }
    }

    // ---- the details panel --------------------------------------------------------------------------------------------

    /** What the panel says about the current card, worked out when it changes, and where it goes. */
    private static final class Details {
        private final TextRenderer costText = new TextRenderer(), bodyText = new TextRenderer(), statsText = new TextRenderer();
        String name, cost, type, body, stats, action;
        final List<String> chips = new ArrayList<>();
        float[] chipW = new float[8];
        FSkinFont nameFont, typeFont, bodyFont, chipFont, statsFont;
        float costW, chipH, chipsH, statsW, statsH;
        float bottom; // the lowest y the text may reach (above the pills), set by the view's layout
        private float laidW = -1, laidH = -1;

        void forCard(final CardView cv, final boolean alt, final String action0) {
            final boolean canShow = MatchController.instance.mayView(cv);
            final CardStateView state = cv.getState(alt);
            name = CardDetailUtil.formatCardName(cv, canShow, alt);
            final ManaCost mc = state == null ? null : state.getManaCost();
            cost = canShow && mc != null && !mc.isNoCost() ? mc.toString() : "";
            type = state == null ? "" : CardDetailUtil.formatCardType(state, canShow);
            String text = null;
            try {
                text = state == null ? null : CardDetailUtil.composeCardText(state, MatchController.instance.getGameView(), canShow);
            } catch (final RuntimeException e) {
                // a view the details cannot be worked out for: the picture says it
            }
            body = text == null ? "" : text.trim();
            stats = state == null ? "" : CardDetailUtil.formatPrimaryCharacteristic(state, canShow);
            action = action0 == null || action0.isEmpty() ? null : action0;
            chips.clear();
            if (cv.getZone() != null) {
                final Multiset<CounterType> counters = cv.getCounters();
                if (counters != null) {
                    for (final Multiset.Entry<CounterType> e : counters.entrySet()) {
                        chips.add(e.getCount() + "x " + e.getElement().getCounterOnCardDisplayName());
                    }
                }
                if (cv.isFaceDown()) { chips.add(Forge.getLocalizer().getMessage("lblArenaFaceDown")); }
                if (cv.isTapped()) { chips.add(Forge.getLocalizer().getMessage("lblArenaTapped")); }
                if (cv.isSick()) { chips.add(Forge.getLocalizer().getMessage("lblArenaSummoningSick")); }
                if (cv.isAttacking()) { chips.add(Forge.getLocalizer().getMessage("lblArenaAttacking")); }
                if (cv.isBlocking()) { chips.add(Forge.getLocalizer().getMessage("lblArenaBlocking")); }
                if (cv.isPhasedOut()) { chips.add(Forge.getLocalizer().getMessage("lblArenaPhasedOut")); }
                if (cv.getDamage() > 0) { chips.add(Forge.getLocalizer().getMessage("lblArenaDamage", cv.getDamage())); }
            }
            if (chipW.length < chips.size()) {
                chipW = new float[chips.size()];
            }
            laidW = -1;
        }

        /**
         * Fonts and the room things take for a panel this wide and at most this tall, with pills {@code pillH} high along
         * its foot; sets {@link #contentHeight} (allocates: layout only).
         */
        void layout(final Rectangle panel, final float h, final float pillH) {
            if (laidW == panel.width && laidH == panel.height) {
                return;
            }
            laidW = panel.width;
            laidH = panel.height;
            final float pad = ArenaTheme.px(PAD), room = panel.width - 2 * pad;
            nameFont = ArenaTheme.fontPx(Math.min(72, Math.round(h * 0.032f)));
            typeFont = ArenaTheme.fontPx(Math.round(h * 0.02f));
            chipFont = ArenaTheme.fontPx(Math.round(h * 0.016f));
            statsFont = ArenaTheme.fontPx(Math.min(72, Math.round(h * 0.03f)));
            costW = cost.isEmpty() ? 0f : costText.getBounds(cost, nameFont).width;
            chipH = chipFont.getLineHeight() + ArenaTheme.px(6);
            int rows = 0;
            float cx = 0f;
            for (int i = 0; i < chips.size(); i++) {
                chipW[i] = ArenaArt.textWidth(chips.get(i), chipFont) + ArenaTheme.px(18);
                if (rows == 0 || cx + chipW[i] > room) {
                    rows++;
                    cx = 0f;
                }
                cx += chipW[i] + ArenaTheme.px(6);
            }
            chipsH = rows == 0 ? 0f : rows * (chipH + ArenaTheme.px(6));
            statsH = stats.isEmpty() ? 0f : statsFont.getLineHeight() + ArenaTheme.px(10);
            statsW = stats.isEmpty() ? 0f : statsText.getBounds(stats, statsFont).width + statsH * 1.1f;
            // the largest text that fits what is left
            final float textTop = pad + nameFont.getLineHeight() + typeFont.getLineHeight() + ArenaTheme.px(22);
            final float lower = chipsH + (statsH > 0 ? statsH + ArenaTheme.px(10) : 0f) + pillH + ArenaTheme.px(14) + pad;
            final float textRoom = Math.max(0f, panel.height - textTop - lower - ArenaTheme.px(10));
            bodyFont = ArenaTheme.fontPx(Math.round(h * 0.013f));
            bodyH = 0f;
            if (!body.isEmpty()) {
                final int[] sizes = {Math.round(h * 0.02f), Math.round(h * 0.018f), Math.round(h * 0.016f), Math.round(h * 0.0145f), Math.round(h * 0.013f)};
                for (final int size : sizes) {
                    final FSkinFont f = ArenaTheme.fontPx(size);
                    bodyH = bodyText.getWrappedBounds(body, f, room).height;
                    bodyFont = f;
                    if (bodyH <= textRoom) {
                        break;
                    }
                }
                bodyH = Math.min(bodyH, textRoom);
            }
            contentHeight = textTop + (bodyH > 0 ? bodyH + ArenaTheme.px(10) : 0f) + lower;
        }

        /** How tall the panel wants to be for what it says (set by layout). */
        float contentHeight;
        private float bodyH;

        void draw(final Graphics g, final float px, final float py, final float rise) {
            final float pad = ArenaTheme.px(PAD), room = laidW - 2 * pad;
            float y = py + pad;
            // name and cost
            final float nameH = nameFont.getLineHeight();
            final float nameRoom = room - (costW > 0 ? costW + ArenaTheme.px(10) : 0f);
            ArenaArt.textShadowed(g, name, nameFont, ArenaTheme.WHITE, px + pad, y, nameRoom, nameH, Align.left);
            if (costW > 0) {
                costText.drawText(g, cost, nameFont, ArenaTheme.WHITE, px + pad + room - costW, y, costW, nameH, y, nameH, false, Align.left, true);
            }
            y += nameH;
            final float typeH = typeFont.getLineHeight();
            ArenaArt.text(g, type, typeFont, ArenaTheme.TEXT_DIM, px + pad, y, room, typeH, Align.left);
            y += typeH + ArenaTheme.px(10);
            ArenaArt.divider(g, px + pad, y, room, ArenaTheme.alpha(ArenaTheme.GOLD, 0.7f, S2), Math.max(1f, ArenaTheme.px(1.5f)));
            y += ArenaTheme.px(12);
            // the lower blocks stand on the pills: stats, then chips above them
            float foot = bottom + rise;
            if (statsH > 0) {
                foot -= statsH;
                final float sx = px + pad, sw = statsW;
                g.fillSmoothRoundRect(PLATE_EDGE, sx - 1.5f, foot - 1.5f, sw + 3f, statsH + 3f, (statsH + 3f) / 2f);
                g.fillSmoothRoundRectGradient(PLATE_TOP, PLATE_BOTTOM, true, sx, foot, sw, statsH, statsH / 2f);
                statsText.drawText(g, stats, statsFont, STATS_TEXT, sx, foot, sw, statsH, foot, statsH, false, Align.center, true);
                foot -= ArenaTheme.px(10);
            }
            if (chipsH > 0) {
                foot -= chipsH;
                float cx = px + pad, cy = foot;
                boolean first = true;
                for (int i = 0; i < chips.size(); i++) {
                    if (!first && cx + chipW[i] > px + pad + room) {
                        cx = px + pad;
                        cy += chipH + ArenaTheme.px(6);
                    }
                    first = false;
                    g.fillSmoothRoundRect(CHIP_FILL, cx, cy, chipW[i], chipH, chipH / 2f);
                    g.drawSmoothRoundRect(1f, ArenaTheme.alpha(ArenaTheme.WHITE, 0.16f, S3), cx, cy, chipW[i], chipH, chipH / 2f);
                    ArenaArt.text(g, chips.get(i), chipFont, ArenaTheme.WHITE, cx, cy, chipW[i], chipH, Align.center);
                    cx += chipW[i] + ArenaTheme.px(6);
                }
            }
            // the rules text takes what is left between
            final float textBottom = foot - ArenaTheme.px(10);
            if (!body.isEmpty() && textBottom > y) {
                g.startClip(px + pad, y, room, textBottom - y);
                bodyText.drawText(g, body, bodyFont, ArenaTheme.TEXT, px + pad, y, room, textBottom - y, y, textBottom - y, true, Align.left, false);
                g.endClip();
            }
        }
    }

    // ---- buttons ------------------------------------------------------------------------------------------------------

    /** One of the panel's buttons: the Arena pill, primary (molten orange) or plain. */
    private static final class Pill {
        final Rectangle bounds = new Rectangle();
        final boolean primary;
        String text = "";
        boolean visible, hover, pressed;
        Runnable action;
        private final ArenaMotion.Fade lit = new ArenaMotion.Fade(0f);

        Pill(final boolean primary0) {
            primary = primary0;
            visible = !primary0;
        }

        boolean contains(final float x, final float y) {
            return visible && bounds.width > 0 && bounds.contains(x, y);
        }

        void setHover(final boolean b) {
            if (b != hover) {
                hover = b;
                lit.to(b ? 1f : 0f, b ? 110 : 180);
            }
        }

        void fire() {
            if (visible && action != null) {
                action.run();
            }
        }

        void draw(final Graphics g, final float rise) {
            if (!visible || bounds.width <= 0) {
                return;
            }
            final float h = bounds.height;
            final FSkinFont f = ArenaTheme.fontPx(Math.round(Math.min(h * 0.42f, 40f)));
            ArenaArt.button(g, text, f, bounds.x, bounds.y + rise, bounds.width, h, primary, lit.get() > 0.5f, pressed, false, false, true);
        }
    }
}
