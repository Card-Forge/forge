package forge.ai.simulation;

import java.util.HashSet;
import java.util.Set;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.AITest;
import forge.ai.AiCostDecision;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.cost.CostPart;
import forge.game.cost.CostSacrifice;
import forge.game.cost.PaymentDecision;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Costs that sacrifice several permanents ("a blue creature, a black creature, and a red creature": Demon's Herald)
 * need a different permanent for each part, however many of the parts one multicolored creature fits.
 */
public class SacrificeCostTest extends AITest {
    private Player ai;
    private SpellAbility tutor;

    /** Demon's Herald and its tutor ability with the board of the bug report: three Swamps, Prince of Thralls to find. */
    private void setUp(final String... otherCreatures) {
        final Game game = initAndCreateGame();
        ai = game.getPlayers().get(1);
        final Card herald = addCard("Demon's Herald", ai);
        for (final String name : otherCreatures) {
            addCard(name, ai);
        }
        for (int i = 0; i < 3; i++) {
            addCard("Swamp", ai);
        }
        addCardToZone("Prince of Thralls", ai, ZoneType.Library);
        game.getAction().checkStateEffects(true);
        for (final SpellAbility sa : herald.getSpellAbilities()) {
            if (sa.isActivatedAbility() && sa.getPayCosts() != null && sa.getPayCosts().hasSpecificCostType(CostSacrifice.class)) {
                sa.setActivatingPlayer(ai);
                tutor = sa;
            }
        }
        AssertJUnit.assertNotNull("Demon's Herald has its tutor ability", tutor);
    }

    /** What the AI's payment decides for each sacrifice part, in the order they are decided: false when one has no card. */
    private boolean decidesDifferentCardsForEveryPart() {
        final AiCostDecision decision = new AiCostDecision(ai, tutor, false);
        final Set<Card> chosen = new HashSet<>();
        for (final CostPart part : tutor.getPayCosts().getCostParts()) {
            if (part instanceof CostSacrifice) {
                final PaymentDecision pay = decision.visit((CostSacrifice) part);
                if (pay == null) {
                    return false;
                }
                for (final Card c : pay.cards) {
                    AssertJUnit.assertTrue(c + " is chosen for two parts of the same cost", chosen.add(c));
                }
            }
        }
        return true;
    }

    @Test
    public void oneMulticoloredCreatureCannotPayForThreeSacrifices() {
        setUp("Grixis Grimblade"); // blue, black and red on its own, and the Herald is only black
        AssertJUnit.assertTrue("the planner must not offer an ability that cannot be paid",
                ActivationGuard.sacrificesNeedMoreCards(ai, tutor, tutor.getPayCosts()));
        AssertJUnit.assertFalse("the AI's payment has no red creature left once the Grimblade is the blue one",
                decidesDifferentCardsForEveryPart());
    }

    @Test
    public void threeDifferentCreaturesPayForThreeSacrifices() {
        setUp("Grixis Grimblade", "Raging Goblin"); // the Grimblade is blue, the Herald black, the Goblin red
        AssertJUnit.assertFalse(ActivationGuard.sacrificesNeedMoreCards(ai, tutor, tutor.getPayCosts()));
        AssertJUnit.assertTrue(decidesDifferentCardsForEveryPart());
    }
}
