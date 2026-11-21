package forge.gamesimulationtests.priority;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import forge.ai.AiCostDecision;
import forge.game.card.Card;
import forge.game.cost.CostPayment;
import forge.game.spellability.SpellAbility;
import org.testng.annotations.Test;

public class TapCostPaymentTest extends PriorityTestBase {
    @Test
    public void tappedAbilitySourceCannotPayTapCost() {
        setUpGame();
        final Card source = addCard("D'Vana Tendi, Optimistic Ensign", a);
        final SpellAbility ability = firstAbility(source);
        ability.setActivatingPlayer(a);
        source.setTapped(true);

        assertFalse(new CostPayment(ability.getPayCosts(), ability)
                .payComputerCosts(new AiCostDecision(a, ability, false)));
        assertTrue(source.isTapped());
    }
}
