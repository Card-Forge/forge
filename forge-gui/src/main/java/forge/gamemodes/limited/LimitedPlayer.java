package forge.gamemodes.limited;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import forge.card.CardEdition;
import forge.card.MagicColor;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.gui.util.SGuiChoose;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.util.TextUtil;

import java.util.*;
import java.util.stream.Collectors;

public class LimitedPlayer {
    // A Player class for inside some type of limited environment, like Draft.
    final protected int order;
    protected int currentPack;
    protected int draftedThisRound;
    protected Deck deck;
    protected PaperCard lastPick;

    protected Queue<DraftPack> packQueue;
    protected Queue<DraftPack> unopenedPacks;
    protected List<PaperCard> removedFromCardPool = new ArrayList<>();

    protected List<Integer> archdemonFavors;
    protected int dealBrokers = 0;

    private static final int AgentAcquisitionsCanDraftAll = 1;
    private static final int AgentAcquisitionsIsDraftingAll = 1 << 1;
    private static final int AgentAcquisitionsSkipDraftRound = 1 << 2;
    private static final int CogworkLibrarianExtraDraft = 1 << 3;
    private static final int CogworkLibrarianReturnLibrarian = 1 << 4;
    private static final int AnimusRemoveFromPool = 1 << 5;
    private static final int NobleBanneretActive = 1 << 6;
    private static final int PalianoVanguardActive = 1 << 7;
    private static final int GrinderRemoveFromPool = 1 << 8;
    private static final int SearcherNoteNext = 1 << 9;
    private static final int WhispergearBoosterPeek = 1 << 10;
    private static final int IllusionaryInformantPeek = 1 << 11;
    private static final int LeovoldsOperativeCanExtraDraft = 1 << 12;
    private static final int LeovoldsOperativeExtraDraft = 1 << 13;
    private static final int LeovoldsOperativeSkipNext = 1 << 14;
    private static final int SpyNextCardDrafted = 1 << 15;
    private static final int CanalDredgerLastPick = 1 << 16;
    private static final int ArchdemonOfPalianoCurse = 1 << 17;
    private static final int SmugglerCaptainActive = 1 << 18;

    private int playerFlags = 0;

    private final List<PaperCard> faceUp = Lists.newArrayList();
    private final List<PaperCard> revealed = Lists.newArrayList();
    private final Map<String, List<String>> noted = new HashMap<>();
    private final HashSet<String> semicolonDelimiter = Sets.newHashSet("Noble Banneret", "Cogwork Grinder", "Aether Searcher", "Smuggler Captain");

    IBoosterDraft draft;

    public LimitedPlayer(int seatingOrder, IBoosterDraft draft) {
        order = seatingOrder;
        deck = new Deck();

        packQueue = new LinkedList<>();
        unopenedPacks = new LinkedList<>();
        archdemonFavors = new ArrayList<>();
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

    public PaperCard getLastPick() {
        return lastPick;
    }

    public Deck getDeck() {
        return deck;
    }

    public List<PaperCard> getRemovedFromCardPool() {
        return removedFromCardPool;
    }

    public PaperCard chooseCard() {
        // A non-AI LimitedPlayer chooses cards via the UI instead of this function
        return null;
    }

    public Boolean draftCard(PaperCard bestPick) {
        return draftCard(bestPick, DeckSection.Sideboard);
    }
    public Boolean draftCard(PaperCard bestPick, DeckSection section) {
        if (bestPick == null) {
            return null;
        }

        DraftPack chooseFrom = packQueue.peek();
        if (chooseFrom == null) {
            return null;
        }

        boolean removedFromPool = false;
        boolean alreadyRevealed = false;
        boolean passPack = true;

        chooseFrom.remove(bestPick);
        lastPick = bestPick;

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


        LimitedPlayer fromPlayer = chooseFrom.getPassedFrom();
        // If the previous player has an active Cogwork Spy, show them this card
        if (fromPlayer != null && (fromPlayer.playerFlags & SpyNextCardDrafted) == SpyNextCardDrafted) {
            if (fromPlayer instanceof LimitedPlayerAI) {
                // I'm honestly not sure what the AI would do by learning this information
                // But just log that a reveal "happened"
                addLog(this.name() + " revealed a card to " + fromPlayer.name() + " via Cogwork Spy.");
            } else {
                addLog(this.name() + " revealed " + bestPick.getName() + " to you with Cogwork Spy.");
            }

            fromPlayer.playerFlags &= ~SpyNextCardDrafted;
        }

        if ((playerFlags & SearcherNoteNext) == SearcherNoteNext) {
            addLog(name() + " revealed " + bestPick.getName() + " for Aether Searcher.");
            playerFlags &= ~SearcherNoteNext;
            List<String> note = noted.computeIfAbsent("Aether Searcher", k -> Lists.newArrayList());
            note.add(String.valueOf(bestPick.getName()));
        }

        if ((playerFlags & SmugglerCaptainActive) == SmugglerCaptainActive) {
            if (revealWithSmuggler(bestPick)) {
                addLog(name() + " revealed " + bestPick.getName() + " for Smuggler Captain.");
                playerFlags &= ~SmugglerCaptainActive;
                List<String> note = noted.computeIfAbsent("Smuggler Captain", k -> Lists.newArrayList());
                note.add(String.valueOf(bestPick.getName()));
            }
        }

        if ((playerFlags & WhispergearBoosterPeek) == WhispergearBoosterPeek) {
            if (handleWhispergearSneak()) {
                addLog(name() + " peeked at a booster pack with Whispergear Sneak and turned it face down.");
                playerFlags &= ~WhispergearBoosterPeek;
            }
        }

        if ((playerFlags & IllusionaryInformantPeek) == IllusionaryInformantPeek) {
            if (handleIllusionaryInformant()) {
                addLog(name() + " peeked at " + fromPlayer.name() + "'s next pick with Illusionary Informant and turned it face down.");
                playerFlags &= ~IllusionaryInformantPeek;
            }
        }

        if ((playerFlags & AgentAcquisitionsCanDraftAll) == AgentAcquisitionsCanDraftAll) {
            if (handleAgentOfAcquisitions(chooseFrom, bestPick)) {
                addLog(name() + " drafted the rest of the pack with Agent of Acquisitions");
                playerFlags &= ~AgentAcquisitionsCanDraftAll;
                playerFlags |= AgentAcquisitionsIsDraftingAll;
            }
        }

        if ((playerFlags & AgentAcquisitionsIsDraftingAll) == AgentAcquisitionsIsDraftingAll) {
            if (chooseFrom.isEmpty()) {
                playerFlags &= ~AgentAcquisitionsIsDraftingAll;
                playerFlags |= AgentAcquisitionsSkipDraftRound;
            } else {
                passPack = false;
            }
        }

        if ((playerFlags & LeovoldsOperativeExtraDraft) == LeovoldsOperativeExtraDraft) {
            if (handleLeovoldsOperative(chooseFrom, bestPick)) {
                addLog(name() + " skipped their next pick with Leovold's Operative.");
                playerFlags &= ~LeovoldsOperativeExtraDraft;
                playerFlags |= LeovoldsOperativeSkipNext;
                passPack = false;
            }
        }

        if ((playerFlags & LeovoldsOperativeCanExtraDraft) == LeovoldsOperativeCanExtraDraft) {
            if (handleLeovoldsOperative(chooseFrom, bestPick)) {
                addLog(name() + " picking again with Leovold's Operative.");
                playerFlags &= ~LeovoldsOperativeCanExtraDraft;
                playerFlags |= LeovoldsOperativeExtraDraft;
                passPack = false;
            }
        }

        if ((playerFlags & CogworkLibrarianReturnLibrarian) == CogworkLibrarianReturnLibrarian) {
            addLog(name() + " returned Cogwork Librarian to the pack.");

            PaperCard librarian = deck.removeCardName("Cogwork Librarian");
            // TODO The librarian needs to be removed from the UI

            // We shouldn't get here unless we've drafted librarian so we should be able to find one in Deck
            // If somehow we don't wellll.. we should remove the bitflag anyway
            playerFlags &= ~CogworkLibrarianReturnLibrarian;
            if (librarian != null) {
                chooseFrom.add(librarian);
            } else {
                System.out.println("This shouldn't happen. We drafted a libarian but didn't remove it properly.");
            }
        }

        if ((playerFlags & CogworkLibrarianExtraDraft) == CogworkLibrarianExtraDraft) {
            if (handleCogworkLibrarian(chooseFrom, bestPick)) {
                addLog(name() + " drafted an extra card with Cogwork Librarian.");
                playerFlags &= ~CogworkLibrarianExtraDraft;
                playerFlags |= CogworkLibrarianReturnLibrarian;
                passPack = false;
            }
        }

        if (chooseFrom.getAwaitingGuess() != null) {
            comparePhantasmGuess(chooseFrom, bestPick);
        }

        if (removedFromPool) {
            // Can we hide this from UI?
            return passPack;
        }

        CardPool pool = deck.getOrCreate(section);
        pool.add(bestPick);

        alreadyRevealed |= handleNobleBanneret(bestPick);
        alreadyRevealed |= handlePalianoVanguard(bestPick);

        Iterable<String> draftActions = bestPick.getRules().getMainPart().getDraftActions();
        if (draftActions == null || !draftActions.iterator().hasNext()) {
            return passPack;
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
                if (Iterables.contains(draftActions, "You may look at the next card drafted from this booster pack.")) {
                    playerFlags |= SpyNextCardDrafted;
                } else if (fromPlayer != null && Iterables.contains(draftActions, "Note the player who passed CARDNAME to you.")) {
                    List<String> note = noted.computeIfAbsent(bestPick.getName(), k -> Lists.newArrayList());
                    note.add(String.valueOf(fromPlayer.order));
                    addLog(name() + " revealed " + bestPick.getName() + " and noted " + fromPlayer.name() + " passed it.");
                } else if (Iterables.contains(draftActions, "Reveal the next card you draft and note its name.")) {
                    playerFlags |= SearcherNoteNext;
                } else if (Iterables.contains(draftActions, "The next time a player drafts a card from this booster pack, guess that card's name. Then that player reveals the drafted card.")) {
                    chooseFrom.setAwaitingGuess(this, handleSpirePhantasm(chooseFrom));
                } else if (Iterables.contains(draftActions, "After you draft CARDNAME, you may add a booster pack to the draft. (Your next pick is from that booster pack. Pass it to the next player and it's drafted this draft round.)")) {
                    addSingleBoosterPack();
                }

                addLog(name() + " revealed " + bestPick.getName() + " as " + name() + " drafted it.");
            }
        }
        if (Iterables.contains(draftActions, "Draft CARDNAME face up.")) {
            faceUp.add(bestPick);
            addLog(name() + " drafted " + bestPick.getName() + " face up.");
            if (!alreadyRevealed) {
                showRevealedCard(bestPick);
            }

            if (Iterables.contains(draftActions, "As you draft a card, you may remove it from the draft face up. (It isn't in your card pool.)") &&
                    bestPick.getName().equals("Animus of Predation")) {
                playerFlags |= AnimusRemoveFromPool;
            } else if (Iterables.contains(draftActions, "As you draft a card, you may remove it from the draft face down. (Those cards aren't in your card pool.)") &&
                    bestPick.getName().equals("Cogwork Grinder")) {
                playerFlags |= GrinderRemoveFromPool;
            } else if (Iterables.contains(draftActions, "As you draft a creature card, you may reveal it, note its name, then turn CARDNAME face down.")) {
                playerFlags |= NobleBanneretActive;
            } else if (Iterables.contains(draftActions, "As you draft a card, you may reveal it, note its name, then turn CARDNAME face down.")) {
                playerFlags |= SmugglerCaptainActive;
            } else if (Iterables.contains(draftActions, "As you draft a creature card, you may reveal it, note its creature types, then turn CARDNAME face down.")) {
                playerFlags |= PalianoVanguardActive;
            } else if (Iterables.contains(draftActions, "During the draft, you may turn CARDNAME face down. If you do, look at any unopened booster pack in the draft or any booster pack not being looked at by another player.")) {
                playerFlags |= WhispergearBoosterPeek;
                // Do we need to ask to use the Sneak immediately?
            } else if (Iterables.contains(draftActions, "During the draft, you may turn CARDNAME face down. If you do, look at the next card drafted by a player of your choice.")) {
                playerFlags |= IllusionaryInformantPeek;
            } else if (Iterables.contains(draftActions, "As you draft a card, you may draft an additional card from that booster pack. If you do, put CARDNAME into that booster pack.")) {
                playerFlags |= CogworkLibrarianExtraDraft;
            } else if (Iterables.contains(draftActions, "As you draft a card, you may draft an additional card from that booster pack. If you do, turn CARDNAME face down, then pass the next booster pack without drafting a card from it. (You may look at that booster pack.)")) {
                playerFlags |= LeovoldsOperativeExtraDraft;
            } else if (Iterables.contains(draftActions, "Instead of drafting a card from a booster pack, you may draft each card in that booster pack, one at a time. If you do, turn CARDNAME face down and you can't draft cards for the rest of this draft round. (You may look at booster packs passed to you.)")) {
                playerFlags |= AgentAcquisitionsCanDraftAll;
            } else if (Iterables.contains(draftActions, "Each player passes the last card from each booster pack to a player who drafted a card named CARDNAME.")) {
                playerFlags |= CanalDredgerLastPick;
            } else if (Iterables.contains(draftActions, "As long as CARDNAME is face up during the draft, you can't look at booster packs and must draft cards at random. After you draft three cards this way, turn CARDNAME face down. (You may look at cards as you draft them.)")) {
                playerFlags |= ArchdemonOfPalianoCurse;
                archdemonFavors.add(3);
            } else if (Iterables.contains(draftActions, "Immediately after the draft, you may reveal a card in your card pool. Each other player may offer you one card in their card pool in exchange. You may accept any one offer.")) {
                dealBrokers++;
            }
        }

        return true;
    }

    public void addLog(String message) {
        if (this.draft.getDraftLog() != null) {
            this.draft.getDraftLog().addLogEntry(message);
        }
        // Mobile doesnt have a draft log yet
    }

    public DraftPack nextChoice() {
        DraftPack pack = packQueue.peek();
        if (pack != null) {
            adjustPackNumber(pack);
        }

        return pack;
    }

    public void newPack() {
        currentPack = order;
        draftedThisRound = 0;
        packQueue.add(unopenedPacks.poll());
        playerFlags &= ~AgentAcquisitionsSkipDraftRound;
    }

    public void adjustPackNumber(DraftPack pack) {
        currentPack = pack.getId();
    }

    public DraftPack passPack() {
        DraftPack pack = packQueue.poll();
        if (pack != null) {
            pack.setPassedFrom(this);
        }
        return pack;
    }

    public boolean shouldSkipThisPick() {
        boolean skipping = (playerFlags & AgentAcquisitionsSkipDraftRound) == AgentAcquisitionsSkipDraftRound || (playerFlags & LeovoldsOperativeSkipNext) == LeovoldsOperativeSkipNext;

        if (skipping && (playerFlags & LeovoldsOperativeSkipNext) == LeovoldsOperativeSkipNext) {
            playerFlags &= ~LeovoldsOperativeSkipNext;
        }

        return skipping;
    }

    public boolean hasCanalDredger() {
        return (playerFlags & CanalDredgerLastPick) == CanalDredgerLastPick;
    }

    public void receiveUnopenedPack(DraftPack pack) {
        unopenedPacks.add(pack);
    }

    public void receiveOpenedPack(DraftPack pack) {
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

    protected boolean revealWithSmuggler(PaperCard bestPick) {
        return SGuiChoose.one("Reveal this " + bestPick + " for Smuggler Captain?", Lists.newArrayList("Yes", "No")).equals("Yes");
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

    public boolean handleWhispergearSneak() {
        if (Objects.equals(SGuiChoose.oneOrNone("Peek at a booster pack with Whispergear Sneak?", Lists.newArrayList("Yes", "No")), "No")) {
            return false;
        }

        int round = 3;
        if (draft.getRound() != 3) {
            round = SGuiChoose.getInteger("Which round would you like to peek at?", draft.getRound(), 3);
        }

        int playerId = SGuiChoose.getInteger("Which player would you like to peek at?", 0, draft.getOpposingPlayers().length);
        SGuiChoose.reveal("Peeked booster", peekAtBoosterPack(round, playerId));
        // This reveal popup doesn't update the card detail panel in draft
        // How do we get to do that?
        return true;
    }

    protected DraftPack peekAtBoosterPack(int round, int playerNumber) {
        if (draft.getRound() > round) {
            // There aren't any unopened packs from earlier rounds
            return null;
        }

        int relativeRound = round - draft.getRound();
        LimitedPlayer player;
        if (playerNumber == 0) {
            player = this.draft.getHumanPlayer();
        } else {
            player = this.draft.getOpposingPlayers()[playerNumber - 1];
        }
        if (relativeRound == 0) {
            // I want to see a pack from the current round
            return player.packQueue.peek();
        } else {
            return player.unopenedPacks.peek();
        }
    }

    public boolean handleIllusionaryInformant() {
        Integer player = SGuiChoose.getInteger("Peek at another player's last pick?", 0, draft.getOpposingPlayers().length);
        if (Objects.equals(player, null)) {
            return false;
        }

        LimitedPlayer peekAt = draft.getPlayer(player);
        if (peekAt == null) {
            return false;
        }

        SGuiChoose.reveal("Player " + player + " lastPicked: ",  Lists.newArrayList(peekAt.getLastPick()));

        return true;
    }

    public PaperCard handleSpirePhantasm(DraftPack chooseFrom) {
        if (chooseFrom.isEmpty()) {
            return null;
        }

        return SGuiChoose.one("Guess the next card drafted from this pack", chooseFrom);
    }

    public boolean handleLeovoldsOperative(DraftPack pack, PaperCard drafted) {
        if (Objects.equals(SGuiChoose.one("Draft an extra pick with Leovold's Operative?", Lists.newArrayList("Yes", "No")), "No")) {
            return false;
        }

        playerFlags |= LeovoldsOperativeExtraDraft;
        return true;
    }

    public boolean handleCogworkLibrarian(DraftPack pack, PaperCard drafted) {
        return !Objects.equals(SGuiChoose.one("Draft an extra pick with Cogwork Librarian?", Lists.newArrayList("Yes", "No")), "No");
    }

    public boolean handleAgentOfAcquisitions(DraftPack pack, PaperCard drafted) {
        return !Objects.equals(SGuiChoose.one("Draft the rest of the pack with Agent of Acquisitions?", Lists.newArrayList("Yes", "No")), "No");
    }

    public void comparePhantasmGuess(DraftPack pack, PaperCard drafted) {
        LimitedPlayer guesser = pack.getAwaitingGuess().getKey();
        PaperCard guess = pack.getAwaitingGuess().getValue();

        addLog(name() + " reveals " + drafted.getName() + " from " + guesser.name() + "'s guess of " + guess.getName() + " with Spire Phantasm.");
        if (guess.equals(drafted)) {
            addLog(guesser.name() + " correctly guessed " + guess.getName() + " with Spire Phantasm.");
            guesser.getDraftNotes().computeIfAbsent("Spire Phantasm", k -> Lists.newArrayList()).add(guess.getName());
        } else {
            addLog(guesser.name() + " incorrectly guessed " + guess.getName() + " with Spire Phantasm.");
        }

        pack.resetAwaitingGuess();
    }

    public boolean hasArchdemonCurse() {
        return (playerFlags & ArchdemonOfPalianoCurse) == ArchdemonOfPalianoCurse;
    }

    public boolean hasBrokers() {
        return dealBrokers > 0;
    }

    public void reduceArchdemonOfPalianoCurse() {
        if (hasArchdemonCurse()) {
            archdemonFavors.replaceAll(integer -> integer - 1);
            archdemonFavors.removeIf(integer -> integer <= 0);
            if (archdemonFavors.isEmpty()) {
                playerFlags &= ~ArchdemonOfPalianoCurse;
            }
        }
    }

    public PaperCard pickFromArchdemonCurse(DraftPack chooseFrom) {
        Collections.shuffle(chooseFrom);
        reduceArchdemonOfPalianoCurse();
        return chooseFrom.get(0);
    }

    public void addSingleBoosterPack() {
        // if this is just a normal draft, allow picking a pack from any set
        // If this is adventure or quest or whatever then we should limit it to something
        List<CardEdition> possibleEditions = FModel.getMagicDb().getEditions().stream()
                .filter(CardEdition.Predicates.CAN_MAKE_BOOSTER)
                .collect(Collectors.toList());
        CardEdition edition = chooseEdition(possibleEditions);
        if (edition == null) {
            addLog(name() + " chose not to add a booster pack to the draft.");
            return;
        }

        packQueue.add(draft.addBooster(edition));
        addLog(name() + " added " + edition.getName() + " to be drafted this round");
    }

    protected CardEdition chooseEdition(List<CardEdition> possibleEditions) {
        return SGuiChoose.oneOrNone("Choose a booster pack to add to the draft", possibleEditions);
    }

    public void activateBrokers(List<LimitedPlayer> players) {
        while(dealBrokers > 0) {
            dealBrokers--;
            addLog(name() + " activated Deal Broker.");

            PaperCard exchangeCard = chooseExchangeCard(null);
            Map<PaperCard, LimitedPlayer> offers = new HashMap<>();
            for(LimitedPlayer player : players) {
                if (player == this) {
                    continue;
                }

                PaperCard offer = player.chooseExchangeCard(exchangeCard);
                if (offer == null) {
                    continue;
                }

                addLog(player.name() + " offered " + offer.getName() + " to " + name() + " for " + exchangeCard.getName());
                offers.put(offer, player);
            }

            PaperCard exchangeOffer = chooseCardToExchange(exchangeCard, offers);
            if (exchangeOffer == null) {
                addLog(name() + " chose not to accept any offers.");
                continue;
            }
            exchangeAcceptedOffer(exchangeCard, offers.get(exchangeOffer), exchangeOffer);
        }
    }

    protected PaperCard chooseExchangeCard(PaperCard offer) {
        // Choose a card in your deck to trade for offer
        List<PaperCard> deckCards = deck.getOrCreate(DeckSection.Sideboard).toFlatList();

        if (offer == null) {
            return SGuiChoose.oneOrNone("Choose a card to offer for trade: ", deckCards);
        }

        return SGuiChoose.oneOrNone("Choose a card to trade for " + offer.getName() + ": ", deckCards);
    }

    protected PaperCard chooseCardToExchange(PaperCard exchangeCard, Map<PaperCard, LimitedPlayer> offers) {
        return SGuiChoose.oneOrNone("Choose a card to accept trade of " + exchangeCard + ": ", offers.keySet());
    }

    protected void exchangeAcceptedOffer(PaperCard exchangeCard, LimitedPlayer player, PaperCard offer) {
        addLog(name() + " accepted the offer of " + exchangeCard + " for " + offer + " from " + player.name() + ".");

        player.getDeck().removeCardName(offer.getName());
        player.getDeck().get(DeckSection.Sideboard).add(exchangeCard);
        deck.removeCardName(exchangeCard.getName());
        deck.get(DeckSection.Sideboard).add(offer);

        // Exchange noted information
        player.getDraftNotes().getOrDefault(offer.getName(), Lists.newArrayList()).forEach(note -> {
            List<String> noteList = noted.computeIfAbsent(offer.getName(), k -> Lists.newArrayList());
            noteList.add(note);
        });

        this.getDraftNotes().getOrDefault(exchangeCard.getName(), Lists.newArrayList()).forEach(note -> {
            List<String> noteList = player.getDraftNotes().computeIfAbsent(exchangeCard.getName(), k -> Lists.newArrayList());
            noteList.add(note);
        });
    }
}
