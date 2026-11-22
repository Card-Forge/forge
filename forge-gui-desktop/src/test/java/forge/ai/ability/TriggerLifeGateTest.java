package forge.ai.ability;

import forge.ai.AITest;
import forge.ai.SpellApiToAi;
import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.zone.ZoneType;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

/**
 * An optional trigger must respect the AI's life-cost safety, the way the activated path already
 * does (the gate lives in SpellAbilityAi.doTrigger). Exercised with Bringer of the Black Dawn's
 * "upkeep: you may pay 2 life, tutor" trigger, asserting both directions: it fires when the AI can
 * spare the life, and declines when paying would drop it below the safety threshold (the bug).
 */
public class TriggerLifeGateTest extends AITest {

    @Test
    public void optionalPayLifeTriggerRespectsLifeSafety() {
        AssertJUnit.assertTrue("should tutor when it can spare the life",
                tutors(bringerSetup(20)));
        AssertJUnit.assertFalse("should not pay 2 life down to 3 to tutor",
                tutors(bringerSetup(5))); // 5 - 2 = 3, below the threshold of 4
    }

    private Player bringerSetup(int life) {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        addCard("Bringer of the Black Dawn", ai);
        addCardToZone("Griselbrand", ai, ZoneType.Library);
        for (int i = 0; i < 6; i++) addCardToZone("Forest", ai, ZoneType.Library);
        ai.setLife(life, null);
        return ai;
    }

    private boolean tutors(Player ai) {
        Card bringer = null;
        for (Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            if (c.getName().equals("Bringer of the Black Dawn")) {
                bringer = c;
                break;
            }
        }
        AssertJUnit.assertNotNull(bringer);
        SpellAbility sa = null;
        for (Trigger t : bringer.getTriggers()) {
            SpellAbility ability = t.ensureAbility();
            if (ability != null && ability.getApi() == ApiType.ChangeZone) {
                sa = ability;
                break;
            }
        }
        AssertJUnit.assertNotNull("no tutor trigger on Bringer", sa);
        sa.setActivatingPlayer(ai);
        return SpellApiToAi.Converter.get(sa).doTrigger(ai, sa, false);
    }
}
