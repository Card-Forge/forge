package forge.gamemodes.limited;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
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

    private static final int CantDraftThisRound = 1;
    private static final int SpyNextCardDrafted = 1 << 1;
    private static final int ReceiveLastCard = 1 << 2;
    private static final int CanRemoveAfterDraft = 1 << 3;
    private static final int CanTradeAfterDraft = 1 << 4;
    private static final int AnimusRemoveFromPool = 1 << 5;
    private static final int NobleBanneretActive = 1 << 6;
    private static final int PalianoVanguardActive = 1 << 7;
    private static final int GrinderRemoveFromPool = 1 << 8;

    private static final int MAXFLAGS = CantDraftThisRound | ReceiveLastCard | CanRemoveAfterDraft | SpyNextCardDrafted
                                    | CanTradeAfterDraft | AnimusRemoveFromPool | NobleBanneretActive | PalianoVanguardActive
                                    | GrinderRemoveFromPool;

    private int playerFlags = 0;

    private final List<PaperCard> faceUp = Lists.newArrayList();
    private final List<PaperCard> revealed = Lists.newArrayList();
    private final Map<String, List<String>> noted = new HashMap<>();
    private final HashSet<String> semicolonDelimiter = Sets.newHashSet("Noble Banneret", "Cogwork Grinder");

    IBoosterDraft draft;

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
            serialized.put(entry.getKey(), TextUtil.join(entry.getValue(),
                    semicolonDelimiter.contains(entry.getKey()) ? ";" : ","));
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

        boolean removedFromPool = false;
        boolean alreadyRevealed = false;

        chooseFrom.remove(bestPick);

        draftedThisRound++;

        List<String> removeFromSource = Lists.newArrayList();
        String choice = null;
        if ((playerFlags & AnimusRemoveFromPool) == AnimusRemoveFromPool) {
            removeFromSource.add("Animus of Predation");
        }
        if ((playerFlags & GrinderRemoveFromPool) == GrinderRemoveFromPool) {
            removeFromSource.add("Cogwork Grinder");
        }

        if (!removeFromSource.isEmpty()) {
            removeFromSource.add(0, "Don't Remove");
            choice = removeWithAny(bestPick, removeFromSource);
        }
        if (choice != null && !choice.equals("Don't Remove")) {
            removedFromPool = true;
            removedFromCardPool.add(bestPick);
            if (choice.equals("Animus of Predation")) {
                addLog(name() + " removed " + bestPick.getName() + " from the draft with " + choice + ".");
            } else if (choice.equals("Cogwork Grinder")) {
                addLog(name() + " removed a card face down from the draft with " + choice + ".");
            }

            recordRemoveFromDraft(bestPick, choice);
        }

        if (removedFromPool) {
            // Can we hide this from UI?
            return true;
        }

        CardPool pool = deck.getOrCreate(section);
        pool.add(bestPick);

        alreadyRevealed |= handleNobleBanneret(bestPick);
        alreadyRevealed |= handlePalianoVanguard(bestPick);

        Iterable<String> draftActions = bestPick.getRules().getMainPart().getDraftActions();
        if (draftActions == null || !draftActions.iterator().hasNext()) {
            return true;
        }

        // Draft Actions
        if (Iterables.contains(draftActions, "Reveal CARDNAME as you draft it.")) {
            if (!alreadyRevealed) {
                revealed.add(bestPick);
                showRevealedCard(bestPick);
            }

            if (Iterables.contains(draftActions, "Note how many cards you've drafted this draft round, including CARDNAME.")) {
                List<String> note = noted.computeIfAbsent(bestPick.getName(), k -> Lists.newArrayList());
                note.add(String.valueOf(draftedThisRound));

                addLog(name() + " revealed " + bestPick.getName() + " and noted " + draftedThisRound + " cards drafted this round.");
            } else if (Iterables.contains(draftActions, "As you draft CARDNAME, the player to your right chooses a color, you choose another color, then the player to your left chooses a third color.")) {
                List<String> chosenColors = new ArrayList<>();

                LimitedPlayer leftPlayer = draft.getNeighbor(this, true);
                LimitedPlayer rightPlayer = draft.getNeighbor(this, false);
                List<String> availableColors = new ArrayList<>(MagicColor.Constant.ONLY_COLORS);

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
            if (!alreadyRevealed) {
                showRevealedCard(bestPick);
            }

            if (Iterables.contains(draftActions, "As you draft a card, you may remove it from the draft face up. (It isn’t in your card pool.)") &&
                    bestPick.getName().equals("Animus of Predation")) {
                playerFlags |= AnimusRemoveFromPool;
            } else if (Iterables.contains(draftActions, "As you draft a card, you may remove it from the draft face down. (Those cards aren’t in your card pool.)") &&
                    bestPick.getName().equals("Cogwork Grinder")) {
                playerFlags |= GrinderRemoveFromPool;
            } else if (Iterables.contains(draftActions, "As you draft a creature card, you may reveal it, note its name, then turn CARDNAME face down.")) {
                playerFlags |= NobleBanneretActive;
            } else if (Iterables.contains(draftActions, "As you draft a creature card, you may reveal it, note its creature types, then turn CARDNAME face down.")) {
                playerFlags |= PalianoVanguardActive;
            }
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

    protected String removeWithAny(PaperCard bestPick, List<String> options) {
        return SGuiChoose.one("Remove this " + bestPick + " from the draft with?", options);
    }

    protected boolean revealWithBanneret(PaperCard bestPick) {
        return SGuiChoose.one("Reveal this " + bestPick + " for Noble Banneret?", Lists.newArrayList("Yes", "No")).equals("Yes");
    }

    protected boolean revealWithVanguard(PaperCard bestPick) {
        return SGuiChoose.one("Reveal this " + bestPick + " for Paliano Vanguard?", Lists.newArrayList("Yes", "No")).equals("Yes");
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

    public void recordRemoveFromDraft(PaperCard bestPick, String host) {
        List<String> note = noted.computeIfAbsent(host, k -> Lists.newArrayList());

        if (host.equals("Animus of Predation")) {
            if (!bestPick.getRules().getType().isCreature()) {
                return;
            }
            List<String> keywords = new ArrayList<>();
            for (String keyword : bestPick.getRules().getMainPart().getKeywords()) {
                if (keyword.startsWith("Hexproof")) {
                    keywords.add(keyword);
                    continue;
                }

                switch (keyword) {
                    case "Flying":
                        keywords.add("Flying");
                        break;
                    case "First Strike":
                        keywords.add("First Strike");
                        break;
                    case "Double Strike":
                        keywords.add("Double Strike");
                        break;
                    case "Deathtouch":
                        keywords.add("Deathtouch");
                        break;
                    case "Haste":
                        keywords.add("Haste");
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

            note.add(String.join(",", keywords));
        } else if (host.equals("Cogwork Grinder")) {
            note.add(bestPick.getName());
        }
    }

    public boolean handleNobleBanneret(PaperCard bestPick) {
        boolean alreadyRevealed;
        if ((playerFlags & NobleBanneretActive) != NobleBanneretActive) {
            return false;
        }

        if (!bestPick.getRules().getType().isCreature()) {
            return false;
        }

        boolean remaining = false;
        PaperCard found = null;

        for(PaperCard c : faceUp) {
            if (c.getName().equals("Noble Banneret")) {
                if (found == null) {
                    found = c;
                } else {
                    remaining = true;
                    break;
                }
            }
        }

        if (found == null) {
            playerFlags &= ~NobleBanneretActive;
            return false;
        }

        if (!revealWithBanneret(bestPick)) {
            return false;
        }

        // As you draft a creature card, you may reveal it, note its name, then turn CARDNAME face down.
        List<String> note = noted.computeIfAbsent(found.getName(), k -> Lists.newArrayList());
        revealed.add(bestPick);
        note.add(bestPick.getName());
        addLog(name() + " revealed " + bestPick.getName() + " and noted its name for Noble Banneret.");
        addLog(name() + " has flipped Noble Banneret face down.");
        alreadyRevealed = true;

        faceUp.remove(found);

        if (!remaining) {
            playerFlags &= ~NobleBanneretActive;
        }
        return alreadyRevealed;
    }

    public boolean handlePalianoVanguard(PaperCard bestPick) {
        boolean alreadyRevealed;
        if ((playerFlags & PalianoVanguardActive) != PalianoVanguardActive) {
            return false;
        }

        if (!bestPick.getRules().getType().isCreature()) {
            return false;
        }

        boolean remaining = false;
        PaperCard found = null;

        for(PaperCard c : faceUp) {
            if (c.getName().equals("Paliano Vanguard")) {
                if (found == null) {
                    found = c;
                } else {
                    remaining = true;
                    break;
                }
            }
        }

        if (found == null) {
            playerFlags &= ~PalianoVanguardActive;
            return false;
        }

        if (!revealWithVanguard(bestPick)) {
            return false;
        }

        // As you draft a creature card, you may reveal it, note its name, then turn CARDNAME face down.
        List<String> note = noted.computeIfAbsent(found.getName(), k -> Lists.newArrayList());
        revealed.add(bestPick);
        note.addAll(bestPick.getRules().getType().getCreatureTypes());
        addLog(name() + " revealed " + bestPick.getName() + " and noted - " + TextUtil.join(bestPick.getRules().getType().getCreatureTypes(), ",") + " for Paliano Vanguard.");
        addLog(name() + " has flipped Paliano Vanguard face down.");
        alreadyRevealed = true;

        faceUp.remove(found);

        if (!remaining) {
            playerFlags &= ~PalianoVanguardActive;
        }
        return alreadyRevealed;
    }

    /*
    public void addSingleBoosterPack(boolean random) {
        // TODO Lore Seeker
        // Generate booster pack then, insert it "before" the pack we're currently drafting from
    }
    */
}
