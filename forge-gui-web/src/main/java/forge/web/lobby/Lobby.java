package forge.web.lobby;

import forge.ai.AiProfileUtil;
import forge.deck.CardPool;
import forge.deck.CommanderOptions;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.deck.FormatChoices;
import forge.game.GameFormat;
import forge.game.GameType;
import forge.gamemodes.match.GameLobby;
import forge.gamemodes.match.GameLobby.GameLobbyData;
import forge.gamemodes.match.LobbySlot;
import forge.gamemodes.match.LobbySlotType;
import forge.gamemodes.net.event.UpdateLobbyPlayerEvent;
import forge.gamemodes.net.server.ServerGameLobby;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.util.ImageUtil;
import forge.util.Localizer;
import forge.util.MyRandom;
import forge.web.WebGuiBase;
import forge.web.deck.DeckCatalog;
import forge.web.deck.DeckFacts;
import forge.web.deck.ExtraSections;
import forge.web.deck.ExtraSections.Extra;
import forge.web.images.SkinSprites;
import forge.web.match.NetplaySeat;
import forge.web.protocol.BrowserChannel;
import forge.web.protocol.FromBrowser.*;
import forge.web.protocol.ToBrowser;
import forge.web.protocol.ToBrowser.*;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Match setup, read from the engine's lobby. A browser changes its own seat as a client, and the host sets up the rest of the table.
 * This class builds the deck lists a seat may choose from and sends each seat's chosen deck to the table. It sets the format, card
 * pool, variants and teams, adds and changes seats, and keeps a hosted table's seats so that the next table starts the same way.
 */
public final class Lobby {
    private static final int MOST_TEAMS = 4;
    private static final int GIANT_SEATS = 4;
    private final DeckCatalog catalog = new DeckCatalog();
    /** This browser's choices for each seat whose deck it sends, by seat. Only the host UI thread and settle() change them. */
    private final Map<Integer, SeatChoice> choices = new ConcurrentHashMap<>();
    private final NetplaySeat local;
    private final LimitedEvents events;
    /** True once the game was opened for others to join. Only then is a link worth showing. */
    private volatile boolean shareable;
    /** Whether this browser belongs to a guest. A guest's own decks are the ones its browser stores. */
    private final BooleanSupplier guest;
    /** The UI whose thread is the only one that changes the table. */
    private final WebGuiBase ui;
    /** A guest's decks, keyed by the id its browser stores each one under. */
    private final Map<String, DeckCatalog.OnDevice> device;
    /** The format that the start page's deck finder lists. It is read only while no table is open, so it never affects a seat's deck. */
    private volatile GameType browseFormat = GameType.Constructed;
    /** Counts the tables this browser has sat at, so work started at one table can tell that it is now at another. */
    private volatile int table;
    /** Held while a deck list or the table is built and sent. Two threads then cannot both send the same list, or send an older table after a newer one. */
    private final Object sentLock = new Object();
    private BrowserChannel decksSentTo;
    private Decks decksSent;
    /** The format, card pool and rules this browser's deck list was last built for, recorded by {@link #settle()}. */
    private GameType seenFormat;
    private String seenCardPool;
    private String seenRules;
    /** Whether another player had a seat when the deck list was last built, so their arrival is noticed only once. */
    private volatile boolean othersSeated;

    public Lobby(final NetplaySeat local, final BooleanSupplier guest, final Map<String, DeckCatalog.OnDevice> device, final WebGuiBase ui) {
        this.local = local;
        this.ui = ui;
        this.events = new LimitedEvents(this, local);
        this.guest = guest;
        this.device = device;
    }

    public void setShareable(final boolean value) {
        shareable = value;
    }

    /** For the host, the server's own lobby, whose client copy is one update behind it. For a guest, the client copy. */
    GameLobby view() {
        final GameLobby hosted = local.hostedLobby();
        return hosted != null ? hosted : local.clientLobby();
    }

    ServerGameLobby host() {
        return local.hostedLobby();
    }

    /** Read from the lobby's variants, because the game type is not in the serialised data and stays Constructed on a client. */
    public GameType format() {
        final GameLobby lobby = view();
        if (lobby == null) {
            return browseFormat;
        }
        // A Limited table plays event decks, which are checked the same way startGame checks them
        if (limited(lobby)) {
            return GameType.Draft;
        }
        return lobby.getFormatChoice() instanceof GameType type ? type : GameType.Constructed;
    }

    public void setBrowseFormat(final GameType format) {
        browseFormat = format;
    }

    /** The seat this browser sits in, or -1 before it has one. */
    public int mySeat() {
        return local.webSeat();
    }

    public int table() {
        return table;
    }

    public DeckCatalog catalog() {
        return catalog;
    }

    public LimitedEvents events() {
        return events;
    }

    /** The card pool every deck must come from, or null. Only Constructed has one. */
    public GameFormat cardPool() {
        final GameLobby lobby = view();
        return lobby == null ? null : lobby.getFormat();
    }

    /** The card pool belongs to the game, so only the host sets it, and only for Constructed. */
    public void setCardPool(final String name) {
        final ServerGameLobby lobby = host();
        if (lobby == null || format() != GameType.Constructed) {
            return;
        }
        final GameFormat pool = name == null ? null : FModel.getFormats().getFormat(name);
        lobby.setFormatChoice(pool == null ? GameType.Constructed : pool);
        lobby.saveSetup();
    }

    /** Skips the send when this browser already has this exact list. The list is always rebuilt, because refresh also rebuilds the keys that deck lookups read. */
    public void sendDecks(final BrowserChannel to) {
        settle();
        sendBuilt(to);
    }

    /**
     * Like sendDecks, but the list is built and sent through build, where it may wait in a queue. Any table changes that the new list
     * needs are made now, in order with the browser's other commands, so a Ready pressed after the request is not undone by a later list.
     */
    public void queueDecks(final BrowserChannel to, final Executor build) {
        settle();
        build.execute(() -> sendBuilt(to));
    }

    private void sendBuilt(final BrowserChannel to) {
        synchronized (sentLock) {
            final Decks out = decks();
            if (to == decksSentTo && out.equals(decksSent)) {
                return;
            }
            decksSentTo = to;
            decksSent = out;
            to.send(out);
        }
    }

    /** Every deck this format can be played with. It is rebuilt each time because the pool differs per format. */
    private Decks decks() {
        synchronized (DeckCatalog.DECKS) {
            final GameFormat cardPool = cardPool();
            return new Decks(catalog.refresh(format(), cardPool, guest.getAsBoolean(), device, eventFilter()),
                    cardPool == null ? null : cardPool.getName(), guest.getAsBoolean() ? null
                            : new ToBrowser.DeckGenOptions(FModel.getPreferences().getPrefBoolean(FPref.DECKGEN_SINGLETONS),
                                    FModel.getPreferences().getPrefBoolean(FPref.DECKGEN_ARTIFACTS)));
        }
    }

    /** Updates the table to the format, card pool and rules that a list is about to be built for. */
    private void settle() {
        final boolean newPool;
        final boolean newRules;
        synchronized (DeckCatalog.DECKS) {
            final GameType format = format();
            // A deck key names a deck in one format's lists, so it means nothing in another format
            if (seenFormat != null && format != seenFormat) {
                choices.replaceAll((i, c) -> c.withDeckKey(null));
            }
            newPool = format == seenFormat && newCardPool();
            newRules = format != seenFormat || newRules();
            seenFormat = format;
            seenCardPool = cardPoolName();
            seenRules = rules();
        }
        // Any thread may ask for the list, but the table changes it implies are made only on the host UI thread
        ui.invokeInEdtNow(() -> {
            if (newPool) {
                dealGeneratorsAgain();
            }
            settleReady();
            if (newRules) {
                composeMine();
            }
        });
    }

    /** Runs a change to the table on the host UI thread, and returns the change's result. */
    String onTable(final Supplier<String> change) {
        final String[] answer = new String[1];
        ui.invokeInEdtAndWait(() -> answer[0] = change.get());
        return answer[0];
    }

    /** A seat alone at the table is ready once it has a deck, or at once if the format deals the deck. With another player seated, each player presses Ready. */
    private void settleReady() {
        final GameLobby lobby = view();
        final int mine = local.webSeat();
        // An event's seats press Ready before any deck exists
        if (lobby == null || mine < 0 || mine >= lobby.getNumberOfSlots() || limited(lobby)) {
            return;
        }
        final boolean others = withOthers(lobby);
        final boolean arrived = others != othersSeated;
        othersSeated = others;
        final boolean ready = !others && (format().isAutoGenerated() || deckAt(mine) != null);
        if ((arrived || ready) && lobby.getSlot(mine).isReady() != ready) {
            local.updateOwnSeat(UpdateLobbyPlayerEvent.isReadyUpdate(ready));
        }
    }

    /** Whether a player at another browser holds a seat at this Constructed table. An event handles Ready in its own way. */
    private boolean withOthers(final GameLobby lobby) {
        if (limited(lobby)) {
            return false;
        }
        for (int i = 0; i < lobby.getNumberOfSlots(); i++) {
            final LobbySlotType type = lobby.getSlot(i).getType();
            if (i != local.webSeat() && type != LobbySlotType.AI && type != LobbySlotType.OPEN) {
                return true;
            }
        }
        return false;
    }

    /** A slot keeps the generated deck that was built from the old card pool, so a new pool deals this browser's seats the same generator again. */
    private void dealGeneratorsAgain() {
        final GameLobby lobby = view();
        if (lobby == null) {
            return;
        }
        for (final Map.Entry<Integer, SeatChoice> e : Map.copyOf(choices).entrySet()) {
            final int i = e.getKey();
            final String key = e.getValue().deckKey();
            if (i < lobby.getNumberOfSlots() && key != null && key.startsWith("gen:") && dealsFor(lobby, i)) {
                setDeck(i, key);
            }
        }
    }

    /** The event whose decks are the only ones the finder lists, or null for all events' decks. Only a Limited table has one. */
    private String eventFilter() {
        final GameLobby lobby = view();
        final GameLobbyData data = lobby == null ? null : lobby.getData();
        return data != null && data.isLimitedMode() && data.isActiveConformance() ? data.getActiveEventId() : null;
    }

    /** The casual variants that are on, which seat is the archenemy, and the event filter. These decide which decks a seat may use. */
    private String rules() {
        final GameLobby lobby = view();
        if (lobby == null) {
            return "";
        }
        final StringBuilder sb = new StringBuilder(variantsOn(lobby).toString()).append(eventFilter());
        for (int i = 0; i < lobby.getNumberOfSlots(); i++) {
            sb.append(lobby.getSlot(i).isArchenemy() ? i : "");
        }
        return sb.toString();
    }

    private static List<String> variantsOn(final GameLobby lobby) {
        final List<String> out = new ArrayList<>();
        for (final GameType v : FormatChoices.VARIANTS) {
            if (lobby.hasVariant(v)) {
                out.add(v.name());
            }
        }
        return out;
    }

    /** Whether this browser sends the seat's deck. It sends its own, and the computer seats' decks when it runs the game. */
    private boolean dealsFor(final GameLobby lobby, final int index) {
        return index == local.webSeat() || (host() != null && index < lobby.getNumberOfSlots()
                && lobby.getSlot(index).getType() == LobbySlotType.AI);
    }

    private static boolean forComputer(final GameLobby lobby, final int index) {
        return lobby.getSlot(index).getType() == LobbySlotType.AI;
    }

    private static List<DeckSection> sectionsFor(final GameLobby lobby, final int index) {
        final List<DeckSection> out = new ArrayList<>();
        if (lobby.hasVariant(GameType.Planechase)) {
            out.add(DeckSection.Planes);
        }
        if (lobby.hasVariant(GameType.ArchenemyRumble)
                || (lobby.hasVariant(GameType.Archenemy) && lobby.getSlot(index).isArchenemy())) {
            out.add(DeckSection.Schemes);
        }
        if (lobby.hasVariant(GameType.Vanguard)) {
            out.add(DeckSection.Avatar);
        }
        return out;
    }

    private SeatChoice choice(final int index) {
        return choices.getOrDefault(index, SeatChoice.NONE);
    }

    private void change(final int index, final UnaryOperator<SeatChoice> how) {
        choices.compute(index, (i, c) -> how.apply(c == null ? SeatChoice.NONE : c));
    }

    private void composeMine() {
        final GameLobby lobby = view();
        for (int i = 0; lobby != null && i < lobby.getNumberOfSlots(); i++) {
            if (dealsFor(lobby, i)) {
                compose(i);
            }
        }
    }

    /** A seat that lacks a section its variants need gets generated planes or schemes, or a random avatar. */
    private void fillDefaults(final GameLobby lobby, final int index) {
        for (final DeckSection section : sectionsFor(lobby, index)) {
            if (!choice(index).extras().containsKey(section)) {
                final Extra fallback = ExtraSections.resolveExtra(section,
                        section == DeckSection.Avatar ? ExtraSections.RANDOM : ExtraSections.GENERATE, forComputer(lobby, index));
                if (fallback != null) {
                    change(index, c -> c.withExtra(section, fallback));
                }
            }
        }
    }

    /** Sends a seat's main deck and extras in one update, because a full deck update replaces every section. */
    private void compose(final int index) {
        final GameLobby lobby = view();
        if (lobby == null || index >= lobby.getNumberOfSlots() || !dealsFor(lobby, index)) {
            return;
        }
        fillDefaults(lobby, index);
        final Deck main = catalog.deck(key(index));
        final boolean dealt = format().isAutoGenerated();
        Deck deck = null;
        if (main != null || dealt) {
            final List<PaperCard> leaders = main == null ? null : pickedCommanders(index, main);
            deck = main == null ? new Deck(Localizer.getInstance().getMessage("lblWebLobbyDealtDeck"))
                    : leaders == null ? new Deck(main) : CommanderOptions.withCommanders(main, leaders);
            for (final DeckSection section : sectionsFor(lobby, index)) {
                final Extra extra = choice(index).extras().get(section);
                final CardPool cards = extra == null ? null : extra.cards() != null ? extra.cards()
                        : main == null ? null : main.get(section);
                if (cards != null) {
                    deck.putSection(section, cards);
                }
            }
        }
        final Deck sent = deck;
        change(index, c -> c.withComposed(sent));
        final String planes = labelOf(index, DeckSection.Planes);
        final String schemes = labelOf(index, DeckSection.Schemes);
        final String avatar = labelOf(index, DeckSection.Avatar);
        if (index == local.webSeat()) {
            local.updateOwnSeat(UpdateLobbyPlayerEvent.deckUpdate(deck));
            // Sent only on a change, because the chat reports each one
            final boolean ready = deck != null && !withOthers(lobby);
            if (lobby.getSlot(index).isReady() != ready) {
                local.updateOwnSeat(UpdateLobbyPlayerEvent.isReadyUpdate(ready));
            }
            local.updateOwnSeat(UpdateLobbyPlayerEvent.setDeckSchemePlaneVanguard(
                    deck == null ? null : deck.getName(), schemes, planes, avatar));
        } else {
            final LobbySlot slot = lobby.getSlot(index);
            slot.setDeck(deck);
            slot.setPlanarDeckName(planes);
            slot.setSchemeDeckName(schemes);
            slot.setAvatarVanguard(avatar);
            local.pushLobby();
        }
    }

    private String labelOf(final int index, final DeckSection section) {
        final Extra extra = choice(index).extras().get(section);
        return extra == null ? null : extra.label();
    }

    /** A seat's planar deck, scheme deck or avatar, chosen by a key from {@link #extraChoices}. */
    public void setSeatExtra(final int index, final String sectionName, final String choice) {
        final GameLobby lobby = view();
        final DeckSection section = extraSection(sectionName);
        if (lobby == null || section == null || !dealsFor(lobby, index)) {
            return;
        }
        final Extra extra = ExtraSections.resolveExtra(section, choice, forComputer(lobby, index));
        if (extra != null) {
            change(index, c -> c.withExtra(section, extra));
            compose(index);
        }
    }

    public ToBrowser.ExtraChoices extraChoices(final int index, final String sectionName) {
        final GameLobby lobby = view();
        final DeckSection section = extraSection(sectionName);
        if (lobby == null || section == null || index < 0 || index >= lobby.getNumberOfSlots()) {
            return null;
        }
        return new ToBrowser.ExtraChoices(section.name(), index,
                ExtraSections.extraChoices(section, forComputer(lobby, index), catalog.deck(key(index))));
    }

    private static DeckSection extraSection(final String name) {
        for (final DeckSection s : List.of(DeckSection.Planes, DeckSection.Schemes, DeckSection.Avatar)) {
            if (s.name().equals(name)) {
                return s;
            }
        }
        return null;
    }

    /** A casual variant belongs to the game, so only the host turns one on or off. The engine removes any variants that it excludes. */
    public void setVariant(final String id, final boolean on, final boolean open) {
        final ServerGameLobby lobby = host();
        if (lobby == null) {
            return;
        }
        for (final GameType v : FormatChoices.VARIANTS) {
            if (v.name().equals(id)) {
                if (on) {
                    lobby.applyVariant(v);
                } else {
                    lobby.removeVariant(v);
                }
                // The archenemy's role or the giant's pairs set the teams. Without them, every seat goes back to a team of its own
                if ((v == GameType.Archenemy || v == GameType.TwoHeadedGiant) && !on) {
                    for (int i = 0; i < lobby.getNumberOfSlots(); i++) {
                        lobby.getSlot(i).setTeam(i);
                    }
                }
                if (fixedPairs(lobby)) {
                    setPlayerCount(GIANT_SEATS, open);
                }
                lobby.saveSetup();
                local.pushLobby();
                return;
            }
        }
    }

    /** Opens a new table with the format, card pool and variants that the host last chose, here or in desktop Forge. */
    public void restoreSetup(final boolean open) {
        final ServerGameLobby lobby = host();
        if (lobby == null) {
            return;
        }
        lobby.restoreSetup();
        if (fixedPairs(lobby)) {
            setPlayerCount(GIANT_SEATS, open);
        }
        local.pushLobby();
    }

    /** Whether seats can be put on teams. They cannot when a draft's pairings or the archenemy's role already decide the sides. */
    private static boolean teamsAllowed(final GameLobby lobby) {
        return !limited(lobby) && !lobby.hasVariant(GameType.Archenemy) && !lobby.hasVariant(GameType.ArchenemyRumble);
    }

    /** Two-Headed Giant is four seats paired in order, so the seat count and the teams are shown but cannot be changed. */
    private static boolean fixedPairs(final GameLobby lobby) {
        return lobby.hasVariant(GameType.TwoHeadedGiant);
    }

    /** How many teams the table is split into, or 0 for a free-for-all, where every seat is a team of its own. */
    private static int teams(final GameLobby lobby) {
        if (!teamsAllowed(lobby)) {
            return 0;
        }
        final Set<Integer> seen = new HashSet<>();
        boolean shared = false;
        int most = 0;
        for (int i = 0; i < lobby.getNumberOfSlots(); i++) {
            final int team = lobby.getSlot(i).getTeam();
            shared |= !seen.add(team);
            most = Math.max(most, team);
        }
        return shared ? Math.max(2, most + 1) : 0;
    }

    /** The team counts the host may split the seats into. There are fewer teams than seats, because one team per seat is a free-for-all. */
    private static List<Integer> teamChoices(final GameLobby lobby) {
        final List<Integer> out = new ArrayList<>();
        for (int n = 2; teamsAllowed(lobby) && !fixedPairs(lobby) && n < lobby.getNumberOfSlots() && n <= MOST_TEAMS; n++) {
            out.add(n);
        }
        return out;
    }

    /** Deals the seats into that many teams in turn. With 0, every seat goes back to a team of its own. */
    public void setTeams(final int count) {
        final ServerGameLobby lobby = host();
        if (lobby == null || fixedPairs(lobby) || (count != 0 && !teamChoices(lobby).contains(count))) {
            return;
        }
        for (int i = 0; i < lobby.getNumberOfSlots(); i++) {
            lobby.getSlot(i).setTeam(count == 0 ? i : i % count);
        }
        local.pushLobby();
    }

    /** Moves a seat to another of the table's teams. A player may move their own seat, and the host may move any seat. */
    public void setTeam(final int index, final int team) {
        final GameLobby table = view();
        if (table == null || fixedPairs(table) || index < 0 || index >= table.getNumberOfSlots() || team < 0 || team >= teams(table)) {
            return;
        }
        if (index == local.webSeat()) {
            local.updateOwnSeat(UpdateLobbyPlayerEvent.teamUpdate(team));
        } else if (host() != null) {
            host().getSlot(index).setTeam(team);
            local.pushLobby();
        }
    }

    /** The team that a newly added seat joins: the smallest team, or in a free-for-all a team that nobody is on. */
    private static int teamForNewSeat(final GameLobby lobby, final int teams) {
        final Map<Integer, Integer> sizes = new HashMap<>();
        for (int i = 0; i < lobby.getNumberOfSlots(); i++) {
            sizes.merge(lobby.getSlot(i).getTeam(), 1, Integer::sum);
        }
        int best = 0;
        for (int team = 0; team < (teams > 0 ? teams : lobby.getNumberOfSlots() + 1); team++) {
            if (teams == 0 && !sizes.containsKey(team)) {
                return team;
            }
            if (sizes.getOrDefault(team, 0) < sizes.getOrDefault(best, 0)) {
                best = team;
            }
        }
        return best;
    }

    /** Moves the archenemy's role. The engine's lobby keeps one archenemy and sets the teams from it. */
    public void setArchenemy(final int index) {
        final ServerGameLobby lobby = host();
        if (lobby == null || !lobby.hasVariant(GameType.Archenemy) || index < 0 || index >= lobby.getNumberOfSlots()) {
            return;
        }
        lobby.applyToSlot(index, UpdateLobbyPlayerEvent.create(null, null, -1, -1, -1, true,
                lobby.getSlot(index).isDevMode(), null, null));
        local.pushLobby();
    }

    /** Downloads a net deck category and adds it to the catalogue. NetDeckCategory asks the host's browser which category to load. */
    public void loadNetDecks(final BrowserChannel to) {
        catalog.loadNetDecks(format());
        sendDecks(to);
    }

    public ToBrowser.DeckMatches deckMatches(final DeckQueryKind kind, final String value) {
        return new ToBrowser.DeckMatches(kind, value, catalog.matching(kind, value));
    }

    public DeckDetailsMessage deckDetails(final String key) {
        final DeckDetails details = catalog.details(key, format());
        return details == null ? null : new DeckDetailsMessage(details);
    }

    public void sendState(final BrowserChannel to) {
        synchronized (sentLock) {
            to.send(state());
        }
    }

    private LobbyMessage state() {
        synchronized (DeckCatalog.DECKS) {
            final GameLobby lobby = view();
            if (lobby == null) {
                return new LobbyMessage(null);
            }
            final List<Seat> seats = new ArrayList<>();
            for (int i = 0; i < lobby.getNumberOfSlots(); i++) {
                seats.add(seat(lobby, i));
            }
            final List<String> illegal = new ArrayList<>();
            final List<String> problems = problems(illegal);
            final GameFormat cardPool = cardPool();
            // Only the machine running the game can start it. Everyone else waits for the host
            return new LobbyMessage(new LobbyTable(local.isHost(), local.webSeat(), shareable, format().name(),
                    cardPool == null ? null : cardPool.getName(),
                    FormatChoices.VARIANTS.stream().map(LobbyFormats::explainedVariant).toList(), variantsOn(lobby),
                    lobby.getSlotLimit(), FModel.getPreferences().getPrefInt(FPref.UI_MATCHES_PER_GAME), seats, problems,
                    local.isHost() && problems.isEmpty(), illegal, FModel.getPreferences().getPrefBoolean(FPref.ENFORCE_DECK_LEGALITY),
                    events.limitedTable(lobby), maxBracket(), overBracket(lobby), teams(lobby), fixedPairs(lobby),
                    local.isHost() ? teamChoices(lobby) : List.of(),
                    local.isHost() ? AiProfileUtil.getAvailableProfiles() : List.of()));
        }
    }

    private Seat seat(final GameLobby lobby, final int index) {
        final LobbySlot slot = lobby.getSlot(index);
        final Deck deck = deckAt(index);
        // A player may edit their own seat, and the host may also edit the computer's seats. Another player's seat belongs to that player
        final boolean mayEdit = index == local.webSeat() || (local.isHost() && slot.getType() == LobbySlotType.AI);
        final List<DeckSection> extras = sectionsFor(lobby, index);
        return new Seat(slot.getName(), slot.getType(), index == local.webSeat(), mayEdit,
                slot.isReady(), slot.getAvatarIndex(), slot.getSleeveIndex(), key(index),
                deck == null ? null : deck.getName(),
                deck == null ? 0 : DeckFacts.played(deck),
                deck == null ? "" : DeckFacts.colors(deck),
                deck == null || format().isAutoGenerated() ? null : DeckFacts.problemAt(deck, format(), cardPool()),
                // A deck with a card-art sleeve overrides the numbered sleeve, as in every other client
                deck == null ? "" : deck.getSleeveArtKey(),
                deck == null ? 0 : deck.getSleeveArtOffset(),
                lobby.hasVariant(GameType.ArchenemyRumble) ? "archenemy"
                        : lobby.hasVariant(GameType.Archenemy) ? (slot.isArchenemy() ? "archenemy" : "hero") : null,
                extras.contains(DeckSection.Planes) ? seatExtra(lobby, index, deck, DeckSection.Planes) : null,
                extras.contains(DeckSection.Schemes) ? seatExtra(lobby, index, deck, DeckSection.Schemes) : null,
                extras.contains(DeckSection.Avatar) ? seatExtra(lobby, index, deck, DeckSection.Avatar) : null,
                slot.isSittingOut(),
                deck == null || !format().getDeckFormat().hasCommander() || deck.getCommanders().isEmpty() ? null
                        : deck.getCommanders().get(0).getImageKey(false),
                deck == null || format() != GameType.Commander ? null : DeckFacts.bracketOf(deck),
                teams(lobby) > 0 ? slot.getTeam() : null, commanderChoices(index, mayEdit, deck),
                slot.getType() == LobbySlotType.AI ? aiProfile(slot) : null);
    }

    /** A computer seat with no playstyle of its own uses the one named in the settings, as GameLobby does. */
    private static String aiProfile(final LobbySlot slot) {
        return StringUtils.isEmpty(slot.getAiProfile()) ? FModel.getPreferences().getPref(FPref.UI_CURRENT_AI_PROFILE) : slot.getAiProfile();
    }

    /** The highest Commander bracket this table plays at, 1 to 4, or 5 for any. It is the host's own setting, stored as on desktop. */
    static int maxBracket() {
        return FModel.getPreferences().getPrefInt(FPref.DECKGEN_MAXIMUM_COMMANDER_BRACKET);
    }

    /** The decks above this table's bracket. They do not stop the match, but the host is asked before it starts. */
    private List<String> overBracket(final GameLobby lobby) {
        final List<String> out = new ArrayList<>();
        final int most = maxBracket();
        if (format() != GameType.Commander || most >= 5) {
            return out;
        }
        for (int i = 0; i < lobby.getNumberOfSlots(); i++) {
            final Deck deck = deckAt(i);
            if (lobby.getSlot(i).isSittingOut() || deck == null || DeckFacts.bracketOf(deck) <= most) {
                continue;
            }
            final String why = String.join(", ", DeckFacts.bracket(deck).reasons().stream()
                    .filter(r -> r.raises() > most).map(ToBrowser.BracketReason::brief).toList());
            out.add(Localizer.getInstance().getMessage("lblWebLobbyAboveBracket", String.valueOf(lobby.getSlot(i).getName()), most, why));
        }
        return out;
    }

    /** Sets the host's bracket preference, which netplay sends to every client with the lobby. Returns whether it changed. */
    public boolean setMaxBracket(final int bracket) {
        final var prefs = FModel.getPreferences();
        if (host() == null || bracket < 1 || bracket > 5 || prefs.getPrefInt(FPref.DECKGEN_MAXIMUM_COMMANDER_BRACKET) == bracket) {
            return false;
        }
        prefs.setPref(FPref.DECKGEN_MAXIMUM_COMMANDER_BRACKET, String.valueOf(bracket));
        prefs.save();
        local.pushLobby();
        return true;
    }

    /** Another browser's seat is described by the names it published. A random avatar is labelled "Random", so the drawn avatar stays hidden. */
    private SeatExtra seatExtra(final GameLobby lobby, final int index, final Deck deck, final DeckSection section) {
        final LobbySlot slot = lobby.getSlot(index);
        final String published = switch (section) {
            case Planes -> slot.getPlanarDeckName();
            case Schemes -> slot.getSchemeDeckName();
            default -> slot.getAvatarVanguard();
        };
        final Extra mine = dealsFor(lobby, index) ? choice(index).extras().get(section) : null;
        final String label = mine != null ? mine.label() : published;
        final boolean hidden = mine != null ? mine.hidden() : ExtraSections.randomLabel().equals(published);
        final CardPool cards = deck == null ? null : deck.get(section);
        final int count = cards == null ? 0 : cards.countAll();
        String detail = null;
        if (section == DeckSection.Avatar && count > 0 && !hidden) {
            final var rules = cards.iterator().next().getKey().getRules();
            detail = Localizer.getInstance().getMessage("lblWebLobbyAvatarMods", signed(rules.getHand()), signed(rules.getLife()));
        }
        // Before a seat has a deck, nothing has been dealt to it, so an empty section is not yet an error
        final String problem = deck == null ? null : section == DeckSection.Avatar ? (count == 0 ? Localizer.getInstance().getMessage("lblWebLobbyNoAvatar") : null)
                : ExtraSections.sectionProblem(section, cards);
        return new SeatExtra(label == null ? Localizer.getInstance().getMessage("lblNone") : label, count, detail, problem);
    }

    private static String signed(final int n) {
        return n < 0 ? "−" + (-n) : "+" + n;
    }

    private String key(final int index) {
        return choice(index).deckKey();
    }

    /** A deck that this browser chose for a seat is found by its key. Another player's deck arrives with their seat. */
    private Deck deckAt(final int index) {
        final Deck made = choice(index).composed();
        if (made != null) {
            return made;
        }
        final Deck chosen = catalog.deck(key(index));
        if (chosen != null) {
            return chosen;
        }
        final GameLobby lobby = view();
        return lobby != null && index >= 0 && index < lobby.getNumberOfSlots() ? lobby.getSlot(index).getDeck() : null;
    }

    /** What stops the match from starting: all of startGame's own refusals, then an open seat, which only the page refuses. startGame still runs as the final check. */
    public List<String> problems() {
        return problems(new ArrayList<>());
    }

    /** Adds to illegal the decks that break their format's rules. When deck legality is enforced, startGame asks the host whether to play them anyway. */
    private List<String> problems(final List<String> illegal) {
        synchronized (DeckCatalog.DECKS) {
            final GameLobby lobby = view();
            if (lobby == null) {
                return new ArrayList<>();
            }
            final List<String> out = new ArrayList<>(lobby.startProblems(this::deckAt));
            illegal.addAll(lobby.legalityProblems(this::deckAt));
            for (int i = 0; i < lobby.getNumberOfSlots(); i++) {
                final LobbySlot slot = lobby.getSlot(i);
                // A seat that sits out is not in the match, so nothing about it stops the match
                if (!slot.isSittingOut() && slot.getType() == LobbySlotType.OPEN) {
                    out.add(Localizer.getInstance().getMessage("lblWebLobbySeatOpen"));
                }
            }
            return out;
        }
    }

    /** Drops the deck choices, because a new lobby's slots hold no decks and the two must agree. */
    public void forget() {
        table++;
        choices.clear();
        // settle() reads the seen values under the same lock, from whichever thread asked for the list
        synchronized (DeckCatalog.DECKS) {
            seenFormat = null;
            seenCardPool = null;
            seenRules = null;
            othersSeated = false;
        }
    }

    /** The table this browser hosts, as it is now, or null when it hosts none. */
    public Kept keep() {
        final ServerGameLobby lobby = host();
        if (lobby == null) {
            return null;
        }
        final List<KeptSeat> seats = new ArrayList<>();
        for (int i = 0; i < lobby.getNumberOfSlots(); i++) {
            final LobbySlot slot = lobby.getSlot(i);
            seats.add(new KeptSeat(slot.getType(), slot.getName(), slot.getAvatarIndex(), slot.getSleeveIndex(), slot.getTeam(), slot.getAiProfile()));
        }
        return new Kept(teams(lobby), seats, Map.copyOf(choices));
    }

    /**
     * Sets up a new table's seats the same way as a kept one. Its format and variants come from {@link #restoreSetup}. A seat that a person
     * held is left open for them to take again, with whatever deck they bring.
     */
    public void restore(final Kept kept) {
        final ServerGameLobby lobby = host();
        if (lobby == null) {
            return;
        }
        setPlayerCount(kept.seats().size(), false);
        // The archenemy's role sets the teams by itself
        final boolean teamed = kept.teams() > 0 && !lobby.hasVariant(GameType.Archenemy);
        if (teamed) {
            setTeams(kept.teams());
        }
        for (int i = 0; i < Math.min(kept.seats().size(), lobby.getNumberOfSlots()); i++) {
            final KeptSeat seat = kept.seats().get(i);
            if (seat.type() == LobbySlotType.AI) {
                if (lobby.getSlot(i).getType() != LobbySlotType.AI) {
                    aiSeat(i);
                }
                setName(i, seat.name());
                if (StringUtils.isNotEmpty(seat.aiProfile())) {
                    setAiProfile(i, seat.aiProfile());
                }
            } else if (i != local.webSeat()) {
                openSeat(i);
                continue;
            }
            setAvatar(i, seat.avatar());
            setSleeve(i, seat.sleeve());
            if (teamed) {
                setTeam(i, seat.team());
            }
        }
        for (int i = 0; i < lobby.getNumberOfSlots(); i++) {
            if (dealsFor(lobby, i) && kept.choices().containsKey(i)) {
                choices.put(i, kept.choices().get(i));
                compose(i);
            }
        }
        local.pushLobby();
    }

    /** A new format, card pool or variant can change which decks a seat may use, so the list is sent again, through build as for queueDecks. */
    public void relistDecks(final BrowserChannel to, final Executor build) {
        if (restrictionsChanged()) {
            queueDecks(to, build);
        }
    }

    /** True when the format, card pool or rules differ from those the last deck list was built for. */
    private boolean restrictionsChanged() {
        synchronized (DeckCatalog.DECKS) {
            return format() != seenFormat || newCardPool() || newRules();
        }
    }

    private boolean newCardPool() {
        return !Objects.equals(cardPoolName(), seenCardPool);
    }

    private boolean newRules() {
        return !rules().equals(seenRules);
    }

    private String cardPoolName() {
        final GameFormat pool = cardPool();
        return pool == null ? null : pool.getName();
    }

    /** The format belongs to the game, so only the host sets it. The choice is saved for the next table, as desktop saves it. */
    public void setFormat(final String id) {
        final ServerGameLobby lobby = host();
        for (final GameType wanted : LobbyFormats.FORMATS) {
            if (lobby != null && wanted.name().equals(id)) {
                applyFormat(wanted);
                lobby.saveSetup();
                return;
            }
        }
    }

    void applyFormat(final GameType wanted) {
        final ServerGameLobby lobby = host();
        if (lobby == null || wanted == format()) {
            return;
        }
        // Decks are cleared before the variants change. That change is announced at once, and the seats are then dealt their new decks
        choices.replaceAll((i, c) -> c.withDeckKey(null).withComposed(null));
        for (int i = 0; i < lobby.getNumberOfSlots(); i++) {
            lobby.getSlot(i).setDeck(null);
        }
        lobby.setFormatChoice(wanted);
        local.pushLobby();
    }

    static boolean limited(final GameLobby lobby) {
        final GameLobbyData data = lobby.getData();
        return data != null && data.isLimitedMode();
    }

    /** Seats stay unchanged while their players draft, because each player's seat is where their packs go. */
    public static boolean drafting(final ServerGameLobby lobby) {
        return lobby.getDraftHost() != null && !lobby.getDraftHost().isFinished();
    }

    /** Sets the host's match length preference. Returns whether it changed, because no lobby update reports it. */
    public boolean setMatchLength(final int games) {
        final var prefs = FModel.getPreferences();
        if (host() == null || (games != 1 && games != 3 && games != 5) || prefs.getPrefInt(FPref.UI_MATCHES_PER_GAME) == games) {
            return false;
        }
        prefs.setPref(FPref.UI_MATCHES_PER_GAME, String.valueOf(games));
        prefs.save();
        return true;
    }

    /** New seats are added at the end, open when others can join and AI otherwise. Removal takes open seats first, then AI seats, from the end, and never a person's seat. */
    public void setPlayerCount(final int count, final boolean open) {
        final ServerGameLobby lobby = host();
        if (lobby == null || drafting(lobby) || (fixedPairs(lobby) && count != GIANT_SEATS)) {
            return;
        }
        final int wanted = Math.min(lobby.getSlotLimit(), Math.max(2, count));
        while (lobby.getNumberOfSlots() < wanted) {
            final int before = lobby.getNumberOfSlots();
            addSeat(open);
            if (lobby.getNumberOfSlots() == before) {
                return;
            }
        }
        while (lobby.getNumberOfSlots() > wanted) {
            final int seat = lastSeatOf(lobby, LobbySlotType.OPEN) >= 0 ? lastSeatOf(lobby, LobbySlotType.OPEN) : lastSeatOf(lobby, LobbySlotType.AI);
            if (seat < 0) {
                return;
            }
            removeSeat(seat);
        }
        local.pushLobby();
    }

    private int lastSeatOf(final GameLobby lobby, final LobbySlotType type) {
        for (int i = lobby.getNumberOfSlots() - 1; i >= 0; i--) {
            if (i != local.webSeat() && lobby.getSlot(i).getType() == type) {
                return i;
            }
        }
        return -1;
    }

    private void addSeat(final boolean open) {
        final ServerGameLobby lobby = host();
        if (lobby != null && !drafting(lobby) && lobby.getNumberOfSlots() < lobby.getSlotLimit()) {
            // The engine puts a new seat on the team with its index number, which another seat may already use
            final int team = teamForNewSeat(lobby, teams(lobby));
            lobby.addSlot();
            lobby.getSlot(lobby.getNumberOfSlots() - 1).setTeam(team);
            if (open) {
                openSeat(lobby.getNumberOfSlots() - 1);
            } else {
                aiSeat(lobby.getNumberOfSlots() - 1);
            }
        }
    }

    /** Leaves a seat open for someone to join, instead of filling it with an AI. */
    public void openSeat(final int index) {
        final ServerGameLobby lobby = host();
        if (lobby != null && !drafting(lobby) && index != local.webSeat() && index < lobby.getNumberOfSlots()) {
            final LobbySlot slot = lobby.getSlot(index);
            slot.setType(LobbySlotType.OPEN);
            slot.setName(null);
            slot.setIsReady(false);
            slot.setDeck(null);
            // Whoever sits here brings their own choices, so this browser's choices for the seat are dropped
            choices.remove(index);
            local.pushLobby();
        }
    }

    public void aiSeat(final int index) {
        final ServerGameLobby lobby = host();
        if (lobby != null && !drafting(lobby) && index != local.webSeat() && index < lobby.getNumberOfSlots()) {
            final LobbySlot slot = lobby.getSlot(index);
            slot.setType(LobbySlotType.AI);
            slot.setName(lobby.randomName());
            slot.setIsReady(true);
            local.pushLobby();
            compose(index);
        }
    }

    private static List<String> seatNames(final GameLobby lobby, final int except) {
        final List<String> out = new ArrayList<>();
        for (int i = 0; i < lobby.getNumberOfSlots(); i++) {
            if (i != except && lobby.getSlot(i).getName() != null) {
                out.add(lobby.getSlot(i).getName());
            }
        }
        return out;
    }

    public void removeSeat(final int index) {
        final ServerGameLobby lobby = host();
        if (lobby != null && !drafting(lobby) && !fixedPairs(lobby) && index != local.webSeat() && lobby.getNumberOfSlots() > 2) {
            lobby.removeSlot(index);
            // The later seats move up by one, as the lobby's slots move
            final Map<Integer, SeatChoice> moved = new HashMap<>();
            choices.forEach((i, c) -> {
                if (i != index) {
                    moved.put(i > index ? i - 1 : i, c);
                }
            });
            choices.clear();
            choices.putAll(moved);
        }
    }

    /** The named seats of one kind at this table, such as those people hold from another browser or an app. */
    public List<LobbySlot> seatsOf(final LobbySlotType type) {
        final List<LobbySlot> out = new ArrayList<>();
        final GameLobby lobby = view();
        if (lobby != null) {
            for (int i = 0; i < lobby.getNumberOfSlots(); i++) {
                final LobbySlot slot = lobby.getSlot(i);
                if (slot.getType() == type && slot.getName() != null) {
                    out.add(slot);
                }
            }
        }
        return out;
    }

    /** A blank name gives a computer seat a new random name, and leaves a person's name unchanged. */
    public void setName(final int index, final String name) {
        if (name == null) {
            return;
        }
        if (index == local.webSeat()) {
            if (!name.isBlank()) {
                local.updateOwnSeat(UpdateLobbyPlayerEvent.nameUpdate(name.trim()));
            }
        } else if (host() != null && index >= 0 && index < host().getNumberOfSlots()
                && host().getSlot(index).getType() == LobbySlotType.AI
                && seatNames(host(), index).stream().noneMatch(name.trim()::equalsIgnoreCase)) {
            host().getSlot(index).setName(name.isBlank() ? host().randomName() : name.trim());
            local.pushLobby();
        }
    }

    public void setDeck(final int index, final String key) {
        final GameLobby lobby = view();
        if (lobby == null || index < 0 || index >= lobby.getNumberOfSlots()) {
            return;
        }
        change(index, c -> c.withDeckKey(key));
        compose(index);
    }

    /** Leads the seat's deck with one of the commanders it offers, for this table only. The saved deck is not changed. */
    public void setCommander(final int index, final int option) {
        final GameLobby lobby = view();
        final Deck main = catalog.deck(key(index));
        if (lobby == null || index < 0 || index >= lobby.getNumberOfSlots() || !dealsFor(lobby, index) || main == null) {
            return;
        }
        final List<CommanderOptions.Option> options = CommanderOptions.getOptions(main, format().getDeckFormat());
        if (option < 0 || option >= options.size()) {
            return;
        }
        change(index, c -> c.withCommanders(names(options.get(option).getCommanders())));
        compose(index);
    }

    private List<PaperCard> pickedCommanders(final int index, final Deck main) {
        final List<String> picked = choice(index).commanders();
        if (picked == null || !format().getDeckFormat().hasCommander()) {
            return null;
        }
        for (final CommanderOptions.Option option : CommanderOptions.getOptions(main, format().getDeckFormat())) {
            if (names(option.getCommanders()).equals(picked)) {
                return option.getCommanders();
            }
        }
        return null;
    }

    private static List<String> names(final List<PaperCard> cards) {
        return cards.stream().map(PaperCard::getName).toList();
    }

    /** The commanders that can lead a seat's deck. They are offered only when there is a choice and this browser may change the seat. */
    private List<CommanderChoice> commanderChoices(final int index, final boolean mayEdit, final Deck deck) {
        final Deck main = catalog.deck(key(index));
        if (!mayEdit || main == null || deck == null || !format().getDeckFormat().hasCommander()) {
            return List.of();
        }
        final List<CommanderOptions.Option> options = CommanderOptions.getOptions(main, format().getDeckFormat());
        if (options.size() < 2) {
            return List.of();
        }
        final List<String> leading = names(deck.getCommanders());
        return options.stream().map(o -> new CommanderChoice(String.join(" + ", names(o.getCommanders())),
                o.getCommanders().get(0).getImageKey(false), o.getKind(), names(o.getCommanders()).equals(leading))).toList();
    }

    /** Returns whether the seat took the avatar. It does not when another seat already holds that avatar. */
    public boolean setAvatar(final int index, final int value) {
        final GameLobby table = view();
        if (value < 0 || value >= SkinSprites.avatarCount() || table == null || avatarsHeld(table, index).contains(value)) {
            return false;
        }
        if (index == local.webSeat()) {
            local.updateOwnSeat(UpdateLobbyPlayerEvent.avatarUpdate(value));
        } else if (host() != null) {
            host().getSlot(index).setAvatarIndex(value);
            local.pushLobby();
        }
        return true;
    }

    private static Set<Integer> avatarsHeld(final GameLobby lobby, final int except) {
        final Set<Integer> held = new HashSet<>();
        for (int i = 0; i < lobby.getNumberOfSlots(); i++) {
            if (i != except) {
                held.add(lobby.getSlot(i).getAvatarIndex());
            }
        }
        return held;
    }

    /** Players are told apart by portrait in a match. A seat that arrives with a portrait an earlier seat holds gets the first free one. */
    public static void distinctAvatars(final GameLobby lobby) {
        final Set<Integer> held = avatarsHeld(lobby, -1);
        final Set<Integer> seen = new HashSet<>();
        for (int i = 0; i < lobby.getNumberOfSlots(); i++) {
            final LobbySlot slot = lobby.getSlot(i);
            if (!seen.add(slot.getAvatarIndex())) {
                int free = 0;
                while (held.contains(free)) {
                    free++;
                }
                slot.setAvatarIndex(free);
                held.add(free);
                seen.add(free);
            }
        }
    }

    public void setSleeve(final int index, final int value) {
        if (value < 0 || value >= SkinSprites.sleeveCount()) {
            return;
        }
        if (index == local.webSeat()) {
            local.updateOwnSeat(UpdateLobbyPlayerEvent.sleeveUpdate(value));
        } else if (host() != null) {
            host().getSlot(index).setSleeveIndex(value);
            local.pushLobby();
        }
    }

    /** Sets how a computer seat plays. Only the host can do this. */
    public void setAiProfile(final int index, final String profile) {
        final GameLobby lobby = host();
        if (lobby == null || index < 0 || index >= lobby.getNumberOfSlots() || lobby.getSlot(index).getType() != LobbySlotType.AI
                || !AiProfileUtil.getAvailableProfiles().contains(profile)) {
            return;
        }
        lobby.getSlot(index).setAiProfile(profile);
        local.pushLobby();
    }

    /** Deals a seat a random sleeve. It picks one that no seat uses yet while there are enough, and never the seat's current sleeve. */
    public void dealSleeve(final int index) {
        final GameLobby lobby = view();
        final int count = SkinSprites.sleeveCount();
        if (lobby == null || index < 0 || index >= lobby.getNumberOfSlots() || count < 2) {
            return;
        }
        final List<Integer> free = new ArrayList<>();
        final List<Integer> notMine = new ArrayList<>();
        for (int n = 0; n < count; n++) {
            boolean worn = false;
            for (int i = 0; i < lobby.getNumberOfSlots(); i++) {
                worn |= lobby.getSlot(i).getSleeveIndex() == n;
            }
            if (!worn) {
                free.add(n);
            }
            if (n != lobby.getSlot(index).getSleeveIndex()) {
                notMine.add(n);
            }
        }
        final List<Integer> from = free.isEmpty() ? notMine : free;
        setSleeve(index, from.get(MyRandom.getRandom().nextInt(from.size())));
    }

    public void setReady(final boolean ready) {
        local.updateOwnSeat(UpdateLobbyPlayerEvent.isReadyUpdate(ready));
    }

    /** Writes a card-art sleeve onto the seat's deck, which every client reads it from. */
    public void setSleeveArt(final int index, final String imageKey, final int offset) {
        final GameLobby lobby = view();
        // Only the host writes the deck file, and the key is written unchanged, so it must name a real card
        if (lobby == null || guest.getAsBoolean() || !dealsFor(lobby, index) || imageKey == null
                || (!imageKey.isEmpty() && ImageUtil.getPaperCardFromImageKey(imageKey) == null)) {
            return;
        }
        catalog.saveSleeveArt(key(index), imageKey, offset);
        // The slot holds a copy of the deck, so the new sleeve reaches the slot only when the deck is sent again
        compose(index);
    }

    /** Saves the avatars and sleeves that the seats chose. The desktop lobby uses the same preferences. */
    public void saveLooks() {
        final GameLobby lobby = view();
        // Several browsers share one set of preferences, so only the machine running the game writes them
        if (lobby == null || !local.isHost()) {
            return;
        }
        // Desktop and mobile store the first two seats only, and the mobile lobby fails on a longer list
        FModel.getPreferences().setPref(FPref.UI_AVATARS, lobby.getSlot(0).getAvatarIndex() + "," + lobby.getSlot(1).getAvatarIndex());
        FModel.getPreferences().setPref(FPref.UI_SLEEVES, lobby.getSlot(0).getSleeveIndex() + "," + lobby.getSlot(1).getSleeveIndex());
        final int mine = local.webSeat();
        if (mine >= 0 && lobby.getSlot(mine).getName() != null) {
            FModel.getPreferences().setPref(FPref.PLAYER_NAME, lobby.getSlot(mine).getName());
        }
        FModel.getPreferences().save();
    }

    /** A table's seats as its host set them up. They are kept across a match, so the next table opens with the same seats and decks. */
    public record Kept(int teams, List<KeptSeat> seats, Map<Integer, SeatChoice> choices) { }

    private record KeptSeat(LobbySlotType type, String name, int avatar, int sleeve, int team, String aiProfile) { }

    /**
     * This browser's choices for one seat. The lobby slot holds the deck itself, and this holds the choice: the deck by catalogue key,
     * the planar deck, scheme deck and avatar, and the commanders picked to lead the deck, by name. composed is the deck last sent, with
     * main deck and extras together. deckAt reads it before the catalogue deck.
     */
    private record SeatChoice(String deckKey, Map<DeckSection, Extra> extras, List<String> commanders, Deck composed) {
        static final SeatChoice NONE = new SeatChoice(null, Map.of(), null, null);

        /** The commanders are kept until another deck is chosen. */
        SeatChoice withDeckKey(final String key) {
            return new SeatChoice(key, extras, Objects.equals(key, deckKey) ? commanders : null, composed);
        }

        SeatChoice withExtra(final DeckSection section, final Extra extra) {
            final Map<DeckSection, Extra> more = new EnumMap<>(DeckSection.class);
            more.putAll(extras);
            more.put(section, extra);
            return new SeatChoice(deckKey, Collections.unmodifiableMap(more), commanders, composed);
        }

        SeatChoice withCommanders(final List<String> names) {
            return new SeatChoice(deckKey, extras, names, composed);
        }

        SeatChoice withComposed(final Deck deck) {
            return new SeatChoice(deckKey, extras, commanders, deck);
        }
    }
}
