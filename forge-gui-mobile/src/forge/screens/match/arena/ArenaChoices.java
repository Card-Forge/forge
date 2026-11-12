package forge.screens.match.arena;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import forge.Forge;
import forge.game.GameEntityView;
import forge.game.card.CardFaceView;
import forge.game.card.CardView;
import forge.game.spellability.SpellAbilityView;
import forge.gamemodes.match.PromptContext;
import forge.gui.interfaces.IGuiGame.OrderResult;
import forge.util.CardTranslation;

/**
 * Where the game's questions go when the Arena look is on the match screen (port of the Swing {@code CMatchUI}
 * routing): cards to pick from, reveal or put in order go to the {@link ArenaCardBrowser}, everything else that fits to
 * the {@link ArenaOptionsOverlay}. Each method runs on the render thread and returns false (opening nothing) when the
 * Arena overlays do not apply, so the caller carries on with its classic dialog, which the Arena widget skins then
 * dress for the match. The shared toolbox (GuiChoose, FOptionPane) calls these first.
 */
public final class ArenaChoices {
    private ArenaChoices() { }

    // what the game says the next question is about (IGuiGame.setPromptContext): set on the game thread, read on the
    // render thread when the dialog opens
    private static volatile PromptContext context = PromptContext.NONE;

    /** What the next question is about: the card whose effect asks, or why when no card does, and what picks are for. */
    public static void setContext(final PromptContext context0) {
        context = context0 == null ? PromptContext.NONE : context0;
    }

    /** What the cards the next dialog picks are for, so its button can say it ("Discard 2"). */
    static PromptContext.Verb verb() {
        return context.verb();
    }

    /**
     * Whether the question being asked has a verb (its main button says what it does, "Put 2 on bottom"): the board's
     * prompt then keeps that button in view, dim, until enough is picked, rather than hiding it.
     */
    public static boolean asksWithVerb() {
        return context.verb() != PromptContext.Verb.CHOOSE;
    }

    /**
     * The card a dialog stands beside as what asks: the one the game named, none when the game gave only a reason (a
     * discard to hand size has no card behind it), else the card of the prompt on the board.
     */
    static CardView sourceCard() {
        final PromptContext now = context;
        if (now.source() != null) {
            return now.source();
        }
        return now.reason() != null && !now.reason().isEmpty() ? null : ArenaDialogKit.promptCard();
    }

    /**
     * A dialog's title saying what it is for: the reason the game gave, or "Fireball: Choose a Number" when a card's
     * effect asks (as a modal spell's "Abzan Charm: choose a mode"), else the plain title.
     */
    static String titleFor(final String title) {
        final PromptContext now = context;
        final String reason = now.reason();
        if (reason != null && !reason.isEmpty()) {
            return reason;
        }
        final CardView source = now.source();
        if (source == null || source.getCurrentState() == null) {
            return title;
        }
        final String name = CardTranslation.getTranslatedName(source.getCurrentState().getName());
        return name == null || name.isEmpty() ? title : Forge.getLocalizer().getMessage("lblArenaTitleFor", name, title);
    }

    /** GuiChoose.getChoices: pick between min and max (max below zero: only reveal), the picks in the order made. */
    @SuppressWarnings("unchecked")
    public static <T> boolean getChoices(final String message, final int min, final int max, final Collection<T> choices0, final Collection<T> selected,
            final Function<T, String> display, final Consumer<List<T>> callback0) {
        if (!ArenaTheme.isMatchBoard() || choices0 == null || choices0.isEmpty()) {
            return false;
        }
        final Consumer<List<T>> callback = callback0 != null ? callback0 : r -> { }; // a reveal may want no answer
        final List<T> choices = new ArrayList<>(choices0);
        if (isAllCards(choices)) {
            final List<CardView> cards = (List<CardView>) choices;
            if (max < 0 || (min < 0 && max <= 0)) { // nothing to choose: the cards are only being revealed
                return ArenaCardBrowser.reveal(message, cards, () -> callback.accept(new ArrayList<>()));
            }
            return ArenaCardBrowser.choose(titleFor(chooseTitle(min, max, cards.size())), message, Math.max(0, min), max, cards,
                    (Collection<CardView>) selected, picks -> callback.accept((List<T>) picks));
        }
        if (max < 0) {
            return ArenaOptionsOverlay.choose(Forge.getLocalizer().getMessage("lblRevealed"), message, 0, -1, choices, null, display, null,
                    picks -> callback.accept(new ArrayList<>()));
        }
        return ArenaOptionsOverlay.choose(titleFor(optionsTitle(min, max, choices)), message, Math.max(0, min), max, choices, selected, display, null,
                callback);
    }

    /**
     * chooseSingleEntityForEffect with cards revealed first (a search): the browser shows what was revealed (Done), then
     * the choice opens over the match. Render thread.
     */
    public static boolean chooseEntity(final String title, final List<? extends GameEntityView> options, final String revealMessage,
            final Collection<CardView> revealed, final boolean optional, final Consumer<GameEntityView> callback) {
        if (!ArenaTheme.isMatchBoard() || options == null) {
            return false;
        }
        final List<CardView> shown = revealed == null ? new ArrayList<>() : new ArrayList<>(revealed);
        final Runnable choose = () -> {
            if (options.isEmpty()) {
                callback.accept(null);
                return;
            }
            final List<GameEntityView> list = new ArrayList<>(options);
            final Consumer<List<GameEntityView>> picked = r -> callback.accept(r == null || r.isEmpty() ? null : r.get(0));
            if (!getChoices(title, optional ? 0 : 1, 1, list, null, null, picked)) {
                forge.toolbox.GuiChoose.getChoices(title, optional ? 0 : 1, 1, list, null, null, picked);
            }
        };
        if (shown.isEmpty() || !ArenaCardBrowser.reveal(revealMessage, shown, choose)) {
            choose.run();
        }
        return true;
    }

    /** An ability to play from a card: the card stands beside its abilities. */
    public static boolean chooseAbility(final CardView host, final List<SpellAbilityView> abilities, final Consumer<SpellAbilityView> callback0) {
        final Consumer<SpellAbilityView> callback = callback0 != null ? callback0 : r -> { };
        return ArenaOptionsOverlay.choose(Forge.getLocalizer().getMessage("lblArenaChooseAbility"),
                Forge.getLocalizer().getMessage("lblChooseAbilityToPlay"), 0, 1, abilities, null, null, host,
                picks -> callback.accept(picks.isEmpty() ? null : picks.get(0)));
    }

    /**
     * GuiChoose.order: putting everything in order drags cards along a row or options into order; taking some of them
     * picks them in order. False when the classic dual list is the better fit (sideboarding, very long lists).
     */
    @SuppressWarnings("unchecked")
    public static <T> boolean order(final String title, final String top, final int remainingObjectsMin, final int remainingObjectsMax,
            final List<T> sourceChoices, final List<T> destChoices, final CardView referenceCard, final boolean showRememberCheckbox,
            final Consumer<OrderResult<T>> callback0) {
        if (!ArenaTheme.isMatchBoard()) {
            return false;
        }
        final Consumer<OrderResult<T>> callback = callback0 != null ? callback0 : r -> { };
        final List<T> all = new ArrayList<>();
        if (destChoices != null) {
            all.addAll(destChoices);
        }
        if (sourceChoices != null) {
            all.addAll(sourceChoices);
        }
        if (all.isEmpty()) {
            return false;
        }
        final boolean cards = isAllCards(all);
        if (!cards && all.size() > ArenaOptionsOverlay.MAX_OPTIONS) {
            return false;
        }
        if (remainingObjectsMax == 0) { // every one of them goes into the order
            if (all.size() == 1) {
                callback.accept(new OrderResult<>(all, false));
                return true;
            }
            if (cards && !showRememberCheckbox) {
                return ArenaCardBrowser.order(title, top, (List<CardView>) all, ordered -> callback.accept(new OrderResult<>((List<T>) ordered, false)));
            }
            return ArenaOptionsOverlay.order(title, top, all, referenceCard, showRememberCheckbox,
                    (ordered, remember) -> callback.accept(new OrderResult<>(ordered, remember)));
        }
        // some of them are taken, in the order picked
        final int pickMin = remainingObjectsMax < 0 ? 0 : Math.max(0, all.size() - remainingObjectsMax);
        final int pickMax = remainingObjectsMin < 0 ? all.size() : Math.max(pickMin, all.size() - remainingObjectsMin);
        if (pickMax <= 0) {
            return false;
        }
        if (cards) {
            return ArenaCardBrowser.choose(chooseTitle(pickMin, pickMax, all.size()), title, pickMin, pickMax, (List<CardView>) all,
                    (Collection<CardView>) destChoices, picks -> callback.accept(new OrderResult<>((List<T>) picks, false)));
        }
        return ArenaOptionsOverlay.choose(optionsTitle(pickMin, pickMax, all), title, pickMin, pickMax, all, destChoices, null, referenceCard,
                picks -> callback.accept(new OrderResult<>(picks, false)));
    }

    /**
     * FOptionPane.showOptionDialog: a question or a notice answered with one of a few buttons. {@code standardIcon} is
     * whether the dialog's picture is one of the plain question/information/warning/error marks: a dialog that shows a
     * picture of its own (an achievement's trophy) stays classic, so the picture is not lost.
     */
    public static boolean ask(final String message, final String title, final boolean standardIcon, final List<String> options,
            final int defaultOption, final Consumer<Integer> callback) {
        if (!standardIcon || !ArenaTheme.isMatchBoard()) {
            return false;
        }
        final String heading = ArenaDialogKit.dialogTitle(title, options != null && options.size() == 1 ? "lblArenaNotice" : "lblArenaQuestion");
        return ArenaOptionsOverlay.ask(heading, message, options, defaultOption, null, callback);
    }

    /** FOptionPane.showCardOptionDialog: a question about a card, the card large above the answers. */
    public static boolean askAboutCard(final CardView card, final String message, final String title, final List<String> options,
            final int defaultOption, final Consumer<Integer> callback) {
        if (!ArenaTheme.isMatchBoard()) {
            return false;
        }
        String heading = title;
        if (heading == null || heading.trim().isEmpty()) {
            heading = card != null && card.getCurrentState() != null ? CardTranslation.getTranslatedName(card.getCurrentState().getName())
                    : Forge.getLocalizer().getMessage("lblArenaQuestion");
        }
        String text = message;
        if (text == null || text.trim().isEmpty()) {
            text = title;
        }
        return ArenaOptionsOverlay.ask(heading, text, options, defaultOption, card != null ? card : sourceCard(), callback);
    }

    // ---- titles -----------------------------------------------------------------------------------------------------

    static boolean isAllCards(final Collection<?> items) {
        if (items == null || items.isEmpty()) {
            return false;
        }
        for (final Object o : items) {
            if (!(o instanceof CardView)) {
                return false;
            }
        }
        return true;
    }

    static String optionsTitle(final int min, final int max, final List<?> choices) {
        if (max == 1 && ArenaOptionsOverlay.isNumberList(choices)) {
            return Forge.getLocalizer().getMessage("lblArenaChooseNumber");
        }
        if (max == 1 && !choices.isEmpty() && choices.get(0) instanceof SpellAbilityView) {
            return Forge.getLocalizer().getMessage("lblArenaChooseAbility");
        }
        if (max == 1 && !choices.isEmpty() && choices.get(0) instanceof CardFaceView) {
            return Forge.getLocalizer().getMessage("lblArenaNameACard");
        }
        if (max == 1) {
            return Forge.getLocalizer().getMessage("lblArenaChooseOne");
        }
        return min == max ? Forge.getLocalizer().getMessage("lblArenaChooseN", max) : Forge.getLocalizer().getMessage("lblArenaChooseUpTo", max);
    }

    static String chooseTitle(final int min, final int max, final int count) {
        if (max == 1) {
            return Forge.getLocalizer().getMessage("lblArenaChooseOne");
        }
        if (max >= count && min == 0) {
            return Forge.getLocalizer().getMessage("lblArenaChooseCards");
        }
        return min == max ? Forge.getLocalizer().getMessage("lblArenaChooseN", max) : Forge.getLocalizer().getMessage("lblArenaChooseUpTo", max);
    }
}
