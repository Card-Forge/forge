package forge.game.cost;

import forge.ai.AITest;
import forge.card.mana.ManaCost;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.player.Player;
import forge.game.decision.PriorityCostFeasibility;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;

public class CostAdjustmentPreviewTest extends AITest {

    @Test
    public void previewMatchesForgeForOneFixedStaticReductionWithoutMutation() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final Card divination = addCardToZone("Divination", player, ZoneType.Hand);
        final Card electromancer = addCard("Goblin Electromancer", player);
        final SpellAbility ability = spell(divination);
        ability.setActivatingPlayer(player);

        final int battlefieldBefore = player.getCardsIn(ZoneType.Battlefield).size();
        final boolean electromancerTappedBefore = electromancer.isTapped();
        final CostAdjustmentPreview preview = CostAdjustment.preview(ability.getPayCosts(), ability, player, false);
        final ManaCostBeingPaid actual = new ManaCostBeingPaid(manaCost(ability));
        CostAdjustment.adjust(actual, ability, player, null, true, false);

        assertEquals(preview.getStatus(), CostAdjustmentPreview.Status.ADJUSTED);
        assertNotNull(preview.getAdjustedManaCost());
        assertEquals(preview.getAdjustedManaCost().toManaCost(), actual.toManaCost());
        assertEquals(player.getCardsIn(ZoneType.Battlefield).size(), battlefieldBefore);
        assertEquals(electromancer.isTapped(), electromancerTappedBefore);
    }

    @Test
    public void previewMatchesForgeForFixedStaticIncrease() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final Card bolt = addCardToZone("Lightning Bolt", player, ZoneType.Hand);
        addCard("Thorn of Amethyst", player);
        final SpellAbility ability = spell(bolt);
        ability.setActivatingPlayer(player);

        final CostAdjustmentPreview preview = CostAdjustment.preview(ability.getPayCosts(), ability, player, false);

        assertEquals(preview.getStatus(), CostAdjustmentPreview.Status.ADJUSTED);
        assertEquals(preview.getAdjustedManaCost().toManaCost(), actualAdjustedMana(ability, player));
        assertEquals(preview.getAdjustedManaCost().toManaCost(), new ManaCost("2 R"));
    }

    @Test
    public void previewMatchesForgeForDeterministicSetCost() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final Card memnite = addCardToZone("Memnite", player, ZoneType.Hand);
        addCard("Trinisphere", player);
        final SpellAbility ability = spell(memnite);
        ability.setActivatingPlayer(player);

        final CostAdjustmentPreview preview = CostAdjustment.preview(ability.getPayCosts(), ability, player, false);

        assertEquals(preview.getStatus(), CostAdjustmentPreview.Status.ADJUSTED);
        assertEquals(preview.getAdjustedManaCost().toManaCost(), actualAdjustedMana(ability, player));
        assertEquals(preview.getAdjustedManaCost().toManaCost(), ManaCost.get(3));
    }

    @Test
    public void twoApplicableReductionsRequireAChoiceInsteadOfSelectingAnOrder() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final Card divination = addCardToZone("Divination", player, ZoneType.Hand);
        addCard("Goblin Electromancer", player);
        addCard("Goblin Electromancer", player);

        final SpellAbility ability = spell(divination);
        ability.setActivatingPlayer(player);
        final CostAdjustmentPreview preview = CostAdjustment.preview(ability.getPayCosts(), ability, player, false);

        assertEquals(preview.getStatus(), CostAdjustmentPreview.Status.CHOICE_REQUIRED);
        assertEquals(preview.getReason(), CostAdjustmentPreview.Reason.REDUCTION_ORDER);
        assertFalse(preview.hasAdjustedManaCost());
    }

    @Test
    public void controllerDependentConvokeIsReportedWithoutCallingAController() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final Card spell = addCardToZone("Stoke the Flames", player, ZoneType.Hand);

        final SpellAbility ability = spell(spell);
        ability.setActivatingPlayer(player);
        final CostAdjustmentPreview preview = CostAdjustment.preview(ability.getPayCosts(), ability, player, false);

        assertEquals(preview.getStatus(), CostAdjustmentPreview.Status.CHOICE_REQUIRED);
        assertEquals(preview.getReason(), CostAdjustmentPreview.Reason.CONVOKE);
    }

    @Test
    public void feasibilityUsesTheDeterministicReductionPreview() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final Card divination = addCardToZone("Divination", player, ZoneType.Hand);
        addCard("Goblin Electromancer", player);
        addCard("Island", player);
        addCard("Mountain", player);

        final PriorityCostFeasibility.Assessment assessment = new PriorityCostFeasibility().assessPayment(player, spell(divination));

        assertEquals(assessment.getResult(), PriorityCostFeasibility.Result.PAYABLE);
    }

    @Test
    public void feasibilityAppliesSafeMinimumXBeforeTheFixedReductionPreview() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final Card fireball = addCardToZone("Fireball", player, ZoneType.Hand);
        addCard("Goblin Electromancer", player);
        addCard("Mountain", player);

        final PriorityCostFeasibility.Assessment assessment = new PriorityCostFeasibility().assessPayment(player, spell(fireball));

        assertEquals(assessment.getResult(), PriorityCostFeasibility.Result.PAYABLE);
    }

    @Test
    public void previewMatchesForgeWhenThereIsNoCostAdjustment() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final Card bolt = addCardToZone("Lightning Bolt", player, ZoneType.Hand);
        final SpellAbility ability = spell(bolt);
        ability.setActivatingPlayer(player);

        final CostAdjustmentPreview preview = CostAdjustment.preview(ability.getPayCosts(), ability, player, false);

        assertEquals(preview.getStatus(), CostAdjustmentPreview.Status.ADJUSTED);
        assertEquals(preview.getAdjustedManaCost().toManaCost(), actualAdjustedMana(ability, player));
    }

    private static ManaCost manaCost(final SpellAbility ability) {
        return ability.getPayCosts().getCostMana().getManaCostFor(ability);
    }

    private static ManaCost actualAdjustedMana(final SpellAbility ability, final Player player) {
        final Cost adjustedCost = CostAdjustment.adjust(ability.getPayCosts(), ability, false);
        final ManaCostBeingPaid adjustedMana = new ManaCostBeingPaid(adjustedCost.getCostMana().getManaCostFor(ability));
        CostAdjustment.adjust(adjustedMana, ability, player, null, true, false);
        return adjustedMana.toManaCost();
    }

    private static SpellAbility spell(final Card card) {
        for (final SpellAbility ability : card.getSpellAbilities()) {
            if (ability.isSpell()) {
                return ability;
            }
        }
        throw new AssertionError("No spell ability on " + card.getName());
    }
}
