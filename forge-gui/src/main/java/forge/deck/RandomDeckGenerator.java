package forge.deck;

import java.util.ArrayList;
import java.util.List;

import forge.game.GameType;
import forge.game.IHasGameType;
import forge.model.FModel;
import forge.quest.QuestController;
import forge.util.Aggregates;
import forge.util.storage.IStorage;

public class RandomDeckGenerator extends DeckProxy implements Comparable<RandomDeckGenerator> {
    private final String name;
    private final int index;
    private final IHasGameType lstDecks;
    private final boolean isAi;

    public RandomDeckGenerator(String name0, int index0, IHasGameType lstDecks0, boolean isAi0) {
        super();
        name = name0;
        index = index0;
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
        return d instanceof RandomDeckGenerator ? Integer.compare(index, ((RandomDeckGenerator)d).index) : 1;
    }

    @Override
    public Deck getDeck() {
        switch (lstDecks.getGameType()) {
        case Commander:
            if (index == 1) {
                IStorage<Deck> decks = FModel.getDecks().getCommander();
                if (decks.size() > 0) {
                    return Aggregates.random(decks);
                }
            }
            return DeckgenUtil.generateCommanderDeck(isAi, GameType.Commander);
        case TinyLeaders:
            if (index == 1) {
                IStorage<Deck> decks = FModel.getDecks().getTinyLeaders();
                if (decks.size() > 0) {
                    return Aggregates.random(decks);
                }
            }
            return DeckgenUtil.generateCommanderDeck(isAi, GameType.TinyLeaders);
        case Archenemy:
            if (index == 1) {
                IStorage<Deck> decks = FModel.getDecks().getScheme();
                if (decks.size() > 0) {
                    return Aggregates.random(decks);
                }
            }
            return DeckgenUtil.generateSchemeDeck();
        case Planechase:
            if (index == 1) {
                IStorage<Deck> decks = FModel.getDecks().getPlane();
                if (decks.size() > 0) {
                    return Aggregates.random(decks);
                }
            }
            return DeckgenUtil.generatePlanarDeck();
        default:
            if (index == 1) {
                IStorage<Deck> decks = FModel.getDecks().getConstructed();
                if (decks.size() > 0) {
                    return Aggregates.random(decks);
                }
            }
            while (true) {
                switch (Aggregates.random(DeckType.values())) {
                case PRECONSTRUCTED_DECK:
                    return Aggregates.random(DeckProxy.getAllPreconstructedDecks(QuestController.getPrecons())).getDeck();
                case QUEST_OPPONENT_DECK:
                    return Aggregates.random(DeckProxy.getAllQuestEventAndChallenges()).getDeck();
                case COLOR_DECK:
                    List<String> colors = new ArrayList<String>();
                    int count = Aggregates.randomInt(1, 3);
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

    @Override
    public boolean isGeneratedDeck() {
        return true;
    }
}
