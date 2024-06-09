package forge.gamemodes.limited;

import forge.card.ColorSet;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgePreferences;
import forge.util.MyRandom;

import java.util.Collections;
import java.util.List;
import java.util.Set;

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

        List<PaperCard> chooseFrom = packQueue.peek();
        if (chooseFrom.isEmpty()) {
            return null;
        }

        CardPool pool = deck.getOrCreate(DeckSection.Sideboard);
        if (ForgePreferences.DEV_MODE) {
            System.out.println("Player[" + order + "] pack: " + chooseFrom);
        }

        // TODO Archdemon of Paliano random draft while active


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
}
