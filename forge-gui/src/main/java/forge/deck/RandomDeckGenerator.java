package forge.deck;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import forge.game.GameType;
import forge.game.IHasGameType;
import forge.gamemodes.quest.QuestController;
import forge.model.FModel;
import forge.util.Aggregates;

public class RandomDeckGenerator extends DeckProxy implements Comparable<RandomDeckGenerator> {
    private enum RandomDeckType {
        Generated,
        User,
        Favorite
    }

    public static List<DeckProxy> getRandomDecks(final IHasGameType lstDecks0, final boolean isAi0) {
        final List<DeckProxy> decks = new ArrayList<>();

        decks.add(new RandomDeckGenerator("Random Generated Deck", RandomDeckType.Generated, lstDecks0, isAi0));
        decks.add(new RandomDeckGenerator("Random User Deck", RandomDeckType.User, lstDecks0, isAi0));
        decks.add(new RandomDeckGenerator("Random Favorite Deck", RandomDeckType.Favorite, lstDecks0, isAi0));

        return decks;
    }

    public static Deck getRandomUserDeck(final IHasGameType lstDecks0, final boolean isAi0) {
        RandomDeckGenerator generator = new RandomDeckGenerator(null, RandomDeckType.User, lstDecks0, isAi0);
        return generator.getDeck();
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
        case Oathbreaker:
            return DeckgenUtil.generateCommanderDeck(isAi, GameType.Oathbreaker);
        case TinyLeaders:
            return DeckgenUtil.generateCommanderDeck(isAi, GameType.TinyLeaders);
        case Brawl:
            return DeckgenUtil.generateCommanderDeck(isAi, GameType.Brawl);
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
                    List<String> colors = new ArrayList<>();
                    int count = Aggregates.randomInt(1, 3);
                    for (int i = 1; i <= count; i++) {
                        colors.add("Random " + i);
                    }
                    return DeckgenUtil.buildColorDeck(colors, null, isAi);
                case STANDARD_CARDGEN_DECK:
                        return DeckgenUtil.buildLDACArchetypeDeck(FModel.getFormats().getStandard(),isAi);
                case PIONEER_CARDGEN_DECK:
                        return DeckgenUtil.buildLDACArchetypeDeck(FModel.getFormats().getPioneer(),isAi);
                case HISTORIC_CARDGEN_DECK:
                        return DeckgenUtil.buildLDACArchetypeDeck(FModel.getFormats().getHistoric(),isAi);
                case MODERN_CARDGEN_DECK:
                        return DeckgenUtil.buildLDACArchetypeDeck(FModel.getFormats().getModern(),isAi);
                case LEGACY_CARDGEN_DECK:
                        return DeckgenUtil.buildLDACArchetypeDeck(FModel.getFormats().get("Legacy"),isAi);
                case VINTAGE_CARDGEN_DECK:
                        return DeckgenUtil.buildLDACArchetypeDeck(FModel.getFormats().get("Vintage"),isAi);
                case STANDARD_COLOR_DECK:
                    colors = new ArrayList<>();
                    count = Aggregates.randomInt(1, 3);
                    for (int i = 1; i <= count; i++) {
                        colors.add("Random " + i);
                    }
                    return DeckgenUtil.buildColorDeck(colors, FModel.getFormats().getStandard().getFilterPrinted(), isAi);
                case MODERN_COLOR_DECK:
                    colors = new ArrayList<>();
                    count = Aggregates.randomInt(1, 3);
                    for (int i = 1; i <= count; i++) {
                        colors.add("Random " + i);
                    }
                    return DeckgenUtil.buildColorDeck(colors, FModel.getFormats().getModern().getFilterPrinted(), isAi);
                case THEME_DECK:
                    return Aggregates.random(DeckProxy.getAllThemeDecks()).getDeck();
                default:
                    continue;
                }
            }
        }
    }

    private Deck getUserDeck() {
        Iterable<DeckProxy> decks;
        final GameType gameType = lstDecks.getGameType();
        switch (gameType) {
        case Commander:
            decks = DeckProxy.getAllCommanderDecks(DeckFormat.Commander.isLegalDeckPredicate());
            break;
        case Oathbreaker:
            decks = DeckProxy.getAllOathbreakerDecks(DeckFormat.Oathbreaker.isLegalDeckPredicate());
            break;
        case TinyLeaders:
            decks = DeckProxy.getAllTinyLeadersDecks(DeckFormat.TinyLeaders.isLegalDeckPredicate());
            break;
        case Brawl:
            decks = DeckProxy.getAllBrawlDecks(DeckFormat.Brawl.isLegalDeckPredicate());
            break;
        case Archenemy:
            decks = DeckProxy.getAllSchemeDecks(DeckFormat.Archenemy.isLegalDeckPredicate());
            break;
        case Planechase:
            decks = DeckProxy.getAllPlanarDecks(DeckFormat.Planechase.isLegalDeckPredicate());
            break;
        default:
            decks = DeckProxy.getAllConstructedDecks(gameType.getDeckFormat().isLegalDeckPredicate());
            break;
        }
        if (Iterables.isEmpty(decks)) {
            return getGeneratedDeck(); //fall back to generated deck if no decks in filtered list
        }
        return Aggregates.random(decks).getDeck();
    }

    private Deck getFavoriteDeck() {
        Iterable<DeckProxy> decks;
        switch (lstDecks.getGameType()) {
        case Commander:
            decks = DeckProxy.getAllCommanderDecks();
            break;
        case Oathbreaker:
            decks = DeckProxy.getAllOathbreakerDecks();
            break;
        case TinyLeaders:
            decks = DeckProxy.getAllTinyLeadersDecks();
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
