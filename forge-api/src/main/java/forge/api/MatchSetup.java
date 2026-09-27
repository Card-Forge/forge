package forge.api;

import forge.deck.Deck;
import forge.deck.DeckFormat;
import forge.deck.DeckSection;
import forge.item.PaperCard;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Validates a detached deck for a match without rewriting the workshop's saved list. */
public final class MatchSetup {
    public record CommanderChoice(String id, String name, boolean valid, String problem) { }
    public record Preview(String format, int startingLife, String commanderId, List<String> commanders,
                          boolean needsCommander, List<CommanderChoice> commanderChoices, String problem) { }
    public record Prepared(Deck deck, Preview preview) { }

    public static String suggestedFormat(Deck deck) {
        if (!deck.getCommanders().isEmpty()) return "Commander";
        if (deck.getMain().countAll() == 100) {
            var preview = prepare(deck, "Commander", "").preview();
            if (preview.problem() == null || preview.commanderChoices().stream().anyMatch(CommanderChoice::valid)) return "Commander";
        }
        return "Constructed";
    }

    public static Prepared prepare(Deck source, String format, String chosenId) {
        Deck deck = new Deck(source);
        boolean commander = format.equals("Commander");
        boolean needsCommander = commander && deck.getCommanders().isEmpty();
        var candidates = new ArrayList<CommanderChoice>();
        String selected = chosenId == null ? "" : chosenId;
        if (needsCommander) {
            for (var entry : source.getMain()) {
                PaperCard card = entry.getKey();
                if (!DeckFormat.Commander.isLegalCommander(card.getRules())) continue;
                String problem = DeckFormat.Commander.getDeckConformanceProblem(withCommander(source, card));
                candidates.add(new CommanderChoice(CardCatalog.id(card), card.getName(), problem == null, problem));
            }
            candidates.sort(Comparator.comparing(CommanderChoice::name));
            // A single valid leader is unambiguous; still display it explicitly in setup.
            var valid = candidates.stream().filter(CommanderChoice::valid).toList();
            if (selected.isEmpty() && valid.size() == 1) selected = valid.get(0).id();
            if (!selected.isEmpty()) {
                String id = selected;
                PaperCard card = source.getMain().toFlatList().stream().filter(c -> CardCatalog.id(c).equals(id))
                        .findFirst().orElseThrow(() -> new IllegalArgumentException("Choose a commander from this deck"));
                if (!DeckFormat.Commander.isLegalCommander(card.getRules())) throw new IllegalArgumentException("That card cannot be a commander");
                deck = withCommander(source, card);
            }
        } else if (!selected.isEmpty()) {
            throw new IllegalArgumentException("This deck already defines its commanders, or is not a Commander deck");
        }
        String problem = !List.of("Constructed", "Commander").contains(format)
                ? "AI matches support Constructed and Commander decks. Select one in the workshop."
                : needsCommander && selected.isEmpty() ? candidates.isEmpty()
                    ? "This deck needs a commander. Add one in the workshop's Cmd section."
                    : "Choose a commander from your deck to start this game."
                : DeckFormat.valueOf(format).getDeckConformanceProblem(deck);
        return new Prepared(deck, new Preview(format, commander ? 40 : 20, selected,
                deck.getCommanders().stream().map(PaperCard::getName).toList(), needsCommander, List.copyOf(candidates), problem));
    }

    private static Deck withCommander(Deck source, PaperCard card) {
        Deck deck = new Deck(source);
        deck.getMain().remove(card, 1);
        deck.getOrCreate(DeckSection.Commander).add(card, 1);
        return deck;
    }
}
