package forge.screens.match.arena;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;

import forge.Forge;
import forge.Forge.KeyInputAdapter;
import forge.Graphics;
import forge.game.card.CardView;
import forge.game.player.PlayerView;
import forge.game.zone.ZoneType;
import forge.gamemodes.match.Selection;
import forge.gui.FThreads;
import forge.interfaces.IGameController;
import forge.screens.match.MatchController;
import forge.screens.match.MatchScreen;
import forge.screens.match.views.VPrompt;
import forge.toolbox.FButton;
import forge.toolbox.FCardPanel;
import forge.util.ThreadUtil;

/**
 * The Arena card browser (port of the Swing {@code ArenaCardBrowser}): a zone looked through from its pile, cards to
 * pick from a list, cards that are only being revealed, the cards the game's current input lets the player pick from
 * hidden zones, and cards to put in order. A fanned row of real card faces over the dimmed board: a few degrees of tilt
 * and a slight arc, the rightmost on top, a cyan rim on what can be picked and a gold one on what has been, the card
 * under the pointer lifted. It scrolls with the wheel, the arrow keys or its chevrons when the row is too long; right
 * click (or a long press) zooms a card. Like the other Arena dialogs it can step aside with "View Battlefield".
 *
 * <p>Render thread only. The openers return false, opening nothing, when the Arena look is not on the match screen,
 * so that the caller can fall back to its classic dialog.</p>
 */
public final class ArenaCardBrowser extends ArenaDialogOverlay {
    private static final float CARD_HEIGHT = 0.38f;
    /** The furthest a row of cards reaches across the window before it scrolls instead. */
    private static final float FAN_CAP = 0.91f;
    private static final float MIN_CARD_HEIGHT = 0.34f;
    /** The least of a card that stays in view in a row, as a share of its width (beside the prompt's card: less). */
    private static final float MIN_SHOWN = 0.6f;
    private static final float MIN_SHOWN_SOURCE = 0.4f;
    private static final float ARRANGE_HEIGHT = 0.38f;
    private static final float FAN_ANGLE = 4f;
    private static final float FAN_ARC = 0.02f;
    private static final float HOVER_LIFT = 0.045f;
    private static final float CHOSEN_SCALE = 1.05f;
    private static final float HOVER_SCALE = 1.05f;

    /** What the browser is for. */
    private enum Mode {
        /** pick cards, then hand the picks back to the caller */
        CHOOSE,
        /** nothing to pick; the cards are only being shown */
        REVEAL,
        /** look through a zone; a click plays a card that can be played from there */
        ZONE,
        /** the game's current input is picking cards from hidden zones; clicks and Submit go to it */
        SELECTION,
        /** put cards in order by dragging them along one row */
        ARRANGE
    }

    private static ArenaCardBrowser selectionBrowser;
    private static final Color S1 = new Color(), S2 = new Color();
    private static final Rectangle SOURCE = new Rectangle();

    private final Mode mode;
    private final String title;
    private final String subtitle;
    private final int min;
    private final int max;
    private final List<CardView> cards;
    private final List<CardView> chosen = new ArrayList<>();
    private final List<Item> items = new ArrayList<>();
    private final List<Item> drawOrder = new ArrayList<>();
    private final CardView focusCard;
    private final ArenaDialogKit.Pill btnSubmit = new ArenaDialogKit.Pill();
    private final ArenaDialogKit.Pill btnSecondary = new ArenaDialogKit.Pill();
    private Runnable whenClosed;
    private CardView played;
    private String topLabel; // ARRANGE: what the left end of the row means ("Closest to top"...)
    private Item focus; // the card the arrow keys are on

    // the row
    private float cw, ch, maxAngle, rowStep;
    private boolean rowScrolls;
    private float viewX, viewW, viewY, viewH;
    private float fanLeft, fanRight, fanFoot;
    private final ArenaMotion.Spring scroll = new ArenaMotion.Spring(0f, 60, 0.5f);
    private float maxScroll;
    // ARRANGE
    private Item dragItem;
    private float dragGrabX, dragGrabY, dragDownX, dragDownY;
    private boolean dragMoved;
    private float railLeft, railRight, railY, captionY, firstBadge, lastBadge;

    // ---- opening ----------------------------------------------------------------------------------------------------

    /**
     * Shows a player's zone, newest card first (from its pile on the board); a click on a card that can be played from
     * there closes the browser and plays it, as a click on the zone's card would. Render thread. False if the Arena
     * browser cannot open (Arena look off, no match): then show the zone the classic way.
     */
    public static boolean browseZone(final PlayerView player, final ZoneType zone) {
        if (!ArenaTheme.isMatchBoard() || player == null || zone == null) {
            return false;
        }
        final List<CardView> cards = new ArrayList<>();
        final Iterable<CardView> in = player.getCards(zone);
        if (in != null) {
            for (final CardView c : in) {
                cards.add(0, c);
            }
        }
        final String name = zone.getTranslatedName();
        final String title = name == null || name.isEmpty() ? "" : Character.toUpperCase(name.charAt(0)) + name.substring(1);
        final String subtitle = Forge.getLocalizer().getMessage("lblArenaZoneSubtitle", player.getName(), cards.size());
        final ArenaCardBrowser browser = new ArenaCardBrowser(Mode.ZONE, title, subtitle, cards, 0, 0, null);
        browser.whenClosed = () -> {
            final CardView card = browser.played;
            if (card != null) {
                ThreadUtil.invokeInGameThread(() -> controller().selectCard(card, null, null));
            }
        };
        browser.openOverlay();
        return true;
    }

    /** Lets the player pick between min and max of {@code cards}; tells {@code done} the picks, in the order picked. */
    public static boolean choose(final String title, final String subtitle, final int min, final int max, final List<CardView> cards,
            final Collection<CardView> selected, final Consumer<List<CardView>> done) {
        if (!ArenaTheme.isMatchBoard() || cards == null || cards.isEmpty() || max <= 0) {
            return false;
        }
        final ArenaCardBrowser browser = new ArenaCardBrowser(Mode.CHOOSE, title, subtitle, cards, min, max, selected);
        browser.whenClosed = () -> done.accept(new ArrayList<>(browser.chosen));
        browser.openOverlay();
        return true;
    }

    /** Shows cards that are only being revealed: nothing to pick, just Done. */
    public static boolean reveal(final String message, final List<CardView> cards, final Runnable done) {
        if (!ArenaTheme.isMatchBoard() || cards == null || cards.isEmpty()) {
            return false;
        }
        final ArenaCardBrowser browser = new ArenaCardBrowser(Mode.REVEAL, Forge.getLocalizer().getMessage("lblRevealed"), message, cards, 0, 0, null);
        browser.whenClosed = done;
        browser.openOverlay();
        return true;
    }

    /**
     * Lets the player drag {@code cards} into order along one row, the leftmost being {@code caption} ("Closest to
     * top"...); tells {@code done} the cards left to right.
     */
    public static boolean order(final String title, final String caption, final List<CardView> cards, final Consumer<List<CardView>> done) {
        if (!ArenaTheme.isMatchBoard() || cards == null || cards.isEmpty()) {
            return false;
        }
        final ArenaCardBrowser browser = new ArenaCardBrowser(Mode.ARRANGE, title, Forge.getLocalizer().getMessage("lblArenaDragToReorder"), cards, 0, 0, null);
        browser.topLabel = caption == null || caption.isEmpty() ? null : caption;
        browser.whenClosed = () -> {
            final List<CardView> out = new ArrayList<>();
            for (final Item item : browser.items) {
                out.add(item.card);
            }
            done.accept(out);
        };
        browser.openOverlay();
        return true;
    }

    /**
     * Follows the game's current input (MatchController.setSelection): when it lets the player pick cards from zones
     * the board does not show (a graveyard, exile, a library's top, an opponent's hand), they open here, the clicks and
     * Submit going to the input; when it stops, the browser closes. Any thread.
     */
    public static void followSelection(final Selection selection) {
        FThreads.invokeInEdtNowOrLater(() -> {
            if (!ArenaTheme.isMatchBoard() || selection == null || selection.max() <= 0) {
                closeSelection();
                return;
            }
            final List<CardView> hidden = new ArrayList<>();
            for (final CardView card : selection.cards()) {
                if (card != null && !boardShows(card)) {
                    hidden.add(card);
                }
            }
            if (hidden.isEmpty()) {
                closeSelection();
                return;
            }
            showSelection(ArenaChoices.chooseTitle(selection.min(), selection.max(), hidden.size()), hidden);
        });
    }

    /** Whether the board itself shows this card (a permanent, a spell, a card in a local player's hand). */
    private static boolean boardShows(final CardView card) {
        final ZoneType zone = card.getZone();
        if (zone == null || zone == ZoneType.Battlefield || zone == ZoneType.Stack) {
            return true;
        }
        if (zone == ZoneType.Hand) {
            final PlayerView holder = card.getController() != null ? card.getController() : card.getOwner();
            return holder != null && MatchController.instance.getLocalPlayers().contains(holder);
        }
        return false;
    }

    /** Shows the cards the game's current input lets the player pick from hidden zones, replacing those shown before. */
    public static void showSelection(final String title, final List<CardView> cards) {
        if (selectionBrowser != null) {
            if (selectionBrowser.cards.equals(cards) && !selectionBrowser.isClosed()) {
                ArenaMotion.requestFrame();
                return;
            }
            closeSelection();
        }
        if (!ArenaTheme.isMatchBoard()) {
            return;
        }
        selectionBrowser = new ArenaCardBrowser(Mode.SELECTION, title, null, cards, 0, 0, null);
        selectionBrowser.openOverlay();
    }

    /** The input changed what is chosen or what its buttons say: the browser reads them as it draws. */
    public static void refreshSelection() {
        ArenaMotion.requestFrame();
    }

    public static void closeSelection() {
        if (selectionBrowser != null) {
            final ArenaCardBrowser browser = selectionBrowser;
            selectionBrowser = null;
            if (!browser.isClosed()) {
                browser.closeQuietly();
            }
        }
    }

    private static IGameController controller() {
        return MatchController.instance.getGameController();
    }

    private ArenaCardBrowser(final Mode mode, final String title, final String subtitle, final List<CardView> cards, final int min,
            final int max, final Collection<CardView> selected) {
        this.mode = mode;
        this.title = title;
        this.subtitle = subtitle;
        this.cards = new ArrayList<>(cards);
        this.min = min;
        this.max = max;
        this.focusCard = mode == Mode.ZONE || mode == Mode.REVEAL ? null : ArenaDialogKit.promptCard();
        if (selected != null) {
            for (final CardView c : selected) {
                if (this.cards.contains(c) && chosen.size() < Math.max(1, max)) {
                    chosen.add(c);
                }
            }
        }
        for (final CardView card : cards) {
            items.add(new Item(card));
        }
        setOpenMillis(340);
        btnSubmit.primary = true;
        btnSubmit.action = this::submit;
        addPill(btnSubmit);
        btnSecondary.action = () -> {
            if (mode == Mode.SELECTION) {
                ThreadUtil.invokeInGameThread(() -> controller().selectButtonCancel());
            }
        };
        btnSecondary.visible = mode == Mode.SELECTION;
        addPill(btnSecondary);
        updateButtons();
    }

    @Override
    protected void onClosed() {
        if (selectionBrowser == this) {
            selectionBrowser = null;
        }
        if (whenClosed != null) {
            final Runnable r = whenClosed;
            whenClosed = null;
            r.run();
        }
    }

    /** Closes without the fade (another browser takes its place at once). */
    private void closeQuietly() {
        whenClosed = null;
        close();
    }

    // ---- answering --------------------------------------------------------------------------------------------------

    private void submit() {
        if (!btnSubmit.enabled || viewingBattlefield || isClosed()) {
            return;
        }
        if (mode == Mode.SELECTION) {
            ThreadUtil.invokeInGameThread(() -> controller().selectButtonOk()); // the input closes the browser when it is done
            return;
        }
        close();
    }

    private boolean isChosen(final CardView card) {
        return mode == Mode.SELECTION ? MatchController.instance.isHighlighted(card) : chosen.contains(card);
    }

    private int chosenCount() {
        if (mode != Mode.SELECTION) {
            return chosen.size();
        }
        int n = 0;
        for (final CardView card : cards) {
            if (MatchController.instance.isHighlighted(card)) {
                n++;
            }
        }
        return n;
    }

    private VPrompt prompt() {
        final MatchScreen view = MatchController.getView();
        return view == null ? null : view.getActivePrompt();
    }

    private void updateButtons() {
        switch (mode) {
            case CHOOSE:
                btnSubmit.text = Forge.getLocalizer().getMessage("lblArenaSubmit", chosen.size());
                btnSubmit.enabled = chosen.size() >= min && chosen.size() <= max;
                break;
            case ARRANGE:
                btnSubmit.text = Forge.getLocalizer().getMessage("lblArenaSubmitPlain");
                btnSubmit.enabled = true;
                break;
            case SELECTION: {
                // mirror the prompt's own buttons, which the input keeps up to date
                final VPrompt p = prompt();
                final FButton ok = p == null ? null : p.getBtnOk(), cancel = p == null ? null : p.getBtnCancel();
                btnSubmit.text = Forge.getLocalizer().getMessage("lblArenaSubmit", chosenCount());
                btnSubmit.enabled = ok != null && ok.isEnabled();
                btnSecondary.text = cancel == null || cancel.getText() == null ? "" : cancel.getText();
                btnSecondary.enabled = cancel != null && cancel.isEnabled();
                btnSecondary.visible = !btnSecondary.text.isEmpty();
                break;
            }
            default:
                btnSubmit.text = Forge.getLocalizer().getMessage("lblArenaDone");
                btnSubmit.enabled = true;
                break;
        }
    }

    private void onCardClicked(final Item item, final int count) {
        switch (mode) {
            case ZONE:
                if (playable(item.card)) {
                    played = item.card;
                    close();
                } else {
                    forge.card.CardZoom.show(item.card);
                }
                return;
            case SELECTION:
                ThreadUtil.invokeInGameThread(() -> controller().selectCard(item.card, null, null));
                ArenaMotion.requestFrame();
                return;
            case REVEAL:
            case ARRANGE:
                return;
            default:
                break;
        }
        if (chosen.contains(item.card)) {
            chosen.remove(item.card);
        } else if (max == 1) {
            chosen.clear();
            chosen.add(item.card);
            if (count > 1) {
                updateButtons();
                close(); // a double click is answer enough for a single pick
                return;
            }
        } else if (chosen.size() < max) {
            chosen.add(item.card);
        }
        updateButtons();
        relayout();
    }

    /** Whether a click on this card in a zone does something: it can be played or picked from there. */
    private static boolean playable(final CardView card) {
        return MatchController.instance.isActionable(card) || MatchController.instance.isSelectable(card);
    }

    // ---- layout -----------------------------------------------------------------------------------------------------

    @Override
    protected void layoutDialog(final float w, final float h) {
        heading.set(title, headingLine(), fallbackTitle());
        ArenaDialogKit.placeButtons(w, h, btnSubmit, btnSecondary);
        if (mode == Mode.ARRANGE) {
            layoutArrange(w, h, false);
        } else {
            layoutRow(w, h);
        }
    }

    private void relayout() {
        if (getWidth() > 0) {
            if (mode == Mode.ARRANGE) {
                layoutArrange(getWidth(), getHeight(), true);
            } else {
                layoutRow(getWidth(), getHeight());
            }
        }
        ArenaMotion.requestFrame();
    }

    /** How much of each card the next one covers when there is room to spare. */
    private static float overlapFor(final int n) {
        return n <= 3 ? 0.35f : n == 4 ? 0.42f : n <= 6 ? 0.5f : 0.38f;
    }

    /**
     * The fan: rightmost on top, a few degrees of tilt and a slight arc, centred on the window while that keeps it clear
     * of the prompt's card and else in the space left of that card. It closes up until 60% of each card shows (40%
     * beside the prompt's card) and shrinks the cards a little; when even that is not enough it lies flat at that overlap
     * and scrolls. A chosen card lies whole on top, a little larger, and pushes its neighbours aside.
     */
    private void layoutRow(final float w, final float h) {
        final int n = items.size();
        final boolean source = focusCard != null;
        final float margin = Math.round(w * 0.03f);
        final float shown = source ? MIN_SHOWN_SOURCE : MIN_SHOWN;
        final float room = Math.min(ArenaDialogKit.rowRoom(w, h, source), source ? Float.MAX_VALUE : Math.round(w * FAN_CAP));
        final float tilt = n <= 1 ? 0f : Math.min(FAN_ANGLE, 1.2f * (n - 1));
        final float size = fitRow(n, h, tilt, room, shown);
        rowScrolls = size < 0f;
        ch = Math.round(h * (rowScrolls ? MIN_CARD_HEIGHT : size));
        cw = Math.round(ch / FCardPanel.ASPECT_RATIO);
        rowStep = rowScrolls ? Math.round(cw * shown) : rowStepFor(n, cw, ch, tilt, room);
        maxAngle = rowScrolls ? 0f : tilt;
        final float arcH = rowScrolls ? 0f : h * FAN_ARC * Math.min(1f, (n - 1) / 4f);
        final float rowW = cw + Math.max(0, n - 1) * rowStep;
        final float rightEdge = ArenaDialogKit.rowRight(w, h, source) - Math.round(ch * (float) Math.sin(Math.toRadians(maxAngle)) / 2f);
        final float rowCx = ArenaDialogKit.rowCentre(w, h, rowScrolls ? room : rowW, source);

        // what the chosen cards ask of the row, as far as there is room for it
        final float aside = cw * (CHOSEN_SCALE - 1f) / 2f;
        final float through = Math.max(0f, cw + cw * shown - 2f * rowStep);
        float wanted = 0f;
        for (int i = 0; i < n; i++) {
            if (isChosen(items.get(i).card)) {
                wanted += 2f * aside + (i <= n - 3 ? through : 0f);
            }
        }
        final float slack = rowScrolls ? Float.MAX_VALUE : rightEdge - margin - rowW;
        final float fit = wanted <= slack ? 1f : Math.max(0f, slack) / Math.max(1f, wanted);
        final float[] push = new float[n];
        float run = 0f, before = 0f;
        for (int i = 0; i < n; i++) {
            final float t = isChosen(items.get(i).card) ? fit : 0f;
            before += aside * t;
            push[i] = run + aside * t;
            run += t * (2f * aside + (i <= n - 3 ? through : 0f));
        }
        final float total = run;

        final float areaY = Math.round(h * ArenaDialogKit.ROW_AREA_TOP), areaH = Math.round(h * ArenaDialogKit.ROW_AREA_BOTTOM) - areaY;
        viewX = rowScrolls ? rowCx - room / 2f : 0;
        viewW = rowScrolls ? room : w;
        viewY = areaY;
        viewH = areaH;
        final float left;
        if (rowScrolls) {
            left = viewX + Math.round(viewW * 0.06f) + Math.round(h * 0.03f);
            maxScroll = Math.max(0f, left - viewX + rowW + total + Math.round(viewW * 0.06f) + Math.round(h * 0.03f) - viewW);
        } else {
            float at = rowCx - rowW / 2f - before;
            at -= Math.max(0f, at + rowW + total - rightEdge);
            left = Math.max(margin, at);
            maxScroll = 0f;
        }
        if (scroll.target() > maxScroll) {
            scroll.set(maxScroll);
        }
        final float rowCy = Math.round(h * ArenaDialogKit.ROW_CENTRE_Y);
        for (int i = 0; i < n; i++) {
            final float at = n <= 1 ? 0f : 2f * i / (n - 1) - 1f;
            items.get(i).place(left + cw / 2f + i * rowStep + push[i], rowCy + arcH * at * at, maxAngle * at);
        }
        fanLeft = rowScrolls ? viewX : left;
        fanRight = rowScrolls ? viewX + viewW : left + rowW + total;
        fanFoot = rowCy + arcH + ch / 2f + cw / 2f * (float) Math.sin(Math.toRadians(maxAngle));
        restack();
    }

    /**
     * The largest card height at which this share of each card shows in this room, or -1 if there is none. A short row
     * overlaps by design (half of each card for five or six), which counts as fitting.
     */
    private static float fitRow(final int n, final float h, final float tilt, final float room, final float shown) {
        final float least = Math.min(shown, 1f - overlapFor(n));
        for (float size = CARD_HEIGHT; size > MIN_CARD_HEIGHT - 0.001f; size -= 0.02f) {
            final float ch = Math.round(h * size), cw = Math.round(ch / FCardPanel.ASPECT_RATIO);
            if (n <= 1 || rowStepFor(n, cw, ch, tilt, room) >= Math.round(cw * least)) {
                return size;
            }
        }
        return -1f;
    }

    private static float rowStepFor(final int n, final float cw, final float ch, final float tilt, final float room) {
        if (n <= 1) {
            return 0;
        }
        final float pad = Math.round(ch * (float) Math.sin(Math.toRadians(tilt)) / 2f);
        return Math.min(Math.round(cw * (1f - overlapFor(n))), (room - 2 * pad - cw) / (n - 1));
    }

    /** One row of cards on one baseline, side by side while they fit, overlapping after that; its numbers on a rail. */
    private void layoutArrange(final float w, final float h, final boolean keepDragged) {
        final boolean source = focusCard != null;
        final float room = ArenaDialogKit.rowRoom(w, h, source);
        ch = Math.round(h * ARRANGE_HEIGHT);
        cw = Math.round(ch / FCardPanel.ASPECT_RATIO);
        maxAngle = 0f;
        rowScrolls = false;
        maxScroll = 0f;
        final float gap = Math.round(w * 0.02f);
        final int n = items.size();
        final float natural = n <= 1 ? cw : n * cw + (n - 1) * gap;
        final float areaW = Math.min(room, natural);
        rowStep = n <= 1 ? 0 : Math.min(cw + gap, (areaW - cw) / (n - 1));
        final float rowW = cw + Math.max(0, n - 1) * rowStep;
        final float cx0 = ArenaDialogKit.rowCentre(w, h, rowW, source) - rowW / 2f + cw / 2f;
        final float cy = Math.round(h * 0.485f);
        for (int i = 0; i < n; i++) {
            final Item item = items.get(i);
            item.overlapped = n > 1 && rowStep < cw;
            if (!(keepDragged && item == dragItem)) {
                item.place(cx0 + i * rowStep, cy, 0f);
            }
        }
        viewX = 0;
        viewW = w;
        viewY = Math.round(h * ArenaDialogKit.ROW_AREA_TOP);
        viewH = Math.round(h * ArenaDialogKit.ROW_AREA_BOTTOM) - viewY;
        railLeft = cx0 - cw / 2f;
        railRight = railLeft + rowW;
        final float rowBottom = cy + ch / 2f;
        railY = rowBottom + ArenaDialogKit.railDrop(h);
        captionY = rowBottom + ArenaDialogKit.orderRowDrop(h);
        final float inset = n > 1 && rowStep < cw ? Math.round(ArenaDialogKit.badgeSize(h) * 0.8f) : cw / 2f;
        firstBadge = railLeft + inset;
        lastBadge = railLeft + (n - 1) * rowStep + inset;
        fanLeft = railLeft;
        fanRight = railRight;
        fanFoot = rowBottom;
        restack();
    }

    /** The rightmost card lies on top, with what has been chosen above the rest. */
    private void restack() {
        drawOrder.clear();
        for (final Item item : items) {
            if (!isChosen(item.card)) {
                drawOrder.add(item);
            }
        }
        for (final Item item : items) {
            if (isChosen(item.card)) {
                drawOrder.add(item);
            }
        }
    }

    /** The line under the title; a live selection takes it from the prompt, which the input keeps current. */
    private String headingLine() {
        String instruction = subtitle;
        if (mode == Mode.SELECTION) {
            final VPrompt p = prompt();
            instruction = p == null ? null : p.getMessage();
        }
        final String line = instruction == null ? null : ArenaDialogKit.plainText(instruction);
        return line == null || line.isEmpty() || ArenaDialogKit.sameWords(line, title) ? null : line;
    }

    /** What stands in for a title too long to be one. */
    private String fallbackTitle() {
        return Forge.getLocalizer().getMessage(mode == Mode.ARRANGE ? "lblArenaChooseOrder" : "lblArenaChooseCards");
    }

    // ---- drawing ----------------------------------------------------------------------------------------------------

    @Override
    protected void drawScene(final Graphics g, final float w, final float h, final float alpha) {
        if (mode == Mode.SELECTION) {
            heading.set(title, headingLine(), fallbackTitle()); // the input may have changed its words
        }
        heading.draw(g, w, h, alpha);
        if (mode == Mode.ARRANGE) {
            if (topLabel != null) {
                ArenaDialogKit.railEnds(g, topLabel, ArenaDialogKit.lastCaption(topLabel), firstBadge, lastBadge, captionY, h, alpha);
            }
        } else if (anySelectable() && fanRight > fanLeft) {
            ArenaDialogKit.rowShards(g, fanLeft, fanRight, fanFoot, h, lightAt, alpha);
        }
        if (focusCard != null) {
            ArenaDialogKit.sourceBounds(w, h, SOURCE);
            ArenaDialogKit.source(g, focusCard, SOURCE.x, SOURCE.y, SOURCE.width, SOURCE.height, true, alpha);
        }
    }

    private boolean anySelectable() {
        if (mode == Mode.CHOOSE) {
            return true;
        }
        if (mode == Mode.SELECTION) {
            for (final CardView card : cards) {
                if (MatchController.instance.isSelectable(card)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** The colour of the rim of the card that stands over this x of the window. */
    private final ArenaDialogKit.LightAt lightAt = x -> {
        Item nearest = null;
        float best = Float.MAX_VALUE;
        final float dx = rowScrolls ? -scroll.get() : 0f;
        for (final Item item : items) {
            final float away = Math.abs(item.x.get() + dx - x);
            if (away < best) {
                best = away;
                nearest = item;
            }
        }
        return nearest != null && isChosen(nearest.card) ? ArenaDialogKit.PICKED : ArenaDialogKit.CYAN;
    };

    @Override
    protected void drawContent(final Graphics g, final float w, final float h, final float alpha) {
        if (mode == Mode.SELECTION) {
            updateButtons(); // the input keeps the prompt's buttons current
            restack();
        }
        if (mode == Mode.ARRANGE) {
            ArenaDialogKit.rail(g, railLeft, railRight, railY, h, alpha);
        }
        final boolean clip = rowScrolls;
        if (clip) {
            g.startClip(viewX, viewY - h * 0.05f, viewW, viewH + h * 0.1f);
        }
        final float dx = rowScrolls ? -scroll.get() : 0f;
        Item top = null;
        // the row, then what the pointer has raised over it, the card under the pointer over all
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < drawOrder.size(); i++) {
                final Item item = drawOrder.get(i);
                final boolean raised = item.isRaised();
                if ((pass == 0) == raised) {
                    continue;
                }
                if (pass == 1 && (item.hovered || item == dragItem)) {
                    top = item;
                    continue;
                }
                drawItem(g, item, dx, h, alpha);
            }
        }
        if (top != null) {
            drawItem(g, top, dx, h, alpha);
        }
        if (clip) {
            g.endClip();
            drawChevrons(g, h, alpha);
        }
        if (mode == Mode.ARRANGE) {
            for (int i = 0; i < items.size(); i++) {
                final Item item = items.get(i);
                final float d = ArenaDialogKit.badgeSize(h);
                final float bx = item.overlapped ? item.x.get() - cw / 2f + d * 0.8f : item.x.get();
                ArenaDialogKit.badge(g, i + 1, bx, railY, d, alpha);
            }
        }
        if (focus != null) {
            drawFocus(g, focus, dx, h, alpha);
        }
    }

    private void drawItem(final Graphics g, final Item item, final float dx, final float h, final float alpha) {
        final int index = items.indexOf(item);
        final float dealt = entrance.isOver() ? 1f : entrance.inTurn(index, items.size());
        final float a = alpha * dealt;
        if (a <= 0f) {
            return;
        }
        final float dealRise = (1f - dealt) * h * 0.05f;
        final boolean picked = isChosen(item.card);
        final int kind;
        if (picked || item == dragItem) {
            kind = ArenaDialogKit.CHOSEN;
        } else if (mode == Mode.CHOOSE || mode == Mode.ARRANGE || (mode == Mode.SELECTION && MatchController.instance.isSelectable(item.card))
                || (mode == Mode.ZONE && playable(item.card))) {
            kind = ArenaDialogKit.SELECTABLE;
        } else {
            kind = item.hovered ? ArenaDialogKit.PLAIN : -1;
        }
        // the limit is reached; the rest wait dimmed
        final boolean dimmed = mode == Mode.CHOOSE && !picked && chosen.size() >= max && max > 1;
        final float cx = item.x.get() + dx, cy = item.y.get() - item.lift() + dealRise;
        ArenaDialogKit.card(g, item.card, cx, cy, cw, ch, item.tilt(), item.scale(), dimmed ? -1 : kind, item.hovered ? 1.3f : 1f, dimmed, h, a);
        if (mode == Mode.CHOOSE && max > 1 && picked) {
            // the number of a pick straddles the card's bottom edge
            final int place = chosen.indexOf(item.card) + 1;
            final float d = ArenaDialogKit.badgeSize(h), s = item.scale();
            final double rad = Math.toRadians(-item.tilt());
            final double bx = -s * cw / 2d + d * 0.8, by = s * ch / 2d;
            final float px = cx + (float) (bx * Math.cos(rad) - by * Math.sin(rad));
            final float py = cy + (float) (bx * Math.sin(rad) + by * Math.cos(rad));
            ArenaDialogKit.badge(g, place, px, py, d, a);
        }
    }

    /** The round arrows at the ends of a row that scrolls, on the sides that have more. */
    private void drawChevrons(final Graphics g, final float h, final float alpha) {
        final float d = Math.round(h * 0.06f) + 2, cy = Math.round(h * ArenaDialogKit.ROW_CENTRE_Y);
        if (scroll.get() > 1f) {
            ArenaDialogKit.roundButton(g, viewX + Math.round(h * 0.004f) + d / 2f, cy, d, -1, 0, false, false, alpha);
        }
        if (scroll.get() < maxScroll - 1f) {
            ArenaDialogKit.roundButton(g, viewX + viewW - Math.round(h * 0.004f) - d / 2f, cy, d, 1, 0, false, false, alpha);
        }
    }

    private boolean chevronAt(final float x, final float y, final int side) {
        if (!rowScrolls) {
            return false;
        }
        final float h = getHeight();
        final float d = Math.round(h * 0.06f) + 2, cy = Math.round(h * ArenaDialogKit.ROW_CENTRE_Y);
        final float cx = side < 0 ? viewX + Math.round(h * 0.004f) + d / 2f : viewX + viewW - Math.round(h * 0.004f) - d / 2f;
        final boolean shown = side < 0 ? scroll.get() > 1f : scroll.get() < maxScroll - 1f;
        return shown && Math.abs(x - cx) <= d / 2f && Math.abs(y - cy) <= d / 2f;
    }

    /** A thin gold ring round the card the arrow keys are on. */
    private void drawFocus(final Graphics g, final Item item, final float dx, final float h, final float alpha) {
        final float s = item.scale();
        final float w = cw * s, hh = ch * s;
        final float cx = item.x.get() + dx, cy = item.y.get() - item.lift();
        final float reach = Math.max(4f, Math.round(h * 0.012f));
        final float tilt = item.tilt();
        final boolean turned = Math.abs(tilt) > 0.05f;
        if (turned) {
            g.startRotateTransform(cx, cy, -tilt);
        }
        g.drawSmoothRoundRect(Math.max(1.5f, ArenaTheme.px(2)), ArenaTheme.alpha(ArenaTheme.GOLD_BRIGHT, 0.8f * alpha, S1), cx - w / 2f - reach,
                cy - hh / 2f - reach, w + 2 * reach, hh + 2 * reach, ArenaDialogKit.cardRadius(w) + reach);
        if (turned) {
            g.endTransform();
        }
    }

    // ---- input ------------------------------------------------------------------------------------------------------

    private Item itemAt(final float x, final float y) {
        final float dx = rowScrolls ? -scroll.get() : 0f;
        if (rowScrolls && (x < viewX || x > viewX + viewW)) {
            return null;
        }
        // the topmost first: the one under the pointer, the raised, then the row from the right
        for (int i = drawOrder.size() - 1; i >= 0; i--) {
            final Item item = drawOrder.get(i);
            if (item.isRaised() && item.contains(x - dx, y)) {
                return item;
            }
        }
        for (int i = drawOrder.size() - 1; i >= 0; i--) {
            final Item item = drawOrder.get(i);
            if (item.contains(x - dx, y)) {
                return item;
            }
        }
        return null;
    }

    private void setHovered(final Item item) {
        for (final Item it : items) {
            final boolean on = it == item;
            if (it.hovered != on) {
                it.hovered = on;
                it.hover.to(on ? 1f : 0f, on ? 110 : 170);
            }
        }
        ArenaMotion.requestFrame();
    }

    @Override
    protected void onPointer(final float x, final float y, final boolean onButton) {
        if (dragItem != null) {
            return;
        }
        final Item over = onButton ? null : itemAt(x, y);
        setHovered(over);
        if (over != null) {
            focus = null;
        }
    }

    @Override
    protected void onTap(final float x, final float y, final int count) {
        if (chevronAt(x, y, -1)) {
            scrollBy(-viewW * 0.6f);
            return;
        }
        if (chevronAt(x, y, 1)) {
            scrollBy(viewW * 0.6f);
            return;
        }
        if (dragMoved) {
            return;
        }
        final Item item = itemAt(x, y);
        if (item != null) {
            onCardClicked(item, count);
        }
    }

    @Override
    protected void onPress(final float x, final float y) {
        if (mode != Mode.ARRANGE) {
            return;
        }
        final Item item = itemAt(x, y);
        if (item != null) {
            dragItem = item;
            dragGrabX = x - item.x.get();
            dragGrabY = y - item.y.get();
            dragDownX = x;
            dragDownY = y;
            dragMoved = false;
            setHovered(item);
            restack();
        }
    }

    @Override
    protected void onRelease(final float x, final float y) {
        endDrag();
    }

    @Override
    protected void onDrag(final float x, final float y, final float dx, final float dy) {
        if (dragItem == null) {
            return;
        }
        if (!dragMoved && Math.abs(x - dragDownX) + Math.abs(y - dragDownY) < ArenaTheme.px(6)) {
            return;
        }
        dragMoved = true;
        dragItem.x.set(x - dragGrabX);
        dragItem.y.set(y - dragGrabY);
        final float cx = dragItem.x.get();
        int index = 0;
        for (final Item other : items) {
            if (other != dragItem && other.x.target() < cx) {
                index++;
            }
        }
        if (items.indexOf(dragItem) != index) {
            items.remove(dragItem);
            items.add(index, dragItem);
            layoutArrange(getWidth(), getHeight(), true);
        }
        ArenaMotion.requestFrame();
    }

    @Override
    protected void onDragEnd(final float x, final float y) {
        endDrag();
    }

    private void endDrag() {
        if (dragItem == null) {
            return;
        }
        dragItem = null;
        layoutArrange(getWidth(), getHeight(), false);
        if (dragMoved) {
            ArenaMotion.play(60, ArenaMotion.LINEAR, null, () -> dragMoved = false);
        }
        ArenaMotion.requestFrame();
    }

    private void scrollBy(final float dx) {
        if (rowScrolls) {
            scroll.to(Math.max(0f, Math.min(maxScroll, scroll.target() + dx)));
        }
    }

    @Override
    protected void onScroll(final float x, final float y, final float dx, final float dy) {
        // the wheel scrolls the row sideways
        scrollBy(-(dx != 0 ? dx : dy) * 1.5f);
    }

    @Override
    protected void onArrow(final int dx, final int dy) {
        if (items.isEmpty() || dx == 0) {
            return;
        }
        int at = focus == null ? -1 : items.indexOf(focus);
        if (mode == Mode.ARRANGE && focus != null && KeyInputAdapter.isShiftKeyDown()) {
            // Shift and an arrow carries the card along the row
            final int to = Math.max(0, Math.min(items.size() - 1, at + dx));
            if (to != at) {
                items.remove(focus);
                items.add(to, focus);
                layoutArrange(getWidth(), getHeight(), false);
            }
            return;
        }
        at = at < 0 ? (dx > 0 ? 0 : items.size() - 1) : Math.max(0, Math.min(items.size() - 1, at + dx));
        focus = items.get(at);
        setHovered(focus);
        if (rowScrolls) {
            // keep it in view (a card's place on the window is its place in the row less the scroll)
            final float cx = focus.x.target() - scroll.target();
            if (cx - cw / 2f < viewX + cw * 0.2f) {
                scroll.to(Math.max(0f, focus.x.target() - cw / 2f - viewX - cw * 0.2f));
            } else if (cx + cw / 2f > viewX + viewW - cw * 0.2f) {
                scroll.to(Math.min(maxScroll, focus.x.target() + cw / 2f - viewX - viewW + cw * 0.2f));
            }
        }
    }

    @Override
    public boolean keyDown(final int keyCode) {
        // Space picks (or puts back) the card the arrow keys are on
        if (keyCode == Keys.SPACE && !viewingBattlefield && focus != null && (mode == Mode.CHOOSE || mode == Mode.SELECTION || mode == Mode.ZONE)) {
            onCardClicked(focus, 1);
            return true;
        }
        return super.keyDown(keyCode);
    }

    @Override
    protected void onSubmitKey() {
        if (mode == Mode.CHOOSE && max == 1 && chosen.isEmpty() && focus != null) {
            chosen.add(focus.card);
            updateButtons();
        }
        submit();
    }

    @Override
    protected void onEscapeKey() {
        if (mode == Mode.REVEAL || mode == Mode.ZONE || (mode == Mode.CHOOSE && min == 0)) {
            chosen.clear();
            close();
        } else if (mode == Mode.SELECTION && btnSecondary.visible && btnSecondary.enabled) {
            btnSecondary.fire();
        }
    }

    @Override
    protected CardView cardAt(final float x, final float y) {
        final Item item = itemAt(x, y);
        if (item != null) {
            return item.card;
        }
        if (focusCard != null) {
            ArenaDialogKit.sourceBounds(getWidth(), getHeight(), SOURCE);
            if (SOURCE.contains(x, y)) {
                return focusCard;
            }
        }
        return null;
    }

    // ---- one card ---------------------------------------------------------------------------------------------------

    /** One card in the row: a cyan rim while it can be picked, a gold one once chosen. */
    private final class Item {
        final CardView card;
        boolean hovered;
        boolean overlapped;
        boolean placed;
        float angle;
        final ArenaMotion.Fade hover = new ArenaMotion.Fade(0f);
        final ArenaMotion.Fade rise = new ArenaMotion.Fade(0f);
        /** Where its middle rests; it glides there when the row changes. */
        final ArenaMotion.Spring x = new ArenaMotion.Spring(0f, 55, 0.25f), y = new ArenaMotion.Spring(0f, 55, 0.25f);

        Item(final CardView card) {
            this.card = card;
        }

        void place(final float cx, final float cy, final float angle0) {
            angle = angle0;
            if (!placed) {
                x.set(cx);
                y.set(cy);
                placed = true;
            } else {
                x.to(cx);
                y.to(cy);
            }
        }

        boolean isRaised() {
            final float goal = isChosen(card) ? 1f : 0f;
            if (rise.goal() != goal) {
                rise.to(goal, 170);
            }
            return hover.get() > 0f || hovered || this == dragItem;
        }

        /** How far the pointer has raised it; a chosen card stays in its place in the arc even under the pointer. */
        float raised() {
            return hover.get() * (1f - rise.get());
        }

        float lift() {
            return mode == Mode.ARRANGE ? 0f : getHeight() * HOVER_LIFT * raised();
        }

        float scale() {
            return mode == Mode.ARRANGE ? 1f : Math.max(1f + (HOVER_SCALE - 1f) * raised(), 1f + (CHOSEN_SCALE - 1f) * rise.get());
        }

        float tilt() {
            return angle * (1f - raised());
        }

        boolean contains(final float px, final float py) {
            if (cw <= 0) {
                return false;
            }
            final float cx = x.get(), cy = y.get();
            if (ArenaDialogKit.inRotated(px, py, cx, cy, cw / 2f, ch / 2f, angle)) {
                return true;
            }
            // what is raised can be grabbed where it now is, too
            final float lift = lift(), s = scale();
            return lift > 0f && ArenaDialogKit.inRotated(px, py, cx, cy - lift, s * cw / 2f, s * ch / 2f, tilt());
        }
    }
}
