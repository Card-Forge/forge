package forge.gamemodes.limited;

import forge.card.CardEdition;
import forge.card.ColorSet;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.deck.generation.DeckGeneratorBase;
import forge.item.PaperCard;
import forge.item.PaperCardPredicates;
import forge.localinstance.properties.ForgePreferences;
import forge.util.IterableUtil;
import forge.util.MyRandom;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static forge.gamemodes.limited.CardRanker.getOrderedRawScores;
import static forge.gamemodes.limited.CardRanker.rankCardsInPack;

public class LimitedPlayerAI extends LimitedPlayer {
    protected DeckColors deckCols;

    public LimitedPlayerAI(int seatingOrder, BoosterDraft draft) {
        super(seatingOrder, draft);
        deckCols = new DeckColors();
    }

    @Override
    public PaperCard chooseCard() {
        if (packQueue.isEmpty()) {
            return null;
        }

        DraftPack chooseFrom = packQueue.peek();
        if (chooseFrom.isEmpty()) {
            return null;
        }

        CardPool pool = deck.getOrCreate(DeckSection.Sideboard);
        if (ForgePreferences.DEV_MODE) {
            System.out.println("Player[" + order + "] pack: " + chooseFrom);
        }

        PaperCard bestPick;
        if (hasArchdemonCurse()) {
            bestPick = pickFromArchdemonCurse(chooseFrom);
        } else {
            final ColorSet chosenColors = deckCols.getChosenColors();
            final boolean canAddMoreColors = deckCols.canChoseMoreColors();

            List<PaperCard> rankedCards = rankCardsInPack(chooseFrom, pool.toFlatList(), chosenColors, canAddMoreColors);
            bestPick = rankedCards.get(0);

            if (canAddMoreColors) {
                deckCols.addColorsOf(bestPick);
            }
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

    @Override
    protected String chooseColor(List<String> colors, LimitedPlayer player, String title) {
        if (player.equals(this)) {
            // For Paliano, choose one of my colors
            // For Regicide, random is fine?
        } else {
            // For Paliano, if player has revealed anything, try to avoid that color
            // For Regicide, don't choose one of my colors
        }
        Collections.shuffle(colors);
        return colors.get(0);
    }

    @Override
    protected String removeWithAny(PaperCard bestPick, List<String> options) {
        // If we have multiple remove from draft options, do none of them for now

        Collections.shuffle(options);
        if (options.get(0).equals("Animus of Predation")) {
            if (removeWithAnimus(bestPick)) {
                return "Animus of Predation";
            }
        } else if (options.get(0).equals("Cogwork Grinder")) {
            if (removeWithGrinder(bestPick)) {
                return "Cogwork Grinder";
            }
        }

        return null;
    }

    private boolean removeWithAnimus(PaperCard bestPick) {
        // TODO Animus of Predation logic
        // Feel free to remove any cards that we won't play and can give us a bonus
        // We should verify we don't already have the keyword bonus that card would grant
        return false;
    }

    private boolean removeWithGrinder(PaperCard bestPick) {
        // TODO Cogwork Grinder logic
        // Feel free to remove any cards that we won't play and can give us a bonus
        // We should verify we don't already have the keyword bonus that card would grant
        return false;
    }

    private boolean isUsefulCard(PaperCard bestPick) {
        // Determine if this card is useful. How likely is it to make the deck we're building.
        // If no, then we need to figure out which card we should remove it for
        return false;
    }

    @Override
    protected boolean revealWithBanneret(PaperCard bestPick) {
        // Just choose the first creature that we haven't noted yet.
        // This is a very simple heuristic, but it's good enough for now.
        if (!bestPick.getRules().getType().isCreature()) {
            return false;
        }

        List<String> nobleBanneret = getDraftNotes().getOrDefault("Noble Banneret", null);
        return nobleBanneret == null || !nobleBanneret.contains(bestPick.getName());
    }

    @Override
    protected boolean revealWithVanguard(PaperCard bestPick) {
        // Just choose the first creature that we haven't noted types of yet.
        // This is a very simple heuristic, but it's good enough for now.
        if (!bestPick.getRules().getType().isCreature()) {
            return false;
        }

        List<String> notedTypes = getDraftNotes().getOrDefault("Paliano Vanguard", null);

        Set<String> types = bestPick.getRules().getType().getCreatureTypes();

        if (notedTypes == null || types.isEmpty()) {
            return false;
        }

        return types.containsAll(notedTypes);
    }

    @Override
    protected boolean revealWithSmuggler(PaperCard bestPick) {
        // Note a name we haven't noted yet
        List<String> notedNames = getDraftNotes().getOrDefault("Smuggler Captain", null);
        if (!notedNames.isEmpty() && notedNames.contains(bestPick.getName())) {
            return false;
        }

        if (bestPick.getRules().getType().isConspiracy()) {
            return false;
        }

        if (currentPack == 3) {
            // If we're already on the last pack, we may not get a better choice
            return true;
        }

        // If we're on the first two packs get the bombiest of cards available.
        return draftedThisRound < 3;
    }

    @Override
    public boolean handleWhispergearSneak() {
        // Always choose the next pack I will open
        // What do I do with this information? Great question. I have no idea.
        List<PaperCard> cards;
        if (draft.getRound() == 3) {
            // Take a peek at the pack you are about to get if its the last round
            cards = peekAtBoosterPack(this.order, 1);
        } else {
            cards = peekAtBoosterPack(this.order, draft.getRound() + 1);
        }

        return true;
    }

    @Override
    public boolean handleIllusionaryInformant() {
        // Always choose the next pack I will open
        // What do I do with this information? Great question. I have no idea.
        int player;
        do {
            player = MyRandom.getRandom().nextInt(draft.getOpposingPlayers().length + 1);
        } while(player == this.order);


        LimitedPlayer peekAt = draft.getPlayer(player);
        if (peekAt == null) {
            return false;
        }

        // Not really sure what the AI does with this information. But its' known now.
        //peekAt.getLastPick();
        return true;
    }

    @Override
    public PaperCard handleSpirePhantasm(DraftPack chooseFrom) {
        if (chooseFrom.isEmpty()) {
            return null;
        }

        // Choose the card with the highest rank left
        return getOrderedRawScores(chooseFrom).get(0);
    }

    @Override
    public boolean handleLeovoldsOperative(DraftPack pack, PaperCard drafted) {
        // Whats the score of the thing I just drafted?
        // Whats the next card I would draft?
        if (currentPack == 3) {
            return true;
        }

        return draftedThisRound < 3;
    }

    @Override
    public boolean handleAgentOfAcquisitions(DraftPack pack, PaperCard drafted) {
        // Whats the score of the thing I just drafted?
        // Whats the total score of the rest of the pack?
        // How many of these cards would actually make my deck?
        if (currentPack == 3) {
            return true;
        }

        return draftedThisRound > 2 && draftedThisRound < 6;
    }

    @Override
    public boolean handleCogworkLibrarian(DraftPack pack, PaperCard drafted) {
        if (currentPack == 3) {
            return true;
        }

        return draftedThisRound < 3;
    }

    @Override
    protected CardEdition chooseEdition(List<CardEdition> possibleEditions) {
        Collections.shuffle(possibleEditions);
        return possibleEditions.get(0);
    }

    @Override
    protected PaperCard chooseExchangeCard(PaperCard offer) {
        final ColorSet colors = deckCols.getChosenColors();
        List<PaperCard> deckCards = deck.getOrCreate(DeckSection.Sideboard).toFlatList();

        DeckGeneratorBase.MatchColorIdentity hasColor = new DeckGeneratorBase.MatchColorIdentity(colors);
        Iterable<PaperCard> colorList = IterableUtil.filter(deckCards,
                PaperCardPredicates.fromRules(hasColor).negate());

        PaperCard exchangeCard = null;

        if (offer == null) {
            // Choose the highest rated card outside your colors
            List<PaperCard> rankedColorList = CardRanker.rankCardsInDeck(colorList);
            return rankedColorList.get(0);
        }

        // Choose a card in my deck outside my colors with similar value
        List<Pair<Double, PaperCard>> rankedColorList = CardRanker.getScores(colorList);
        double score = CardRanker.getRawScore(offer);
        double closestScore = Double.POSITIVE_INFINITY;

        for (Pair<Double, PaperCard> pair : rankedColorList) {
            double diff = Math.abs(pair.getLeft() - score);
            if (diff < closestScore) {
                closestScore = diff;
                exchangeCard = pair.getRight();
            }
        }

        return exchangeCard;
    }

    protected PaperCard chooseCardToExchange(PaperCard exchangeCard, Map<PaperCard, LimitedPlayer> offers) {
        double score = CardRanker.getRawScore(exchangeCard);
        List<Pair<Double, PaperCard>> rankedColorList = CardRanker.getScores(offers.keySet());
        final ColorSet colors = deckCols.getChosenColors();
        for(Pair<Double, PaperCard> pair : rankedColorList) {
            ColorSet cardColors = pair.getRight().getRules().getColorIdentity();
            if (!cardColors.hasNoColorsExcept(colors)) {
                continue;
            }

            if (score < pair.getLeft()) {
                return pair.getRight();
            }

            double threshold = Math.abs(pair.getLeft() - score) / pair.getLeft();
            if (threshold < 0.1) {
                return pair.getRight();
            }
        }

        return null;
    }
}
