package forge.game;

import java.util.List;

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
import forge.util.FCollectionView;

public class GameView extends TrackableObject {
    private static GameView currentGame;

    public static GameView getCurrentGame() {
        return currentGame;
    }

    /*private final TrackableIndex<CardView> cards = new TrackableIndex<CardView>();
    private final TrackableIndex<PlayerView> players = new TrackableIndex<PlayerView>();
    private final TrackableIndex<SpellAbilityView> spellAbilities = new TrackableIndex<SpellAbilityView>();
    private final TrackableIndex<StackItemView> stackItems = new TrackableIndex<StackItemView>();*/
    private final TrackableCollection<PlayerView> players;
    private CombatView combatView;
    private final Game game; //TODO: Remove this when possible before network support added

    public GameView(Game game0) {
        super(-1); //ID not needed
        currentGame = this;
        game = game0;
        set(TrackableProperty.WinningTeam, -1);

        GameRules rules = game.getRules();
        set(TrackableProperty.Commander, rules.hasAppliedVariant(GameType.Commander));
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

    public boolean isCommander() {
        return get(TrackableProperty.Commander);
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

    public int getStormCount() {
        return get(TrackableProperty.StormCount);
    }
    void updateStack(MagicStack stack) {
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
    public int getWinningTeam() {
        return get(TrackableProperty.WinningTeam);
    }
    void updateGameOver(Game game) {
        set(TrackableProperty.GameOver, game.isGameOver());
        set(TrackableProperty.MatchOver, game.getMatch().isMatchOver());
        set(TrackableProperty.WinningTeam, game.getOutcome() == null ? -1 : game.getOutcome().getWinningTeam());
        if (game.isGameOver()) {
            currentGame = null;
        }
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

        combatView = new CombatView();
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

    public FCollectionView<StackItemView> getStack() {
        return StackItemView.getCollection(game.getStack());
    }

    public boolean isMatchWonBy(LobbyPlayer questPlayer) {
        return game.getMatch().isWonBy(questPlayer);
    }

    public Iterable<GameOutcome> getOutcomesOfMatch() {
        return game.getMatch().getOutcomes();
    }

    public LobbyPlayer getWinningPlayer() {
        return game.getOutcome().getWinningLobbyPlayer();
    }

    public StackItemView peekStack() {
        return StackItemView.get(game.getStack().peek());
    }

    public boolean isWinner(LobbyPlayer guiPlayer) {
        return game.getOutcome().isWinner(guiPlayer);
    }

    public int getGamesWonBy(LobbyPlayer questPlayer) {
        return game.getMatch().getGamesWonBy(questPlayer);
    }

    public Deck getDeck(LobbyPlayer guiPlayer) {
        for (Player p : game.getRegisteredPlayers()) {
            if (p.getLobbyPlayer().equals(guiPlayer)) {
                return p.getRegisteredPlayer().getDeck();
            }
        }
        return null;
    }

    public AnteResult getAnteResult(PlayerView player) {
        return game.getOutcome().anteResult.get(Player.get(player));
    }
}
