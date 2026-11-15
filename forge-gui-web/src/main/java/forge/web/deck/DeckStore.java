package forge.web.deck;

import forge.deck.Deck;
import forge.deck.DeckFormat;
import forge.game.GameType;
import forge.model.FModel;
import forge.util.Localizer;
import forge.util.storage.IStorage;

import java.util.Collection;

/** Where the host stores each format's decks, and the rules a deck name must follow to be saved there. */
public final class DeckStore {
    static final int MAX_NAME = 60;
    private static final String FORBIDDEN = "\\/:*?\"<>|";

    private DeckStore() {
    }

    /** The storage a format's decks are kept in. Tests pass their own, so they never touch the player's decks. */
    interface Storages {
        IStorage<Deck> of(GameType format);
    }

    static Storages real() {
        return format -> switch (family(format)) {
            case Commander -> FModel.getDecks().getCommander();
            case Oathbreaker -> FModel.getDecks().getOathbreaker();
            case Brawl -> FModel.getDecks().getBrawl();
            case TinyLeaders -> FModel.getDecks().getTinyLeaders();
            default -> FModel.getDecks().getConstructed();
        };
    }

    /** The format that a deck is listed under. Sealed and draft decks are listed under Draft. */
    static GameType family(final GameType format) {
        return switch (format) {
            case Commander, Oathbreaker, Brawl, TinyLeaders -> format;
            case Draft, Sealed -> GameType.Draft;
            default -> GameType.Constructed;
        };
    }

    /** The same, for the format that a deck file records. */
    static GameType family(final DeckFormat format) {
        return switch (format) {
            case Commander -> GameType.Commander;
            case Oathbreaker -> GameType.Oathbreaker;
            case Brawl -> GameType.Brawl;
            case TinyLeaders -> GameType.TinyLeaders;
            case Limited -> GameType.Draft;
            default -> GameType.Constructed;
        };
    }

    /** Why a name cannot be used for a deck, or null when it can. */
    public static String nameProblem(final String name) {
        if (name == null || name.isBlank()) {
            return Localizer.getInstance().getMessage("lblWebDeckStoreNameEmpty");
        }
        if (name.trim().length() > MAX_NAME) {
            return Localizer.getInstance().getMessage("lblWebDeckStoreNameTooLong", MAX_NAME);
        }
        for (final char c : FORBIDDEN.toCharArray()) {
            if (name.indexOf(c) >= 0) {
                return Localizer.getInstance().getMessage("lblWebDeckStoreNameBadChars");
            }
        }
        return null;
    }

    /** Whether two names save to the same file. Windows ignores case in file names, so this does too. */
    static boolean sameFile(final String a, final String b) {
        return new Deck(a).getBestFileName().equalsIgnoreCase(new Deck(b).getBestFileName());
    }

    /** The name of the deck already saved where this name would go, or null. */
    static String taken(final IStorage<Deck> storage, final String name) {
        return taken(storage.getItemNames(), name);
    }

    private static String taken(final Collection<String> names, final String name) {
        for (final String existing : names) {
            if (sameFile(existing, name)) {
                return existing;
            }
        }
        return null;
    }

    /** The wanted name, or "wanted (2)" and so on. Returns the first one whose file no deck other than mine uses. */
    public static String freeName(final IStorage<Deck> storage, final String wanted, final String mine) {
        return freeName(storage.getItemNames(), wanted, mine);
    }

    /** As above, for any list of names. A guest's decks are stored in its browser, not in storage. */
    static String freeName(final Collection<String> names, final String wanted, final String mine) {
        String candidate = wanted;
        for (int n = 2; ; n++) {
            final String existing = taken(names, candidate);
            if (existing == null || (mine != null && sameFile(existing, mine))) {
                return candidate;
            }
            candidate = wanted + " (" + n + ")";
        }
    }
}
