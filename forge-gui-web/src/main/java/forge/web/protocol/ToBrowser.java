package forge.web.protocol;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import forge.card.CardRarity;
import forge.deck.CommanderOptions;
import forge.deck.DeckSection;
import forge.game.GameLogEntryType;
import forge.game.GameLogVerbosity;
import forge.game.phase.PhaseType;
import forge.game.zone.ZoneType;
import forge.gamemodes.match.LobbySlotType;
import forge.gamemodes.net.DeltaPacket;
import forge.player.AutoYieldStore.TriggerDecision;
import forge.web.protocol.FromBrowser.DeckQueryKind;
import forge.web.protocol.FromBrowser.EventKind;
import forge.web.protocol.MessageJson.Event;
import forge.web.protocol.MessageJson.Message;
import forge.web.protocol.MessageJson.Nullable;
import forge.web.protocol.MessageJson.Request;
import forge.web.protocol.MessageJson.Ts;

import java.util.List;
import java.util.Map;

/** Every message the server sends the browser, as records. The build generates src/main/ts/protocol.gen.ts from this file, so a changed record fails the build wherever the browser reads it. */
public final class ToBrowser {
    private ToBrowser() {
    }

    /** A pointer to an object in the game's table, by its delta key. */
    public record Ref(int ref) {
        public static Ref card(final int id) {
            return new Ref(DeltaPacket.makeDeltaKey(DeltaPacket.TYPE_CARD_VIEW, id));
        }

        public static Ref player(final int id) {
            return new Ref(DeltaPacket.makeDeltaKey(DeltaPacket.TYPE_PLAYER_VIEW, id));
        }
    }

    /** The page the browser shows: the start page, a seat being taken, match setup, the Limited pages, or a match. */
    public enum Screen { menu, opening, setup, event, playing }

    /**
     * eventKind is set on the event screen.
     * hostPassword says the host's seat needs the password from this browser.
     */
    @Message("hello")
    public record Hello(Screen screen, boolean spectating, boolean host,
            boolean canClaimHost, boolean hostPassword, boolean networked, @Nullable String playerName, List<Integer> avatars, List<Integer> sleeves, int avatarCount,
            int sleeveCount, List<SavedSleeveArt> sleeveArt, @Nullable String eventPool, int sealedPools,
            @Nullable EventKind eventKind, boolean drafting, int draftPools,
            @Nullable String version, ServerSettings settings, int session) {
    }

    public record SavedSleeveArt(String key, int offset) {
    }

    /** Everyone on this server who has set a name, for the panel that shows who is here. */
    @Message("presence")
    public record Presence(List<Person> people) {
    }

    public enum Doing { waiting, joining, table, playing, watching }

    /** disconnected is true for a guest whose browser has disconnected while the guest holds a seat. */
    public record Person(String name, int avatar, Doing doing, boolean host, boolean disconnected) {
    }

    @Message("error")
    public record ErrorMessage(String message) {
    }

    @Message("notice")
    public record Notice(@Nullable String title, @Nullable String message, boolean error) {
    }

    /** The auto-yields and trigger answers the player has set, and whether either kind is switched off. */
    @Message("autoDecisions")
    public record AutoDecisions(List<AutoDecision> entries, boolean yieldsOff, boolean triggersOff) {
    }

    /** One remembered decision: always yield to it, or always accept or decline the trigger. */
    public record AutoDecision(String key, AutoDecisionKind kind) {
    }

    public enum AutoDecisionKind { yield, accept, decline }

    /** A draw offer: who made it, whether it is still open, and whether this player still has to answer. The browser clears its offer when it closes. */
    @Message("drawOffer")
    public record DrawOffer(@Nullable Ref offerer, boolean open, boolean mine, boolean waitingOnMe) {
    }

    @Message("decks")
    public record Decks(List<DeckSummary> decks, @Nullable String cardPool, @Nullable DeckGenOptions deckGen) {
    }

    /** How the host's generators build their decks, as desktop's lobby sets it. Sent only to the host, because these are the host's preferences. */
    public record DeckGenOptions(boolean singletons, boolean noArtifacts) {
    }

    /**
     * A deck in the finder. A generator's entry is only a name until it is picked, so it has no counts.
     * aiSkips counts the main deck's cards the AI will not play. It is left out when there are none.
     */
    public record DeckSummary(String key, String name, String source, String colors, @Nullable Boolean generated,
            @Nullable String note, @Nullable Integer main, @Nullable Integer sideboard, @Nullable String problem,
            @Nullable List<String> legalIn, @Nullable String formats, @Nullable String sleeveArt,
            @Nullable Integer sleeveOffset, @Nullable Boolean readOnly, @Nullable String sourceUrl,
            @Nullable Integer bracket, @Nullable Integer averageMana, @Nullable Boolean favourite,
            @Nullable Integer aiSkips) {
    }

    /** The keys of the listed decks that a card or set filter matches, for the query it answers. */
    @Message("deckMatches")
    public record DeckMatches(DeckQueryKind kind, String value, List<String> keys) {
    }

    @Message("deckDetails")
    public record DeckDetailsMessage(DeckDetails deck) {
    }

    /** aiSkips names the main deck's cards the AI will not play. It is left out when the AI plays them all. */
    public record DeckDetails(String key, String name, @Nullable String problem, String colors, DeckStats stats,
            List<EditorGroup> main, List<EditorCard> sideboard, @Nullable String sleeveArt, int sleeveOffset,
            @Nullable Bracket bracket, @Nullable List<String> aiSkips) {
    }

    /** A Commander deck's suggested minimum bracket. clear names the kinds of card the deck has none of. */
    public record Bracket(int level, List<BracketReason> reasons, List<String> clear) {
    }

    public enum BracketReasonKind { gameChangers, massLandDenial, extraTurns, chainedExtraTurns, lateCombos, earlyCombos }

    /** One kind of card that affects the bracket. raises is the bracket it puts the deck in, or 0 when it does not raise the bracket by itself. */
    public record BracketReason(BracketReasonKind kind, String title, String brief, List<String> cards, int raises) {
    }

    /**
     * Forge's DeckStatistics for the main deck and commanders, as mobile shows them. total counts the commanders with the main deck.
     * curve counts the nonland cards by mana value, and its last entry holds that value and above. spells is the part of each entry
     * that is instants and sorceries. symbols counts the mana symbols in their costs as W, U, B, R, G and C.
     */
    public record DeckStats(int total, int main, int sideboard, int lands, float averageMana, List<Integer> curve, List<Integer> spells,
            List<TypeCount> types, List<Integer> symbols) {
    }

    public record TypeCount(String name, int count) {
    }

    /** Match setup. No table means no lobby is open. */
    @Message("lobby")
    public record LobbyMessage(@Nullable LobbyTable table) {
    }

    public record LobbyTable(boolean host, int mySeat, boolean shareable, String format,
            @Nullable String cardPool, List<Format> casualVariants, List<String> variantsOn,
            int maxSeats, int gamesPerMatch, List<Seat> seats, List<String> problems, boolean canStart,
            List<String> illegalDecks, boolean legalityEnforced, @Nullable LimitedTable limited, int maxBracket,
            List<String> overBracket, int teams, boolean fixedPairs, List<Integer> teamChoices, List<String> aiProfiles) {
    }

    /** Every entry of the lobby's Format menu, in FormatChoices' order. The deck finder and editor offer the same entries. Sent each time a browser connects. */
    @Message("formats")
    public record Formats(List<Format> formats) {
    }

    /** Every edition, newest first, for the catalogue's set filter. The browser asks for it the first time that filter opens. */
    @Message("cardSets")
    public record CardSets(List<LimitedEdition> sets) {
    }

    /**
     * Constructed, its legality formats by type, the Commander family, the rest, and the casual variants.
     * Limited is the page's own group for Draft and Sealed.
     */
    public enum FormatGroup { Constructed, Sanctioned, Casual, Custom, Commander, Other, Variant, Limited }

    /**
     * A format as the lobby offers it: its group, a description, short facts about its deck and life, and what changes in a match.
     * deckCards is the deck size a player builds to, or null when every deck is dealt. deckAtLeast means the deck may be larger.
     */
    public record Format(String id, String name, FormatGroup group, String desc, List<String> facts, String play, @Nullable Integer deckCards,
            boolean deckAtLeast) {
    }

    /** sittingOut means the seat sits out the next match. aiProfile is how an AI seat plays. */
    public record Seat(@Nullable String name, LobbySlotType type, boolean mine, boolean mayEdit, boolean ready, int avatar,
            int sleeve, @Nullable String deck, @Nullable String deckName, int deckSize, String colors,
            @Nullable String problem, @Nullable String sleeveArt, int sleeveOffset, @Nullable String role,
            @Nullable SeatExtra planes, @Nullable SeatExtra schemes, @Nullable SeatExtra vanguard, boolean sittingOut,
            @Nullable String commander, @Nullable Integer bracket, @Nullable Integer team, List<CommanderChoice> commanderChoices,
            @Nullable String aiProfile) {
    }

    /** A commander that can lead a seat's deck, as desktop's lobby offers it. name is the leader's name, two joined by +. image is the first one's picture.
     *  kind says whether it is the deck's own, one its file suggests, or another card in the deck. chosen says whether it leads now. */
    public record CommanderChoice(String name, String image, CommanderOptions.Kind kind, boolean chosen) {
    }

    /** A planar deck, scheme deck or avatar a seat brings: its name, its size, a detail such as modifiers, and a problem. */
    public record SeatExtra(String label, int count, @Nullable String detail, @Nullable String problem) {
    }

    /** What a seat may choose for one extra section. An avatar choice includes its image and its two modifiers. */
    @Message("extraChoices")
    public record ExtraChoices(String section, int index, List<ExtraChoice> choices) {
    }

    public record ExtraChoice(String key, String label, @Nullable Integer count, @Nullable String problem,
            @Nullable String image, @Nullable Integer hand, @Nullable Integer life) {
    }

    @Message("addresses")
    public record Addresses(List<Address> list) {
    }

    public record Address(String label, String url) {
    }

    /** A line with no sender is an announcement from netplay itself. earlier marks a line replayed from before this browser connected. */
    @Message("chat")
    public record ChatLine(@Nullable String from, String text, boolean earlier) {
    }

    /** hiddenBySwitch counts the matching cards the deck can't use. It is sent only when nothing else matched. */
    @Message("catalogue")
    public record CataloguePage(int request, List<CatalogueRow> rows, int total, int offset, int hiddenBySwitch, boolean ranked) {
    }

    /** The deck open in the editor, or none when the editor is closed. */
    @Message("editor")
    public record EditorMessage(@Nullable EditorState state) {
    }

    /**
     * inBrowser says the deck is saved in a guest's browser, not in the host's decks. copyOf names a read-only deck that the first change will copy.
     * commanderZone says the format has a commander. copyLimit is the most copies allowed of a card other than a basic land, or null for no limit.
     */
    public record EditorState(String name, String check, String format, @Nullable String cardPool, boolean unrestricted,
            boolean inBrowser, @Nullable String copyOf, List<EditorCard> commanders, boolean commanderZone, boolean commanderWanted,
            @Nullable Integer copyLimit, String identity,
            List<EditorGroup> main, List<EditorCard> sideboard, List<EditorLand> lands, DeckStats stats,
            @Nullable String verdict, boolean canUndo, @Nullable String landed,
            boolean limited, @Nullable String landSet, List<LandSet> landSets,
            List<EditorSection> sections) {
    }

    /** A section a deck may have beyond the main deck, sideboard and commander, such as planes or schemes. */
    public record EditorSection(DeckSection section, String name, List<EditorCard> cards) {
    }

    /** An edition basic lands can come from, for a limited deck's land row. */
    public record LandSet(String code, String name) {
    }

    public record EditorGroup(String heading, List<EditorCard> cards) {
    }

    /** One card name in a section, however many printings it is split over. mv and colors let the browser group it in other ways. */
    public record EditorCard(String name, int count, String image, String cost, int mv, String colors, int printings,
            List<EditorPrinting> split, @Nullable String problem, boolean foil) {
    }

    /** How many copies of a card in one section are of one printing, named by its image key. */
    public record EditorPrinting(String key, int count) {
    }

    /** A basic land in the row under the main deck. allowed is false when the deck's commanders forbid it. image is the printing a new copy uses. */
    public record EditorLand(String name, int count, boolean allowed, String image) {
    }

    /** What reading a pasted, fetched or dropped list found: a mark for each line, the problems with their fixes, and the resulting deck. */
    @Message("importResult")
    public record ImportResult(int request, List<ImportLine> lines, List<ImportProblem> problems, ImportSummary summary,
            @Nullable String name, @Nullable Fetched fetched) {
    }

    /** A heading is a section heading or the deck's name. */
    public enum ImportLineKind { read, problem, ignored, heading }

    public record ImportLine(ImportLineKind kind) {
    }

    /** line counts from 0. It is -1 for a problem that belongs to no single line, such as a missing commander. */
    public record ImportProblem(int line, String title, String detail, List<ImportFix> fixes) {
    }

    public enum ImportFixKind { use, leaveOut, commander, other }

    /** text is the name to use, or the commander's. */
    public record ImportFix(ImportFixKind kind, String label, @Nullable String text) {
    }

    public record ImportSummary(int cards, int sideboard, int notImported, @Nullable String commander, boolean commanderChosen,
            String colors, @Nullable String verdict, List<EditorGroup> main, List<EditorCard> sideboardCards) {
    }

    /** A list fetched from a site: which site, the link, the text the list was read from, and the format the site gave it. */
    public record Fetched(String site, String url, String text, String format) {
    }

    /** An import's name is already used by a deck. The browser asks whether to replace that deck or keep both. */
    @Message("nameTaken")
    public record NameTaken(String name) {
    }

    /**
     * ownLimit is the most copies the card's own rules allow in any deck, in place of the format's limit, or null when its rules set none.
     * It is Integer.MAX_VALUE for a card a deck may hold any number of.
     */
    public record CatalogueRow(String name, String image, String cost, int mv, String colors, String type, @Nullable String pt,
            String heading, int inDeck, @Nullable String problem, @Nullable Integer ownLimit) {
    }

    @Message("cardSearch")
    public record CardSearch(List<String> names) {
    }

    @Message("printings")
    public record Printings(String name, List<Printing> printings) {
    }

    /** One printing of a card: its set's code and name, its year, the part of the set it is in (cards, borderless, showcase and so on), and why it can't be used, if it can't. */
    public record Printing(String name, String edition, String key, String setName, int year, @Nullable String style, @Nullable String problem) {
    }

    /** A guest's deck changed. text is the deck as .dck text to store in its browser, or null when the deck was deleted. */
    @Message("deviceDeck")
    public record DeviceDeck(String id, @Nullable String text, String format) {
    }

    /** The deck open in the editor as .dck text, for the browser to save as a file. */
    @Message("deckFileText")
    public record DeckFileText(String name, String text) {
    }

    /** A few buttons, or a list. */
    public enum HostChoiceKind { confirm, choices }

    /** A question outside a match, answered with the indices chosen. */
    @Message("hostChoice")
    public record HostChoice(int id, HostChoiceKind kind, @Nullable String title, @Nullable String message, int min, int max, List<String> options,
            @Nullable List<String> images, @Nullable String pictured) {
    }

    /** A property set to null in a delta has returned to its default. events are in the order the game fired them. */
    @Message("state")
    public record StateMessage(boolean full, long seq, int root,
            @Ts("Record<string, TrackedProps>") Map<String, JsonObject> newObjects,
            @Ts("Record<string, TrackedDelta>") Map<String, JsonObject> deltas,
            List<Integer> visible, List<Integer> localPlayers, @Ts("GameEvent[]") List<Record> events) {
    }

    // Game events describe what happened, never how to show it

    /** A zone that belongs to one player, or to nobody (the stack). */
    public record Place(ZoneType zone, @Nullable Ref player) {
    }

    /** A null from means the card was created there. caster is set for a card put on the stack, because the browser does not have that card yet. */
    @Event("cardMoved")
    public record CardMoved(Ref card, @Nullable Place from, @Nullable Place to, @Nullable Ref caster) {
    }

    @Event("cardDamaged")
    public record CardDamaged(Ref card, @Nullable Ref source, int amount) {
    }

    @Event("playerDamaged")
    public record PlayerDamaged(Ref player, @Nullable Ref source, int amount, boolean combat) {
    }

    public record Attack(Ref attacker, @Nullable Ref defender) {
    }

    @Event("attackersDeclared")
    public record AttackersDeclared(Ref player, List<Attack> attacks) {
    }

    public record Block(Ref blocker, Ref attacker) {
    }

    /** The blocks one defending player made. It is empty when that player blocked no attacker. */
    @Event("blockersDeclared")
    public record BlockersDeclared(Ref player, List<Block> blocks) {
    }

    /** A spell or an ability went onto the stack. */
    @Event("stackAdded")
    public record StackAdded(@Nullable Ref player) {
    }

    /** The card a spell or an ability came from, and whether it did nothing because its targets were gone. */
    @Event("stackResolved")
    public record StackResolved(@Nullable Ref player, @Nullable Ref source, boolean fizzled) {
    }

    /** Life gained or lost in any way, damage included. */
    @Event("lifeChanged")
    public record LifeChanged(Ref player, int from, int to) {
    }

    @Event("playerCounters")
    public record PlayerCounters(Ref player) {
    }

    @Event("cardCounters")
    public record CardCounters(Ref card) {
    }

    /** A null to means the card was removed from what it was attached to. */
    @Event("cardAttached")
    public record CardAttached(Ref card, @Nullable Ref to) {
    }

    @Event("shuffled")
    public record Shuffled(Ref player) {
    }

    /** A game began. The first player is decided before anyone looks at a hand, so it is sent here. */
    @Event("gameStarted")
    public record GameStarted(Ref first) {
    }

    /** The coin toss before a match's first game. winner is the player who won it. */
    @Event("coinTossed")
    public record CoinTossed(Ref winner) {
    }

    static final List<Class<? extends Record>> EVENTS = MessageJson.marked(Event.class);

    /** Why this player chooses who goes first: the player won the coin toss, or lost the last game. */
    public enum StarterChoice { toss, lastGame }

    /** endsTurn is true when the cancel button ends the turn, as the end turn shortcut does, and not when it undoes or declines. */
    @Message("prompt")
    public record Prompt(String message, boolean priority, @Nullable Ref card, PromptButton ok, PromptButton cancel,
            boolean focusOk, boolean paying, boolean endsTurn, List<Ref> selectable, int selectableMin, List<Ref> selectablePlayers,
            List<Integer> selectableStack, List<Integer> highlighted, @Nullable StarterChoice starterChoice) {
    }

    /** The engine's buttons the page treats specially. Each is known by its label, which is the only thing the engine sends. */
    public enum ButtonKind { ok, auto, endTurn, alphaStrike, callBack }

    public record PromptButton(String label, boolean enabled, @Nullable ButtonKind kind) {
    }

    @Message("playable")
    public record Playable(List<Ref> cards, List<Ref> autoTap) {
    }

    @Message("zones")
    public record Zones(List<ShownZone> show) {
    }

    public record ShownZone(Ref player, ZoneType zone) {
    }

    /** The values of dev mode's two switches for the host's seat. */
    @Message("devState")
    public record DevState(boolean unlimitedLands, boolean viewAll) {
    }

    /** The game written out as a game state, for the browser to save as a file. */
    @Message("devDump")
    public record DevDump(String text) {
    }

    @Message("controls")
    public record Controls(List<PhaseType> myStops, List<PhaseType> otherStops, boolean autoPass, @Nullable String dayTime,
            @Nullable TurnMarker marker, boolean untilEndOfTurn, boolean untilStackEmpty, ServerSettings settings) {
    }

    /** Pass priority until this phase of this side's turn. */
    public record TurnMarker(PhaseType phase, boolean mine) {
    }

    /** The Forge preferences the options dialog shares with the desktop client. */
    public record ServerSettings(boolean interruptAttackers, boolean interruptOpponentSpell, boolean interruptTargeting,
            boolean interruptTriggers, boolean interruptMassRemoval, boolean interruptReveal,
            String suggestStackYield, String suggestNoActions, boolean suppressAfterYield, boolean suppressOwnTurn,
            String autoYieldMode, GameLogVerbosity logDetail, boolean devMode,
            String highlightColor, int soundVolume, int musicVolume, String soundSet, List<String> soundSets,
            boolean filteredHands, String mulliganRule, List<String> mulliganRules,
            String aiProfile, List<String> aiProfiles, boolean ante, String undoMode, boolean draftRanking, List<ShortcutPref> shortcuts) {
    }

    /** One of Forge's keyboard shortcut preferences: its name, its label, and its keys and default keys as Java key codes separated by spaces. */
    public record ShortcutPref(String pref, String label, String keys, String defaultKeys) {
    }

    @Message("log")
    public record LogMessage(boolean full, List<LogEntry> entries) {
    }

    public record LogEntry(GameLogEntryType type, String message, @Nullable Integer card, @Nullable String imageKey) {
    }

    @Message("detail")
    public record Detail(int key, List<CardFace> faces) {
    }

    /** One face of a card as the preview shows it. colors is the face's colour mask, in the same form as the Colors property. turn is the clockwise turn in degrees that makes a sideways picture readable. */
    public record CardFace(@Nullable String name, String cost, String type, @Nullable String pt, String text,
            @Nullable String imageKey, int colors, @Nullable String set, @Nullable CardRarity rarity, @Nullable Integer turn) {
    }

    @Message("playerDetail")
    public record PlayerDetail(int key, @Nullable String name, List<String> lines) {
    }

    /** What a right-click on a stack item offers. A field is present only when that choice applies. */
    @Message("stackMenu")
    public record StackMenu(int key, @Nullable Boolean autoYield, @Nullable TriggerDecision trigger) {
    }

    @Message("sound")
    public record Sound(String name, boolean sync) {
    }

    @Message("flash")
    public record Flash() {
    }

    /** A picture the page was told is missing has been downloaded. key is the key the page asked for, and art says it is sleeve art. */
    @Message("pictureReady")
    public record PictureReady(String key, boolean art) {
    }

    /** The game has ended. score is each player's games won in the match so far, once the engine has counted them. */
    @Message("gameOver")
    public record GameOver(List<MatchScore> score) {
    }

    public record MatchScore(Ref player, int won) {
    }

    /**
     * Which fields are set depends on the option: a card on the table, a card that is not (a split pile), or a player.
     * A card name to name has only its imageKey, for its preview. An ability gives the card it comes from as its source. A colour gives its mana symbol's letter.
     */
    public record RequestOption(String label, @Nullable Ref card, @Nullable String name, @Nullable String imageKey,
            @Nullable Ref player, @Nullable Ref source, @Nullable String symbol) {
    }

    /** costs asks for a spell's optional costs, which have no way to decline. */
    public enum ChoiceKind { choices, reveal, costs }

    /**
     * A reveal only shows the list. stackKeys is set when every option is a spell on the stack, so the player picks it on the stack.
     */
    @Request
    public record ChoicesRequest(ChoiceKind kind, @Nullable String message, int min, int max, List<RequestOption> options,
            List<Integer> selected, @Nullable List<Integer> stackKeys, @Nullable Integer atX, @Nullable Integer atY,
            @SerializedName("default") List<Integer> defaultAnswer) {
    }

    public record OrderAnswer(List<Integer> indices, boolean remember) {
    }

    /** bottom is the label of the other end, for an order with a top and a bottom such as a library. */
    @Request("order")
    public record OrderRequest(@Nullable String title, @Nullable String top, @Nullable String bottom, int min, int max, List<RequestOption> options,
            List<Integer> selected, boolean remember, @Nullable Ref card, @SerializedName("default") OrderAnswer defaultAnswer) {
    }

    /** Scry and similar effects: the whole library, with only the top cards movable. */
    @Request("manipulate")
    public record ManipulateRequest(@Nullable String title, List<RequestOption> options, List<Integer> movable, boolean toTop,
            boolean toBottom, @SerializedName("default") List<Integer> defaultAnswer) {
    }

    /** A row of buttons. The answer is the index of the button pressed. */
    @Request("option")
    public record OptionRequest(@Nullable String title, @Nullable String message, @Nullable Ref card, List<String> labels,
            @SerializedName("default") int defaultAnswer) {
    }

    @Request("text")
    public record TextRequest(@Nullable String title, @Nullable String message, @Nullable String initial, boolean numeric,
            @SerializedName("default") @Nullable String defaultAnswer) {
    }

    /** A whole number from min up, and up to max when there is one. When maySkip allows it, null picks no number. */
    @Request("number")
    public record NumberRequest(@Nullable String message, int min, @Nullable Integer max, boolean maySkip,
            @SerializedName("default") @Nullable Integer defaultAnswer) {
    }

    /** Divide an amount among the options. When maySkip allows it, null skips. For combat damage, lethal is the damage that destroys each option. */
    @Request("distribute")
    public record DistributeRequest(@Nullable String message, int amount, int perMin, List<RequestOption> options, @Nullable Ref card,
            boolean maySkip, @SerializedName("default") List<Integer> defaultAnswer, @Nullable List<Integer> lethal) {
    }

    public record SideboardEntry(String name, String imageKey, int total) {
    }

    /** Between games: how many copies of each card go in the main deck. */
    @Request("sideboard")
    public record SideboardRequest(@Nullable String message, List<SideboardEntry> entries, List<Integer> main,
            @SerializedName("default") List<Integer> defaultAnswer) {
    }

    public record LimitedEdition(String code, String name) {
    }

    /** Every message record. The TypeScript is generated from these. */
    static final List<Class<? extends Record>> MESSAGES = MessageJson.marked(Message.class);

    static final List<Class<? extends Record>> REQUESTS = MessageJson.marked(Request.class);
}
