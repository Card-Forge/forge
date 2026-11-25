package forge.gamemodes.rogue;

import com.google.common.eventbus.Subscribe;
import forge.game.Game;
import forge.game.event.GameEventPlayerPriority;
import forge.game.event.GameEventTurnBegan;
import forge.game.player.Player;
import forge.gamemodes.rogue.path.NodePlanebound;
import forge.gamemodes.rogue.path.NodeType;
import forge.gamemodes.rogue.path.RoguePathNode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/** When each Rogue Commander tutorial is due. The caller skips the ones already seen. */
public final class RogueTutorialTriggers {

    private RogueTutorialTriggers() {
    }

    public static List<RogueTutorial> onMap(RogueRun run) {
        Set<RogueTutorial> due = new LinkedHashSet<>();
        due.add(RogueTutorial.MAP_NAVIGATION);
        if (run != null && run.getEndlessLevel() > 0) {
            due.add(RogueTutorial.ENDLESS_MODE);
        }
        if (run == null || run.getCurrentNode() == null) {
            return new ArrayList<>(due);
        }
        for (RoguePathNode node : run.getPath().getNodes()) {
            if (node.getType() == NodeType.PLANEBOUND && node.isCompleted()) {
                due.add(RogueTutorial.POST_BATTLE);
                break;
            }
        }
        for (RoguePathNode node : run.getPath().getNodesInRow(run.getCurrentNode().getRowIndex())) {
            if (node instanceof NodePlanebound planebound) {
                due.add(planebound.getPlaneboundType() == RoguePlaneboundType.ELITE
                    ? RogueTutorial.ELITE_PLANEBOUND : RogueTutorial.PRE_BATTLE);
            }
        }
        return new ArrayList<>(due);
    }

    public static List<RogueTutorial> onStartPage(RogueMetaProgress progress) {
        List<RogueTutorial> due = new ArrayList<>();
        if (progress.isDescensionModeUnlocked()) {
            due.add(RogueTutorial.DESCENSION_UNLOCKED);
        }
        if (progress.getHighestDescensionWon() >= 1) {
            due.add(RogueTutorial.DESCENSION_LEVEL_1_WIN);
        }
        return due;
    }

    /** Card highlighting when the human gets priority on their second turn; phases and yields as their third turn begins. */
    public static void watchMatch(Game game, RogueMetaProgress progress, Consumer<RogueTutorial> show) {
        if (progress.hasSeenTutorial(RogueTutorial.MATCH_CARD_HIGHLIGHTING)
            && progress.hasSeenTutorial(RogueTutorial.MATCH_PHASES_AND_YIELDS)) {
            return;
        }
        for (Player player : game.getPlayers()) {
            if (player.getController().isGuiPlayer()) {
                game.subscribeToEvents(new MatchListener(player, progress, show));
                return;
            }
        }
    }

    private record MatchListener(Player human, RogueMetaProgress progress, Consumer<RogueTutorial> show) {
        @Subscribe
        public void onPlayerPriority(GameEventPlayerPriority event) {
            if (human.getView().equals(event.turn()) && human.getView().equals(event.priority())
                && human.getTurn() == 2 && !progress.hasSeenTutorial(RogueTutorial.MATCH_CARD_HIGHLIGHTING)) {
                show.accept(RogueTutorial.MATCH_CARD_HIGHLIGHTING);
            }
        }

        @Subscribe
        public void onTurnBegan(GameEventTurnBegan event) {
            // TurnBegan is emitted before the player's turn counter increments
            if (human.getView().equals(event.turnOwner()) && human.getTurn() + 1 == 3
                && !progress.hasSeenTutorial(RogueTutorial.MATCH_PHASES_AND_YIELDS)) {
                show.accept(RogueTutorial.MATCH_PHASES_AND_YIELDS);
            }
        }
    }
}
