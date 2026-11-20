package forge.web.deck;

import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckFormat;
import forge.deck.DeckGroup;
import forge.deck.DeckRecognizer;
import forge.deck.DeckSection;
import forge.deck.DeckUrlLoader;
import forge.deck.DeckUrlProvider;
import forge.deck.io.DeckSerializer;
import forge.game.GameFormat;
import forge.game.GameType;
import forge.item.PaperCard;
import forge.itemmanager.SFilterUtil;
import forge.model.FModel;
import forge.util.FileSection;
import forge.util.IHasForgeLog;
import forge.util.Localizer;
import forge.util.storage.IStorage;
import forge.web.WebGuiBase;
import forge.web.deck.DeckCatalog.OnDevice;
import forge.web.lobby.Lobby;
import forge.web.protocol.BrowserChannel;
import forge.web.protocol.Commands;
import forge.web.protocol.Commands.Handler;
import forge.web.protocol.FromBrowser;
import forge.web.protocol.FromBrowser.*;
import forge.web.protocol.ToBrowser;
import forge.web.protocol.ToBrowser.*;
import org.apache.commons.lang3.EnumUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Predicate;

/** One browser's deck editor and importer. It also holds the decks that a guest's browser stores. */
public final class DeckWorkspace implements IHasForgeLog {
    public static final Commands<DeckWorkspace> COMMANDS = new Commands<DeckWorkspace>()
            .on(BrowseFormat.class, (d, ch, b) -> {
                if (d.host.getAsBoolean() && !d.atTable.getAsBoolean()) {
                    d.lobby.setBrowseFormat(gameType(b.format()));
                    d.lobby.sendDecks(ch);
                }
            })
            .on(DeviceDecks.class, (d, ch, dd) -> {
                if (!d.host.getAsBoolean()) {
                    d.keepDeviceDecks(dd.decks());
                    d.lobby.sendDecks(ch);
                }
            })
            .on(CatalogueQuery.class, (d, ch, q) -> ch.send(d.catalogue(q)))
            .on(ImportRead.class, DeckWorkspace::read)
            // Fetching waits for a web site
            .on(ImportFetch.class, (d, ch, f) -> d.ui.runBackgroundTask("Import", () -> ch.send(d.fetched(f))))
            .on(ImportCommit.class, DeckWorkspace::commit)
            .on(DeckFile.class, DeckWorkspace::addFile)
            .on(DeckDelete.class, (d, ch, del) -> d.deleteDeck(ch, del.key()))
            // The stars belong to the host and are stored where desktop stores them
            .on(DeckFavourite.class, (d, ch, star) -> {
                if (d.host.getAsBoolean()) {
                    d.lobby.catalog().setFavourite(star.key(), star.on());
                    d.lobby.sendDecks(ch);
                }
            })
            .on(EditorOpen.class, DeckWorkspace::openFromBrowser)
            .on(EditorEdit.class, edit((d, ch, e) -> d.change(e)))
            .on(EditorRename.class, edit((d, ch, r) -> d.editor.rename(r.name())))
            .on(EditorCheck.class, edit((d, ch, c) -> d.editor.setCheck(check(c.format(), c.cardPool(), c.unrestricted()))))
            .on(Plain.editorUndo, edit((d, ch, c) -> d.editor.undo()))
            .on(EditorDeck.class, edit((d, ch, e) -> {
                if (e.op() == DeckOp.file) {
                    ch.send(new DeckFileText(d.editor.deck().getName(), text(d.editor.deck())));
                    return null;
                }
                return e.op() == DeckOp.delete ? d.delete(ch) : d.editor.duplicate();
            }))
            .on(Plain.editorClose, edit((d, ch, c) -> d.close(ch)));
    private final Lobby lobby;
    private final WebGuiBase ui;
    private final BooleanSupplier host;
    private final BooleanSupplier atTable;
    private final Executor work = WebGuiBase.worker("WebDecks");
    private final DeckStore.Storages storages = DeckStore.real();
    /** A guest's decks, keyed by the id its browser stores each one under. The lobby lists them too. */
    private final Map<String, OnDevice> device;

    private DeckEditor editor;
    /** The seat whose finder opened the editor, and the table it was at. Done puts the deck back only where it came from. */
    private Integer editorSeat;
    private int editorTable;
    /** The subfolder of the format's decks that the open deck came from, or "" for the top folder. */
    private String editorPath = "";
    /** Whether the open deck is an event's pool. The pool goes on its player's seat when the editor closes, built or not. */
    private boolean eventPool;
    /** A guest's event pool that its browser has not yet confirmed it stored. */
    private volatile EventPool pendingPool;
    /** Where to send a guest's deck. The session updates it as the browser connects and disconnects. */
    private final AtomicReference<BrowserChannel> channelFor = new AtomicReference<>();

    public DeckWorkspace(final Lobby lobby, final WebGuiBase ui, final BooleanSupplier host, final BooleanSupplier atTable,
            final Map<String, OnDevice> device) {
        this.lobby = lobby;
        this.ui = ui;
        this.host = host;
        this.atTable = atTable;
        this.device = device;
    }

    /** A browser reconnected. It is shown the editor it left, and a guest is sent any deck it may have missed. */
    public synchronized void reconnected(final BrowserChannel channel) {
        final EventPool p = pendingPool;
        if (p != null) {
            channel.send(new DeviceDeck(p.id(), text(p.deck()), p.format().name()));
        }
        if (editor == null) {
            return;
        }
        channel.send(new EditorMessage(editor.state()));
        if (editor.target() instanceof DeckEditor.Device d) {
            channel.send(new DeviceDeck(d.id(), text(editor.deck()), editor.check().format().name()));
        }
    }

    /** Makes a change to the open deck, if there is one, then shows the updated editor. */
    private static <T extends Record> Handler<DeckWorkspace, T> edit(final Change<T> change) {
        return (d, ch, command) -> {
            synchronized (d) {
                if (d.editor == null) {
                    return;
                }
                final String refused = change.run(d, ch, command);
                if (refused != null) {
                    ch.send(new Notice(refused, null, false));
                }
                if (d.editor != null) {
                    ch.send(new EditorMessage(d.editor.state()));
                }
            }
        };
    }

    private synchronized void openFromBrowser(final BrowserChannel channel, final EditorOpen o) {
        open(o, null, channel);
        if (editor != null) {
            channel.send(new EditorMessage(editor.state()));
        }
    }

    private void read(final BrowserChannel channel, final ImportRead read) {
        final LegalityScope check = check(read.format(), read.cardPool(), read.unrestricted());
        final DeckImport.Read parsed = DeckImport.read(read.text(), check);
        channel.send(DeckImport.result(read.request(), parsed, parsed.name(), check, null));
    }

    /** Opens the editor. checkedAgainst is the check to open with. Null means the table's check, or the finder's when there is no table. */
    private void open(final EditorOpen o, final LegalityScope checkedAgainst, final BrowserChannel channel) {
        final boolean guest = !host.getAsBoolean();
        final Deck deck;
        final boolean readOnly;
        final DeckEditor.Target target;
        LegalityScope check = checkedAgainst != null ? checkedAgainst
                : check(lobby.format().name(), atTable.getAsBoolean() && lobby.cardPool() != null ? lobby.cardPool().getName() : null, false);
        editorPath = "";
        if (o.newFormat() != null) {
            check = LegalityScope.of(gameType(o.newFormat()), null);
            deck = new Deck(DeckEditor.newDeckName());
            readOnly = false;
            target = guest ? new DeckEditor.Device(UUID.randomUUID().toString()) : new DeckEditor.Stored(storages.of(check.format()));
        } else {
            deck = o.key() == null ? null : lobby.catalog().deck(o.key());
            if (deck == null) {
                channel.send(new Notice(Localizer.getInstance().getMessage("lblWebEditorCannotOpen"), null, true));
                return;
            }
            readOnly = o.copy() || DeckCatalog.readOnly(o.key(), guest);
            if (!readOnly) {
                editorPath = DeckCatalog.pathOf(o.key());
            }
            target = targetOf(o.key(), editorPath, check);
        }
        editor = new DeckEditor(deck, readOnly, o.key() != null, target, check, storages, guest, this::sendDeviceDeck);
        if (o.newFormat() != null) {
            editor.unnamed();
        }
        eventPool = false;
        editorSeat = o.seat();
        editorTable = lobby.table();
    }

    /** Where a deck is saved: the browser's own list, or a folder in its format's decks ("" for the top). */
    private DeckEditor.Target targetOf(final String key, final String path, final LegalityScope check) {
        if (key.startsWith(DeckCatalog.DEVICE + ":")) {
            return new DeckEditor.Device(key.substring(DeckCatalog.DEVICE.length() + 1));
        }
        final IStorage<Deck> root = storages.of(check.format());
        final IStorage<Deck> folder = path.isEmpty() ? null : root.tryGetFolder(path.substring(1));
        return new DeckEditor.Stored(folder == null ? root : folder);
    }

    /** Deletes a deck the way the editor's Delete does, but without the editor, which may be open on another deck. */
    private synchronized void deleteDeck(final BrowserChannel channel, final String key) {
        final boolean guest = !host.getAsBoolean();
        final Deck deck = lobby.catalog().deck(key);
        final String problem;
        if (deck == null || DeckCatalog.readOnly(key, guest)) {
            problem = Localizer.getInstance().getMessage("lblWebEditorOnlyOwnDelete");
        } else {
            final LegalityScope check = check(lobby.format().name(), null, false);
            problem = DeckEditor.delete(targetOf(key, DeckCatalog.pathOf(key), check), deck.getName(), check.format(), this::sendDeviceDeck);
        }
        if (problem != null) {
            channel.send(new Notice(problem, null, false));
        }
        lobby.sendDecks(channel);
    }

    /**
     * Opens a pool's deck, which saves back into its pool. channel is null when no browser is attached to show it yet.
     * freeCommander is the commander that a Commander Draft's set gives every player, or null. It is known only just after the draft.
     */
    public synchronized void openPool(final Deck human, final IStorage<DeckGroup> storage, final GameType type, final String freeCommander,
            final BrowserChannel channel) {
        editor = new DeckEditor(human, false, true, new DeckEditor.Group(storage), LegalityScope.of(type, null), storages,
                !host.getAsBoolean(), this::sendDeviceDeck);
        editor.setFreeCommander(freeCommander);
        editorSeat = null;
        editorPath = "";
        if (channel != null) {
            channel.send(new EditorMessage(editor.state()));
        }
    }

    /** A guest's event pool is stored in its browser. It is sent again on each reconnect until the browser confirms it stored it. */
    public synchronized void openEventPool(final Deck pool, final GameType type, final IStorage<Deck> eventDecks, final String deviceId,
            final BrowserChannel channel) {
        final DeckEditor.Target target;
        if (eventDecks == null) {
            pendingPool = new EventPool(deviceId, pool, type);
            sendDeviceDeck(deviceId, text(pool), type);
            target = new DeckEditor.Device(deviceId);
        } else {
            target = new DeckEditor.Stored(eventDecks);
        }
        editor = new DeckEditor(pool, false, true, target, LegalityScope.of(type, null), storages, eventDecks == null, this::sendDeviceDeck);
        eventPool = true;
        editorSeat = lobby.mySeat();
        editorTable = lobby.table();
        editorPath = "";
        if (channel != null) {
            channel.send(new EditorMessage(editor.state()));
        }
    }

    /**
     * Runs a deck list, search or catalogue request on this browser's deck thread, in the order requested. The socket threads
     * also serve every picture, so a list that takes a second to build would block them.
     */
    public void inTurn(final Runnable job) {
        work.execute(job);
    }

    public synchronized String openPoolName() {
        return editor != null && editor.target() instanceof DeckEditor.Group ? editor.deck().getName() : null;
    }

    private String change(final EditorEdit e) {
        final DeckSection from = e.from() == null ? DeckSection.Main : e.from();
        final DeckSection to = e.to() == null ? DeckSection.Main : e.to();
        return switch (e.op()) {
            // An add that names a printing adds one copy of that printing
            case add -> e.printings() == null || e.printings().isEmpty() ? editor.add(e.name(), to, Math.max(1, e.count()))
                    : editor.addPrinting(e.name(), to, e.printings().get(0).name());
            case remove -> editor.remove(e.name(), from, Math.max(1, e.count()));
            case move -> editor.move(e.name(), from, to, Math.max(1, e.count()));
            case commander -> editor.makeCommander(e.name(), e.from());
            case printings -> editor.setPrintings(e.name(), from, counts(e.printings()));
            case lands -> editor.setLands(counts(e.lands()));
            case foil -> editor.toggleFoil(e.name(), from);
            case landSet -> editor.setLandSet(e.name());
            case suggestLands -> editor.suggestLands();
        };
    }

    /** Done. The deck goes on the seat whose finder opened the editor, if that seat is still at the same table and plays this format. */
    private String close(final BrowserChannel channel) {
        final DeckEditor done = editor;
        final Integer seat = editorSeat;
        editor = null;
        channel.send(new EditorMessage(null));
        lobby.sendDecks(channel);
        // A deck that was never saved has nothing to put on the seat. Examples are an untouched new deck, or a precon that was only viewed
        if (seat == null || seat < 0 || !atTable.getAsBoolean() || (!done.saved() && !eventPool)) {
            return null;
        }
        if (lobby.table() != editorTable) {
            return Localizer.getInstance().getMessage("lblWebEditorTableChanged");
        }
        if (DeckStore.family(done.check().format()) != DeckStore.family(lobby.format())) {
            return Localizer.getInstance().getMessage("lblWebEditorWrongFormat", done.check().format());
        }
        final String key = adopt(done);
        // The table's seats are changed only on the host UI thread
        ui.invokeInEdtLater(() -> {
            lobby.setDeck(seat, key);
            lobby.sendState(channel);
        });
        return null;
    }

    private String delete(final BrowserChannel channel) {
        final String problem = editor.delete();
        if (problem != null) {
            return problem;
        }
        editor = null;
        channel.send(new EditorMessage(null));
        lobby.sendDecks(channel);
        return null;
    }

    /** Registers the editor's deck under the finder's current key for it. A rename or a move can change that key. */
    private String adopt(final DeckEditor done) {
        if (done.target() instanceof DeckEditor.Device d) {
            return lobby.catalog().adopt(DeckCatalog.DEVICE, d.id(), done.deck());
        }
        // The host's pool is stored with the event decks, which the finder lists under their own heading
        if (eventPool) {
            return lobby.catalog().adopt(DeckCatalog.EVENT, "", done.deck());
        }
        final boolean atRoot = done.target() instanceof DeckEditor.Stored s && s.storage() == storages.of(done.check().format());
        return lobby.catalog().adopt(DeckCatalog.MINE, atRoot ? "" : editorPath, done.deck());
    }

    private ToBrowser.CataloguePage catalogue(final CatalogueQuery q) {
        final DeckEditor e;
        final LegalityScope check;
        final Deck deck;
        // Take a copy, because other threads can open or change the editor while the search runs
        synchronized (this) {
            e = editor;
            check = e == null ? LegalityScope.none() : e.check();
            deck = e == null ? new Deck("") : new Deck(e.deck());
        }
        final DeckSection section = q.section();
        if (section != null && section != DeckSection.Main && section != DeckSection.Sideboard && section != DeckSection.Commander) {
            // A section's own cards follow no format, so none is hidden as unusable
            final CardPool held = deck.get(section);
            return CardCatalog.of(section).query(q.request(), new CardCatalog.Query(q.text(), q.colours(), q.type(), q.filters(), q.sort(),
                    q.offset(), q.showAll()), c -> null, printedIn(q.sets(), q.format()), name -> held == null ? 0 : held.countByName(name));
        }
        final List<PaperCard> commanders = Legality.commanders(deck, check);
        final DeckFormat df = check.deckFormat();
        // A commander deck without a commander shows only cards that can be its commander. A Background is shown only once the deck has a commander
        final Predicate<PaperCard> commanderOnly = df.hasCommander() && commanders.isEmpty()
                ? c -> df.isAllowedCommander(c.getRules(), Legality.enforced()) && !c.getRules().getType().hasSubtype("Background")
                : null;
        final Predicate<PaperCard> only = SFilterUtil.optimizedAnd(printedIn(q.sets(), q.format()), commanderOnly);
        final CardPool inDeck = deck.getAllCardsInASinglePool(true, false);
        final Function<PaperCard, String> problems = Legality.cardProblems(check, commanders);
        return (e == null ? CardCatalog.get() : e.catalogue()).query(q.request(), new CardCatalog.Query(q.text(), q.colours(), q.type(), q.filters(), q.sort(),
                q.offset(), q.showAll()), problems, only, inDeck::countByName);
    }

    /** Cards printed in any of these sets and legal in this format, read as desktop's card filters read them. Any printing counts. */
    private static Predicate<PaperCard> printedIn(final List<String> sets, final String format) {
        final GameFormat named = format == null ? null : FModel.getFormats().getFormat(format);
        return SFilterUtil.optimizedAnd(sets == null || sets.isEmpty() ? null : new GameFormat(null, sets, null).getFilterRules(),
                named == null ? null : named.getFilterRules());
    }

    private ImportResult fetched(final ImportFetch fetch) {
        try {
            final DeckUrlProvider.RemoteDeck f = DeckUrlLoader.fetch(fetch.url());
            final String text = String.join("\n", DeckUrlLoader.getRecognizableImportLines(new DeckRecognizer(), f.importText()));
            final LegalityScope check = LegalityScope.of(DeckStore.family(f.format()), null);
            return DeckImport.result(fetch.request(), DeckImport.read(text, check), f.name(), check,
                    new Fetched(f.providerName(), f.sourceUrl(), text, check.format().name()));
        } catch (final IOException | RuntimeException e) {
            netLog.warn(e, "Could not fetch a deck from " + fetch.url());
            return new ImportResult(fetch.request(), List.of(),
                    List.of(new ImportProblem(-1, Localizer.getInstance().getMessage("lblWebEditorFetchFailed"),
                            e.getMessage() == null ? Localizer.getInstance().getMessage("lblWebEditorSiteSilent") : e.getMessage(), List.of())),
                    new ImportSummary(0, 0, 0, null, false, "", null, List.of(), List.of()), null, null);
        }
    }

    /** Saves an imported deck, then does what the player asked: puts it on a seat, opens it, or adds it to the open deck. */
    private synchronized void commit(final BrowserChannel channel, final ImportCommit c) {
        final LegalityScope check = check(c.format(), c.cardPool(), c.unrestricted());
        final Deck deck = DeckImport.read(c.text(), check).deck();
        if (c.action() == FromBrowser.ImportAction.add || c.action() == FromBrowser.ImportAction.replace) {
            if (editor != null) {
                final String refused = c.action() == FromBrowser.ImportAction.add ? editor.addAll(deck) : editor.replaceAll(deck);
                if (refused != null) {
                    channel.send(new Notice(refused, null, false));
                }
                channel.send(new EditorMessage(editor.state()));
            }
            return;
        }
        save(channel, deck, check, c);
    }

    /** Saves a deck file as it is, without the importer, because it is already a deck. */
    private synchronized void addFile(final BrowserChannel channel, final DeckFile f) {
        final FileDeck read;
        try {
            read = readFile(f.text());
        } catch (final RuntimeException e) {
            netLog.warn(e, "A deck file could not be read");
            channel.send(new Notice(Localizer.getInstance().getMessage("lblWebDeckFileUnreadable"), null, true));
            return;
        }
        if (read == null || read.deck().isEmpty()) {
            channel.send(new Notice(Localizer.getInstance().getMessage("lblWebDeckFileUnreadable"), null, true));
            return;
        }
        final ImportCommit c = new ImportCommit(f.text(), read.deck().getName(), f.format(), null, false, FromBrowser.ImportAction.save, null, null, f.clash());
        if (save(channel, read.deck(), check(f.format(), null, false), c) && read.leftOut() > 0) {
            channel.send(new Notice(Localizer.getInstance().getMessage("lblWebDeckFileLeftOut", read.leftOut()), null, false));
        }
    }

    /** Null when the text is not a deck file. */
    static FileDeck readFile(final String text) {
        final Deck deck = parse(text);
        if (deck == null) {
            return null;
        }
        int leftOut = 0;
        for (final Map.Entry<DeckSection, CardPool> part : deck) {
            // Forge reads a card it does not have as a placeholder that no game can play
            for (final PaperCard card : part.getValue().toFlatList()) {
                if (card.getRules() != null && card.getRules().isUnsupported()) {
                    part.getValue().remove(card);
                    leftOut++;
                }
            }
        }
        return new FileDeck(deck, leftOut);
    }

    /** Returns false when nothing was saved: the name is not valid, or it is taken and the browser must be asked. */
    private boolean save(final BrowserChannel channel, final Deck deck, final LegalityScope check, final ImportCommit c) {
        final String nameProblem = DeckStore.nameProblem(c.name());
        if (nameProblem != null) {
            channel.send(new Notice(nameProblem, null, false));
            return false;
        }
        deck.setName(c.name().trim());
        if (c.url() != null) {
            deck.setSourceUrl(c.url());
        }
        final String key = host.getAsBoolean() ? saveOnHost(channel, deck, check, c) : saveOnDevice(channel, deck, check, c);
        if (key == null) {
            return false;
        }
        switch (c.action()) {
            case use -> {
                lobby.sendDecks(channel);
                if (c.seat() == null || !atTable.getAsBoolean()) {
                    return true;
                }
                if (DeckStore.family(check.format()) != DeckStore.family(lobby.format())) {
                    channel.send(new Notice(Localizer.getInstance().getMessage("lblWebEditorNotSeated"), Localizer.getInstance().getMessage("lblWebEditorFormatMismatch", check.format(), lobby.format()), false));
                    return true;
                }
                final String listed = relisted(key, deck);
                ui.invokeInEdtLater(() -> {
                    lobby.setDeck(c.seat(), listed);
                    lobby.sendState(channel);
                });
            }
            case edit -> {
                lobby.sendDecks(channel);
                open(new EditorOpen(relisted(key, deck), null, c.seat(), false), check, channel);
                if (editor != null) {
                    channel.send(new EditorMessage(editor.state()));
                }
            }
            default -> lobby.sendDecks(channel);
        }
        return true;
    }

    /** Registers a saved deck's key again after the list was rebuilt, in case the rebuild left it out. Brawl drops illegal decks, for example. */
    private String relisted(final String key, final Deck deck) {
        if (key.startsWith(DeckCatalog.DEVICE + ":")) {
            return lobby.catalog().adopt(DeckCatalog.DEVICE, key.substring(DeckCatalog.DEVICE.length() + 1), deck);
        }
        return lobby.catalog().adopt(key.substring(0, key.indexOf(':')), "", deck);
    }

    /** Saves to the host's decks, or to the decks loaded from links. Returns null when the name is taken and the browser must be asked. */
    private String saveOnHost(final BrowserChannel channel, final Deck deck, final LegalityScope check, final ImportCommit c) {
        final boolean linked = c.url() != null;
        final IStorage<Deck> storage = linked ? DeckUrlLoader.getStorage() : storages.of(check.format());
        synchronized (DeckCatalog.DECKS) {
            final String taken = DeckStore.taken(storage, deck.getName());
            if (taken != null && c.clash() == null) {
                channel.send(new NameTaken(deck.getName()));
                return null;
            }
            if (taken != null && c.clash() == FromBrowser.Clash.keep) {
                deck.setName(DeckStore.freeName(storage, deck.getName(), null));
            } else if (taken != null) {
                storage.delete(taken);
            }
            storage.add(deck);
        }
        return lobby.catalog().adopt(linked ? DeckCatalog.LINKED : DeckCatalog.MINE, "", deck);
    }

    private String saveOnDevice(final BrowserChannel channel, final Deck deck, final LegalityScope check, final ImportCommit c) {
        String id = null;
        for (final Map.Entry<String, OnDevice> e : device.entrySet()) {
            if (DeckStore.sameFile(e.getValue().deck().getName(), deck.getName())) {
                id = e.getKey();
            }
        }
        if (id != null && c.clash() == null) {
            channel.send(new NameTaken(deck.getName()));
            return null;
        }
        if (id != null && c.clash() == FromBrowser.Clash.keep) {
            final List<String> onDevice = device.values().stream().map(d -> d.deck().getName()).toList();
            deck.setName(DeckStore.freeName(onDevice, deck.getName(), null));
            id = null;
        }
        final String kept = id == null ? UUID.randomUUID().toString() : id;
        sendDeviceDeck(kept, text(deck), check.format());
        return lobby.catalog().adopt(DeckCatalog.DEVICE, kept, deck);
    }

    /** Stores a guest's deck in the lobby's list and sends it to the guest's browser, which saves it. */
    private void sendDeviceDeck(final String id, final String text, final GameType format) {
        if (text == null) {
            device.remove(id);
        } else {
            device.put(id, new OnDevice(parse(text), format));
        }
        // A pool that the browser has not yet confirmed is resent in its current form, so a deck built from it is not lost
        final EventPool p = pendingPool;
        if (p != null && p.id().equals(id)) {
            pendingPool = text == null ? null : new EventPool(id, parse(text), format);
        }
        final BrowserChannel b = channelFor.get();
        if (b != null) {
            b.send(new DeviceDeck(id, text, format.name()));
        }
    }

    public void attach(final BrowserChannel channel) {
        channelFor.set(channel);
    }

    private void keepDeviceDecks(final List<DeviceDeckText> decks) {
        final EventPool p = pendingPool;
        if (p != null && decks.stream().anyMatch(d -> p.id().equals(d.id()))) {
            pendingPool = null;
        } else if (p != null) {
            sendDeviceDeck(p.id(), text(p.deck()), p.format());
        }
        for (final DeviceDeckText d : decks) {
            try {
                // Text with no deck sections, as a hand-edited backup can hold, parses to no deck
                final Deck deck = parse(d.text());
                if (deck == null) {
                    netLog.warn("A deck kept in a guest's browser is not a deck: {}", d.id());
                    continue;
                }
                device.put(d.id(), new OnDevice(deck, gameType(d.format())));
            } catch (final RuntimeException e) {
                netLog.warn(e, "A deck kept in a guest's browser could not be read");
            }
        }
    }

    /** A deck as the text of a .dck file. The serializer ends some lines with Windows line endings and some with plain ones, so all are made plain. */
    static String text(final Deck deck) {
        return String.join("\n", DeckSerializer.serializeDeck(deck)).replace("\r\n", "\n");
    }

    private static Deck parse(final String text) {
        return DeckSerializer.fromSections(FileSection.parseSections(Arrays.asList(text.split("\r?\n"))));
    }

    private static Map<String, Integer> counts(final List<CountedName> names) {
        final Map<String, Integer> out = new LinkedHashMap<>();
        if (names != null) {
            names.forEach(n -> out.merge(n.name(), n.count(), Integer::sum));
        }
        return out;
    }

    private static LegalityScope check(final String format, final String cardPool, final boolean unrestricted) {
        if (unrestricted) {
            return LegalityScope.none();
        }
        final GameType type = gameType(format);
        final GameFormat pool = type == GameType.Constructed && cardPool != null ? FModel.getFormats().getFormat(cardPool) : null;
        return LegalityScope.of(type, pool);
    }

    private static GameType gameType(final String name) {
        return EnumUtils.getEnum(GameType.class, name, GameType.Constructed);
    }

    /** A change to the open deck. It returns why it was refused, or null. */
    private interface Change<T> {
        String run(DeckWorkspace decks, BrowserChannel channel, T command);
    }

    private record EventPool(String id, Deck deck, GameType format) {
    }

    /** A deck file's deck, and how many of its cards Forge does not have. */
    record FileDeck(Deck deck, int leftOut) {
    }
}
