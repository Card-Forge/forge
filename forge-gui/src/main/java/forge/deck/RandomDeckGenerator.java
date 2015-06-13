package forge.deck;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import forge.game.GameType;
import forge.game.IHasGameType;
import forge.model.FModel;
import forge.quest.QuestController;
import forge.util.Aggregates;

public class RandomDeckGenerator extends DeckProxy implements Comparable<RandomDeckGenerator> {
    private enum RandomDeckType {
        Generated,
        User,
        Favorite
    }

    public static List<DeckProxy> getRandomDecks(final IHasGameType lstDecks0, final boolean isAi0) {
        final List<DeckProxy> decks = new ArrayList<DeckProxy>();

        decks.add(new RandomDeckGenerator("Random Generated Deck", RandomDeckType.Generated, lstDecks0, isAi0));
        decks.add(new RandomDeckGenerator("Random User Deck", RandomDeckType.User, lstDecks0, isAi0));
        decks.add(new RandomDeckGenerator("Random Favorite Deck", RandomDeckType.Favorite, lstDecks0, isAi0));

        return decks;
    }

    private final String name;
    private final RandomDeckType type;
    private final IHasGameType lstDecks;
    private final boolean isAi;

    private RandomDeckGenerator(final String name0, final RandomDeckType type0, final IHasGameType lstDecks0, final boolean isAi0) {
        super();
        name = name0;
        type = type0;
        lstDecks = lstDecks0;
        isAi = isAi0;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int compareTo(final RandomDeckGenerator d) {
        return Integer.compare(type.ordinal(), d.type.ordinal());
    }

    @Override
    public Deck getDeck() {
        switch (type) {
        case Generated:
            return getGeneratedDeck();
        case User:
            return getUserDeck();
        default:
            return getFavoriteDeck();
        }
    }

    private Deck getGeneratedDeck() {
        switch (lstDecks.getGameType()) {
        case Commander:
            return DeckgenUtil.generateCommanderDeck(isAi, GameType.Commander);
        case TinyLeaders:
            return DeckgenUtil.generateCommanderDeck(isAi, GameType.TinyLeaders);
        case Archenemy:
            return DeckgenUtil.generateSchemeDeck();
        case Planechase:
            return DeckgenUtil.generatePlanarDeck();
        default:
            while (true) {
                switch (Aggregates.random(DeckType.ConstructedOptions)) {
                case PRECONSTRUCTED_DECK:
                    return Aggregates.random(DeckProxy.getAllPreconstructedDecks(QuestController.getPrecons())).getDeck();
                case QUEST_OPPONENT_DECK:
                    return Aggregates.random(DeckProxy.getAllQuestEventAndChallenges()).getDeck();
                case COLOR_DECK:
                    final List<String> colors = new ArrayList<String>();
                    final int count = Aggregates.randomInt(1, 3);
                    for (int i = 1; i <= count; i++) {
                        colors.add("Random " + i);
                    }
                    return DeckgenUtil.buildColorDeck(colors, isAi);
                case THEME_DECK:
                    return Aggregates.random(DeckProxy.getAllThemeDecks()).getDeck();
                default:
                    continue;
                }
            }
        }
    }

    private Deck getUserDeck() {
        Iterable<Deck> decks;
        switch (lstDecks.getGameType()) {
        case Commander:
            decks = FModel.getDecks().getCommander();
            break;
        case TinyLeaders:
            decks = DeckFormat.TinyLeaders.getLegalDecks(FModel.getDecks().getCommander());
            break;
        case Archenemy:
            decks = FModel.getDecks().getScheme();
            break;
        case Planechase:
            decks = FModel.getDecks().getPlane();
            break;
        default:
            decks = FModel.getDecks().getConstructed();
            break;
        }
        if (Iterables.isEmpty(decks)) {
            return getGeneratedDeck(); //fall back to generated deck if no decks in filtered list
        }
        return Aggregates.random(decks);
    }

    private Deck getFavoriteDeck() {
        Iterable<DeckProxy> decks;
        switch (lstDecks.getGameType()) {
        case Commander:
            decks = DeckProxy.getAllCommanderDecks();
            break;
        case TinyLeaders:
            decks = Iterables.filter(DeckProxy.getAllCommanderDecks(), new Predicate<DeckProxy>() {
                @Override public boolean apply(final DeckProxy deck) {
                    return DeckFormat.TinyLeaders.getDeckConformanceProblem(deck.getDeck()) == null;
                }
            });
            break;
        case Archenemy:
            decks = DeckProxy.getAllSchemeDecks();
            break;
        case Planechase:
            decks = DeckProxy.getAllPlanarDecks();
            break;
        default:
            decks = DeckProxy.getAllConstructedDecks();
            break;
        }
        decks = Iterables.filter(decks, new Predicate<DeckProxy>() {
            @Override public boolean apply(final DeckProxy deck) {
                return deck.isFavoriteDeck();
            }
        });
        if (Iterables.isEmpty(decks)) {
            return getGeneratedDeck(); //fall back to generated deck if no favorite decks
        }
        return Aggregates.random(decks).getDeck();
    }

    @Override
    public boolean isGeneratedDeck() {
        return true;
    }
}
