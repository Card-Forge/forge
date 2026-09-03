package forge.headless;

import forge.LobbyPlayer;
import forge.ai.PlayerControllerAi;
import forge.game.Game;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

import java.util.List;

/**
 * Test controller that always passes priority (chooses no action).
 * Used for testing that passive play results in a loss.
 */
public class TestPassController extends PlayerControllerAi {

    private int choicesMade = 0;
    private int totalOptions = 0;

    public TestPassController(Game game, Player p, LobbyPlayer lp) {
        super(game, p, lp);
    }

    @Override
    public boolean isAI() {
        return false; // Act like a human player for testing
    }

    @Override
    public List<SpellAbility> chooseSpellAbilityToPlay() {
        // Count this as a choice point
        choicesMade++;

        // We would count options here if we had access to them
        // For now, just always pass (return null)
        return null;
    }

    public int getChoicesMade() {
        return choicesMade;
    }

    public int getTotalOptions() {
        return totalOptions;
    }
}
