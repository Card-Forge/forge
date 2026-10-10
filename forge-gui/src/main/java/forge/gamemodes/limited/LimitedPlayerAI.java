package forge.gamemodes.limited;

import forge.card.CardEdition;
import forge.card.ColorSet;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.deck.generation.DeckGeneratorBase;
import forge.item.PaperCard;
import forge.item.PaperCardPredicates;
import forge.util.Aggregates;
import forge.util.IterableUtil;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static forge.gamemodes.limited.CardRanker.getOrderedRawScores;
import static forge.gamemodes.limited.CardRanker.rankCardsInPack;

public class LimitedPlayerAI extends LimitedPlayer {
    protected DeckColors deckCols;

    public LimitedPlayerAI(int seatingOrder, BoosterDraft draft) {
        super(seatingOrder, draft);
        deckCols = new DeckColors();
    }

    private PaperCard chooseCard() {
        if (packQueue.isEmpty()) {
            return null;
        }

        DraftPack chooseFrom = packQueue.peek();
        debugPrint(chooseFrom.toString());
        if (chooseFrom.isEmpty()) {
            debugPrint("Skipped (Empty pack)");
            return null;
        }

        CardPool pool = deck.getOrCreate(DeckSection.Sideboard);

        PaperCard bestPick;
        if (isPackHidden()) {
            // The engine replaces a hidden pick with a random card
            bestPick = chooseFrom.get(0);
            debugPrint("Pick forced by Archdemon Curse.");
        } else {
            final ColorSet chosenColors = deckCols.getChosenColors();
            final boolean canAddMoreColors = deckCols.canChoseMoreColors();

            List<PaperCard> rankedCards = rankCardsInPack(chooseFrom, pool.toFlatList(), chosenColors, canAddMoreColors);
            bestPick = rankedCards.get(0);

            if (canAddMoreColors) {
                deckCols.addColorsOf(bestPick);
            }
        }

        return bestPick;
    }

    @Override
    protected void chooseAbilities(PaperCard pick, List<DraftAction> offers, boolean random, DraftPack pack,
                                   Consumer<List<DraftAction>> then) {
        List<DraftAction> chosen = new ArrayList<>();
        for (DraftAction offer : offers) {
            boolean take = switch (abilityEffect(offer.source())) {
                case NOTE_CREATURE_NAME -> revealWithBanneret(pick);
                case NOTE_CREATURE_TYPES -> revealWithVanguard(pick);
                case NOTE_CARD_NAME -> revealWithSmuggler(pick);
                case REMOVE_FACE_UP -> removeWithAnimus(pick);
                case REMOVE_FACE_DOWN -> removeWithGrinder(pick);
                case EXTRA_PICK_RETURN -> handleCogworkLibrarian();
                case EXTRA_PICK_SKIP -> handleLeovoldsOperative();
                case DRAFT_WHOLE_PACK -> handleAgentOfAcquisitions();
                default -> false;
            };
            if (take) {
                chosen.add(offer);
                if (!canCombine(chosen)) {
                    chosen.remove(chosen.size() - 1);
                }
            }
        }
        then.accept(chosen);
    }

    public Boolean draftNext() {
        PaperCard pick = chooseCard();
        if (pick == null) {
            return null;
        }
        return draftCard(pick, DeckSection.Sideboard, DraftAction.choose(pick));
    }
    public Deck buildDeck(String landSetCode) {
        CardPool section = deck.getOrCreate(DeckSection.Sideboard);
        return new BoosterDeckBuilder(section.toFlatList(), deckCols).buildDeck(landSetCode);
    }

    @Override
    protected void chooseColor(List<String> colors, LimitedPlayer drafter, String title, Consumer<String> then) {
        then.accept(Aggregates.random(colors));
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

    private boolean revealWithBanneret(PaperCard bestPick) {
        // Just choose the first creature that we haven't noted yet.
        // This is a very simple heuristic, but it's good enough for now.
        if (!bestPick.getRules().getType().isCreature()) {
            return false;
        }

        List<String> nobleBanneret = getDraftNotes().getOrDefault("Noble Banneret", null);
        return nobleBanneret == null || !nobleBanneret.contains(bestPick.getName());
    }

    private boolean revealWithVanguard(PaperCard bestPick) {
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

    private boolean revealWithSmuggler(PaperCard bestPick) {
        // Note a name we haven't noted yet
        List<String> notedNames = getDraftNotes().getOrDefault("Smuggler Captain", null);
        if (notedNames != null && !notedNames.isEmpty() && notedNames.contains(bestPick.getName())) {
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
    protected void guessCard(DraftPack pack, PaperCard source, Consumer<PaperCard> then) {
        if (!isPackHidden()) {
            then.accept(getOrderedRawScores(pack).get(0));
            return;
        }
        List<PaperCard> options = guessOptions(pack, source);
        if (!options.isEmpty()) {
            then.accept(Aggregates.random(options));
        }
    }

    private boolean handleLeovoldsOperative() {
        // Whats the score of the thing I just drafted?
        // Whats the next card I would draft?
        if (currentPack == 3) {
            return true;
        }

        return draftedThisRound < 3;
    }

    private boolean handleAgentOfAcquisitions() {
        // Whats the score of the thing I just drafted?
        // Whats the total score of the rest of the pack?
        // How many of these cards would actually make my deck?
        if (currentPack == 3) {
            return true;
        }

        return draftedThisRound > 2 && draftedThisRound < 6;
    }

    private boolean handleCogworkLibrarian() {
        if (currentPack == 3) {
            return true;
        }

        return draftedThisRound < 3;
    }

    @Override
    protected void chooseEdition(List<CardEdition> editions, PaperCard source, Consumer<CardEdition> then) {
        then.accept(Aggregates.random(editions));
    }

    @Override
    protected void chooseDredgerSeat(List<LimitedPlayer> eligible, DraftPack pack, Consumer<LimitedPlayer> then) {
        then.accept(eligible.contains(this) ? this : Aggregates.random(eligible));
    }

    @Override
    protected void chooseExchangeCard(PaperCard offer, Consumer<PaperCard> then) {
        then.accept(pickExchangeCard(offer));
    }

    @Override
    protected void chooseCardToExchange(PaperCard exchangeCard, List<Pair<PaperCard, LimitedPlayer>> offers,
                                        Consumer<Pair<PaperCard, LimitedPlayer>> then) {
        PaperCard accepted = pickOfferToAccept(exchangeCard, offers.stream().map(Pair::getKey).collect(Collectors.toList()));
        then.accept(offers.stream().filter(o -> o.getKey().equals(accepted)).findFirst().orElse(null));
    }

    private PaperCard pickExchangeCard(PaperCard offer) {
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

    private PaperCard pickOfferToAccept(PaperCard exchangeCard, List<PaperCard> offers) {
        double score = CardRanker.getRawScore(exchangeCard);
        List<Pair<Double, PaperCard>> rankedColorList = CardRanker.getScores(offers);
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
