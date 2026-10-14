package forge.game;

import org.testng.annotations.Test;

import forge.ai.AITest;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class MetamorphoseTargetingTest extends AITest {

    @Test
    public void casterControlledPermanentIsNotLegalTarget() {
        assertTarget(0, 0, false);
    }

    @Test
    public void opponentControlledPermanentIsLegalTarget() {
        assertTarget(1, 1, true);
    }

    @Test
    public void opponentOwnedCasterControlledPermanentIsNotLegalTarget() {
        assertTarget(1, 0, false);
    }

    @Test
    public void casterOwnedOpponentControlledPermanentIsLegalTarget() {
        assertTarget(0, 1, true);
    }

    private void assertTarget(int ownerIndex, int controllerIndex, boolean expected) {
        Game game = initAndCreateGame();
        Player caster = game.getPlayers().get(0);
        Player opponent = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, caster);

        Card metamorphose = addCardToZone("Metamorphose", caster, forge.game.zone.ZoneType.Hand);
        SpellAbility ability = metamorphose.getFirstSpellAbility();
        ability.setActivatingPlayer(caster);
        Player owner = ownerIndex == 0 ? caster : opponent;
        Player controller = controllerIndex == 0 ? caster : opponent;
        Card target = addCard("Island", owner);
        target.setController(controller, game.getNextTimestamp());
        game.getAction().controllerChangeZoneCorrection(target);
        assertTrue(metamorphose.getZone().is(ZoneType.Hand), "the spell starts in its caster's hand");
        assertTrue(target.getZone().equals(controller.getZone(ZoneType.Battlefield)),
                "target is physically on its controller's battlefield");
        assertTrue(controller.getCardsIn(ZoneType.Battlefield).contains(target),
                "controller's battlefield collection contains the target");
        if (!owner.equals(controller)) {
            assertFalse(owner.getCardsIn(ZoneType.Battlefield).contains(target),
                    "former controller's battlefield no longer contains the target");
        }
        assertTrue(target.getOwner().equals(owner) && target.getController().equals(controller),
                "fixture preserves its requested owner and controller");
        if (expected) {
            assertTrue(ability.canTarget(target), "target legality follows opponent control");
        } else {
            assertFalse(ability.canTarget(target), "target legality follows opponent control");
        }
    }

}
