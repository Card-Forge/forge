package forge.api;

import com.google.gson.Gson;
import forge.StaticData;
import forge.deck.Deck;
import forge.deck.DeckFormat;
import forge.deck.DeckSection;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

/** Bundled, attributed precons; every imported or AI copy is validated by the engine. */
public final class DeckPresets {
    public record CardEntry(int quantity, String name) { }
    public record Preset(String id, String name, String format, String set, String theme, String description,
                         String moxfieldUrl, String sourceUrl, String verifiedAt,
                         List<CardEntry> commanders, List<CardEntry> main) { }
    private static final List<Preset> PRESETS = load();

    private static List<Preset> load() {
        try (var stream = Objects.requireNonNull(DeckPresets.class.getResourceAsStream("deck-presets.json"));
             var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return List.of(new Gson().fromJson(reader, Preset[].class));
        } catch (Exception failure) {
            throw new IllegalStateException("Could not load preset decks", failure);
        }
    }

    public static List<Preset> list() { return PRESETS; }

    public static Preset find(String id) {
        return PRESETS.stream().filter(preset -> preset.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown preset deck"));
    }

    public static Deck create(String id) {
        var preset = find(id);
        var deck = new Deck(preset.name());
        add(deck, DeckSection.Commander, preset.commanders());
        add(deck, DeckSection.Main, preset.main());
        String problem = DeckFormat.Commander.getDeckConformanceProblem(deck);
        if (problem != null) throw new IllegalStateException("Preset " + preset.name() + ": " + problem);
        return deck;
    }

    private static void add(Deck deck, DeckSection section, List<CardEntry> entries) {
        for (var entry : entries) {
            var card = StaticData.instance().getCommonCards().getCard(entry.name());
            if (card == null) throw new IllegalStateException("Missing preset card: " + entry.name());
            deck.getOrCreate(section).add(card, entry.quantity());
        }
    }
}
