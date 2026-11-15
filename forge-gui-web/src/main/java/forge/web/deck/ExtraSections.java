package forge.web.deck;

import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckFormat;
import forge.deck.DeckProxy;
import forge.deck.DeckSection;
import forge.deck.DeckgenUtil;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.util.Localizer;
import forge.util.MyRandom;
import forge.web.protocol.ToBrowser.ExtraChoice;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** The extra decks that some variants play with: a planar deck, a scheme deck or a Vanguard avatar. A lobby seat chooses them here. */
public final class ExtraSections {
    private ExtraSections() {
    }

    /** A seat's choice for one extra section: its label and its cards. cards is null to use the main deck's own section. hidden is true when the label hides which card was drawn. */
    public record Extra(String label, CardPool cards, boolean hidden) {
        Extra(final String label, final CardPool cards) {
            this(label, cards, false);
        }
    }

    static final String OWN = "own";
    public static final String GENERATE = "generate";
    public static final String RANDOM = "random";
    private static final String SAVED = "deck:";
    private static final String AVATAR = "avatar:";

    /** The choices a seat has for a planar deck, a scheme deck or an avatar, most common first. */
    public static List<ExtraChoice> extraChoices(final DeckSection section, final boolean forComputer, final Deck main) {
        final List<ExtraChoice> out = new ArrayList<>();
        final CardPool own = main == null ? null : main.get(section);
        if (own != null && !own.isEmpty()) {
            out.add(new ExtraChoice(OWN, Localizer.getInstance().getMessage(section == DeckSection.Avatar ? "lblWebDeckCatalogDeckDefault" : "lblWebDeckCatalogDeckOwn"),
                    own.countAll(), sectionProblem(section, own), null, null, null));
        }
        if (section == DeckSection.Avatar) {
            out.add(new ExtraChoice(RANDOM, randomLabel(), null, null, null, null, null));
            for (final PaperCard avatar : avatars(forComputer)) {
                out.add(new ExtraChoice(AVATAR + avatar.getName(), avatar.getName(), null, null, avatar.getImageKey(false),
                        avatar.getRules().getHand(), avatar.getRules().getLife()));
            }
            return out;
        }
        out.add(new ExtraChoice(GENERATE, Localizer.getInstance().getMessage("lblWebFinderSourceGenerated"), null, null, null, null, null));
        out.add(new ExtraChoice(RANDOM, Localizer.getInstance().getMessage("lblWebDeckCatalogRandomSaved"), null, null, null, null, null));
        for (final DeckProxy proxy : savedDecks(section)) {
            final CardPool cards = proxy.getDeck().get(section);
            out.add(new ExtraChoice(SAVED + proxy.getPath() + "/" + proxy.getName(), proxy.getName(),
                    cards == null ? 0 : cards.countAll(), sectionProblem(section, cards), null, null, null));
        }
        return out;
    }

    /** The cards that a choice selects. Generated and random choices are drawn now, so the seat shows what it got. */
    public static Extra resolveExtra(final DeckSection section, final String choice, final boolean forComputer) {
        if (OWN.equals(choice)) {
            return new Extra(Localizer.getInstance().getMessage(section == DeckSection.Avatar ? "lblWebDeckCatalogDeckDefault" : "lblWebDeckCatalogDeckOwn"), null);
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
            return new Extra(RANDOM.equals(choice) ? randomLabel() : avatar.getName(), one, RANDOM.equals(choice));
        }
        if (RANDOM.equals(choice)) {
            final List<DeckProxy> saved = savedDecks(section);
            if (!saved.isEmpty()) {
                final DeckProxy pick = saved.get(MyRandom.getRandom().nextInt(saved.size()));
                return new Extra(Localizer.getInstance().getMessage("lblWebDeckCatalogRandomSaved"), pick.getDeck().get(section));
            }
        } else if (choice != null && choice.startsWith(SAVED)) {
            final String key = choice.substring(SAVED.length());
            for (final DeckProxy proxy : savedDecks(section)) {
                if ((proxy.getPath() + "/" + proxy.getName()).equals(key)) {
                    return new Extra(proxy.getName(), proxy.getDeck().get(section));
                }
            }
            return null;
        }
        return new Extra(Localizer.getInstance().getMessage("lblWebFinderSourceGenerated"), section == DeckSection.Planes ? DeckgenUtil.generatePlanarPool()
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
