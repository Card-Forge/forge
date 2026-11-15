package forge.web.protocol;

import com.google.gson.JsonElement;
import forge.deck.DeckSection;
import forge.game.phase.PhaseType;
import forge.gamemodes.match.DrawOfferMessage;
import forge.gamemodes.match.NextGameDecision;
import forge.web.protocol.MessageJson.Command;
import forge.web.protocol.MessageJson.Nullable;
import forge.web.protocol.MessageJson.Ts;

import java.util.List;

/** Every message the browser sends the server. src/main/ts/protocol.gen.ts is generated from these. */
public final class FromBrowser {
    private FromBrowser() {
    }

    /** Messages that are only a name. */
    public enum Plain { decks, join, lobby, leaveLobby, addresses, cardSets, netDecks, leave, limitedLeave, poolClose, draftDiscard, gauntletNext, gauntletRestart, eventStart, eventNew,
        ok, cancel, endTurn, stopYield, autoPass, undo, concede, macroRecord, macroRepeat,
        editorClose, editorUndo }

    @Command
    public record Bare(Plain t) {
    }

    /** Takes the host's seat. A browser on another machine gives the host password when one is set. */
    @Command("claimHost")
    public record ClaimHost(@Nullable String password) {
    }

    /** The name and avatar this browser plays under. Both are chosen before anything else. A guest's browser also sends the sleeve it last chose. */
    @Command("setName")
    public record SetName(String name, @Nullable Integer avatar, @Nullable Integer sleeve) {
    }

    /** A script error in the page. Without this message, the server's log would never record it. */
    @Command("pageError")
    public record PageError(String text) {
    }

    @Command("ready")
    public record Ready(boolean ready) {
    }

    public enum SeatAction { openSeat, aiSeat, removeSeat }

    @Command
    public record SeatCommand(SeatAction t, int index) {
    }

    /** How many seats the table has. Seats are added and removed at the end. A seat a person holds is never removed. */
    @Command("setPlayerCount")
    public record SetPlayerCount(int count) {
    }

    /** Changes one or more of a seat's choices. A field left out is not changed. A blank name gives an AI seat a new random name.
     *  aiProfile is how an AI seat plays, one of the table's aiProfiles. */
    @Command("setSeat")
    public record SetSeat(int index, @Nullable String name, @Nullable String deck, @Nullable Integer avatar,
            @Nullable Integer sleeve, @Nullable Integer commander, @Nullable String aiProfile) {
    }

    /** How many games the table's match is: 1, 3 or 5. */
    @Command("setMatchLength")
    public record SetMatchLength(int games) {
    }

    /** The highest Commander bracket the table plays at, 1 to 4, or 5 for any. */
    @Command("setMaxBracket")
    public record SetMaxBracket(int bracket) {
    }

    @Command("setFormat")
    public record SetFormat(String format) {
    }

    /** Switches a casual variant on or off. The engine decides what else that switches off. */
    @Command("setVariant")
    public record SetVariant(String variant, boolean on) {
    }

    @Command("setArchenemy")
    public record SetArchenemy(int index) {
    }

    /** How many teams the seats are dealt into, or 0 for a free-for-all. */
    @Command("setTeams")
    public record SetTeams(int count) {
    }

    @Command("setTeam")
    public record SetTeam(int index, int team) {
    }

    /** A seat's planar deck, scheme deck or avatar: the section's name and a choice key from its list. */
    @Command("setSeatExtra")
    public record SetSeatExtra(int index, String section, String choice) {
    }

    @Command("extraChoices")
    public record AskExtraChoices(int index, String section) {
    }

    /** A card pool for Constructed, or null to remove the restriction. */
    @Command("setCardPool")
    public record SetCardPool(@Nullable String cardPool) {
    }

    @Command("deckDetails")
    public record AskDeckDetails(String key) {
    }

    public enum DeckQueryKind { card, sideboard, set }

    /** Which listed decks hold a card in the main deck or the sideboard, or cards from a set. */
    @Command("deckQuery")
    public record DeckQuery(DeckQueryKind kind, String value) {
    }

    @Command("hostChoice")
    public record HostChoiceAnswer(int id, List<Integer> value) {
    }

    @Command("cardSearch")
    public record SearchCards(String query) {
    }

    /** A card's printings. cardPool, when set, marks the printings outside it. */
    @Command("printings")
    public record AskPrintings(String name, @Nullable String cardPool) {
    }

    @Command("sleeveArt")
    public record SleeveArt(int index, String key, int offset) {
    }

    /** Removes a card-art sleeve from the saved ones. A deck that already uses it keeps it. */
    @Command("forgetSleeveArt")
    public record ForgetSleeveArt(String key) {
    }

    @Command("start")
    public record Start(boolean spectate) {
    }

    /** The format the start page's deck finder lists, while no table is open. */
    @Command("browseFormat")
    public record BrowseFormat(String format) {
    }

    /** Opens a deck from the finder by key, or a new deck for a format. seat is the seat whose finder it came from. copy edits a copy. */
    @Command("editorOpen")
    public record EditorOpen(@Nullable String key, @Nullable String newFormat, @Nullable Integer seat, boolean copy) {
    }

    public enum EditOp { add, remove, move, commander, printings, lands, landSet, suggestLands, foil }

    /** A name and a count. The name is a card printing's image key in a printings change, or a basic land's name in a lands change. */
    public record CountedName(String name, int count) {
    }

    @Command("editorEdit")
    public record EditorEdit(EditOp op, @Nullable String name, @Nullable DeckSection from, @Nullable DeckSection to, int count,
            @Nullable List<CountedName> printings, @Nullable List<CountedName> lands) {
    }

    @Command("editorRename")
    public record EditorRename(String name) {
    }

    /** "Check legality against": a format, a card pool for Constructed, or no restriction at all. */
    @Command("editorCheck")
    public record EditorCheck(String format, @Nullable String cardPool, boolean unrestricted) {
    }

    public enum DeckOp { duplicate, delete, file }

    @Command("editorDeck")
    public record EditorDeck(DeckOp op) {
    }

    /** Deletes one of the player's own decks from the finder, without opening it. */
    @Command("deckDelete")
    public record DeckDelete(String key) {
    }

    /** Stars one of the decks the finder lists, or removes its star. */
    @Command("deckFavourite")
    public record DeckFavourite(String key, boolean on) {
    }

    /** A page of the catalogue. request is sent back with the answer, so a late answer to an old query is dropped. */
    @Command("catalogue")
    public record CatalogueQuery(int request, String text, String colours, String type, String filters, String sort, int offset,
            boolean showAll,
            @Nullable List<String> sets, @Nullable String format, @Nullable DeckSection section) {
    }

    @Command("importRead")
    public record ImportRead(int request, String text, String format, @Nullable String cardPool, boolean unrestricted) {
    }

    @Command("importFetch")
    public record ImportFetch(int request, String url) {
    }

    /** use puts the deck on a seat, edit opens it, and save keeps it. add and replace change the deck open in the editor. */
    public enum ImportAction { use, edit, save, add, replace }

    /** What to do when the name is taken: replace that deck, or keep both. */
    public enum Clash { replace, keep }

    @Command("importCommit")
    public record ImportCommit(String text, String name, String format, @Nullable String cardPool, boolean unrestricted,
            ImportAction action, @Nullable Integer seat, @Nullable String url, @Nullable Clash clash) {
    }

    /** A .dck file's text, saved as a deck of the format without using the importer. */
    @Command("deckFile")
    public record DeckFile(String text, String format, @Nullable Clash clash) {
    }

    @Command("reply")
    public record Reply(int id, @Ts("unknown") JsonElement value) {
    }

    /** A click on a card. menu means the right button. x and y place the list of abilities the click may open. */
    @Command("selectCard")
    public record SelectCard(int key, boolean menu, int x, int y) {
    }

    public enum KeyAction { selectPlayer, selectStackItem, detail, playerDetail, stackMenu }

    /** An action on one object in the game, named by its delta key. */
    @Command
    public record KeyCommand(KeyAction t, int key) {
    }

    public enum YieldAction { autoYield, alwaysYes, alwaysNo, yieldToStack, yieldToEntireStack }

    @Command("stackYield")
    public record StackYield(int key, YieldAction action) {
    }

    /** The Always Yes and Always No shortcuts, for the ability on top of the stack. */
    @Command("autoYieldTop")
    public record AutoYieldTop(boolean yes) {
    }

    /** Sets every stop of one row instead of toggling them, so a browser can restore its remembered stops. Null phases means Forge's defaults. */
    @Command("setStops")
    public record SetStops(boolean mine, @Nullable List<PhaseType> phases) {
    }

    public enum PhaseAction { toggleStop, toggleMarker }

    /** A stop, or a pass-priority-until marker, at a phase of the player's own turns (mine) or the opponents' turns. */
    @Command
    public record PhaseCommand(PhaseAction t, PhaseType phase, boolean mine) {
    }

    /** Pays with one colour from the mana pool. color is its bit, as in the player's Mana property. */
    @Command("useMana")
    public record UseMana(byte color) {
    }

    /** How the host's generators build decks. singletons means one copy of each card. */
    @Command("setDeckGen")
    public record SetDeckGen(boolean singletons, boolean noArtifacts) {
    }

    @Command("setSetting")
    public record SetSetting(String key, String value) {
    }

    /** Forge's developer cheats, by their names in IDevModeCheats. state only asks for the current values of the two switches. */
    public enum DevAction { state, unlimitedLands, viewAll, generateMana, tutorForCard, addCardToHand, addCardToBattlefield,
        addTokenToBattlefield, addCardToLibrary, addCardToGraveyard, addCardToExile, repeatLastAddition, castASpell,
        exileCardsFromHand, exileCardsFromBattlefield, removeCardsFromGame, addCountersToPermanent,
        removeCountersFromPermanent, tapPermanents, untapPermanents, setPlayerLife, winGame,
        riggedPlanarRoll, planeswalkTo, setupGameState, dumpGameState }

    /** A cheat for the host's own seat. text is the game state to set up, for setupGameState. */
    @Command("dev")
    public record Dev(DevAction action, @Nullable String text) {
    }

    @Command("nextGame")
    public record NextGame(NextGameDecision decision) {
    }

    /** Offers a draw, or answers another player's offer. */
    @Command("drawOffer")
    public record DrawOfferCommand(DrawOfferMessage.Action action) {
    }

    public enum AutoDecisionAction { list, remove, clear, clearOrders, disableYields, disableTriggers }

    /** Lists or forgets the player's auto-yields and trigger answers, or forgets saved ability orders. disableYields and disableTriggers switch either kind off when on is true. */
    @Command("autoDecisions")
    public record AutoDecisionCommand(AutoDecisionAction action, @Nullable String key, boolean on) {
    }

    /** Every command record. MessageJson decodes commands by these, and the TypeScript is generated from them. */
    static final List<Class<? extends Record>> COMMANDS = MessageJson.marked(Command.class);
}
