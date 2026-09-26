package forge.api;

import java.nio.file.Path;
import java.util.ArrayList;

/** Runnable smoke example against the checkout's actual cards and deck import engine. */
public final class CatalogExample {
    private CatalogExample() { }

    public static void main(String[] args) {
        if (args.length < 1) { throw new IllegalArgumentException("Usage: CatalogExample <forge-gui/res> [search]"); }
        var data = EngineResources.load(Path.of(args[0]));
        var cards = new ArrayList<>(data.getCommonCards().getAllCards());
        cards.addAll(data.getVariantCards().getAllCards());
        var catalog = new CardCatalog(cards);
        var result = catalog.search(new CardCatalog.Query(args.length > 1 ? args[1] : "Lightning Bolt", null, null, 0, 5));
        System.out.println("Matching printings: " + result.total());
        result.cards().forEach(card -> System.out.println(card.name() + " [" + card.edition() + "] " + card.manaCost()));
        var preview = DeckImport.preview("Deck\n4 Lightning Bolt\n20 Mountain\nSideboard\n2 Shock");
        if (!preview.problems().isEmpty()) { throw new IllegalStateException(preview.problems().toString()); }
        var deck = preview.open("API smoke example", catalog);
        if (deck.toDeck().getMain().countAll() != 24) { throw new IllegalStateException("Import count mismatch"); }
        if (DeckImport.preview("4 Definitely Not A Real Card 12345").problems().isEmpty()) {
            throw new IllegalStateException("Unknown card was silently accepted");
        }
        System.out.println("Headless catalog and deck import OK: " + deck.snapshot().entries().size() + " entries");
    }
}
