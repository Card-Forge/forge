package forge.limited;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.common.collect.Iterables;

import forge.deck.CardPool;
import forge.deck.Deck;
import forge.item.PaperCard;
import forge.util.MyRandom;

public class WinstonDraft extends BoosterDraft {
    private WinstonDraftAI draftAI = null;
    private static int NUM_PILES = 3;
    private static int NUM_PLAYERS = 2;

    private Stack<PaperCard> deck;           // main deck where all cards
    private List<List<PaperCard>> piles;    // 3 piles to draft from

    public static WinstonDraft createDraft(final LimitedPoolType draftType) {
        final WinstonDraft draft = new WinstonDraft(draftType);
        if (!draft.generateProduct()) {
            return null;
        }
        draft.initializeWinstonDraft();
        return draft;
    }

    private WinstonDraft(final LimitedPoolType draftType) {
        draftFormat = draftType;
        draftAI = new WinstonDraftAI();

    }

    private void initializeWinstonDraft() {
        this.deck = new Stack<PaperCard>();
        for (int i = 0; i < this.product.size(); i++) {
            final Supplier<List<PaperCard>> supply = this.product.get(i);
            for(int j = 0; j < NUM_PLAYERS; j++) {
                // Remove Basic Lands from draft for simplicity
                for (final PaperCard paperCard : Iterables.filter(supply.get(), Predicates.not(PaperCard.Predicates.Presets.IS_BASIC_LAND))) {
                    this.deck.add(paperCard);
                }
            }
        }
        Collections.shuffle(this.deck, MyRandom.getRandom());

        // Create three Winston piles, adding the top card from the Winston deck to start each pile
        this.piles = new ArrayList<>();
        for(int i = 0; i < NUM_PILES; i++) {
            final List<PaperCard> pile = new ArrayList<PaperCard>();
            pile.add(this.deck.pop());
            this.piles.add(pile);
        }

        this.nextBoosterGroup = 0;

        draftAI.setDraft(this);

        if (MyRandom.percentTrue(50)) {
            // 50% chance of the AI picking the first card in a Winston Draft
            //this.computerChoose();
        }
    }

    @Override
    public CardPool nextChoice() {
        // This is called for two reasons: Skipping a pile, and getting the next one in line
        // Or taking a pile and getting reset back to the first non-empty pile
        int nextPile = -1;
        for(int i = nextBoosterGroup; i < this.piles.size(); i++) {
            if (this.piles.get(i).size() > 0) {
                nextPile = i;
                break;
            }
        }

        if (nextPile < 0 || nextPile > this.piles.size()) {
            return null;
        }

        nextBoosterGroup = nextPile;

        return getPoolByPile(nextBoosterGroup);
    }

    public CardPool getActivePool() {
        return getPoolByPile(this.nextBoosterGroup);
    }

    private CardPool getPoolByPile(final int i) {
        final CardPool result = new CardPool();
        result.addAllFlat(this.piles.get(i));
        return result;
    }

    @Override
    public void computerChoose() {
        nextBoosterGroup = 0;
        draftAI.choose();
    }

    public void refillPile(final List<PaperCard> pile) {
        if (this.deck.size() > 0) {
            pile.add(this.deck.pop());
        }
    }

    public int getNextChoice(final int startPile) {
        for(int i = startPile; i < NUM_PILES; i++) {
            if (this.piles.get(i).size() > 0) {
                return i;
            }
        }
        // All piles are empty, so draft is about to end.
        return -1;
    }

    @Override
    public boolean hasNextChoice() {
        return getNextChoice(0) >= 0;
    }

    @Override
    public boolean isRoundOver() {
        return hasNextChoice();
    }

    public boolean isLastPileAndEmptyDeck(final int pile) {
        return this.deck.size() == 0 && getNextChoice(pile+1) >= 0;
    }

    @Override
    public int getCurrentBoosterIndex() {
        return nextBoosterGroup;
    }

    public CardPool takeActivePile(final boolean humanAction) {
        final CardPool pool = getPoolByPile(this.nextBoosterGroup);

        this.piles.get(this.nextBoosterGroup).clear();
        this.refillPile(this.piles.get(this.nextBoosterGroup));
        this.nextBoosterGroup = 0;
        if (humanAction) {
            computerChoose();
        }
        return pool;
    }

    public CardPool passActivePile(final boolean humanAction) {
        this.refillPile(this.piles.get(this.nextBoosterGroup));
        this.nextBoosterGroup++;
        if (this.nextBoosterGroup >= this.piles.size()) {
            final CardPool pool = new CardPool();
            if (this.deck.size() > 0) {
                pool.add(this.deck.pop());
            }
            this.nextBoosterGroup = 0;
            if (humanAction) {
                computerChoose();
            }
            return pool;
        }
        return null;
    }

    public int getNumPiles() {
        return NUM_PILES;
    }

    @Override
    public Deck[] getDecks() {
        this.determineAIDeckColors();
        return this.draftAI.getDecks();
    }

    public void determineAIDeckColors() {
        this.draftAI.determineDeckColor();
    }

    @Override
    public boolean isPileDraft() {
        return true;
    }

    public int getDeckSize() { return this.deck.size(); }

    public int getAIDraftSize() { return this.draftAI.getAIDraftSize(); }
}
