package forge.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import forge.deck.Deck;
import forge.deck.DeckFormat;
import forge.deck.DeckSection;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Local desktop protocol over private child-process pipes. No network listener. */
public final class DesktopEngine {
    private static final Gson JSON = new GsonBuilder().serializeNulls().create();
    private final CardCatalog catalog;
    private final Path directory;
    private DeckEditor editor;
    private String deckId;
    private String format = "Constructed";
    private String saveError;
    private final List<CardCatalog.CardInfo> library = new ArrayList<>();
    private final List<CardCatalog.CardInfo> hand = new ArrayList<>();
    private int mulligans;
    private int draws;
    private Path resources;
    private MatchSession match;

    public DesktopEngine(CardCatalog catalog, Path directory) throws Exception {
        this.catalog = catalog;
        this.directory = directory.toAbsolutePath().normalize();
        Files.createDirectories(this.directory);
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) { throw new IllegalArgumentException("Expected resource and deck directories"); }
        PrintStream protocol = System.out;
        System.setOut(System.err); // Forge diagnostics must never corrupt the JSON protocol.
        protocol.println(JSON.toJson(Map.of("event", "loading", "message", "Loading Forge card library…")));
        var data = EngineResources.load(Path.of(args[0]));
        var engine = new DesktopEngine(CardCatalog.fromDatabases(data.getAvailableDatabases().values()), Path.of(args[1]));
        engine.resources = Path.of(args[0]);
        protocol.println(JSON.toJson(Map.of("event", "ready", "printings", engine.catalog.size())));
        try (var input = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = input.readLine()) != null) {
                JsonObject request = null;
                try {
                    if (line.length() > 2_000_000) { throw new IllegalArgumentException("Request too large"); }
                    request = JsonParser.parseString(line).getAsJsonObject();
                    Object result = engine.dispatch(request.get("method").getAsString(), request.getAsJsonObject("params"));
                    var reply = new JsonObject();
                    reply.add("id", request.get("id"));
                    reply.add("result", JSON.toJsonTree(result));
                    protocol.println(JSON.toJson(reply));
                } catch (Exception error) {
                    var reply = new JsonObject();
                    if (request != null) { reply.add("id", request.get("id")); }
                    reply.addProperty("error", error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage());
                    protocol.println(JSON.toJson(reply));
                }
            }
        }
    }

    public Object dispatch(String method, JsonObject arguments) throws Exception {
        JsonObject p = arguments == null ? new JsonObject() : arguments;
        return switch (method) {
            case "search" -> catalog.browse(new CardCatalog.Query(string(p, "text", ""),
                    integer(p, "colors"), integer(p, "maxManaValue"), number(p, "offset", 0), number(p, "limit", 48)),
                    string(p, "type", ""), string(p, "sort", "name"), !p.has("unique") || p.get("unique").getAsBoolean());
            case "list" -> list();
            case "new" -> create(string(p, "name", "Untitled deck"), string(p, "format", "Constructed"));
            case "open" -> open(string(p, "id", ""));
            case "snapshot" -> state();
            case "edit" -> {
                requireDeck();
                var edits = JSON.fromJson(p.get("edits"), DeckEditor.Edit[].class);
                editor.apply(p.get("revision").getAsLong(), List.of(edits));
                yield save();
            }
            case "rename" -> {
                requireDeck();
                editor.rename(p.get("revision").getAsLong(), string(p, "name", ""));
                yield save();
            }
            case "undo" -> { requireDeck(); editor.undo(p.get("revision").getAsLong()); yield save(); }
            case "redo" -> { requireDeck(); editor.redo(p.get("revision").getAsLong()); yield save(); }
            case "format" -> {
                requireDeck();
                if (p.get("revision").getAsLong() != editor.snapshot().revision()) {
                    throw new IllegalArgumentException("Deck changed; refresh before changing format");
                }
                format = checkedFormat(string(p, "format", "Constructed"));
                yield save();
            }
            case "save" -> { requireDeck(); yield save(); }
            case "importPreview" -> DeckImport.preview(string(p, "text", ""));
            case "import" -> {
                var preview = DeckImport.preview(string(p, "text", ""));
                if (preview.entries().isEmpty()) { throw new IllegalArgumentException("Paste at least one card"); }
                DeckEditor imported = preview.open(checkedName(string(p, "name", "Imported deck")), catalog);
                String importedFormat = checkedFormat(string(p, "format", "Constructed"));
                ensureSaved();
                editor = imported;
                deckId = UUID.randomUUID().toString();
                format = importedFormat;
                clearPractice();
                yield save();
            }
            case "export" -> export(string(p, "kind", "text"));
            case "practice" -> practice(string(p, "action", "shuffle"), number(p, "index", -1));
            case "matchOpponents" -> MatchSession.opponents(format);
            case "matchSetup" -> {
                requireDeck();
                var preview = MatchSetup.prepare(editor.toDeck(), format, string(p, "commanderId", "")).preview();
                var result = new LinkedHashMap<String, Object>();
                result.put("deckId", deckId); result.put("revision", editor.snapshot().revision());
                result.put("name", editor.snapshot().name()); result.put("setup", preview);
                result.put("saveError", saveError); result.put("opponents", MatchSession.opponents(format));
                yield result;
            }
            case "matchStart" -> {
                requireDeck();
                ensureSaved();
                if (match != null && !match.finished()) throw new IllegalStateException("Finish or concede the current match first");
                if (p.has("deckId") && !deckId.equals(string(p, "deckId", "")) || p.has("revision") && p.get("revision").getAsLong() != editor.snapshot().revision()) {
                    throw new IllegalArgumentException("The deck changed. Reopen match setup before starting.");
                }
                var prepared = MatchSetup.prepare(editor.toDeck(), format, string(p, "commanderId", ""));
                if (prepared.preview().problem() != null) throw new IllegalArgumentException(prepared.preview().problem());
                match = new MatchSession(prepared.deck(), format, string(p, "opponent", "green"), resources, directory.getParent());
                yield match.state();
            }
            case "matchState" -> match == null ? null : match.state();
            case "matchAction" -> { if (match == null) throw new IllegalStateException("No active match"); yield match.action(p); }
            case "matchConcede" -> { if (match == null) throw new IllegalStateException("No active match"); yield match.concede(p); }
            default -> throw new IllegalArgumentException("Unknown engine command");
        };
    }

    private Object create(String name, String requestedFormat) throws Exception {
        String newName = checkedName(name);
        String newFormat = checkedFormat(requestedFormat);
        ensureSaved();
        editor = new DeckEditor(catalog, new Deck(newName));
        deckId = UUID.randomUUID().toString();
        format = newFormat;
        clearPractice();
        return save();
    }

    private Object open(String id) throws Exception {
        ensureSaved();
        var stored = JSON.fromJson(Files.readString(file(id)), StoredDeck.class);
        if (stored.version() != 1) { throw new IllegalArgumentException("Unsupported deck file version"); }
        var deck = new Deck(checkedName(stored.name()));
        var loaded = new DeckEditor(catalog, deck);
        loaded.apply(0, stored.cards());
        String loadedFormat = checkedFormat(stored.format());
        editor = new DeckEditor(catalog, loaded.toDeck());
        deckId = id;
        format = loadedFormat;
        saveError = null;
        clearPractice();
        return state();
    }

    private record StoredDeck(int version, String name, String format, List<DeckEditor.Edit> cards, long updated) { }

    private Object list() throws Exception {
        var decks = new ArrayList<Map<String, Object>>();
        var problems = new ArrayList<String>();
        try (var files = Files.list(directory)) {
            for (Path file : files.filter(path -> path.getFileName().toString().endsWith(".json")).toList()) {
                try {
                    var stored = JSON.fromJson(Files.readString(file), StoredDeck.class);
                    if (stored.version() != 1) { throw new IllegalArgumentException("Unsupported version"); }
                    int main = stored.cards().stream().filter(card -> card.section().equals("Main"))
                            .mapToInt(DeckEditor.Edit::quantity).sum();
                    String id = file.getFileName().toString().replace(".json", "");
                    decks.add(Map.of("id", id, "name", stored.name(), "format", stored.format(),
                            "count", main, "updated", stored.updated()));
                } catch (Exception error) { problems.add(file.getFileName() + ": " + error.getMessage()); }
            }
        }
        decks.sort(Comparator.comparingLong((Map<String, Object> deck) -> (long) deck.get("updated")).reversed());
        return Map.of("decks", decks, "problems", problems);
    }

    private Object save() {
        try {
            var snapshot = editor.snapshot();
            var stored = new StoredDeck(1, snapshot.name(), format, snapshot.entries().stream()
                    .map(entry -> new DeckEditor.Edit(entry.section(), entry.card().id(), entry.quantity())).toList(),
                    System.currentTimeMillis());
            Path target = file(deckId);
            Path temporary = directory.resolve(deckId + ".tmp");
            Files.writeString(temporary, JSON.toJson(stored), StandardCharsets.UTF_8);
            try { Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException error) { Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING); }
            saveError = null;
        } catch (Exception error) { saveError = error.getMessage(); }
        return state();
    }

    private Object state() {
        requireDeck();
        var result = new LinkedHashMap<String, Object>();
        result.put("id", deckId);
        result.put("format", format);
        result.put("deck", editor.snapshot());
        result.put("validation", editor.validate(DeckFormat.valueOf(format)));
        result.put("saveError", saveError);
        return result;
    }

    private String export(String kind) {
        requireDeck();
        var result = new StringBuilder();
        if (kind.equals("forge")) {
            result.append("[metadata]\nName=").append(editor.snapshot().name().replaceAll("[\\r\\n]", " ")).append("\n");
            for (var section : editor.toDeck()) {
                if (!section.getValue().isEmpty()) {
                    result.append('[').append(section.getKey().name()).append("]\n")
                            .append(section.getValue().toCardList("\n")).append('\n');
                }
            }
        } else {
            for (DeckSection section : DeckSection.values()) {
                var entries = editor.snapshot().entries().stream().filter(entry -> entry.section().equals(section.name())).toList();
                if (entries.isEmpty()) { continue; }
                result.append(section == DeckSection.Main ? "Deck" : section.name()).append('\n');
                var counts = new LinkedHashMap<String, Integer>();
                entries.forEach(entry -> counts.merge(entry.card().name(), entry.quantity(), Integer::sum));
                counts.forEach((name, count) -> result.append(count).append(' ').append(name).append('\n'));
                result.append('\n');
            }
        }
        return result.toString();
    }

    private Object practice(String action, int index) {
        requireDeck();
        switch (action) {
            case "shuffle", "mulligan" -> {
                if (action.equals("shuffle")) { mulligans = 0; } else { mulligans++; }
                library.clear(); hand.clear(); draws = 0;
                for (var entry : editor.snapshot().entries()) {
                    if (entry.section().equals("Main")) {
                        for (int i = 0; i < entry.quantity(); i++) { library.add(entry.card()); }
                    }
                }
                if (library.size() < 7) { throw new IllegalArgumentException("Add at least 7 main-deck cards to practice"); }
                Collections.shuffle(library);
                for (int i = 0; i < 7; i++) { hand.add(library.remove(library.size() - 1)); }
            }
            case "draw" -> {
                if (!library.isEmpty()) { hand.add(library.remove(library.size() - 1)); draws++; }
            }
            case "bottom" -> {
                if (index < 0 || index >= hand.size()) { throw new IllegalArgumentException("Choose a card from your hand"); }
                library.add(0, hand.remove(index));
            }
            default -> throw new IllegalArgumentException("Unknown practice action");
        }
        return Map.of("hand", List.copyOf(hand), "remaining", library.size(), "mulligans", mulligans, "draws", draws);
    }

    private void clearPractice() { library.clear(); hand.clear(); mulligans = 0; draws = 0; }
    private void requireDeck() { if (editor == null) { throw new IllegalStateException("Open or create a deck first"); } }
    private void ensureSaved() {
        if (saveError != null) { throw new IllegalStateException("Save the current deck before switching: " + saveError); }
    }
    private Path file(String id) {
        if (!UUID.fromString(id).toString().equals(id)) { throw new IllegalArgumentException("Invalid deck ID"); }
        return directory.resolve(id + ".json");
    }
    private static String checkedFormat(String value) {
        if (!List.of("Constructed", "Commander", "Limited").contains(value)) {
            throw new IllegalArgumentException("Unsupported format");
        }
        return value;
    }
    private static String checkedName(String value) {
        if (value == null || value.isBlank() || value.length() > 100) {
            throw new IllegalArgumentException("Deck name must be 1..100 characters");
        }
        return value.strip();
    }
    private static String string(JsonObject value, String key, String fallback) {
        return value.has(key) && !value.get(key).isJsonNull() ? value.get(key).getAsString() : fallback;
    }
    private static Integer integer(JsonObject value, String key) {
        return value.has(key) && !value.get(key).isJsonNull() ? value.get(key).getAsInt() : null;
    }
    private static int number(JsonObject value, String key, int fallback) {
        Integer result = integer(value, key); return result == null ? fallback : result;
    }
}
