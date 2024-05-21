package forge.gamemodes.limited;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.item.PaperCard;
import forge.util.TextUtil;

import java.util.*;
//import forge.gamemodes.limited.powers.DraftPower;

public class LimitedPlayer {
    // A Player class for inside some type of limited environment, like Draft.
    final protected int order;
    protected int currentPack;
    protected int draftedThisRound;
    protected Deck deck;

    protected Queue<List<PaperCard>> packQueue;
    protected Queue<List<PaperCard>> unopenedPacks;

    private static final int  CantDraftThisRound = 1;
    private static final int SpyNextCardDrafted = 1 << 1;
    private static final int ReceiveLastCard = 1 << 2;
    private static final int CanRemoveAfterDraft = 1 << 3;
    private static final int CanTradeAfterDraft = 1 << 4;

    private static final int MAXFLAGS = CantDraftThisRound | ReceiveLastCard | CanRemoveAfterDraft | SpyNextCardDrafted
                                    | CanTradeAfterDraft;

    private final int playerFlags = 0;

    private final List<PaperCard> faceUp = Lists.newArrayList();
    private final List<PaperCard> revealed = Lists.newArrayList();
    private final Map<String, List<String>> noted = new HashMap<>();
    //private Map<DraftPower, Integer> powers = new HashMap<>();

    IBoosterDraft draft = null;

    public LimitedPlayer(int seatingOrder, IBoosterDraft draft) {
        order = seatingOrder;
        deck = new Deck();

        packQueue = new LinkedList<>();
        unopenedPacks = new LinkedList<>();
        this.draft = draft;
    }

    public Map<String, List<String>> getDraftNotes() {
        return noted;
    }

    public Map<String, String> getSerializedDraftNotes() {
        Map<String, String> serialized = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : noted.entrySet()) {
            serialized.put(entry.getKey(), TextUtil.join(entry.getValue(), ","));
        }
        return serialized;
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

        if (bestPick.getRules().getMainPart().getDraftActions() == null) {
            return true;
        }

        // Draft Actions
        Iterable<String> draftActions = bestPick.getRules().getMainPart().getDraftActions();
        if (Iterables.contains(draftActions, "Reveal CARDNAME as you draft it.")) {
            revealed.add(bestPick);

            if (Iterables.contains(draftActions, "Note how many cards you've drafted this draft round, including CARDNAME.")) {
                List<String> note = noted.computeIfAbsent(bestPick.getName(), k -> Lists.newArrayList());
                note.add(String.valueOf(draftedThisRound));

                addLog(name() + " revealed " + bestPick.getName() + " and noted " + draftedThisRound + " cards drafted this round.");
            } else {
                addLog(name() + " revealed " + bestPick.getName() + " as they drafted it.");
            }
        }

        // Colors
        // TODO Note Paliano, the High City
        // TODO Note Regicide
        // TODO Note Paliano Vanguard
        // TODO Note Aether Searcher (for the next card)

        return true;
    }

    public void addLog(String message) {
        this.draft.getDraftLog().addLogEntry(message);
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

    public String name() {
        if (this instanceof LimitedPlayerAI) {
            return "Player[" + order + "]";
        }

        return "You";
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
