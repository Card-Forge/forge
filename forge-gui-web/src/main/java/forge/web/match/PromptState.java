package forge.web.match;

import forge.game.card.CardView;
import forge.game.player.PlayerView;
import forge.game.spellability.StackItemView;
import forge.gamemodes.match.Selection;
import forge.gamemodes.net.DeltaPacket;
import forge.trackable.TrackableObject;
import forge.util.Localizer;
import forge.web.protocol.ToBrowser.ButtonKind;
import forge.web.protocol.ToBrowser.Prompt;
import forge.web.protocol.ToBrowser.PromptButton;
import forge.web.protocol.ToBrowser.Ref;
import forge.web.protocol.ToBrowser.StarterChoice;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** The prompt shown to the browser. Both the dispatch thread and the host UI thread change it, so every change is made and sent under one lock. */
final class PromptState {
    // Every web seat runs in the host's process with the host's Localizer, so the engine's labels match these keys
    private static final Map<ButtonKind, String> KIND_KEYS = Map.of(ButtonKind.ok, "lblOK", ButtonKind.auto, "lblAuto",
            ButtonKind.endTurn, "lblEndTurn", ButtonKind.alphaStrike, "lblAlphaStrike", ButtonKind.callBack, "lblCallBack");

    private final Consumer<Prompt> send;
    private String message = "";
    /** Set while the player is asked who starts. It says whether the player won the toss or lost the last game. */
    private StarterChoice starterChoice;
    private boolean priority;
    private Ref card;
    private PromptButton ok = new PromptButton("", false, null);
    private PromptButton cancel = new PromptButton("", false, null);
    private boolean focusOk;
    private boolean paying;
    private boolean endsTurn;
    private List<Ref> selectable = List.of();
    private int selectableMin;
    private List<Ref> selectablePlayers = List.of();
    private List<Integer> selectableStack = List.of();
    private List<Integer> highlighted = List.of();

    PromptState(final Consumer<Prompt> send) {
        this.send = send;
    }

    /** Sends the current prompt to one browser. A reconnecting browser needs this. */
    synchronized void sendTo(final Consumer<Prompt> to) {
        to.accept(current());
    }

    synchronized void message(final String text, final CardView about) {
        final String trimmed = withoutTurnState(text);
        message = trimmed;
        priority = !trimmed.equals(text);
        starterChoice = opensWith(trimmed, "lblYouHaveWonTheCoinToss") ? StarterChoice.toss
                : opensWith(trimmed, "lblYouLostTheLastGame") ? StarterChoice.lastGame : null;
        card = cardRef(about);
        changed();
    }

    synchronized void buttons(final String label1, final String label2, final boolean enable1, final boolean enable2,
            final boolean focus1) {
        ok = new PromptButton(label1, enable1, kindOf(label1));
        cancel = new PromptButton(label2, enable2, kindOf(label2));
        focusOk = focus1;
        // Only a mana payment offers Auto. During a payment the browser keeps the card being paid for in its waiting slot
        paying = ok.kind() == ButtonKind.auto;
        // At priority the engine labels the cancel button End Turn only when it ends the turn, and Undo when it undoes
        endsTurn = cancel.kind() == ButtonKind.endTurn;
        changed();
    }

    private static ButtonKind kindOf(final String label) {
        final Localizer loc = Localizer.getInstance();
        for (final ButtonKind kind : ButtonKind.values()) {
            if (loc.getMessage(KIND_KEYS.get(kind)).equals(label)) {
                return kind;
            }
        }
        return null;
    }

    /** Sets what the waiting prompt offers and what has been chosen so far. This replaces the marks of the last prompt. */
    synchronized void selection(final Selection selection) {
        selectable = selection.cards().stream().map(PromptState::cardRef).toList();
        selectableMin = selection.min();
        final List<Ref> players = new ArrayList<>();
        final List<Integer> stack = new ArrayList<>();
        for (final TrackableObject target : selection.targets()) {
            if (target instanceof PlayerView p) {
                players.add(Ref.player(p.getId()));
            } else if (target instanceof StackItemView item) {
                stack.add(DeltaPacket.makeDeltaKey(item));
            }
        }
        selectablePlayers = players;
        selectableStack = stack;
        highlighted = selection.chosen().stream().map(DeltaPacket::makeDeltaKey).distinct().toList();
        changed();
    }

    synchronized void card(final CardView about) {
        card = cardRef(about);
        changed();
    }

    private void changed() {
        send.accept(current());
    }

    private Prompt current() {
        return new Prompt(message, priority, card, ok, cancel, focusOk, paying, endsTurn, selectable, selectableMin,
                selectablePlayers, selectableStack, highlighted, starterChoice);
    }

    /** Whether the message's first line is the text of the key, with any player's name filled in. */
    private static boolean opensWith(final String message, final String key) {
        final String[] around = Localizer.getInstance().getMessage(key, "\u0000").split("\u0000", -1);
        final String first = message.split("\n", 2)[0];
        return around.length == 2 && first.startsWith(around[0]) && first.endsWith(around[1]);
    }

    static Ref cardRef(final CardView card) {
        return card == null ? null : Ref.card(card.getId());
    }

    // Drops the lines of the priority prompt that the phase pill and the stack pile already show
    private static String withoutTurnState(final String message) {
        final Localizer loc = Localizer.getInstance();
        if (!message.startsWith(loc.getMessage("lblPriority") + ":")) {
            return message;
        }
        final List<String> labels = List.of(loc.getMessage("lblPriority"), loc.getMessage("lblTurn"),
                loc.getMessage("lblPhase"), loc.getMessage("lblStack"));
        final StringBuilder kept = new StringBuilder();
        for (final String line : message.split("\n")) {
            if (line.isBlank() || labels.stream().anyMatch(label -> line.startsWith(label + ":"))) {
                continue;
            }
            kept.append(kept.isEmpty() ? "" : "\n").append(line);
        }
        return kept.toString();
    }
}
