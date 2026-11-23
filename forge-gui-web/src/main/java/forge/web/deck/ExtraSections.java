package forge.web.deck;

import forge.StaticData;
import forge.card.CardEdition;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckFormat;
import forge.deck.DeckProxy;
import forge.deck.DeckSection;
import forge.deck.DeckgenUtil;
import forge.game.GameType;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.util.Localizer;
import forge.util.MyRandom;
import forge.web.deck.DeckCatalog.OnDevice;
import forge.web.protocol.ToBrowser.ExtraCard;
import forge.web.protocol.ToBrowser.ExtraChoice;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** The extra decks that some variants play with: a planar deck, a scheme deck or a Vanguard avatar. A lobby seat chooses them here. */
public final class ExtraSections {
    private ExtraSections() {
    }

    /** A seat's choice for one extra section: the choice's key, its label and its cards. cards is null to use the main deck's own section. hidden is true when the label hides which card was drawn. */
    public record Extra(String key, String label, CardPool cards, boolean hidden) {
        Extra(final String key, final String label, final CardPool cards) {
            this(key, label, cards, false);
        }
    }

    static final String OWN = "own";
    public static final String GENERATE = "generate";
    public static final String RANDOM = "random";
    private static final String SAVED = "deck:";
    private static final String DEVICE = DeckCatalog.DEVICE + ":";
    private static final String AVATAR = "avatar:";
    private static final int CARDS_SHOWN = 3;

    /** The choices a seat has for a planar deck, a scheme deck or an avatar, most common first. */
    public static List<ExtraChoice> extraChoices(final DeckSection section, final boolean forComputer, final Deck main,
            final boolean guest, final Map<String, OnDevice> device) {
        final List<ExtraChoice> out = new ArrayList<>();
        final CardPool own = main == null ? null : main.get(section);
        if (own != null && !own.isEmpty()) {
            out.add(new ExtraChoice(OWN, Localizer.getInstance().getMessage(section == DeckSection.Avatar ? "lblWebDeckCatalogDeckDefault" : "lblWebDeckCatalogDeckOwn"),
                    own.countAll(), sectionProblem(section, own), null, null, null, shown(own.toFlatList()), false));
        }
        if (section == DeckSection.Avatar) {
            out.add(new ExtraChoice(RANDOM, randomLabel(), null, null, null, null, null, List.of(), false));
            for (final PaperCard avatar : avatars(forComputer)) {
                out.add(new ExtraChoice(AVATAR + avatar.getName(), avatar.getName(), null, null, avatar.getImageKey(false),
                        avatar.getRules().getHand(), avatar.getRules().getLife(), List.of(), false));
            }
            return out;
        }
        // A playtest card often has no picture to show
        final List<PaperCard> all = (section == DeckSection.Planes ? FModel.getPlanechaseCards() : FModel.getArchenemyCards()).toFlatList().stream()
                .filter(c -> StaticData.instance().getCardEdition(c.getEdition()).getType() != CardEdition.Type.FUNNY).toList();
        out.add(new ExtraChoice(GENERATE, generatedLabel(section), null, null, null, null, null, shown(all), false));
        out.add(new ExtraChoice(RANDOM, Localizer.getInstance().getMessage("lblWebDeckCatalogRandomSaved"), null, null, null, null, null, List.of(), false));
        device.forEach((id, d) -> {
            if (section == DeckSection.Planes && d.format() == GameType.Planechase) {
                out.add(saved(DEVICE + id, d.deck(), section, guest));
            }
        });
        for (final DeckProxy proxy : savedDecks(section)) {
            out.add(saved(keyOf(proxy), proxy.getDeck(), section, guest));
        }
        return out;
    }

    private static ExtraChoice saved(final String key, final Deck deck, final DeckSection section, final boolean guest) {
        final CardPool cards = deck.get(section);
        return new ExtraChoice(key, deck.getName(), cards == null ? 0 : cards.countAll(), sectionProblem(section, cards), null, null, null,
                cards == null ? List.of() : shown(cards.toFlatList()), section == DeckSection.Planes && !readOnly(key, guest));
    }

    private static String generatedLabel(final DeckSection section) {
        return Localizer.getInstance().getMessage(section == DeckSection.Planes ? "lblWebLobbyRandomPlanes" : "lblWebLobbyRandomSchemes");
    }

    private static String keyOf(final DeckProxy proxy) {
        return SAVED + proxy.getPath() + "/" + proxy.getName();
    }

    /** The key of a planar deck saved by the editor. path is its folder, or "" for the top. */
    static String keyOf(final DeckEditor.Target target, final String path, final Deck deck) {
        return target instanceof DeckEditor.Device d ? DEVICE + d.id() : SAVED + path + "/" + deck.getName();
    }

    /** Whether the saved deck behind a key cannot be changed in place. A guest may change only the decks in its own browser. */
    static boolean readOnly(final String key, final boolean guest) {
        return guest && !key.startsWith(DEVICE);
    }

    /** A saved planar or scheme deck by its key, or null when the key names none. Only planar decks are stored in a browser. */
    static Deck savedDeck(final DeckSection section, final String key, final Map<String, OnDevice> device) {
        if (key != null && key.startsWith(DEVICE)) {
            final OnDevice d = device.get(key.substring(DEVICE.length()));
            return section == DeckSection.Planes && d != null && d.format() == GameType.Planechase ? d.deck() : null;
        }
        if (key != null && key.startsWith(SAVED)) {
            for (final DeckProxy proxy : savedDecks(section)) {
                if (keyOf(proxy).equals(key)) {
                    return proxy.getDeck();
                }
            }
        }
        return null;
    }

    /** A few of the cards, spread evenly through the list, to picture a choice. A phenomenon is never shown. */
    private static List<ExtraCard> shown(final List<PaperCard> cards) {
        final List<PaperCard> from = cards.stream().filter(c -> !c.getRules().getType().isPhenomenon()).distinct().toList();
        final int count = Math.min(CARDS_SHOWN, from.size());
        final List<ExtraCard> out = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            final PaperCard card = from.get(i * (from.size() / count));
            out.add(new ExtraCard(card.getName(), card.getImageKey(false)));
        }
        return out;
    }

    /** The cards that a choice selects. Generated and random choices are drawn now, so the seat shows what it got. */
    public static Extra resolveExtra(final DeckSection section, final String choice, final boolean forComputer, final Map<String, OnDevice> device) {
        if (OWN.equals(choice)) {
            return new Extra(OWN, Localizer.getInstance().getMessage(section == DeckSection.Avatar ? "lblWebDeckCatalogDeckDefault" : "lblWebDeckCatalogDeckOwn"), null);
        }
        if (section == DeckSection.Avatar) {
            final List<PaperCard> pool = avatars(forComputer);
            PaperCard avatar = null;
            if (RANDOM.equals(choice)) {
                // A random avatar skips the cards marked as unfit for random decks, as on desktop
                final List<PaperCard> fit = pool.stream().filter(c -> !c.getRules().getAiHints().getRemRandomDecks()).toList();
                avatar = fit.isEmpty() ? null : fit.get(MyRandom.getRandom().nextInt(fit.size()));
            } else if (choice != null && choice.startsWith(AVATAR)) {
                final String name = choice.substring(AVATAR.length());
                avatar = pool.stream().filter(c -> c.getName().equals(name)).findFirst().orElse(null);
            }
            if (avatar == null) {
                return null;
            }
            final CardPool one = new CardPool();
            one.add(avatar);
            return new Extra(choice, RANDOM.equals(choice) ? randomLabel() : avatar.getName(), one, RANDOM.equals(choice));
        }
        if (RANDOM.equals(choice)) {
            final List<DeckProxy> saved = savedDecks(section);
            if (!saved.isEmpty()) {
                final DeckProxy pick = saved.get(MyRandom.getRandom().nextInt(saved.size()));
                return new Extra(RANDOM, Localizer.getInstance().getMessage("lblWebDeckCatalogRandomSaved"), pick.getDeck().get(section));
            }
        } else if (choice != null && (choice.startsWith(SAVED) || choice.startsWith(DEVICE))) {
            final Deck deck = savedDeck(section, choice, device);
            return deck == null ? null : new Extra(choice, deck.getName(), new CardPool(deck.getOrCreate(section)));
        }
        return new Extra(GENERATE, generatedLabel(section), section == DeckSection.Planes ? DeckgenUtil.generatePlanarPool()
                : DeckgenUtil.generateSchemePool());
    }

    /** The label that a seat shows for a random avatar. It does not show which avatar was drawn. */
    public static String randomLabel() {
        return Localizer.getInstance().getMessage("lblRandom");
    }

    /** Why a planar or scheme deck cannot be played, or null. An avatar section has no size rule. */
    public static String sectionProblem(final DeckSection section, final CardPool cards) {
        return switch (section) {
            case Planes -> DeckFormat.getPlaneSectionConformanceProblem(cards);
            case Schemes -> DeckFormat.getSchemeSectionConformanceProblem(cards);
            default -> null;
        };
    }

    /** Whether a planar deck is all phenomena. The engine would look through it for a first plane without end. */
    public static boolean noPlanes(final CardPool cards) {
        return cards != null && !cards.isEmpty() && cards.toFlatList().stream().allMatch(c -> c.getRules().getType().isPhenomenon());
    }

    private static List<DeckProxy> savedDecks(final DeckSection section) {
        final List<DeckProxy> out = new ArrayList<>();
        synchronized (DeckCatalog.DECKS) {
            (section == DeckSection.Planes ? DeckProxy.getAllPlanarDecks() : DeckProxy.getAllSchemeDecks()).forEach(out::add);
        }
        return out;
    }

    /** Every Vanguard card. For a computer seat, only the cards that the computer can play well. */
    private static List<PaperCard> avatars(final boolean forComputer) {
        final List<PaperCard> out = new ArrayList<>();
        for (final PaperCard card : FModel.getMagicDb().getVariantCards().getAllCards()) {
            if (card.getRules().getType().isVanguard() && !(forComputer && card.getRules().getAiHints().getRemAIDecks())) {
                out.add(card);
            }
        }
        out.sort(Comparator.comparing(PaperCard::getName));
        return out;
    }
}
