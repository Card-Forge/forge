package forge.ai;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

public class AvailableActionsTest extends AITest {

    private Card convokeSpellWithCreatures(Game game, Player p, int creatures) {
        addCards("Raging Goblin", creatures, p);
        Card stoke = addCardToZone("Stoke the Flames", p, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);
        return stoke;
    }

    // Fails if the sweep follows the AI's combat plan, which convokes nothing before attackers are declared
    @Test
    public void convokeSpellIsActionableBeforeCombat() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card stoke = convokeSpellWithCreatures(game, p, 4);

        AssertJUnit.assertTrue(AvailableActions.collectActionable(p, 5000).contains(stoke.getView()));
    }

    // Fails if the sweep counts more convoke payment than the creatures can give
    @Test
    public void convokeSpellIsNotActionableWithTooFewCreatures() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card stoke = convokeSpellWithCreatures(game, p, 2);

        AssertJUnit.assertFalse(AvailableActions.collectActionable(p, 5000).contains(stoke.getView()));
    }

    // Fails if a mana creature pays once by convoke and again with its mana, because the sweep taps nothing
    @Test
    public void manaCreatureIsNotCountedTwice() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        addCard("Llanowar Elves", p).setSickness(false);
        // Two red creatures pay {R}{R} and the Elves pay one generic, which leaves {1} unpaid
        Card stoke = convokeSpellWithCreatures(game, p, 2);

        AssertJUnit.assertFalse(AvailableActions.collectActionable(p, 5000).contains(stoke.getView()));
    }
}
