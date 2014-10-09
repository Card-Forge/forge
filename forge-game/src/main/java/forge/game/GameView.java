package forge.game;

import java.util.List;

import forge.game.card.Card;
import forge.game.card.CardView;
import forge.game.combat.AttackingBand;
import forge.game.combat.Combat;
import forge.game.combat.CombatView;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.PlayerView;
import forge.game.spellability.SpellAbilityView;
import forge.game.spellability.StackItemView;
import forge.game.zone.MagicStack;
import forge.trackable.TrackableIndex;
import forge.trackable.TrackableObject;
import forge.trackable.TrackableProperty;

public class GameView extends TrackableObject {
    private final TrackableIndex<CardView> cards = new TrackableIndex<CardView>();
    private final TrackableIndex<PlayerView> players = new TrackableIndex<PlayerView>();
    private final TrackableIndex<SpellAbilityView> spellAbilities = new TrackableIndex<SpellAbilityView>();
    private final TrackableIndex<StackItemView> stackItems = new TrackableIndex<StackItemView>();
    private CombatView combatView;

    public GameView(Game game) {
        super(-1); //ID not needed
        set(TrackableProperty.WinningTeam, -1);

        GameRules rules = game.getRules();
        set(TrackableProperty.Commander, rules.hasAppliedVariant(GameType.Commander));
        set(TrackableProperty.GameType, rules.getGameType());
        set(TrackableProperty.PoisonCountersToLose, rules.getPoisonCountersToLose());
        set(TrackableProperty.NumGamesInMatch, rules.getGamesPerMatch());

        set(TrackableProperty.GameLog, game.getGameLog());
        set(TrackableProperty.NumPlayedGamesInMatch, game.getMatch().getPlayedGames().size());
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
}
