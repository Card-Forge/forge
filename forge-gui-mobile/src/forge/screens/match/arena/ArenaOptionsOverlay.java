package forge.screens.match.arena;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Align;

import forge.Forge;
import forge.Graphics;
import forge.assets.FImageComplex;
import forge.assets.FSkinFont;
import forge.assets.TextRenderer;
import forge.card.CardRenderer;
import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.card.mana.ManaCost;
import forge.game.card.CardView;
import forge.game.card.CardView.CardStateView;
import forge.game.card.IHasCardView;
import forge.game.player.PlayerView;
import forge.game.spellability.SpellAbilityView;
import forge.toolbox.FCardPanel;
import forge.util.CardTranslation;

/**
 * The Arena overlay for the decisions that are not a row of cards (port of the Swing {@code ArenaOptionsOverlay}):
 * picking from a few options (modes, abilities, players, colours, numbers), putting options in order, and plain
 * questions with buttons. Long options become card-like tiles in the row language of the card browser, short ones
 * pills, consecutive numbers a stepper. It dims the board, shows the card the decision is about, can step aside with
 * "View Battlefield", and takes every click while it is up.
 *
 * <p>Everything here runs on the render thread: the static openers return false (and open nothing) when the Arena
 * look is not on the match screen or the options do not suit the overlay, so the caller falls back to its own dialog;
 * otherwise the answer goes to the callback when the overlay closes.</p>
 */
public final class ArenaOptionsOverlay extends ArenaDialogOverlay {
    /** Longer lists than this stay in the (Arena-styled) list, which can be searched and scrolled quickly. */
    public static final int MAX_OPTIONS = 40;
    /** Number lists up to this long still fit the number stepper. */
    public static final int MAX_NUMBERS = 101;

    private static final float TILE_H = 0.39f;
    private static final float TILE_ART = 0.3f;
    private static final float TILE_TEXT = 0.022f;
    private static final float TILE_PITCH = 0.0262f;
    private static final float TILE_LEADING_MOST = 1.36f;
    private static final float GRID_TEXT = 0.019f;
    private static final float GRID_LINE = 1.24f;
    private static final float HOVER_LIFT = 0.04f;
    private static final float NUMERAL_SIZE = 0.115f;
    private static final float ALONE_CARD = 0.38f;
    private static final float ALONE_CARD_CY = 0.42f;
    private static final float STEP_BUTTON = 0.06f;
    private static final float QUESTION_CARD_GAP = 0.04f;
    private static final float QUESTION_CARD_TOP = 0.15f;
    private static final int LONGEST_PILL = 24;
    private static final int MOST_TILES = 6;
    private static final Pattern BRACED = Pattern.compile("\\{([^}]*)\\}");
    private static final String[] NUMERALS = {"I", "II", "III", "IV", "V", "VI"};

    private static final Color PARCHMENT = ArenaTheme.rgb(0xF4ECD8);
    private static final Color PAPER_TOP = ArenaTheme.rgb(0xF8F2E3), PAPER_BOTTOM = ArenaTheme.rgb(0xE8DDC6);
    private static final Color NO_CARD_FRAME = ArenaTheme.rgb(0x9C8656);
    private static final Color TILE_BODY = ArenaTheme.rgb(0x0B0C10);
    private static final Color INK = ArenaTheme.rgb(0x1C1610), BAR_INK = ArenaTheme.rgb(0x15110B);
    private static final Color FRAME_SHINE = ArenaTheme.rgba(255, 255, 255, 60);
    private static final Color KEYLINE = ArenaTheme.rgba(0, 0, 0, 190), BOX_LINE = ArenaTheme.rgba(0, 0, 0, 170);
    private static final Color BAR_SHINE = ArenaTheme.rgba(255, 255, 255, 110);
    private static final Color TINT_W = ArenaTheme.rgb(0xE2DAC0), TINT_U = ArenaTheme.rgb(0x2E6FB4), TINT_B = ArenaTheme.rgb(0x37333D),
            TINT_R = ArenaTheme.rgb(0xBF4128), TINT_G = ArenaTheme.rgb(0x2C7A46), TINT_MULTI = ArenaTheme.rgb(0xC9A640);

    private enum Kind {
        /** pick between min and max options; a single pick closes at once */
        CHOICE,
        /** drag every option into order, first on the left */
        ORDER,
        /** a question answered with one of a few buttons */
        BUTTONS,
        /** the options are only being shown */
        REVEAL
    }

    private enum Style {
        /** the answers themselves are the buttons, in the shared slot */
        ANSWERS,
        /** short words as pills, in a tidy centred wall */
        PILLS,
        /** one row of card-like tiles */
        TILES,
        /** too many long options for one row: two columns of flat tiles that scroll if they must */
        GRID,
        /** consecutive numbers: one big numeral between two arrows */
        STEPPER
    }

    // render-thread scratch
    private static final Color S1 = new Color(), S2 = new Color(), S3 = new Color(), S4 = new Color(), S5 = new Color();

    private final Kind kind;
    private Style style = Style.ANSWERS;
    private final String title;
    private final String message;
    private final String orderCaption;
    private final int min;
    private final int max;
    private final CardView focusCard;
    private final List<Option> options = new ArrayList<>();
    private final List<Option> picked = new ArrayList<>();
    private final ArenaDialogKit.Pill btnSubmit = new ArenaDialogKit.Pill();
    private final ArenaDialogKit.Pill btnCancel = new ArenaDialogKit.Pill();
    private ArenaDialogKit.Pill btnOther;
    private final boolean offerRemember;
    private boolean remember = true;
    private boolean rememberHover;
    private final Rectangle rememberBox = new Rectangle();
    private boolean cancelled;
    private int answer = -1;
    private Runnable whenClosed;
    private Option focus; // the option the arrow keys are on

    // PILLS and GRID: the list's area, scrolled when it is taller
    private final Rectangle area = new Rectangle();
    private float listH;
    private boolean listScrolls;
    private float listStep;
    private final ArenaMotion.Spring scroll = new ArenaMotion.Spring(0f, 60, 0.5f);
    private FSkinFont pillFont;

    // ORDER
    private Option dragging;
    private float dragGrabX;
    private float dragDownX;
    private boolean dragMoved;
    private float rowLeft = -1, rowRight, rowFoot, railY, captionY, firstBadge, lastBadge;

    // STEPPER
    private final List<Option> numbers = new ArrayList<>();
    private int numberIndex;
    private Option otherOption;
    private final Rectangle stepLower = new Rectangle(), stepHigher = new Rectangle();
    private int stepHover;
    private int stepHeld;
    private ArenaMotion.Tween stepRepeat;
    private String rangeText = "";

    // the one text size of all the tiles
    private FSkinFont tileFont;
    private float tilePitch;
    private float sizedTextW = -1, sizedTextH = -1;

    // BUTTONS: the question's words under its card
    private final List<String> questionLines = new ArrayList<>();
    private FSkinFont questionFont;
    private float questionLineH;
    private final Rectangle questionCard = new Rectangle();
    private float questionTextY;

    // ---- opening ----------------------------------------------------------------------------------------------------

    /** Whether the Arena decisions are in use now: the Arena look, on the match screen. */
    public static boolean isOn() {
        return ArenaTheme.isMatchBoard();
    }

    /** Few enough options for the overlay; numbers (with "Other..." after them) fit a stepper of up to a hundred. */
    public static boolean fits(final List<?> choices) {
        if (choices == null || choices.isEmpty()) {
            return false;
        }
        return choices.size() <= (isNumberList(choices) ? MAX_NUMBERS : MAX_OPTIONS);
    }

    /**
     * Lets the player pick between {@code min} and {@code max} of {@code choices} (the picks come back in the order made,
     * an empty list when an optional choice is declined); {@code max < 0} only shows them. Render thread. False if the
     * overlay does not apply, and then nothing was opened.
     */
    public static <T> boolean choose(final String title, final String message, final int min, final int max, final List<T> choices,
            final Collection<T> selected, final Function<T, String> display, final CardView card, final Consumer<List<T>> done) {
        if (!isOn() || choices == null || choices.isEmpty() || !fits(choices) || max == 0) {
            return false;
        }
        final boolean reveal = max < 0;
        final ArenaOptionsOverlay o = new ArenaOptionsOverlay(reveal ? Kind.REVEAL : Kind.CHOICE, title, message, null,
                reveal ? 0 : Math.max(0, min), reveal ? 0 : max, card, false);
        o.addOptions(choices, display);
        if (selected != null && !reveal) {
            for (final Option option : o.options) {
                if (selected.contains(option.value) && o.picked.size() < max) {
                    o.picked.add(option);
                }
            }
        }
        o.whenClosed = () -> {
            final List<T> result = new ArrayList<>();
            if (!o.cancelled && !reveal) {
                for (final Option option : o.picked) {
                    result.add(choices.get(option.index));
                }
            }
            done.accept(result);
        };
        o.openOverlay();
        return true;
    }

    /**
     * Lets the player drag {@code items} into order, the first on the left; tells {@code done} that order and whether to
     * remember it. Render thread. False if the overlay does not apply.
     */
    public static <T> boolean order(final String title, final String caption, final List<T> items, final CardView card,
            final boolean offerRemember, final BiConsumer<List<T>, Boolean> done) {
        if (!isOn() || items == null || items.isEmpty() || items.size() > MAX_OPTIONS) {
            return false;
        }
        final ArenaOptionsOverlay o = new ArenaOptionsOverlay(Kind.ORDER, title, null, caption, items.size(), items.size(), card, offerRemember);
        o.addOptions(items, null);
        o.whenClosed = () -> {
            final List<T> result = new ArrayList<>();
            for (final Option option : o.options) {
                result.add(items.get(option.index));
            }
            done.accept(result, offerRemember && o.remember);
        };
        o.openOverlay();
        return true;
    }

    /**
     * Asks a question answered by one of {@code buttons}; tells {@code done} the index of the one pressed. Render
     * thread. False if the overlay does not apply.
     */
    public static boolean ask(final String title, final String message, final List<String> buttons, final int defaultIndex, final CardView card,
            final Consumer<Integer> done) {
        if (!isOn() || buttons == null || buttons.isEmpty() || buttons.size() > MAX_OPTIONS) {
            return false;
        }
        final ArenaOptionsOverlay o = new ArenaOptionsOverlay(Kind.BUTTONS, title, message, null, 1, 1, card, false);
        o.addOptions(buttons, null);
        final int primary = defaultIndex >= 0 && defaultIndex < o.options.size() ? defaultIndex : 0;
        o.options.get(primary).primary = true;
        o.options.get(primary).answerPill.primary = true;
        o.whenClosed = () -> {
            if (done != null) {
                done.accept(o.answer >= 0 ? o.answer : primary);
            }
        };
        o.openOverlay();
        return true;
    }

    private ArenaOptionsOverlay(final Kind kind, final String title, final String message, final String orderCaption, final int min,
            final int max, final CardView card, final boolean offerRemember) {
        this.kind = kind;
        this.title = title;
        this.message = ArenaDialogKit.plainText(message);
        this.orderCaption = orderCaption;
        this.min = min;
        this.max = max;
        // a plain question shows only the card it was asked about
        this.focusCard = card != null || kind == Kind.BUTTONS ? card : ArenaDialogKit.promptCard();
        this.offerRemember = offerRemember;
        setOpenMillis(320);

        btnSubmit.primary = true;
        btnSubmit.action = this::submit;
        addPill(btnSubmit);
        btnCancel.text = Forge.getLocalizer().getMessage(kind == Kind.CHOICE && max > 1 ? "lblCancel" : "lblArenaDecline");
        btnCancel.action = this::cancel;
        btnCancel.visible = kind == Kind.CHOICE && min == 0;
        addPill(btnCancel);
    }

    private <T> void addOptions(final List<T> values, final Function<T, String> display) {
        for (int i = 0; i < values.size(); i++) {
            final T value = values.get(i);
            options.add(new Option(i, value, display != null ? display.apply(value) : describe(value)));
        }
        style = chooseStyle(values);
        if (style == Style.STEPPER) {
            buildStepper();
        } else if (style == Style.ANSWERS) {
            for (final Option option : options) {
                final Option o = option;
                option.answerPill = new ArenaDialogKit.Pill(option.label, false, () -> answer(o));
                addPill(option.answerPill);
            }
        }
        updateButtons();
    }

    private Style chooseStyle(final List<?> values) {
        if (kind == Kind.BUTTONS) {
            return Style.ANSWERS;
        }
        if (kind == Kind.ORDER) {
            return Style.TILES;
        }
        if (kind == Kind.CHOICE && max == 1 && isConsecutiveNumbers(values)) {
            return Style.STEPPER;
        }
        boolean allShort = true;
        for (final Option option : options) {
            if (option.card() != null || option.label.contains("\n") || option.label.length() > LONGEST_PILL) {
                allShort = false;
            }
        }
        if (allShort) {
            return Style.PILLS;
        }
        return options.size() <= MOST_TILES ? Style.TILES : Style.GRID;
    }

    /** At least two whole numbers one apart, either way round, maybe with "Other..." after them. */
    private static boolean isConsecutiveNumbers(final List<?> values) {
        final int size = values.size();
        final int count = size > 2 && values.get(size - 1) instanceof String ? size - 1 : size;
        if (count < 2) {
            return false;
        }
        Integer direction = null;
        for (int i = 0; i < count; i++) {
            if (!(values.get(i) instanceof Integer)) {
                return false;
            }
            if (i > 0) {
                final int diff = (Integer) values.get(i) - (Integer) values.get(i - 1);
                if (Math.abs(diff) != 1 || (direction != null && direction != diff)) {
                    return false;
                }
                direction = diff;
            }
        }
        return true;
    }

    /** Numbers, maybe followed by one "Other..." to type a bigger one, as getInteger offers them. */
    public static boolean isNumberList(final List<?> choices) {
        if (choices == null || choices.isEmpty()) {
            return false;
        }
        for (int i = 0; i < choices.size(); i++) {
            if (!(choices.get(i) instanceof Integer) && !(i == choices.size() - 1 && i > 0 && choices.get(i) instanceof String)) {
                return false;
            }
        }
        return true;
    }

    private void buildStepper() {
        for (final Option option : options) {
            if (option.value instanceof Integer) {
                numbers.add(option);
            } else {
                otherOption = option;
            }
        }
        // the first one offered is where it starts, even in a list that counts down
        final Option first = numbers.get(0);
        numbers.sort((a, b) -> Integer.compare((Integer) a.value, (Integer) b.value));
        numberIndex = numbers.indexOf(first);
        if (otherOption != null) {
            btnOther = new ArenaDialogKit.Pill(otherOption.label, false, () -> {
                picked.clear();
                picked.add(otherOption);
                close();
            });
            addPill(btnOther);
        }
    }

    private static String describe(final Object value) {
        if (value instanceof CardView) {
            final CardView card = (CardView) value;
            return card.getCurrentState() == null ? card.toString() : CardTranslation.getTranslatedName(card.getCurrentState().getName());
        }
        if (value instanceof PlayerView) {
            return ((PlayerView) value).getName();
        }
        return String.valueOf(value);
    }

    @Override
    protected void onOpened() {
        if (style == Style.STEPPER && !picked.isEmpty() && numbers.contains(picked.get(0))) {
            numberIndex = numbers.indexOf(picked.get(0)); // the selection it was given
        }
        updateButtons();
    }

    @Override
    protected void onClosed() {
        if (stepRepeat != null) {
            stepRepeat.cancel();
            stepRepeat = null;
        }
        if (whenClosed != null) {
            final Runnable r = whenClosed;
            whenClosed = null;
            r.run();
        }
    }

    // ---- answering --------------------------------------------------------------------------------------------------

    private boolean submitShown() {
        return kind == Kind.ORDER || kind == Kind.REVEAL || (kind == Kind.CHOICE && (style != Style.PILLS || max > 1));
    }

    private void updateButtons() {
        btnSubmit.visible = submitShown();
        if (kind == Kind.REVEAL) {
            btnSubmit.text = Forge.getLocalizer().getMessage("lblOK");
            btnSubmit.enabled = true;
        } else if (kind == Kind.ORDER || style == Style.STEPPER) {
            btnSubmit.text = Forge.getLocalizer().getMessage("lblArenaSubmitPlain");
            btnSubmit.enabled = true;
        } else {
            // declining a single pick has its own button; a list of picks may be left empty
            final int least = max == 1 ? 1 : min;
            btnSubmit.enabled = picked.size() >= least && picked.size() <= max;
            // the button says what it does: what is still to pick, or "Choose" a single pick, "Submit 2" several
            btnSubmit.text = picked.size() < least ? Forge.getLocalizer().getMessage("lblArenaPickMore", least - picked.size())
                    : max == 1 ? Forge.getLocalizer().getMessage("lblArenaChooseThis")
                    : Forge.getLocalizer().getMessage("lblArenaSubmit", picked.size());
        }
        if (getWidth() > 0) {
            ArenaDialogKit.placeButtons(getWidth(), getHeight(), btnSubmit, btnOther, btnCancel);
        }
    }

    private void submit() {
        if (!btnSubmit.visible || !btnSubmit.enabled || isClosed()) {
            return;
        }
        if (style == Style.STEPPER) {
            picked.clear();
            picked.add(numbers.get(numberIndex));
        }
        close();
    }

    private void cancel() {
        if (isClosed()) {
            return;
        }
        cancelled = true;
        picked.clear();
        close();
    }

    private void answer(final Option option) {
        if (isClosed()) {
            return;
        }
        answer = option.index;
        close();
    }

    private void onOptionClicked(final Option option, final int clicks) {
        switch (kind) {
            case BUTTONS:
                answer(option);
                return;
            case ORDER:
            case REVEAL:
                return;
            default:
                break;
        }
        if (max == 1) {
            picked.clear();
            picked.add(option);
            if (style == Style.PILLS || clicks > 1) {
                close(); // a pill is answer enough; a tile takes a double click or Submit
                return;
            }
        } else if (picked.contains(option)) {
            picked.remove(option);
        } else if (picked.size() < max) {
            picked.add(option);
        }
        updateButtons();
        ArenaMotion.requestFrame();
    }

    // ---- layout -----------------------------------------------------------------------------------------------------

    @Override
    protected void layoutDialog(final float w, final float h) {
        if (questionIsTitle(w, h)) {
            heading.setQuestion(message);
        } else {
            final String[] parts = headingParts();
            heading.set(parts[0], parts[1], parts[2]);
        }
        ArenaDialogKit.placeButtons(w, h, btnSubmit, btnOther, btnCancel);
        switch (style) {
            case ANSWERS:
                layoutAnswers(w, h);
                break;
            case PILLS:
                layoutPills(w, h);
                break;
            case TILES:
                layoutTiles(w, h, false);
                break;
            case GRID:
                layoutGrid(w, h);
                break;
            default:
                layoutStepper(w, h);
                break;
        }
        if (offerRemember) {
            // under the row, between the words for its two ends
            final float box = Math.round(h * 0.022f);
            final String words = Forge.getLocalizer().getMessage("lblAlwaysUseThisOrder");
            final float bw = ArenaDialogKit.checkBoxWidth(words, box);
            rememberBox.set((rowLeft + rowRight - bw) / 2f, captionY - box / 2f + Math.round(h * 0.035f), bw, box);
        }
    }

    private float rightLimit(final float w, final float h) {
        if (focusCard == null || kind == Kind.BUTTONS) {
            return w;
        }
        final Rectangle r = new Rectangle();
        ArenaDialogKit.sourceBounds(w, h, r);
        return r.x - Math.round(w * 0.012f);
    }

    private void layoutAnswers(final float w, final float h) {
        final float pillH = ArenaDialogKit.buttonH(h);
        final FSkinFont f = ArenaDialogKit.font(Math.min(pillH * 0.46f, ArenaTheme.px(26)));
        float widest = 0;
        for (final Option option : options) {
            widest = Math.max(widest, ArenaDialogKit.measure(option.label, f));
        }
        final float bw = Math.max(ArenaDialogKit.buttonW(w), Math.min(Math.round(w * 0.26f), widest + pillH * 1.2f));
        int i = 0;
        for (final Option option : options) {
            ArenaDialogKit.placeInRow(w / 2f, w, h, bw, i++, options.size(), option.answerPill.bounds);
        }
        layoutQuestion(w, h);
    }

    /** Sets the list's area for a list {@code height} tall, scrolled when that is taller than the room. */
    private void fitList(final float w, final float h, final float height) {
        final float right = rightLimit(w, h);
        final float top = Math.round(h * ArenaDialogKit.ROW_AREA_TOP), bottom = Math.round(h * ArenaDialogKit.ROW_AREA_BOTTOM);
        area.set(0, top, right, bottom - top);
        listH = height;
        listScrolls = height > area.height;
        clampScroll();
    }

    private float maxScroll() {
        return listScrolls ? Math.max(0f, listH - area.height) : 0f;
    }

    private void clampScroll() {
        final float most = maxScroll();
        if (scroll.target() > most || scroll.target() < 0f) {
            scroll.set(Math.max(0f, Math.min(most, scroll.target())));
        }
    }

    private void scrollBy(final float dy) {
        if (!listScrolls) {
            return;
        }
        scroll.to(Math.max(0f, Math.min(maxScroll(), scroll.target() + dy)));
    }

    /** Equal pills in centred rows. */
    private void layoutPills(final float w, final float h) {
        final float right = rightLimit(w, h), margin = Math.round(w * 0.04f);
        final float pillH = ArenaDialogKit.buttonH(h), gap = Math.round(h * 0.014f);
        final FSkinFont f = ArenaDialogKit.font(Math.min(pillH * 0.46f, ArenaTheme.px(26)));
        float widest = 0;
        for (final Option option : options) {
            widest = Math.max(widest, ArenaDialogKit.measure(option.label, f));
        }
        final float pillW = Math.max(Math.round(w * 0.09f), Math.min(Math.round(w * 0.24f), widest + Math.round(pillH * 1.2f)));
        // one size of words for the whole wall: the largest at which the longest still fits its pill
        String longest = "";
        for (final Option option : options) {
            if (ArenaDialogKit.measure(option.label, f) > ArenaDialogKit.measure(longest, f)) {
                longest = option.label;
            }
        }
        final float inner = pillW - 2 * ArenaArt.buttonPad(pillH);
        pillFont = ArenaDialogKit.fitFont(longest, Math.max(ArenaTheme.px(12), Math.min((pillH - 2 * ArenaArt.buttonPad(pillH)) * 0.5f, ArenaTheme.px(30))),
                ArenaTheme.px(10), inner - (pillH - 2 * ArenaArt.buttonPad(pillH)) * 0.7f);
        final float room = right - 2 * margin;
        final int n = options.size();
        final int fit = Math.max(1, (int) ((room + gap) / (pillW + gap)));
        // an even wall of pills looks better than a long row and an orphan
        final int perRow = n <= fit ? n : Math.max(Math.min(fit, (int) Math.ceil(Math.sqrt(n) * 1.6)), (n + 4) / 5);
        final int rows = (n + perRow - 1) / perRow;
        final float wallH = rows * pillH + (rows - 1) * gap;
        fitList(w, h, wallH + 2 * gap);
        listStep = pillH + gap;
        final float top = listScrolls ? gap : Math.max(gap, Math.round(h * ArenaDialogKit.ROW_CENTRE_Y) - area.y - wallH / 2f);
        for (int i = 0; i < n; i++) {
            final int row = i / perRow;
            final int inRow = Math.min(perRow, n - row * perRow);
            final float rowW = inRow * pillW + (inRow - 1) * gap;
            final float x = Math.max(margin, Math.min((w - rowW) / 2f, right - margin - rowW));
            options.get(i).box.set(x + (i % perRow) * (pillW + gap), area.y + top + row * (pillH + gap), pillW, pillH);
        }
    }

    /** The row of card-like tiles, each of a card's shape; they narrow rather than overlap unless there are too many to read. */
    private void layoutTiles(final float w, final float h, final boolean keepDragged) {
        final int n = options.size();
        final boolean source = focusCard != null;
        final float room = ArenaDialogKit.rowRoom(w, h, source);
        final float gap = Math.round(w * 0.02f), narrowest = Math.round(w * 0.11f);
        final float th = Math.round(h * TILE_H);
        float tw = Math.round(th / FCardPanel.ASPECT_RATIO);
        float step = tw + gap;
        if (n * tw + (n - 1) * gap > room) {
            final float fit = (room - (n - 1) * gap) / n;
            if (fit >= narrowest) {
                tw = Math.round(fit);
                step = tw + gap;
            } else {
                tw = narrowest;
                step = Math.min(tw + gap, (room - tw) / Math.max(1, n - 1));
            }
        }
        final float rowW = tw + (n - 1) * step;
        final float left = ArenaDialogKit.rowCentre(w, h, rowW, source) - rowW / 2f;
        final float cy = Math.round(h * ArenaDialogKit.ROW_CENTRE_Y);
        final boolean overlapped = step < tw;
        for (int i = 0; i < n; i++) {
            final Option option = options.get(i);
            option.overlapped = overlapped;
            option.place(left + tw / 2f + i * step, cy, tw, th, false, !(keepDragged && option == dragging));
        }
        rowLeft = left;
        rowRight = left + rowW;
        final float inset = overlapped ? Math.round(ArenaDialogKit.badgeSize(h) * 0.8f) : tw / 2f;
        firstBadge = left + inset;
        lastBadge = left + (n - 1) * step + inset;
        rowFoot = cy + th / 2f;
        railY = rowFoot + ArenaDialogKit.railDrop(h);
        captionY = rowFoot + ArenaDialogKit.orderRowDrop(h);
        sizeTileText(tw, th, h, false);
    }

    /** Two columns of flat tiles, all the height of the longest. */
    private void layoutGrid(final float w, final float h) {
        final float right = rightLimit(w, h), margin = Math.round(w * 0.04f);
        final float gap = Math.round(h * 0.014f);
        final float cellW = Math.min(Math.round(w * 0.42f), (right - 2 * margin - gap) / 2f);
        final FSkinFont f = ArenaDialogKit.font(h * GRID_TEXT);
        final float lineH = Math.round(f.getLineHeight() * GRID_LINE);
        final float textW = cellW - gridTextInset(h);
        float rowH = Math.round(h * 0.075f);
        for (final Option option : options) {
            final float textH = option.textRenderer.getWrappedBounds(option.text(), f, textW).height;
            final float lines = Math.max(1f, Math.round(textH / Math.max(1f, f.getLineHeight())));
            rowH = Math.max(rowH, Math.min(Math.round(h * 0.15f), lines * lineH + Math.round(h * 0.03f)));
        }
        final int rows = (options.size() + 1) / 2;
        final float gridH = rows * rowH + (rows - 1) * gap;
        final float pad = Math.round(h * 0.03f);
        fitList(w, h, gridH + 2 * pad);
        listStep = rowH + gap;
        final float top = listScrolls ? pad : Math.max(pad, (area.height - gridH) / 2f);
        final float gridW = 2 * cellW + gap;
        final float left = Math.max(margin, Math.min((w - gridW) / 2f, right - margin - gridW));
        for (int i = 0; i < options.size(); i++) {
            final int col = i % 2, row = i / 2;
            options.get(i).place(left + cellW / 2f + col * (cellW + gap), area.y + top + rowH / 2f + row * (rowH + gap), cellW, rowH, true, true);
        }
        sizeTileText(cellW, rowH, h, true);
    }

    private static float gridTextInset(final float h) {
        return Math.round(h * 0.085f) + Math.round(h * 0.036f);
    }

    /** Where the prompt's card stands when it is alone above a question or a number: the same on both. */
    private static void aloneCard(final float w, final float h, final Rectangle out) {
        final float fh = Math.round(h * ALONE_CARD), fw = Math.round(fh / FCardPanel.ASPECT_RATIO);
        out.set(Math.round((w - fw) / 2f), Math.round(h * ALONE_CARD_CY) - fh / 2f, fw, fh);
    }

    /** The line through the middle of the numeral and its buttons: below the card if there is one, else level with a row. */
    private float stepperLine(final float w, final float h) {
        if (focusCard == null) {
            return Math.round(h * (ArenaDialogKit.ROW_CENTRE_Y - 0.013f));
        }
        final Rectangle card = STEP_RECT;
        aloneCard(w, h, card);
        return card.y + card.height + Math.round(h * 0.065f);
    }

    private void layoutStepper(final float w, final float h) {
        final float cy = stepperLine(w, h), d = Math.round(h * STEP_BUTTON) + 2;
        // the buttons stand clear of the widest number there is
        final FSkinFont f = numeralFont(h);
        float widest = 0;
        for (final Option option : numbers) {
            widest = Math.max(widest, ArenaArt.textWidth(String.valueOf(option.value), f));
        }
        final float off = Math.max(Math.round(h * 0.135f), widest / 2f + Math.round(h * 0.022f) + d / 2f);
        stepLower.set(w / 2f - off - d / 2f, cy - d / 2f, d, d);
        stepHigher.set(w / 2f + off - d / 2f, cy - d / 2f, d, d);
        rangeText = Forge.getLocalizer().getMessage("lblArenaRange", numbers.get(0).label, numbers.get(numbers.size() - 1).label);
    }

    private static final Rectangle STEP_RECT = new Rectangle();
    private static final TextRenderer MEASURE = new TextRenderer();

    private static FSkinFont numeralFont(final float h) {
        return ArenaDialogKit.font(h * NUMERAL_SIZE);
    }

    /** The one text size of a set of tiles: the largest at which the longest of them still fits. */
    private void sizeTileText(final float tw, final float th, final float h, final boolean flat) {
        final float textW, textH;
        if (flat) {
            textW = tw - gridTextInset(h) - Math.round(tw * 0.05f);
            textH = th - Math.round(h * 0.02f);
        } else {
            final float m = tileMargin(tw, th, false);
            final float panelTop = m + Math.round(h * 0.036f) + Math.round(th * TILE_ART) + Math.round(h * 0.028f) + 4 * Math.max(2, Math.round(tw * 0.01f));
            textW = tw - 2 * (m + Math.max(2, Math.round(tw * 0.016f))) - 2 * Math.round(tw * 0.055f);
            textH = th - panelTop - m - Math.round(h * 0.012f);
        }
        if (textW == sizedTextW && textH == sizedTextH && tileFont != null) {
            return;
        }
        sizedTextW = textW;
        sizedTextH = textH;
        float size = h * (flat ? GRID_TEXT : TILE_TEXT);
        final float least = h * 0.0135f;
        FSkinFont f = ArenaDialogKit.font(size);
        for (final Option option : options) {
            while (size > least && option.textRenderer.getWrappedBounds(option.text(), f, textW).height > textH) {
                size -= 2f;
                f = ArenaDialogKit.font(size);
            }
        }
        tileFont = f;
        tilePitch = flat ? Math.round(f.getLineHeight() * GRID_LINE) : Math.min(Math.round(h * TILE_PITCH), Math.round(f.getLineHeight() * TILE_LEADING_MOST));
    }

    private static float tileMargin(final float tw, final float th, final boolean flat) {
        final float b = flat ? Math.max(2, Math.round(th * 0.03f)) : Math.max(2, Math.round(tw * 0.014f));
        return b + (flat ? Math.max(3, Math.round(th * 0.05f)) : Math.max(4, Math.round(tw * 0.028f)));
    }

    // ---- the heading and the question ------------------------------------------------------------------------------

    /** The heading as {title, subtitle, what stands in for the title if it is too long}. */
    private String[] headingParts() {
        if (kind == Kind.ORDER) {
            return new String[] {title, Forge.getLocalizer().getMessage("lblArenaDragToReorder"), Forge.getLocalizer().getMessage("lblArenaChooseOrder")};
        }
        if (kind == Kind.CHOICE || kind == Kind.REVEAL) {
            final String line = message != null && !message.isEmpty() && !ArenaDialogKit.sameWords(message, title) ? message : null;
            return new String[] {title, line, Forge.getLocalizer().getMessage("lblArenaMakeChoice")};
        }
        return new String[] {fillerTitle() ? null : title, null, null};
    }

    /** Whether the title only says that this is a question or a notice, which needs no heading. */
    private boolean fillerTitle() {
        return title == null || title.trim().isEmpty() || title.equals(Forge.getLocalizer().getMessage("lblArenaQuestion"))
                || title.equals(Forge.getLocalizer().getMessage("lblArenaNotice"));
    }

    /** A question about a card is the title itself, if it will go in two lines; a longer one, or one with no card to go with it, is body text. */
    private boolean questionIsTitle(final float w, final float h) {
        return kind == Kind.BUTTONS && focusCard != null && message != null && !message.isEmpty() && message.indexOf('\n') < 0
                && ArenaDialogKit.Heading.fitsAsQuestion(message, w, h);
    }

    /** The question's card as large as the space allows, close above the buttons, with any words not in the heading under it. */
    private void layoutQuestion(final float w, final float h) {
        final boolean hasCard = focusCard != null;
        final String text = questionIsTitle(w, h) || message == null ? "" : message;
        final float top = heading.bottom(w, h), bottom = ArenaDialogKit.buttonY(h);
        questionLines.clear();
        float size = h * (hasCard ? 0.024f : 0.035f);
        FSkinFont f = ArenaDialogKit.font(size);
        if (!text.isEmpty()) {
            final int most = hasCard ? 3 : 8;
            final float width = Math.round(w * (hasCard ? 0.6f : 0.56f));
            List<String> lines = ArenaDialogKit.wrap(text, f, width);
            while (lines.size() > most && size > h * 0.018f) {
                size -= 2f;
                f = ArenaDialogKit.font(size);
                lines = ArenaDialogKit.wrap(text, f, width);
            }
            if (lines.size() > most) {
                lines = new ArrayList<>(lines.subList(0, most));
                lines.set(most - 1, lines.get(most - 1) + " …");
            }
            questionLines.addAll(lines);
        }
        questionFont = f;
        questionLineH = Math.round(f.getLineHeight() * 0.96f);
        final float textH = questionLines.size() * questionLineH;
        if (hasCard) {
            final float cardBottom = bottom - Math.round(h * QUESTION_CARD_GAP) - textH;
            final float cardTop = Math.max(top, Math.round(h * QUESTION_CARD_TOP)) + Math.round(h * 0.02f);
            final float fh = Math.max(0f, cardBottom - cardTop), fw = Math.round(fh / FCardPanel.ASPECT_RATIO);
            questionCard.set(Math.round((w - fw) / 2f), cardTop, fw, fh);
            questionTextY = cardBottom + Math.round(h * 0.012f);
        } else {
            questionCard.set(0, 0, 0, 0);
            questionTextY = top + (bottom - top - textH) / 2f;
        }
    }

    // ---- drawing ----------------------------------------------------------------------------------------------------

    @Override
    protected void drawScene(final Graphics g, final float w, final float h, final float alpha) {
        heading.draw(g, w, h, alpha);
        if (kind == Kind.BUTTONS) {
            drawQuestion(g, w, h, alpha);
        } else if (style == Style.STEPPER) {
            if (focusCard != null) {
                final Rectangle r = SCRATCH_RECT;
                aloneCard(w, h, r);
                ArenaDialogKit.source(g, focusCard, r.x, r.y, r.width, r.height, false, alpha);
            }
        } else {
            if (focusCard != null) {
                final Rectangle r = SCRATCH_RECT;
                ArenaDialogKit.sourceBounds(w, h, r);
                ArenaDialogKit.source(g, focusCard, r.x, r.y, r.width, r.height, true, alpha);
            }
            if (kind == Kind.ORDER && rowLeft >= 0 && orderCaption != null && !orderCaption.isEmpty()) {
                ArenaDialogKit.railEnds(g, orderCaption, ArenaDialogKit.lastCaption(orderCaption), firstBadge, lastBadge, captionY, h, alpha);
            } else if (kind == Kind.CHOICE && style == Style.TILES && rowLeft >= 0) {
                ArenaDialogKit.rowShards(g, rowLeft, rowRight, rowFoot, h, lightAt, alpha);
            }
        }
    }

    private static final Rectangle SCRATCH_RECT = new Rectangle();

    /** The colour of the rim of the tile that stands over this x of the window. */
    private final ArenaDialogKit.LightAt lightAt = x -> {
        Option nearest = null;
        float best = Float.MAX_VALUE;
        for (final Option option : options) {
            final float away = Math.abs(option.px.get() - x);
            if (away < best) {
                best = away;
                nearest = option;
            }
        }
        return nearest != null && picked.contains(nearest) ? ArenaDialogKit.PICKED : ArenaDialogKit.CYAN;
    };

    private void drawQuestion(final Graphics g, final float w, final float h, final float alpha) {
        if (focusCard != null && questionCard.height > 0) {
            ArenaDialogKit.source(g, focusCard, questionCard.x, questionCard.y, questionCard.width, questionCard.height, false, alpha);
        }
        if (questionLines.isEmpty()) {
            return;
        }
        float y = questionTextY;
        final Color white = ArenaTheme.alpha(ArenaTheme.WHITE, alpha, S1);
        for (final String line : questionLines) {
            ArenaDialogKit.edged(g, line, questionFont, white, 0, y, w, questionLineH, Align.center);
            y += questionLineH;
        }
    }

    @Override
    protected void drawContent(final Graphics g, final float w, final float h, final float alpha) {
        switch (style) {
            case PILLS:
                drawPills(g, w, h, alpha);
                break;
            case TILES:
                drawTiles(g, w, h, alpha);
                break;
            case GRID:
                drawGrid(g, w, h, alpha);
                break;
            case STEPPER:
                drawStepper(g, w, h, alpha);
                break;
            default:
                break;
        }
        if (offerRemember) {
            ArenaDialogKit.checkBox(g, Forge.getLocalizer().getMessage("lblAlwaysUseThisOrder"), remember, rememberHover, rememberBox.x,
                    rememberBox.y, rememberBox.height, alpha);
        }
        if (focus != null && style != Style.STEPPER) {
            drawFocus(g, focus, alpha);
        }
    }

    private boolean isDimmed(final Option option) {
        return kind == Kind.CHOICE && !picked.contains(option) && max > 1 && picked.size() >= max; // the limit is reached
    }

    private void drawPills(final Graphics g, final float w, final float h, final float alpha) {
        final float dy = -scroll.get();
        final boolean clip = listScrolls;
        if (clip) {
            g.startClip(area.x, area.y, area.width, area.height);
        }
        for (final Option option : options) {
            final Rectangle b = option.box;
            if (b.y + dy + b.height < area.y || b.y + dy > area.y + area.height) {
                continue;
            }
            final boolean isPicked = picked.contains(option);
            final float a = isDimmed(option) ? 0.45f * alpha : alpha;
            ArenaDialogKit.pill(g, option.label, pillFont, b.x, b.y + dy, b.width, b.height, option.primary, option.hovered && kind != Kind.REVEAL,
                    false, isPicked, true, a);
        }
        if (clip) {
            g.endClip();
            drawChevrons(g, w, h, alpha);
        }
    }

    /** The round arrows at the top and bottom of a list that scrolls, on the sides that have more. */
    private void drawChevrons(final Graphics g, final float w, final float h, final float alpha) {
        final float d = Math.round(h * 0.044f);
        final float cx = area.x + area.width / 2f;
        if (scroll.get() > 1f) {
            ArenaDialogKit.roundButton(g, cx, area.y + d / 2f + 4, d, 0, -1, false, false, alpha);
        }
        if (scroll.get() < maxScroll() - 1f) {
            ArenaDialogKit.roundButton(g, cx, area.y + area.height - d / 2f - 4, d, 0, 1, false, false, alpha);
        }
    }

    private void drawTiles(final Graphics g, final float w, final float h, final float alpha) {
        final boolean railed = kind == Kind.ORDER && rowLeft >= 0;
        if (railed) {
            ArenaDialogKit.rail(g, rowLeft, rowRight, railY, h, alpha);
        }
        // the row, rightmost on top; raised ones over it; the one under the pointer (or dragged) over all
        Option top = null;
        final int n = options.size();
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < n; i++) {
                final Option option = options.get(i);
                final boolean raised = option.isRaised();
                if (pass == 0 && raised || pass == 1 && !raised) {
                    continue;
                }
                if (pass == 1 && (option.hovered || option == dragging)) {
                    top = option;
                    continue;
                }
                drawTile(g, option, i, h, alpha);
            }
        }
        if (top != null) {
            drawTile(g, top, options.indexOf(top), h, alpha);
        }
        if (railed) {
            for (int i = 0; i < n; i++) {
                drawBadge(g, options.get(i), i, h, alpha);
            }
        }
    }

    private void drawGrid(final Graphics g, final float w, final float h, final float alpha) {
        final float dy = -scroll.get();
        g.startClip(area.x, area.y, area.width, area.height);
        for (int i = 0; i < options.size(); i++) {
            final Option option = options.get(i);
            final float cy = option.cy + dy;
            if (cy + option.th / 2f + h * 0.03f < area.y || cy - option.th / 2f - h * 0.03f > area.y + area.height) {
                continue;
            }
            drawTileAt(g, option, i, option.px.get(), cy, h, alpha);
        }
        g.endClip();
        if (listScrolls) {
            drawChevrons(g, w, h, alpha);
        }
    }

    private void drawTile(final Graphics g, final Option option, final int index, final float h, final float alpha) {
        drawTileAt(g, option, index, option.px.get(), option.cy, h, alpha);
    }

    private void drawTileAt(final Graphics g, final Option option, final int index, final float cx, final float cy0, final float h,
            final float alpha) {
        final float raise = option.raised();
        final float lift = option.flat ? 0f : h * HOVER_LIFT * raise;
        final float scale = option.flat ? 1f : 1f + 0.04f * raise;
        final float tw = option.tw * scale, th = option.th * scale;
        final float x = cx - tw / 2f, y = cy0 - lift - th / 2f;
        final boolean isPicked = picked.contains(option) || option == dragging;
        final boolean dimmed = isDimmed(option);
        final boolean lit = !dimmed && kind != Kind.REVEAL;
        final float r = ArenaDialogKit.tileRadius(tw, th);
        // dealt in one after the other as the dialog comes in
        final float dealt = entrance.isOver() ? 1f : entrance.inTurn(index, options.size());
        final float a = alpha * dealt;
        if (a <= 0f) {
            return;
        }
        final float dealRise = (1f - dealt) * h * 0.05f;
        final float ty = y + dealRise;
        ArenaDialogKit.cardShadow(g, x, ty, tw, th, r, a);
        final int light = isPicked ? ArenaDialogKit.CHOSEN : ArenaDialogKit.SELECTABLE;
        if (lit) {
            ArenaDialogKit.cardLight(g, x, ty, tw, th, r, light, option.hovered ? 1.3f : 1f, option.flat, h, a);
        }
        drawTileFace(g, option, index, x, ty, tw, th, h, a);
        ArenaDialogKit.cardRimAfter(g, x, ty, tw, th, r, light, lit, dimmed, h, a);
        if (kind != Kind.ORDER) {
            drawBadgeAt(g, option, x, ty, tw, th, h, a);
        }
    }

    /** Numbers only where order matters: straddling the bottom edge of a pick, and on the rail in a row in order. */
    private void drawBadge(final Graphics g, final Option option, final int index, final float h, final float alpha) {
        final float raise = option.raised();
        final float scale = 1f + 0.04f * raise;
        final float d = ArenaDialogKit.badgeSize(h);
        final float tw = option.tw * scale;
        final float bx = option.overlapped ? option.px.get() - tw / 2f + d * 0.8f : option.px.get();
        ArenaDialogKit.badge(g, index + 1, bx, railY, d, alpha);
    }

    private void drawBadgeAt(final Graphics g, final Option option, final float x, final float y, final float tw, final float th, final float h,
            final float alpha) {
        if (!(max > 1 && picked.contains(option))) {
            return;
        }
        final int place = picked.indexOf(option) + 1;
        final float d = ArenaDialogKit.badgeSize(h);
        final float bx = option.overlapped ? x + d * 0.8f : x + tw / 2f;
        final float by = option.flat ? y + th - d / 2f : y + th;
        ArenaDialogKit.badge(g, place, bx, by, d, alpha);
    }

    /** A card's frame drawn on the GPU: dark body, frame in the card's colour, title bar, art, type bar and a parchment text box. */
    private void drawTileFace(final Graphics g, final Option option, final int index, final float x, final float y, final float tw, final float th,
            final float h, final float alpha) {
        final Color tint = option.tint;
        final Color line = ArenaTheme.frameLine(tint);
        final float arc = ArenaDialogKit.tileRadius(tw, th);
        g.fillSmoothRoundRect(ArenaTheme.fadeBy(TILE_BODY, alpha, S1), x, y, tw, th, arc);
        final float b = option.flat ? Math.max(2, Math.round(th * 0.03f)) : Math.max(2, Math.round(tw * 0.014f));
        final boolean pale = ArenaTheme.luma(line) > 170 / 255f;
        final Color frameTop = ArenaTheme.mix(line, ArenaTheme.WHITE, 0.22f, S2);
        frameTop.a = alpha;
        final Color frameBottom = ArenaTheme.mix(line, ArenaTheme.BLACK, pale ? 0.1f : 0.34f, S3);
        frameBottom.a = alpha;
        final float fr = Math.max(2f, arc - b);
        g.fillSmoothRoundRectGradient(frameTop, frameBottom, true, x + b, y + b, tw - 2 * b, th - 2 * b, fr);
        g.drawSmoothRoundRect(1f, ArenaTheme.fadeBy(FRAME_SHINE, alpha, S1), x + b, y + b, tw - 2 * b, th - 2 * b, fr);
        final float m = tileMargin(tw, th, option.flat);
        final float px, py, pw, ph;
        if (option.flat) {
            final FImageComplex art = option.art();
            final float artW = art == null ? 0 : Math.round((th - 2 * m) * 1.32f);
            if (art != null) {
                drawArt(g, option, art, x + m, y + m, artW, th - 2 * m, alpha);
            }
            px = art == null ? x + m : x + m + artW + m;
            py = y + m;
            pw = x + tw - m - px;
            ph = th - 2 * m;
        } else {
            final float k = th / (h * TILE_H); // the tile's own scale: under the pointer it grows a little
            final float gap = Math.max(2, Math.round(tw * 0.01f));
            final float barH = Math.round(h * 0.036f * k);
            drawBar(g, option.tileTitle, option.cost, option.costRenderer, x + m, y + m, tw - 2 * m, barH, h * 0.0185f, tint, alpha);
            final float artY = y + m + barH + gap, artH = Math.round(th * TILE_ART);
            final FImageComplex art = option.art();
            if (art != null) {
                drawArt(g, option, art, x + m, artY, tw - 2 * m, artH, alpha);
            } else {
                drawPlainArt(g, x + m, artY, tw - 2 * m, artH, index + 1, tint, alpha);
            }
            float ny = artY + artH + gap;
            if (option.kindWords != null) {
                final float kindH = Math.round(h * 0.028f * k);
                drawBar(g, option.kindWords, null, null, x + m, ny, tw - 2 * m, kindH, h * 0.0155f, tint, alpha);
                ny += kindH + gap;
            }
            // narrower than the art, so that the frame shows down both of its sides
            final float inset = m + Math.max(2, Math.round(tw * 0.016f));
            px = x + inset;
            py = ny + Math.max(1, gap / 2f);
            pw = tw - 2 * inset;
            ph = y + th - inset - py;
        }
        if (pw <= 4 || ph <= 4) {
            return;
        }
        final float inner = option.flat ? ArenaTheme.px(2.5f) : ArenaTheme.px(3f);
        final Color paperTop = ArenaTheme.mix(PAPER_TOP, line, 0.1f, S2);
        paperTop.a = alpha;
        final Color paperBottom = ArenaTheme.mix(PAPER_BOTTOM, line, 0.2f, S3);
        paperBottom.a = alpha;
        g.fillSmoothRoundRectGradient(paperTop, paperBottom, true, px, py, pw, ph, inner);
        g.drawSmoothRoundRect(1f, ArenaTheme.fadeBy(BOX_LINE, alpha, S1), px, py, pw, ph, inner);
        // the words, at the size and pitch all the tiles share
        if (tileFont != null) {
            final float padX = Math.round(pw * (option.flat ? 0.025f : 0.055f)), padY = Math.round(h * 0.006f);
            option.textRenderer.drawText(g, option.text(), tileFont, ArenaTheme.fadeBy(INK, alpha, S4), px + padX, py + padY, pw - 2 * padX,
                    ph - 2 * padY, py, ph, true, Align.left, option.flat);
        }
    }

    /** A bar across the frame in a pale cast of the card's colour, as a card's title and type lines; a title has its cost at the right end. */
    private static void drawBar(final Graphics g, final String words, final String cost, final TextRenderer costRenderer, final float x, final float y,
            final float w, final float bh, final float textSize, final Color tint, final float alpha) {
        if (bh <= 2 || w <= 4) {
            return;
        }
        final float arc = bh * 0.21f;
        final Color top = ArenaTheme.mix(tint, PARCHMENT, 0.72f, S2);
        top.a = alpha;
        final Color bottom = ArenaTheme.mix(tint, PARCHMENT, 0.5f, S3);
        bottom.a = alpha;
        g.fillSmoothRoundRectGradient(top, bottom, true, x, y, w, bh, arc);
        g.fillRect(ArenaTheme.fadeBy(BAR_SHINE, alpha, S1), x + arc, y + 1, w - 2 * arc, 1);
        g.drawSmoothRoundRect(1f, ArenaTheme.fadeBy(KEYLINE, alpha, S1), x, y, w, bh, arc);
        final float padX = Math.round(bh * 0.3f);
        float room = w - 2 * padX;
        if (cost != null && costRenderer != null) {
            final FSkinFont cf = ArenaDialogKit.font(bh * 0.62f);
            final float cw = MEASURE.getBounds(cost, cf).width;
            final float cx = x + w - cw - Math.max(2, Math.round(bh * 0.14f));
            costRenderer.drawText(g, cost, cf, ArenaTheme.fadeBy(BAR_INK, alpha, S4), cx, y, cw + 2, bh, y, bh, false, Align.left, true);
            room = cx - x - padX - Math.round(bh * 0.15f);
        }
        if (words != null && room > 8) {
            final FSkinFont f = ArenaDialogKit.fitFont(words, Math.min(textSize, bh * 0.8f), Math.max(ArenaTheme.px(9), bh * 0.45f), room);
            ArenaArt.textBold(g, ellipsize(words, f, room), f, ArenaTheme.fadeBy(BAR_INK, alpha, S4), x + padX, y, room, bh, Align.left);
        }
    }

    /** {@code words} shortened with an ellipsis to fit {@code room} (as is when it fits: no allocation then). */
    private static String ellipsize(final String words, final FSkinFont f, final float room) {
        if (ArenaArt.textWidth(words, f) <= room) {
            return words;
        }
        String s = words;
        while (s.length() > 1 && ArenaArt.textWidth(s + "…", f) > room) {
            s = s.substring(0, s.length() - 1);
        }
        return s + "…";
    }

    /** The art, cut to fill its window without stretching, with a dark keyline. */
    private void drawArt(final Graphics g, final Option option, final FImageComplex art, final float x, final float y, final float w,
            final float h, final float alpha) {
        if (w <= 2 || h <= 2) {
            return;
        }
        final TextureRegion region = option.crop(art, w / h);
        final float old = g.getfloatAlphaComposite();
        if (alpha < 1f) {
            g.setAlphaComposite(old * alpha);
        }
        if (region != null) {
            g.drawImage(region, x, y, w, h);
        } else {
            g.drawImage(art, x, y, w, h);
        }
        if (alpha < 1f) {
            g.setAlphaComposite(old);
        }
        g.drawSmoothRoundRect(1f, ArenaTheme.fadeBy(KEYLINE, alpha, S1), x, y, w, h, ArenaTheme.px(1.5f));
    }

    /** When no card is known: a dark plate in the frame's colour with the option's numeral, in place of art. */
    private static void drawPlainArt(final Graphics g, final float x, final float y, final float w, final float h, final int place,
            final Color tint, final float alpha) {
        if (w <= 2 || h <= 2) {
            return;
        }
        final Color top = ArenaTheme.mix(tint, ArenaTheme.BLACK, 0.4f, S2);
        top.a = alpha;
        final Color bottom = ArenaTheme.mix(tint, ArenaTheme.BLACK, 0.82f, S3);
        bottom.a = alpha;
        g.fillSmoothRoundRectGradient(top, bottom, true, x, y, w, h, ArenaTheme.px(1.5f));
        g.drawSmoothRoundRect(1f, ArenaTheme.fadeBy(KEYLINE, alpha, S1), x, y, w, h, ArenaTheme.px(1.5f));
        final float cx = x + w / 2f, cy = y + h / 2f, ring = h * 0.36f;
        g.drawSmoothCircle(Math.max(1.5f, h * 0.012f), ArenaTheme.alpha(ArenaTheme.BLACK, 0.35f * alpha, S1), cx, cy + 1, ring);
        g.drawSmoothCircle(Math.max(1.5f, h * 0.012f), ArenaTheme.alpha(ArenaTheme.GOLD, (150 / 255f) * alpha, S1), cx, cy, ring);
        g.drawSmoothCircle(1f, ArenaTheme.alpha(ArenaTheme.GOLD, (60 / 255f) * alpha, S1), cx, cy, ring * 1.16f);
        final String numeral = place <= NUMERALS.length ? NUMERALS[place - 1] : String.valueOf(place);
        final FSkinFont f = ArenaDialogKit.font(ring * 0.95f);
        g.drawText(numeral, f, ArenaTheme.alpha(ArenaTheme.BLACK, 0.55f * alpha, S1), cx - ring + 1, cy - ring + 2, 2 * ring, 2 * ring, false, Align.center, true);
        ArenaArt.textBold(g, numeral, f, ArenaTheme.alpha(ArenaTheme.GOLD_BRIGHT, alpha, S1), cx - ring, cy - ring, 2 * ring, 2 * ring, Align.center);
    }

    private void drawStepper(final Graphics g, final float w, final float h, final float alpha) {
        final float cx = w / 2f, cy = stepperLine(w, h);
        final FSkinFont f = numeralFont(h);
        final String n = numbers.get(numberIndex).label;
        final float lh = f.getLineHeight();
        g.drawText(n, f, ArenaTheme.alpha(ArenaTheme.BLACK, (170 / 255f) * alpha, S1), cx - w / 2f, cy - lh / 2f + 2, w, lh, false, Align.center, true);
        ArenaArt.textOutlined(g, n, f, ArenaTheme.alpha(ArenaTheme.WHITE, alpha, S2), cx - w / 2f, cy - lh / 2f, w, lh, Align.center);
        final FSkinFont rf = ArenaDialogKit.font(h * 0.024f);
        ArenaDialogKit.edged(g, rangeText, rf, ArenaTheme.alpha(ArenaTheme.WHITE, alpha, S3), 0, cy + f.getCapHeight() / 2f + Math.round(h * 0.03f), w,
                rf.getLineHeight(), Align.center);
        final boolean atLow = numberIndex == 0, atHigh = numberIndex == numbers.size() - 1;
        ArenaDialogKit.roundButton(g, stepLower.x + stepLower.width / 2f, stepLower.y + stepLower.height / 2f, stepLower.width - 2, -1, 0,
                stepHover < 0 && !atLow, atLow, alpha);
        ArenaDialogKit.roundButton(g, stepHigher.x + stepHigher.width / 2f, stepHigher.y + stepHigher.height / 2f, stepHigher.width - 2, 1, 0,
                stepHover > 0 && !atHigh, atHigh, alpha);
    }

    /** A thin gold ring round the option the arrow keys are on. */
    private void drawFocus(final Graphics g, final Option option, final float alpha) {
        final float h = getHeight();
        final float o = Math.max(2f, ArenaTheme.px(5));
        if (style == Style.PILLS) {
            final Rectangle b = option.box;
            final float y = b.y - scroll.get();
            g.drawSmoothRoundRect(Math.max(1.5f, ArenaTheme.px(2)), ArenaTheme.alpha(ArenaTheme.GOLD_BRIGHT, 0.85f * alpha, S1), b.x - o / 2f,
                    y - o / 2f, b.width + o, b.height + o, (b.height + o) / 2f);
        } else if (style == Style.TILES || style == Style.GRID) {
            final float raise = option.raised();
            final float lift = option.flat ? 0f : h * HOVER_LIFT * raise;
            final float scale = option.flat ? 1f : 1f + 0.04f * raise;
            final float tw = option.tw * scale, th = option.th * scale;
            final float cy = option.cy - (style == Style.GRID ? scroll.get() : 0f) - lift;
            final float reach = Math.max(4f, Math.round(h * 0.012f));
            g.drawSmoothRoundRect(Math.max(1.5f, ArenaTheme.px(2)), ArenaTheme.alpha(ArenaTheme.GOLD_BRIGHT, 0.75f * alpha, S1),
                    option.px.get() - tw / 2f - reach, cy - th / 2f - reach, tw + 2 * reach, th + 2 * reach, ArenaDialogKit.tileRadius(tw, th) + reach);
        }
    }

    // ---- input ------------------------------------------------------------------------------------------------------

    private Option optionAt(final float x, final float y) {
        switch (style) {
            case PILLS: {
                if (listScrolls && !area.contains(x, y)) {
                    return null;
                }
                final float sy = y + scroll.get();
                for (final Option option : options) {
                    if (option.box.contains(x, sy)) {
                        return option;
                    }
                }
                return null;
            }
            case TILES: {
                // the raised and the rightmost lie on top
                for (int i = options.size() - 1; i >= 0; i--) {
                    final Option option = options.get(i);
                    if (option.isRaised() && option.contains(x, y, 0f)) {
                        return option;
                    }
                }
                for (int i = options.size() - 1; i >= 0; i--) {
                    final Option option = options.get(i);
                    if (option.contains(x, y, 0f)) {
                        return option;
                    }
                }
                return null;
            }
            case GRID: {
                if (!area.contains(x, y)) {
                    return null;
                }
                for (final Option option : options) {
                    if (option.contains(x, y, scroll.get())) {
                        return option;
                    }
                }
                return null;
            }
            default:
                return null;
        }
    }

    private void setHovered(final Option option) {
        for (final Option o : options) {
            final boolean on = o == option;
            if (o.hovered != on) {
                o.hovered = on;
                o.hover.to(on ? 1f : 0f, on ? 110 : 170);
            }
        }
        ArenaMotion.requestFrame();
    }

    @Override
    protected void onPointer(final float x, final float y, final boolean onButton) {
        if (dragging != null) {
            return;
        }
        final Option over = onButton ? null : optionAt(x, y);
        setHovered(kind == Kind.REVEAL && style == Style.PILLS ? null : over);
        if (over != null) {
            focus = null; // the mouse took over from the keys
        }
        if (style == Style.STEPPER) {
            stepHover = onButton ? 0 : stepLower.contains(x, y) ? -1 : stepHigher.contains(x, y) ? 1 : 0;
        }
        rememberHover = offerRemember && rememberBox.contains(x, y);
    }

    @Override
    protected void onTap(final float x, final float y, final int count) {
        if (offerRemember && rememberBox.contains(x, y)) {
            remember = !remember;
            ArenaMotion.requestFrame();
            return;
        }
        if (style == Style.STEPPER) {
            return; // stepped on press
        }
        if (listScrolls && style != Style.TILES) {
            final float d = Math.round(getHeight() * 0.044f), cx = area.x + area.width / 2f;
            if (Math.abs(x - cx) < d && Math.abs(y - (area.y + d / 2f + 4)) < d / 2f && scroll.get() > 1f) {
                scrollBy(-area.height * 0.6f);
                return;
            }
            if (Math.abs(x - cx) < d && Math.abs(y - (area.y + area.height - d / 2f - 4)) < d / 2f && scroll.get() < maxScroll() - 1f) {
                scrollBy(area.height * 0.6f);
                return;
            }
        }
        if (dragMoved) {
            return;
        }
        final Option option = optionAt(x, y);
        if (option != null) {
            onOptionClicked(option, count);
        }
    }

    @Override
    protected void onPress(final float x, final float y) {
        if (style == Style.STEPPER) {
            final int dir = stepLower.contains(x, y) ? -1 : stepHigher.contains(x, y) ? 1 : 0;
            if (dir != 0) {
                stepNumber(dir);
                stepHeld = dir;
                scheduleRepeat(380);
            }
            return;
        }
        if (kind == Kind.ORDER) {
            final Option option = optionAt(x, y);
            if (option != null) {
                dragging = option;
                dragGrabX = x - option.px.get();
                dragDownX = x;
                dragMoved = false;
                setHovered(option);
            }
        }
    }

    @Override
    protected void onRelease(final float x, final float y) {
        stopRepeat();
        endDrag();
    }

    @Override
    protected void onDrag(final float x, final float y, final float dx, final float dy) {
        if (dragging == null) {
            return;
        }
        if (!dragMoved && Math.abs(x - dragDownX) < ArenaTheme.px(6)) {
            return;
        }
        dragMoved = true;
        final float w = getWidth();
        final float cx = Math.max(dragging.tw / 2f, Math.min(w - dragging.tw / 2f, x - dragGrabX));
        dragging.px.set(cx);
        int index = 0;
        for (final Option other : options) {
            if (other != dragging && other.px.target() < cx) {
                index++;
            }
        }
        if (options.indexOf(dragging) != index) {
            options.remove(dragging);
            options.add(index, dragging);
            layoutTiles(w, getHeight(), true);
        }
        ArenaMotion.requestFrame();
    }

    @Override
    protected void onDragEnd(final float x, final float y) {
        endDrag();
    }

    private void endDrag() {
        if (dragging == null) {
            return;
        }
        dragging = null;
        layoutTiles(getWidth(), getHeight(), false);
        ArenaMotion.requestFrame();
        // a tap that did not move is not a drag: let the next tap through
        if (dragMoved) {
            ArenaMotion.play(60, ArenaMotion.LINEAR, null, () -> dragMoved = false);
        }
    }

    @Override
    protected void onScroll(final float x, final float y, final float dx, final float dy) {
        if (style == Style.STEPPER) {
            stepNumber(dy > 0 ? 1 : dy < 0 ? -1 : 0); // the wheel up counts up
            return;
        }
        if (style == Style.PILLS || style == Style.GRID) {
            scrollBy(-dy * 1.5f);
        }
    }

    @Override
    protected void onArrow(final int dx, final int dy) {
        if (style == Style.STEPPER) {
            stepNumber(dx != 0 ? dx : -dy);
            return;
        }
        if (style == Style.ANSWERS || options.isEmpty()) {
            return;
        }
        final int columns = style == Style.GRID ? 2 : style == Style.TILES ? options.size() : pillColumns();
        int at = focus == null ? -1 : options.indexOf(focus);
        if (at < 0) {
            at = 0;
        } else {
            at += dx + dy * columns;
            at = Math.max(0, Math.min(options.size() - 1, at));
        }
        focus = options.get(at);
        setHovered(focus);
        if (style == Style.PILLS || style == Style.GRID) {
            // keep it in view
            final float top = style == Style.PILLS ? focus.box.y : focus.cy - focus.th / 2f;
            final float bottom = style == Style.PILLS ? focus.box.y + focus.box.height : focus.cy + focus.th / 2f;
            if (top - scroll.target() < area.y) {
                scroll.to(Math.max(0f, top - area.y - listStep * 0.25f));
            } else if (bottom - scroll.target() > area.y + area.height) {
                scroll.to(Math.min(maxScroll(), bottom - area.y - area.height + listStep * 0.25f));
            }
        }
    }

    private int pillColumns() {
        if (options.size() < 2) {
            return 1;
        }
        int n = 1;
        final float y0 = options.get(0).box.y;
        while (n < options.size() && options.get(n).box.y == y0) {
            n++;
        }
        return n;
    }

    private void stepNumber(final int by) {
        if (viewingBattlefield || by == 0) {
            return;
        }
        numberIndex = Math.max(0, Math.min(numbers.size() - 1, numberIndex + by));
        ArenaMotion.requestFrame();
    }

    /** Holding a step button keeps counting: after a pause, every 90 ms. */
    private void scheduleRepeat(final int delayMs) {
        stopRepeat();
        stepRepeat = ArenaMotion.play(1, delayMs, ArenaMotion.LINEAR, null, () -> {
            if (stepHeld != 0 && !isClosed()) {
                stepNumber(stepHeld);
                scheduleRepeat(90);
            }
        });
    }

    private void stopRepeat() {
        stepHeld = 0;
        if (stepRepeat != null) {
            stepRepeat.cancel();
            stepRepeat = null;
        }
    }

    @Override
    public boolean keyDown(final int keyCode) {
        // Space picks (or puts back) the option the arrow keys are on, when several may be picked
        if (keyCode == Keys.SPACE && !viewingBattlefield && kind == Kind.CHOICE && max > 1 && focus != null) {
            onOptionClicked(focus, 1);
            return true;
        }
        return super.keyDown(keyCode);
    }

    @Override
    protected void onSubmitKey() {
        if (kind == Kind.BUTTONS) {
            for (final Option option : options) {
                if (focus == option) {
                    answer(option);
                    return;
                }
            }
            for (final Option option : options) {
                if (option.primary) {
                    answer(option);
                    return;
                }
            }
            return;
        }
        if (kind == Kind.CHOICE && max == 1 && focus != null && style != Style.STEPPER) {
            picked.clear();
            picked.add(focus);
            updateButtons();
            close();
            return;
        }
        submit();
    }

    @Override
    protected void onEscapeKey() {
        if (btnCancel.visible) {
            cancel();
        } else if (kind == Kind.BUTTONS && options.size() > 1) {
            answer(options.get(options.size() - 1)); // as the classic dialog: the last answer, "No" or "Cancel"
        } else if (kind == Kind.BUTTONS || kind == Kind.REVEAL) {
            if (kind == Kind.BUTTONS) {
                answer(options.get(0));
            } else {
                close();
            }
        }
    }

    @Override
    protected CardView cardAt(final float x, final float y) {
        final Option option = optionAt(x, y);
        if (option != null) {
            return option.card();
        }
        if (focusCard != null) {
            final Rectangle r = SCRATCH_RECT;
            if (kind == Kind.BUTTONS) {
                r.set(questionCard);
            } else if (style == Style.STEPPER) {
                aloneCard(getWidth(), getHeight(), r);
            } else {
                ArenaDialogKit.sourceBounds(getWidth(), getHeight(), r);
            }
            if (r.contains(x, y)) {
                return focusCard;
            }
        }
        return null;
    }

    // ---- one option -------------------------------------------------------------------------------------------------

    /** One option: a pill for a short answer, or a tile in a card's frame with a piece of art and its text. */
    private final class Option {
        final int index;
        final Object value;
        final String label;
        boolean primary;
        boolean hovered;
        ArenaDialogKit.Pill answerPill;
        final ArenaMotion.Fade hover = new ArenaMotion.Fade(0f);
        final ArenaMotion.Fade rise = new ArenaMotion.Fade(0f);
        /** The middle of the tile across the window; it glides when the row is put in a new order. */
        final ArenaMotion.Spring px = new ArenaMotion.Spring(0f, 55, 0.25f);
        float cy, tw, th;
        boolean flat, overlapped, placed;
        /** PILLS: where the pill lies, in window coordinates before scrolling. */
        final Rectangle box = new Rectangle();
        // the face's words, worked out once
        final String tileTitle, kindWords, cost;
        final Color tint;
        final TextRenderer textRenderer = new TextRenderer(), costRenderer = new TextRenderer();
        private String text;
        private FImageComplex cropFrom;
        private float cropAspect;
        private TextureRegion crop;

        Option(final int index, final Object value, final String label) {
            this.index = index;
            this.value = value;
            this.label = label == null ? "" : label.trim();
            this.tileTitle = tileTitle();
            this.kindWords = tileKind();
            final ManaCost c = tileCost();
            this.cost = c == null ? null : c.toString();
            this.tint = tileTint();
        }

        void place(final float cx, final float cy0, final float tw0, final float th0, final boolean flat0, final boolean moveX) {
            cy = cy0;
            tw = tw0;
            th = th0;
            flat = flat0;
            if (!placed) {
                px.set(cx);
                placed = true;
            } else if (moveX) {
                px.to(cx);
            }
        }

        /** How far the pointer has raised it; a picked tile stays on the row's baseline. */
        float raised() {
            return hover.get() * (1f - rise.get());
        }

        boolean isRaised() {
            final float goal = picked.contains(this) ? 1f : 0f;
            if (rise.goal() != goal) {
                rise.to(goal, 170);
            }
            return (style == Style.TILES || style == Style.GRID) && (hover.get() > 0f || hovered || this == dragging);
        }

        boolean contains(final float x, final float y, final float scrolled) {
            if (tw <= 0) {
                return false;
            }
            final float cx = px.get(), c = cy - scrolled;
            if (Math.abs(x - cx) <= tw / 2f && Math.abs(y - c) <= th / 2f) {
                return true;
            }
            final float lift = flat ? 0f : getHeight() * HOVER_LIFT * raised();
            return lift > 0f && Math.abs(x - cx) <= tw / 2f && y <= c + th / 2f && y >= c - lift - th / 2f;
        }

        /** The card this option comes from, if any. */
        CardView card() {
            if (value instanceof CardView) {
                return (CardView) value;
            }
            if (value instanceof SpellAbilityView) {
                return ((SpellAbilityView) value).getHostCard();
            }
            if (value instanceof IHasCardView) {
                return ((IHasCardView) value).getCardView();
            }
            return null;
        }

        private CardView artCard() {
            final CardView card = card();
            return card != null ? card : focusCard;
        }

        FImageComplex art() {
            final CardView card = artCard();
            if (card == null || card.getCurrentState() == null) {
                return null;
            }
            try {
                return CardRenderer.getCardArt(card);
            } catch (final RuntimeException e) {
                return null;
            }
        }

        /** The part of the art that fills a window of this shape (cut once, kept until the art or the shape changes). */
        TextureRegion crop(final FImageComplex art, final float aspect) {
            if (art == cropFrom && Math.abs(aspect - cropAspect) < 0.01f && crop != null) {
                return crop;
            }
            final TextureRegion region = art.getTextureRegion();
            if (region == null) {
                return null;
            }
            final int rw = region.getRegionWidth(), rh = region.getRegionHeight();
            if (rw <= 0 || rh <= 0) {
                return null;
            }
            int cw = rw, ch = rh;
            if (rw / (float) rh > aspect) {
                cw = Math.max(1, Math.round(rh * aspect));
            } else {
                ch = Math.max(1, Math.round(rw / aspect));
            }
            crop = new TextureRegion(region, (rw - cw) / 2, (rh - ch) / 2, cw, ch);
            cropFrom = art;
            cropAspect = aspect;
            return crop;
        }

        private String cardName() {
            final CardView card = card();
            return card == null || card.getCurrentState() == null ? null : CardTranslation.getTranslatedName(card.getCurrentState().getName());
        }

        /** What an option in order says before its colon, when it comes as a string and names what it belongs to. */
        private String namedPrefix() {
            if (kind != Kind.ORDER || card() != null) {
                return null;
            }
            final int colon = label.indexOf(':');
            final String head = colon > 0 && colon <= 30 ? label.substring(0, colon) : "";
            return !head.isEmpty() && !head.contains("{") && Character.isUpperCase(head.charAt(0)) ? head : null;
        }

        /** The words on the tile's title bar: the card's name, or failing that which option this is. */
        private String tileTitle() {
            final String name = cardName(), prefix = namedPrefix();
            if (name != null) {
                return name;
            }
            if (prefix != null) {
                return prefix;
            }
            if (kind == Kind.ORDER) {
                return Forge.getLocalizer().getMessage("lblArenaAbility");
            }
            if (focusCard != null && focusCard.getCurrentState() != null) {
                return CardTranslation.getTranslatedName(focusCard.getCurrentState().getName());
            }
            return Forge.getLocalizer().getMessage("lblArenaModeN", index + 1);
        }

        /** The type line under the art: of the card itself, or of the card a mode or ability belongs to. */
        private String tileKind() {
            final CardView card = artCard();
            final CardStateView state = card == null ? null : card.getCurrentState();
            if (state == null || state.getType() == null) {
                return null;
            }
            final String type = state.getType().toString();
            return type.isEmpty() ? null : type.replace(" - ", " — ");
        }

        /** The cost on the title bar: of that same card, when it has one. */
        private ManaCost tileCost() {
            final CardView card = artCard();
            final CardStateView state = card == null ? null : card.getCurrentState();
            final ManaCost c = state == null ? null : state.getManaCost();
            return c == null || c.isNoCost() ? null : c;
        }

        /** The frame takes the colour of the card behind the option; one with no colour of its own takes its identity's. */
        private Color tileTint() {
            final CardView card = artCard();
            final CardStateView state = card == null ? null : card.getCurrentState();
            if (state == null) {
                return NO_CARD_FRAME;
            }
            final ColorSet own = state.getColors();
            if (state.isLand() || (own != null && !own.isColorless())) {
                return ArenaTheme.tileTint(state);
            }
            final Color identity = identityTint(state);
            return identity == null ? ArenaTheme.tileTint(state) : identity;
        }

        /** The words for the tile, without the card's name where the tile already shows it. */
        String text() {
            if (text != null) {
                return text;
            }
            String t = label;
            if (value instanceof CardView) {
                final CardView c = (CardView) value;
                if (c.getCurrentState() != null && c.getCurrentState().getOracleText() != null && !c.getCurrentState().getOracleText().isEmpty()) {
                    t = c.getCurrentState().getOracleText();
                }
            } else {
                final String prefix = namedPrefix();
                final String name = cardName();
                if (prefix != null && label.length() > prefix.length() + 1 && !label.substring(prefix.length() + 1).trim().isEmpty()) {
                    t = label.substring(prefix.length() + 1).trim();
                } else if (name != null && label.startsWith(name)) {
                    final String rest = label.substring(name.length()).replaceFirst("^\\s*[-:\\u2014]\\s*", "").trim();
                    if (!rest.isEmpty()) {
                        t = rest;
                    }
                }
            }
            text = t;
            return text;
        }
    }

    /** The frame's colour from a card's colour identity (the symbols in its cost and rules); none when it has none. */
    private static Color identityTint(final CardStateView state) {
        int mask = state.getManaCost() == null ? 0 : state.getManaCost().getColorProfile();
        if (state.getOracleText() != null) {
            final Matcher symbols = BRACED.matcher(state.getOracleText());
            while (symbols.find()) {
                final String symbol = symbols.group(1);
                mask |= (symbol.indexOf('W') >= 0 ? MagicColor.WHITE : 0) | (symbol.indexOf('U') >= 0 ? MagicColor.BLUE : 0)
                        | (symbol.indexOf('B') >= 0 ? MagicColor.BLACK : 0) | (symbol.indexOf('R') >= 0 ? MagicColor.RED : 0)
                        | (symbol.indexOf('G') >= 0 ? MagicColor.GREEN : 0);
            }
        }
        final ColorSet identity = ColorSet.fromMask(mask);
        if (identity.isColorless()) {
            return null;
        }
        if (identity.isMulticolor()) {
            return TINT_MULTI;
        }
        return identity.hasWhite() ? TINT_W : identity.hasBlue() ? TINT_U : identity.hasBlack() ? TINT_B : identity.hasRed() ? TINT_R : TINT_G;
    }
}
