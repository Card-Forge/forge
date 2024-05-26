package forge.gamemodes.limited;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import forge.card.MagicColor;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.gui.util.SGuiChoose;
import forge.item.PaperCard;
import forge.util.TextUtil;

import java.util.*;

public class LimitedPlayer {
    // A Player class for inside some type of limited environment, like Draft.
    final protected int order;
    protected int currentPack;
    protected int draftedThisRound;
    protected Deck deck;

    protected Queue<List<PaperCard>> packQueue;
    protected Queue<List<PaperCard>> unopenedPacks;
    protected List<PaperCard> removedFromCardPool = new ArrayList<>();

    private static final int  CantDraftThisRound = 1;
    private static final int SpyNextCardDrafted = 1 << 1;
    private static final int ReceiveLastCard = 1 << 2;
    private static final int CanRemoveAfterDraft = 1 << 3;
    private static final int CanTradeAfterDraft = 1 << 4;
    private static final int AnimusRemoveFromPool = 1 << 5;

    private static final int MAXFLAGS = CantDraftThisRound | ReceiveLastCard | CanRemoveAfterDraft | SpyNextCardDrafted
                                    | CanTradeAfterDraft | AnimusRemoveFromPool;

    private int playerFlags = 0;

    private final List<PaperCard> faceUp = Lists.newArrayList();
    private final List<PaperCard> revealed = Lists.newArrayList();
    private final Map<String, List<String>> noted = new HashMap<>();

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

    public Deck getDeck() {
        return deck;
    }

    public List<PaperCard> getRemovedFromCardPool() {
        return removedFromCardPool;
    }

    public PaperCard chooseCard() {
        // A non-AI LimitedPlayer chooses cards via the UI instead of this function
        // TODO Archdemon of Paliano random draft while active

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

        draftedThisRound++;

        if ((playerFlags & AnimusRemoveFromPool) == AnimusRemoveFromPool &&
                removeWithAnimus(bestPick)) {
            removedFromCardPool.add(bestPick);
            addLog(name() + " removed " + bestPick.getName() + " from the draft for Animus of Predation.");

            List<String> keywords = new ArrayList<String>();
            if (bestPick.getRules().getType().isCreature()) {
                for (String keyword : bestPick.getRules().getMainPart().getKeywords()) {
                    switch (keyword) {
                        case "Flying":
                            keywords.add("Flying");
                            break;
                        case "First strike":
                            keywords.add("First Strike");
                            break;
                        case "Double strike":
                            keywords.add("Double Strike");
                            break;
                        case "Deathtouch":
                            keywords.add("Deathtouch");
                            break;
                        case "Haste":
                            keywords.add("Haste");
                            break;
                        case "Hexproof":
                            keywords.add("Hexproof");
                            break;
                        case "Indestructible":
                            keywords.add("Indestructible");
                            break;
                        case "Lifelink":
                            keywords.add("Lifelink");
                            break;
                        case "Menace":
                            keywords.add("Menace");
                            break;
                        case "Reach":
                            keywords.add("Reach");
                            break;
                        case "Vigilance":
                            keywords.add("Vigilance");
                            break;
                    }
                }

                if (!keywords.isEmpty()) {
                    List<String> note = noted.computeIfAbsent("Animus of Predation", k -> Lists.newArrayList());
                    note.add(String.join(",", keywords));
                    addLog(name() + " added " + String.join(",", keywords) + " for Animus of Predation.");
                }
            }

            return true;
        } else {
            CardPool pool = deck.getOrCreate(section);
            pool.add(bestPick);
        }

        if (bestPick.getRules().getMainPart().getDraftActions() == null) {
            return true;
        }

        // Draft Actions
        Iterable<String> draftActions = bestPick.getRules().getMainPart().getDraftActions();
        if (Iterables.contains(draftActions, "Reveal CARDNAME as you draft it.")) {
            revealed.add(bestPick);
            showRevealedCard(bestPick);

            if (Iterables.contains(draftActions, "Note how many cards you've drafted this draft round, including CARDNAME.")) {
                List<String> note = noted.computeIfAbsent(bestPick.getName(), k -> Lists.newArrayList());
                note.add(String.valueOf(draftedThisRound));

                addLog(name() + " revealed " + bestPick.getName() + " and noted " + draftedThisRound + " cards drafted this round.");
            } else if (Iterables.contains(draftActions, "As you draft CARDNAME, the player to your right chooses a color, you choose another color, then the player to your left chooses a third color.")) {
                List<String> chosenColors = new ArrayList<String>();

                LimitedPlayer leftPlayer = draft.getNeighbor(this, true);
                LimitedPlayer rightPlayer = draft.getNeighbor(this, false);
                List<String> availableColors = new ArrayList<String>(MagicColor.Constant.ONLY_COLORS);

                String c = rightPlayer.chooseColor(availableColors, this, bestPick.getName());
                chosenColors.add(c);
                availableColors.remove(c);

                c = this.chooseColor(availableColors, this, bestPick.getName());
                chosenColors.add(c);
                availableColors.remove(c);

                c = leftPlayer.chooseColor(availableColors, this, bestPick.getName());
                chosenColors.add(c);
                availableColors.remove(c);

                List<String> note = noted.computeIfAbsent(bestPick.getName(), k -> Lists.newArrayList());
                note.add(String.join(",", chosenColors));

                addLog(name() + " revealed " + bestPick.getName() + " and noted " + String.join(",", chosenColors) + " chosen colors.");
            }
            else {
                addLog(name() + " revealed " + bestPick.getName() + " as they drafted it.");
            }
        }
        if (Iterables.contains(draftActions, "Draft CARDNAME face up.")) {
            faceUp.add(bestPick);
            addLog(name() + " drafted " + bestPick.getName() + " face up.");
            showRevealedCard(bestPick);

            // TODO Noble Banneret
            // TODO Paliano Vanguard
            // As you draft a VALID, you may Note its [name/type/], and turn this face down

            if (Iterables.contains(draftActions, "As you draft a card, you may remove it from the draft face up. (It isn’t in your card pool.)")) {
                // Animus of Predation
                playerFlags |= AnimusRemoveFromPool;
            }
            // As you draft a VALID, you may remove it face up. (It's no longer in your draft pool)
            // TODO We need a deck section that's not your sideboard but is your cardpool?
            // Keyword absorption: If creature is absorbed, it gains all the abilities of the creature it absorbed. This includes
            // flying, first strike, double strike, deathtouch, haste, hexproof, indestructible, lifelink, menace, reach, and vigilance.
        }

        // Note who passed it to you. (Either player before you in draft passing order except if you receive the last card
        // TODO Cogwork Tracker


        // Note next card on this card
        // TODO Aether Searcher (for the next card)

        // Peek at next card from this pack
        // TODO Cogwork Spy

        // TODO Lore Seeker
        // This adds a pack and MIGHT screw up all of our assumptions about pack passing. Do this last probably

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

    protected String chooseColor(List<String> colors, LimitedPlayer player, String title) {
        return SGuiChoose.one(player.name() + " drafted " + title + ": Choose a color", colors);
    }

    protected boolean removeWithAnimus(PaperCard bestPick) {
        return SGuiChoose.one("Remove this " + bestPick + " from the draft for ANnimus of Predation?", Lists.newArrayList("Yes", "No")).equals("Yes");
    }

    public String name() {
        if (this instanceof LimitedPlayerAI) {
            return "Player[" + order + "]";
        }

        return "You";
    }

    public void showRevealedCard(PaperCard pick) {
        // TODO Show the revealed card in the CardDetailPanel

    }

    /*
    public void addSingleBoosterPack(boolean random) {
        // TODO Lore Seeker
        // Generate booster pack then, insert it "before" the pack we're currently drafting from
    }
    */
}
