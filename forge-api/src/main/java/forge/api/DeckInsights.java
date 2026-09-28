package forge.api;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Offline building aids. Role estimates and suggestions never change deck validation. */
public final class DeckInsights {
    private DeckInsights() { }
    static final List<String> ROLES = List.of("Lands", "Ramp", "Draw", "Interaction");
    private static final Pattern DRAW = Pattern.compile("\\bdraws? (?:a|an|one|two|three|four|five|seven|that many|x|[0-9]+) cards?\\b");
    private static final Pattern INTERACTION = Pattern.compile(
            "(?:destroy|exile|return) (?:up to [a-z0-9]+ )?(?:target|all|each) [^.\\n]*(?:creature|permanent|artifact|enchantment|planeswalker)|counter target [^.\\n]*spell|damage to (?:any target|target creature|each creature)");

    public record Suggestion(CardCatalog.CardInfo card, String role, String reason) { }
    public record Report(String deckId, long revision, Integer identity, String basis,
                         Map<String, Integer> counts, Map<String, List<String>> cardRoles,
                         List<Suggestion> suggestions, String note) { }
    private record Candidate(String name, String role, String reason, boolean commanderOnly, int creatures) { }
    // A small, intentional starting pool, not a scraped popularity or power ranking.
    private static final List<Candidate> CANDIDATES = List.of(
            new Candidate("Arcane Signet", "Ramp", "A two-mana rock that produces your commander's colors.", true, 0),
            new Candidate("Sol Ring", "Ramp", "Adds two colorless mana; it does not pay colored costs.", true, 0),
            new Candidate("Rampant Growth", "Ramp", "Finds a basic land and puts it onto the battlefield tapped.", false, 0),
            new Candidate("Cultivate", "Ramp", "Finds basic lands for the battlefield and your next land drop.", false, 0),
            new Candidate("Llanowar Elves", "Ramp", "A one-mana creature that can provide green mana.", false, 0),
            new Candidate("Mind Stone", "Ramp", "Provides colorless mana and can later be exchanged for a card.", false, 0),
            new Candidate("Sign in Blood", "Draw", "Trades two life for two cards.", false, 0),
            new Candidate("Night's Whisper", "Draw", "Trades two life for two cards with one black mana required.", false, 0),
            new Candidate("Harmonize", "Draw", "Draws three cards in green.", false, 0),
            new Candidate("Beast Whisperer", "Draw", "Draws when you cast creatures; this deck has at least 15 creatures.", false, 15),
            new Candidate("Guardian Project", "Draw", "Rewards differently named nontoken creatures entering; fits a creature-heavy Commander deck.", true, 15),
            new Candidate("Chart a Course", "Draw", "Draws two cards; without attacking this turn, you also discard.", false, 0),
            new Candidate("Fact or Fiction", "Draw", "Offers a choice of revealed piles to refill your hand.", false, 0),
            new Candidate("Thrill of Possibility", "Draw", "Discards a card to draw two at instant speed.", false, 0),
            new Candidate("Skullclamp", "Draw", "Draws when the equipped creature dies; the +1/-1 needs suitable creatures.", true, 15),
            new Candidate("Swords to Plowshares", "Interaction", "Exiles a creature; its controller gains life.", false, 0),
            new Candidate("Generous Gift", "Interaction", "Destroys a permanent and gives its controller a 3/3 Elephant.", false, 0),
            new Candidate("Beast Within", "Interaction", "Destroys a permanent and gives its controller a 3/3 Beast.", false, 0),
            new Candidate("Chaos Warp", "Interaction", "Answers a permanent, with a chance of giving its owner another one.", false, 0),
            new Candidate("Abrade", "Interaction", "Choose between creature damage and destroying an artifact.", false, 0),
            new Candidate("Feed the Swarm", "Interaction", "Removes an opposing creature or enchantment at a life cost.", false, 0),
            new Candidate("Go for the Throat", "Interaction", "Destroys a nonartifact creature.", false, 0),
            new Candidate("Counterspell", "Interaction", "Counters a spell while it is on the stack.", false, 0),
            new Candidate("Negate", "Interaction", "Counters a noncreature spell.", false, 0),
            new Candidate("Reality Shift", "Interaction", "Exiles a creature and gives its controller a manifested card.", false, 0),
            new Candidate("Command Tower", "Lands", "Produces any color in your commander's color identity.", true, 0),
            new Candidate("Evolving Wilds", "Lands", "Finds a basic land; that land enters tapped.", false, 0),
            new Candidate("Plains", "Lands", "A basic white mana source.", false, 0),
            new Candidate("Island", "Lands", "A basic blue mana source.", false, 0),
            new Candidate("Swamp", "Lands", "A basic black mana source.", false, 0),
            new Candidate("Mountain", "Lands", "A basic red mana source.", false, 0),
            new Candidate("Forest", "Lands", "A basic green mana source.", false, 0),
            new Candidate("Wastes", "Lands", "A basic colorless mana source.", false, 0));

    public static List<String> roles(CardCatalog.CardInfo card) {
        if (card.type().contains("Land")) return List.of("Lands");
        if (!"Main".equals(card.deckSection())) return List.of();
        String text = (card.oracleText() + (card.otherFace() == null ? "" : "\n" + card.otherFace().oracleText())).toLowerCase(Locale.ROOT);
        var roles = new ArrayList<String>();
        if (text.contains("add {") || text.contains("add one mana") || text.contains("add two mana")
                || text.contains("add three mana") || (text.contains("search your library")
                && text.contains("land") && text.contains("onto the battlefield"))) roles.add("Ramp");
        if (DRAW.matcher(text).find()) roles.add("Draw");
        if (INTERACTION.matcher(text).find()) roles.add("Interaction");
        return List.copyOf(roles);
    }

    public static Integer identity(DeckEditor.Snapshot deck, String format) {
        var relevant = deck.entries().stream().filter(entry -> "Commander".equals(format)
                ? entry.section().equals("Commander") : entry.section().equals("Main")).toList();
        if (relevant.isEmpty()) return null;
        return relevant.stream().mapToInt(entry -> entry.card().colorIdentity()).reduce(0, (a, b) -> a | b);
    }

    public static Report analyze(CardCatalog catalog, DeckEditor.Snapshot deck, String format, String deckId) {
        Integer identity = identity(deck, format);
        boolean commander = "Commander".equals(format);
        var counts = new LinkedHashMap<String, Integer>();
        ROLES.forEach(role -> counts.put(role, 0));
        var cardRoles = new LinkedHashMap<String, List<String>>();
        for (var entry : deck.entries()) {
            var roles = roles(entry.card());
            cardRoles.put(entry.card().name(), roles);
            if (entry.section().equals("Main")) roles.forEach(role -> counts.merge(role, entry.quantity(), Integer::sum));
        }
        String basis = commander ? "commander color identity" : "main-deck color identity";
        if (identity == null) return new Report(deckId, deck.revision(), null, basis, counts, cardRoles, List.of(),
                commander ? "Add your commander in the Cmd section to get suggestions in its colors."
                        : "Add a few cards to establish your colors and get suggestions.");
        if ("Limited".equals(format)) return new Report(deckId, deck.revision(), identity, basis, counts, cardRoles, List.of(),
                "Limited suggestions need your draft or sealed pool. Use the library to explore cards.");
        Set<String> present = deck.entries().stream().map(entry -> entry.card().name()).collect(Collectors.toSet());
        int creatures = deck.entries().stream().filter(e -> e.section().equals("Main") && e.card().type().contains("Creature"))
                .mapToInt(DeckEditor.Entry::quantity).sum();
        boolean basics = deck.entries().stream().anyMatch(e -> e.section().equals("Main") && e.card().type().contains("Basic Land"));
        var suggestions = new ArrayList<Suggestion>();
        var roleOrder = new ArrayList<>(ROLES);
        roleOrder.sort(Comparator.comparingInt(counts::get));
        for (String role : roleOrder) {
            int added = 0;
            for (var candidate : CANDIDATES) {
                if (!candidate.role().equals(role) || present.contains(candidate.name()) || candidate.creatures() > creatures
                        || (candidate.commanderOnly() && !commander)) continue;
                var card = catalog.named(candidate.name());
                if (card == null || (card.colorIdentity() & ~identity) != 0) continue;
                if (identity == 0 && Set.of("Command Tower", "Arcane Signet").contains(card.name())) continue;
                if (identity != 0 && card.name().equals("Wastes")) continue;
                if (!basics && Set.of("Rampant Growth", "Cultivate", "Evolving Wilds").contains(card.name())) continue;
                suggestions.add(new Suggestion(card, role, candidate.reason() + " You have " + counts.get(role)
                        + " " + role.toLowerCase(Locale.ROOT) + " cards in your main deck (estimated)."));
                if (++added == 3) break;
            }
        }
        return new Report(deckId, deck.revision(), identity, basis, counts, cardRoles, List.copyOf(suggestions),
                "Starting points from a small curated pool, ordered by your lowest role counts. Roles are rules-text estimates and can overlap. Suggestions do not check set legality, ban lists, prices, or combo strength.");
    }
}
