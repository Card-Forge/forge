package forge.game;

import java.util.List;

import com.google.common.collect.Iterables;

import forge.LobbyPlayer;
import forge.deck.Deck;
import forge.game.GameOutcome.AnteResult;
import forge.game.card.Card;
import forge.game.card.CardView;
import forge.game.combat.AttackingBand;
import forge.game.combat.Combat;
import forge.game.combat.CombatView;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerView;
import forge.game.spellability.StackItemView;
import forge.game.zone.MagicStack;
import forge.trackable.TrackableCollection;
import forge.trackable.TrackableObject;
import forge.trackable.TrackableProperty;
import forge.util.collect.FCollectionView;

public class GameView extends TrackableObject {
    private static final long serialVersionUID = 8522884512960961528L;

    private final TrackableCollection<PlayerView> players;
    private CombatView combatView;
    private final transient Game game; //TODO: Remove this when possible before network support added

    public GameView(final Game game0) {
        super(-1, game0.getTracker()); //ID not needed
        game = game0;
        set(TrackableProperty.Title, game.getMatch().getTitle());
        set(TrackableProperty.WinningTeam, -1);

        GameRules rules = game.getRules();
        set(TrackableProperty.IsCommander, rules.hasCommander());
        set(TrackableProperty.GameType, rules.getGameType());
        set(TrackableProperty.PoisonCountersToLose, rules.getPoisonCountersToLose());
        set(TrackableProperty.NumGamesInMatch, rules.getGamesPerMatch());

        set(TrackableProperty.GameLog, game.getGameLog());
        set(TrackableProperty.NumPlayedGamesInMatch, game.getMatch().getPlayedGames().size());

        players = PlayerView.getCollection(game.getPlayers());
    }

    public FCollectionView<PlayerView> getPlayers() {
        return players;
    }

    public String getTitle() {
        return get(TrackableProperty.Title);
    }
    public boolean isCommander() {
        return get(TrackableProperty.IsCommander);
    }
    public GameType getGameType() {
        return get(TrackableProperty.GameType);
    }
    public int getPoisonCountersToLose() {
        return get(TrackableProperty.PoisonCountersToLose);
    }
    public int getNumGamesInMatch() {
        return get(TrackableProperty.NumGamesInMatch);
    }

    public int getTurn() {
        return get(TrackableProperty.Turn);
    }
    void updateTurn(PhaseHandler phaseHandler) {
        set(TrackableProperty.Turn, phaseHandler.getTurn());
    }
    public PhaseType getPhase() {
        return get(TrackableProperty.Phase);
    }
    void updatePhase(PhaseHandler phaseHandler) {
        set(TrackableProperty.Phase, phaseHandler.getPhase());
    }
    public PlayerView getPlayerTurn() {
        return get(TrackableProperty.PlayerTurn);
    }
    void updatePlayerTurn(PhaseHandler phaseHandler) {
        set(TrackableProperty.PlayerTurn, PlayerView.get(phaseHandler.getPlayerTurn()));
    }

    public FCollectionView<StackItemView> getStack() {
        return get(TrackableProperty.Stack);
    }
    public StackItemView peekStack() {
        return Iterables.getFirst(getStack(), null);
    }
    public int getStormCount() {
        return get(TrackableProperty.StormCount);
    }
    void updateStack(final MagicStack stack) {
        set(TrackableProperty.Stack, StackItemView.getCollection(stack));
        set(TrackableProperty.StormCount, stack.getSpellsCastThisTurn().size());
    }

    public boolean isFirstGameInMatch() {
        return getNumPlayedGamesInMatch() == 0;
    }
    public int getNumPlayedGamesInMatch() {
        return get(TrackableProperty.NumPlayedGamesInMatch);
    }

    public boolean isGameOver() {
        return get(TrackableProperty.GameOver);
    }
    public boolean isMatchOver() {
        return get(TrackableProperty.MatchOver);
    }
    public String getWinningPlayerName() {
        return get(TrackableProperty.WinningPlayerName);
    }
    public int getWinningTeam() {
        return get(TrackableProperty.WinningTeam);
    }
    void updateGameOver(final Game game) {
        set(TrackableProperty.GameOver, game.isGameOver());
        set(TrackableProperty.MatchOver, game.getMatch().isMatchOver());
        final Player winner = game.getOutcome().getWinningPlayer();
        set(TrackableProperty.WinningPlayerName, winner == null ? null : winner.getName());
        set(TrackableProperty.WinningTeam, game.getOutcome() == null ? -1 : game.getOutcome().getWinningTeam());
    }

    public GameLog getGameLog() {
        return get(TrackableProperty.GameLog);
    }
    void updateGameLog(GameLog gameLog) {
        flagAsChanged(TrackableProperty.GameLog); //don't need to set the property since it won't change
    }

    public CombatView getCombat() {
        return combatView;
    }
    void updateCombat(Combat combat) {
        if (combat == null) {
            combatView = null;
            return;
        }

        combatView = new CombatView(combat.getAttackingPlayer().getGame().getTracker());
        for (final AttackingBand b : combat.getAttackingBands()) {
            if (b == null) continue;
            final GameEntity defender = combat.getDefenderByAttacker(b);
            final List<Card> blockers = combat.getBlockers(b);
            final boolean isBlocked = b.isBlocked() == Boolean.TRUE;
            combatView.addAttackingBand(
                    CardView.getCollection(b.getAttackers()),
                    GameEntityView.get(defender),
                    isBlocked ? CardView.getCollection(blockers) : null,
                    CardView.getCollection(blockers));
        }
    }

    public void serialize() {
        /*try {
            GameStateSerializer serializer = new GameStateSerializer(filename);
            game.saveState(serializer);
            serializer.writeEndOfFile();
            serializer.bw.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }*/
    }

    public void deserialize() {
        /*GameStateDeserializer deserializer = new GameStateDeserializer();
        deserializer.readObject();*/
    }

    //TODO: Find better ways to make this information available to all GUIs without using the Game class

    public boolean isMatchWonBy(LobbyPlayer questPlayer) {
        return game.getMatch().isWonBy(questPlayer);
    }

    public Iterable<GameOutcome> getOutcomesOfMatch() {
        return game.getMatch().getOutcomes();
    }

    public boolean isWinner(LobbyPlayer guiPlayer) {
        return game.getOutcome().isWinner(guiPlayer);
    }

    public int getGamesWonBy(LobbyPlayer questPlayer) {
        return game.getMatch().getGamesWonBy(questPlayer);
    }

    public Deck getDeck(final String lobbyPlayerName) {
        for (final Player p : game.getRegisteredPlayers()) {
            if (p.getLobbyPlayer().getName().equals(lobbyPlayerName)) {
                return p.getRegisteredPlayer().getDeck();
            }
        }
        return null;
    }

    public AnteResult getAnteResult(PlayerView player) {
        return game.getOutcome().anteResult.get(game.getPlayer(player));
    }
}
