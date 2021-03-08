package forge.limited;

import forge.card.ColorSet;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgePreferences;

import java.util.List;

public class LimitedPlayerAI extends LimitedPlayer {
    protected DeckColors deckCols;

    public LimitedPlayerAI(int seatingOrder) {
        super(seatingOrder);
        deckCols = new DeckColors();

    }

    @Override
    public PaperCard chooseCard() {
        if (packQueue.isEmpty()) {
            return null;
        }

        List<PaperCard> chooseFrom = packQueue.peek();
        if (chooseFrom.isEmpty()) {
            return null;
        }

        CardPool pool = deck.getOrCreate(DeckSection.Sideboard);
        if (ForgePreferences.DEV_MODE) {
            System.out.println("Player[" + order + "] pack: " + chooseFrom.toString());
        }

        final ColorSet chosenColors = deckCols.getChosenColors();
        final boolean canAddMoreColors = deckCols.canChoseMoreColors();

        List<PaperCard> rankedCards = CardRanker.rankCardsInPack(chooseFrom, pool.toFlatList(), chosenColors, canAddMoreColors);
        PaperCard bestPick = rankedCards.get(0);

        if (canAddMoreColors) {
            deckCols.addColorsOf(bestPick);
        }

        if (ForgePreferences.DEV_MODE) {
            System.out.println("Player[" + order + "] picked: " + bestPick);
        }

        return bestPick;
    }

    public Deck buildDeck(String landSetCode) {
        CardPool section = deck.getOrCreate(DeckSection.Sideboard);
        return new BoosterDeckBuilder(section.toFlatList(), deckCols).buildDeck(landSetCode);
    }
}
