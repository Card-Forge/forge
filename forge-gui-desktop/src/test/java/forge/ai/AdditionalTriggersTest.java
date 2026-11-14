package forge.ai;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

/**
 * How many times a triggered ability triggers (Roaming Throne, Panharmonicon, ...) is determined when its trigger
 * event happens, not when the trigger is put on the stack. Dack Fayden puts creatures onto the battlefield and gives
 * them to the opponents as part of the same resolution, but Preston still has to trigger an additional time
 * for the Runeclaw Bear, even though by then he is no longer controlled by the player that controls Roaming Throne.
 */
public class AdditionalTriggersTest extends AITest {

    @Test
    public void additionalTriggersAreDeterminedBeforePermanentsAreGivenAway() {
        Game game = initAndCreateThreePlayerGame();
        Player ai = game.getPlayers().get(1);
        int team = 0;
        for (Player p : game.getPlayers()) {
            p.setTeam(team++);
        }

        addCard("Roaming Throne", ai).setChosenType("Rabbit");
        addCards("Plains", 6, ai);
        Card dack = addCardToZone("Dack Fayden, Helping Hand", ai, ZoneType.Hand);
        addCardToZone("Preston, the Vanisher", ai, ZoneType.Library);
        addCardToZone("Runeclaw Bear", ai, ZoneType.Library);
        for (int i = 0; i < 10; i++) {
            addCardToZone("Plains", ai, ZoneType.Library);
        }
        game.getAction().checkStateEffects(true);

        SpellAbility castDack = dack.getSpellAbilities().get(0);
        castDack.setActivatingPlayer(ai);
        AssertJUnit.assertTrue(ComputerUtil.playStack(castDack, ai, game));
        playUntilPhase(game, PhaseType.END_OF_TURN);

        Card preston = findCardWithName(game, "Preston, the Vanisher");
        Card bear = null;
        int bearTokens = 0;
        for (Card c : game.getCardsIn(ZoneType.Battlefield)) {
            if (c.getName().equals("Runeclaw Bear")) {
                if (c.isToken()) {
                    bearTokens++;
                } else {
                    bear = c;
                }
            }
        }
        // the premise: both creatures were given to different opponents
        AssertJUnit.assertNotSame(ai, preston.getController());
        AssertJUnit.assertNotSame(ai, bear.getController());
        AssertJUnit.assertNotSame(preston.getController(), bear.getController());

        AssertJUnit.assertEquals("Preston should have triggered twice for the Runeclaw Bear", 2, bearTokens);
    }
}
