package forge.api;

import forge.deck.Deck;
import forge.deck.DeckFormat;
import forge.deck.DeckSection;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

/** In-memory editor; each batch is atomic, revision checked, and undoable. */
public final class DeckEditor {
    public record Edit(String section, String cardId, int quantity) { }
    public record Entry(String section, CardCatalog.CardInfo card, int quantity) { }
    public record Snapshot(String name, long revision, List<Entry> entries, boolean canUndo, boolean canRedo) {
        public Snapshot { entries = List.copyOf(entries); }
    }
    /** Checks Forge's structural deck rules, not rotating set/ban-list legality. */
    public record Validation(String format, boolean valid, String problem) { }

    private final CardCatalog catalog;
    private final Deque<Deck> undo = new ArrayDeque<>();
    private final Deque<Deck> redo = new ArrayDeque<>();
    private Deck deck;
    private long revision;

    public DeckEditor(CardCatalog catalog, Deck initial) {
        this.catalog = Objects.requireNonNull(catalog);
        deck = new Deck(Objects.requireNonNull(initial));
    }

    public synchronized Snapshot snapshot() {
        List<Entry> entries = new ArrayList<>();
        for (var section : deck) {
            for (var card : section.getValue()) {
                entries.add(new Entry(section.getKey().name(), CardCatalog.describe(card.getKey()), card.getValue()));
            }
        }
        entries.sort(Comparator.comparing(Entry::section).thenComparing(e -> e.card().name())
                .thenComparing(e -> e.card().id()));
        return new Snapshot(deck.getName(), revision, entries, !undo.isEmpty(), !redo.isEmpty());
    }

    public synchronized Snapshot apply(long expectedRevision, List<Edit> edits) {
        checkRevision(expectedRevision);
        Objects.requireNonNull(edits);
        if (edits.isEmpty()) { return snapshot(); }
        Deck updated = new Deck(deck);
        for (Edit edit : edits) {
            if (edit.quantity() < 0 || edit.quantity() > 1000) {
                throw new IllegalArgumentException("Quantity must be 0..1000");
            }
            DeckSection section = DeckSection.valueOf(edit.section());
            var card = catalog.resolve(edit.cardId());
            if (edit.quantity() > 0 && !section.validate(card)) {
                throw new IllegalArgumentException("Card does not belong in " + section);
            }
            var pool = updated.getOrCreate(section);
            pool.remove(card, pool.count(card));
            pool.add(card, edit.quantity());
        }
        remember(undo, deck);
        deck = updated;
        redo.clear();
        revision++;
        return snapshot();
    }

    public synchronized Snapshot undo(long expectedRevision) {
        checkRevision(expectedRevision);
        if (!undo.isEmpty()) {
            remember(redo, deck);
            deck = undo.pop();
            revision++;
        }
        return snapshot();
    }

    public synchronized Snapshot rename(long expectedRevision, String name) {
        checkRevision(expectedRevision);
        if (name == null || name.isBlank() || name.length() > 100) {
            throw new IllegalArgumentException("Deck name must be 1..100 characters");
        }
        remember(undo, deck);
        deck = new Deck(deck, name.strip());
        redo.clear();
        revision++;
        return snapshot();
    }

    public synchronized Snapshot redo(long expectedRevision) {
        checkRevision(expectedRevision);
        if (!redo.isEmpty()) {
            remember(undo, deck);
            deck = redo.pop();
            revision++;
        }
        return snapshot();
    }

    public synchronized Validation validate(DeckFormat format) {
        String problem = Objects.requireNonNull(format).getDeckConformanceProblem(deck);
        return new Validation(format.name(), problem == null, problem);
    }

    /** A detached copy for Forge's match launcher or existing DeckSerializer. */
    public synchronized Deck toDeck() { return new Deck(deck); }

    private void checkRevision(long expected) {
        if (expected != revision) {
            throw new ConcurrentModificationException("Expected revision " + expected + "; current is " + revision);
        }
    }

    private static void remember(Deque<Deck> history, Deck previous) {
        history.push(previous);
        if (history.size() > 100) { history.removeLast(); }
    }
}
