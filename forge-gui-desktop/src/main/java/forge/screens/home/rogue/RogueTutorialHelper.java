package forge.screens.home.rogue;

import com.google.common.eventbus.Subscribe;
import forge.game.Game;
import forge.game.event.GameEventPlayerPriority;
import forge.game.event.GameEventTurnBegan;
import forge.game.player.Player;
import forge.gamemodes.rogue.RogueMetaProgress;
import forge.gamemodes.rogue.RogueTutorial;
import forge.gamemodes.rogue.npc.NPC;
import forge.gamemodes.rogue.npc.NPCContext;
import forge.gui.FThreads;
import forge.toolbox.FOptionPane;
import forge.util.Localizer;
import java.util.List;

/**
 * Helper class for showing Rogue Commander tutorials. Centralizes the logic for checking and
 * displaying tutorials.
 */
public class RogueTutorialHelper {

  private RogueTutorialHelper() {
  }

  /**
   * Show the specified tutorials (always shown, marks as seen). Tutorials are shown in the order
   * provided. Marks as seen BEFORE showing dialog to prevent duplicate popups from concurrent
   * calls.
   */
  public static void show(RogueTutorial... tutorials) {
    RogueMetaProgress progress = RogueMetaProgress.getInstance();
    for (RogueTutorial tutorial : tutorials) {
      progress.markTutorialSeen(tutorial);
      showTutorialDialog(tutorial, progress);
    }
  }

  /**
   * Show the specified tutorials if they haven't been seen yet. Tutorials are shown in the order
   * provided. Marks as seen BEFORE showing dialog to prevent duplicate popups from concurrent
   * calls.
   */
  public static void showIfNotSeen(RogueTutorial... tutorials) {
    RogueMetaProgress progress = RogueMetaProgress.getInstance();
    for (RogueTutorial tutorial : tutorials) {
      if (!hasSeenTutorial(tutorial)) {
        progress.markTutorialSeen(tutorial);
        showTutorialDialog(tutorial, progress);
      }
    }
  }

  public static boolean hasSeenTutorial(RogueTutorial tutorial) {
    RogueMetaProgress progress = RogueMetaProgress.getInstance();
    return progress.hasSeenTutorial(tutorial);
  }

  public static void registerMatchTutorials(Game game) {
    if (hasSeenTutorial(RogueTutorial.MATCH_CARD_HIGHLIGHTING)
        && hasSeenTutorial(RogueTutorial.MATCH_PHASES_AND_YIELDS)) {
      return;
    }
    for (Player player : game.getPlayers()) {
      if (player.getController().isGuiPlayer()) {
        game.subscribeToEvents(new MatchTutorialListener(player));
        return;
      }
    }
  }

  private static void showTutorialDialog(RogueTutorial tutorial, RogueMetaProgress progress) {
    NPCContext context = new NPCContext(
        NPC.TEFERI,
        tutorial.getMessageChunks(),
        List.of(),
        null,
        null);
    NPCDialog.forTutorial(context, () -> confirmSkipTutorials(progress)).show();
  }

  private static boolean confirmSkipTutorials(RogueMetaProgress progress) {
    Localizer localizer = Localizer.getInstance();
    String skipLabel = localizer.getMessage("lblRogueSkipAllTutorials");
    if (!FOptionPane.showConfirmDialog(localizer.getMessage("lblRogueSkipAllTutorialsConfirm"),
        skipLabel, skipLabel, localizer.getMessage("lblCancel"), false)) {
      return false;
    }
    progress.markAllTutorialsSeen();
    return true;
  }

  private static final class MatchTutorialListener {
    private final Player human;

    private MatchTutorialListener(Player human) {
      this.human = human;
    }

    @Subscribe
    public void onPlayerPriority(GameEventPlayerPriority event) {
      if (human.getView().equals(event.turn()) && human.getView().equals(event.priority())
          && human.getTurn() == 2 && !hasSeenTutorial(RogueTutorial.MATCH_CARD_HIGHLIGHTING)) {
        FThreads.invokeInEdtAndWait(() -> showIfNotSeen(RogueTutorial.MATCH_CARD_HIGHLIGHTING));
      }
    }

    @Subscribe
    public void onTurnBegan(GameEventTurnBegan event) {
      // TurnBegan is emitted before the player's turn counter increments.
      if (human.getView().equals(event.turnOwner()) && human.getTurn() + 1 == 3
          && !hasSeenTutorial(RogueTutorial.MATCH_PHASES_AND_YIELDS)) {
        FThreads.invokeInEdtAndWait(() -> showIfNotSeen(RogueTutorial.MATCH_PHASES_AND_YIELDS));
      }
    }
  }
}
