package forge.web.deck;

import forge.StaticData;
import forge.card.CardEdition;
import forge.card.CardRules;
import forge.card.MagicColor;
import forge.card.mana.ManaCostShard;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckFormat;
import forge.deck.DeckGroup;
import forge.deck.DeckProxy;
import forge.deck.DeckSection;
import forge.deck.DeckgenUtil;
import forge.game.GameType;
import forge.gamemodes.limited.CommanderDraftUtil;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.util.Aggregates;
import forge.util.ImageUtil;
import forge.util.Localizer;
import forge.util.storage.IStorage;
import forge.web.protocol.ToBrowser.EditorLand;
import forge.web.protocol.ToBrowser.EditorSection;
import forge.web.protocol.ToBrowser.EditorState;
import forge.web.protocol.ToBrowser.LandSet;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** One deck open in the browser's editor. Every change is checked against the engine's limits, applied and saved at once. */
public final class DeckEditor {
    private static final int MOST_UNDO = 100;
    /** The fewest cards a planar deck may have, as DeckFormat checks it. */
    private static final int FEWEST_PLANES = 10;
    /** The message key for the reply to renaming, duplicating or deleting a pool's deck. */
    private static final String GROUP_OWNS_IT = "lblWebEditorPoolOwnsDeck";
    /** The message key for the reply to making a pool's cards foil. */
    private static final String DECK_FIXED = "lblWebCollectionDeckFixed";
    /** The basics the land row offers, with the colour each makes. */
    private static final Map<String, MagicColor.Color> BASICS = new LinkedHashMap<>();
    private final DeckStore.Storages storages;
    private final boolean guest;
    private final DeviceSink sink;
    private String freeCommander;
    private final Deque<Snapshot> undo = new ArrayDeque<>();
    private Deck deck;
    private Target target;
    private LegalityScope check;
    /** The name of the deck that this editor cannot change in place, until the first change copies it. */
    private String copyOf;
    /** The name this editor has saved the deck under, or null before its first save. */
    private String owned;
    /** Whether the deck exists where this editor saves it: it was opened from there, or saved there since. */
    private boolean saved;
    private String landed;
    /** The edition new basic lands come from. */
    private String landSet;
    /** False for a new deck until its commander or the player names it. */
    private boolean named = true;

    /** The name a new deck starts with, until its commander or the player names it. */
    static String newDeckName() {
        return Localizer.getInstance().getMessage("lblNewDeck");
    }

    static {
        for (final MagicColor.Color colour : MagicColor.Color.values()) {
            BASICS.put(colour == MagicColor.Color.COLORLESS ? "Wastes" : colour.getBasicLandType(), colour);
        }
    }

    /** saved says the deck was opened from where target saves it. A new deck, or one that is only being viewed, was not. */
    DeckEditor(final Deck deck, final boolean readOnly, final boolean saved, final Target target, final LegalityScope check,
            final DeckStore.Storages storages, final boolean guest, final DeviceSink sink) {
        // Edit a copy, so a deck that another screen holds (a catalogue entry, a seat) never changes under that screen
        this.deck = new Deck(deck, deck.getName());
        this.target = target;
        this.check = check;
        this.storages = storages;
        this.guest = guest;
        this.sink = sink;
        this.copyOf = readOnly ? deck.getName() : null;
        this.saved = saved && !readOnly;
        this.owned = this.saved && target instanceof Stored ? deck.getName() : null;
        this.landSet = DeckProxy.getDefaultLandSet(deck).getCode();
    }

    /** Whether this is a sealed or draft deck. The pool is the catalogue, and cards move between the pool and the deck instead of being created. */
    boolean limited() {
        return !check.unrestricted() && (check.deckFormat() == DeckFormat.Limited || commanderDraft());
    }

    /** Whether this is a Commander Draft pool. Its commander comes from the pool, or from the few that every player may use without drafting them. */
    private boolean commanderDraft() {
        return !check.unrestricted() && check.deckFormat() == DeckFormat.CommanderDraft;
    }

    /** Whether this is a planar deck. All its cards are in its Planes section, with at most one of each. */
    boolean planar() {
        return !check.unrestricted() && check.format() == GameType.Planechase;
    }

    /** Marks a new deck, whose name is a placeholder that its first commander replaces. */
    void unnamed() {
        named = false;
    }

    void setFreeCommander(final String name) {
        freeCommander = name;
    }

    private List<String> freeCommanders() {
        final List<String> names = new ArrayList<>();
        if (freeCommander != null && !freeCommander.isEmpty()) {
            names.add(freeCommander);
        }
        names.addAll(List.of(CommanderDraftUtil.FREE_COMMANDER_FALLBACKS));
        return names;
    }

    private boolean isFreeCommander(final String name) {
        return commanderDraft() && freeCommanders().contains(name);
    }

    /** The cards the catalogue offers: the pool in limited mode, otherwise every card. */
    CardCatalog catalogue() {
        if (!limited()) {
            return CardCatalog.get();
        }
        final Map<String, PaperCard> byName = new LinkedHashMap<>();
        final CardPool side = deck.get(DeckSection.Sideboard);
        if (side != null) {
            side.forEach(e -> byName.putIfAbsent(e.getKey().getName(), e.getKey()));
        }
        for (final Map.Entry<PaperCard, Integer> e : deck.getMain()) {
            if (!e.getKey().getRules().getType().isBasicLand()) {
                byName.putIfAbsent(e.getKey().getName(), e.getKey());
            }
        }
        if (commanderDraft()) {
            final CardPool commanders = deck.get(DeckSection.Commander);
            if (commanders != null) {
                commanders.forEach(e -> byName.putIfAbsent(e.getKey().getName(), e.getKey()));
            }
            for (final String name : freeCommanders()) {
                final PaperCard free = FModel.getMagicDb().getCommonCards().getCard(name);
                if (free != null) {
                    byName.putIfAbsent(name, free);
                }
            }
        }
        return CardCatalog.of(byName.values());
    }

    Deck deck() {
        return deck;
    }

    LegalityScope check() {
        return check;
    }

    Target target() {
        return target;
    }

    String copyOf() {
        return copyOf;
    }

    /** Whether the deck exists where it is saved, so Done has a deck to put on a seat. */
    boolean saved() {
        return saved;
    }

    String add(final String name, final DeckSection to, final int count) {
        if (limited()) {
            return addFromPool(name, to, count);
        }
        final PaperCard card = printingFor(name);
        if (card == null || wrongKind(card, to)) {
            return Localizer.getInstance().getMessage("lblWebEditorNoSuchCard", String.valueOf(name));
        }
        final String limit = overLimit(card, count);
        if (limit != null) {
            return limit;
        }
        return change(name, () -> deck.getOrCreate(to).add(card, count));
    }

    /** Adds one copy of the printing chosen by its image key. A pool holds its own printings, so there the card is added like any other. */
    String addPrinting(final String name, final DeckSection to, final String imageKey) {
        if (limited()) {
            return add(name, to, 1);
        }
        final PaperCard card = ImageUtil.getPaperCardFromImageKey(imageKey);
        if (card == null || !card.getName().equals(name) || wrongKind(card, to)) {
            return Localizer.getInstance().getMessage("lblWebEditorNotAPrinting", String.valueOf(name));
        }
        final String limit = overLimit(card, 1);
        if (limit != null) {
            return limit;
        }
        return change(name, () -> deck.getOrCreate(to).add(card, 1));
    }

    /** A planar deck holds only planes and phenomena, all in one section. */
    private boolean wrongKind(final PaperCard card, final DeckSection to) {
        return planar() && (to != DeckSection.Planes || DeckSection.matchingSection(card) != DeckSection.Planes);
    }

    /** Adds random planes to a planar deck until it has the fewest cards it may be played with. */
    String fillWithRandomPlanes() {
        if (!planar()) {
            return null;
        }
        final CardPool held = deck.getOrCreate(DeckSection.Planes);
        final int wanted = FEWEST_PLANES - held.countAll();
        if (wanted <= 0) {
            return null;
        }
        final List<PaperCard> left = StaticData.instance().getVariantCards().getUniqueCards().stream()
                .filter(c -> c.getRules().getType().isPlane() && held.countByName(c.getName()) == 0).toList();
        return change(null, () -> Aggregates.random(left, wanted).forEach(held::add));
    }

    /** Makes every copy of a card in a section foil, or plain again when all copies are foil, as desktop's editor does. */
    String toggleFoil(final String name, final DeckSection in) {
        if (limited()) {
            return Localizer.getInstance().getMessage(DECK_FIXED);
        }
        final CardPool pool = deck.get(in);
        final int have = pool == null ? 0 : pool.countByName(name);
        if (have == 0) {
            return Localizer.getInstance().getMessage("lblWebEditorNoneToMove", String.valueOf(name));
        }
        return change(name, () -> {
            final CardPool copies = take(pool, name, have);
            final boolean plain = copies.toFlatList().stream().anyMatch(c -> !c.isFoil());
            copies.forEach(e -> pool.add(plain ? e.getKey().getFoiled() : e.getKey().getUnFoiled(), e.getValue()));
        });
    }

    String remove(final String name, final DeckSection from, final int count) {
        if (limited() && from != DeckSection.Sideboard) {
            return move(name, from, DeckSection.Sideboard, count);
        }
        final CardPool pool = deck.get(from);
        final int have = pool == null ? 0 : pool.countByName(name);
        if (have == 0) {
            return Localizer.getInstance().getMessage("lblWebEditorNoneToRemove", String.valueOf(name));
        }
        return change(name, () -> take(pool, name, Math.min(count, have)));
    }

    String move(final String name, final DeckSection from, final DeckSection to, final int count) {
        if (to == DeckSection.Commander) {
            return makeCommander(name, from);
        }
        final CardPool pool = deck.get(from);
        final int have = pool == null ? 0 : pool.countByName(name);
        if (have == 0 || from == to) {
            return Localizer.getInstance().getMessage("lblWebEditorNoneToMove", String.valueOf(name));
        }
        // A drafted commander that is removed goes back to the pool. A commander that was never drafted is dropped
        final boolean leaving = commanderDraft() && from == DeckSection.Commander;
        final DeckSection home = leaving ? DeckSection.Sideboard : to;
        return change(name, () -> {
            final CardPool moving = take(pool, name, Math.min(count, have));
            if (!(leaving && isFreeCommander(name))) {
                deck.getOrCreate(home).addAll(moving);
            }
        });
    }

    /** Makes a card the commander, from a section of the deck or, when from is null, from the catalogue. A commander it replaces goes to the main deck, or back to the pool in Commander Draft. */
    String makeCommander(final String name, final DeckSection from) {
        final DeckFormat df = check.deckFormat();
        if (!df.hasCommander()) {
            return Localizer.getInstance().getMessage("lblWebCardMenuOnlyCommanderFormats");
        }
        final CardPool source = from != null ? deck.get(from) : commanderDraft() ? holding(name) : null;
        final PaperCard card = source != null ? source.find(c -> c.getName().equals(name))
                : commanderDraft() ? (isFreeCommander(name) ? FModel.getMagicDb().getCommonCards().getCard(name) : null)
                : printingFor(name);
        if (card == null && commanderDraft()) {
            return Localizer.getInstance().getMessage("lblWebEditorNoneInPool", String.valueOf(name));
        }
        if (card == null) {
            return Localizer.getInstance().getMessage("lblWebEditorNoSuchCard", String.valueOf(name));
        }
        final CardRules rules = card.getRules();
        if (!df.isAllowedCommander(rules, Legality.enforced()) && !(df.hasSignatureSpell() && rules.canBeSignatureSpell())) {
            return Localizer.getInstance().getMessage("lblWebEditorCannotCommand", String.valueOf(name));
        }
        final CardPool commanders = deck.getOrCreate(DeckSection.Commander);
        if (commanders.countByName(name) > 0) {
            return Localizer.getInstance().getMessage("lblWebEditorAlreadyCommander", String.valueOf(name));
        }
        if (source == null && !commanderDraft()) {
            final String limit = overLimit(card, 1);
            if (limit != null) {
                return limit;
            }
        }
        return change(name, () -> {
            if (source != null) {
                source.remove(card, 1);
            }
            placeCommander(card, commanders);
            if (!named) {
                nameAfterCommander();
            }
        });
    }

    /** Sets how many copies of each printing of one card a section holds. The total cannot change. */
    String setPrintings(final String name, final DeckSection in, final Map<String, Integer> countsByImageKey) {
        final CardPool pool = deck.get(in);
        final int have = pool == null ? 0 : pool.countByName(name);
        final int total = countsByImageKey.values().stream().mapToInt(Integer::intValue).sum();
        if (total != have) {
            return Localizer.getInstance().getMessage("lblWebEditorTotalFixed", have);
        }
        final Map<PaperCard, Integer> wanted = new LinkedHashMap<>();
        for (final Map.Entry<String, Integer> e : countsByImageKey.entrySet()) {
            final PaperCard printing = ImageUtil.getPaperCardFromImageKey(e.getKey());
            if (printing == null || !printing.getName().equals(name)) {
                return Localizer.getInstance().getMessage("lblWebEditorNotAPrinting", String.valueOf(name));
            }
            wanted.merge(printing, e.getValue(), Integer::sum);
        }
        return change(name, () -> {
            take(pool, name, have);
            wanted.forEach((printing, n) -> pool.add(printing, n));
        });
    }

    String setLands(final Map<String, Integer> countsByLand) {
        for (final Map.Entry<String, Integer> e : countsByLand.entrySet()) {
            if (!BASICS.containsKey(e.getKey())) {
                return Localizer.getInstance().getMessage("lblWebEditorNotBasic", String.valueOf(e.getKey()));
            }
            if (e.getValue() > deck.getMain().countByName(e.getKey()) && !landAllowed(e.getKey())) {
                return Localizer.getInstance().getMessage("lblWebEditorOutsideColours", e.getKey());
            }
        }
        return change(countsByLand.keySet().iterator().next(), () -> {
            for (final Map.Entry<String, Integer> e : countsByLand.entrySet()) {
                final int have = deck.getMain().countByName(e.getKey());
                if (e.getValue() > have) {
                    addLands(e.getKey(), e.getValue() - have);
                } else if (e.getValue() < have) {
                    final CardPool taken = take(deck.getMain(), e.getKey(), have - e.getValue());
                    // A pool's lands belong to the pool, and desktop's limited editor moves them back to it
                    if (limited()) {
                        deck.getOrCreate(DeckSection.Sideboard).addAll(taken);
                    }
                }
            }
        });
    }

    /** Chooses the edition new basic lands come from, as desktop's Add Basic Lands dialog does. */
    String setLandSet(final String editionCode) {
        final CardEdition edition = StaticData.instance().getEditions().get(editionCode);
        if (edition == null || !edition.hasBasicLands()) {
            return Localizer.getInstance().getMessage("lblWebEditorNoBasicsFrom", String.valueOf(editionCode));
        }
        landSet = edition.getCode();
        return null;
    }

    /** Sets the basics to the counts desktop's Add Basic Lands dialog suggests for the rest of the deck. */
    String suggestLands() {
        final Map<ManaCostShard, Integer> suggested = DeckgenUtil.suggestBasicLandCount(deck);
        final Map<String, Integer> counts = new LinkedHashMap<>();
        BASICS.forEach((land, colour) -> {
            if (colour != MagicColor.Color.COLORLESS) {
                counts.put(land, suggested.getOrDefault(ManaCostShard.valueOf(colour.getColorMask()), 0));
            }
        });
        return setLands(counts);
    }

    String rename(final String wanted) {
        if (target instanceof Group) {
            return Localizer.getInstance().getMessage(GROUP_OWNS_IT);
        }
        final String problem = DeckStore.nameProblem(wanted);
        if (problem != null) {
            return problem;
        }
        final String name = wanted.trim();
        if (target instanceof Stored s && copyOf == null) {
            final String existing = DeckStore.taken(s.storage(), name);
            if (existing != null && (owned == null || !DeckStore.sameFile(existing, owned))) {
                return Localizer.getInstance().getMessage("lblWebEditorNameTaken", existing);
            }
        }
        named = true;
        return change(null, () -> renameTo(name));
    }

    /** Changes what the deck is checked against. A change of format family moves the deck to that format's folder. */
    String setCheck(final LegalityScope wanted) {
        // A planar deck is not a deck of any other format
        if (planar()) {
            return null;
        }
        return change(null, () -> moveTo(wanted));
    }

    String undo() {
        final Snapshot previous = undo.poll();
        if (previous == null) {
            return Localizer.getInstance().getMessage("lblWebEditorNothingToUndo");
        }
        landed = null;
        moveTo(previous.check());
        if (!previous.deck().getName().equals(deck.getName())) {
            // Another deck may have taken the old name since then, and storage would overwrite that deck
            final String name = previous.deck().getName();
            renameTo(target instanceof Stored s ? DeckStore.freeName(s.storage(), name, owned) : name);
        }
        // Replace every section, because a limited deck's Attractions and Contraptions move there from the pool
        replaceCards(previous.deck(), List.of(DeckSection.values()));
        return save();
    }

    /** Saves a copy under a free name, and continues editing the copy. */
    String duplicate() {
        if (target instanceof Group) {
            return Localizer.getInstance().getMessage(GROUP_OWNS_IT);
        }
        if (copyOf != null) {
            becomeCopy();
            return save();
        }
        final Deck copy = new Deck(deck, deck.getName());
        if (target instanceof Stored s) {
            copy.setName(DeckStore.freeName(s.storage(), deck.getName(), null));
            owned = null;
        } else {
            target = new Device(UUID.randomUUID().toString());
            copy.setName(Localizer.getInstance().getMessage("lblWebEditorCopyName", deck.getName()));
        }
        deck = copy;
        undo.clear();
        landed = null;
        return save();
    }

    String delete() {
        if (target instanceof Group) {
            return Localizer.getInstance().getMessage(GROUP_OWNS_IT);
        }
        // The player is only viewing a precon or another player's deck, so there is no saved deck of theirs to delete
        if (copyOf != null) {
            return Localizer.getInstance().getMessage("lblWebEditorOnlyOwnDelete");
        }
        return delete(target, owned, check.format(), sink);
    }

    /** Deletes a deck from where it is saved. name is the deck's saved name, or null when it was never saved. Returns why it failed, or null. */
    static String delete(final Target target, final String name, final GameType format, final DeviceSink sink) {
        try {
            if (target instanceof Device d) {
                sink.deviceDeck(d.id(), null, format);
            } else if (target instanceof Stored s && name != null) {
                synchronized (DeckCatalog.DECKS) {
                    s.storage().delete(name);
                }
            }
        } catch (final RuntimeException e) {
            return Localizer.getInstance().getMessage("lblWebEditorDeleteFailed", String.valueOf(e.getMessage()));
        }
        return null;
    }

    /** Adds another deck's cards, as far as the copy limit allows. */
    String addAll(final Deck other) {
        return change(null, () -> addCards(other));
    }

    private void addCards(final Deck other) {
        for (final DeckSection section : List.of(DeckSection.Main, DeckSection.Sideboard)) {
            final CardPool from = other.get(section);
            if (from == null) {
                continue;
            }
            for (final Map.Entry<PaperCard, Integer> e : from) {
                final int room = room(e.getKey());
                if (room > 0) {
                    deck.getOrCreate(section).add(e.getKey(), Math.min(room, e.getValue()));
                }
            }
        }
    }

    /** Replaces the deck's cards with another deck's cards. The deck keeps its name and save location. */
    String replaceAll(final Deck other) {
        return change(null, () -> replaceCards(other, List.of(DeckSection.Main, DeckSection.Sideboard, DeckSection.Commander)));
    }

    EditorState state() {
        final DeckFormat df = check.deckFormat();
        final List<PaperCard> leaders = Legality.commanders(deck, check);
        final Legality.Result legality = Legality.check(deck, check);
        final boolean wanted = df.hasCommander() && (leaders.isEmpty() || (df.hasSignatureSpell() && deck.getSignatureSpell() == null));
        final int most = planar() ? 1 : df.getMaxCardCopies();
        return new EditorState(deck.getName(), check.label(), check.format().name(),
                check.pool() == null ? null : check.pool().getName(), check.unrestricted(),
                target instanceof Device, copyOf,
                CardCatalog.cards(deck.get(DeckSection.Commander), legality), df.hasCommander(), wanted, most == Integer.MAX_VALUE ? null : most,
                Legality.identityLetters(leaders),
                CardCatalog.groups(planar() ? deck.getOrCreate(DeckSection.Planes) : deck.getMain(), legality),
                CardCatalog.cards(deck.get(DeckSection.Sideboard), legality), lands(),
                DeckFacts.stats(deck), legality.verdict(), !undo.isEmpty(), landed,
                limited(), landSet, LandSets.ALL, sections(legality));
    }

    /** Every section beyond the main deck that a constructed deck can have, as desktop's editor offers them. A pool or a planar deck has none. */
    private List<EditorSection> sections(final Legality.Result legality) {
        if (limited() || planar()) {
            return List.of();
        }
        final List<EditorSection> out = new ArrayList<>();
        for (final DeckSection section : DeckSection.NONTRADITIONAL_SECTIONS) {
            out.add(new EditorSection(section, section.getLocalizedName(), CardCatalog.cards(deck.get(section), legality)));
        }
        return out;
    }

    private String change(final String cardName, final Runnable op) {
        final Snapshot before = new Snapshot(new Deck(deck, deck.getName()), check);
        op.run();
        undo.push(before);
        if (undo.size() > MOST_UNDO) {
            undo.removeLast();
        }
        landed = cardName;
        if (copyOf != null) {
            becomeCopy();
        }
        return save();
    }

    /** The first change to a deck that cannot be changed in place. From now on the editor works on its own copy. */
    private void becomeCopy() {
        if (deck.getName().equals(copyOf)) {
            deck.setName(Localizer.getInstance().getMessage("lblWebEditorCopyName", copyOf));
        }
        copyOf = null;
        owned = null;
        target = guest ? new Device(UUID.randomUUID().toString()) : new Stored(storages.of(check.format()));
    }

    private String save() {
        try {
            if (target instanceof Device d) {
                sink.deviceDeck(d.id(), DeckWorkspace.text(deck), check.format());
                saved = true;
                return null;
            }
            if (target instanceof Group g) {
                synchronized (DeckCatalog.DECKS) {
                    final DeckGroup group = g.storage().get(deck.getName());
                    if (group == null) {
                        return Localizer.getInstance().getMessage("lblWebEditorPoolGone", deck.getName());
                    }
                    group.setHumanDeck(new Deck(deck, deck.getName()));
                    g.storage().add(group);
                }
                saved = true;
                return null;
            }
            final IStorage<Deck> storage = ((Stored) target).storage();
            synchronized (DeckCatalog.DECKS) {
                // Storage overwrites by file name, so a deck saved for the first time gets a name that no other deck uses
                if (owned == null) {
                    deck.setName(DeckStore.freeName(storage, deck.getName(), null));
                }
                storage.add(deck);
                // Storage keeps this same deck object, which is still being edited, so the list's cached facts about it are out of date
                DeckFacts.changed(deck);
                owned = deck.getName();
            }
            saved = true;
            return null;
        } catch (final RuntimeException e) {
            return Localizer.getInstance().getMessage("lblWebEditorSaveFailed", String.valueOf(e.getMessage()));
        }
    }

    /** Renames the deck, and its file with it. */
    private void renameTo(final String name) {
        if (!(target instanceof Stored s) || owned == null) {
            deck.setName(name);
            return;
        }
        synchronized (DeckCatalog.DECKS) {
            moveFile(s.storage(), s.storage(), name);
        }
    }

    /** Changes the check. Moves the deck when the new format stores its decks in another folder. */
    private void moveTo(final LegalityScope wanted) {
        final boolean newFamily = DeckStore.family(wanted.format()) != DeckStore.family(check.format());
        check = wanted;
        if (!newFamily || !(target instanceof Stored s) || copyOf != null) {
            return;
        }
        final IStorage<Deck> to = storages.of(wanted.format());
        synchronized (DeckCatalog.DECKS) {
            if (owned == null) {
                target = new Stored(to);
                return;
            }
            moveFile(s.storage(), to, DeckStore.freeName(to, deck.getName(), null));
        }
    }

    /** Storage deletes by the deck's own name, so the old deck keeps its name until it is deleted, and it is deleted first when both names map to one file. */
    private void moveFile(final IStorage<Deck> from, final IStorage<Deck> to, final String name) {
        if (from == to && DeckStore.sameFile(owned, name)) {
            from.delete(owned);
            deck.setName(name);
            to.add(deck);
        } else {
            final Deck moved = new Deck(deck, name);
            to.add(moved);
            from.delete(owned);
            deck = moved;
        }
        owned = name;
        target = new Stored(to);
    }

    private void nameAfterCommander() {
        named = true;
        final String wanted = Localizer.getInstance().getMessage("lblDeckUrlDefaultDeckName", deck.getCommanders().get(0).getName());
        if (target instanceof Stored s) {
            renameTo(DeckStore.freeName(s.storage(), wanted, owned));
        } else {
            deck.setName(wanted);
        }
    }

    /** Puts a card in the command zone as desktop's editors do. A partner joins the commander, any other card replaces it, and in Oathbreaker each slot holds one card. Desktop keeps a copy of this rule in each of its editors, so there is no shared one to call. */
    private void placeCommander(final PaperCard card, final CardPool commanders) {
        if (check.format() == GameType.Oathbreaker) {
            final boolean oathbreaker = card.getRules().canBeOathbreaker();
            final PaperCard sameSlot = commanders.find(c -> c.getRules().canBeOathbreaker() == oathbreaker);
            if (sameSlot != null) {
                deck.getMain().add(sameSlot, commanders.count(sameSlot));
                commanders.remove(sameSlot, commanders.count(sameSlot));
            }
        } else if (commanders.countAll() > 0) {
            final List<PaperCard> existing = commanders.toFlatList();
            final boolean partner = existing.size() == 1 && (card.getRules().canBePartnerCommander()
                    && existing.get(0).getRules().canBePartnerCommanders(card.getRules())
                    // A Commander Draft also pairs two commanders that the draft rules allow as partners, such as two one-colour legends
                    || commanderDraft() && CommanderDraftUtil.isPartnerEligible(existing.get(0)) && CommanderDraftUtil.isPartnerEligible(card));
            if (!partner) {
                final CardPool home = commanderDraft() ? deck.getOrCreate(DeckSection.Sideboard) : deck.getMain();
                for (final Map.Entry<PaperCard, Integer> e : commanders) {
                    if (!isFreeCommander(e.getKey().getName())) {
                        home.add(e.getKey(), e.getValue());
                    }
                }
                commanders.clear();
            }
        }
        commanders.add(card, 1);
    }

    private void replaceCards(final Deck from, final List<DeckSection> sections) {
        for (final DeckSection section : sections) {
            if (!deck.has(section) && !from.has(section)) {
                continue;
            }
            final CardPool pool = deck.getOrCreate(section);
            pool.clear();
            final CardPool source = from.get(section);
            if (source != null) {
                pool.addAll(source);
            }
        }
    }

    /** Removes copies of one card from a pool, starting with the printings that have the fewest copies. Returns the removed copies. */
    private static CardPool take(final CardPool pool, final String name, final int count) {
        final CardPool taken = new CardPool();
        final List<Map.Entry<PaperCard, Integer>> printings = new ArrayList<>();
        for (final Map.Entry<PaperCard, Integer> e : pool) {
            if (e.getKey().getName().equals(name)) {
                printings.add(Map.entry(e.getKey(), e.getValue()));
            }
        }
        printings.sort(Map.Entry.comparingByValue());
        int left = count;
        for (final Map.Entry<PaperCard, Integer> e : printings) {
            final int n = Math.min(left, e.getValue());
            pool.remove(e.getKey(), n);
            taken.add(e.getKey(), n);
            left -= n;
            if (left == 0) {
                break;
            }
        }
        return taken;
    }

    /** The section of a pool's deck that holds the card to make commander. The pool is checked first, then the main deck. */
    private CardPool holding(final String name) {
        for (final DeckSection section : List.of(DeckSection.Sideboard, DeckSection.Main)) {
            final CardPool pool = deck.get(section);
            if (pool != null && pool.countByName(name) > 0) {
                return pool;
            }
        }
        return null;
    }

    private String addFromPool(final String name, final DeckSection to, final int count) {
        final CardPool side = deck.get(DeckSection.Sideboard);
        final PaperCard card = side == null ? null : side.find(c -> c.getName().equals(name));
        if (card == null) {
            return Localizer.getInstance().getMessage("lblWebEditorNoneInPool", String.valueOf(name));
        }
        // Attractions and Contraptions go to their own sections, as in desktop's limited editor
        return move(name, DeckSection.Sideboard, to == DeckSection.Main ? DeckSection.matchingSection(card) : to, count);
    }

    /** Adds basics to the main deck. In limited mode the pool's own basics are used first, then new ones. */
    private void addLands(final String name, final int count) {
        int wanted = count;
        if (limited()) {
            final CardPool side = deck.get(DeckSection.Sideboard);
            final int fromPool = side == null ? 0 : Math.min(wanted, side.countByName(name));
            if (fromPool > 0) {
                deck.getMain().addAll(take(side, name, fromPool));
                wanted -= fromPool;
            }
        }
        if (wanted > 0) {
            deck.getMain().add(newLand(name), wanted);
        }
    }

    /** The printing a new basic land uses: the chosen land set's, or else the usual one. */
    private PaperCard newLand(final String name) {
        final PaperCard fromSet = StaticData.instance().getCommonCards().getCard(name, landSet);
        return fromSet != null ? fromSet : printingFor(name);
    }

    /** The printing a new copy uses: the one the deck already holds if it holds only one, otherwise the preferred art. */
    private PaperCard printingFor(final String name) {
        PaperCard only = null;
        for (final Map.Entry<PaperCard, Integer> e : deck.getAllCardsInASinglePool(true, false)) {
            if (e.getKey().getName().equals(name)) {
                if (only != null && !only.equals(e.getKey())) {
                    only = null;
                    break;
                }
                only = e.getKey();
            }
        }
        if (only != null) {
            return only;
        }
        // Planes, schemes and avatars are stored separately from all other cards
        final PaperCard common = StaticData.instance().getCommonCards().getCard(name);
        return common != null ? common : StaticData.instance().getVariantCards().getCard(name);
    }

    /** A planar deck has no second copy of a phenomenon either, though the format's copy limit allows two. */
    private int most(final PaperCard card) {
        return planar() ? 1 : check.deckFormat().getMaxCardCopies(card);
    }

    /** How many more copies of a card the copy limit allows, counted by name across the deck. */
    private int room(final PaperCard card) {
        final int most = most(card);
        final CardPool held = planar() ? deck.getOrCreate(DeckSection.Planes)
                : deck.getAllCardsInASinglePool(check.deckFormat().hasCommander(), false);
        return most == Integer.MAX_VALUE ? Integer.MAX_VALUE : most - held.countByName(card.getName());
    }

    private String overLimit(final PaperCard card, final int adding) {
        final int room = room(card);
        if (adding <= room) {
            return null;
        }
        final int most = most(card);
        if (most == Integer.MAX_VALUE) {
            return Localizer.getInstance().getMessage("lblWebDeckNoneLeft", card.getName());
        }
        return most == 1 ? Localizer.getInstance().getMessage("lblWebEditorOnlyOne", card.getName())
                : Localizer.getInstance().getMessage("lblWebEditorCopiesAlready", most - room, most);
    }

    private boolean landAllowed(final String land) {
        final List<PaperCard> leaders = Legality.commanders(deck, check);
        if (leaders.isEmpty()) {
            return true;
        }
        // The engine's own test, so a colourless basic such as Wastes is allowed under any commander
        return check.deckFormat().isLegalCardForCommanderPredicate(leaders).test(newLand(land));
    }

    private List<EditorLand> lands() {
        return BASICS.keySet().stream().map(name -> new EditorLand(name, deck.getMain().countByName(name), landAllowed(name),
                newLand(name).getImageKey(false))).toList();
    }

    sealed interface Target permits Stored, Device, Group {
    }

    /** A folder of the host's decks: a format's own, or a subfolder of it. */
    record Stored(IStorage<Deck> storage) implements Target {
    }

    /** A guest's browser, which stores the deck under this id. */
    record Device(String id) implements Target {
    }

    /** A sealed or draft pool. The deck is the human deck of the group with its name, and the group's opponents stay unchanged. */
    record Group(IStorage<DeckGroup> storage) implements Target {
    }

    /** Tells a guest's browser that its deck changed. text is the deck as .dck text, or null when it was deleted. */
    interface DeviceSink {
        void deviceDeck(String id, String text, GameType format);
    }

    private record Snapshot(Deck deck, LegalityScope check) {
    }

    /** Built once here. StaticData's sorted list is built lazily without a lock, so two editors opening at once could read it half sorted. */
    private static final class LandSets {
        static final List<LandSet> ALL = build();

        private static List<LandSet> build() {
            final List<CardEdition> editions = new ArrayList<>();
            for (final CardEdition e : StaticData.instance().getEditions()) {
                if (e.hasBasicLands()) {
                    editions.add(e);
                }
            }
            editions.sort(Comparator.reverseOrder());
            return editions.stream().map(e -> new LandSet(e.getCode(), e.getName())).toList();
        }
    }
}
