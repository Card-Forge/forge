package forge.web.protocol;

import com.google.gson.JsonElement;
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

    @Command("hostChoice")
    public record HostChoiceAnswer(int id, List<Integer> value) {
    }

    @Command("start")
    public record Start(boolean spectate) {
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
