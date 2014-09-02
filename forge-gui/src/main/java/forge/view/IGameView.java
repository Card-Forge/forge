package forge.view;

import java.util.List;

import forge.LobbyPlayer;
import forge.game.GameLog;
import forge.game.GameOutcome;
import forge.game.GameType;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;

public interface IGameView {

    public abstract boolean isCommander();

    public abstract GameType getGameType();

    public abstract boolean isWinner(LobbyPlayer p);
    public abstract LobbyPlayer getWinningPlayer();
    public abstract int getWinningTeam();

    public abstract boolean isFirstGameInMatch();
    public abstract boolean isMatchOver();
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

    public abstract boolean mayShowCard(CardView c, Player viewer);

    public abstract boolean getDisableAutoYields();
    public abstract void setDisableAutoYields(boolean b);

}