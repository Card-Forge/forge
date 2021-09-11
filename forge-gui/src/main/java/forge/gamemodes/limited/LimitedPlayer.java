package forge.gamemodes.limited;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

//import com.google.common.collect.Lists;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.item.PaperCard;
//import forge.gamemodes.limited.powers.DraftPower;

public class LimitedPlayer {
    // A Player class for inside some type of limited environment, like Draft.
    final protected int order;
    protected int currentPack;
    protected int draftedThisRound;
    protected Deck deck;

    protected Queue<List<PaperCard>> packQueue;
    protected Queue<List<PaperCard>> unopenedPacks;

    // WIP - Draft Matters cards
    /*
    private static int  CantDraftThisRound = 1,
                        SpyNextCardDrafted = 1 << 1,
                        ReceiveLastCard = 1 << 2,
                        CanRemoveAfterDraft = 1 << 3,
                        CanTradeAfterDraft = 1 << 4;

    private static int MAXFLAGS = CantDraftThisRound | ReceiveLastCard | CanRemoveAfterDraft | SpyNextCardDrafted
                                    | CanTradeAfterDraft;


    private int playerFlags = 0;

    private List<PaperCard> revealed = Lists.newArrayList();
    private Map<String, List<Object>> noted = new HashMap<>();
    private Map<DraftPower, Integer> powers = new HashMap<>();
    */

    public LimitedPlayer(int seatingOrder) {
        order = seatingOrder;
        deck = new Deck();

        packQueue = new LinkedList<>();
        unopenedPacks = new LinkedList<>();
    }

    public PaperCard chooseCard() {
        // A basic LimitedPlayer chooses cards via the UI instead of this function
        return null;
    }

    public boolean draftCard(PaperCard bestPick) {
        return draftCard(bestPick, DeckSection.Sideboard);
    }
    public boolean draftCard(PaperCard bestPick, DeckSection section) {
        if (bestPick == null) {
            return false;
        }

        List<PaperCard> chooseFrom = packQueue.peek();
        if (chooseFrom == null) {
            return false;
        }

        chooseFrom.remove(bestPick);

        CardPool pool = deck.getOrCreate(section);
        pool.add(bestPick);
        draftedThisRound++;

        // TODO Note Lurking Automaton
        // TODO Note Paliano, the High City
        // TODO Note Aether Searcher
        // TODO Note Custodi Peacepeeper
        // TODO Note Paliano Vanguard
        // TODO Note Garbage Fire

        return true;
    }

    public List<PaperCard> nextChoice() {
        return packQueue.peek();
    }

    public void newPack() {
        currentPack = order;
        draftedThisRound = 0;
        packQueue.add(unopenedPacks.poll());
    }
    public void adjustPackNumber(int adjust, int numPacks) {
        currentPack = (currentPack + adjust + numPacks) % numPacks;
    }

    public List<PaperCard> passPack() {
        return packQueue.poll();
    }

    public void receiveUnopenedPack(List<PaperCard> pack) {
        unopenedPacks.add(pack);
    }

    public void receiveOpenedPack(List<PaperCard> pack) {
        packQueue.add(pack);
    }

    /*
    public void addSingleBoosterPack(boolean random) {
        // TODO Lore Seeker
        // Generate booster pack then, "receive" that pack
    }

    public boolean activatePower(DraftPower power) {
        if (!powers.containsKey(power)) {
            return false;
        }

        int i = (int)powers.get(power);
        if (i == 1) {
            powers.remove(power);
        } else {
            powers.put(power, i-1);
        }

        power.activate(this);


        return true;
    }

    public boolean noteObject(String cardName, Object notedObj) {
        // Returns boolean based on creation of new mapped param
        boolean alreadyContained = noted.containsKey(cardName);

        if (alreadyContained) {
            noted.get(cardName).add(notedObj);
        } else {
            noted.put(cardName, Lists.newArrayList(notedObj));
        }

        return !alreadyContained;
    }
    */
}
