package forge.gamemodes.limited;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import forge.card.CardEdition;
import forge.card.MagicColor;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;
import forge.util.Aggregates;
import forge.util.TextUtil;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class LimitedPlayer {
    // A Player class for inside some type of limited environment, like Draft.
    final protected int order;
    protected String name;
    protected int currentPack;
    protected int draftedThisRound;
    protected Deck deck;
    protected PaperCard lastPick;

    protected LinkedList<DraftPack> packQueue;
    protected Queue<DraftPack> unopenedPacks;

    protected List<Integer> archdemonFavors;

    private int notesOwedOnNextPick;
    // Extra-pick sources whose additional card is still owed, oldest first
    private final List<PaperCard> owedExtraPicks = new ArrayList<>();
    private final List<PaperCard> librariansToReturn = new ArrayList<>();
    private boolean draftingWholePack;
    private boolean skipRestOfRound;
    private int packsToSkip;
    private DraftPack lastDraftedFrom;
    private final List<LimitedPlayer> informantWatchers = new ArrayList<>();
    private final List<PaperCard> poolAdded = new ArrayList<>();
    private final List<PaperCard> poolRemoved = new ArrayList<>();
    private boolean stateChanged;

    public record PoolDelta(List<PaperCard> added, List<PaperCard> removed) { }

    /** Receives the prompts of a seat whose answers arrive later, such as a network seat. */
    public interface PromptSink {
        void ask(DraftPrompt prompt, boolean blocking, Consumer<List<Integer>> onAnswer);
    }

    private PromptSink promptSink;
    private int blockingPrompts;
    // Identity, because DraftPack equality compares contents
    private final Set<DraftPack> promptHeldPacks = Collections.newSetFromMap(new IdentityHashMap<>());

    private final List<PaperCard> faceUp = Lists.newArrayList();
    private final Map<String, List<String>> noted = new HashMap<>();
    private final HashSet<String> semicolonDelimiter = Sets.newHashSet("Noble Banneret", "Cogwork Grinder", "Aether Searcher", "Smuggler Captain");

    IBoosterDraft draft;

    /** One constant per distinct Draft: sentence; matching happens only here. */
    protected enum Effect {
        REVEAL("Reveal CARDNAME as you draft it."),
        FACE_UP("Draft CARDNAME face up."),
        NOTE_COUNT("Note how many cards you've drafted this draft round, including CARDNAME."),
        CHOOSE_COLORS("As you draft CARDNAME, the player to your right chooses a color, you choose another color, then the player to your left chooses a third color."),
        SPY("You may look at the next card drafted from this booster pack."),
        NOTE_PASSER("Note the player who passed CARDNAME to you."),
        NOTE_NEXT("Reveal the next card you draft and note its name."),
        GUESS_NEXT("The next time a player drafts a card from this booster pack, guess that card's name. Then that player reveals the drafted card."),
        ADD_BOOSTER("After you draft CARDNAME, you may add a booster pack to the draft. (Your next pick is from that booster pack. Pass it to the next player and it's drafted this draft round.)"),
        REMOVE_FACE_UP("As you draft a card, you may remove it from the draft face up. (It isn't in your card pool.)"),
        REMOVE_FACE_DOWN("As you draft a card, you may remove it from the draft face down. (Those cards aren't in your card pool.)"),
        NOTE_CREATURE_NAME("As you draft a creature card, you may reveal it, note its name, then turn CARDNAME face down."),
        NOTE_CARD_NAME("As you draft a card, you may reveal it, note its name, then turn CARDNAME face down."),
        NOTE_CREATURE_TYPES("As you draft a creature card, you may reveal it, note its creature types, then turn CARDNAME face down."),
        PEEK_PACK("During the draft, you may turn CARDNAME face down. If you do, look at any unopened booster pack in the draft or any booster pack not being looked at by another player."),
        PEEK_NEXT_PICK("During the draft, you may turn CARDNAME face down. If you do, look at the next card drafted by a player of your choice."),
        EXTRA_PICK_RETURN("As you draft a card, you may draft an additional card from that booster pack. If you do, put CARDNAME into that booster pack."),
        EXTRA_PICK_SKIP("As you draft a card, you may draft an additional card from that booster pack. If you do, turn CARDNAME face down, then pass the next booster pack without drafting a card from it. (You may look at that booster pack.)"),
        DRAFT_WHOLE_PACK("Instead of drafting a card from a booster pack, you may draft each card in that booster pack, one at a time. If you do, turn CARDNAME face down and you can't draft cards for the rest of this draft round. (You may look at booster packs passed to you.)"),
        LAST_CARD("Each player passes the last card from each booster pack to a player who drafted a card named CARDNAME."),
        RANDOM_PICKS("As long as CARDNAME is face up during the draft, you can't look at booster packs and must draft cards at random. After you draft three cards this way, turn CARDNAME face down. (You may look at cards as you draft them.)"),
        POST_DRAFT_TRADE("Immediately after the draft, you may reveal a card in your card pool. Each other player may offer you one card in their card pool in exchange. You may accept any one offer.");

        private static final Map<String, Effect> BY_SENTENCE =
                Arrays.stream(values()).collect(Collectors.toMap(e -> e.sentence, e -> e));

        private final String sentence;

        Effect(String sentence) {
            this.sentence = sentence;
        }

        static EnumSet<Effect> of(PaperCard card) {
            EnumSet<Effect> result = EnumSet.noneOf(Effect.class);
            Iterable<String> lines = card.getRules().getMainPart().getDraftActions();
            if (lines != null) {
                for (String line : lines) {
                    Effect e = BY_SENTENCE.get(line);
                    if (e != null) {
                        result.add(e);
                    }
                }
            }
            return result;
        }
    }

    private static final EnumSet<Effect> ABILITIES = EnumSet.of(Effect.NOTE_CREATURE_NAME,
            Effect.NOTE_CARD_NAME, Effect.NOTE_CREATURE_TYPES, Effect.REMOVE_FACE_UP, Effect.REMOVE_FACE_DOWN,
            Effect.EXTRA_PICK_RETURN, Effect.EXTRA_PICK_SKIP, Effect.DRAFT_WHOLE_PACK, Effect.PEEK_PACK, Effect.PEEK_NEXT_PICK);
    private static final Set<String> ANIMUS_KEYWORDS = Set.of("Flying", "First Strike", "Double Strike",
            "Deathtouch", "Haste", "Indestructible", "Lifelink", "Menace", "Reach", "Vigilance");

    /** The pick variant or pool action a face-up source offers, or null. */
    protected static Effect abilityEffect(PaperCard source) {
        return Effect.of(source).stream().filter(ABILITIES::contains).findFirst().orElse(null);
    }

    public LimitedPlayer(int seatingOrder, IBoosterDraft draft) {
        order = seatingOrder;
        deck = new Deck();

        packQueue = new LinkedList<>();
        unopenedPacks = new LinkedList<>();
        archdemonFavors = new ArrayList<>();
        this.draft = draft;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        if(this.name == null)
            return "Player " + (this.order + 1);
        return name;
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

    /** Number of packs waiting in this player's queue. */
    public int getPackQueueSize() {
        return packQueue.size();
    }

    public List<PaperCard> getFaceUp() {
        return Collections.unmodifiableList(faceUp);
    }

    public List<PaperCard> getPoolCards() {
        return deck.getOrCreate(DeckSection.Sideboard).toFlatList();
    }

    public boolean hasStateChanged() {
        return stateChanged;
    }

    public PoolDelta drainPoolDelta() {
        PoolDelta delta = new PoolDelta(List.copyOf(poolAdded), List.copyOf(poolRemoved));
        poolAdded.clear();
        poolRemoved.clear();
        stateChanged = false;
        return delta;
    }

    public boolean isPackHidden() {
        return !archdemonFavors.isEmpty();
    }

    public boolean shouldSkipThisPick() {
        return skipRestOfRound || packsToSkip > 0;
    }

    public boolean holdsPack(DraftPack pack) {
        return !owedExtraPicks.isEmpty() || (draftingWholePack && !pack.isEmpty());
    }

    private boolean passesPack(DraftPack pack) {
        return !holdsPack(pack) && !isPromptHeld(pack);
    }

    public int brokerUses() {
        return (int) faceUp.stream().filter(c -> Effect.of(c).contains(Effect.POST_DRAFT_TRADE)).count();
    }

    protected boolean hasFaceUp(Effect effect) {
        return faceUpCopyWith(effect) != null;
    }

    private PaperCard faceUpCopy(PaperCard source) {
        return faceUp.stream().filter(c -> c.getName().equals(source.getName())).findFirst().orElse(null);
    }

    private PaperCard faceUpCopyWith(Effect effect) {
        return faceUp.stream().filter(c -> Effect.of(c).contains(effect)).findFirst().orElse(null);
    }

    private void turnFaceDown(PaperCard copy) {
        if (copy != null && faceUp.remove(copy)) {
            stateChanged = true;
            addLog(name() + " turned " + copy.getDisplayName() + " face down.", copy);
        }
    }

    private void note(String key, String value) {
        noted.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
    }

    public List<DraftAction> getActions(DraftPack head) {
        List<DraftAction> result = new ArrayList<>();
        if (head != null && !head.isEmpty() && !shouldSkipThisPick() && !isPackHidden()) {
            for (PaperCard target : new LinkedHashSet<>(head)) {
                List<DraftAction> offers = pickOffers(head, target);
                result.addAll(offers);
                if (offersCombination(offers)) {
                    result.add(DraftAction.choose(target));
                }
            }
        }
        Set<String> seen = new HashSet<>();
        for (PaperCard source : faceUp) {
            if (!seen.add(source.getName())) {
                continue;
            }
            Effect effect = abilityEffect(source);
            if ((effect == Effect.PEEK_PACK && !hasFaceUp(Effect.RANDOM_PICKS))
                    || (effect == Effect.PEEK_NEXT_PICK && draft.getAllPlayers().size() > 1)) {
                result.add(DraftAction.pool(source));
            }
        }
        return result;
    }

    /** The pick variants for one card, offered while the pack still holds it. */
    private List<DraftAction> pickOffers(DraftPack head, PaperCard target) {
        List<DraftAction> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (PaperCard source : faceUp) {
            Effect effect = abilityEffect(source);
            if (effect == null || !seen.add(source.getName())) {
                continue;
            }
            boolean offered = switch (effect) {
                case NOTE_CREATURE_NAME, NOTE_CREATURE_TYPES -> target.getRules().getType().isCreature();
                case NOTE_CARD_NAME, REMOVE_FACE_UP, REMOVE_FACE_DOWN -> true;
                // After this pick the pack must hold a card for every additional card owed, this one included
                case EXTRA_PICK_RETURN, EXTRA_PICK_SKIP -> !draftingWholePack && isUnspent(source)
                        && head.size() - 1 > Math.max(0, owedExtraPicks.size() - 1);
                case DRAFT_WHOLE_PACK -> owedExtraPicks.isEmpty() && !draftingWholePack;
                default -> false;
            };
            if (offered) {
                result.add(DraftAction.pick(source, target));
            }
        }
        return result;
    }

    // A Librarian or Operative stays face up until its additional card is drafted, but can be used only once
    private boolean isUnspent(PaperCard source) {
        String name = source.getName();
        return countNamed(faceUp, name) > countNamed(owedExtraPicks, name) + countNamed(librariansToReturn, name);
    }

    private static long countNamed(List<PaperCard> cards, String name) {
        return cards.stream().filter(c -> c.getName().equals(name)).count();
    }

    /** A card is removed at most once and takes one extra-pick or whole-pack ability; a chained pick can use another. */
    protected static boolean canCombine(List<DraftAction> variants) {
        Set<String> sources = new HashSet<>();
        int removals = 0;
        int packHolders = 0;
        for (DraftAction variant : variants) {
            if (!sources.add(variant.source().getName())) {
                return false;
            }
            switch (abilityEffect(variant.source())) {
                case REMOVE_FACE_UP, REMOVE_FACE_DOWN -> removals++;
                case EXTRA_PICK_RETURN, EXTRA_PICK_SKIP, DRAFT_WHOLE_PACK -> packHolders++;
                default -> { }
            }
        }
        return removals <= 1 && packHolders <= 1;
    }

    private static boolean offersCombination(List<DraftAction> offers) {
        for (int i = 0; i < offers.size(); i++) {
            for (int j = i + 1; j < offers.size(); j++) {
                if (canCombine(List.of(offers.get(i), offers.get(j)))) {
                    return true;
                }
            }
        }
        return false;
    }

    public Boolean draftCard(PaperCard pick, DeckSection section, DraftAction variant) {
        DraftPack pack = packQueue.peek();
        if (pack == null || pack.isEmpty()) {
            return null;
        }
        // Archdemon lets the player look at the random card as they draft it, so its abilities still apply
        if (isPackHidden()) {
            return draftChoosingAbilities(pack, drawRandom(pack), section, true);
        }
        if (pick == null || !pack.contains(pick) || (variant != null && !variant.isPickFor(pick))) {
            return null;
        }
        if (variant != null && variant.kind() == DraftAction.Kind.CHOOSE) {
            return draftChoosingAbilities(pack, pick, section, false);
        }
        return completePick(pack, pick, section, variant == null ? List.of() : List.of(variant));
    }

    private boolean draftChoosingAbilities(DraftPack pack, PaperCard pick, DeckSection section, boolean random) {
        List<DraftAction> offers = pickOffers(pack, pick);
        // Agent of Acquisitions replaces the draft, so it cannot be chosen once the random card is seen
        if (random) {
            offers.removeIf(o -> abilityEffect(o.source()) == Effect.DRAFT_WHOLE_PACK);
        }
        if (offers.isEmpty()) {
            return completePick(pack, pick, section, List.of());
        }
        chooseAbilities(pick, offers, random, pack, chosen -> completePick(pack, pick, section, chosen));
        return passesPack(pack);
    }

    private boolean completePick(DraftPack pack, PaperCard pick, DeckSection section, List<DraftAction> variants) {
        debugPrint("Picked: " + pick);
        boolean owesExtra = !owedExtraPicks.isEmpty();

        pack.remove(pick);
        lastPick = pick;
        lastDraftedFrom = pack;
        draftedThisRound++;
        boolean revealed = notifyWatchers(pack, pick);

        PaperCard remover = null;
        for (DraftAction variant : variants) {
            PaperCard source = faceUpCopy(variant.source());
            Effect effect = source == null ? null : abilityEffect(source);
            if (effect == null) {
                continue;
            }
            switch (effect) {
                case NOTE_CREATURE_NAME, NOTE_CARD_NAME -> {
                    note(source.getName(), pick.getName());
                    addLog(name() + " revealed " + pick.getDisplayName() + " and noted its name for " + source.getName() + ".", pick);
                    revealed = true;
                    turnFaceDown(source);
                }
                case NOTE_CREATURE_TYPES -> {
                    Set<String> types = pick.getRules().getType().getCreatureTypes();
                    for (String type : types) {
                        note(source.getName(), type);
                    }
                    addLog(name() + " revealed " + pick.getDisplayName() + " and noted " + TextUtil.join(types, ",")
                            + " for " + source.getName() + ".", pick);
                    revealed = true;
                    turnFaceDown(source);
                }
                case REMOVE_FACE_UP, REMOVE_FACE_DOWN -> {
                    remover = source;
                    recordRemoval(pick, source, effect);
                }
                case EXTRA_PICK_RETURN, EXTRA_PICK_SKIP -> owedExtraPicks.add(source);
                case DRAFT_WHOLE_PACK -> {
                    turnFaceDown(source);
                    draftingWholePack = true;
                    addLog(name() + " is drafting the rest of the pack with " + source.getName() + ".", source);
                }
                default -> { }
            }
        }

        boolean removed = remover != null;
        if (!removed) {
            deck.getOrCreate(section).add(pick);
            poolAdded.add(pick);
            stateChanged = true;
        }

        EnumSet<Effect> own = Effect.of(pick);
        applyOwnEffects(pick, pack, own, removed, revealed);
        if (removed) {
            logRemoval(pick, remover, abilityEffect(remover), own, revealed);
        }

        if (owesExtra) {
            resolveOwedExtra(owedExtraPicks.remove(0));
        }
        if (owedExtraPicks.isEmpty()) {
            returnLibrarians(pack);
        }
        if (draftingWholePack && pack.isEmpty()) {
            draftingWholePack = false;
            skipRestOfRound = true;
        }
        // After the Librarians return, so a returned Librarian counts as a card that can be guessed
        if (own.contains(Effect.GUESS_NEXT) && !pack.isEmpty()) {
            guessCard(pack, pick, guess -> {
                pack.setAwaitingGuess(this, guess);
                addLog(name() + " made a guess for Spire Phantasm.", pick);
            });
        }
        return passesPack(pack);
    }

    /** Tells earlier watchers about this pick; returns whether that revealed it publicly. */
    private boolean notifyWatchers(DraftPack pack, PaperCard pick) {
        boolean revealed = false;
        LimitedPlayer spy = pack.getSpyWatcher();
        if (spy != null) {
            draft.addPrivateLog(spy, name() + " drafted " + pick.getDisplayName()
                    + " from the booster pack you watched with Cogwork Spy.", pick);
            pack.setSpyWatcher(null);
        }
        for (LimitedPlayer watcher : informantWatchers) {
            draft.addPrivateLog(watcher, name() + " drafted " + pick.getDisplayName() + " (Illusionary Informant).", pick);
        }
        informantWatchers.clear();
        if (pack.getAwaitingGuess() != null) {
            comparePhantasmGuess(pack, pick);
            revealed = true;
        }
        if (notesOwedOnNextPick > 0) {
            notesOwedOnNextPick--;
            note("Aether Searcher", pick.getName());
            addLog(name() + " revealed " + pick.getDisplayName() + " for Aether Searcher.", pick);
            revealed = true;
        }
        return revealed;
    }

    private void applyOwnEffects(PaperCard pick, DraftPack pack, EnumSet<Effect> own, boolean removed, boolean revealed) {
        if (own.contains(Effect.REVEAL) && !revealed) {
            addLog(name() + " revealed " + pick.getDisplayName() + " as they drafted it.", pick);
        }
        if (own.contains(Effect.FACE_UP) && !removed) {
            faceUp.add(pick);
            stateChanged = true;
            if (own.contains(Effect.RANDOM_PICKS)) {
                archdemonFavors.add(3);
            }
            addLog(name() + " drafted " + pick.getDisplayName() + " face up.", pick);
        }
        if (own.contains(Effect.NOTE_COUNT)) {
            note(pick.getName(), String.valueOf(draftedThisRound));
            addLog(name() + " noted " + draftedThisRound + " cards drafted this round for " + pick.getDisplayName() + ".", pick);
        }
        if (own.contains(Effect.NOTE_PASSER) && pack.getPassedFrom() != null) {
            note(pick.getName(), String.valueOf(pack.getPassedFrom().order));
            addLog(name() + " noted that " + pack.getPassedFrom().name() + " passed " + pick.getDisplayName() + ".", pick);
        }
        if (own.contains(Effect.SPY)) {
            pack.setSpyWatcher(this);
        }
        if (own.contains(Effect.NOTE_NEXT)) {
            notesOwedOnNextPick++;
        }
        if (own.contains(Effect.CHOOSE_COLORS)) {
            chooseNoteColors(pick);
        }
        if (own.contains(Effect.ADD_BOOSTER)) {
            offerExtraBooster(pack, pick);
        }
    }

    private void logRemoval(PaperCard pick, PaperCard source, Effect effect, EnumSet<Effect> own, boolean revealed) {
        boolean isPublic = effect == Effect.REMOVE_FACE_UP || revealed || own.contains(Effect.REVEAL) || own.contains(Effect.FACE_UP);
        if (isPublic) {
            addLog(name() + " removed " + pick.getDisplayName() + " from the draft with " + source.getName() + ".", pick);
        } else {
            addLog(name() + " removed a card face down from the draft with " + source.getName() + ".");
            draft.addPrivateLog(this, "You removed " + pick.getDisplayName() + " from the draft with " + source.getName() + ".", pick);
        }
    }

    private void recordRemoval(PaperCard pick, PaperCard source, Effect effect) {
        if (effect == Effect.REMOVE_FACE_DOWN) {
            note(source.getName(), pick.getName());
        } else if (pick.getRules().getType().isCreature()) {
            note(source.getName(), String.join(",", animusKeywords(pick)));
        }
    }

    private static List<String> animusKeywords(PaperCard pick) {
        List<String> keywords = new ArrayList<>();
        for (String keyword : pick.getRules().getMainPart().getKeywords()) {
            if (keyword.startsWith("Hexproof") || ANIMUS_KEYWORDS.contains(keyword)) {
                keywords.add(keyword);
            }
        }
        return keywords;
    }

    private void resolveOwedExtra(PaperCard source) {
        if (Effect.of(source).contains(Effect.EXTRA_PICK_RETURN)) {
            librariansToReturn.add(source);
        } else {
            // By name, because a chained use can record one printing for two different copies
            turnFaceDown(faceUpCopy(source));
            packsToSkip++;
            addLog(name() + " will pass their next booster pack with " + source.getDisplayName() + ".", source);
        }
    }

    // As in the ruling's two-Librarian example, every Librarian goes into the pack after the last additional card
    private void returnLibrarians(DraftPack pack) {
        for (PaperCard source : librariansToReturn) {
            faceUp.remove(faceUpCopy(source));
            PaperCard returned = deck.removeCardName(source.getName());
            if (returned != null) {
                pack.add(returned);
                poolRemoved.add(returned);
            }
            stateChanged = true;
            addLog(name() + " returned " + source.getDisplayName() + " to the pack.", source);
        }
        librariansToReturn.clear();
    }

    private PaperCard drawRandom(DraftPack pack) {
        PaperCard drawn = Aggregates.random(pack);
        for (int i = archdemonFavors.size() - 1; i >= 0; i--) {
            int left = archdemonFavors.get(i) - 1;
            if (left > 0) {
                archdemonFavors.set(i, left);
            } else {
                archdemonFavors.remove(i);
                turnFaceDown(faceUpCopyWith(Effect.RANDOM_PICKS));
            }
        }
        return drawn;
    }

    public void addLog(String message) {
        addLog(message, null);
    }

    // Only a card the message already names, so its image reveals nothing more
    private void addLog(String message, PaperCard card) {
        draft.addLog(message, card);
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
        skipRestOfRound = false;
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

    public void activate(DraftAction action) {
        PaperCard source = action.kind() == DraftAction.Kind.POOL ? faceUpCopy(action.source()) : null;
        Effect effect = source == null ? null : abilityEffect(source);
        if (effect == Effect.PEEK_PACK) {
            peekPack(source);
        } else if (effect == Effect.PEEK_NEXT_PICK) {
            peekNextPick(source);
        }
    }

    private void peekPack(PaperCard source) {
        List<DraftPack> eligible = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        collectPeekablePacks(eligible, labels);
        if (eligible.isEmpty()) {
            draft.addPrivateLog(this, "No booster pack is available to look at with " + source.getName() + ".", null);
            return;
        }
        turnFaceDown(source);
        choosePeekPack(eligible, labels, source, chosen -> showPeekedPack(source, chosen));
    }

    // A network answer arrives later, when the chosen pack may have reached a player who is looking at it
    private void showPeekedPack(PaperCard source, DraftPack chosen) {
        List<DraftPack> eligible = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        collectPeekablePacks(eligible, labels);
        if (eligible.stream().anyMatch(p -> p == chosen)) {
            ask(DraftPrompt.info(order, "Booster pack seen with " + source.getName(), chosen), false, null, answer -> { });
        } else if (eligible.isEmpty()) {
            draft.addPrivateLog(this, "No booster pack is available to look at with " + source.getName() + ".", null);
        } else {
            choosePeekPack(eligible, labels, source, again -> showPeekedPack(source, again));
        }
    }

    private void collectPeekablePacks(List<DraftPack> eligible, List<String> labels) {
        for (LimitedPlayer p : draft.getAllPlayers()) {
            int n = 0;
            for (DraftPack unopened : p.unopenedPacks) {
                eligible.add(unopened);
                labels.add(p.getName() + ": unopened pack " + (++n));
            }
            for (int i = 0; i < p.packQueue.size(); i++) {
                DraftPack queued = p.packQueue.get(i);
                // A head pack is in use, except an AI's between its pick and its pass
                boolean between = i == 0 && p instanceof LimitedPlayerAI && queued == p.lastDraftedFrom;
                if (!queued.isEmpty() && (i > 0 || between)) {
                    eligible.add(queued);
                    labels.add(p.getName() + ": waiting pack (" + queued.size() + " cards)");
                }
            }
        }
    }

    private void peekNextPick(PaperCard source) {
        List<LimitedPlayer> others = draft.getAllPlayers().stream().filter(p -> p != this).collect(Collectors.toList());
        turnFaceDown(source);
        choosePeekSeat(others, source, target -> {
            target.informantWatchers.add(this);
            draft.addPrivateLog(this, "You will see the next card " + target.getName() + " drafts.", null);
        });
    }

    /** Uses up one skip after the skipped pack has been routed onward. */
    public void consumeSkip(DraftPack skipped) {
        if (packsToSkip > 0) {
            packsToSkip--;
        }
        if (skipped != null && !skipped.isEmpty() && !isPackHidden()) {
            draft.addPrivateLog(this, "You passed a booster pack without drafting: "
                    + skipped.stream().map(PaperCard::getDisplayName).collect(Collectors.joining(", ")), null);
        }
    }

    public void receiveUnopenedPack(DraftPack pack) {
        unopenedPacks.add(pack);
    }

    public void receiveOpenedPack(DraftPack pack) {
        packQueue.add(pack);
    }

    public void setPromptSink(PromptSink sink) {
        this.promptSink = sink;
    }

    public boolean isBlocked() {
        return blockingPrompts > 0;
    }

    public boolean isPromptHeld(DraftPack pack) {
        return promptHeldPacks.contains(pack);
    }

    public void clearPromptHold(DraftPack pack) {
        promptHeldPacks.remove(pack);
    }

    /**
     * Ask this seat a question. A blocking prompt stops the seat picking until answered;
     * {@code heldPack} stays in the seat's queue if the answer does not arrive at once.
     */
    protected void ask(DraftPrompt prompt, boolean blocking, DraftPack heldPack, Consumer<List<Integer>> onAnswer) {
        // A mandatory prompt with one option needs no answer
        if (prompt.min() == 1 && prompt.options().size() == 1) {
            onAnswer.accept(List.of(0));
            return;
        }
        if (blocking) {
            blockingPrompts++;
        }
        boolean[] answered = {false};
        Consumer<List<Integer>> wrapped = answer -> {
            answered[0] = true;
            if (blocking) {
                blockingPrompts--;
            }
            onAnswer.accept(answer);
        };
        if (promptSink != null) {
            promptSink.ask(prompt, blocking, wrapped);
        } else {
            wrapped.accept(prompt.answerLocally());
        }
        if (!answered[0] && heldPack != null) {
            promptHeldPacks.add(heldPack);
        }
    }

    protected void chooseColor(List<String> colors, LimitedPlayer drafter, String title, Consumer<String> then) {
        ask(DraftPrompt.text(order, drafter.name() + " drafted " + title + ": choose a color", colors, false),
                false, null, answer -> then.accept(colors.get(answer.get(0))));
    }

    protected void guessCard(DraftPack pack, PaperCard source, Consumer<PaperCard> then) {
        List<PaperCard> options = guessOptions(pack, source);
        if (options.isEmpty()) {
            return;
        }
        ask(DraftPrompt.cards(order, source.getName() + ": guess the next card drafted from this pack", options, false),
                true, pack, answer -> then.accept(options.get(answer.get(0))));
    }

    /** The pack's cards, or every card in the source's set while an Archdemon hides the pack. */
    protected List<PaperCard> guessOptions(DraftPack pack, PaperCard source) {
        if (!isPackHidden()) {
            return new ArrayList<>(new LinkedHashSet<>(pack));
        }
        CardEdition edition = FModel.getMagicDb().getEditions().get(source.getEdition());
        if (edition == null) {
            return List.of();
        }
        Map<String, PaperCard> byName = new TreeMap<>();
        for (PaperCard card : FModel.getMagicDb().getCommonCards().getAllCards(edition)) {
            byName.putIfAbsent(card.getName(), card);
        }
        return new ArrayList<>(byName.values());
    }

    protected void chooseAbilities(PaperCard pick, List<DraftAction> offers, boolean random, DraftPack pack,
                                   Consumer<List<DraftAction>> then) {
        List<PaperCard> sources = offers.stream().map(DraftAction::source).collect(Collectors.toList());
        String message = (random ? "You drafted " + pick.getDisplayName() + " at random. " : "")
                + "Choose the abilities to use as you draft " + pick.getDisplayName();
        ask(DraftPrompt.anyCards(order, message, sources), true, pack, answer -> {
            List<DraftAction> chosen = answer.stream().map(offers::get).collect(Collectors.toList());
            if (canCombine(chosen)) {
                then.accept(chosen);
                return;
            }
            draft.addPrivateLog(this, "A card can be removed only once, and only one of Cogwork Librarian,"
                    + " Leovold's Operative or Agent of Acquisitions applies to it.", null);
            chooseAbilities(pick, offers, random, pack, then);
        });
    }

    protected void chooseEdition(List<CardEdition> editions, PaperCard source, Consumer<CardEdition> then) {
        List<String> names = editions.stream().map(CardEdition::getName).collect(Collectors.toList());
        ask(DraftPrompt.text(order, source.getName() + ": choose a booster pack to add to the draft", names, true),
                true, null, answer -> then.accept(answer.isEmpty() ? null : editions.get(answer.get(0))));
    }

    protected void chooseDredgerSeat(List<LimitedPlayer> eligible, DraftPack pack, Consumer<LimitedPlayer> then) {
        List<String> names = eligible.stream().map(LimitedPlayer::getName).collect(Collectors.toList());
        ask(DraftPrompt.text(order, "Which player with Canal Dredger receives the last card?", names, false),
                true, pack, answer -> then.accept(eligible.get(answer.get(0))));
    }

    protected void choosePeekPack(List<DraftPack> packs, List<String> labels, PaperCard source, Consumer<DraftPack> then) {
        ask(DraftPrompt.text(order, source.getName() + ": choose a booster pack to look at", labels, false),
                false, null, answer -> then.accept(packs.get(answer.get(0))));
    }

    protected void choosePeekSeat(List<LimitedPlayer> others, PaperCard source, Consumer<LimitedPlayer> then) {
        List<String> names = others.stream().map(LimitedPlayer::getName).collect(Collectors.toList());
        ask(DraftPrompt.text(order, source.getName() + ": look at the next card drafted by which player?", names, false),
                false, null, answer -> then.accept(others.get(answer.get(0))));
    }

    protected void chooseExchangeCard(PaperCard offer, Consumer<PaperCard> then) {
        List<PaperCard> pool = deck.getOrCreate(DeckSection.Sideboard).toFlatList();
        String message = offer == null ? "Choose a card to reveal for Deal Broker"
                : "Choose a card to offer for " + offer.getDisplayName();
        ask(DraftPrompt.cards(order, message, pool, true), false, null,
                answer -> then.accept(answer.isEmpty() ? null : pool.get(answer.get(0))));
    }

    protected void chooseCardToExchange(PaperCard exchangeCard, List<Pair<PaperCard, LimitedPlayer>> offers,
                                        Consumer<Pair<PaperCard, LimitedPlayer>> then) {
        List<PaperCard> options = offers.stream().map(Pair::getKey).collect(Collectors.toList());
        ask(DraftPrompt.cards(order, "Choose an offer to accept for " + exchangeCard.getDisplayName(), options, true),
                false, null, answer -> then.accept(answer.isEmpty() ? null : offers.get(answer.get(0))));
    }

    /**
     * @return Name to insert into draft messages.
     */
    protected String name() {
        if (name != null || this instanceof LimitedPlayerAI) {
            return getName();
        }

        return "You";
    }

    public void comparePhantasmGuess(DraftPack pack, PaperCard drafted) {
        LimitedPlayer guesser = pack.getAwaitingGuess().getKey();
        PaperCard guess = pack.getAwaitingGuess().getValue();

        addLog(name() + " reveals " + drafted.getDisplayName() + " from " + guesser.name() + "'s guess of " + guess.getDisplayName() + " with Spire Phantasm.", drafted);
        if (guess.getName().equals(drafted.getName())) {
            addLog(guesser.name() + " correctly guessed " + guess.getDisplayName() + " with Spire Phantasm.", guess);
            guesser.getDraftNotes().computeIfAbsent("Spire Phantasm", k -> Lists.newArrayList()).add(guess.getName());
        } else {
            addLog(guesser.name() + " incorrectly guessed " + guess.getDisplayName() + " with Spire Phantasm.", guess);
        }

        pack.resetAwaitingGuess();
    }

    private void chooseNoteColors(PaperCard pick) {
        LimitedPlayer right = draft.getNeighbor(this, false);
        LimitedPlayer left = draft.getNeighbor(this, true);
        List<String> available = new ArrayList<>(MagicColor.Constant.ONLY_COLORS);
        List<String> chosen = new ArrayList<>();
        right.chooseColor(List.copyOf(available), this, pick.getName(), first -> {
            chosen.add(first);
            available.remove(first);
            chooseColor(List.copyOf(available), this, pick.getName(), second -> {
                chosen.add(second);
                available.remove(second);
                left.chooseColor(List.copyOf(available), this, pick.getName(), third -> {
                    chosen.add(third);
                    String colors = String.join(",", chosen);
                    note(pick.getName(), colors);
                    addLog(name() + " revealed " + pick.getDisplayName() + " and noted " + colors + " chosen colors.", pick);
                });
            });
        });
    }

    private void offerExtraBooster(DraftPack sourcePack, PaperCard source) {
        List<CardEdition> editions = FModel.getMagicDb().getEditions().stream()
                .filter(CardEdition.Predicates.CAN_MAKE_BOOSTER)
                .collect(Collectors.toList());
        chooseEdition(editions, source, edition -> {
            if (edition == null) {
                addLog(name() + " chose not to add a booster pack to the draft.");
                return;
            }
            // The new pack is the next pick: behind the Lore Seeker pack if that is still here, otherwise first
            packQueue.add(packQueue.peek() == sourcePack ? 1 : 0, draft.addBooster(edition));
            addLog(name() + " added " + edition.getName() + " to be drafted this round.");
        });
    }

    public void activateBrokers(List<LimitedPlayer> players, int remaining, Runnable done) {
        if (remaining <= 0) {
            done.run();
            return;
        }
        addLog(name() + " activated Deal Broker.");
        chooseExchangeCard(null, exchangeCard -> {
            if (exchangeCard == null) {
                addLog(name() + " chose not to reveal a card.");
                activateBrokers(players, remaining - 1, done);
                return;
            }
            addLog(name() + " revealed " + exchangeCard.getDisplayName() + " for Deal Broker.", exchangeCard);
            List<LimitedPlayer> others = players.stream().filter(p -> p != this).collect(Collectors.toList());
            List<Pair<PaperCard, LimitedPlayer>> offers = new ArrayList<>();
            int[] waiting = {others.size()};
            // Offers are revealed together, so nothing is logged until every seat has answered
            Runnable afterOffers = () -> {
                offers.forEach(o -> addLog(o.getValue().name() + " offered " + o.getKey().getDisplayName()
                        + " for " + exchangeCard.getDisplayName() + ".", o.getKey()));
                chooseCardToExchange(exchangeCard, offers, accepted -> {
                    if (accepted == null) {
                        addLog(name() + " chose not to accept any offers.");
                    } else {
                        exchangeAcceptedOffer(exchangeCard, accepted.getValue(), accepted.getKey());
                    }
                    activateBrokers(players, remaining - 1, done);
                });
            };
            if (others.isEmpty()) {
                afterOffers.run();
                return;
            }
            for (LimitedPlayer other : others) {
                other.chooseExchangeCard(exchangeCard, offer -> {
                    if (offer != null) {
                        offers.add(Pair.of(offer, other));
                    }
                    if (--waiting[0] == 0) {
                        afterOffers.run();
                    }
                });
            }
        });
    }

    protected void exchangeAcceptedOffer(PaperCard exchangeCard, LimitedPlayer player, PaperCard offer) {
        addLog(name() + " accepted the offer of " + exchangeCard + " for " + offer + " from " + player.name() + ".", offer);

        player.swapPoolCard(offer, exchangeCard);
        swapPoolCard(exchangeCard, offer);

        // Exchange noted information
        player.getDraftNotes().getOrDefault(offer.getName(), Lists.newArrayList()).forEach(note -> {
            List<String> noteList = noted.computeIfAbsent(offer.getName(), k -> Lists.newArrayList());
            noteList.add(note);
        });

        this.getDraftNotes().getOrDefault(exchangeCard.getName(), Lists.newArrayList()).forEach(note -> {
            List<String> noteList = player.getDraftNotes().computeIfAbsent(exchangeCard.getName(), k -> Lists.newArrayList());
            noteList.add(note);
        });

        // Notes stay with the exchanged card, so a player who gave away their last copy loses them
        if (player.getDeck().countByName(offer.getName()) == 0) {
            player.getDraftNotes().remove(offer.getName());
        }
        if (deck.countByName(exchangeCard.getName()) == 0) {
            noted.remove(exchangeCard.getName());
        }
    }

    private void swapPoolCard(PaperCard given, PaperCard received) {
        PaperCard removed = deck.removeCardName(given.getName());
        deck.get(DeckSection.Sideboard).add(received);
        if (removed != null) {
            poolRemoved.add(removed);
        }
        poolAdded.add(received);
        stateChanged = true;
    }

    public void debugPrint(String text) {
        if(!ForgePreferences.DEV_MODE)
            return;
        System.out.println("Player[" + order + "] - " + text);
    }
}
