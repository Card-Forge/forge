package forge.view;

import java.util.List;

import forge.LobbyPlayer;
import forge.game.GameLog;
import forge.game.GameOutcome;
import forge.game.GameType;
import forge.game.phase.PhaseType;
import forge.game.player.RegisteredPlayer;

public interface IGameView {

    public abstract boolean isCommander();

    public abstract GameType getGameType();

    // Game-related methods
    public abstract int getTurnNumber();
    public abstract boolean isCommandZoneNeeded();
    public abstract boolean isWinner(LobbyPlayer p);
    public abstract LobbyPlayer getWinningPlayer();
    public abstract int getWinningTeam();

    // Match-related methods
    public abstract boolean isFirstGameInMatch();
    public abstract boolean isMatchOver();
    public abstract int getNumGamesInMatch();
    public abstract int getNumPlayedGamesInMatch();
    public abstract boolean isMatchWonBy(LobbyPlayer p);
    public abstract int getGamesWonBy(LobbyPlayer p);
    public abstract GameOutcome.AnteResult getAnteResult();

    public abstract boolean isCombatDeclareAttackers();

    public abstract boolean isGameOver();

    public abstract int getPoisonCountersToLose();

    public abstract void subscribeToEvents(Object subscriber);

    public abstract CombatView getCombat();

    // the following methods should eventually be replaced by methods returning
    // View classes
    @Deprecated
    public abstract GameLog getGameLog();
    @Deprecated
    public abstract RegisteredPlayer getGuiRegisteredPlayer(LobbyPlayer p);

    public abstract List<PlayerView> getPlayers();

    public abstract PlayerView getPlayerTurn();

    public abstract PhaseType getPhase();

    public abstract List<StackItemView> getStack();
    public abstract StackItemView peekStack();

    public abstract boolean mayShowCard(CardView c);

    // Auto-yield related methods
    public abstract Iterable<String> getAutoYields();
    public abstract boolean shouldAutoYield(String key);
    public abstract void setShouldAutoYield(String key, boolean autoYield);
    public abstract boolean getDisableAutoYields();
    public abstract void setDisableAutoYields(boolean b);

    public abstract boolean shouldAlwaysAcceptTrigger(Integer trigger);
    public abstract boolean shouldAlwaysDeclineTrigger(Integer trigger);
    public abstract boolean shouldAlwaysAskTrigger(Integer trigger);

    public abstract void setShouldAlwaysAcceptTrigger(Integer trigger);
    public abstract void setShouldAlwaysDeclineTrigger(Integer trigger);
    public abstract void setShouldAlwaysAskTrigger(Integer trigger);

    public abstract void autoPassUntilEndOfTurn();
    public abstract void autoPassCancel();

    public abstract void devTogglePlayManyLands(boolean b);
    public abstract void devGenerateMana();
    public abstract void devSetupGameState();
    public abstract void devTutorForCard();
    public abstract void devAddCardToHand();
    public abstract void devAddCounterToPermanent();
    public abstract void devTapPermanent();
    public abstract void devUntapPermanent();
    public abstract void devSetPlayerLife();
    public abstract void devWinGame();
    public abstract void devAddCardToBattlefield();
    public abstract void devRiggedPlanerRoll();
    public abstract void devPlaneswalkTo();
}