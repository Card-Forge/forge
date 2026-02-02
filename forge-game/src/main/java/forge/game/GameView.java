package forge.game;

import java.util.List;
import java.util.Set;

import com.google.common.collect.Iterables;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;

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
import forge.game.player.PlayerView;
import forge.game.player.RegisteredPlayer;
import forge.game.spellability.StackItemView;
import forge.game.staticability.StaticAbility;
import forge.game.staticability.StaticAbilityLayer;
import forge.game.zone.MagicStack;
import forge.trackable.TrackableCollection;
import forge.trackable.TrackableObject;
import forge.trackable.TrackableProperty;
import forge.util.collect.FCollectionView;

public class GameView extends TrackableObject {
    private static final long serialVersionUID = 8522884512960961528L;

    private final transient Game game; //TODO: Remove this when possible before network support added
    private final transient Match match; //TODO: Remove this when possible before network support added

    public GameView(final Game game) {
        super(game.getId(), game.getTracker());
        match = game.getMatch();
        this.game = game;
        set(TrackableProperty.Title, game.getMatch().getTitle());
        set(TrackableProperty.WinningTeam, -1);

        GameRules rules = game.getRules();
        set(TrackableProperty.IsCommander, rules.hasCommander());
        set(TrackableProperty.GameType, rules.getGameType());
        set(TrackableProperty.PoisonCountersToLose, rules.getPoisonCountersToLose());
        set(TrackableProperty.NumGamesInMatch, rules.getGamesPerMatch());

        set(TrackableProperty.GameLog, game.getGameLog());
        set(TrackableProperty.NumPlayedGamesInMatch, game.getMatch().getOutcomes().size());
    }

    public Match getMatch() {
        return match;
    }

    public Game getGame() {
        return game;
    }

    public FCollectionView<PlayerView> getPlayers() {
        return get(TrackableProperty.Players);
    }

    public void updatePlayers(final Game game) {
        set(TrackableProperty.Players, PlayerView.getCollection(game.getPlayers()));
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

    public void updateNeedsPhaseRedrawn(PlayerView p, PhaseType ph) {
        set(TrackableProperty.PlayerTurn, p);
        set(TrackableProperty.Phase, ph);
        set(TrackableProperty.NeedsPhaseRedrawn, true);
    }

    public boolean getNeedsPhaseRedrawn() {
        if (get(TrackableProperty.NeedsPhaseRedrawn) == null)
            return false;
        return get(TrackableProperty.NeedsPhaseRedrawn);
    }

    public void clearNeedsPhaseRedrawn() {
        set(TrackableProperty.NeedsPhaseRedrawn, false);
    }

    public void updatePlanarPlayer(PlayerView p) {
        set(TrackableProperty.PlanarPlayer, p);
    }

    public PlayerView getPlanarPlayer() {
        return get(TrackableProperty.PlanarPlayer);
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

    public boolean isMulligan() {
        if (get(TrackableProperty.Mulligan) == null)
            return false;
        return get(TrackableProperty.Mulligan);
    }

    public void updateIsMulligan(boolean value) {
        set(TrackableProperty.Mulligan, value);
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
        if (game.getOutcome() != null && game.getOutcome().getWinningLobbyPlayer() != null) {
            set(TrackableProperty.WinningPlayerName, game.getOutcome().getWinningLobbyPlayer().getName());
        }
        set(TrackableProperty.WinningTeam, game.getOutcome() == null ? -1 : game.getOutcome().getWinningTeam());
    }

    public GameLog getGameLog() {
        return get(TrackableProperty.GameLog);
    }

    void updateGameLog(GameLog gameLog) {
        flagAsChanged(TrackableProperty.GameLog); //don't need to set the property since it won't change
    }

    public TrackableCollection<CardView> getRevealedCollection() {
        return get(TrackableProperty.RevealedCardsCollection);
    }
    public void updateRevealedCards(TrackableCollection<CardView> collection) {
        set(TrackableProperty.RevealedCardsCollection, collection);
    }

    public String getDependencies() {
        return get(TrackableProperty.Dependencies);
    }
    public void setDependencies(Table<StaticAbility, StaticAbility, Set<StaticAbilityLayer>> dependencies) {
        if (dependencies.isEmpty()) {
            set(TrackableProperty.Dependencies, "");
            return;
        }
        StringBuilder sb = new StringBuilder();
        StaticAbilityLayer layer = null;
        for (StaticAbilityLayer sal : StaticAbilityLayer.CONTINUOUS_LAYERS_WITH_DEPENDENCY) {
            for (Cell<StaticAbility, StaticAbility, Set<StaticAbilityLayer>> dep : dependencies.cellSet()) {
                if (dep.getValue().contains(sal)) {
                    if (layer != sal) {
                        layer = sal;
                        sb.append("Layer " + layer.num).append(": ");
                    }
                    sb.append(dep.getColumnKey().getHostCard().toString()).append(" <- ").append(dep.getRowKey().getHostCard().toString()).append("\n");
                }
            }
        }
        set(TrackableProperty.Dependencies, sb.toString());
    }

    public CombatView getCombat() {
        return get(TrackableProperty.CombatView);
    }
    public void updateCombatView(CombatView combatView) {
        set(TrackableProperty.CombatView, combatView);
    }

    void updateCombat(Combat combat) {
        if (combat == null) {
            set(TrackableProperty.CombatView, null);
            return;
        }

        final CombatView combatView = new CombatView(combat.getAttackingPlayer().getGame().getTracker());
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
        updateCombatView(combatView);
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
        return getMatch().isWonBy(questPlayer);
    }

    public Iterable<GameOutcome> getOutcomesOfMatch() {
        return getMatch().getOutcomes();
    }

    public boolean isWinner(LobbyPlayer guiPlayer) {
        return getOutcome().isWinner(guiPlayer);
    }

    public int getGamesWonBy(LobbyPlayer questPlayer) {
        return getMatch().getGamesWonBy(questPlayer);
    }

    public Deck getDeck(final PlayerView pv) {
        for (final RegisteredPlayer rp : getMatch().getPlayers()) {
            if (pv.isLobbyPlayer(rp.getPlayer())) {
                return rp.getDeck();
            }
        }
        return null;
    }

    public GameOutcome getOutcome() {
        return getMatch().getOutcomeById(getId());
    }

    public AnteResult getAnteResult(PlayerView player) {
        return getOutcome().getAnteResult(player);
    }

    @Override
    public String toString() {
        return String.format("GameView[id=%d, turn=%d, phase=%s, players=%d, gameOver=%b]",
                getId(), getTurn(), getPhase(),
                getPlayers() != null ? getPlayers().size() : 0,
                isGameOver());
    }
}
